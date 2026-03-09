'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Field, Success } from '@/components/ui';

/* ── types ─────────────────────────────────────────────── */
interface ExchangeRate {
  id: string;
  tenantId: string;
  baseCurrency: string;
  currencyCode: string;
  rate: number;
  rateDate: string;
}
interface ConvertResult {
  from: string;
  to: string;
  amount: number;
  converted: number;
  date: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function ExchangeRatesPage() {
  const t = useT();

  const [rates, setRates] = useState<ExchangeRate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* add rate */
  const [showAdd, setShowAdd] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ baseCurrency: 'AMD', currencyCode: '', rate: '', rateDate: '' });

  /* lookup */
  const [lookupCode, setLookupCode] = useState('');
  const [lookupDate, setLookupDate] = useState('');
  const [lookupResult, setLookupResult] = useState<ExchangeRate | null>(null);

  /* convert */
  const [convFrom, setConvFrom] = useState('');
  const [convTo, setConvTo] = useState('');
  const [convAmount, setConvAmount] = useState('');
  const [convDate, setConvDate] = useState('');
  const [convResult, setConvResult] = useState<ConvertResult | null>(null);

  /* fetch API */
  const [showFetch, setShowFetch] = useState(false);
  const [fetchBase, setFetchBase] = useState('AMD');
  const [fetchTargets, setFetchTargets] = useState('USD,EUR');

  const loadRates = useCallback(async () => {
    try {
      setLoading(true);
      const data = await apiFetch<ExchangeRate[]>(`/v3/exchange-rates?tenantId=${TENANT_ID}`);
      setRates(data);
    } catch (e) { setError(String(e)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadRates(); }, [loadRates]);

  const addRate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await apiFetch(`/v3/exchange-rates?tenantId=${TENANT_ID}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          baseCurrency: form.baseCurrency,
          currencyCode: form.currencyCode,
          rate: parseFloat(form.rate),
          rateDate: form.rateDate,
        }),
      });
      setSuccess(t('exchangeRates.rateAdded'));
      setShowAdd(false);
      setForm({ baseCurrency: 'AMD', currencyCode: '', rate: '', rateDate: '' });
      loadRates();
    } catch (e) { setError(String(e)); }
    finally { setSaving(false); }
  };

  const doLookup = async () => {
    try {
      const r = await apiFetch<ExchangeRate>(
        `/v3/exchange-rates/lookup?tenantId=${TENANT_ID}&currencyCode=${lookupCode}&date=${lookupDate}`
      );
      setLookupResult(r);
    } catch (e) { setError(String(e)); }
  };

  const doConvert = async () => {
    try {
      const r = await apiFetch<ConvertResult>(
        `/v3/exchange-rates/convert?tenantId=${TENANT_ID}&from=${convFrom}&to=${convTo}&amount=${convAmount}&date=${convDate}`
      );
      setConvResult(r);
    } catch (e) { setError(String(e)); }
  };

  const doFetch = async () => {
    try {
      await apiFetch(`/v3/exchange-rates/fetch?tenantId=${TENANT_ID}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ baseCurrency: fetchBase, targetCurrencies: fetchTargets.split(',').map(s => s.trim()) }),
      });
      setSuccess(t('exchangeRates.ratesFetched'));
      setShowFetch(false);
      loadRates();
    } catch (e) { setError(String(e)); }
  };

  if (loading) return <Spinner />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('exchangeRates.title')}</h1>
        <div className="flex gap-2">
          <button onClick={() => setShowFetch(true)} className="rounded bg-green-600 px-4 py-2 text-white hover:bg-green-700">{t('exchangeRates.fetchApi')}</button>
          <button onClick={() => setShowAdd(true)} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">{t('exchangeRates.addRate')}</button>
        </div>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* Rates table */}
      <Table
        cols={[t('exchangeRates.baseCurrency'), t('exchangeRates.targetCurrency'), t('exchangeRates.rate'), t('exchangeRates.rateDate')]}
        rows={rates.map(r => [r.baseCurrency, r.currencyCode, r.rate.toFixed(4), r.rateDate])}
        empty={t('exchangeRates.noRates')}
      />

      {/* Lookup section */}
      <div className="rounded border p-4 bg-gray-50">
        <h3 className="font-semibold mb-2">{t('exchangeRates.lookup')}</h3>
        <div className="flex gap-2 items-end">
          <Field label={t('exchangeRates.targetCurrency')}><input className="border rounded px-2 py-1 w-24" value={lookupCode} onChange={e => setLookupCode(e.target.value)} /></Field>
          <Field label={t('exchangeRates.rateDate')}><input type="date" className="border rounded px-2 py-1" value={lookupDate} onChange={e => setLookupDate(e.target.value)} /></Field>
          <button onClick={doLookup} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 h-fit">{t('exchangeRates.lookup')}</button>
        </div>
        {lookupResult && <p className="mt-2 text-sm">1 {lookupResult.baseCurrency} = {lookupResult.rate} {lookupResult.currencyCode} ({lookupResult.rateDate})</p>}
      </div>

      {/* Convert section */}
      <div className="rounded border p-4 bg-gray-50">
        <h3 className="font-semibold mb-2">{t('exchangeRates.convert')}</h3>
        <div className="flex gap-2 items-end flex-wrap">
          <Field label={t('exchangeRates.fromCurrency')}><input className="border rounded px-2 py-1 w-24" value={convFrom} onChange={e => setConvFrom(e.target.value)} /></Field>
          <Field label={t('exchangeRates.toCurrency')}><input className="border rounded px-2 py-1 w-24" value={convTo} onChange={e => setConvTo(e.target.value)} /></Field>
          <Field label={t('exchangeRates.amount')}><input type="number" className="border rounded px-2 py-1 w-32" value={convAmount} onChange={e => setConvAmount(e.target.value)} /></Field>
          <Field label={t('exchangeRates.rateDate')}><input type="date" className="border rounded px-2 py-1" value={convDate} onChange={e => setConvDate(e.target.value)} /></Field>
          <button onClick={doConvert} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 h-fit">{t('exchangeRates.convert')}</button>
        </div>
        {convResult && <p className="mt-2 text-sm font-semibold">{convResult.amount} {convResult.from} = {convResult.converted.toFixed(2)} {convResult.to}</p>}
      </div>

      {/* Add rate modal */}
      {showAdd && (
        <Modal title={t('exchangeRates.addRate')} onClose={() => setShowAdd(false)}>
          <form onSubmit={addRate} className="space-y-4">
            <Field label={t('exchangeRates.baseCurrency')}><input className="border rounded px-2 py-1 w-full" value={form.baseCurrency} onChange={e => setForm({ ...form, baseCurrency: e.target.value })} required /></Field>
            <Field label={t('exchangeRates.targetCurrency')}><input className="border rounded px-2 py-1 w-full" value={form.currencyCode} onChange={e => setForm({ ...form, currencyCode: e.target.value })} required /></Field>
            <Field label={t('exchangeRates.rate')}><input type="number" step="0.0001" className="border rounded px-2 py-1 w-full" value={form.rate} onChange={e => setForm({ ...form, rate: e.target.value })} required /></Field>
            <Field label={t('exchangeRates.rateDate')}><input type="date" className="border rounded px-2 py-1 w-full" value={form.rateDate} onChange={e => setForm({ ...form, rateDate: e.target.value })} required /></Field>
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setShowAdd(false)} className="rounded border px-4 py-2">{t('common.cancel')}</button>
              <button type="submit" disabled={saving} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50">{t('common.save')}</button>
            </div>
          </form>
        </Modal>
      )}

      {/* Fetch from API modal */}
      {showFetch && (
        <Modal title={t('exchangeRates.fetchApi')} onClose={() => setShowFetch(false)}>
          <div className="space-y-4">
            <Field label={t('exchangeRates.baseCurrency')}><input className="border rounded px-2 py-1 w-full" value={fetchBase} onChange={e => setFetchBase(e.target.value)} /></Field>
            <Field label={t('exchangeRates.fetchTargets')}><input className="border rounded px-2 py-1 w-full" value={fetchTargets} onChange={e => setFetchTargets(e.target.value)} /></Field>
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowFetch(false)} className="rounded border px-4 py-2">{t('common.cancel')}</button>
              <button onClick={doFetch} className="rounded bg-green-600 px-4 py-2 text-white hover:bg-green-700">{t('exchangeRates.fetchApi')}</button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
