import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../mock/mock_data.dart';
import '../models/app_notification.dart';

/// In-app notification inbox. Push delivery is via Firebase Cloud Messaging (TODO: firebase_messaging).
class NotificationRepository {
  NotificationRepository(this._api);

  final ApiClient _api;

  /// GET /client/notifications
  Future<List<AppNotification>> list() {
    if (Env.useMock) return mockDelay(MockData.notifications, 250);
    return _api.get('/client/notifications', parse: (d) {
      return (d! as List<dynamic>).map((e) {
        final m = e as Map<String, dynamic>;
        return AppNotification(
          id: m['id'] as String,
          title: m['title'] as String,
          body: m['body'] as String? ?? '',
          kind: NotificationKind.values.firstWhere((k) => k.name == m['kind'], orElse: () => NotificationKind.reminder),
          isRead: m['isRead'] as bool? ?? false,
        );
      }).toList();
    });
  }
}
