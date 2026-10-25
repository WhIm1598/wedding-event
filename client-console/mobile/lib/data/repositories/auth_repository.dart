import 'package:google_sign_in/google_sign_in.dart';

import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../mock/mock_data.dart';
import '../models/app_user.dart';

/// Module 1: Authentication & Onboarding (FN-AUTH-01..04, Google login)
class AuthRepository {
  AuthRepository(this._api);

  final ApiClient _api;

  AppUser _readLogin(Object? data) {
    final map = data! as Map<String, dynamic>;
    _api.accessToken = map['accessToken'] as String?;
    return AppUser.fromJson(map['user'] as Map<String, dynamic>);
  }

  /// POST /auth/login
  Future<AppUser> login(String email, String password) {
    if (Env.useMock) {
      return mockDelay(AppUser(id: 'u-1', email: email, fullName: 'Ngọc & Hoàng', isVerified: false), 800);
    }
    return _api.post('/auth/login', body: {'email': email, 'password': password}, parse: _readLogin);
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
    return _api.post('/auth/google', body: {'idToken': idToken}, parse: _readLogin);
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

  /// Also signs out of Google, so the next Google login shows the account picker again
  void logout() {
    _api.accessToken = null;
    if (!Env.useMock && _googleReady) GoogleSignIn.instance.signOut();
  }
}
