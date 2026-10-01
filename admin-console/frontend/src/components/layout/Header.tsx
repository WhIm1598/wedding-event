import { FileText, Menu } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { GlobalSearch } from './GlobalSearch';
import { NotificationBell } from './NotificationBell';

export function Header({ onOpenMenu, onCreateContract }: { onOpenMenu: () => void; onCreateContract: () => void }) {
  const { isAdmin } = useAuth();

  return (
    <header className="bg-white/80 backdrop-blur-md border-b border-slate-200 h-16 flex items-center justify-between px-4 sm:px-6 flex-shrink-0 z-10">
      <div className="flex items-center gap-4 flex-1">
        <button className="md:hidden p-2 text-slate-500 hover:bg-slate-100 rounded-lg transition-colors" onClick={onOpenMenu} aria-label="Mở menu">
          <Menu size={20} />
        </button>
        <GlobalSearch />
      </div>

      <div className="flex items-center gap-3 sm:gap-4">
        <NotificationBell />
        {isAdmin && (
          <button
            onClick={onCreateContract}
            className="bg-slate-900 hover:bg-slate-800 text-white text-sm font-medium px-4 py-2 rounded-lg transition-all shadow-sm hidden sm:flex items-center gap-2"
          >
            <FileText size={16} /> Tạo Hợp Đồng
          </button>
        )}
      </div>
    </header>
  );
}
