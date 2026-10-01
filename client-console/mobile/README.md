# WedPlanner Mobile (Flutter)

Ứng dụng di động cho **Cặp đôi** và **Freelancer / Vendor**, dựng từ template `docs/templates/wedding_planner_app.tsx`.

## Chạy demo

```bash
cd client-console/mobile
flutter create . --platforms=android,ios --org vn.wedplanner   # lần đầu: sinh thư mục android/ & ios/
flutter pub get
flutter run                                                      # mặc định USE_MOCK=true, không cần backend
```

Kết nối backend thật (`client-console/backend`, port 8081):

```bash
flutter run --dart-define=USE_MOCK=false --dart-define=API_BASE_URL=http://10.0.2.2:8081/api/v1
```

Kiểm tra: `flutter analyze` và `flutter test`.

**Luồng demo:** Splash → Đăng nhập (đã điền sẵn) → OTP (nhập 4 số bất kỳ, `0000` để thử lỗi) → Chọn vai trò → Trang chủ.

## Cấu trúc

```
lib/
├── main.dart / app.dart        # Khởi tạo Provider (DI) + MaterialApp.router
├── core/
│   ├── config/env.dart         # --dart-define: USE_MOCK, API_BASE_URL
│   ├── network/                # ApiClient (Dio) bóc envelope {success, code, message, data}
│   ├── router/app_router.dart  # go_router + redirect theo trạng thái đăng nhập / vai trò
│   ├── theme/                  # Bảng màu & ThemeData theo design system (spec §8)
│   ├── utils/                  # Định dạng VND/ngày, validators theo spec
│   └── widgets/common.dart     # AppCard, ScreenHeader, PrimaryButton, AsyncView, ...
├── data/
│   ├── models/                 # AppUser, WeddingPlan, Guest, ServiceItem, Vendor...
│   ├── mock/mock_data.dart     # Dữ liệu demo (lấy từ template)
│   └── repositories/           # 1 repository / module API, mỗi hàm có nhánh mock + REST thật
├── state/session_controller.dart  # ChangeNotifier: user, role, hasPlan
└── features/                   # Màn hình theo tính năng
    ├── auth/        splash, login, otp, role_select       (FN-AUTH-01..03)
    ├── shell/       bottom navigation theo vai trò
    ├── home/        couple_home, vendor_home
    ├── plan/        create_plan, plan_management (tabs)    (FN-PLAN-01, 02)
    ├── ai_chat/     trợ lý AI sinh kế hoạch                 (FN-PLAN-03)
    ├── sync/        mã kết nối bạn đời                      (FN-PLAN-04)
    ├── guests/      danh sách + thêm khách mời              (FN-GUEST-01, 02)
    ├── services/    khám phá + chi tiết dịch vụ             (FN-SVC-01)
    ├── notifications/
    ├── vendor/      dự án, tin nhắn, quản lý dịch vụ
    └── profile/     hồ sơ, cài đặt tài khoản
```

## Việc cần làm khi triển khai thật (TODO)

- Lưu token vào secure storage (`flutter_secure_storage`) + refresh token; khôi phục phiên ở Splash.
- Google Sign-In (`google_sign_in`) → `POST /auth/google`.
- Firebase Cloud Messaging cho push notification; WebSocket STOMP cho chat realtime.
- Endpoint vendor (`/client/vendor/**`) và chat trợ lý AI chưa có trong spec — đang dùng mock.
- Bật lại `prefer_const_constructors` trong `analysis_options.yaml` rồi chạy `dart fix --apply`.
