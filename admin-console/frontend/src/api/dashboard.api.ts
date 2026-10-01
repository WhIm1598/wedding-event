import { env } from '@/config/env';
import { http } from './http';
import { db, delay } from './mock/db';
import type { DashboardStats, RevenuePoint } from '@/types';

export const dashboardApi = {
  /** GET /admin/dashboard/stats (FN-ADM-DASH-01) */
  getStats(): Promise<DashboardStats> {
    if (!env.useMock) return http.get('/admin/dashboard/stats');

    const assetsTotal = db.assets.filter((a) => a.category !== 'CAMERA_EQUIPMENT').length || 1;
    const inUse = db.assets.filter((a) => a.category !== 'CAMERA_EQUIPMENT' && a.status === 'IN_USE').length;
    return delay({
      monthlyRevenue: { amount: 245_000_000, trendPercentage: 12.5, isIncrease: true },
      activeContractsCount: { value: db.contracts.filter((c) => c.status !== 'COMPLETED').length, trendPercentage: 5, isIncrease: true },
      pendingLeadsCount: { value: db.leads.filter((l) => l.stage === 'NEW_LEAD' || l.stage === 'IN_CONSULTATION').length, trendPercentage: 2, isIncrease: false },
      costumeRentalRate: { percentage: Math.round((inUse / assetsTotal) * 100), trendPercentage: 15, isIncrease: true },
    });
  },

  /** GET /admin/dashboard/revenue-chart */
  getRevenueChart(year: number): Promise<RevenuePoint[]> {
    if (!env.useMock) return http.get('/admin/dashboard/revenue-chart', { year });
    const factor = year === new Date().getFullYear() ? 1 : 0.8;
    return delay(db.revenueByMonth.map((p) => ({ ...p, amount: Math.round(p.amount * factor) })));
  },
};
