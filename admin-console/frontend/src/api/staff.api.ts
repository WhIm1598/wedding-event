import { env } from '@/config/env';
import { http } from './http';
import { db, delay, nextId } from './mock/db';
import type { CreateStaffRequest, CreateTaskRequest, StaffMember, StaffTask } from '@/types';

export const staffApi = {
  /** GET /admin/staff */
  list(): Promise<StaffMember[]> {
    if (!env.useMock) return http.get('/admin/staff');
    return delay(db.staff);
  },

  /** POST /admin/staff */
  create(req: CreateStaffRequest): Promise<StaffMember> {
    if (!env.useMock) return http.post('/admin/staff', req);
    const id = nextId('nv');
    const member: StaffMember = { ...req, id, code: id.toUpperCase(), workStatus: 'WORKING', completedThisMonth: 0 };
    db.staff.push(member);
    return delay(member);
  },

  /** GET /admin/staff/{id}/tasks */
  listTasks(staffId: string): Promise<StaffTask[]> {
    if (!env.useMock) return http.get(`/admin/staff/${staffId}/tasks`);
    return delay(db.tasks.filter((t) => t.staffId === staffId));
  },

  /** POST /admin/staff/{id}/tasks */
  createTask(req: CreateTaskRequest): Promise<StaffTask> {
    if (!env.useMock) return http.post(`/admin/staff/${req.staffId}/tasks`, req);
    const task: StaffTask = { ...req, id: nextId('t'), completed: false };
    db.tasks.push(task);
    return delay(task);
  },

  /** PATCH /admin/staff/tasks/{id} */
  toggleTask(taskId: string, completed: boolean): Promise<StaffTask> {
    if (!env.useMock) return http.patch(`/admin/staff/tasks/${taskId}`, { completed });
    const task = db.tasks.find((t) => t.id === taskId)!;
    task.completed = completed;
    return delay(task, 100);
  },

  /** DELETE /admin/staff/tasks/{id} */
  deleteTask(taskId: string): Promise<void> {
    if (!env.useMock) return http.delete(`/admin/staff/tasks/${taskId}`);
    db.tasks = db.tasks.filter((t) => t.id !== taskId);
    return delay(undefined, 100);
  },
};
