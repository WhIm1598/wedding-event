// In-memory mock database seeded from docs/templates/lumi_re_studios_admin_dashboard.tsx.
// Dates are relative to today so the demo always has "today" data. Data resets on page reload.
import { addDays, todayISO } from '@/lib/date';
import type {
  AppNotification,
  Asset,
  Booking,
  Contract,
  Lead,
  ServicePackage,
  StaffMember,
  StaffTask,
  StudioSettings,
  Transaction,
  Studio,
  UserAccount,
  ZaloOaSettings,
} from '@/types';

const today = todayISO();
const minutesAgo = (m: number) => new Date(Date.now() - m * 60000).toISOString();

export const delay = <T>(value: T, ms = 300): Promise<T> =>
  new Promise((resolve) => setTimeout(() => resolve(structuredClone(value)), ms));

let seq = 1000;
export const nextId = (prefix: string): string => `${prefix}-${++seq}`;

const staff: StaffMember[] = [
  { id: 'nv-01', code: 'NV-001', name: 'Tuấn Photo', position: 'Thợ Chụp Chính', phone: '0901234567', workStatus: 'WORKING', completedThisMonth: 12 },
  { id: 'nv-02', code: 'NV-002', name: 'Hương Sale', position: 'Tư Vấn Viên', phone: '0987654321', workStatus: 'WORKING', completedThisMonth: 45 },
  { id: 'nv-03', code: 'NV-003', name: 'Minh Video', position: 'Thợ Quay Phim', phone: '0912345678', workStatus: 'ON_LEAVE', completedThisMonth: 8 },
  { id: 'nv-04', code: 'NV-004', name: 'Trang Makeup', position: 'Chuyên Viên Trang Điểm', phone: '0977777777', workStatus: 'WORKING', completedThisMonth: 15 },
];

const ref = (id: string) => {
  const s = staff.find((x) => x.id === id)!;
  return { id: s.id, name: s.name, role: s.position };
};

