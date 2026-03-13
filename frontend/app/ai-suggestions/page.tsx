'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Spinner, Alert, Success, Field } from '@/components/ui';
import { SectionTitle, Button, Card } from '@/components/design-system';
import { Package, BarChart3, Factory, X, ShoppingCart } from 'lucide-react';

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

/* ── Mini bar chart ────────────────────────────────────── */
function MiniBarChart({ data }: { data: { label: string; value: number }[] }) {
  const max = Math.max(...data.map(d => d.value), 1);
  return (
    <div className="flex items-end gap-2 h-40 px-2">
      {data.map((d, i) => (
        <div key={i} className="flex-1 flex flex-col items-center gap-1">
          <span className="text-[10px] font-medium text-gray-600">{d.value}</span>
          <div
            className="w-full rounded-t-lg bg-blue-500 transition-all"
            style={{ height: `${(d.value / max) * 100}%`, minHeight: 4 }}
          />
          <span className="text-[10px] text-gray-500 truncate max-w-full">{d.label}</span>
        </div>
      ))}
    </div>
  );
}

/* ── page ──────────────────────────────────────────────── */
export default function AiSuggestionsPage() {
  const t = useT();

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

  /* ── load all on mount ───────────────────────────────── */
  useEffect(() => { loadHints(); }, [loadHints]);
  useEffect(() => { loadForecasts(); }, [loadForecasts]);
  useEffect(() => { loadSuggestions(); }, [loadSuggestions]);

  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="AI Tools" title={t('aiSuggestions.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* ── 3-card grid ────────────────────────────────── */}
      <div className="grid gap-6 xl:grid-cols-3">

        {/* ── Card 1: Replenishment ──────────────────── */}
        <div className="rounded-2xl border border-gray-200 bg-white shadow-sm flex flex-col">
          <div className="flex items-center gap-2 border-b border-gray-100 px-5 py-4">
            <Package className="h-5 w-5 text-orange-500" />
            <h3 className="font-semibold text-gray-900">{t('aiSuggestions.replenishment')}</h3>
            <span className="ml-auto rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-700">
              {hints.filter(h => h.status === 'PENDING').length} {t('aiSuggestions.pending')}
            </span>
          </div>
          <div className="p-4 flex-1">
            <div className="flex gap-2 items-end flex-wrap mb-3">
              <Field label={t('aiSuggestions.period')}>
                <select className="border rounded-lg px-2 py-1 text-sm" value={repPeriod} onChange={e => setRepPeriod(e.target.value as 'WEEKLY' | 'MONTHLY')}>
                  <option value="WEEKLY">{t('aiSuggestions.weekly')}</option>
                  <option value="MONTHLY">{t('aiSuggestions.monthly')}</option>
                </select>
              </Field>
              <Button variant="primary" size="sm" onClick={generateHints}>{t('aiSuggestions.generate')}</Button>
              <label className="flex items-center gap-1 text-xs ml-auto">
                <input type="checkbox" checked={pendingOnly} onChange={e => setPendingOnly(e.target.checked)} />
                {t('aiSuggestions.pending')}
              </label>
            </div>
            {hintsLoading ? <Spinner /> : hints.length === 0 ? (
              <p className="text-center text-sm text-gray-400 py-8">{t('aiSuggestions.noHints')}</p>
            ) : (
              <div className="space-y-2 max-h-[50vh] overflow-y-auto">
                {hints.map(h => (
                  <div key={h.id} className="rounded-xl border border-gray-200 p-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-gray-900">{h.productName}</span>
                      <span className="text-xs text-gray-400">{h.daysLeft?.toFixed(1)}d left</span>
                    </div>
                    <div className="mt-1 flex items-center gap-3 text-xs text-gray-500">
                      <span>Stock: {h.currentStock}</span>
                      <span>Qty: <strong className="text-gray-900">{h.suggestedQuantity}</strong></span>
                    </div>
                    <p className="mt-1 text-xs text-gray-500">{h.reason}</p>
                    {h.status === 'PENDING' ? (
                      <div className="mt-2 flex gap-2">
                        <button className="flex items-center gap-1 rounded-lg bg-blue-600 px-2.5 py-1 text-xs text-white hover:bg-blue-700 transition">
                          <ShoppingCart className="h-3 w-3" /> Create PO
                        </button>
                        <button onClick={() => dismissHint(h.id)} className="flex items-center gap-1 rounded-lg border border-gray-300 px-2.5 py-1 text-xs text-gray-600 hover:bg-gray-50 transition">
                          <X className="h-3 w-3" /> {t('aiSuggestions.dismiss')}
                        </button>
                      </div>
                    ) : (
                      <span className="mt-2 inline-block text-xs text-gray-400">{h.status}</span>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* ── Card 2: Demand Forecast ────────────────── */}
        <div className="rounded-2xl border border-gray-200 bg-white shadow-sm flex flex-col">
          <div className="flex items-center gap-2 border-b border-gray-100 px-5 py-4">
            <BarChart3 className="h-5 w-5 text-blue-500" />
            <h3 className="font-semibold text-gray-900">{t('aiSuggestions.forecast')}</h3>
            <span className="ml-auto rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
              {forecastDays}d
            </span>
          </div>
          <div className="p-4 flex-1">
            <div className="flex gap-2 items-end mb-3">
              <Field label={t('aiSuggestions.forecastDays')}>
                <select className="border rounded-lg px-2 py-1 text-sm" value={forecastDays} onChange={e => setForecastDays(Number(e.target.value))}>
                  <option value={7}>7</option>
                  <option value={14}>14</option>
                  <option value={30}>30</option>
                </select>
              </Field>
              <Button variant="primary" size="sm" onClick={generateForecast}>{t('aiSuggestions.generate')}</Button>
            </div>
            {forecastLoading ? <Spinner /> : forecasts.length === 0 ? (
              <p className="text-center text-sm text-gray-400 py-8">{t('aiSuggestions.noForecasts')}</p>
            ) : (
              <>
                <MiniBarChart
                  data={forecasts.slice(0, 7).map(f => ({
                    label: f.productName.slice(0, 8),
                    value: f.predictedQuantity,
                  }))}
                />
                <div className="mt-4 space-y-2 max-h-[30vh] overflow-y-auto">
                  {forecasts.map(f => (
                    <div key={f.id} className="flex items-center justify-between rounded-xl bg-gray-50 px-3 py-2">
                      <span className="text-sm font-medium text-gray-700">{f.productName}</span>
                      <div className="flex items-center gap-3 text-xs">
                        <span className="text-gray-500">{f.forecastDays}d</span>
                        <span className="font-semibold text-gray-900">{f.predictedQuantity}</span>
                        <span className={`rounded-full px-2 py-0.5 font-medium ${
                          f.confidence >= 0.8 ? 'bg-green-100 text-green-700'
                            : f.confidence >= 0.6 ? 'bg-yellow-100 text-yellow-700'
                            : 'bg-red-100 text-red-700'
                        }`}>
                          {(f.confidence * 100).toFixed(0)}%
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </div>

        {/* ── Card 3: Production Suggestions ─────────── */}
        <div className="rounded-2xl border border-gray-200 bg-white shadow-sm flex flex-col">
          <div className="flex items-center gap-2 border-b border-gray-100 px-5 py-4">
            <Factory className="h-5 w-5 text-green-500" />
            <h3 className="font-semibold text-gray-900">{t('aiSuggestions.production')}</h3>
            <span className="ml-auto rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">
              {suggestions.length}
            </span>
          </div>
          <div className="p-4 flex-1">
            <div className="flex gap-2 items-end mb-3">
              <Field label={t('aiSuggestions.planDate')}>
                <input type="date" className="border rounded-lg px-2 py-1 text-sm" value={planDate} onChange={e => setPlanDate(e.target.value)} />
              </Field>
              <Button variant="primary" size="sm" onClick={generateSuggestions}>{t('aiSuggestions.generate')}</Button>
            </div>
            {prodLoading ? <Spinner /> : suggestions.length === 0 ? (
              <p className="text-center text-sm text-gray-400 py-8">{t('aiSuggestions.noSuggestions')}</p>
            ) : (
              <div className="space-y-2 max-h-[50vh] overflow-y-auto">
                {suggestions.map(s => (
                  <div key={s.id} className="rounded-xl border border-gray-200 p-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-gray-900">{s.productName}</span>
                      <span className="font-semibold text-sm text-gray-900">×{s.suggestedQuantity}</span>
                    </div>
                    <p className="mt-1 text-xs text-gray-500">{s.reason}</p>
                    <p className="mt-0.5 text-xs text-gray-400">{s.planDate}</p>
                    <button className="mt-2 flex items-center gap-1 rounded-lg bg-green-600 px-2.5 py-1 text-xs text-white hover:bg-green-700 transition">
                      <Factory className="h-3 w-3" /> Create Plan
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
