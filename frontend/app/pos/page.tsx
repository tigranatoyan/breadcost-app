'use client';
import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Spinner, Alert } from '@/components/ui';
import { useT } from '@/lib/i18n';
import { getUsername } from '@/lib/auth';
import { Package } from 'lucide-react';

// ─── types ────────────────────────────────────────────────────────────────────

interface Product {
  productId: string;
  name: string;
  departmentId: string;
  departmentName?: string;
  baseUom: string;
  saleUnit: string;
  status: string;
}

interface Department {
  departmentId: string;
  name: string;
}

interface CartLine {
  productId: string;
  productName: string;
  departmentId: string;
  departmentName: string;
  qty: number;
  uom: string;
  unitPrice: number;
}

interface SaleLine {
  productName: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  lineTotal: number;
}

interface SaleResult {
  saleId: string;
  cashierName: string;
  paymentMethod: 'CASH' | 'CARD';
  totalAmount: number;
  cashReceived: number | null;
  changeGiven: number | null;
  cardReference: string | null;
  completedAt: string;
  lines: SaleLine[];
}

interface ReconcileResult {
  date: string;
  totalTransactions: number;
  cashTotal: number;
  cardTotal: number;
  refunds: number;
  netSales: number;
  expectedCashInDrawer: number;
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function fmt(n: number) {
  return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function lineTotal(line: CartLine) {
  return line.qty * line.unitPrice;
}

// ─── quick-add popover ───────────────────────────────────────────────────────

interface QuickAddProps {
  product: Product;
  deptName: string;
  onAdd: (line: Omit<CartLine, 'productName' | 'departmentName'> & { productName: string; departmentName: string }) => void;
  onCancel: () => void;
}

function QuickAdd({ product, deptName, onAdd, onCancel }: QuickAddProps) {
  const t = useT();
  const [qty, setQty] = useState('1');
  const [price, setPrice] = useState('0.00');

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const q = parseFloat(qty);
    const p = parseFloat(price);
    if (!q || q <= 0) return;
    onAdd({
      productId: product.productId,
      productName: product.name,
      departmentId: product.departmentId,
      departmentName: deptName,
      qty: q,
      uom: product.baseUom,
      unitPrice: p,
    });
  };

  return (
    <div className="absolute inset-0 z-10 bg-white/95 backdrop-blur-sm rounded-xl border border-indigo-300 shadow-lg p-4 flex flex-col">
      <div className="font-semibold text-sm mb-3 truncate">{product.name}</div>
      <form onSubmit={submit} className="flex flex-col gap-2 flex-1">
        <div>
          <label className="text-xs text-gray-500 mb-1 block">{t('orders.qty')} ({product.baseUom})</label>
          <input
            autoFocus
            className="input w-full text-center text-lg font-bold"
            type="number"
            min={0.001}
            step="0.001"
            value={qty}
            onChange={(e) => setQty(e.target.value)}
          />
        </div>
        <div>
          <label className="text-xs text-gray-500 mb-1 block">{t('pos.unitPrice')}</label>
          <input
            className="input w-full text-center"
            type="number"
            min={0}
            step="0.01"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
          />
        </div>
        <div className="text-xs text-right text-gray-500 mt-0.5">
          {t('common.total')}: {fmt(parseFloat(qty || '0') * parseFloat(price || '0'))}
        </div>
        <div className="flex gap-2 mt-auto pt-2">
          <button type="button" className="flex-1 btn-secondary text-xs py-1.5" onClick={onCancel}>{t('common.cancel')}</button>
          <button type="submit" className="flex-1 btn-primary text-xs py-1.5">{t('pos.addToCart')}</button>
        </div>
      </form>
    </div>
  );
}

// ─── main component ───────────────────────────────────────────────────────────

export default function POSPage() {
  const t = useT();
  const [products, setProducts] = useState<Product[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // filters
  const [deptFilter, setDeptFilter] = useState('ALL');
  const [search, setSearch] = useState('');

  // adding to cart
  const [quickAddId, setQuickAddId] = useState<string | null>(null);

  // cart
  const [cart, setCart] = useState<CartLine[]>([]);

  // checkout form
  const [customerName, setCustomerName] = useState('Walk-In');
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'CARD'>('CASH');
  const [cashReceived, setCashReceived] = useState('');
  const [cardReference, setCardReference] = useState('');
  const [notes, setNotes] = useState('');
  const [checkingOut, setCheckingOut] = useState(false);

  // receipt modal (BC-1601)
  const [receiptData, setReceiptData] = useState<SaleResult | null>(null);
  const receiptRef = useRef<HTMLDivElement>(null);

  // EOD reconciliation (BC-1603)
  const [eodData, setEodData] = useState<ReconcileResult | null>(null);
  const [eodLoading, setEodLoading] = useState(false);
  const eodRef = useRef<HTMLDivElement>(null);

  // ─── load ─────────────────────────────────────────────────────────────────

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const [prods, depts] = await Promise.all([
        apiFetch<Product[]>(`/v1/products?tenantId=${TENANT_ID}`),
        apiFetch<Department[]>(`/v1/departments?tenantId=${TENANT_ID}`),
      ]);
      setProducts(prods);
      setDepartments(depts);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // ─── department map ───────────────────────────────────────────────────────

  const deptMap = useMemo(() => {
    const m: Record<string, string> = {};
    departments.forEach((d) => { m[d.departmentId] = d.name; });
    return m;
  }, [departments]);

  // ─── filtered products ────────────────────────────────────────────────────

  const filtered = useMemo(() => {
    return products.filter((p) => {
      if (p.status !== 'ACTIVE') return false;
      if (deptFilter !== 'ALL' && p.departmentId !== deptFilter) return false;
      if (search && !p.name.toLowerCase().includes(search.toLowerCase())) return false;
      return true;
    });
  }, [products, deptFilter, search]);

  // ─── cart helpers ─────────────────────────────────────────────────────────

  const addLine = (line: CartLine) => {
    setCart((prev) => {
      const existing = prev.findIndex((l) => l.productId === line.productId);
      if (existing >= 0) {
        const next = [...prev];
        next[existing] = { ...next[existing], qty: next[existing].qty + line.qty };
        return next;
      }
      return [...prev, line];
    });
    setQuickAddId(null);
  };

  const removeLine = (productId: string) => {
    setCart((prev) => prev.filter((l) => l.productId !== productId));
  };

  const updateQty = (productId: string, qty: number) => {
    if (qty <= 0) { removeLine(productId); return; }
    setCart((prev) => prev.map((l) => l.productId === productId ? { ...l, qty } : l));
  };

  const cartTotal = cart.reduce((s, l) => s + lineTotal(l), 0);
  const cartCount = cart.reduce((s, l) => s + l.qty, 0);

  // ─── checkout ─────────────────────────────────────────────────────────────

  const changeAmount = (() => {
    if (paymentMethod !== 'CASH') return null;
    const received = parseFloat(cashReceived || '0');
    if (!received || received < cartTotal) return null;
    return received - cartTotal;
  })();

  const completeSale = async () => {
    if (cart.length === 0) return;
    if (paymentMethod === 'CARD' && !cardReference.trim()) return;
    try {
      setCheckingOut(true);
      setError('');

      const body: Record<string, unknown> = {
        tenantId: TENANT_ID,
        siteId: 'MAIN',
        paymentMethod,
        lines: cart.map((l) => ({
          productId: l.productId,
          productName: l.productName,
          quantity: l.qty,
          unit: l.uom,
          unitPrice: l.unitPrice,
        })),
      };
      if (paymentMethod === 'CASH' && cashReceived) {
        body.cashReceived = parseFloat(cashReceived);
      }
      if (paymentMethod === 'CARD' && cardReference.trim()) {
        body.cardReference = cardReference.trim();
      }

      const sale = await apiFetch<SaleResult>('/v1/pos/sales', {
        method: 'POST',
        body: JSON.stringify(body),
      });

      // Show receipt modal (BC-1601)
      setReceiptData(sale);
      setSuccess('');
    } catch (e) {
      setError(String(e));
    } finally {
      setCheckingOut(false);
    }
  };

  const resetAfterSale = () => {
    setReceiptData(null);
    setCart([]);
    setCustomerName('Walk-In');
    setCashReceived('');
    setCardReference('');
    setNotes('');
  };

  const printReceipt = () => {
    if (!receiptRef.current) return;
    const win = window.open('', '_blank', 'width=400,height=600');
    if (!win) return;
    win.document.write('<html><head><title>Receipt</title><style>body{font-family:monospace;padding:20px;font-size:12px}table{width:100%;border-collapse:collapse}td,th{text-align:left;padding:4px 0}th{border-bottom:1px dashed #000}.right{text-align:right}.total{border-top:2px solid #000;font-weight:bold}</style></head><body>');
    win.document.write(receiptRef.current.innerHTML);
    win.document.write('</body></html>');
    win.document.close();
    win.print();
  };

  // EOD reconciliation (BC-1603)
  const runEod = async () => {
    try {
      setEodLoading(true);
      setError('');
      const result = await apiFetch<ReconcileResult>('/v1/pos/reconcile', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, siteId: 'MAIN', date: new Date().toISOString().substring(0, 10) }),
      });
      setEodData(result);
    } catch (e) {
      setError(String(e));
    } finally {
      setEodLoading(false);
    }
  };

  const printEod = () => {
    if (!eodRef.current) return;
    const win = window.open('', '_blank', 'width=400,height=500');
    if (!win) return;
    win.document.write('<html><head><title>End of Day</title><style>body{font-family:monospace;padding:20px;font-size:12px}table{width:100%;border-collapse:collapse}td{padding:4px 0}.right{text-align:right}.total{border-top:2px solid #000;font-weight:bold}</style></head><body>');
    win.document.write(eodRef.current.innerHTML);
    win.document.write('</body></html>');
    win.document.close();
    win.print();
  };

  // ─── render ───────────────────────────────────────────────────────────────

  return (
    <div className="h-[calc(100vh-64px)] flex flex-col">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between mb-3 shrink-0">
        <div>
          <div className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-600">Retail</div>
          <h1 className="mt-1 text-2xl font-bold text-gray-900">{t('pos.title')}</h1>
          <p className="mt-1 text-sm text-gray-500">{t('pos.subtitle')}</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            className="btn-secondary text-xs"
            disabled={eodLoading}
            onClick={runEod}
          >
            {eodLoading ? t('pos.processing') : t('pos.endOfDay')}
          </button>
        </div>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && (
        <div className="mb-3 bg-green-50 border border-green-200 text-green-800 rounded-xl px-4 py-3 text-sm flex items-center justify-between shrink-0">
          {success}
          <button className="ml-4 text-green-600 hover:text-green-800 font-medium" onClick={() => setSuccess('')}>×</button>
        </div>
      )}

      <div className="flex gap-4 flex-1 min-h-0">
        {/* ── Product Catalog ─────────────────────────────────────────────── */}
        <div className="flex-1 flex flex-col min-w-0">
          {/* Filter bar */}
          <div className="flex gap-2 mb-3 shrink-0">
            <input
              className="input flex-1"
              placeholder={t('pos.searchProducts')}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="input w-44"
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
            >
              <option value="ALL">{t('pos.allDepartments')}</option>
              {departments.map((d) => (
                <option key={d.departmentId} value={d.departmentId}>{d.name}</option>
              ))}
            </select>
          </div>

          {/* Grid */}
          {loading ? (
            <Spinner />
          ) : filtered.length === 0 ? (
            <div className="flex-1 flex items-center justify-center text-sm text-gray-400">
              {products.filter((p) => p.status === 'ACTIVE').length === 0
                ? t('pos.noActiveProducts')
                : t('pos.noProductsMatch')}
            </div>
          ) : (
            <div className="overflow-y-auto flex-1 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 content-start">
              {filtered.map((p) => {
                const inCart = cart.find((l) => l.productId === p.productId);
                return (
                  <div
                    key={p.productId}
                    className="relative rounded-2xl border border-gray-200 bg-white shadow-sm hover:shadow-md transition-shadow"
                  >
                    {/* Product card */}
                    <button
                      className="w-full text-left p-4 rounded-xl"
                      onClick={() => setQuickAddId(p.productId)}
                    >
                      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gray-100 text-gray-400 mb-2"><Package className="h-5 w-5" /></div>
                      <div className="font-semibold text-sm leading-tight">{p.name}</div>
                      <div className="text-xs text-gray-400 mt-1">{deptMap[p.departmentId] ?? '—'}</div>
                      <div className="text-xs text-gray-400 mt-0.5">{p.baseUom}</div>
                      {inCart && (
                        <div className="mt-2 inline-block bg-indigo-100 text-indigo-700 text-xs px-2 py-0.5 rounded-full font-medium">
                          {t('pos.inCart', { qty: inCart.qty })}
                        </div>
                      )}
                    </button>

                    {/* Quick-add overlay */}
                    {quickAddId === p.productId && (
                      <QuickAdd
                        product={p}
                        deptName={deptMap[p.departmentId] ?? ''}
                        onAdd={addLine}
                        onCancel={() => setQuickAddId(null)}
                      />
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* ── Cart ────────────────────────────────────────────────────────── */}
        <div className="w-80 shrink-0 flex flex-col bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="px-4 py-3 border-b bg-gray-50 flex items-center justify-between">
            <span className="font-semibold text-sm">{t('pos.cart')}</span>
            {cart.length > 0 && (
              <button
                className="text-xs text-red-500 hover:underline"
                onClick={() => setCart([])}
              >
                {t('pos.clear')}
              </button>
            )}
          </div>

          {/* Cart lines */}
          <div className="flex-1 overflow-y-auto divide-y">
            {cart.length === 0 ? (
              <div className="py-12 text-center text-sm text-gray-400">
                {t('pos.tapToAdd')}
              </div>
            ) : (
              cart.map((line) => (
                <div key={line.productId} className="px-4 py-3">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium truncate">{line.productName}</div>
                      <div className="text-xs text-gray-400">{line.unitPrice.toFixed(2)} × </div>
                    </div>
                    <button
                      className="text-gray-300 hover:text-red-500 text-xs mt-0.5 shrink-0"
                      onClick={() => removeLine(line.productId)}
                    >
                      ✕
                    </button>
                  </div>
                  <div className="flex items-center gap-2 mt-2">
                    <button
                      className="w-7 h-7 rounded-full border text-gray-600 hover:bg-gray-100 text-sm font-bold flex items-center justify-center"
                      onClick={() => updateQty(line.productId, +(line.qty - 1).toFixed(3))}
                    >
                      −
                    </button>
                    <input
                      className="w-14 border rounded text-center text-sm py-0.5"
                      type="number"
                      min={0.001}
                      step="0.001"
                      value={line.qty}
                      onChange={(e) => {
                        const v = parseFloat(e.target.value);
                        if (!isNaN(v)) updateQty(line.productId, v);
                      }}
                    />
                    <button
                      className="w-7 h-7 rounded-full border text-gray-600 hover:bg-gray-100 text-sm font-bold flex items-center justify-center"
                      onClick={() => updateQty(line.productId, +(line.qty + 1).toFixed(3))}
                    >
                      +
                    </button>
                    <span className="ml-auto text-sm font-semibold">{fmt(lineTotal(line))}</span>
                  </div>
                </div>
              ))
            )}
          </div>

          {/* Summary + checkout */}
          <div className="border-t px-4 py-4 space-y-3">
            <div className="flex justify-between items-center text-sm font-semibold">
              <span>{t('pos.totalItems', { count: cartCount })}</span>
              <span className="text-lg">{fmt(cartTotal)}</span>
            </div>

            <div>
              <label className="text-xs text-gray-500 mb-1 block">{t('pos.customerName')}</label>
              <input
                className="input w-full"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                placeholder="Walk-In"
              />
            </div>

            <div>
              <label className="text-xs text-gray-500 mb-1 block">{t('pos.paymentMethod')}</label>
              <div className="flex gap-2">
                {(['CASH', 'CARD'] as const).map((m) => (
                  <button
                    key={m}
                    type="button"
                    className={`flex-1 py-2 rounded-lg border text-sm font-medium transition-colors ${
                      paymentMethod === m
                        ? 'bg-indigo-600 text-white border-indigo-600'
                        : 'bg-white text-gray-600 border-gray-300 hover:border-indigo-400'
                    }`}
                    onClick={() => setPaymentMethod(m)}
                  >
                    {m === 'CASH' ? t('pos.cash') : t('pos.card')}
                  </button>
                ))}
              </div>
            </div>

            {paymentMethod === 'CASH' && (
              <div>
                <label className="text-xs text-gray-500 mb-1 block">{t('pos.cashReceived')}</label>
                <input
                  className="input w-full"
                  type="number"
                  min={0}
                  step="0.01"
                  placeholder={fmt(cartTotal)}
                  value={cashReceived}
                  onChange={(e) => setCashReceived(e.target.value)}
                />
                {changeAmount !== null && (
                  <div className="mt-1 text-right text-sm font-semibold text-green-600">
                    {t('pos.change')}: {fmt(changeAmount)}
                  </div>
                )}
              </div>
            )}

            {paymentMethod === 'CARD' && (
              <div>
                <label className="text-xs text-gray-500 mb-1 block">{t('pos.terminalRef')} *</label>
                <input
                  className="input w-full"
                  placeholder={t('pos.terminalRefPlaceholder')}
                  value={cardReference}
                  onChange={(e) => setCardReference(e.target.value)}
                  required
                />
              </div>
            )}

            <div>
              <label className="text-xs text-gray-500 mb-1 block">{t('pos.notesOptional')}</label>
              <input
                className="input w-full"
                placeholder={t('pos.notesPaidCash')}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>

            <button
              className="w-full btn-primary py-3 text-base font-semibold disabled:opacity-50"
              disabled={cart.length === 0 || checkingOut || (paymentMethod === 'CARD' && !cardReference.trim())}
              onClick={completeSale}
            >
              {checkingOut ? t('pos.processing') : t('pos.completeSale', { total: fmt(cartTotal) })}
            </button>
          </div>
        </div>
      </div>

      {/* ── Receipt Modal (BC-1601) ───────────────────────────────────── */}
      {receiptData && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md max-h-[90vh] flex flex-col">
            <div className="px-6 py-4 border-b flex items-center justify-between">
              <h2 className="font-semibold text-lg">{t('pos.receiptTitle')}</h2>
              <button className="text-gray-400 hover:text-gray-600 text-xl" onClick={resetAfterSale}>×</button>
            </div>
            <div className="px-6 py-4 overflow-y-auto flex-1" ref={receiptRef}>
              <div style={{ textAlign: 'center', marginBottom: 16 }}>
                <div style={{ fontWeight: 'bold', fontSize: 16 }}>BreadCost POS</div>
                <div style={{ fontSize: 12 }}>{t('pos.receiptSaleId')}: #{receiptData.saleId.slice(0, 8).toUpperCase()}</div>
                <div style={{ fontSize: 12 }}>{new Date(receiptData.completedAt).toLocaleString()}</div>
                <div style={{ fontSize: 12 }}>{t('pos.receiptCashier')}: {receiptData.cashierName || getUsername() || '—'}</div>
              </div>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr style={{ borderBottom: '1px dashed #000' }}>
                    <th style={{ textAlign: 'left', padding: '4px 0' }}>{t('pos.receiptItem')}</th>
                    <th style={{ textAlign: 'right', padding: '4px 0' }}>{t('orders.qty')}</th>
                    <th style={{ textAlign: 'right', padding: '4px 0' }}>{t('pos.unitPrice')}</th>
                    <th style={{ textAlign: 'right', padding: '4px 0' }}>{t('common.total')}</th>
                  </tr>
                </thead>
                <tbody>
                  {receiptData.lines.map((line, i) => (
                    <tr key={i}>
                      <td style={{ padding: '3px 0' }}>{line.productName}</td>
                      <td style={{ textAlign: 'right', padding: '3px 0' }}>{line.quantity}</td>
                      <td style={{ textAlign: 'right', padding: '3px 0' }}>{fmt(line.unitPrice)}</td>
                      <td style={{ textAlign: 'right', padding: '3px 0' }}>{fmt(line.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div style={{ borderTop: '2px solid #000', marginTop: 8, paddingTop: 8, fontWeight: 'bold', display: 'flex', justifyContent: 'space-between', fontSize: 14 }}>
                <span>{t('pos.receiptGrandTotal')}</span>
                <span>{fmt(receiptData.totalAmount)}</span>
              </div>
              <div style={{ marginTop: 8, fontSize: 12 }}>
                <div>{t('pos.paymentMethod')}: {receiptData.paymentMethod === 'CASH' ? t('pos.cash') : t('pos.card')}</div>
                {receiptData.paymentMethod === 'CASH' && receiptData.cashReceived != null && (
                  <>
                    <div>{t('pos.cashReceived')}: {fmt(receiptData.cashReceived)}</div>
                    <div>{t('pos.change')}: {fmt(receiptData.changeGiven ?? 0)}</div>
                  </>
                )}
                {receiptData.paymentMethod === 'CARD' && receiptData.cardReference && (
                  <div>{t('pos.terminalRef')}: {receiptData.cardReference}</div>
                )}
              </div>
            </div>
            <div className="px-6 py-4 border-t flex gap-3">
              <button className="flex-1 btn-secondary" onClick={printReceipt}>{t('pos.printReceipt')}</button>
              <button className="flex-1 btn-primary" onClick={resetAfterSale}>{t('pos.newSale')}</button>
            </div>
          </div>
        </div>
      )}

      {/* ── EOD Reconciliation Panel (BC-1603) ────────────────────────── */}
      {eodData && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md">
            <div className="px-6 py-4 border-b flex items-center justify-between">
              <h2 className="font-semibold text-lg">{t('pos.eodTitle')}</h2>
              <button className="text-gray-400 hover:text-gray-600 text-xl" onClick={() => setEodData(null)}>×</button>
            </div>
            <div className="px-6 py-4" ref={eodRef}>
              <div style={{ textAlign: 'center', marginBottom: 16, fontWeight: 'bold' }}>
                {t('pos.eodTitle')} — {eodData.date}
              </div>
              <table style={{ width: '100%', fontSize: 13 }}>
                <tbody>
                  <tr><td style={{ padding: '4px 0' }}>{t('pos.eodTotalTransactions')}</td><td style={{ textAlign: 'right' }}>{eodData.totalTransactions}</td></tr>
                  <tr><td style={{ padding: '4px 0' }}>{t('pos.eodCashTotal')}</td><td style={{ textAlign: 'right' }}>{fmt(eodData.cashTotal)}</td></tr>
                  <tr><td style={{ padding: '4px 0' }}>{t('pos.eodCardTotal')}</td><td style={{ textAlign: 'right' }}>{fmt(eodData.cardTotal)}</td></tr>
                  <tr><td style={{ padding: '4px 0' }}>{t('pos.eodRefunds')}</td><td style={{ textAlign: 'right' }}>{fmt(eodData.refunds)}</td></tr>
                  <tr style={{ borderTop: '2px solid #000', fontWeight: 'bold' }}><td style={{ padding: '6px 0' }}>{t('pos.eodNetSales')}</td><td style={{ textAlign: 'right' }}>{fmt(eodData.netSales)}</td></tr>
                  <tr style={{ fontWeight: 'bold' }}><td style={{ padding: '4px 0' }}>{t('pos.eodExpectedCash')}</td><td style={{ textAlign: 'right' }}>{fmt(eodData.expectedCashInDrawer)}</td></tr>
                </tbody>
              </table>
            </div>
            <div className="px-6 py-4 border-t flex gap-3">
              <button className="flex-1 btn-secondary" onClick={printEod}>{t('pos.printReceipt')}</button>
              <button className="flex-1 btn-primary" onClick={() => setEodData(null)}>{t('common.close')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
