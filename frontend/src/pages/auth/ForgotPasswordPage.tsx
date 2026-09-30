import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  TrendingUp,
  Lock,
  Mail,
  KeyRound,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
  Eye,
  EyeOff,
  Sparkles,
  ShieldCheck,
  AlertCircle
} from 'lucide-react';
import { api } from '../../services/api';

type Step = 'EMAIL' | 'CODE' | 'NEW_PASSWORD' | 'SUCCESS';

export const ForgotPasswordPage: React.FC = () => {
  const navigate = useNavigate();

  const [step, setStep] = useState<Step>('EMAIL');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [demoOtp, setDemoOtp] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Step 1: Send verification code
  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;

    setLoading(true);
    setError(null);
    try {
      const res = await api.forgotPassword(email.trim());
      setSuccessMessage(res.message);
      if (res.demoOtp) {
        setDemoOtp(res.demoOtp);
      }
      setStep('CODE');
    } catch (err: any) {
      setError(err.message || 'Failed to dispatch verification code. Please check your email.');
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Verify code
  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code) return;

    setLoading(true);
    setError(null);
    try {
      const res = await api.verifyResetCode(email.trim(), code.trim());
      setSuccessMessage(res.message);
      setStep('NEW_PASSWORD');
    } catch (err: any) {
      setError(err.message || 'Invalid or expired verification code.');
    } finally {
      setLoading(false);
    }
  };

  // Step 3: Reset password
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPassword) return;

    if (newPassword.length < 6) {
      setError('Password must be at least 6 characters long.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const res = await api.resetPassword({
        email: email.trim(),
        code: code.trim(),
        newPassword
      });
      setSuccessMessage(res.message);
      setStep('SUCCESS');
    } catch (err: any) {
      setError(err.message || 'Failed to reset password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleFillDemo = () => {
    setEmail('admin@techmart.in');
  };

  const handleAutoFillOtp = () => {
    if (demoOtp) {
      setCode(demoOtp);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#090d16] p-4 text-slate-100 relative overflow-hidden">
      {/* Background ambient lighting */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-96 h-96 bg-indigo-600/15 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-10 right-10 w-72 h-72 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none"></div>

      <div className="w-full max-w-md relative z-10">
        {/* Logo & Header */}
        <div className="text-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 to-cyan-400 mx-auto flex items-center justify-center shadow-xl shadow-indigo-500/25 mb-4">
            <TrendingUp className="w-7 h-7 text-white" />
          </div>
          <h1 className="text-2xl lg:text-3xl font-bold text-white tracking-tight">
            GrowthPilot <span className="text-indigo-400">AI</span>
          </h1>
          <p className="text-sm text-slate-400 mt-1.5">
            Account Recovery & Security Verification
          </p>
        </div>

        {/* Card */}
        <div className="glass-card rounded-3xl p-8 border border-slate-800 shadow-2xl backdrop-blur-xl bg-slate-900/70">
          {/* Progress Indicators */}
          {step !== 'SUCCESS' && (
            <div className="flex items-center justify-center gap-2 mb-6">
              <div className={`h-1.5 rounded-full transition-all duration-300 ${
                step === 'EMAIL' ? 'w-8 bg-indigo-500' : 'w-4 bg-indigo-500/40'
              }`} />
              <div className={`h-1.5 rounded-full transition-all duration-300 ${
                step === 'CODE' ? 'w-8 bg-indigo-500' : 'w-4 bg-slate-700'
              }`} />
              <div className={`h-1.5 rounded-full transition-all duration-300 ${
                step === 'NEW_PASSWORD' ? 'w-8 bg-indigo-500' : 'w-4 bg-slate-700'
              }`} />
            </div>
          )}

          {/* Feedback Alerts */}
          {error && (
            <div className="mb-5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-medium flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {successMessage && step !== 'SUCCESS' && (
            <div className="mb-5 p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-medium flex items-start gap-2.5">
              <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
              <span>{successMessage}</span>
            </div>
          )}

          {/* STEP 1: ENTER WORK EMAIL */}
          {step === 'EMAIL' && (
            <div>
              <h2 className="text-lg font-bold text-white mb-2">Forgot Password?</h2>
              <p className="text-xs text-slate-400 mb-6">
                Enter your registered workspace email. We'll send a 6-digit security code to verify your identity.
              </p>

              <form onSubmit={handleSendCode} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Registered Work Email
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                    <input
                      type="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="admin@techmart.in"
                      className="w-full bg-slate-900/80 border border-slate-700/80 rounded-xl pl-10 pr-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading || !email}
                  className="w-full py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 flex items-center justify-center gap-2 transition-all disabled:opacity-50 cursor-pointer"
                >
                  {loading ? 'Sending Code...' : 'Send Verification Code'}
                  <ArrowRight className="w-4 h-4" />
                </button>
              </form>

              <div className="mt-5 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={handleFillDemo}
                  className="w-full py-2.5 px-3 rounded-xl bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/30 text-indigo-300 text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer"
                >
                  <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                  <span>Fill Demo Account (admin@techmart.in)</span>
                </button>
              </div>

              <div className="mt-6 text-center">
                <Link
                  to="/login"
                  className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-200 transition-colors"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  Back to Sign In
                </Link>
              </div>
            </div>
          )}

          {/* STEP 2: VERIFY 6-DIGIT CODE */}
          {step === 'CODE' && (
            <div>
              <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 flex items-center justify-center mb-4">
                <KeyRound className="w-6 h-6" />
              </div>
              <h2 className="text-lg font-bold text-white mb-1.5">Verify Security Code</h2>
              <p className="text-xs text-slate-400 mb-5">
                We sent a 6-digit code to <span className="text-indigo-300 font-semibold">{email}</span>.
              </p>

              {/* Demo Helper Pill */}
              {demoOtp && (
                <div
                  onClick={handleAutoFillOtp}
                  className="mb-5 p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs font-medium cursor-pointer hover:bg-amber-500/15 transition-all flex items-center justify-between"
                  title="Click to fill automatically"
                >
                  <div className="flex items-center gap-2">
                    <Sparkles className="w-4 h-4 text-amber-400 shrink-0" />
                    <span>Demo OTP: <strong className="text-white tracking-wider font-mono text-sm ml-1">{demoOtp}</strong></span>
                  </div>
                  <span className="text-[11px] underline font-semibold text-amber-200">Click to fill</span>
                </div>
              )}

              <form onSubmit={handleVerifyCode} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    6-Digit Verification Code
                  </label>
                  <input
                    type="text"
                    required
                    maxLength={6}
                    value={code}
                    onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                    placeholder="123456"
                    className="w-full text-center tracking-[0.5em] font-mono text-lg py-3 bg-slate-900/80 border border-slate-700/80 rounded-xl text-white placeholder-slate-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading || code.length !== 6}
                  className="w-full py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 flex items-center justify-center gap-2 transition-all disabled:opacity-50 cursor-pointer"
                >
                  {loading ? 'Verifying...' : 'Verify Code'}
                  <ArrowRight className="w-4 h-4" />
                </button>
              </form>

              <div className="mt-6 flex items-center justify-between text-xs text-slate-400">
                <button
                  type="button"
                  onClick={() => setStep('EMAIL')}
                  className="inline-flex items-center gap-1 hover:text-slate-200"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  Change Email
                </button>
                <button
                  type="button"
                  onClick={handleSendCode}
                  disabled={loading}
                  className="text-indigo-400 hover:text-indigo-300 font-medium"
                >
                  Resend Code
                </button>
              </div>
            </div>
          )}

          {/* STEP 3: RE-CREATE NEW PASSWORD */}
          {step === 'NEW_PASSWORD' && (
            <div>
              <div className="w-12 h-12 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 flex items-center justify-center mb-4">
                <ShieldCheck className="w-6 h-6" />
              </div>
              <h2 className="text-lg font-bold text-white mb-1.5">Reset Your Password</h2>
              <p className="text-xs text-slate-400 mb-5">
                Set a new password for <span className="text-indigo-300 font-semibold">{email}</span>. Minimum 6 characters.
              </p>

              <form onSubmit={handleResetPassword} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    New Password
                  </label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      required
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full bg-slate-900/80 border border-slate-700/80 rounded-xl pl-10 pr-10 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3.5 top-3 text-slate-400 hover:text-slate-200"
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Confirm New Password
                  </label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      required
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full bg-slate-900/80 border border-slate-700/80 rounded-xl pl-10 pr-10 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-3.5 top-3 text-slate-400 hover:text-slate-200"
                    >
                      {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                {newPassword && confirmPassword && (
                  <div className="text-[11px] font-medium">
                    {newPassword === confirmPassword ? (
                      <span className="text-emerald-400 flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5" /> Passwords match
                      </span>
                    ) : (
                      <span className="text-rose-400 flex items-center gap-1">
                        <AlertCircle className="w-3.5 h-3.5" /> Passwords do not match
                      </span>
                    )}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading || !newPassword || newPassword !== confirmPassword || newPassword.length < 6}
                  className="w-full py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 flex items-center justify-center gap-2 transition-all disabled:opacity-50 cursor-pointer"
                >
                  {loading ? 'Updating Password...' : 'Save New Password & Continue'}
                  <ArrowRight className="w-4 h-4" />
                </button>
              </form>
            </div>
          )}

          {/* STEP 4: SUCCESS CONFIRMATION */}
          {step === 'SUCCESS' && (
            <div className="text-center py-4">
              <div className="w-16 h-16 rounded-full bg-emerald-500/15 border border-emerald-500/30 text-emerald-400 flex items-center justify-center mx-auto mb-4 animate-bounce">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <h2 className="text-xl font-bold text-white mb-2">Password Reset Successful!</h2>
              <p className="text-xs text-slate-400 mb-6">
                Your workspace password has been updated. You can now securely sign in to GrowthPilot AI with your new password.
              </p>

              <button
                type="button"
                onClick={() => navigate('/login')}
                className="w-full py-3 rounded-xl bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white font-semibold text-sm shadow-lg shadow-indigo-600/30 flex items-center justify-center gap-2 transition-all cursor-pointer"
              >
                <span>Proceed to Sign In</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
