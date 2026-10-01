/// Formatting helpers for VND money and Vietnamese dates (no locale data needed).
class Fmt {
  Fmt._();

  /// 15000000 -> "15.000.000"
  static String thousands(int value) {
    final digits = value.abs().toString();
    final buffer = StringBuffer();
    for (var i = 0; i < digits.length; i++) {
      if (i > 0 && (digits.length - i) % 3 == 0) buffer.write('.');
      buffer.write(digits[i]);
    }
    return value < 0 ? '-$buffer' : buffer.toString();
  }

  /// 15000000 -> "15.000.000 ₫"
  static String vnd(int value) => '${thousands(value)} ₫';

  /// 150000000 -> "150tr"
  static String millions(int value) {
    final m = value / 1000000;
    return m == m.roundToDouble() ? '${m.round()}tr' : '${m.toStringAsFixed(1)}tr';
  }

  static String _two(int n) => n.toString().padLeft(2, '0');

  /// 2026-10-20 -> "20/10/2026"
  static String date(DateTime d) => '${_two(d.day)}/${_two(d.month)}/${d.year}';

  /// 2026-10-20 -> "20/10"
  static String dayMonth(DateTime d) => '${_two(d.day)}/${_two(d.month)}';

  /// 2026-10-20 -> "20 Tháng 10, 2026"
  static String longDate(DateTime d) => '${d.day} Tháng ${d.month}, ${d.year}';

  /// ISO date for API payloads: "2026-10-20"
  static String isoDate(DateTime d) => '${d.year}-${_two(d.month)}-${_two(d.day)}';

  /// Days from today (date-only) to [d]; negative if in the past.
  static int daysUntil(DateTime d, {DateTime? now}) {
    final today = now ?? DateTime.now();
    final a = DateTime(today.year, today.month, today.day);
    final b = DateTime(d.year, d.month, d.day);
    return b.difference(a).inDays;
  }

  /// Parses user input like "300.000.000" -> 300000000
  static int parseMoney(String raw) => int.tryParse(raw.replaceAll(RegExp(r'[^0-9]'), '')) ?? 0;
}
