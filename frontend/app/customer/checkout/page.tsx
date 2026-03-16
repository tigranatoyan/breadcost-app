'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { API_BASE, TENANT_ID } from '@/lib/api';
import { getCustomerToken, getCustomerInfo } from '@/lib/customer-auth';
import {
  Minus, Plus, Trash2, ShoppingCart, CalendarDays, Clock,
  Zap, AlertCircle, ChevronLeft,
} from 'lucide-react';

interface CartItem {
  productId: string;
  name: string;
  saleUnit: string;
  price: number;
  qty: number;
}

const TIME_SLOTS = [
  '08:00 - 10:00', '10:00 - 12:00', '12:00 - 14:00',
  '14:00 - 16:00', '16:00 - 18:00', '18:00 - 20:00',
];

export default function CheckoutPage() {
  const router = useRouter();
  const [cart, setCart] = useState<CartItem[]>([]);
  const [deliveryDate, setDeliveryDate] = useState('');
  const [timeSlot, setTimeSlot] = useState('');
  const [rush, setRush] = useState(false);
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  // Load cart from sessionStorage
  useEffect(() => {
    if (globalThis.window === undefined) return;
    const raw = sessionStorage.getItem('bc_cart');
    if (!raw) {
      router.replace('/customer/catalog');
      return;
    }
    try {
      const items: CartItem[] = JSON.parse(raw);
      if (!items.length) { router.replace('/customer/catalog'); return; }
      setCart(items);
    } catch {
      router.replace('/customer/catalog');
    }
  }, [router]);

  // Min delivery date = tomorrow
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const minDate = tomorrow.toISOString().split('T')[0];

  const updateQty = (productId: string, delta: number) => {
    setCart(prev => {
      const next = prev.map(item =>
        item.productId === productId
          ? { ...item, qty: Math.max(1, item.qty + delta) }
          : item
      );
      sessionStorage.setItem('bc_cart', JSON.stringify(next));
      return next;
    });
  };

  const removeItem = (productId: string) => {
    setCart(prev => {
      const next = prev.filter(i => i.productId !== productId);
      if (next.length === 0) {
        sessionStorage.removeItem('bc_cart');
        router.replace('/customer/catalog');
        return [];
      }
      sessionStorage.setItem('bc_cart', JSON.stringify(next));
      return next;
    });
  };

  const subtotal = cart.reduce((s, i) => s + i.price * i.qty, 0);
  const rushPremium = rush ? subtotal * 0.15 : 0;
  const total = subtotal + rushPremium;

  const handlePlaceOrder = async () => {
    setError('');
    const customer = getCustomerInfo();
    const token = getCustomerToken();
    if (!customer || !token) {
      router.push('/customer/login');
      return;
    }

    setSubmitting(true);
    try {
      let requestedDeliveryTime: string | undefined;
      if (deliveryDate) {
        const slot = timeSlot || '08:00 - 10:00';
        const hour = slot.split(':')[0];
        requestedDeliveryTime = new Date(`${deliveryDate}T${hour}:00:00`).toISOString();
      }

      const body = {
        tenantId: TENANT_ID,
        customerId: customer.customerId,
        customerName: customer.name,
        items: cart.map(i => ({ productId: i.productId, qty: i.qty })),
        requestedDeliveryTime,
        notes: notes || undefined,
      };

      const res = await fetch(`${API_BASE}/v2/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(body),
      });

      if (!res.ok) {
        const text = await res.text().catch(() => 'Unknown error');
        throw new Error(text);
      }

      const data = await res.json();
      sessionStorage.removeItem('bc_cart');
      router.push(`/customer/order-confirmation/${data.orderId}`);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to place order');
    } finally {
      setSubmitting(false);
    }
  };

  if (!cart.length) return null;

  return (
    <div className="max-w-3xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <button
          onClick={() => router.push('/customer/catalog')}
          className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-2"
        >
          <ChevronLeft className="h-4 w-4" /> Back to Catalog
        </button>
        <p className="text-xs font-semibold uppercase tracking-widest text-amber-600 mb-1">
          Checkout
        </p>
        <h1 className="text-2xl font-bold text-gray-900">Review Your Order</h1>
      </div>

      {/* Cart items */}
      <div className="rounded-2xl border border-gray-200 bg-white shadow-sm mb-6 divide-y divide-gray-100">
        {cart.map(item => (
          <div key={item.productId} className="flex items-center gap-4 p-4">
            <div className="flex-1 min-w-0">
              <p className="font-medium text-gray-900 truncate">{item.name}</p>
              <p className="text-sm text-gray-500">{item.price.toLocaleString()} AMD / {item.saleUnit}</p>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => updateQty(item.productId, -1)}
                className="rounded-lg border border-gray-200 p-1.5 hover:bg-gray-50"
              >
                <Minus className="h-3.5 w-3.5" />
              </button>
              <span className="w-8 text-center font-medium text-sm">{item.qty}</span>
              <button
                onClick={() => updateQty(item.productId, 1)}
                className="rounded-lg border border-gray-200 p-1.5 hover:bg-gray-50"
              >
                <Plus className="h-3.5 w-3.5" />
              </button>
            </div>
            <p className="w-24 text-right font-semibold text-gray-900">
              {(item.price * item.qty).toLocaleString()} AMD
            </p>
            <button
              onClick={() => removeItem(item.productId)}
              className="p-1.5 text-gray-400 hover:text-red-500"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        ))}
      </div>

      {/* Delivery options */}
      <div className="rounded-2xl border border-gray-200 bg-white shadow-sm p-5 mb-6">
        <h2 className="font-semibold text-gray-900 mb-4">Delivery Options</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="delivery-date" className="block text-sm font-medium text-gray-700 mb-1">
              <CalendarDays className="inline h-4 w-4 mr-1" /> Delivery Date
            </label>
            <input
              id="delivery-date"
              type="date"
              min={minDate}
              value={deliveryDate}
              onChange={e => setDeliveryDate(e.target.value)}
              className="input"
            />
          </div>
          <div>
            <label htmlFor="time-slot" className="block text-sm font-medium text-gray-700 mb-1">
              <Clock className="inline h-4 w-4 mr-1" /> Time Slot
            </label>
            <select
              id="time-slot"
              value={timeSlot}
              onChange={e => setTimeSlot(e.target.value)}
              className="input"
            >
              <option value="">Select time slot</option>
              {TIME_SLOTS.map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
        </div>
        <label className="flex items-center gap-2 mt-4 cursor-pointer">
          <input
            type="checkbox"
            checked={rush}
            onChange={e => setRush(e.target.checked)}
            className="rounded border-gray-300 text-amber-600 focus:ring-amber-500"
          />
          <Zap className="h-4 w-4 text-amber-500" />
          <span className="text-sm text-gray-700">Rush order (+15% premium)</span>
        </label>
      </div>

      {/* Notes */}
      <div className="rounded-2xl border border-gray-200 bg-white shadow-sm p-5 mb-6">
        <h2 className="font-semibold text-gray-900 mb-3">Notes</h2>
        <textarea
          aria-label="Order notes"
          value={notes}
          onChange={e => setNotes(e.target.value)}
          placeholder="Any special instructions…"
          className="input resize-none h-20"
          rows={3}
        />
      </div>

      {/* Price breakdown */}
      <div className="rounded-2xl border border-gray-200 bg-white shadow-sm p-5 mb-6">
        <h2 className="font-semibold text-gray-900 mb-3">Order Summary</h2>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-gray-600">Subtotal ({cart.reduce((s, i) => s + i.qty, 0)} items)</span>
            <span className="font-medium">{subtotal.toLocaleString()} AMD</span>
          </div>
          {rush && (
            <div className="flex justify-between text-amber-600">
              <span>Rush premium (15%)</span>
              <span className="font-medium">+{rushPremium.toLocaleString()} AMD</span>
            </div>
          )}
          <div className="border-t pt-2 flex justify-between text-base font-bold text-gray-900">
            <span>Total</span>
            <span>{total.toLocaleString()} AMD</span>
          </div>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-4 flex items-start gap-2 rounded-xl bg-red-50 border border-red-200 p-3 text-sm text-red-700">
          <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Place order button */}
      <button
        onClick={handlePlaceOrder}
        disabled={submitting}
        className="w-full rounded-xl bg-amber-600 px-6 py-3 text-white font-semibold hover:bg-amber-700 disabled:opacity-50 flex items-center justify-center gap-2"
      >
        <ShoppingCart className="h-5 w-5" />
        {submitting ? 'Placing Order…' : 'Place Order'}
      </button>
    </div>
  );
}
