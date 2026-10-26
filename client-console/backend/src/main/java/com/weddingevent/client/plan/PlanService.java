package com.weddingevent.client.plan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.weddingevent.client.notification.ClientNotificationService;
import com.weddingevent.client.notification.PushSender;
import com.weddingevent.client.plan.PlanDtos.PlanDto;
import com.weddingevent.client.plan.PlanDtos.SyncCodeDto;
import com.weddingevent.client.plan.PlanDtos.TaskDto;
import com.weddingevent.client.plan.ai.AiPlannerClient;
import com.weddingevent.client.plan.ai.AiPlannerClient.AiPlan;
import com.weddingevent.client.plan.ai.AiPlannerClient.AiPlanInput;
import com.weddingevent.common.domain.BudgetItem;
import com.weddingevent.common.domain.PlanSyncCode;
import com.weddingevent.common.domain.PlanTask;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.WeddingPlan;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.PlanStatus;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.PlanSyncCodeRepository;
import com.weddingevent.common.repository.PlanTaskRepository;
import com.weddingevent.common.repository.UserRepository;
import com.weddingevent.common.repository.WeddingPlanRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** FN-PLAN-01..05: the couple's wedding plan, AI generation, partner sync and the checklist. */
@Service
public class PlanService {

    static final int MIN_DAYS_AHEAD = 7;
    static final long MIN_BUDGET = 10_000_000L;
    static final Duration SYNC_CODE_TTL = Duration.ofHours(24);
    /** Upper-case letters and digits without the look-alikes O, 0, I, 1 (FN-PLAN-04) */
    static final String SYNC_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public record CategoryRequest(
            @NotBlank(message = "Tên danh mục không được để trống") @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự") String name,
            @NotNull(message = "Vui lòng nhập số tiền") @PositiveOrZero(message = "Số tiền không được âm") Long allocatedAmount) {}

    public record CreatePlanRequest(
            @NotNull(message = "Vui lòng chọn ngày cưới") LocalDate weddingDate,
            LocalDate engagementDate,
            @NotNull(message = "Vui lòng nhập tổng ngân sách") @Min(value = MIN_BUDGET, message = "Ngân sách tối thiểu 10.000.000 VND") Long totalBudget,
            @NotNull(message = "Cần ít nhất 1 danh mục chi phí") @Size(min = 1, max = 30, message = "Cần từ 1 đến 30 danh mục chi phí")
            List<@Valid CategoryRequest> categories) {}

    public record AiGenerateRequest(
            @NotBlank(message = "Vui lòng mô tả mong muốn của bạn") @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự") String prompt,
            @Min(value = MIN_BUDGET, message = "Ngân sách tối thiểu 10.000.000 VND") Long totalBudget,
            LocalDate weddingDate,
            @Positive(message = "Số khách phải lớn hơn 0") @Max(value = 5000, message = "Số khách tối đa 5000") Integer guestCount,
            WeddingStyle style) {}

    public record TaskUpdateRequest(@JsonProperty("isCompleted") @NotNull(message = "Thiếu trạng thái hoàn thành") Boolean isCompleted) {}

