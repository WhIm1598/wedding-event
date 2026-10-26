import { env } from '@/config/env';
import { todayISO } from '@/lib/date';
import { ApiError, http } from './http';
import { db, delay, nextId } from './mock/db';
import type { CreatedAccount, CreatedStudio, CreateStudioRequest, Studio, UpdateStudioRequest } from '@/types';

const mockTemporaryPassword = () => `Tam${Math.random().toString(36).slice(2, 7)}${Math.floor(Math.random() * 90 + 10)}`;

const usable = (s: Pick<Studio, 'status' | 'subscriptionExpiresAt'>) =>
  s.status === 'ACTIVE' && (!s.subscriptionExpiresAt || s.subscriptionExpiresAt >= todayISO());

function mockAdmin(email: string, name: string): CreatedAccount {
  if (db.users.some((u) => u.email.toLowerCase() === email.trim().toLowerCase())) {
    throw new ApiError(409, 'EMAIL_EXISTS', 'Email đã được dùng cho một tài khoản khác');
  }
  const user = { id: nextId('u'), email: email.trim(), name: name.trim(), role: 'ROLE_ADMIN' as const, staffId: null, staffName: null, isActive: true, lastLoginAt: null };
  return { user, temporaryPassword: mockTemporaryPassword() };
}

/** Studios of the platform (FN-PLT-01..03, ROLE_PLATFORM_ADMIN only) */
export const platformApi = {
  /** GET /platform/studios */
  list(): Promise<Studio[]> {
    if (!env.useMock) return http.get('/platform/studios');
    return delay(db.studios);
  },

  /** POST /platform/studios — the studio with its first admin (temporary password shown once) */
  create(req: CreateStudioRequest): Promise<CreatedStudio> {
    if (!env.useMock) return http.post('/platform/studios', req);
    if (!req.name.trim()) throw new ApiError(400, 'BAD_REQUEST', 'Cần tên studio');
    const admin = mockAdmin(req.adminEmail, req.adminName);
    const studio: Studio = {
      id: nextId('st'),
      name: req.name.trim(),
      status: 'ACTIVE',
      subscriptionExpiresAt: req.subscriptionExpiresAt,
      usable: usable({ status: 'ACTIVE', subscriptionExpiresAt: req.subscriptionExpiresAt }),
      adminCount: 1,
      createdAt: new Date().toISOString(),
    };
    db.studios.push(studio);
    return delay({ studio, admin });
  },

  /** PATCH /platform/studios/{id} — status and subscription end (null = no end) */
  update(id: string, req: UpdateStudioRequest): Promise<Studio> {
    if (!env.useMock) return http.patch(`/platform/studios/${id}`, req);
    const studio = db.studios.find((s) => s.id === id)!;
    Object.assign(studio, req, { usable: usable(req) });
    return delay(studio);
  },

  /** POST /platform/studios/{id}/admins */
  createAdmin(id: string, email: string, name: string): Promise<CreatedAccount> {
    if (!env.useMock) return http.post(`/platform/studios/${id}/admins`, { email, name });
    const created = mockAdmin(email, name);
    db.studios.find((s) => s.id === id)!.adminCount += 1;
    return delay(created);
  },
};
