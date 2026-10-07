package com.weddingevent.client.demo;

import com.weddingevent.client.plan.DefaultChecklist;
import com.weddingevent.common.domain.BudgetItem;
import com.weddingevent.common.domain.Guest;
import com.weddingevent.common.domain.Notification;
import com.weddingevent.common.domain.PlanTask;
import com.weddingevent.common.domain.ServicePackage;
import com.weddingevent.common.domain.User;
import com.weddingevent.common.domain.VendorProfile;
import com.weddingevent.common.domain.WeddingPlan;
import com.weddingevent.common.domain.enums.BudgetStatus;
import com.weddingevent.common.domain.enums.GuestStatus;
import com.weddingevent.common.domain.enums.NotificationKind;
import com.weddingevent.common.domain.enums.PlanStatus;
import com.weddingevent.common.domain.enums.Role;
import com.weddingevent.common.domain.enums.ServiceCategory;
import com.weddingevent.common.repository.GuestRepository;
import com.weddingevent.common.repository.NotificationRepository;
import com.weddingevent.common.repository.ServicePackageRepository;
import com.weddingevent.common.repository.UserRepository;
import com.weddingevent.common.repository.VendorProfileRepository;
import com.weddingevent.common.repository.WeddingPlanRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds mobile demo data (profile dev/test, {@code app.demo-data.enabled}) once, matching the app's mock data.
 * All accounts use the password {@code Password123@}:
 * <ul>
 *   <li>{@code ngoc.hoang@example.com} — couple with a plan, guests and notifications;</li>
 *   <li>{@code moi.dangky@example.com} — unverified, no role yet (OTP + role picker flow; the code is logged in dev);</li>
 *   <li>{@code lumiere.vendor@example.com} — vendor; plus three more vendors for the catalog.</li>
 * </ul>
 * Independent of the admin backend's seeder (both may run on the shared dev database).
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class ClientDemoDataSeeder implements ApplicationRunner {

    static final String DEMO_COUPLE = "ngoc.hoang@example.com";
    private static final String PASSWORD = "Password123@";
    private static final Logger log = LoggerFactory.getLogger(ClientDemoDataSeeder.class);

    private final UserRepository users;
    private final WeddingPlanRepository plans;
    private final GuestRepository guests;
    private final VendorProfileRepository vendors;
    private final ServicePackageRepository packages;
    private final NotificationRepository notifications;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ClientDemoDataSeeder(UserRepository users, WeddingPlanRepository plans, GuestRepository guests, VendorProfileRepository vendors,
            ServicePackageRepository packages, NotificationRepository notifications, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.plans = plans;
        this.guests = guests;
        this.vendors = vendors;
        this.packages = packages;
        this.notifications = notifications;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByEmailIgnoreCase(DEMO_COUPLE)) {
            return;
        }
        log.info("Seeding mobile demo data (app.demo-data.enabled=true)");
        LocalDate today = LocalDate.now(clock);

        User couple = user(DEMO_COUPLE, "Ngọc & Hoàng", Role.ROLE_COUPLE, true);
        user("moi.dangky@example.com", "Khách Mới", null, false);

        VendorProfile lumiere = vendor("lumiere.vendor@example.com", "Lumiere Studio", "Quận 1, TP.HCM", "4.9", 128);
        pkg(lumiere, "Gói Chụp Pre-Wedding Ngoại Cảnh", ServiceCategory.PHOTO, 15_000_000, true,
                "Gói chụp ảnh ngoại cảnh 2 ngày 1 đêm tại Đà Lạt hoặc các điểm lân cận TP.HCM. Bao gồm 3 váy cưới, 2 vest, trang điểm và 1 album photobook.",
                "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?q=80&w=1000&auto=format&fit=crop");
        pkg(lumiere, "Chụp Phóng Sự Cưới", ServiceCategory.PHOTO, 8_000_000, true,
                "Chụp phóng sự ngày cưới, giao toàn bộ file gốc và 100 ảnh chỉnh sửa.",
                "https://images.unsplash.com/photo-1519225421980-715cb0215aed?w=1000");
        pkg(lumiere, "Quay Phim Highlight", ServiceCategory.PHOTO, 10_000_000, false,
                "Phim highlight 3–5 phút và phim truyền thống ngày cưới.",
                "https://images.unsplash.com/photo-1606800052052-a08af7148866?w=1000");
        pkg(vendor("dreamy.decor@example.com", "Dreamy Decor", "Quận Phú Nhuận, TP.HCM", "4.8", 85),
                "Trang trí tiệc cưới", ServiceCategory.DECOR, 25_000_000, true,
                "Trang trí trọn gói với concept hoa tươi 100%: cổng hoa, bàn gallery, lối đi sân khấu và backdrop chụp hình.",
                "https://images.unsplash.com/photo-1519225421980-715cb0215aed?q=80&w=1000&auto=format&fit=crop");
        pkg(vendor("gem.center@example.com", "Gem Center", "Quận 1, TP.HCM", "4.7", 312),
                "Tiệc cưới sảnh Diamond", ServiceCategory.RESTAURANT, 120_000_000, true,
                "Sảnh tiệc sức chứa 300 khách, thực đơn 8 món, âm thanh ánh sáng sân khấu và MC chuyên nghiệp.",
                "https://images.unsplash.com/photo-1519167758481-83f550bb49b3?q=80&w=1000&auto=format&fit=crop");
        pkg(vendor("bella.bridal@example.com", "Bella Bridal", "Quận 3, TP.HCM", "4.6", 64),
                "Thuê váy cưới thiết kế", ServiceCategory.DRESS, 8_000_000, true,
                "Gói thuê 2 váy cưới thiết kế + 1 áo dài, chỉnh form theo số đo, giặt hấp miễn phí.",
                "https://images.unsplash.com/photo-1594552072238-b8a33785b261?q=80&w=1000&auto=format&fit=crop");

        LocalDate wedding = today.plusMonths(6);
        WeddingPlan plan = new WeddingPlan();
        plan.setOwner(couple);
        plan.setStatus(PlanStatus.PLANNING);
        plan.setWeddingDate(wedding);
        plan.setEngagementDate(wedding.minusDays(5));
        plan.setTotalBudget(BigDecimal.valueOf(300_000_000L));
        budget(plan, 0, "Nhà hàng & Tiệc", 150_000_000, 50_000_000, BudgetStatus.DEPOSITED);
        budget(plan, 1, "Chụp ảnh & Quay phim", 30_000_000, 0, BudgetStatus.PLANNED);
        budget(plan, 2, "Trang trí", 40_000_000, 40_000_000, BudgetStatus.PAID_FULL);
        budget(plan, 3, "Trang phục", 25_000_000, 10_000_000, BudgetStatus.DEPOSITED);
        budget(plan, 4, "Thiệp cưới", 10_000_000, 0, BudgetStatus.PLANNED);
        for (int i = 0; i < DefaultChecklist.ITEMS.size(); i++) {
            DefaultChecklist.Item item = DefaultChecklist.ITEMS.get(i);
            PlanTask task = new PlanTask();
            task.setPlan(plan);
            task.setPosition(i);
            task.setTitle(item.title());
            task.setMonthGroup(item.monthGroup());
            task.setDueDate(item.dueDate(wedding));
            task.setCompleted(i < 2);
            plan.getTasks().add(task);
        }
        plan.setCompletedPercentage(33);
        plans.save(plan);

        guest(plan, "Phạm Văn A", "Gia đình nhà Gái", GuestStatus.ATTENDING);
        guest(plan, "Nguyễn Thị B", "Bạn đại học", GuestStatus.PENDING);
        guest(plan, "Lê C", "Đồng nghiệp", GuestStatus.DECLINED);
        guest(plan, "Trần D", "Gia đình nhà Trai", GuestStatus.ATTENDING);
        guest(plan, "Đỗ E", "Bạn cấp 3", GuestStatus.ATTENDING);

        notification(couple, NotificationKind.AI, "Kế hoạch đã sẵn sàng", "Trợ lý AI đã hoàn tất bản nháp kế hoạch cưới của bạn.");
        notification(couple, NotificationKind.MESSAGE, "Lumiere Studio", "Nhà cung cấp đã phản hồi tin nhắn của bạn.");
    }

    private User user(String email, String name, Role role, boolean verified) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setFullName(name);
        u.setRole(role);
        u.setVerified(verified);
        return users.save(u);
    }

    private VendorProfile vendor(String email, String businessName, String address, String rating, int reviews) {
        VendorProfile v = new VendorProfile();
        v.setUser(user(email, businessName, Role.ROLE_VENDOR, true));
        v.setBusinessName(businessName);
        v.setAddress(address);
        v.setRating(new BigDecimal(rating));
        v.setReviewCount(reviews);
        return vendors.save(v);
    }

    private void pkg(VendorProfile vendor, String name, ServiceCategory category, long price, boolean active, String description, String image) {
        ServicePackage p = new ServicePackage();
        p.setVendorId(vendor.getId());
        p.setName(name);
        p.setType(category.name());
        p.setCategory(category);
        p.setPrice(BigDecimal.valueOf(price));
        p.setActive(active);
        p.setDescription(description);
        p.setThumbnailUrl(image);
        packages.save(p);
    }

    private static void budget(WeddingPlan plan, int position, String name, long estimated, long actual, BudgetStatus status) {
        BudgetItem b = new BudgetItem();
        b.setPlan(plan);
        b.setPosition(position);
        b.setCategoryName(name);
        b.setEstimatedAmount(BigDecimal.valueOf(estimated));
        b.setActualAmount(BigDecimal.valueOf(actual));
        b.setStatus(status);
        plan.getBudgetItems().add(b);
    }

    private void guest(WeddingPlan plan, String name, String group, GuestStatus status) {
        Guest g = new Guest();
        g.setPlan(plan);
        g.setName(name);
        g.setGroupName(group);
        g.setStatus(status);
        guests.save(g);
    }

    private void notification(User recipient, NotificationKind kind, String title, String body) {
        Notification n = new Notification();
        n.setRecipientUserId(recipient.getId());
        n.setKind(kind);
        n.setTitle(title);
        n.setDescription(body);
        notifications.save(n);
    }
}
