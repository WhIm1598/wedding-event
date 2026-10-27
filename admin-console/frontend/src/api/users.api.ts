import { env } from '@/config/env';
import { ApiError, http } from './http';
import { db, delay, nextId } from './mock/db';
import type { CreatedAccount, CreateUserRequest, PageResponse, Role, UpdateUserRequest, UserAccount } from '@/types';

/** Mock only: the demo admin is the caller */
const MOCK_ACTOR_ID = 'u-admin';

const mockTemporaryPassword = () => `Tam${Math.random().toString(36).slice(2, 7)}${Math.floor(Math.random() * 90 + 10)}`;

const staffName = (staffId: string | null) => db.staff.find((s) => s.id === staffId)?.name ?? null;

function checkStaffLink(staffId: string | null, accountId: string | null) {
  if (staffId && db.users.some((u) => u.staffId === staffId && u.id !== accountId)) {
    throw new ApiError(409, 'STAFF_ALREADY_LINKED', 'Nhân sự này đã có tài khoản đăng nhập');
  }
}

export const usersApi = {
  /** GET /admin/users (FN-ADM-USER-01) */
  list(query: { page?: number; size?: number; role?: Role; active?: boolean } = {}): Promise<PageResponse<UserAccount>> {
    if (!env.useMock) return http.get('/admin/users', query);
    const items = db.users.filter((u) => (!query.role || u.role === query.role) && (query.active === undefined || u.isActive === query.active));
    return delay({ items, page: 0, size: items.length, totalElements: items.length, totalPages: 1 });
  },

  /** POST /admin/users — the temporary password is returned only here */
  create(req: CreateUserRequest): Promise<CreatedAccount> {
    if (!env.useMock) return http.post('/admin/users', req);
    if (db.users.some((u) => u.email.toLowerCase() === req.email.trim().toLowerCase())) {
      throw new ApiError(409, 'EMAIL_EXISTS', 'Email đã được dùng cho một tài khoản khác');
    }
    checkStaffLink(req.staffId, null);
    const user: UserAccount = { ...req, id: nextId('u'), staffName: staffName(req.staffId), isActive: true, lastLoginAt: null };
    db.users.push(user);
    return delay({ user, temporaryPassword: mockTemporaryPassword() });
  },

  /** PATCH /admin/users/{id} — the form sends every field */
  update(id: string, req: UpdateUserRequest): Promise<UserAccount> {
    if (!env.useMock) return http.patch(`/admin/users/${id}`, req);
    const user = db.users.find((u) => u.id === id)!;
    const staysActiveAdmin = req.isActive && req.role === 'ROLE_ADMIN';
    if (id === MOCK_ACTOR_ID && !staysActiveAdmin) {
      throw new ApiError(409, 'CANNOT_MODIFY_SELF', 'Không thể tự khóa hoặc tự hạ quyền tài khoản của mình');
    }
    const otherAdmins = db.users.filter((u) => u.id !== id && u.isActive && u.role === 'ROLE_ADMIN');
    if (user.isActive && user.role === 'ROLE_ADMIN' && !staysActiveAdmin && otherAdmins.length === 0) {
      throw new ApiError(409, 'LAST_ADMIN', 'Phải còn ít nhất một quản lý đang hoạt động');
    }
    checkStaffLink(req.staffId, id);
    Object.assign(user, req, { staffName: staffName(req.staffId) });
    return delay(user);
  },

  /** POST /admin/users/{id}/reset-password */
  resetPassword(id: string): Promise<CreatedAccount> {
    if (!env.useMock) return http.post(`/admin/users/${id}/reset-password`);
    return delay({ user: db.users.find((u) => u.id === id)!, temporaryPassword: mockTemporaryPassword() });
  },
};
