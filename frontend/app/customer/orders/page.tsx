'use client';
import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { API_BASE, TENANT_ID } from '@/lib/api';
import { getCustomerToken, getCustomerInfo, type CustomerInfo } from '@/lib/customer-auth';
import {
  Package, Clock, ChevronDown, ChevronUp,
  RefreshCw, ShoppingCart, CheckCircle2, Circle,
  Truck, ChefHat, ClipboardCheck,
} from 'lucide-react';
import { useDateFmt, useDateTimeFmt, useT } from '@/lib/i18n';

interface OrderLine {
  productName: string;
  qty: number;
  unitPrice: number;
}

interface Order {
  orderId: string;
  status: string;
  totalAmount: number;
  customerName: string;
  orderPlacedAt: string;
  requestedDeliveryTime: string | null;
  rushOrder: boolean;
  notes: string | null;
  lines: OrderLine[];
}

interface TimelineEntry {
  status: string;
  timestamp: number;
  description: string;
}

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  CONFIRMED: 'bg-blue-100 text-blue-700',
  IN_PRODUCTION: 'bg-yellow-100 text-yellow-700',
  READY: 'bg-green-100 text-green-700',
  OUT_FOR_DELIVERY: 'bg-purple-100 text-purple-700',
  DELIVERED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-red-100 text-red-700',
};

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Pending',
  CONFIRMED: 'Confirmed',
  IN_PRODUCTION: 'In Production',
  READY: 'Ready',
  OUT_FOR_DELIVERY: 'Out for Delivery',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

const TIMELINE_STEPS = [
  { status: 'CONFIRMED', icon: ClipboardCheck, label: 'Confirmed' },
  { status: 'IN_PRODUCTION', icon: ChefHat, label: 'In Production' },
  { status: 'READY', icon: Package, label: 'Ready' },
  { status: 'OUT_FOR_DELIVERY', icon: Truck, label: 'Out for Delivery' },
  { status: 'DELIVERED', icon: CheckCircle2, label: 'Delivered' },
];

