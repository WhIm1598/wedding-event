# WedPlanner — Mobile API (Spring Boot 4)

Backend cho `client-console/mobile`. Java 21 · Spring Boot 4.1 · Spring Security 7 (JWT HS256) · Spring Data JPA / Hibernate 7 ·
Flyway · PostgreSQL 16 · STOMP over WebSocket. Dùng chung database và `backend-common` với Backend Admin.

## Chạy local

```bash
docker compose -f docker-compose.dev.yml up -d          # PostgreSQL 16 trên :5432 (từ thư mục gốc repo)
./gradlew :client-console:backend:bootRun                # http://localhost:8081, profile "dev"

cd client-console/mobile
flutter run --dart-define=USE_MOCK=false                 # emulator Android gọi http://10.0.2.2:8081/api/v1
```

Profile `dev` tự tạo dữ liệu demo một lần (mật khẩu chung `Password123@`):

| Tài khoản | Vai trò | Ghi chú |
|---|---|---|
| `ngoc.hoang@example.com` | `ROLE_COUPLE` | Đã có kế hoạch cưới, khách mời, thông báo |
| `moi.dangky@example.com` | chưa chọn | Chưa xác thực: thử luồng OTP + chọn vai trò. Mã OTP in ra log (`app.otp.log-code=true` chỉ ở dev) |
| `lumiere.vendor@example.com` | `ROLE_VENDOR` | Có 3 gói dịch vụ; thêm `dreamy.decor@`, `gem.center@`, `bella.bridal@example.com` cho danh mục |

## Test

```bash
./gradlew :client-console:backend:build    # unit + integration tests (PostgreSQL qua Testcontainers, cần Docker)
                                           # + cổng coverage JaCoCo ≥ 85% dòng của module này
                                           #   báo cáo: client-console/backend/build/reports/jacoco/test/html/index.html

# Không có Docker: chỉ định một database PostgreSQL trống
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/wedding_client_test \
TEST_DATABASE_USERNAME=wedding_admin TEST_DATABASE_PASSWORD=... ./gradlew :client-console:backend:build
```

## Cấu hình (biến môi trường)

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `jdbc:postgresql://localhost:5432/wedding_db` | Cùng database với Backend Admin |
| `JWT_SECRET` | — (dev có giá trị mặc định) | **Bắt buộc** ngoài dev, tối thiểu 32 bytes. Token của hai backend khác `issuer` nên không dùng lẫn được |
| `GOOGLE_CLIENT_IDS` | trống = tắt đăng nhập Google | OAuth client id (Android/iOS) của app, cách nhau dấu phẩy |
| `AI_PLANNER_BASE_URL` | trống = bộ lập kế hoạch dựng sẵn theo quy tắc | Ví dụ `http://ai-planner-service:8000` (FN-PLAN-03) |
| `CORS_ALLOWED_ORIGINS` | trống | App native không cần CORS |
| `SERVER_PORT` | `8081` | |

## API

Đặc tả: Phần A của `docs/features/detailed_functional_specification.md`. Tất cả trả về envelope `{success, code, message, data, errors}`.

| Endpoint | Quyền | Spec |
|---|---|---|
| `POST /api/v1/auth/login`, `/auth/google`, `/auth/send-otp`, `/auth/verify-otp` | public | FN-AUTH-01/02 |
| `GET /api/v1/auth/me`, `PUT /api/v1/users/role`, `PUT /api/v1/users/me` | đã đăng nhập | FN-AUTH-03/04 |
| `GET /api/v1/client/plans/my-plan`, `POST /client/plans`, `POST /client/plans/ai-generate` | COUPLE | FN-PLAN-01..03 |
| `POST /client/plans/sync-code`, `POST /client/plans/join-sync`, `PATCH /client/plans/tasks/{id}` | COUPLE | FN-PLAN-04/05 |
| `GET /client/guests`, `POST /client/guests`, `PUT /client/guests/{id}` | COUPLE | FN-GUEST-01/02 |
| `GET /client/services`, `GET /client/services/{id}` | COUPLE, VENDOR | FN-SVC-01/02 |
| `GET /client/notifications`, `PATCH /client/notifications/{id}/read` | đã đăng nhập | FN-NOTI-01 |
| `POST /api/v1/messages/send`; STOMP `/ws`: `SEND /app/chat`, `SUBSCRIBE /topic/messages/{roomId}`, `/user/queue/errors` | COUPLE ↔ VENDOR | FN-MSG-01 |

STOMP: gửi header `Authorization: Bearer <accessToken>` trong frame `CONNECT`. Chỉ người trong phòng mới subscribe được
`/topic/messages/{roomId}`. Người nhận không online sẽ nhận thông báo trong app (+ push).

## TODO hạ tầng

- Gửi email OTP thật (`OtpSender`), push FCM (`PushSender` + đăng ký device token).
- Broker STOMP và trạng thái online đang chạy trong bộ nhớ — chạy nhiều instance cần Redis/broker relay.
- Chưa có endpoint làm mới token (refresh token đã được cấp), lịch sử hội thoại, và không gian Vendor (chưa có trong spec).
