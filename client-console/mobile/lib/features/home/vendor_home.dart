import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/vendor.dart';
import '../../data/repositories/vendor_repository.dart';
import '../../state/session_controller.dart';
import 'couple_home.dart' show NotificationBellButton;

class VendorHome extends StatefulWidget {
  const VendorHome({super.key});

  @override
  State<VendorHome> createState() => _VendorHomeState();
}

class _VendorHomeState extends State<VendorHome> {
  late Future<VendorDashboard> _dashboard;

  @override
  void initState() {
    super.initState();
    _dashboard = context.read<VendorRepository>().getDashboard();
  }

  @override
  Widget build(BuildContext context) {
    final name = context.watch<SessionController>().user?.fullName ?? '';

    return AsyncView<VendorDashboard>(
      future: _dashboard,
      builder: (context, d) => Column(
        children: [
          Container(
            padding: EdgeInsets.fromLTRB(24, MediaQuery.paddingOf(context).top + 16, 24, 32),
            decoration: const BoxDecoration(
              color: AppColors.slate900,
              borderRadius: BorderRadius.vertical(bottom: Radius.circular(40)),
            ),
            child: Column(
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Dashboard', style: TextStyle(color: AppColors.slate400, fontSize: 14, fontWeight: FontWeight.w500)),
                          Text(name, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.white)),
                        ],
                      ),
                    ),
                    const NotificationBellButton(background: AppColors.slate800, color: AppColors.white),
                  ],
                ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    _StatBox(label: 'Doanh thu', value: Fmt.millions(d.monthlyRevenue), color: AppColors.white),
                    const SizedBox(width: 12),
                    _StatBox(label: 'Yêu cầu', value: '${d.newRequestCount} mới', color: AppColors.pink500),
                    const SizedBox(width: 12),
                    _StatBox(label: 'Đánh giá', value: '${d.rating} ★', color: AppColors.amber400),
                  ],
                ),
              ],
            ),
          ),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(24),
              children: [
                const Text('Yêu cầu báo giá mới', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                const SizedBox(height: 16),
                for (final q in d.quoteRequests)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: AppCard(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          const IconBubble(icon: Icons.person_outline, color: AppColors.slate500, background: AppColors.slate100, size: 40),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(q.customerName, style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.slate800)),
                                Text(q.packageName, style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
                              ],
                            ),
                          ),
                          TextButton(
                            style: TextButton.styleFrom(backgroundColor: AppColors.pink50, foregroundColor: AppColors.pink600),
                            onPressed: () => showAppSnack(context, 'TODO: mở hội thoại với ${q.customerName}'),
                            child: const Text('Phản hồi', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                          ),
                        ],
                      ),
                    ),
                  ),
                const SizedBox(height: 12),
                const Text('Lịch trình sắp tới', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                const SizedBox(height: 16),
                for (final p in d.upcoming) _UpcomingCard(project: p),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _StatBox extends StatelessWidget {
  const _StatBox({required this.label, required this.value, required this.color});

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: AppColors.slate800, borderRadius: BorderRadius.circular(16)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: const TextStyle(fontSize: 12, color: AppColors.slate400)),
            const SizedBox(height: 4),
            Text(value, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: color)),
          ],
        ),
      ),
    );
  }
}

class _UpcomingCard extends StatelessWidget {
  const _UpcomingCard({required this.project});

  final VendorProject project;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Row(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(color: AppColors.blue50, borderRadius: BorderRadius.circular(12)),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('TH ${project.date.month}', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.blue600)),
                Text('${project.date.day}', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.blue600)),
              ],
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(project.name, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.slate800)),
                if (project.venue != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Row(
                      children: [
                        const Icon(Icons.place_outlined, size: 14, color: AppColors.slate500),
                        const SizedBox(width: 4),
                        Text(project.venue!, style: const TextStyle(fontSize: 13, color: AppColors.slate500)),
                      ],
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
