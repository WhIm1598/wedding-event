import { env } from '@/config/env';
import { todayISO } from '@/lib/date';
import { http } from './http';
import { db, delay, nextId } from './mock/db';
import type { Contract, CreateContractRequest } from '@/types';

export const contractsApi = {
  /** GET /admin/contracts */
  list(): Promise<Contract[]> {
    if (!env.useMock) return http.get('/admin/contracts');
    return delay(db.contracts);
  },

  /** POST /admin/contracts */
  create(req: CreateContractRequest): Promise<Contract> {
    if (!env.useMock) return http.post('/admin/contracts', req);
    const pkg = db.packages.find((p) => p.id === req.servicePackageId);
    const id = nextId('hd');
    const contract: Contract = {
      id,
      contractNumber: id.toUpperCase(),
      customerName: req.customerName,
      phone: req.phone,
      hasZalo: true,
      packageName: pkg?.name ?? '—',
      totalAmount: req.totalAmount,
      paidAmount: req.depositAmount,
      remainingAmount: req.totalAmount - req.depositAmount,
      contractDate: todayISO(),
      status: req.depositAmount > 0 ? 'DEPOSITED' : 'DRAFT',
      notes: req.notes,
    };
    db.contracts.unshift(contract);
    return delay(contract);
  },

  /** GET /admin/contracts/{id}/export-pdf — A4 PDF binary */
  async exportPdf(contract: Contract): Promise<void> {
    if (env.useMock) return delay(undefined, 500);
    const blob = await http.download(`/admin/contracts/${contract.id}/export-pdf`);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${contract.contractNumber}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  },

  /** POST /admin/contracts/{id}/share-zalo */
  shareZalo(contract: Contract): Promise<void> {
    if (!env.useMock) return http.post(`/admin/contracts/${contract.id}/share-zalo`, { phoneNumber: contract.phone });
    return delay(undefined, 500);
  },
};
