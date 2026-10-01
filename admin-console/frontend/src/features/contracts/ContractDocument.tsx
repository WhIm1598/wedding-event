import { Camera } from 'lucide-react';
import { formatDate, formatVND } from '@/lib/format';
import type { Contract, StudioSettings } from '@/types';

// Payment schedule required by FN-ADM-CONTR-01
const INSTALLMENTS = [
  { label: 'Đợt 1: Đặt cọc khi ký hợp đồng', ratio: 0.3 },
  { label: 'Đợt 2: Thanh toán vào ngày chụp / ngày cưới', ratio: 0.5 },
  { label: 'Đợt 3: Thanh toán khi nhận album & ảnh hoàn thiện', ratio: 0.2 },
];

/** A4 contract preview (210mm x 297mm). Backend renders the same layout to PDF. */
export function ContractDocument({ contract, studio }: { contract: Contract; studio?: StudioSettings }) {
  return (
    <div className="print-area bg-white p-8 sm:p-12 shadow-md w-full max-w-[210mm] min-h-[297mm] text-slate-800 font-serif relative">
      <div className="absolute top-0 left-0 w-full h-2 bg-rose-600" />
      <div className="flex justify-between items-start border-b-2 border-slate-100 pb-6 mb-6">
        <div className="flex items-center gap-3">
          <div className="bg-rose-600 text-white p-2 rounded-lg">
            <Camera size={24} />
          </div>
          <div>
            <h1 className="font-bold text-2xl tracking-tight text-slate-900">{studio?.name ?? 'LUMIÈRE STUDIOS'}</h1>
            <p className="text-xs text-slate-500 uppercase tracking-widest">Lưu Giữ Khoảnh Khắc</p>
          </div>
        </div>
        <div className="text-right text-sm">
          <p>
            Số HĐ: <strong>{contract.contractNumber}</strong>
          </p>
          <p>Ngày lập: {formatDate(contract.contractDate)}</p>
        </div>
      </div>

      <h2 className="text-xl font-bold text-center mb-8 uppercase tracking-wider text-slate-900">Hợp đồng dịch vụ cưới</h2>

      <div className="space-y-6 text-sm">
        <section>
          <h3 className="font-bold text-slate-900 border-b border-slate-200 pb-1 mb-2">ĐẠI DIỆN KHÁCH HÀNG (BÊN A)</h3>
          <div className="grid grid-cols-2 gap-2">
            <p>
              <strong>Họ và tên:</strong> {contract.customerName}
            </p>
            <p>
              <strong>Số điện thoại:</strong> {contract.phone}
            </p>
          </div>
        </section>

        <section>
          <h3 className="font-bold text-slate-900 border-b border-slate-200 pb-1 mb-2">ĐẠI DIỆN STUDIO (BÊN B)</h3>
          <div className="grid grid-cols-2 gap-2">
            <p>
              <strong>Đơn vị:</strong> {studio?.name ?? 'LUMIÈRE STUDIOS'}
            </p>
            <p>
              <strong>Mã số thuế:</strong> {studio?.taxCode ?? '—'}
            </p>
            <p>
              <strong>Đại diện:</strong> {studio?.legalRepresentative ?? '—'}
            </p>
            <p>
              <strong>Địa chỉ:</strong> {studio?.address ?? '—'}
            </p>
          </div>
        </section>

        <section>
          <h3 className="font-bold text-slate-900 border-b border-slate-200 pb-1 mb-2">CHI TIẾT DỊCH VỤ</h3>
          <table className="w-full border-collapse border border-slate-300 mt-2">
            <thead>
              <tr className="bg-slate-50">
                <th className="border border-slate-300 p-2 text-left">Nội dung</th>
                <th className="border border-slate-300 p-2 text-right">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="border border-slate-300 p-2">
                  {contract.packageName}
                  {contract.notes && <div className="text-xs text-slate-500 mt-1">Ghi chú: {contract.notes}</div>}
                </td>
                <td className="border border-slate-300 p-2 text-right font-medium">{formatVND(contract.totalAmount)}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section>
          <h3 className="font-bold text-slate-900 border-b border-slate-200 pb-1 mb-2">ĐIỀU KHOẢN THANH TOÁN</h3>
          <div className="space-y-1">
            {INSTALLMENTS.map((i) => (
              <p key={i.label} className="flex justify-between">
                <span>
                  {i.label} ({i.ratio * 100}%)
                </span>
                <span>{formatVND(Math.round(contract.totalAmount * i.ratio))}</span>
              </p>
            ))}
            <p className="flex justify-between border-t border-slate-200 pt-1 mt-2">
              <span>Tổng giá trị hợp đồng:</span> <strong className="text-lg">{formatVND(contract.totalAmount)}</strong>
            </p>
            <p className="flex justify-between text-emerald-700">
              <span>Đã thanh toán:</span> <strong>{formatVND(contract.paidAmount)}</strong>
            </p>
            <p className="flex justify-between text-rose-600">
              <span>Số tiền còn lại:</span> <strong>{formatVND(contract.remainingAmount)}</strong>
            </p>
          </div>
          {studio?.bankInfo && <p className="mt-3 text-xs whitespace-pre-line text-slate-600">Thông tin chuyển khoản:{'\n'}{studio.bankInfo}</p>}
        </section>
      </div>

      <div className="mt-16 flex justify-between px-8 text-center">
        {['Đại diện Khách hàng', 'Đại diện Studio'].map((label) => (
          <div key={label}>
            <p className="font-bold text-slate-900 mb-16">{label}</p>
            <p className="text-sm text-slate-500">(Ký và ghi rõ họ tên)</p>
          </div>
        ))}
      </div>
    </div>
  );
}
