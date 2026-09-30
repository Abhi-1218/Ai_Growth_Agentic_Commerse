import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  ShoppingCart,
  Send,
  Sparkles,
  CheckCircle2,
  X,
  Bot
} from 'lucide-react';
import { api, type CartRecovery, type PageResponse } from '../../services/api';
import { StatusBadge } from '../../components/common/StatusBadge';

export const AbandonedCartsPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [selectedCart, setSelectedCart] = useState<CartRecovery | null>(null);
  const [customDiscount, setCustomDiscount] = useState<number>(10);
  const [customMessage, setCustomMessage] = useState<string>('');
  const [page] = useState(0);
  const [successToast, setSuccessToast] = useState<string | null>(null);

  const { data: cartsPage, isLoading } = useQuery<PageResponse<CartRecovery>>({
    queryKey: ['abandonedCarts', page],
    queryFn: () => api.getAbandonedCarts(page, 10),
  });

  const recoverMutation = useMutation({
    mutationFn: (cartId: number) =>
      api.triggerCartRecovery(cartId, {
        discountPercent: customDiscount,
        message: customMessage || undefined,
        executeDirectly: false,
      }),
    onSuccess: () => {
      setSelectedCart(null);
      setSuccessToast('Recovery action proposed and added to AI Agent Approval Queue!');
      queryClient.invalidateQueries({ queryKey: ['abandonedCarts'] });
      queryClient.invalidateQueries({ queryKey: ['pendingActionsCount'] });
      setTimeout(() => setSuccessToast(null), 5000);
    },
  });

  const formatINR = (val: number) =>
    '₹' + Number(val || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 });

  const handleOpenModal = (cart: CartRecovery) => {
    setSelectedCart(cart);
    setCustomDiscount(cart.recoveryProbability >= 75 ? 10 : 15);
    setCustomMessage(cart.suggestedMessage);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <ShoppingCart className="w-6 h-6 text-cyan-400" />
            Cart Abandonment Recovery Hub
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Detect abandoned carts, compute dynamic win-back probabilities, and dispatch personalized AI recovery offers.
          </p>
        </div>

        <button
          onClick={() => navigate('/agent')}
          className="px-4 py-2 rounded-xl bg-cyan-500/10 hover:bg-cyan-500/20 border border-cyan-500/30 text-cyan-300 text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer"
        >
          <Bot className="w-4 h-4" />
          <span>Ask AI: "Find high-value abandoned carts"</span>
        </button>
      </div>

      {successToast && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4" />
            <span>{successToast}</span>
          </div>
          <button
            onClick={() => navigate('/agent/activity')}
            className="underline text-emerald-300 font-bold cursor-pointer"
          >
            Review Approvals
          </button>
        </div>
      )}

      {/* Abandoned Carts Table */}
      <div className="glass-card rounded-3xl border border-slate-800 overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-xs text-slate-400 animate-pulse">
            Scanning abandoned checkout sessions...
          </div>
        ) : cartsPage && cartsPage.content.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="py-3.5 px-6 font-semibold">Customer</th>
                  <th className="py-3.5 px-4 font-semibold">Items Preview</th>
                  <th className="py-3.5 px-4 font-semibold">Cart Total</th>
                  <th className="py-3.5 px-4 font-semibold">Recovery Prob.</th>
                  <th className="py-3.5 px-4 font-semibold">AI Incentive</th>
                  <th className="py-3.5 px-6 text-right font-semibold">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {cartsPage.content.map((c) => (
                  <tr key={c.cartId} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-4 px-6">
                      <div className="font-semibold text-white">{c.customerName}</div>
                      <div className="text-[11px] text-slate-400">{c.customerEmail}</div>
                      <div className="mt-1">
                        <StatusBadge status={c.customerSegment} type="segment" />
                      </div>
                    </td>
                    <td className="py-4 px-4 max-w-[220px]">
                      {c.items.map((item, i) => (
                        <div key={i} className="text-slate-200 font-medium truncate">
                          {item.quantity}x {item.productName}
                        </div>
                      ))}
                    </td>
                    <td className="py-4 px-4 font-bold text-white text-sm">
                      {formatINR(c.totalValue)}
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-16 bg-slate-800 rounded-full h-2 overflow-hidden">
                          <div
                            className={`h-2 rounded-full ${
                              c.recoveryProbability >= 70
                                ? 'bg-emerald-400'
                                : c.recoveryProbability >= 45
                                ? 'bg-amber-400'
                                : 'bg-rose-400'
                            }`}
                            style={{ width: `${c.recoveryProbability}%` }}
                          ></div>
                        </div>
                        <span className="font-bold text-white">{c.recoveryProbability}%</span>
                      </div>
                    </td>
                    <td className="py-4 px-4 text-cyan-300 font-semibold">
                      {c.recommendedIncentive}
                    </td>
                    <td className="py-4 px-6 text-right">
                      <button
                        onClick={() => handleOpenModal(c)}
                        className="px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-600 hover:from-cyan-400 hover:to-indigo-500 text-slate-950 font-bold text-xs shadow-md flex items-center gap-1.5 ml-auto cursor-pointer transition-all"
                      >
                        <Send className="w-3.5 h-3.5" />
                        <span>Recover Cart</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12 text-center">
            <ShoppingCart className="w-10 h-10 text-slate-600 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-300">No abandoned carts currently</p>
          </div>
        )}
      </div>

      {/* Recovery Modal */}
      {selectedCart && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 lg:p-8 max-w-lg w-full border border-slate-700 shadow-2xl">
            <div className="flex items-start justify-between gap-4 mb-4">
              <div>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-400 uppercase">
                  Cart #{selectedCart.cartId} • {selectedCart.customerName}
                </span>
                <h2 className="text-lg font-bold text-white mt-1">
                  Recover Cart Worth {formatINR(selectedCart.totalValue)}
                </h2>
              </div>
              <button
                onClick={() => setSelectedCart(null)}
                className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Discount Coupon: {customDiscount}% Instant Discount
                </label>
                <input
                  type="range"
                  min={5}
                  max={25}
                  step={5}
                  value={customDiscount}
                  onChange={(e) => setCustomDiscount(Number(e.target.value))}
                  className="w-full accent-cyan-400 cursor-pointer"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center justify-between">
                  <span>AI Generated Win-Back Message</span>
                  <span className="text-[10px] text-indigo-400 font-bold flex items-center gap-1">
                    <Sparkles className="w-3 h-3 text-amber-300" /> Tailored Copy
                  </span>
                </label>
                <textarea
                  rows={4}
                  value={customMessage}
                  onChange={(e) => setCustomMessage(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-400"
                />
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-slate-800 flex justify-end gap-3">
              <button
                onClick={() => setSelectedCart(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={() => recoverMutation.mutate(selectedCart.cartId)}
                disabled={recoverMutation.isPending}
                className="px-5 py-2 rounded-xl bg-gradient-to-r from-cyan-400 to-indigo-600 text-slate-950 font-bold text-xs shadow-lg flex items-center gap-2 cursor-pointer disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{recoverMutation.isPending ? 'Queuing Action...' : 'Dispatch via AI Agent'}</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
