import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../mock/mock_data.dart';
import '../models/vendor.dart';

/// Vendor workspace (feature spec §4.3). Specified in the detailed spec as FN-VND-01..05 (Module 4B, v1.1.3) but
/// client-console/backend doesn't serve them yet, so these work in mock mode only.
class VendorRepository {
  VendorRepository(this._api);

  // ignore: unused_field
  final ApiClient _api;

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
