'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Table, Spinner, Alert, Badge, Success } from '@/components/ui';

/* ── types ─────────────────────────────────────────────── */
interface PricingSuggestion {
  id: string;
  productId: string;
  productName: string;
  currentPrice: number;
  suggestedPrice: number;
  reasoning: string;
  status: string;
}
interface AnomalyAlert {
  id: string;
  alertType: string;
  severity: string;
  deviationPercent: number;
  explanation: string;
  suggestedAction: string;
  status: string;
}

const SEVERITY_COLORS: Record<string, string> = {
  LOW: 'bg-blue-100 text-blue-800',
  MEDIUM: 'bg-yellow-100 text-yellow-800',
  HIGH: 'bg-orange-100 text-orange-800',
  CRITICAL: 'bg-red-100 text-red-800',
};

/* ── page ──────────────────────────────────────────────── */
export default function AiPricingPage() {
  const t = useT();

  const [tab, setTab] = useState<'pricing' | 'anomalies'>('pricing');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* pricing */
  const [pricing, setPricing] = useState<PricingSuggestion[]>([]);
  const [pricingLoading, setPricingLoading] = useState(false);
  const [pendingOnly, setPendingOnly] = useState(false);

  /* anomalies */
  const [alerts, setAlerts] = useState<AnomalyAlert[]>([]);
  const [alertsLoading, setAlertsLoading] = useState(false);
  const [activeOnly, setActiveOnly] = useState(false);

  /* ── pricing loaders ─────────────────────────────────── */
  const loadPricing = useCallback(async () => {
    setPricingLoading(true);
    try {
      const endpoint = pendingOnly
        ? `/v3/ai/pricing/pending?tenantId=${TENANT_ID}`
        : `/v3/ai/pricing?tenantId=${TENANT_ID}`;
      setPricing(await apiFetch<PricingSuggestion[]>(endpoint));
    } catch (e) { setError(String(e)); }
    finally { setPricingLoading(false); }
  }, [pendingOnly]);

  const generatePricing = async () => {
    try {
      const data = await apiFetch<PricingSuggestion[]>(`/v3/ai/pricing/generate?tenantId=${TENANT_ID}`, { method: 'POST' });
      setPricing(data);
      setSuccess(t('aiPricing.generated'));
    } catch (e) { setError(String(e)); }
  };

  const dismissPricing = async (id: string) => {
    try {
      await apiFetch(`/v3/ai/pricing/${id}/dismiss?tenantId=${TENANT_ID}`, { method: 'POST' });
      loadPricing();
    } catch (e) { setError(String(e)); }
  };

  const acceptPricing = async (id: string) => {
    try {
      await apiFetch(`/v3/ai/pricing/${id}/accept?tenantId=${TENANT_ID}`, { method: 'POST' });
      setSuccess(t('aiPricing.accepted'));
      loadPricing();
    } catch (e) { setError(String(e)); }
  };

  /* ── anomaly loaders ─────────────────────────────────── */
  const loadAlerts = useCallback(async () => {
    setAlertsLoading(true);
    try {
      const endpoint = activeOnly
        ? `/v3/ai/anomalies/active?tenantId=${TENANT_ID}`
        : `/v3/ai/anomalies?tenantId=${TENANT_ID}`;
      setAlerts(await apiFetch<AnomalyAlert[]>(endpoint));
    } catch (e) { setError(String(e)); }
    finally { setAlertsLoading(false); }
  }, [activeOnly]);

  const generateAlerts = async () => {
    try {
      const data = await apiFetch<AnomalyAlert[]>(`/v3/ai/anomalies/generate?tenantId=${TENANT_ID}`, { method: 'POST' });
      setAlerts(data);
      setSuccess(t('aiPricing.generated'));
    } catch (e) { setError(String(e)); }
  };

  const acknowledgeAlert = async (id: string) => {
    try {
      await apiFetch(`/v3/ai/anomalies/${id}/acknowledge?tenantId=${TENANT_ID}`, { method: 'POST' });
      loadAlerts();
    } catch (e) { setError(String(e)); }
  };

  const dismissAlert = async (id: string) => {
    try {
      await apiFetch(`/v3/ai/anomalies/${id}/dismiss?tenantId=${TENANT_ID}`, { method: 'POST' });
      loadAlerts();
    } catch (e) { setError(String(e)); }
  };

  /* ── tab change effects ──────────────────────────────── */
  useEffect(() => {
    if (tab === 'pricing') loadPricing();
    else loadAlerts();
  }, [tab, loadPricing, loadAlerts]);

  const tabs: { key: typeof tab; label: string }[] = [
    { key: 'pricing', label: t('aiPricing.pricingSuggestions') },
    { key: 'anomalies', label: t('aiPricing.anomalyAlerts') },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">{t('aiPricing.title')}</h1>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* tabs */}
      <div className="flex gap-1">
        {tabs.map(tb => (
          <button key={tb.key} onClick={() => setTab(tb.key)}
            className={`px-4 py-2 rounded-t font-medium ${tab === tb.key ? 'bg-white border-t border-x text-blue-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
            {tb.label}
          </button>
        ))}
      </div>

      {/* ── Pricing Suggestions ────────────────────────── */}
      {tab === 'pricing' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-center">
            <button onClick={generatePricing} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">{t('aiPricing.generate')}</button>
            <label className="flex items-center gap-1 text-sm ml-auto">
              <input type="checkbox" checked={pendingOnly} onChange={e => setPendingOnly(e.target.checked)} />
              {t('aiPricing.pending')}
            </label>
          </div>
          {pricingLoading ? <Spinner /> : (
            <Table
              cols={[t('aiPricing.product'), t('aiPricing.currentPrice'), t('aiPricing.suggestedPrice'), t('aiPricing.reasoning'), '']}
              rows={pricing.map(p => [
                p.productName,
                p.currentPrice?.toFixed(2),
                <span key={p.id + 'sp'} className="font-semibold">{p.suggestedPrice?.toFixed(2)}</span>,
                <span key={p.id + 'r'} className="text-sm text-gray-600 max-w-xs truncate block">{p.reasoning}</span>,
                p.status === 'PENDING' ? (
                  <div key={p.id} className="flex gap-1">
                    <button onClick={() => acceptPricing(p.id)} className="text-green-600 hover:underline text-sm">{t('aiPricing.accept')}</button>
                    <button onClick={() => dismissPricing(p.id)} className="text-red-600 hover:underline text-sm">{t('aiPricing.dismiss')}</button>
                  </div>
                ) : <Badge key={p.id} status={p.status} />,
              ])}
              empty={t('aiPricing.noPricing')}
            />
          )}
        </div>
      )}

      {/* ── Anomaly Alerts ─────────────────────────────── */}
      {tab === 'anomalies' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-center">
            <button onClick={generateAlerts} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">{t('aiPricing.generate')}</button>
            <label className="flex items-center gap-1 text-sm ml-auto">
              <input type="checkbox" checked={activeOnly} onChange={e => setActiveOnly(e.target.checked)} />
              {t('aiPricing.active')}
            </label>
          </div>
          {alertsLoading ? <Spinner /> : (
            <Table
              cols={[t('aiPricing.type'), t('aiPricing.severity'), t('aiPricing.deviation'), t('aiPricing.explanation'), t('aiPricing.suggestedAction'), '']}
              rows={alerts.map(a => [
                a.alertType,
                <span key={a.id + 's'} className={`px-2 py-0.5 rounded text-xs font-medium ${SEVERITY_COLORS[a.severity] || 'bg-gray-100'}`}>{a.severity}</span>,
                `${a.deviationPercent?.toFixed(1)}%`,
                <span key={a.id + 'e'} className="text-sm text-gray-600 max-w-xs truncate block">{a.explanation}</span>,
                <span key={a.id + 'sa'} className="text-sm">{a.suggestedAction}</span>,
                a.status === 'ACTIVE' ? (
                  <div key={a.id} className="flex gap-1">
                    <button onClick={() => acknowledgeAlert(a.id)} className="text-blue-600 hover:underline text-sm">{t('aiPricing.acknowledge')}</button>
                    <button onClick={() => dismissAlert(a.id)} className="text-red-600 hover:underline text-sm">{t('aiPricing.dismiss')}</button>
                  </div>
                ) : <Badge key={a.id} status={a.status} />,
              ])}
              empty={t('aiPricing.noAlerts')}
            />
          )}
        </div>
      )}
    </div>
  );
}
