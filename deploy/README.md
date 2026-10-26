# Deploy production — Lumière Studios web admin

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
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Tài khoản admin đầu tiên, chỉ dùng khi DB chưa có admin. Mật khẩu ≥ 8 ký tự, có chữ và số |

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

1. Mở `https://<DOMAIN>`, đăng nhập bằng `ADMIN_EMAIL` / `ADMIN_PASSWORD`.
2. Đổi mật khẩu ngay (nút chìa khoá cạnh nút Đăng xuất).
3. Xoá hai dòng `ADMIN_EMAIL`, `ADMIN_PASSWORD` khỏi `.env`, rồi `docker compose -f docker-compose.prod.yml up -d`.
4. Vào *Cài đặt* nhập thông tin studio.

Tài khoản nhân viên: chưa có màn hình quản lý tài khoản (FN-ADM-USER-01 chưa làm). Đến khi có, studio dùng tài khoản admin.

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
- Chỉ `/api/v1/admin/**` và `/api/v1/public/**` đi vào backend; actuator và các API khác không ra ngoài.
- Log: `docker compose -f docker-compose.prod.yml logs -f admin-backend` (không ghi mật khẩu, token).
