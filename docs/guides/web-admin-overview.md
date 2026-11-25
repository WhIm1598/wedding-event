# Web admin hoạt động thế nào

Tài liệu cho chủ sản phẩm, người hỗ trợ studio và đối tác. Mô tả web admin **đang làm được gì** (theo code và spec
v1.2.0), từ phía người cung cấp sản phẩm tới công việc hằng ngày trong một studio, và khi có nhiều studio cùng dùng.

Hướng dẫn cài đặt server: [docs/setup/deploy-linux.md](../setup/deploy-linux.md). Đặc tả chi tiết:
[docs/features/detailed_functional_specification.md](../features/detailed_functional_specification.md).

---

## 1. Ba vai trò

```
Quản trị nền tảng (người cung cấp sản phẩm)  →  màn /platform
 ├── Studio A ── Quản lý A ── Nhân viên A1, A2…   → dữ liệu riêng của A
 ├── Studio B ── Quản lý B ── Nhân viên B1…       → dữ liệu riêng của B
 └── …
Ngoài web admin:
 - Khách cưới mở link hợp đồng từ Zalo (/c/…), không cần đăng nhập
 - App mobile WedPlanner hiện gói dịch vụ của mọi studio đang hoạt động
```

| Vai trò | Là ai | Thấy gì |
|---|---|---|
| **Quản trị nền tảng** | Người bán/cho thuê web admin | Chỉ danh sách studio, hạn thuê bao, số quản lý. **Không** xem được dữ liệu bên trong studio |
| **Quản lý** (studio) | Chủ hoặc quản lý studio | Toàn bộ dữ liệu và cài đặt của studio mình |
| **Nhân viên** (studio) | Thợ chụp, makeup, sale… | Phần việc hằng ngày, không thấy tiền và cài đặt |

---

## 2. Phía người cung cấp: Quản trị nền tảng

Đăng nhập xong chỉ có một màn **Quản trị nền tảng**.

