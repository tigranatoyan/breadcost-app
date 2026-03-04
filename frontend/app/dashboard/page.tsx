'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Spinner, Badge } from '@/components/ui';
import Link from 'next/link';

// ─── helpers ──────────────────────────────────────────────────────────────────
function msUntil(iso: string): number {
  return new Date(iso).getTime() - Date.now();
}

function fmtDuration(ms: number): { label: string; urgent: boolean; overdue: boolean } {
  const overdue = ms < 0;
  const abs = Math.abs(ms);
  const h = Math.floor(abs / 3_600_000);
  const m = Math.floor((abs % 3_600_000) / 60_000);
  const label = overdue
    ? `${h}h ${m}m overdue`
    : h > 0
    ? `in ${h}h ${m}m`
    : `in ${m}m`;
  return { label, urgent: !overdue && h < 12, overdue };
}

function fmtMoney(n: number): string {
  if (n >= 1_000_000) return `$${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `$${(n / 1_000).toFixed(1)}K`;
  return `$${n.toFixed(0)}`;
}

// ─── interfaces ───────────────────────────────────────────────────────────────
interface OrderLine {
  orderLineId: string;
  productName: string;
  qty: number;
  uom: string;
  leadTimeConflict: boolean;
}

interface Order {
  orderId: string;
  customerName: string;
  status: string;
  requestedDeliveryTime: string;
  totalAmount: number;
  rushOrder: boolean;
  isRushOrder: boolean;
  lines: OrderLine[];
}

interface StockPosition {
  id: string;
  itemId: string;
  onHandQty: number;
  valuationAmount: number;
}

interface StockItem {
  itemId: string;
  name: string;
  minStockThreshold: number;
}

interface WorkOrderSummary {
  workOrderId: string;
  productName: string;
  status: string;
}

interface Plan {
  planId: string;
  planDate: string;
  shift: string;
  status: string;
  workOrders: WorkOrderSummary[];
}

interface StatCard {
  label: string;
  value: number | string;
  sub?: string;
  href: string;
  icon: string;
  color: string;
}

// ─── sub-components ───────────────────────────────────────────────────────────
function KpiCard({ label, value, sub, href, icon, color }: StatCard) {
  return (
    <Link href={href} className={`border rounded-xl p-4 ${color} hover:shadow-md transition-shadow`}>
      <div className="text-2xl mb-1">{icon}</div>
      <div className="text-3xl font-bold text-gray-800">{value}</div>
      <div className="text-sm font-medium text-gray-600 mt-0.5">{label}</div>
      {sub && <div className="text-xs text-gray-400 mt-0.5">{sub}</div>}
    </Link>
  );
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-3">{children}</h2>;
}

function Card({ children, className = '' }: { children: React.ReactNode; className?: string }) {
  return <div className={`bg-white border rounded-xl shadow-sm ${className}`}>{children}</div>;
}

// ─── main component ───────────────────────────────────────────────────────────
export default function DashboardPage() {
  const [loading, setLoading] = useState(true);
  const [deptCount, setDeptCount] = useState(0);
  const [productCount, setProductCount] = useState(0);
  const [orders, setOrders] = useState<Order[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [positions, setPositions] = useState<StockPosition[]>([]);
  const [stockItems, setStockItems] = useState<StockItem[]>([]);
  const [tick, setTick] = useState(0);
  const today = new Date().toISOString().substring(0, 10);

  const load = useCallback(() => {
    Promise.all([
      apiFetch<unknown[]>(`/v1/departments?tenantId=${TENANT_ID}`).then((d) => setDeptCount(d.length)),
      apiFetch<unknown[]>(`/v1/products?tenantId=${TENANT_ID}`).then((d) => setProductCount(d.length)),
      apiFetch<Order[]>(`/v1/orders?tenantId=${TENANT_ID}`).then(setOrders),
      apiFetch<Plan[]>(`/v1/production-plans?tenantId=${TENANT_ID}`).then(setPlans),
      apiFetch<StockPosition[]>(`/v1/inventory/positions?tenantId=${TENANT_ID}`).then(setPositions).catch(() => {}),
      apiFetch<StockItem[]>(`/v1/items?tenantId=${TENANT_ID}`).then(setStockItems).catch(() => {}),
    ])
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  // refresh countdowns every 30s
  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), 30_000);
    return () => clearInterval(id);
  }, []);

  // ── derived metrics ────────────────────────────────────────────────────────
  const activeOrders = orders.filter((o) => o.status !== 'CANCELLED' && o.status !== 'DELIVERED');
  const criticalOrders = activeOrders.filter((o) => {
    const ms = msUntil(o.requestedDeliveryTime);
    return ms < 0 || ms < 12 * 3_600_000;
  });
  const rushOrders = orders.filter((o) => o.rushOrder || o.isRushOrder);
  const confirmedOrders = orders.filter((o) => o.status === 'CONFIRMED');
  const todayPlans = plans.filter((p) => p.planDate === today);
  const activePlan = plans.find((p) => p.status === 'IN_PROGRESS' || p.status === 'PUBLISHED');

  // inventory metrics
  const totalStockValue = positions.reduce((s, p) => s + (p.valuationAmount ?? 0), 0);
  const itemMap: Record<string, StockItem> = {};
  stockItems.forEach((i) => { itemMap[i.itemId] = i; });
  const lowStockPositions = positions.filter((p) => {
    const threshold = itemMap[p.itemId]?.minStockThreshold ?? 0;
    return threshold > 0 && p.onHandQty < threshold;
  });

  const runningRevenue = orders
    .filter((o) => o.status !== 'CANCELLED' && o.status !== 'DRAFT')
    .reduce((sum, o) => sum + (o.totalAmount ?? 0), 0);

  // delivery timeline — non-cancelled, sorted by delivery time asc
  const deliveryTimeline = [...activeOrders]
    .filter((o) => !!o.requestedDeliveryTime)
    .sort((a, b) => new Date(a.requestedDeliveryTime).getTime() - new Date(b.requestedDeliveryTime).getTime())
    .slice(0, 8);

  // next big event
  const nextEvent: { label: string; detail: string; accent: string } | null = (() => {
    const nextDelivery = deliveryTimeline[0];
    if (nextDelivery) {
      const { label, overdue } = fmtDuration(msUntil(nextDelivery.requestedDeliveryTime));
      return {
        label: `Next delivery — ${nextDelivery.customerName}`,
        detail: `${label} · ${fmtMoney(nextDelivery.totalAmount ?? 0)}`,
        accent: overdue ? 'bg-red-700' : 'bg-slate-800',
      };
    }
    const nextPlan = plans.find((p) => p.status === 'DRAFT' || p.status === 'PUBLISHED');
    if (nextPlan) return {
      label: `Next production run — ${nextPlan.planDate}`,
      detail: `${nextPlan.shift} · ${nextPlan.status}`,
      accent: 'bg-slate-800',
    };
    return null;
  })();

  // issues
  interface Issue { level: 'error' | 'warn' | 'info'; msg: string }
  const issues: Issue[] = [];
  for (const o of activeOrders) {
    const ms = msUntil(o.requestedDeliveryTime);
    if (ms < 0) issues.push({ level: 'error', msg: `Overdue: order for ${o.customerName} (${fmtDuration(ms).label})` });
  }
  const conflictCount = orders.flatMap((o) => o.lines ?? []).filter((l) => l.leadTimeConflict).length;
  if (conflictCount > 0) issues.push({ level: 'warn', msg: `${conflictCount} order line(s) have lead-time conflicts` });
  if (rushOrders.length > 0) issues.push({ level: 'warn', msg: `${rushOrders.length} rush order(s) in queue` });
  const draftPlansNoWO = todayPlans.filter((p) => p.status === 'DRAFT' && (p.workOrders?.length ?? 0) === 0);
  if (draftPlansNoWO.length > 0) issues.push({ level: 'warn', msg: `${draftPlansNoWO.length} of today's plans are DRAFT with no work orders` });
  if (lowStockPositions.length > 0) issues.push({ level: 'warn', msg: `${lowStockPositions.length} stock position(s) below minimum threshold` });
  const confirmedNoMatch = confirmedOrders.filter(
    (o) => !plans.some((p) => p.planDate === o.requestedDeliveryTime?.substring(0, 10))
  );
  if (confirmedNoMatch.length > 0) issues.push({ level: 'info', msg: `${confirmedNoMatch.length} confirmed order(s) have no matching production plan date` });
  const cancelledCount = orders.filter((o) => o.status === 'CANCELLED').length;
  if (cancelledCount > 0) issues.push({ level: 'info', msg: `${cancelledCount} cancelled order(s) on record` });

  // kpi stats
  const stats: StatCard[] = [
    { label: 'Running Revenue', value: fmtMoney(runningRevenue), sub: `${activeOrders.length} active orders`, href: '/orders', icon: '💰', color: 'bg-emerald-50 border-emerald-200' },
    { label: 'Open Orders', value: activeOrders.length, sub: criticalOrders.length > 0 ? `⚠️ ${criticalOrders.length} critical` : `${confirmedOrders.length} confirmed`, href: '/orders', icon: '📦', color: criticalOrders.length > 0 ? 'bg-red-50 border-red-300' : 'bg-green-50 border-green-200' },
    { label: "Today's Plans", value: todayPlans.length, sub: activePlan ? `Active: ${activePlan.shift}` : 'None active', href: '/production-plans', icon: '📅', color: 'bg-purple-50 border-purple-200' },
    { label: 'Stock Value', value: fmtMoney(totalStockValue), sub: lowStockPositions.length > 0 ? `⚠️ ${lowStockPositions.length} below threshold` : `${positions.length} positions`, href: '/inventory', icon: '🏬', color: lowStockPositions.length > 0 ? 'bg-red-50 border-red-300' : 'bg-blue-50 border-blue-200' },
  ];

  // production floor progress
  const floorPlans = plans.filter((p) => ['DRAFT', 'PUBLISHED', 'IN_PROGRESS'].includes(p.status)).slice(0, 6);

  const shiftColor: Record<string, string> = {
    MORNING: 'bg-yellow-100 text-yellow-800',
    AFTERNOON: 'bg-orange-100 text-orange-800',
    NIGHT: 'bg-indigo-100 text-indigo-800',
  };

  if (loading) return <Spinner />;

  return (
    <div className="max-w-6xl space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold">Operations Dashboard</h1>
        <p className="text-sm text-gray-400 mt-0.5">
          {new Date().toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
        </p>
      </div>

      {/* KPI row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {stats.map((s) => <KpiCard key={s.label} {...s} />)}
      </div>

      {/* Next big event banner */}
      {nextEvent && (
        <div className={`${nextEvent.accent} text-white rounded-xl px-6 py-4 flex items-center justify-between`}>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide opacity-70 mb-0.5">Next Event</div>
            <div className="text-lg font-bold">{nextEvent.label}</div>
            <div className="text-sm opacity-80 mt-0.5">{nextEvent.detail}</div>
          </div>
          <Link href="/orders" className="text-white underline text-sm opacity-80 hover:opacity-100 whitespace-nowrap ml-4">View →</Link>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Delivery Timeline (spans 2 cols) */}
        <div className="lg:col-span-2 space-y-4">
          <SectionTitle>Delivery Timeline</SectionTitle>
          <Card>
            {deliveryTimeline.length === 0 ? (
              <div className="py-10 text-center text-sm text-gray-400">No upcoming deliveries. <Link href="/orders" className="text-blue-600 hover:underline">Create an order</Link></div>
            ) : (
              <div className="divide-y">
                {deliveryTimeline.map((o) => {
                  const { label: dtLabel, urgent, overdue } = fmtDuration(msUntil(o.requestedDeliveryTime));
                  void tick; // force re-render on tick
                  const conflictsInOrder = (o.lines ?? []).filter((l) => l.leadTimeConflict).length;
                  const rowBg = overdue ? 'bg-red-50' : urgent ? 'bg-orange-50' : '';
                  const timeColor = overdue ? 'text-red-600 font-semibold' : urgent ? 'text-orange-600 font-semibold' : 'text-gray-500';
                  return (
                    <div key={o.orderId} className={`px-5 py-3 flex items-center gap-3 ${rowBg}`}>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium truncate">{o.customerName}</span>
                          {(o.rushOrder || o.isRushOrder) && <span className="text-orange-500 text-xs font-bold">⚡ RUSH</span>}
                          {conflictsInOrder > 0 && (
                            <span className="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700">
                              {conflictsInOrder} conflict{conflictsInOrder > 1 ? 's' : ''}
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-gray-400 mt-0.5">
                          {new Date(o.requestedDeliveryTime).toLocaleString()} · {fmtMoney(o.totalAmount ?? 0)}
                        </div>
                      </div>
                      <div className={`text-xs ${timeColor} text-right whitespace-nowrap`}>{dtLabel}</div>
                      <Badge status={o.status} />
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        </div>

        {/* Issues panel */}
        <div className="space-y-4">
          <SectionTitle>Issues Detected</SectionTitle>
          <Card>
            {issues.length === 0 ? (
              <div className="px-5 py-10 text-center text-sm text-gray-400">✅ No issues detected</div>
            ) : (
              <div className="divide-y">
                {issues.map((iss, i) => {
                  const dot = iss.level === 'error' ? '🔴' : iss.level === 'warn' ? '🟡' : '🔵';
                  return (
                    <div key={i} className="px-4 py-3 flex gap-2 text-sm">
                      <span className="mt-0.5 shrink-0">{dot}</span>
                      <span className="text-gray-700">{iss.msg}</span>
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* Production Floor */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <SectionTitle>Production Floor</SectionTitle>
          <Link href="/production-plans" className="text-xs text-blue-600 hover:underline">View all →</Link>
        </div>
        {floorPlans.length === 0 ? (
          <Card>
            <div className="py-10 text-center text-sm text-gray-400">No active production plans. <Link href="/production-plans" className="text-blue-600 hover:underline">Create one</Link></div>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {floorPlans.map((p) => {
              const total = p.workOrders?.length ?? 0;
              const done = p.workOrders?.filter((w) => w.status === 'COMPLETED').length ?? 0;
              const inProg = p.workOrders?.filter((w) => w.status === 'IN_PROGRESS' || w.status === 'STARTED').length ?? 0;
              const pct = total > 0 ? Math.round(((done + inProg * 0.5) / total) * 100) : 0;
              return (
                <Card key={p.planId} className="p-4">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-semibold">{p.planDate}</span>
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${shiftColor[p.shift] ?? 'bg-gray-100 text-gray-700'}`}>{p.shift}</span>
                  </div>
                  <div className="text-xs text-gray-500 mb-3">{done}/{total} work orders complete · {inProg} in progress</div>
                  <div className="w-full bg-gray-100 rounded-full h-2 mb-3">
                    <div
                      className={`h-2 rounded-full transition-all ${p.status === 'IN_PROGRESS' ? 'bg-blue-500' : p.status === 'COMPLETED' ? 'bg-green-500' : 'bg-gray-300'}`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>
                  <Badge status={p.status} />
                </Card>
              );
            })}
          </div>
        )}
      </div>

      {/* Quick-start guide if empty */}
      {deptCount === 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-xl p-5">
          <h3 className="font-semibold text-blue-800 mb-2">🚀 Getting started</h3>
          <ol className="text-sm text-blue-700 space-y-1 list-decimal list-inside">
            <li><Link href="/departments" className="underline hover:text-blue-900">Create a Department</Link> (e.g. Bakery, Confectionery)</li>
            <li><Link href="/products" className="underline hover:text-blue-900">Add Products</Link> (e.g. White Bread 500g)</li>
            <li><Link href="/recipes" className="underline hover:text-blue-900">Create a Recipe</Link> and activate it</li>
            <li><Link href="/orders" className="underline hover:text-blue-900">Place and confirm an Order</Link></li>
            <li><Link href="/production-plans" className="underline hover:text-blue-900">Create a Production Plan</Link>, generate work orders, and run the production workflow</li>
          </ol>
        </div>
      )}
    </div>
  );
}
