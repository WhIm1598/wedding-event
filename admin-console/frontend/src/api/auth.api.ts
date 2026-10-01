import { env } from '@/config/env';
import { http, tokenStorage } from './http';
import { delay } from './mock/db';
import type { AuthUser, LoginResponse, Role } from '@/types';

const DEMO_USERS: Record<Role, AuthUser> = {
  ROLE_ADMIN: { id: 'u-admin', name: 'Quản Lý Studio', email: 'admin@lumiere.vn', role: 'ROLE_ADMIN' },
  ROLE_STAFF: { id: 'u-staff', name: 'Nhân Viên', email: 'staff@lumiere.vn', role: 'ROLE_STAFF' },
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

  logout() {
    tokenStorage.clear();
  },
};
