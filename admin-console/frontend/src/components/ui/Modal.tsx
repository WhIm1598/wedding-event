import { useEffect, type ReactNode } from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/cn';

interface OverlayProps {
  isOpen: boolean;
  onClose: () => void;
  title: ReactNode;
  children: ReactNode;
  width?: string;
  footer?: ReactNode;
}

const useEscape = (isOpen: boolean, onClose: () => void) =>
  useEffect(() => {
    if (!isOpen) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [isOpen, onClose]);

/** Centered modal used for "create" actions */
export function Modal({ isOpen, onClose, title, children, width = 'max-w-md', footer }: OverlayProps) {
  useEscape(isOpen, onClose);
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-[60] overflow-hidden flex items-center justify-center p-4 sm:p-6">
      <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={onClose} />
      <div className={cn('relative bg-white rounded-2xl shadow-2xl w-full max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-200', width)}>
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <h2 className="text-lg font-bold text-slate-900 flex items-center">{title}</h2>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200 transition-colors" aria-label="Đóng">
            <X size={20} />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-6">{children}</div>
        {footer && <div className="p-4 bg-white border-t border-slate-200 flex justify-end gap-3">{footer}</div>}
      </div>
    </div>
  );
}

/** Right-hand slide-over panel used for detail views */
export function SlideOver({ isOpen, onClose, title, children, width = 'max-w-md' }: OverlayProps) {
  useEscape(isOpen, onClose);
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm" onClick={onClose} />
      <div className={cn('fixed inset-y-0 right-0 w-full flex', width)}>
        <div className="w-full h-full bg-white shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">
          <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
            <h2 className="text-lg font-bold text-slate-900">{title}</h2>
            <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200 transition-colors" aria-label="Đóng">
              <X size={20} />
            </button>
          </div>
          <div className="flex-1 overflow-y-auto p-6">{children}</div>
        </div>
      </div>
    </div>
  );
}
