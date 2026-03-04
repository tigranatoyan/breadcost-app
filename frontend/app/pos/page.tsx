'use client';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Spinner, Alert } from '@/components/ui';

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
          <label className="text-xs text-gray-500 mb-1 block">Qty ({product.baseUom})</label>
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
          <label className="text-xs text-gray-500 mb-1 block">Unit Price</label>
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
          Total: {fmt(parseFloat(qty || '0') * parseFloat(price || '0'))}
        </div>
        <div className="flex gap-2 mt-auto pt-2">
          <button type="button" className="flex-1 btn-secondary text-xs py-1.5" onClick={onCancel}>Cancel</button>
          <button type="submit" className="flex-1 btn-primary text-xs py-1.5">Add to Cart</button>
        </div>
      </form>
    </div>
  );
}

// ─── main component ───────────────────────────────────────────────────────────

export default function POSPage() {
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
  const [notes, setNotes] = useState('');
  const [checkingOut, setCheckingOut] = useState(false);

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

      const sale = await apiFetch<{ saleId: string }>('/v1/pos/sales', {
        method: 'POST',
        body: JSON.stringify(body),
      });

      const change = changeAmount !== null ? `  Change: ${fmt(changeAmount)}` : '';
      setSuccess(`✅ Sale complete — #${sale.saleId.slice(0, 8).toUpperCase()}${change}`);
      setCart([]);
      setCustomerName('Walk-In');
      setCashReceived('');
      setNotes('');
    } catch (e) {
      setError(String(e));
    } finally {
      setCheckingOut(false);
    }
  };

  // ─── render ───────────────────────────────────────────────────────────────

  return (
    <div className="h-[calc(100vh-64px)] flex flex-col">
      <div className="flex items-center justify-between mb-3 shrink-0">
        <h1 className="text-2xl font-semibold">Point of Sale</h1>
        <span className="text-sm text-gray-400">POS — walk-in &amp; counter sales</span>
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
              placeholder="Search products…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="input w-44"
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
            >
              <option value="ALL">All Departments</option>
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
                ? 'No active products. Add products in Admin → Products.'
                : 'No products match the search.'}
            </div>
          ) : (
            <div className="overflow-y-auto flex-1 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3 content-start">
              {filtered.map((p) => {
                const inCart = cart.find((l) => l.productId === p.productId);
                return (
                  <div
                    key={p.productId}
                    className="relative border rounded-xl bg-white shadow-sm hover:shadow-md transition-shadow"
                  >
                    {/* Product card */}
                    <button
                      className="w-full text-left p-4 rounded-xl"
                      onClick={() => setQuickAddId(p.productId)}
                    >
                      <div className="text-2xl mb-2">🍞</div>
                      <div className="font-semibold text-sm leading-tight">{p.name}</div>
                      <div className="text-xs text-gray-400 mt-1">{deptMap[p.departmentId] ?? '—'}</div>
                      <div className="text-xs text-gray-400 mt-0.5">{p.baseUom}</div>
                      {inCart && (
                        <div className="mt-2 inline-block bg-indigo-100 text-indigo-700 text-xs px-2 py-0.5 rounded-full font-medium">
                          ×{inCart.qty} in cart
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
        <div className="w-80 shrink-0 flex flex-col bg-white border rounded-xl shadow-sm overflow-hidden">
          <div className="px-4 py-3 border-b bg-gray-50 flex items-center justify-between">
            <span className="font-semibold text-sm">Cart</span>
            {cart.length > 0 && (
              <button
                className="text-xs text-red-500 hover:underline"
                onClick={() => setCart([])}
              >
                Clear
              </button>
            )}
          </div>

          {/* Cart lines */}
          <div className="flex-1 overflow-y-auto divide-y">
            {cart.length === 0 ? (
              <div className="py-12 text-center text-sm text-gray-400">
                Tap a product to add it to the cart.
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
              <span>Total ({cartCount} items)</span>
              <span className="text-lg">{fmt(cartTotal)}</span>
            </div>

            <div>
              <label className="text-xs text-gray-500 mb-1 block">Customer Name</label>
              <input
                className="input w-full"
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                placeholder="Walk-In"
              />
            </div>

            <div>
              <label className="text-xs text-gray-500 mb-1 block">Payment Method</label>
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
                    {m === 'CASH' ? '💵 Cash' : '💳 Card'}
                  </button>
                ))}
              </div>
            </div>

            {paymentMethod === 'CASH' && (
              <div>
                <label className="text-xs text-gray-500 mb-1 block">Cash Received</label>
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
                    Change: {fmt(changeAmount)}
                  </div>
                )}
              </div>
            )}

            <div>
              <label className="text-xs text-gray-500 mb-1 block">Notes (optional)</label>
              <input
                className="input w-full"
                placeholder="e.g. paid cash"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>

            <button
              className="w-full btn-primary py-3 text-base font-semibold disabled:opacity-50"
              disabled={cart.length === 0 || checkingOut}
              onClick={completeSale}
            >
              {checkingOut ? 'Processing…' : `Complete Sale — ${fmt(cartTotal)}`}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
