import { env } from '@/config/env';
import { ApiError, http } from './http';
import { db, delay } from './mock/db';
import type { AppNotification, SearchResult, StudioSettings, UpdateZaloOaRequest, ZaloOaSettings } from '@/types';

export const notificationsApi = {
  /** GET /admin/notifications */
  list(): Promise<AppNotification[]> {
    if (!env.useMock) return http.get('/admin/notifications');
    return delay(db.notifications, 150);
  },

  /** PATCH /admin/notifications/{id}/read */
  markRead(id: string): Promise<void> {
    if (!env.useMock) return http.patch(`/admin/notifications/${id}/read`);
    db.notifications.forEach((n) => n.id === id && (n.read = true));
    return delay(undefined, 50);
  },

  /** PATCH /admin/notifications/read-all */
  markAllRead(): Promise<void> {
    if (!env.useMock) return http.patch('/admin/notifications/read-all');
    db.notifications.forEach((n) => (n.read = true));
    return delay(undefined, 50);
  },
};

export const searchApi = {
  /** GET /admin/search?q — global search (Ctrl+K) across customers, contracts, leads */
  search(q: string): Promise<SearchResult[]> {
    if (!env.useMock) return http.get('/admin/search', { q });
    const k = q.trim().toLowerCase();
    const contracts: SearchResult[] = db.contracts.map((c) => ({ id: c.id, label: `${c.contractNumber} (${c.customerName}) • ${c.phone}`, kind: 'contract' }));
    const leads: SearchResult[] = db.leads.map((l) => ({ id: l.id, label: `${l.name} • ${l.phone}`, kind: 'lead' }));
    const all = [...contracts, ...leads];
    return delay(k ? all.filter((r) => r.label.toLowerCase().includes(k)).slice(0, 8) : all.slice(0, 4), 100);
  },
};

export const settingsApi = {
  /** GET /admin/settings/studio */
  get(): Promise<StudioSettings> {
    if (!env.useMock) return http.get('/admin/settings/studio');
    return delay(db.settings);
  },

  /** PUT /admin/settings/studio */
  update(settings: StudioSettings): Promise<StudioSettings> {
    if (!env.useMock) return http.put('/admin/settings/studio', settings);
    if (settings.zaloPhone && !/^0(3|5|7|8|9)[0-9]{8}$/.test(settings.zaloPhone)) {
      throw new ApiError(400, 'BAD_REQUEST', 'Số Zalo không hợp lệ');
    }
    db.settings = { ...settings, zaloPhone: settings.zaloPhone || null };
    return delay(settings);
  },

  /** GET /admin/settings/zalo-oa (FN-ADM-SET-02) — secrets are never returned */
  getZaloOa(): Promise<ZaloOaSettings> {
    if (!env.useMock) return http.get('/admin/settings/zalo-oa');
    return delay(db.zaloOa);
  },

  /** PUT /admin/settings/zalo-oa — empty appSecret / refreshToken keep the stored ones */
  updateZaloOa(req: UpdateZaloOaRequest): Promise<ZaloOaSettings> {
    if (!env.useMock) return http.put('/admin/settings/zalo-oa', req);
    if (!req.appId.trim() || !req.templateId.trim()) throw new ApiError(400, 'BAD_REQUEST', 'Cần App ID và mã mẫu ZNS');
    const hasAppSecret = db.zaloOa.hasAppSecret || !!req.appSecret.trim();
    const hasRefreshToken = db.zaloOa.hasRefreshToken || !!req.refreshToken.trim();
    db.zaloOa = {
      appId: req.appId.trim(),
      templateId: req.templateId.trim(),
      hasAppSecret,
      hasRefreshToken,
      configured: hasAppSecret && hasRefreshToken,
      accessTokenExpiresAt: null,
    };
    return delay(db.zaloOa);
  },
};
