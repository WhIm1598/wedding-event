package com.weddingevent.admin.demo;

import com.weddingevent.admin.finance.FinanceService;
import com.weddingevent.common.domain.Asset;
import com.weddingevent.common.domain.AssetReservation;
import com.weddingevent.common.domain.Booking;
import com.weddingevent.common.domain.Contract;
import com.weddingevent.common.domain.Lead;
import com.weddingevent.common.domain.Notification;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.StaffMember;
import com.weddingevent.common.domain.StaffTask;
import com.weddingevent.common.domain.StudioSettings;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.enums.AssetCategory;
import com.weddingevent.common.domain.enums.AssetStatus;
import com.weddingevent.common.domain.enums.BookingStatus;
import com.weddingevent.common.domain.enums.ContractStatus;
import com.weddingevent.common.domain.enums.LeadStage;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.domain.enums.StaffWorkStatus;
import com.weddingevent.common.domain.enums.TransactionType;
import com.weddingevent.common.repository.AssetRepository;
import com.weddingevent.common.repository.AssetReservationRepository;
import com.weddingevent.common.repository.BookingRepository;
import com.weddingevent.common.repository.ContractRepository;
import com.weddingevent.common.repository.LeadRepository;
import com.weddingevent.common.repository.NotificationRepository;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.StaffRepository;
import com.weddingevent.common.repository.StaffTaskRepository;
import com.weddingevent.common.repository.StudioSettingsRepository;
import com.weddingevent.common.repository.UserRepository;
import com.weddingevent.common.support.CodeGenerator;
import com.weddingevent.common.support.CodeGenerator.Code;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the data shown in the UI template (docs/templates/lumi_re_studios_admin_dashboard.tsx), with
 * dates relative to today. Runs only when app.demo-data.enabled=true and the database has no users.
 *
 * Demo logins: admin@lumiere.vn / Admin@123 (ROLE_ADMIN), staff@lumiere.vn / Staff@123 (ROLE_STAFF).
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final StaffRepository staff;
    private final StaffTaskRepository tasks;
    private final ServicePackageRepository packages;
    private final BookingRepository bookings;
    private final AssetRepository assets;
    private final AssetReservationRepository reservations;
    private final LeadRepository leads;
    private final ContractRepository contracts;
    private final NotificationRepository notifications;
    private final StudioSettingsRepository settings;
    private final FinanceService finance;
    private final CodeGenerator codes;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataSeeder(UserRepository users, StaffRepository staff, StaffTaskRepository tasks, ServicePackageRepository packages,
            BookingRepository bookings, AssetRepository assets, AssetReservationRepository reservations, LeadRepository leads,
            ContractRepository contracts, NotificationRepository notifications, StudioSettingsRepository settings,
            FinanceService finance, CodeGenerator codes, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.staff = staff;
        this.tasks = tasks;
        this.packages = packages;
        this.bookings = bookings;
        this.assets = assets;
        this.reservations = reservations;
        this.leads = leads;
        this.contracts = contracts;
        this.notifications = notifications;
        this.settings = settings;
        this.finance = finance;
        this.codes = codes;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        log.info("Seeding demo data (app.demo-data.enabled=true)");

        StaffMember tuan = staff("Tuấn Photo", "Thợ Chụp Chính", "0901234567", StaffWorkStatus.WORKING);
        StaffMember huong = staff("Hương Sale", "Tư Vấn Viên", "0987654321", StaffWorkStatus.WORKING);
        StaffMember minh = staff("Minh Video", "Thợ Quay Phim", "0912345678", StaffWorkStatus.ON_LEAVE);
        StaffMember trang = staff("Trang Makeup", "Chuyên Viên Trang Điểm", "0977777777", StaffWorkStatus.WORKING);

        user("admin@lumiere.vn", "Admin@123", "Quản Lý Studio", Role.ROLE_ADMIN, null);
        user("staff@lumiere.vn", "Staff@123", "Hương Sale", Role.ROLE_STAFF, huong);

        ServicePackage diamond = pkg("Gói Kim Cương (Ngày Cưới)", "Trọn gói", 25_000_000,
                List.of("2 Thợ Phóng sự", "1 Quay Phim", "1 Album Photobook 30x30", "Makeup & Làm tóc trọn ngày", "Tặng 2 Ảnh Cổng"));
        ServicePackage standard = pkg("Gói Tiêu Chuẩn (Pre-wedding)", "Pre-wedding", 12_500_000,
                List.of("1 Thợ Chụp", "2 Địa điểm nội thành", "2 Váy + 2 Vest", "Makeup & Làm tóc đi kèm"));
        ServicePackage engagement = pkg("Gói Lễ Ăn Hỏi", "Lễ Ăn Hỏi", 6_000_000,
                List.of("1 Thợ Chụp truyền thống", "Giao toàn bộ file gốc", "Chỉnh sửa 50 file"));

        booking("Lan & Hoàng", "Chụp Pre-wedding", standard, today, "09:00", BookingStatus.IN_PROGRESS, tuan);
        booking("Mai & Huy", "Thử Váy / Vest", diamond, today, "11:30", BookingStatus.UPCOMING, huong);
        booking("Linh & Tuấn", "Lễ Ăn Hỏi", engagement, today.plusDays(1), "14:00", BookingStatus.UPCOMING, minh);
        booking("Trang & Hiếu", "Chụp Pre-wedding", diamond, today.plusDays(2), "08:00", BookingStatus.CONFIRMED, tuan, trang);
        booking("Ngọc & Hoàng", "Chụp Phóng sự cưới", diamond, today.plusDays(9), "07:30", BookingStatus.CONFIRMED, tuan, minh);

        Asset vay001 = asset("VAY-001", "Vera Wang đuôi cá đính đá", AssetCategory.DRESS, "M", 3);
        Asset vst002 = asset("VST-002", "Vest đen Hàn Quốc", AssetCategory.SUIT, "42R", 1);
        Asset vay003 = asset("VAY-003", "Soiree bồng bềnh công chúa", AssetCategory.DRESS, "S", 3);
        asset("MAY-004", "Sony A7IV + Lens 35mm GM", AssetCategory.CAMERA_EQUIPMENT, "N/A", 0);
        reservation(vst002, today.minusDays(1), today.plusDays(1));                 // currently rented
        reservation(vay003, today.minusDays(3), today.minusDays(1));                 // inside cleaning buffer
        reservation(vay001, today.plusDays(11), today.plusDays(12));
        reservation(vay001, today.plusDays(14), today.plusDays(15));                 // legacy overlap -> conflict banner

        lead("Minh & Châu", "0933111222", true, "Gói Lễ Ăn Hỏi", LeadStage.NEW_LEAD);
        lead("Quốc & Anh", "0977888999", true, "Gói Kim Cương", LeadStage.IN_CONSULTATION);
        lead("Thảo & Đạt", "0988111333", false, "Gói Tiêu Chuẩn", LeadStage.AWAITING_DEPOSIT);
        Lead lan = lead("Nguyễn Thị Lan", "0901112222", true, "Gói Kim Cương", LeadStage.IN_PROGRESS);
        Lead huy = lead("Trần Văn Huy", "0988889999", true, "Gói Lễ Ăn Hỏi", LeadStage.COMPLETED);
        Lead hoangC = lead("Lê Hoàng C", "0911223344", false, "Gói Tiêu Chuẩn", LeadStage.IN_PROGRESS);

        Contract hd102 = contract(lan, diamond, today.minusDays(20), ContractStatus.IN_PROGRESS);
        Contract hd103 = contract(huy, engagement, today.minusDays(45), ContractStatus.DRAFT);
        Contract hd104 = contract(hoangC, standard, today.minusDays(3), ContractStatus.DRAFT);
        finance.record(TransactionType.INCOME, money(10_000_000), "Doanh thu HĐ", "Thanh toán cọc hợp đồng " + hd102.getContractNumber(), hd102, today.minusDays(20));
        finance.record(TransactionType.INCOME, money(6_000_000), "Doanh thu HĐ", "Tất toán hợp đồng " + hd103.getContractNumber(), hd103, today.minusDays(40));
        finance.record(TransactionType.INCOME, money(2_500_000), "Doanh thu HĐ", "Thanh toán cọc hợp đồng Lê Hoàng C", hd104, today);
        finance.record(TransactionType.EXPENSE, money(1_200_000), "Chi phí in ấn", "In album photobook khách Mai Huy", null, today.minusDays(1));
        finance.record(TransactionType.EXPENSE, money(800_000), "Chi phí vận hành", "Thuê xe di chuyển chụp Pre-wedding", null, today.minusDays(2));
        seedRevenueHistory(today);

        task(tuan, "Chuẩn bị trang phục cho khách Lan & Hoàng", today, false);
        task(trang, "Makeup cô dâu Mai - Lễ Ăn Hỏi", today.minusDays(1), true);
        task(tuan, "Kiểm tra pin và thẻ nhớ máy Sony A7IV", today, false);

        notification("Khách hàng tới hạn thanh toán 💰", "Khách \"Lê Hoàng C\" tới hạn thanh toán đợt 2 (10.000.000đ). Hãy nhắn Zalo nhắc nhở.", false);
        notification("Trùng lịch trang phục 👗", "Váy \"Vera Wang\" đang được xếp cho 2 khách trong cùng khoảng bảo dưỡng. Vui lòng kiểm tra lại.", false);
        notification("Lịch chụp ngày mai 📸", "Có lịch trình vào ngày mai. Đã nhắc thợ chụp chuẩn bị thiết bị chưa?", true);

        StudioSettings s = settings.findById(StudioSettings.SINGLETON_ID).orElseGet(StudioSettings::new);
        s.setName("LUMIÈRE STUDIOS");
        s.setAddress("123 Nguyễn Văn Linh, Q7, TP.HCM");
        s.setTaxCode("0312345678");
        s.setLegalRepresentative("Nguyễn Văn A");
        s.setBankInfo("Vietcombank - Chi nhánh HCM\nSTK: 0123456789\nChủ TK: NGUYEN VAN A");
        settings.save(s);
    }

    /** One studio-income entry per earlier month of this year so the revenue chart has a shape */
    private void seedRevenueHistory(LocalDate today) {
        int[] millions = {40, 70, 45, 90, 65, 85, 100, 50, 75, 60, 80, 95};
        for (int m = 1; m < today.getMonthValue(); m++) {
            finance.record(TransactionType.INCOME, money(millions[m - 1] * 1_000_000L), "Doanh thu HĐ",
                    "Doanh thu tổng hợp tháng " + m, null, LocalDate.of(today.getYear(), m, 15));
        }
    }

    private StaffMember staff(String name, String position, String phone, StaffWorkStatus status) {
        StaffMember s = new StaffMember();
        s.setCode(codes.next(Code.STAFF));
        s.setFullName(name);
        s.setPosition(position);
        s.setPhone(phone);
        s.setWorkStatus(status);
        return staff.save(s);
    }

    private void user(String email, String password, String name, Role role, StaffMember staffMember) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setFullName(name);
        u.setRole(role);
        u.setVerified(true);
        u.setStaffMember(staffMember);
        users.save(u);
    }

    private ServicePackage pkg(String name, String type, long price, List<String> features) {
        ServicePackage p = new ServicePackage();
        p.setName(name);
        p.setType(type);
        p.setPrice(money(price));
        p.getFeatures().addAll(features);
        return packages.save(p);
    }

    private void booking(String client, String type, ServicePackage pkg, LocalDate date, String time, BookingStatus status, StaffMember... assigned) {
        Booking b = new Booking();
        b.setCustomerName(client);
        b.setType(type);
        b.setServicePackage(pkg);
        b.setEventDate(date);
        b.setEventTime(LocalTime.parse(time));
        b.setVenueAddress("Studio Lumière chi nhánh 1");
        b.setStatus(status);
        b.getStaff().addAll(List.of(assigned));
        bookings.save(b);
    }

    private Asset asset(String code, String name, AssetCategory category, String size, int buffer) {
        Asset a = new Asset();
        a.setCode(code);
        a.setName(name);
        a.setCategory(category);
        a.setSize(size);
        a.setStatus(AssetStatus.AVAILABLE);
        a.setMaintenanceBufferDays(buffer);
        return assets.save(a);
    }

    private void reservation(Asset asset, LocalDate start, LocalDate end) {
        AssetReservation r = new AssetReservation();
        r.setAsset(asset);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setLockEndDate(end.plusDays(asset.getMaintenanceBufferDays()));
        reservations.save(r);
    }

    private Lead lead(String name, String phone, boolean zalo, String interest, LeadStage stage) {
        Lead l = new Lead();
        l.setName(name);
        l.setPhone(phone);
        l.setHasZalo(zalo);
        l.setInterest(interest);
        l.setStage(stage);
        return leads.save(l);
    }

    private Contract contract(Lead lead, ServicePackage pkg, LocalDate date, ContractStatus status) {
        Contract c = new Contract();
        c.setContractNumber(codes.next(Code.CONTRACT));
        c.setLead(lead);
        c.setCustomerName(lead.getName());
        c.setPhone(lead.getPhone());
        c.setHasZalo(lead.isHasZalo());
        c.setServicePackage(pkg);
        c.setPackageName(pkg.getName());
        c.setTotalAmount(pkg.getPrice());
        c.setRemainingAmount(pkg.getPrice());
        c.setContractDate(date);
        c.setStatus(status);
        return contracts.save(c);
    }

    private void task(StaffMember member, String title, LocalDate due, boolean done) {
        StaffTask t = new StaffTask();
        t.setStaffMember(member);
        t.setTitle(title);
        t.setDueDate(due);
        t.setCompleted(done);
        tasks.save(t);
    }

    private void notification(String title, String description, boolean read) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setDescription(description);
        n.setRead(read);
        notifications.save(n);
    }

    private static BigDecimal money(long value) {
        return BigDecimal.valueOf(value);
    }
}
