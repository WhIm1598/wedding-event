# Kết nối Zalo OA để gửi hợp đồng qua Zalo (ZNS)

Nút **Gửi Zalo** trong màn *Xuất hợp đồng* gửi một tin **ZNS** (Zalo Notification Service) tới số Zalo của khách. Tin có link mở trang hợp đồng công khai `/c/{mã}`. Trên trang đó khách xem hợp đồng, tải PDF và bấm "Tôi đã đọc và đồng ý" (FN-ADM-CONTR-01 endpoint 3, FN-PUB-CONTR-01).

Tin được gửi **từ Zalo Official Account (OA) của chính studio**. Mỗi studio phải tự làm các bước dưới đây một lần, rồi nhập kết quả vào **Cài đặt → Zalo OA** trên web admin.

> Giao diện Zalo thay đổi thường xuyên. Tên menu dưới đây đúng vào thời điểm viết; nếu khác, tìm mục có cùng chức năng. Phí ZNS do Zalo tính trên từng tin gửi thành công, trừ vào tài khoản Zalo Cloud Account (ZCA) của studio.

---

## 1. Tạo và xác thực Zalo OA

1. Vào <https://oa.zalo.me/> và tạo Official Account cho studio. Chọn loại **Doanh nghiệp**.
2. Gửi hồ sơ **xác thực OA** (giấy phép kinh doanh, ...). ZNS chỉ dùng được với OA **đã xác thực**.

## 2. Tạo ứng dụng Zalo for Developers và liên kết OA

1. Vào <https://developers.zalo.me/>, đăng nhập bằng tài khoản quản trị OA, rồi **Tạo ứng dụng mới**.
2. Ghi lại **App ID** và **Khóa bí mật của ứng dụng** (App Secret), ở mục *Cài đặt* của ứng dụng.
3. Mục **Official Account → Liên kết OA**: chọn OA của studio.
4. Cấp các quyền gửi tin ZNS / tin OA cho ứng dụng theo hướng dẫn trên trang. Một số quyền phải gửi yêu cầu để Zalo duyệt.

## 3. Nạp tiền Zalo Cloud Account và đăng ký mẫu ZNS

1. Vào **Zalo Cloud Account** (<https://account.zalo.cloud/>), liên kết với OA và nạp tiền.
2. Mục **ZNS → Tạo mẫu**. Loại mẫu: *Giao dịch / Chăm sóc khách hàng*. Mẫu **phải dùng đúng tên tham số** sau, vì backend gửi đúng bốn tham số này:

   | Tham số | Kiểu | Giá trị backend gửi |
   |---|---|---|
   | `customer_name` | Tên khách hàng | Họ tên trên hợp đồng |
   | `contract_number` | Mã | Số HĐ, ví dụ `HD-102` |
   | `total_amount` | Tiền tệ | Tổng giá trị, số nguyên VND, ví dụ `25000000` |
   | `contract_url` | URL | `https://<domain>/c/<mã bí mật>` — đặt vào **nút** "Xem hợp đồng" |

   Ví dụ nội dung mẫu:

   > Xin chào {{customer_name}}, studio gửi bạn hợp đồng dịch vụ cưới số {{contract_number}}, tổng giá trị {{total_amount}}. Bấm "Xem hợp đồng" để xem chi tiết, tải PDF và xác nhận.

   Nút: **Xem hợp đồng** → liên kết `{{contract_url}}`. Nếu Zalo chỉ cho nút trỏ tới domain đã đăng ký, thêm domain của web admin vào mục quản lý domain của ứng dụng/OA.
3. Chờ Zalo **duyệt mẫu**, thường 1–2 ngày làm việc. Ghi lại **Template ID**.

## 4. Lấy Refresh Token của OA

1. Ở trang ứng dụng trên Zalo for Developers, mở **Công cụ → API Explorer**.
2. Chọn loại token **OA Access Token**, chọn OA của studio, bấm **Lấy Access Token**.
3. Sao chép **Refresh Token**. Không cần Access Token: backend tự lấy access token mới (hạn khoảng 25 giờ) mỗi khi cần.

> Refresh token có hạn **3 tháng** và **chỉ dùng được một lần**: mỗi lần làm mới, Zalo trả về một refresh token mới. Backend tự lưu token mới này, nên chỉ cần nhập một lần. Phải làm lại bước 4 khi:
> - studio không gửi tin Zalo nào trong hơn 3 tháng (refresh token hết hạn), hoặc
> - nút Gửi Zalo báo lỗi `ZALO_OA_AUTH_FAILED`.

## 5. Nhập vào web admin

**Cài đặt → Zalo OA** (chỉ Quản lý):

| Ô | Giá trị |
|---|---|
| App ID | Từ bước 2 |
| App Secret | Từ bước 2 (sau khi lưu, ô này trống; để trống khi sửa các ô khác) |
| Refresh Token (OA) | Từ bước 4 |
| Mã mẫu ZNS gửi hợp đồng | Template ID từ bước 3 |

Bấm **Lưu**. Trạng thái chuyển sang **Đã kết nối**.

Server cũng phải biết domain công khai để tạo link. `deploy/docker-compose.prod.yml` tự đặt `PUBLIC_BASE_URL=https://${DOMAIN}`. Khi chạy dev, giá trị mặc định là `http://localhost:5173`.

## 6. Gửi thử

1. Tạo một hợp đồng với **số Zalo của bạn**, rồi mở *Xuất hợp đồng* → **Gửi Zalo**.
2. Kiểm tra trên Zalo:
   - tin đến từ OA của studio;
   - nút "Xem hợp đồng" mở trang hợp đồng;
   - bấm **Tôi đã đọc và đồng ý** thì web admin nhận thông báo "Khách đã đồng ý hợp đồng".

## Lỗi thường gặp

| Mã lỗi trên web | Nguyên nhân | Cách xử lý |
|---|---|---|
| `ZALO_OA_NOT_CONFIGURED` | Chưa nhập đủ 4 ô ở Cài đặt → Zalo OA | Làm bước 5 |
| `ZALO_OA_AUTH_FAILED` | App ID / App Secret sai, hoặc refresh token hết hạn hay đã bị dùng | Làm lại bước 4, nhập refresh token mới |
| `ZALO_SEND_FAILED (mã …)` | Zalo từ chối tin: số không dùng Zalo, mẫu chưa duyệt hoặc sai tên tham số, hết tiền ZCA, ... | Tra mã trong tài liệu "Mã lỗi ZNS" của Zalo. Trong lúc chờ, dùng nút **Nhắn Zalo** để gửi PDF thủ công |
| `ZALO_NOT_AVAILABLE` | Hợp đồng ghi khách không có Zalo | Nhập số Zalo khác khi được hỏi |

**Bảo mật:**
- App Secret và refresh token chỉ lưu trong database, không API nào trả về.
- Link hợp đồng chỉ lưu dạng băm SHA-256 và hết hạn sau 30 ngày.
- Gửi lại Zalo tạo link mới; link cũ ngừng hoạt động.
- Quản lý thu hồi link bằng nút **Thu hồi link**.
