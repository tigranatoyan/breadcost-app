'use client';
import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Alert, Spinner } from '@/components/ui';

// ─── master data panels ────────────────────────────────────────────────────────
const MASTER_PANELS = [
  {
    href: '/departments',
    icon: '🏢',
    title: 'Departments',
    desc: 'Create and manage production departments. Each department groups products and sets a fallback lead time.',
    color: 'border-blue-200 bg-blue-50 hover:bg-blue-100',
  },
  {
    href: '/products',
    icon: '🍞',
    title: 'Products',
    desc: 'Define the products your facility makes. Assign each product a department, base unit of measure, and cost centre.',
    color: 'border-amber-200 bg-amber-50 hover:bg-amber-100',
  },
  {
    href: '/recipes',
    icon: '📋',
    title: 'Recipes & Formulas',
    desc: 'Manage recipe versions including ingredients, batch sizes, expected yields, lead times, and production notes.',
    color: 'border-emerald-200 bg-emerald-50 hover:bg-emerald-100',
  },
  {
    href: '/inventory',
    icon: '🏬',
    title: 'Items & Raw Materials',
    desc: 'Define the raw materials, packaging, and semi-finished goods consumed in production. Set minimum stock thresholds.',
    color: 'border-purple-200 bg-purple-50 hover:bg-purple-100',
  },
];

// ─── demo users (matches SecurityConfig hard-coded in-memory users) ────────────
const DEMO_USERS = [
  { username: 'admin',      password: 'admin',      role: 'Admin',         access: 'Full access — all screens',                color: 'bg-red-100 text-red-700' },
  { username: 'production', password: 'production', role: 'ProductionUser', access: 'Production plans, orders, floor, inventory', color: 'bg-orange-100 text-orange-700' },
  { username: 'finance',    password: 'finance',    role: 'FinanceUser',   access: 'Read-only access to plans, orders, reports', color: 'bg-blue-100 text-blue-700' },
  { username: 'viewer',     password: 'viewer',     role: 'Viewer',        access: 'Dashboard and read-only views only',         color: 'bg-gray-100 text-gray-700' },
  { username: 'cashier',    password: 'cashier',    role: 'Cashier',       access: 'POS and end-of-day reconciliation',          color: 'bg-green-100 text-green-700' },
  { username: 'warehouse',  password: 'warehouse',  role: 'Warehouse',     access: 'Inventory receiving and adjustments',        color: 'bg-purple-100 text-purple-700' },
  { username: 'technologist','password': 'technologist', role: 'Technologist', access: 'Recipe creation and management',        color: 'bg-amber-100 text-amber-700' },
];

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="text-xs font-semibold uppercase tracking-widest text-gray-400 mb-3 mt-8">
      {children}
    </h2>
  );
}

interface DbUser {
  userId: string;
  username: string;
  displayName: string;
  roles: string;
  active: boolean;
}

interface TenantConfig {
  orderCutoffTime: string;
  rushOrderPremiumPct: number;
  mainCurrency: string;
}

