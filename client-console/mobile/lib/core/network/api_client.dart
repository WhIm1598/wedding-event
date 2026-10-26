import 'package:dio/dio.dart';

import '../config/env.dart';
import 'api_exception.dart';
import 'token_store.dart';

typedef JsonParser<T> = T Function(Object? data);
typedef Json = Map<String, dynamic>;

/// Thin wrapper over Dio that unwraps the standard response envelope:
/// { success, code, message, data } (detailed spec §1.2).
///
/// Session (FN-AUTH-07): keeps the token pair (persisted in [TokenStore]); a `401` from the API triggers one
/// refresh — single-flight, since sending the same refresh token twice counts as theft and revokes every session —
/// and the request is sent again. A refused refresh clears the tokens and calls [onSessionExpired].
class ApiClient {
  ApiClient({TokenStore? store, Dio? dio})
      : _store = store,
        _dio = dio ??
            Dio(
              BaseOptions(
                baseUrl: Env.apiBaseUrl,
                connectTimeout: const Duration(seconds: 10),
                receiveTimeout: const Duration(seconds: 30),
                contentType: Headers.jsonContentType,
              ),
            ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          final token = _access;
          if (token != null && options.extra['noAuth'] != true) options.headers['Authorization'] = 'Bearer $token';
          handler.next(options);
        },
        onError: (e, handler) async {
          final req = e.requestOptions;
          final canRetry = e.response?.statusCode == 401 && _refresh != null && req.extra['retried'] != true && !_sessionCalls.contains(req.path);
          if (!canRetry || !await _refreshOnce()) return handler.next(e);
          req.extra['retried'] = true;
          req.headers['Authorization'] = 'Bearer $_access';
          try {
            handler.resolve(await _dio.fetch<dynamic>(req));
          } on DioException catch (retryError) {
            handler.next(retryError);
          }
        },
      ),
    );
  }

  /// A 401 from these means wrong credentials or a dead refresh token, not an expired access token
  static const _sessionCalls = {'/auth/login', '/auth/google', '/auth/refresh', '/auth/logout'};

  final Dio _dio;
  final TokenStore? _store;
  String? _access;
  String? _refresh;
  Future<bool>? _refreshing;

  static const _networkError = 'Không kết nối được máy chủ, vui lòng kiểm tra mạng và thử lại';

  /// Called when the server refuses the refresh token: the user must sign in again
  void Function()? onSessionExpired;

  String? get refreshToken => _refresh;

  /// Loads the pair saved by an earlier run; true when there is one to try
  Future<bool> restoreTokens() async {
    final saved = await _store?.read();
    _access = saved?.access;
    _refresh = saved?.refresh;
    return _refresh != null;
  }

  Future<void> setTokens(String access, String refresh) async {
    _access = access;
    _refresh = refresh;
    await _store?.save(access, refresh);
  }

  Future<void> clearTokens() async {
    _access = null;
    _refresh = null;
    await _store?.clear();
  }

  Future<bool> _refreshOnce() => _refreshing ??= _doRefresh().whenComplete(() => _refreshing = null);

  Future<bool> _doRefresh() async {
    try {
      final res = await _dio.post<Json>('/auth/refresh', data: {'refreshToken': _refresh}, options: Options(extra: {'noAuth': true}));
      final data = res.data?['data'] as Json;
      await setTokens(data['accessToken'] as String, data['refreshToken'] as String);
      return true;
    } on DioException catch (e) {
      // Only a refusal ends the session; no answer (offline) or a server error (502 during a deploy) keeps it
      final status = e.response?.statusCode;
      if (status != 401 && status != 403) return false;
      await clearTokens();
      onSessionExpired?.call();
      return false;
    }
  }

  Future<T> get<T>(String path, {Map<String, dynamic>? query, required JsonParser<T> parse}) =>
      _send(() => _dio.get<Json>(path, queryParameters: query), parse);

  Future<T> post<T>(String path, {Object? body, required JsonParser<T> parse}) =>
      _send(() => _dio.post<Json>(path, data: body), parse);

  Future<T> put<T>(String path, {Object? body, required JsonParser<T> parse}) =>
      _send(() => _dio.put<Json>(path, data: body), parse);

  Future<T> delete<T>(String path, {Map<String, dynamic>? query, required JsonParser<T> parse}) =>
      _send(() => _dio.delete<Json>(path, queryParameters: query), parse);

  Future<T> patch<T>(String path, {Object? body, required JsonParser<T> parse}) =>
      _send(() => _dio.patch<Json>(path, data: body), parse);

  Future<T> _send<T>(Future<Response<Json>> Function() call, JsonParser<T> parse) async {
    try {
      final res = await call();
      final payload = res.data ?? const <String, dynamic>{};
      if (payload['success'] != true) {
        throw ApiException(
          code: payload['code'] as String? ?? 'UNKNOWN',
          message: payload['message'] as String? ?? 'Đã có lỗi xảy ra',
          statusCode: res.statusCode,
        );
      }
      return parse(payload['data']);
    } on DioException catch (e) {
      final data = e.response?.data;
      if (data is Map<String, dynamic>) {
        throw ApiException(
          code: data['code'] as String? ?? 'NETWORK_ERROR',
          message: data['message'] as String? ?? _networkError,
          statusCode: e.response?.statusCode,
        );
      }
      // Offline, timeout or a non-envelope answer (gateway HTML): never show Dio's English text
      throw ApiException(code: 'NETWORK_ERROR', message: _networkError, statusCode: e.response?.statusCode);
    }
  }
}

/// Simulated latency for mock repositories.
Future<T> mockDelay<T>(T value, [int ms = 400]) => Future<T>.delayed(Duration(milliseconds: ms), () => value);
