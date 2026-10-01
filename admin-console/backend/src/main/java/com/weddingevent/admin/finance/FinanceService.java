package com.weddingevent.admin.finance;

import com.weddingevent.admin.notification.NotificationService;
import com.weddingevent.admin.support.DateRange;
import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.PaymentTransaction;
import com.weddingevent.common.domain.enums.ContractStatus;
import com.weddingevent.common.domain.enums.TransactionType;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.PaymentTransactionRepository;
import com.weddingevent.common.support.CodeGenerator;
import com.weddingevent.common.support.CodeGenerator.Code;
import com.weddingevent.common.util.VndFormatter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cash book (FN-ADM-FIN-01, FN-ADM-FIN-03). */
@Service
public class FinanceService {

    public record TransactionDto(
            String id, String code, LocalDate transactionDate, String description, TransactionType type,
            BigDecimal amount, String category, String contractId) {
        static TransactionDto of(PaymentTransaction t) {
            return new TransactionDto(t.getId().toString(), t.getCode(), t.getTransactionDate(), t.getDescription(), t.getType(),
                    t.getAmount(), t.getCategory(), t.getContract() == null ? null : t.getContract().getId().toString());
        }
    }

    public record SummaryDto(String periodLabel, BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal netProfit) {}

    public record CreateTransactionRequest(
            @NotNull TransactionType type,
            @NotNull @Positive BigDecimal amount,
            @NotBlank @Size(max = 100) String category,
            @NotBlank @Size(max = 255) String description,
            UUID contractId,
            @NotNull LocalDate transactionDate) {}

    private final PaymentTransactionRepository transactions;
    private final ContractRepository contracts;
    private final CodeGenerator codes;
    private final NotificationService notifications;
    private final Clock clock;

    public FinanceService(PaymentTransactionRepository transactions, ContractRepository contracts, CodeGenerator codes,
            NotificationService notifications, Clock clock) {
        this.transactions = transactions;
        this.contracts = contracts;
        this.codes = codes;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> list() {
        return transactions.findAllByOrderByTransactionDateDescCreatedAtDesc().stream().map(TransactionDto::of).toList();
    }

    @Transactional(readOnly = true)
    public SummaryDto currentMonthSummary() {
        LocalDate today = LocalDate.now(clock);
        DateRange month = DateRange.month(today);
        BigDecimal income = transactions.sumByTypeBetween(TransactionType.INCOME, month.from(), month.to());
        BigDecimal expense = transactions.sumByTypeBetween(TransactionType.EXPENSE, month.from(), month.to());
        return new SummaryDto("Tháng " + today.getMonthValue(), income, expense, income.subtract(expense));
    }

    @Transactional
    public TransactionDto create(CreateTransactionRequest req) {
        if (req.transactionDate().isAfter(LocalDate.now(clock))) {
            throw AppException.badRequest("BAD_REQUEST", "Ngày giao dịch không được sau ngày hôm nay");
        }
        Contract contract = req.contractId() == null ? null
                : contracts.findById(req.contractId()).orElseThrow(() -> new ResourceNotFoundException("hợp đồng", req.contractId()));
        PaymentTransaction t = record(req.type(), req.amount(), req.category().trim(), req.description().trim(), contract, req.transactionDate());
        // The final payment already raises "đã tất toán" inside record()
        if (contract != null && t.getType() == TransactionType.INCOME && contract.getStatus() != ContractStatus.COMPLETED) {
            notifications.notify("Đã thu tiền 💰", "Thu " + VndFormatter.vnd(t.getAmount()) + " cho hợp đồng " + contract.getContractNumber()
                    + " (" + contract.getCustomerName() + "). Còn lại " + VndFormatter.vnd(contract.getRemainingAmount()) + ".");
        }
        return TransactionDto.of(t);
    }

    /**
     * Records a transaction. An INCOME linked to a contract is added to its paid amount and reduces the
     * remaining debt; a settled contract can't take more money (409 CONTRACT_NOT_PAYABLE) and paying more than
     * the remaining amount is rejected (400 PAYMENT_EXCEEDS_REMAINING).
     */
    @Transactional
    public PaymentTransaction record(TransactionType type, BigDecimal amount, String category, String description,
            Contract contract, LocalDate date) {
        if (contract != null && type == TransactionType.INCOME) {
            if (contract.getStatus() == ContractStatus.COMPLETED) {
                throw AppException.conflict("CONTRACT_NOT_PAYABLE",
                        "Hợp đồng " + contract.getContractNumber() + " đã hoàn tất, không thể ghi thêm khoản thu");
            }
            if (amount.compareTo(contract.getRemainingAmount()) > 0) {
                throw AppException.badRequest("PAYMENT_EXCEEDS_REMAINING",
                        "Số tiền thu vượt quá công nợ còn lại của hợp đồng (" + VndFormatter.vnd(contract.getRemainingAmount()) + ")");
            }
            contract.applyPayment(amount);
            if (contract.getStatus() == ContractStatus.COMPLETED) {
                notifications.notify("Hợp đồng đã tất toán ✅",
                        "Hợp đồng " + contract.getContractNumber() + " của " + contract.getCustomerName() + " đã thanh toán đủ.");
            }
        }
        PaymentTransaction t = new PaymentTransaction();
        t.setCode(codes.next(Code.TRANSACTION));
        t.setType(type);
        t.setAmount(amount);
        t.setCategory(category);
        t.setDescription(description);
        t.setContract(contract);
        t.setTransactionDate(date);
        return transactions.save(t);
    }
}
