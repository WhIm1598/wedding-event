# ADR-0001: Nhiều studio dùng chung một hệ thống (DB chung + `studio_id`)

- **Trạng thái:** Đã chấp nhận hướng đi (2026-10-26). Thiết kế chi tiết dưới đây **chờ duyệt** trước khi code.
- **Người quyết định:** chủ sản phẩm.

## Bối cảnh

Web admin (Lumière Studios) sẽ được bán cho nhiều studio. Hiện code chỉ phục vụ **một** studio:
- `studio_settings` chỉ có một dòng (`id = 1`);
- mã chứng từ `HD-`/`NV-`/`TRX-` đánh số chung bằng sequence PostgreSQL;
- tài khoản console không gắn với studio nào;
- app WedPlanner là một chợ chung: cặp đôi xem gói của mọi studio và vendor.

Có ba lựa chọn:
- **A.** Mỗi khách mua một bộ source code + DB riêng.
- **B.** Một hệ thống, DB chung, mỗi dòng dữ liệu studio có `studio_id`.
- **C.** Mỗi studio một schema/DB, chung code.

## Quyết định

Chọn **B**, bán theo **thuê bao**. Studio mới do **chủ nền tảng tạo** qua màn super-admin; chưa có tự đăng ký và chưa thu tiền tự động. Mỗi studio gửi Zalo bằng **OA riêng** (đã làm sẵn ở FN-ADM-SET-02: cấu hình nằm trong cài đặt studio).

**Lý do:**
- **Chợ chung trên app mobile:** chỉ B cho app đọc được gói của mọi studio trong một truy vấn. A làm hỏng chợ chung. C buộc phải gộp dữ liệu từ N schema.
- **Vận hành:** một lần deploy, một lần backup và một lần chạy migration cho tất cả. A và C nhân số việc này theo số khách.
- **Kiểm soát code:** bán source code (A) thì không ngăn được sao chép, và mỗi khách sửa ra một nhánh riêng.

Cài riêng cho khách lớn vẫn làm được bằng chính code này và thư mục `deploy/`, nhưng là ngoại lệ, ràng buộc bằng hợp đồng license.

**Rủi ro chính:** lộ dữ liệu giữa các studio nếu một truy vấn quên lọc. Chặn ở tầng framework, không trông vào từng truy vấn (xem thiết kế).

## Thiết kế đề xuất (chờ duyệt)

### Dữ liệu
- **Bảng `studios` mới:** `id UUID`, `name`, `status` (`ACTIVE`/`SUSPENDED`), `subscription_expires_at`, `created_at`.
- **Migration:**
  - tạo dòng Lumière từ `studio_settings` hiện có;
  - thêm `studio_id UUID NOT NULL REFERENCES studios` cho mọi bảng dữ liệu studio, giá trị mặc định là id của Lumière, rồi bỏ mặc định;
  - không mất dữ liệu cũ.
- **Bảng có `studio_id`:** `staff_members`, `staff_tasks`, `service_packages` (gói của studio; gói vendor để null), `bookings`, `assets`, `asset_reservations`, `leads`, `contracts`, `payment_transactions`, `notifications` (feed studio), `users` (chỉ tài khoản console; tài khoản mobile để null).
- **`studio_settings`:** khoá chính đổi thành `studio_id` (một dòng mỗi studio), gồm cả cấu hình Zalo OA.
- **Mã chứng từ theo từng studio:** bảng `studio_counters(studio_id, kind, next_value)`, tăng có khoá dòng. Mỗi studio bắt đầu từ `HD-001`. Unique `(studio_id, contract_number)` thay cho unique toàn cục.
- **Không đổi:** bảng mobile (`wedding_plans`, `guests`, `vendor_*`, `quote_requests`, …), vì chúng thuộc cặp đôi/vendor, không thuộc studio.

### Cách lọc theo studio
- **Hibernate `@TenantId`** trên trường `studioId` của các entity studio:
  - Hibernate tự thêm điều kiện `studio_id` vào mọi truy vấn entity/JPQL và tự gán khi insert;
  - một `CurrentTenantIdentifierResolver` đọc claim `studioId` trong JWT của request.
- **Ngoại lệ, chạy ở chế độ "root" (không lọc):**
  - client-console/backend: app mobile đọc catalog của mọi studio;
  - job nền (dọn token, nhắc thanh toán);
  - trang hợp đồng công khai: tìm hợp đồng theo hash token, rồi làm việc trong studio của hợp đồng đó;
  - API super-admin.
- **Truy vấn native SQL** (nếu có) phải tự lọc `studio_id`. Review và test chéo bắt chỗ thiếu.
- **Phương án dự phòng nếu `@TenantId` không đủ:** Row Level Security của PostgreSQL (`SET app.studio_id` mỗi transaction).

### Tài khoản & phân quyền
- **JWT console thêm claim `studioId`.** `ConsoleAuthenticationConverter` đã đọc lại tài khoản mỗi request; thêm kiểm tra studio còn `ACTIVE` và chưa hết hạn. Nếu không → `403 STUDIO_SUSPENDED` (mã mới).
- **Vai trò mới `ROLE_PLATFORM_ADMIN`** (chủ nền tảng), không thuộc studio nào. API riêng `/api/v1/platform/**`:
  - tạo studio cùng admin đầu tiên (dùng lại luồng mật khẩu tạm của FN-ADM-USER-01);
  - xem danh sách studio;
  - gia hạn / khoá studio.
- **`AdminBootstrap`** tạo platform admin thay cho admin của studio.
- **Email đăng nhập vẫn unique toàn hệ thống:** một email thuộc một studio. Đăng nhập không cần chọn studio, tài khoản tự xác định studio.
- **Một domain chung** cho mọi studio, không cần subdomain riêng.

### App mobile (client-console/backend)
- Catalog: gói của studio lấy tên, địa chỉ và Zalo từ `studio_settings` của **studio sở hữu gói** (hiện lấy từ dòng duy nhất), và không hiện gói của studio đang bị khoá.

### Kiểm thử
- Bộ test chéo cho từng module: tạo hai studio, đăng nhập studio B rồi gọi mọi endpoint list/get/update với id của studio A. Kết quả phải là `404` hoặc danh sách rỗng, không bao giờ trả dữ liệu.
- Test mã chứng từ: hai studio đều bắt đầu từ `HD-001`.

### Chia giai đoạn
1. Bảng `studios`, migration `studio_id`, `@TenantId` + resolver, JWT `studioId`, test chéo. Lumière chạy như cũ.
2. API và màn super-admin: tạo studio, khoá, gia hạn; `STUDIO_SUSPENDED`.
3. Mã chứng từ theo studio; `studio_settings` theo studio.
4. Catalog mobile nhiều studio.

## Hệ quả
- Mỗi tính năng mới cho studio phải đặt `studio_id` (có `@TenantId`) và có test chéo.
- Backup/restore là toàn hệ thống. Muốn khôi phục riêng một studio thì phải lọc khi restore (chấp nhận được ở quy mô hiện tại).
- Thu tiền thuê bao làm thủ công ngoài hệ thống (gia hạn `subscription_expires_at`) cho tới khi có cổng thanh toán.
