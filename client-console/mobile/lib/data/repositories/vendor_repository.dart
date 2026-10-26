import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../../core/utils/formatters.dart';
import '../mock/mock_data.dart';
import '../models/service_item.dart';
import '../models/vendor.dart';

/// Vendor workspace, detailed spec Module 4B (FN-VND-01..05), both the couple's and the vendor's side.
/// Calls still throwing [UnimplementedError] work in mock mode only until their ticket lands.
class VendorRepository {
  VendorRepository(this._api);

  final ApiClient _api;

  /// POST /client/quote-requests (FN-VND-01) — couple asks a vendor package for a quote
  Future<QuoteRequest> sendQuoteRequest(ServiceItem service, {DateTime? eventDate, String? venue, int? guestCount, String? message}) {
    if (Env.useMock) {
      if (service.providerId == null) {
        throw const ApiException(code: 'PACKAGE_NOT_QUOTABLE', message: 'Gói của Studio không nhận yêu cầu báo giá, vui lòng liên hệ Studio qua Zalo');
      }
      if (MockData.quoteRequests.any((q) => q.packageId == service.id && q.status.isOpen)) {
        throw const ApiException(code: 'QUOTE_ALREADY_OPEN', message: 'Bạn đã có yêu cầu báo giá đang mở cho gói này');
      }
      final q = QuoteRequest(
        id: 'q-${MockData.quoteRequests.length + 1}',
        coupleName: 'Ngọc & Hoàng',
        packageId: service.id,
        packageName: service.name,
        vendorName: service.provider,
        status: QuoteRequestStatus.pending,
        createdAt: DateTime.now(),
        eventDate: eventDate,
        venue: venue,
        guestCount: guestCount,
        message: message,
      );
      MockData.quoteRequests.insert(0, q);
      return mockDelay(q, 400);
    }
    return _api.post(
      '/client/quote-requests',
      body: {
        'servicePackageId': service.id,
        if (eventDate != null) 'eventDate': Fmt.isoDate(eventDate),
        if (venue != null && venue.isNotEmpty) 'venue': venue,
        if (guestCount != null) 'guestCount': guestCount,
        if (message != null && message.isNotEmpty) 'message': message,
      },
      parse: (d) => QuoteRequest.fromJson(d! as Map<String, dynamic>),
    );
  }

  /// GET /client/quote-requests (couple: sent) or /client/vendor/quote-requests (vendor: received) — FN-VND-02
  Future<List<QuoteRequest>> getQuoteRequests({required bool asVendor, QuoteRequestStatus? status}) {
    if (Env.useMock) {
      return mockDelay(MockData.quoteRequests.where((q) => status == null || q.status == status).toList(), 250);
    }
    return _api.get(
      asVendor ? '/client/vendor/quote-requests' : '/client/quote-requests',
      // shortcut: first 100 requests only, no paging in the app yet; add infinite scroll when vendors get more
      query: {if (status != null) 'status': status.apiName, 'size': 100},
      parse: (d) => ((d! as Map<String, dynamic>)['items'] as List<dynamic>)
          .map((e) => QuoteRequest.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  /// GET /client/vendor/dashboard
  Future<VendorDashboard> getDashboard() {
    if (Env.useMock) return mockDelay(MockData.vendorDashboard());
    throw UnimplementedError('GET /client/vendor/dashboard');
  }

  /// GET /client/vendor/projects
  Future<List<VendorProject>> getProjects({ProjectStatus? status}) {
    if (Env.useMock) {
      return mockDelay(MockData.vendorProjects.where((p) => status == null || p.status == status).toList(), 250);
    }
    throw UnimplementedError('GET /client/vendor/projects');
  }

  /// GET /client/vendor/conversations
  Future<List<ChatThread>> getThreads() {
    if (Env.useMock) return mockDelay(MockData.chatThreads, 250);
    throw UnimplementedError('GET /client/vendor/conversations');
  }

  /// GET /client/vendor/services
  Future<List<VendorService>> getServices() {
    if (Env.useMock) return mockDelay(List.of(MockData.vendorServices), 250);
    throw UnimplementedError('GET /client/vendor/services');
  }

  /// PATCH /client/vendor/services/{id}/active — show/hide a package
  Future<VendorService> setServiceActive(String id, bool isActive) {
    if (Env.useMock) {
      final i = MockData.vendorServices.indexWhere((s) => s.id == id);
      MockData.vendorServices[i] = MockData.vendorServices[i].copyWith(isActive: isActive);
      return mockDelay(MockData.vendorServices[i], 200);
    }
    throw UnimplementedError('PATCH /client/vendor/services/$id/active');
  }
}
