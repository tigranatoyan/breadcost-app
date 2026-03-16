'use client';
import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { API_BASE, TENANT_ID } from '@/lib/api';
import { getCustomerToken, getCustomerInfo } from '@/lib/customer-auth';
import { CheckCircle, ShoppingCart, ArrowRight } from 'lucide-react';
import Link from 'next/link';

interface OrderDetail {
  orderId: string;
  status: string;
  totalAmount: number;
  customerName: string;
  lines: Array<{
    productName: string;
    qty: number;
    unitPrice: number;
  }>;
}

export default function OrderConfirmationPage() {
  const params = useParams();
  const router = useRouter();
  const orderId = params.orderId as string;
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const customer = getCustomerInfo();
    const token = getCustomerToken();
    if (!customer || !token) { router.replace('/customer/login'); return; }

    fetch(`${API_BASE}/v2/orders/${orderId}?tenantId=${TENANT_ID}&customerId=${customer.customerId}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(r => r.ok ? r.json() : null)
      .then(data => { if (data) setOrder(data); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [orderId, router]);

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <div className="w-8 h-8 border-4 border-amber-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      {/* Success banner */}
      <div className="rounded-2xl bg-gradient-to-br from-green-50 to-emerald-50 border border-green-200 p-8 text-center mb-8">
        <CheckCircle className="mx-auto h-14 w-14 text-green-500 mb-4" />
        <h1 className="text-2xl font-bold text-gray-900 mb-2">Order Confirmed!</h1>
        <p className="text-gray-600">
          Order <span className="font-mono font-semibold">#{orderId?.slice(0, 8)}</span> has been placed successfully.
        </p>
        <p className="text-sm text-gray-500 mt-2">
          You will receive a WhatsApp notification when your order status changes.
        </p>
      </div>

      {/* Order summary */}
      {order && (
        <div className="rounded-2xl border border-gray-200 bg-white shadow-sm p-6 mb-8">
          <h2 className="font-semibold text-gray-900 mb-4">Order Summary</h2>
          <div className="divide-y divide-gray-100">
            {order.lines?.map((line) => (
              <div key={`${line.productName}-${line.qty}`} className="flex justify-between py-2 text-sm">
                <span className="text-gray-700">
                  {line.productName} × {line.qty}
                </span>
                <span className="font-medium text-gray-900">
                  {((line.unitPrice || 0) * line.qty).toLocaleString()} AMD
                </span>
              </div>
            ))}
          </div>
          <div className="border-t mt-3 pt-3 flex justify-between font-bold text-gray-900">
            <span>Total</span>
            <span>{order.totalAmount?.toLocaleString()} AMD</span>
          </div>
        </div>
      )}

      {/* CTA buttons */}
      <div className="flex flex-col sm:flex-row gap-3">
        <Link
          href="/customer/orders"
          className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-amber-600 px-6 py-3 text-white font-semibold hover:bg-amber-700"
        >
          Track Order <ArrowRight className="h-4 w-4" />
        </Link>
        <Link
          href="/customer/catalog"
          className="flex-1 flex items-center justify-center gap-2 rounded-xl border border-gray-200 bg-white px-6 py-3 font-semibold text-gray-700 hover:bg-gray-50"
        >
          <ShoppingCart className="h-4 w-4" /> Back to Catalog
        </Link>
      </div>
    </div>
  );
}
