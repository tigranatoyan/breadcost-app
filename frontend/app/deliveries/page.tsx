'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';

/* ── types ─────────────────────────────────────────────── */
interface DeliveryRun {
  runId: string;
  tenantId: string;
  status: string;
  driverId?: string;
  driverName?: string;
  vehiclePlate?: string;
  scheduledDate?: string;
  completedAt?: string;
  notes?: string;
  createdAt?: string;
  runNumber?: number;
}
interface RunOrder {
  orderId: string;
  orderNumber?: number;
  customerName?: string;
  address?: string;
  status?: string;
}
interface ManifestEntry {
  orderId: string;
  orderNumber?: number;
  customerName?: string;
  address?: string;
  items?: { productName: string; qty: number }[];
}

/* ── page ──────────────────────────────────────────────── */
export default function DeliveriesPage() {
  const t = useT();

  const [runs, setRuns] = useState<DeliveryRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* create */
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ driverId: '', driverName: '', vehiclePlate: '', scheduledDate: '', notes: '' });
  const [saving, setSaving] = useState(false);

  /* detail / manifest */
  const [detailRun, setDetailRun] = useState<DeliveryRun | null>(null);
  const [runOrders, setRunOrders] = useState<RunOrder[]>([]);
  const [manifest, setManifest] = useState<ManifestEntry[] | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  /* assign */
  const [showAssign, setShowAssign] = useState<string | null>(null);
  const [readyOrders, setReadyOrders] = useState<{ orderId: string; customerName: string; totalAmount: number; requestedDeliveryTime: string }[]>([]);
  const [selectedOrderIds, setSelectedOrderIds] = useState<Set<string>>(new Set());
  const [loadingReady, setLoadingReady] = useState(false);

  /* loaders */
  const loadRuns = useCallback(async () => {
    try {
      setLoading(true);
      const data = await apiFetch<DeliveryRun[]>(`/v2/delivery-runs?tenantId=${TENANT_ID}`);
      setRuns(data);
    } catch (e) { setError(String(e)); } finally { setLoading(false); }
  }, []);

  useEffect(() => { loadRuns(); }, [loadRuns]);

  /* create */
  const createRun = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v2/delivery-runs', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID, ...form }) });
      setSuccess(t('deliveries.created'));
      setShowCreate(false);
      setForm({ driverId: '', driverName: '', vehiclePlate: '', scheduledDate: '', notes: '' });
      loadRuns();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  /* detail + manifest */
  const openDetail = async (run: DeliveryRun) => {
    setDetailRun(run);
    setDetailLoading(true);
    try {
      const [orders, man] = await Promise.all([
        apiFetch<RunOrder[]>(`/v2/delivery-runs/${run.runId}/orders?tenantId=${TENANT_ID}`).catch(() => []),
        apiFetch<ManifestEntry[]>(`/v2/delivery-runs/${run.runId}/manifest?tenantId=${TENANT_ID}`).catch(() => null),
      ]);
      setRunOrders(orders);
      setManifest(man);
    } catch (e) { setError(String(e)); } finally { setDetailLoading(false); }
  };

  /* assign orders */
  const assignOrders = async (runId: string) => {
    try {
      const ids = Array.from(selectedOrderIds);
      if (!ids.length) return;
      for (const orderId of ids) {
        await apiFetch(`/v2/delivery-runs/${runId}/orders`, {
          method: 'POST',
          body: JSON.stringify({ tenantId: TENANT_ID, orderId }),
        });
      }
      setSuccess(t('deliveries.assigned'));
      setShowAssign(null);
      setSelectedOrderIds(new Set());
      if (detailRun) openDetail(detailRun);
    } catch (e) { setError(String(e)); }
  };

  /* status actions */
  const completeRun = async (id: string) => {
    try {
      await apiFetch(`/v2/delivery-runs/${id}/complete?tenantId=${TENANT_ID}`, { method: 'PUT' });
      setSuccess(t('deliveries.completed'));
      loadRuns();
      setDetailRun(null);
    } catch (e) { setError(String(e)); }
  };
  const failDelivery = async (runId: string, orderId: string) => {
    const reason = prompt(t('deliveries.failReason'));
    if (!reason) return;
    try {
      await apiFetch(`/v2/delivery-runs/${runId}/fail?tenantId=${TENANT_ID}`, { method: 'PUT', body: JSON.stringify({ orderId, reason }) });
      setSuccess(t('deliveries.markedFailed'));
      if (detailRun) openDetail(detailRun);
    } catch (e) { setError(String(e)); }
  };
  const redelivery = async (runId: string, orderId: string) => {
    try {
      await apiFetch(`/v2/delivery-runs/${runId}/redeliver?tenantId=${TENANT_ID}`, { method: 'POST', body: JSON.stringify({ orderId }) });
      setSuccess(t('deliveries.redeliveryScheduled'));
      if (detailRun) openDetail(detailRun);
    } catch (e) { setError(String(e)); }
  };
  const waive = async (runId: string, orderId: string) => {
    try {
      await apiFetch(`/v2/delivery-runs/${runId}/waive?tenantId=${TENANT_ID}`, { method: 'POST', body: JSON.stringify({ orderId }) });
      setSuccess(t('deliveries.waived'));
      if (detailRun) openDetail(detailRun);
    } catch (e) { setError(String(e)); }
  };

  return (
    <div className="max-w-[1800px]">
      <SectionTitle
        eyebrow="Logistics"
        title={t('deliveries.title')}
        action={<Button variant="primary" size="sm" onClick={() => setShowCreate(true)}>+ {t('deliveries.createRun')}</Button>}
      />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {loading ? <Spinner /> : (
        <Table
          cols={[t('deliveries.runNumber'), t('deliveries.driver'), t('deliveries.vehicle'), t('deliveries.scheduledDate'), t('common.status'), t('common.actions')]}
          rows={runs.map(r => [
            <button key={r.runId} className="text-blue-600 underline text-sm" onClick={() => openDetail(r)}>RUN-{String(r.runNumber ?? 0).padStart(3, '0')}</button>,
            r.driverName || r.driverId || '—',
            r.vehiclePlate || '—',
            r.scheduledDate || '—',
            <Badge key={`s-${r.runId}`} status={r.status} />,
            <div key={`a-${r.runId}`} className="flex gap-1 flex-wrap">
              <Button variant="secondary" size="xs" onClick={async () => {
                setShowAssign(r.runId);
                setSelectedOrderIds(new Set());
                setLoadingReady(true);
                try {
                  const orders = await apiFetch<{ orderId: string; customerName: string; totalAmount: number; requestedDeliveryTime: string }[]>(`/v1/orders?tenantId=${TENANT_ID}&status=READY`);
                  setReadyOrders(orders);
                } catch { setReadyOrders([]); }
                finally { setLoadingReady(false); }
              }}>{t('deliveries.assign')}</Button>
              {(r.status === 'PLANNED' || r.status === 'IN_PROGRESS') && (
                <Button variant="primary" size="xs" className="bg-green-600 hover:bg-green-700" onClick={() => completeRun(r.runId)}>{t('deliveries.complete')}</Button>
              )}
            </div>,
          ])}
          empty={t('deliveries.empty')}
        />
      )}

      {/* create run modal */}
      {showCreate && (
        <Modal title={t('deliveries.createRun')} onClose={() => setShowCreate(false)}>
          <form onSubmit={createRun} className="space-y-4">
            <Field label={t('deliveries.driverName')}><input className="input w-full" value={form.driverName} onChange={e => setForm({ ...form, driverName: e.target.value })} /></Field>
            <Field label={t('deliveries.vehicle')}><input className="input w-full" value={form.vehiclePlate} onChange={e => setForm({ ...form, vehiclePlate: e.target.value })} /></Field>
            <Field label={t('deliveries.scheduledDate')}><input className="input w-full" type="date" value={form.scheduledDate} onChange={e => setForm({ ...form, scheduledDate: e.target.value })} /></Field>
            <Field label={t('common.notes')}><textarea className="input w-full" rows={2} value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowCreate(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{saving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* assign orders modal */}
      {showAssign && (
        <Modal title={t('deliveries.assignOrders')} onClose={() => setShowAssign(null)}>
          <p className="text-sm text-gray-500 mb-3">{t('deliveries.assignHint')}</p>
          {loadingReady ? <Spinner /> : readyOrders.length === 0 ? (
            <p className="text-sm text-gray-400 italic">{t('deliveries.noReadyOrders')}</p>
          ) : (
            <div className="max-h-64 overflow-y-auto border rounded divide-y">
              {readyOrders.map(o => (
                <label key={o.orderId} className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 cursor-pointer text-sm">
                  <input
                    type="checkbox"
                    checked={selectedOrderIds.has(o.orderId)}
                    onChange={() => {
                      setSelectedOrderIds(prev => {
                        const next = new Set(prev);
                        if (next.has(o.orderId)) next.delete(o.orderId); else next.add(o.orderId);
                        return next;
                      });
                    }}
                  />
                  <span className="font-mono text-xs text-gray-500">{o.orderNumber ? `ORD-${String(o.orderNumber).padStart(4, '0')}` : o.orderId.slice(0, 8)}</span>
                  <span className="flex-1">{o.customerName}</span>
                  <span className="text-gray-500">{o.totalAmount?.toLocaleString()}</span>
                </label>
              ))}
            </div>
          )}
          <div className="flex justify-end gap-2 mt-4">
            <Button variant="secondary" size="sm" onClick={() => setShowAssign(null)}>{t('common.cancel')}</Button>
            <Button variant="primary" size="sm" disabled={selectedOrderIds.size === 0} onClick={() => assignOrders(showAssign)}>{t('deliveries.assign')}</Button>
          </div>
        </Modal>
      )}

      {/* detail modal */}
      {detailRun && (
        <Modal title={`${t('deliveries.runDetail')} — RUN-${String(detailRun.runNumber ?? 0).padStart(3, '0')}`} onClose={() => { setDetailRun(null); setManifest(null); }} wide>
          {detailLoading ? <Spinner /> : (
            <>
              <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
                <div><span className="font-medium">{t('common.status')}:</span> <Badge status={detailRun.status} /></div>
                <div><span className="font-medium">{t('deliveries.driver')}:</span> {detailRun.driverName || '—'}</div>
                <div><span className="font-medium">{t('deliveries.vehicle')}:</span> {detailRun.vehiclePlate || '—'}</div>
                <div><span className="font-medium">{t('deliveries.scheduledDate')}:</span> {detailRun.scheduledDate || '—'}</div>
              </div>
              <h3 className="font-semibold text-sm mb-2">{t('deliveries.orders')}</h3>
              <Table
                cols={[t('deliveries.orderId'), t('deliveries.customer'), t('deliveries.address'), t('common.status'), t('common.actions')]}
                rows={runOrders.map(o => [
                  o.orderNumber ? `ORD-${String(o.orderNumber).padStart(4, '0')}` : o.orderId.slice(0, 8),
                  o.customerName || '—',
                  o.address || '—',
                  <Badge key={`os-${o.orderId}`} status={o.status || ''} />,
                  <div key={`oa-${o.orderId}`} className="flex gap-1">
                    <Button variant="danger" size="xs" onClick={() => failDelivery(detailRun!.runId, o.orderId)}>{t('deliveries.fail')}</Button>
                    <Button variant="secondary" size="xs" onClick={() => redelivery(detailRun!.runId, o.orderId)}>{t('deliveries.redeliver')}</Button>
                    <Button variant="secondary" size="xs" onClick={() => waive(detailRun!.runId, o.orderId)}>{t('deliveries.waive')}</Button>
                  </div>,
                ])}
                empty={t('deliveries.noOrders')}
              />
              {manifest && manifest.length > 0 && (
                <>
                  <h3 className="font-semibold text-sm mt-4 mb-2">{t('deliveries.manifest')}</h3>
                  <Table
                    cols={[t('deliveries.orderId'), t('deliveries.customer'), t('deliveries.address'), t('deliveries.items')]}
                    rows={manifest.map(m => [
                      m.orderNumber ? `ORD-${String(m.orderNumber).padStart(4, '0')}` : m.orderId.slice(0, 8),
                      m.customerName || '—',
                      m.address || '—',
                      m.items?.map(i => `${i.productName} x${i.qty}`).join(', ') || '—',
                    ])}
                    empty=""
                  />
                </>
              )}
            </>
          )}
        </Modal>
      )}
    </div>
  );
}
