import 'package:flutter/foundation.dart';

import '../data/models/app_user.dart';
import '../data/repositories/auth_repository.dart';

/// App-wide session state: who is logged in, their role and whether they have a plan.
/// The router listens to this to redirect between auth / onboarding / main flows.
class SessionController extends ChangeNotifier {
  SessionController(this._auth);

  final AuthRepository _auth;

  AppUser? _user;
  String? _pendingEmail;

  AppUser? get user => _user;
  bool get isLoggedIn => _user != null;
  bool get needsRole => _user != null && _user!.role == null;
  bool get isVendor => _user?.role == UserRole.vendor;
  bool get hasPlan => _user?.hasPlan ?? false;
  String get pendingEmail => _pendingEmail ?? '';

  /// Returns true when OTP verification is required (FN-AUTH-01 step 4).
  /// The session is only committed after OTP succeeds, so the router keeps the user on /otp.
  Future<bool> login(String email, String password) async {
    final user = await _auth.login(email, password);
    if (!user.isVerified) {
      _pendingEmail = email;
      _pendingUser = user;
      return true;
    }
    _setUser(user);
    return false;
  }

  AppUser? _pendingUser;

  Future<void> verifyOtp(String code) async {
    await _auth.verifyOtp(pendingEmail, code);
    final pending = _pendingUser;
    if (pending != null) _setUser(pending.copyWith(isVerified: true));
    _pendingUser = null;
  }

  Future<void> resendOtp() => _auth.resendOtp(pendingEmail);

  Future<void> loginWithGoogle() async => _setUser(await _auth.loginWithGoogle());

  Future<void> selectRole(UserRole role) async => _setUser(await _auth.selectRole(_user!, role));

  Future<void> updateProfile({required String fullName, required String email, String? phone}) async =>
      _setUser(await _auth.updateProfile(_user!.copyWith(fullName: fullName, email: email, phone: phone)));

  void markPlanCreated() {
    if (_user == null || _user!.hasPlan) return;
    _setUser(_user!.copyWith(hasPlan: true));
  }

  void logout() {
    _auth.logout();
    _user = null;
    _pendingUser = null;
    notifyListeners();
  }

  void _setUser(AppUser user) {
    _user = user;
    notifyListeners();
  }
}
