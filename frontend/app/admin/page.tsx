'use client';
import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { apiFetch, API_BASE, TENANT_ID } from '@/lib/api';
import { Alert, Spinner, Modal, Field, Success } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';
import { useT } from '@/lib/i18n';

// ─── master data panels ────────────────────────────────────────────────────────
const MASTER_PANELS = [
  { href: '/departments', icon: '🏢', titleKey: 'admin.departments.title', descKey: 'admin.departments.desc', color: 'border-blue-200 bg-blue-50 hover:bg-blue-100' },
  { href: '/products',    icon: '🍞', titleKey: 'admin.products.title',    descKey: 'admin.products.desc',    color: 'border-amber-200 bg-amber-50 hover:bg-amber-100' },
  { href: '/recipes',     icon: '📋', titleKey: 'admin.recipes.title',     descKey: 'admin.recipes.desc',     color: 'border-emerald-200 bg-emerald-50 hover:bg-emerald-100' },
  { href: '/inventory',   icon: '🏬', titleKey: 'admin.items.title',       descKey: 'admin.items.desc',       color: 'border-purple-200 bg-purple-50 hover:bg-purple-100' },
];

const ROLES = ['Admin','ProductionUser','FinanceUser','Viewer','Cashier','Warehouse','Technologist'];

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

interface DbUser {
  userId: string;
  username: string;
  displayName: string;
  roles: string;
  active: boolean;
  departmentId?: string;
}

interface TenantConfig {
  orderCutoffTime: string;
  rushOrderPremiumPct: number;
  mainCurrency: string;
}

interface Dept { departmentId: string; name: string; }

type Tab = 'users' | 'system';

