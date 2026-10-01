import type { ReactNode } from 'react';
import { AlertCircle } from 'lucide-react';

export function Spinner({ label = 'Đang tải...' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-3 py-12 text-slate-400 text-sm">
      <span className="w-5 h-5 border-2 border-rose-500 border-t-transparent rounded-full animate-spin" />
      {label}
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: Error; onRetry?: () => void }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-12 text-red-600 text-sm">
      <AlertCircle size={32} />
      <p>{error.message || 'Đã có lỗi xảy ra'}</p>
      {onRetry && (
        <button onClick={onRetry} className="text-rose-600 font-medium hover:underline">
          Thử lại
        </button>
      )}
    </div>
  );
}

export function EmptyState({ icon, message, action }: { icon?: ReactNode; message: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center text-slate-400 space-y-3 py-12">
      {icon}
      <p>{message}</p>
      {action}
    </div>
  );
}
