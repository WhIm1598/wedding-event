import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../../core/utils/formatters.dart';
import '../mock/mock_data.dart';
import '../models/service_item.dart';
import '../models/vendor.dart';

/// Vendor workspace, detailed spec Module 4B (FN-VND-01..05), both the couple's and the vendor's side.
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

  /// POST /client/vendor/quote-requests/{id}/quote (FN-VND-03) — vendor quotes, or quotes again
  Future<QuoteRequest> quote(String id, int quotedPrice, {String? vendorNote}) {
    if (Env.useMock) {
      return _mockTransition(id, (q) => q.status.isOpen,
          (q) => q.copyWith(status: QuoteRequestStatus.quoted, quotedPrice: quotedPrice, vendorNote: vendorNote, respondedAt: DateTime.now()));
    }
    return _api.post('/client/vendor/quote-requests/$id/quote',
        body: {'quotedPrice': quotedPrice, if (vendorNote != null && vendorNote.isNotEmpty) 'vendorNote': vendorNote},
        parse: (d) => QuoteRequest.fromJson(d! as Map<String, dynamic>));
  }

  /// POST /client/vendor/quote-requests/{id}/decline (FN-VND-03)
  Future<QuoteRequest> decline(String id, {String? vendorNote}) {
    if (Env.useMock) {
      return _mockTransition(id, (q) => q.status.isOpen,
          (q) => q.copyWith(status: QuoteRequestStatus.declined, vendorNote: vendorNote, respondedAt: DateTime.now()));
    }
    return _api.post('/client/vendor/quote-requests/$id/decline',
        body: {if (vendorNote != null && vendorNote.isNotEmpty) 'vendorNote': vendorNote},
        parse: (d) => QuoteRequest.fromJson(d! as Map<String, dynamic>));
  }

  /// POST /client/quote-requests/{id}/cancel (FN-VND-03) — couple withdraws an open request
  Future<QuoteRequest> cancelQuoteRequest(String id) {
    if (Env.useMock) return _mockTransition(id, (q) => q.status.isOpen, (q) => q.copyWith(status: QuoteRequestStatus.cancelled));
    return _api.post('/client/quote-requests/$id/cancel', parse: (d) => QuoteRequest.fromJson(d! as Map<String, dynamic>));
  }

  /// POST /client/quote-requests/{id}/accept (FN-VND-03) — creates the Vendor Project. [eventDate] is required when the
  /// request had none.
  Future<VendorProject> acceptQuote(QuoteRequest request, {DateTime? eventDate, String? venue}) async {
    if (Env.useMock) {
      final date = request.eventDate ?? eventDate;
      if (date == null) throw const ApiException(code: 'BAD_REQUEST', message: 'Vui lòng chọn ngày tổ chức');
      final accepted = await _mockTransition(request.id, (q) => q.status == QuoteRequestStatus.quoted,
          (q) => q.copyWith(status: QuoteRequestStatus.accepted, eventDate: date, venue: venue));
      final project = VendorProject(
        id: 'p-${MockData.vendorProjects.length + 1}',
        coupleName: accepted.coupleName,
        vendorName: accepted.vendorName,
        packageName: accepted.packageName,
        eventDate: date,
        agreedPrice: accepted.quotedPrice!,
        status: ProjectStatus.upcoming,
        venue: accepted.venue,
        quoteRequestId: accepted.id,
      );
      MockData.vendorProjects.add(project);
      return project;
    }
    return _api.post('/client/quote-requests/${request.id}/accept',
        body: {if (eventDate != null) 'eventDate': Fmt.isoDate(eventDate), if (venue != null && venue.isNotEmpty) 'venue': venue},
        parse: (d) => VendorProject.fromJson(d! as Map<String, dynamic>));
  }

  /// Mock of the §1.6 state machine: 409 INVALID_STATUS_TRANSITION unless [allowed]
  Future<QuoteRequest> _mockTransition(String id, bool Function(QuoteRequest) allowed, QuoteRequest Function(QuoteRequest) change) {
    final i = MockData.quoteRequests.indexWhere((q) => q.id == id);
    if (i < 0) throw const ApiException(code: 'NOT_FOUND', message: 'Không tìm thấy yêu cầu báo giá');
    if (!allowed(MockData.quoteRequests[i])) {
      throw const ApiException(code: 'INVALID_STATUS_TRANSITION', message: 'Yêu cầu báo giá không còn ở trạng thái cho phép thao tác này');
    }
    MockData.quoteRequests[i] = change(MockData.quoteRequests[i]);
    return mockDelay(MockData.quoteRequests[i], 300);
  }

  /// GET /client/vendor/dashboard (FN-VND-05)
  Future<VendorDashboard> getDashboard() {
    if (Env.useMock) return mockDelay(MockData.vendorDashboard());
    return _api.get('/client/vendor/dashboard', parse: (d) => VendorDashboard.fromJson(d! as Map<String, dynamic>));
  }

  /// GET /client/vendor/projects (vendor) or /client/vendor-projects (couple) — FN-VND-04
  Future<List<VendorProject>> getProjects({required bool asVendor, ProjectStatus? status}) {
    if (Env.useMock) {
      return mockDelay(MockData.vendorProjects.where((p) => status == null || p.status == status).toList(), 250);
    }
    return _api.get(
      asVendor ? '/client/vendor/projects' : '/client/vendor-projects',
      // shortcut: first 100 projects only, no paging in the app yet
      query: {if (asVendor && status != null) 'status': status.apiName, 'size': 100},
      parse: (d) => ((d! as Map<String, dynamic>)['items'] as List<dynamic>)
          .map((e) => VendorProject.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  /// PATCH /client/vendor/projects/{id}/status (FN-VND-04) — moving on rejects a pending cancellation request
  Future<VendorProject> updateProjectStatus(VendorProject project, ProjectStatus status) {
    if (Env.useMock) {
      final allowed = status == ProjectStatus.cancelled ? project.isOpen : project.nextSteps.contains(status);
      return _mockProjectChange(project.id, allowed,
          (p) => status == ProjectStatus.cancelled ? p.copyWith(status: status) : p.copyWith(status: status, clearCancel: true));
    }
    return _api.patch('/client/vendor/projects/${project.id}/status',
        body: {'status': status.apiName}, parse: (d) => VendorProject.fromJson(d! as Map<String, dynamic>));
  }

  /// POST /client/vendor-projects/{id}/cancel-request (FN-VND-04) — couple asks, the vendor decides
  Future<VendorProject> requestProjectCancel(VendorProject project, String reason) {
    if (Env.useMock) {
      return _mockProjectChange(project.id, project.isOpen && project.cancelRequestedAt == null,
          (p) => p.copyWith(cancelRequestedAt: DateTime.now(), cancelReason: reason));
    }
    return _api.post('/client/vendor-projects/${project.id}/cancel-request',
        body: {'reason': reason}, parse: (d) => VendorProject.fromJson(d! as Map<String, dynamic>));
  }

  /// POST /client/vendor/projects/{id}/cancel-request/approve|reject (FN-VND-04)
  Future<VendorProject> decideProjectCancel(VendorProject project, {required bool approve}) {
    if (Env.useMock) {
      return _mockProjectChange(project.id, project.isOpen && project.cancelRequestedAt != null,
          (p) => approve ? p.copyWith(status: ProjectStatus.cancelled) : p.copyWith(clearCancel: true));
    }
    return _api.post('/client/vendor/projects/${project.id}/cancel-request/${approve ? 'approve' : 'reject'}',
        parse: (d) => VendorProject.fromJson(d! as Map<String, dynamic>));
  }

  Future<VendorProject> _mockProjectChange(String id, bool allowed, VendorProject Function(VendorProject) change) {
    if (!allowed) {
      throw const ApiException(code: 'INVALID_STATUS_TRANSITION', message: 'Dự án không còn ở trạng thái cho phép thao tác này');
    }
    final i = MockData.vendorProjects.indexWhere((p) => p.id == id);
    MockData.vendorProjects[i] = change(MockData.vendorProjects[i]);
    return mockDelay(MockData.vendorProjects[i], 300);
  }

  /// GET /client/vendor/services (FN-VND-05) — hidden packages included
  Future<List<VendorService>> getServices() {
    if (Env.useMock) return mockDelay(List.of(MockData.vendorServices), 250);
    return _api.get('/client/vendor/services',
        parse: (d) => [for (final e in d! as List<dynamic>) VendorService.fromJson(e as Map<String, dynamic>)]);
  }

  /// POST /client/vendor/services, or PUT /client/vendor/services/{id} when [service] has an id (FN-VND-05)
  Future<VendorService> saveService(VendorService service) {
    if (Env.useMock) {
      if (service.discountPrice != null && service.discountPrice! >= service.price) {
        throw const ApiException(code: 'BAD_REQUEST', message: 'Giá sau giảm phải thấp hơn giá gốc');
      }
      final i = MockData.vendorServices.indexWhere((s) => s.id == service.id);
      final saved = VendorService.fromJson({
        ...service.toJson(),
        'id': i < 0 ? 'vs-${MockData.vendorServices.length + 1}' : service.id,
        'isActive': i < 0 || MockData.vendorServices[i].isActive,
        'projectCount': i < 0 ? 0 : MockData.vendorServices[i].projectCount,
      });
      i < 0 ? MockData.vendorServices.insert(0, saved) : MockData.vendorServices[i] = saved;
      return mockDelay(saved, 300);
    }
    VendorService parse(Object? d) => VendorService.fromJson(d! as Map<String, dynamic>);
    return service.id.isEmpty
        ? _api.post('/client/vendor/services', body: service.toJson(), parse: parse)
        : _api.put('/client/vendor/services/${service.id}', body: service.toJson(), parse: parse);
  }

  /// PATCH /client/vendor/services/{id}/active (FN-VND-05) — show/hide a package in the catalog
  Future<VendorService> setServiceActive(String id, bool isActive) {
    if (Env.useMock) {
      final i = MockData.vendorServices.indexWhere((s) => s.id == id);
      MockData.vendorServices[i] = MockData.vendorServices[i].copyWith(isActive: isActive);
      return mockDelay(MockData.vendorServices[i], 200);
    }
    return _api.patch('/client/vendor/services/$id/active',
        body: {'isActive': isActive}, parse: (d) => VendorService.fromJson(d! as Map<String, dynamic>));
  }
}
