# Chuẩn bị vận hành

Những gì cần có để chạy web admin ổn định cho các studio: tài khoản dịch vụ, cách lưu ảnh và file, mục tiêu tốc độ (mọi
màn hình **dưới 3 giây**) kèm số đo thực tế. Viết ngày 2026-10-27, theo code của PR #55.

Cách hệ thống hoạt động: [web-admin-overview.md](web-admin-overview.md). Cài server: [deploy-linux.md](../setup/deploy-linux.md).

---

## 1. Tài khoản cần chuẩn bị

Nguyên tắc: **mọi dịch vụ đăng ký bằng một email dự án**, không dùng email cá nhân của ai. Người nghỉ việc thì đổi mật
khẩu, không mất quyền sở hữu tên miền, server hay dữ liệu.

### Cần ngay trước khi go-live

| # | Tài khoản | Dùng để | Ghi chú |
|---|---|---|---|
| 1 | **Email dự án** (Google, ví dụ `vanhanh.<thuonghieu>@gmail.com`, hoặc Google Workspace theo tên miền) | Chủ sở hữu mọi tài khoản bên dưới, nhận cảnh báo | Bật xác minh 2 bước, thêm số điện thoại và email khôi phục của công ty. Google Workspace (~150.000đ/tháng) cho email `@tenmien.vn` chuyên nghiệp hơn, không bắt buộc |
| 2 | **Trình quản lý mật khẩu** (Bitwarden, 1Password) | Lưu `deploy/.env`, mật khẩu quản trị nền tảng, mật khẩu các tài khoản dưới | Bitwarden miễn phí đủ dùng. Chia sẻ cho người thứ hai tin cậy, phòng khi mất người giữ |
| 3 | **Nhà đăng ký tên miền** | Tên miền web admin | Bật **tự gia hạn**, khoá chuyển tên miền. Hết hạn tên miền = web chết |
| 4 | **Cloudflare** (miễn phí) | Quản lý DNS; sau này lưu ảnh (R2) | Khuyên chuyển DNS về Cloudflare: đổi bản ghi nhanh, có sẵn khi cần lưu ảnh. Để bản ghi A ở chế độ **DNS only** (mây xám) khi lấy chứng chỉ lần đầu |
| 5 | **Nhà cung cấp VPS** | Server chạy hệ thống | Chọn máy đặt ở **Việt Nam hoặc Singapore** (độ trễ < 50 ms tới người dùng). Bật **snapshot tự động hằng tuần** nếu có (thêm một lớp bảo vệ ngoài backup database) |
| 6 | **Nơi lưu backup ngoài server** (Google Drive của email dự án, hoặc Cloudflare R2 / Backblaze B2) | Bản sao database hằng ngày | Cấu hình bằng `rclone` ([deploy-linux.md mục 9.4](../setup/deploy-linux.md#94-chép-backup-ra-ngoài-server)). Google Drive 15 GB miễn phí đủ nhiều năm (mỗi bản backup vài MB) |
| 7 | **Giám sát uptime** (UptimeRobot hoặc Better Stack, miễn phí) | Báo khi web sập hoặc chậm | Theo dõi `https://<tên-miền>/` và `https://<tên-miền>/api/v1/admin/auth/me` (mong đợi 401), mỗi 5 phút; gửi cảnh báo qua email và Zalo/Telegram. Xem được thời gian phản hồi theo ngày |

### Không cần lúc này

| Tài khoản | Khi nào cần |
|---|---|
| **Dịch vụ gửi email (SMTP)** | Hệ thống hiện **không gửi email**: mật khẩu tạm hiện trên màn hình để người quản lý chuyển tay. Cần khi làm "quên mật khẩu qua email" |
| **Zalo OA** | Mỗi **studio tự đăng ký** OA của mình ([zalo-oa-zns.md](../setup/zalo-oa-zns.md)). Người cung cấp chỉ cần OA riêng nếu muốn có kênh hỗ trợ khách hàng |
| **Google Cloud + Firebase** | Khi phát hành app mobile (đăng nhập Google, thông báo đẩy) — [google-cloud-firebase.md](../setup/google-cloud-firebase.md) |
| **Apple Developer** (99 USD/năm), **Google Play Console** (25 USD một lần) | Khi đưa app mobile lên cửa hàng |
| **Tổ chức GitHub** | Repo đang thuộc tài khoản cá nhân và **công khai**. Không có bí mật trong repo, nhưng mã nguồn ai cũng xem được. Chuyển sang tổ chức + private khi có người cùng làm hoặc muốn giữ mã nguồn riêng (khi đó server cần deploy key để `git pull`) |

### Chi phí tháng ước tính giai đoạn đầu

| Khoản | Ước tính |
|---|---|
| VPS 2–4 vCPU / 4–8 GB RAM | 250.000–600.000đ |
| Tên miền `.vn` / `.com` | ~40.000–80.000đ (chia theo tháng) |
| Cloudflare, UptimeRobot, Bitwarden, Google Drive | 0đ |
| **Tổng** | **~300.000–700.000đ/tháng** |

---

## 2. Lưu trữ hình ảnh và file

### Hiện trạng

| Loại | Lưu ở đâu bây giờ |
|---|---|
| Ảnh đại diện tài khoản, ảnh bìa gói dịch vụ | Chỉ là **đường link** (`avatar_url`, `thumbnail_url`), chưa có chức năng tải ảnh lên |
| PDF hợp đồng | **Không lưu**, tạo lại mỗi lần bấm Xuất PDF hoặc khách tải (0,08–0,7 giây) |
| Ảnh cưới giao cho khách (ảnh gốc, album) | Ngoài hệ thống. Studio tự dùng Google Drive, Google Photos… |

Giai đoạn đầu **không cần dịch vụ lưu ảnh**.

### Khi làm chức năng tải ảnh (gallery gói, portfolio vendor trên mobile)

Dùng **object storage** (kho file kiểu S3), không lưu ảnh trong database hay trong ổ của container.

| Lựa chọn | Ưu | Nhược |
|---|---|---|
| **Cloudflare R2** *(khuyên dùng)* | 10 GB miễn phí, **không tính phí băng thông tải xuống**, phục vụ qua CDN của Cloudflare nên ảnh tải nhanh ở mọi nơi, chuẩn S3 | Cần thẻ thanh toán quốc tế để kích hoạt |
| Backblaze B2 | Rẻ (~6 USD/TB/tháng), chuẩn S3 | Băng thông tải xuống tính phí nếu không đi qua Cloudflare |
| MinIO trên chính VPS (kiến trúc ban đầu dự tính) | Miễn phí, dữ liệu nằm trên máy mình | Chiếm ổ VPS, ăn băng thông VPS, **phải backup riêng**, VPS chết là mất ảnh |

Quy tắc khi làm:

1. **Thu nhỏ ảnh trước khi tải lên** (trên trình duyệt/app): cạnh dài ≤ 1600 px, WebP hoặc JPEG chất lượng ~80
   (khoảng 150–400 KB), kèm bản thu nhỏ ~400 px cho danh sách. Ảnh gốc 5–10 MB từ điện thoại làm trang chậm hẳn.
2. **Tải thẳng từ trình duyệt/app lên kho** bằng link tải lên có thời hạn (presigned URL) do backend cấp. Ảnh không đi
   qua backend, server không tốn RAM và băng thông.
3. Database chỉ lưu **khoá file** (ví dụ `studios/<id>/packages/<uuid>.webp`), không lưu cả đường dẫn đầy đủ; đổi nhà
   cung cấp chỉ cần đổi tên miền gốc.
4. **Hai bucket:** công khai cho ảnh gói (hiện trên app), riêng tư cho PDF hợp đồng và giấy tờ (chỉ mở qua link có hạn).
5. Đặt `Cache-Control: public, max-age=31536000, immutable` cho ảnh; ảnh mới thì tên file mới.

---

## 3. Tốc độ: mục tiêu dưới 3 giây

**Mục tiêu:** mở bất kỳ màn hình nào **< 3 giây** trên điện thoại 4G; thao tác (lưu, chuyển giai đoạn) **< 1 giây**.

### Số đo ngày 2026-10-27

Đo trên máy dev (backend thật + PostgreSQL), frontend ở chế độ dev (chậm hơn bản build production), **chưa tính độ trễ
mạng**.

**Backend** (thời gian trả lời một request, lần thứ hai trở đi):

| API | Dữ liệu demo | ~3 năm dữ liệu một studio đông* |
|---|---|---|
| Tổng quan (số liệu, biểu đồ) | 0,01–0,03 s | 0,01–0,03 s |
| Danh sách hợp đồng | 0,01 s | 0,07 s, **0,98 MB** |
| Khách hàng (Kanban) | 0,01 s | 0,07 s, **0,87 MB** |
| Sổ thu chi | 0,02 s | 0,31 s, **3,7 MB** |
| Tìm kiếm | 0,02–0,04 s | 0,02 s |
| Xuất PDF hợp đồng | 0,08 s (lần đầu sau khởi động 0,7 s) | — |
| Đăng nhập | 0,11 s | — |

\* Bơm tạm 3.000 hợp đồng, 20.000 phiếu thu chi, 5.000 khách vào một studio, đo xong đã xoá.

**Trình duyệt** (bấm menu tới khi bảng hiện đủ dòng), với ~3 năm dữ liệu:

| Màn | Thời gian |
|---|---|
| Kế toán (20.000 phiếu) | **3,7 s** ❌ |
| Khách hàng → Hợp đồng (3.000 hợp đồng) | **2,0 s** ⚠️ |
| Tổng quan | tức thì |

**Tải trang lần đầu:** JavaScript 351 KB (106 KB nếu nén), CSS 32 KB. File được cache 30 ngày, lần sau gần như tức thì.

### Kết luận

- **Giai đoạn đầu đạt mục tiêu.** Backend trả lời trong vài chục mili giây; một studio cỡ vừa (~300 hợp đồng,
  ~2.000 phiếu thu chi mỗi năm) còn xa ngưỡng chậm.
- **Hai việc cần làm, theo thứ tự:**

| # | Việc | Vì sao | Khi nào | Công sức |
|---|---|---|---|---|
| 1 | **Bật nén gzip ở gateway nginx** | Hiện chưa nén gì: JSON nhỏ đi ~10 lần, JavaScript ~3 lần. Trên 4G, danh sách 1 MB mất ~0,5–1 s chỉ để tải | **Trước go-live** | Vài dòng cấu hình |
| 2 | **Phân trang / lọc theo tháng** cho Kế toán, Hợp đồng, Khách hàng | Các danh sách đang trả **toàn bộ** dữ liệu; trình duyệt vẽ hàng chục nghìn dòng thì chậm, dù backend nhanh | Khi một studio vượt ~5.000 phiếu thu chi hoặc ~1.500 hợp đồng (vài năm với studio cỡ vừa), hoặc khi giám sát thấy màn nào > 2 s | Cần đổi API + giao diện, theo spec |

### Theo dõi trong lúc vận hành

- **UptimeRobot / Better Stack** (mục 1) ghi thời gian phản hồi mỗi 5 phút: thường xuyên > 1 s là dấu hiệu cần xem.
- Mỗi tháng, mở Kế toán và Khách hàng của studio nhiều dữ liệu nhất trên điện thoại, bấm giờ.
- Trên server: `docker stats` (CPU, RAM từng service), `df -h` (ổ đĩa). RAM thường xuyên > 85% thì nâng VPS.
- Đếm dữ liệu studio lớn nhất (chạy trong `deploy/`):

  ```bash
  docker compose -f docker-compose.prod.yml exec postgres psql -U wedding_admin -d wedding_db -c \
    "select studio_id, count(*) from payment_transactions group by 1 order by 2 desc limit 3"
  ```

---

## 4. Danh sách việc trước go-live

- [ ] Email dự án, trình quản lý mật khẩu, tên miền (tự gia hạn), Cloudflare DNS, VPS (Việt Nam/Singapore, snapshot)
- [ ] Deploy theo [deploy-linux.md](../setup/deploy-linux.md), lưu `deploy/.env` vào trình quản lý mật khẩu
- [ ] Bật gzip ở gateway (mục 3, việc 1)
- [ ] Backup chép ra ngoài server, **thử khôi phục một lần**
- [ ] Giám sát uptime với cảnh báo
- [ ] Chạy danh sách kiểm thử chức năng (deploy-linux.md mục 9.2)
- [ ] Thử gửi Zalo thật với một studio (#53)
