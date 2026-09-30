import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { ProtectedRoute } from './components/common/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';

import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { ForgotPasswordPage } from './pages/auth/ForgotPasswordPage';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { CustomersPage } from './pages/customers/CustomersPage';
import { CustomerDetailPage } from './pages/customers/CustomerDetailPage';
import { ProductsPage } from './pages/products/ProductsPage';
import { RecommendationsPage } from './pages/recommendations/RecommendationsPage';
import { OpportunitiesPage } from './pages/opportunities/OpportunitiesPage';
import { AbandonedCartsPage } from './pages/carts/AbandonedCartsPage';
import { CampaignsPage } from './pages/campaigns/CampaignsPage';
import { AgentCopilotPage } from './pages/agent/AgentCopilotPage';
import { AgentActivityPage } from './pages/agent/AgentActivityPage';
import { SettingsPage } from './pages/settings/SettingsPage';
import { RazorpayPage } from './pages/integrations/RazorpayPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          {/* Public Auth Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />

          {/* Protected Routes inside AppLayout */}
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/customers" element={<CustomersPage />} />
              <Route path="/customers/:id" element={<CustomerDetailPage />} />
              <Route path="/products" element={<ProductsPage />} />
              <Route path="/recommendations" element={<RecommendationsPage />} />
              <Route path="/opportunities" element={<OpportunitiesPage />} />
              <Route path="/abandoned-carts" element={<AbandonedCartsPage />} />
              <Route path="/campaigns" element={<CampaignsPage />} />
              <Route path="/agent" element={<AgentCopilotPage />} />
              <Route path="/agent/activity" element={<AgentActivityPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/integrations/razorpay" element={<RazorpayPage />} />
            </Route>
          </Route>

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;
