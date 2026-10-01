import type { ReactNode } from 'react';

export type BadgeVariant = 'default' | 'success' | 'warning' | 'danger' | 'brand' | 'info';

const VARIANTS: Record<BadgeVariant, string> = {
  default: 'bg-slate-100 text-slate-700',
  success: 'bg-emerald-100 text-emerald-700',
  warning: 'bg-amber-100 text-amber-700',
  danger: 'bg-red-100 text-red-700',
  brand: 'bg-rose-100 text-rose-700',
  info: 'bg-blue-100 text-blue-700',
};

export function Badge({ children, variant = 'default' }: { children: ReactNode; variant?: BadgeVariant }) {
  return <span className={`px-2.5 py-1 rounded-full text-xs font-medium whitespace-nowrap ${VARIANTS[variant]}`}>{children}</span>;
}
