# Tạo studio đầy đủ: nhiều quản lý, logo, thông tin hợp đồng

Ngày 2026-11-25. Phần 1 trong 4 góp ý sau khi test bản demo (2: mẫu hợp đồng docx, 3–4: giao việc và lịch trình,
làm sau). Sửa FN-PLT-01 và FN-ADM-SET-01.

## Vì sao

Chủ sản phẩm thử tạo studio từ tài khoản quản trị nền tảng: form chỉ có tên + **một** email quản lý, trông như một
studio chỉ có một quản lý, và studio không có nhận diện riêng. Sản phẩm bán cho nhiều studio, mỗi studio cần thấy
"của mình" (tên, logo trên web và trên hợp đồng gửi khách) và có thể có nhiều quản lý ngay từ đầu.

Hiện trạng: một studio **đã** có thể có nhiều quản lý (FN-PLT-03, và quản lý tự tạo trong *Cài đặt → Tài khoản*), chỉ
form tạo là một email. Chưa có logo. Thông tin hợp đồng của studio mới là "Chưa cập nhật".

## Mục tiêu

- Form *Thêm studio*: tên, logo, hạn thuê bao, thông tin hợp đồng (tuỳ chọn), **danh sách quản lý** (email + tên, 1–10).
  Tạo xong hiện bảng email / mật khẩu tạm của từng quản lý, chỉ một lần.
- Logo hiện ở đầu menu trái, trên PDF hợp đồng và trang hợp đồng công khai của khách.
- Quản lý studio sửa tên và logo trong *Cài đặt*; quản trị nền tảng sửa tên, logo, hạn, trạng thái.
- Danh sách studio hiện logo và số quản lý.

## Không làm

- Vẫn **hai quyền**: Quản lý (`ROLE_ADMIN`), Nhân viên (`ROLE_STAFF`). Không thêm vai trò, không phân quyền chi tiết.
- Nhân viên **không** nhập ở form tạo studio: quản lý studio tự thêm trong *Cài đặt → Tài khoản* (đã có), vì tài
  khoản nhân viên cần hồ sơ nhân sự và danh sách nhân sự thay đổi thường xuyên.
- Không dùng object storage (R2) cho logo; không làm upload ảnh chung.

## Phải tránh

- Tạo dở: một email lỗi thì không được còn studio thiếu quản lý. Cả lệnh tạo là một transaction.
- Logo nặng làm chậm màn hình (mục tiêu < 3 giây, `docs/guides/operations-readiness.md`).
- Lộ studio này cho studio khác: logo/tên chỉ đọc theo studio của người gọi (ADR-0001), trừ trang hợp đồng công khai.

## Các cách đã cân nhắc (lưu logo)

| Cách | Được | Mất |
|---|---|---|
| **Database (đã chọn)** — ảnh thu nhỏ trên trình duyệt, lưu cột `bytea` của `studio_settings` | Không cần dịch vụ hay tài khoản mới; backup `pg_dump` hằng ngày có luôn logo; đi chung giao dịch tạo studio | Chỉ hợp ảnh nhỏ, ít; ảnh gallery sau này không làm kiểu này |
| Cloudflare R2 | Đúng hướng lâu dài cho ảnh nhiều | Cần tài khoản + thẻ quốc tế, thêm thư viện S3, cấu hình mới — quá nặng cho vài logo |
| Ổ đĩa container (volume) | Đơn giản | Phải backup riêng, dễ mất khi dựng lại máy |

Cách nhập người: chủ sản phẩm chọn **B** (chỉ quản lý ở form tạo, nhân viên do studio tự thêm) thay vì A (nhập hết ở form).

## Thiết kế

### Dữ liệu

Migration mới (V19 hoặc số tiếp theo): `studio_settings` thêm `logo bytea null`, `logo_content_type varchar(20) null`.

### Logo đi trong JSON dưới dạng data URL

Trường `logo`: `"data:image/png|jpeg|webp;base64,..."` hoặc `null`. Không cần endpoint multipart hay endpoint GET ảnh
riêng: `<img src>` dùng thẳng (CSP `img-src` đã cho `data:`), renderer PDF nhúng được.

- Trình duyệt thu nhỏ trước khi gửi: cạnh dài ≤ 256 px, WebP (JPEG nếu trình duyệt không xuất WebP), dưới 200 KB.
- Backend kiểm tra: đúng tiền tố một trong ba loại, base64 hợp lệ, giải mã ≤ 200 KB; sai → `400 VALIDATION_ERROR`
  lỗi trường `logo`.
