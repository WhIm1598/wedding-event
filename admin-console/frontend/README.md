# Lumière Studios — Admin Web (React + TypeScript)

Cổng quản trị vận hành Studio, dựng từ template `docs/templates/lumi_re_studios_admin_dashboard.tsx`.

## Chạy demo

```bash
cd admin-console/frontend
npm install
npm run dev          # http://localhost:5173 — mặc định dùng mock API, không cần backend
npm run build        # tsc --noEmit + vite build
```

Kết nối backend thật (`admin-console/backend`, port 8082): `npm run dev:api` (đọc `.env.api`, `VITE_USE_MOCK=false`).
Vite proxy chuyển `/api/**` sang `VITE_API_PROXY_TARGET`. Đăng nhập bằng tài khoản demo: `admin@lumiere.vn / Admin@123`
hoặc `staff@lumiere.vn / Staff@123`. Token hết hạn sẽ tự refresh; refresh thất bại thì quay về trang đăng nhập.

**Demo:** chọn "Quản Lý" (toàn quyền) hoặc "Nhân Viên" (ẩn Gói dịch vụ, Nhân sự, Kế toán, Cài đặt).

## Stack

React 18 · TypeScript (strict) · Vite 5 · Tailwind CSS 3 · React Router 6 · lucide-react

## Cấu trúc

```
src/
├── main.tsx                 # Providers (Toast, Auth) + RouterProvider
├── app/
│   ├── router.tsx           # Routes + RequireAuth / RequireRole (RBAC)
│   └── navigation.ts        # Menu sidebar & quyền theo vai trò
├── api/
│   ├── http.ts              # fetch wrapper: Bearer token, envelope {success, code, message, data}, ApiError
│   ├── mock/db.ts           # In-memory mock DB (dữ liệu từ template, ngày tương đối theo hôm nay)
│   └── *.api.ts             # 1 file / module; mỗi hàm ghi rõ endpoint spec + nhánh mock
├── components/
│   ├── layout/              # AppLayout, Sidebar, Header, GlobalSearch (Ctrl+K), NotificationBell
│   └── ui/                  # Badge, Button, Card, Form, Modal/SlideOver, PageHeader, States
├── contexts/                # AuthContext, ToastContext
├── constants/labels.ts      # Enum → nhãn tiếng Việt + màu Badge
├── hooks/useAsync.ts        # Data fetching tối giản (có thể thay bằng TanStack Query)
├── lib/                     # format (VND, ngày), date helpers, cn
├── types/index.ts           # Domain types theo detailed_functional_specification.md
└── features/
    ├── auth/                # LoginPage
    ├── dashboard/           # KPI, biểu đồ doanh thu, lịch hôm nay        (FN-ADM-DASH-01)
    ├── bookings/            # Lịch Ngày/Tuần/Tháng, tạo lịch + check trùng (FN-ADM-CAL-01, 02)
    ├── customers/           # CRM Kanban (kéo thả) + bảng hợp đồng         (FN-ADM-CRM-01)
    ├── contracts/           # Tạo hợp đồng, xem trước A4, PDF/Zalo         (FN-ADM-CONTR-01)
    ├── packages/            # Gói dịch vụ, bật/tắt kinh doanh
    ├── assets/              # Kho váy/vest/thiết bị + bộ đệm bảo dưỡng     (FN-ADM-ASSET-01)
    ├── staff/               # Nhân sự, giao việc
    ├── financials/          # Sổ quỹ thu/chi, trừ công nợ hợp đồng         (FN-ADM-FIN-01)
    └── settings/            # Thông tin studio, mẫu HĐ, ma trận phân quyền
```

## Ghi chú cho giai đoạn triển khai

- Tất cả endpoint trong `src/api` đã được `admin-console/backend` triển khai và có trong spec v1.1 (Module 5A–9 của `detailed_functional_specification.md`).
- Đăng nhập admin dùng `/api/v1/admin/auth/*` (không phải `/api/v1/auth`) để nginx gateway định tuyến đúng.
- Lọc lịch theo nhân viên (`ROLE_STAFF` chỉ thấy lịch của mình) do backend thực thi từ JWT.
- Việc tiếp theo theo spec v1.1 (🆕): thêm `CANCELLED` + `paymentStatus` cho hợp đồng, bộ chọn trang phục & timeline lịch thuê, bảng 3 đợt thanh toán, hồ sơ khách hàng, sửa/hủy, đổi mật khẩu/đăng xuất, trang công khai `/c/:token`, điều hướng kết quả tìm kiếm (`?id=`), phân trang.
