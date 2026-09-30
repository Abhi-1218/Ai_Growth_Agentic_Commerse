import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  IndianRupee,
  Users,
  Zap,
  ShoppingCart,
  AlertTriangle,
  ArrowUpRight,
  Sparkles,
  Bot,
  Package,
  Layers
} from 'lucide-react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import { api, type DashboardSummary } from '../../services/api';
import { MetricCard } from '../../components/common/MetricCard';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: summary, isLoading, error } = useQuery<DashboardSummary>({
    queryKey: ['dashboardSummary'],
    queryFn: api.getDashboardSummary,
    refetchInterval: 30000,
  });

  const { data: opportunities } = useQuery({
    queryKey: ['topOpportunities'],
    queryFn: api.getOpportunities,
  });

  const executeOppMutation = useMutation({
    mutationFn: (id: number) => api.executeOpportunity(id, { autoApprove: false }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboardSummary'] });
      queryClient.invalidateQueries({ queryKey: ['topOpportunities'] });
      queryClient.invalidateQueries({ queryKey: ['pendingActionsCount'] });
      navigate('/agent/activity');
    },
  });

  if (isLoading) {
    return (
      <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-slate-800 rounded-lg w-64"></div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="h-32 bg-slate-800/60 rounded-2xl"></div>
          ))}
        </div>
        <div className="h-80 bg-slate-800/40 rounded-3xl"></div>
      </div>
    );
  }

  if (error || !summary) {
    return (
      <div className="p-8 text-center glass-card rounded-3xl border border-rose-500/30">
        <AlertTriangle className="w-12 h-12 text-rose-400 mx-auto mb-3" />
        <h2 className="text-lg font-bold text-white">Failed to load growth analytics</h2>
        <p className="text-xs text-slate-400 mt-1">Please ensure the backend API is running on port 8080.</p>
      </div>
    );
  }

  const formatINR = (val: number) =>
    '₹' + Number(val || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 });

  const SEGMENT_COLORS = ['#6366f1', '#06b6d4', '#10b981', '#f59e0b', '#f43f5e', '#a855f7', '#ec4899'];

  return (
    <div className="space-y-8">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl lg:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            Executive Growth Dashboard
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Real-time e-commerce intelligence, conversion analytics & AI growth telemetry.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/opportunities')}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-xs font-semibold text-slate-200 flex items-center gap-2 transition-all cursor-pointer"
          >
            <Zap className="w-3.5 h-3.5 text-amber-400" />
            <span>Opportunities ({summary.growthOpportunities})</span>
          </button>
          <button
            onClick={() => navigate('/agent')}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-xs font-semibold text-white shadow-lg shadow-indigo-600/30 flex items-center gap-2 transition-all cursor-pointer"
          >
            <Bot className="w-4 h-4" />
            <span>Growth Copilot</span>
          </button>
        </div>
      </div>

      {/* 6 Primary Key Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        <MetricCard
          title="Total Revenue"
          value={formatINR(summary.totalRevenue)}
          trend="+18.4%"
          isPositive={true}
          icon={IndianRupee}
          color="indigo"
          subtitle={`${summary.totalOrders} total orders`}
          onClick={() => navigate('/dashboard')}
        />
        <MetricCard
          title="Growth Opportunities"
          value={summary.growthOpportunities}
          trend="Actionable"
          isPositive={true}
          icon={Zap}
          color="amber"
          subtitle="AI-detected levers"
          onClick={() => navigate('/opportunities')}
        />
        <MetricCard
          title="Revenue at Risk"
          value={formatINR(summary.revenueAtRisk)}
          trend="High Churn"
          isPositive={false}
          icon={AlertTriangle}
          color="rose"
          subtitle="At-risk VIP segment"
          onClick={() => navigate('/customers?churnRisk=HIGH')}
        />
        <MetricCard
          title="Recoverable Carts"
          value={formatINR(summary.recoverableCartRevenue)}
          trend={`${summary.abandonedCarts} carts`}
          isPositive={true}
          icon={ShoppingCart}
          color="cyan"
          subtitle="Avg. 78% win-back prob."
          onClick={() => navigate('/abandoned-carts')}
        />
        <MetricCard
          title="High-Intent Buyers"
          value={summary.highIntentCustomers}
          trend="Score ≥70"
          isPositive={true}
          icon={Users}
          color="emerald"
          subtitle="Ready to checkout"
          onClick={() => navigate('/customers?segment=High Intent')}
        />
        <MetricCard
          title="Pending Approvals"
          value={summary.pendingApprovals}
          trend={summary.pendingApprovals > 0 ? "Review Needed" : "All clear"}
          isPositive={summary.pendingApprovals === 0}
          icon={Bot}
          color="amber"
          subtitle="Human-in-the-loop actions"
          onClick={() => navigate('/agent/activity')}
        />
      </div>

      {/* Secondary Quick Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 p-4 rounded-2xl bg-slate-900/60 border border-slate-800">
        <div className="text-center md:text-left md:border-r border-slate-800 px-4">
          <p className="text-[11px] text-slate-400 uppercase font-semibold">Average Order Value</p>
          <p className="text-lg font-bold text-white mt-0.5">{formatINR(summary.averageOrderValue)}</p>
        </div>
        <div className="text-center md:text-left md:border-r border-slate-800 px-4">
          <p className="text-[11px] text-slate-400 uppercase font-semibold">Conversion Rate</p>
          <p className="text-lg font-bold text-emerald-400 mt-0.5">{summary.overallConversionRate}%</p>
        </div>
        <div className="text-center md:text-left md:border-r border-slate-800 px-4">
          <p className="text-[11px] text-slate-400 uppercase font-semibold">Active Customers</p>
          <p className="text-lg font-bold text-white mt-0.5">{summary.activeCustomers} <span className="text-xs font-normal text-slate-400">/ {summary.totalCustomers}</span></p>
        </div>
        <div className="text-center md:text-left px-4">
          <p className="text-[11px] text-slate-400 uppercase font-semibold">Repeat Customer Rate</p>
          <p className="text-lg font-bold text-indigo-400 mt-0.5">
            {summary.totalCustomers > 0 ? Math.round((summary.repeatCustomers / summary.totalCustomers) * 100) : 0}%
          </p>
        </div>
      </div>

      {/* Top Growth Opportunity Highlight Banner */}
      {opportunities && opportunities.length > 0 && (
        <div className="glass-card rounded-3xl p-6 border border-amber-500/30 bg-gradient-to-r from-amber-500/10 via-indigo-500/5 to-transparent relative overflow-hidden">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 relative z-10">
            <div className="flex items-start gap-4">
              <div className="p-3 rounded-2xl bg-amber-500/20 text-amber-400 border border-amber-500/30 flex-shrink-0">
                <Sparkles className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500 text-slate-950 uppercase">
                    Highest Impact Opportunity
                  </span>
                  <span className="text-xs text-slate-400">
                    Confidence: <strong className="text-white">{opportunities[0].confidenceScore}%</strong>
                  </span>
                </div>
                <h3 className="text-base font-bold text-white">{opportunities[0].title}</h3>
                <p className="text-xs text-slate-300 mt-1 max-w-2xl">{opportunities[0].reason}</p>
                <p className="text-xs text-amber-300/90 mt-1 font-medium">
                  <strong>Recommended Action:</strong> {opportunities[0].recommendedAction}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-4 flex-shrink-0">
              <div className="text-right">
                <p className="text-[10px] uppercase text-slate-400 font-semibold">Potential Impact</p>
                <p className="text-xl font-extrabold text-emerald-400">{formatINR(opportunities[0].estimatedImpact)}</p>
              </div>
              <button
                onClick={() => executeOppMutation.mutate(opportunities[0].id)}
                disabled={executeOppMutation.isPending}
                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-bold text-xs shadow-lg shadow-amber-500/20 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
              >
                <Bot className="w-4 h-4" />
                <span>{executeOppMutation.isPending ? 'Proposing...' : 'Execute with AI Agent'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Revenue & Orders Trend Chart */}
        <div className="lg:col-span-2 glass-card rounded-3xl p-6 border border-slate-800">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-base font-bold text-white">Revenue & Orders Trajectory</h2>
              <p className="text-xs text-slate-400">Monthly gross merchandise value and order volume</p>
            </div>
            <div className="flex items-center gap-4 text-xs">
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-full bg-indigo-500"></span>
                <span className="text-slate-300">Revenue (₹)</span>
              </div>
            </div>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={summary.revenueTrend}>
                <defs>
                  <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="date" stroke="#64748b" fontSize={11} tickLine={false} />
                <YAxis
                  stroke="#64748b"
                  fontSize={11}
                  tickLine={false}
                  tickFormatter={(val) => `₹${val >= 1000 ? `${Math.round(val / 1000)}k` : val}`}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: '#334155',
                    borderRadius: '12px',
                    fontSize: '12px',
                    color: '#fff',
                  }}
                  formatter={(val: any) => [formatINR(val), 'Revenue']}
                />
                <Area
                  type="monotone"
                  dataKey="revenue"
                  stroke="#6366f1"
                  strokeWidth={3}
                  fillOpacity={1}
                  fill="url(#revenueGrad)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Customer Segments Distribution */}
        <div className="glass-card rounded-3xl p-6 border border-slate-800 flex flex-col justify-between">
          <div>
            <h2 className="text-base font-bold text-white mb-1">Customer Segments</h2>
            <p className="text-xs text-slate-400 mb-4">Distribution by purchasing lifecycle</p>

            <div className="h-44 w-full relative">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={summary.customerSegments}
                    dataKey="count"
                    nameKey="segment"
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={70}
                    paddingAngle={3}
                  >
                    {summary.customerSegments.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={SEGMENT_COLORS[index % SEGMENT_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: '#0f172a',
                      borderColor: '#334155',
                      borderRadius: '8px',
                      fontSize: '11px',
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-2 text-xs pt-2 border-t border-slate-800">
            {summary.customerSegments.slice(0, 4).map((seg, idx) => (
              <div key={seg.segment} className="flex items-center gap-2">
                <span
                  className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                  style={{ backgroundColor: SEGMENT_COLORS[idx % SEGMENT_COLORS.length] }}
                ></span>
                <span className="text-slate-300 truncate">{seg.segment}:</span>
                <span className="font-semibold text-white">{seg.count}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Conversion Funnel & Top Products Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Conversion Funnel */}
        <div className="glass-card rounded-3xl p-6 border border-slate-800">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Layers className="w-4 h-4 text-cyan-400" />
                Store Conversion Funnel
              </h2>
              <p className="text-xs text-slate-400">Visitor-to-purchase pipeline performance</p>
            </div>
          </div>

          <div className="space-y-4">
            {summary.conversionFunnel.map((stage) => (
              <div key={stage.stage}>
                <div className="flex justify-between text-xs mb-1.5">
                  <span className="font-medium text-slate-300">{stage.stage}</span>
                  <span className="text-slate-400">
                    <strong className="text-white">{stage.count.toLocaleString()}</strong> ({stage.conversionRate}%)
                  </span>
                </div>
                <div className="w-full bg-slate-800/80 rounded-full h-2.5 overflow-hidden">
                  <div
                    className="h-2.5 rounded-full bg-gradient-to-r from-indigo-500 via-cyan-400 to-emerald-400 transition-all duration-500"
                    style={{ width: `${Math.max(8, stage.conversionRate)}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Top Products Table */}
        <div className="glass-card rounded-3xl p-6 border border-slate-800">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Package className="w-4 h-4 text-indigo-400" />
                Top Performing Products
              </h2>
              <p className="text-xs text-slate-400">Bestsellers ranked by order volume</p>
            </div>
            <button
              onClick={() => navigate('/products')}
              className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 flex items-center gap-1 cursor-pointer"
            >
              View Catalog <ArrowUpRight className="w-3.5 h-3.5" />
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-[11px] uppercase tracking-wider text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3 font-semibold">Product</th>
                  <th className="pb-3 font-semibold">Price</th>
                  <th className="pb-3 font-semibold">Sales</th>
                  <th className="pb-3 font-semibold">Conv. Rate</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {summary.topProducts.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-3 font-medium text-white max-w-[180px] truncate">
                      {p.name}
                      <span className="block text-[10px] text-slate-400">{p.category}</span>
                    </td>
                    <td className="py-3 text-slate-300">{formatINR(p.price)}</td>
                    <td className="py-3 font-semibold text-white">{p.totalSales}</td>
                    <td className="py-3">
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        {p.conversionRate}%
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
