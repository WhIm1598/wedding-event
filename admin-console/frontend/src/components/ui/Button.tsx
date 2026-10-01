import type { ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

type Variant = 'primary' | 'dark' | 'secondary' | 'outline' | 'ghost';

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-rose-600 hover:bg-rose-700 text-white shadow-sm',
  dark: 'bg-slate-900 hover:bg-slate-800 text-white shadow-sm',
  secondary: 'bg-rose-50 hover:bg-rose-100 text-rose-700',
  outline: 'border border-slate-200 hover:bg-slate-50 text-slate-700',
  ghost: 'text-slate-600 hover:bg-slate-100',
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  loading?: boolean;
}

export function Button({ variant = 'primary', loading, className, children, disabled, ...props }: ButtonProps) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center px-4 py-2 rounded-lg font-medium transition-colors disabled:opacity-60 disabled:cursor-not-allowed',
        VARIANTS[variant],
        className,
      )}
      disabled={disabled || loading}
      {...props}
    >
      {loading && <span className="w-4 h-4 mr-2 border-2 border-current border-t-transparent rounded-full animate-spin" />}
      {children}
    </button>
  );
}
