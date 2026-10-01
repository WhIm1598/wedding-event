import { useState, type FormEvent } from 'react';
import { CheckCircle2, Clock, Phone, Trash2, Users } from 'lucide-react';
import { errorMessage } from '@/api/http';
import { staffApi } from '@/api/staff.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Button } from '@/components/ui/Button';
import { Field, Input, Textarea } from '@/components/ui/Form';
import { Modal, SlideOver } from '@/components/ui/Modal';
import { EmptyState, Spinner } from '@/components/ui/States';
import { todayISO } from '@/lib/date';
import { formatDate } from '@/lib/format';
import { cn } from '@/lib/cn';
import type { StaffMember } from '@/types';

export function StaffDetailPanel({ staff, onClose }: { staff: StaffMember | null; onClose: () => void }) {
  const showToast = useToast();
  const [isTaskOpen, setIsTaskOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const tasks = useAsync(() => (staff ? staffApi.listTasks(staff.id) : Promise.resolve([])), [staff?.id]);

  // Optimistic updates; reload from the server if the call fails
  const toggle = async (id: string, completed: boolean) => {
    tasks.setData((prev) => prev?.map((t) => (t.id === id ? { ...t, completed } : t)));
    try {
      await staffApi.toggleTask(id, completed);
    } catch (err) {
      showToast(errorMessage(err), 'error');
      tasks.reload();
    }
  };

  const remove = async (id: string) => {
    tasks.setData((prev) => prev?.filter((t) => t.id !== id));
    try {
      await staffApi.deleteTask(id);
    } catch (err) {
      showToast(errorMessage(err), 'error');
      tasks.reload();
    }
  };

  const handleCreateTask = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!staff) return;
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      await staffApi.createTask({ staffId: staff.id, title: String(fd.get('title')), dueDate: String(fd.get('dueDate')), notes: String(fd.get('notes') || '') });
      showToast('Đã giao việc thành công!');
      setIsTaskOpen(false);
      tasks.reload();
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <SlideOver isOpen={!!staff} onClose={onClose} title="Hồ Sơ Nhân Sự" width="sm:max-w-xl">
        {staff && (
          <div className="space-y-6">
            <div className="flex items-center gap-4 pb-6 border-b border-slate-200">
              <div className="w-16 h-16 bg-rose-100 rounded-full flex items-center justify-center">
                <Users size={32} className="text-rose-600" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-slate-900">{staff.name}</h2>
                <p className="text-sm text-slate-500">
                  {staff.position} • {staff.code}
                </p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <p className="text-xs font-medium text-slate-500 uppercase">Liên hệ</p>
                <p className="text-sm font-bold text-slate-900 mt-1 flex items-center">
                  <Phone size={14} className="mr-2 text-slate-400" /> {staff.phone}
                </p>
              </div>
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <p className="text-xs font-medium text-slate-500 uppercase">KPI Tháng này</p>
                <p className="text-sm font-bold text-slate-900 mt-1 flex items-center">
                  <CheckCircle2 size={14} className="mr-2 text-emerald-500" /> Hoàn thành {staff.completedThisMonth} show
                </p>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-200">
              <div className="flex justify-between items-center mb-4">
                <h3 className="font-bold text-slate-900 flex items-center">
                  <Clock className="mr-2 text-rose-600" size={18} /> Giao Việc & Theo Dõi
                </h3>
                <button onClick={() => setIsTaskOpen(true)} className="text-xs font-medium text-rose-600 bg-rose-50 px-3 py-1.5 rounded-lg hover:bg-rose-100">
                  + Giao việc mới
                </button>
              </div>

              {tasks.loading && <Spinner />}
              {!tasks.loading && (tasks.data ?? []).length === 0 && <EmptyState message="Chưa có công việc nào." />}
              <div className="space-y-3">
                {(tasks.data ?? []).map((task) => (
                  <div key={task.id} className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-white hover:border-rose-300 transition-colors">
                    <input
                      type="checkbox"
                      checked={task.completed}
                      onChange={(e) => toggle(task.id, e.target.checked)}
                      className="mt-1 w-4 h-4 accent-rose-600"
                    />
                    <div className="flex-1">
                      <p className={cn('text-sm font-medium', task.completed ? 'text-slate-400 line-through' : 'text-slate-800')}>{task.title}</p>
                      <p className="text-xs text-slate-400 mt-0.5">Hạn chót: {formatDate(task.dueDate)}</p>
                    </div>
                    <button onClick={() => remove(task.id)} className="text-slate-400 hover:text-red-500" aria-label="Xóa">
                      <Trash2 size={16} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </SlideOver>

      <Modal isOpen={isTaskOpen} onClose={() => setIsTaskOpen(false)} title="Giao Việc Mới">
        <form className="space-y-4" onSubmit={handleCreateTask}>
          <Field label="Tên công việc" required>
            <Input name="title" placeholder="VD: Chuẩn bị váy Vera Wang, makeup cô dâu..." required />
          </Field>
          <Field label="Hạn chót hoàn thành" required>
            <Input name="dueDate" type="date" defaultValue={todayISO()} required />
          </Field>
          <Field label="Ghi chú thêm">
            <Textarea name="notes" rows={3} />
          </Field>
          <Button type="submit" className="w-full mt-4" loading={submitting}>
            Lưu Công Việc
          </Button>
        </form>
      </Modal>
    </>
  );
}
