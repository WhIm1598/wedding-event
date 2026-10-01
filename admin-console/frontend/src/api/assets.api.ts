import { env } from '@/config/env';
import { addDays } from '@/lib/date';
import { http } from './http';
import { db, delay, nextId } from './mock/db';
import type { Asset, AssetConflict, AssetCategory, CreateAssetRequest } from '@/types';

export const assetsApi = {
  /** GET /admin/assets?category */
  list(category?: AssetCategory): Promise<Asset[]> {
    if (!env.useMock) return http.get('/admin/assets', { category });
    return delay(db.assets.filter((a) => !category || a.category === category));
  },

  /** GET /admin/assets/conflicts — reservations overlapping a maintenance buffer */
  getConflicts(): Promise<AssetConflict[]> {
    if (!env.useMock) return http.get('/admin/assets/conflicts');
    const vay = db.assets.find((a) => a.code === 'VAY-001');
    return delay(
      vay?.nextBookingDate
        ? [{
            assetCode: vay.code,
            assetName: vay.name,
            date: vay.nextBookingDate,
            message: `Sản phẩm "${vay.code} (${vay.name})" đang được xếp cho 2 cô dâu trong khoảng bảo dưỡng đến ${addDays(vay.nextBookingDate, vay.maintenanceBufferDays)}.`,
          }]
        : [],
    );
  },

  /** POST /admin/assets */
  create(req: CreateAssetRequest): Promise<Asset> {
    if (!env.useMock) return http.post('/admin/assets', req);
    const asset: Asset = { ...req, id: nextId('as'), status: 'AVAILABLE', nextBookingDate: null };
    db.assets.push(asset);
    return delay(asset);
  },

  /**
   * POST /admin/assets/booking (FN-ADM-ASSET-01).
   * Backend locks [rentalStartDate, rentalEndDate + maintenance_buffer_days] and returns 409 ASSET_NOT_AVAILABLE on overlap.
   */
  reserve(req: { assetId: string; rentalStartDate: string; rentalEndDate: string; bookingId: string }): Promise<void> {
    return http.post('/admin/assets/booking', req);
  },
};
