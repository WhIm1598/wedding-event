import { useState } from 'react';
import { Download, FileText, Printer, Share2 } from 'lucide-react';
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
  const [busy, setBusy] = useState<'pdf' | 'zalo' | null>(null);
  const { data: studio } = useAsync(() => settingsApi.get());

  if (!contract) return null;

  const run = async (kind: 'pdf' | 'zalo') => {
    setBusy(kind);
    try {
      if (kind === 'pdf') {
        await contractsApi.exportPdf(contract);
        showToast(`Đã lưu PDF hợp đồng của ${contract.customerName} vào máy.`);
      } else {
        await contractsApi.shareZalo(contract);
        showToast(`Đã gửi hợp đồng qua Zalo cho ${contract.customerName}.`, 'info');
      }
      onClose();
    } catch (err) {
      showToast((err as Error).message, 'error');
    } finally {
      setBusy(null);
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
          {contract.hasZalo && (
            <Button variant="outline" className="bg-blue-50 text-blue-600 hover:bg-blue-100 border-blue-100" loading={busy === 'zalo'} onClick={() => run('zalo')}>
              <Share2 size={18} className="mr-2" /> Chia sẻ Zalo
            </Button>
          )}
          <Button loading={busy === 'pdf'} onClick={() => run('pdf')}>
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
