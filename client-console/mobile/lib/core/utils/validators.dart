/// Form validators implementing the detailed spec's validation rules.
class Validators {
  Validators._();

  static final RegExp _email = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$');
  static final RegExp _vnPhone = RegExp(r'^0(3|5|7|8|9)[0-9]{8}$');
  static final RegExp _otp = RegExp(r'^[0-9]{4}$');

  /// FN-AUTH-01
  static String? email(String? v) {
    if (v == null || v.trim().isEmpty) return 'Vui lòng nhập email';
    return _email.hasMatch(v.trim()) ? null : 'Email không hợp lệ';
  }

  /// FN-AUTH-01: at least 6 characters
  static String? password(String? v) => (v == null || v.length < 6) ? 'Mật khẩu tối thiểu 6 ký tự' : null;

  /// FN-AUTH-02
  static bool isOtp(String v) => _otp.hasMatch(v);

  /// FN-GUEST-02: optional, 10 digits starting with 03/05/07/08/09
  static String? optionalPhone(String? v) {
    if (v == null || v.trim().isEmpty) return null;
    return _vnPhone.hasMatch(v.trim()) ? null : 'Số điện thoại không hợp lệ';
  }

  /// FN-GUEST-02: 1-100 characters
  static String? name(String? v) {
    if (v == null || v.trim().isEmpty) return 'Không được để trống';
    return v.trim().length > 100 ? 'Tối đa 100 ký tự' : null;
  }

  /// FN-PLAN-01: minimum 10,000,000 VND
  static String? totalBudget(int amount) => amount < 10000000 ? 'Ngân sách tối thiểu 10.000.000 ₫' : null;
}
