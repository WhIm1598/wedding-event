import { Link } from 'react-router-dom';
import { Users } from 'lucide-react';
import { bookingsApi } from '@/api/bookings.api';
import { useAsync } from '@/hooks/useAsync';
import { Badge } from '@/components/ui/Badge';
import { Card } from '@/components/ui/Card';
import { EmptyState, Spinner } from '@/components/ui/States';
import { BOOKING_STATUS } from '@/constants/labels';
import { todayISO } from '@/lib/date';
import { splitTime12h } from '@/lib/format';
import { cn } from '@/lib/cn';

export function TodaySchedule({ wide, showStaff }: { wide: boolean; showStaff: boolean }) {
  const { data = [], loading } = useAsync(() => bookingsApi.getCalendar('day', todayISO()));
  const items = data.slice(0, wide ? 6 : 3);

  return (
    <Card className={cn('p-6', wide && 'lg:col-span-3')}>
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-lg font-bold text-slate-900">Lịch trình hôm nay</h3>
        <Link to="/bookings" className="text-rose-600 text-sm font-medium hover:text-rose-700">
          Xem tất cả
        </Link>
      </div>

      {loading && <Spinner />}
      {!loading && items.length === 0 && <EmptyState message="Hôm nay chưa có lịch trình." />}

      <div className={cn(wide ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4' : 'space-y-4')}>
        {items.map((item) => {
          const t = splitTime12h(item.time);
          const status = BOOKING_STATUS[item.status];
          return (
            <div key={item.id} className="flex gap-4 p-4 rounded-lg border border-slate-100 hover:border-rose-100 hover:bg-rose-50/30 transition-colors">
              <div className="flex flex-col items-center justify-center min-w-[60px] border-r border-slate-100 pr-4">
                <span className="text-sm font-bold text-slate-900">{t.time}</span>
                <span className="text-xs text-slate-500">{t.period}</span>
              </div>
              <div className="min-w-0">
                <p className="text-sm font-bold text-slate-900">{item.clientName}</p>
                <p className="text-xs text-slate-500 mb-2">{item.type}</p>
                <div className="flex flex-wrap items-center gap-2">
                  <Badge variant={status.variant}>{status.label}</Badge>
                  {showStaff && (
                    <span className="text-xs text-slate-400 flex items-center">
                      <Users size={12} className="mr-1" /> {item.assignedStaff.map((s) => s.name).join(', ')}
                    </span>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </Card>
  );
}
