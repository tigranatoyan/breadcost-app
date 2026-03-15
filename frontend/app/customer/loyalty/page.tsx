'use client';
import { useState, useEffect } from 'react';
import { getCustomerToken, getCustomerInfo } from '@/lib/customer-auth';
import { API_BASE } from '@/lib/api';
import { Star, TrendingUp, Gift, ArrowUp, ArrowDown } from 'lucide-react';
import { useDateFmt } from '@/lib/i18n';

interface LoyaltyAccount {
  accountId: string;
  pointsBalance: number;
  pointsEarned: number;
  pointsRedeemed: number;
  tierName: string;
}

interface LoyaltyTransaction {
  txId: string;
  type: string;
  points: number;
  orderId: string;
  description: string;
  createdAt: string;
}

async function custFetch<T>(path: string): Promise<T> {
  const token = getCustomerToken();
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
  });
  if (!res.ok) throw new Error(`${res.status}`);
  const text = await res.text();
  return text ? JSON.parse(text) : ({} as T);
}

export default function CustomerLoyaltyPage() {
  const fmtDate = useDateFmt();
  const info = getCustomerInfo();
  const tenantId = info?.tenantId ?? 'tenant1';
  const customerId = info?.customerId ?? '';

  const [account, setAccount] = useState<LoyaltyAccount | null>(null);
  const [history, setHistory] = useState<LoyaltyTransaction[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!customerId) return;
    Promise.all([
      custFetch<LoyaltyAccount>(`/v2/loyalty/balance?tenantId=${tenantId}&customerId=${customerId}`),
      custFetch<LoyaltyTransaction[]>(`/v2/loyalty/history?tenantId=${tenantId}&customerId=${customerId}`),
    ])
      .then(([acc, hist]) => { setAccount(acc); setHistory(hist); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [customerId, tenantId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-amber-600 border-t-transparent" />
      </div>
    );
  }

  const pts = account?.pointsBalance ?? 0;
  const earned = account?.pointsEarned ?? 0;
  const tier = account?.tierName ?? 'Standard';
  const nextTierPts = 1000;
  const progress = Math.min((earned / nextTierPts) * 100, 100);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Loyalty Dashboard</h1>

      {/* Tier status card */}
      <div className="rounded-2xl bg-gradient-to-br from-slate-900 to-slate-700 p-6 text-white shadow-xl">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-sm text-slate-300">{info?.name ?? 'Customer'}</p>
            <p className="mt-1 text-4xl font-bold">{pts.toLocaleString()}</p>
            <p className="text-sm text-slate-300">points available</p>
          </div>
          <div className="flex items-center gap-2 rounded-full bg-amber-500/20 px-3 py-1.5">
            <Star className="h-4 w-4 text-amber-400" />
            <span className="text-sm font-semibold text-amber-300">{tier}</span>
          </div>
        </div>

        {/* Progress bar to next tier */}
        <div className="mt-6">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>Progress to next tier</span>
            <span>{Math.round(progress)}%</span>
          </div>
          <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-slate-600">
            <div className="h-full rounded-full bg-amber-500 transition-all" style={{ width: `${progress}%` }} />
          </div>
        </div>
      </div>

      {/* Stats row */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="rounded-xl border bg-white p-4 shadow-sm">
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <TrendingUp className="h-4 w-4" /> Total Earned
          </div>
          <p className="mt-1 text-2xl font-bold text-gray-900">{earned.toLocaleString()}</p>
        </div>
        <div className="rounded-xl border bg-white p-4 shadow-sm">
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <Gift className="h-4 w-4" /> Redeemed
          </div>
          <p className="mt-1 text-2xl font-bold text-gray-900">{(account?.pointsRedeemed ?? 0).toLocaleString()}</p>
        </div>
        <div className="rounded-xl border bg-white p-4 shadow-sm">
          <div className="flex items-center gap-2 text-sm text-gray-500">
            <Star className="h-4 w-4" /> Redemption Rate
          </div>
          <p className="mt-1 text-2xl font-bold text-gray-900">100 pts = $1</p>
        </div>
      </div>

      {/* Tier benefits */}
      <div className="rounded-xl border bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-gray-900">Tier Benefits — {tier}</h2>
        <ul className="mt-3 space-y-2 text-sm text-gray-600">
          <li className="flex items-center gap-2"><span className="text-amber-500">✦</span> Priority order processing</li>
          <li className="flex items-center gap-2"><span className="text-amber-500">✦</span> Earn 1 point per $1 spent</li>
          <li className="flex items-center gap-2"><span className="text-amber-500">✦</span> Free delivery on orders over $50</li>
          <li className="flex items-center gap-2"><span className="text-amber-500">✦</span> Exclusive seasonal promotions</li>
        </ul>
      </div>

      {/* Points history */}
      <div className="rounded-xl border bg-white shadow-sm">
        <div className="border-b px-5 py-4">
          <h2 className="text-lg font-semibold text-gray-900">Points History</h2>
        </div>
        {history.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm text-gray-400">No transactions yet.</div>
        ) : (
          <ul className="divide-y">
            {history.map((tx) => (
              <li key={tx.txId} className="flex items-center justify-between px-5 py-3">
                <div className="flex items-center gap-3">
                  <div className={`flex h-8 w-8 items-center justify-center rounded-full ${
                    tx.type === 'AWARD' ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'
                  }`}>
                    {tx.type === 'AWARD' ? <ArrowUp className="h-4 w-4" /> : <ArrowDown className="h-4 w-4" />}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900">
                      {tx.description || (tx.type === 'AWARD' ? 'Points Earned' : 'Points Redeemed')}
                    </p>
                    <p className="text-xs text-gray-500">
                      {tx.orderId ? `Order ${tx.orderId}` : ''} · {fmtDate(tx.createdAt)}
                    </p>
                  </div>
                </div>
                <span className={`text-sm font-semibold ${tx.type === 'AWARD' ? 'text-green-600' : 'text-red-600'}`}>
                  {tx.type === 'AWARD' ? '+' : '-'}{tx.points}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