- Gửi `null` → xoá logo; không gửi trường → giữ nguyên (chỉ ở endpoint sửa).

### API

**FN-PLT-01 (sửa)** `POST /api/v1/platform/studios`

```json
{ "name": "Lumière Studios", "subscriptionExpiresAt": "2027-11-30", "logo": "data:image/webp;base64,...",
  "address": null, "taxCode": null, "legalRepresentative": null, "bankInfo": null,
  "admins": [ { "email": "a@lumiere.vn", "name": "Anh A" }, { "email": "b@lumiere.vn", "name": "Chị B" } ] }
```

- `admins` 1–10 dòng; email trùng nhau trong danh sách (không phân biệt hoa thường) → `400 VALIDATION_ERROR`;
  email đã có tài khoản → `409 EMAIL_EXISTS` và không tạo gì.
- Các trường thông tin hợp đồng bỏ trống → "Chưa cập nhật" như hiện nay; giới hạn độ dài như FN-ADM-SET-01.
- Trả `201 { studio, admins: [ { user, temporaryPassword } ] }` (thay cho `admin` đơn).

**FN-PLT-01 (sửa)** `GET /api/v1/platform/studios`: mỗi phần tử thêm `logo`.

**FN-PLT-02 (sửa)** `PATCH /api/v1/platform/studios/{id}`: nhận thêm `name`, `logo` (tuỳ chọn).

**FN-PLT-03**: giữ nguyên.

**FN-ADM-SET-01 (sửa)**: dữ liệu cài đặt studio thêm `logo`; quản lý sửa được `name`, `logo`.

**Menu trái**: tên + logo studio của người đang đăng nhập (cả nhân viên). Lấy từ `auth/me` thêm `studio { name, logo }`
nếu nhân viên không đọc được cài đặt — để builder chọn chỗ ít đổi nhất.

**Hợp đồng**: PDF (`ContractDocumentRenderer`) và trang công khai FN-PUB-CONTR-01 hiện logo cạnh tên Bên B nếu có.

### Web

- *Quản trị nền tảng → Thêm studio*: form theo phác thảo dưới; danh sách quản lý thêm/xoá dòng; sau khi tạo hiện bảng
  mật khẩu tạm (có nút sao chép), cảnh báo chỉ hiện một lần.
- Danh sách studio: cột logo; sửa studio thêm tên và logo.
- *Cài đặt*: ô logo (chọn ảnh, xem trước, xoá).
- Mock mode (`npm run dev`) chạy được hết, gồm logo.

```
┌ Thêm studio ─────────────────────────────────┐
│ Tên studio*        [Lumière Studios        ] │
│ Biểu tượng         [⬆ Chọn ảnh]  (xem trước) │
│ Hạn thuê bao       [2027-11-30]              │
│ ▸ Thông tin hợp đồng (tuỳ chọn)              │
│ Quản lý*                                     │
│   [email          ] [tên        ]  ✕         │
│   + Thêm quản lý                             │
│                          [Huỷ] [Tạo studio]  │
└──────────────────────────────────────────────┘
```

### Tài liệu cần cập nhật

Spec chi tiết (FN-PLT-01/02, FN-ADM-SET-01, FN-PUB-CONTR-01 + dòng lịch sử phiên bản), `docs/api/admin-api.yml`,
`docs/guides/web-admin-overview.md` (luồng tạo studio), `deploy/README.md` mục 3.

### Kiểm thử (JUnit trước, rồi code)

- Tạo studio với 2 quản lý → 2 tài khoản `ROLE_ADMIN` cùng studio, 2 mật khẩu tạm, `adminCount = 2`.
- Email thứ hai đã tồn tại → 409, không có studio/settings/tài khoản nào được tạo.
- Email trùng trong danh sách, 0 hoặc 11 quản lý → 400.
- Logo: hợp lệ lưu và đọc lại đúng; sai loại, base64 hỏng, > 200 KB → 400; `null` xoá.
- Quản lý studio A sửa logo không ảnh hưởng studio B (`StudioIsolationApiTest`); nhân viên không sửa được.
- PDF và trang công khai có logo khi có, vẫn đúng khi không có.

## Để builder quyết định

- Số migration, tên cột, cách nhân viên nhận logo cho menu (cài đặt hay `auth/me`).
- Cách thu nhỏ ảnh trên trình duyệt (canvas, không thêm thư viện).
