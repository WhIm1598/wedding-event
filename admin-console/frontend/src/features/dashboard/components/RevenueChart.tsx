import { useState } from 'react';
import { dashboardApi } from '@/api/dashboard.api';
import { useAsync } from '@/hooks/useAsync';
import { Card } from '@/components/ui/Card';
import { Spinner } from '@/components/ui/States';
import { formatMillions } from '@/lib/format';
import { cn } from '@/lib/cn';

export function RevenueChart({ className }: { className?: string }) {
  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const { data = [], loading } = useAsync(() => dashboardApi.getRevenueChart(year), [year]);
  const max = Math.max(1, ...data.map((p) => p.amount));

  return (
    <Card className={cn('p-6', className)}>
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-lg font-bold text-slate-900">Tổng quan doanh thu</h3>
        <select
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
          className="bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-1.5 outline-none focus:ring-2 focus:ring-rose-500"
        >
          <option value={currentYear}>Năm nay</option>
          <option value={currentYear - 1}>Năm ngoái</option>
        </select>
      </div>

      {loading ? (
        <Spinner />
      ) : (
        <>
          <div className="h-64 flex items-end justify-between gap-2 pt-4">
            {data.map((point) => (
              <div key={point.month} className="w-full bg-slate-100 rounded-t-sm relative group h-full flex items-end">
                <div className="w-full bg-rose-200 hover:bg-rose-400 transition-colors rounded-t-sm" style={{ height: `${(point.amount / max) * 100}%` }} />
                <div className="opacity-0 group-hover:opacity-100 absolute -top-8 left-1/2 -translate-x-1/2 bg-slate-800 text-white text-xs py-1 px-2 rounded pointer-events-none transition-opacity whitespace-nowrap z-10">
                  {formatMillions(point.amount)}
                </div>
              </div>
            ))}
          </div>
          <div className="flex justify-between mt-2 text-xs text-slate-400 font-medium px-1">
            {data.map((p) => (
              <span key={p.month}>T{p.month}</span>
            ))}
          </div>
        </>
      )}
    </Card>
  );
}
