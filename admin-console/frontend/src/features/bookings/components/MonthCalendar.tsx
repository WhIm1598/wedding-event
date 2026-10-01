import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { addMonths, parseISODate, toISODate } from '@/lib/date';
import { cn } from '@/lib/cn';

const WEEKDAYS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

interface Props {
  selectedDate: string;
  bookedDates: Set<string>;
  onSelect: (iso: string) => void;
}

export function MonthCalendar({ selectedDate, bookedDates, onSelect }: Props) {
  const d = parseISODate(selectedDate);
  const year = d.getFullYear();
  const month = d.getMonth();
  const leadingBlanks = new Date(year, month, 1).getDay();
  const days = Array.from({ length: new Date(year, month + 1, 0).getDate() }, (_, i) => toISODate(new Date(year, month, i + 1)));

  return (
    <Card className="p-6 lg:w-96 shrink-0 flex flex-col">
      <div className="flex justify-between items-center mb-6">
        <button onClick={() => onSelect(addMonths(selectedDate, -1))} className="p-2 hover:bg-slate-100 rounded-lg text-slate-600" aria-label="Tháng trước">
          <ChevronLeft size={20} />
        </button>
        <h3 className="text-lg font-bold text-slate-900">
          Tháng {month + 1}, {year}
        </h3>
        <button onClick={() => onSelect(addMonths(selectedDate, 1))} className="p-2 hover:bg-slate-100 rounded-lg text-slate-600" aria-label="Tháng sau">
          <ChevronRight size={20} />
        </button>
      </div>

      <div className="grid grid-cols-7 gap-1 text-center mb-2">
        {WEEKDAYS.map((day) => (
          <div key={day} className="text-xs font-bold text-slate-400 py-2">
            {day}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: leadingBlanks }, (_, i) => (
          <div key={`blank-${i}`} />
        ))}
        {days.map((iso) => {
          const isSelected = iso === selectedDate;
          const hasBooking = bookedDates.has(iso);
          return (
            <button
              key={iso}
              onClick={() => onSelect(iso)}
              className={cn(
                'relative p-2 h-10 w-full rounded-lg text-sm font-medium flex items-center justify-center transition-all',
                isSelected && 'bg-rose-600 text-white shadow-md',
                !isSelected && hasBooking && 'bg-rose-50 text-rose-700 font-bold border border-rose-100',
                !isSelected && !hasBooking && 'hover:bg-slate-100 text-slate-700',
              )}
            >
              {Number(iso.slice(8))}
              {hasBooking && !isSelected && <span className="absolute bottom-1 w-1 h-1 bg-rose-500 rounded-full" />}
            </button>
          );
        })}
      </div>

      <div className="mt-auto pt-6 border-t border-slate-100 flex items-center gap-4 text-xs text-slate-500">
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-slate-100 border border-slate-300" /> Trống lịch
        </div>
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-full bg-rose-500" /> Có lịch hẹn
        </div>
      </div>
    </Card>
  );
}
