import 'package:flutter_test/flutter_test.dart';
import 'package:wedplanner/data/models/page_result.dart';
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

  test('PageResult reads the §1.2 page shape and knows when more pages follow', () {
    final first = PageResult.fromJson({'items': [1, 2].map((i) => {'v': i}).toList(), 'page': 0, 'size': 2, 'totalElements': 3, 'totalPages': 2},
        (j) => j['v'] as int);
    expect(first.items, [1, 2]);
    expect(first.hasMore, isTrue);

    final all = List.generate(45, (i) => i);
    expect(PageResult.slice(all, 0, 20).items.length, 20);
    expect(PageResult.slice(all, 2, 20).items, [40, 41, 42, 43, 44]);
    expect(PageResult.slice(all, 2, 20).hasMore, isFalse);
    expect(PageResult.slice(<int>[], 0, 20).hasMore, isFalse);
  });
}
