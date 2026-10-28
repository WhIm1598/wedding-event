# Deploy bản demo miễn phí: Oracle Cloud + DuckDNS + Let's Encrypt

Hướng dẫn chạy web admin ở mức **demo** (vài studio dùng thử) với chi phí **0đ**:

| Thành phần | Dịch vụ miễn phí |
|---|---|
| Server | Oracle Cloud **Always Free**, máy ARM Ampere A1 |
| Tên miền | **DuckDNS**: `tenban.duckdns.org` |
| HTTPS | **Let's Encrypt**, tự gia hạn |
| Cài đặt + chạy | Script [`deploy/install.sh`](../../deploy/install.sh) |

Làm lần lượt từ bước 1. Tổng thời gian khoảng 1–2 giờ, phần lâu nhất là chờ Oracle duyệt tài khoản.

> **Giao diện Oracle và DuckDNS thay đổi thường xuyên.** Tên menu dưới đây đúng tại thời điểm viết (10/2026); nếu khác,
> tìm mục có cùng chức năng. Hạn mức miễn phí cũng có thể đổi: các nguồn gần đây cho biết từ 6/2026 Oracle giảm Ampere A1
> miễn phí xuống khoảng **2 OCPU / 12 GB RAM** (trước là 4/24). Kiểm tra trang
> [Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)
> trước khi tạo máy.

