'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Alert, Field, Success, PageSkeleton } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';

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
const CURRENCIES = ['AMD', 'USD', 'EUR', 'RUB', 'GEL', 'GBP', 'CHF', 'TRY', 'AED', 'CNY', 'JPY', 'KRW', 'INR', 'BRL', 'CAD', 'AUD'] as const;

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

  if (loading) return <PageSkeleton />;

  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="Currency" title={t('exchangeRates.title')} action={
        <div className="flex gap-2">
          <Button variant="primary" size="sm" className="bg-green-600 hover:bg-green-700" onClick={() => setShowFetch(true)}>{t('exchangeRates.fetchApi')}</Button>
          <Button variant="primary" size="sm" onClick={() => setShowAdd(true)}>{t('exchangeRates.addRate')}</Button>
        </div>
      } />

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
          <Field label={t('exchangeRates.targetCurrency')}><select className="border rounded px-2 py-1 w-24" value={lookupCode} onChange={e => setLookupCode(e.target.value)}><option value="">—</option>{CURRENCIES.map(c => <option key={c} value={c}>{c}</option>)}</select></Field>
          <Field label={t('exchangeRates.rateDate')}><input type="date" className="border rounded px-2 py-1" value={lookupDate} onChange={e => setLookupDate(e.target.value)} /></Field>
          <Button variant="primary" size="sm" onClick={doLookup} className="h-fit">{t('exchangeRates.lookup')}</Button>
        </div>
        {lookupResult && <p className="mt-2 text-sm">1 {lookupResult.baseCurrency} = {lookupResult.rate} {lookupResult.currencyCode} ({lookupResult.rateDate})</p>}
      </div>

      {/* Convert section */}
      <div className="rounded border p-4 bg-gray-50">
        <h3 className="font-semibold mb-2">{t('exchangeRates.convert')}</h3>
        <div className="flex gap-2 items-end flex-wrap">
          <Field label={t('exchangeRates.fromCurrency')}><select className="border rounded px-2 py-1 w-24" value={convFrom} onChange={e => setConvFrom(e.target.value)}><option value="">—</option>{CURRENCIES.map(c => <option key={c} value={c}>{c}</option>)}</select></Field>
          <Field label={t('exchangeRates.toCurrency')}><select className="border rounded px-2 py-1 w-24" value={convTo} onChange={e => setConvTo(e.target.value)}><option value="">—</option>{CURRENCIES.map(c => <option key={c} value={c}>{c}</option>)}</select></Field>
          <Field label={t('exchangeRates.amount')}><input type="number" className="border rounded px-2 py-1 w-32" value={convAmount} onChange={e => setConvAmount(e.target.value)} /></Field>
          <Field label={t('exchangeRates.rateDate')}><input type="date" className="border rounded px-2 py-1" value={convDate} onChange={e => setConvDate(e.target.value)} /></Field>
          <Button variant="primary" size="sm" onClick={doConvert} className="h-fit">{t('exchangeRates.convert')}</Button>
        </div>
        {convResult && <p className="mt-2 text-sm font-semibold">{convResult.amount} {convResult.from} = {convResult.converted.toFixed(2)} {convResult.to}</p>}
      </div>

      {/* Add rate modal */}
      {showAdd && (
        <Modal title={t('exchangeRates.addRate')} onClose={() => setShowAdd(false)}>
          <form onSubmit={addRate} className="space-y-4">
            <Field label={t('exchangeRates.baseCurrency')}><select className="border rounded px-2 py-1 w-full" value={form.baseCurrency} onChange={e => setForm({ ...form, baseCurrency: e.target.value })} required>{CURRENCIES.map(c => <option key={c} value={c}>{c}</option>)}</select></Field>
            <Field label={t('exchangeRates.targetCurrency')}><select className="border rounded px-2 py-1 w-full" value={form.currencyCode} onChange={e => setForm({ ...form, currencyCode: e.target.value })} required><option value="">—</option>{CURRENCIES.map(c => <option key={c} value={c}>{c}</option>)}</select></Field>
            <Field label={t('exchangeRates.rate')}><input type="number" step="0.0001" className="border rounded px-2 py-1 w-full" value={form.rate} onChange={e => setForm({ ...form, rate: e.target.value })} required /></Field>
            <Field label={t('exchangeRates.rateDate')}><input type="date" className="border rounded px-2 py-1 w-full" value={form.rateDate} onChange={e => setForm({ ...form, rateDate: e.target.value })} required /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowAdd(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* Fetch from API modal */}
      {showFetch && (
        <Modal title={t('exchangeRates.fetchApi')} onClose={() => setShowFetch(false)}>
          <div className="space-y-4">
            <Field label={t('exchangeRates.baseCurrency')}><select className="border rounded px-2 py-1 w-full" value={fetchBase} onChange={e => setFetchBase(e.target.value)}>{CURRENCIES.map(c => <option key={c} value={c}>{c}</option>)}</select></Field>
            <Field label={t('exchangeRates.fetchTargets')}><input className="border rounded px-2 py-1 w-full" value={fetchTargets} onChange={e => setFetchTargets(e.target.value)} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowFetch(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" className="bg-green-600 hover:bg-green-700" onClick={doFetch}>{t('exchangeRates.fetchApi')}</Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
