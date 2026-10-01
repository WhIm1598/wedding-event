package com.weddingevent.admin.contract;

import com.weddingevent.admin.config.AppProperties;
import com.weddingevent.admin.finance.FinanceService;
import com.weddingevent.admin.notification.NotificationService;
import com.weddingevent.admin.staff.StaffService;
import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.Lead;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.domain.enums.ContractStatus;
import com.weddingevent.common.domain.enums.LeadStage;
import com.weddingevent.common.domain.enums.TransactionType;
import com.weddingevent.common.exception.AppException;
import com.weddingevent.common.exception.ResourceNotFoundException;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.LeadRepository;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.StudioSettingsRepository;
import com.weddingevent.common.support.CodeGenerator;
import com.weddingevent.common.support.CodeGenerator.Code;
import com.weddingevent.common.util.VndFormatter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractService {

    public record ContractDto(
            String id, String contractNumber, String customerName, String phone, boolean hasZalo, String packageName,
            BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal remainingAmount, LocalDate contractDate,
            ContractStatus status, String notes) {
        static ContractDto of(Contract c) {
            return new ContractDto(c.getId().toString(), c.getContractNumber(), c.getCustomerName(), c.getPhone(), c.isHasZalo(),
                    c.getPackageName(), c.getTotalAmount(), c.getPaidAmount(), c.getRemainingAmount(), c.getContractDate(),
                    c.getStatus(), c.getNotes());
        }
    }

    public record CreateContractRequest(
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Pattern(regexp = StaffService.VN_PHONE, message = "Số điện thoại không hợp lệ") String phone,
            @NotNull UUID servicePackageId,
            @NotNull @Positive BigDecimal totalAmount,
            @NotNull @PositiveOrZero BigDecimal depositAmount,
            Boolean hasZalo,
            @Size(max = 1000) String notes) {}

    public record ShareZaloRequest(@Pattern(regexp = "^$|" + StaffService.VN_PHONE, message = "Số điện thoại không hợp lệ") String phoneNumber) {}

    public record Document(String fileName, byte[] content) {}

    private static final EnumSet<LeadStage> BEFORE_CONTRACT = EnumSet.of(LeadStage.NEW_LEAD, LeadStage.IN_CONSULTATION, LeadStage.AWAITING_DEPOSIT);

    private final ContractRepository contracts;
    private final ServicePackageRepository packages;
    private final LeadRepository leads;
    private final StudioSettingsRepository settings;
    private final FinanceService finance;
    private final NotificationService notifications;
    private final ContractDocumentRenderer renderer;
    private final ZaloClient zalo;
    private final CodeGenerator codes;
    private final AppProperties props;
    private final Clock clock;

    public ContractService(ContractRepository contracts, ServicePackageRepository packages, LeadRepository leads,
            StudioSettingsRepository settings, FinanceService finance, NotificationService notifications,
            ContractDocumentRenderer renderer, ZaloClient zalo, CodeGenerator codes, AppProperties props, Clock clock) {
        this.contracts = contracts;
        this.packages = packages;
        this.leads = leads;
        this.settings = settings;
        this.finance = finance;
        this.notifications = notifications;
        this.renderer = renderer;
        this.zalo = zalo;
        this.codes = codes;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ContractDto> list() {
        return contracts.findAllByOrderByContractDateDescCreatedAtDesc().stream().map(ContractDto::of).toList();
    }

    /**
     * Creates the contract, books the deposit into the cash book (status becomes DEPOSITED) and moves the
     * matching CRM lead (same phone) to IN_PROGRESS.
     */
    @Transactional
    public ContractDto create(CreateContractRequest req) {
        if (req.depositAmount().compareTo(req.totalAmount()) > 0) {
            throw AppException.badRequest("DEPOSIT_EXCEEDS_TOTAL", "Tiền cọc không được lớn hơn tổng giá trị hợp đồng");
        }
        ServicePackage pkg = packages.findById(req.servicePackageId())
                .orElseThrow(() -> new ResourceNotFoundException("gói dịch vụ", req.servicePackageId()));
        Lead lead = leads.findFirstByPhoneOrderByCreatedAtDesc(req.phone()).orElse(null);
        LocalDate today = LocalDate.now(clock);

        Contract c = new Contract();
        c.setContractNumber(codes.next(Code.CONTRACT));
        c.setLead(lead);
        c.setCustomerName(req.customerName().trim());
        c.setPhone(req.phone());
        c.setHasZalo(req.hasZalo() != null ? req.hasZalo() : lead == null || lead.isHasZalo());
        c.setServicePackage(pkg);
        c.setPackageName(pkg.getName());
        c.setTotalAmount(req.totalAmount());
        c.setRemainingAmount(req.totalAmount());
        c.setContractDate(today);
        c.setNotes(req.notes() == null || req.notes().isBlank() ? null : req.notes().trim());
        c = contracts.save(c);

        if (req.depositAmount().signum() > 0) {
            finance.record(TransactionType.INCOME, req.depositAmount(), "Doanh thu HĐ",
                    "Thanh toán cọc hợp đồng " + c.getContractNumber() + " - " + c.getCustomerName(), c, today);
        }
        if (lead != null && BEFORE_CONTRACT.contains(lead.getStage())) {
            lead.setStage(LeadStage.IN_PROGRESS);
        }
        notifications.notify("Hợp đồng mới 📄", "Đã tạo hợp đồng " + c.getContractNumber() + " cho " + c.getCustomerName()
                + " (" + VndFormatter.vnd(c.getTotalAmount()) + ").");
        return ContractDto.of(c);
    }

    @Transactional(readOnly = true)
    public String previewHtml(UUID id) {
        return renderer.html(require(id), studio());
    }

    @Transactional(readOnly = true)
    public Document exportPdf(UUID id) {
        Contract c = require(id);
        return new Document(c.getContractNumber() + ".pdf", renderer.pdf(c, studio()));
    }

    @Transactional(readOnly = true)
    public void shareZalo(UUID id, String phoneNumber) {
        Contract c = require(id);
        String phone = phoneNumber == null || phoneNumber.isBlank() ? c.getPhone() : phoneNumber;
        if (!c.isHasZalo() && (phoneNumber == null || phoneNumber.isBlank())) {
            throw AppException.badRequest("ZALO_NOT_AVAILABLE", "Số điện thoại của khách chưa đăng ký Zalo");
        }
        zalo.sendContractLink(phone, c.getCustomerName(), c.getContractNumber(), props.publicBaseUrl() + "/contracts/" + c.getId());
    }

    private Contract require(UUID id) {
        return contracts.findById(id).orElseThrow(() -> new ResourceNotFoundException("hợp đồng", id));
    }

    private StudioSettings studio() {
        return settings.findById(StudioSettings.SINGLETON_ID).orElseGet(StudioSettings::new);
    }
}
