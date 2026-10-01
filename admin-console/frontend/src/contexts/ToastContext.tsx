import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { AlertCircle, CheckCircle2, XCircle } from 'lucide-react';
import { cn } from '@/lib/cn';

type ToastType = 'success' | 'info' | 'error';
interface Toast {
  id: number;
  message: string;
  type: ToastType;
}

const ToastContext = createContext<(message: string, type?: ToastType) => void>(() => {});

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((message: string, type: ToastType = 'success') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 4000);
  }, []);

  return (
    <ToastContext.Provider value={showToast}>
      {children}
      <div className="fixed bottom-4 right-4 z-[70] flex flex-col gap-2">
        {toasts.map((toast) => (
          <div key={toast.id} className="animate-in slide-in-from-right-5 fade-in duration-300">
            <div
              className={cn(
                'flex items-center gap-3 px-4 py-3 rounded-lg shadow-lg border bg-white',
                toast.type === 'info' && 'border-blue-200 text-blue-800',
                toast.type === 'success' && 'border-emerald-200 text-emerald-800',
                toast.type === 'error' && 'border-red-200 text-red-800',
              )}
            >
              {toast.type === 'info' && <AlertCircle size={20} className="text-blue-500" />}
              {toast.type === 'success' && <CheckCircle2 size={20} className="text-emerald-500" />}
              {toast.type === 'error' && <XCircle size={20} className="text-red-500" />}
              <p className="text-sm font-medium">{toast.message}</p>
            </div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);
