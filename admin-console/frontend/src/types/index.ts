// Domain types mirror docs/features/detailed_functional_specification.md (Part B).
// Money is VND as a plain number; dates are ISO strings (YYYY-MM-DD).

export type Role = 'ROLE_ADMIN' | 'ROLE_STAFF';

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: Role;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

// --- Dashboard (FN-ADM-DASH-01) ---
export interface TrendMetric {
  trendPercentage: number;
  isIncrease: boolean;
}

export interface DashboardStats {
  monthlyRevenue: TrendMetric & { amount: number };
  activeContractsCount: TrendMetric & { value: number };
  pendingLeadsCount: TrendMetric & { value: number };
  costumeRentalRate: TrendMetric & { percentage: number };
}

export interface RevenuePoint {
  month: number; // 1-12
  amount: number;
}

// --- Bookings (FN-ADM-CAL-01/02) ---
export type BookingStatus = 'UPCOMING' | 'IN_PROGRESS' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';
export type CalendarView = 'day' | 'week' | 'month';

export interface StaffRef {
  id: string;
  name: string;
  role: string;
}

export interface Booking {
  id: string;
  date: string;
  time: string; // HH:mm (24h)
  clientName: string;
  phone?: string;
  type: string;
  packageName: string;
  venueAddress?: string;
  status: BookingStatus;
  assignedStaff: StaffRef[];
}

export interface CreateBookingRequest {
  clientName: string;
  phone?: string;
  servicePackageId?: string;
  type: string;
  eventDate: string;
  eventTime: string;
  venueAddress?: string;
  staffIds: string[];
}

// --- Packages ---
export interface ServicePackage {
  id: string;
  name: string;
  type: string;
  price: number;
  features: string[];
  isActive: boolean;
}

export type CreatePackageRequest = Omit<ServicePackage, 'id' | 'isActive'>;

// --- Assets (FN-ADM-ASSET-01) ---
export type AssetCategory = 'DRESS' | 'SUIT' | 'CAMERA_EQUIPMENT';
export type AssetStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE';

export interface Asset {
  id: string;
  code: string;
  name: string;
  category: AssetCategory;
  size: string;
  status: AssetStatus;
  nextBookingDate: string | null;
  maintenanceBufferDays: number;
}

export interface AssetConflict {
  assetCode: string;
  assetName: string;
  date: string;
  message: string;
}

export type CreateAssetRequest = Omit<Asset, 'id' | 'status' | 'nextBookingDate'>;

// --- Staff ---
export type StaffWorkStatus = 'WORKING' | 'ON_LEAVE';

export interface StaffMember {
  id: string;
  code: string;
  name: string;
  position: string;
  phone: string;
  workStatus: StaffWorkStatus;
  completedThisMonth: number;
}

export interface StaffTask {
  id: string;
  staffId: string;
  title: string;
  dueDate: string;
  notes?: string;
  completed: boolean;
}

export type CreateStaffRequest = Pick<StaffMember, 'name' | 'phone' | 'position'>;
export type CreateTaskRequest = Pick<StaffTask, 'staffId' | 'title' | 'dueDate' | 'notes'>;

// --- CRM (FN-ADM-CRM-01) ---
export type LeadStage = 'NEW_LEAD' | 'IN_CONSULTATION' | 'AWAITING_DEPOSIT' | 'IN_PROGRESS' | 'COMPLETED';

export interface Lead {
  id: string;
  name: string;
  phone: string;
  hasZalo: boolean;
  interest: string;
  stage: LeadStage;
  createdAt: string;
}

export type CreateLeadRequest = Pick<Lead, 'name' | 'phone' | 'hasZalo' | 'interest'>;

// --- Contracts (FN-ADM-CONTR-01) ---
// NOTE: the spec lists DRAFT | DEPOSITED | COMPLETED; IN_PROGRESS is added to match the UI template.
export type ContractStatus = 'DRAFT' | 'DEPOSITED' | 'IN_PROGRESS' | 'COMPLETED';

export interface Contract {
  id: string;
  contractNumber: string;
  customerName: string;
  phone: string;
  hasZalo: boolean;
  packageName: string;
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  contractDate: string;
  status: ContractStatus;
  notes?: string;
}

export interface CreateContractRequest {
  customerName: string;
  phone: string;
  servicePackageId: string;
  totalAmount: number;
  depositAmount: number;
  notes?: string;
}

// --- Financials (FN-ADM-FIN-01) ---
export type TransactionType = 'INCOME' | 'EXPENSE';

export interface Transaction {
  id: string;
  code: string;
  transactionDate: string;
  description: string;
  type: TransactionType;
  amount: number;
  category: string;
  contractId?: string | null;
}

export interface FinancialSummary {
  periodLabel: string;
  totalIncome: number;
  totalExpense: number;
  netProfit: number;
}

export type CreateTransactionRequest = Omit<Transaction, 'id' | 'code'>;

// --- Misc ---
export interface AppNotification {
  id: string;
  title: string;
  description: string;
  createdAt: string;
  read: boolean;
}

export interface StudioSettings {
  name: string;
  address: string;
  taxCode: string;
  legalRepresentative: string;
  bankInfo: string;
}

export interface SearchResult {
  id: string;
  label: string;
  kind: 'customer' | 'contract' | 'lead';
}

// Standard response envelope (spec section 1.2)
export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  errors?: unknown[];
}
