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

  /// Optional discounted price of a vendor package: below the price (FN-VND-05)
  static String? discountPrice(String? v, int price) {
    if (v == null || v.trim().isEmpty) return null;
    final n = int.tryParse(v.replaceAll(RegExp(r'[^0-9]'), '')) ?? 0;
    return n <= 0 || n >= price ? 'Giá sau giảm phải thấp hơn giá gốc' : null;
  }

  /// Package features, one per line: at most 20, each at most 200 characters (FN-VND-05)
  static String? features(String? v) {
    final items = (v ?? '').split('\n').map((f) => f.trim()).where((f) => f.isNotEmpty).toList();
    if (items.length > 20) return 'Tối đa 20 quyền lợi';
    return items.any((f) => f.length > 200) ? 'Mỗi quyền lợi tối đa 200 ký tự' : null;
  }

  /// Optional guest count of a quote request: 1–5000 (FN-VND-01)
  static String? optionalGuestCount(String? v) {
    if (v == null || v.trim().isEmpty) return null;
    final n = int.tryParse(v.trim());
    return n == null || n < 1 || n > 5000 ? 'Số khách từ 1 đến 5000' : null;
  }
}
