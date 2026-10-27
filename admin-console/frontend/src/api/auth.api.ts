import { env } from '@/config/env';
import { ApiError, http, tokenStorage } from './http';
import { delay } from './mock/db';
import type { AuthUser, LoginResponse, Role } from '@/types';

const DEMO_USERS: Record<Role, AuthUser> = {
  ROLE_ADMIN: { id: 'u-admin', name: 'Quản Lý Studio', email: 'admin@lumiere.vn', role: 'ROLE_ADMIN', mustChangePassword: false },
  ROLE_STAFF: { id: 'u-staff', name: 'Nhân Viên', email: 'staff@lumiere.vn', role: 'ROLE_STAFF', mustChangePassword: false },
};

export const authApi = {
  /** POST /admin/auth/login (FN-AUTH-01) — under /admin so the nginx gateway routes it to admin-backend */
  async login(email: string, password: string): Promise<AuthUser> {
    const res = await http.post<LoginResponse>('/admin/auth/login', { email, password });
    tokenStorage.set(res.accessToken, res.refreshToken);
    return res.user;
  },

  /** Demo-only login used by the role picker while VITE_USE_MOCK=true */
  async loginAsDemo(role: Role): Promise<AuthUser> {
    tokenStorage.set(`mock-token-${role}`);
    return delay(DEMO_USERS[role], 200);
  },

  /** GET /admin/auth/me — restores the session from the stored token */
  async me(): Promise<AuthUser | null> {
    const token = tokenStorage.get();
    if (!token) return null;
    if (env.useMock) {
      const role = token.replace('mock-token-', '') as Role;
      return DEMO_USERS[role] ?? null;
    }
    try {
      return await http.get<AuthUser>('/admin/auth/me');
    } catch {
      tokenStorage.clear();
      return null;
    }
  },

  /** POST /admin/auth/logout (FN-ADM-AUTH-04) — revokes the refresh token; the local session ends either way */
  logout() {
    const refreshToken = tokenStorage.getRefresh();
    tokenStorage.clear();
    if (!env.useMock && refreshToken) http.post('/admin/auth/logout', { refreshToken }).catch(() => {});
  },

  /** POST /admin/auth/change-password (FN-ADM-AUTH-04) — other sessions end, this one gets a new token pair */
  async changePassword(currentPassword: string, newPassword: string): Promise<AuthUser> {
    if (!env.useMock) {
      const res = await http.post<LoginResponse>('/admin/auth/change-password', { currentPassword, newPassword });
      tokenStorage.set(res.accessToken, res.refreshToken);
      return res.user;
    }
    if (newPassword.length < 8 || newPassword.length > 100) throw new ApiError(400, 'BAD_REQUEST', 'Mật khẩu mới phải từ 8 đến 100 ký tự');
    if (!/\p{L}/u.test(newPassword) || !/\d/.test(newPassword)) throw new ApiError(400, 'BAD_REQUEST', 'Mật khẩu mới phải có cả chữ và số');
    if (newPassword === currentPassword) throw new ApiError(400, 'BAD_REQUEST', 'Mật khẩu mới phải khác mật khẩu hiện tại');
    const role = (tokenStorage.get() ?? '').replace('mock-token-', '') as Role;
    return delay(DEMO_USERS[role], 300);
  },
};
