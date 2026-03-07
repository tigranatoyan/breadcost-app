'use client';
import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Alert, Spinner } from '@/components/ui';
import { useT } from '@/lib/i18n';

// ─── master data panels ────────────────────────────────────────────────────────
const MASTER_PANELS = [
  {
    href: '/departments',
    icon: '🏢',
    titleKey: 'admin.departments.title',
    descKey: 'admin.departments.desc',
    color: 'border-blue-200 bg-blue-50 hover:bg-blue-100',
  },
  {
    href: '/products',
    icon: '🍞',
    titleKey: 'admin.products.title',
    descKey: 'admin.products.desc',
    color: 'border-amber-200 bg-amber-50 hover:bg-amber-100',
  },
  {
    href: '/recipes',
    icon: '📋',
    titleKey: 'admin.recipes.title',
    descKey: 'admin.recipes.desc',
    color: 'border-emerald-200 bg-emerald-50 hover:bg-emerald-100',
  },
  {
    href: '/inventory',
    icon: '🏬',
    titleKey: 'admin.items.title',
    descKey: 'admin.items.desc',
    color: 'border-purple-200 bg-purple-50 hover:bg-purple-100',
  },
];

// ─── demo users (matches SecurityConfig hard-coded in-memory users) ────────────
const DEMO_USERS = [
  { username: 'admin',      password: 'admin',      role: 'Admin',         accessKey: 'admin.demoAccess.admin',       color: 'bg-red-100 text-red-700' },
  { username: 'production', password: 'production', role: 'ProductionUser', accessKey: 'admin.demoAccess.production', color: 'bg-orange-100 text-orange-700' },
  { username: 'finance',    password: 'finance',    role: 'FinanceUser',   accessKey: 'admin.demoAccess.finance',    color: 'bg-blue-100 text-blue-700' },
  { username: 'viewer',     password: 'viewer',     role: 'Viewer',        accessKey: 'admin.demoAccess.viewer',     color: 'bg-gray-100 text-gray-700' },
  { username: 'cashier',    password: 'cashier',    role: 'Cashier',       accessKey: 'admin.demoAccess.cashier',    color: 'bg-green-100 text-green-700' },
  { username: 'warehouse',  password: 'warehouse',  role: 'Warehouse',     accessKey: 'admin.demoAccess.warehouse',  color: 'bg-purple-100 text-purple-700' },
  { username: 'technologist','password': 'technologist', role: 'Technologist', accessKey: 'admin.demoAccess.technologist', color: 'bg-amber-100 text-amber-700' },
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
  const t = useT();
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
        <h1 className="text-2xl font-semibold">{t('admin.title')}</h1>
        <p className="text-sm text-gray-500 mt-1">
          {t('admin.subtitle')}
        </p>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {/* ── Master Data ────────────────────────────────────────────────────── */}
      <SectionTitle>{t('admin.masterData')}</SectionTitle>
      <div className="grid grid-cols-1 gap-4">
        {MASTER_PANELS.map((p) => (
          <Link
            key={p.href}
            href={p.href}
            className={`flex items-start gap-5 border rounded-xl p-5 transition-colors ${p.color}`}
          >
            <span className="text-3xl">{p.icon}</span>
            <div className="flex-1 min-w-0">
              <div className="font-semibold text-gray-800">{t(p.titleKey)}</div>
              <div className="text-sm text-gray-600 mt-0.5">{t(p.descKey)}</div>
            </div>
            <span className="ml-auto text-gray-400 self-center">→</span>
          </Link>
        ))}
      </div>

      {/* ── DB Users ──────────────────────────────────────────────────────── */}
      <SectionTitle>{t('admin.usersAndRoles')}</SectionTitle>
      <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-4 py-3 border-b bg-gray-50">
          <span className="text-sm font-medium text-gray-700">
            {dbUsers.length > 0 ? t('admin.usersInDb', { count: dbUsers.length }) : t('admin.demoAccountsFallback')}
          </span>
          <button
            className="text-xs bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
            onClick={() => setShowNewUserForm((s) => !s)}
          >
            {showNewUserForm ? t('common.cancel') : t('admin.newUser')}
          </button>
        </div>

        {showNewUserForm && (
          <form onSubmit={createUser} className="px-4 py-3 border-b bg-blue-50 grid grid-cols-2 gap-3 text-sm">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">{t('admin.username')}</label>
              <input className="input text-xs" required value={newUser.username}
                onChange={(e) => setNewUser((u) => ({ ...u, username: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">{t('admin.password')}</label>
              <input className="input text-xs" type="password" required value={newUser.password}
                onChange={(e) => setNewUser((u) => ({ ...u, password: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">{t('admin.displayName')}</label>
              <input className="input text-xs" value={newUser.displayName}
                onChange={(e) => setNewUser((u) => ({ ...u, displayName: e.target.value }))} />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">{t('admin.role')}</label>
              <select className="input text-xs" value={newUser.roles}
                onChange={(e) => setNewUser((u) => ({ ...u, roles: e.target.value }))}>
                {['Admin','ProductionUser','FinanceUser','Viewer','Cashier','Warehouse','Technologist'].map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            <div className="col-span-2 flex justify-end">
              <button type="submit" className="btn-primary text-xs py-1 px-3" disabled={saving}>
                {saving ? t('common.saving') : t('admin.createUser')}
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
                <th className="px-4 py-2 text-left">{t('admin.username')}</th>
                <th className="px-4 py-2 text-left">{t('admin.displayName')}</th>
                <th className="px-4 py-2 text-left">{t('admin.role')}</th>
                <th className="px-4 py-2 text-left">{t('common.status')}</th>
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
                      {u.active ? t('admin.active') : t('admin.inactive')}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 text-right">
                    <button
                      className="text-xs text-gray-500 hover:text-gray-700 underline"
                      onClick={() => toggleActive(u)}
                    >
                      {u.active ? t('admin.deactivate') : t('admin.activateUser')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <>
            <div className="flex items-center justify-between px-4 py-2 border-b bg-gray-50">
              <span className="text-xs text-gray-500">{t('admin.showingDemoAccounts')}</span>
              <button
                className="text-xs text-blue-600 hover:underline"
                onClick={() => setShowPasswords((s) => !s)}
              >
                {showPasswords ? t('admin.hidePasswords') : t('admin.showPasswords')}
              </button>
            </div>
            <table className="min-w-full text-sm">
              <thead>
                <tr className="text-xs font-semibold text-gray-500 uppercase tracking-wide bg-gray-50 border-b">
                  <th className="px-4 py-2 text-left">{t('admin.username')}</th>
                  {showPasswords && <th className="px-4 py-2 text-left">{t('admin.password')}</th>}
                  <th className="px-4 py-2 text-left">{t('admin.role')}</th>
                  <th className="px-4 py-2 text-left">{t('admin.accessLevel')}</th>
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
                    <td className="px-4 py-2.5 text-gray-500 text-xs">{t(u.accessKey)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="px-4 py-2 bg-yellow-50 border-t text-xs text-yellow-700">
              {t('admin.demoWarning')}
            </div>
          </>
        )}
      </div>

      {/* ── Tenant Config ─────────────────────────────────────────────────── */}
      {config && (
        <>
          <SectionTitle>{t('admin.operationalSettings')}</SectionTitle>
          <div className="bg-white border rounded-xl shadow-sm divide-y text-sm">
            {[
              { label: t('admin.orderCutoffTime'), value: config.orderCutoffTime ?? '—' },
              { label: t('admin.rushOrderPremium'), value: config.rushOrderPremiumPct != null ? `${config.rushOrderPremiumPct}%` : '—' },
              { label: t('admin.mainCurrency'), value: config.mainCurrency ?? '—' },
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
      <SectionTitle>{t('admin.system')}</SectionTitle>
      <div className="bg-white border rounded-xl shadow-sm divide-y text-sm">
        {[
          { label: t('admin.appLabel'), value: 'BreadCost v1.0.0' },
          { label: t('admin.backendLabel'), value: 'Spring Boot 3.4.2 · Java 21 · H2 File DB' },
          { label: t('admin.frontendLabel'), value: 'Next.js 14 · React 18 · Tailwind CSS 3' },
          { label: t('admin.apiBaseLabel'), value: 'http://localhost:8080/v1' },
          { label: t('admin.tenantIdLabel'), value: TENANT_ID },
          { label: t('admin.defaultSiteLabel'), value: 'MAIN' },
        ].map(({ label, value }) => (
          <div key={label} className="flex items-center px-4 py-2.5">
            <span className="w-40 text-gray-500 shrink-0">{label}</span>
            <span className="font-mono text-gray-700">{value}</span>
          </div>
        ))}
      </div>

      <div className="mt-6 bg-yellow-50 border border-yellow-200 rounded-xl p-4 text-sm text-yellow-800">
        {t('admin.adminOnlyWarning')}
      </div>
    </div>
  );
}
