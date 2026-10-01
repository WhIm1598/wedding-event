import { useEffect, useState, type FormEvent } from 'react';
import { DollarSign, HeartHandshake, Package } from 'lucide-react';
import { contractsApi } from '@/api/contracts.api';
import { errorMessage } from '@/api/http';
import { packagesApi } from '@/api/packages.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Button } from '@/components/ui/Button';
import { Input, Select, Textarea } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { formatVND, parseMoney } from '@/lib/format';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onCreated: () => void;
}

const Label = ({ children, required }: { children: string; required?: boolean }) => (
  <label className="block text-xs font-medium text-slate-700 mb-1">
    {children} {required && <span className="text-red-500">*</span>}
  </label>
);

export function CreateContractModal({ isOpen, onClose, onCreated }: Props) {
  const showToast = useToast();
  const [submitting, setSubmitting] = useState(false);
  const { data: packages = [] } = useAsync(() => packagesApi.list(), [isOpen]);
  const [packageId, setPackageId] = useState('');
  const [total, setTotal] = useState(0);

  const activePackages = packages.filter((p) => p.isActive);

  // Default to the first package; total follows the selected package price
  useEffect(() => {
    if (!packageId && activePackages[0]) setPackageId(activePackages[0].id);
  }, [activePackages, packageId]);
  useEffect(() => {
    const pkg = packages.find((p) => p.id === packageId);
    if (pkg) setTotal(pkg.price);
  }, [packageId, packages]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const deposit = parseMoney(fd.get('depositAmount'));
    if (deposit > total) {
      showToast('Tiền cọc không được lớn hơn tổng giá trị hợp đồng', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await contractsApi.create({
        customerName: String(fd.get('customerName')),
        phone: String(fd.get('phone')),
        servicePackageId: packageId,
        totalAmount: total,
        depositAmount: deposit,
        notes: String(fd.get('notes') || ''),
      });
      showToast('Đã tạo hợp đồng thành công!');
      onCreated();
      onClose();
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Tạo Hợp Đồng Mới" width="max-w-xl">
      <form className="space-y-5" onSubmit={handleSubmit}>
        <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-4">
          <h3 className="font-bold text-slate-900 text-sm flex items-center">
            <HeartHandshake className="mr-2 text-rose-600" size={16} /> Thông tin khách hàng
          </h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label required>Họ tên cô dâu / chú rể</Label>
              <Input name="customerName" className="text-sm" required />
            </div>
            <div>
              <Label required>Số Zalo / Điện thoại</Label>
              <Input name="phone" type="tel" pattern="0[35789][0-9]{8}" className="text-sm" required />
            </div>
          </div>
        </div>

        <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-4">
          <h3 className="font-bold text-slate-900 text-sm flex items-center">
            <Package className="mr-2 text-rose-600" size={16} /> Chi tiết dịch vụ
          </h3>
          <div>
            <Label required>Chọn gói dịch vụ</Label>
            <Select value={packageId} onChange={(e) => setPackageId(e.target.value)} className="text-sm">
              {activePackages.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} - {formatVND(p.price)}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label>Ghi chú thêm (dịch vụ phát sinh)</Label>
            <Textarea name="notes" rows={2} className="text-sm" />
          </div>
        </div>

        <div className="bg-rose-50/50 p-4 rounded-xl border border-rose-100 space-y-4">
          <h3 className="font-bold text-rose-900 text-sm flex items-center">
            <DollarSign className="mr-2 text-rose-600" size={16} /> Thanh toán
          </h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label required>Tổng giá trị hợp đồng</Label>
              <Input
                value={total ? total.toLocaleString('vi-VN') : ''}
                onChange={(e) => setTotal(parseMoney(e.target.value))}
                inputMode="numeric"
                className="text-sm font-bold"
                required
              />
            </div>
            <div>
              <Label required>Tiền cọc đợt 1 (gợi ý 30%)</Label>
              <Input
                name="depositAmount"
                key={total}
                defaultValue={Math.round(total * 0.3).toLocaleString('vi-VN')}
                inputMode="numeric"
                className="text-sm font-bold text-rose-700 border-rose-300"
                required
              />
            </div>
          </div>
        </div>

        <Button type="submit" className="w-full py-3 rounded-xl font-bold shadow-md shadow-rose-200" loading={submitting}>
          Lưu & Tạo Hợp Đồng
        </Button>
      </form>
    </Modal>
  );
}