Khi có studio trả tiền, chuyển sang tên miền thật: [mục 9](#9-chuyển-sang-tên-miền-thật). Bản production đầy đủ:
[deploy-linux.md](deploy-linux.md).

---

## 0. Cần chuẩn bị

- **Email** (Gmail của dự án, xem [operations-readiness.md](../guides/operations-readiness.md#1-tài-khoản-cần-chuẩn-bị)).
- **Thẻ Visa/Mastercard** để Oracle xác minh danh tính. Oracle tạm giữ một khoản nhỏ rồi hoàn lại, không trừ tiền khi
  dùng trong hạn mức miễn phí. Một số thẻ ghi nợ nội địa bị từ chối; thẻ tín dụng hoặc thẻ ghi nợ quốc tế thường được.
- **Số điện thoại** nhận mã xác minh.
- **Máy tính** có cửa sổ dòng lệnh: Windows 10+ dùng *PowerShell* (có sẵn `ssh`), macOS/Linux dùng *Terminal*.

---

## 1. Đăng ký Oracle Cloud

1. Vào <https://www.oracle.com/cloud/free/> → **Start for free**.
2. Điền email, quốc gia (**Vietnam**), xác minh email.
3. Điền thông tin:
   - **Account type:** Individual (cá nhân).
   - **Cloud Account Name:** tên ngắn, viết liền, ví dụ `wedplannerdemo`. Ghi lại, cần khi đăng nhập.
   - **Home Region:** chọn **Singapore** (`ap-singapore-1`) hoặc **Japan East (Tokyo)**.
     > ⚠️ **Không đổi được Home Region sau này**, và máy miễn phí chỉ tạo được ở Home Region. Singapore gần Việt Nam
     > nhất nhưng hay hết máy ARM; nếu sau nhiều lần vẫn không tạo được máy (bước 5), chỉ còn cách tạo tài khoản mới.
4. Xác minh số điện thoại, nhập thẻ, đồng ý điều khoản → **Start my free trial**.
5. Đợi email *"Your Oracle Cloud account is fully provisioned"* (vài phút tới vài giờ). Đăng nhập tại
   <https://cloud.oracle.com> bằng Cloud Account Name + email + mật khẩu.
6. **Bật xác minh 2 bước** khi được hỏi (ứng dụng *Oracle Mobile Authenticator* hoặc Google Authenticator).

### 1.1. (Khuyên làm) Nâng lên Pay As You Go

Oracle có thể **thu hồi máy Always Free chạy quá nhàn** (CPU thấp kéo dài), mà web demo thì rất nhàn. Tài khoản
*Pay As You Go* không bị thu hồi kiểu này và **vẫn miễn phí** nếu chỉ dùng tài nguyên có nhãn *Always Free-eligible*.

1. Menu ☰ → **Billing & Cost Management** → **Upgrade and Manage Payment** → **Upgrade to Pay As You Go**.
2. Đặt cảnh báo chi phí cho yên tâm: **Billing & Cost Management → Budgets → Create Budget**, số tiền `1` USD, gửi
   cảnh báo về email. Nếu lỡ tạo tài nguyên tính phí, bạn biết ngay.

> Đọc lại chính sách thu hồi máy nhàn rỗi trong trang Always Free của Oracle trước khi quyết định.

---

## 2. Tạo khoá SSH (trên máy của bạn)

Khoá SSH thay cho mật khẩu khi đăng nhập server. Trên **PowerShell** (Windows) hoặc **Terminal** (macOS/Linux):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/oracle_demo -C "wedding-demo"
```

Hỏi *passphrase*: đặt một mật khẩu cho khoá (khuyên làm) hoặc Enter để bỏ trống. Kết quả là hai file:

| File | Là gì |
|---|---|
| `~/.ssh/oracle_demo` | **Khoá bí mật**: không gửi cho ai, sao lưu vào trình quản lý mật khẩu |
| `~/.ssh/oracle_demo.pub` | Khoá công khai: dán vào Oracle ở bước 5 |

Trên Windows, `~` là `C:\Users\<tên bạn>`.

---

## 3. Tạo mạng (VCN)

1. Menu ☰ → **Networking** → **Virtual Cloud Networks**.
2. Góc trên bên trái, **Compartment** để `(root)`.
3. **Start VCN Wizard** → **Create VCN with Internet Connectivity** → **Start VCN Wizard**.
4. **VCN Name:** `wedding-vcn`, các ô khác giữ mặc định → **Next** → **Create**.

## 4. Mở cổng 80 và 443 ở Oracle

Oracle chặn mọi cổng trừ SSH (22) ở mức mạng. Let's Encrypt cần cổng 80, web cần cổng 443.

1. Trong `wedding-vcn` → tab **Subnets** (hoặc **Resources → Subnets**) → **public subnet-wedding-vcn**.
2. Tab **Security** (hoặc **Security Lists**) → **Default Security List for wedding-vcn**.
3. **Add Ingress Rules**, thêm **hai** luật:

   | Ô | Luật 1 | Luật 2 |
   |---|---|---|
   | Source Type | CIDR | CIDR |
   | Source CIDR | `0.0.0.0/0` | `0.0.0.0/0` |
   | IP Protocol | TCP | TCP |
   | Destination Port Range | `80` | `443` |
   | Description | HTTP (Let's Encrypt) | HTTPS |

4. **Add Ingress Rules**. Danh sách phải có luật cổng 22 (có sẵn), 80 và 443.

> Lớp tường lửa thứ hai nằm **trong** máy (iptables). `install.sh` tự mở, xem bước 7.

---

## 5. Tạo máy ảo miễn phí

1. Menu ☰ → **Compute** → **Instances** → **Create instance**.
2. **Name:** `wedding-demo`.
3. **Placement:** giữ mặc định (nếu báo hết máy, thử *Availability Domain* khác nếu có).
4. **Image and shape** → **Edit**:
   - **Change image** → **Ubuntu** → **Canonical Ubuntu 24.04** (bản thường, **không** chọn *Minimal*) → **Select image**.
   - **Change shape** → **Ampere** → **VM.Standard.A1.Flex** → **Number of OCPUs: 2**, **Amount of memory (GB): 12**
     (hoặc mức tối đa mà trang Always Free hiện cho phép). Phải thấy nhãn **Always Free-eligible** → **Select shape**.
5. **Networking** → **Edit**:
   - **Select existing virtual cloud network:** `wedding-vcn`.
   - **Subnet:** `public subnet-wedding-vcn`.
   - **Automatically assign public IPv4 address:** bật.
6. **Add SSH keys** → **Upload public key files (.pub)** → chọn `~/.ssh/oracle_demo.pub`
   (hoặc **Paste public keys** rồi dán nội dung file `.pub`).
7. **Boot volume** → **Specify a custom boot volume size:** `50` GB (miễn phí tổng 200 GB).
8. **Create**. Đợi trạng thái chuyển **Running** (1–3 phút).
9. Ở trang máy, ghi lại **Public IP address**, ví dụ `140.245.10.20`.

**Báo `Out of capacity for shape VM.Standard.A1.Flex`?** Vùng đang hết máy ARM miễn phí. Cách xử lý:

- Thử lại sau vài giờ, nhất là sáng sớm hoặc khuya (giờ Việt Nam).
- Tạm chọn **1 OCPU / 6 GB**, sau này **Edit → Shape** để tăng.
- Sau khi nâng Pay As You Go (mục 1.1), tỉ lệ tạo được máy thường cao hơn.

> IP công khai gắn với máy cho tới khi **xoá** máy. Nếu xoá rồi tạo lại, IP đổi: cập nhật lại DuckDNS (bước 6).

---

## 6. Lấy tên miền miễn phí trên DuckDNS

1. Vào <https://www.duckdns.org> → đăng nhập bằng **GitHub** hoặc **Google** (dùng tài khoản của dự án).
2. Ô **sub domain**: gõ tên, ví dụ `wedplanner-demo` → **add domain**. Tên đầy đủ là `wedplanner-demo.duckdns.org`.
   Mỗi tài khoản được tối đa 5 tên.
3. Ở dòng tên vừa tạo, ô **current ip**: gõ **Public IP của Oracle** (bước 5.9) → **update ip**.
4. Kiểm tra từ máy của bạn (vài phút sau):

   ```bash
   nslookup wedplanner-demo.duckdns.org
   ```

   Phải ra đúng IP Oracle.

> Trang DuckDNS có hiện **token**. Token này cho phép đổi IP của tên miền: giữ bí mật như mật khẩu. Máy Oracle có IP cố
> định nên không cần cài chương trình tự cập nhật IP.

---

## 7. Đăng nhập server và chạy `install.sh`

### 7.1. Đăng nhập

```bash
ssh -i ~/.ssh/oracle_demo ubuntu@140.245.10.20
```

Lần đầu hỏi `Are you sure you want to continue connecting?` → `yes`. Người dùng mặc định của Ubuntu trên Oracle là
`ubuntu`, đã có quyền `sudo`.

### 7.2. Chạy script

```bash
sudo apt update && sudo apt install -y git
git clone https://github.com/WhIm1598/wedding-event.git && cd wedding-event
sudo deploy/install.sh wedplanner-demo.duckdns.org ban@gmail.com
```

- Tham số 1: tên miền DuckDNS. Tham số 2: email của bạn, dùng làm **tài khoản quản trị nền tảng** và để Let's Encrypt
  báo khi chứng chỉ sắp hết hạn.
- Lần đầu mất **10–20 phút** (máy ARM build Java và React chậm hơn máy Intel một chút).

Script làm lần lượt:

| Bước | Việc |
|---|---|
| 1 | Cập nhật hệ thống, cài Docker |
| 2 | **Tường lửa trong máy:** phát hiện image Oracle (có `/etc/iptables/rules.v4`), thêm luật mở cổng 80, 443 **phía trên** luật REJECT, cả trong file (giữ sau khi khởi động lại) lẫn đang chạy. **Không** bật `ufw`, theo khuyến cáo của Oracle |
| 3 | Bỏ qua swap (máy 12 GB RAM không cần) |
| 4 | Tạo `deploy/.env` với mật khẩu ngẫu nhiên |
| 5 | Kiểm tra tên miền DuckDNS trỏ đúng IP của máy |
| 6 | Lấy chứng chỉ HTTPS từ Let's Encrypt (mục 8) |
| 7 | Build và chạy, đợi backend sẵn sàng |
| 8 | Kiểm thử HTTPS, API; in **mật khẩu quản trị nền tảng (chỉ hiện một lần)** |

Chép mật khẩu được in ra vào trình quản lý mật khẩu. Script dừng giữa chừng thì đọc thông báo, sửa theo
[mục 10](#10-xử-lý-sự-cố), rồi **chạy lại đúng lệnh đó**: script giữ những gì đã xong.

### 7.3. Sau khi script xong

Làm tiếp theo [deploy-linux.md](deploy-linux.md):

- **Mục 8:** đăng nhập `https://wedplanner-demo.duckdns.org`, đổi mật khẩu, xoá `ADMIN_EMAIL`/`ADMIN_PASSWORD` khỏi
  `deploy/.env`, tạo studio demo.
- **Mục 9.2–9.4:** kiểm thử chức năng, thử khôi phục backup, chép backup ra Google Drive.
- **Mục 11:** lệnh vận hành hằng ngày, cập nhật phiên bản.

Thêm giám sát miễn phí: tạo monitor trên [UptimeRobot](https://uptimerobot.com) cho `https://wedplanner-demo.duckdns.org/`.

---

## 8. Chứng chỉ HTTPS từ Let's Encrypt

`install.sh` đã làm việc này. Mục này giải thích để bạn biết nó chạy thế nào và tự làm khi cần.

### 8.1. Cách cấp chứng chỉ

1. Container `certbot` mở cổng 80 tạm thời (*standalone*), xin Let's Encrypt cấp chứng chỉ cho tên miền.
2. Let's Encrypt gọi `http://<tên-miền>/.well-known/acme-challenge/...` để kiểm tra bạn đúng là chủ tên miền
   (*HTTP-01 challenge*). Vì vậy cần: DNS trỏ đúng IP (bước 6) và cổng 80 mở ở **cả hai lớp** (bước 4 và 7).
3. Chứng chỉ lưu ở `deploy/certbot/conf/live/<tên-miền>/`, hạn **90 ngày**.

Lệnh tương đương (chạy trong `~/wedding-event/deploy`, khi gateway **chưa** chạy):

```bash
source .env
sudo docker compose -f docker-compose.prod.yml run --rm -p 80:80 --entrypoint certbot certbot \
  certonly --standalone -d "$DOMAIN" --email ban@gmail.com --agree-tos --no-eff-email --non-interactive
```

### 8.2. Gia hạn tự động

- Service `certbot` kiểm tra mỗi 12 giờ, tự gia hạn khi còn dưới 30 ngày (qua đường `/.well-known/` mà gateway
  phục vụ, không cần dừng web).
- `gateway` nạp lại chứng chỉ mỗi 6 giờ.
- Let's Encrypt gửi email cảnh báo nếu chứng chỉ sắp hết hạn mà chưa gia hạn được.

Thử gia hạn (không đổi chứng chỉ thật):

```bash
cd ~/wedding-event/deploy
sudo docker compose -f docker-compose.prod.yml exec certbot certbot renew --dry-run --webroot -w /var/www/certbot
```

Xem hạn hiện tại:

```bash
echo | openssl s_client -connect wedplanner-demo.duckdns.org:443 -servername wedplanner-demo.duckdns.org 2>/dev/null \
  | openssl x509 -noout -dates
```

### 8.3. Giới hạn của Let's Encrypt

- Tối đa **5 chứng chỉ giống hệt nhau mỗi tuần** cho cùng một tên miền. Đừng xoá thư mục `deploy/certbot` rồi xin lại
  liên tục khi đang sửa lỗi; lấy được một lần là đủ, script không xin lại nếu đã có.
- Muốn thử nghiệm nhiều lần: thêm `--staging` vào lệnh 8.1 (chứng chỉ thử, trình duyệt báo không an toàn), xong thì xoá
  `deploy/certbot/conf/live/<tên-miền>` và xin lại bản thật.

---

## 9. Chuyển sang tên miền thật

Khi có studio trả tiền, mua tên miền (ví dụ `wedplanner.vn`) và chuyển:

1. Tạo bản ghi **A** của tên miền mới trỏ về IP Oracle (xem [deploy-linux.md bước 1](deploy-linux.md#1-trỏ-tên-miền-về-vps)).
2. Trên server, sửa `DOMAIN` trong `deploy/.env` thành tên miền mới:

   ```bash
   cd ~/wedding-event && nano deploy/.env
   ```

3. Chạy lại script với tên miền mới (nó thấy chưa có chứng chỉ cho tên miền này nên tự xin, rồi khởi động lại gateway):

   ```bash
   sudo deploy/install.sh wedplanner.vn ban@wedplanner.vn
   ```

4. Link hợp đồng đã gửi khách bằng tên miền DuckDNS sẽ **không mở được nữa**: gửi lại link cho hợp đồng còn đang chạy.
5. Giữ tên DuckDNS thêm một thời gian hoặc xoá trên trang DuckDNS.

Dữ liệu (studio, hợp đồng, tài khoản) giữ nguyên, chỉ địa chỉ web đổi.

---

## 10. Xử lý sự cố

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Tạo máy báo `Out of capacity` | Vùng hết máy ARM miễn phí | Mục 5: thử lại giờ khác, giảm 1 OCPU / 6 GB, nâng Pay As You Go |
| `ssh` báo `Permission denied (publickey)` | Sai khoá hoặc sai tên người dùng | Dùng `-i ~/.ssh/oracle_demo` và người dùng `ubuntu`; kiểm tra đã dán đúng file `.pub` khi tạo máy |
| `ssh` treo, không phản hồi | Máy chưa chạy xong hoặc luật cổng 22 bị xoá | Đợi trạng thái *Running*; kiểm tra Security List còn luật cổng 22 |
| Script báo `... đang trỏ về '...', server là ...` | DuckDNS chưa cập nhật IP hoặc DNS chưa lan | Bước 6.3 nhập lại IP, đợi vài phút, chạy lại script |
| Lấy chứng chỉ báo `Timeout during connect` | Cổng 80 bị chặn | Kiểm tra luật cổng 80 ở Security List (bước 4). Trong máy: `sudo iptables -L INPUT -n --line-numbers`, luật `dpt:80` phải nằm **trên** dòng `REJECT` |
| Lấy chứng chỉ báo `too many certificates already issued` | Vượt giới hạn 5 chứng chỉ/tuần (mục 8.3) | Đợi đủ 7 ngày kể từ lần xin đầu tiên |
| Build dừng với `exit code 137` / `Killed` | Hết RAM (máy 1 OCPU / 6 GB vẫn đủ, nhỏ hơn thì không) | Tăng RAM của máy (Edit → Shape) |
| Web mở được bằng IP nhưng không bằng tên miền | DNS sai | `nslookup <tên-miền>` phải ra IP Oracle |
| Máy bị dừng hoặc xoá sau vài tuần không dùng | Oracle thu hồi máy Always Free nhàn rỗi | Mục 1.1: nâng Pay As You Go. Khôi phục: tạo máy mới, chạy lại script, khôi phục database từ backup ngoài server ([deploy-linux.md mục 11](deploy-linux.md#khôi-phục-từ-backup-khi-có-sự-cố-thật)) |
| Lỗi khác | | [deploy-linux.md mục 10](deploy-linux.md#10-xử-lý-sự-cố) |

---

## 11. Giới hạn của bản demo

- **Không có cam kết uptime**: Oracle có thể dừng máy miễn phí, DuckDNS có thể gián đoạn. Luôn chép backup ra ngoài
  server ([deploy-linux.md mục 9.4](deploy-linux.md#94-chép-backup-ra-ngoài-server)).
- **Tên miền `*.duckdns.org`** trông không chuyên nghiệp khi gửi link hợp đồng qua Zalo cho khách thật.
- **Hạn mức Always Free có thể bị cắt tiếp** như lần 6/2026. Hệ thống cần tối thiểu khoảng 4 GB RAM, nên vẫn chạy được
  trên 1 OCPU / 6 GB nếu bị giảm.
- Dùng cho demo và studio dùng thử. Khi có studio trả tiền: tên miền thật (mục 9), cân nhắc VPS trả phí
  ([operations-readiness.md](../guides/operations-readiness.md)).
