import { useState } from 'react';
import { Outlet, useOutletContext } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { CreateContractModal } from '@/features/contracts/CreateContractModal';

export interface LayoutContext {
  openCreateContract: () => void;
  /** bumps after a contract is created so pages can reload */
  contractsVersion: number;
}

export const useLayout = () => useOutletContext<LayoutContext>();

export function AppLayout() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isContractOpen, setIsContractOpen] = useState(false);
  const [contractsVersion, setContractsVersion] = useState(0);

  const context: LayoutContext = { openCreateContract: () => setIsContractOpen(true), contractsVersion };

  return (
    <div className="flex h-screen bg-slate-50 overflow-hidden">
      {isMobileMenuOpen && (
        <div className="fixed inset-0 bg-slate-900/50 z-20 md:hidden backdrop-blur-sm" onClick={() => setIsMobileMenuOpen(false)} />
      )}

      <Sidebar isMobileOpen={isMobileMenuOpen} onClose={() => setIsMobileMenuOpen(false)} />

      <main className="flex-1 flex flex-col h-screen overflow-hidden relative">
        <Header onOpenMenu={() => setIsMobileMenuOpen(true)} onCreateContract={context.openCreateContract} />
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 bg-slate-50/50">
          <div className="max-w-6xl mx-auto h-full">
            <Outlet context={context} />
          </div>
        </div>
      </main>

      <CreateContractModal
        isOpen={isContractOpen}
        onClose={() => setIsContractOpen(false)}
        onCreated={() => setContractsVersion((v) => v + 1)}
      />
    </div>
  );
}
