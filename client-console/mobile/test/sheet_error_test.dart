import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:wedplanner/core/network/api_client.dart';
import 'package:wedplanner/data/models/service_item.dart';
import 'package:wedplanner/data/repositories/vendor_repository.dart';
import 'package:wedplanner/features/services/widgets/quote_request_sheet.dart';

void main() {
  // #31: a SnackBar would sit under the sheet; the error has to show inside it and the sheet stays open
  testWidgets('a refused quote request shows its error inside the sheet', (tester) async {
    const studioPackage = ServiceItem(
      id: 'svc-1',
      name: 'Chụp ảnh Pre-Wedding',
      provider: 'Lumiere Studio',
      price: 15000000,
      rating: 4.9,
      reviews: 1,
      location: 'Q1',
      category: ServiceCategory.photo,
      imageUrl: '',
      description: '',
    );
    await tester.pumpWidget(Provider<VendorRepository>(
      create: (_) => VendorRepository(ApiClient()),
      child: const MaterialApp(home: Scaffold(body: SingleChildScrollView(child: QuoteRequestSheet(service: studioPackage)))),
    ));

    await tester.tap(find.text('Gửi yêu cầu'));
    await tester.pumpAndSettle();

    expect(
      find.descendant(of: find.byType(QuoteRequestSheet), matching: find.textContaining('Gói của Studio không nhận yêu cầu báo giá')),
      findsOneWidget,
    );
    expect(find.byType(QuoteRequestSheet), findsOneWidget);
  });
}
