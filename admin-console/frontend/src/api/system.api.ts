import { env } from '@/config/env';
import { http } from './http';
import { db, delay } from './mock/db';
import type { AppNotification, SearchResult, StudioSettings } from '@/types';

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
    db.settings = settings;
    return delay(settings);
  },
};
