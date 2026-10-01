# Lumière Studios — Admin API (Spring Boot 4)

Backend for `admin-console/frontend`. Java 21 · Spring Boot 4.1 · Spring Security 7 (JWT HS256) · Spring Data JPA / Hibernate 7 · Flyway · PostgreSQL 16 · OpenHTMLtoPDF.

## Chạy local

```bash
docker compose -f docker-compose.dev.yml up -d          # PostgreSQL 16 trên :5432 (từ thư mục gốc repo)
./gradlew :admin-console:backend:bootRun                 # http://localhost:8082, profile "dev"

cd admin-console/frontend && npm run dev:api             # UI http://localhost:5173 gọi API thật qua Vite proxy
```

Profile `dev` tự tạo dữ liệu demo khi database trống:

| Tài khoản | Mật khẩu | Vai trò |
|---|---|---|
| `admin@lumiere.vn` | `Admin@123` | `ROLE_ADMIN` |
| `staff@lumiere.vn` | `Staff@123` | `ROLE_STAFF` (liên kết nhân sự "Hương Sale") |

## Test

```bash
./gradlew build                     # unit + integration tests, PostgreSQL 16 qua Testcontainers (cần Docker)

# Không có Docker: chỉ định một database PostgreSQL trống
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/wedding_test \
TEST_DATABASE_USERNAME=wedding_admin TEST_DATABASE_PASSWORD=... ./gradlew build
```

## Cấu hình (biến môi trường)

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `jdbc:postgresql://localhost:5432/wedding_db` | |
| `JWT_SECRET` | — (dev có giá trị mặc định) | **Bắt buộc** ngoài dev, tối thiểu 32 bytes |
| `CORS_ALLOWED_ORIGINS` | dev: `http://localhost:5173` | Không cần khi đi qua Vite proxy / nginx gateway |
| `PUBLIC_BASE_URL` | `http://localhost` | Link gửi khách qua Zalo |
| `SERVER_PORT` | `8082` | |

## Cấu trúc

```
backend-common/            (dùng chung với client-console/backend)
  domain/                  JPA entities + enums (User, Booking, Asset, Contract, ...)
  repository/              Spring Data repositories (truy vấn theo công thức trong spec)
  api/ApiResponse          Envelope {success, code, message, data, errors}
  exception/               AppException + GlobalExceptionHandler (mã lỗi theo spec)
  support/CodeGenerator    Mã NV-001 / HD-105 / TRX-001 từ PostgreSQL sequence

admin-console/backend/
  security/                JWT (access 2h / refresh 30d), RBAC theo URL + @PreAuthorize
  auth/ dashboard/ booking/ packages/ asset/ staff/ crm/ contract/ finance/
  notification/ search/ settings/ demo/
  resources/db/migration/  Flyway (Hibernate chạy ddl-auto=validate)
```

## API

Tất cả dưới `/api/v1/admin` (nginx gateway chỉ định tuyến tiền tố này tới service). `ADMIN` = chỉ quản lý; còn lại cả quản lý và nhân viên.

| Endpoint | Quyền | Spec |
|---|---|---|
| `POST /auth/login`, `POST /auth/refresh`, `GET /auth/me` | public / đã đăng nhập | FN-AUTH-01 |
| `GET /dashboard/stats`, `GET /dashboard/revenue-chart?year=` | ADMIN | FN-ADM-DASH-01 |
| `GET /bookings/calendar?view=day\|week\|month&date=&staffId=`, `POST /bookings` | tất cả (nhân viên chỉ thấy lịch của mình) | FN-ADM-CAL-01/02 |
| `GET /packages`, `POST /packages`, `PATCH /packages/{id}/active` | đọc: tất cả · ghi: ADMIN | §5.4 |
| `GET /assets?category=`, `GET /assets/conflicts`, `POST /assets`, `POST /assets/booking` | tạo đồ: ADMIN | FN-ADM-ASSET-01 |
| `GET /staff`, `POST /staff`, `GET/POST /staff/{id}/tasks`, `PATCH/DELETE /staff/tasks/{id}` | đọc DS: tất cả · còn lại: ADMIN | §5.6 |
| `GET /crm/pipeline`, `POST /crm/customers`, `PATCH /crm/customers/{id}/stage` | tất cả | FN-ADM-CRM-01 |
| `GET /contracts`, `POST /contracts`, `GET /contracts/{id}/preview-html`, `GET /contracts/{id}/export-pdf`, `POST /contracts/{id}/share-zalo` | tạo: ADMIN | FN-ADM-CONTR-01 |
| `GET /financials/transactions`, `GET /financials/summary`, `POST /financials/transactions` | ADMIN | FN-ADM-FIN-01 |
| `GET /notifications`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` | tất cả | |
| `GET /search?q=` | tất cả | |
| `GET /settings/studio`, `PUT /settings/studio` | ghi: ADMIN | |

## Khoảng cách với spec v1.1

Spec `docs/features/detailed_functional_specification.md` v1.1 đã cập nhật theo hành vi hiện tại (401 `INVALID_CREDENTIALS`, `/api/v1/admin/auth/**`, bộ đệm bảo dưỡng hai chiều, `PAYMENT_EXCEEDS_REMAINING`, ma trận RBAC). Các chức năng đánh dấu ⚠️/🆕 trong spec chưa có trong code — đáng chú ý:

- **Vòng đời hợp đồng (§1.6):** code chuyển `COMPLETED` ngay khi trả đủ; spec yêu cầu trả đủ **và** xác nhận bàn giao, thêm `CANCELLED`, tự chuyển `IN_PROGRESS` vào ngày sự kiện.
- **Auth (FN-ADM-AUTH-01..04):** refresh token chưa lưu Redis nên chưa thu hồi/xoay vòng được; chưa có logout, đổi mật khẩu, khóa sau 5 lần sai, quản lý tài khoản (FN-ADM-USER-01).
- **Phân trang (§1.2)** cho giao dịch, hợp đồng, khách hàng, thông báo — hiện trả toàn bộ.
- **Kiểm tra trùng lịch nhân sự** chưa khóa đồng thời (2 request cùng lúc có thể cùng lọt) — spec yêu cầu `SELECT … FOR UPDATE` trên nhân sự.
- **Hợp đồng gắn khách bằng SĐT** — spec yêu cầu `leadId` (FN-ADM-CRM-03); link Zalo đang chứa UUID hợp đồng thay vì token công khai (FN-PUB-CONTR-01).
- **Hợp đồng chưa có địa chỉ khách (Bên A)**, chưa có `eventDate`/`paymentStatus` trong danh sách; danh sách quyền lợi gói được in theo gói hiện tại (chưa chụp lại lúc ký).
- Tính năng mới: lịch thanh toán 3 đợt + nhắc nợ, UI giữ lịch & giao/nhận trang phục, sửa/hủy, hủy phiếu thu chi, tự phục vụ cho nhân viên, nhật ký thao tác.

## TODO hạ tầng

- Zalo ZNS: `ZaloClient.LoggingZaloClient` chỉ ghi log — cần OA access token + template ZNS.
- Lưu PDF vào MinIO (`pdf_file_url`) thay vì render mỗi lần tải.
