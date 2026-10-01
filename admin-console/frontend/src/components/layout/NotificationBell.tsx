import { useState } from 'react';
import { Bell } from 'lucide-react';
import { notificationsApi } from '@/api/system.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { timeAgo } from '@/lib/format';
import { cn } from '@/lib/cn';

export function NotificationBell() {
  const showToast = useToast();
  const [isOpen, setIsOpen] = useState(false);
  const { data: notifications = [], setData } = useAsync(() => notificationsApi.list());
  const unread = notifications.filter((n) => !n.read).length;

  const markRead = (id: string) => {
    setData((prev) => prev?.map((n) => (n.id === id ? { ...n, read: true } : n)));
    notificationsApi.markRead(id);
  };

  const markAllRead = () => {
    setData((prev) => prev?.map((n) => ({ ...n, read: true })));
    notificationsApi.markAllRead();
    showToast('Đã đánh dấu tất cả thông báo là đã đọc', 'info');
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={cn('relative p-2 transition-colors rounded-lg', isOpen ? 'bg-slate-100 text-slate-900' : 'text-slate-400 hover:text-slate-600')}
        aria-label="Thông báo"
      >
        <Bell size={20} />
        {unread > 0 && <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-rose-500 rounded-full border-2 border-white" />}
      </button>

      {isOpen && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />
          <div className="absolute top-full right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-200 z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
            <div className="p-4 border-b border-slate-100 flex items-center justify-between bg-slate-50">
              <h3 className="font-bold text-slate-900">Thông báo thông minh</h3>
              {unread > 0 && <span className="text-xs font-medium text-rose-600 bg-rose-50 px-2 py-1 rounded-full">{unread} chưa đọc</span>}
            </div>
            <div className="max-h-[300px] overflow-y-auto">
              {notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => markRead(n.id)}
                  className={cn('p-4 border-b border-slate-50 hover:bg-slate-50 transition-colors cursor-pointer', n.read ? 'opacity-60' : 'bg-rose-50/30')}
                >
                  <div className="flex justify-between items-start mb-1">
                    <p className={cn('text-sm', n.read ? 'font-medium text-slate-700' : 'font-bold text-slate-900')}>{n.title}</p>
                    {!n.read && <span className="w-2 h-2 rounded-full bg-rose-500 mt-1.5 shrink-0" />}
                  </div>
                  <p className="text-xs text-slate-500">{n.description}</p>
                  <p className="text-[10px] text-slate-400 mt-2">{timeAgo(n.createdAt)}</p>
                </div>
              ))}
            </div>
            <div className="p-2 text-center bg-slate-50 border-t border-slate-100">
              <button onClick={markAllRead} className="text-xs font-medium text-rose-600 hover:text-rose-700 disabled:opacity-50" disabled={unread === 0}>
                Đánh dấu tất cả đã đọc
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
