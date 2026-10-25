import { env } from '@/config/env';
import { todayISO } from '@/lib/date';
import { ApiError, http } from './http';
import { applyMockPayment, db, delay, nextId, syncCustomerStage } from './mock/db';
import type { Contract, CreateContractRequest } from '@/types';

export const contractsApi = {
  /** GET /admin/contracts */
  list(): Promise<Contract[]> {
    if (!env.useMock) return http.get('/admin/contracts');
    return delay(db.contracts);
  },

  /** POST /admin/contracts — a deposit makes it effective and moves the customer to IN_PROGRESS (FN-ADM-CRM-03) */
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
      paidAmount: 0,
      remainingAmount: req.totalAmount,
      contractDate: todayISO(),
      status: 'DRAFT',
      notes: req.notes,
      deliveredAt: null,
    };
    db.contracts.unshift(contract);
    if (req.depositAmount > 0) applyMockPayment(contract, req.depositAmount);
    return delay(contract);
  },

  /** POST /admin/contracts/{id}/deliver (FN-ADM-CONTR-02) — completes it when paid in full, otherwise keeps the debt */
  deliver(contract: Contract): Promise<Contract> {
    if (!env.useMock) return http.post(`/admin/contracts/${contract.id}/deliver`);
    const c = db.contracts.find((x) => x.id === contract.id)!;
    if ((c.status !== 'DEPOSITED' && c.status !== 'IN_PROGRESS') || c.deliveredAt) {
      throw new ApiError(409, 'INVALID_STATUS_TRANSITION', 'Chỉ xác nhận bàn giao được hợp đồng đã đặt cọc và chưa bàn giao');
    }
    c.deliveredAt = new Date().toISOString();
    if (c.remainingAmount === 0) c.status = 'COMPLETED';
    syncCustomerStage(c);
    return delay(c);
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
