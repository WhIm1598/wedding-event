import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/service_item.dart';
import '../../data/repositories/service_repository.dart';

class ServiceDetailScreen extends StatefulWidget {
  const ServiceDetailScreen({super.key, required this.serviceId});

  final String serviceId;

  @override
  State<ServiceDetailScreen> createState() => _ServiceDetailScreenState();
}

class _ServiceDetailScreenState extends State<ServiceDetailScreen> {
  late Future<ServiceItem> _future;
  bool _contacting = false;

  @override
  void initState() {
    super.initState();
    _future = context.read<ServiceRepository>().getById(widget.serviceId);
  }

  Future<void> _contact(ServiceItem s) async {
    setState(() => _contacting = true);
    try {
      await context.read<ServiceRepository>().contactVendor(s, 'Chào ${s.provider}, gói "${s.name}" còn lịch trống không ạ?');
      if (mounted) showAppSnack(context, 'Đã gửi yêu cầu đặt lịch tới ${s.provider}');
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _contacting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AsyncView<ServiceItem>(
        future: _future,
        builder: (context, s) => Column(
          children: [
            Expanded(
              child: ListView(
                padding: EdgeInsets.zero,
                children: [
                  Stack(
                    children: [
                      SizedBox(
                        height: 260,
                        width: double.infinity,
                        child: Image.network(
                          s.imageUrl,
                          fit: BoxFit.cover,
                          errorBuilder: (_, __, ___) => Container(color: AppColors.slate200, child: const Icon(Icons.image_outlined, size: 48)),
                        ),
                      ),
                      Positioned(
                        top: MediaQuery.paddingOf(context).top + 8,
                        left: 16,
                        child: IconButton(
                          style: IconButton.styleFrom(backgroundColor: AppColors.white80),
                          icon: const Icon(Icons.arrow_back, color: AppColors.slate800),
                          onPressed: () => context.pop(),
                        ),
                      ),
                    ],
                  ),
                  Container(
                    transform: Matrix4.translationValues(0, -24, 0),
                    padding: const EdgeInsets.all(24),
                    decoration: const BoxDecoration(
                      color: AppColors.white,
                      borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
                    ),
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
                                  Text(s.name, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                                  const SizedBox(height: 4),
                                  Text('${s.provider} • ${s.location}', style: const TextStyle(color: AppColors.slate500, fontWeight: FontWeight.w500)),
                                ],
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                              decoration: BoxDecoration(color: AppColors.amber50, borderRadius: BorderRadius.circular(8)),
                              child: Row(
                                children: [
                                  const Icon(Icons.star, size: 14, color: AppColors.amber500),
                                  const SizedBox(width: 4),
                                  Text('${s.rating}', style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.amber600)),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        Text(Fmt.vnd(s.price), style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.pink600)),
                        Text('${s.reviews} đánh giá', style: const TextStyle(fontSize: 12, color: AppColors.slate400)),
                        const Divider(height: 32, color: AppColors.slate100),
                        const Text('Mô tả dịch vụ', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.slate800)),
                        const SizedBox(height: 8),
                        Text(s.description, style: const TextStyle(fontSize: 14, height: 1.6, color: AppColors.slate600)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            BottomActionBar(child: PrimaryButton(label: 'Liên hệ đặt lịch', loading: _contacting, onPressed: () => _contact(s))),
          ],
        ),
      ),
    );
  }
}
