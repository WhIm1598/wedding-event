import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/vendor.dart';
import '../../data/repositories/vendor_repository.dart';

class VendorProjectsScreen extends StatefulWidget {
  const VendorProjectsScreen({super.key});

  @override
  State<VendorProjectsScreen> createState() => _VendorProjectsScreenState();
}

class _VendorProjectsScreenState extends State<VendorProjectsScreen> {
  ProjectStatus? _filter;
  late Future<List<VendorProject>> _future;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  void _reload() => _future = context.read<VendorRepository>().getProjects(status: _filter);

  static ({Color color, Color bg}) _statusStyle(ProjectStatus s) => switch (s) {
        ProjectStatus.upcoming => (color: AppColors.blue600, bg: AppColors.blue50),
        ProjectStatus.inProgress => (color: AppColors.amber600, bg: AppColors.amber50),
        ProjectStatus.completed => (color: AppColors.emerald600, bg: AppColors.emerald50),
      };

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TabHeader(
          title: 'Dự án của tôi',
          // String keys: PopupMenuButton treats a null value as "cancelled"
          trailing: PopupMenuButton<String>(
            icon: const Icon(Icons.filter_list, color: AppColors.slate600),
            onSelected: (key) => setState(() {
              _filter = ProjectStatus.values.where((s) => s.name == key).firstOrNull;
              _reload();
            }),
            itemBuilder: (_) => [
              const PopupMenuItem(value: 'all', child: Text('Tất cả')),
              for (final s in ProjectStatus.values) PopupMenuItem(value: s.name, child: Text(s.label)),
            ],
          ),
        ),
        Expanded(
          child: AsyncView<List<VendorProject>>(
            future: _future,
            builder: (context, projects) => ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: projects.length,
              separatorBuilder: (_, __) => const SizedBox(height: 16),
              itemBuilder: (context, i) {
                final p = projects[i];
                final style = _statusStyle(p.status);
                return AppCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(p.name, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                                const SizedBox(height: 4),
                                Text(p.service, style: const TextStyle(fontSize: 14, color: AppColors.slate500)),
                              ],
                            ),
                          ),
                          StatusChip(label: p.status.label, color: style.color, background: style.bg),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(color: AppColors.slate50, borderRadius: BorderRadius.circular(8)),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.calendar_today, size: 14, color: AppColors.slate400),
                            const SizedBox(width: 8),
                            Text(Fmt.date(p.date), style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: AppColors.slate600)),
                          ],
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}
