import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/wedding_plan.dart';
import '../../data/repositories/plan_repository.dart';

/// FN-PLAN-04: couple sync via 6-character pairing code
class SharePlanScreen extends StatefulWidget {
  const SharePlanScreen({super.key});

  @override
  State<SharePlanScreen> createState() => _SharePlanScreenState();
}

class _SharePlanScreenState extends State<SharePlanScreen> {
  late Future<SyncCode> _code;
  final _joinCode = TextEditingController();
  bool _joining = false;

  @override
  void initState() {
    super.initState();
    _code = context.read<PlanRepository>().createSyncCode();
  }

  @override
  void dispose() {
    _joinCode.dispose();
    super.dispose();
  }

  // TODO: replace with deep link domain once configured
  String _linkFor(String code) => 'https://wedplanner.app/join/$code';

  Future<void> _copy(String text, String message) async {
    await Clipboard.setData(ClipboardData(text: text));
    if (mounted) showAppSnack(context, message);
  }

  Future<void> _join() async {
    setState(() => _joining = true);
    try {
      await context.read<PlanRepository>().joinSync(_joinCode.text.trim().toUpperCase());
      if (mounted) showAppSnack(context, 'Bạn và nửa kia đã kết nối thành công!');
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _joining = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          const ScreenHeader(title: 'Đồng bộ Kế hoạch'),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(24),
              children: [
                const _PairIllustration(),
                const Text('Mời nửa kia của bạn', textAlign: TextAlign.center, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                const SizedBox(height: 8),
                const Text(
                  'Đồng bộ tài khoản để cả hai có thể cùng xem và chỉnh sửa chung một kế hoạch cưới.',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 14, color: AppColors.slate500),
                ),
                const SizedBox(height: 32),
                AppCard(
                  padding: const EdgeInsets.all(24),
                  child: AsyncView<SyncCode>(
                    future: _code,
                    builder: (context, sync) => Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const Text('Mã kết nối của bạn', textAlign: TextAlign.center, style: TextStyle(fontSize: 14, color: AppColors.slate500)),
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          decoration: BoxDecoration(
                            color: AppColors.slate50,
                            border: Border.all(color: AppColors.slate200),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Text(
                            sync.code,
                            textAlign: TextAlign.center,
                            style: const TextStyle(fontSize: 30, fontWeight: FontWeight.bold, color: AppColors.slate800, letterSpacing: 6),
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text('Hết hạn lúc ${Fmt.date(sync.expiresAt)}', textAlign: TextAlign.center, style: const TextStyle(fontSize: 11, color: AppColors.slate400)),
                        const SizedBox(height: 16),
                        Row(
                          children: [
                            Expanded(
                              child: PrimaryButton(label: 'Chia sẻ mã', icon: Icons.share, onPressed: () => _copy(sync.code, 'Đã sao chép mã ${sync.code}')),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: PrimaryButton(
                                label: 'Sao chép link',
                                icon: Icons.link,
                                color: AppColors.slate700,
                                onPressed: () => _copy(_linkFor(sync.code), 'Đã sao chép link kết nối'),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                AppCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const FieldLabel('Bạn đã có mã từ nửa kia?'),
                      TextField(
                        controller: _joinCode,
                        maxLength: 6,
                        textCapitalization: TextCapitalization.characters,
                        decoration: const InputDecoration(hintText: 'VD: 8A9B2C', counterText: ''),
                      ),
                      const SizedBox(height: 12),
                      PrimaryButton(label: 'Kết nối', loading: _joining, onPressed: _join),
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

class _PairIllustration extends StatelessWidget {
  const _PairIllustration();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 24),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconBubble(icon: Icons.person, color: AppColors.pink500, background: AppColors.pink100, size: 80),
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 12),
            child: Icon(Icons.favorite, color: AppColors.pink500, size: 20),
          ),
          IconBubble(icon: Icons.person_add_alt, color: AppColors.slate400, background: AppColors.slate100, size: 80),
        ],
      ),
    );
  }
}
