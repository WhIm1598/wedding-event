import type { BadgeVariant } from '@/components/ui/Badge';
import type { AssetCategory, AssetStatus, BookingProgressStatus, BookingStatus, ContractStatus, LeadStage, StaffWorkStatus, StudioRole, StudioStatus } from '@/types';

type LabelMap<K extends string> = Record<K, { label: string; variant: BadgeVariant }>;

export const ROLES: LabelMap<StudioRole> = {
  ROLE_ADMIN: { label: 'Quản lý', variant: 'brand' },
  ROLE_STAFF: { label: 'Nhân viên', variant: 'info' },
};

export const STUDIO_STATUS: LabelMap<StudioStatus> = {
  ACTIVE: { label: 'Đang hoạt động', variant: 'success' },
  SUSPENDED: { label: 'Tạm ngưng', variant: 'danger' },
};

export const BOOKING_STATUS: LabelMap<BookingStatus> = {
  UPCOMING: { label: 'Chờ xác nhận', variant: 'warning' },
  IN_PROGRESS: { label: 'Đang thực hiện', variant: 'brand' },
  CONFIRMED: { label: 'Đã xác nhận', variant: 'success' },
  COMPLETED: { label: 'Hoàn thành', variant: 'default' },
  CANCELLED: { label: 'Đã hủy', variant: 'danger' },
};

/** Next execution steps per status (spec §1.6, FN-ADM-CAL-04) with their button labels */
export const BOOKING_NEXT_STEPS: Record<BookingStatus, Array<{ status: BookingProgressStatus; label: string }>> = {
  UPCOMING: [
    { status: 'CONFIRMED', label: 'Xác nhận' },
    { status: 'IN_PROGRESS', label: 'Bắt đầu' },
  ],
  CONFIRMED: [{ status: 'IN_PROGRESS', label: 'Bắt đầu' }],
  IN_PROGRESS: [{ status: 'COMPLETED', label: 'Hoàn thành' }],
  COMPLETED: [],
  CANCELLED: [],
};

export const ASSET_STATUS: LabelMap<AssetStatus> = {
  AVAILABLE: { label: 'Sẵn sàng', variant: 'success' },
  IN_USE: { label: 'Đang thuê', variant: 'brand' },
  MAINTENANCE: { label: 'Bảo trì', variant: 'warning' },
};

export const ASSET_CATEGORY: Record<AssetCategory, string> = {
  DRESS: 'Váy Cưới',
  SUIT: 'Vest',
  CAMERA_EQUIPMENT: 'Thiết bị',
};

export const CONTRACT_STATUS: LabelMap<ContractStatus> = {
  DRAFT: { label: 'Nháp', variant: 'default' },
  DEPOSITED: { label: 'Mới cọc', variant: 'warning' },
  IN_PROGRESS: { label: 'Đang thực hiện', variant: 'brand' },
  COMPLETED: { label: 'Đã hoàn thành', variant: 'success' },
};

export const STAFF_STATUS: LabelMap<StaffWorkStatus> = {
  WORKING: { label: 'Đang làm việc', variant: 'success' },
  ON_LEAVE: { label: 'Nghỉ phép', variant: 'warning' },
};

/** Kanban columns in pipeline order (FN-ADM-CRM-01) */
export const LEAD_STAGES: Array<{ id: LeadStage; label: string }> = [
  { id: 'NEW_LEAD', label: 'Mới hỏi' },
  { id: 'IN_CONSULTATION', label: 'Đang tư vấn' },
  { id: 'AWAITING_DEPOSIT', label: 'Chờ cọc' },
  { id: 'IN_PROGRESS', label: 'Đang thực hiện' },
  { id: 'COMPLETED', label: 'Đã hoàn thành' },
];

export const BOOKING_TYPES = ['Chụp Pre-wedding', 'Chụp Phóng sự cưới', 'Thử Váy / Vest', 'Lễ Ăn Hỏi', 'Tư vấn hợp đồng'];
export const PACKAGE_TYPES = ['Trọn gói', 'Pre-wedding', 'Ngày Cưới / Lễ', 'Lễ Ăn Hỏi'];
export const STAFF_POSITIONS = ['Thợ Chụp Chính', 'Thợ Phụ / Ánh Sáng', 'Thợ Quay Phim', 'Chuyên Viên Trang Điểm', 'Tư Vấn Viên'];
export const INCOME_CATEGORIES = ['Doanh thu HĐ', 'Thu cho thuê trang phục', 'Thu khác'];
export const EXPENSE_CATEGORIES = ['Chi phí in ấn', 'Chi phí vận hành', 'Giặt ủi trang phục', 'Tiền công cộng tác viên', 'Chi khác'];
