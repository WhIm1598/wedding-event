import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/widgets/common.dart';
import '../../data/models/app_notification.dart';
import '../../data/repositories/notification_repository.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  late Future<List<AppNotification>> _future;

  @override
  void initState() {
    super.initState();
    _future = context.read<NotificationRepository>().list();
  }

  static ({IconData icon, Color color, Color bg}) _style(NotificationKind kind) => switch (kind) {
        NotificationKind.ai => (icon: Icons.auto_awesome, color: AppColors.purple500, bg: AppColors.purple50),
        NotificationKind.message => (icon: Icons.chat_bubble_outline, color: AppColors.blue500, bg: AppColors.blue50),
        NotificationKind.sync => (icon: Icons.favorite_border, color: AppColors.pink500, bg: AppColors.pink50),
        NotificationKind.reminder => (icon: Icons.alarm, color: AppColors.amber500, bg: AppColors.amber50),
      };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          const ScreenHeader(title: 'Thông báo'),
          Expanded(
            child: AsyncView<List<AppNotification>>(
              future: _future,
              builder: (context, items) => ListView.separated(
                padding: const EdgeInsets.all(16),
                itemCount: items.length,
                separatorBuilder: (_, __) => const SizedBox(height: 12),
                itemBuilder: (context, i) {
                  final n = items[i];
                  final style = _style(n.kind);
                  return Opacity(
                    opacity: n.isRead ? 0.7 : 1,
                    child: AppCard(
                      padding: const EdgeInsets.all(16),
                      borderColor: n.isRead ? AppColors.slate100 : AppColors.pink100,
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          IconBubble(icon: style.icon, color: style.color, background: style.bg),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  n.title,
                                  style: TextStyle(fontSize: 14, fontWeight: n.isRead ? FontWeight.w600 : FontWeight.bold, color: AppColors.slate800),
                                ),
                                const SizedBox(height: 4),
                                Text(n.body, style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}
