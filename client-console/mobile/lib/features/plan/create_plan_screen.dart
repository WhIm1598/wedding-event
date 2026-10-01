import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/validators.dart';
import '../../core/widgets/common.dart';
import '../../data/models/wedding_plan.dart';
import '../../data/repositories/plan_repository.dart';
import '../../state/session_controller.dart';

class _CategoryDraft {
  _CategoryDraft(String name, int amount)
      : name = TextEditingController(text: name),
        amount = TextEditingController(text: amount == 0 ? '' : '$amount');

  final TextEditingController name;
  final TextEditingController amount;

  void dispose() {
    name.dispose();
    amount.dispose();
  }
}

/// FN-PLAN-01: manual plan creation
class CreatePlanScreen extends StatefulWidget {
  const CreatePlanScreen({super.key});

  @override
  State<CreatePlanScreen> createState() => _CreatePlanScreenState();
}

class _CreatePlanScreenState extends State<CreatePlanScreen> {
  final _budget = TextEditingController();
  final List<_CategoryDraft> _categories = [
    _CategoryDraft('Nhà hàng & Tiệc', 150000000),
    _CategoryDraft('Chụp ảnh & Quay phim', 30000000),
  ];
  DateTime _weddingDate = DateTime.now().add(const Duration(days: 180));
  bool _saving = false;

  @override
  void dispose() {
    _budget.dispose();
    for (final c in _categories) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _weddingDate,
      firstDate: now.add(const Duration(days: 7)), // spec: at least 7 days from today
      lastDate: DateTime(now.year + 5),
    );
    if (picked != null) setState(() => _weddingDate = picked);
  }

  void _removeCategory(_CategoryDraft draft) {
    setState(() => _categories.remove(draft));
    // Dispose after the row's TextFields have been unmounted
    WidgetsBinding.instance.addPostFrameCallback((_) => draft.dispose());
  }

  String? _validate(int total, List<({String name, int allocatedAmount})> categories) {
    final budgetError = Validators.totalBudget(total);
    if (budgetError != null) return budgetError;
    if (categories.isEmpty) return 'Cần ít nhất 1 danh mục chi phí';
    final allocated = categories.fold<int>(0, (s, c) => s + c.allocatedAmount);
    if (allocated > total * 1.5) return 'Tổng phân bổ vượt quá 150% ngân sách';
    return null;
  }

  Future<void> _submit() async {
    final total = Fmt.parseMoney(_budget.text);
    final categories = [
      for (final c in _categories)
        if (c.name.text.trim().isNotEmpty) (name: c.name.text.trim(), allocatedAmount: Fmt.parseMoney(c.amount.text)),
    ];
    final error = _validate(total, categories);
    if (error != null) {
      showAppSnack(context, error, error: true);
      return;
    }

    setState(() => _saving = true);
    final session = context.read<SessionController>();
    try {
      await context.read<PlanRepository>().createPlan(
            CreatePlanRequest(weddingDate: _weddingDate, totalBudget: total, categories: categories),
          );
      session.markPlanCreated();
      if (mounted) context.go(Routes.plan);
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          const ScreenHeader(title: 'Tạo Kế Hoạch'),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(24),
              children: [
                AppCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const FieldLabel('Ngày cưới'),
                      OutlinedButton.icon(
                        onPressed: _pickDate,
                        style: OutlinedButton.styleFrom(
                          alignment: Alignment.centerLeft,
                          minimumSize: const Size.fromHeight(50),
                          side: const BorderSide(color: AppColors.slate200),
                          foregroundColor: AppColors.slate800,
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        icon: const Icon(Icons.calendar_today, size: 18, color: AppColors.pink500),
                        label: Text(Fmt.date(_weddingDate)),
                      ),
                      const SizedBox(height: 20),
                      const FieldLabel('Tổng ngân sách'),
                      TextField(
                        controller: _budget,
                        keyboardType: TextInputType.number,
                        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.pink600),
                        decoration: const InputDecoration(hintText: 'VD: 300000000', suffixText: '₫'),
                      ),
                      const SizedBox(height: 20),
                      const FieldLabel('Danh mục chi phí'),
                      for (final c in _categories) _CategoryRow(key: ObjectKey(c), draft: c, onRemove: () => _removeCategory(c)),
                      const SizedBox(height: 8),
                      OutlinedButton.icon(
                        onPressed: () => setState(() => _categories.add(_CategoryDraft('', 0))),
                        style: OutlinedButton.styleFrom(
                          minimumSize: const Size.fromHeight(50),
                          backgroundColor: AppColors.pink50,
                          foregroundColor: AppColors.pink600,
                          side: const BorderSide(color: AppColors.pink200, width: 2),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        ),
                        icon: const Icon(Icons.add, size: 18),
                        label: const Text('Thêm danh mục', style: TextStyle(fontWeight: FontWeight.w600)),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          BottomActionBar(child: PrimaryButton(label: 'Xác nhận tạo Kế Hoạch', loading: _saving, onPressed: _submit)),
        ],
      ),
    );
  }
}

class _CategoryRow extends StatelessWidget {
  const _CategoryRow({super.key, required this.draft, required this.onRemove});

  final _CategoryDraft draft;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.fromLTRB(12, 4, 4, 4),
      decoration: BoxDecoration(
        color: AppColors.slate50,
        border: Border.all(color: AppColors.slate200),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              children: [
                TextField(
                  controller: draft.name,
                  style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.slate800, fontSize: 14),
                  decoration: const InputDecoration(
                    hintText: 'Tên danh mục...',
                    isDense: true,
                    filled: false,
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                  ),
                ),
                TextField(
                  controller: draft.amount,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.pink600, fontSize: 13),
                  decoration: const InputDecoration(
                    prefixText: 'Giá: ',
                    suffixText: 'đ',
                    isDense: true,
                    filled: false,
                    border: InputBorder.none,
                    enabledBorder: InputBorder.none,
                    focusedBorder: InputBorder.none,
                  ),
                ),
              ],
            ),
          ),
          IconButton(onPressed: onRemove, icon: const Icon(Icons.delete_outline, color: AppColors.slate400)),
        ],
      ),
    );
  }
}
