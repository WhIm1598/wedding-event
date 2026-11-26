import { useState, type FormEvent } from 'react';
import { Building2, KeyRound, LogOut, Pencil, Plus, UserPlus, X } from 'lucide-react';
import { errorMessage } from '@/api/http';
import { platformApi } from '@/api/platform.api';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/contexts/ToastContext';
import { useAsync } from '@/hooks/useAsync';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field, Input, Textarea } from '@/components/ui/Form';
import { LogoInput } from '@/components/ui/LogoInput';
import { Modal } from '@/components/ui/Modal';
import { EmptyState, ErrorState, Spinner } from '@/components/ui/States';
import { STUDIO_STATUS } from '@/constants/labels';
import { formatDate } from '@/lib/format';
import { ChangePasswordModal } from '@/features/auth/ChangePasswordModal';
import { TemporaryPasswordModal } from '@/features/settings/AccountsSection';
import type { CreatedAccount, NewStudioAdmin, Studio } from '@/types';

/** FN-PLT-01..03 (ADR-0001): the platform admin's only screen — studios, their subscription, logo and managers */
export function PlatformPage() {
  const { user, logout } = useAuth();
  const showToast = useToast();
  const { data: studios = [], loading, error, reload, setData } = useAsync(() => platformApi.list());
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<Studio | null>(null);
  const [renaming, setRenaming] = useState<Studio | null>(null);
  const [addingAdmin, setAddingAdmin] = useState<Studio | null>(null);
  const [issued, setIssued] = useState<CreatedAccount | CreatedAccount[] | null>(null);
  const [changingPassword, setChangingPassword] = useState(false);

  const replace = (s: Studio) => setData((list) => list?.map((x) => (x.id === s.id ? s : x)));

  const toggle = async (s: Studio) => {
    const suspend = s.status === 'ACTIVE';
    if (suspend && !window.confirm(`Tạm ngưng ${s.name}? Mọi người của studio sẽ không đăng nhập được.`)) return;
    try {
      replace(await platformApi.update(s.id, { status: suspend ? 'SUSPENDED' : 'ACTIVE', subscriptionExpiresAt: s.subscriptionExpiresAt }));
    } catch (err) {
      showToast(errorMessage(err), 'error');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-white border-b border-slate-200 px-4 sm:px-8 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Building2 className="text-rose-600" />
          <div>
            <h1 className="font-bold text-slate-900">Quản trị nền tảng</h1>
            <p className="text-xs text-slate-500">{user?.name}</p>
          </div>
        </div>
        <div className="flex gap-2">
          <Button variant="ghost" onClick={() => setChangingPassword(true)} aria-label="Đổi mật khẩu">
            <KeyRound size={18} />
          </Button>
          <Button variant="ghost" onClick={logout}>
            <LogOut size={18} className="mr-2" /> Đăng xuất
          </Button>
        </div>
      </header>
      <ChangePasswordModal isOpen={changingPassword} onClose={() => setChangingPassword(false)} />

      <main className="max-w-5xl mx-auto p-4 sm:p-8 space-y-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">Studio</h2>
            <p className="text-sm text-slate-500">Tạo studio cho khách mua, gia hạn hoặc tạm ngưng khi hết hạn thuê bao.</p>
          </div>
          <Button onClick={() => setCreating(true)}>
            <Plus size={18} className="mr-2" /> Thêm studio
          </Button>
        </div>

        {loading && <Spinner />}
        {error && <ErrorState error={error} onRetry={reload} />}
        {!loading && !error && studios.length === 0 && <EmptyState message="Chưa có studio nào" />}

        <div className="grid gap-4">
          {studios.map((s) => (
            <Card key={s.id} className="p-5 flex flex-col sm:flex-row sm:items-center gap-4">
              <StudioMark studio={s} />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-semibold text-slate-900">{s.name ?? '—'}</span>
                  <Badge variant={STUDIO_STATUS[s.status].variant}>{STUDIO_STATUS[s.status].label}</Badge>
                  {s.status === 'ACTIVE' && !s.usable && <Badge variant="warning">Hết hạn</Badge>}
                </div>
                <p className="text-sm text-slate-500">
                  Hạn thuê bao: {s.subscriptionExpiresAt ? formatDate(s.subscriptionExpiresAt) : 'Không giới hạn'} · {s.adminCount} quản lý · tạo{' '}
                  {formatDate(s.createdAt)}
                </p>
              </div>
              <div className="flex gap-2 flex-wrap">
                <Button variant="outline" onClick={() => setRenaming(s)} aria-label={`Sửa tên và logo của ${s.name}`}>
                  <Pencil size={16} />
                </Button>
                <Button variant="outline" onClick={() => setEditing(s)}>
                  Gia hạn
                </Button>
                <Button variant="outline" onClick={() => setAddingAdmin(s)} aria-label={`Thêm quản lý cho ${s.name}`}>
                  <UserPlus size={16} />
                </Button>
                <Button variant={s.status === 'ACTIVE' ? 'ghost' : 'secondary'} onClick={() => toggle(s)}>
                  {s.status === 'ACTIVE' ? 'Tạm ngưng' : 'Kích hoạt'}
                </Button>
              </div>
            </Card>
          ))}
        </div>
      </main>

      {creating && (
        <CreateStudioModal
          onClose={() => setCreating(false)}
          onCreated={(created) => {
            setCreating(false);
            setData((list) => [...(list ?? []), created.studio]);
            setIssued(created.admins);
          }}
        />
      )}
      {editing && (
        <ExtendModal
          studio={editing}
          onClose={() => setEditing(null)}
          onSaved={(s) => {
            setEditing(null);
            replace(s);
            showToast('Đã cập nhật hạn thuê bao');
          }}
        />
      )}
      {renaming && (
        <EditStudioModal
          studio={renaming}
          onClose={() => setRenaming(null)}
          onSaved={(s) => {
            setRenaming(null);
            replace(s);
            showToast('Đã cập nhật studio');
          }}
        />
      )}
      {addingAdmin && (
        <AddAdminModal
          studio={addingAdmin}
          onClose={() => setAddingAdmin(null)}
          onCreated={(created) => {
            setAddingAdmin(null);
            setIssued(created);
            reload();
          }}
        />
      )}
      {issued && <TemporaryPasswordModal issued={issued} onClose={() => setIssued(null)} />}
    </div>
  );
}

/** The studio's logo, or its initial when it has none */
function StudioMark({ studio }: { studio: Studio }) {
  return studio.logo ? (
    <img src={studio.logo} alt="" className="w-10 h-10 rounded-lg object-contain border border-slate-200 bg-white shrink-0" />
  ) : (
    <div className="w-10 h-10 rounded-lg bg-rose-50 text-rose-600 font-bold flex items-center justify-center shrink-0">
      {(studio.name ?? '?').charAt(0)}
    </div>
  );
}

const MAX_ADMINS = 10;
const text = (fd: FormData, key: string) => String(fd.get(key) ?? '').trim() || undefined;

function CreateStudioModal({ onClose, onCreated }: { onClose: () => void; onCreated: (c: Awaited<ReturnType<typeof platformApi.create>>) => void }) {
  const showToast = useToast();
  const [saving, setSaving] = useState(false);
  const [logo, setLogo] = useState<string | null>(null);
  const [admins, setAdmins] = useState<NewStudioAdmin[]>([{ email: '', name: '' }]);

  const setAdmin = (i: number, patch: Partial<NewStudioAdmin>) => setAdmins((list) => list.map((a, j) => (j === i ? { ...a, ...patch } : a)));

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSaving(true);
    try {
      onCreated(
        await platformApi.create({
          name: String(fd.get('name')).trim(),
          subscriptionExpiresAt: String(fd.get('subscriptionExpiresAt') || '') || null,
          logo,
          address: text(fd, 'address'),
          taxCode: text(fd, 'taxCode'),
          legalRepresentative: text(fd, 'legalRepresentative'),
          bankInfo: text(fd, 'bankInfo'),
          admins: admins.map((a) => ({ email: a.email.trim(), name: a.name.trim() })),
        }),
      );
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal isOpen onClose={onClose} title="Thêm studio">
      <form onSubmit={submit} className="space-y-4">
        <Field label="Tên studio" required>
          <Input name="name" required maxLength={200} />
        </Field>
        <Field label="Logo (PNG/JPEG, tự thu nhỏ)">
          <LogoInput value={logo} onChange={setLogo} />
        </Field>
        <Field label="Hạn thuê bao (để trống = không giới hạn)">
          <Input name="subscriptionExpiresAt" type="date" />
        </Field>
        <details className="rounded-lg border border-slate-200 px-4 py-3">
          <summary className="cursor-pointer text-sm font-medium text-slate-700">Thông tin hợp đồng (tuỳ chọn)</summary>
          <div className="space-y-3 pt-3">
            <Field label="Địa chỉ">
              <Input name="address" maxLength={255} />
            </Field>
            <Field label="Mã số thuế">
              <Input name="taxCode" maxLength={20} />
            </Field>
            <Field label="Người đại diện pháp luật">
              <Input name="legalRepresentative" maxLength={100} />
            </Field>
            <Field label="Thông tin ngân hàng">
              <Textarea name="bankInfo" rows={3} maxLength={500} />
            </Field>
          </div>
        </details>
        <div className="space-y-2">
          <p className="text-sm font-medium text-slate-700">
            Quản lý <span className="text-rose-600">*</span>
          </p>
          {admins.map((a, i) => (
            <div key={i} className="flex gap-2">
              <Input
                aria-label={`Email quản lý ${i + 1}`}
                placeholder="Email"
                type="email"
                required
                maxLength={255}
                value={a.email}
                onChange={(e) => setAdmin(i, { email: e.target.value })}
              />
              <Input
                aria-label={`Họ tên quản lý ${i + 1}`}
                placeholder="Họ tên"
                required
                maxLength={100}
                value={a.name}
                onChange={(e) => setAdmin(i, { name: e.target.value })}
              />
              {admins.length > 1 && (
                <Button type="button" variant="ghost" onClick={() => setAdmins((list) => list.filter((_, j) => j !== i))} aria-label={`Bỏ quản lý ${i + 1}`}>
                  <X size={16} />
                </Button>
              )}
            </div>
          ))}
          {admins.length < MAX_ADMINS && (
            <Button type="button" variant="ghost" onClick={() => setAdmins((list) => [...list, { email: '', name: '' }])}>
              <Plus size={16} className="mr-1" /> Thêm quản lý
            </Button>
          )}
        </div>
        <p className="text-xs text-slate-500">Mỗi quản lý nhận mật khẩu tạm (hiện một lần). Nhân viên do quản lý tự thêm trong Cài đặt → Tài khoản.</p>
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" loading={saving}>
            Tạo studio
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function EditStudioModal({ studio, onClose, onSaved }: { studio: Studio; onClose: () => void; onSaved: (s: Studio) => void }) {
  const showToast = useToast();
  const [saving, setSaving] = useState(false);
  const [logo, setLogo] = useState<string | null>(studio.logo);

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const name = String(new FormData(e.currentTarget).get('name')).trim();
    setSaving(true);
    try {
      // logo '' removes it (FN-PLT-02)
      onSaved(await platformApi.update(studio.id, { status: studio.status, subscriptionExpiresAt: studio.subscriptionExpiresAt, name, logo: logo ?? '' }));
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal isOpen onClose={onClose} title={`Sửa studio — ${studio.name ?? ''}`}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Tên studio" required>
          <Input name="name" required maxLength={200} defaultValue={studio.name ?? ''} />
        </Field>
        <Field label="Logo (PNG/JPEG, tự thu nhỏ)">
          <LogoInput value={logo} onChange={setLogo} />
        </Field>
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" loading={saving}>
            Lưu
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function ExtendModal({ studio, onClose, onSaved }: { studio: Studio; onClose: () => void; onSaved: (s: Studio) => void }) {
  const showToast = useToast();
  const [saving, setSaving] = useState(false);

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const value = String(new FormData(e.currentTarget).get('subscriptionExpiresAt') || '') || null;
    setSaving(true);
    try {
      onSaved(await platformApi.update(studio.id, { status: studio.status, subscriptionExpiresAt: value }));
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal isOpen onClose={onClose} title={`Gia hạn — ${studio.name ?? ''}`}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Hạn thuê bao mới (để trống = không giới hạn)">
          <Input name="subscriptionExpiresAt" type="date" defaultValue={studio.subscriptionExpiresAt ?? ''} />
        </Field>
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" loading={saving}>
            Lưu
          </Button>
        </div>
      </form>
    </Modal>
  );
}

function AddAdminModal({ studio, onClose, onCreated }: { studio: Studio; onClose: () => void; onCreated: (c: CreatedAccount) => void }) {
  const showToast = useToast();
  const [saving, setSaving] = useState(false);

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSaving(true);
    try {
      onCreated(await platformApi.createAdmin(studio.id, String(fd.get('email')).trim(), String(fd.get('name')).trim()));
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal isOpen onClose={onClose} title={`Thêm quản lý — ${studio.name ?? ''}`}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Email" required>
          <Input name="email" type="email" required maxLength={255} />
        </Field>
        <Field label="Họ tên" required>
          <Input name="name" required maxLength={100} />
        </Field>
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" loading={saving}>
            Tạo tài khoản
          </Button>
        </div>
      </form>
    </Modal>
  );
}
