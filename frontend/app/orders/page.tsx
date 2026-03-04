'use client';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Modal, Spinner, Alert, Badge, Field } from '@/components/ui';

// ─── types ────────────────────────────────────────────────────────────────────

interface Product {
  productId: string;
  name: string;
  departmentId: string;
  departmentName: string;
  baseUom: string;
}

interface OrderLine {
  orderLineId: string;
  productId: string;
  productName: string;
  qty: number;
  uom: string;
  unitPrice: number;
  lineTotal: number;
  leadTimeConflict: boolean;
}

interface Order {
  orderId: string;
  customerName: string;
  status: string;
  requestedDeliveryTime: string;
  orderPlacedAt: string;
  totalAmount: number;
  rushOrder: boolean;
  rushPremiumPct: number | null;
  notes: string | null;
  lines: OrderLine[];
}

type LineForm = {
  productId: string;
  productName: string;
  departmentId: string;
  departmentName: string;
  qty: number;
  uom: string;
  unitPrice: string;
};

// ─── status helpers ───────────────────────────────────────────────────────────

const ALL_STATUSES = ['DRAFT', 'CONFIRMED', 'IN_PRODUCTION', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

const STATUS_NEXT: Record<string, string | null> = {
  CONFIRMED: 'IN_PRODUCTION',
  IN_PRODUCTION: 'READY',
  READY: 'OUT_FOR_DELIVERY',
  OUT_FOR_DELIVERY: 'DELIVERED',
};

const STATUS_NEXT_LABEL: Record<string, string> = {
  CONFIRMED: '▶ Start Production',
  IN_PRODUCTION: '✓ Mark Ready',
  READY: '🚚 Out for Delivery',
  OUT_FOR_DELIVERY: '✅ Mark Delivered',
};

// ─── helpers ──────────────────────────────────────────────────────────────────

function makeDefaultLine(products: Product[]): LineForm {
  const p = products[0];
  return {
    productId: p?.productId ?? '',
    productName: p?.name ?? '',
    departmentId: p?.departmentId ?? '',
    departmentName: p?.departmentName ?? '',
    qty: 10,
    uom: p?.baseUom ?? 'PCS',
    unitPrice: '1.00',
  };
}

function fmtDate(iso: string | null | undefined) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString();
}

