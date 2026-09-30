import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Bot, Sparkles, User, Bell } from 'lucide-react';

interface NavbarProps {
  businessName?: string;
  userEmail?: string;
}

export const Navbar: React.FC<NavbarProps> = ({
  businessName = 'TechMart India',
  userEmail = 'admin@techmart.in',
}) => {
  const navigate = useNavigate();

  return (
    <header className="h-16 border-b border-slate-800 bg-[#0c121e]/80 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-30">
      {/* Left: Workspace Indicator */}
      <div className="flex items-center gap-3">
        <div className="px-3 py-1.5 rounded-lg bg-slate-800/80 border border-slate-700/80 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
          <span className="text-xs font-semibold text-slate-200">{businessName}</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded bg-indigo-500/20 text-indigo-400 uppercase font-bold">
            Live Store
          </span>
        </div>
      </div>

      {/* Right: Agent Status, Quick Copilot Trigger & User Info */}
      <div className="flex items-center gap-3">
        {/* Quick Launch Copilot Button */}
        <button
          onClick={() => navigate('/agent')}
          className="flex items-center gap-2 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white text-xs font-semibold shadow-md shadow-indigo-600/25 transition-all duration-150"
        >
          <Bot className="w-4 h-4" />
          <span>Ask Growth Copilot</span>
          <Sparkles className="w-3.5 h-3.5 text-amber-300" />
        </button>

        {/* Notifications / Alerts Placeholder */}
        <button
          onClick={() => navigate('/agent/activity')}
          title="Agent Notifications"
          className="p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-800 border border-slate-800 transition-colors relative"
        >
          <Bell className="w-4 h-4" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-amber-400"></span>
        </button>

        {/* User Pill */}
        <div className="flex items-center gap-2.5 pl-2 border-l border-slate-800">
          <div className="w-8 h-8 rounded-full bg-indigo-600/20 border border-indigo-500/40 flex items-center justify-center text-xs font-bold text-indigo-400">
            <User className="w-4 h-4" />
          </div>
          <div className="hidden md:block text-left">
            <p className="text-xs font-medium text-slate-200 truncate max-w-[150px]">{userEmail}</p>
            <p className="text-[10px] text-slate-400 font-semibold uppercase">Administrator</p>
          </div>
        </div>
      </div>
    </header>
  );
};
