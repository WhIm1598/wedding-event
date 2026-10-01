import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/service_item.dart';
import '../../data/models/wedding_plan.dart';
import '../../data/repositories/plan_repository.dart';
import '../../data/repositories/service_repository.dart';
import '../../state/session_controller.dart';
import '../services/widgets/service_tile.dart';

class CoupleHome extends StatefulWidget {
  const CoupleHome({super.key});

  @override
  State<CoupleHome> createState() => _CoupleHomeState();
}

class _CoupleHomeState extends State<CoupleHome> {
  late Future<WeddingPlan?> _plan;
  late Future<List<ServiceItem>> _featured;

  @override
  void initState() {
    super.initState();
    _plan = context.read<PlanRepository>().getMyPlan();
    _featured = context.read<ServiceRepository>().featured();
  }

  @override
  Widget build(BuildContext context) {
    final session = context.watch<SessionController>();
    final hasPlan = session.hasPlan;

    return Column(
      children: [
        _Header(name: session.user?.fullName ?? ''),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 24),
            children: [
              hasPlan
                  ? FutureBuilder<WeddingPlan?>(
                      future: _plan,
                      builder: (context, snap) => _PlanSummaryCard(plan: snap.data),
                    )
                  : const _StartPlanCard(),
              const SizedBox(height: 24),
              _QuickActions(hasPlan: hasPlan),
              const SizedBox(height: 24),
              Row(
                children: [
                  const Expanded(
                    child: Text('Dịch vụ đề xuất', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                  ),
                  TextButton(
                    onPressed: () => context.go(Routes.search),
                    child: const Text('Xem tất cả', style: TextStyle(color: AppColors.pink600, fontWeight: FontWeight.w600)),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              AsyncView<List<ServiceItem>>(
                future: _featured,
                builder: (context, services) => Column(
                  children: [
                    for (final s in services)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: ServiceTile(service: s, onTap: () => context.push(Routes.serviceDetailFor(s.id))),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.name});

  final String name;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(24, MediaQuery.paddingOf(context).top + 16, 24, 20),
      decoration: const BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.vertical(bottom: Radius.circular(32)),
        boxShadow: [BoxShadow(color: AppColors.shadow, blurRadius: 6, offset: Offset(0, 2))],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Xin chào,', style: TextStyle(color: AppColors.slate500, fontSize: 14, fontWeight: FontWeight.w500)),
                Text(name, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.slate800)),
              ],
            ),
          ),
          NotificationBellButton(background: AppColors.slate50, color: AppColors.slate700),
        ],
      ),
    );
  }
}

/// Bell with unread dot, opens the notification screen
class NotificationBellButton extends StatelessWidget {
  const NotificationBellButton({super.key, required this.background, required this.color});

  final Color background;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        IconButton(
          style: IconButton.styleFrom(backgroundColor: background),
          icon: Icon(Icons.notifications_none, color: color),
          onPressed: () => context.push(Routes.notifications),
        ),
        const Positioned(
          top: 10,
          right: 10,
          child: SizedBox(width: 8, height: 8, child: DecoratedBox(decoration: BoxDecoration(color: AppColors.pink500, shape: BoxShape.circle))),
        ),
      ],
    );
  }
}

class _StartPlanCard extends StatelessWidget {
  const _StartPlanCard();

  @override
  Widget build(BuildContext context) {
    return Container(
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(gradient: AppColors.brandGradient, borderRadius: BorderRadius.circular(24)),
      child: Stack(
        children: [
          const Positioned(right: -16, bottom: -16, child: Icon(Icons.favorite, size: 100, color: AppColors.white20)),
          Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Bắt đầu hành trình', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.white)),
                const SizedBox(height: 8),
                const Text('Lên kế hoạch hoàn hảo cho ngày trọng đại ngay hôm nay.', style: TextStyle(color: AppColors.pink100, fontSize: 14)),
                const SizedBox(height: 16),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.white,
                    foregroundColor: AppColors.pink600,
                    minimumSize: const Size(0, 44),
                    textStyle: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                  ),
                  onPressed: () => context.push(Routes.createPlan),
                  child: const Text('Bắt đầu ngay'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PlanSummaryCard extends StatelessWidget {
  const _PlanSummaryCard({required this.plan});

  final WeddingPlan? plan;

  @override
  Widget build(BuildContext context) {
    final p = plan;
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(gradient: AppColors.successGradient, borderRadius: BorderRadius.circular(24)),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Ngày cưới (Dự kiến)', style: TextStyle(color: AppColors.emerald50, fontSize: 14)),
                    const SizedBox(height: 4),
                    Text(
                      p == null ? '...' : Fmt.longDate(p.weddingDate),
                      style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.white),
                    ),
                    if (p != null)
                      Text('Còn ${p.daysRemaining} ngày', style: const TextStyle(color: AppColors.emerald50, fontSize: 13)),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: AppColors.white20, borderRadius: BorderRadius.circular(16)),
                child: const Icon(Icons.calendar_today, color: AppColors.white, size: 28),
              ),
            ],
          ),
          const SizedBox(height: 16),
          const Divider(color: AppColors.white20, height: 1),
          const SizedBox(height: 16),
          Row(
            children: [
              const Expanded(child: Text('Đã hoàn thành', style: TextStyle(color: AppColors.white))),
              Text(
                p == null ? '--' : '${p.progressPercentage.round()}%',
                style: const TextStyle(color: AppColors.white, fontWeight: FontWeight.bold),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _QuickAction {
  const _QuickAction(this.label, this.icon, this.color, this.bg, this.onTap);

  final String label;
  final IconData icon;
  final Color color;
  final Color bg;
  final VoidCallback onTap;
}

class _QuickActions extends StatelessWidget {
  const _QuickActions({required this.hasPlan});

  final bool hasPlan;

  @override
  Widget build(BuildContext context) {
    final actions = <_QuickAction>[
      hasPlan
          ? _QuickAction('Kế hoạch', Icons.list_alt, AppColors.blue500, AppColors.blue50, () => context.go(Routes.plan))
          : _QuickAction('Tạo mới', Icons.check_circle_outline, AppColors.emerald500, AppColors.emerald50, () => context.push(Routes.createPlan)),
      _QuickAction('AI Chat', Icons.auto_awesome, AppColors.purple500, AppColors.purple50, () => context.push(Routes.aiChat)),
      _QuickAction('Khách mời', Icons.groups_outlined, AppColors.orange500, AppColors.orange50, () => context.push(Routes.guests)),
      _QuickAction('Đồng bộ', Icons.share_outlined, AppColors.pink500, AppColors.pink50, () => context.push(Routes.sharePlan)),
    ];

    return Row(
      children: [
        for (var i = 0; i < actions.length; i++) ...[
          if (i > 0) const SizedBox(width: 12),
          Expanded(
            child: AppCard(
              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 4),
              onTap: actions[i].onTap,
              child: Column(
                children: [
                  IconBubble(icon: actions[i].icon, color: actions[i].color, background: actions[i].bg),
                  const SizedBox(height: 8),
                  Text(
                    actions[i].label.toUpperCase(),
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 10, fontWeight: FontWeight.w600, color: AppColors.slate700),
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }
}