export const db = {
  staff,

  // Studios of the platform (FN-PLT-01); the demo studio is the first one
  studios: [
    { id: 'st-1', name: 'LUMIÈRE STUDIOS', status: 'ACTIVE', subscriptionExpiresAt: null, usable: true, adminCount: 1, createdAt: minutesAgo(60 * 24 * 90) },
    { id: 'st-2', name: 'Studio Hoa Biển', status: 'SUSPENDED', subscriptionExpiresAt: addDays(today, -5), usable: false, adminCount: 1, createdAt: minutesAgo(60 * 24 * 30) },
  ] as Studio[],

  // Login accounts (FN-ADM-USER-01); ids match the demo users in auth.api.ts
  users: [
    { id: 'u-admin', email: 'admin@lumiere.vn', name: 'Quản Lý Studio', role: 'ROLE_ADMIN', staffId: null, staffName: null, isActive: true, lastLoginAt: minutesAgo(5) },
    { id: 'u-staff', email: 'staff@lumiere.vn', name: 'Nhân Viên', role: 'ROLE_STAFF', staffId: 'nv-02', staffName: 'Hương Sale', isActive: true, lastLoginAt: minutesAgo(180) },
  ] as UserAccount[],

  packages: [
    { id: 'pkg-1', name: 'Gói Kim Cương (Ngày Cưới)', price: 25_000_000, type: 'Trọn gói', isActive: true,
      features: ['2 Thợ Phóng sự', '1 Quay Phim', '1 Album Photobook 30x30', 'Makeup & Làm tóc trọn ngày', 'Tặng 2 Ảnh Cổng'] },
    { id: 'pkg-2', name: 'Gói Tiêu Chuẩn (Pre-wedding)', price: 12_500_000, type: 'Pre-wedding', isActive: true,
      features: ['1 Thợ Chụp', '2 Địa điểm nội thành', '2 Váy + 2 Vest', 'Makeup & Làm tóc đi kèm'] },
    { id: 'pkg-3', name: 'Gói Lễ Ăn Hỏi', price: 6_000_000, type: 'Lễ Ăn Hỏi', isActive: true,
      features: ['1 Thợ Chụp truyền thống', 'Giao toàn bộ file gốc', 'Chỉnh sửa 50 file'] },
  ] as ServicePackage[],

  bookings: [
    { id: 'bk-101', date: today, time: '09:00', clientName: 'Lan & Hoàng', phone: '0901112222', type: 'Chụp Pre-wedding', packageName: 'Gói Kim Cương (Ngày Cưới)', status: 'IN_PROGRESS', assignedStaff: [ref('nv-01')], contractId: 'hd-102' },
    { id: 'bk-102', date: today, time: '11:30', clientName: 'Mai & Huy', type: 'Thử Váy / Vest', packageName: 'Gói Kim Cương (Ngày Cưới)', status: 'UPCOMING', assignedStaff: [ref('nv-02')] },
    { id: 'bk-103', date: addDays(today, 1), time: '14:00', clientName: 'Linh & Tuấn', type: 'Lễ Ăn Hỏi', packageName: 'Gói Lễ Ăn Hỏi', status: 'UPCOMING', assignedStaff: [ref('nv-03')] },
    { id: 'bk-104', date: addDays(today, 2), time: '08:00', clientName: 'Trang & Hiếu', type: 'Chụp Pre-wedding', packageName: 'Gói Kim Cương (Ngày Cưới)', status: 'CONFIRMED', assignedStaff: [ref('nv-01'), ref('nv-04')] },
    { id: 'bk-105', date: addDays(today, 9), time: '07:30', clientName: 'Ngọc & Hoàng', type: 'Chụp Phóng sự cưới', packageName: 'Gói Kim Cương (Ngày Cưới)', status: 'CONFIRMED', assignedStaff: [ref('nv-01'), ref('nv-03')] },
  ] as Booking[],

  assets: [
    { id: 'as-1', code: 'VAY-001', name: 'Vera Wang đuôi cá đính đá', category: 'DRESS', size: 'M', status: 'AVAILABLE', nextBookingDate: addDays(today, 11), maintenanceBufferDays: 3 },
    { id: 'as-2', code: 'VST-002', name: 'Vest đen Hàn Quốc', category: 'SUIT', size: '42R', status: 'IN_USE', nextBookingDate: null, maintenanceBufferDays: 1 },
    { id: 'as-3', code: 'VAY-003', name: 'Soiree bồng bềnh công chúa', category: 'DRESS', size: 'S', status: 'MAINTENANCE', nextBookingDate: null, maintenanceBufferDays: 3 },
    { id: 'as-4', code: 'MAY-004', name: 'Sony A7IV + Lens 35mm GM', category: 'CAMERA_EQUIPMENT', size: 'N/A', status: 'AVAILABLE', nextBookingDate: addDays(today, 14), maintenanceBufferDays: 0 },
  ] as Asset[],

  tasks: [
    { id: 't-1', staffId: 'nv-01', title: 'Chuẩn bị trang phục cho khách Lan & Hoàng', dueDate: today, completed: false },
    { id: 't-2', staffId: 'nv-04', title: 'Makeup cô dâu Mai - Lễ Ăn Hỏi', dueDate: addDays(today, -1), completed: true },
    { id: 't-3', staffId: 'nv-01', title: 'Kiểm tra pin và thẻ nhớ máy Sony A7IV', dueDate: today, completed: false },
  ] as StaffTask[],

  contracts: [
    { id: 'hd-102', contractNumber: 'HD-102', customerName: 'Nguyễn Thị Lan', phone: '0901112222', hasZalo: true, packageName: 'Gói Kim Cương (Ngày Cưới)', totalAmount: 25_000_000, paidAmount: 10_000_000, remainingAmount: 15_000_000, contractDate: addDays(today, -20), status: 'IN_PROGRESS' },
    { id: 'hd-103', contractNumber: 'HD-103', customerName: 'Trần Văn Huy', phone: '0988889999', hasZalo: true, packageName: 'Gói Lễ Ăn Hỏi', totalAmount: 6_000_000, paidAmount: 6_000_000, remainingAmount: 0, contractDate: addDays(today, -45), status: 'COMPLETED', deliveredAt: `${addDays(today, -41)}T03:00:00Z` },
    { id: 'hd-104', contractNumber: 'HD-104', customerName: 'Lê Hoàng C', phone: '0911223344', hasZalo: false, packageName: 'Gói Tiêu Chuẩn (Pre-wedding)', totalAmount: 12_500_000, paidAmount: 2_500_000, remainingAmount: 10_000_000, contractDate: addDays(today, -3), status: 'DEPOSITED' },
  ] as Contract[],

  leads: [
    { id: 'ld-1', name: 'Minh & Châu', phone: '0933111222', hasZalo: true, interest: 'Gói Lễ Ăn Hỏi', stage: 'NEW_LEAD', createdAt: today },
    { id: 'ld-2', name: 'Quốc & Anh', phone: '0977888999', hasZalo: true, interest: 'Gói Kim Cương', stage: 'IN_CONSULTATION', createdAt: addDays(today, -3) },
    { id: 'ld-3', name: 'Thảo & Đạt', phone: '0988111333', hasZalo: false, interest: 'Gói Tiêu Chuẩn', stage: 'AWAITING_DEPOSIT', createdAt: addDays(today, -4) },
    { id: 'ld-4', name: 'Nguyễn Thị Lan', phone: '0901112222', hasZalo: true, interest: 'Gói Kim Cương', stage: 'IN_PROGRESS', createdAt: addDays(today, -25) },
    { id: 'ld-5', name: 'Trần Văn Huy', phone: '0988889999', hasZalo: true, interest: 'Gói Lễ Ăn Hỏi', stage: 'COMPLETED', createdAt: addDays(today, -50) },
  ] as Lead[],

  transactions: [
    { id: 'trx-1', code: 'TRX-001', transactionDate: today, description: 'Thanh toán cọc hợp đồng Lê Hoàng C', type: 'INCOME', amount: 2_500_000, category: 'Doanh thu HĐ', contractId: 'hd-104' },
    { id: 'trx-2', code: 'TRX-002', transactionDate: addDays(today, -1), description: 'In album photobook khách Mai Huy', type: 'EXPENSE', amount: 1_200_000, category: 'Chi phí in ấn' },
    { id: 'trx-3', code: 'TRX-003', transactionDate: addDays(today, -2), description: 'Thuê xe di chuyển chụp Pre-wedding', type: 'EXPENSE', amount: 800_000, category: 'Chi phí vận hành' },
  ] as Transaction[],

  notifications: [
    { id: 'n-1', title: 'Khách hàng tới hạn thanh toán 💰', description: 'Khách "Lê Hoàng C" tới hạn thanh toán đợt 2 (10.000.000đ). Hãy nhắn Zalo nhắc nhở.', createdAt: minutesAgo(10), read: false },
    { id: 'n-2', title: 'Trùng lịch trang phục 👗', description: 'Váy "Vera Wang" đang được xếp cho 2 khách vào cùng tuần. Vui lòng kiểm tra lại.', createdAt: minutesAgo(120), read: false },
    { id: 'n-3', title: 'Lịch chụp ngày mai 📸', description: 'Có lịch trình vào ngày mai. Đã nhắc thợ chụp chuẩn bị thiết bị chưa?', createdAt: minutesAgo(300), read: true },
  ] as AppNotification[],

  settings: {
    name: 'LUMIÈRE STUDIOS',
    address: '123 Nguyễn Văn Linh, Q7, TP.HCM',
    taxCode: '0312345678',
    legalRepresentative: 'Nguyễn Văn A',
    bankInfo: 'Vietcombank - Chi nhánh HCM\nSTK: 0123456789\nChủ TK: NGUYEN VAN A',
  } as StudioSettings,

  // Zalo OA of the studio (FN-ADM-SET-02); the mock keeps it connected so "Gửi Zalo" works in the demo
  zaloOa: { appId: 'demo-app', templateId: 'demo-template', hasAppSecret: true, hasRefreshToken: true, configured: true, accessTokenExpiresAt: null } as ZaloOaSettings,
  /** public link token → contract (FN-PUB-CONTR-01) */
  // /c/demo opens in demo mode without sharing first (the in-memory db resets on reload)
  shareLinks: { demo: { contractId: 'hd-102', expiresAt: '2099-12-31T00:00:00Z' } } as Record<string, { contractId: string; expiresAt: string }>,
  acceptedAt: {} as Record<string, string>,

  revenueByMonth: [40, 70, 45, 90, 65, 85, 100, 50, 75, 60, 80, 95].map((m, i) => ({ month: i + 1, amount: m * 1_000_000 })),
};

