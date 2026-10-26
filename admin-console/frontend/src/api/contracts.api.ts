import { env } from '@/config/env';
import { todayISO } from '@/lib/date';
import { ApiError, http } from './http';
import { applyMockPayment, db, delay, nextId, syncCustomerStage } from './mock/db';
import type { Contract, CreateContractRequest, PublicContract, ShareResult } from '@/types';

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

  /** POST /admin/contracts/{id}/share-zalo — ZNS with a new 30-day public link through the studio's Zalo OA */
  shareZalo(contract: Contract, phoneNumber?: string): Promise<ShareResult> {
    if (!env.useMock) return http.post(`/admin/contracts/${contract.id}/share-zalo`, phoneNumber ? { phoneNumber } : {});
    const phone = phoneNumber || contract.phone;
    if (!phoneNumber && !contract.hasZalo) throw new ApiError(400, 'ZALO_NOT_AVAILABLE', 'Khách chưa có Zalo, vui lòng nhập số Zalo khác');
    if (!/^0(3|5|7|8|9)[0-9]{8}$/.test(phone)) throw new ApiError(400, 'BAD_REQUEST', 'Số điện thoại không hợp lệ');
    if (!db.zaloOa.configured) throw new ApiError(400, 'ZALO_OA_NOT_CONFIGURED', 'Studio chưa kết nối Zalo OA (Cài đặt → Zalo OA)');
    dropLinks(contract.id);
    const token = `mock-${Math.random().toString(36).slice(2, 12)}`;
    const expiresAt = new Date(Date.now() + 30 * 86_400_000).toISOString();
    db.shareLinks[token] = { contractId: contract.id, expiresAt };
    return delay({ sentTo: maskPhone(phone), publicUrl: `${window.location.origin}/c/${token}`, shareExpiresAt: expiresAt }, 500);
  },

  /** POST /admin/contracts/{id}/revoke-link (FN-PUB-CONTR-01) */
  revokeLink(contract: Contract): Promise<void> {
    if (!env.useMock) return http.post(`/admin/contracts/${contract.id}/revoke-link`);
    dropLinks(contract.id);
    return delay(undefined);
  },
};

const maskPhone = (phone: string) => `${phone.slice(0, 4)}***${phone.slice(-3)}`;

/** Sharing again replaces the link, like the backend (only a hash is stored there) */
const dropLinks = (contractId: string) =>
  Object.keys(db.shareLinks).forEach((t) => {
    if (db.shareLinks[t].contractId === contractId) delete db.shareLinks[t];
  });

function mockShared(token: string): Contract {
  const link = db.shareLinks[token];
  const contract = link && link.expiresAt > new Date().toISOString() ? db.contracts.find((c) => c.id === link.contractId) : undefined;
  if (!contract) throw new ApiError(410, 'SHARE_LINK_EXPIRED', 'Link đã hết hạn, vui lòng liên hệ studio');
  return contract;
}

const INSTALLMENTS: Array<[string, number]> = [
  ['Đợt 1: Đặt cọc khi ký hợp đồng', 30],
  ['Đợt 2: Thanh toán vào ngày chụp / ngày cưới', 50],
  ['Đợt 3: Thanh toán khi nhận album & ảnh hoàn thiện', 20],
];

function toPublic(c: Contract): PublicContract {
  return {
    contractNumber: c.contractNumber,
    studio: { name: db.settings.name, address: db.settings.address, bankInfo: db.settings.bankInfo },
    customerName: c.customerName,
    phone: maskPhone(c.phone),
    packageName: c.packageName,
    features: db.packages.find((p) => p.name === c.packageName)?.features ?? [],
    totalAmount: c.totalAmount,
    paidAmount: c.paidAmount,
    remainingAmount: c.remainingAmount,
    installments: INSTALLMENTS.map(([label, percent]) => ({ label, percent, amount: Math.round((c.totalAmount * percent) / 100) })),
    eventDate: db.bookings.filter((b) => b.contractId === c.id && b.status !== 'CANCELLED').map((b) => b.date).sort()[0] ?? null,
    status: c.status,
    acceptedAt: db.acceptedAt[c.id] ?? null,
  };
}

/** The customer's page behind the Zalo link (FN-PUB-CONTR-01); no login */
export const publicContractsApi = {
  /** GET /public/contracts/{token} */
  async get(token: string): Promise<PublicContract> {
    if (!env.useMock) return http.get(`/public/contracts/${encodeURIComponent(token)}`);
    return delay(toPublic(mockShared(token)));
  },

  /** GET /public/contracts/{token}/pdf */
  async downloadPdf(token: string, contractNumber: string): Promise<void> {
    if (env.useMock) return delay(undefined, 500);
    const blob = await http.download(`/public/contracts/${encodeURIComponent(token)}/pdf`);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${contractNumber}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  },

  /** POST /public/contracts/{token}/accept — recorded once */
  async accept(token: string): Promise<PublicContract> {
    if (!env.useMock) return http.post(`/public/contracts/${encodeURIComponent(token)}/accept`);
    const c = mockShared(token);
    db.acceptedAt[c.id] ??= new Date().toISOString();
    return delay(toPublic(c));
  },
};
