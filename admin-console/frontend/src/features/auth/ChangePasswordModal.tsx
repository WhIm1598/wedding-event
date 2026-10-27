import { useState, type FormEvent } from "react";
import { KeyRound } from "lucide-react";
import { authApi } from "@/api/auth.api";
import { errorMessage } from "@/api/http";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/contexts/ToastContext";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Form";
import { Modal } from "@/components/ui/Modal";

/** FN-ADM-AUTH-04: change one's own password. `forced`: first login with a temporary password (FN-ADM-AUTH-01 step 6) */
export function ChangePasswordModal({
  isOpen,
  onClose,
  forced = false,
}: {
  isOpen: boolean;
  onClose: () => void;
  forced?: boolean;
}) {
  const showToast = useToast();
  const { setUser } = useAuth();
  const [saving, setSaving] = useState(false);

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    const newPassword = String(fd.get("newPassword"));
    if (newPassword !== fd.get("confirmPassword")) {
      showToast("Mật khẩu nhập lại không khớp", "error");
      return;
    }
    setSaving(true);
    try {
      setUser(
        await authApi.changePassword(
          String(fd.get("currentPassword")),
          newPassword,
        ),
      );
      showToast(
        forced
          ? "Đã đổi mật khẩu, chào mừng bạn!"
          : "Đã đổi mật khẩu. Các thiết bị khác cần đăng nhập lại.",
      );
      onClose();
    } catch (err) {
      showToast(errorMessage(err), "error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        <>
          <KeyRound className="mr-2" size={20} /> Đổi mật khẩu
        </>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        {forced && (
          <p className="text-sm text-slate-600">
            Bạn đang dùng mật khẩu tạm do quản lý cấp. Hãy đặt mật khẩu mới để
            tiếp tục.
          </p>
        )}
        <Field
          label={forced ? "Mật khẩu tạm" : "Mật khẩu hiện tại"}
          required
        >
          <Input
            name="currentPassword"
            type="password"
            autoComplete="current-password"
            required
          />
        </Field>
        <Field label="Mật khẩu mới (ít nhất 8 ký tự, có chữ và số)" required>
          <Input
            name="newPassword"
            type="password"
            autoComplete="new-password"
            minLength={8}
            maxLength={100}
            required
          />
        </Field>
        <Field label="Nhập lại mật khẩu mới" required>
          <Input
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            required
          />
        </Field>
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="ghost" onClick={onClose}>
            {forced ? "Đăng xuất" : "Hủy"}
          </Button>
          <Button type="submit" loading={saving}>
            Đổi mật khẩu
          </Button>
        </div>
      </form>
    </Modal>
  );
}
