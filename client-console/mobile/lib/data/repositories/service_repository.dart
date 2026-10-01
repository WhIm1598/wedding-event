import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../mock/mock_data.dart';
import '../models/service_item.dart';

/// Module 4: Service catalog (FN-SVC-01)
class ServiceRepository {
  ServiceRepository(this._api);

  final ApiClient _api;

  /// GET /client/services?category&keyword&page&size
  Future<List<ServiceItem>> search({ServiceCategory? category, String keyword = '', int page = 0, int size = 10}) {
    if (Env.useMock) {
      final k = keyword.trim().toLowerCase();
      return mockDelay(
        MockData.services
            .where((s) => category == null || s.category == category)
            .where((s) => k.isEmpty || s.name.toLowerCase().contains(k) || s.provider.toLowerCase().contains(k))
            .toList(),
        250,
      );
    }
    return _api.get(
      '/client/services',
      query: {'category': category?.name, 'keyword': keyword, 'page': page, 'size': size},
      parse: (d) {
        final content = (d! as Map<String, dynamic>)['content'] as List<dynamic>;
        return content.map((e) => ServiceItem.fromJson(e as Map<String, dynamic>)).toList();
      },
    );
  }

  /// Featured services for the couple home screen
  Future<List<ServiceItem>> featured() => search(size: 2).then((list) => list.take(2).toList());

  /// GET /client/services/{id}
  Future<ServiceItem> getById(String id) {
    if (Env.useMock) return mockDelay(MockData.services.firstWhere((s) => s.id == id), 150);
    return _api.get('/client/services/$id', parse: (d) => ServiceItem.fromJson(d! as Map<String, dynamic>));
  }

  /// POST /messages/send (FN-MSG-01) — "Liên hệ đặt lịch" opens a conversation with the vendor
  Future<void> contactVendor(ServiceItem service, String message) async {
    if (Env.useMock) return mockDelay(null, 600);
    await _api.post('/messages/send', body: {'recipientId': service.id, 'content': message}, parse: (_) {});
  }
}
