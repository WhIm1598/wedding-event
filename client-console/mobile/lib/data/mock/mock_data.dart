// In-memory demo data seeded from docs/templates/wedding_planner_app.tsx.
// Used by repositories while Env.useMock is true. Resets on app restart.
import '../models/app_notification.dart';
import '../models/guest.dart';
import '../models/service_item.dart';
import '../models/vendor.dart';
import '../models/wedding_plan.dart';

class MockData {
  MockData._();

  static const String demoEmail = 'ngoc.hoang@example.com';

  static WeddingPlan? plan;

  static WeddingPlan samplePlan({DateTime? weddingDate, int totalBudget = 300000000}) {
    final wedding = weddingDate ?? DateTime(DateTime.now().year, DateTime.now().month + 6, 20);
    DateTime before(int months) => DateTime(wedding.year, wedding.month - months, wedding.day);
    return WeddingPlan(
      id: 'plan-1',
      weddingDate: wedding,
      engagementDate: wedding.subtract(const Duration(days: 5)),
      totalBudget: totalBudget,
      budgetItems: const [
        BudgetItem(id: 'b-1', categoryName: 'Nhà hàng', estimatedAmount: 150000000, actualAmount: 105000000, status: BudgetStatus.deposited),
        BudgetItem(id: 'b-2', categoryName: 'Chụp ảnh', estimatedAmount: 30000000, actualAmount: 30000000, status: BudgetStatus.paidFull),
        BudgetItem(id: 'b-3', categoryName: 'Trang trí', estimatedAmount: 35000000),
        BudgetItem(id: 'b-4', categoryName: 'Trang phục', estimatedAmount: 25000000),
      ],
      tasks: [
        PlanTask(id: 't-1', title: 'Khảo sát địa điểm', dueDate: before(6), monthGroup: 'Trước 6 tháng', isCompleted: true),
        PlanTask(id: 't-2', title: 'Chọn concept', dueDate: before(6), monthGroup: 'Trước 6 tháng', isCompleted: true),
        PlanTask(id: 't-3', title: 'Chụp ảnh Pre-wedding', dueDate: before(3), monthGroup: 'Trước 3 tháng'),
        PlanTask(id: 't-4', title: 'Đặt váy cưới', dueDate: before(3), monthGroup: 'Trước 3 tháng'),
        PlanTask(id: 't-5', title: 'Chốt danh sách khách mời', dueDate: before(1), monthGroup: 'Trước 1 tháng'),
        PlanTask(id: 't-6', title: 'In thiệp cưới', dueDate: before(1), monthGroup: 'Trước 1 tháng'),
      ],
    );
  }

  static final List<Guest> guests = [
    const Guest(id: 'g-1', name: 'Phạm Văn A', phone: '0912345678', group: 'Gia đình nhà Gái', status: GuestStatus.attending, tableNumber: 5),
    const Guest(id: 'g-2', name: 'Nguyễn Thị B', group: 'Bạn đại học', status: GuestStatus.pending),
    const Guest(id: 'g-3', name: 'Lê C', group: 'Đồng nghiệp', status: GuestStatus.declined),
    const Guest(id: 'g-4', name: 'Trần D', group: 'Gia đình nhà Trai', status: GuestStatus.attending),
    const Guest(id: 'g-5', name: 'Đỗ E', group: 'Bạn cấp 3', status: GuestStatus.attending, notes: 'Ăn chay'),
  ];

