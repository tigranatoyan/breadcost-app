'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Spinner, Alert, Success } from '@/components/ui';
import { SectionTitle, Button, Card, StatCard, Table } from '@/components/design-system';
import { Building2, Users, ShoppingCart, Download, Globe } from 'lucide-react';

/* ── Types ─────────────────────────────────────────────── */
interface OnboardingRequest {
  requestId: string;
  tenantSlug: string;
  businessName: string;
  ownerEmail: string;
  ownerName: string;
  status: string;
  requestedTier: string;
  createdAt: string;
}
interface TenantSummary {
  tenantId: string;
  displayName: string;
  currency: string;
  productsCount: number;
  ordersCount: number;
  customersCount: number;
  subscriptionTier?: string;
  subscriptionActive?: boolean;
}
interface PlatformOverview {
  totalTenants: number;
  pendingOnboarding: number;
  tenants: TenantSummary[];
}
interface Branding {
  tenantId: string;
  logoUrl: string;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  receiptBusinessName: string;
  receiptFooter: string;
  receiptHeader: string;
  locale: string;
  timezone: string;
}

export default function TenantManagementPage() {
  const t = useT();
  const [tab, setTab] = useState<'overview' | 'onboarding' | 'branding' | 'export'>('overview');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* ── Overview ────────────────────────────────────────── */
  const [overview, setOverview] = useState<PlatformOverview | null>(null);
  const [overviewLoading, setOverviewLoading] = useState(false);

  const loadOverview = useCallback(async () => {
    setOverviewLoading(true);
    try {
      setOverview(await apiFetch('/v3/tenants/platform/overview') as PlatformOverview);
    } catch (e) { setError(String(e)); }
    finally { setOverviewLoading(false); }
  }, []);

  useEffect(() => { if (tab === 'overview') loadOverview(); }, [tab, loadOverview]);

  /* ── Onboarding ──────────────────────────────────────── */
  const [requests, setRequests] = useState<OnboardingRequest[]>([]);
  const [onboardingLoading, setOnboardingLoading] = useState(false);

  const loadRequests = useCallback(async () => {
    setOnboardingLoading(true);
    try {
      setRequests(await apiFetch('/v3/tenants/onboarding') as OnboardingRequest[]);
    } catch (e) { setError(String(e)); }
    finally { setOnboardingLoading(false); }
  }, []);

  useEffect(() => { if (tab === 'onboarding') loadRequests(); }, [tab, loadRequests]);

  const approve = async (id: string) => {
    try {
      await apiFetch(`/v3/tenants/onboarding/${id}/approve`, { method: 'POST' });
      setSuccess(t('tenantMgmt.provisioned'));
      loadRequests();
    } catch (e) { setError(String(e)); }
  };

  const reject = async (id: string) => {
    try {
      await apiFetch(`/v3/tenants/onboarding/${id}/reject`, {
        method: 'POST', body: JSON.stringify({ reason: 'Rejected by admin' }),
      });
      loadRequests();
    } catch (e) { setError(String(e)); }
  };

  /* ── Branding ────────────────────────────────────────── */
  const [branding, setBranding] = useState<Branding | null>(null);
  const [brandingLoading, setBrandingLoading] = useState(false);

  const loadBranding = useCallback(async () => {
    setBrandingLoading(true);
    try {
      setBranding(await apiFetch(`/v3/tenants/${TENANT_ID}/branding`) as Branding);
    } catch (e) { setError(String(e)); }
    finally { setBrandingLoading(false); }
  }, []);

  useEffect(() => { if (tab === 'branding') loadBranding(); }, [tab, loadBranding]);

  const saveBranding = async () => {
    if (!branding) return;
    try {
      await apiFetch(`/v3/tenants/${TENANT_ID}/branding`, {
        method: 'PUT', body: JSON.stringify(branding),
      });
      setSuccess(t('tenantMgmt.brandingSaved'));
    } catch (e) { setError(String(e)); }
  };

  /* ── Export ──────────────────────────────────────────── */
  const [exportData, setExportData] = useState<Record<string, unknown> | null>(null);
  const [exporting, setExporting] = useState(false);

  const doExport = async () => {
    setExporting(true);
    try {
      const data = await apiFetch(`/v3/tenants/${TENANT_ID}/export`) as Record<string, unknown>;
      setExportData(data);
      // Trigger download
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `tenant-export-${TENANT_ID}-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      setSuccess(t('tenantMgmt.exported'));
    } catch (e) { setError(String(e)); }
    finally { setExporting(false); }
  };

  /* ── Tab rendering ───────────────────────────────────── */
  const tabs = [
    { key: 'overview', label: t('tenantMgmt.tabOverview') },
    { key: 'onboarding', label: t('tenantMgmt.tabOnboarding') },
    { key: 'branding', label: t('tenantMgmt.tabBranding') },
    { key: 'export', label: t('tenantMgmt.tabExport') },
  ] as const;

  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="D4" title={t('tenantMgmt.title')} subtitle={t('tenantMgmt.subtitle')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* Tabs */}
      <div className="flex gap-1 bg-gray-100 rounded-lg p-1">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 text-sm font-medium rounded-md transition ${
              tab === t.key ? 'bg-white shadow text-blue-700' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* ── Overview Tab ──────────────────────────────── */}
      {tab === 'overview' && (overviewLoading ? <Spinner /> : overview && (
        <div className="space-y-4">
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <StatCard icon={Building2} label={t('tenantMgmt.totalTenants')} value={String(overview.totalTenants)} />
            <StatCard icon={Users} label={t('tenantMgmt.pendingOnboarding')} value={String(overview.pendingOnboarding)} />
            <StatCard icon={ShoppingCart} label={t('tenantMgmt.totalOrders')} value={
              String(overview.tenants.reduce((s, tn) => s + tn.ordersCount, 0))
            } />
          </div>
          <Card title={t('tenantMgmt.tenantsCard')}>
            <Table
              cols={[t('tenantMgmt.colTenant'), t('tenantMgmt.colCurrency'), t('tenantMgmt.colProducts'), t('tenantMgmt.colOrders'), t('tenantMgmt.colCustomers'), t('tenantMgmt.colTier'), t('tenantMgmt.colActive')]}
              rows={overview.tenants.map((tn) => [
                tn.displayName || tn.tenantId,
                tn.currency,
                String(tn.productsCount),
                String(tn.ordersCount),
                String(tn.customersCount),
                tn.subscriptionTier || '—',
                tn.subscriptionActive ? t('tenantMgmt.yes') : t('tenantMgmt.no'),
              ])}
              empty={t('tenantMgmt.noTenants')}
            />
          </Card>
        </div>
      ))}

      {/* ── Onboarding Tab ────────────────────────────── */}
      {tab === 'onboarding' && (onboardingLoading ? <Spinner /> : (
        <Card title={t('tenantMgmt.onboardingCard')}>
          <Table
            cols={[t('tenantMgmt.colBusiness'), t('tenantMgmt.colOwner'), t('tenantMgmt.colEmail'), t('tenantMgmt.colTier'), t('tenantMgmt.colStatus'), t('tenantMgmt.colActions')]}
            rows={requests.map((r) => [
              r.businessName,
              r.ownerName || '—',
              r.ownerEmail,
              r.requestedTier,
              r.status,
              r.status === 'PENDING' ? '...' : '—',
            ])}
            empty={t('tenantMgmt.noRequests')}
          />
          {/* Action buttons for pending requests */}
          {requests.filter((r) => r.status === 'PENDING').map((r) => (
            <div key={r.requestId} className="mt-2 flex items-center gap-2 p-3 bg-yellow-50 rounded-lg">
              <span className="text-sm font-medium">{r.businessName}</span>
              <span className="text-xs text-gray-500">{r.ownerEmail}</span>
              <div className="ml-auto flex gap-2">
                <Button size="sm" onClick={() => approve(r.requestId)}>{t('tenantMgmt.approve')}</Button>
                <Button size="sm" variant="danger" onClick={() => reject(r.requestId)}>{t('tenantMgmt.reject')}</Button>
              </div>
            </div>
          ))}
        </Card>
      ))}

      {/* ── Branding Tab ──────────────────────────────── */}
      {tab === 'branding' && (brandingLoading ? <Spinner /> : branding && (
        <Card title={t('tenantMgmt.brandingCard')} action={<Button onClick={saveBranding}>{t('tenantMgmt.save')}</Button>}>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('tenantMgmt.businessName')}</label>
              <input
                className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.receiptBusinessName || ''}
                onChange={(e) => setBranding({ ...branding, receiptBusinessName: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('tenantMgmt.logoUrl')}</label>
              <input
                className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.logoUrl || ''}
                onChange={(e) => setBranding({ ...branding, logoUrl: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('tenantMgmt.primaryColor')}</label>
              <div className="flex gap-2 items-center">
                <input type="color" value={branding.primaryColor || '#2563eb'}
                  onChange={(e) => setBranding({ ...branding, primaryColor: e.target.value })} />
                <span className="text-sm text-gray-500">{branding.primaryColor}</span>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('tenantMgmt.accentColor')}</label>
              <div className="flex gap-2 items-center">
                <input type="color" value={branding.accentColor || '#f59e0b'}
                  onChange={(e) => setBranding({ ...branding, accentColor: e.target.value })} />
                <span className="text-sm text-gray-500">{branding.accentColor}</span>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('tenantMgmt.locale')}</label>
              <select className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.locale || 'en'}
                onChange={(e) => setBranding({ ...branding, locale: e.target.value })}>
                <option value="en">English</option>
                <option value="hy">Armenian</option>
                <option value="uz">Uzbek</option>
                <option value="ru">Russian</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('tenantMgmt.timezone')}</label>
              <input
                className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.timezone || 'UTC'}
                onChange={(e) => setBranding({ ...branding, timezone: e.target.value })}
              />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('tenantMgmt.receiptHeader')}</label>
              <input
                className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.receiptHeader || ''}
                onChange={(e) => setBranding({ ...branding, receiptHeader: e.target.value })}
              />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('tenantMgmt.receiptFooter')}</label>
              <textarea
                className="w-full px-3 py-2 border rounded-lg text-sm"
                rows={3}
                value={branding.receiptFooter || ''}
                onChange={(e) => setBranding({ ...branding, receiptFooter: e.target.value })}
              />
            </div>
          </div>
          {/* Color preview */}
          <div className="mt-4 p-4 rounded-lg" style={{ backgroundColor: branding.primaryColor || '#2563eb' }}>
            <p className="text-white font-semibold">{branding.receiptBusinessName || t('tenantMgmt.yourBusiness')}</p>
            <p className="text-white/80 text-sm">{branding.receiptHeader || t('tenantMgmt.receiptPreview')}</p>
          </div>
        </Card>
      ))}

      {/* ── Export Tab ─────────────────────────────────── */}
      {tab === 'export' && (
        <Card title={t('tenantMgmt.exportCard')}>
          <p className="text-sm text-gray-600 mb-4">
            {t('tenantMgmt.exportDesc')}
          </p>
          <Button onClick={doExport}>
            {exporting ? t('tenantMgmt.exporting') : <><Download className="w-4 h-4 mr-1 inline" /> {t('tenantMgmt.exportButton')}</>}
          </Button>
          {exportData && (
            <div className="mt-4 grid grid-cols-3 gap-4">
              <StatCard icon={ShoppingCart} label={t('tenantMgmt.colProducts')} value={String((exportData as Record<string, unknown>).productsCount || 0)} />
              <StatCard icon={Users} label={t('tenantMgmt.colCustomers')} value={String((exportData as Record<string, unknown>).customersCount || 0)} />
              <StatCard icon={Globe} label={t('tenantMgmt.colOrders')} value={String((exportData as Record<string, unknown>).ordersCount || 0)} />
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
