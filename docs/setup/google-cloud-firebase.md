# Thiết lập Google Cloud & Firebase cho WedPlanner

Hướng dẫn tạo **một** project Google Cloud dùng chung cho hai việc:

- **Đăng nhập Google** (FN-AUTH-05): cần các OAuth client.
- **Thông báo đẩy** (FN-NOTI-02, Firebase Cloud Messaging): cần Firebase, file cấu hình app và một service account cho backend.

Làm theo đúng thứ tự dưới đây. Firebase dùng lại project Google Cloud nên không phải cấu hình hai lần.

> Kết quả cuối cùng là một bảng giá trị (mục 7) để điền vào backend và app. Đừng commit các file bí mật (service account JSON) vào git.

---

## 1. Tạo project Google Cloud

1. Vào <https://console.cloud.google.com/> và đăng nhập tài khoản Google của dự án.
2. Trên thanh trên cùng, bấm chọn project → **New Project**. Tên: `wedplanner`; Organization để mặc định → **Create**.
3. Ghi lại **Project ID** (ví dụ `wedplanner-473812`). Firebase và backend dùng ID này.

## 2. Màn hình đồng ý OAuth (OAuth consent screen)

1. Vào **APIs & Services → OAuth consent screen** (giao diện mới tên là *Google Auth Platform → Branding*).
2. **User type: External** → Create.
3. Điền:
   - App name: `WedPlanner`;
   - User support email;
   - Developer contact email.
4. **Scopes:** giữ mặc định (`openid`, `email`, `profile`), app chỉ cần ba scope này.
5. **Test users:** khi app còn ở chế độ *Testing*, chỉ các tài khoản trong danh sách này đăng nhập Google được. Thêm email của người test.
6. Khi phát hành: bấm **Publish app**. Với ba scope cơ bản này, Google không cần xét duyệt.

## 3. OAuth client cho đăng nhập Google

Vào **APIs & Services → Credentials → Create credentials → OAuth client ID**. Tạo lần lượt:

### 3.1 Web client (bắt buộc)
- Application type: **Web application**, tên `WedPlanner backend`. Không cần nhập URI.
- Lấy **Client ID** dạng `1234…-abc.apps.googleusercontent.com`. Đây là *audience* của ID token:
  - Backend: biến môi trường `GOOGLE_CLIENT_IDS`.
  - App: `--dart-define=GOOGLE_SERVER_CLIENT_ID=…`.

### 3.2 Android client
1. Application type: **Android**, package name `vn.wedplanner.wedplanner`.
2. **SHA-1** của khoá ký. Bản debug lấy bằng lệnh:
   ```
   keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android
   ```
   (sinh ra sau lần build Android đầu tiên). Bản phát hành:
   - dùng SHA-1 của khoá upload;
   - nếu phát hành qua Google Play với *Play App Signing*, thêm cả SHA-1 trong *Play Console → App integrity*.
3. Android client không có giá trị nào cần cấu hình trong code. Google chỉ kiểm tra package name + SHA-1 khớp với đăng ký.

### 3.3 iOS client (cần máy Mac)
1. Application type: **iOS**, Bundle ID `vn.wedplanner.wedplanner`.
2. Lấy **Client ID**:
   - truyền `--dart-define=GOOGLE_IOS_CLIENT_ID=…`;
   - thêm *reversed client ID* làm URL scheme trong `ios/Runner/Info.plist` (xem `client-console/mobile/CLAUDE.md`).

## 4. Thêm Firebase vào project

1. Vào <https://console.firebase.google.com/> → **Add project** → chọn **project Google Cloud vừa tạo** (đừng tạo project mới).
2. Google Analytics: không bắt buộc, có thể tắt.
3. Ở **Project settings → Cloud Messaging**, kiểm tra **Firebase Cloud Messaging API (V1)** đang *Enabled*. Nếu chưa, bấm vào link để bật trong Google Cloud.
   - Không dùng *Legacy API* (server key) vì đã bị Google tắt.

## 5. Đăng ký app với Firebase

### 5.1 Android
1. **Project settings → General → Your apps → Add app → Android**:
   - package `vn.wedplanner.wedplanner`;
   - SHA-1 như mục 3.2: không bắt buộc cho FCM, nhưng nên nhập.
2. Tải **`google-services.json`**, đặt vào `client-console/mobile/android/app/`.
   - Thư mục `android/` được sinh khi chạy `flutter create`, không commit. Mỗi máy dev phải tự đặt file này.
   - File này không phải bí mật, nhưng mỗi môi trường (dev/prod) có một file riêng.
