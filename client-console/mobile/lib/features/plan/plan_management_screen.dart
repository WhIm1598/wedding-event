import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/wedding_plan.dart';
import '../../data/repositories/plan_repository.dart';

/// FN-PLAN-02: plan details with Overview / Tasks / Budget tabs
class PlanManagementScreen extends StatefulWidget {
  const PlanManagementScreen({super.key});

  @override
  State<PlanManagementScreen> createState() => _PlanManagementScreenState();
}

class _PlanManagementScreenState extends State<PlanManagementScreen> {
  late Future<WeddingPlan?> _future;
  WeddingPlan? _plan;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    _future = context.read<PlanRepository>().getMyPlan().then((p) {
      if (mounted) setState(() => _plan = p); // also refreshes the task count in the tab label
      return p;
    });
  }

  Future<void> _toggleTask(PlanTask task, bool completed) async {
    final plan = _plan;
    if (plan == null) return;
    setState(() {
      _plan = plan.copyWith(tasks: [for (final t in plan.tasks) t.id == task.id ? t.copyWith(isCompleted: completed) : t]);
    });
    await context.read<PlanRepository>().setTaskCompleted(task.id, completed);
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Column(
        children: [
          ScreenHeader(
            title: 'Chi tiết kế hoạch',
            onBack: () => context.go(Routes.home),
            bottom: TabBar(
              labelColor: AppColors.pink600,
              unselectedLabelColor: AppColors.slate400,
              indicatorColor: AppColors.pink600,
              labelStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
              tabs: [
                const Tab(text: 'Tổng quan'),
                Tab(text: 'Công việc (${_plan?.tasks.length ?? 0})'),
                const Tab(text: 'Ngân sách'),
              ],
            ),
          ),
          Expanded(
            child: AsyncView<WeddingPlan?>(
              future: _future,
              onRetry: () => setState(_load),
              builder: (context, _) {
                final plan = _plan;
                if (plan == null) return const _NoPlan();
                return TabBarView(
                  children: [
                    _OverviewTab(plan: plan),
                    _TasksTab(plan: plan, onToggle: _toggleTask),
                    _BudgetTab(plan: plan),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class _NoPlan extends StatelessWidget {
  const _NoPlan();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.event_note, size: 56, color: AppColors.slate300),
            const SizedBox(height: 12),
            const Text('Bạn chưa có kế hoạch cưới', style: TextStyle(color: AppColors.slate500)),
            const SizedBox(height: 16),
            PrimaryButton(label: 'Tạo kế hoạch', onPressed: () => context.push(Routes.createPlan)),
          ],
        ),
      ),
    );
  }
}

class _OverviewTab extends StatelessWidget {
  const _OverviewTab({required this.plan});

  final WeddingPlan plan;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(
                children: [
                  Icon(Icons.calendar_today, size: 18, color: AppColors.pink500),
                  SizedBox(width: 8),
                  Text('Ngày trọng đại', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.slate700)),
                ],
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(color: AppColors.slate50, borderRadius: BorderRadius.circular(12)),
                child: Row(
                  children: [
                    _DateColumn(label: 'Đám hỏi', value: plan.engagementDate == null ? '—' : Fmt.date(plan.engagementDate!), color: AppColors.slate800),
                    Container(width: 1, height: 32, color: AppColors.slate200),
                    _DateColumn(label: 'Lễ Cưới', value: Fmt.date(plan.weddingDate), color: AppColors.pink600),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Text('Còn ${plan.daysRemaining} ngày • Hoàn thành ${plan.progressPercentage.round()}% công việc',
                  style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
            ],
          ),
        ),
        const SizedBox(height: 16),
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(
                children: [
                  Icon(Icons.list_alt, size: 18, color: AppColors.blue500),
                  SizedBox(width: 8),
                  Text('Tóm tắt ngân sách', style: TextStyle(fontWeight: FontWeight.bold, color: AppColors.slate700)),
                ],
              ),
              const SizedBox(height: 8),
              Text(Fmt.vnd(plan.totalBudget), style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: AppColors.slate800)),
              const SizedBox(height: 16),
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: LinearProgressIndicator(
                  value: (plan.spentPercentage / 100).clamp(0, 1).toDouble(),
                  minHeight: 8,
                  backgroundColor: AppColors.slate100,
                  color: AppColors.pink500,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Đã chi tiêu: ${plan.spentPercentage.round()}% (${Fmt.millions(plan.totalSpent)})',
                style: const TextStyle(fontSize: 12, color: AppColors.slate500),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _DateColumn extends StatelessWidget {
  const _DateColumn({required this.label, required this.value, required this.color});

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Column(
        children: [
          Text(label, style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
          const SizedBox(height: 4),
          Text(value, style: TextStyle(fontWeight: FontWeight.bold, color: color)),
        ],
      ),
    );
  }
}

class _TasksTab extends StatelessWidget {
  const _TasksTab({required this.plan, required this.onToggle});

  final WeddingPlan plan;
  final void Function(PlanTask task, bool completed) onToggle;

  @override
  Widget build(BuildContext context) {
    // Group tasks by milestone ("Trước 6 tháng", ...), keeping insertion order
    final groups = <String, List<PlanTask>>{};
    for (final t in plan.tasks) {
      groups.putIfAbsent(t.monthGroup, () => []).add(t);
    }

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        for (final entry in groups.entries)
          Padding(
            padding: const EdgeInsets.only(bottom: 16),
            child: AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(entry.key, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.slate700)),
                  const SizedBox(height: 8),
                  for (final task in entry.value)
                    CheckboxListTile(
                      contentPadding: EdgeInsets.zero,
                      controlAffinity: ListTileControlAffinity.leading,
                      activeColor: AppColors.pink600,
                      value: task.isCompleted,
                      onChanged: (v) => onToggle(task, v ?? false),
                      title: Text(
                        task.title,
                        style: TextStyle(
                          fontWeight: FontWeight.w600,
                          color: task.isCompleted ? AppColors.slate400 : AppColors.slate700,
                          decoration: task.isCompleted ? TextDecoration.lineThrough : null,
                        ),
                      ),
                      subtitle: Text('Hạn chót: ${Fmt.date(task.dueDate)}', style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
                    ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}

class _BudgetTab extends StatelessWidget {
  const _BudgetTab({required this.plan});

  final WeddingPlan plan;

  static const _statusLabel = {
    BudgetStatus.planned: 'Chưa chốt',
    BudgetStatus.deposited: 'Đã đặt cọc',
    BudgetStatus.paidFull: 'Đã thanh toán',
  };

  static const _palette = [
    (AppColors.orange500, AppColors.orange50, Icons.restaurant),
    (AppColors.blue500, AppColors.blue50, Icons.photo_camera_outlined),
    (AppColors.amber500, AppColors.amber50, Icons.auto_awesome),
    (AppColors.purple500, AppColors.purple50, Icons.checkroom),
    (AppColors.emerald500, AppColors.emerald50, Icons.mail_outline),
  ];

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        AppCard(
          child: Column(
            children: [
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Tổng ngân sách', style: TextStyle(fontSize: 14, color: AppColors.slate500)),
                        Text(Fmt.vnd(plan.totalBudget), style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                      ],
                    ),
                  ),
                  IconButton(
                    style: IconButton.styleFrom(backgroundColor: AppColors.pink50, foregroundColor: AppColors.pink600),
                    onPressed: () => showAppSnack(context, 'TODO: thêm hạng mục ngân sách'),
                    icon: const Icon(Icons.add),
                  ),
                ],
              ),
              const Divider(height: 32, color: AppColors.slate100),
              for (var i = 0; i < plan.budgetItems.length; i++)
                Padding(
                  padding: const EdgeInsets.only(bottom: 16),
                  child: Row(
                    children: [
                      IconBubble(
                        icon: _palette[i % _palette.length].$3,
                        color: _palette[i % _palette.length].$1,
                        background: _palette[i % _palette.length].$2,
                        size: 40,
                        radius: 12,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(plan.budgetItems[i].categoryName,
                                style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14, color: AppColors.slate700)),
                            Text(_statusLabel[plan.budgetItems[i].status]!, style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
                          ],
                        ),
                      ),
                      Text('${Fmt.millions(plan.budgetItems[i].estimatedAmount)} ₫',
                          style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.slate700)),
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
