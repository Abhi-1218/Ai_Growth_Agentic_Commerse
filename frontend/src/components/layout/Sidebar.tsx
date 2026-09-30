import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  Package,
  Sparkles,
  Zap,
  ShoppingCart,
  Megaphone,
  Bot,
  Activity,
  Settings,
  LogOut,
  TrendingUp,
  ShieldCheck,
  CreditCard,
} from 'lucide-react';

interface SidebarProps {
  pendingApprovalsCount?: number;
}

export const Sidebar: React.FC<SidebarProps> = ({ pendingApprovalsCount = 0 }) => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('growthpilot_token');
    localStorage.removeItem('growthpilot_user');
    navigate('/login');
  };

  const navItems = [
    { to: '/dashboard', label: 'Executive Dashboard', icon: LayoutDashboard },
    { to: '/customers', label: 'Customer Intelligence', icon: Users },
    { to: '/products', label: 'Product Intelligence', icon: Package },
    { to: '/recommendations', label: 'AI Recommendations', icon: Sparkles },
    { to: '/opportunities', label: 'Growth Opportunities', icon: Zap },
    { to: '/abandoned-carts', label: 'Cart Recovery', icon: ShoppingCart },
    { to: '/campaigns', label: 'Campaigns & Offers', icon: Megaphone },
    { to: '/agent', label: 'AI Growth Copilot', icon: Bot, isHighlight: true },
    {
      to: '/agent/activity',
      label: 'Agent Approvals & Audit',
      icon: Activity,
      badge: pendingApprovalsCount > 0 ? pendingApprovalsCount : undefined,
    },
    { to: '/integrations/razorpay', label: 'Razorpay Integration', icon: CreditCard },
    { to: '/settings', label: 'Workspace Settings', icon: Settings },
  ];

  return (
    <aside className="w-64 flex-shrink-0 bg-[#0c121e] border-r border-slate-800 flex flex-col justify-between h-screen sticky top-0">
      <div>
        {/* Brand Header */}
        <div className="p-5 border-b border-slate-800 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-indigo-500/25">
            <TrendingUp className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="text-base font-bold text-white tracking-tight flex items-center gap-1.5">
              GrowthPilot <span className="text-[10px] px-1.5 py-0.5 rounded bg-indigo-500/20 text-indigo-400 font-semibold uppercase">AI</span>
            </h1>
            <p className="text-[11px] text-slate-400">Agentic Commerce Engine</p>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="p-3 space-y-1 overflow-y-auto max-h-[calc(100vh-160px)]">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-150 ${
                    isActive
                      ? 'bg-indigo-600/15 text-indigo-400 border border-indigo-500/30 shadow-sm'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 border border-transparent'
                  } ${item.isHighlight ? 'hover:border-indigo-500/30' : ''}`
                }
              >
                <div className="flex items-center gap-3">
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </div>
                {item.badge !== undefined && (
                  <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-amber-500 text-slate-950 animate-pulse">
                    {item.badge}
                  </span>
                )}
              </NavLink>
            );
          })}
        </nav>
      </div>

      {/* Footer User Info & Logout */}
      <div className="p-4 border-t border-slate-800 bg-[#090e17]">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-slate-800 border border-slate-700 flex items-center justify-center text-xs font-semibold text-indigo-400">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
            </div>
            <div className="overflow-hidden">
              <p className="text-xs font-semibold text-white truncate">TechMart India</p>
              <p className="text-[10px] text-emerald-400 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span>
                AI Agent Active
              </p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            title="Sign Out"
            className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
};
