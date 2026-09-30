import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Zap,
  RefreshCw,
  Sparkles,
  Bot,
  ShoppingCart,
  Users,
  Layers,
  AlertTriangle
} from 'lucide-react';
import { api, type GrowthOpportunity } from '../../services/api';

export const OpportunitiesPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: opportunities, isLoading, refetch } = useQuery<GrowthOpportunity[]>({
    queryKey: ['growthOpportunities'],
    queryFn: api.getOpportunities,
  });

  const scanMutation = useMutation({
    mutationFn: api.generateOpportunities,
    onSuccess: () => {
      refetch();
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
    },
  });

  const executeMutation = useMutation({
    mutationFn: (id: number) => api.executeOpportunity(id, { autoApprove: false }),
    onSuccess: () => {
      refetch();
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
      queryClient.invalidateQueries({ queryKey: ['pendingActionsCount'] });
      navigate('/agent/activity');
    },
  });

  const formatINR = (val: number) =>
    '₹' + Number(val || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 });

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'RECOVER_CART':
        return <ShoppingCart className="w-5 h-5 text-cyan-400" />;
      case 'HIGH_INTENT':
        return <Users className="w-5 h-5 text-emerald-400" />;
      case 'PREVENT_CHURN':
        return <AlertTriangle className="w-5 h-5 text-rose-400" />;
      case 'BUNDLE':
      case 'UPSELL':
        return <Layers className="w-5 h-5 text-indigo-400" />;
      default:
        return <Zap className="w-5 h-5 text-amber-400" />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Zap className="w-6 h-6 text-amber-400" />
            AI Growth Opportunity Engine
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Autonomous opportunity detector ranking highest-ROI actions across carts, churn, and bundles.
          </p>
        </div>

        <button
          onClick={() => scanMutation.mutate()}
          disabled={scanMutation.isPending}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold text-xs shadow-lg shadow-amber-500/20 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${scanMutation.isPending ? 'animate-spin' : ''}`} />
          <span>{scanMutation.isPending ? 'Scanning Catalog...' : 'Scan Store for Opportunities'}</span>
        </button>
      </div>

      {/* Opportunities List */}
      {isLoading ? (
        <div className="p-12 text-center text-xs text-slate-400 animate-pulse">
          Scanning customer events, cart velocity, and catalog affinity...
        </div>
      ) : opportunities && opportunities.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {opportunities.map((opp) => (
            <div
              key={opp.id}
              className="glass-card glass-card-hover rounded-3xl p-6 border border-slate-800 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between gap-3 mb-3">
                  <div className="flex items-center gap-2.5">
                    <div className="p-2 rounded-xl bg-slate-800 border border-slate-700">
                      {getTypeIcon(opp.type)}
                    </div>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 uppercase">
                      {opp.type}
                    </span>
                  </div>

                  <div className="text-right">
                    <span className="text-xs text-slate-400">Confidence: </span>
                    <strong className="text-xs text-emerald-400 font-bold">{opp.confidenceScore}%</strong>
                  </div>
                </div>

                <h3 className="text-base font-bold text-white mb-2">{opp.title}</h3>
                <p className="text-xs text-slate-300 leading-relaxed mb-3">{opp.reason}</p>

                {/* Recommended Action Card */}
                <div className="p-3.5 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 text-xs text-slate-200">
                  <span className="text-[10px] uppercase font-bold text-indigo-400 flex items-center gap-1 mb-1">
                    <Sparkles className="w-3 h-3 text-amber-300" />
                    Recommended Growth Action
                  </span>
                  {opp.recommendedAction}
                </div>
              </div>

              <div className="mt-5 pt-4 border-t border-slate-800 flex items-center justify-between">
                <div>
                  <span className="text-[10px] uppercase text-slate-500 font-semibold block">
                    Estimated Impact
                  </span>
                  <span className="text-lg font-black text-emerald-400">
                    {formatINR(opp.estimatedImpact)}
                  </span>
                </div>

                <button
                  onClick={() => executeMutation.mutate(opp.id)}
                  disabled={executeMutation.isPending}
                  className="px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 text-white text-xs font-bold shadow-md shadow-indigo-600/30 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
                >
                  <Bot className="w-4 h-4" />
                  <span>{executeMutation.isPending ? 'Submitting...' : 'Execute via AI Agent'}</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="p-12 text-center glass-card rounded-3xl border border-slate-800">
          <Zap className="w-10 h-10 text-slate-600 mx-auto mb-2" />
          <p className="text-sm font-semibold text-slate-300">No active growth opportunities</p>
          <button
            onClick={() => scanMutation.mutate()}
            className="mt-3 text-xs text-indigo-400 underline font-semibold cursor-pointer"
          >
            Click here to run an automated scan
          </button>
        </div>
      )}
    </div>
  );
};
