import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

const CONTROL = 'w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none bg-white';

export function Field({ label, required, children, className }: { label: string; required?: boolean; children: ReactNode; className?: string }) {
  return (
    <div className={className}>
      <label className="block text-sm font-medium text-slate-700 mb-1">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      {children}
    </div>
  );
}

export const Input = ({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) => <input className={cn(CONTROL, className)} {...props} />;

export const Select = ({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) => <select className={cn(CONTROL, className)} {...props} />;

export const Textarea = ({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) => <textarea className={cn(CONTROL, className)} {...props} />;