export default function AdminPage() {
  const t = useT();
  const [tab, setTab] = useState<Tab>('users');
  const [showPasswords, setShowPasswords] = useState(false);
  const [dbUsers, setDbUsers] = useState<DbUser[]>([]);
  const [depts, setDepts] = useState<Dept[]>([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [config, setConfig] = useState<TenantConfig | null>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [newUser, setNewUser] = useState({ username: '', password: '', roles: 'Viewer', displayName: '' });
  const [showNewUserForm, setShowNewUserForm] = useState(false);
  const [saving, setSaving] = useState(false);

  // Edit user (BC-1701)
  const [editUser, setEditUser] = useState<DbUser | null>(null);
  const [editForm, setEditForm] = useState({ displayName: '', roles: '', active: true, departmentId: '' });

  // Password reset (BC-1702)
  const [resetUser, setResetUser] = useState<DbUser | null>(null);
  const [resetPwd, setResetPwd] = useState('');
  const [resetSaving, setResetSaving] = useState(false);

  // Config editing (BC-1703)
  const [editingConfig, setEditingConfig] = useState(false);
  const [cfgForm, setCfgForm] = useState({ orderCutoffTime: '', rushOrderPremiumPct: '', mainCurrency: '' });
  const [cfgSaving, setCfgSaving] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const [users, cfg, deps] = await Promise.all([
        apiFetch<DbUser[]>(`/v1/users?tenantId=${TENANT_ID}`).catch(() => []),
        apiFetch<TenantConfig>(`/v1/config?tenantId=${TENANT_ID}`).catch(() => null),
        apiFetch<Dept[]>(`/v1/departments?tenantId=${TENANT_ID}`).catch(() => []),
      ]);
      setDbUsers(users);
      setConfig(cfg);
      setDepts(deps);
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
      setSuccess(t('admin.userCreated'));
      loadData();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  // ── Edit user (BC-1701) ──────────────────────────────────────────────────────
  const openEditUser = (u: DbUser) => {
    setEditUser(u);
    setEditForm({ displayName: u.displayName || '', roles: u.roles, active: u.active, departmentId: u.departmentId || '' });
  };

  const saveEditUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editUser) return;
    try {
      setSaving(true);
      await apiFetch(`/v1/users/${editUser.userId}?tenantId=${TENANT_ID}`, {
        method: 'PUT',
        body: JSON.stringify({ displayName: editForm.displayName, roles: editForm.roles, active: editForm.active, departmentId: editForm.departmentId || null }),
      });
      setEditUser(null);
      setSuccess(t('admin.userUpdated'));
      loadData();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  // ── Password reset (BC-1702) ─────────────────────────────────────────────────
  const submitResetPwd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetUser) return;
    try {
      setResetSaving(true);
      await apiFetch(`/v1/users/${resetUser.userId}/reset-password?tenantId=${TENANT_ID}`, {
        method: 'POST',
        body: JSON.stringify({ newPassword: resetPwd }),
      });
      setResetUser(null);
      setResetPwd('');
      setSuccess(t('admin.passwordReset'));
    } catch (e) {
      setError(String(e));
    } finally {
      setResetSaving(false);
    }
  };

  // ── Config editing (BC-1703) ─────────────────────────────────────────────────
  const startEditConfig = () => {
    if (!config) return;
    setCfgForm({
      orderCutoffTime: config.orderCutoffTime ?? '',
      rushOrderPremiumPct:
        config.rushOrderPremiumPct !== null && config.rushOrderPremiumPct !== undefined
          ? String(config.rushOrderPremiumPct)
          : '',
      mainCurrency: config.mainCurrency ?? '',
    });
    setEditingConfig(true);
  };

  const saveConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setCfgSaving(true);
      await apiFetch(`/v1/config?tenantId=${TENANT_ID}`, {
        method: 'PUT',
        body: JSON.stringify({
          orderCutoffTime: cfgForm.orderCutoffTime || null,
          rushOrderPremiumPct: cfgForm.rushOrderPremiumPct !== '' ? Number(cfgForm.rushOrderPremiumPct) : null,
          mainCurrency: cfgForm.mainCurrency || null,
        }),
      });
      setEditingConfig(false);
      setSuccess(t('admin.configSaved'));
      loadData();
    } catch (e) {
      setError(String(e));
    } finally {
      setCfgSaving(false);
    }
  };

  const TABS: { key: Tab; label: string }[] = [
    { key: 'users', label: t('admin.tabUsers') },
    { key: 'system', label: t('admin.tabSystem') },
  ];

  return (
    <div className="max-w-[1800px]">
      <SectionTitle
        eyebrow={t('admin.eyebrow')}
        title={t('admin.title')}
        subtitle={t('admin.subtitle')}
      />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* ── Master Data Links ─────────────────────────────────────────────── */}
      <h2 className="text-xs font-semibold uppercase tracking-widest text-gray-400 mb-3">{t('admin.masterData')}</h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-6">
        {MASTER_PANELS.map((p) => (
          <Link key={p.href} href={p.href}
            className={`flex items-start gap-4 border rounded-xl p-4 transition-colors ${p.color}`}>
            <span className="text-2xl">{p.icon}</span>
            <div className="flex-1 min-w-0">
              <div className="font-semibold text-gray-800 text-sm">{t(p.titleKey)}</div>
              <div className="text-xs text-gray-600 mt-0.5">{t(p.descKey)}</div>
            </div>
            <span className="ml-auto text-gray-400 self-center">→</span>
          </Link>
        ))}
      </div>

      {/* ── Tab Bar (BC-1704) ─────────────────────────────────────────────── */}
      <div className="flex border-b mb-6">
        {TABS.map((t_) => (
          <button key={t_.key}
            className={`px-5 py-2.5 text-sm font-semibold border-b-2 transition-colors ${
              tab === t_.key ? 'border-blue-600 text-blue-700' : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setTab(t_.key)}>
            {t_.label}
          </button>
        ))}
      </div>

      {/* ═══════════════ USERS TAB ═══════════════════════════════════════════ */}
      {tab === 'users' && (
        <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b bg-gray-50">
            <span className="text-sm font-medium text-gray-700">
              {dbUsers.length > 0 ? t('admin.usersInDb', { count: dbUsers.length }) : t('admin.demoAccountsFallback')}
            </span>
            <button className="text-xs bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
              onClick={() => setShowNewUserForm((s) => !s)}>
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
                  {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
              <div className="col-span-2 flex justify-end">
                <Button variant="primary" size="xs" type="submit" disabled={saving}>
                  {saving ? t('common.saving') : t('admin.createUser')}
                </Button>
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
                  <th className="px-4 py-2 text-right">{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {dbUsers.map((u) => (
                  <tr key={u.userId} className="hover:bg-gray-50">
                    <td className="px-4 py-2.5 font-mono font-medium">{u.username}</td>
                    <td className="px-4 py-2.5 text-gray-600">{u.displayName || '—'}</td>
                    <td className="px-4 py-2.5">
                      <span className="inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700">{u.roles}</span>
                    </td>
                    <td className="px-4 py-2.5">
                      <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {u.active ? t('admin.active') : t('admin.inactive')}
                      </span>
                    </td>
                    <td className="px-4 py-2.5 text-right space-x-2">
                      <button className="text-xs text-blue-600 hover:underline" onClick={() => openEditUser(u)}>{t('common.edit')}</button>
                      <button className="text-xs text-amber-600 hover:underline" onClick={() => { setResetUser(u); setResetPwd(''); }}>{t('admin.resetPwd')}</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <>
              <div className="flex items-center justify-between px-4 py-2 border-b bg-gray-50">
                <span className="text-xs text-gray-500">{t('admin.showingDemoAccounts')}</span>
                <button className="text-xs text-blue-600 hover:underline" onClick={() => setShowPasswords((s) => !s)}>
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
                      {showPasswords && <td className="px-4 py-2.5 font-mono text-gray-500">{u.password}</td>}
                      <td className="px-4 py-2.5">
                        <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${u.color}`}>{u.role}</span>
                      </td>
                      <td className="px-4 py-2.5 text-gray-500 text-xs">{t(u.accessKey)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="px-4 py-2 bg-yellow-50 border-t text-xs text-yellow-700">{t('admin.demoWarning')}</div>
            </>
          )}
        </div>
      )}

      {/* ═══════════════ SYSTEM TAB ══════════════════════════════════════════ */}
      {tab === 'system' && (
        <div className="space-y-6">
          {config && (
            <div>
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-xs font-semibold uppercase tracking-widest text-gray-400">{t('admin.operationalSettings')}</h3>
                {!editingConfig && (
                  <button className="text-xs text-blue-600 hover:underline" onClick={startEditConfig}>{t('common.edit')}</button>
                )}
              </div>
              {editingConfig ? (
                <form onSubmit={saveConfig} className="bg-white border rounded-xl shadow-sm p-5 space-y-4">
                  <Field label={t('admin.orderCutoffTime')} hint={t('admin.cutoffHint')}>
                    <input className="input" type="time" value={cfgForm.orderCutoffTime}
                      onChange={(e) => setCfgForm((f) => ({ ...f, orderCutoffTime: e.target.value }))} />
                  </Field>
                  <Field label={t('admin.rushOrderPremium')}>
                    <input className="input" type="number" min="0" max="100" step="0.1" placeholder="e.g. 15"
                      value={cfgForm.rushOrderPremiumPct}
                      onChange={(e) => setCfgForm((f) => ({ ...f, rushOrderPremiumPct: e.target.value }))} />
                  </Field>
                  <Field label={t('admin.mainCurrency')}>
                    <input className="input" placeholder="e.g. AMD" value={cfgForm.mainCurrency}
                      onChange={(e) => setCfgForm((f) => ({ ...f, mainCurrency: e.target.value }))} />
                  </Field>
                  <div className="flex justify-end gap-2 pt-2 border-t">
                    <Button variant="secondary" size="sm" onClick={() => setEditingConfig(false)}>{t('common.cancel')}</Button>
                    <Button variant="primary" size="sm" type="submit" disabled={cfgSaving}>
                      {cfgSaving ? t('common.saving') : t('common.save')}
                    </Button>
                  </div>
                </form>
              ) : (
                <div className="bg-white border rounded-xl shadow-sm divide-y text-sm">
                  {[
                    { label: t('admin.orderCutoffTime'), value: config.orderCutoffTime ?? '—' },
                    {
                      label: t('admin.rushOrderPremium'),
                      value:
                        config.rushOrderPremiumPct !== null && config.rushOrderPremiumPct !== undefined
                          ? `${config.rushOrderPremiumPct}%`
                          : '—',
                    },
                    { label: t('admin.mainCurrency'), value: config.mainCurrency ?? '—' },
                  ].map(({ label, value }) => (
                    <div key={label} className="flex items-center px-4 py-2.5">
                      <span className="w-48 text-gray-500 shrink-0">{label}</span>
                      <span className="font-mono text-gray-700">{value}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          <div>
            <h3 className="text-xs font-semibold uppercase tracking-widest text-gray-400 mb-2">{t('admin.system')}</h3>
            <div className="bg-white border rounded-xl shadow-sm divide-y text-sm">
              {[
                { label: t('admin.appLabel'), value: 'BreadCost v1.0.0' },
                { label: t('admin.backendLabel'), value: 'Spring Boot 3.4.2 · Java 21 · H2 File DB' },
                { label: t('admin.frontendLabel'), value: 'Next.js 14 · React 18 · Tailwind CSS 3' },
                { label: t('admin.apiBaseLabel'), value: `${API_BASE}/v1` },
                { label: t('admin.tenantIdLabel'), value: TENANT_ID },
                { label: t('admin.defaultSiteLabel'), value: 'MAIN' },
              ].map(({ label, value }) => (
                <div key={label} className="flex items-center px-4 py-2.5">
                  <span className="w-40 text-gray-500 shrink-0">{label}</span>
                  <span className="font-mono text-gray-700">{value}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4 text-sm text-yellow-800">
            {t('admin.adminOnlyWarning')}
          </div>
        </div>
      )}

      {/* ═══════════════ EDIT USER MODAL (BC-1701) ═══════════════════════════ */}
      {editUser && (
        <Modal title={t('admin.editUserTitle')} onClose={() => setEditUser(null)}>
          <form onSubmit={saveEditUser} className="space-y-4">
            <div className="text-sm text-gray-500 mb-2">{t('admin.editingUser', { username: editUser.username })}</div>
            <Field label={t('admin.displayName')}>
              <input className="input" value={editForm.displayName}
                onChange={(e) => setEditForm((f) => ({ ...f, displayName: e.target.value }))} />
            </Field>
            <Field label={t('admin.role')}>
              <select className="input" value={editForm.roles}
                onChange={(e) => setEditForm((f) => ({ ...f, roles: e.target.value }))}>
                {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </Field>
            <Field label={t('admin.department')}>
              <select className="input" value={editForm.departmentId}
                onChange={(e) => setEditForm((f) => ({ ...f, departmentId: e.target.value }))}>
                <option value="">{t('admin.noDepartment')}</option>
                {depts.map((d) => <option key={d.departmentId} value={d.departmentId}>{d.name}</option>)}
              </select>
            </Field>
            <Field label={t('common.status')}>
              <label className="flex items-center gap-2">
                <input type="checkbox" checked={editForm.active}
                  onChange={(e) => setEditForm((f) => ({ ...f, active: e.target.checked }))} />
                <span className="text-sm">{editForm.active ? t('admin.active') : t('admin.inactive')}</span>
              </label>
            </Field>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="secondary" size="sm" onClick={() => setEditUser(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>
                {saving ? t('common.saving') : t('common.save')}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* ═══════════════ PASSWORD RESET MODAL (BC-1702) ═══════════════════════ */}
      {resetUser && (
        <Modal title={t('admin.resetPwdTitle')} onClose={() => setResetUser(null)}>
          <form onSubmit={submitResetPwd} className="space-y-4">
            <div className="text-sm text-gray-500">{t('admin.resetPwdFor', { username: resetUser.username })}</div>
            <Field label={t('admin.newPassword')}>
              <input className="input" type="password" required minLength={4}
                value={resetPwd} onChange={(e) => setResetPwd(e.target.value)}
                placeholder={t('admin.newPasswordPlaceholder')} />
            </Field>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="secondary" size="sm" onClick={() => setResetUser(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={resetSaving}>
                {resetSaving ? t('common.saving') : t('admin.resetPwdBtn')}
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