/**
 * Mock of the backend's CustomerStageSync (FN-ADM-CRM-03): the customer (same phone) is "in progress" once a
 * contract is effective and "completed" once all of their contracts are completed.
 */
export function syncCustomerStage(contract: Contract): void {
  const lead = db.leads.find((l) => l.phone === contract.phone);
  if (!lead || contract.status === 'DRAFT') return;
  if (lead.stage === 'NEW_LEAD' || lead.stage === 'IN_CONSULTATION' || lead.stage === 'AWAITING_DEPOSIT') lead.stage = 'IN_PROGRESS';
  // an abandoned draft isn't effective, so it doesn't keep the customer "in progress"
  const open = db.contracts.some((c) => c.phone === lead.phone && (c.status === 'DEPOSITED' || c.status === 'IN_PROGRESS'));
  if (contract.status === 'COMPLETED' && lead.stage === 'IN_PROGRESS' && !open) lead.stage = 'COMPLETED';
}

/** Mock of Contract.applyPayment (spec §1.6): first payment → DEPOSITED; COMPLETED needs paid in full AND delivered */
export function applyMockPayment(contract: Contract, amount: number): void {
  contract.paidAmount += amount;
  contract.remainingAmount = Math.max(0, contract.totalAmount - contract.paidAmount);
  if (contract.status === 'DRAFT') contract.status = 'DEPOSITED';
  // the shoot may have started before the deposit
  const started = db.bookings.some((b) => b.contractId === contract.id && (b.status === 'IN_PROGRESS' || b.status === 'COMPLETED'));
  if (contract.status === 'DEPOSITED' && started) contract.status = 'IN_PROGRESS';
  if (contract.remainingAmount === 0 && contract.deliveredAt) contract.status = 'COMPLETED';
  syncCustomerStage(contract);
}
