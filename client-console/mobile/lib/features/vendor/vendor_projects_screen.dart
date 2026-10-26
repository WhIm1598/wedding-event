import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/widgets/common.dart';
import '../../core/widgets/paged_list.dart';
import '../../data/models/vendor.dart';
import '../../data/repositories/vendor_repository.dart';
import '../../state/session_controller.dart';
import 'widgets/project_card.dart';

/// FN-VND-04. Vendor: the "Dự án" tab, filterable by status. Couple: the vendors they booked (full-screen page).
class VendorProjectsScreen extends StatefulWidget {
  const VendorProjectsScreen({super.key});

  @override
  State<VendorProjectsScreen> createState() => _VendorProjectsScreenState();
}

class _VendorProjectsScreenState extends State<VendorProjectsScreen> {
  late final bool _asVendor;
  ProjectStatus? _filter;

  /// Bumped to restart the list from page 0 (filter change, after an action)
  int _version = 0;

  @override
  void initState() {
    super.initState();
    _asVendor = context.read<SessionController>().isVendor;
  }

  void _reload() => _version++;

  Widget _list() => PagedListView<VendorProject>(
        key: ValueKey('$_filter-$_version'),
        emptyText: 'Chưa có dự án',
        fetch: (page) => context.read<VendorRepository>().getProjects(asVendor: _asVendor, status: _filter, page: page),
        itemBuilder: (_, p) => ProjectCard(project: p, asVendor: _asVendor, onChanged: () => setState(_reload)),
      );

  @override
  Widget build(BuildContext context) {
    if (!_asVendor) {
      return Scaffold(
        body: Column(children: [const ScreenHeader(title: 'Nhà cung cấp đã chốt'), Expanded(child: _list())]),
      );
    }
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
        Expanded(child: _list()),
      ],
    );
  }
}
