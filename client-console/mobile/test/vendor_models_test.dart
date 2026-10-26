import 'package:flutter_test/flutter_test.dart';
import 'package:wedplanner/data/models/vendor.dart';

void main() {
  test('project and quote statuses map to the API names (spec §1.6)', () {
    expect(ProjectStatus.inProgress.apiName, 'IN_PROGRESS');
    expect(ProjectStatus.fromApi('CANCELLED'), ProjectStatus.cancelled);
    expect(QuoteRequestStatus.fromApi('QUOTED'), QuoteRequestStatus.quoted);
    expect(QuoteRequestStatus.quoted.isOpen, isTrue);
    expect(QuoteRequestStatus.accepted.isOpen, isFalse);
  });

  test('VendorProject reads the VendorProjectDto', () {
    final p = VendorProject.fromJson({
      'id': 'p1',
      'coupleName': 'Ngọc & Hoàng',
      'vendorName': 'Dreamy Decor',
      'packageName': 'Trang trí cổng hoa',
      'eventDate': '2026-12-20',
      'venue': null,
      'agreedPrice': 18000000,
      'status': 'IN_PROGRESS',
      'quoteRequestId': 'q1',
      'cancelRequestedAt': '2026-11-01T03:00:00Z',
      'cancelReason': 'Đổi địa điểm',
    });
    expect(p.status, ProjectStatus.inProgress);
    expect(p.eventDate, DateTime(2026, 12, 20));
    expect(p.cancelRequestedAt, isNotNull);
    expect(p.nextSteps, [ProjectStatus.completed]);
  });
}
