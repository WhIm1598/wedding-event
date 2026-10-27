# Deploy web admin lên server Linux — hướng dẫn cho người mới

Tài liệu này dẫn từng bước, từ một server Linux trống tới khi web admin chạy ở `https://<tên-miền>` và đã kiểm thử xong.
Không cần biết Java, React hay Docker trước. Chỉ cần gõ lệnh theo đúng thứ tự.

Bản tóm tắt cho người đã quen: [deploy/README.md](../../deploy/README.md).

> **Phạm vi:** web admin (backend admin, frontend, PostgreSQL, nginx HTTPS, sao lưu). Backend của app mobile chưa nằm
> trong bản deploy này (issue #56).

## Cách nhanh: script `install.sh`

Sau khi làm **bước 0, 1** (VPS, tên miền đã trỏ về server) và đăng nhập SSH, một script làm thay các bước 2.2–9.1:

```bash
sudo apt update && sudo apt install -y git
git clone https://github.com/WhIm1598/wedding-event.git && cd wedding-event
sudo deploy/install.sh admin.tenstudio.vn ban@tenstudio.vn
```

Script cài Docker, bật tường lửa và swap, **tự sinh mật khẩu** vào `deploy/.env` (không cần gõ, không đi qua chat), kiểm tra
tên miền trỏ đúng, lấy chứng chỉ HTTPS, build và chạy, đợi backend sẵn sàng rồi chạy các kiểm tra ở mục 9.1. Cuối cùng in
ra **mật khẩu quản trị nền tảng (chỉ hiện một lần)** và các việc còn lại: bước 8 (đổi mật khẩu, xoá `ADMIN_*`, tạo studio),
9.2–9.4 (kiểm thử chức năng, thử khôi phục, chép backup ra ngoài).

Chạy lại script nhiều lần không sao: nó giữ `.env`, chứng chỉ và database đã có. Script dừng ở bước nào thì đọc thông báo,
xem mục tương ứng bên dưới và [Xử lý sự cố](#10-xử-lý-sự-cố). Bước 2.3 (tạo người dùng riêng) vẫn nên tự làm trước.

Muốn hiểu từng bước, hoặc làm tay: đọc tiếp từ bước 0.

---

## 0. Cần chuẩn bị trước

| Thứ cần có | Ghi chú |
|---|---|
| **Một VPS Linux** | Ubuntu 22.04 hoặc 24.04, **2 vCPU / 4 GB RAM / 40 GB ổ đĩa** trở lên. Mua ở nhà cung cấp bất kỳ (Vultr, DigitalOcean, Viettel IDC, …). Ghi lại **địa chỉ IP** và **mật khẩu root** (hoặc SSH key) |
| **Một tên miền** | Ví dụ `admin.tenstudio.vn`. Mua ở nhà đăng ký tên miền bất kỳ |
| **Máy tính của bạn** | Windows 10+, macOS hoặc Linux, có cửa sổ dòng lệnh (Windows: *PowerShell* hoặc *Windows Terminal*) |
| **Một email** | Để Let's Encrypt báo khi chứng chỉ HTTPS sắp hết hạn |

Trong tài liệu, thay các chỗ sau bằng giá trị thật của bạn:

- `203.0.113.10`: IP của VPS
- `admin.tenstudio.vn`: tên miền
- `deploy`: tên người dùng Linux sẽ tạo ở bước 2

---

## 1. Trỏ tên miền về VPS

Làm việc này **đầu tiên**, vì DNS cần vài phút tới vài giờ mới có hiệu lực.

1. Vào trang quản lý DNS của nhà đăng ký tên miền.
2. Thêm bản ghi:

   | Loại | Tên (Host) | Giá trị | TTL |
   |---|---|---|---|
   | `A` | `admin` (hoặc `@` nếu dùng tên miền gốc) | `203.0.113.10` | mặc định |

3. Kiểm tra từ máy của bạn (đợi tới khi ra đúng IP):

   ```bash
   nslookup admin.tenstudio.vn
   ```

---

## 2. Đăng nhập server và chuẩn bị hệ điều hành

### 2.1. Đăng nhập lần đầu

Trên máy của bạn:

```bash
ssh root@203.0.113.10
```

Lần đầu sẽ hỏi `Are you sure you want to continue connecting?` → gõ `yes`. Nhập mật khẩu root (gõ không hiện chữ, cứ gõ rồi Enter).

### 2.2. Cập nhật hệ thống

```bash
apt update && apt upgrade -y
timedatectl set-timezone Asia/Ho_Chi_Minh
```

### 2.3. Tạo người dùng riêng (không làm việc bằng root)

```bash
adduser deploy            # đặt mật khẩu, các câu hỏi khác Enter bỏ qua
usermod -aG sudo deploy
```

Thoát và đăng nhập lại bằng người dùng mới:

```bash
exit
ssh deploy@203.0.113.10
```

Từ đây, lệnh có `sudo` sẽ hỏi mật khẩu của `deploy`.

### 2.4. Tường lửa: chỉ mở SSH và web

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80,443/tcp
sudo ufw enable           # hỏi "Proceed?" → y
sudo ufw status
```

> Phải `allow OpenSSH` **trước** `enable`, nếu không bạn sẽ tự khoá mình khỏi server.

### 2.5. Thêm bộ nhớ swap (nên làm nếu RAM ≤ 4 GB)

Lần build đầu (biên dịch Java và React bên trong Docker) cần nhiều RAM. Swap giúp không bị dừng giữa chừng.

```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h                   # dòng "Swap" phải hiện 4.0Gi
```

---

## 3. Cài Docker và Git

```bash
sudo apt install -y git ca-certificates curl
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker deploy
```

**Thoát ra và đăng nhập lại** (`exit` rồi `ssh deploy@...`) để quyền dùng Docker có hiệu lực. Kiểm tra:

```bash
docker --version          # Docker version 2x.x ...
docker compose version    # Docker Compose version v2.x ...
docker run --rm hello-world
```

---

## 4. Lấy mã nguồn

```bash
cd ~
git clone https://github.com/WhIm1598/wedding-event.git
cd wedding-event/deploy
```

Mọi lệnh từ đây chạy trong thư mục `~/wedding-event/deploy`, trừ khi ghi khác.

---

## 5. Tạo file cấu hình bí mật `.env`

```bash
cp .env.example .env
chmod 600 .env
```

Tạo sẵn hai chuỗi ngẫu nhiên:

```bash
openssl rand -base64 32   # dùng cho DB_PASSWORD
openssl rand -base64 48   # dùng cho JWT_SECRET
```

Mở file để sửa:

```bash
nano .env
```

Điền như sau (giữ nguyên tên biến, không có khoảng trắng quanh dấu `=`):

```dotenv
DOMAIN=admin.tenstudio.vn
DB_PASSWORD=<chuỗi thứ nhất>
JWT_SECRET=<chuỗi thứ hai>
ADMIN_EMAIL=ban@tenstudio.vn
ADMIN_PASSWORD=<mật khẩu ≥ 8 ký tự, có cả chữ và số>
```

Lưu trong `nano`: `Ctrl+O`, Enter, rồi `Ctrl+X` để thoát.

| Biến | Ý nghĩa |
|---|---|
| `DOMAIN` | Tên miền ở bước 1 |
| `DB_PASSWORD` | Mật khẩu database. **Không đổi** sau khi đã chạy lần đầu (database đã tạo với mật khẩu này) |
| `JWT_SECRET` | Khoá ký phiên đăng nhập. Đổi giá trị này sẽ đăng xuất mọi người |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Tài khoản **quản trị nền tảng** đầu tiên (quản lý các studio). Chỉ được đọc khi database chưa có tài khoản này |

> `.env` chứa bí mật: **không commit lên git, không gửi qua chat/email**. Lưu thêm một bản trong trình quản lý mật khẩu
> (Bitwarden, 1Password, …). Mất `DB_PASSWORD` thì vẫn khôi phục được từ backup, nhưng rất phiền.

---

## 6. Lấy chứng chỉ HTTPS (một lần)

Chạy khi DNS ở bước 1 đã trỏ đúng và cổng 80 đang trống (chưa bật stack):

```bash
source .env
docker compose -f docker-compose.prod.yml run --rm -p 80:80 --entrypoint certbot certbot \
  certonly --standalone -d "$DOMAIN" --email ban@tenstudio.vn --agree-tos --no-eff-email
```

Thành công khi thấy `Successfully received certificate`. Chứng chỉ nằm trong `deploy/certbot/conf`.
Sau này service `certbot` tự gia hạn, bạn không phải làm gì thêm.

Nếu lỗi, xem mục [Xử lý sự cố](#10-xử-lý-sự-cố).

---

## 7. Build và chạy

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

Lần đầu mất **5–15 phút** (tải image, biên dịch backend và frontend). Các lần sau nhanh hơn.

Xem trạng thái:

```bash
docker compose -f docker-compose.prod.yml ps
```

Đợi tới khi:

| Service | Trạng thái mong đợi |
|---|---|
| `postgres` | `Up ... (healthy)` |
| `admin-backend` | `Up ... (healthy)`, có thể mất 1–2 phút sau khi khởi động (đang chạy migration database) |
| `admin-frontend`, `gateway`, `certbot`, `backup` | `Up` |

Xem log nếu một service không lên:

```bash
docker compose -f docker-compose.prod.yml logs --tail 100 admin-backend
```

---

## 8. Thiết lập lần đầu trên web

1. Mở `https://admin.tenstudio.vn` trên trình duyệt. Có ổ khoá HTTPS là đúng.
2. Đăng nhập bằng `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Bạn sẽ vào màn **Quản trị nền tảng**.
3. Bấm biểu tượng **chìa khoá** ở góc trên để **đổi mật khẩu** khác với mật khẩu trong `.env`.
4. Xoá hai dòng `ADMIN_EMAIL`, `ADMIN_PASSWORD` khỏi `.env` rồi áp dụng:

   ```bash
   nano .env          # xoá hai dòng, lưu lại
   docker compose -f docker-compose.prod.yml up -d
   ```

5. Ở màn **Quản trị nền tảng**:
   - Database mới đã có sẵn một studio. Bấm nút **thêm quản lý** (biểu tượng người) để tạo tài khoản quản lý cho studio đó.
   - Khách mới mua: bấm **Thêm studio** (tên, hạn thuê bao, email quản lý).
   - **Mật khẩu tạm chỉ hiện một lần**: sao chép và gửi riêng cho quản lý studio.
6. Quản lý studio đăng nhập → bắt buộc đổi mật khẩu tạm → **Cài đặt** nhập thông tin studio (tên, địa chỉ, mã số thuế,
   tài khoản ngân hàng), tạo **Gói dịch vụ**, tạo tài khoản nhân viên ở **Cài đặt → Tài khoản**.
7. *(Tuỳ chọn)* Gửi hợp đồng qua Zalo: làm theo [zalo-oa-zns.md](zalo-oa-zns.md).

---

## 9. Kiểm thử sau khi deploy

### 9.1. Kiểm tra kỹ thuật (trên server hoặc máy bạn)

```bash
# HTTP phải chuyển sang HTTPS (dòng đầu: 301, có "Location: https://...")
curl -sI http://admin.tenstudio.vn | head -3

# Trang web trả 200
curl -sI https://admin.tenstudio.vn | head -1

# API sống: chưa đăng nhập thì 401 kèm JSON
curl -s https://admin.tenstudio.vn/api/v1/admin/auth/me

# Chứng chỉ còn hạn bao lâu
echo | openssl s_client -connect admin.tenstudio.vn:443 -servername admin.tenstudio.vn 2>/dev/null | openssl x509 -noout -dates

# Có file backup đầu tiên
ls -lh backups/
```

### 9.2. Kiểm tra chức năng (trên trình duyệt)

Đánh dấu từng dòng. Dùng một studio thử, xong thì tạm ngưng nó ở màn Quản trị nền tảng.

- [ ] Quản trị nền tảng: đăng nhập, thấy danh sách studio, tạo được studio mới, mật khẩu tạm hiện ra
- [ ] Quản lý studio mới: đăng nhập bằng mật khẩu tạm, bị bắt đổi mật khẩu, đổi xong vào được Tổng quan
- [ ] Cài đặt: lưu thông tin studio, tạo một gói dịch vụ
- [ ] Tạo khách hàng (CRM), tạo hợp đồng có tiền cọc → Tổng quan và Thu chi hiện đúng số tiền
- [ ] Xuất PDF hợp đồng mở được, tên studio và số tiền đúng
- [ ] Tạo lịch hẹn, thấy trên Lịch
- [ ] Tạo tài khoản nhân viên, đăng nhập bằng nhân viên: không thấy Thu chi, Cài đặt
- [ ] Studio khác **không** thấy dữ liệu của studio thử (đăng nhập quản lý studio khác để xem)
- [ ] Tạm ngưng studio thử → quản lý studio đó đăng nhập bị báo studio đã tạm ngưng
- [ ] Sai mật khẩu 5 lần → bị khoá 15 phút
- [ ] Mở trên điện thoại: giao diện dùng được

### 9.3. Thử khôi phục backup (bắt buộc làm một lần trước khi dùng thật)

Không đụng database thật, chỉ khôi phục vào một database tạm:

```bash
F=$(ls -t backups/*.dump | head -1); echo "$F"
docker compose -f docker-compose.prod.yml exec backup sh -c \
  "createdb restore_test && pg_restore -d restore_test /backups/$(basename "$F") && psql -d restore_test -c 'select count(*) from studios' && dropdb restore_test"
```

Ra một con số (số studio) là backup dùng được.

### 9.4. Chép backup ra ngoài server

Backup nằm cùng server: hỏng ổ đĩa hoặc mất VPS là mất cả hai. Đẩy ra ngoài hằng ngày, ví dụ bằng
[rclone](https://rclone.org) tới Google Drive:

```bash
sudo apt install -y rclone
rclone config             # tạo remote tên "offsite", làm theo câu hỏi (chọn Google Drive)
rclone copy ~/wedding-event/deploy/backups offsite:wedding-backups
rclone ls offsite:wedding-backups   # thấy file .dump là được
crontab -e                # thêm dòng dưới, lưu lại
```

```cron
30 4 * * * rclone copy /home/deploy/wedding-event/deploy/backups offsite:wedding-backups --max-age 48h
```

---

## 10. Xử lý sự cố

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Bước 6 báo `DNS problem` / `NXDOMAIN` | Tên miền chưa trỏ về IP | Kiểm tra lại bước 1 bằng `nslookup`, đợi thêm |
| Bước 6 báo `Timeout during connect` | Cổng 80 bị chặn | `sudo ufw status` phải có `80,443/tcp ALLOW`; kiểm tra tường lửa trên trang quản lý VPS |
| Bước 6 báo `port 80 is already in use` | Stack (hoặc nginx/apache cài sẵn) đang chạy | `docker compose -f docker-compose.prod.yml down`, `sudo systemctl stop nginx apache2` rồi chạy lại |
| Build dừng giữa chừng, log có `Killed` / `exit code 137` | Hết RAM | Làm bước 2.5 (swap), chạy lại `up -d --build` |
| `gateway` khởi động lại liên tục | Chưa có chứng chỉ (chưa làm bước 6) hoặc `DOMAIN` gõ sai | `docker compose -f docker-compose.prod.yml logs gateway`; làm bước 6 với đúng `DOMAIN` |
| `admin-backend` không `healthy` | Sai `DB_PASSWORD` so với lần chạy đầu, hoặc `ADMIN_PASSWORD` quá yếu | Xem `logs admin-backend`. Lỗi `password authentication failed`: đặt lại đúng `DB_PASSWORD` cũ. Lỗi `ADMIN_PASSWORD must have`: đặt mật khẩu ≥ 8 ký tự có chữ và số |
| Web báo `502 Bad Gateway` | Backend đang khởi động hoặc đã dừng | Đợi 1–2 phút; `ps` và `logs admin-backend` |
| Đăng nhập báo sai dù đúng | `ADMIN_*` chỉ được đọc khi database **chưa có** quản trị nền tảng; đổi `.env` sau đó không có tác dụng | Dùng mật khẩu đã đặt lần đầu. Quên mật khẩu: xem [Đặt lại mật khẩu quản trị nền tảng](#đặt-lại-mật-khẩu-quản-trị-nền-tảng) |
| Ổ đĩa đầy | Image build cũ, log | `df -h`; `docker system prune -f` (xoá image/container không dùng, **không** xoá database) |

---

## 11. Vận hành hằng ngày

Các lệnh chạy trong `~/wedding-event/deploy`. Đặt alias cho ngắn:

```bash
echo "alias dc='docker compose -f ~/wedding-event/deploy/docker-compose.prod.yml'" >> ~/.bashrc && source ~/.bashrc
```

| Việc | Lệnh |
|---|---|
| Xem trạng thái | `dc ps` |
| Xem log backend (theo dõi liên tục, `Ctrl+C` để thoát) | `dc logs -f admin-backend` |
| Khởi động lại một service | `dc restart admin-backend` |
| Dừng toàn bộ (database vẫn còn) | `dc down` |
| Bật lại | `dc up -d` |
| Backup thủ công | `dc exec backup sh -c 'pg_dump -Fc -f /backups/manual-$(date +%F-%H%M).dump'` |

> **Không bao giờ** chạy `docker compose down -v` hay `docker volume rm`: chữ `-v` xoá luôn database.

Server khởi động lại (reboot) thì các service tự chạy lại (`restart: unless-stopped`).

### Cập nhật phiên bản mới

```bash
cd ~/wedding-event/deploy
dc exec backup sh -c 'pg_dump -Fc -f /backups/pre-update-$(date +%F-%H%M).dump'   # backup trước
git pull
dc up -d --build
dc ps                     # đợi admin-backend healthy
```

Migration database chạy tự động khi backend khởi động. Sau khi cập nhật, làm nhanh vài dòng ở mục 9.2 (đăng nhập, mở
một hợp đồng, xuất PDF).

### Đặt lại mật khẩu quản trị nền tảng

Khi quên mật khẩu (hoặc tài khoản bị khoá 15 phút do sai nhiều lần):

1. Thêm ba dòng vào `.env` (email là email của tài khoản quản trị nền tảng hiện có):

   ```dotenv
   ADMIN_EMAIL=ban@tenstudio.vn
   ADMIN_PASSWORD=<mật khẩu mới ≥ 8 ký tự, có chữ và số>
   ADMIN_RESET_PASSWORD=true
   ```

2. Khởi động lại backend và xem log:

   ```bash
   dc up -d admin-backend
   dc logs --tail 50 admin-backend | grep -i "platform admin"
   ```

   Thấy `Reset the platform admin password` là xong: mật khẩu đã đổi, tài khoản được mở khoá, mọi phiên đăng nhập cũ
   bị đăng xuất. Email sai (không phải tài khoản quản trị nền tảng) thì backend dừng với lỗi
   `ADMIN_EMAIL is not a platform admin account` và không đổi tài khoản nào.

3. **Xoá cả ba dòng** khỏi `.env` rồi `dc up -d admin-backend`. Nếu để lại, mỗi lần backend khởi động sẽ đặt lại mật khẩu
   về giá trị trong `.env`.
4. Đăng nhập bằng mật khẩu mới, bấm biểu tượng chìa khoá để đổi sang mật khẩu chỉ bạn biết.

Quản lý studio quên mật khẩu thì không cần làm vậy: quản lý khác của studio đặt lại ở *Cài đặt → Tài khoản*, hoặc quản
trị nền tảng tạo thêm quản lý cho studio đó.

### Khôi phục từ backup (khi có sự cố thật)

Thao tác này **thay toàn bộ** database bằng bản backup:

```bash
cd ~/wedding-event/deploy
dc stop admin-backend
dc exec backup sh -c 'dropdb wedding_db && createdb wedding_db && pg_restore -d wedding_db /backups/wedding_db-YYYY-MM-DD.dump'
dc start admin-backend
```

Thay `YYYY-MM-DD` bằng ngày của file trong `backups/` (hoặc tải từ `offsite` về thư mục đó bằng `rclone copy`).