export default function MyOrdersPage() {
  const router = useRouter();
  const t = useT();
  const formatDate = useDateFmt();
  const formatDateTime = useDateTimeFmt();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [timeline, setTimeline] = useState<TimelineEntry[]>([]);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [, setCustomer] = useState<CustomerInfo | null>(null);

  const fetchOrders = useCallback(async () => {
    const c = getCustomerInfo();
    const token = getCustomerToken();
    if (!c || !token) { router.replace('/customer/login'); return; }
    setCustomer(c);
    setLoading(true);
    try {
      const res = await fetch(
        `${API_BASE}/v2/orders?tenantId=${TENANT_ID}&customerId=${c.customerId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      if (res.ok) {
        const data: Order[] = await res.json();
        // Sort newest first
        data.sort((a, b) => new Date(b.orderPlacedAt).getTime() - new Date(a.orderPlacedAt).getTime());
        setOrders(data);
      }
    } catch { /* ignore */ }
    finally { setLoading(false); }
  }, [router]);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  const toggleExpand = async (orderId: string) => {
    if (expandedId === orderId) { setExpandedId(null); return; }
    setExpandedId(orderId);
    setTimelineLoading(true);
    setTimeline([]);
    const c = getCustomerInfo();
    const token = getCustomerToken();
    if (!c || !token) return;
    try {
      const res = await fetch(
        `${API_BASE}/v2/orders/${orderId}/timeline?tenantId=${TENANT_ID}&customerId=${c.customerId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      if (res.ok) setTimeline(await res.json());
    } catch { /* ignore */ }
    finally { setTimelineLoading(false); }
  };

  const completedStatuses = new Set(timeline.map(t => t.status));

  // Separate active/recent vs completed orders
  const activeOrders = orders.filter(o => !['DELIVERED', 'CANCELLED'].includes(o.status));
  const pastOrders = orders.filter(o => ['DELIVERED', 'CANCELLED'].includes(o.status)).slice(0, 5);

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-end justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-amber-600 mb-1">Orders</p>
          <h1 className="text-2xl font-bold text-gray-900">My Orders</h1>
        </div>
        <button
          onClick={fetchOrders}
          className="flex items-center gap-1.5 rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-600 hover:bg-gray-50"
        >
          <RefreshCw className="h-4 w-4" /> Refresh
        </button>
      </div>

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="w-8 h-8 border-4 border-amber-600 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : orders.length === 0 ? (
        <div className="text-center py-16">
          <Package className="mx-auto h-12 w-12 text-gray-300 mb-3" />
          <p className="text-gray-500">No orders yet.</p>
          <button
            onClick={() => router.push('/customer/catalog')}
            className="mt-4 inline-flex items-center gap-2 rounded-xl bg-amber-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-amber-700"
          >
            <ShoppingCart className="h-4 w-4" /> Browse Catalog
          </button>
        </div>
      ) : (
        <>
          {/* Active orders */}
          {activeOrders.length > 0 && (
            <div className="mb-8">
              <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">Active Orders</h2>
              <div className="space-y-3">
                {activeOrders.map(order => (
                  <OrderCard
                    key={order.orderId}
                    order={order}
                    expanded={expandedId === order.orderId}
                    onToggle={() => toggleExpand(order.orderId)}
                    timeline={expandedId === order.orderId ? timeline : []}
                    timelineLoading={expandedId === order.orderId && timelineLoading}
                    completedStatuses={expandedId === order.orderId ? completedStatuses : new Set()}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Recent history */}
          {pastOrders.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">Recent History</h2>
              <div className="space-y-3">
                {pastOrders.map(order => (
                  <OrderCard
                    key={order.orderId}
                    order={order}
                    expanded={expandedId === order.orderId}
                    onToggle={() => toggleExpand(order.orderId)}
                    timeline={expandedId === order.orderId ? timeline : []}
                    timelineLoading={expandedId === order.orderId && timelineLoading}
                    completedStatuses={expandedId === order.orderId ? completedStatuses : new Set()}
                  />
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function OrderCard({
  order, expanded, onToggle, timeline, timelineLoading, completedStatuses,
}: {
  order: Order;
  expanded: boolean;
  onToggle: () => void;
  timeline: TimelineEntry[];
  timelineLoading: boolean;
  completedStatuses: Set<string>;
}) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white shadow-sm overflow-hidden">
      {/* Summary row */}
      <button
        onClick={onToggle}
        className="w-full flex items-center gap-4 p-4 text-left hover:bg-gray-50 transition-colors"
      >
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <span className="font-mono text-sm font-semibold text-gray-900">
              #{order.orderId.slice(0, 8)}
            </span>
            <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-700'}`}>
              {t(`statusLabels.${order.status}` as any) || order.status}
            </span>
            {order.rushOrder && (
              <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">Rush</span>
            )}
          </div>
          <p className="text-sm text-gray-500">
            {formatDate(order.orderPlacedAt)} · {order.lines?.length || 0} item{(order.lines?.length || 0) !== 1 ? 's' : ''}
          </p>
        </div>
        <p className="font-semibold text-gray-900 mr-2">
          {order.totalAmount?.toLocaleString()} AMD
        </p>
        {expanded ? <ChevronUp className="h-4 w-4 text-gray-400" /> : <ChevronDown className="h-4 w-4 text-gray-400" />}
      </button>

      {/* Expanded details */}
      {expanded && (
        <div className="border-t border-gray-100 p-4">
          {/* Tracking timeline */}
          {order.status !== 'CANCELLED' && (
            <div className="mb-5">
              <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-1.5">
                <Clock className="h-4 w-4" /> Tracking
              </h3>
              {timelineLoading ? (
                <div className="flex justify-center py-4">
                  <div className="w-5 h-5 border-2 border-amber-600 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : (
                <div className="flex items-center gap-0">
                  {TIMELINE_STEPS.map((step, i) => {
                    const done = completedStatuses.has(step.status);
                    const Icon = step.icon;
                    return (
                      <div key={step.status} className="flex items-center flex-1">
                        <div className="flex flex-col items-center">
                          <div className={`flex h-8 w-8 items-center justify-center rounded-full ${done ? 'bg-amber-600 text-white' : 'bg-gray-100 text-gray-400'}`}>
                            <Icon className="h-4 w-4" />
                          </div>
                          <span className={`mt-1 text-[10px] text-center leading-tight ${done ? 'font-medium text-gray-900' : 'text-gray-400'}`}>
                            {t(`statusLabels.${step.status}` as any)}
                          </span>
                        </div>
                        {i < TIMELINE_STEPS.length - 1 && (
                          <div className={`flex-1 h-0.5 mx-1 ${done ? 'bg-amber-600' : 'bg-gray-200'}`} />
                        )}
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Detailed timeline entries */}
              {timeline.length > 0 && (
                <div className="mt-3 space-y-1">
                  {timeline.map((t, i) => (
                    <div key={i} className="flex items-start gap-2 text-xs text-gray-500">
                      <Circle className="h-2.5 w-2.5 mt-0.5 fill-amber-400 text-amber-400 shrink-0" />
                      <span className="font-medium text-gray-700">{t.status}</span>
                      <span>—</span>
                      <span>{t.description}</span>
                      <span className="ml-auto text-gray-400">{formatDateTime(t.timestamp)}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Line items */}
          <div className="rounded-xl bg-gray-50 p-3">
            <h3 className="text-sm font-semibold text-gray-700 mb-2">Items</h3>
            {order.lines?.map((line, i) => (
              <div key={i} className="flex justify-between text-sm py-1">
                <span className="text-gray-600">{line.productName} × {line.qty}</span>
                <span className="font-medium text-gray-900">
                  {((line.unitPrice || 0) * line.qty).toLocaleString()} AMD
                </span>
              </div>
            ))}
          </div>

          {order.notes && (
            <p className="mt-3 text-sm text-gray-500">
              <span className="font-medium">Notes:</span> {order.notes}
            </p>
          )}
        </div>
      )}
    </div>
  );
}
