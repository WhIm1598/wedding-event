package com.weddingevent.common.domain;

import com.weddingevent.common.domain.enums.ContractStatus;
import com.weddingevent.common.exception.AppException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Service contract (FN-ADM-CONTR-01). Optimistic locking protects concurrent payments. */
@Getter
@Setter
@Entity
@Table(name = "contracts")
public class Contract extends BaseEntity {

    /** e.g. HD-102 */
    @Column(name = "contract_number", nullable = false, unique = true)
    private String contractNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String phone;

    @Column(name = "has_zalo", nullable = false)
    private boolean hasZalo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_package_id")
    private ServicePackage servicePackage;

    /** Snapshot so later package renames don't change signed contracts */
    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalAmount;

    /** Sum of INCOME transactions linked to this contract (GLOSSARY: Paid Amount; the spec's deposit_amount) */
    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal remainingAmount;

    @Column(name = "contract_date", nullable = false)
    private LocalDate contractDate;

    @Column(name = "pdf_file_url")
    private String pdfFileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.DRAFT;

    private String notes;

    /** When the studio confirmed handing over everything (FN-ADM-CONTR-02 "Xác nhận bàn giao") */
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Version
    private long version;

    /**
     * Applies an incoming payment (FN-ADM-FIN-01). The first payment makes a draft effective (DEPOSITED); paying in
     * full completes the contract only once it has been delivered (§1.6).
     */
    public void applyPayment(BigDecimal amount) {
        paidAmount = paidAmount.add(amount);
        remainingAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);
        if (status == ContractStatus.DRAFT) {
            status = ContractStatus.DEPOSITED;
        }
        completeIfDone();
    }

    /** The first booking of an effective contract started (FN-ADM-CAL-04): DEPOSITED → IN_PROGRESS. */
    public void startWork() {
        if (status == ContractStatus.DEPOSITED) {
            status = ContractStatus.IN_PROGRESS;
        }
    }

    /** Records the hand-over; 409 INVALID_STATUS_TRANSITION unless in progress and not yet delivered. */
    public void deliver(Instant at) {
        if (status != ContractStatus.IN_PROGRESS || deliveredAt != null) {
            throw AppException.conflict("INVALID_STATUS_TRANSITION",
                    "Chỉ xác nhận bàn giao được hợp đồng đang thực hiện và chưa bàn giao");
        }
        deliveredAt = at;
        completeIfDone();
    }

    /** GLOSSARY: Paid in Full — a payment state, not the contract's completion */
    public boolean isPaidInFull() {
        return remainingAmount.signum() == 0;
    }

    private void completeIfDone() {
        if (isPaidInFull() && deliveredAt != null) {
            status = ContractStatus.COMPLETED;
        }
    }
}
