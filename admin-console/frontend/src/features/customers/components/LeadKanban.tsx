import { useState, type DragEvent } from 'react';
import { Phone } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Card } from '@/components/ui/Card';
import { LEAD_STAGES } from '@/constants/labels';
import { formatDate } from '@/lib/format';
import { cn } from '@/lib/cn';
import type { Lead, LeadStage } from '@/types';

/** 5-column CRM pipeline with HTML5 drag & drop (FN-ADM-CRM-01) */
export function LeadKanban({ leads, onMove }: { leads: Lead[]; onMove: (leadId: string, stage: LeadStage) => void }) {
  const [dragOver, setDragOver] = useState<LeadStage | null>(null);

  const handleDrop = (e: DragEvent, stage: LeadStage) => {
    e.preventDefault();
    setDragOver(null);
    const leadId = e.dataTransfer.getData('text/plain');
    const lead = leads.find((l) => l.id === leadId);
    if (lead && lead.stage !== stage) onMove(leadId, stage);
  };

  return (
    <div className="flex-1 overflow-x-auto pb-4">
      <div className="flex gap-6 h-full min-w-max">
        {LEAD_STAGES.map((stage) => {
          const items = leads.filter((l) => l.stage === stage.id);
          return (
            <div
              key={stage.id}
              onDragOver={(e) => {
                e.preventDefault();
                setDragOver(stage.id);
              }}
              onDragLeave={() => setDragOver(null)}
              onDrop={(e) => handleDrop(e, stage.id)}
              className={cn('w-72 bg-slate-100/80 rounded-xl flex flex-col p-4 transition-colors', dragOver === stage.id && 'bg-rose-50 ring-2 ring-rose-200')}
            >
              <h4 className="font-bold text-slate-700 mb-4 flex items-center justify-between">
                {stage.label}
                <span className="bg-white text-slate-500 text-xs px-2 py-0.5 rounded-full shadow-sm">{items.length}</span>
              </h4>
              <div className="flex-1 overflow-y-auto space-y-3">
                {items.length === 0 ? (
                  <div className="text-center py-6 border-2 border-dashed border-slate-200 rounded-lg text-slate-400 text-sm">Kéo thả lead vào đây</div>
                ) : (
                  items.map((lead) => (
                    <Card
                      key={lead.id}
                      draggable
                      onDragStart={(e) => e.dataTransfer.setData('text/plain', lead.id)}
                      className="p-4 cursor-grab active:cursor-grabbing hover:border-rose-300 hover:shadow-md transition-all group"
                    >
                      <div className="flex justify-between items-start mb-2 gap-2">
                        <p className="font-bold text-sm text-slate-900 group-hover:text-rose-600 transition-colors">{lead.name}</p>
                        <span className="text-[10px] text-slate-400 whitespace-nowrap">{formatDate(lead.createdAt)}</span>
                      </div>
                      <p className="text-xs text-slate-500 flex items-center">
                        <Phone size={12} className="mr-1" /> {lead.phone}
                        {lead.hasZalo && <span className="ml-2 bg-blue-100 text-blue-700 text-[10px] px-1.5 rounded font-bold">ZALO</span>}
                      </p>
                      <div className="mt-3 pt-3 border-t border-slate-100">
                        <Badge>{lead.interest}</Badge>
                      </div>
                    </Card>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
