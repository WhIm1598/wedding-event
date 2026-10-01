import 'dart:math';

import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../mock/mock_data.dart';
import '../models/wedding_plan.dart';

/// Module 2: Couple wedding plan (FN-PLAN-01..04)
class PlanRepository {
  PlanRepository(this._api);

  final ApiClient _api;

  WeddingPlan _parsePlan(Object? d) => WeddingPlan.fromJson(d! as Map<String, dynamic>);

  /// GET /client/plans/my-plan
  Future<WeddingPlan?> getMyPlan() {
    if (Env.useMock) return mockDelay(MockData.plan);
    return _api.get('/client/plans/my-plan', parse: (d) => d == null ? null : _parsePlan(d));
  }

  /// POST /client/plans — server also generates the default checklist
  Future<WeddingPlan> createPlan(CreatePlanRequest req) {
    if (Env.useMock) {
      final base = MockData.samplePlan(weddingDate: req.weddingDate, totalBudget: req.totalBudget);
      final plan = WeddingPlan(
        id: base.id,
        weddingDate: req.weddingDate,
        engagementDate: req.engagementDate,
        totalBudget: req.totalBudget,
        budgetItems: [
          for (var i = 0; i < req.categories.length; i++)
            BudgetItem(id: 'b-$i', categoryName: req.categories[i].name, estimatedAmount: req.categories[i].allocatedAmount),
        ],
        tasks: base.tasks.map((t) => t.copyWith(isCompleted: false)).toList(),
      );
      MockData.plan = plan;
      return mockDelay(plan, 800);
    }
    return _api.post('/client/plans', body: req.toJson(), parse: _parsePlan);
  }

  /// POST /client/plans/ai-generate — proxied to the in-house AI planner service
  Future<WeddingPlan> generateWithAi(AiPlanRequest req) {
    if (Env.useMock) {
      MockData.plan = MockData.samplePlan(totalBudget: req.totalBudget ?? 250000000);
      return mockDelay(MockData.plan!, 1200);
    }
    return _api.post('/client/plans/ai-generate', body: req.toJson(), parse: _parsePlan);
  }

  /// Conversational step before generation. TODO: backend endpoint is not in the spec yet.
  Future<String> askAssistant(String message) {
    return mockDelay('Mình đã ghi nhận. Hãy nhấn nút bên dưới để tạo kế hoạch tự động nhé!', 1000);
  }

  /// PATCH /client/plans/tasks/{id}
  Future<void> setTaskCompleted(String taskId, bool completed) async {
    if (Env.useMock) {
      final plan = MockData.plan;
      if (plan != null) {
        MockData.plan = plan.copyWith(tasks: [for (final t in plan.tasks) t.id == taskId ? t.copyWith(isCompleted: completed) : t]);
      }
      return;
    }
    await _api.patch('/client/plans/tasks/$taskId', body: {'isCompleted': completed}, parse: (_) {});
  }

  /// POST /client/plans/sync-code — 6 chars, no ambiguous O/0/I/1, valid 24h
  Future<SyncCode> createSyncCode() {
    if (Env.useMock) {
      const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
      final rnd = Random.secure();
      final code = List.generate(6, (_) => alphabet[rnd.nextInt(alphabet.length)]).join();
      return mockDelay(SyncCode(code: code, expiresAt: DateTime.now().add(const Duration(hours: 24))));
    }
    return _api.post('/client/plans/sync-code', parse: (d) {
      final map = d! as Map<String, dynamic>;
      return SyncCode(code: map['syncCode'] as String, expiresAt: DateTime.parse(map['expiresAt'] as String));
    });
  }

  /// POST /client/plans/join-sync
  Future<void> joinSync(String syncCode) async {
    if (Env.useMock) {
      await mockDelay(null, 800);
      if (syncCode.length != 6) throw const ApiException(code: 'INVALID_SYNC_CODE', message: 'Mã kết nối không hợp lệ hoặc đã hết hạn');
      return;
    }
    await _api.post('/client/plans/join-sync', body: {'syncCode': syncCode}, parse: (_) {});
  }
}
