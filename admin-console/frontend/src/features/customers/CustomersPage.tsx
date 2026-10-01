import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Kanban, List, Plus } from 'lucide-react';
import { contractsApi } from '@/api/contracts.api';
import { crmApi } from '@/api/crm.api';
import { errorMessage } from '@/api/http';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { useLayout } from '@/components/layout/AppLayout';
import { Button } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { ErrorState, Spinner } from '@/components/ui/States';
import { cn } from '@/lib/cn';
import { ContractExportModal } from '@/features/contracts/ContractExportModal';
import type { Contract, LeadStage } from '@/types';
import { LeadKanban } from './components/LeadKanban';
import { ContractTable } from './components/ContractTable';
import { CreateLeadModal } from './components/CreateLeadModal';

type ViewType = 'kanban' | 'table';

export function CustomersPage() {
  const { contractsVersion } = useLayout();
  const showToast = useToast();
  const [params, setParams] = useSearchParams();
  const viewType: ViewType = params.get('view') === 'kanban' ? 'kanban' : 'table';
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [exporting, setExporting] = useState<Contract | null>(null);

  const leads = useAsync(() => crmApi.getPipeline());
  const contracts = useAsync(() => contractsApi.list(), [contractsVersion]);

  const moveLead = async (leadId: string, stage: LeadStage) => {
    leads.setData((prev) => prev?.map((l) => (l.id === leadId ? { ...l, stage } : l)));
    try {
      await crmApi.updateStage(leadId, stage);
    } catch (err) {
      showToast(errorMessage(err), 'error');
      leads.reload(); // revert the optimistic move
    }
  };

  const active = viewType === 'kanban' ? leads : contracts;

  return (
    <div className="space-y-6 h-full flex flex-col">
      <PageHeader
        title="Khách Hàng (CRM)"
        description="Quản lý Lead (Khách tiềm năng) & Hợp đồng đang chạy."
        actions={
          <>
            <div className="bg-slate-100 p-1 rounded-lg flex text-sm font-medium">
              <button
                onClick={() => setParams({ view: 'kanban' })}
                className={cn('px-3 py-1.5 rounded-md transition-colors flex items-center', viewType === 'kanban' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500')}
              >
                <Kanban size={16} className="mr-1.5" /> Pipeline Lead
              </button>
              <button
                onClick={() => setParams({ view: 'table' })}
                className={cn('px-3 py-1.5 rounded-md transition-colors flex items-center', viewType === 'table' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500')}
              >
                <List size={16} className="mr-1.5" /> Hợp Đồng
              </button>
            </div>
            <Button onClick={() => setIsCreateOpen(true)} className="shadow-rose-200">
              <Plus size={18} className="mr-2" /> Thêm Mới
            </Button>
          </>
        }
      />

      {active.loading && <Spinner />}
      {active.error && <ErrorState error={active.error} onRetry={active.reload} />}

      {!active.loading &&
        !active.error &&
        (viewType === 'kanban' ? (
          <LeadKanban leads={leads.data ?? []} onMove={moveLead} />
        ) : (
          <ContractTable contracts={contracts.data ?? []} onExport={setExporting} />
        ))}

      <CreateLeadModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onCreated={() => {
          leads.reload();
          setParams({ view: 'kanban' });
        }}
      />
      <ContractExportModal contract={exporting} onClose={() => setExporting(null)} />
    </div>
  );
}
