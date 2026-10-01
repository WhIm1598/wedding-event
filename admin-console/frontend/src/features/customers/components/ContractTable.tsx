import { HeartHandshake, MessageCircle, Phone, Printer } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Card } from '@/components/ui/Card';
import { CONTRACT_STATUS } from '@/constants/labels';
import { formatVND } from '@/lib/format';
import type { Contract } from '@/types';

const HEADERS = ['Thông tin Khách', 'Gói Dịch Vụ', 'Thanh Toán', 'Trạng Thái'];

export function ContractTable({ contracts, onExport }: { contracts: Contract[]; onExport: (c: Contract) => void }) {
  return (
    <Card className="overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              {HEADERS.map((h) => (
                <th key={h} className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">
                  {h}
                </th>
              ))}
              <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {contracts.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-12 text-center text-slate-400">
                  <HeartHandshake size={48} className="mx-auto mb-3 text-slate-200" />
                  Chưa có hợp đồng nào.
                </td>
              </tr>
            ) : (
              contracts.map((c) => (
                <tr key={c.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="py-4 px-6">
                    <div className="text-sm font-bold text-slate-900 flex items-center">
                      {c.customerName}
                      {c.hasZalo && <span className="ml-2 bg-blue-100 text-blue-700 text-[10px] px-1.5 py-0.5 rounded font-bold uppercase tracking-wide">Zalo</span>}
                    </div>
                    <div className="text-xs text-slate-500 mt-1 flex items-center">
                      <Phone size={12} className="mr-1" /> {c.phone}
                    </div>
                  </td>
                  <td className="py-4 px-6">
                    <div className="text-sm font-medium text-slate-700">{c.packageName}</div>
                    <div className="text-xs text-slate-400 mt-1 font-mono">{c.contractNumber}</div>
                  </td>
                  <td className="py-4 px-6">
                    <div className="text-sm font-bold text-slate-900">{formatVND(c.totalAmount)}</div>
                    <div className="text-xs text-rose-600 mt-1 font-medium">Còn nợ: {formatVND(c.remainingAmount)}</div>
                  </td>
                  <td className="py-4 px-6">
                    <Badge variant={CONTRACT_STATUS[c.status].variant}>{CONTRACT_STATUS[c.status].label}</Badge>
                  </td>
                  <td className="py-4 px-6">
                    <div className="flex justify-end gap-2">
                      <button onClick={() => onExport(c)} className="p-2 bg-rose-50 text-rose-600 rounded-lg hover:bg-rose-100 transition-colors" title="Xuất hợp đồng">
                        <Printer size={16} />
                      </button>
                      {c.hasZalo && (
                        <a
                          href={`https://zalo.me/${c.phone}`}
                          target="_blank"
                          rel="noreferrer"
                          className="p-2 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100 transition-colors"
                          title="Nhắn tin Zalo"
                        >
                          <MessageCircle size={16} />
                        </a>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </Card>
  );
}
