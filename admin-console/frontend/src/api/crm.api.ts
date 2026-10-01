import { env } from '@/config/env';
import { todayISO } from '@/lib/date';
import { ApiError, http } from './http';
import { db, delay, nextId } from './mock/db';
import type { CreateLeadRequest, Lead, LeadStage } from '@/types';

export const crmApi = {
  /** GET /admin/crm/pipeline (FN-ADM-CRM-01) */
  getPipeline(): Promise<Lead[]> {
    if (!env.useMock) return http.get('/admin/crm/pipeline');
    return delay(db.leads);
  },

  /** POST /admin/crm/customers */
  createLead(req: CreateLeadRequest): Promise<Lead> {
    if (!env.useMock) return http.post('/admin/crm/customers', req);
    const lead: Lead = { ...req, id: nextId('ld'), stage: 'NEW_LEAD', createdAt: todayISO() };
    db.leads.push(lead);
    return delay(lead);
  },

  /** PATCH /admin/crm/customers/{id}/stage */
  updateStage(id: string, stage: LeadStage): Promise<Lead> {
    if (!env.useMock) return http.patch(`/admin/crm/customers/${id}/stage`, { stage });
    const lead = db.leads.find((l) => l.id === id)!;
    // "In progress" / "Completed" only once the customer has a contract (§1.6); mock contracts link by phone
    const contractStage = stage === 'IN_PROGRESS' || stage === 'COMPLETED';
    if (contractStage && stage !== lead.stage && !db.contracts.some((c) => c.phone === lead.phone)) {
      throw new ApiError(409, 'INVALID_STATUS_TRANSITION', 'Khách hàng chưa có hợp đồng, hãy tạo hợp đồng trước khi chuyển sang giai đoạn này');
    }
    lead.stage = stage;
    return delay(lead, 100);
  },
};
