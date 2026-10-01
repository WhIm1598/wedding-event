enum NotificationKind { ai, message, sync, reminder }

class AppNotification {
  const AppNotification({required this.id, required this.title, required this.body, required this.kind, this.isRead = false});

  final String id;
  final String title;
  final String body;
  final NotificationKind kind;
  final bool isRead;
}

class ChatMessage {
  const ChatMessage({required this.id, required this.text, required this.fromUser});

  final String id;
  final String text;
  final bool fromUser;
}
