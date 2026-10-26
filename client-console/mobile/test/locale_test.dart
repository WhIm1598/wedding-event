import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wedplanner/app.dart';

void main() {
  // #32: the date picker and other Material widgets speak Vietnamese and weeks start on Monday
  testWidgets('Material widgets use the Vietnamese locale', (tester) async {
    late MaterialLocalizations l10n;
    await tester.pumpWidget(MaterialApp(
      locale: appLocale,
      supportedLocales: const [appLocale],
      localizationsDelegates: appLocalizationsDelegates,
      home: Builder(builder: (context) {
        l10n = MaterialLocalizations.of(context);
        return const SizedBox();
      }),
    ));
    expect(l10n.cancelButtonLabel, 'Huỷ');
    expect(l10n.firstDayOfWeekIndex, 1);
    expect(l10n.formatMediumDate(DateTime(2026, 10, 26)), isNot(contains('Oct')));
  });
}
