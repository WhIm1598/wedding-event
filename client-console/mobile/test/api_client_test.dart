import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wedplanner/core/network/api_client.dart';
import 'package:wedplanner/core/network/api_exception.dart';

/// Fake server: `/auth/me` wants `Bearer new`; `/auth/refresh` answers with [refreshStatus] or no answer at all
class _FakeServer implements HttpClientAdapter {
  int refreshCalls = 0;
  int refreshStatus = 200;
  bool offline = false;

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream, Future<void>? cancelFuture) async {
    if (options.path == '/auth/refresh') {
      refreshCalls++;
      await Future<void>.delayed(const Duration(milliseconds: 20));
      if (offline) throw DioException.connectionError(requestOptions: options, reason: 'offline');
      return refreshStatus == 200
          ? _json(200, {'success': true, 'data': {'accessToken': 'new', 'refreshToken': 'r2', 'user': {}}})
          : _json(refreshStatus, {'success': false, 'code': 'INVALID_REFRESH_TOKEN', 'message': 'Hết hạn'});
    }
    return options.headers['Authorization'] == 'Bearer new'
        ? _json(200, {'success': true, 'data': 'me'})
        : _json(401, {'success': false, 'code': 'UNAUTHORIZED', 'message': 'Hết hạn'});
  }

  static ResponseBody _json(int status, Object body) => ResponseBody.fromString(jsonEncode(body), status, headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      });

  @override
  void close({bool force = false}) {}
}

void main() {
  late _FakeServer server;
  late ApiClient api;
  var expired = 0;

  setUp(() async {
    server = _FakeServer();
    api = ApiClient(dio: Dio(BaseOptions(baseUrl: 'http://test'))..httpClientAdapter = server);
    expired = 0;
    api.onSessionExpired = () => expired++;
    await api.setTokens('old', 'r1');
  });

  Future<String> me() => api.get('/auth/me', parse: (d) => d! as String);

  test('concurrent 401s refresh once and both calls succeed (FN-AUTH-07 single-flight)', () async {
    final results = await Future.wait([me(), me(), me()]);
    expect(results, ['me', 'me', 'me']);
    expect(server.refreshCalls, 1);
    expect(api.refreshToken, 'r2');
  });

  test('a refused refresh token ends the session', () async {
    server.refreshStatus = 401;
    await expectLater(me(), throwsA(isA<ApiException>().having((e) => e.statusCode, 'statusCode', 401)));
    expect(api.refreshToken, isNull);
    expect(expired, 1);
  });

  test('a server error while refreshing (502 during a deploy) keeps the session', () async {
    server.refreshStatus = 502;
    await expectLater(me(), throwsA(isA<ApiException>()));
    expect(api.refreshToken, 'r1');
    expect(expired, 0);
  });

  test('no answer while refreshing keeps the session for later', () async {
    server.offline = true;
    await expectLater(me(), throwsA(isA<ApiException>()));
    expect(api.refreshToken, 'r1');
    expect(expired, 0);
  });
}
