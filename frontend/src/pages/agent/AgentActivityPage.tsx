import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Activity,
  Clock,
  ShieldCheck,
  Code,
  Check,
  X
} from 'lucide-react';
import { api, type AgentAction, type PageResponse } from '../../services/api';
import { StatusBadge } from '../../components/common/StatusBadge';

export const AgentActivityPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedStatus, setSelectedStatus] = useState<string>('');
  const [page, setPage] = useState(0);

  const { data: actionsPage, isLoading, refetch } = useQuery<PageResponse<AgentAction>>({
    queryKey: ['agentActions', selectedStatus, page],
    queryFn: () => api.getAgentActions(selectedStatus || undefined, page, 15),
  });

  const approveMutation = useMutation({
    mutationFn: (id: number) => api.approveAgentAction(id),
    onSuccess: () => {
      refetch();
      queryClient.invalidateQueries({ queryKey: ['pendingActionsCount'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
      queryClient.invalidateQueries({ queryKey: ['campaigns'] });
    },
  });

  const rejectMutation = useMutation({
    mutationFn: (id: number) => api.rejectAgentAction(id),
    onSuccess: () => {
      refetch();
      queryClient.invalidateQueries({ queryKey: ['pendingActionsCount'] });
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
    },
  });

  const statuses = ['PENDING_APPROVAL', 'EXECUTED', 'APPROVED', 'REJECTED', 'FAILED'];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Activity className="w-6 h-6 text-indigo-400" />
            AI Agent Audit Log & Approvals
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Full governance, tool execution traces, and human-in-the-loop approval workflow.
          </p>
        </div>

        {/* Status Filter */}
        <div className="flex items-center gap-2">
          <select
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value);
              setPage(0);
            }}
            className="bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2 text-xs text-slate-200 focus:outline-none focus:border-indigo-500 cursor-pointer"
          >
            <option value="">All Action States</option>
            {statuses.map((st) => (
              <option key={st} value={st}>
                {st.replace('_', ' ')}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Actions List Table */}
      <div className="glass-card rounded-3xl border border-slate-800 overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-xs text-slate-400 animate-pulse">
            Loading agent activity audit trail...
          </div>
        ) : actionsPage && actionsPage.content.length > 0 ? (
          <div className="divide-y divide-slate-800/60">
            {actionsPage.content.map((act) => (
              <div key={act.id} className="p-6 hover:bg-slate-800/30 transition-colors space-y-3">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center gap-2.5">
                    <StatusBadge status={act.status} type="action" />
                    <span className="font-bold text-white text-sm">{act.goal}</span>
                  </div>
                  <span className="text-[11px] text-slate-500 flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />
                    {new Date(act.createdAt).toLocaleString('en-IN', {
                      month: 'short',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </span>
                </div>

                {/* Reasoning Summary */}
                <div className="text-xs text-slate-300 bg-slate-900/60 p-3.5 rounded-2xl border border-slate-800">
                  <span className="text-[10px] uppercase font-bold text-indigo-400 block mb-1">
                    AI Observation & Reasoning
                  </span>
                  {act.reasoningSummary}
                </div>

                {/* Tool Invocation Traces */}
                <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400">
                  <div className="flex items-center gap-1.5 font-mono text-[11px] text-cyan-400 bg-cyan-500/10 px-2.5 py-1 rounded-lg border border-cyan-500/20">
                    <Code className="w-3.5 h-3.5" />
                    <span>{act.toolUsed || 'customAction'}()</span>
                  </div>

                  {act.parameters && (
                    <span className="font-mono text-[10px] text-slate-400 bg-slate-900 px-2.5 py-1 rounded-lg border border-slate-800 truncate max-w-md">
                      params: {act.parameters}
                    </span>
                  )}
                </div>

                {/* Execution Result (if any) */}
                {act.result && (
                  <div className="p-3 rounded-2xl bg-emerald-500/5 border border-emerald-500/20 text-xs text-emerald-300">
                    <span className="text-[10px] uppercase font-bold text-emerald-400 block mb-0.5">
                      Execution Result
                    </span>
                    {act.result}
                  </div>
                )}

                {/* Approval Action Buttons */}
                {act.status === 'PENDING_APPROVAL' && (
                  <div className="pt-2 flex items-center justify-end gap-3">
                    <button
                      onClick={() => rejectMutation.mutate(act.id)}
                      disabled={rejectMutation.isPending}
                      className="px-4 py-1.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-300 font-semibold text-xs flex items-center gap-1.5 transition-all cursor-pointer disabled:opacity-50"
                    >
                      <X className="w-3.5 h-3.5" />
                      <span>Reject Action</span>
                    </button>
                    <button
                      onClick={() => approveMutation.mutate(act.id)}
                      disabled={approveMutation.isPending}
                      className="px-4 py-1.5 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 border border-emerald-500/40 text-emerald-300 font-bold text-xs flex items-center gap-1.5 shadow-md transition-all cursor-pointer disabled:opacity-50"
                    >
                      <Check className="w-3.5 h-3.5" />
                      <span>{approveMutation.isPending ? 'Executing...' : 'Approve & Execute'}</span>
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="p-12 text-center">
            <ShieldCheck className="w-10 h-10 text-slate-600 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-300">No actions in log</p>
          </div>
        )}

        {/* Pagination */}
        {actionsPage && actionsPage.totalPages > 1 && (
          <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>Page {page + 1} of {actionsPage.totalPages}</span>
            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 rounded-lg bg-slate-800 text-slate-200 disabled:opacity-40 cursor-pointer"
              >
                Previous
              </button>
              <button
                disabled={page >= actionsPage.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 rounded-lg bg-slate-800 text-slate-200 disabled:opacity-40 cursor-pointer"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
