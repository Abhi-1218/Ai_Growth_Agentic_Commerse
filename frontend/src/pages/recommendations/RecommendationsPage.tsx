import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Sparkles, RefreshCw, ArrowRight } from 'lucide-react';
import { api, type RecommendationItem } from '../../services/api';

export const RecommendationsPage: React.FC = () => {
  const [selectedCustomerId, setSelectedCustomerId] = useState<number>(1);

  const { data: customerPage } = useQuery({
    queryKey: ['recommendationCustomers'],
    queryFn: () => api.getCustomers({ page: 0, size: 50 }),
  });

  const {
    data: recommendations,
    isLoading: recsLoading,
    refetch,
  } = useQuery<RecommendationItem[]>({
    queryKey: ['recommendations', selectedCustomerId],
    queryFn: () => api.getRecommendations(selectedCustomerId),
    enabled: !!selectedCustomerId,
  });

  const regenerateMutation = useMutation({
    mutationFn: () => api.regenerateRecommendations(selectedCustomerId),
    onSuccess: () => refetch(),
  });

  const formatINR = (val: number) =>
    '₹' + Number(val || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 });

  const currentCustomer = customerPage?.content.find((c) => c.id === selectedCustomerId);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Sparkles className="w-6 h-6 text-amber-400" />
            AI Product Recommendation Engine
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Hybrid rule + scoring engine aligning purchase history, category affinity, and price elasticity.
          </p>
        </div>

        <button
          onClick={() => regenerateMutation.mutate()}
          disabled={regenerateMutation.isPending}
          className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold text-slate-200 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${regenerateMutation.isPending ? 'animate-spin' : ''}`} />
          <span>Re-compute Recommendations</span>
        </button>
      </div>

      {/* Customer Selector Ribbon */}
      <div className="glass-card rounded-2xl p-4 border border-slate-800 flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3 w-full md:w-auto">
          <label className="text-xs font-semibold text-slate-300 flex-shrink-0">
            Select Customer Profile:
          </label>
          <select
            value={selectedCustomerId}
            onChange={(e) => setSelectedCustomerId(Number(e.target.value))}
            className="bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:border-indigo-500 cursor-pointer w-full md:w-80"
          >
            {customerPage?.content.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.segment} • Intent: {c.purchaseIntentScore}/100 • Pref: {c.preferredCategory})
              </option>
            ))}
          </select>
        </div>

        {currentCustomer && (
          <div className="flex items-center gap-4 text-xs text-slate-400">
            <span>Segment: <strong className="text-white">{currentCustomer.segment}</strong></span>
            <span>Category: <strong className="text-indigo-400">{currentCustomer.preferredCategory}</strong></span>
            <span>Intent Score: <strong className="text-emerald-400">{currentCustomer.purchaseIntentScore}/100</strong></span>
          </div>
        )}
      </div>

      {/* Recommendations Output */}
      {recsLoading ? (
        <div className="p-12 text-center text-xs text-slate-400 animate-pulse">
          Computing high-affinity product recommendations...
        </div>
      ) : recommendations && recommendations.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {recommendations.map((rec, index) => (
            <div
              key={rec.id}
              className="glass-card rounded-3xl p-6 border border-slate-800 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-3">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 uppercase">
                    Rank #{index + 1} • {rec.productCategory}
                  </span>
                  <span className="text-xs font-bold text-emerald-400 bg-emerald-500/10 px-2.5 py-0.5 rounded-full border border-emerald-500/20">
                    {Math.round(rec.score * 100)}% Match
                  </span>
                </div>

                <h3 className="text-base font-bold text-white mb-1.5">{rec.productName}</h3>
                <p className="text-lg font-extrabold text-white mb-3">{formatINR(rec.productPrice)}</p>

                <div className="p-3 rounded-2xl bg-slate-900/90 border border-slate-800 text-xs text-slate-300">
                  <span className="text-[10px] uppercase font-bold text-indigo-400 block mb-1">
                    Recommendation Reason
                  </span>
                  {rec.reason}
                </div>
              </div>

              <div className="mt-5 pt-4 border-t border-slate-800/80 flex items-center justify-between">
                <span className="text-[11px] text-slate-400">Ready to pitch</span>
                <button
                  onClick={() =>
                    alert(`Targeted recommendation offer for ${rec.productName} generated for ${currentCustomer?.name}`)
                  }
                  className="px-3 py-1.5 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/40 border border-indigo-500/30 text-indigo-300 text-xs font-semibold flex items-center gap-1 cursor-pointer transition-all"
                >
                  <span>Create Offer</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="p-12 text-center glass-card rounded-3xl border border-slate-800">
          <Sparkles className="w-10 h-10 text-slate-600 mx-auto mb-2" />
          <p className="text-sm font-semibold text-slate-300">No active recommendations</p>
          <button
            onClick={() => regenerateMutation.mutate()}
            className="mt-3 text-xs text-indigo-400 underline font-semibold cursor-pointer"
          >
            Click here to generate recommendations
          </button>
        </div>
      )}
    </div>
  );
};
