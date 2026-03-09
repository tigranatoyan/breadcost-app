'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Table, Spinner, Alert, Badge, Modal, Field, Success } from '@/components/ui';

/* ── types ─────────────────────────────────────────────── */
interface DriverSession {
  id: string;
  driverId: string;
  driverName: string;
  runId: string;
  status: string;
  startTime: string;
  endTime?: string;
  lastLat?: number;
  lastLng?: number;
}
interface PackagingConfirmation {
  id: string;
  runId: string;
  driverId: string;
  allConfirmed: boolean;
  discrepancies?: string;
  confirmedAt: string;
}
interface DriverPayment {
  id: string;
  sessionId: string;
  orderId: string;
  amount: number;
  paymentMethod: string;
  reference?: string;
  createdAt: string;
}
interface ManifestItem {
  id: string;
  orderId: string;
  status: string;
  customerName?: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function DriverPage() {
  const t = useT();

  const [tab, setTab] = useState<'sessions' | 'packaging' | 'payments'>('sessions');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* sessions */
  const [sessions, setSessions] = useState<DriverSession[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [manifestModal, setManifestModal] = useState<DriverSession | null>(null);
  const [manifest, setManifest] = useState<ManifestItem[]>([]);

  /* packaging */
  const [packaging, setPackaging] = useState<PackagingConfirmation[]>([]);
  const [packagingLoading, setPackagingLoading] = useState(false);

  /* payments */
  const [payments, setPayments] = useState<DriverPayment[]>([]);
  const [paymentsLoading, setPaymentsLoading] = useState(false);
  const [paySessionId, setPaySessionId] = useState('');

  /* ── loaders ─────────────────────────────────────────── */
  const loadSessions = useCallback(async () => {
    setSessionsLoading(true);
    try {
      setSessions(await apiFetch<DriverSession[]>(`/v3/driver/sessions/active?tenantId=${TENANT_ID}`));
    } catch (e) { setError(String(e)); }
    finally { setSessionsLoading(false); }
  }, []);

  const openManifest = async (s: DriverSession) => {
    setManifestModal(s);
    try {
      setManifest(await apiFetch<ManifestItem[]>(`/v3/driver/sessions/${s.id}/manifest?tenantId=${TENANT_ID}`));
    } catch (e) { setError(String(e)); }
  };

  const endSession = async (id: string) => {
    try {
      await apiFetch(`/v3/driver/sessions/${id}/end?tenantId=${TENANT_ID}`, { method: 'POST' });
      setSuccess(t('driver.sessionEnded'));
      loadSessions();
    } catch (e) { setError(String(e)); }
  };

  const loadPayments = useCallback(async () => {
    if (!paySessionId.trim()) return;
    setPaymentsLoading(true);
    try {
      setPayments(await apiFetch<DriverPayment[]>(`/v3/driver/payments/session/${paySessionId}`));
    } catch (e) { setError(String(e)); }
    finally { setPaymentsLoading(false); }
  }, [paySessionId]);

  /* ── effects ─────────────────────────────────────────── */
  useEffect(() => { if (tab === 'sessions') loadSessions(); }, [tab, loadSessions]);

  const tabs: { key: typeof tab; label: string }[] = [
    { key: 'sessions', label: t('driver.activeSessions') },
    { key: 'packaging', label: t('driver.packaging') },
    { key: 'payments', label: t('driver.payments') },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">{t('driver.title')}</h1>

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

      {/* ── Active Sessions ────────────────────────────── */}
      {tab === 'sessions' && (
        <div className="space-y-4">
          <button onClick={loadSessions} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">{t('driver.refresh')}</button>
          {sessionsLoading ? <Spinner /> : (
            <Table
              cols={[t('driver.driverName'), t('driver.runId'), t('driver.status'), t('driver.startTime'), t('driver.location'), '']}
              rows={sessions.map(s => [
                s.driverName,
                s.runId,
                <Badge key={s.id} status={s.status} />,
                new Date(s.startTime).toLocaleString(),
                s.lastLat && s.lastLng ? `${s.lastLat.toFixed(4)}, ${s.lastLng.toFixed(4)}` : '–',
                <div key={s.id + 'a'} className="flex gap-2">
                  <button onClick={() => openManifest(s)} className="text-blue-600 hover:underline text-sm">{t('driver.manifest')}</button>
                  <button onClick={() => endSession(s.id)} className="text-red-600 hover:underline text-sm">{t('driver.endSession')}</button>
                </div>,
              ])}
              empty={t('driver.noSessions')}
            />
          )}
        </div>
      )}

      {/* ── Packaging ──────────────────────────────────── */}
      {tab === 'packaging' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-end">
            <Field label={t('driver.runId')}><input className="input w-full" value={paySessionId} onChange={e => setPaySessionId(e.target.value)} /></Field>
            <button onClick={async () => {
              if (!paySessionId.trim()) return;
              setPackagingLoading(true);
              try {
                const p = await apiFetch<PackagingConfirmation>(`/v3/driver/packaging/${paySessionId}?tenantId=${TENANT_ID}`);
                setPackaging(p ? [p] : []);
              } catch (e) { setError(String(e)); }
              finally { setPackagingLoading(false); }
            }} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 h-10">{t('driver.lookup')}</button>
          </div>
          {packagingLoading ? <Spinner /> : (
            <Table
              cols={[t('driver.runId'), t('driver.driverId'), t('driver.confirmed'), t('driver.discrepancies'), t('driver.confirmedAt')]}
              rows={packaging.map(p => [
                p.runId,
                p.driverId,
                p.allConfirmed ? '✅' : '❌',
                p.discrepancies || '–',
                new Date(p.confirmedAt).toLocaleString(),
              ])}
              empty={t('driver.noPackaging')}
            />
          )}
        </div>
      )}

      {/* ── Payments ───────────────────────────────────── */}
      {tab === 'payments' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-end">
            <Field label={t('driver.sessionId')}><input className="input w-full" value={paySessionId} onChange={e => setPaySessionId(e.target.value)} /></Field>
            <button onClick={loadPayments} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 h-10">{t('driver.lookup')}</button>
          </div>
          {paymentsLoading ? <Spinner /> : (
            <Table
              cols={[t('driver.orderId'), t('driver.amount'), t('driver.paymentMethod'), t('driver.reference'), t('driver.date')]}
              rows={payments.map(p => [
                p.orderId,
                p.amount?.toFixed(2),
                p.paymentMethod,
                p.reference || '–',
                new Date(p.createdAt).toLocaleString(),
              ])}
              empty={t('driver.noPayments')}
            />
          )}
        </div>
      )}

      {/* ── Manifest Modal ─────────────────────────────── */}
      {manifestModal && (
        <Modal title={`${t('driver.manifest')} – ${manifestModal.driverName}`} onClose={() => { setManifestModal(null); setManifest([]); }}>
          <Table
            cols={[t('driver.orderId'), t('driver.status'), t('driver.customer')]}
            rows={manifest.map(m => [
              m.orderId,
              <Badge key={m.id} status={m.status} />,
              m.customerName || '–',
            ])}
            empty={t('driver.noManifest')}
          />
        </Modal>
      )}
    </div>
  );
}
