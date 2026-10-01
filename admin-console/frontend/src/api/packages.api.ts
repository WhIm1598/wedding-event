import { env } from '@/config/env';
import { http } from './http';
import { db, delay, nextId } from './mock/db';
import type { CreatePackageRequest, ServicePackage } from '@/types';

export const packagesApi = {
  /** GET /admin/packages */
  list(): Promise<ServicePackage[]> {
    if (!env.useMock) return http.get('/admin/packages');
    return delay(db.packages);
  },

  /** POST /admin/packages */
  create(req: CreatePackageRequest): Promise<ServicePackage> {
    if (!env.useMock) return http.post('/admin/packages', req);
    const pkg: ServicePackage = { ...req, id: nextId('pkg'), isActive: true };
    db.packages.push(pkg);
    return delay(pkg);
  },

  /** PATCH /admin/packages/{id}/active — toggle selling status */
  setActive(id: string, isActive: boolean): Promise<ServicePackage> {
    if (!env.useMock) return http.patch(`/admin/packages/${id}/active`, { isActive });
    const pkg = db.packages.find((p) => p.id === id)!;
    pkg.isActive = isActive;
    return delay(pkg);
  },
};
