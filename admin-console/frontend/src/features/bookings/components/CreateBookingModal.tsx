import { useState, type FormEvent } from 'react';
import { bookingsApi } from '@/api/bookings.api';
import { packagesApi } from '@/api/packages.api';
import { staffApi } from '@/api/staff.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Button } from '@/components/ui/Button';
import { Field, Input, Select } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { BOOKING_TYPES } from '@/constants/labels';

interface Props {
  isOpen: boolean;
  defaultDate: string;
  onClose: () => void;
  onCreated: () => void;
}

export function CreateBookingModal({ isOpen, defaultDate, onClose, onCreated }: Props) {
  const showToast = useToast();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string>();
  const { data: staff = [] } = useAsync(() => staffApi.list());
  const { data: packages = [] } = useAsync(() => packagesApi.list());

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    setError(undefined);
    try {
      await bookingsApi.create({
        clientName: String(fd.get('clientName')),
        phone: String(fd.get('phone') || ''),
        type: String(fd.get('type')),
        servicePackageId: String(fd.get('servicePackageId')),
        eventDate: String(fd.get('eventDate')),
        eventTime: String(fd.get('eventTime')),
        venueAddress: String(fd.get('venueAddress') || ''),
        staffIds: fd.getAll('staffIds').map(String),
      });
      showToast('Đã thêm lịch trình thành công!');
      onCreated();
      onClose();
    } catch (err) {
      setError((err as Error).message); // e.g. 409 STAFF_SCHEDULE_CONFLICT
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Thêm Lịch Trình Mới">
      <form className="space-y-4" onSubmit={handleSubmit}>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Tên khách hàng" required>
            <Input name="clientName" placeholder="VD: Lan & Hoàng" required />
          </Field>
          <Field label="Số điện thoại">
            <Input name="phone" type="tel" pattern="0[35789][0-9]{8}" placeholder="09xxxxxxxx" />
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Ngày hẹn" required>
            <Input name="eventDate" type="date" defaultValue={defaultDate} required />
          </Field>
          <Field label="Giờ hẹn" required>
            <Input name="eventTime" type="time" defaultValue="09:00" required />
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Loại lịch hẹn">
            <Select name="type">
              {BOOKING_TYPES.map((t) => (
                <option key={t}>{t}</option>
              ))}
            </Select>
          </Field>
          <Field label="Gói dịch vụ">
            <Select name="servicePackageId">
              {packages.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <Field label="Địa điểm">
          <Input name="venueAddress" placeholder="VD: Studio Lumière chi nhánh 1" />
        </Field>
        <Field label="Nhân viên phụ trách">
          <div className="grid grid-cols-2 gap-2 p-3 border border-slate-200 rounded-lg">
            {staff.map((s) => (
              <label key={s.id} className="flex items-center text-sm text-slate-700">
                <input type="checkbox" name="staffIds" value={s.id} className="mr-2 accent-rose-600" />
                {s.name}
              </label>
            ))}
          </div>
        </Field>
        {error && <p className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg p-3">{error}</p>}
        <Button type="submit" className="w-full mt-4" loading={submitting}>
          Lưu Lịch Trình
        </Button>
      </form>
    </Modal>
  );
}
