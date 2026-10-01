import { useState, type FormEvent } from 'react';
import { AlertCircle, CalendarDays, Clock, Plus } from 'lucide-react';
import { assetsApi } from '@/api/assets.api';
import { errorMessage } from '@/api/http';
import { useAsync } from '@/hooks/useAsync';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/contexts/ToastContext';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field, Input, Select } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { ErrorState, Spinner } from '@/components/ui/States';
import { ASSET_CATEGORY, ASSET_STATUS } from '@/constants/labels';
import { formatDate } from '@/lib/format';
import type { Asset, AssetCategory } from '@/types';

const nextBookingLabel = (asset: Asset): string => {
  if (asset.status === 'IN_USE') return 'Đang cho thuê';
  if (asset.status === 'MAINTENANCE') return 'Đang giặt hấp';
  return asset.nextBookingDate ? formatDate(asset.nextBookingDate) : 'Chưa có lịch';
};

export function AssetsPage() {
  const { isAdmin } = useAuth();
  const [category, setCategory] = useState<AssetCategory | ''>('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const { data: assets = [], loading, error, reload } = useAsync(() => assetsApi.list(category || undefined), [category]);
  const { data: conflicts = [] } = useAsync(() => assetsApi.getConflicts());

  return (
    <div className="space-y-6">
      <PageHeader
        title="Kho Đồ & Thiết Bị"
        description="Quản lý Váy Cưới, Vest, và thiết bị chụp ảnh để tránh trùng lịch."
        actions={
          <>
            <Select value={category} onChange={(e) => setCategory(e.target.value as AssetCategory | '')} className="w-auto">
              <option value="">Tất cả danh mục</option>
              {Object.entries(ASSET_CATEGORY).map(([id, label]) => (
                <option key={id} value={id}>
                  {label}
                </option>
              ))}
            </Select>
            {isAdmin && (
              <Button onClick={() => setIsCreateOpen(true)}>
                <Plus size={18} className="mr-2" /> Thêm Đồ
              </Button>
            )}
          </>
        }
      />

      {conflicts.map((c) => (
        <div key={c.assetCode} className="bg-amber-50 border border-amber-200 rounded-lg p-4 flex items-start gap-3">
          <AlertCircle className="text-amber-600 shrink-0 mt-0.5" size={20} />
          <div>
            <h4 className="text-sm font-bold text-amber-800">Cảnh báo: Trùng lịch trang phục</h4>
            <p className="text-sm text-amber-700 mt-1">{c.message}</p>
          </div>
        </div>
      ))}

      <Card className="overflow-hidden">
        {loading && <Spinner />}
        {error && <ErrorState error={error} onRetry={reload} />}
        {!loading && !error && (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200">
                  {['Mã / Tên SP', 'Danh mục & Size', 'Trạng thái', 'Lịch tiếp theo & Buffer'].map((h) => (
                    <th key={h} className="py-3 px-6 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {assets.map((asset) => (
                  <tr key={asset.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-4 px-6">
                      <div className="text-sm font-bold text-slate-900">{asset.name}</div>
                      <div className="text-xs text-slate-500 font-mono mt-0.5">{asset.code}</div>
                    </td>
                    <td className="py-4 px-6">
                      <div className="text-sm text-slate-700">{ASSET_CATEGORY[asset.category]}</div>
                      <div className="text-xs text-slate-500 mt-0.5">Size: {asset.size}</div>
                    </td>
                    <td className="py-4 px-6">
                      <Badge variant={ASSET_STATUS[asset.status].variant}>{ASSET_STATUS[asset.status].label}</Badge>
                    </td>
                    <td className="py-4 px-6">
                      <div className="text-sm text-slate-700 flex items-center">
                        <CalendarDays size={14} className="mr-2 text-slate-400" />
                        {nextBookingLabel(asset)}
                      </div>
                      {asset.maintenanceBufferDays > 0 && (
                        <div className="text-[10px] text-amber-600 font-medium mt-1 flex items-center">
                          <Clock size={10} className="mr-1" /> Block thêm {asset.maintenanceBufferDays} ngày giặt hấp
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <CreateAssetModal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} onCreated={reload} />
    </div>
  );
}

function CreateAssetModal({ isOpen, onClose, onCreated }: { isOpen: boolean; onClose: () => void; onCreated: () => void }) {
  const showToast = useToast();
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      await assetsApi.create({
        code: String(fd.get('code')).toUpperCase(),
        name: String(fd.get('name')),
        category: fd.get('category') as AssetCategory,
        size: String(fd.get('size') || 'N/A'),
        maintenanceBufferDays: Number(fd.get('maintenanceBufferDays')) || 0,
      });
      showToast('Đã thêm sản phẩm mới!');
      onCreated();
      onClose();
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Thêm Sản Phẩm Mới">
      <form className="space-y-4" onSubmit={handleSubmit}>
        <Field label="Mã sản phẩm" required>
          <Input name="code" placeholder="VD: VAY-005" required />
        </Field>
        <Field label="Tên sản phẩm" required>
          <Input name="name" required />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Danh mục">
            <Select name="category">
              {Object.entries(ASSET_CATEGORY).map(([id, label]) => (
                <option key={id} value={id}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Kích cỡ">
            <Input name="size" placeholder="S, M, L..." />
          </Field>
        </div>
        <Field label="Thời gian bảo dưỡng / giặt hấp (Ngày)">
          <Input name="maintenanceBufferDays" type="number" min={0} max={7} defaultValue={1} />
          <p className="text-xs text-slate-400 mt-1">Hệ thống sẽ tự động block những ngày này sau khi khách trả đồ.</p>
        </Field>
        <Button type="submit" className="w-full mt-4" loading={submitting}>
          Lưu Sản Phẩm
        </Button>
      </form>
    </Modal>
  );
}
