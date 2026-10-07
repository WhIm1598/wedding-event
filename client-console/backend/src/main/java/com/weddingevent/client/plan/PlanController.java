package com.weddingevent.client.plan;

import com.weddingevent.client.plan.PlanDtos.PlanDto;
import com.weddingevent.client.plan.PlanDtos.SyncCodeDto;
import com.weddingevent.client.plan.PlanDtos.TaskDto;
import com.weddingevent.client.plan.PlanService.AiGenerateRequest;
import com.weddingevent.client.plan.PlanService.CreatePlanRequest;
import com.weddingevent.client.plan.PlanService.JoinSyncRequest;
import com.weddingevent.client.plan.PlanService.TaskUpdateRequest;
import com.weddingevent.client.security.CurrentUser;
import com.weddingevent.client.security.SecurityConfig;
import com.weddingevent.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** FN-PLAN-01..05 — ROLE_COUPLE (the owner or the synced partner) */
@RestController
@RequestMapping("/api/v1/client/plans")
@PreAuthorize(SecurityConfig.COUPLE)
public class PlanController {

    private final PlanService plans;

    public PlanController(PlanService plans) {
        this.plans = plans;
    }

    @GetMapping("/my-plan")
    public ApiResponse<PlanDto> myPlan(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(plans.myPlan(CurrentUser.id(jwt)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanDto> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreatePlanRequest req) {
        return ApiResponse.ok(plans.create(CurrentUser.id(jwt), req), "Đã tạo kế hoạch cưới");
    }

    @PostMapping("/ai-generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanDto> generate(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AiGenerateRequest req) {
        return ApiResponse.ok(plans.generateWithAi(CurrentUser.id(jwt), req), "Trợ lý AI đã tạo kế hoạch cưới");
    }

    @PatchMapping("/tasks/{taskId}")
    public ApiResponse<TaskDto> setTask(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID taskId, @Valid @RequestBody TaskUpdateRequest req) {
        return ApiResponse.ok(plans.setTaskCompleted(CurrentUser.id(jwt), taskId, req.isCompleted()));
    }

    @PostMapping("/sync-code")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SyncCodeDto> syncCode(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(plans.createSyncCode(CurrentUser.id(jwt)));
    }

    @PostMapping("/join-sync")
    public ApiResponse<PlanDto> joinSync(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JoinSyncRequest req) {
        return ApiResponse.ok(plans.joinSync(CurrentUser.id(jwt), req), "Bạn và nửa kia đã kết nối thành công!");
    }
}
