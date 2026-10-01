package com.weddingevent.admin.crm;

import com.weddingevent.admin.staff.StaffService;
import com.weddingevent.common.api.ApiResponse;
import com.weddingevent.common.domain.Lead;
import com.weddingevent.common.domain.enums.LeadStage;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.LeadRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRM pipeline Kanban (FN-ADM-CRM-01). Sales staff work leads, so ROLE_STAFF has full access. */
@RestController
@RequestMapping("/api/v1/admin/crm")
public class CrmController {

    public record LeadDto(String id, String name, String phone, boolean hasZalo, String interest, LeadStage stage, LocalDate createdAt) {}

    public record CreateLeadRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = StaffService.VN_PHONE, message = "Số điện thoại không hợp lệ") String phone,
            Boolean hasZalo,
            @Size(max = 200) String interest) {}

    public record UpdateStageRequest(@NotNull LeadStage stage) {}

    /** Stages that only exist once the customer has signed (§1.6) */
    private static final EnumSet<LeadStage> CONTRACT_STAGES = EnumSet.of(LeadStage.IN_PROGRESS, LeadStage.COMPLETED);

    private final LeadRepository leads;
    private final ContractRepository contracts;
    private final Clock clock;

    public CrmController(LeadRepository leads, ContractRepository contracts, Clock clock) {
        this.leads = leads;
        this.contracts = contracts;
        this.clock = clock;
    }

    @GetMapping("/pipeline")
    @Transactional(readOnly = true)
    public ApiResponse<List<LeadDto>> pipeline() {
        return ApiResponse.ok(leads.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList());
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<LeadDto> create(@Valid @RequestBody CreateLeadRequest req) {
        Lead lead = new Lead();
        lead.setName(req.name().trim());
        lead.setPhone(req.phone());
        lead.setHasZalo(Boolean.TRUE.equals(req.hasZalo()));
        lead.setInterest(req.interest() == null || req.interest().isBlank() ? null : req.interest().trim());
        return ApiResponse.ok(toDto(leads.save(lead)), "Đã lưu khách hàng");
    }

    @PatchMapping("/customers/{id}/stage")
    @Transactional
    public ApiResponse<LeadDto> updateStage(@PathVariable UUID id, @Valid @RequestBody UpdateStageRequest req) {
        Lead lead = leads.findById(id).orElseThrow(() -> new ResourceNotFoundException("khách hàng", id));
        if (CONTRACT_STAGES.contains(req.stage()) && req.stage() != lead.getStage() && !contracts.existsByLeadId(id)) {
            throw AppException.conflict("INVALID_STATUS_TRANSITION",
                    "Khách hàng chưa có hợp đồng, hãy tạo hợp đồng trước khi chuyển sang giai đoạn này");
        }
        lead.setStage(req.stage());
        return ApiResponse.ok(toDto(lead));
    }

    private LeadDto toDto(Lead l) {
        LocalDate created = l.getCreatedAt() == null ? LocalDate.now(clock) : l.getCreatedAt().atZone(clock.getZone()).toLocalDate();
        return new LeadDto(l.getId().toString(), l.getName(), l.getPhone(), l.isHasZalo(), l.getInterest(), l.getStage(), created);
    }
}
