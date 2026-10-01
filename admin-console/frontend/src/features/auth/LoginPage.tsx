import { useState, type FormEvent } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Camera, Lock, Users } from 'lucide-react';
import { env } from '@/config/env';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/contexts/ToastContext';
import { Button } from '@/components/ui/Button';
import { Field, Input } from '@/components/ui/Form';
import type { Role } from '@/types';

export function LoginPage() {
  const { user, login, loginAsDemo } = useAuth();
  const showToast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [submitting, setSubmitting] = useState(false);

  const redirectTo = (location.state as { from?: string } | null)?.from ?? '/dashboard';
  if (user) return <Navigate to={redirectTo} replace />;

  const handleLogin = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const fd = new FormData(e.currentTarget);
    setSubmitting(true);
    try {
      await login(String(fd.get('email')), String(fd.get('password')));
      navigate(redirectTo, { replace: true });
    } catch (err) {
      showToast((err as Error).message || 'Đăng nhập thất bại', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDemo = async (role: Role) => {
    await loginAsDemo(role);
    navigate(redirectTo, { replace: true });
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="bg-white max-w-md w-full rounded-2xl shadow-xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in duration-500">
        <div className="bg-rose-600 p-8 text-center">
          <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center mx-auto mb-4 shadow-lg">
            <Camera size={32} className="text-rose-600" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">LUMIÈRE STUDIOS</h1>
          <p className="text-rose-100 text-sm mt-1">Hệ thống Quản lý Vận hành</p>
        </div>

        <div className="p-8 space-y-4">
          {env.useMock ? (
            <>
              <p className="text-center text-slate-500 text-sm mb-6">Vui lòng chọn vai trò đăng nhập để trải nghiệm hệ thống</p>
              <button
                onClick={() => handleDemo('ROLE_ADMIN')}
                className="w-full flex items-center justify-center gap-3 bg-slate-900 hover:bg-slate-800 text-white py-3 rounded-xl font-bold transition-all hover:scale-[1.02]"
              >
                <Lock size={18} /> Đăng nhập quyền Quản Lý
              </button>
              <button
                onClick={() => handleDemo('ROLE_STAFF')}
                className="w-full flex items-center justify-center gap-3 bg-rose-50 hover:bg-rose-100 text-rose-700 py-3 rounded-xl font-bold transition-all hover:scale-[1.02]"
              >
                <Users size={18} /> Đăng nhập quyền Nhân Viên
              </button>
              <p className="text-center text-xs text-slate-400 pt-2">Chế độ demo (VITE_USE_MOCK=true)</p>
            </>
          ) : (
            <form className="space-y-4" onSubmit={handleLogin}>
              <Field label="Email" required>
                <Input name="email" type="email" placeholder="admin@lumiere.vn" required />
              </Field>
              <Field label="Mật khẩu" required>
                <Input name="password" type="password" minLength={6} placeholder="••••••••" required />
              </Field>
              <Button type="submit" variant="dark" className="w-full py-3 rounded-xl" loading={submitting}>
                <Lock size={18} className="mr-2" /> Đăng nhập
              </Button>
              {import.meta.env.DEV && (
                <p className="text-center text-xs text-slate-400 pt-2">
                  Demo: admin@lumiere.vn / Admin@123 • staff@lumiere.vn / Staff@123
                </p>
              )}
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
