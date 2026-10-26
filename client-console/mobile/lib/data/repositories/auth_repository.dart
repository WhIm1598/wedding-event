import 'package:google_sign_in/google_sign_in.dart';

import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../mock/mock_data.dart';
import '../models/app_user.dart';

/// Module 1: Authentication & Onboarding (FN-AUTH-01..07)
class AuthRepository {
  AuthRepository(this._api);

  final ApiClient _api;

  /// Keeps the token pair of a login response (FN-AUTH-01 step 4) and returns its user
  Future<AppUser> _signIn(Future<Map<String, dynamic>> call) async {
    final data = await call;
    await _api.setTokens(data['accessToken'] as String, data['refreshToken'] as String);
    return AppUser.fromJson(data['user'] as Map<String, dynamic>);
  }

  static Map<String, dynamic> _map(Object? d) => d! as Map<String, dynamic>;

  /// FN-AUTH-06 with the tokens saved by an earlier run; null when there is no session to restore
  Future<AppUser?> restoreSession() async {
    if (Env.useMock || !await _api.restoreTokens()) return null;
    try {
      // An expired access token is refreshed by ApiClient on the way
      return await _api.get('/auth/me', parse: (d) => AppUser.fromJson(_map(d)));
    } on ApiException catch (e) {
      // Refused: the session is gone; offline or a server error: keep the tokens for the next start
      if (e.statusCode == 401 || e.statusCode == 403) await _api.clearTokens();
      return null;
    }
  }

  /// POST /auth/login
  Future<AppUser> login(String email, String password) {
    if (Env.useMock) {
      return mockDelay(AppUser(id: 'u-1', email: email, fullName: 'Ngọc & Hoàng', isVerified: false), 800);
    }
    return _signIn(_api.post('/auth/login', body: {'email': email, 'password': password}, parse: _map));
  }

  bool _googleReady = false;

  /// POST /auth/google with the ID token from the Google Sign-In sheet. Returns null when the user closes the sheet.
  Future<AppUser?> loginWithGoogle() async {
    if (Env.useMock) {
      return mockDelay(const AppUser(id: 'u-1', email: MockData.demoEmail, fullName: 'Ngọc & Hoàng', isVerified: true), 800);
    }
    if (Env.googleServerClientId.isEmpty) {
      throw const ApiException(code: 'INVALID_GOOGLE_TOKEN', message: 'Đăng nhập Google chưa được cấu hình');
    }
    final signIn = GoogleSignIn.instance;
    if (!_googleReady) {
      // initialize() must run exactly once (google_sign_in 7)
      await signIn.initialize(
        serverClientId: Env.googleServerClientId,
        clientId: Env.googleIosClientId.isEmpty ? null : Env.googleIosClientId,
      );
      _googleReady = true;
    }
    final String? idToken;
    try {
      idToken = (await signIn.authenticate()).authentication.idToken;
    } on GoogleSignInException catch (e) {
      if (e.code == GoogleSignInExceptionCode.canceled) return null;
      throw ApiException(code: 'INVALID_GOOGLE_TOKEN', message: 'Không thể đăng nhập bằng Google (${e.code.name})');
    }
    if (idToken == null) {
      throw const ApiException(code: 'INVALID_GOOGLE_TOKEN', message: 'Google không trả về ID token');
    }
    return _signIn(_api.post('/auth/google', body: {'idToken': idToken}, parse: _map));
  }

  /// POST /auth/verify-otp — 4 digits, valid 5 minutes, max 5 attempts
  Future<void> verifyOtp(String email, String otpCode) async {
    if (Env.useMock) {
      await mockDelay(null, 800);
      if (otpCode == '0000') throw const ApiException(code: 'INVALID_OTP', message: 'Mã OTP không chính xác');
      return;
    }
    await _api.post('/auth/verify-otp', body: {'email': email, 'otpCode': otpCode}, parse: (_) {});
  }

  /// POST /auth/send-otp
  Future<void> resendOtp(String email) async {
    if (Env.useMock) return mockDelay(null);
    await _api.post('/auth/send-otp', body: {'email': email}, parse: (_) {});
  }

  /// PUT /users/role (FN-AUTH-03)
  Future<AppUser> selectRole(AppUser user, UserRole role) {
    if (Env.useMock) {
      final named = role == UserRole.vendor ? user.copyWith(fullName: 'Lumiere Studio', hasPlan: true) : user;
      return mockDelay(named.copyWith(role: role), 600);
    }
    return _api.put('/users/role', body: {'role': role.apiValue}, parse: (d) => AppUser.fromJson(d! as Map<String, dynamic>));
  }

  /// PUT /users/me (FN-AUTH-04) — a new e-mail comes back as `pendingEmail` until its OTP is confirmed
  Future<AppUser> updateProfile(AppUser current, {required String fullName, required String email, String? phone}) {
    if (Env.useMock) {
      final changed = email.toLowerCase() != current.email.toLowerCase();
      final updated = AppUser(
        id: current.id,
        email: current.email,
        fullName: fullName,
        phone: phone,
        avatarUrl: current.avatarUrl,
        role: current.role,
        hasPlan: current.hasPlan,
        isVerified: current.isVerified,
        pendingEmail: changed ? email : null,
      );
      return mockDelay(updated);
    }
    return _api.put(
      '/users/me',
      body: {'fullName': fullName, 'email': email, 'phone': phone},
      parse: (d) => AppUser.fromJson(d! as Map<String, dynamic>),
    );
  }

  /// POST /auth/logout (FN-AUTH-07) revokes this device's refresh token; also signs out of Google, so the next
  /// Google login shows the account picker again. Signing out locally never waits on or fails because of the server.
  Future<void> logout() async {
    final refresh = _api.refreshToken;
    if (!Env.useMock && refresh != null) {
      _api.post('/auth/logout', body: {'refreshToken': refresh}, parse: (_) {}).ignore();
    }
    await _api.clearTokens();
    if (!Env.useMock && _googleReady) await GoogleSignIn.instance.signOut();
  }
}
