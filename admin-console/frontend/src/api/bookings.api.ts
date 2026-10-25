import { env } from '@/config/env';
import { isWithin, monthRange, todayISO, weekRange } from '@/lib/date';
import { BOOKING_NEXT_STEPS } from '@/constants/labels';
import { ApiError, http } from './http';
import { db, delay, nextId } from './mock/db';
import type { Booking, BookingProgressStatus, CalendarView, CreateBookingRequest } from '@/types';

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

  /** POST /admin/bookings (FN-ADM-CAL-02) — rejects with 409 STAFF_SCHEDULE_CONFLICT; a contract fills phone and package */
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
    const contract = req.contractId ? db.contracts.find((c) => c.id === req.contractId) : undefined;
    if (req.contractId && !contract) throw new ApiError(404, 'NOT_FOUND', 'Không tìm thấy hợp đồng');

    const pkg = db.packages.find((p) => p.id === req.servicePackageId);
    const booking: Booking = {
      id: nextId('bk'),
      date: req.eventDate,
      time: req.eventTime,
      clientName: req.clientName,
      phone: req.phone || contract?.phone,
      type: req.type,
      venueAddress: req.venueAddress,
      packageName: pkg?.name ?? contract?.packageName ?? '—',
      status: 'UPCOMING',
      assignedStaff: db.staff.filter((s) => req.staffIds.includes(s.id)).map((s) => ({ id: s.id, name: s.name, role: s.position })),
      contractId: contract?.id ?? null,
    };
    db.bookings.push(booking);
    return delay(booking);
  },

  /**
   * PATCH /admin/bookings/{id}/status (FN-ADM-CAL-04) — 409 INVALID_STATUS_TRANSITION outside §1.6; the first booking
   * of an effective contract going IN_PROGRESS starts the contract. Staff scoping (403) is enforced by the backend.
   */
  updateStatus(id: string, status: BookingProgressStatus): Promise<Booking> {
    if (!env.useMock) return http.patch(`/admin/bookings/${id}/status`, { status });

    const booking = db.bookings.find((b) => b.id === id);
    if (!booking) throw new ApiError(404, 'NOT_FOUND', 'Không tìm thấy lịch hẹn');
    if (!BOOKING_NEXT_STEPS[booking.status].some((s) => s.status === status)) {
      throw new ApiError(409, 'INVALID_STATUS_TRANSITION', `Không thể chuyển lịch hẹn từ trạng thái ${booking.status} sang ${status}`);
    }
    booking.status = status;
    const contract = db.contracts.find((c) => c.id === booking.contractId);
    if (status === 'IN_PROGRESS' && contract?.status === 'DEPOSITED') contract.status = 'IN_PROGRESS';
    return delay(booking, 150);
  },
};
