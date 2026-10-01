import '../../core/utils/formatters.dart';

enum BudgetStatus { planned, deposited, paidFull }

class BudgetItem {
  const BudgetItem({
    required this.id,
    required this.categoryName,
    required this.estimatedAmount,
    this.actualAmount = 0,
    this.status = BudgetStatus.planned,
    this.notes,
  });

  final String id;
  final String categoryName;
  final int estimatedAmount;
  final int actualAmount;
  final BudgetStatus status;
  final String? notes;

  factory BudgetItem.fromJson(Map<String, dynamic> json) => BudgetItem(
        id: json['id'] as String,
        categoryName: json['categoryName'] as String,
        estimatedAmount: (json['estimatedAmount'] as num).toInt(),
        actualAmount: (json['actualAmount'] as num?)?.toInt() ?? 0,
        status: switch (json['status']) {
          'DEPOSITED' => BudgetStatus.deposited,
          'PAID_FULL' => BudgetStatus.paidFull,
          _ => BudgetStatus.planned,
        },
        notes: json['notes'] as String?,
      );
}

class PlanTask {
  const PlanTask({
    required this.id,
    required this.title,
    required this.dueDate,
    required this.monthGroup,
    this.isCompleted = false,
  });

  final String id;
  final String title;
  final DateTime dueDate;
  final String monthGroup;
  final bool isCompleted;

  PlanTask copyWith({bool? isCompleted}) =>
      PlanTask(id: id, title: title, dueDate: dueDate, monthGroup: monthGroup, isCompleted: isCompleted ?? this.isCompleted);

  factory PlanTask.fromJson(Map<String, dynamic> json) => PlanTask(
        id: json['id'] as String,
        title: json['title'] as String,
        dueDate: DateTime.parse(json['dueDate'] as String),
        monthGroup: json['monthGroup'] as String? ?? '',
        isCompleted: json['isCompleted'] as bool? ?? false,
      );
}

class Partner {
  const Partner({required this.id, required this.name, this.avatarUrl});

  final String id;
  final String name;
  final String? avatarUrl;
}

/// FN-PLAN-02 payload, extended with budget items and tasks for the plan tabs.
class WeddingPlan {
  const WeddingPlan({
    required this.id,
    required this.weddingDate,
    this.engagementDate,
    required this.totalBudget,
    this.budgetItems = const [],
    this.tasks = const [],
    this.partner,
  });

  final String id;
  final DateTime weddingDate;
  final DateTime? engagementDate;
  final int totalBudget;
  final List<BudgetItem> budgetItems;
  final List<PlanTask> tasks;
  final Partner? partner;

  int get daysRemaining => Fmt.daysUntil(weddingDate);
  int get totalSpent => budgetItems.fold(0, (sum, b) => sum + b.actualAmount);
  double get spentPercentage => totalBudget == 0 ? 0 : totalSpent / totalBudget * 100;
  int get completedTasks => tasks.where((t) => t.isCompleted).length;
  double get progressPercentage => tasks.isEmpty ? 0 : completedTasks / tasks.length * 100;

  WeddingPlan copyWith({List<PlanTask>? tasks, Partner? partner}) => WeddingPlan(
        id: id,
        weddingDate: weddingDate,
        engagementDate: engagementDate,
        totalBudget: totalBudget,
        budgetItems: budgetItems,
        tasks: tasks ?? this.tasks,
        partner: partner ?? this.partner,
      );

  factory WeddingPlan.fromJson(Map<String, dynamic> json) {
    final partner = json['partner'] as Map<String, dynamic>?;
    return WeddingPlan(
      id: json['planId'] as String,
      weddingDate: DateTime.parse(json['weddingDate'] as String),
      engagementDate: json['engagementDate'] == null ? null : DateTime.parse(json['engagementDate'] as String),
      totalBudget: (json['totalBudget'] as num).toInt(),
      budgetItems: (json['budgetItems'] as List<dynamic>? ?? []).map((e) => BudgetItem.fromJson(e as Map<String, dynamic>)).toList(),
      tasks: (json['tasks'] as List<dynamic>? ?? []).map((e) => PlanTask.fromJson(e as Map<String, dynamic>)).toList(),
      partner: partner == null
          ? null
          : Partner(id: partner['id'] as String, name: partner['name'] as String, avatarUrl: partner['avatarUrl'] as String?),
    );
  }
}

/// Request body of POST /client/plans (FN-PLAN-01)
class CreatePlanRequest {
  const CreatePlanRequest({required this.weddingDate, this.engagementDate, required this.totalBudget, required this.categories});

  final DateTime weddingDate;
  final DateTime? engagementDate;
  final int totalBudget;
  final List<({String name, int allocatedAmount})> categories;

  Map<String, dynamic> toJson() => {
        'weddingDate': Fmt.isoDate(weddingDate),
        if (engagementDate != null) 'engagementDate': Fmt.isoDate(engagementDate!),
        'totalBudget': totalBudget,
        'categories': [
          for (final c in categories) {'name': c.name, 'allocatedAmount': c.allocatedAmount},
        ],
      };
}

enum WeddingStyle { modern, classic, minimal, elegant }

/// Request body of POST /client/plans/ai-generate (FN-PLAN-03)
class AiPlanRequest {
  const AiPlanRequest({required this.prompt, this.totalBudget, this.weddingDate, this.guestCount, this.style});

  final String prompt;
  final int? totalBudget;
  final DateTime? weddingDate;
  final int? guestCount;
  final WeddingStyle? style;

  Map<String, dynamic> toJson() => {
        'prompt': prompt,
        if (totalBudget != null) 'totalBudget': totalBudget,
        if (weddingDate != null) 'weddingDate': Fmt.isoDate(weddingDate!),
        if (guestCount != null) 'guestCount': guestCount,
        if (style != null) 'style': style!.name.toUpperCase(),
      };
}

class SyncCode {
  const SyncCode({required this.code, required this.expiresAt});

  final String code;
  final DateTime expiresAt;
}
