import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/widgets/common.dart';
import '../../data/models/vendor.dart';
import '../../data/repositories/vendor_repository.dart';

/// Vendor inbox. Real-time updates will come from WebSocket STOMP (/topic/messages/{roomId}) — TODO.
class VendorMessagesScreen extends StatefulWidget {
  const VendorMessagesScreen({super.key});

  @override
  State<VendorMessagesScreen> createState() => _VendorMessagesScreenState();
}

class _VendorMessagesScreenState extends State<VendorMessagesScreen> {
  late Future<List<ChatThread>> _future;

  @override
  void initState() {
    super.initState();
    _future = context.read<VendorRepository>().getThreads();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        const TabHeader(title: 'Tin nhắn'),
        Expanded(
          child: AsyncView<List<ChatThread>>(
            future: _future,
            builder: (context, threads) => ListView.separated(
              itemCount: threads.length,
              separatorBuilder: (_, __) => const Divider(height: 1, color: AppColors.slate100),
              itemBuilder: (context, i) => _ThreadTile(thread: threads[i]),
            ),
          ),
        ),
      ],
    );
  }
}

class _ThreadTile extends StatelessWidget {
  const _ThreadTile({required this.thread});

  final ChatThread thread;

  @override
  Widget build(BuildContext context) {
    final unread = thread.unread > 0;
    return Material(
      color: unread ? AppColors.pink50 : AppColors.white,
      child: InkWell(
        onTap: () => showAppSnack(context, 'TODO: mở cuộc trò chuyện với ${thread.name}'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              CircleAvatar(
                radius: 28,
                backgroundColor: AppColors.slate100,
                child: Text(thread.name.characters.first, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.slate500)),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(thread.name, style: TextStyle(fontWeight: FontWeight.bold, fontSize: unread ? 15 : 14, color: AppColors.slate800)),
                        ),
                        Text(thread.timeLabel, style: const TextStyle(fontSize: 10, color: AppColors.slate400, fontWeight: FontWeight.w500)),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      thread.lastMessage,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 12,
                        color: unread ? AppColors.slate800 : AppColors.slate500,
                        fontWeight: unread ? FontWeight.w600 : FontWeight.normal,
                      ),
                    ),
                  ],
                ),
              ),
              if (unread) ...[
                const SizedBox(width: 12),
                CircleAvatar(
                  radius: 12,
                  backgroundColor: AppColors.pink500,
                  child: Text('${thread.unread}', style: const TextStyle(fontSize: 12, color: AppColors.white, fontWeight: FontWeight.bold)),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
