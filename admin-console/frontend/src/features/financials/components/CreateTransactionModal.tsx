import { useState, type FormEvent } from 'react';
import { contractsApi } from '@/api/contracts.api';
import { financialsApi } from '@/api/financials.api';
import { errorMessage } from '@/api/http';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Button } from '@/components/ui/Button';
import { Field, Input, Select } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from '@/constants/labels';
import { todayISO } from '@/lib/date';
import { formatVND, parseMoney } from '@/lib/format';
import { cn } from '@/lib/cn';
import type { TransactionType } from '@/types';

/** Records income/expense (FN-ADM-FIN-01). INCOME linked to a contract reduces its remaining debt. */
export function CreateTransactionModal({ isOpen, onClose, onCreated }: { isOpen: boolean; onClose: () => void; onCreated: () => void }) {
  const showToast = useToast();
  const [type, setType] = useState<TransactionType>('INCOME');
  const [submitting, setSubmitting] = useState(false);
  const { data: contracts = [] } = useAsync(() => contractsApi.list(), [isOpen]);
  const openContracts = contracts.filter((c) => c.remainingAmount > 0);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      await financialsApi.create({
        type,
        amount: parseMoney(fd.get('amount')),
        category: String(fd.get('category')),
        description: String(fd.get('description')),
        contractId: type === 'INCOME' ? String(fd.get('contractId') || '') || null : null,
        transactionDate: String(fd.get('transactionDate')),
      });
      showToast(type === 'INCOME' ? 'Đã ghi nhận phiếu thu!' : 'Đã ghi nhận phiếu chi!');
      onCreated();
      onClose();
    } catch (err) {
      showToast(errorMessage(err), 'error'); // e.g. 400 PAYMENT_EXCEEDS_REMAINING
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Ghi Chép Thu / Chi">
      <form className="space-y-4" onSubmit={handleSubmit}>
        <div className="bg-slate-100 p-1 rounded-lg grid grid-cols-2 text-sm font-medium">
          {(['INCOME', 'EXPENSE'] as const).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setType(t)}
              className={cn('py-2 rounded-md transition-colors', type === t ? (t === 'INCOME' ? 'bg-white text-emerald-700 shadow-sm' : 'bg-white text-rose-700 shadow-sm') : 'text-slate-500')}
            >
              {t === 'INCOME' ? 'Khoản thu' : 'Khoản chi'}
            </button>
          ))}
        </div>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Số tiền" required>
            <Input name="amount" inputMode="numeric" placeholder="VD: 2.500.000" required />
          </Field>
          <Field label="Ngày giao dịch" required>
            <Input name="transactionDate" type="date" defaultValue={todayISO()} required />
          </Field>
        </div>
        <Field label="Loại khoản">
          <Select name="category" key={type}>
            {(type === 'INCOME' ? INCOME_CATEGORIES : EXPENSE_CATEGORIES).map((c) => (
              <option key={c}>{c}</option>
            ))}
          </Select>
        </Field>
        {type === 'INCOME' && (
          <Field label="Gắn với hợp đồng (tự động trừ công nợ)">
            <Select name="contractId">
              <option value="">— Không gắn hợp đồng —</option>
              {openContracts.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.contractNumber} • {c.customerName} • còn nợ {formatVND(c.remainingAmount)}
                </option>
              ))}
            </Select>
          </Field>
        )}
        <Field label="Nội dung" required>
          <Input name="description" placeholder="VD: Thanh toán cọc hợp đồng Lê Hoàng C" required />
        </Field>
        <Button type="submit" className="w-full mt-4" loading={submitting}>
          Lưu Giao Dịch
        </Button>
      </form>
    </Modal>
  );
}
