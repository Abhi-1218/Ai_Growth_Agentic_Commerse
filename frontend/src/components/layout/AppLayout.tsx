import React from 'react';
import { Outlet } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Sidebar } from './Sidebar';
import { Navbar } from './Navbar';
import { api } from '../../services/api';

export const AppLayout: React.FC = () => {
  const { data: userProfile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: api.getMe,
    staleTime: 60000,
  });

  const { data: actionsData } = useQuery({
    queryKey: ['pendingActionsCount'],
    queryFn: () => api.getAgentActions('PENDING_APPROVAL', 0, 1),
    refetchInterval: 15000,
  });

  const pendingCount = actionsData?.totalElements || 0;

  return (
    <div className="min-h-screen flex bg-[#090d16] text-slate-100 antialiased selection:bg-indigo-500 selection:text-white">
      <Sidebar pendingApprovalsCount={pendingCount} />
      <div className="flex-1 flex flex-col min-w-0">
        <Navbar
          businessName={userProfile?.businessName || 'TechMart India'}
          userEmail={userProfile?.email || 'admin@techmart.in'}
        />
        <main className="flex-1 p-6 lg:p-8 max-w-7xl w-full mx-auto overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
