'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
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
      setSuccess('Tenant provisioned successfully');
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
      setSuccess('Branding saved');
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
      setSuccess('Data exported as JSON');
    } catch (e) { setError(String(e)); }
    finally { setExporting(false); }
  };

  /* ── Tab rendering ───────────────────────────────────── */
  const tabs = [
    { key: 'overview', label: 'Platform Overview' },
    { key: 'onboarding', label: 'Onboarding Requests' },
    { key: 'branding', label: 'Branding' },
    { key: 'export', label: 'Data Export' },
  ] as const;

  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="D4" title="Tenant Management" subtitle="Multi-tenant administration" />

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
            <StatCard icon={Building2} label="Total Tenants" value={String(overview.totalTenants)} />
            <StatCard icon={Users} label="Pending Onboarding" value={String(overview.pendingOnboarding)} />
            <StatCard icon={ShoppingCart} label="Total Orders" value={
              String(overview.tenants.reduce((s, t) => s + t.ordersCount, 0))
            } />
          </div>
          <Card title="Tenants">
            <Table
              cols={['Tenant', 'Currency', 'Products', 'Orders', 'Customers', 'Tier', 'Active']}
              rows={overview.tenants.map((t) => [
                t.displayName || t.tenantId,
                t.currency,
                String(t.productsCount),
                String(t.ordersCount),
                String(t.customersCount),
                t.subscriptionTier || '—',
                t.subscriptionActive ? 'Yes' : 'No',
              ])}
              empty="No tenants"
            />
          </Card>
        </div>
      ))}

      {/* ── Onboarding Tab ────────────────────────────── */}
      {tab === 'onboarding' && (onboardingLoading ? <Spinner /> : (
        <Card title="Onboarding Requests">
          <Table
            cols={['Business', 'Owner', 'Email', 'Tier', 'Status', 'Actions']}
            rows={requests.map((r) => [
              r.businessName,
              r.ownerName || '—',
              r.ownerEmail,
              r.requestedTier,
              r.status,
              r.status === 'PENDING' ? '...' : '—',
            ])}
            empty="No onboarding requests"
          />
          {/* Action buttons for pending requests */}
          {requests.filter((r) => r.status === 'PENDING').map((r) => (
            <div key={r.requestId} className="mt-2 flex items-center gap-2 p-3 bg-yellow-50 rounded-lg">
              <span className="text-sm font-medium">{r.businessName}</span>
              <span className="text-xs text-gray-500">{r.ownerEmail}</span>
              <div className="ml-auto flex gap-2">
                <Button size="sm" onClick={() => approve(r.requestId)}>Approve</Button>
                <Button size="sm" variant="danger" onClick={() => reject(r.requestId)}>Reject</Button>
              </div>
            </div>
          ))}
        </Card>
      ))}

      {/* ── Branding Tab ──────────────────────────────── */}
      {tab === 'branding' && (brandingLoading ? <Spinner /> : branding && (
        <Card title="Tenant Branding" action={<Button onClick={saveBranding}>Save</Button>}>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Business Name</label>
              <input
                className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.receiptBusinessName || ''}
                onChange={(e) => setBranding({ ...branding, receiptBusinessName: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Logo URL</label>
              <input
                className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.logoUrl || ''}
                onChange={(e) => setBranding({ ...branding, logoUrl: e.target.value })}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Primary Color</label>
              <div className="flex gap-2 items-center">
                <input type="color" value={branding.primaryColor || '#2563eb'}
                  onChange={(e) => setBranding({ ...branding, primaryColor: e.target.value })} />
                <span className="text-sm text-gray-500">{branding.primaryColor}</span>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Accent Color</label>
              <div className="flex gap-2 items-center">
                <input type="color" value={branding.accentColor || '#f59e0b'}
                  onChange={(e) => setBranding({ ...branding, accentColor: e.target.value })} />
                <span className="text-sm text-gray-500">{branding.accentColor}</span>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Locale</label>
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
              <label className="block text-sm font-medium text-gray-700 mb-1">Timezone</label>
              <input
                className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.timezone || 'UTC'}
                onChange={(e) => setBranding({ ...branding, timezone: e.target.value })}
              />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Receipt Header</label>
              <input
                className="w-full px-3 py-2 border rounded-lg text-sm"
                value={branding.receiptHeader || ''}
                onChange={(e) => setBranding({ ...branding, receiptHeader: e.target.value })}
              />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">Receipt Footer</label>
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
            <p className="text-white font-semibold">{branding.receiptBusinessName || 'Your Business'}</p>
            <p className="text-white/80 text-sm">{branding.receiptHeader || 'Receipt Preview'}</p>
          </div>
        </Card>
      ))}

      {/* ── Export Tab ─────────────────────────────────── */}
      {tab === 'export' && (
        <Card title="GDPR Data Export">
          <p className="text-sm text-gray-600 mb-4">
            Export all tenant data as a JSON file. This includes products, customers, orders,
            subscription details, and configuration.
          </p>
          <Button onClick={doExport}>
            {exporting ? 'Exporting...' : <><Download className="w-4 h-4 mr-1 inline" /> Export Tenant Data</>}
          </Button>
          {exportData && (
            <div className="mt-4 grid grid-cols-3 gap-4">
              <StatCard icon={ShoppingCart} label="Products" value={String((exportData as Record<string, unknown>).productsCount || 0)} />
              <StatCard icon={Users} label="Customers" value={String((exportData as Record<string, unknown>).customersCount || 0)} />
              <StatCard icon={Globe} label="Orders" value={String((exportData as Record<string, unknown>).ordersCount || 0)} />
            </div>
          )}
        </Card>
      )}
    </div>
  );
}