export default function AdminPage() {
  const [showPasswords, setShowPasswords] = useState(false);
  const [dbUsers, setDbUsers] = useState<DbUser[]>([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [config, setConfig] = useState<TenantConfig | null>(null);
  const [error, setError] = useState('');
  const [newUser, setNewUser] = useState({ username: '', password: '', roles: 'Viewer', displayName: '' });
  const [showNewUserForm, setShowNewUserForm] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const [users, cfg] = await Promise.all([
        apiFetch<DbUser[]>(`/v1/users?tenantId=${TENANT_ID}`).catch(() => []),
        apiFetch<TenantConfig>(`/v1/config?tenantId=${TENANT_ID}`).catch(() => null),
      ]);
      setDbUsers(users);
      setConfig(cfg);
    } catch (e) {
      setError(String(e));
    } finally {
      setUsersLoading(false);
    }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  const createUser = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v1/users', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, ...newUser }),
      });
      setShowNewUserForm(false);
      setNewUser({ username: '', password: '', roles: 'Viewer', displayName: '' });
      loadData();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  const toggleActive = async (u: DbUser) => {
    try {
      await apiFetch(`/v1/users/${u.userId}?tenantId=${TENANT_ID}`, {
        method: 'PUT',
        body: JSON.stringify({ ...u, active: !u.active }),
      });
      loadData();
    } catch (e) {
      setError(String(e));
    }
  };

  return (
    <div className="max-w-3xl">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold">Admin Panel</h1>
        <p className="text-sm text-gray-500 mt-1">
          System configuration &mdash; master data, users, and operational settings.
        </p>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {/* ── Master Data ────────────────────────────────────────────────────── */}
      <SectionTitle>Master Data</SectionTitle>
      <div className="grid grid-cols-1 gap-4">
        {MASTER_PANELS.map((p) => (
          <Link
            key={p.href}
            href={p.href}
            className={`flex items-start gap-5 border rounded-xl p-5 transition-colors ${p.color}`}
          >
            <span className="text-3xl">{p.icon}</span>
            <div className="flex-1 min-w-0">
              <div className="font-semibold text-gray-800">{p.title}</div>
              <div className="text-sm text-gray-600 mt-0.5">{p.desc}</div>
            </div>
            <span className="ml-auto text-gray-400 self-center">→</span>
          </Link>
        ))}
      </div>

      {/* ── DB Users ──────────────────────────────────────────────────────── */}
      <SectionTitle>Users &amp; Roles</SectionTitle>
      <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-4 py-3 border-b bg-gray-50">
          <span className="text-sm font-medium text-gray-700">
            {dbUsers.length > 0 ? `${dbUsers.length} user(s) in database` : 'Demo accounts (fallback)'}
          </span>
          <button
            className="text-xs bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
            onClick={() => setShowNewUserForm((s) => !s)}
          >
            {showNewUserForm ? 'Cancel' : '+ New User'}
          </button>
        </div>

        {showNewUserForm && (
          <form onSubmit={createUser} className="px-4 py-3 border-b bg-blue-50 grid grid-cols-2 gap-3 text-sm">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Username</label>
              <input className="input text-xs" required value={newUser.username}
                onChange={(e) => setNewUser((u) => ({ ...u, username: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Password</label>
              <input className="input text-xs" type="password" required value={newUser.password}
                onChange={(e) => setNewUser((u) => ({ ...u, password: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Display Name</label>
              <input className="input text-xs" value={newUser.displayName}
                onChange={(e) => setNewUser((u) => ({ ...u, displayName: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Role</label>
              <select className="input text-xs" value={newUser.roles}
                onChange={(e) => setNewUser((u) => ({ ...u, roles: e.target.value }))}>
                {['Admin','ProductionUser','FinanceUser','Viewer','Cashier','Warehouse','Technologist'].map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            <div className="col-span-2 flex justify-end">
              <button type="submit" className="btn-primary text-xs py-1 px-3" disabled={saving}>
                {saving ? 'Saving…' : 'Create User'}
              </button>
            </div>
          </form>
        )}

        {usersLoading ? (
          <div className="px-4 py-4"><Spinner /></div>
        ) : dbUsers.length > 0 ? (
          <table className="min-w-full text-sm">
            <thead>
              <tr className="text-xs font-semibold text-gray-500 uppercase tracking-wide bg-gray-50 border-b">
                <th className="px-4 py-2 text-left">Username</th>
                <th className="px-4 py-2 text-left">Display Name</th>
                <th className="px-4 py-2 text-left">Role</th>
                <th className="px-4 py-2 text-left">Status</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {dbUsers.map((u) => (
                <tr key={u.userId} className="hover:bg-gray-50">
                  <td className="px-4 py-2.5 font-mono font-medium">{u.username}</td>
                  <td className="px-4 py-2.5 text-gray-600">{u.displayName || '—'}</td>
                  <td className="px-4 py-2.5">
                    <span className="inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">
                      {u.roles}
                    </span>
                  </td>
                  <td className="px-4 py-2.5">
                    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                      {u.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-right">
                    <button
                      className="text-xs text-gray-500 hover:text-gray-700 underline"
                      onClick={() => toggleActive(u)}
                    >
                      {u.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <>
            <div className="flex items-center justify-between px-4 py-2 border-b bg-gray-50">
              <span className="text-xs text-gray-500">Showing demo fallback accounts (no DB users yet)</span>
              <button
                className="text-xs text-blue-600 hover:underline"
                onClick={() => setShowPasswords((s) => !s)}
              >
                {showPasswords ? 'Hide passwords' : 'Show passwords'}
              </button>
            </div>
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-xs font-semibold text-gray-500 uppercase tracking-wide bg-gray-50 border-b">
                  <th className="px-4 py-2 text-left">Username</th>
                  {showPasswords && <th className="px-4 py-2 text-left">Password</th>}
                  <th className="px-4 py-2 text-left">Role</th>
                  <th className="px-4 py-2 text-left">Access Level</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {DEMO_USERS.map((u) => (
                  <tr key={u.username} className="hover:bg-gray-50">
                    <td className="px-4 py-2.5 font-mono font-medium">{u.username}</td>
                    {showPasswords && (
                      <td className="px-4 py-2.5 font-mono text-gray-500">{u.password}</td>
                    )}
                    <td className="px-4 py-2.5">
                      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${u.color}`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-gray-500 text-xs">{u.access}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="px-4 py-2 bg-yellow-50 border-t text-xs text-yellow-700">
              ⚠ Demo deployment — credentials are hardcoded. Use "+ New User" above to create real DB users.
            </div>
          </>
        )}
      </div>

      {/* ── Tenant Config ─────────────────────────────────────────────────── */}
      {config && (
        <>
          <SectionTitle>Operational Settings</SectionTitle>
          <div className="bg-white border rounded-xl shadow-sm divide-y text-sm">
            {[
              { label: 'Order Cut-off Time', value: config.orderCutoffTime ?? '—' },
              { label: 'Rush Order Premium', value: config.rushOrderPremiumPct != null ? `${config.rushOrderPremiumPct}%` : '—' },
              { label: 'Main Currency', value: config.mainCurrency ?? '—' },
            ].map(({ label, value }) => (
              <div key={label} className="flex items-center px-4 py-2.5">
                <span className="w-48 text-gray-500 shrink-0">{label}</span>
                <span className="font-mono text-gray-700">{value}</span>
              </div>
            ))}
          </div>
        </>
      )}

      {/* ── System Info ───────────────────────────────────────────────────── */}
      <SectionTitle>System</SectionTitle>
      <div className="bg-white border rounded-xl shadow-sm divide-y text-sm">
        {[
          { label: 'Application', value: 'BreadCost v1.0.0' },
          { label: 'Backend', value: 'Spring Boot 3.4.2 · Java 21 · H2 File DB' },
          { label: 'Frontend', value: 'Next.js 14 · React 18 · Tailwind CSS 3' },
          { label: 'API Base', value: 'http://localhost:8080/v1' },
          { label: 'Tenant ID', value: TENANT_ID },
          { label: 'Default Site', value: 'MAIN' },
        ].map(({ label, value }) => (
          <div key={label} className="flex items-center px-4 py-2.5">
            <span className="w-40 text-gray-500 shrink-0">{label}</span>
            <span className="font-mono text-gray-700">{value}</span>
          </div>
        ))}
      </div>

      <div className="mt-6 bg-yellow-50 border border-yellow-200 rounded-xl p-4 text-sm text-yellow-800">
        <strong>Admin-only.</strong> Changes here affect all users and active production runs. Activating a new recipe
        version archives the previous one. Departments and products referenced by orders cannot be deleted.
      </div>
    </div>
  );
}
