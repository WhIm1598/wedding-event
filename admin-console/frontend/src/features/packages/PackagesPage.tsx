import { useState, type FormEvent } from 'react';
import { CheckCircle2, Plus } from 'lucide-react';
import { errorMessage } from '@/api/http';
import { packagesApi } from '@/api/packages.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { ErrorState, Spinner } from '@/components/ui/States';
import { PACKAGE_TYPES } from '@/constants/labels';
import { formatVND, parseMoney } from '@/lib/format';
import { cn } from '@/lib/cn';
import type { ServicePackage } from '@/types';

export function PackagesPage() {
  const showToast = useToast();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const { data: packages = [], loading, error, reload, setData } = useAsync(() => packagesApi.list());

  const toggleActive = async (pkg: ServicePackage) => {
    try {
      const updated = await packagesApi.setActive(pkg.id, !pkg.isActive);
      setData((prev) => prev?.map((p) => (p.id === pkg.id ? updated : p)));
      showToast(updated.isActive ? `Đã mở bán "${pkg.name}"` : `Đã tạm ẩn "${pkg.name}"`, 'info');
    } catch (err) {
      showToast(errorMessage(err), 'error');
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Quản lý Gói Dịch Vụ"
        description="Thiết lập giá và chi tiết các gói chụp, makeup, thuê đồ."
        actions={
          <Button onClick={() => setIsCreateOpen(true)}>
            <Plus size={18} className="mr-2" /> Tạo Gói Mới
          </Button>
        }
      />

      {loading && <Spinner />}
      {error && <ErrorState error={error} onRetry={reload} />}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {packages.map((pkg) => (
          <Card key={pkg.id} className={cn('flex flex-col overflow-hidden hover:border-rose-300 transition-colors', !pkg.isActive && 'opacity-60')}>
            <div className="h-32 bg-slate-100 relative p-6 flex flex-col justify-end overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-br from-rose-100 to-slate-100 opacity-50" />
              <div className="relative z-10 flex justify-between items-end">
                <Badge variant="brand">{pkg.type}</Badge>
                <span className="text-2xl font-bold text-slate-900">{formatVND(pkg.price)}</span>
              </div>
            </div>
            <div className="p-6 flex-1 flex flex-col">
              <h3 className="text-lg font-bold text-slate-900 leading-tight mb-4">{pkg.name}</h3>
              <ul className="space-y-2 mb-6 flex-1">
                {pkg.features.map((feature) => (
                  <li key={feature} className="flex items-start text-sm text-slate-600">
                    <CheckCircle2 size={16} className="text-emerald-500 mr-2 mt-0.5 shrink-0" />
                    {feature}
                  </li>
                ))}
              </ul>
              <label className="flex items-center justify-between py-2 px-3 border border-slate-200 rounded-lg text-sm font-medium text-slate-700">
                {pkg.isActive ? 'Đang kinh doanh' : 'Tạm ngừng'}
                <input type="checkbox" checked={pkg.isActive} onChange={() => toggleActive(pkg)} className="accent-rose-600 w-4 h-4" />
              </label>
            </div>
          </Card>
        ))}
      </div>

      <CreatePackageModal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} onCreated={reload} />
    </div>
  );
}

function CreatePackageModal({ isOpen, onClose, onCreated }: { isOpen: boolean; onClose: () => void; onCreated: () => void }) {
  const showToast = useToast();
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      await packagesApi.create({
        name: String(fd.get('name')),
        price: parseMoney(fd.get('price')),
        type: String(fd.get('type')),
        features: String(fd.get('features')).split('\n').map((s) => s.trim()).filter(Boolean),
      });
      showToast('Đã tạo gói dịch vụ mới!');
      onCreated();
      onClose();
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Tạo Gói Dịch Vụ Mới">
      <form className="space-y-4" onSubmit={handleSubmit}>
        <Field label="Tên gói dịch vụ" required>
          <Input name="name" placeholder="VD: Gói Chụp Phóng Sự" required />
        </Field>
        <Field label="Giá trọn gói" required>
          <Input name="price" inputMode="numeric" placeholder="VD: 10.000.000" required />
        </Field>
        <Field label="Loại dịch vụ">
          <Select name="type">
            {PACKAGE_TYPES.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </Select>
        </Field>
        <Field label="Mô tả chi tiết (Mỗi dòng 1 mục)">
          <Textarea name="features" rows={4} placeholder="1 Thợ Chụp..." />
        </Field>
        <Button type="submit" className="w-full mt-4" loading={submitting}>
          Lưu Gói Dịch Vụ
        </Button>
      </form>
    </Modal>
  );
}
