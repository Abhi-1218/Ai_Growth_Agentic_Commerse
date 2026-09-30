import React, { useState, useRef, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Bot,
  Send,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  User
} from 'lucide-react';
import { api, type AiChatResponse } from '../../services/api';

interface Message {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  timestamp: string;
  proposedActions?: AiChatResponse['proposedActions'];
  toolsInvoked?: string[];
}

export const AgentCopilotPage: React.FC = () => {
  const navigate = useNavigate();
  const [inputMessage, setInputMessage] = useState('');
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 'welcome',
      sender: 'agent',
      text: "### 👋 Welcome to GrowthPilot AI Copilot\n\nI am your autonomous commerce assistant, connected directly to your store's customer events, cart sessions, and product catalog.\n\nAsk me anything or pick one of the high-impact queries below to uncover immediate revenue levers.",
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const chatMutation = useMutation({
    mutationFn: (msg: string) => api.copilotChat(msg),
    onSuccess: (data) => {
      const agentMsg: Message = {
        id: Date.now().toString(),
        sender: 'agent',
        text: data.response,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        proposedActions: data.proposedActions,
        toolsInvoked: data.toolsInvoked,
      };
      setMessages((prev) => [...prev, agentMsg]);
    },
  });

  const handleSend = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!inputMessage.trim() || chatMutation.isPending) return;

    const userMsgText = inputMessage.trim();
    const userMsg: Message = {
      id: Date.now().toString(),
      sender: 'user',
      text: userMsgText,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputMessage('');
    chatMutation.mutate(userMsgText);
  };

  const handlePromptChip = (prompt: string) => {
    const userMsg: Message = {
      id: Date.now().toString(),
      sender: 'user',
      text: prompt,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages((prev) => [...prev, userMsg]);
    chatMutation.mutate(prompt);
  };

  const promptChips = [
    'Which customers should I target today?',
    'Find high-value abandoned carts',
    'Which products should I bundle?',
    'Give me the top 5 growth opportunities',
    'Create a retention campaign for customers at high churn risk',
  ];

  return (
    <div className="h-[calc(100vh-120px)] flex flex-col glass-card rounded-3xl border border-slate-800 overflow-hidden">
      {/* Copilot Header */}
      <div className="p-4 px-6 border-b border-slate-800 bg-[#0c121e] flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-indigo-500/25">
            <Bot className="w-5 h-5 text-white" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              GrowthPilot AI Copilot
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-bold">
                Online & Autonomous
              </span>
            </h2>
            <p className="text-[11px] text-slate-400">Contextual growth assistant connected to live store database</p>
          </div>
        </div>

        <button
          onClick={() => navigate('/agent/activity')}
          className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold flex items-center gap-1.5 cursor-pointer transition-all"
        >
          <ShieldCheck className="w-3.5 h-3.5 text-amber-400" />
          <span>Audit & Approvals</span>
        </button>
      </div>

      {/* Message Chat Feed */}
      <div className="flex-1 p-6 overflow-y-auto space-y-6">
        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex items-start gap-3.5 ${msg.sender === 'user' ? 'flex-row-reverse' : 'flex-row'}`}
          >
            {/* Avatar */}
            <div
              className={`w-8 h-8 rounded-xl flex items-center justify-center text-xs font-bold flex-shrink-0 ${
                msg.sender === 'user'
                  ? 'bg-indigo-600 text-white'
                  : 'bg-slate-800 border border-slate-700 text-indigo-400 shadow-md'
              }`}
            >
              {msg.sender === 'user' ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
            </div>

            {/* Chat Bubble */}
            <div
              className={`max-w-2xl rounded-3xl p-5 text-xs leading-relaxed space-y-3 ${
                msg.sender === 'user'
                  ? 'bg-indigo-600 text-white rounded-tr-sm shadow-md'
                  : 'glass-card border border-slate-800 text-slate-200 rounded-tl-sm shadow-lg'
              }`}
            >
              {/* Tool Execution Tag */}
              {msg.toolsInvoked && msg.toolsInvoked.length > 0 && (
                <div className="flex flex-wrap items-center gap-1.5 pb-2 border-b border-slate-800">
                  <span className="text-[10px] text-slate-400 font-semibold uppercase">Tool Traces:</span>
                  {msg.toolsInvoked.map((tool) => (
                    <span
                      key={tool}
                      className="px-2 py-0.5 rounded bg-slate-800/80 text-cyan-300 text-[10px] font-mono border border-slate-700"
                    >
                      {tool}()
                    </span>
                  ))}
                </div>
              )}

              {/* Message Markdown formatted */}
              <div className="prose prose-invert prose-xs max-w-none whitespace-pre-wrap">
                {msg.text}
              </div>

              {/* Proposed Actions Decision Cards */}
              {msg.proposedActions && msg.proposedActions.length > 0 && (
                <div className="pt-3 border-t border-slate-800 space-y-2.5">
                  <span className="text-[10px] uppercase font-bold text-amber-400 flex items-center gap-1">
                    <Sparkles className="w-3 h-3" />
                    AI Proposed Actions (Requires Approval)
                  </span>

                  {msg.proposedActions.map((action, i) => (
                    <div
                      key={i}
                      className="p-3.5 rounded-2xl bg-slate-900/90 border border-indigo-500/30 flex items-center justify-between gap-4"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-white text-xs">{action.title}</span>
                          <span className="text-[10px] px-1.5 py-0.2 rounded bg-indigo-500/20 text-indigo-300 font-bold">
                            {action.actionType}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-400 mt-0.5">{action.description}</p>
                        <p className="text-[10px] text-emerald-400 font-bold mt-1">
                          Estimated Impact: {action.estimatedImpact}
                        </p>
                      </div>

                      <button
                        onClick={() => navigate('/agent/activity')}
                        className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 text-white font-bold text-xs shadow-md flex items-center gap-1 flex-shrink-0 cursor-pointer"
                      >
                        <span>Review in Queue</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <span className={`block text-[10px] ${msg.sender === 'user' ? 'text-indigo-200' : 'text-slate-500'}`}>
                {msg.timestamp}
              </span>
            </div>
          </div>
        ))}

        {chatMutation.isPending && (
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-indigo-400">
              <Bot className="w-4 h-4 animate-bounce" />
            </div>
            <div className="p-4 rounded-2xl glass-card border border-slate-800 text-xs text-slate-400 flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-indigo-400 animate-ping"></div>
              <span>Observing store telemetry and computing reasoning...</span>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Quick Suggestion Chips */}
      <div className="p-3 px-6 bg-[#0c121e]/70 border-t border-slate-800/80 flex items-center gap-2 overflow-x-auto no-scrollbar">
        <span className="text-[10px] uppercase font-bold text-slate-500 flex-shrink-0">Try asking:</span>
        {promptChips.map((chip) => (
          <button
            key={chip}
            type="button"
            onClick={() => handlePromptChip(chip)}
            className="px-3 py-1 rounded-full bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 text-[11px] font-medium whitespace-nowrap transition-all cursor-pointer hover:text-white"
          >
            {chip}
          </button>
        ))}
      </div>

      {/* Message Input Box */}
      <form onSubmit={handleSend} className="p-4 px-6 bg-[#090d16] border-t border-slate-800 flex gap-3 items-center">
        <input
          type="text"
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          placeholder="Ask Copilot about customers, abandoned carts, bundles, or growth actions..."
          className="flex-1 bg-slate-900 border border-slate-700 rounded-2xl px-4 py-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 transition-all"
        />
        <button
          type="submit"
          disabled={!inputMessage.trim() || chatMutation.isPending}
          className="p-3 rounded-2xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 text-white shadow-lg shadow-indigo-600/30 disabled:opacity-40 cursor-pointer transition-all"
        >
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
};
