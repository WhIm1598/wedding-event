// Date helpers operating on ISO date strings (YYYY-MM-DD), timezone-safe.

export const toISODate = (d: Date): string =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

export const parseISODate = (iso: string): Date => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
};

export const todayISO = (): string => toISODate(new Date());

export const addDays = (iso: string, days: number): string => {
  const d = parseISODate(iso);
  d.setDate(d.getDate() + days);
  return toISODate(d);
};

export const addMonths = (iso: string, months: number): string => {
  const d = parseISODate(iso);
  return toISODate(new Date(d.getFullYear(), d.getMonth() + months, 1));
};

/** Monday-based week range containing the date */
export const weekRange = (iso: string): [string, string] => {
  const d = parseISODate(iso);
  const offset = (d.getDay() + 6) % 7;
  const start = addDays(iso, -offset);
  return [start, addDays(start, 6)];
};

export const monthRange = (iso: string): [string, string] => {
  const d = parseISODate(iso);
  return [toISODate(new Date(d.getFullYear(), d.getMonth(), 1)), toISODate(new Date(d.getFullYear(), d.getMonth() + 1, 0))];
};

export const isWithin = (iso: string, [start, end]: [string, string]): boolean => iso >= start && iso <= end;
