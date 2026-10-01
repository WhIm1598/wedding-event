import { useState, type FormEvent } from 'react';
import { Plus, Users } from 'lucide-react';
import { errorMessage } from '@/api/http';
import { staffApi } from '@/api/staff.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field, Input, Select } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { ErrorState, Spinner } from '@/components/ui/States';
import { STAFF_POSITIONS, STAFF_STATUS } from '@/constants/labels';
import type { StaffMember } from '@/types';
import { StaffDetailPanel } from './components/StaffDetailPanel';

export function StaffPage() {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selected, setSelected] = useState<StaffMember | null>(null);
  const { data: staff = [], loading, error, reload } = useAsync(() => staffApi.list());

  return (
    <div className="space-y-6 relative">
      <PageHeader
        title="Quản Lý Nhân Sự"
        description="Quản lý hồ sơ nhân viên, thợ chụp, chuyên viên makeup."
        actions={
          <Button onClick={() => setIsCreateOpen(true)}>
            <Plus size={18} className="mr-2" /> Thêm Nhân Sự
          </Button>
        }
      />

      {loading && <Spinner />}
      {error && <ErrorState error={error} onRetry={reload} />}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {staff.map((member) => (
          <Card key={member.id} className="p-6 text-center hover:shadow-md transition-shadow">
            <div className="w-20 h-20 mx-auto bg-slate-100 rounded-full flex items-center justify-center mb-4">
              <Users size={32} className="text-slate-400" />
            </div>
            <h3 className="text-lg font-bold text-slate-900">{member.name}</h3>
            <p className="text-sm text-rose-600 font-medium mb-3">{member.position}</p>
            <Badge variant={STAFF_STATUS[member.workStatus].variant}>{STAFF_STATUS[member.workStatus].label}</Badge>
            <div className="mt-4 pt-4 border-t border-slate-100">
              <button
                onClick={() => setSelected(member)}
                className="w-full py-2 bg-slate-50 hover:bg-rose-50 text-slate-700 hover:text-rose-700 text-sm font-medium rounded-lg transition-colors"
              >
                Chi tiết & Giao Việc
              </button>
            </div>
          </Card>
        ))}
      </div>

      <CreateStaffModal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} onCreated={reload} />
      <StaffDetailPanel staff={selected} onClose={() => setSelected(null)} />
    </div>
  );
}

function CreateStaffModal({ isOpen, onClose, onCreated }: { isOpen: boolean; onClose: () => void; onCreated: () => void }) {
  const showToast = useToast();
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      await staffApi.create({ name: String(fd.get('name')), phone: String(fd.get('phone')), position: String(fd.get('position')) });
      showToast('Đã thêm nhân sự thành công!');
      onCreated();
      onClose();
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Thêm Nhân Sự Mới">
      <form className="space-y-4" onSubmit={handleSubmit}>
        <Field label="Họ và tên" required>
          <Input name="name" required />
        </Field>
        <Field label="Số điện thoại" required>
          <Input name="phone" type="tel" pattern="0[35789][0-9]{8}" placeholder="09xxxxxxxx" required />
        </Field>
        <Field label="Vị trí công việc">
          <Select name="position">
            {STAFF_POSITIONS.map((p) => (
              <option key={p}>{p}</option>
            ))}
          </Select>
        </Field>
        <Button type="submit" className="w-full mt-4" loading={submitting}>
          Lưu Hồ Sơ
        </Button>
      </form>
    </Modal>
  );
}
