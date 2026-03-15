'use client';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Modal, Alert, Badge, Field, PageSkeleton, Pagination, useToast } from '@/components/ui';
import { SectionTitle, Button, InputField, SelectField } from '@/components/design-system';
import { useT, useDateFmt, useDateTimeFmt } from '@/lib/i18n';
import { Plus, Calendar, ChevronUp, ChevronDown } from 'lucide-react';

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

const STATUS_NEXT_KEY: Record<string, string> = {
  CONFIRMED: 'orders.startProduction',
  IN_PRODUCTION: 'orders.markReady',
  READY: 'orders.outForDelivery',
  OUT_FOR_DELIVERY: 'orders.markDelivered',
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

// ─── component ────────────────────────────────────────────────────────────────

export default function OrdersPage() {
  const t = useT();
  const fmtDate = useDateFmt();
  const fmtDateTime = useDateTimeFmt();
  const { toastSuccess, toastError } = useToast();
  const [orders, setOrders] = useState<Order[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const PAGE_SIZE = 20;

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

  // edit mode
  const [editTarget, setEditTarget] = useState<string | null>(null);

  // cancel reason dialog
  const [cancelTarget, setCancelTarget] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState('');

  // ─── load ────────────────────────────────────────────────────────────────────

  const load = useCallback(async (p = page) => {
    try {
      setLoading(true);
      setError('');
      const [pagedResult, prods] = await Promise.all([
        apiFetch<{ content: Order[]; totalPages: number; totalElements: number }>(
          `/v1/orders/paged?tenantId=${TENANT_ID}&page=${p}&size=${PAGE_SIZE}`
        ),
        apiFetch<Product[]>(`/v1/products?tenantId=${TENANT_ID}`),
      ]);
      setOrders(pagedResult.content);
      setTotalPages(pagedResult.totalPages);
      setProducts(prods);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { load(); }, [load]);

  const changePage = (newPage: number) => {
    setPage(newPage);
    load(newPage);
  };

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
    setEditTarget(null);
    setOpen(true);
  };

  const openEdit = (o: Order) => {
    setForm({
      customerName: o.customerName,
      requestedDeliveryTime: o.requestedDeliveryTime ? o.requestedDeliveryTime.slice(0, 16) : '',
      notes: o.notes || '',
      forceRush: o.rushOrder,
      customRushPremiumPct: o.rushPremiumPct ? String(o.rushPremiumPct) : '',
      lines: o.lines.map((l) => ({
        productId: l.productId,
        productName: l.productName,
        departmentId: '',
        departmentName: '',
        qty: l.qty,
        uom: l.uom,
        unitPrice: String(l.unitPrice ?? 0),
      })),
    });
    setEditTarget(o.orderId);
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
      const payload = {
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
      };
      if (editTarget) {
        const updated = await apiFetch<Order>(`/v1/orders/${editTarget}?tenantId=${TENANT_ID}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        });
        setOrders((prev) => prev.map((o) => (o.orderId === editTarget ? updated : o)));
        toastSuccess(t('orders.orderUpdated'));
      } else {
        await apiFetch(`/v1/orders?tenantId=${TENANT_ID}`, {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        load();
        toastSuccess(t('orders.orderCreated'));
      }
      setOpen(false);
      setEditTarget(null);
    } catch (e) {
      toastError(String(e));
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
    <div className="max-w-[1800px]">
      {/* header */}
      <div className="mb-5">
        <SectionTitle
          eyebrow="Operations"
          title={t('orders.title')}
          subtitle={t('orders.orderCount', { count: filtered.length })}
          action={<Button variant="primary" size="sm" onClick={openForm}><Plus className="h-4 w-4" /> {t('orders.newOrder')}</Button>}
        />
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {/* filters */}
      <div className="flex flex-wrap gap-3 mb-4">
        <SelectField
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="w-44"
          options={[
            { value: 'ALL', label: t('common.allStatuses') },
            ...ALL_STATUSES.map((s) => ({ value: s, label: s.replace(/_/g, ' ') })),
          ]}
        />
        <InputField
          className="w-56"
          placeholder={t('orders.searchCustomer')}
          value={customerSearch}
          onChange={(e) => setCustomerSearch(e.target.value)}
        />
      </div>

      {/* list */}
      {loading ? (
        <PageSkeleton />
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 text-sm text-gray-400 rounded-2xl border border-gray-200 bg-white">
          {orders.length === 0 ? t('orders.noOrders') : t('orders.noMatch')}
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map((o) => (
            <div key={o.orderId} className="rounded-2xl border border-gray-200 bg-white shadow-sm">
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
                  <Calendar className="inline h-3.5 w-3.5 mr-0.5" /> {fmtDate(o.requestedDeliveryTime)}
                </span>
                {/* line count */}
                <span className="text-xs text-gray-400 shrink-0">
                  {t('orders.lines', { count: o.lines?.length ?? 0 })}
                </span>
                {/* total */}
                <span className="text-sm font-semibold shrink-0">
                  {(o.totalAmount ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
                {/* rush badge */}
                {o.rushOrder && (
                  <span className="bg-orange-100 text-orange-700 text-xs font-medium px-2 py-0.5 rounded-full shrink-0">
                    {t('orders.rush')}
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
                  {/* DRAFT → Edit */}
                  {o.status === 'DRAFT' && (
                    <Button variant="secondary" size="xs" disabled={actionId === o.orderId} onClick={() => openEdit(o)}>
                      {t('orders.editBtn')}
                    </Button>
                  )}
                  {/* DRAFT → Confirm */}
                  {o.status === 'DRAFT' && (
                    <Button variant="primary" size="xs" disabled={actionId === o.orderId} onClick={() => doConfirm(o.orderId)}>
                      {t('orders.confirmBtn')}
                    </Button>
                  )}
                  {/* Status advance */}
                  {STATUS_NEXT[o.status] && (
                    <Button variant="primary" size="xs" disabled={actionId === o.orderId} onClick={() => doAdvanceStatus(o.orderId, STATUS_NEXT[o.status]!)}>
                      {t(STATUS_NEXT_KEY[o.status])}
                    </Button>
                  )}
                  {/* Cancel */}
                  {(o.status === 'DRAFT' || o.status === 'CONFIRMED') && (
                    <Button variant="danger" size="xs" disabled={actionId === o.orderId} onClick={() => openCancelDialog(o.orderId)}>
                      {t('orders.cancelBtn')}
                    </Button>
                  )}
                </div>
                <span className="text-gray-400 text-xs shrink-0">
                  {expanded === o.orderId ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                </span>
              </div>

              {/* expanded detail */}
              {expanded === o.orderId && (
                <div className="border-t px-4 py-3 space-y-3">
                  {/* meta row */}
                  <div className="flex flex-wrap gap-4 text-xs text-gray-500">
                    <span><span className="font-medium text-gray-700">{t('orders.orderId')}:</span> {o.orderId.slice(0, 8)}…</span>
                    <span><span className="font-medium text-gray-700">{t('orders.placed')}:</span> {fmtDateTime(o.orderPlacedAt)}</span>
                    <span><span className="font-medium text-gray-700">{t('orders.delivery')}:</span> {fmtDateTime(o.requestedDeliveryTime)}</span>
                    {o.notes && <span><span className="font-medium text-gray-700">{t('common.notes')}:</span> {o.notes}</span>}
                  </div>
                  {/* status flow */}
                  <div className="flex items-center gap-1 flex-wrap">
                    {ALL_STATUSES.filter((s) => s !== 'CANCELLED').map((s, i) => {
                      const reached = ALL_STATUSES.indexOf(o.status) >= ALL_STATUSES.indexOf(s);
                      const current = o.status === s;
                      return (
                        <span key={s} className="flex items-center gap-1">
                          {i > 0 && <span className={`w-4 h-px ${reached ? 'bg-blue-400' : 'bg-gray-200'}`} />}
                          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${current ? 'bg-blue-600 text-white' : reached ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-400'}`}>
                            {s.replace(/_/g, ' ')}
                          </span>
                        </span>
                      );
                    })}
                    {o.status === 'CANCELLED' && (
                      <span className="ml-2 px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-700">CANCELLED</span>
                    )}
                  </div>
                  {/* lines table */}
                  {o.lines && o.lines.length > 0 && (
                    <table className="w-full text-xs">
                      <thead>
                        <tr className="text-gray-500 bg-gray-50">
                          <th className="text-left px-2 py-1.5 rounded-l">{t('orders.product')}</th>
                          <th className="text-right px-2 py-1.5">{t('orders.qty')}</th>
                          <th className="text-right px-2 py-1.5">{t('orders.unitPrice')}</th>
                          <th className="text-right px-2 py-1.5">{t('orders.lineTotal')}</th>
                          <th className="text-center px-2 py-1.5 rounded-r">{t('orders.leadTime')}</th>
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
                                ? <span className="text-orange-600 font-medium">{t('orders.conflict')}</span>
                                : <span className="text-green-600">{t('orders.ltOk')}</span>}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                      <tfoot>
                        <tr className="border-t">
                          <td colSpan={3} className="px-2 py-1.5 text-right text-gray-500 font-medium">{t('common.total')}</td>
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

      {!loading && <Pagination page={page} totalPages={totalPages} onPageChange={changePage} />}

      {/* ─── Create / Edit Order Modal ──────────────────────────────────────── */}
      {open && (
        <Modal title={editTarget ? t('orders.editOrderTitle') : t('orders.newOrderTitle')} onClose={() => { setOpen(false); setEditTarget(null); }} wide>
          <form onSubmit={submit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('orders.customerName')}>
                <input
                  className="input"
                  required
                  placeholder={t('orders.customerPlaceholder')}
                  value={form.customerName}
                  onChange={(e) => setForm((f) => ({ ...f, customerName: e.target.value }))}
                />
              </Field>
              <Field label={t('orders.requestedDelivery')}>
                <input
                  className="input"
                  type="datetime-local"
                  required
                  value={form.requestedDeliveryTime}
                  onChange={(e) => setForm((f) => ({ ...f, requestedDeliveryTime: e.target.value }))}
                />
              </Field>
            </div>

            <Field label={t('common.notes')}>
              <input
                className="input"
                placeholder={t('orders.notesPlaceholder')}
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
                <span className="text-sm font-medium text-orange-800">⚡ {t('orders.rushOrder')}</span>
              </label>
              {form.forceRush && (
                <div className="flex items-center gap-2">
                  <label className="text-xs text-orange-700">{t('orders.rushPremium')}</label>
                  <input
                    className="input w-24 text-sm"
                    type="number"
                    min={0}
                    step="0.1"
                    placeholder="e.g. 15"
                    value={form.customRushPremiumPct}
                    onChange={(e) => setForm((f) => ({ ...f, customRushPremiumPct: e.target.value }))}
                  />
                  <span className="text-xs text-orange-600">{t('orders.rushPremiumHint')}</span>
                </div>
              )}
            </div>

            {/* lines */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-medium text-gray-700">{t('orders.orderLines')}</span>
                <button type="button" className="text-xs text-blue-600 hover:underline" onClick={addLine}>
                  {t('orders.addLine')}
                </button>
              </div>
              {form.lines.length === 0 && (
                <p className="text-xs text-gray-400 py-2">
                  {t('orders.noProducts')}
                </p>
              )}
              <div className="space-y-2">
                {form.lines.map((line, i) => (
                  <div key={i} className="border rounded-lg p-3 bg-gray-50">
                    <div className="grid grid-cols-4 gap-2 items-end">
                      <div className="col-span-2">
                        <label className="text-xs text-gray-500">{t('orders.product')}</label>
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
                        <label className="text-xs text-gray-500">{t('orders.qty')} ({line.uom})</label>
                        <input
                          className="input"
                          type="number"
                          min={1}
                          value={line.qty}
                          onChange={(e) => updateLine(i, 'qty', +e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">{t('orders.unitPrice')}</label>
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
                        {t('orders.removeLine')}
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="secondary" size="sm" type="button" onClick={() => { setOpen(false); setEditTarget(null); }}>
                {t('common.cancel')}
              </Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving || form.lines.length === 0}>
                {saving ? t('common.saving') : editTarget ? t('common.save') : t('orders.newOrder')}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* ─── Cancel Reason Dialog ───────────────────────────────────────────── */}
      {cancelTarget && (
        <Modal title={t('orders.cancelOrder')} onClose={() => setCancelTarget(null)}>
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              {t('orders.cancelWarning')}
            </p>
            <Field label={t('orders.cancelReason')}>
              <textarea
                className="input h-20 resize-none"
                placeholder={t('orders.cancelReasonPlaceholder')}
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
              />
            </Field>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="secondary" size="sm" onClick={() => setCancelTarget(null)}>
                {t('common.cancel')}
              </Button>
              <Button variant="danger" size="sm" disabled={!!actionId} onClick={doCancel}>
                {actionId ? t('common.saving') : t('orders.confirmCancel')}
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
