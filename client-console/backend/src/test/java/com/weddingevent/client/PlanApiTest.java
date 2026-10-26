package com.weddingevent.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.WeddingPlan;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.repository.NotificationRepository;
import com.weddingevent.common.repository.PlanSyncCodeRepository;
import com.weddingevent.common.repository.WeddingPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

/** FN-PLAN-01..05 */
class PlanApiTest extends ClientIntegrationTest {

    @Autowired
    WeddingPlanRepository plans;

    @Autowired
    PlanSyncCodeRepository syncCodes;

    @Autowired
    NotificationRepository notifications;

    @Autowired
    TransactionTemplate tx;

    // ---------------------------------------------------------------- FN-PLAN-01 / 02

    @Test
    void createPlanStoresBudgetAndTheDefaultChecklist() throws Exception {
        String token = coupleToken();
        LocalDate wedding = today().plusMonths(8);
        mvc.perform(authed(post("/api/v1/client/plans"), token).content(planBody(wedding, 300_000_000L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.planId").isString())
                .andExpect(jsonPath("$.data.weddingDate").value(wedding.toString()))
                .andExpect(jsonPath("$.data.engagementDate").value(wedding.minusDays(5).toString()))
                .andExpect(jsonPath("$.data.totalBudget").value(300_000_000L))
                .andExpect(jsonPath("$.data.budgetItems.length()").value(2))
                .andExpect(jsonPath("$.data.budgetItems[0].categoryName").value("Nhà hàng & Tiệc"))
                .andExpect(jsonPath("$.data.budgetItems[0].estimatedAmount").value(150_000_000))
                .andExpect(jsonPath("$.data.budgetItems[0].actualAmount").value(0))
                .andExpect(jsonPath("$.data.budgetItems[0].status").value("PLANNED"))
                .andExpect(jsonPath("$.data.budgetItems[1].categoryName").value("Chụp ảnh & Quay phim"))
                .andExpect(jsonPath("$.data.tasks.length()").value(6))
                .andExpect(jsonPath("$.data.tasks[0].title").value("Khảo sát địa điểm"))
                .andExpect(jsonPath("$.data.tasks[0].monthGroup").value("Trước 6 tháng"))
                .andExpect(jsonPath("$.data.tasks[0].dueDate").value(wedding.minusMonths(6).toString()))
                .andExpect(jsonPath("$.data.tasks[0].isCompleted").value(false))
                .andExpect(jsonPath("$.data.tasks[1].title").value("Chọn concept"))
                .andExpect(jsonPath("$.data.tasks[2].title").value("Chụp ảnh Pre-wedding"))
                .andExpect(jsonPath("$.data.tasks[2].dueDate").value(wedding.minusMonths(3).toString()))
                .andExpect(jsonPath("$.data.tasks[3].title").value("Đặt váy cưới"))
                .andExpect(jsonPath("$.data.tasks[4].title").value("Chốt danh sách khách mời"))
                .andExpect(jsonPath("$.data.tasks[4].monthGroup").value("Trước 1 tháng"))
                .andExpect(jsonPath("$.data.tasks[5].title").value("In thiệp cưới"))
                .andExpect(jsonPath("$.data.tasksProgress.totalTasks").value(6))
                .andExpect(jsonPath("$.data.tasksProgress.completedTasks").value(0))
                .andExpect(jsonPath("$.data.tasksProgress.progressPercentage").value(0.0))
                .andExpect(jsonPath("$.data.totalSpent").value(0))
                .andExpect(jsonPath("$.data.spentPercentage").value(0.0))
                .andExpect(jsonPath("$.data.daysRemaining").value(ChronoUnit.DAYS.between(today(), wedding)))
                .andExpect(jsonPath("$.data.partner").doesNotExist());

        mvc.perform(authed(get("/api/v1/auth/me"), token)).andExpect(jsonPath("$.data.hasPlan").value(true));
    }

    @Test
    void myPlanIsNullBeforeAPlanExists() throws Exception {
        mvc.perform(authed(get("/api/v1/client/plans/my-plan"), coupleToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void aCoupleHasOnlyOneActivePlan() throws Exception {
        String token = coupleToken();
        createPlan(token);
        mvc.perform(authed(post("/api/v1/client/plans"), token).content(planBody(today().plusMonths(7), 300_000_000L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_ALREADY_EXISTS"));
    }

    @Test
    void weddingDateMustBeAtLeastSevenDaysAway() throws Exception {
        mvc.perform(authed(post("/api/v1/client/plans"), coupleToken()).content(planBody(today().plusDays(6), 300_000_000L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errors[0].field").value("weddingDate"));
        mvc.perform(authed(post("/api/v1/client/plans"), coupleToken()).content(planBody(today().plusDays(7), 300_000_000L)))
                .andExpect(status().isCreated());
    }

    @Test
    void budgetMustBeAtLeastTenMillion() throws Exception {
        mvc.perform(authed(post("/api/v1/client/plans"), coupleToken()).content(planBody(today().plusMonths(6), 9_999_999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("totalBudget"));
    }

    @Test
    void categoriesAreRequiredAndMayExceedTheBudgetByAtMostHalf() throws Exception {
        String wedding = today().plusMonths(6).toString();
        mvc.perform(authed(post("/api/v1/client/plans"), coupleToken())
                        .content(json("{'weddingDate':'" + wedding + "','totalBudget':100000000,'categories':[]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("categories"));
        mvc.perform(authed(post("/api/v1/client/plans"), coupleToken())
                        .content(json("{'weddingDate':'" + wedding + "','totalBudget':100000000,"
                                + "'categories':[{'name':'A','allocatedAmount':100000000},{'name':'B','allocatedAmount':50000001}]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("categories"));
        mvc.perform(authed(post("/api/v1/client/plans"), coupleToken())
                        .content(json("{'weddingDate':'" + wedding + "','totalBudget':100000000,"
                                + "'categories':[{'name':'A','allocatedAmount':100000000},{'name':'B','allocatedAmount':50000000}]}")))
                .andExpect(status().isCreated());
        mvc.perform(authed(post("/api/v1/client/plans"), coupleToken())
                        .content(json("{'weddingDate':'" + wedding + "','totalBudget':100000000,"
                                + "'categories':[{'name':'','allocatedAmount':-1}]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void vendorsCannotUsePlanEndpoints() throws Exception {
        String vendor = token(createUser(Role.ROLE_VENDOR, true));
        mvc.perform(authed(post("/api/v1/client/plans"), vendor).content(planBody(today().plusMonths(6), 300_000_000L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void spentAndProgressFollowTheSpecFormulas() throws Exception {
        String token = coupleToken();
        UUID planId = UUID.fromString(createPlan(token));
        tx.executeWithoutResult(s -> {
            WeddingPlan plan = plans.findById(planId).orElseThrow();
            plan.getBudgetItems().get(0).setActualAmount(new BigDecimal(100_000_000));
            plan.getBudgetItems().get(1).setActualAmount(new BigDecimal(35_000_000));
        });
        String taskId = read(body(mvc.perform(authed(get("/api/v1/client/plans/my-plan"), token))), "$.data.tasks[0].id");
        mvc.perform(authed(patch("/api/v1/client/plans/tasks/" + taskId), token).content(json("{'isCompleted':true}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.isCompleted").value(true));

        mvc.perform(authed(get("/api/v1/client/plans/my-plan"), token))
                .andExpect(jsonPath("$.data.totalSpent").value(135_000_000))
                .andExpect(jsonPath("$.data.spentPercentage").value(45.0))
                .andExpect(jsonPath("$.data.tasksProgress.completedTasks").value(1))
                .andExpect(jsonPath("$.data.tasksProgress.progressPercentage").value(16.7)) // 1/6, HALF_UP to 1 decimal
                .andExpect(jsonPath("$.data.tasks[0].isCompleted").value(true));
        assertThat(plans.findById(planId).orElseThrow().getCompletedPercentage()).isEqualTo(17);

        mvc.perform(authed(patch("/api/v1/client/plans/tasks/" + taskId), token).content(json("{'isCompleted':false}")))
                .andExpect(jsonPath("$.data.isCompleted").value(false));
        assertThat(plans.findById(planId).orElseThrow().getCompletedPercentage()).isZero();
    }

    @Test
    void daysRemainingNeverGoesNegative() throws Exception {
        String token = coupleToken();
        UUID planId = UUID.fromString(createPlan(token));
        tx.executeWithoutResult(s -> plans.findById(planId).orElseThrow().setWeddingDate(today().minusDays(3)));
        mvc.perform(authed(get("/api/v1/client/plans/my-plan"), token)).andExpect(jsonPath("$.data.daysRemaining").value(0));
    }

    // ---------------------------------------------------------------- FN-PLAN-05

    @Test
    void tasksOfOtherPlansAreNotFound() throws Exception {
        String owner = coupleToken();
        createPlan(owner);
        String taskId = read(body(mvc.perform(authed(get("/api/v1/client/plans/my-plan"), owner))), "$.data.tasks[0].id");

        String stranger = coupleToken();
        createPlan(stranger);
        mvc.perform(authed(patch("/api/v1/client/plans/tasks/" + taskId), stranger).content(json("{'isCompleted':true}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(authed(patch("/api/v1/client/plans/tasks/" + UUID.randomUUID()), owner).content(json("{'isCompleted':true}")))
                .andExpect(status().isNotFound());
        mvc.perform(authed(patch("/api/v1/client/plans/tasks/" + taskId), owner).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("isCompleted"));
    }

    // ---------------------------------------------------------------- FN-PLAN-03

    @Test
    void aiGeneratesAndSavesAPlanAndNotifiesTheCouple() throws Exception {
        User couple = createUser(Role.ROLE_COUPLE, true);
        String token = token(couple);
        mvc.perform(authed(post("/api/v1/client/plans/ai-generate"), token)
                        .content(json("{'prompt':'Ngân sách 250 triệu, phong cách thanh lịch, 200 khách',"
                                + "'totalBudget':250000000,'guestCount':200,'style':'ELEGANT'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalBudget").value(250_000_000))
                .andExpect(jsonPath("$.data.weddingDate").value(today().plusMonths(6).toString())) // default when not given
                .andExpect(jsonPath("$.data.budgetItems.length()").value(5))
                .andExpect(jsonPath("$.data.budgetItems[0].categoryName").value("Nhà hàng & Tiệc"))
                .andExpect(jsonPath("$.data.budgetItems[0].estimatedAmount").value(125_000_000))
                .andExpect(jsonPath("$.data.budgetItems[0].notes").isString())
                .andExpect(jsonPath("$.data.tasks.length()").value(5))
                .andExpect(jsonPath("$.data.tasks[4].monthGroup").value("Trước 1 tuần"))
                .andExpect(jsonPath("$.data.tasks[4].dueDate").value(today().plusMonths(6).minusWeeks(1).toString()));

        var inbox = notifications.findAllByRecipientUserIdOrderByCreatedAtDesc(couple.getId(), PageRequest.of(0, 10)).getContent();
        assertThat(inbox).anySatisfy(n -> {
            assertThat(n.getKind()).isEqualTo(NotificationKind.AI);
            assertThat(n.getTitle()).isEqualTo("Kế hoạch đã sẵn sàng");
        });

        mvc.perform(authed(post("/api/v1/client/plans/ai-generate"), token).content(json("{'prompt':'Làm lại'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_ALREADY_EXISTS"));
    }

    @Test
    void aiBudgetSplitAlwaysAddsUpAndRespectsTheGivenDate() throws Exception {
        LocalDate wedding = today().plusMonths(10);
        String body = body(mvc.perform(authed(post("/api/v1/client/plans/ai-generate"), coupleToken())
                        .content(json("{'prompt':'Cưới tối giản','totalBudget':123456789,'weddingDate':'" + wedding + "','style':'MINIMAL'}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weddingDate").value(wedding.toString())));
        long sum = 0;
        for (int i = 0; i < 5; i++) {
            sum += Long.parseLong(read(body, "$.data.budgetItems[" + i + "].estimatedAmount"));
        }
        assertThat(sum).isEqualTo(123_456_789L);
    }

    @Test
    void aiRequestValidation() throws Exception {
        String token = coupleToken();
        mvc.perform(authed(post("/api/v1/client/plans/ai-generate"), token).content(json("{'prompt':' '}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("prompt"));
        mvc.perform(authed(post("/api/v1/client/plans/ai-generate"), token)
                        .content(json("{'prompt':'x','weddingDate':'" + today().plusDays(3) + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("weddingDate"));
        mvc.perform(authed(post("/api/v1/client/plans/ai-generate"), token)
                        .content(json("{'prompt':'x','totalBudget':5000000}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("totalBudget"));
        mvc.perform(authed(post("/api/v1/client/plans/ai-generate"), token)
                        .content(json("{'prompt':'x','style':'GOTHIC'}")))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- FN-PLAN-04

    @Test
    void partnersShareOnePlanAfterSync() throws Exception {
        User owner = createUser(Role.ROLE_COUPLE, true);
        User partner = createUser(Role.ROLE_COUPLE, true);
        String ownerToken = token(owner);
        String partnerToken = token(partner);
        String planId = createPlan(ownerToken);

        String created = body(mvc.perform(authed(post("/api/v1/client/plans/sync-code"), ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.syncCode").value(org.hamcrest.Matchers.matchesPattern("[A-HJ-NP-Z2-9]{6}")))
                .andExpect(jsonPath("$.data.expiresAt").isString()));
        String code = read(created, "$.data.syncCode");
        Instant expires = Instant.parse(read(created, "$.data.expiresAt"));
        assertThat(expires).isBetween(Instant.now().plus(23, ChronoUnit.HOURS), Instant.now().plus(25, ChronoUnit.HOURS));

        mvc.perform(authed(post("/api/v1/client/plans/join-sync"), partnerToken).content(json("{'syncCode':'" + code.toLowerCase() + "'}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planId").value(planId))
                .andExpect(jsonPath("$.data.partner.id").value(owner.getId().toString()))
                .andExpect(jsonPath("$.data.partner.name").value(owner.getFullName()));

        mvc.perform(authed(get("/api/v1/client/plans/my-plan"), ownerToken))
                .andExpect(jsonPath("$.data.partner.id").value(partner.getId().toString()));
        mvc.perform(authed(get("/api/v1/auth/me"), partnerToken)).andExpect(jsonPath("$.data.hasPlan").value(true));

        // The partner manages the same checklist
        String taskId = read(body(mvc.perform(authed(get("/api/v1/client/plans/my-plan"), partnerToken))), "$.data.tasks[0].id");
        mvc.perform(authed(patch("/api/v1/client/plans/tasks/" + taskId), partnerToken).content(json("{'isCompleted':true}")))
                .andExpect(status().isOk());
        mvc.perform(authed(get("/api/v1/client/plans/my-plan"), ownerToken))
                .andExpect(jsonPath("$.data.tasksProgress.completedTasks").value(1));
        // FN-PLAN-05 (v1.1.5): only the other half's devices are told to reload, silently
        assertThat(pushSender.sentTo(owner.getId())).anySatisfy(p -> {
            assertThat(p.title()).isNull();
            assertThat(p.data()).containsEntry("type", "plan_updated").containsEntry("planId", planId);
        });
        assertThat(pushSender.sentTo(partner.getId())).noneMatch(p -> p.data() != null && "plan_updated".equals(p.data().get("type")));

        // Both are notified, in the inbox and by push
        for (User u : new User[] {owner, partner}) {
            mvc.perform(authed(get("/api/v1/client/notifications"), token(u)))
                    .andExpect(jsonPath("$.data.items[*].body", hasItem("Bạn và nửa kia đã kết nối thành công!")))
                    .andExpect(jsonPath("$.data.items[0].kind").value("sync"));
            assertThat(pushSender.sentTo(u.getId())).isNotEmpty();
        }

        // Codes are single use, and a plan has at most one partner
        mvc.perform(authed(post("/api/v1/client/plans/join-sync"), coupleToken()).content(json("{'syncCode':'" + code + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SYNC_CODE"));
        mvc.perform(authed(post("/api/v1/client/plans/sync-code"), ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_ALREADY_SYNCED"));
    }

    @Test
    void joinRules() throws Exception {
        String ownerToken = coupleToken();
        createPlan(ownerToken);
        String code = read(body(mvc.perform(authed(post("/api/v1/client/plans/sync-code"), ownerToken))), "$.data.syncCode");

        mvc.perform(authed(post("/api/v1/client/plans/join-sync"), ownerToken).content(json("{'syncCode':'" + code + "'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_JOIN_OWN_PLAN"));

        String withOwnPlan = coupleToken();
        createPlan(withOwnPlan);
        mvc.perform(authed(post("/api/v1/client/plans/join-sync"), withOwnPlan).content(json("{'syncCode':'" + code + "'}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_ALREADY_EXISTS"));

        mvc.perform(authed(post("/api/v1/client/plans/join-sync"), coupleToken()).content(json("{'syncCode':'ZZZZZZ'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SYNC_CODE"));
        mvc.perform(authed(post("/api/v1/client/plans/join-sync"), coupleToken()).content(json("{'syncCode':'12'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("syncCode"));

        // A new code replaces the previous one; an expired code is rejected
        String second = read(body(mvc.perform(authed(post("/api/v1/client/plans/sync-code"), ownerToken))), "$.data.syncCode");
        mvc.perform(authed(post("/api/v1/client/plans/join-sync"), coupleToken()).content(json("{'syncCode':'" + code + "'}")))
                .andExpect(jsonPath("$.code").value("INVALID_SYNC_CODE"));
        tx.executeWithoutResult(s -> syncCodes.findUnusedForUpdate(second).orElseThrow().setExpiresAt(Instant.now().minusSeconds(1)));
        mvc.perform(authed(post("/api/v1/client/plans/join-sync"), coupleToken()).content(json("{'syncCode':'" + second + "'}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SYNC_CODE"));
    }

    @Test
    void syncCodeNeedsAPlan() throws Exception {
        mvc.perform(authed(post("/api/v1/client/plans/sync-code"), coupleToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
