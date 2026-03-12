'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { API_BASE, TENANT_ID } from '@/lib/api';
import { setCustomerSession } from '@/lib/customer-auth';
import { ChefHat } from 'lucide-react';

type Tab = 'login' | 'register' | 'forgot' | 'reset';

export default function CustomerLoginPage() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>('login');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Login fields
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  // Register fields
  const [regName, setRegName] = useState('');
  const [regEmail, setRegEmail] = useState('');
  const [regPassword, setRegPassword] = useState('');

  // Forgot / Reset fields
  const [forgotEmail, setForgotEmail] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const clear = () => { setError(''); setSuccess(''); };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    clear();
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/v2/customers/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tenantId: TENANT_ID, email, password }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        setError(data.message ?? 'Invalid email or password.');
        return;
      }
      const data = await res.json();
      setCustomerSession(data.token, {
        customerId: data.customerId,
        name: data.name,
        tenantId: TENANT_ID,
      });
      router.push('/customer/catalog');
    } catch {
      setError('Server unreachable. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    clear();
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/v2/customers/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenantId: TENANT_ID,
          name: regName,
          email: regEmail,
          password: regPassword,
        }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        setError(data.message ?? 'Registration failed.');
        return;
      }
      setSuccess('Account created! You can now sign in.');
      setTab('login');
      setEmail(regEmail);
    } catch {
      setError('Server unreachable. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleForgot = async (e: React.FormEvent) => {
    e.preventDefault();
    clear();
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/v2/customers/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tenantId: TENANT_ID, email: forgotEmail }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        setError(data.message ?? 'Email not found.');
        return;
      }
      const data = await res.json();
      setResetToken(data.resetToken);
      setSuccess('Reset token generated. Enter it below to set a new password.');
      setTab('reset');
    } catch {
      setError('Server unreachable. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = async (e: React.FormEvent) => {
    e.preventDefault();
    clear();
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/v2/customers/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: resetToken, newPassword }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        setError(data.message ?? 'Reset failed. Token may be expired.');
        return;
      }
      setSuccess('Password reset! You can now sign in with your new password.');
      setTab('login');
    } catch {
      setError('Server unreachable. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-amber-50 to-orange-50 flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="text-center mb-8">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-600 text-white shadow-lg mb-3">
            <ChefHat className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">BreadCost</h1>
          <p className="text-gray-500 text-sm mt-1">Customer Portal</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-6">
          {/* Tab switcher (login/register) */}
          {(tab === 'login' || tab === 'register') && (
            <div className="flex gap-1 mb-6 bg-gray-100 rounded-lg p-1">
              {(['login', 'register'] as const).map((t) => (
                <button
                  key={t}
                  onClick={() => { setTab(t); clear(); }}
                  className={`flex-1 rounded-md py-2 text-sm font-medium transition-colors ${
                    tab === t ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'
                  }`}
                >
                  {t === 'login' ? 'Sign In' : 'Register'}
                </button>
              ))}
            </div>
          )}

          {/* Alerts */}
          {error && (
            <div className="mb-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">
              {error}
            </div>
          )}
          {success && (
            <div className="mb-4 bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg px-4 py-3">
              {success}
            </div>
          )}

          {/* Login form */}
          {tab === 'login' && (
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input className="input" type="email" required value={email}
                       onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                <input className="input" type="password" required value={password}
                       onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" />
              </div>
              <button type="submit" className="btn-primary w-full justify-center py-2.5" disabled={loading}>
                {loading ? 'Signing in…' : 'Sign In'}
              </button>
              <button type="button" onClick={() => { setTab('forgot'); clear(); }}
                      className="w-full text-center text-sm text-amber-600 hover:text-amber-700">
                Forgot password?
              </button>
            </form>
          )}

          {/* Register form */}
          {tab === 'register' && (
            <form onSubmit={handleRegister} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                <input className="input" type="text" required value={regName}
                       onChange={(e) => setRegName(e.target.value)} autoComplete="name" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input className="input" type="email" required value={regEmail}
                       onChange={(e) => setRegEmail(e.target.value)} autoComplete="email" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
                <input className="input" type="password" required minLength={6} value={regPassword}
                       onChange={(e) => setRegPassword(e.target.value)} autoComplete="new-password" />
              </div>
              <button type="submit" className="btn-primary w-full justify-center py-2.5" disabled={loading}>
                {loading ? 'Creating account…' : 'Create Account'}
              </button>
            </form>
          )}

          {/* Forgot password form */}
          {tab === 'forgot' && (
            <form onSubmit={handleForgot} className="space-y-4">
              <p className="text-sm text-gray-500 mb-2">
                Enter your email and we&apos;ll send you a reset token.
              </p>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input className="input" type="email" required value={forgotEmail}
                       onChange={(e) => setForgotEmail(e.target.value)} autoComplete="email" />
              </div>
              <button type="submit" className="btn-primary w-full justify-center py-2.5" disabled={loading}>
                {loading ? 'Sending…' : 'Request Reset Token'}
              </button>
              <button type="button" onClick={() => { setTab('login'); clear(); }}
                      className="w-full text-center text-sm text-gray-500 hover:text-gray-700">
                ← Back to sign in
              </button>
            </form>
          )}

          {/* Reset password form */}
          {tab === 'reset' && (
            <form onSubmit={handleReset} className="space-y-4">
              <p className="text-sm text-gray-500 mb-2">
                Enter the reset token and your new password.
              </p>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Reset Token</label>
                <input className="input font-mono text-xs" type="text" required value={resetToken}
                       onChange={(e) => setResetToken(e.target.value)} />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
                <input className="input" type="password" required minLength={6} value={newPassword}
                       onChange={(e) => setNewPassword(e.target.value)} autoComplete="new-password" />
              </div>
              <button type="submit" className="btn-primary w-full justify-center py-2.5" disabled={loading}>
                {loading ? 'Resetting…' : 'Reset Password'}
              </button>
              <button type="button" onClick={() => { setTab('login'); clear(); }}
                      className="w-full text-center text-sm text-gray-500 hover:text-gray-700">
                ← Back to sign in
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
