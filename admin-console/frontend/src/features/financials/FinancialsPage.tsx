import { useSearchParams } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { financialsApi } from '@/api/financials.api';
import { useAsync } from '@/hooks/useAsync';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { ErrorState, Spinner } from '@/components/ui/States';
import { formatDate, formatVND } from '@/lib/format';
import { CreateTransactionModal } from './components/CreateTransactionModal';

export function FinancialsPage() {
  const [params, setParams] = useSearchParams();
  const isCreateOpen = params.get('create') === '1';
  const summary = useAsync(() => financialsApi.getSummary());
  const transactions = useAsync(() => financialsApi.listTransactions());

  const cards = summary.data
    ? [
        { label: `Tổng Thu (${summary.data.periodLabel})`, value: summary.data.totalIncome, className: 'from-emerald-500 to-emerald-600', sub: 'text-emerald-100' },
        { label: 'Tổng Chi Phí Thực Tế', value: summary.data.totalExpense, className: 'from-rose-500 to-rose-600', sub: 'text-rose-100' },
        { label: 'Lợi Nhuận Tạm Tính', value: summary.data.netProfit, className: 'from-blue-500 to-blue-600', sub: 'text-blue-100' },
      ]
    : [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Kế Toán & Chi Phí"
        description="Theo dõi thu chi, lợi nhuận thực tế và các khoản outsource."
        actions={
          <Button onClick={() => setParams({ create: '1' })}>
            <Plus size={18} className="mr-2" /> Ghi Chép Mới
          </Button>
        }
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {cards.map((c) => (
          <Card key={c.label} className={`p-6 bg-gradient-to-br text-white border-0 ${c.className}`}>
            <p className={`font-medium mb-1 ${c.sub}`}>{c.label}</p>
            <h3 className="text-3xl font-bold">{formatVND(c.value)}</h3>
          </Card>
        ))}
      </div>

      <Card className="overflow-hidden">
        <div className="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <h3 className="font-bold text-slate-900">Giao dịch gần đây</h3>
        </div>
        {transactions.loading && <Spinner />}
        {transactions.error && <ErrorState error={transactions.error} onRetry={transactions.reload} />}
        {transactions.data && (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 text-xs uppercase tracking-wider">
                  <th className="py-3 px-6">Ngày</th>
                  <th className="py-3 px-6">Nội dung</th>
                  <th className="py-3 px-6">Loại khoản</th>
                  <th className="py-3 px-6 text-right">Số tiền</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {transactions.data.map((trx) => {
                  const isIncome = trx.type === 'INCOME';
                  return (
                    <tr key={trx.id} className="hover:bg-slate-50/50">
                      <td className="py-3 px-6 text-sm text-slate-600 whitespace-nowrap">{formatDate(trx.transactionDate)}</td>
                      <td className="py-3 px-6">
                        <p className="text-sm font-bold text-slate-900">{trx.description}</p>
                        <p className="text-xs text-slate-500">{trx.code}</p>
                      </td>
                      <td className="py-3 px-6">
                        <Badge variant={isIncome ? 'success' : 'danger'}>{trx.category}</Badge>
                      </td>
                      <td className={`py-3 px-6 text-right font-bold text-sm whitespace-nowrap ${isIncome ? 'text-emerald-600' : 'text-rose-600'}`}>
                        {isIncome ? '+' : '-'}
                        {formatVND(trx.amount)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <CreateTransactionModal
        isOpen={isCreateOpen}
        onClose={() => setParams({}, { replace: true })}
        onCreated={() => {
          transactions.reload();
          summary.reload();
        }}
      />
    </div>
  );
}
