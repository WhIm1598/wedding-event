import { useState } from 'react';
import { Download, FileText, Link2Off, MessageCircle, Printer, Send } from 'lucide-react';
import { contractsApi } from '@/api/contracts.api';
import { errorMessage } from '@/api/http';
import { useAuth } from '@/contexts/AuthContext';
import { settingsApi } from '@/api/system.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import type { Contract } from '@/types';
import { ContractDocument } from './ContractDocument';

export function ContractExportModal({ contract, onClose }: { contract: Contract | null; onClose: () => void }) {
  const showToast = useToast();
  const { isAdmin } = useAuth();
  const [busy, setBusy] = useState(false);
  const [sending, setSending] = useState(false);
  const { data: studio } = useAsync(() => settingsApi.get());

  if (!contract) return null;

  /** FN-ADM-CONTR-01 endpoint 3: ZNS with the public contract link */
  const sendZalo = async () => {
    let phoneNumber: string | undefined;
    if (!contract.hasZalo) {
      phoneNumber = window.prompt('Khách chưa có Zalo trên hợp đồng. Nhập số Zalo để gửi:')?.trim();
      if (!phoneNumber) return;
    } else if (!window.confirm(`Gửi tin Zalo kèm link hợp đồng ${contract.contractNumber} tới ${contract.customerName}?`)) {
      return;
    }
    setSending(true);
    try {
      const res = await contractsApi.shareZalo(contract, phoneNumber);
      showToast(`Đã gửi Zalo tới ${res.sentTo}. Link có hiệu lực 30 ngày; link cũ (nếu có) đã ngừng hoạt động.`);
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSending(false);
    }
  };

  const revokeLink = async () => {
    if (!window.confirm('Thu hồi link hợp đồng đã gửi? Khách sẽ không mở được link nữa.')) return;
    try {
      await contractsApi.revokeLink(contract);
      showToast('Đã thu hồi link hợp đồng');
    } catch (err) {
      showToast(errorMessage(err), 'error');
    }
  };

  const downloadPdf = async () => {
    setBusy(true);
    try {
      await contractsApi.exportPdf(contract);
      showToast(`Đã lưu PDF hợp đồng của ${contract.customerName} vào máy.`);
      onClose();
    } catch (err) {
      showToast((err as Error).message, 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      width="max-w-3xl"
      title={
        <>
          <FileText className="mr-2" size={20} /> Xuất Hợp Đồng
        </>
      }
      footer={
        <>
          <Button variant="ghost" onClick={() => window.print()}>
            <Printer size={18} className="mr-2" /> In
          </Button>
          {isAdmin && (
            <Button variant="ghost" onClick={revokeLink} title="Thu hồi link hợp đồng đã gửi qua Zalo">
              <Link2Off size={18} className="mr-2" /> Thu hồi link
            </Button>
          )}
          <Button variant="secondary" loading={sending} onClick={sendZalo} title="Gửi tin Zalo (ZNS) từ Zalo OA của studio kèm link hợp đồng">
            <Send size={18} className="mr-2" /> Gửi Zalo
          </Button>
          {/* Fallback when the studio has no Zalo OA: staff open the chat and send the downloaded PDF themselves */}
          {contract.hasZalo && (
            <a
              href={`https://zalo.me/${contract.phone}`}
              target="_blank"
              rel="noopener noreferrer"
              title="Mở Zalo của khách, rồi gửi file PDF vừa tải"
              className="inline-flex items-center justify-center px-4 py-2 rounded-lg font-medium border bg-blue-50 text-blue-600 hover:bg-blue-100 border-blue-100 transition-colors"
            >
              <MessageCircle size={18} className="mr-2" /> Nhắn Zalo
            </a>
          )}
          <Button loading={busy} onClick={downloadPdf}>
            <Download size={18} className="mr-2" /> Tải xuống PDF
          </Button>
        </>
      }
    >
      <div className="-m-6 p-4 sm:p-8 flex justify-center bg-slate-100">
        <ContractDocument contract={contract} studio={studio} />
      </div>
    </Modal>
  );
}
