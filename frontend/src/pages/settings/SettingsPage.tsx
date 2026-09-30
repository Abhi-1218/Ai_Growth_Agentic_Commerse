import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Settings,
  Building,
  Save,
  CheckCircle2,
  Database
} from 'lucide-react';
import { api, type UserProfile } from '../../services/api';

export const SettingsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [businessName, setBusinessName] = useState('');
  const [businessDescription, setBusinessDescription] = useState('');
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [seedStatus, setSeedStatus] = useState<string | null>(null);

  const seedMutation = useMutation({
    mutationFn: () => api.seedDemoData(),
    onSuccess: (data) => {
      setSeedStatus(data.message || 'Demo data successfully generated!');
      queryClient.invalidateQueries();
      setTimeout(() => setSeedStatus(null), 6000);
    },
    onError: (err: any) => {
      setSeedStatus(err.message || 'Failed to seed data');
      setTimeout(() => setSeedStatus(null), 6000);
    }
  });

  const { data: userProfile, isLoading } = useQuery<UserProfile>({
    queryKey: ['workspaceSettings'],
    queryFn: async () => {
      const data = await api.getMe();
      setBusinessName(data.businessName || '');
      setBusinessDescription(data.businessDescription || '');
      return data;
    },
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      api.updateWorkspaceProfile({
        name: businessName,
        description: businessDescription,
      }),
    onSuccess: () => {
      setSaveSuccess(true);
      queryClient.invalidateQueries({ queryKey: ['userProfile'] });
      queryClient.invalidateQueries({ queryKey: ['workspaceSettings'] });
      setTimeout(() => setSaveSuccess(false), 4000);
    },
  });

  if (isLoading) {
    return <div className="p-12 text-center text-xs text-slate-400 animate-pulse">Loading workspace settings...</div>;
  }

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
          <Settings className="w-6 h-6 text-indigo-400" />
          Workspace & AI Settings
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Configure store profiles, currency display, and AI provider integration parameters.
        </p>
      </div>

      {saveSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4" />
          <span>Workspace profile settings updated successfully!</span>
        </div>
      )}

      {/* Store Workspace Form */}
      <div className="glass-card rounded-3xl p-6 lg:p-8 border border-slate-800 space-y-6">
        <h2 className="text-base font-bold text-white flex items-center gap-2">
          <Building className="w-4 h-4 text-indigo-400" />
          Store Profile Configuration
        </h2>

        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Workspace / Store Name
            </label>
            <input
              type="text"
              value={businessName}
              onChange={(e) => setBusinessName(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Store Description & Category Focus
            </label>
            <textarea
              rows={3}
              value={businessDescription}
              onChange={(e) => setBusinessDescription(e.target.value)}
              className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Admin Email
              </label>
              <input
                type="text"
                disabled
                value={userProfile?.email || 'admin@techmart.in'}
                className="w-full bg-slate-900/50 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-slate-400 cursor-not-allowed"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Account Role
              </label>
              <input
                type="text"
                disabled
                value={userProfile?.role || 'ADMIN'}
                className="w-full bg-slate-900/50 border border-slate-800 rounded-xl px-3.5 py-2.5 text-xs text-slate-400 cursor-not-allowed"
              />
            </div>
          </div>
        </div>

        <div className="pt-4 border-t border-slate-800 flex justify-end">
          <button
            onClick={() => updateMutation.mutate()}
            disabled={updateMutation.isPending}
            className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/30 flex items-center gap-2 cursor-pointer transition-all disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>{updateMutation.isPending ? 'Saving...' : 'Save Workspace Changes'}</span>
          </button>
        </div>
      </div>

      {/* Development & Testing Tools (Seed Demo Data) */}
      <div className="glass-card rounded-3xl p-6 lg:p-8 border border-slate-800 space-y-4">
        <h2 className="text-base font-bold text-white flex items-center gap-2">
          <Database className="w-4 h-4 text-emerald-400" />
          Development & Testing Tools
        </h2>
        <p className="text-xs text-slate-400">
          Razorpay Test Mode only provides payment events. Populate this workspace with a realistic e-commerce product catalog, abandoned carts, and opportunities to test the AI Copilot and intelligence modules.
        </p>

        {seedStatus && (
          <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4" />
            <span>{seedStatus}</span>
          </div>
        )}

        <div className="flex items-center justify-between p-4 rounded-2xl bg-slate-900/80 border border-slate-800">
          <div>
            <div className="text-xs font-semibold text-slate-200">Seed E-Commerce Catalog & Scenarios</div>
            <div className="text-[11px] text-slate-500 mt-0.5">Creates 30 products, customers, carts, and opportunities for this store.</div>
          </div>
          <button
            onClick={() => seedMutation.mutate()}
            disabled={seedMutation.isPending}
            className="px-4 py-2 rounded-xl bg-emerald-600/20 hover:bg-emerald-600/30 border border-emerald-500/30 text-emerald-300 font-semibold text-xs transition-all cursor-pointer disabled:opacity-50"
          >
            {seedMutation.isPending ? 'Generating...' : 'Generate Demo Data'}
          </button>
        </div>
      </div>
    </div>
  );
};
