import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/vendor.dart';
import '../../data/repositories/vendor_repository.dart';

/// Vendor service packages: list, show/hide (spec §4.3 "Quản lý Danh mục Dịch vụ")
class ServiceManagementScreen extends StatefulWidget {
  const ServiceManagementScreen({super.key});

  @override
  State<ServiceManagementScreen> createState() => _ServiceManagementScreenState();
}

class _ServiceManagementScreenState extends State<ServiceManagementScreen> {
  late Future<List<VendorService>> _future;
  List<VendorService> _services = [];

  @override
  void initState() {
    super.initState();
    _future = context.read<VendorRepository>().getServices().then((list) => _services = list);
  }

  Future<void> _toggle(VendorService s, bool active) async {
    final updated = await context.read<VendorRepository>().setServiceActive(s.id, active);
    if (!mounted) return;
    setState(() => _services = [for (final x in _services) x.id == s.id ? updated : x]);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          const ScreenHeader(title: 'Quản lý dịch vụ'),
          Expanded(
            child: AsyncView<List<VendorService>>(
              future: _future,
              builder: (context, _) => ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  for (final s in _services)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: AppCard(
                        padding: const EdgeInsets.all(16),
                        child: Row(
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(12),
                              child: Image.network(
                                s.imageUrl,
                                width: 80,
                                height: 80,
                                fit: BoxFit.cover,
                                errorBuilder: (_, __, ___) => Container(width: 80, height: 80, color: AppColors.slate100),
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(s.name, maxLines: 2, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                                  const SizedBox(height: 4),
                                  Text(Fmt.vnd(s.price), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.pink600)),
                                  const SizedBox(height: 4),
                                  Row(
                                    children: [
                                      s.isActive
                                          ? const StatusChip(label: 'Đang hoạt động', color: AppColors.emerald600, background: AppColors.emerald50)
                                          : const StatusChip(label: 'Tạm ẩn', color: AppColors.slate500, background: AppColors.slate100),
                                      const Spacer(),
                                      Text('Đã bán: ${s.bookings}', style: const TextStyle(fontSize: 10, color: AppColors.slate500)),
                                    ],
                                  ),
                                ],
                              ),
                            ),
                            Switch(value: s.isActive, onChanged: (v) => _toggle(s, v)),
                          ],
                        ),
                      ),
                    ),
                  OutlinedButton.icon(
                    onPressed: () => showAppSnack(context, 'TODO: form tạo gói dịch vụ mới'),
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size.fromHeight(56),
                      backgroundColor: AppColors.pink50,
                      foregroundColor: AppColors.pink600,
                      side: const BorderSide(color: AppColors.pink200, width: 2),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    icon: const Icon(Icons.add),
                    label: const Text('Thêm dịch vụ mới', style: TextStyle(fontWeight: FontWeight.bold)),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
