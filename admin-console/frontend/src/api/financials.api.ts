import { env } from '@/config/env';
import { todayISO } from '@/lib/date';
import { ApiError, http } from './http';
import { db, delay, nextId } from './mock/db';
import type { CreateTransactionRequest, FinancialSummary, Transaction } from '@/types';

export const financialsApi = {
  /** GET /admin/financials/transactions */
  listTransactions(): Promise<Transaction[]> {
    if (!env.useMock) return http.get('/admin/financials/transactions');
    return delay([...db.transactions].sort((a, b) => b.transactionDate.localeCompare(a.transactionDate)));
  },

  /** GET /admin/financials/summary */
  getSummary(): Promise<FinancialSummary> {
    if (!env.useMock) return http.get('/admin/financials/summary');
    const totalIncome = 245_000_000;
    const totalExpense = 82_500_000;
    return delay({ periodLabel: `Tháng ${new Date().getMonth() + 1}`, totalIncome, totalExpense, netProfit: totalIncome - totalExpense });
  },

  /** POST /admin/financials/transactions (FN-ADM-FIN-01) — INCOME with contractId reduces contract debt */
  create(req: CreateTransactionRequest): Promise<Transaction> {
    if (!env.useMock) return http.post('/admin/financials/transactions', req);
    if (req.transactionDate > todayISO()) throw new ApiError(400, 'BAD_REQUEST', 'Ngày giao dịch không được sau ngày hôm nay');

    const contract = req.contractId ? db.contracts.find((c) => c.id === req.contractId) : undefined;
    if (contract && req.type === 'INCOME') {
      if (contract.status === 'COMPLETED') {
        throw new ApiError(409, 'CONTRACT_NOT_PAYABLE', `Hợp đồng ${contract.contractNumber} đã hoàn tất, không thể ghi thêm khoản thu`);
      }
      if (req.amount > contract.remainingAmount) {
        throw new ApiError(400, 'PAYMENT_EXCEEDS_REMAINING', 'Số tiền thu vượt quá công nợ còn lại của hợp đồng');
      }
    }
    const id = nextId('trx');
    const trx: Transaction = { ...req, id, code: id.toUpperCase() };
    db.transactions.push(trx);

    if (contract && req.type === 'INCOME') {
      contract.paidAmount += req.amount;
      contract.remainingAmount = Math.max(0, contract.totalAmount - contract.paidAmount);
      if (contract.remainingAmount === 0) contract.status = 'COMPLETED';
    }
    return delay(trx);
  },
};
