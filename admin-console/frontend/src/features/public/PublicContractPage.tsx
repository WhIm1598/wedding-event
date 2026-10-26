import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { CheckCircle2, Download, FileText } from 'lucide-react';
import { publicContractsApi } from '@/api/contracts.api';
import { ApiError, errorMessage } from '@/api/http';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Spinner } from '@/components/ui/States';
import { CONTRACT_STATUS } from '@/constants/labels';
import { formatDate, formatVND } from '@/lib/format';

/** FN-PUB-CONTR-01: what the Zalo link opens on the customer's phone. No login; the token in the URL is the credential. */
export function PublicContractPage() {
  const { token = '' } = useParams();
  const showToast = useToast();
  const { data, loading, error, setData } = useAsync(() => publicContractsApi.get(token), [token]);
  const [accepting, setAccepting] = useState(false);
  const [downloading, setDownloading] = useState(false);

  if (loading) return <Spinner />;
  if (error || !data) {
    const expired = error instanceof ApiError && error.code === 'SHARE_LINK_EXPIRED';
    return (
      <main className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <Card className="p-8 max-w-md text-center space-y-2">
          <FileText className="mx-auto text-slate-300" size={40} />
          <h1 className="text-lg font-bold text-slate-900">{expired ? 'Link đã hết hạn' : 'Không mở được hợp đồng'}</h1>
          <p className="text-sm text-slate-500">{expired ? 'Vui lòng liên hệ studio để nhận link mới.' : errorMessage(error)}</p>
        </Card>
      </main>
    );
  }

  const accept = async () => {
    setAccepting(true);
    try {
      const updated = await publicContractsApi.accept(token);
      setData(() => updated);
      showToast('Cảm ơn bạn đã xác nhận hợp đồng');
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setAccepting(false);
    }
  };

  const download = async () => {
    setDownloading(true);
    try {
      await publicContractsApi.downloadPdf(token, data.contractNumber);
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setDownloading(false);
    }
  };

  const status = CONTRACT_STATUS[data.status];

  return (
    <main className="min-h-screen bg-slate-50 py-6 px-4">
      <div className="max-w-xl mx-auto space-y-4">
        <header className="text-center">
          <p className="text-xs tracking-widest text-slate-400">HỢP ĐỒNG DỊCH VỤ CƯỚI</p>
          <h1 className="text-2xl font-bold text-slate-900">{data.studio.name}</h1>
          <p className="text-sm text-slate-500">{data.studio.address}</p>
        </header>

        <Card className="p-5 space-y-3">
          <div className="flex items-center justify-between gap-2">
            <span className="font-semibold text-slate-900">Số HĐ: {data.contractNumber}</span>
            <Badge variant={status.variant}>{status.label}</Badge>
          </div>
          <dl className="grid grid-cols-[auto,1fr] gap-x-4 gap-y-1 text-sm">
            <dt className="text-slate-500">Khách hàng</dt>
            <dd className="text-slate-900">{data.customerName}</dd>
            <dt className="text-slate-500">Số điện thoại</dt>
            <dd className="text-slate-900">{data.phone}</dd>
            <dt className="text-slate-500">Ngày sự kiện</dt>
            <dd className="text-slate-900">{formatDate(data.eventDate)}</dd>
          </dl>
        </Card>

        <Card className="p-5 space-y-2">
          <h2 className="font-semibold text-slate-900">{data.packageName}</h2>
          {data.features.length > 0 && (
            <ul className="list-disc pl-5 text-sm text-slate-600 space-y-0.5">
              {data.features.map((f) => (
                <li key={f}>{f}</li>
              ))}
            </ul>
          )}
        </Card>

        <Card className="p-5 space-y-3">
          <h2 className="font-semibold text-slate-900">Thanh toán</h2>
          <table className="w-full text-sm">
            <tbody className="divide-y divide-slate-100">
              {data.installments.map((i) => (
                <tr key={i.label}>
                  <td className="py-2 pr-2 text-slate-600">
                    {i.label} ({i.percent}%)
                  </td>
                  <td className="py-2 text-right font-medium text-slate-900 whitespace-nowrap">{formatVND(i.amount)}</td>
                </tr>
              ))}
              <tr>
                <td className="py-2 text-slate-600">Tổng giá trị</td>
                <td className="py-2 text-right font-bold text-slate-900">{formatVND(data.totalAmount)}</td>
              </tr>
              <tr>
                <td className="py-2 text-emerald-700">Đã thanh toán</td>
                <td className="py-2 text-right text-emerald-700">{formatVND(data.paidAmount)}</td>
              </tr>
              <tr>
                <td className="py-2 text-rose-600">Còn lại</td>
                <td className="py-2 text-right font-semibold text-rose-600">{formatVND(data.remainingAmount)}</td>
              </tr>
            </tbody>
          </table>
          <div className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
            <p className="font-medium text-slate-700 mb-1">Thông tin chuyển khoản</p>
            <p className="whitespace-pre-line">{data.studio.bankInfo}</p>
            <p className="mt-1">
              Nội dung: <span className="font-mono font-medium text-slate-900">{data.contractNumber}</span>
            </p>
          </div>
        </Card>

        <div className="flex flex-col sm:flex-row gap-3">
          <Button variant="outline" className="flex-1" loading={downloading} onClick={download}>
            <Download size={18} className="mr-2" /> Tải PDF
          </Button>
          {data.acceptedAt ? (
            <p className="flex-1 flex items-center justify-center text-sm text-emerald-700 font-medium">
              <CheckCircle2 size={18} className="mr-2" /> Đã đồng ý lúc {new Date(data.acceptedAt).toLocaleString('vi-VN')}
            </p>
          ) : (
            <Button className="flex-1" loading={accepting} onClick={accept}>
              <CheckCircle2 size={18} className="mr-2" /> Tôi đã đọc và đồng ý
            </Button>
          )}
        </div>
      </div>
    </main>
  );
}
