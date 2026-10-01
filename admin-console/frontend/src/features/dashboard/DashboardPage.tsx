import { useNavigate } from 'react-router-dom';
import { CalendarDays, CreditCard, Plus } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useLayout } from '@/components/layout/AppLayout';
import { StatCards } from './components/StatCards';
import { RevenueChart } from './components/RevenueChart';
import { TodaySchedule } from './components/TodaySchedule';

export function DashboardPage() {
  const { user, isAdmin } = useAuth();
  const { openCreateContract } = useLayout();
  const navigate = useNavigate();

  return (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Xin chào, {user?.name ?? 'Admin'}! 👋</h2>
          <p className="text-slate-500 mt-1">Chúc bạn một ngày làm việc hiệu quả. Dưới đây là tổng quan tình hình studio.</p>
        </div>

        <div className="flex gap-2 overflow-x-auto pb-2 md:pb-0">
          {isAdmin && (
            <button onClick={openCreateContract} className="shrink-0 flex items-center px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg text-sm font-bold transition-colors">
              <Plus size={16} className="mr-2" /> Tạo Hợp Đồng
            </button>
          )}
          <button onClick={() => navigate('/bookings?create=1')} className="shrink-0 flex items-center px-4 py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded-lg text-sm font-bold transition-colors">
            <CalendarDays size={16} className="mr-2" /> Thêm Lịch
          </button>
          {isAdmin && (
            <button onClick={() => navigate('/financials?create=1')} className="shrink-0 flex items-center px-4 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-lg text-sm font-bold transition-colors">
              <CreditCard size={16} className="mr-2" /> Thu Tiền
            </button>
          )}
        </div>
      </div>

      {isAdmin && <StatCards />}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {isAdmin && <RevenueChart className="lg:col-span-2" />}
        <TodaySchedule wide={!isAdmin} showStaff={isAdmin} />
      </div>
    </div>
  );
}
