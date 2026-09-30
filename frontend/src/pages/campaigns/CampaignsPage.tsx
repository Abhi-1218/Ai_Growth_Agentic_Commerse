import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Megaphone,
  Plus,
  Bot,
  X
} from 'lucide-react';
import { api, type Campaign, type PageResponse } from '../../services/api';
import { StatusBadge } from '../../components/common/StatusBadge';

export const CampaignsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [name, setName] = useState('');
  const [targetSegment, setTargetSegment] = useState('High Intent');
  const [offerDetails, setOfferDetails] = useState('');
  const [generatedMessage, setGeneratedMessage] = useState('');
  const [estimatedImpact] = useState(25000);

  const { data: campaignPage, isLoading } = useQuery<PageResponse<Campaign>>({
    queryKey: ['campaigns'],
    queryFn: () => api.getCampaigns(0, 20),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      api.createCampaign({
        name,
        targetSegment,
        offerDetails,
        generatedMessage,
        estimatedImpact,
        status: 'ACTIVE',
      }),
    onSuccess: () => {
      setShowCreateModal(false);
      setName('');
      setOfferDetails('');
      setGeneratedMessage('');
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      api.updateCampaignStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
    },
  });

  const formatINR = (val: number) =>
    '₹' + Number(val || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 });

  const segments = ['VIP', 'High Intent', 'Loyal', 'New Customer', 'At Risk', 'Dormant', 'Cart Abandoner'];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Megaphone className="w-6 h-6 text-indigo-400" />
            Campaign & Offer Management
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Design targeted promotional campaigns, automated discount sequences, and AI win-back triggers.
          </p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 text-white font-bold text-xs shadow-lg shadow-indigo-600/25 flex items-center gap-2 transition-all cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          <span>Launch New Campaign</span>
        </button>
      </div>

      {/* Campaigns Grid */}
      {isLoading ? (
        <div className="p-12 text-center text-xs text-slate-400 animate-pulse">Loading active campaigns...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {campaignPage?.content.map((c) => (
            <div
              key={c.id}
              className="glass-card rounded-3xl p-6 border border-slate-800 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between gap-3 mb-3">
                  <div className="flex items-center gap-2">
                    <StatusBadge status={c.status} type="campaign" />
                    {c.createdByAgent && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 flex items-center gap-1">
                        <Bot className="w-3 h-3 text-cyan-300" /> AI Agent Created
                      </span>
                    )}
                  </div>
                  <span className="text-xs font-bold text-emerald-400">
                    Est. Impact: {formatINR(c.estimatedImpact)}
                  </span>
                </div>

                <h3 className="text-base font-bold text-white mb-1.5">{c.name}</h3>
                <p className="text-xs text-indigo-300 font-medium mb-3">
                  Target Segment: <strong className="text-white">{c.targetSegment}</strong>
                </p>

                <div className="space-y-2 text-xs">
                  <div className="p-3 rounded-2xl bg-slate-900 border border-slate-800">
                    <span className="text-[10px] uppercase font-bold text-slate-400 block mb-1">
                      Offer Mechanism
                    </span>
                    <p className="text-slate-300">{c.offerDetails}</p>
                  </div>

                  <div className="p-3 rounded-2xl bg-slate-900 border border-slate-800">
                    <span className="text-[10px] uppercase font-bold text-indigo-400 block mb-1">
                      Personalized Message Copy
                    </span>
                    <p className="text-slate-300 italic">"{c.generatedMessage}"</p>
                  </div>
                </div>
              </div>

              {/* Status Controls */}
              <div className="mt-5 pt-4 border-t border-slate-800 flex items-center justify-between">
                <span className="text-[11px] text-slate-500">
                  Created {new Date(c.createdAt).toLocaleDateString('en-IN')}
                </span>

                <div className="flex items-center gap-2">
                  {c.status === 'PENDING_APPROVAL' && (
                    <>
                      <button
                        onClick={() => updateStatusMutation.mutate({ id: c.id, status: 'ACTIVE' })}
                        className="px-3 py-1 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 text-xs font-semibold cursor-pointer"
                      >
                        Approve & Launch
                      </button>
                      <button
                        onClick={() => updateStatusMutation.mutate({ id: c.id, status: 'REJECTED' })}
                        className="px-3 py-1 rounded-lg bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 text-xs font-semibold cursor-pointer"
                      >
                        Reject
                      </button>
                    </>
                  )}
                  {c.status === 'ACTIVE' && (
                    <button
                      onClick={() => updateStatusMutation.mutate({ id: c.id, status: 'COMPLETED' })}
                      className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold cursor-pointer"
                    >
                      Mark Completed
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 lg:p-8 max-w-lg w-full border border-slate-700 shadow-2xl">
            <div className="flex items-start justify-between gap-4 mb-4">
              <h2 className="text-lg font-bold text-white">Create Targeted Growth Campaign</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                createMutation.mutate();
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Campaign Title
                </label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. VIP Early Access Flash Sale"
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Target Customer Segment
                </label>
                <select
                  value={targetSegment}
                  onChange={(e) => setTargetSegment(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500 cursor-pointer"
                >
                  {segments.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Offer Mechanism
                </label>
                <input
                  type="text"
                  required
                  value={offerDetails}
                  onChange={(e) => setOfferDetails(e.target.value)}
                  placeholder="e.g. Flat 15% discount + Free Express Delivery"
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Promotional Copy / Message
                </label>
                <textarea
                  rows={3}
                  required
                  value={generatedMessage}
                  onChange={(e) => setGeneratedMessage(e.target.value)}
                  placeholder="Exclusive offer for our VIP members..."
                  className="w-full bg-slate-900 border border-slate-700 rounded-xl p-3 text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs font-semibold cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="px-5 py-2 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 text-white font-bold text-xs shadow-lg cursor-pointer disabled:opacity-50"
                >
                  {createMutation.isPending ? 'Deploying...' : 'Launch Campaign'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