function fmtDateTime(iso: string | null | undefined) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// ─── component ────────────────────────────────────────────────────────────────

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // filters
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [customerSearch, setCustomerSearch] = useState('');

  // create modal
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState<{
    customerName: string;
    requestedDeliveryTime: string;
    notes: string;
    forceRush: boolean;
    customRushPremiumPct: string;
    lines: LineForm[];
  }>({
    customerName: '',
    requestedDeliveryTime: '',
    notes: '',
    forceRush: false,
    customRushPremiumPct: '',
    lines: [],
  });

  // row expand
  const [expanded, setExpanded] = useState<string | null>(null);

  // action in-flight
  const [actionId, setActionId] = useState('');

  // cancel reason dialog
  const [cancelTarget, setCancelTarget] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState('');

  // ─── load ────────────────────────────────────────────────────────────────────

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [ords, prods] = await Promise.all([
        apiFetch<Order[]>(`/v1/orders?tenantId=${TENANT_ID}`),
        apiFetch<Product[]>(`/v1/products?tenantId=${TENANT_ID}`),
      ]);
      setOrders(ords);
      setProducts(prods);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // ─── filtered list ────────────────────────────────────────────────────────────

  const filtered = useMemo(() => {
    return orders.filter((o) => {
      if (statusFilter !== 'ALL' && o.status !== statusFilter) return false;
      if (customerSearch && !o.customerName.toLowerCase().includes(customerSearch.toLowerCase())) return false;
      return true;
    });
  }, [orders, statusFilter, customerSearch]);

  // ─── create order ─────────────────────────────────────────────────────────────

  const openForm = () => {
    setForm({
      customerName: '',
      requestedDeliveryTime: '',
      notes: '',
      forceRush: false,
      customRushPremiumPct: '',
      lines: products.length > 0 ? [makeDefaultLine(products)] : [],
    });
    setOpen(true);
  };

  const addLine = () =>
    setForm((f) => ({ ...f, lines: [...f.lines, makeDefaultLine(products)] }));

  const removeLine = (i: number) =>
    setForm((f) => ({ ...f, lines: f.lines.filter((_, j) => j !== i) }));

  const updateLine = (i: number, field: string, value: unknown) => {
    setForm((f) => ({
      ...f,
      lines: f.lines.map((l, j) => {
        if (j !== i) return l;
        if (field === 'productId') {
          const p = products.find((p) => p.productId === value);
          return {
            ...l,
            productId: p?.productId ?? '',
            productName: p?.name ?? '',
            departmentId: p?.departmentId ?? '',
            departmentName: p?.departmentName ?? '',
            uom: p?.baseUom ?? 'PCS',
          };
        }
        return { ...l, [field]: value };
      }),
    }));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch(`/v1/orders?tenantId=${TENANT_ID}`, {
        method: 'POST',
        body: JSON.stringify({
          tenantId: TENANT_ID,
          customerName: form.customerName,
          requestedDeliveryTime: new Date(form.requestedDeliveryTime).toISOString(),
          notes: form.notes || null,
          forceRush: form.forceRush,
          customRushPremiumPct: form.customRushPremiumPct ? parseFloat(form.customRushPremiumPct) : null,
          lines: form.lines.map((l) => ({
            productId: l.productId,
            productName: l.productName,
            departmentId: l.departmentId,
            departmentName: l.departmentName,
            qty: l.qty,
            uom: l.uom,
            unitPrice: parseFloat(l.unitPrice) || 0,
          })),
        }),
      });
      setOpen(false);
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  // ─── actions ──────────────────────────────────────────────────────────────────

  const doConfirm = async (orderId: string) => {
    try {
      setActionId(orderId);
      const updated = await apiFetch<Order>(
        `/v1/orders/${orderId}/confirm?tenantId=${TENANT_ID}`,
        { method: 'POST' }
      );
      setOrders((prev) => prev.map((o) => (o.orderId === orderId ? updated : o)));
    } catch (e) {
      setError(String(e));
    } finally {
      setActionId('');
    }
  };

  const openCancelDialog = (orderId: string) => {
    setCancelTarget(orderId);
    setCancelReason('');
  };

  const doCancel = async () => {
    if (!cancelTarget) return;
    try {
      setActionId(cancelTarget);
      const url = `/v1/orders/${cancelTarget}/cancel?tenantId=${TENANT_ID}${cancelReason ? `&reason=${encodeURIComponent(cancelReason)}` : ''}`;
      const updated = await apiFetch<Order>(url, { method: 'POST' });
      setOrders((prev) => prev.map((o) => (o.orderId === cancelTarget ? updated : o)));
      setCancelTarget(null);
    } catch (e) {
      setError(String(e));
    } finally {
      setActionId('');
    }
  };

  const doAdvanceStatus = async (orderId: string, targetStatus: string) => {
    try {
      setActionId(orderId);
      const updated = await apiFetch<Order>(
        `/v1/orders/${orderId}/status?tenantId=${TENANT_ID}&targetStatus=${targetStatus}`,
        { method: 'POST' }
      );
      setOrders((prev) => prev.map((o) => (o.orderId === orderId ? updated : o)));
    } catch (e) {
      setError(String(e));
    } finally {
      setActionId('');
    }
  };

  // ─── render ───────────────────────────────────────────────────────────────────

  return (
    <div className="max-w-5xl">
      {/* header */}
      <div className="flex items-center justify-between mb-5">
        <h1 className="text-2xl font-semibold">Orders</h1>
        <button className="btn-primary" onClick={openForm}>
          + New Order
        </button>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {/* filters */}
      <div className="flex flex-wrap gap-3 mb-4">
        <select
          className="input w-44"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All Statuses</option>
          {ALL_STATUSES.map((s) => (
            <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
          ))}
        </select>
        <input
          className="input w-56"
          placeholder="Search customer…"
          value={customerSearch}
          onChange={(e) => setCustomerSearch(e.target.value)}
        />
        <span className="text-sm text-gray-400 self-center ml-auto">
          {filtered.length} order{filtered.length !== 1 ? 's' : ''}
        </span>
      </div>

      {/* list */}
      {loading ? (
        <Spinner />
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 text-sm text-gray-400 border rounded-xl bg-white">
          {orders.length === 0 ? 'No orders yet. Create the first one.' : 'No orders match the current filters.'}
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map((o) => (
            <div key={o.orderId} className="border rounded-xl bg-white shadow-sm">
              {/* row header */}
              <div
                className="flex items-center gap-3 px-4 py-3 cursor-pointer hover:bg-gray-50 select-none"
                onClick={() => setExpanded(expanded === o.orderId ? null : o.orderId)}
              >
                {/* customer */}
                <span className="font-medium text-sm flex-1 min-w-0 truncate">
                  {o.customerName}
                </span>
                {/* delivery date */}
                <span className="text-xs text-gray-500 shrink-0 hidden sm:inline">
                  📅 {fmtDate(o.requestedDeliveryTime)}
                </span>
                {/* line count */}
                <span className="text-xs text-gray-400 shrink-0">
                  {o.lines?.length ?? 0} line{(o.lines?.length ?? 0) !== 1 ? 's' : ''}
                </span>
                {/* total */}
                <span className="text-sm font-semibold shrink-0">
                  {(o.totalAmount ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
                {/* rush badge */}
                {o.rushOrder && (
                  <span className="bg-orange-100 text-orange-700 text-xs font-medium px-2 py-0.5 rounded-full shrink-0">
                    ⚡ RUSH
                    {o.rushPremiumPct ? ` +${o.rushPremiumPct}%` : ''}
                  </span>
                )}
                {/* status */}
                <Badge status={o.status} />
                {/* action buttons */}
                <div
                  className="flex gap-1 shrink-0"
                  onClick={(e) => e.stopPropagation()}
                >
                  {/* DRAFT → Confirm */}
                  {o.status === 'DRAFT' && (
                    <button
                      className="btn-xs bg-blue-600 text-white hover:bg-blue-700"
                      disabled={actionId === o.orderId}
                      onClick={() => doConfirm(o.orderId)}
                    >
                      Confirm
                    </button>
                  )}
                  {/* Status advance */}
                  {STATUS_NEXT[o.status] && (
                    <button
                      className="btn-xs bg-indigo-600 text-white hover:bg-indigo-700"
                      disabled={actionId === o.orderId}
                      onClick={() => doAdvanceStatus(o.orderId, STATUS_NEXT[o.status]!)}
                    >
                      {STATUS_NEXT_LABEL[o.status]}
                    </button>
                  )}
                  {/* Cancel */}
                  {(o.status === 'DRAFT' || o.status === 'CONFIRMED') && (
                    <button
                      className="btn-xs bg-red-50 text-red-600 border border-red-200 hover:bg-red-100"
                      disabled={actionId === o.orderId}
                      onClick={() => openCancelDialog(o.orderId)}
                    >
                      Cancel
                    </button>
                  )}
                </div>
                <span className="text-gray-400 text-xs shrink-0">
                  {expanded === o.orderId ? '▲' : '▼'}
                </span>
              </div>

              {/* expanded detail */}
              {expanded === o.orderId && (
                <div className="border-t px-4 py-3 space-y-3">
                  {/* meta row */}
                  <div className="flex flex-wrap gap-4 text-xs text-gray-500">
                    <span><span className="font-medium text-gray-700">Order ID:</span> {o.orderId.slice(0, 8)}…</span>
                    <span><span className="font-medium text-gray-700">Placed:</span> {fmtDateTime(o.orderPlacedAt)}</span>
                    <span><span className="font-medium text-gray-700">Delivery:</span> {fmtDateTime(o.requestedDeliveryTime)}</span>
                    {o.notes && <span><span className="font-medium text-gray-700">Notes:</span> {o.notes}</span>}
                  </div>
                  {/* lines table */}
                  {o.lines && o.lines.length > 0 && (
                    <table className="w-full text-xs">
                      <thead>
                        <tr className="text-gray-500 bg-gray-50">
                          <th className="text-left px-2 py-1.5 rounded-l">Product</th>
                          <th className="text-right px-2 py-1.5">Qty</th>
                          <th className="text-right px-2 py-1.5">Unit Price</th>
                          <th className="text-right px-2 py-1.5">Line Total</th>
                          <th className="text-center px-2 py-1.5 rounded-r">Lead Time</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100">
                        {o.lines.map((ln) => (
                          <tr key={ln.orderLineId}>
                            <td className="px-2 py-1.5 font-medium">{ln.productName}</td>
                            <td className="px-2 py-1.5 text-right">{ln.qty} {ln.uom}</td>
                            <td className="px-2 py-1.5 text-right">
                              {(ln.unitPrice ?? 0).toFixed(2)}
                            </td>
                            <td className="px-2 py-1.5 text-right font-medium">
                              {(ln.lineTotal ?? 0).toFixed(2)}
                            </td>
                            <td className="px-2 py-1.5 text-center">
                              {ln.leadTimeConflict
                                ? <span className="text-orange-600 font-medium">⚠ Conflict</span>
                                : <span className="text-green-600">✓ OK</span>}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                      <tfoot>
                        <tr className="border-t">
                          <td colSpan={3} className="px-2 py-1.5 text-right text-gray-500 font-medium">Total</td>
                          <td className="px-2 py-1.5 text-right font-bold">
                            {(o.totalAmount ?? 0).toFixed(2)}
                          </td>
                          <td />
                        </tr>
                      </tfoot>
                    </table>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* ─── Create Order Modal ─────────────────────────────────────────────── */}
      {open && (
        <Modal title="New Order" onClose={() => setOpen(false)} wide>
          <form onSubmit={submit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Customer Name">
                <input
                  className="input"
                  required
                  placeholder="e.g. Supermarket A"
                  value={form.customerName}
                  onChange={(e) => setForm((f) => ({ ...f, customerName: e.target.value }))}
                />
              </Field>
              <Field label="Requested Delivery">
                <input
                  className="input"
                  type="datetime-local"
                  required
                  value={form.requestedDeliveryTime}
                  onChange={(e) => setForm((f) => ({ ...f, requestedDeliveryTime: e.target.value }))}
                />
              </Field>
            </div>

            <Field label="Notes">
              <input
                className="input"
                placeholder="Optional internal notes"
                value={form.notes}
                onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
              />
            </Field>

            {/* rush toggle */}
            <div className="flex items-center gap-4 bg-orange-50 border border-orange-100 rounded-lg px-4 py-3">
              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  className="w-4 h-4 accent-orange-500"
                  checked={form.forceRush}
                  onChange={(e) => setForm((f) => ({ ...f, forceRush: e.target.checked }))}
                />
                <span className="text-sm font-medium text-orange-800">⚡ Rush Order</span>
              </label>
              {form.forceRush && (
                <div className="flex items-center gap-2">
                  <label className="text-xs text-orange-700">Custom premium %</label>
                  <input
                    className="input w-24 text-sm"
                    type="number"
                    min={0}
                    step="0.1"
                    placeholder="e.g. 15"
                    value={form.customRushPremiumPct}
                    onChange={(e) => setForm((f) => ({ ...f, customRushPremiumPct: e.target.value }))}
                  />
                  <span className="text-xs text-orange-600">Leave blank to use system default</span>
                </div>
              )}
            </div>

            {/* lines */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-medium text-gray-700">Order Lines</span>
                <button type="button" className="text-xs text-blue-600 hover:underline" onClick={addLine}>
                  + Add line
                </button>
              </div>
              {form.lines.length === 0 && (
                <p className="text-xs text-gray-400 py-2">
                  No products available. Add products first.
                </p>
              )}
              <div className="space-y-2">
                {form.lines.map((line, i) => (
                  <div key={i} className="border rounded-lg p-3 bg-gray-50">
                    <div className="grid grid-cols-4 gap-2 items-end">
                      <div className="col-span-2">
                        <label className="text-xs text-gray-500">Product</label>
                        <select
                          className="input"
                          value={line.productId}
                          onChange={(e) => updateLine(i, 'productId', e.target.value)}
                        >
                          {products.map((p) => (
                            <option key={p.productId} value={p.productId}>{p.name}</option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">Qty ({line.uom})</label>
                        <input
                          className="input"
                          type="number"
                          min={1}
                          value={line.qty}
                          onChange={(e) => updateLine(i, 'qty', +e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">Unit Price</label>
                        <input
                          className="input"
                          type="number"
                          min={0}
                          step="0.01"
                          value={line.unitPrice}
                          onChange={(e) => updateLine(i, 'unitPrice', e.target.value)}
                        />
                      </div>
                    </div>
                    {form.lines.length > 1 && (
                      <button
                        type="button"
                        className="mt-1 text-xs text-red-500 hover:underline"
                        onClick={() => removeLine(i)}
                      >
                        Remove line
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <button type="button" className="btn-secondary" onClick={() => setOpen(false)}>
                Discard
              </button>
              <button
                type="submit"
                className="btn-primary"
                disabled={saving || form.lines.length === 0}
              >
                {saving ? 'Saving…' : 'Create Order'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ─── Cancel Reason Dialog ───────────────────────────────────────────── */}
      {cancelTarget && (
        <Modal title="Cancel Order" onClose={() => setCancelTarget(null)}>
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Cancelling this order cannot be undone. Optionally provide a reason.
            </p>
            <Field label="Reason (optional)">
              <textarea
                className="input h-20 resize-none"
                placeholder="e.g. Customer withdrew the order"
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
              />
            </Field>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button className="btn-secondary" onClick={() => setCancelTarget(null)}>
                Go Back
              </button>
              <button
                className="bg-red-600 hover:bg-red-700 text-white text-sm font-medium px-4 py-1.5 rounded-lg disabled:opacity-50"
                disabled={!!actionId}
                onClick={doCancel}
              >
                {actionId ? 'Cancelling…' : 'Yes, Cancel Order'}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