  static const List<ServiceItem> services = [
    ServiceItem(
      id: 'svc-1',
      name: 'Chụp ảnh Pre-Wedding',
      provider: 'Lumiere Studio',
      price: 15000000,
      rating: 4.9,
      reviews: 128,
      location: 'Quận 1, TP.HCM',
      category: ServiceCategory.photo,
      imageUrl: 'https://images.unsplash.com/photo-1511285560929-80b456fea0bc?q=80&w=1000&auto=format&fit=crop',
      description:
          'Gói chụp ảnh ngoại cảnh 2 ngày 1 đêm tại Đà Lạt hoặc các điểm lân cận TP.HCM. Bao gồm 3 váy cưới, 2 vest, trang điểm đi kèm và 1 album photobook cao cấp.',
    ),
    ServiceItem(
      id: 'svc-2',
      name: 'Trang trí tiệc cưới',
      provider: 'Dreamy Decor',
      price: 25000000,
      rating: 4.8,
      reviews: 85,
      location: 'Quận Phú Nhuận, TP.HCM',
      category: ServiceCategory.decor,
      imageUrl: 'https://images.unsplash.com/photo-1519225421980-715cb0215aed?q=80&w=1000&auto=format&fit=crop',
      description:
          'Trang trí trọn gói với concept hoa tươi 100%. Bao gồm cổng hoa, bàn gallery, lối đi sân khấu và backdrop chụp hình chuẩn phong cách hiện đại.',
    ),
    ServiceItem(
      id: 'svc-3',
      name: 'Tiệc cưới sảnh Diamond',
      provider: 'Gem Center',
      price: 120000000,
      rating: 4.7,
      reviews: 312,
      location: 'Quận 1, TP.HCM',
      category: ServiceCategory.restaurant,
      imageUrl: 'https://images.unsplash.com/photo-1519167758481-83f550bb49b3?q=80&w=1000&auto=format&fit=crop',
      description: 'Sảnh tiệc sức chứa 300 khách, thực đơn 8 món, âm thanh ánh sáng sân khấu và MC chuyên nghiệp.',
    ),
    ServiceItem(
      id: 'svc-4',
      name: 'Thuê váy cưới thiết kế',
      provider: 'Bella Bridal',
      price: 8000000,
      rating: 4.6,
      reviews: 64,
      location: 'Quận 3, TP.HCM',
      category: ServiceCategory.dress,
      imageUrl: 'https://images.unsplash.com/photo-1594552072238-b8a33785b261?q=80&w=1000&auto=format&fit=crop',
      description: 'Gói thuê 2 váy cưới thiết kế + 1 áo dài, chỉnh sửa form theo số đo, giặt hấp miễn phí.',
    ),
  ];

  static final List<AppNotification> notifications = [
    const AppNotification(id: 'n-1', title: 'Kế hoạch đã sẵn sàng', body: 'Trợ lý AI đã hoàn tất bản nháp kế hoạch cưới của bạn.', kind: NotificationKind.ai),
    const AppNotification(id: 'n-2', title: 'Lumiere Studio', body: 'Nhà cung cấp đã phản hồi tin nhắn của bạn.', kind: NotificationKind.message, isRead: true),
  ];

  static VendorDashboard vendorDashboard() => VendorDashboard(
        monthlyRevenue: 120000000,
        newRequestCount: 3,
        rating: 4.9,
        quoteRequests: const [QuoteRequest(id: 'q-1', customerName: 'Trần Vy', packageName: 'Gói Chụp Pre-Wedding')],
        upcoming: [vendorProjects.first],
      );

  static final List<VendorProject> vendorProjects = [
    VendorProject(id: 'p-1', name: 'Đám cưới Ngọc & Hoàng', service: 'Chụp Phóng sự', date: DateTime(2026, 10, 20), status: ProjectStatus.upcoming, venue: 'Gem Center, Quận 1'),
    VendorProject(id: 'p-2', name: 'Lễ cưới Lan & Tuấn', service: 'Chụp Pre-Wedding', date: DateTime(2026, 9, 15), status: ProjectStatus.inProgress),
    VendorProject(id: 'p-3', name: 'Tiệc cưới Minh & Vy', service: 'Chụp Phóng sự', date: DateTime(2026, 8, 1), status: ProjectStatus.completed),
  ];

  static const List<ChatThread> chatThreads = [
    ChatThread(id: 'c-1', name: 'Trần Vy', lastMessage: 'Anh ơi, em muốn đổi concept chụp sang phong cách Vintage được không ạ?', timeLabel: '10:30', unread: 2),
    ChatThread(id: 'c-2', name: 'Lan & Tuấn', lastMessage: 'Cảm ơn anh vì bộ ảnh cưới quá đẹp!', timeLabel: 'Hôm qua'),
  ];

  static final List<VendorService> vendorServices = [
    const VendorService(id: 'vs-1', name: 'Gói Chụp Pre-Wedding Ngoại Cảnh', price: 15000000, isActive: true, bookings: 24, imageUrl: 'https://images.unsplash.com/photo-1511285560929-80b456fea0bc?w=200'),
    const VendorService(id: 'vs-2', name: 'Chụp Phóng Sự Cưới', price: 8000000, isActive: true, bookings: 42, imageUrl: 'https://images.unsplash.com/photo-1519225421980-715cb0215aed?w=200'),
    const VendorService(id: 'vs-3', name: 'Quay Phim Highlight', price: 10000000, isActive: false, bookings: 5, imageUrl: 'https://images.unsplash.com/photo-1606800052052-a08af7148866?w=200'),
  ];
}
