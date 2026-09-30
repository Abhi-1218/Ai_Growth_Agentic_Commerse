import React, { useState, useEffect } from 'react';
import { api } from '../../services/api';

export const RazorpayPage: React.FC = () => {
  const [status, setStatus] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [testing, setTesting] = useState(false);
  const [saving, setSaving] = useState(false);
  
  const [credentials, setCredentials] = useState({
    keyId: '',
    keySecret: '',
    webhookSecret: ''
  });

  const [message, setMessage] = useState<{type: 'success' | 'error', text: string} | null>(null);

  useEffect(() => {
    fetchStatus();
  }, []);

  const fetchStatus = async () => {
    try {
      const res = await api.getRazorpayStatus();
      setStatus(res);
    } catch (error) {
      console.error('Failed to fetch status', error);
    } finally {
      setLoading(false);
    }
  };

  const handleTestConnection = async () => {
    setTesting(true);
    setMessage(null);
    try {
      const res = await api.testRazorpayConnection(credentials);
      if (res.success) {
        setMessage({ type: 'success', text: 'Connection successful!' });
      } else {
        setMessage({ type: 'error', text: res.message });
      }
    } catch (error: any) {
      setMessage({ type: 'error', text: 'Connection test failed: ' + error.message });
    } finally {
      setTesting(false);
    }
  };

  const handleSaveCredentials = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setMessage(null);
    try {
      const res = await api.saveRazorpayCredentials(credentials);
      if (res.success) {
        setMessage({ type: 'success', text: 'Credentials saved successfully' });
        fetchStatus();
      }
    } catch (error: any) {
      setMessage({ type: 'error', text: 'Failed to save credentials' });
    } finally {
      setSaving(false);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    setMessage(null);
    try {
      const res = await api.syncRazorpay();
      if (res.success) {
        setMessage({ type: 'success', text: 'Synchronization successful' });
        fetchStatus();
      }
    } catch (error: any) {
      setMessage({ type: 'error', text: 'Synchronization failed: ' + (error.message || 'Unknown error') });
      fetchStatus();
    } finally {
      setSyncing(false);
    }
  };

  if (loading) {
    return <div className="p-8 text-slate-300">Loading Integration Status...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <span className="text-[#3395FF]">Razorpay</span> Integration
          </h1>
          <p className="text-slate-400">Connect Razorpay to ingest orders, payments, and refunds for AI growth intelligence.</p>
        </div>
        <div className="flex items-center gap-4">
          <div className={`px-3 py-1 rounded-full text-xs font-medium border ${
            status?.configured 
              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
              : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
          }`}>
            {status?.configured ? 'Connected' : 'Not Configured'}
          </div>
          {status?.configured && (
            <button
              onClick={handleSync}
              disabled={syncing}
              className="px-4 py-2 bg-[#3395FF] hover:bg-blue-600 text-white rounded-lg font-medium transition-colors disabled:opacity-50 flex items-center gap-2"
            >
              {syncing ? 'Syncing...' : 'Sync Now'}
            </button>
          )}
        </div>
      </div>

      {message && (
        <div className={`p-4 rounded-lg border ${
          message.type === 'success' 
            ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' 
            : 'bg-red-500/10 border-red-500/20 text-red-400'
        }`}>
          {message.text}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-slate-800/50 rounded-xl border border-slate-700/50 p-6 backdrop-blur-sm">
          <h2 className="text-lg font-semibold text-white mb-4">Configuration (Test Mode)</h2>
          <form onSubmit={handleSaveCredentials} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-400 mb-1">Key ID</label>
              <input
                type="text"
                placeholder={status?.configured ? '••••••••••••' : 'rzp_test_...'}
                value={credentials.keyId}
                onChange={e => setCredentials({...credentials, keyId: e.target.value})}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-[#3395FF]"
                required={!status?.configured}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-400 mb-1">Key Secret</label>
              <input
                type="password"
                placeholder={status?.configured ? '••••••••••••' : 'Enter your secret'}
                value={credentials.keySecret}
                onChange={e => setCredentials({...credentials, keySecret: e.target.value})}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-[#3395FF]"
                required={!status?.configured}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-400 mb-1">Webhook Secret (Optional)</label>
              <input
                type="password"
                placeholder={status?.configured ? '••••••••••••' : 'For webhook signature validation'}
                value={credentials.webhookSecret}
                onChange={e => setCredentials({...credentials, webhookSecret: e.target.value})}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-4 py-2 text-white focus:outline-none focus:border-[#3395FF]"
              />
            </div>
            <div className="flex gap-4 pt-2">
              <button
                type="button"
                onClick={handleTestConnection}
                disabled={testing || !credentials.keyId || !credentials.keySecret}
                className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg font-medium transition-colors disabled:opacity-50"
              >
                {testing ? 'Testing...' : 'Test Connection'}
              </button>
              <button
                type="submit"
                disabled={saving || !credentials.keyId || !credentials.keySecret}
                className="flex-1 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg font-medium transition-colors disabled:opacity-50"
              >
                {saving ? 'Saving...' : 'Save Credentials'}
              </button>
            </div>
          </form>
        </div>

        <div className="bg-slate-800/50 rounded-xl border border-slate-700/50 p-6 backdrop-blur-sm">
          <h2 className="text-lg font-semibold text-white mb-4">Synchronization Status</h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center py-2 border-b border-slate-700/50">
              <span className="text-slate-400">Status</span>
              <span className={`font-medium ${
                status?.syncStatus === 'SUCCESS' ? 'text-emerald-400' 
                : status?.syncStatus === 'FAILED' ? 'text-red-400' 
                : 'text-slate-300'
              }`}>
                {status?.syncStatus || 'Never Synced'}
              </span>
            </div>
            <div className="flex justify-between items-center py-2 border-b border-slate-700/50">
              <span className="text-slate-400">Last Successful Sync</span>
              <span className="text-white">
                {status?.lastSyncAt ? new Date(status.lastSyncAt).toLocaleString() : 'N/A'}
              </span>
            </div>
            <div className="flex justify-between items-center py-2 border-b border-slate-700/50">
              <span className="text-slate-400">Records Synchronized</span>
              <span className="text-white font-medium">
                {status?.recordsSynchronized || 0}
              </span>
            </div>
            
            <div className="mt-6 p-4 bg-[#3395FF]/5 border border-[#3395FF]/20 rounded-lg">
              <h3 className="text-sm font-semibold text-[#3395FF] mb-2">How it works</h3>
              <p className="text-sm text-slate-300">
                GrowthPilot synchronizes your Customers, Orders, Payments, and Refunds directly from Razorpay into your intelligence dashboard. 
                This data empowers the AI Copilot to discover failed payment recovery opportunities and analyze refund rates automatically.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