    public record JoinSyncRequest(
            @NotBlank(message = "Vui lòng nhập mã kết nối")
            @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "Mã kết nối gồm 6 ký tự") String syncCode) {}

    private record NewCategory(String name, long amount, String notes) {}

    private record NewTask(String title, String monthGroup, LocalDate dueDate) {}

    private final WeddingPlanRepository plans;
    private final PlanTaskRepository tasks;
    private final PlanSyncCodeRepository syncCodes;
    private final UserRepository users;
    private final AiPlannerClient aiPlanner;
    private final ClientNotificationService notifications;
    private final PushSender push;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public PlanService(WeddingPlanRepository plans, PlanTaskRepository tasks, PlanSyncCodeRepository syncCodes, UserRepository users,
            AiPlannerClient aiPlanner, ClientNotificationService notifications, PushSender push, TransactionTemplate tx, Clock clock) {
        this.plans = plans;
        this.tasks = tasks;
        this.syncCodes = syncCodes;
        this.users = users;
        this.aiPlanner = aiPlanner;
        this.notifications = notifications;
        this.push = push;
        this.tx = tx;
        this.clock = clock;
    }

    /** FN-PLAN-02: null when the couple has no active plan yet (a DRAFT doesn't count) */
    @Transactional(readOnly = true)
    public PlanDto myPlan(UUID userId) {
        return plans.findActiveFor(userId).map(p -> PlanDto.of(p, userId, today())).orElse(null);
    }

    /** FN-PLAN-01 */
    @Transactional
    public PlanDto create(UUID userId, CreatePlanRequest req) {
        requireNoActivePlan(userId);
        checkWeddingDate(req.weddingDate());
        long allocated = req.categories().stream().mapToLong(CategoryRequest::allocatedAmount).sum();
        if (BigDecimal.valueOf(allocated).compareTo(BigDecimal.valueOf(req.totalBudget()).multiply(new BigDecimal("1.5"))) > 0) {
            throw AppException.invalidField("categories", "Tổng phân bổ các danh mục vượt quá 150% ngân sách");
        }
        List<NewCategory> categories = req.categories().stream()
                .map(c -> new NewCategory(c.name().trim(), c.allocatedAmount(), null)).toList();
        List<NewTask> checklist = DefaultChecklist.ITEMS.stream()
                .map(i -> new NewTask(i.title(), i.monthGroup(), i.dueDate(req.weddingDate()))).toList();
        WeddingPlan plan = savePlan(userId, req.weddingDate(), req.engagementDate(), req.totalBudget(), categories, checklist);
        return PlanDto.of(plan, userId, today());
    }

    /**
     * FN-PLAN-03. The AI service is called outside any transaction (it can take seconds); the result is saved in one
     * transaction. Without a wedding date the plan is dated six months from today.
     */
    public PlanDto generateWithAi(UUID userId, AiGenerateRequest req) {
        if (plans.existsActiveFor(userId)) {
            throw planExists();
        }
        LocalDate weddingDate = req.weddingDate() == null ? today().plusMonths(6) : req.weddingDate();
        checkWeddingDate(weddingDate);

        AiPlan ai = aiPlanner.generate(new AiPlanInput(req.prompt().trim(), req.totalBudget(), weddingDate, req.guestCount(),
                req.style() == null ? null : req.style().name()));

        long budget = ai.totalBudget() != null && ai.totalBudget() > 0 ? ai.totalBudget()
                : req.totalBudget() != null ? req.totalBudget()
                : ai.categories().stream().mapToLong(c -> Math.max(0, c.amount())).sum();
        List<NewCategory> categories = ai.categories().stream()
                .map(c -> new NewCategory(truncate(c.name() == null || c.name().isBlank() ? "Hạng mục" : c.name().trim(), 100),
                        Math.max(0, c.amount()), c.notes() == null ? null : truncate(c.notes(), 500)))
                .toList();
        List<NewTask> checklist = ai.tasks().stream()
                .filter(t -> t.title() != null && !t.title().isBlank())
                .map(t -> new NewTask(truncate(t.title().trim(), 200), truncate(t.monthGroup() == null ? "" : t.monthGroup(), 50),
                        t.dueMonthOffset() <= 0 ? weddingDate.minusWeeks(1) : weddingDate.minusMonths(t.dueMonthOffset())))
                .toList();

        return tx.execute(status -> {
            requireNoActivePlan(userId);
            WeddingPlan plan = savePlan(userId, weddingDate, null, budget, categories, checklist);
            notifications.notify(userId, NotificationKind.AI, "Kế hoạch đã sẵn sàng", "Trợ lý AI đã hoàn tất bản nháp kế hoạch cưới của bạn.");
            return PlanDto.of(plan, userId, today());
        });
    }

    /** FN-PLAN-05: the owner or the synced partner ticks a task; anything else is 404 */
    @Transactional
    public TaskDto setTaskCompleted(UUID userId, UUID taskId, boolean completed) {
        PlanTask task = tasks.findById(taskId)
                .filter(t -> t.getPlan().getStatus() == PlanStatus.PLANNING && isMember(t.getPlan(), userId))
                .orElseThrow(() -> new ResourceNotFoundException("công việc", taskId));
        task.setCompleted(completed);
        WeddingPlan plan = task.getPlan();
        long done = plan.getTasks().stream().filter(PlanTask::isCompleted).count();
        plan.setCompletedPercentage(PlanDtos.wholePercent(done, plan.getTasks().size()));
        // FN-PLAN-05 (v1.1.5): the other half's devices reload the plan; data-only, not stored in the inbox
        User other = plan.getOwner().getId().equals(userId) ? plan.getPartner() : plan.getOwner();
        if (other != null) {
            UUID otherId = other.getId();
            String planId = plan.getId().toString();
            ClientNotificationService.afterCommit(() -> push.send(otherId,
                    PushSender.PushMessage.dataOnly(Map.of("type", "plan_updated", "planId", planId))));
        }
        return TaskDto.of(task);
    }

    /** FN-PLAN-04: a new code replaces the plan's unused ones; only a plan without a partner can invite */
    @Transactional
    public SyncCodeDto createSyncCode(UUID userId) {
        WeddingPlan plan = plans.findActiveFor(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Bạn chưa có kế hoạch cưới"));
        if (plan.getPartner() != null) {
            throw alreadySynced();
        }
        syncCodes.deleteUnusedByPlanId(plan.getId());
        String code;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(SYNC_ALPHABET.charAt(random.nextInt(SYNC_ALPHABET.length())));
            }
            code = sb.toString();
        } while (syncCodes.existsByCodeAndUsedFalse(code));

        PlanSyncCode sync = new PlanSyncCode();
        sync.setPlan(plan);
        sync.setCode(code);
        sync.setExpiresAt(clock.instant().plus(SYNC_CODE_TTL));
        sync.setCreatedBy(users.getReferenceById(userId));
        syncCodes.save(sync);
        return new SyncCodeDto(code, sync.getExpiresAt());
    }

    /** FN-PLAN-04: the code row is locked, so two people can't redeem the same code */
    @Transactional
    public PlanDto joinSync(UUID userId, JoinSyncRequest req) {
        Instant now = clock.instant();
        PlanSyncCode code = syncCodes.findUnusedForUpdate(req.syncCode().toUpperCase(Locale.ROOT))
                .filter(c -> c.getExpiresAt().isAfter(now))
                .filter(c -> c.getPlan().getStatus() == PlanStatus.PLANNING)
                .orElseThrow(() -> AppException.badRequest("INVALID_SYNC_CODE", "Mã kết nối không hợp lệ hoặc đã hết hạn"));
        WeddingPlan plan = code.getPlan();
        if (plan.getOwner().getId().equals(userId)) {
            throw AppException.conflict("CANNOT_JOIN_OWN_PLAN", "Bạn không thể dùng mã kết nối của chính mình");
        }
        if (plan.getPartner() != null) {
            throw alreadySynced();
        }
        if (plans.existsActiveFor(userId)) {
            throw planExists();
        }
        User partner = users.findById(userId).orElseThrow();
        plans.findByOwnerIdAndStatus(userId, PlanStatus.DRAFT).ifPresent(plans::delete); // replaced by the shared plan
        plan.setPartner(partner);
        code.setUsed(true);

        for (User member : List.of(plan.getOwner(), partner)) {
            notifications.notify(member.getId(), NotificationKind.SYNC, "Đồng bộ kế hoạch", "Bạn và nửa kia đã kết nối thành công!");
        }
        return PlanDto.of(plan, userId, today());
    }

    // ------------------------------------------------------------------------------------------------------------

    /** Turns the owner's DRAFT (or a new row) into the PLANNING plan with its budget items and checklist */
    private WeddingPlan savePlan(UUID userId, LocalDate weddingDate, LocalDate engagementDate, long totalBudget,
            List<NewCategory> categories, List<NewTask> checklist) {
        User owner = users.findById(userId).orElseThrow();
        WeddingPlan plan = plans.findByOwnerIdAndStatus(userId, PlanStatus.DRAFT).orElseGet(() -> {
            WeddingPlan created = new WeddingPlan();
            created.setOwner(owner);
            return created;
        });
        plan.setStatus(PlanStatus.PLANNING);
        plan.setWeddingDate(weddingDate);
        plan.setEngagementDate(engagementDate);
        plan.setTotalBudget(BigDecimal.valueOf(totalBudget));
        plan.setCompletedPercentage(0);
        plan.getBudgetItems().clear();
        plan.getTasks().clear();
        for (int i = 0; i < categories.size(); i++) {
            BudgetItem item = new BudgetItem();
            item.setPlan(plan);
            item.setPosition(i);
            item.setCategoryName(categories.get(i).name());
            item.setEstimatedAmount(BigDecimal.valueOf(categories.get(i).amount()));
            item.setNotes(categories.get(i).notes());
            plan.getBudgetItems().add(item);
        }
        for (int i = 0; i < checklist.size(); i++) {
            PlanTask task = new PlanTask();
            task.setPlan(plan);
            task.setPosition(i);
            task.setTitle(checklist.get(i).title());
            task.setMonthGroup(checklist.get(i).monthGroup());
            task.setDueDate(checklist.get(i).dueDate());
            plan.getTasks().add(task);
        }
        try {
            return plans.saveAndFlush(plan);
        } catch (DataIntegrityViolationException e) {
            throw planExists(); // a concurrent request created the plan first (unique open plan per owner)
        }
    }

    private void requireNoActivePlan(UUID userId) {
        if (plans.existsActiveFor(userId)) {
            throw planExists();
        }
    }

    private void checkWeddingDate(LocalDate weddingDate) {
        if (weddingDate.isBefore(today().plusDays(MIN_DAYS_AHEAD))) {
            throw AppException.invalidField("weddingDate", "Ngày cưới phải sau ngày hiện tại ít nhất 7 ngày");
        }
    }

    static boolean isMember(WeddingPlan plan, UUID userId) {
        return plan.getOwner().getId().equals(userId) || (plan.getPartner() != null && plan.getPartner().getId().equals(userId));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static AppException planExists() {
        return AppException.conflict("PLAN_ALREADY_EXISTS", "Bạn đã có kế hoạch cưới đang hoạt động");
    }

    private static AppException alreadySynced() {
        return AppException.conflict("PLAN_ALREADY_SYNCED", "Kế hoạch đã được kết nối với bạn đời");
    }
}
