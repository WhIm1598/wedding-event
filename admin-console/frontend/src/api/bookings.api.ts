import { env } from '@/config/env';
import { isWithin, monthRange, todayISO, weekRange } from '@/lib/date';
import { ApiError, http } from './http';
import { db, delay, nextId } from './mock/db';
import type { Booking, CalendarView, CreateBookingRequest } from '@/types';

export const bookingsApi = {
  /** GET /admin/bookings/calendar?view&date&staffId (FN-ADM-CAL-01) — cancelled bookings are hidden */
  getCalendar(view: CalendarView, date: string, staffId?: string): Promise<Booking[]> {
    if (!env.useMock) return http.get('/admin/bookings/calendar', { view, date, staffId });

    const range: [string, string] = view === 'day' ? [date, date] : view === 'week' ? weekRange(date) : monthRange(date);
    const result = db.bookings
      .filter((b) => isWithin(b.date, range) && b.status !== 'CANCELLED')
      .filter((b) => !staffId || b.assignedStaff.some((s) => s.id === staffId))
      .sort((a, b) => (a.date + a.time).localeCompare(b.date + b.time));
    return delay(result);
  },

  /** POST /admin/bookings (FN-ADM-CAL-02) — rejects with 409 STAFF_SCHEDULE_CONFLICT */
  async create(req: CreateBookingRequest): Promise<Booking> {
    if (!env.useMock) return http.post('/admin/bookings', req);

    if (req.eventDate < todayISO()) throw new ApiError(400, 'BAD_REQUEST', 'Ngày hẹn không được ở trong quá khứ');
    const conflict = db.bookings.find(
      (b) => b.date === req.eventDate && b.time === req.eventTime && b.assignedStaff.some((s) => req.staffIds.includes(s.id)),
    );
    if (conflict) {
      const names = conflict.assignedStaff.filter((s) => req.staffIds.includes(s.id)).map((s) => s.name).join(', ');
      throw new ApiError(409, 'STAFF_SCHEDULE_CONFLICT', `Nhân sự ${names} đã có lịch vào khung giờ này.`);
    }

    const pkg = db.packages.find((p) => p.id === req.servicePackageId);
    const booking: Booking = {
      id: nextId('bk'),
      date: req.eventDate,
      time: req.eventTime,
      clientName: req.clientName,
      phone: req.phone,
      type: req.type,
      venueAddress: req.venueAddress,
      packageName: pkg?.name ?? '—',
      status: 'UPCOMING',
      assignedStaff: db.staff.filter((s) => req.staffIds.includes(s.id)).map((s) => ({ id: s.id, name: s.name, role: s.position })),
    };
    db.bookings.push(booking);
    return delay(booking);
  },
};
