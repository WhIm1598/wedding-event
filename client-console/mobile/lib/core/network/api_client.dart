import 'package:dio/dio.dart';

import '../config/env.dart';
import 'api_exception.dart';

typedef JsonParser<T> = T Function(Object? data);
typedef Json = Map<String, dynamic>;

/// Thin wrapper over Dio that unwraps the standard response envelope:
/// { success, code, message, data } (detailed spec §1.2).
class ApiClient {
  ApiClient()
      : _dio = Dio(
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
          final token = accessToken;
          if (token != null) options.headers['Authorization'] = 'Bearer $token';
          handler.next(options);
        },
      ),
    );
  }

  final Dio _dio;

  /// Set after login; TODO: persist in secure storage and add refresh-token flow.
  String? accessToken;

  Future<T> get<T>(String path, {Map<String, dynamic>? query, required JsonParser<T> parse}) =>
      _send(() => _dio.get<Json>(path, queryParameters: query), parse);

  Future<T> post<T>(String path, {Object? body, required JsonParser<T> parse}) =>
      _send(() => _dio.post<Json>(path, data: body), parse);

  Future<T> put<T>(String path, {Object? body, required JsonParser<T> parse}) =>
      _send(() => _dio.put<Json>(path, data: body), parse);

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
          message: data['message'] as String? ?? 'Lỗi kết nối máy chủ',
          statusCode: e.response?.statusCode,
        );
      }
      throw ApiException(code: 'NETWORK_ERROR', message: e.message ?? 'Lỗi kết nối máy chủ', statusCode: e.response?.statusCode);
    }
  }
}

/// Simulated latency for mock repositories.
Future<T> mockDelay<T>(T value, [int ms = 400]) => Future<T>.delayed(Duration(milliseconds: ms), () => value);
