'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT, useDateFmt } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';

/* ── types ─────────────────────────────────────────────── */
interface Customer {
  customerId: string;
  tenantId: string;
  name: string;
  email?: string;
  phone?: string;
  tier?: string;
  active?: boolean;
  createdAt?: string;
}
interface CatalogProduct {
  productId: string;
  name: string;
  description?: string;
  price?: number;
  currency?: string;
  unit?: string;
  category?: string;
}
interface CustomerOrder {
  orderId: string;
  orderNumber?: number;
  customerId: string;
  status: string;
  totalAmount?: number;
  currency?: string;
  items?: { productId: string; productName: string; qty: number; unitPrice: number }[];
  createdAt?: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function CustomersPage() {
  const t = useT();
  const fmtDate = useDateFmt();

  const [tab, setTab] = useState<'customers' | 'catalog' | 'orders'>('customers');

  /* customers */
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showRegister, setShowRegister] = useState(false);
  const [regForm, setRegForm] = useState({ name: '', email: '', phone: '' });
  const [saving, setSaving] = useState(false);

  /* catalog */
  const [catalog, setCatalog] = useState<CatalogProduct[]>([]);
  const [catLoading, setCatLoading] = useState(false);

  /* orders */
  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [showCreateOrder, setShowCreateOrder] = useState(false);
  const [orderForm, setOrderForm] = useState({ customerId: '' });
  const [orderLines, setOrderLines] = useState<{ productId: string; productName: string; qty: string }[]>([]);
  const [orderSaving, setOrderSaving] = useState(false);
  const [orderDetail, setOrderDetail] = useState<CustomerOrder | null>(null);