| Việc | Cách làm | Kết quả |
|---|---|---|
| Bán cho studio mới | **Thêm studio**: tên, hạn thuê bao (trống = không giới hạn), email và họ tên quản lý đầu tiên | Tạo studio và tài khoản quản lý. Mật khẩu tạm hiện **một lần**, gửi riêng cho chủ studio |
| Thêm quản lý | Nút biểu tượng người trên dòng studio | Thêm tài khoản quản lý (mật khẩu tạm). Dùng khi chủ studio quên mật khẩu và studio không còn quản lý nào khác |
| Gia hạn | **Gia hạn** → chọn ngày hết hạn mới | Studio dùng tiếp tới ngày đó |
| Hết hạn | Tự động khi qua ngày hết hạn | Badge "Hết hạn". Mọi người của studio bị chặn ngay, kể cả đang dùng dở |
| Tạm ngưng / Kích hoạt | Nút trên dòng studio | Chặn hoặc mở lại ngay, không phụ thuộc hạn |
| Đổi mật khẩu của mình | Biểu tượng chìa khoá | Quên mật khẩu: đặt lại trên server ([hướng dẫn](../setup/deploy-linux.md#đặt-lại-mật-khẩu-quản-trị-nền-tảng)) |

- Studio bị chặn **không mất dữ liệu**: gia hạn hoặc kích hoạt lại là dùng tiếp. Gói dịch vụ của studio bị ẩn khỏi app
  mobile trong thời gian đó.
- **Chưa có:** thu tiền thuê bao tự động (thu ngoài hệ thống, chỉ ghi ngày hết hạn), xoá studio, xem dữ liệu bên trong
  studio (cố ý, để dữ liệu khách của studio là riêng tư).

---

## 3. Bên trong một studio

### 3.1. Thiết lập lần đầu (quản lý studio)

1. Đăng nhập bằng mật khẩu tạm → **bắt buộc đổi mật khẩu** trước khi làm gì khác.
2. **Cài đặt → Thông tin studio**: tên, địa chỉ, mã số thuế, người đại diện, tài khoản ngân hàng. In lên hợp đồng và hiện
   trên app mobile.
3. **Gói dịch vụ**: tạo các gói (tên, loại, giá, nội dung). Chưa có gói đang bán thì chưa tạo được hợp đồng.
4. **Nhân sự**: thêm thợ chụp, makeup… (mã NV-001…).
5. **Cài đặt → Tài khoản**: tạo đăng nhập cho nhân viên, gắn với hồ sơ nhân sự. Mỗi người nhận mật khẩu tạm.
6. *(Tuỳ chọn)* **Cài đặt → Zalo OA**: kết nối Zalo Official Account riêng của studio để gửi hợp đồng qua Zalo
   ([hướng dẫn](../setup/zalo-oa-zns.md)).

### 3.2. Luồng một khách cưới

```
Khách hỏi → CRM: Mới hỏi → Đang tư vấn → Chờ cọc
          → Tạo hợp đồng (HD-001) ── có tiền cọc? ── có: "Mới cọc", khách sang "Đang thực hiện"
          │                                      └─ không: "Nháp"
          → Gửi hợp đồng: Gửi Zalo (link) / Xuất PDF / Nhắn Zalo thủ công
          → Khách mở link: xem, tải PDF, bấm "Tôi đã đọc và đồng ý" → studio nhận thông báo
          → Lịch hẹn: thử váy, chụp, cưới… phân nhân sự (trùng giờ bị chặn)
          → Thuê đồ: khoá váy/vest theo ngày + ngày giặt hấp
          → Buổi chụp bắt đầu → hợp đồng "Đang thực hiện"
          → Thu các đợt còn lại ở Kế toán (trừ dần công nợ)
          → Xác nhận bàn giao → đủ tiền: hợp đồng và khách "Đã hoàn thành"
```

### 3.3. Các màn hình

| Màn | Dùng để | Quy tắc quan trọng |
|---|---|---|
| **Tổng quan** | Doanh thu tháng, hợp đồng đang chạy, khách đang chờ, tỉ lệ cho thuê trang phục, biểu đồ 12 tháng, lịch hôm nay | Nhân viên chỉ thấy lịch hôm nay của mình |
| **Lịch trình** | Lịch ngày/tuần/tháng, tạo lịch hẹn, phân nhân sự | Một nhân viên không có hai lịch cùng ngày cùng giờ. Trạng thái: Chờ xác nhận → Đã xác nhận → Đang thực hiện → Hoàn thành (hoặc Đã hủy) |
| **Khách hàng (CRM)** | Kanban giai đoạn khách, danh sách hợp đồng | Chưa có hợp đồng thì không kéo khách sang "Đang thực hiện"/"Đã hoàn thành". Giai đoạn tự đổi theo hợp đồng |
| **Hợp đồng** | Tạo (gợi ý cọc 30%), xem, xuất PDF, gửi Zalo, thu hồi link, bàn giao | Cọc ≤ tổng giá trị. Link Zalo hết hạn sau 30 ngày; gửi lại thì link cũ mất hiệu lực |
| **Kho & Thiết bị** | Váy, vest, máy móc; đặt thuê theo ngày | Mỗi món có số ngày bảo dưỡng sau khi trả; ngày thuê trùng hoặc rơi vào ngày bảo dưỡng bị từ chối |
| **Gói dịch vụ** | Tạo gói, bật/tắt đang bán | Gói đang bán hiện trên app mobile |
| **Nhân sự** | Danh sách, số lịch trong tháng, giao việc | Chỉ quản lý |
| **Kế toán** | Sổ thu chi (TRX-001…), tổng thu/chi/lãi tháng | Khoản thu gắn hợp đồng trừ công nợ; không thu quá số còn nợ; hợp đồng đã hoàn thành không thu thêm |
| **Thông báo, tìm kiếm** | Chuông (hợp đồng mới, đã thu tiền, khách đồng ý…), Ctrl+K tìm khách/hợp đồng theo tên, số điện thoại, mã | |

### 3.4. Quản lý và nhân viên

| | Quản lý | Nhân viên |
|---|---|---|
| Tổng quan (số liệu tiền) | ✅ | Chỉ lịch hôm nay |
| Lịch trình | Tất cả | Chỉ lịch của mình, chỉ cập nhật lịch của mình |
| CRM; xem, xuất, gửi Zalo hợp đồng | ✅ | ✅ |
| Tạo hợp đồng, bàn giao, thu hồi link | ✅ | — |
| Kho: xem, đặt thuê | ✅ (+ thêm đồ) | ✅ |
| Gói dịch vụ, Nhân sự, Kế toán, Cài đặt | ✅ | — |

- Quản lý tạo, khoá tài khoản và đặt lại mật khẩu cho người khác. Không tự khoá hay tự hạ quyền mình; studio luôn còn ít
  nhất một quản lý.
- Sai mật khẩu 5 lần → khoá 15 phút. Khoá tài khoản hoặc đổi quyền có hiệu lực **ngay**, không chờ đăng nhập lại.

---

## 4. Khi có nhiều studio

Tất cả dùng chung một web và một database, nhưng:

- **Dữ liệu tách hoàn toàn:** khách, hợp đồng, lịch, kho, nhân sự, sổ thu chi, thông báo, gói, cài đặt. Studio B không
  xem, sửa hay tìm thấy được gì của studio A (có test tự động kiểm tra).
- **Mã chứng từ riêng:** mỗi studio bắt đầu từ HD-001, NV-001, TRX-001.
- **Cài đặt riêng:** thông tin in hợp đồng, tài khoản ngân hàng, Zalo OA. Tin Zalo gửi từ OA của chính studio.
- **Email duy nhất toàn hệ thống:** một email chỉ thuộc một tài khoản. Một người làm cho hai studio cần hai email.
- **App mobile:** cô dâu chú rể thấy gói của **mọi** studio đang hoạt động, kèm tên, địa chỉ, Zalo của studio đó. Studio
  bị tạm ngưng hoặc hết hạn thì gói tự ẩn.
- **Với người cung cấp:** mỗi khách mới chỉ là một lần "Thêm studio", không cài thêm server.

Mọi màn hình và quy tắc ở mục 3 chạy y hệt trong từng studio; người dùng đăng nhập là thấy đúng studio của mình.

---

## 5. Chưa làm

| Issue | Nội dung |
|---|---|
| #49 | Nhật ký thao tác (ai sửa gì, lúc nào) |
| #50 | Mã VietQR trên trang hợp đồng cho khách quét chuyển khoản |
| #51 | Ghi "Đã gửi Zalo" vào lịch sử hợp đồng |
| #52 | Lịch thanh toán nhiều đợt thực tế trên PDF và trang khách (hiện in theo mẫu 30/50/20) |
| #53 | Thử thật gửi tin Zalo và luồng mật khẩu tạm |
| #56 | Đưa backend mobile lên cùng server |

Chuẩn bị để vận hành (tài khoản, lưu ảnh, tốc độ): [operations-readiness.md](operations-readiness.md).
