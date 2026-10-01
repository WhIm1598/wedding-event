import { CalendarDays, HeartHandshake, LayoutDashboard, Package, PieChart, Settings, Shirt, Users, type LucideIcon } from 'lucide-react';
import type { Role } from '@/types';

export interface NavItem {
  path: string;
  label: string;
  icon: LucideIcon;
  roles: Role[];
}

// RBAC: menu + route access per role (spec section 3)
export const NAVIGATION: NavItem[] = [
  { path: '/dashboard', label: 'Tổng Quan', icon: LayoutDashboard, roles: ['ROLE_ADMIN', 'ROLE_STAFF'] },
  { path: '/bookings', label: 'Lịch Trình', icon: CalendarDays, roles: ['ROLE_ADMIN', 'ROLE_STAFF'] },
  { path: '/customers', label: 'Khách Hàng (CRM)', icon: HeartHandshake, roles: ['ROLE_ADMIN', 'ROLE_STAFF'] },
  { path: '/packages', label: 'Gói Dịch Vụ', icon: Package, roles: ['ROLE_ADMIN'] },
  { path: '/assets', label: 'Kho & Thiết bị', icon: Shirt, roles: ['ROLE_ADMIN', 'ROLE_STAFF'] },
  { path: '/staff', label: 'Nhân Sự', icon: Users, roles: ['ROLE_ADMIN'] },
  { path: '/financials', label: 'Kế Toán', icon: PieChart, roles: ['ROLE_ADMIN'] },
  { path: '/settings', label: 'Cài Đặt', icon: Settings, roles: ['ROLE_ADMIN'] },
];
