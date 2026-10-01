import 'package:flutter_test/flutter_test.dart';
import 'package:wedplanner/core/utils/formatters.dart';
import 'package:wedplanner/core/utils/validators.dart';

void main() {
  group('Fmt', () {
    test('formats VND with dot separators', () {
      expect(Fmt.vnd(15000000), '15.000.000 ₫');
      expect(Fmt.thousands(999), '999');
      expect(Fmt.thousands(1000), '1.000');
    });

    test('formats millions compactly', () {
      expect(Fmt.millions(150000000), '150tr');
      expect(Fmt.millions(2500000), '2.5tr');
    });

    test('formats dates', () {
      final d = DateTime(2026, 10, 5);
      expect(Fmt.date(d), '05/10/2026');
      expect(Fmt.isoDate(d), '2026-10-05');
      expect(Fmt.longDate(d), '5 Tháng 10, 2026');
    });

    test('counts days until a date', () {
      expect(Fmt.daysUntil(DateTime(2026, 10, 20), now: DateTime(2026, 10, 1, 23, 59)), 19);
    });

    test('parses money input', () {
      expect(Fmt.parseMoney('300.000.000 ₫'), 300000000);
      expect(Fmt.parseMoney(''), 0);
    });
  });

  group('Validators', () {
    test('Vietnamese phone numbers (FN-GUEST-02)', () {
      expect(Validators.optionalPhone('0987654321'), isNull);
      expect(Validators.optionalPhone(''), isNull);
      expect(Validators.optionalPhone('0187654321'), isNotNull);
      expect(Validators.optionalPhone('098765432'), isNotNull);
    });

    test('OTP is exactly 4 digits (FN-AUTH-02)', () {
      expect(Validators.isOtp('8421'), isTrue);
      expect(Validators.isOtp('842'), isFalse);
      expect(Validators.isOtp('84a1'), isFalse);
    });

    test('minimum plan budget (FN-PLAN-01)', () {
      expect(Validators.totalBudget(9999999), isNotNull);
      expect(Validators.totalBudget(10000000), isNull);
    });
  });
}
