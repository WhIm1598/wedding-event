import { useState, type FormEvent } from 'react';
import { errorMessage } from '@/api/http';
import { settingsApi } from '@/api/system.api';
import { useAsync } from '@/hooks/useAsync';
import { useToast } from '@/contexts/ToastContext';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Field, Input } from '@/components/ui/Form';
import { ErrorState, Spinner } from '@/components/ui/States';

/** FN-ADM-SET-02: the studio's own Zalo Official Account for "Gửi Zalo" (setup: docs/setup/zalo-oa-zns.md) */
export function ZaloOaSection() {
  const showToast = useToast();
  const [saving, setSaving] = useState(false);
  const { data, loading, error, reload, setData } = useAsync(() => settingsApi.getZaloOa());
  if (loading) return <Spinner />;
  if (error || !data) return <ErrorState error={error ?? new Error('Không tải được cấu hình')} onRetry={reload} />;

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    setSaving(true);
    try {
      const saved = await settingsApi.updateZaloOa({
        appId: String(fd.get('appId')),
        appSecret: String(fd.get('appSecret')),
        refreshToken: String(fd.get('refreshToken')),
        templateId: String(fd.get('templateId')),
      });
      setData(() => saved);
      form.reset();
      showToast('Đã lưu cấu hình Zalo OA');
    } catch (err) {
      showToast(errorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  };

  const kept = (has: boolean) => (has ? 'Đã lưu — để trống nếu không đổi' : '');

  return (
    <form className="space-y-5" onSubmit={handleSubmit}>
      <div className="flex items-center justify-between mb-4 border-b border-slate-100 pb-2">
        <h3 className="text-lg font-bold text-slate-900">Zalo Official Account</h3>
        {data.configured ? <Badge variant="success">Đã kết nối</Badge> : <Badge variant="warning">Chưa kết nối</Badge>}
      </div>
      <p className="text-sm text-slate-500">
        Tin nhắn hợp đồng (ZNS) được gửi từ Zalo OA của studio. Lấy App ID, App Secret và Refresh Token trong trang Zalo for Developers; mã mẫu ZNS
        trong Zalo Cloud Account sau khi mẫu được duyệt.
      </p>
      <Field label="App ID" required>
        <Input name="appId" defaultValue={data.appId ?? ''} required maxLength={50} autoComplete="off" />
      </Field>
      <Field label="App Secret">
        <Input name="appSecret" type="password" placeholder={kept(data.hasAppSecret)} required={!data.hasAppSecret} maxLength={255} autoComplete="off" />
      </Field>
      <Field label="Refresh Token (OA)">
        <Input name="refreshToken" type="password" placeholder={kept(data.hasRefreshToken)} required={!data.hasRefreshToken} maxLength={2000} autoComplete="off" />
      </Field>
      <Field label="Mã mẫu ZNS gửi hợp đồng" required>
        <Input name="templateId" defaultValue={data.templateId ?? ''} required maxLength={50} autoComplete="off" />
      </Field>
      {data.accessTokenExpiresAt && (
        <p className="text-xs text-slate-400">Phiên Zalo hiện tại hết hạn lúc {new Date(data.accessTokenExpiresAt).toLocaleString('vi-VN')} (tự gia hạn khi gửi tin).</p>
      )}
      <div className="pt-4 flex justify-end">
        <Button type="submit" variant="dark" className="px-6" loading={saving}>
          Lưu
        </Button>
      </div>
    </form>
  );
}
