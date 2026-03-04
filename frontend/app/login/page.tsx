'use client';
import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { setCredentials, isLoggedIn } from '@/lib/auth';
import { API_BASE } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isLoggedIn()) router.replace('/dashboard');
  }, [router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      // Test credentials against a lightweight endpoint
      const encoded = btoa(`${username}:${password}`);
      const res = await fetch(`${API_BASE}/v1/departments?tenantId=tenant1`, {
        headers: { Authorization: `Basic ${encoded}` },
      });
      if (res.status === 401 || res.status === 403) {
        setError('Invalid username or password.');
        return;
      }
      if (!res.ok && res.status !== 200) {
        // If endpoint itself errored but auth passed (non-4xx), still allow login
        if (res.status >= 400 && res.status < 500) {
          setError('Invalid username or password.');
          return;
        }
      }
      setCredentials(username, password);
      router.push('/dashboard');
    } catch {
      setError('Cannot reach the server. Make sure the backend is running on port 8080.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-800 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-sm">
        {/* Logo / headline */}
        <div className="text-center mb-8">
          <div className="text-5xl mb-3">🍞</div>
          <h1 className="text-2xl font-bold text-white">BreadCost</h1>
          <p className="text-slate-400 text-sm mt-1">Manufacturing Management</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          <h2 className="text-lg font-semibold text-gray-800 mb-6">Sign in</h2>

          {error && (
            <div className="mb-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Username
              </label>
              <input
                className="input"
                type="text"
                autoComplete="username"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Password
              </label>
              <input
                className="input"
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <button
              type="submit"
              className="btn-primary w-full justify-center py-2.5 text-base mt-2"
              disabled={loading}
            >
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <div className="mt-5 pt-4 border-t text-center">
            <p className="text-xs text-gray-400 mb-2">Demo accounts</p>
            <div className="grid grid-cols-2 gap-2 text-xs text-gray-500">
              {[
                ['admin', 'admin'],
                ['production', 'production'],
                ['finance', 'finance'],
                ['viewer', 'viewer'],
              ].map(([u, p]) => (
                <button
                  key={u}
                  type="button"
                  onClick={() => {
                    setUsername(u);
                    setPassword(p);
                  }}
                  className="border rounded px-2 py-1 hover:bg-gray-50 transition-colors text-left"
                >
                  <span className="font-medium text-gray-700">{u}</span>
                  <span className="text-gray-400"> / {p}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
