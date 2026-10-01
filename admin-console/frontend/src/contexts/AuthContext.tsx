import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { authApi } from '@/api/auth.api';
import type { AuthUser, Role } from '@/types';

interface AuthContextValue {
  user: AuthUser | null;
  initializing: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<void>;
  loginAsDemo: (role: Role) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    authApi.me().then(setUser).finally(() => setInitializing(false));
  }, []);

  const login = useCallback(async (email: string, password: string) => setUser(await authApi.login(email, password)), []);
  const loginAsDemo = useCallback(async (role: Role) => setUser(await authApi.loginAsDemo(role)), []);
  const logout = useCallback(() => {
    authApi.logout();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, initializing, isAdmin: user?.role === 'ROLE_ADMIN', login, loginAsDemo, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
