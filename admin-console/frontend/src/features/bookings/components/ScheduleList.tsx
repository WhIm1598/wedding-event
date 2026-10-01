import { CalendarDays, Camera, Clock, MapPin, Package, Users } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Card } from '@/components/ui/Card';
import { EmptyState, Spinner } from '@/components/ui/States';
import { BOOKING_STATUS } from '@/constants/labels';
import { monthRange, weekRange } from '@/lib/date';
import { formatDate, splitTime12h } from '@/lib/format';
import type { Booking, CalendarView } from '@/types';

interface Props {
  view: CalendarView;
  selectedDate: string;
  bookings: Booking[];
  loading: boolean;
  onCreate: () => void;
}

const titleFor = (view: CalendarView, date: string): string => {
  if (view === 'day') return `Lịch trình ngày ${formatDate(date)}`;
  const [start, end] = view === 'week' ? weekRange(date) : monthRange(date);
  return `Lịch trình ${view === 'week' ? 'tuần' : 'tháng'} ${formatDate(start)} – ${formatDate(end)}`;
};

export function ScheduleList({ view, selectedDate, bookings, loading, onCreate }: Props) {
  return (
    <Card className="flex-1 overflow-hidden flex flex-col">
      <div className="border-b border-slate-200 p-6 bg-slate-50 flex items-center gap-3">
        <CalendarDays className="text-rose-600" size={24} />
        <div>
          <h3 className="text-lg font-bold text-slate-900">{titleFor(view, selectedDate)}</h3>
          <p className="text-sm text-slate-500">{bookings.length} công việc được xếp lịch</p>
        </div>
      </div>

      <div className="p-6 overflow-y-auto flex-1 bg-slate-50/50">
        {loading ? (
          <Spinner />
        ) : bookings.length === 0 ? (
          <EmptyState
            icon={<Clock size={48} className="text-slate-200" />}
            message="Không có lịch trình nào trong khoảng thời gian này."
            action={
              <button onClick={onCreate} className="text-rose-600 font-medium hover:underline text-sm">
                + Thêm lịch mới ngay
              </button>
            }
          />
        ) : (
          <div className="space-y-4">
            {bookings.map((booking) => (
              <BookingCard key={booking.id} booking={booking} showDate={view !== 'day'} />
            ))}
          </div>
        )}
      </div>
    </Card>
  );
}

function BookingCard({ booking, showDate }: { booking: Booking; showDate: boolean }) {
  const t = splitTime12h(booking.time);
  const status = BOOKING_STATUS[booking.status];
  return (
    <div className="flex flex-col md:flex-row gap-4 md:gap-6 p-4 rounded-xl border border-slate-200 hover:shadow-md transition-shadow bg-white relative overflow-hidden">
      <div className="absolute left-0 top-0 bottom-0 w-1 bg-rose-500" />
      <div className="md:w-32 shrink-0 flex flex-col justify-center items-center p-3 bg-slate-50 rounded-lg border border-slate-100">
        {showDate && <span className="text-[11px] text-rose-600 font-bold">{formatDate(booking.date)}</span>}
        <span className="text-lg text-slate-900 font-bold">{t.time}</span>
        <span className="text-xs text-slate-500 mt-1 font-medium">{t.period}</span>
      </div>
      <div className="flex-1 flex flex-col justify-center">
        <div className="flex items-start justify-between gap-2">
          <div>
            <h4 className="text-base font-bold text-slate-900">Khách: {booking.clientName}</h4>
            <p className="text-sm text-rose-600 font-medium flex items-center mt-1">
              <Camera size={14} className="mr-1" /> {booking.type}
            </p>
          </div>
          <Badge variant={status.variant}>{status.label}</Badge>
        </div>
        <div className="mt-4 pt-3 border-t border-slate-100 flex flex-wrap gap-4 text-sm text-slate-500">
          <span className="flex items-center">
            <Users size={14} className="mr-1.5" /> Phụ trách: <strong className="ml-1">{booking.assignedStaff.map((s) => s.name).join(', ') || '—'}</strong>
          </span>
          <span className="flex items-center">
            <Package size={14} className="mr-1.5" /> Gói DV: {booking.packageName}
          </span>
          {booking.venueAddress && (
            <span className="flex items-center">
              <MapPin size={14} className="mr-1.5" /> {booking.venueAddress}
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
