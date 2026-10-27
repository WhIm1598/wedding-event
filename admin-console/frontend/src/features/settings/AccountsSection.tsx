import { useState, type FormEvent } from 'react';
import { Copy, KeyRound, Pencil, Plus } from 'lucide-react';
import { errorMessage } from '@/api/http';
import { staffApi } from '@/api/staff.api';
import { usersApi } from '@/api/users.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Field, Input, Select } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { ErrorState, Spinner } from '@/components/ui/States';
import { ROLES } from '@/constants/labels';
import type { CreatedAccount, StudioRole, UserAccount } from '@/types';

const formatLogin = (iso: string | null) => (iso ? new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : 'Chưa đăng nhập');

/** FN-ADM-USER-01: Cài đặt → Tài khoản */
export function AccountsSection() {
  const showToast = useToast();
  // shortcut: one page of 100 accounts, add paging when a studio has more
  const { data, loading, error, reload } = useAsync(() => usersApi.list({ size: 100 }));
  const [editing, setEditing] = useState<UserAccount | 'new' | null>(null);
  const [issued, setIssued] = useState<CreatedAccount | null>(null);

  const resetPassword = async (u: UserAccount) => {
    if (!window.confirm(`Đặt lại mật khẩu cho ${u.name}? Mọi phiên đăng nhập của tài khoản sẽ kết thúc.`)) return;
    try {
      setIssued(await usersApi.resetPassword(u.id));
    } catch (err) {
      showToast(errorMessage(err), 'error');
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-4 border-b border-slate-100 pb-2">
        <h3 className="text-lg font-bold text-slate-900">Tài khoản đăng nhập</h3>
        <Button onClick={() => setEditing('new')}>
          <Plus size={16} className="mr-1" /> Thêm
        </Button>
      </div>

      {loading && <Spinner />}
      {error && <ErrorState error={error} onRetry={reload} />}

      <ul className="divide-y divide-slate-100">
        {data?.items.map((u) => (
          <li key={u.id} className="py-3 flex items-center gap-3">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="font-medium text-slate-900">{u.name}</span>
                <Badge variant={ROLES[u.role].variant}>{ROLES[u.role].label}</Badge>
                {!u.isActive && <Badge variant="danger">Đã khóa</Badge>}
              </div>
              <p className="text-sm text-slate-500 truncate">
                {u.email}
                {u.staffName && ` · ${u.staffName}`}
              </p>
              <p className="text-xs text-slate-400">Đăng nhập gần nhất: {formatLogin(u.lastLoginAt)}</p>
            </div>
            <button onClick={() => setEditing(u)} className="p-2 text-slate-400 hover:text-slate-700" title="Sửa" aria-label={`Sửa ${u.name}`}>
              <Pencil size={16} />
            </button>
            <button onClick={() => resetPassword(u)} className="p-2 text-slate-400 hover:text-slate-700" title="Đặt lại mật khẩu" aria-label={`Đặt lại mật khẩu ${u.name}`}>
              <KeyRound size={16} />
            </button>
          </li>
        ))}
      </ul>

      {editing && (
        <AccountFormModal
          account={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={(created) => {
            setEditing(null);
            if (created) setIssued(created);
            else showToast('Đã cập nhật tài khoản');
            reload();
          }}
        />
      )}
      {issued && <TemporaryPasswordModal issued={issued} onClose={() => setIssued(null)} />}
    </div>
  );
}

function AccountFormModal({ account, onClose, onSaved }: { account: UserAccount | null; onClose: () => void; onSaved: (created?: CreatedAccount) => void }) {
  const showToast = useToast();
  const [saving, setSaving] = useState(false);
  const { data: staff = [] } = useAsync(() => staffApi.list());

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const common = {
      name: String(fd.get('name')).trim(),
      role: fd.get('role') as StudioRole,
      staffId: String(fd.get('staffId') || '') || null,
    };
    setSaving(true);
    try {
      if (account) {
        await usersApi.update(account.id, { ...common, isActive: fd.get('isActive') === 'on' });
        onSaved();
      } else {
        onSaved(await usersApi.create({ ...common, email: String(fd.get('email')).trim() }));
      }
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal isOpen onClose={onClose} title={account ? 'Sửa tài khoản' : 'Thêm tài khoản'}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Email đăng nhập" required>
          <Input name="email" type="email" defaultValue={account?.email} disabled={!!account} required={!account} maxLength={255} />
        </Field>
        <Field label="Họ tên" required>
          <Input name="name" defaultValue={account?.name} required maxLength={100} />
        </Field>
        <Field label="Vai trò" required>
          <Select name="role" defaultValue={account?.role ?? 'ROLE_STAFF'}>
            {(Object.keys(ROLES) as StudioRole[]).map((r) => (
              <option key={r} value={r}>
                {ROLES[r].label}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Hồ sơ nhân sự (để nhân viên chỉ thấy lịch của mình)">
          <Select name="staffId" defaultValue={account?.staffId ?? ''}>
            <option value="">— Không gắn —</option>
            {staff.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} · {s.name}
              </option>
            ))}
          </Select>
        </Field>
        {account && (
          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" name="isActive" defaultChecked={account.isActive} className="rounded" />
            Đang hoạt động (bỏ chọn để khóa tài khoản và đăng xuất mọi thiết bị)
          </label>
        )}
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" loading={saving}>
            {account ? 'Lưu' : 'Tạo tài khoản'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export function TemporaryPasswordModal({ issued, onClose }: { issued: CreatedAccount; onClose: () => void }) {
  const showToast = useToast();
  const copy = () =>
    navigator.clipboard.writeText(issued.temporaryPassword).then(
      () => showToast('Đã sao chép mật khẩu tạm'),
      () => showToast('Không sao chép được, hãy chép tay', 'error'),
    );

  return (
    <Modal isOpen onClose={onClose} title="Mật khẩu tạm">
      <div className="space-y-4 text-sm text-slate-600">
        <p>
          Gửi cho <b>{issued.user.name}</b> ({issued.user.email}). Mật khẩu này <b>chỉ hiện một lần</b>; người dùng phải đổi khi đăng nhập lần đầu.
        </p>
        <div className="flex items-center gap-2">
          <code className="flex-1 px-4 py-3 bg-slate-100 rounded-lg text-lg font-mono text-slate-900 select-all">{issued.temporaryPassword}</code>
          <Button type="button" variant="ghost" onClick={copy} aria-label="Sao chép mật khẩu tạm">
            <Copy size={18} />
          </Button>
        </div>
        <div className="flex justify-end">
          <Button onClick={onClose}>Đã lưu lại</Button>
        </div>
      </div>
    </Modal>
  );
}
