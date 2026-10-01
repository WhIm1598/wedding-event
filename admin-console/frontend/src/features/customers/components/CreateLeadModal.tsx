import { useState, type FormEvent } from 'react';
import { crmApi } from '@/api/crm.api';
import { errorMessage } from '@/api/http';
import { packagesApi } from '@/api/packages.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Button } from '@/components/ui/Button';
import { Field, Input, Select } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';

export function CreateLeadModal({ isOpen, onClose, onCreated }: { isOpen: boolean; onClose: () => void; onCreated: () => void }) {
  const showToast = useToast();
  const [submitting, setSubmitting] = useState(false);
  const { data: packages = [] } = useAsync(() => packagesApi.list());

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      await crmApi.createLead({
        name: String(fd.get('name')),
        phone: String(fd.get('phone')),
        hasZalo: fd.get('hasZalo') === 'on',
        interest: String(fd.get('interest')),
      });
      showToast('Đã lưu dữ liệu thành công!');
      onCreated();
      onClose();
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Thêm Mới Khách / Lead">
      <form className="space-y-4" onSubmit={handleSubmit}>
        <Field label="Tên khách hàng" required>
          <Input name="name" placeholder="VD: Nguyễn Văn A" required />
        </Field>
        <Field label="Số điện thoại" required>
          <Input name="phone" type="tel" pattern="0[35789][0-9]{8}" placeholder="09xxxxxxxx" required />
          <label className="flex items-center mt-2 text-sm text-slate-600">
            <input name="hasZalo" type="checkbox" className="mr-2 accent-rose-600" defaultChecked />
            Số này có sử dụng Zalo
          </label>
        </Field>
        <div className="border-t border-slate-200 pt-4">
          <Field label="Gói dịch vụ quan tâm">
            <Select name="interest">
              <option>Chưa chọn gói</option>
              {packages.map((p) => (
                <option key={p.id}>{p.name}</option>
              ))}
            </Select>
          </Field>
        </div>
        <Button type="submit" className="w-full mt-4" loading={submitting}>
          Lưu Dữ Liệu
        </Button>
      </form>
    </Modal>
  );
}
