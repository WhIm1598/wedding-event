const vndFormatter = new Intl.NumberFormat('vi-VN');

/** 25000000 -> "25.000.000đ" */
export const formatVND = (amount: number): string => `${vndFormatter.format(amount)}đ`;

/** 25000000 -> "25tr" (compact, for charts) */
export const formatMillions = (amount: number): string => `${Math.round(amount / 1_000_000)} Triệu`;

/** "2026-06-28" -> "28/06/2026" */
export const formatDate = (iso: string | null | undefined): string => {
  if (!iso) return '—';
  const [y, m, d] = iso.slice(0, 10).split('-');
  return `${d}/${m}/${y}`;
};

/** "14:30" -> { time: "02:30", period: "PM" } */
export const splitTime12h = (hhmm: string): { time: string; period: string } => {
  const [h, m] = hhmm.split(':').map(Number);
  const period = h >= 12 ? 'PM' : 'AM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return { time: `${String(hour12).padStart(2, '0')}:${String(m).padStart(2, '0')}`, period };
};

/** Relative time in Vietnamese, e.g. "10 phút trước" */
export const timeAgo = (iso: string): string => {
  const diffMin = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  if (diffMin < 60) return `${diffMin} phút trước`;
  const diffHour = Math.round(diffMin / 60);
  if (diffHour < 24) return `${diffHour} giờ trước`;
  return `${Math.round(diffHour / 24)} ngày trước`;
};

/** Parses user-typed money like "25.000.000đ" or "25000000" */
export const parseMoney = (raw: FormDataEntryValue | null): number =>
  Number(String(raw ?? '').replace(/[^\d]/g, '')) || 0;
