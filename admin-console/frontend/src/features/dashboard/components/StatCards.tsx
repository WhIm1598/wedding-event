import { CalendarDays, DollarSign, Shirt, TrendingDown, TrendingUp, Users, type LucideIcon } from 'lucide-react';
import { dashboardApi } from '@/api/dashboard.api';
import { useAsync } from '@/hooks/useAsync';
import { Card } from '@/components/ui/Card';
import { Spinner } from '@/components/ui/States';
import { formatVND } from '@/lib/format';
import type { TrendMetric } from '@/types';

interface StatItem extends TrendMetric {
  label: string;
  value: string;
  icon: LucideIcon;
  color: string;
}

export function StatCards() {
  const { data, loading } = useAsync(() => dashboardApi.getStats());
  if (loading || !data) return <Spinner />;

  const trend = ({ trendPercentage, isIncrease }: TrendMetric): TrendMetric => ({ trendPercentage, isIncrease });
  const stats: StatItem[] = [
    { label: 'Doanh thu tháng', value: formatVND(data.monthlyRevenue.amount), icon: DollarSign, color: 'text-emerald-600 bg-emerald-100', ...trend(data.monthlyRevenue) },
    { label: 'Hợp đồng đang chạy', value: String(data.activeContractsCount.value), icon: CalendarDays, color: 'text-blue-600 bg-blue-100', ...trend(data.activeContractsCount) },
    { label: 'Khách cần tư vấn', value: String(data.pendingLeadsCount.value), icon: Users, color: 'text-amber-600 bg-amber-100', ...trend(data.pendingLeadsCount) },
    { label: 'Tỉ lệ thuê trang phục', value: `${data.costumeRentalRate.percentage}%`, icon: Shirt, color: 'text-purple-600 bg-purple-100', ...trend(data.costumeRentalRate) },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      {stats.map((stat) => {
        const Trend = stat.isIncrease ? TrendingUp : TrendingDown;
        return (
          <Card key={stat.label} className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">{stat.label}</p>
                <h3 className="text-2xl font-bold text-slate-900 mt-1">{stat.value}</h3>
              </div>
              <div className={`w-12 h-12 rounded-full flex items-center justify-center ${stat.color}`}>
                <stat.icon size={24} />
              </div>
            </div>
            <div className="mt-4 flex items-center text-sm">
              <Trend size={16} className={`mr-1 ${stat.isIncrease ? 'text-emerald-500' : 'text-red-500'}`} />
              <span className={`font-medium ${stat.isIncrease ? 'text-emerald-600' : 'text-red-600'}`}>
                {stat.isIncrease ? '+' : '-'}
                {Math.abs(stat.trendPercentage)}%
              </span>
              <span className="text-slate-500 ml-2">so với tháng trước</span>
            </div>
          </Card>
        );
      })}
    </div>
  );
}
