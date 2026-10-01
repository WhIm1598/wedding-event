import { NavLink } from 'react-router-dom';
import { Camera, LogOut, X } from 'lucide-react';
import { NAVIGATION } from '@/app/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { cn } from '@/lib/cn';

export function Sidebar({ isMobileOpen, onClose }: { isMobileOpen: boolean; onClose: () => void }) {
  const { user, logout } = useAuth();
  if (!user) return null;

  const items = NAVIGATION.filter((nav) => nav.roles.includes(user.role));

  return (
    <aside
      className={cn(
        'fixed inset-y-0 left-0 z-30 w-64 bg-white border-r border-slate-200 flex flex-col flex-shrink-0 transition-transform duration-300 ease-in-out md:static md:translate-x-0',
        isMobileOpen ? 'translate-x-0 shadow-2xl' : '-translate-x-full',
      )}
    >
      <div className="p-6 flex items-center gap-3">
        <div className="bg-rose-600 text-white p-2 rounded-lg shadow-sm">
          <Camera size={24} />
        </div>
        <div>
          <h1 className="font-bold text-xl tracking-tight leading-tight">Lumière</h1>
          <p className="text-[10px] text-slate-500 uppercase tracking-widest font-medium">Studios</p>
        </div>
        <button className="md:hidden ml-auto text-slate-400 hover:text-slate-600" onClick={onClose} aria-label="Đóng menu">
          <X size={20} />
        </button>
      </div>

      <nav className="flex-1 px-4 py-2 space-y-1.5 overflow-y-auto">
        {items.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            onClick={onClose}
            className={({ isActive }) =>
              cn(
                'w-full flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200',
                isActive ? 'bg-rose-50 text-rose-700 shadow-sm border border-rose-100/50' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900',
              )
            }
          >
            {({ isActive }) => (
              <>
                <item.icon size={18} className={isActive ? 'text-rose-600' : 'text-slate-400'} />
                {item.label}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      <div className="p-4 border-t border-slate-100 bg-slate-50/50">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center font-bold text-rose-700 shrink-0 border border-rose-200">
            {user.name.charAt(0)}
          </div>
          <div className="flex-1 overflow-hidden">
            <p className="text-sm font-bold truncate text-slate-900">{user.name}</p>
            <p className="text-xs text-emerald-600 font-medium truncate flex items-center gap-1">
              <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full" /> Online
            </p>
          </div>
          <button onClick={logout} className="text-slate-400 hover:text-red-600 transition-colors p-2" title="Đăng xuất">
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </aside>
  );
}
