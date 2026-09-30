import React from 'react';

interface StatusBadgeProps {
  status: string;
  type?: 'segment' | 'churn' | 'campaign' | 'action' | 'product';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const s = status ? status.toUpperCase() : 'UNKNOWN';

  let colorClasses = 'bg-slate-800 text-slate-300 border-slate-700';

  if (s === 'VIP') {
    colorClasses = 'bg-amber-500/10 text-amber-400 border-amber-500/30';
  } else if (s === 'HIGH INTENT' || s === 'HIGH_CONVERTING' || s === 'TRENDING') {
    colorClasses = 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
  } else if (s === 'LOYAL' || s === 'ACTIVE' || s === 'APPROVED' || s === 'EXECUTED') {
    colorClasses = 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30';
  } else if (s === 'PENDING_APPROVAL' || s === 'PENDING' || s === 'DRAFT') {
    colorClasses = 'bg-amber-500/10 text-amber-400 border-amber-500/30';
  } else if (s === 'AT RISK' || s === 'MEDIUM' || s === 'CART ABANDONER') {
    colorClasses = 'bg-orange-500/10 text-orange-400 border-orange-500/30';
  } else if (s === 'HIGH' || s === 'DORMANT' || s === 'REJECTED' || s === 'FAILED' || s === 'LOW_PERFORMING') {
    colorClasses = 'bg-rose-500/10 text-rose-400 border-rose-500/30';
  } else if (s === 'LOW' || s === 'COMPLETED' || s === 'STABLE') {
    colorClasses = 'bg-cyan-500/10 text-cyan-400 border-cyan-500/30';
  }

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${colorClasses}`}>
      {status}
    </span>
  );
};
