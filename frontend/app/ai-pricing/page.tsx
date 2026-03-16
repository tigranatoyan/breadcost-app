'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Spinner, Alert, Badge, Success } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';
import { DollarSign, AlertTriangle, Check, X, ArrowUp, ArrowDown } from 'lucide-react';

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
  LOW: 'bg-blue-100 text-blue-800 border-blue-200',
  MEDIUM: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  HIGH: 'bg-orange-100 text-orange-800 border-orange-200',
  CRITICAL: 'bg-red-100 text-red-800 border-red-200',
};

/* ── page ──────────────────────────────────────────────── */
export default function AiPricingPage() {
  const t = useT();

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

  /* ── load both on mount ──────────────────────────────── */
  useEffect(() => { loadPricing(); }, [loadPricing]);
  useEffect(() => { loadAlerts(); }, [loadAlerts]);

  return (
    <div className="space-y-6">
      <SectionTitle eyebrow={t('aiPricing.eyebrow')} title={t('aiPricing.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* ── Side-by-side layout ────────────────────────── */}
      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">

        {/* LEFT: Pricing Suggestions Table */}
        <div className="rounded-2xl border border-gray-200 bg-white shadow-sm">
          <div className="flex items-center gap-2 border-b border-gray-100 px-5 py-4">
            <DollarSign className="h-5 w-5 text-green-500" />
            <h3 className="font-semibold text-gray-900">{t('aiPricing.pricingSuggestions')}</h3>
            <span className="ml-auto rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">
              {pricing.filter(p => p.status === 'PENDING').length} {t('aiPricing.pending')}
            </span>
          </div>
          <div className="p-4">
            <div className="flex gap-2 items-center mb-3">
              <Button variant="primary" size="sm" onClick={generatePricing}>{t('aiPricing.generate')}</Button>
              <label className="flex items-center gap-1 text-xs ml-auto">
                <input type="checkbox" checked={pendingOnly} onChange={e => setPendingOnly(e.target.checked)} />
                {t('aiPricing.pending')}
              </label>
            </div>
            {pricingLoading ? <Spinner /> : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100 text-left text-xs text-gray-500">
                      <th className="py-2 pr-3">{t('aiPricing.product')}</th>
                      <th className="py-2 pr-3">{t('aiPricing.currentPrice')}</th>
                      <th className="py-2 pr-3">{t('aiPricing.suggestedPrice')}</th>
                      <th className="py-2 pr-3">Change</th>
                      <th className="py-2 pr-3">{t('aiPricing.reasoning')}</th>
                      <th className="py-2"></th>
                    </tr>
                  </thead>
                  <tbody>
                    {pricing.length === 0 && (
                      <tr><td colSpan={6} className="py-8 text-center text-gray-400">{t('aiPricing.noPricing')}</td></tr>
                    )}
                    {pricing.map(p => {
                      const change = p.suggestedPrice - p.currentPrice;
                      const pct = p.currentPrice > 0 ? (change / p.currentPrice * 100) : 0;
                      const isUp = change > 0;
                      return (
                        <tr key={p.id} className="border-b border-gray-50 hover:bg-gray-50/50">
                          <td className="py-2.5 pr-3 font-medium text-gray-900">{p.productName}</td>
                          <td className="py-2.5 pr-3 text-gray-500">{p.currentPrice?.toFixed(2)}</td>
                          <td className="py-2.5 pr-3 font-semibold text-gray-900">{p.suggestedPrice?.toFixed(2)}</td>
                          <td className="py-2.5 pr-3">
                            <span className={`inline-flex items-center gap-0.5 text-xs font-medium ${isUp ? 'text-green-600' : 'text-red-600'}`}>
                              {isUp ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />}
                              {Math.abs(pct).toFixed(1)}%
                            </span>
                          </td>
                          <td className="py-2.5 pr-3 text-xs text-gray-500 max-w-[200px] truncate">{p.reasoning}</td>
                          <td className="py-2.5">
                            {p.status === 'PENDING' ? (
                              <div className="flex gap-1">
                                <button onClick={() => acceptPricing(p.id)} className="rounded-lg bg-green-600 p-1.5 text-white hover:bg-green-700 transition" title={t('aiPricing.accept')}>
                                  <Check className="h-3.5 w-3.5" />
                                </button>
                                <button onClick={() => dismissPricing(p.id)} className="rounded-lg border border-gray-300 p-1.5 text-gray-500 hover:bg-gray-50 transition" title={t('aiPricing.dismiss')}>
                                  <X className="h-3.5 w-3.5" />
                                </button>
                              </div>
                            ) : <Badge status={p.status} />}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* RIGHT: Anomaly Alert Cards */}
        <div className="rounded-2xl border border-gray-200 bg-white shadow-sm">
          <div className="flex items-center gap-2 border-b border-gray-100 px-5 py-4">
            <AlertTriangle className="h-5 w-5 text-amber-500" />
            <h3 className="font-semibold text-gray-900">{t('aiPricing.anomalyAlerts')}</h3>
            <span className="ml-auto rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">
              {alerts.filter(a => a.status === 'ACTIVE').length} {t('aiPricing.active')}
            </span>
          </div>
          <div className="p-4">
            <div className="flex gap-2 items-center mb-3">
              <Button variant="primary" size="sm" onClick={generateAlerts}>{t('aiPricing.generate')}</Button>
              <label className="flex items-center gap-1 text-xs ml-auto">
                <input type="checkbox" checked={activeOnly} onChange={e => setActiveOnly(e.target.checked)} />
                {t('aiPricing.active')}
              </label>
            </div>
            {alertsLoading && <Spinner />}
            {!alertsLoading && alerts.length === 0 && (
              <p className="text-center text-sm text-gray-400 py-8">{t('aiPricing.noAlerts')}</p>
            )}
            {!alertsLoading && alerts.length > 0 && (
              <div className="space-y-3 max-h-[60vh] overflow-y-auto">
                {alerts.map(a => {
                  const colors = SEVERITY_COLORS[a.severity] || 'bg-gray-100 text-gray-800 border-gray-200';
                  return (
                    <div key={a.id} className="rounded-xl border border-gray-200 p-3">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-semibold text-gray-900">{a.alertType}</span>
                        <span className={`rounded-full border px-2 py-0.5 text-xs font-medium ${colors}`}>
                          {a.severity}
                        </span>
                      </div>
                      <p className="text-xs text-gray-500 mb-1">{a.explanation}</p>
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-xs font-medium text-gray-700">
                          Deviation: <strong className={a.deviationPercent > 20 ? 'text-red-600' : 'text-gray-900'}>{a.deviationPercent?.toFixed(1)}%</strong>
                        </span>
                      </div>
                      <p className="text-xs text-blue-700 bg-blue-50 rounded-lg px-2 py-1 mb-2">{a.suggestedAction}</p>
                      {a.status === 'ACTIVE' ? (
                        <div className="flex gap-2">
                          <button onClick={() => acknowledgeAlert(a.id)}
                            className="flex items-center gap-1 rounded-lg bg-blue-600 px-2.5 py-1 text-xs text-white hover:bg-blue-700 transition">
                            <Check className="h-3 w-3" /> {t('aiPricing.acknowledge')}
                          </button>
                          <button onClick={() => dismissAlert(a.id)}
                            className="flex items-center gap-1 rounded-lg border border-gray-300 px-2.5 py-1 text-xs text-gray-600 hover:bg-gray-50 transition">
                            <X className="h-3 w-3" /> {t('aiPricing.dismiss')}
                          </button>
                        </div>
                      ) : <Badge status={a.status} />}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
