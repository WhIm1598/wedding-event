import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/service_item.dart';
import '../../data/repositories/service_repository.dart';
import '../../state/session_controller.dart';
import 'widgets/quote_request_sheet.dart';

class ServiceDetailScreen extends StatefulWidget {
  const ServiceDetailScreen({super.key, required this.serviceId});

  final String serviceId;

  @override
  State<ServiceDetailScreen> createState() => _ServiceDetailScreenState();
}

class _ServiceDetailScreenState extends State<ServiceDetailScreen> {
  late Future<ServiceItem> _future;

  @override
  void initState() {
    super.initState();
    _future = context.read<ServiceRepository>().getById(widget.serviceId);
  }

  /// FN-SVC-02 (v1.1.3): couples talk to the provider on Zalo; in-app chat (FN-MSG-01) is hidden on the app
  Future<void> _contact(ServiceItem s) async {
    // launchUrl without canLaunchUrl: needs no <queries> entry in the (generated) AndroidManifest
    final opened = await launchUrl(Uri.parse(s.zaloUrl!), mode: LaunchMode.externalApplication);
    if (!opened && mounted) showAppSnack(context, 'Không mở được Zalo, vui lòng thử lại', error: true);
  }

  Future<void> _requestQuote(ServiceItem s) async {
    final sent = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => QuoteRequestSheet(service: s),
    );
    if (sent == true && mounted) showAppSnack(context, 'Đã gửi yêu cầu báo giá tới ${s.provider}');
  }

  @override
  Widget build(BuildContext context) {
    // FN-VND-01: only couples, and only vendor packages (the studio's have no providerId), take quote requests
    final canQuote = !context.watch<SessionController>().isVendor;
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
                        Text(Fmt.vnd(s.discountPrice ?? s.price), style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.pink600)),
                        if (s.discountPrice != null)
                          Text(Fmt.vnd(s.price), style: const TextStyle(color: AppColors.slate400, decoration: TextDecoration.lineThrough)),
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
            BottomActionBar(
              child: Row(
                children: [
                  if (canQuote && s.providerId != null) ...[
                    Expanded(
                      child: OutlinedButton(
                        style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(56)),
                        onPressed: () => _requestQuote(s),
                        child: const Text('Yêu cầu báo giá'),
                      ),
                    ),
                    const SizedBox(width: 12),
                  ],
                  if (s.zaloUrl != null) Expanded(child: PrimaryButton(label: 'Liên hệ đặt lịch', onPressed: () => _contact(s))),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
