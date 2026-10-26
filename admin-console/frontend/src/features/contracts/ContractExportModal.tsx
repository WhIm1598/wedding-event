import { useState } from 'react';
import { Download, FileText, MessageCircle, Printer } from 'lucide-react';
import { contractsApi } from '@/api/contracts.api';
import { settingsApi } from '@/api/system.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import type { Contract } from '@/types';
import { ContractDocument } from './ContractDocument';

export function ContractExportModal({ contract, onClose }: { contract: Contract | null; onClose: () => void }) {
  const showToast = useToast();
  const [busy, setBusy] = useState(false);
  const { data: studio } = useAsync(() => settingsApi.get());

  if (!contract) return null;

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
          {/* No Zalo API yet (FN-ADM-CONTR-01 endpoint 3): staff open the chat and send the downloaded PDF themselves (#39) */}
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
