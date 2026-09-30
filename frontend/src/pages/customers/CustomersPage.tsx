import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Users,
  Search,
  ArrowRight,
  Sparkles
} from 'lucide-react';
import { api, type CustomerSummary, type PageResponse } from '../../services/api';
import { StatusBadge } from '../../components/common/StatusBadge';

export const CustomersPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSegment, setSelectedSegment] = useState('');
  const [selectedChurnRisk, setSelectedChurnRisk] = useState('');
  const [page, setPage] = useState(0);

  const { data: customerPage, isLoading } = useQuery<PageResponse<CustomerSummary>>({
    queryKey: ['customers', searchQuery, selectedSegment, selectedChurnRisk, page],
    queryFn: () =>
      api.getCustomers({
        query: searchQuery || undefined,
        segment: selectedSegment || undefined,
        churnRisk: selectedChurnRisk || undefined,
        page,
        size: 15,
      }),
  });

  const formatINR = (val: number) =>
    '₹' + Number(val || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 });

  const segments = ['VIP', 'High Intent', 'Loyal', 'New Customer', 'At Risk', 'Dormant', 'Cart Abandoner'];
  const churnRisks = ['LOW', 'MEDIUM', 'HIGH'];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Users className="w-6 h-6 text-indigo-400" />
            Customer Intelligence
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Segment customers by purchase intent, historical spending velocity, and churn risk.
          </p>
        </div>

        <button
          onClick={() => navigate('/agent')}
          className="px-4 py-2 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/30 border border-indigo-500/30 text-indigo-300 text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer"
        >
          <Sparkles className="w-4 h-4 text-amber-300" />
          <span>Ask AI: "Target high-intent customers"</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="glass-card rounded-2xl p-4 border border-slate-800 flex flex-col md:flex-row gap-3 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by customer name or email..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(0);
            }}
            className="w-full bg-slate-900/80 border border-slate-700/80 rounded-xl pl-10 pr-4 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2.5 w-full md:w-auto">
          {/* Segment Selector */}
          <select
            value={selectedSegment}
            onChange={(e) => {
              setSelectedSegment(e.target.value);
              setPage(0);
            }}
            className="bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-indigo-500 cursor-pointer"
          >
            <option value="">All Segments</option>
            {segments.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>

          {/* Churn Risk Selector */}
          <select
            value={selectedChurnRisk}
            onChange={(e) => {
              setSelectedChurnRisk(e.target.value);
              setPage(0);
            }}
            className="bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-indigo-500 cursor-pointer"
          >
            <option value="">All Churn Risks</option>
            {churnRisks.map((c) => (
              <option key={c} value={c}>
                Churn Risk: {c}
              </option>
            ))}
          </select>

          {(searchQuery || selectedSegment || selectedChurnRisk) && (
            <button
              onClick={() => {
                setSearchQuery('');
                setSelectedSegment('');
                setSelectedChurnRisk('');
                setPage(0);
              }}
              className="text-xs text-slate-400 hover:text-slate-200 underline px-2 cursor-pointer"
            >
              Reset
            </button>
          )}
        </div>
      </div>

      {/* Customer Table */}
      <div className="glass-card rounded-3xl border border-slate-800 overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-xs text-slate-400 animate-pulse">
            Loading customer intelligence profiles...
          </div>
        ) : customerPage && customerPage.content.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/80 text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="py-3.5 px-6 font-semibold">Customer</th>
                  <th className="py-3.5 px-4 font-semibold">Segment</th>
                  <th className="py-3.5 px-4 font-semibold">Category</th>
                  <th className="py-3.5 px-4 font-semibold">Purchase Intent</th>
                  <th className="py-3.5 px-4 font-semibold">Orders & Spend</th>
                  <th className="py-3.5 px-4 font-semibold">Churn Risk</th>
                  <th className="py-3.5 px-6 text-right font-semibold">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {customerPage.content.map((c) => (
                  <tr
                    key={c.id}
                    onClick={() => navigate(`/customers/${c.id}`)}
                    className="hover:bg-slate-800/40 transition-colors cursor-pointer group"
                  >
                    <td className="py-4 px-6">
                      <div className="font-semibold text-white group-hover:text-indigo-400 transition-colors">
                        {c.name}
                      </div>
                      <div className="text-[11px] text-slate-400">{c.email}</div>
                    </td>
                    <td className="py-4 px-4">
                      <StatusBadge status={c.segment || 'Standard'} type="segment" />
                    </td>
                    <td className="py-4 px-4 text-slate-300 font-medium">
                      {c.preferredCategory || 'General'}
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-20 bg-slate-800 rounded-full h-2 overflow-hidden">
                          <div
                            className={`h-2 rounded-full ${
                              c.purchaseIntentScore >= 75
                                ? 'bg-emerald-400'
                                : c.purchaseIntentScore >= 45
                                ? 'bg-amber-400'
                                : 'bg-slate-500'
                            }`}
                            style={{ width: `${c.purchaseIntentScore}%` }}
                          ></div>
                        </div>
                        <span className="font-bold text-white text-xs">{c.purchaseIntentScore}</span>
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <div className="font-semibold text-white">{formatINR(c.totalSpend)}</div>
                      <div className="text-[11px] text-slate-400">{c.totalOrders} orders</div>
                    </td>
                    <td className="py-4 px-4">
                      <StatusBadge status={c.churnRisk || 'LOW'} type="churn" />
                    </td>
                    <td className="py-4 px-6 text-right">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          navigate(`/customers/${c.id}`);
                        }}
                        className="p-2 rounded-xl bg-slate-800 hover:bg-indigo-600 hover:text-white text-slate-300 transition-all inline-flex items-center gap-1 text-xs font-semibold cursor-pointer"
                      >
                        <span>Deep Dive</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12 text-center">
            <Users className="w-10 h-10 text-slate-600 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-300">No customers found</p>
            <p className="text-xs text-slate-500 mt-1">Try adjusting your filters or search terms.</p>
          </div>
        )}

        {/* Pagination */}
        {customerPage && customerPage.totalPages > 1 && (
          <div className="p-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span>
              Showing {customerPage.number * customerPage.size + 1} -{' '}
              {Math.min((customerPage.number + 1) * customerPage.size, customerPage.totalElements)} of{' '}
              {customerPage.totalElements} customers
            </span>
            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-200 disabled:opacity-40 cursor-pointer"
              >
                Previous
              </button>
              <button
                disabled={page >= customerPage.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-slate-200 disabled:opacity-40 cursor-pointer"
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
