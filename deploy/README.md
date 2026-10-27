# Deploy production — Lumière Studios web admin

Lần đầu deploy, chưa quen Linux/Docker: đọc [hướng dẫn từng bước](../docs/setup/deploy-linux.md) (từ VPS trống tới kiểm thử).

Một server Linux (Ubuntu 22.04+, 2 vCPU / 4 GB RAM là đủ), Docker Engine + Docker Compose v2, một tên miền trỏ về IP
server. Stack gồm `postgres`, `admin-backend`, `admin-frontend`, `gateway` (nginx, HTTPS), `certbot` (gia hạn chứng
chỉ) và `backup` (pg_dump hằng ngày). Chỉ `gateway` mở cổng 80/443; PostgreSQL không ra Internet.

App mobile (`client-console`) không nằm trong stack này.

## 1. Chuẩn bị (một lần)

```bash
# Tường lửa: chỉ SSH + web
sudo ufw allow OpenSSH && sudo ufw allow 80,443/tcp && sudo ufw enable

git clone https://github.com/WhIm1598/wedding-event.git && cd wedding-event/deploy
cp .env.example .env && chmod 600 .env
```

Điền `.env`:

| Biến | Giá trị |
|---|---|
| `DOMAIN` | Tên miền admin, ví dụ `admin.lumiere.vn` (bản ghi A đã trỏ về server) |
| `DB_PASSWORD` | `openssl rand -base64 32` |
| `JWT_SECRET` | `openssl rand -base64 48`. Đổi giá trị này sẽ đăng xuất mọi người |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Tài khoản **quản trị nền tảng** đầu tiên (`ROLE_PLATFORM_ADMIN`, ADR-0001), chỉ dùng khi DB chưa có. Mật khẩu ≥ 8 ký tự, có chữ và số |
| `ADMIN_RESET_PASSWORD` | `false`. Quên mật khẩu quản trị nền tảng: đặt `true` cùng `ADMIN_EMAIL` và `ADMIN_PASSWORD` mới, khởi động lại backend, rồi xoá cả ba ([chi tiết](../docs/setup/deploy-linux.md#đặt-lại-mật-khẩu-quản-trị-nền-tảng)) |

`.env` chứa bí mật: không commit, không gửi qua chat. Nên lưu thêm một bản trong trình quản lý mật khẩu của studio.

## 2. Chứng chỉ HTTPS lần đầu

Chạy trước khi bật gateway (cổng 80 phải đang trống):

```bash
source .env
docker compose -f docker-compose.prod.yml run --rm -p 80:80 --entrypoint certbot certbot \
  certonly --standalone -d "$DOMAIN" --email <email-studio> --agree-tos --no-eff-email
```

Sau đó service `certbot` tự gia hạn mỗi 12 giờ, `gateway` nạp lại chứng chỉ mỗi 6 giờ.

## 3. Chạy

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps        # admin-backend phải "healthy" (Flyway chạy migration lúc khởi động)
```

1. Mở `https://<DOMAIN>`, đăng nhập bằng `ADMIN_EMAIL` / `ADMIN_PASSWORD`: đây là tài khoản **quản trị nền tảng**, chỉ thấy màn *Quản trị nền tảng*.
2. Xoá hai dòng `ADMIN_EMAIL`, `ADMIN_PASSWORD` khỏi `.env`, rồi `docker compose -f docker-compose.prod.yml up -d`.
3. Ở màn *Quản trị nền tảng*:
   - DB mới đã có sẵn một studio (tạo từ cài đặt mặc định). Bấm nút thêm quản lý (biểu tượng người) để tạo quản lý cho studio đó, ví dụ Lumière;
   - mỗi khách mới: **Thêm studio** (tên, hạn thuê bao, email quản lý). Mật khẩu tạm chỉ hiện một lần, gửi cho quản lý của studio.
4. Quản lý studio đăng nhập, đổi mật khẩu tạm, vào *Cài đặt* nhập thông tin studio.
5. *Cài đặt → Tài khoản*: tạo tài khoản cho từng nhân viên (gắn hồ sơ nhân sự). Mật khẩu tạm chỉ hiện một lần; nhân viên phải đổi khi đăng nhập lần đầu.
6. *(Tuỳ chọn)* *Cài đặt → Zalo OA*: kết nối Zalo OA của studio để nút **Gửi Zalo** gửi hợp đồng qua ZNS — làm theo [docs/setup/zalo-oa-zns.md](../docs/setup/zalo-oa-zns.md). Link trong tin dùng `https://<DOMAIN>/c/...` (compose tự đặt `PUBLIC_BASE_URL`).

## 4. Sao lưu và khôi phục

Service `backup` ghi `deploy/backups/wedding_db-YYYY-MM-DD.dump` mỗi 24 giờ (lần đầu ngay khi khởi động) và giữ 14 ngày.
**Bản sao trên cùng server không đủ**: hỏng ổ đĩa là mất cả hai. Đẩy ra ngoài server hằng ngày, ví dụ với
[rclone](https://rclone.org) đã cấu hình remote `offsite` (Google Drive, S3, …):

```bash
# crontab -e trên server
30 4 * * * rclone copy /path/to/wedding-event/deploy/backups offsite:wedding-backups --max-age 48h
```

Khôi phục (thử một lần trước khi go-live, và mỗi khi đổi cấu hình):

```bash
cd deploy
docker compose -f docker-compose.prod.yml stop admin-backend
docker compose -f docker-compose.prod.yml exec backup sh -c \
  'dropdb wedding_db && createdb wedding_db && pg_restore -d wedding_db /backups/wedding_db-YYYY-MM-DD.dump'
docker compose -f docker-compose.prod.yml start admin-backend
```

Thử khôi phục mà không đụng DB thật: `createdb restore_test && pg_restore -d restore_test /backups/<file>.dump`,
kiểm tra `psql -d restore_test -c 'select count(*) from contracts'`, rồi `dropdb restore_test`.

## 5. Cập nhật phiên bản

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

Migration mới chạy tự động khi backend khởi động. Chạy một bản backup thủ công trước khi cập nhật:
`docker compose -f docker-compose.prod.yml exec backup sh -c 'pg_dump -Fc -f /backups/pre-update-$(date +%F-%H%M).dump'`.

## Bảo mật đã có sẵn

- HTTPS bắt buộc (HTTP chuyển hướng 301), HSTS; header CSP / X-Frame-Options trên trang web.
- Đăng nhập: tối đa 20 request/phút/IP ở gateway; sai mật khẩu 5 lần liên tiếp thì khoá 15 phút.
- Chỉ `/api/v1/admin/**`, `/api/v1/platform/**` và `/api/v1/public/**` đi vào backend; actuator và các API khác không ra ngoài.
- Trang hợp đồng công khai `/api/v1/public/**`: tối đa 30 request/phút/IP; gateway ghi đè `X-Forwarded-For` bằng IP thật của khách.
- Log: `docker compose -f docker-compose.prod.yml logs -f admin-backend` (không ghi mật khẩu, token).
