import type { ReactNode } from 'react';
import { createBrowserRouter, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Spinner } from '@/components/ui/States';
import { AppLayout } from '@/components/layout/AppLayout';
import { LoginPage } from '@/features/auth/LoginPage';
import { DashboardPage } from '@/features/dashboard/DashboardPage';
import { BookingsPage } from '@/features/bookings/BookingsPage';
import { CustomersPage } from '@/features/customers/CustomersPage';
import { PackagesPage } from '@/features/packages/PackagesPage';
import { AssetsPage } from '@/features/assets/AssetsPage';
import { StaffPage } from '@/features/staff/StaffPage';
import { FinancialsPage } from '@/features/financials/FinancialsPage';
import { SettingsPage } from '@/features/settings/SettingsPage';
import type { Role } from '@/types';

function RequireAuth({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <Spinner />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <>{children}</>;
}

function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user || !roles.includes(user.role)) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
}

const adminOnly = (el: ReactNode) => <RequireRole roles={['ROLE_ADMIN']}>{el}</RequireRole>;

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'bookings', element: <BookingsPage /> },
      { path: 'customers', element: <CustomersPage /> },
      { path: 'assets', element: <AssetsPage /> },
      { path: 'packages', element: adminOnly(<PackagesPage />) },
      { path: 'staff', element: adminOnly(<StaffPage />) },
      { path: 'financials', element: adminOnly(<FinancialsPage />) },
      { path: 'settings', element: adminOnly(<SettingsPage />) },
      { path: '*', element: <Navigate to="/dashboard" replace /> },
    ],
  },
]);