3. Thêm plugin Google Services vào Gradle. Cách nhanh nhất là chạy `flutterfire configure` (xem 5.3), lệnh này tự làm. Nếu làm tay:
   - `android/settings.gradle.kts`, khối `plugins`: `id("com.google.gms.google-services") version "4.4.2" apply false`;
   - `android/app/build.gradle.kts`, khối `plugins`: `id("com.google.gms.google-services")`.

### 5.2 iOS (cần máy Mac)
1. **Add app → iOS**, Bundle ID `vn.wedplanner.wedplanner`.
2. Tải `GoogleService-Info.plist`, kéo vào `ios/Runner` bằng Xcode (tick *Copy items if needed*).
3. **Khoá APNs:** iOS chỉ nhận push qua Apple.
   - Vào <https://developer.apple.com/account> → *Certificates, IDs & Profiles → Keys* → tạo key, tick **Apple Push Notifications service (APNs)**.
   - Tải file `.p8` (chỉ tải được một lần), ghi lại *Key ID* và *Team ID*.
   - Trong Firebase, vào **Project settings → Cloud Messaging → Apple app configuration → APNs Authentication Key**, upload file `.p8` với Key ID và Team ID.
4. Trong Xcode, ở *Signing & Capabilities*, thêm **Push Notifications** và **Background Modes → Remote notifications**.

### 5.3 (Khuyên dùng) FlutterFire CLI
```
dart pub global activate flutterfire_cli
cd client-console/mobile
flutterfire configure --project=<Project ID>
```
Lệnh này:
- tạo `lib/firebase_options.dart`;
- đặt `google-services.json` / `GoogleService-Info.plist` vào đúng chỗ;
- thêm plugin Gradle.

`firebase_options.dart` chứa API key công khai của app (không phải bí mật), nhưng phụ thuộc môi trường. Hiện app gọi `Firebase.initializeApp()` không tham số và đọc cấu hình từ hai file trên, nên không bắt buộc dùng `firebase_options.dart`.

## 6. Service account cho backend gửi push

Backend gửi push qua **FCM HTTP v1**, xác thực bằng một service account:

1. **Firebase → Project settings → Service accounts → Generate new private key**. Firebase tải về một file JSON.
   - Đây là **bí mật**: không commit, không gửi qua chat.
2. Đặt file trên server, ví dụ `/run/secrets/fcm-service-account.json`, và trỏ backend tới đó (mục 7).
3. Service account mặc định `firebase-adminsdk-…@<project>.iam.gserviceaccount.com` đã có quyền gửi FCM.
   - Nếu tự tạo service account khác, cấp role **Firebase Cloud Messaging API Admin**.

## 7. Điền giá trị

| Ở đâu | Biến | Giá trị |
|---|---|---|
| `client-console/backend` | `GOOGLE_CLIENT_IDS` | Web client ID (mục 3.1); nếu có nhiều client thì phân cách bằng dấu phẩy |
| `client-console/backend` | `FCM_PROJECT_ID` | Project ID (mục 1) |
| `client-console/backend` | `FCM_CREDENTIALS_FILE` | Đường dẫn file service account JSON (mục 6). Để trống thì push chỉ được ghi log, không gửi |
| App (`flutter run`) | `--dart-define=GOOGLE_SERVER_CLIENT_ID=…` | Web client ID |
| App (iOS) | `--dart-define=GOOGLE_IOS_CLIENT_ID=…` | iOS client ID |
| App | `--dart-define=FCM_ENABLED=true` | Bật Firebase Messaging. Chỉ bật khi đã đặt `google-services.json` / `GoogleService-Info.plist` |

Ví dụ chạy app trên máy ảo với backend thật:
```
flutter run --dart-define=USE_MOCK=false --dart-define=GOOGLE_SERVER_CLIENT_ID=1234-abc.apps.googleusercontent.com --dart-define=FCM_ENABLED=true
```

## 8. Kiểm tra

1. **Đăng nhập Google:** bấm "Tiếp tục với Google" trên máy ảo có Google Play.
   - Lỗi `DEVELOPER_ERROR` / `10`: SHA-1 hoặc package name của Android client sai.
   - `401 INVALID_GOOGLE_TOKEN`: `GOOGLE_CLIENT_IDS` của backend không chứa Web client ID.
2. **Push:**
   - Đăng nhập app (đã bật `FCM_ENABLED`). Log backend phải có dòng app đăng ký thiết bị (`PUT /users/me/devices`).
   - Tạo một sự kiện có thông báo, ví dụ Vendor báo giá cho một yêu cầu, và kiểm tra máy nhận được push.
   - Thử nhanh không cần backend: **Firebase → Messaging → New campaign → Send test message**, dán FCM token của máy (in ra trong log debug của app).
3. **Đồng bộ checklist:** đăng nhập hai tài khoản đã kết nối bạn đời trên hai máy. Một người tick một việc; máy kia đang mở màn hình Kế hoạch phải tự cập nhật.
