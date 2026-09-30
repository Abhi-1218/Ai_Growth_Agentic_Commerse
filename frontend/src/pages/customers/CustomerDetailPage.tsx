import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  ShoppingBag,
  TrendingUp,
  AlertTriangle,
  Sparkles,
  Bot,
  Mail,
  Phone,
  CheckCircle2,
  Tag
} from 'lucide-react';
import { api, type CustomerIntelligence } from '../../services/api';
import { StatusBadge } from '../../components/common/StatusBadge';

export const CustomerDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const customerId = Number(id);

  const [customDiscount, setCustomDiscount] = useState(10);
  const [showOfferModal, setShowOfferModal] = useState(false);
  const [offerSuccess, setOfferSuccess] = useState(false);

  const { data: customer, isLoading, error } = useQuery<CustomerIntelligence>({
    queryKey: ['customerIntelligence', customerId],
    queryFn: () => api.getCustomerIntelligence(customerId),
    enabled: !isNaN(customerId),
  });

  const triggerOfferMutation = useMutation({
    mutationFn: () =>
      api.copilotChat(
        `Create a personalized ${customDiscount}% discount offer for customer ID ${customerId} (${customer?.name}) with reasoning.`
      ),
    onSuccess: () => {
      setShowOfferModal(false);
      setOfferSuccess(true);
      queryClient.invalidateQueries({ queryKey: ['pendingActionsCount'] });
      setTimeout(() => setOfferSuccess(false), 5000);
    },
  });

  if (isLoading) {
    return <div className="p-12 text-center text-xs text-slate-400 animate-pulse">Loading deep customer telemetry...</div>;
  }

  if (error || !customer) {
    return (
      <div className="p-8 text-center glass-card rounded-3xl border border-rose-500/30">
        <AlertTriangle className="w-12 h-12 text-rose-400 mx-auto mb-3" />
        <h2 className="text-lg font-bold text-white">Customer not found</h2>
        <button onClick={() => navigate('/customers')} className="mt-4 text-xs text-indigo-400 underline">
          Back to Customer List
        </button>
      </div>
    );
  }

  const formatINR = (val: number) =>
    '₹' + Number(val || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 });

  return (
    <div className="space-y-8">
      {/* Back Button */}
      <button
        onClick={() => navigate('/customers')}
        className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors cursor-pointer"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Customers</span>
      </button>

      {/* Success Notification */}
      {offerSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4" />
            <span>AI Growth Action proposed and sent to Agent Approval Queue!</span>
          </div>
          <button
            onClick={() => navigate('/agent/activity')}
            className="underline text-emerald-300 font-bold cursor-pointer"
          >
            View Approvals
          </button>
        </div>
      )}

      {/* Customer Header Card */}
      <div className="glass-card rounded-3xl p-6 lg:p-8 border border-slate-800 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
        <div className="flex items-start gap-4">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-indigo-600 to-cyan-400 flex items-center justify-center text-xl font-bold text-white shadow-xl shadow-indigo-600/20 flex-shrink-0">
            {customer.name.charAt(0)}
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-2.5 mb-1.5">
              <h1 className="text-xl lg:text-2xl font-bold text-white">{customer.name}</h1>
              <StatusBadge status={customer.segment} type="segment" />
              <StatusBadge status={customer.churnRisk} type="churn" />
            </div>

            <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400">
              <span className="flex items-center gap-1.5">
                <Mail className="w-3.5 h-3.5 text-slate-500" />
                {customer.email}
              </span>
              {customer.phone && (
                <span className="flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-slate-500" />
                  {customer.phone}
                </span>
              )}
              <span className="flex items-center gap-1.5">
                <Tag className="w-3.5 h-3.5 text-slate-500" />
                Category: <strong className="text-slate-200">{customer.preferredCategory}</strong>
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3 flex-shrink-0">
          <button
            onClick={() => setShowOfferModal(true)}
            className="px-4 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 text-white text-xs font-semibold shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all cursor-pointer"
          >
            <Bot className="w-4 h-4" />
            <span>Generate AI Growth Offer</span>
          </button>
        </div>
      </div>

      {/* RFM Metrics Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-5 rounded-2xl bg-slate-900/60 border border-slate-800">
        <div>
          <p className="text-[11px] text-slate-400 uppercase font-semibold">Total Lifetime Spend</p>
          <p className="text-xl font-extrabold text-emerald-400 mt-0.5">{formatINR(customer.totalSpend)}</p>
        </div>
        <div>
          <p className="text-[11px] text-slate-400 uppercase font-semibold">Total Orders</p>
          <p className="text-xl font-extrabold text-white mt-0.5">{customer.totalOrders} orders</p>
        </div>
        <div>
          <p className="text-[11px] text-slate-400 uppercase font-semibold">Average Order Value</p>
          <p className="text-xl font-extrabold text-white mt-0.5">{formatINR(customer.averageOrderValue)}</p>
        </div>
        <div>
          <p className="text-[11px] text-slate-400 uppercase font-semibold">Recency (Last Order)</p>
          <p className="text-xl font-extrabold text-indigo-400 mt-0.5">
            {customer.daysSinceLastOrder} days ago
          </p>
        </div>
      </div>

      {/* Dual Intelligence Cards: Intent & Churn */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Purchase Intent Deep-Dive */}
        <div className="glass-card rounded-3xl p-6 border border-slate-800">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <TrendingUp className="w-4 h-4 text-emerald-400" />
                Purchase Intent Scoring Engine
              </h2>
              <p className="text-xs text-slate-400">Behavioral signals contributing to buying intent</p>
            </div>
            <div className="text-right">
              <span className="text-2xl font-black text-emerald-400">{customer.purchaseIntentScore}</span>
              <span className="text-xs text-slate-400 font-medium"> / 100</span>
              <p className="text-[10px] font-bold uppercase text-emerald-400">{customer.purchaseIntentStatus}</p>
            </div>
          </div>

          <div className="space-y-3 mt-4">
            {customer.intentFactors.map((factor) => (
              <div key={factor.signalName} className="p-3 rounded-xl bg-slate-900/60 border border-slate-800/80">
                <div className="flex items-center justify-between text-xs mb-1">
                  <span className="font-semibold text-slate-200">{factor.signalName}</span>
                  <span className="font-bold text-emerald-400">
                    +{factor.scoreContribution} / {factor.maxScore} pts
                  </span>
                </div>
                <p className="text-[11px] text-slate-400">{factor.explanation}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Churn Risk & Retention Intelligence */}
        <div className="glass-card rounded-3xl p-6 border border-slate-800 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-base font-bold text-white flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-amber-400" />
                  Churn Risk & Retention Strategy
                </h2>
                <p className="text-xs text-slate-400">Predictive attrition analysis and win-back action</p>
              </div>
              <StatusBadge status={customer.churnRisk} type="churn" />
            </div>

            <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-3 mt-2">
              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400">Churn Risk Rationale</span>
                <p className="text-xs text-slate-300 mt-0.5">{customer.churnReason}</p>
              </div>

              <div className="pt-3 border-t border-slate-800">
                <span className="text-[10px] uppercase font-bold text-indigo-400 flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                  Recommended AI Retention Action
                </span>
                <p className="text-xs text-slate-200 mt-1 font-medium bg-indigo-500/10 p-3 rounded-xl border border-indigo-500/20">
                  {customer.suggestedRetentionAction}
                </p>
              </div>
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>Engagement Index: <strong className="text-white">{customer.engagementScore}/100</strong></span>
            <span>Category Match: <strong className="text-white">{customer.preferredCategory}</strong></span>
          </div>
        </div>
      </div>

      {/* AI Product Recommendations for this customer */}
      <div className="glass-card rounded-3xl p-6 border border-slate-800">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-amber-400" />
              Tailored AI Product Recommendations
            </h2>
            <p className="text-xs text-slate-400">Hybrid rule + collaborative scoring with explainability</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {customer.recommendations.map((rec) => (
            <div key={rec.id} className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 uppercase">
                    {rec.productCategory}
                  </span>
                  <span className="text-xs font-bold text-emerald-400">
                    {Math.round(rec.score * 100)}% Match
                  </span>
                </div>
                <h3 className="text-sm font-bold text-white">{rec.productName}</h3>
                <p className="text-xs text-slate-400 mt-1 line-clamp-2">{rec.reason}</p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between">
                <span className="text-sm font-extrabold text-white">{formatINR(rec.productPrice)}</span>
                <button
                  onClick={() => setShowOfferModal(true)}
                  className="px-2.5 py-1 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 text-[11px] font-semibold transition-all cursor-pointer"
                >
                  Offer Discount
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent Orders Table */}
      <div className="glass-card rounded-3xl p-6 border border-slate-800">
        <h2 className="text-base font-bold text-white mb-4 flex items-center gap-2">
          <ShoppingBag className="w-4 h-4 text-indigo-400" />
          Recent Order History
        </h2>

        {customer.recentOrders.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3 font-semibold">Order ID</th>
                  <th className="pb-3 font-semibold">Order Date</th>
                  <th className="pb-3 font-semibold">Items</th>
                  <th className="pb-3 font-semibold">Amount</th>
                  <th className="pb-3 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {customer.recentOrders.map((ord) => (
                  <tr key={ord.id} className="hover:bg-slate-800/20">
                    <td className="py-3 font-semibold text-white">#{ord.id}</td>
                    <td className="py-3 text-slate-300">
                      {new Date(ord.orderDate).toLocaleDateString('en-IN', {
                        month: 'short',
                        day: 'numeric',
                        year: 'numeric',
                      })}
                    </td>
                    <td className="py-3 text-slate-300">{ord.itemCount} items</td>
                    <td className="py-3 font-bold text-white">{formatINR(ord.totalAmount)}</td>
                    <td className="py-3">
                      <StatusBadge status={ord.status} type="campaign" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-400">No previous orders recorded for this customer.</p>
        )}
      </div>

      {/* Offer Modal */}
      {showOfferModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 max-w-md w-full border border-slate-700 shadow-2xl">
            <h3 className="text-base font-bold text-white mb-2">Create Growth Offer for {customer.name}</h3>
            <p className="text-xs text-slate-400 mb-4">
              The AI Agent will formulate a tailored incentive and add it to the pending approval queue.
            </p>

            <div className="mb-4">
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Discount Percentage: {customDiscount}%
              </label>
              <input
                type="range"
                min={5}
                max={30}
                step={5}
                value={customDiscount}
                onChange={(e) => setCustomDiscount(Number(e.target.value))}
                className="w-full cursor-pointer accent-indigo-500"
              />
            </div>

            <div className="flex gap-3 mt-6">
              <button
                type="button"
                onClick={() => setShowOfferModal(false)}
                className="flex-1 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => triggerOfferMutation.mutate()}
                disabled={triggerOfferMutation.isPending}
                className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 text-white text-xs font-bold cursor-pointer disabled:opacity-50"
              >
                {triggerOfferMutation.isPending ? 'Submitting...' : 'Dispatch to Agent'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
