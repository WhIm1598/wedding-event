package com.weddingevent.admin.crm;

import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.Lead;
import com.weddingevent.common.domain.enums.ContractStatus;
import com.weddingevent.common.domain.enums.LeadStage;
import com.weddingevent.common.repository.ContractRepository;
import java.util.EnumSet;
import org.springframework.stereotype.Component;

/**
 * Keeps the customer's stage in step with their contracts (FN-ADM-CRM-03, GLOSSARY "Customer Stage"):
 * "Đang thực hiện" once a contract is effective (deposit received), "Đã hoàn thành" once no effective contract is open.
 */
@Component
public class CustomerStageSync {

    private static final EnumSet<LeadStage> BEFORE_CONTRACT = EnumSet.of(LeadStage.NEW_LEAD, LeadStage.IN_CONSULTATION, LeadStage.AWAITING_DEPOSIT);
    /** Effective and not completed; an abandoned draft must not keep the customer "in progress" */
    private static final EnumSet<ContractStatus> OPEN = EnumSet.of(ContractStatus.DEPOSITED, ContractStatus.IN_PROGRESS);

    private final ContractRepository contracts;

    public CustomerStageSync(ContractRepository contracts) {
        this.contracts = contracts;
    }

    /** Call after the contract's status may have changed, inside the same transaction. */
    public void contractChanged(Contract contract) {
        Lead lead = contract.getLead();
        if (lead == null || contract.getStatus() == ContractStatus.DRAFT) {
            return;
        }
        if (BEFORE_CONTRACT.contains(lead.getStage())) {
            lead.setStage(LeadStage.IN_PROGRESS);
        }
        // Hibernate flushes the just-completed contract before this query (auto flush), so it counts as completed
        if (contract.getStatus() == ContractStatus.COMPLETED && lead.getStage() == LeadStage.IN_PROGRESS
                && !contracts.existsByLeadIdAndStatusIn(lead.getId(), OPEN)) {
            lead.setStage(LeadStage.COMPLETED);
        }
    }
}
