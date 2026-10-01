import { useState, type FormEvent } from 'react';
import { Building, FileText, Users, type LucideIcon } from 'lucide-react';
import { errorMessage } from '@/api/http';
import { settingsApi } from '@/api/system.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { NAVIGATION } from '@/app/navigation';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field, Input, Textarea } from '@/components/ui/Form';
import { Spinner } from '@/components/ui/States';
import { cn } from '@/lib/cn';

type Section = 'studio' | 'contract' | 'roles';

const SECTIONS: Array<{ id: Section; label: string; icon: LucideIcon }> = [
  { id: 'studio', label: 'Thông tin Studio', icon: Building },
  { id: 'contract', label: 'Mẫu Hợp Đồng', icon: FileText },
  { id: 'roles', label: 'Phân Quyền', icon: Users },
];

export function SettingsPage() {
  const [section, setSection] = useState<Section>('studio');

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-slate-900">Cài Đặt Hệ Thống</h2>
        <p className="text-slate-500 text-sm">Cấu hình thông tin Studio, mẫu hợp đồng, và phân quyền.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="space-y-2">
          {SECTIONS.map((s) => (
            <button
              key={s.id}
              onClick={() => setSection(s.id)}
              className={cn(
                'w-full text-left px-4 py-3 rounded-xl flex items-center transition-colors',
                section === s.id ? 'bg-white shadow-sm border border-slate-200 font-bold text-rose-600' : 'hover:bg-slate-100 font-medium text-slate-700',
              )}
            >
              <s.icon size={18} className="mr-3" /> {s.label}
            </button>
          ))}
        </div>

        <Card className="p-6 md:col-span-2 space-y-5">
          {section === 'studio' && <StudioForm />}
          {section === 'contract' && <ContractTemplateInfo />}
          {section === 'roles' && <RolesMatrix />}
        </Card>
      </div>
    </div>
  );
}

function StudioForm() {
  const showToast = useToast();
  const [saving, setSaving] = useState(false);
  const { data, loading } = useAsync(() => settingsApi.get());
  if (loading || !data) return <Spinner />;

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSaving(true);
    try {
      await settingsApi.update({
        name: String(fd.get('name')),
        address: String(fd.get('address')),
        taxCode: String(fd.get('taxCode')),
        legalRepresentative: String(fd.get('legalRepresentative')),
        bankInfo: String(fd.get('bankInfo')),
      });
      showToast('Đã lưu cấu hình');
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="space-y-5" onSubmit={handleSubmit}>
      <h3 className="text-lg font-bold text-slate-900 mb-4 border-b border-slate-100 pb-2">Thông tin Studio (Hiển thị trên HĐ)</h3>
      <Field label="Tên Studio">
        <Input name="name" defaultValue={data.name} />
      </Field>
      <Field label="Địa chỉ kinh doanh">
        <Input name="address" defaultValue={data.address} />
      </Field>
      <div className="grid grid-cols-2 gap-4">
        <Field label="Mã số thuế">
          <Input name="taxCode" defaultValue={data.taxCode} />
        </Field>
        <Field label="Đại diện pháp luật">
          <Input name="legalRepresentative" defaultValue={data.legalRepresentative} />
        </Field>
      </div>
      <Field label="Thông tin Ngân Hàng (Nhận cọc)">
        <Textarea name="bankInfo" rows={3} defaultValue={data.bankInfo} />
      </Field>
      <div className="pt-4 flex justify-end">
        <Button type="submit" variant="dark" className="px-6" loading={saving}>
          Lưu Thay Đổi
        </Button>
      </div>
    </form>
  );
}

function ContractTemplateInfo() {
  return (
    <div className="space-y-3 text-sm text-slate-600">
      <h3 className="text-lg font-bold text-slate-900 mb-4 border-b border-slate-100 pb-2">Mẫu Hợp Đồng A4</h3>
      <p>Hợp đồng được render khổ A4 (210 × 297mm) với các điều khoản bắt buộc:</p>
      <ul className="list-disc pl-5 space-y-1">
        <li>Đại diện Bên A (khách hàng) và Bên B (studio, mã số thuế, đại diện pháp luật)</li>
        <li>Nội dung công việc & quy cách bàn giao</li>
        <li>Thanh toán 3 đợt: 30% khi ký • 50% ngày chụp/cưới • 20% khi nhận album</li>
      </ul>
      <p className="text-xs text-slate-400">TODO: cho phép chỉnh sửa mẫu khi backend hỗ trợ template động.</p>
    </div>
  );
}

function RolesMatrix() {
  return (
    <div>
      <h3 className="text-lg font-bold text-slate-900 mb-4 border-b border-slate-100 pb-2">Ma trận Phân Quyền</h3>
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-slate-500 text-xs uppercase">
            <th className="py-2">Chức năng</th>
            <th className="py-2 text-center">Quản lý</th>
            <th className="py-2 text-center">Nhân viên</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {NAVIGATION.map((n) => (
            <tr key={n.path}>
              <td className="py-2 font-medium text-slate-700">{n.label}</td>
              <td className="py-2 text-center">{n.roles.includes('ROLE_ADMIN') ? '✅' : '—'}</td>
              <td className="py-2 text-center">{n.roles.includes('ROLE_STAFF') ? '✅' : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
