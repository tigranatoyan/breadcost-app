'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Table, Spinner, Alert, Success, Field } from '@/components/ui';

/* ── types ─────────────────────────────────────────────── */
interface ReplenishmentHint {
  id: string;
  productId: string;
  productName: string;
  suggestedQuantity: number;
  currentStock: number;
  daysLeft: number;
  reason: string;
  status: string;
}
interface DemandForecast {
  id: string;
  productId: string;
  productName: string;
  forecastDays: number;
  predictedQuantity: number;
  confidence: number;
}
interface ProductionSuggestion {
  id: string;
  productId: string;
  productName: string;
  suggestedQuantity: number;
  planDate: string;
  reason: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function AiSuggestionsPage() {
  const t = useT();

  const [tab, setTab] = useState<'replenishment' | 'forecast' | 'production'>('replenishment');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* replenishment */
  const [hints, setHints] = useState<ReplenishmentHint[]>([]);
  const [hintsLoading, setHintsLoading] = useState(false);
  const [pendingOnly, setPendingOnly] = useState(false);
  const [repPeriod, setRepPeriod] = useState<'WEEKLY' | 'MONTHLY'>('WEEKLY');

  /* forecast */
  const [forecasts, setForecasts] = useState<DemandForecast[]>([]);
  const [forecastLoading, setForecastLoading] = useState(false);
  const [forecastDays, setForecastDays] = useState(7);

  /* production */
  const [suggestions, setSuggestions] = useState<ProductionSuggestion[]>([]);
  const [prodLoading, setProdLoading] = useState(false);
  const [planDate, setPlanDate] = useState(new Date().toISOString().slice(0, 10));

  /* ── replenishment loaders ───────────────────────────── */
  const loadHints = useCallback(async () => {
    setHintsLoading(true);
    try {
      const endpoint = pendingOnly
        ? `/v3/ai/suggestions/replenishment/pending?tenantId=${TENANT_ID}`
        : `/v3/ai/suggestions/replenishment?tenantId=${TENANT_ID}`;
      setHints(await apiFetch<ReplenishmentHint[]>(endpoint));
    } catch (e) { setError(String(e)); }
    finally { setHintsLoading(false); }
  }, [pendingOnly]);

  const generateHints = async () => {
    try {
      const data = await apiFetch<ReplenishmentHint[]>(`/v3/ai/suggestions/replenishment/generate?tenantId=${TENANT_ID}&period=${repPeriod}`, { method: 'POST' });
      setHints(data);
      setSuccess(t('aiSuggestions.generated'));
    } catch (e) { setError(String(e)); }
  };

  const dismissHint = async (id: string) => {
    try {
      await apiFetch(`/v3/ai/suggestions/replenishment/${id}/dismiss?tenantId=${TENANT_ID}`, { method: 'POST' });
      loadHints();
    } catch (e) { setError(String(e)); }
  };

  /* ── forecast loaders ────────────────────────────────── */
  const loadForecasts = useCallback(async () => {
    setForecastLoading(true);
    try {
      setForecasts(await apiFetch<DemandForecast[]>(`/v3/ai/suggestions/forecast?tenantId=${TENANT_ID}`));
    } catch (e) { setError(String(e)); }
    finally { setForecastLoading(false); }
  }, []);

  const generateForecast = async () => {
    try {
      const data = await apiFetch<DemandForecast[]>(`/v3/ai/suggestions/forecast/generate?tenantId=${TENANT_ID}&forecastDays=${forecastDays}`, { method: 'POST' });
      setForecasts(data);
      setSuccess(t('aiSuggestions.generated'));
    } catch (e) { setError(String(e)); }
  };

  /* ── production loaders ──────────────────────────────── */
  const loadSuggestions = useCallback(async () => {
    setProdLoading(true);
    try {
      setSuggestions(await apiFetch<ProductionSuggestion[]>(`/v3/ai/suggestions/production?tenantId=${TENANT_ID}&planDate=${planDate}`));
    } catch (e) { setError(String(e)); }
    finally { setProdLoading(false); }
  }, [planDate]);

  const generateSuggestions = async () => {
    try {
      const data = await apiFetch<ProductionSuggestion[]>(`/v3/ai/suggestions/production/generate?tenantId=${TENANT_ID}&planDate=${planDate}`, { method: 'POST' });
      setSuggestions(data);
      setSuccess(t('aiSuggestions.generated'));
    } catch (e) { setError(String(e)); }
  };

  /* ── tab change effects ──────────────────────────────── */
  useEffect(() => {
    if (tab === 'replenishment') loadHints();
    else if (tab === 'forecast') loadForecasts();
    else loadSuggestions();
  }, [tab, loadHints, loadForecasts, loadSuggestions]);

  const tabs: { key: typeof tab; label: string }[] = [
    { key: 'replenishment', label: t('aiSuggestions.replenishment') },
    { key: 'forecast', label: t('aiSuggestions.forecast') },
    { key: 'production', label: t('aiSuggestions.production') },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">{t('aiSuggestions.title')}</h1>

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

      {/* ── Replenishment ──────────────────────────────── */}
      {tab === 'replenishment' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-end flex-wrap">
            <Field label={t('aiSuggestions.period')}>
              <select className="border rounded px-2 py-1" value={repPeriod} onChange={e => setRepPeriod(e.target.value as 'WEEKLY' | 'MONTHLY')}>
                <option value="WEEKLY">{t('aiSuggestions.weekly')}</option>
                <option value="MONTHLY">{t('aiSuggestions.monthly')}</option>
              </select>
            </Field>
            <button onClick={generateHints} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">{t('aiSuggestions.generate')}</button>
            <label className="flex items-center gap-1 text-sm ml-auto">
              <input type="checkbox" checked={pendingOnly} onChange={e => setPendingOnly(e.target.checked)} />
              {t('aiSuggestions.pending')}
            </label>
          </div>
          {hintsLoading ? <Spinner /> : (
            <Table
              cols={[t('aiSuggestions.product'), t('aiSuggestions.suggestedQty'), t('aiSuggestions.currentStock'), t('aiSuggestions.daysLeft'), t('aiSuggestions.reason'), '']}
              rows={hints.map(h => [
                h.productName,
                h.suggestedQuantity,
                h.currentStock,
                h.daysLeft?.toFixed(1),
                h.reason,
                h.status === 'PENDING' ? <button key={h.id} onClick={() => dismissHint(h.id)} className="text-red-600 hover:underline text-sm">{t('aiSuggestions.dismiss')}</button> : <span key={h.id} className="text-gray-400 text-sm">{h.status}</span>,
              ])}
              empty={t('aiSuggestions.noHints')}
            />
          )}
        </div>
      )}

      {/* ── Demand Forecast ────────────────────────────── */}
      {tab === 'forecast' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-end">
            <Field label={t('aiSuggestions.forecastDays')}>
              <select className="border rounded px-2 py-1" value={forecastDays} onChange={e => setForecastDays(Number(e.target.value))}>
                <option value={7}>7</option>
                <option value={14}>14</option>
                <option value={30}>30</option>
              </select>
            </Field>
            <button onClick={generateForecast} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">{t('aiSuggestions.generate')}</button>
          </div>
          {forecastLoading ? <Spinner /> : (
            <Table
              cols={[t('aiSuggestions.product'), t('aiSuggestions.forecastDays'), t('aiSuggestions.predictedQty'), t('aiSuggestions.confidence')]}
              rows={forecasts.map(f => [f.productName, f.forecastDays, f.predictedQuantity, `${(f.confidence * 100).toFixed(0)}%`])}
              empty={t('aiSuggestions.noForecasts')}
            />
          )}
        </div>
      )}

      {/* ── Production Suggestions ─────────────────────── */}
      {tab === 'production' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-end">
            <Field label={t('aiSuggestions.planDate')}>
              <input type="date" className="border rounded px-2 py-1" value={planDate} onChange={e => setPlanDate(e.target.value)} />
            </Field>
            <button onClick={generateSuggestions} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">{t('aiSuggestions.generate')}</button>
          </div>
          {prodLoading ? <Spinner /> : (
            <Table
              cols={[t('aiSuggestions.product'), t('aiSuggestions.suggestedQty'), t('aiSuggestions.planDate'), t('aiSuggestions.reason')]}
              rows={suggestions.map(s => [s.productName, s.suggestedQuantity, s.planDate, s.reason])}
              empty={t('aiSuggestions.noSuggestions')}
            />
          )}
        </div>
      )}
    </div>
  );
}
