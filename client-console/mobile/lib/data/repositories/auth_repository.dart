import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../mock/mock_data.dart';
import '../models/app_user.dart';

/// Module 1: Authentication & Onboarding (FN-AUTH-01..03)
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

  /// POST /auth/google — Google ID token obtained via google_sign_in (TODO: add plugin)
  Future<AppUser> loginWithGoogle({String idToken = ''}) {
    if (Env.useMock) {
      return mockDelay(const AppUser(id: 'u-1', email: MockData.demoEmail, fullName: 'Ngọc & Hoàng', isVerified: true), 800);
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

  /// PUT /users/me
  Future<AppUser> updateProfile(AppUser user) {
    if (Env.useMock) return mockDelay(user);
    return _api.put(
      '/users/me',
      body: {'fullName': user.fullName, 'email': user.email, 'phone': user.phone},
      parse: (d) => AppUser.fromJson(d! as Map<String, dynamic>),
    );
  }

  void logout() => _api.accessToken = null;
}
