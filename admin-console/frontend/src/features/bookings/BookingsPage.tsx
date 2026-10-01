import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Plus } from 'lucide-react';
import { bookingsApi } from '@/api/bookings.api';
import { useAsync } from '@/hooks/useAsync';
import { Button } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { todayISO } from '@/lib/date';
import { cn } from '@/lib/cn';
import type { CalendarView } from '@/types';
import { MonthCalendar } from './components/MonthCalendar';
import { ScheduleList } from './components/ScheduleList';
import { CreateBookingModal } from './components/CreateBookingModal';

const VIEWS: Array<{ id: CalendarView; label: string }> = [
  { id: 'day', label: 'Ngày' },
  { id: 'week', label: 'Tuần' },
  { id: 'month', label: 'Tháng' },
];

export function BookingsPage() {
  const [params, setParams] = useSearchParams();
  const [selectedDate, setSelectedDate] = useState(todayISO());
  const [view, setView] = useState<CalendarView>('day');
  const isCreateOpen = params.get('create') === '1';
  const setCreateOpen = (open: boolean) => setParams(open ? { create: '1' } : {}, { replace: true });

  // Staff scoping (ROLE_STAFF sees own bookings only) is enforced by the backend from the JWT.
  const schedule = useAsync(() => bookingsApi.getCalendar(view, selectedDate), [view, selectedDate]);
  const month = useAsync(() => bookingsApi.getCalendar('month', selectedDate), [selectedDate.slice(0, 7)]);
  const bookedDates = useMemo(() => new Set((month.data ?? []).map((b) => b.date)), [month.data]);

  const handleCreated = () => {
    schedule.reload();
    month.reload();
  };

  return (
    <div className="space-y-6 h-full flex flex-col">
      <PageHeader
        title="Quản lý Lịch trình"
        description="Theo dõi lịch chụp, thử váy, và các cuộc hẹn khách hàng."
        actions={
          <>
            <div className="bg-slate-100 p-1 rounded-lg flex text-sm font-medium">
              {VIEWS.map((v) => (
                <button
                  key={v.id}
                  onClick={() => setView(v.id)}
                  className={cn('px-3 py-1.5 rounded-md transition-colors', view === v.id ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500')}
                >
                  {v.label}
                </button>
              ))}
            </div>
            <Button onClick={() => setCreateOpen(true)}>
              <Plus size={18} className="mr-2" /> Thêm Lịch Mới
            </Button>
          </>
        }
      />

      <div className="flex flex-col lg:flex-row gap-6 flex-1">
        <MonthCalendar selectedDate={selectedDate} bookedDates={bookedDates} onSelect={setSelectedDate} />
        <ScheduleList
          view={view}
          selectedDate={selectedDate}
          bookings={schedule.data ?? []}
          loading={schedule.loading}
          onCreate={() => setCreateOpen(true)}
        />
      </div>

      <CreateBookingModal isOpen={isCreateOpen} defaultDate={selectedDate} onClose={() => setCreateOpen(false)} onCreated={handleCreated} />
    </div>
  );
}
