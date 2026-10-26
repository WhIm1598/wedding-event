import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Where the token pair lives between app runs (FN-AUTH-07): Keychain on iOS, Keystore-backed storage on Android.
class TokenStore {
  TokenStore([FlutterSecureStorage? storage]) : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  static const _access = 'accessToken';
  static const _refresh = 'refreshToken';

  Future<({String? access, String? refresh})> read() async =>
      (access: await _storage.read(key: _access), refresh: await _storage.read(key: _refresh));

  Future<void> save(String access, String refresh) async {
    await _storage.write(key: _access, value: access);
    await _storage.write(key: _refresh, value: refresh);
  }

  Future<void> clear() async {
    await _storage.delete(key: _access);
    await _storage.delete(key: _refresh);
  }
}