  /* loaders */
  const loadCustomers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await apiFetch<Customer[]>(`/v2/customers?tenantId=${TENANT_ID}`);
      setCustomers(data);
    } catch (e) { setError(String(e)); } finally { setLoading(false); }
  }, []);

  const loadCatalog = useCallback(async () => {
    try {
      setCatLoading(true);
      const data = await apiFetch<{ content: CatalogProduct[] }>(`/v2/products?tenantId=${TENANT_ID}`);
      setCatalog(data.content ?? []);
    } catch (e) { setError(String(e)); } finally { setCatLoading(false); }
  }, []);

  const loadOrders = useCallback(async () => {
    try {
      setOrdersLoading(true);
      const data = await apiFetch<CustomerOrder[]>(`/v1/orders?tenantId=${TENANT_ID}`);
      setOrders(data);
    } catch (e) { setError(String(e)); } finally { setOrdersLoading(false); }
  }, []);

  useEffect(() => { loadCustomers(); }, [loadCustomers]);
  useEffect(() => { if (tab === 'catalog') loadCatalog(); }, [tab, loadCatalog]);
  useEffect(() => { if (tab === 'orders') loadOrders(); }, [tab, loadOrders]);

  /* register customer */
  const registerCustomer = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v2/customers/register', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID, ...regForm }) });
      setSuccess(t('customers.registered'));
      setShowRegister(false);
      setRegForm({ name: '', email: '', phone: '' });
      loadCustomers();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  /* create order */
  const addLine = () => setOrderLines([...orderLines, { productId: '', productName: '', qty: '1' }]);
  const removeLine = (i: number) => setOrderLines(orderLines.filter((_, idx) => idx !== i));
  const updateLine = (i: number, field: string, val: string) => setOrderLines(orderLines.map((l, idx) => idx === i ? { ...l, [field]: val } : l));

  const createOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setOrderSaving(true);
      await apiFetch('/v2/orders', {
        method: 'POST',
        body: JSON.stringify({
          tenantId: TENANT_ID,
          customerId: orderForm.customerId,
          items: orderLines.map(l => ({ productId: l.productId, qty: Number(l.qty) })),
        }),
      });
      setSuccess(t('customers.orderCreated'));
      setShowCreateOrder(false);
      setOrderForm({ customerId: '' });
      setOrderLines([]);
      loadOrders();
    } catch (e) { setError(String(e)); } finally { setOrderSaving(false); }
  };

  /* order detail */
  const openOrderDetail = async (id: string) => {
    try {
      const data = await apiFetch<CustomerOrder>(`/v2/orders/${id}?tenantId=${TENANT_ID}`);
      setOrderDetail(data);
    } catch (e) { setError(String(e)); }
  };

  return (
    <div className="max-w-[1800px]">
      <SectionTitle eyebrow="CRM" title={t('customers.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {(['customers', 'catalog', 'orders'] as const).map(k => (
          <button key={k} onClick={() => setTab(k)}
            className={`px-4 py-2 text-sm font-medium -mb-px ${tab === k ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {t(`customers.tab_${k}`)}
          </button>
        ))}
      </div>

      {/* ───────── CUSTOMERS ───────── */}
      {tab === 'customers' && (
        <>
          <div className="flex justify-end mb-4">
            <Button variant="primary" size="sm" onClick={() => setShowRegister(true)}>+ {t('customers.register')}</Button>
          </div>
          {loading ? <Spinner /> : (
            <Table
              cols={[t('common.name'), t('customers.email'), t('customers.phone'), t('loyalty.tier'), t('common.date')]}
              rows={customers.map(c => [
                c.name,
                c.email || '—',
                c.phone || '—',
                c.tier ? <Badge key={c.customerId} status={c.tier} /> : '—',
                fmtDate(c.createdAt),
              ])}
              empty={t('customers.empty')}
            />
          )}
        </>
      )}

      {/* ───────── CATALOG ───────── */}
      {tab === 'catalog' && (
        <>
          {catLoading ? <Spinner /> : (
            <Table
              cols={[t('common.name'), t('customers.description'), t('customers.price'), t('customers.unit'), t('customers.category')]}
              rows={catalog.map(p => [
                p.name,
                p.description || '—',
                p.price !== null && p.price !== undefined ? `${p.price.toFixed(2)} ${p.currency || ''}` : '—',
                p.unit || '—',
                p.category || '—',
              ])}
              empty={t('customers.catalogEmpty')}
            />
          )}
        </>
      )}

      {/* ───────── ORDERS ───────── */}
      {tab === 'orders' && (
        <>
          <div className="flex justify-end mb-4">
            <Button variant="primary" size="sm" onClick={() => { setShowCreateOrder(true); addLine(); }}>+ {t('customers.createOrder')}</Button>
          </div>
          {ordersLoading ? <Spinner /> : (
            <Table
              cols={[t('customers.orderId'), t('customers.customer'), t('common.status'), t('customers.total'), t('common.date')]}
              rows={orders.map(o => [
                <button key={o.orderId} className="text-blue-600 underline text-sm" onClick={() => openOrderDetail(o.orderId)}>{o.orderNumber ? `ORD-${String(o.orderNumber).padStart(4, '0')}` : o.orderId.slice(0, 8)}</button>,
                o.customerId.slice(0, 8),
                <Badge key={`s-${o.orderId}`} status={o.status} />,
                o.totalAmount !== null && o.totalAmount !== undefined ? `${o.totalAmount.toFixed(2)} ${o.currency || ''}` : '—',
                fmtDate(o.createdAt),
              ])}
              empty={t('customers.noOrders')}
            />
          )}
        </>
      )}

      {/* register modal */}
      {showRegister && (
        <Modal title={t('customers.register')} onClose={() => setShowRegister(false)}>
          <form onSubmit={registerCustomer} className="space-y-4">
            <Field label={t('common.name')}><input className="input w-full" required value={regForm.name} onChange={e => setRegForm({ ...regForm, name: e.target.value })} /></Field>
            <Field label={t('customers.email')}><input className="input w-full" type="email" value={regForm.email} onChange={e => setRegForm({ ...regForm, email: e.target.value })} /></Field>
            <Field label={t('customers.phone')}><input className="input w-full" value={regForm.phone} onChange={e => setRegForm({ ...regForm, phone: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowRegister(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{saving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* create order modal */}
      {showCreateOrder && (
        <Modal title={t('customers.createOrder')} onClose={() => { setShowCreateOrder(false); setOrderLines([]); }} wide>
          <form onSubmit={createOrder} className="space-y-4">
            <Field label={t('customers.customer')}>
              <select className="input w-full" required value={orderForm.customerId} onChange={e => setOrderForm({ ...orderForm, customerId: e.target.value })}>
                <option value="">— {t('common.select')} —</option>
                {customers.map(c => <option key={c.customerId} value={c.customerId}>{c.name}</option>)}
              </select>
            </Field>
            <h3 className="font-semibold text-sm">{t('customers.orderItems')}</h3>
            {orderLines.map((line, i) => (
              <div key={i} className="grid grid-cols-4 gap-2 items-end">
                <Field label={t('customers.product')}>
                  <select className="input w-full" required value={line.productId} onChange={e => updateLine(i, 'productId', e.target.value)}>
                    <option value="">—</option>
                    {catalog.map(p => <option key={p.productId} value={p.productId}>{p.name}</option>)}
                  </select>
                </Field>
                <Field label={t('suppliers.qty')}><input className="input w-full" type="number" min="1" required value={line.qty} onChange={e => updateLine(i, 'qty', e.target.value)} /></Field>
                <div />
                <Button variant="danger" size="xs" onClick={() => removeLine(i)}>✕</Button>
              </div>
            ))}
            <Button variant="secondary" size="sm" onClick={addLine}>+ {t('customers.addItem')}</Button>
            <div className="flex justify-end gap-2 pt-4">
              <Button variant="secondary" size="sm" onClick={() => { setShowCreateOrder(false); setOrderLines([]); }}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={orderSaving}>{orderSaving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* order detail modal */}
      {orderDetail && (
        <Modal title={`${t('customers.orderDetail')} \u2014 ${orderDetail.orderNumber ? `ORD-${String(orderDetail.orderNumber).padStart(4, '0')}` : orderDetail.orderId.slice(0, 8)}`} onClose={() => setOrderDetail(null)} wide>
          <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
            <div><span className="font-medium">{t('common.status')}:</span> <Badge status={orderDetail.status} /></div>
            <div><span className="font-medium">{t('customers.total')}:</span> {orderDetail.totalAmount?.toFixed(2)} {orderDetail.currency}</div>
          </div>
          {orderDetail.items && (
            <Table
              cols={[t('customers.product'), t('suppliers.qty'), t('suppliers.unitPrice')]}
              rows={orderDetail.items.map(it => [it.productName || it.productId, it.qty, it.unitPrice.toFixed(2)])}
              empty=""
            />
          )}
        </Modal>
      )}
    </div>
  );
}
