import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Package,
  Sparkles,
  ArrowRight,
  Layers,
  X
} from 'lucide-react';
import { api, type ProductIntelligence, type PageResponse } from '../../services/api';
import { StatusBadge } from '../../components/common/StatusBadge';

export const ProductsPage: React.FC = () => {
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedProduct, setSelectedProduct] = useState<ProductIntelligence | null>(null);
  const [page, setPage] = useState(0);

  const { data: productPage, isLoading } = useQuery<PageResponse<ProductIntelligence>>({
    queryKey: ['products', selectedCategory, page],
    queryFn: () =>
      api.getProducts({
        category: selectedCategory || undefined,
        page,
        size: 12,
      }),
  });

  const categories = ['Electronics', 'Footwear', 'Clothing', 'Accessories', 'Appliances', 'Beauty', 'Stationery'];

  const formatINR = (val: number) =>
    '₹' + Number(val || 0).toLocaleString('en-IN', { maximumFractionDigits: 0 });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center gap-2.5">
            <Package className="w-6 h-6 text-indigo-400" />
            Product Intelligence Center
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Analyze view-to-cart velocity, conversion rates, and AI-identified bundle opportunities.
          </p>
        </div>

        {/* Category Pills */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => {
              setSelectedCategory('');
              setPage(0);
            }}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
              selectedCategory === ''
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200'
            }`}
          >
            All Categories
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => {
                setSelectedCategory(cat);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                selectedCategory === cat
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                  : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Products Grid */}
      {isLoading ? (
        <div className="p-12 text-center text-xs text-slate-400 animate-pulse">
          Loading catalog intelligence...
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {productPage?.content.map((p) => (
            <div
              key={p.id}
              onClick={() => setSelectedProduct(p)}
              className="glass-card glass-card-hover rounded-3xl p-5 border border-slate-800 flex flex-col justify-between cursor-pointer"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-2.5">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-slate-800 text-slate-300 uppercase">
                    {p.category}
                  </span>
                  <StatusBadge status={p.performanceBadge} type="product" />
                </div>

                <h3 className="text-sm font-bold text-white mb-1 line-clamp-1">{p.name}</h3>
                <p className="text-lg font-black text-white">{formatINR(p.price)}</p>

                {/* AI Insight Snippet */}
                <div className="mt-3 p-2.5 rounded-xl bg-indigo-500/5 border border-indigo-500/20 text-[11px] text-indigo-300/90 line-clamp-2">
                  <Sparkles className="w-3 h-3 text-amber-300 inline mr-1" />
                  {p.aiInsight}
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-800/80">
                <div className="grid grid-cols-3 gap-2 text-center text-[11px]">
                  <div>
                    <span className="text-slate-500 block">Views</span>
                    <strong className="text-slate-200">{p.views}</strong>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Sales</span>
                    <strong className="text-white">{p.totalSales}</strong>
                  </div>
                  <div>
                    <span className="text-slate-500 block">Conv.</span>
                    <strong className="text-emerald-400">{p.conversionRate}%</strong>
                  </div>
                </div>

                <div className="mt-3 flex items-center justify-between text-xs text-indigo-400 font-semibold pt-2 border-t border-slate-800/40">
                  <span>View Cross-Sell & Bundles</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {productPage && productPage.totalPages > 1 && (
        <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800 flex items-center justify-between text-xs text-slate-400">
          <span>Page {page + 1} of {productPage.totalPages}</span>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="px-3 py-1.5 rounded-lg bg-slate-800 text-slate-200 disabled:opacity-40 cursor-pointer"
            >
              Previous
            </button>
            <button
              disabled={page >= productPage.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-3 py-1.5 rounded-lg bg-slate-800 text-slate-200 disabled:opacity-40 cursor-pointer"
            >
              Next
            </button>
          </div>
        </div>
      )}

      {/* Product Detail & Bundle Modal */}
      {selectedProduct && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card rounded-3xl p-6 lg:p-8 max-w-2xl w-full border border-slate-700 shadow-2xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-start justify-between gap-4 mb-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 uppercase">
                    {selectedProduct.category}
                  </span>
                  <StatusBadge status={selectedProduct.performanceBadge} type="product" />
                </div>
                <h2 className="text-xl font-bold text-white">{selectedProduct.name}</h2>
                <p className="text-lg font-black text-emerald-400 mt-1">{formatINR(selectedProduct.price)}</p>
              </div>
              <button
                onClick={() => setSelectedProduct(null)}
                className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Performance Stats Bar */}
            <div className="grid grid-cols-4 gap-3 p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800 text-center text-xs mb-6">
              <div>
                <span className="text-slate-400 block text-[10px]">Total Sold</span>
                <strong className="text-white text-sm">{selectedProduct.totalSales}</strong>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px]">Total Revenue</span>
                <strong className="text-emerald-400 text-sm">{formatINR(selectedProduct.totalRevenue)}</strong>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px]">View Conv.</span>
                <strong className="text-indigo-400 text-sm">{selectedProduct.conversionRate}%</strong>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px]">Cart Conv.</span>
                <strong className="text-cyan-400 text-sm">{selectedProduct.cartConversionRate}%</strong>
              </div>
            </div>

            {/* AI Diagnostics Box */}
            <div className="p-4 rounded-2xl bg-indigo-500/10 border border-indigo-500/25 mb-6">
              <h4 className="text-xs font-bold text-indigo-300 uppercase tracking-wider flex items-center gap-1.5 mb-1">
                <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                AI Commerce Diagnostics
              </h4>
              <p className="text-xs text-slate-200 leading-relaxed">{selectedProduct.aiInsight}</p>
            </div>

            {/* Bundle & Cross-Sell Pairings */}
            <div className="space-y-4">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Layers className="w-4 h-4 text-cyan-400" />
                Suggested Bundle & Cross-Sell Opportunities
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {selectedProduct.bundleOpportunities.map((bundle) => (
                  <div key={bundle.id} className="p-3.5 rounded-2xl bg-slate-900 border border-slate-800">
                    <div className="flex justify-between items-start text-xs mb-1">
                      <span className="font-bold text-white line-clamp-1">{bundle.name}</span>
                      <span className="text-emerald-400 font-semibold">{formatINR(bundle.price)}</span>
                    </div>
                    <p className="text-[11px] text-slate-400 line-clamp-2">{bundle.reason}</p>
                    <div className="mt-2 text-[10px] text-indigo-400 font-semibold">
                      Pairing Affinity: {Math.round(bundle.affinityScore * 100)}%
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setSelectedProduct(null)}
                className="px-5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
