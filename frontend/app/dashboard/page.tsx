'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { PageSkeleton } from '@/components/ui';
import { Badge, Card, StatCard, Progress, Button, SectionTitle, Table } from '@/components/design-system';
import { CircleDollarSign, ShoppingCart, Factory, Warehouse, AlertTriangle, RefreshCw, Settings, DollarSign, TrendingUp, Users, Package } from 'lucide-react';
import Link from 'next/link';
import { useI18n, useDateTimeFmt, BCP47 } from '@/lib/i18n';

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
  if (n >= 1_000_000) return `֏${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `֏${(n / 1_000).toFixed(1)}K`;
  return `֏${n.toFixed(0)}`;
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

interface StockAlert {
  itemId: string;
  itemName: string;
  onHandQty: number;
  minThreshold: number;
  uom: string;
  severity: 'LOW' | 'CRITICAL';
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

interface RevenueSummary {
  today: number;
  week: number;
  month: number;
  allTime: number;
  currency: string;
}

interface StatCardData {
  label: string;
  value: number | string;
  sub?: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  accent?: boolean;
}

// ─── sub-components ───────────────────────────────────────────────────────────
function KpiCard({ label, value, sub, href, icon: Icon }: StatCardData) {
  return (
    <Link href={href} className="block hover:shadow-md transition-shadow rounded-2xl">
      <StatCard icon={Icon} label={label} value={value} hint={sub} />
    </Link>
  );
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return <h2 className="text-xs font-semibold uppercase tracking-[0.22em] text-gray-500 mb-3">{children}</h2>;
}

// ─── widget config ────────────────────────────────────────────────────────────
const WIDGET_IDS = ['kpi', 'todayOrders', 'activePlans', 'nextEvent', 'deliveryTimeline', 'issues', 'stockAlerts', 'revenue', 'productionFloor', 'financialAnalytics', 'topProducts', 'customerInsights'] as const;
type WidgetId = typeof WIDGET_IDS[number];
const WIDGET_STORAGE_KEY = 'bc_dashboard_widgets';

function getVisibleWidgets(): Set<WidgetId> {
  if (typeof window === 'undefined') return new Set(WIDGET_IDS);
  try {
    const raw = localStorage.getItem(WIDGET_STORAGE_KEY);
    if (!raw) return new Set(WIDGET_IDS);
    return new Set(JSON.parse(raw) as WidgetId[]);
  } catch { return new Set(WIDGET_IDS); }
}

function saveVisibleWidgets(ids: Set<WidgetId>) {
  localStorage.setItem(WIDGET_STORAGE_KEY, JSON.stringify([...ids]));
}

// ─── main component ───────────────────────────────────────────────────────────
export default function DashboardPage() {
  const { t, locale } = useI18n();
  const fmtDateTime = useDateTimeFmt();
  const [loading, setLoading] = useState(true);
  const [deptCount, setDeptCount] = useState(0);
  const [, setProductCount] = useState(0);
  const [orders, setOrders] = useState<Order[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [positions, setPositions] = useState<StockPosition[]>([]);
  const [stockItems, setStockItems] = useState<StockItem[]>([]);
  const [serverAlerts, setServerAlerts] = useState<StockAlert[]>([]);
  const [revenue, setRevenue] = useState<RevenueSummary | null>(null);
  const [topProducts, setTopProducts] = useState<Array<Record<string, unknown>>>([]);
  const [analyticsKpis, setAnalyticsKpis] = useState<Record<string, string>>({});
  const [tick, setTick] = useState(0);
  const [visibleWidgets, setVisibleWidgets] = useState<Set<WidgetId>>(() => getVisibleWidgets());
  const [showConfig, setShowConfig] = useState(false);
  const today = new Date().toISOString().substring(0, 10);

  const toggleWidget = (id: WidgetId) => {
    setVisibleWidgets((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      saveVisibleWidgets(next);
      return next;
    });
  };
  const show = (id: WidgetId) => visibleWidgets.has(id);

  const load = useCallback(() => {
    Promise.all([
      apiFetch<unknown[]>(`/v1/departments?tenantId=${TENANT_ID}`).then((d) => setDeptCount(d.length)),
      apiFetch<unknown[]>(`/v1/products?tenantId=${TENANT_ID}`).then((d) => setProductCount(d.length)),
      apiFetch<Order[]>(`/v1/orders?tenantId=${TENANT_ID}`).then(setOrders),
      apiFetch<Plan[]>(`/v1/production-plans?tenantId=${TENANT_ID}`).then(setPlans),
      apiFetch<StockPosition[]>(`/v1/inventory/positions?tenantId=${TENANT_ID}`).then(setPositions).catch(() => {}),
      apiFetch<StockItem[]>(`/v1/items?tenantId=${TENANT_ID}`).then(setStockItems).catch(() => {}),
      apiFetch<StockAlert[]>(`/v1/inventory/alerts?tenantId=${TENANT_ID}`).then(setServerAlerts).catch(() => {}),
      apiFetch<RevenueSummary>(`/v1/reports/revenue-summary?tenantId=${TENANT_ID}`).then(setRevenue).catch(() => {}),
      apiFetch<Array<Record<string, unknown>>>(`/v1/reports/top-products?tenantId=${TENANT_ID}&limit=8`).then(setTopProducts).catch(() => {}),
      (async () => {
        const keys = ['gross_margin_pct', 'avg_order_value', 'overdue_invoices', 'active_customers', 'order_frequency', 'customer_lifetime_value', 'disputed_invoice_rate'];
        const results: Record<string, string> = {};
        for (const key of keys) {
          try {
            const val = await apiFetch<Record<string, unknown>>(`/v2/reports/kpi?blockKey=${key}&tenantId=${TENANT_ID}`);
            results[key] = val?.value != null ? String(val.value) : '\u2014';
          } catch { results[key] = '\u2014'; }
        }
        setAnalyticsKpis(results);
      })(),
    ])
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { load(); }, [load]);

  // auto-refresh every 60s (BC-1505) + refresh countdowns every 30s
  useEffect(() => {
    const refreshId = setInterval(() => { load(); }, 60_000);
    const tickId = setInterval(() => setTick((t) => t + 1), 30_000);
    return () => { clearInterval(refreshId); clearInterval(tickId); };
  }, [load]);

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

  // BC-1807: Today's Orders widget data
  const todayOrders = orders.filter((o) => {
    const placed = (o as unknown as { orderPlacedAt?: string }).orderPlacedAt;
    const dt = placed || o.requestedDeliveryTime;
    return dt && dt.substring(0, 10) === today;
  });
  const todayOrderValue = todayOrders.reduce((s, o) => s + (o.totalAmount ?? 0), 0);

  // BC-1807: Active Plans widget data
  const activePlans = plans.filter((p) => ['IN_PROGRESS', 'PUBLISHED', 'APPROVED'].includes(p.status));
  const activePlanStatusBreakdown: Record<string, number> = {};
  for (const p of activePlans) {
    activePlanStatusBreakdown[p.status] = (activePlanStatusBreakdown[p.status] ?? 0) + 1;
  }

  // next big event
  const nextEvent: { label: string; detail: string; accent: string } | null = (() => {
    const nextDelivery = deliveryTimeline[0];
    if (nextDelivery) {
      const { label, overdue } = fmtDuration(msUntil(nextDelivery.requestedDeliveryTime));
      return {
        label: t('dashboard.nextDelivery', { customer: nextDelivery.customerName }),
        detail: `${label} · ${fmtMoney(nextDelivery.totalAmount ?? 0)}`,
        accent: overdue ? 'bg-red-700' : 'bg-slate-800',
      };
    }
    const nextPlan = plans.find((p) => p.status === 'DRAFT' || p.status === 'PUBLISHED');
    if (nextPlan) return {
      label: t('dashboard.nextProductionRun', { date: nextPlan.planDate }),
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
    if (ms < 0) issues.push({ level: 'error', msg: t('dashboard.overdue', { customer: o.customerName, duration: fmtDuration(ms).label }) });
  }
  const conflictCount = orders.flatMap((o) => o.lines ?? []).filter((l) => l.leadTimeConflict).length;
  if (conflictCount > 0) issues.push({ level: 'warn', msg: t('dashboard.leadTimeConflicts', { count: conflictCount }) });
  if (rushOrders.length > 0) issues.push({ level: 'warn', msg: t('dashboard.rushOrders', { count: rushOrders.length }) });
  const draftPlansNoWO = todayPlans.filter((p) => p.status === 'DRAFT' && (p.workOrders?.length ?? 0) === 0);
  if (draftPlansNoWO.length > 0) issues.push({ level: 'warn', msg: t('dashboard.draftPlansNoWo', { count: draftPlansNoWO.length }) });
  if (serverAlerts.length > 0) {
    const critical = serverAlerts.filter((a) => a.severity === 'CRITICAL');
    const low = serverAlerts.filter((a) => a.severity === 'LOW');
    if (critical.length > 0) issues.push({ level: 'error', msg: t('dashboard.criticalStock', { count: critical.length }) });
    if (low.length > 0) issues.push({ level: 'warn', msg: t('dashboard.lowStock', { count: low.length }) });
  } else if (lowStockPositions.length > 0) {
    issues.push({ level: 'warn', msg: t('dashboard.belowStockThreshold', { count: lowStockPositions.length }) });
  }
  const confirmedNoMatch = confirmedOrders.filter(
    (o) => !plans.some((p) => p.planDate === o.requestedDeliveryTime?.substring(0, 10))
  );
  if (confirmedNoMatch.length > 0) issues.push({ level: 'info', msg: t('dashboard.confirmedNoMatchPlan', { count: confirmedNoMatch.length }) });
  const cancelledCount = orders.filter((o) => o.status === 'CANCELLED').length;
  if (cancelledCount > 0) issues.push({ level: 'info', msg: t('dashboard.cancelledOnRecord', { count: cancelledCount }) });

  // kpi stats
  const stats: StatCardData[] = [
    { label: t('dashboard.runningRevenue'), value: fmtMoney(runningRevenue), sub: t('dashboard.activeOrders', { count: activeOrders.length }), href: '/orders', icon: CircleDollarSign },
    { label: t('dashboard.openOrders'), value: activeOrders.length, sub: criticalOrders.length > 0 ? t('dashboard.critical', { count: criticalOrders.length }) : t('dashboard.confirmed', { count: confirmedOrders.length }), href: '/orders', icon: ShoppingCart, accent: criticalOrders.length > 0 },
    { label: t('dashboard.todayPlans'), value: todayPlans.length, sub: activePlan ? t('dashboard.activeShift', { shift: activePlan.shift }) : t('dashboard.noneActive'), href: '/production-plans', icon: Factory },
    { label: t('dashboard.stockValue'), value: fmtMoney(totalStockValue), sub: serverAlerts.length > 0 ? t('dashboard.stockAlerts', { count: serverAlerts.length }) : t('dashboard.positions', { count: positions.length }), href: '/inventory', icon: Warehouse, accent: serverAlerts.length > 0 },
  ];

  // production floor progress
  const floorPlans = plans.filter((p) => ['DRAFT', 'PUBLISHED', 'IN_PROGRESS'].includes(p.status)).slice(0, 6);

  const shiftColor: Record<string, string> = {
    MORNING: 'bg-yellow-100 text-yellow-800',
    AFTERNOON: 'bg-orange-100 text-orange-800',
    NIGHT: 'bg-indigo-100 text-indigo-800',
  };

  if (loading) return <PageSkeleton />;

  return (
    <div className="max-w-[1800px] space-y-8">
      {/* Header */}
      <SectionTitle
        eyebrow={t('dashboard.overview') ?? 'Overview'}
        title={t('dashboard.title')}
        subtitle={new Date().toLocaleDateString(BCP47[locale], { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
        action={<div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={() => setShowConfig(!showConfig)}><Settings className="h-4 w-4" /></Button>
          <Button variant="secondary" size="sm" onClick={load}><RefreshCw className="h-4 w-4" /> {t('dashboard.refresh')}</Button>
        </div>}
      />

      {/* Widget config panel */}
      {showConfig && (
        <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm">
          <div className="text-sm font-semibold text-gray-700 mb-3">{t('dashboard.configTitle')}</div>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
            {WIDGET_IDS.map((id) => (
              <label key={id} className="flex items-center gap-2 text-sm cursor-pointer">
                <input type="checkbox" checked={visibleWidgets.has(id)} onChange={() => toggleWidget(id)} className="rounded" />
                {t(`dashboard.widget_${id}` as any)}
              </label>
            ))}
          </div>
        </div>
      )}

      {/* KPI row */}
      {show('kpi') && (
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {stats.map((s) => <KpiCard key={s.label} {...s} />)}
      </div>
      )}

      {/* BC-1807: Today's Orders + Active Plans widgets */}
      {(show('todayOrders') || show('activePlans')) && (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Today's Orders */}
        {show('todayOrders') && (
        <Card className="p-5" title={t('dashboard.todaysOrders')} action={
            <Link href="/orders" className="text-xs text-blue-600 hover:underline">{t('dashboard.view')}</Link>}>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <div className="text-3xl font-bold text-gray-800">{todayOrders.length}</div>
              <div className="text-xs text-gray-500">{t('dashboard.ordersToday')}</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-emerald-600">{fmtMoney(todayOrderValue)}</div>
              <div className="text-xs text-gray-500">{t('dashboard.todayValue')}</div>
            </div>
          </div>
          {todayOrders.length > 0 && (
            <div className="mt-3 divide-y max-h-32 overflow-y-auto">
              {todayOrders.slice(0, 5).map((o) => (
                <div key={o.orderId} className="flex items-center justify-between py-1.5 text-xs">
                  <span className="font-medium">{o.customerName}</span>
                  <div className="flex items-center gap-2 shrink-0">
                    <span className="text-gray-500">{fmtMoney(o.totalAmount ?? 0)}</span>
                    <Badge status={o.status} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
        )}

        {/* Active Plans */}
        {show('activePlans') && (
        <Card className="p-5" title={t('dashboard.activePlansWidget')} action={
            <Link href="/production-plans" className="text-xs text-blue-600 hover:underline">{t('dashboard.view')}</Link>}>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <div className="text-3xl font-bold text-gray-800">{activePlans.length}</div>
              <div className="text-xs text-gray-500">{t('dashboard.activePlansCount')}</div>
            </div>
            <div className="space-y-1">
              {Object.entries(activePlanStatusBreakdown).map(([status, count]) => (
                <div key={status} className="flex items-center gap-2 text-xs">
                  <Badge status={status} />
                  <span className="text-gray-600 font-medium">×{count}</span>
                </div>
              ))}
              {activePlans.length === 0 && <div className="text-xs text-gray-400">{t('dashboard.noneActive')}</div>}
            </div>
          </div>
          {activePlans.length > 0 && (
            <div className="mt-3 divide-y max-h-32 overflow-y-auto">
              {activePlans.slice(0, 5).map((p) => {
                const wos = p.workOrders?.length ?? 0;
                const done = p.workOrders?.filter((w) => w.status === 'COMPLETED').length ?? 0;
                return (
                  <div key={p.planId} className="flex items-center justify-between py-1.5 text-xs">
                    <span className="font-medium">{p.planDate} · {t(`productionPlans.shifts.${p.shift}` as any) || p.shift}</span>
                    <div className="flex items-center gap-2">
                      <span className="text-gray-500">{done}/{wos} WOs</span>
                      <Badge status={p.status} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </Card>
        )}
      </div>
      )}

      {/* Next big event banner */}
      {show('nextEvent') && nextEvent && (
        <div className={`${nextEvent.accent} text-white rounded-xl px-6 py-4 flex items-center justify-between`}>
          <div>
            <div className="text-xs font-semibold uppercase tracking-wide opacity-70 mb-0.5">{t('dashboard.nextEvent')}</div>
            <div className="text-lg font-bold">{nextEvent.label}</div>
            <div className="text-sm opacity-80 mt-0.5">{nextEvent.detail}</div>
          </div>
          <Link href="/orders" className="text-white underline text-sm opacity-80 hover:opacity-100 whitespace-nowrap ml-4">{t('dashboard.view')}</Link>
        </div>
      )}

      {(show('deliveryTimeline') || show('issues')) && (
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Delivery Timeline (spans 2 cols) */}
        {show('deliveryTimeline') && (
        <div className="lg:col-span-2 space-y-4">
          <SectionLabel>{t('dashboard.deliveryTimeline')}</SectionLabel>
          <Card>
            {deliveryTimeline.length === 0 ? (
              <div className="py-10 text-center text-sm text-gray-400">{t('dashboard.noDeliveries')} <Link href="/orders" className="text-blue-600 hover:underline">{t('dashboard.createAnOrder')}</Link></div>
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
                          <span className="text-sm font-medium">{o.customerName}</span>
                          {(o.rushOrder || o.isRushOrder) && <span className="text-orange-500 text-xs font-bold">⚡ RUSH</span>}
                          {conflictsInOrder > 0 && (
                            <span className="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700">
                              {conflictsInOrder} conflict{conflictsInOrder > 1 ? 's' : ''}
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-gray-400 mt-0.5">
                          {fmtDateTime(o.requestedDeliveryTime)} · {fmtMoney(o.totalAmount ?? 0)}
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
        )}

        {/* Issues panel */}
        {show('issues') && (
        <div className="space-y-4">
          <SectionLabel>{t('dashboard.issuesDetected')}</SectionLabel>
          <Card>
            {issues.length === 0 ? (
              <div className="px-5 py-10 text-center text-sm text-gray-400">{t('dashboard.noIssues')}</div>
            ) : (
              <div className="divide-y">
                {issues.map((iss, i) => {
                  const colors = iss.level === 'error' ? 'bg-red-50 text-red-600' : iss.level === 'warn' ? 'bg-amber-50 text-amber-600' : 'bg-blue-50 text-blue-600';
                  return (
                    <div key={i} className="px-4 py-3 flex gap-3 text-sm items-start">
                      <div className={`rounded-full p-1.5 shrink-0 ${colors}`}>
                        <AlertTriangle className="h-3.5 w-3.5" />
                      </div>
                      <span className="text-gray-700">{iss.msg}</span>
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        </div>
        )}
      </div>
      )}

      {/* Stock Alerts Widget (BC-1505) */}
      {show('stockAlerts') && serverAlerts.length > 0 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <SectionLabel>{t('dashboard.stockAlertsWidget')}</SectionLabel>
            <Link href="/inventory" className="text-xs text-blue-600 hover:underline">{t('common.viewAll')}</Link>
          </div>
          <Card>
            <div className="divide-y">
              {serverAlerts.map((a) => (
                <div key={a.itemId} className={`px-5 py-3 flex items-center gap-3 ${a.severity === 'CRITICAL' ? 'bg-red-50' : 'bg-yellow-50'}`}>
                  <div className={`rounded-full p-2 shrink-0 ${a.severity === 'CRITICAL' ? 'bg-red-50 text-red-600' : 'bg-amber-50 text-amber-600'}`}>
                    <AlertTriangle className="h-4 w-4" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium truncate">{a.itemName}</div>
                    <div className="text-xs text-gray-500 mt-0.5">
                      {t('dashboard.alertOnHand', { qty: a.onHandQty.toFixed(1), uom: a.uom })} · {t('dashboard.alertThreshold', { threshold: a.minThreshold.toFixed(1) })}
                    </div>
                  </div>
                  <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${a.severity === 'CRITICAL' ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700'}`}>
                    {a.severity}
                  </span>
                </div>
              ))}
            </div>
          </Card>
        </div>
      )}

      {/* Revenue Widget (BC-1604) */}
      {show('revenue') && revenue && (
        <div className="space-y-4">
          <SectionLabel>{t('dashboard.revenueWidget')}</SectionLabel>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {([
              { label: t('dashboard.revenueToday'), value: revenue.today },
              { label: t('dashboard.revenueWeek'), value: revenue.week },
              { label: t('dashboard.revenueMonth'), value: revenue.month },
            ] as const).map((r) => (
              <Card key={r.label} className="p-4 text-center">
                <div className="text-xs text-gray-400 uppercase tracking-wide mb-1">{r.label}</div>
                <div className="text-2xl font-bold text-gray-800">{fmtMoney(r.value)}</div>
                <div className="text-xs text-gray-400 mt-1">{revenue.currency || 'AMD'}</div>
              </Card>
            ))}
          </div>
        </div>
      )}

      {/* Financial Analytics (BC-309: merged from /analytics) */}
      {show('financialAnalytics') && (
        <div className="space-y-4">
          <SectionLabel>{t('dashboard.widget_financialAnalytics')}</SectionLabel>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <StatCard icon={DollarSign} label={t('analytics.grossMargin')} value={`${analyticsKpis.gross_margin_pct ?? '\u2014'}%`} />
            <StatCard icon={ShoppingCart} label={t('analytics.avgOrderValue')} value={analyticsKpis.avg_order_value ?? '\u2014'} />
            <StatCard icon={AlertTriangle} label={t('analytics.overdueInvoices')} value={analyticsKpis.overdue_invoices ?? '\u2014'} />
            <StatCard icon={TrendingUp} label={t('analytics.disputeRate')} value={`${analyticsKpis.disputed_invoice_rate ?? '\u2014'}%`} />
          </div>
        </div>
      )}

      {/* Customer Insights (BC-309: merged from /analytics) */}
      {show('customerInsights') && (
        <div className="space-y-4">
          <SectionLabel>{t('dashboard.widget_customerInsights')}</SectionLabel>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            <StatCard icon={Users} label={t('analytics.activeCustomers')} value={analyticsKpis.active_customers ?? '\u2014'} />
            <StatCard icon={ShoppingCart} label={t('analytics.orderFrequency')} value={`${analyticsKpis.order_frequency ?? '\u2014'}x`} />
            <StatCard icon={DollarSign} label={t('analytics.customerLtv')} value={analyticsKpis.customer_lifetime_value ?? '\u2014'} />
          </div>
        </div>
      )}

      {/* Top Products (BC-309: merged from /analytics) */}
      {show('topProducts') && topProducts.length > 0 && (
        <div className="space-y-4">
          <SectionLabel>{t('dashboard.widget_topProducts')}</SectionLabel>
          <Table
            cols={[t('analytics.productCol'), t('analytics.qtySold'), t('analytics.revenueCol'), t('analytics.ordersCol')]}
            rows={topProducts.map((p) => [
              String(p.name || p.productName || ''),
              String(p.totalQty ?? 0),
              Number(p.totalRevenue ?? 0).toLocaleString(),
              String(p.orderCount ?? 0),
            ])}
            empty={t('analytics.noProductData')}
          />
        </div>
      )}

      {/* Production Floor */}
      {show('productionFloor') && (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <SectionLabel>{t('dashboard.productionFloor')}</SectionLabel>
          <Link href="/floor" className="text-xs text-blue-600 hover:underline">{t('common.viewAll')}</Link>
        </div>
        {floorPlans.length === 0 ? (
          <Card>
            <div className="py-10 text-center text-sm text-gray-400">{t('dashboard.noActivePlans')} <Link href="/production-plans" className="text-blue-600 hover:underline">{t('dashboard.createOne')}</Link></div>
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
                    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${shiftColor[p.shift] ?? 'bg-gray-100 text-gray-700'}`}>{t(`productionPlans.shifts.${p.shift}` as any) || p.shift}</span>
                  </div>
                  <div className="text-xs text-gray-500 mb-3">{t('dashboard.workOrdersComplete', { done: String(done), total: String(total), inProgress: String(inProg) })}</div>
                  <Progress value={pct} />
                  <div className="mt-2" />
                  <Badge status={p.status} />
                </Card>
              );
            })}
          </div>
        )}
      </div>
      )}

      {/* Quick-start guide if empty */}
      {deptCount === 0 && (
        <div className="rounded-2xl border border-blue-200 bg-blue-50/50 p-6">
          <h3 className="font-semibold text-blue-900 text-lg mb-4">{t('dashboard.gettingStarted')}</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-5 gap-4">
            {[
              { step: 1, href: '/departments', label: t('dashboard.step1'), desc: t('dashboard.step1Example'), icon: '🏢' },
              { step: 2, href: '/products', label: t('dashboard.step2'), desc: t('dashboard.step2Example'), icon: '🍞' },
              { step: 3, href: '/recipes', label: t('dashboard.step3'), desc: t('dashboard.step3Extra'), icon: '📋' },
              { step: 4, href: '/orders', label: t('dashboard.step4'), desc: '', icon: '📦' },
              { step: 5, href: '/production-plans', label: t('dashboard.step5'), desc: t('dashboard.step5Extra'), icon: '🏭' },
            ].map(s => (
              <Link key={s.step} href={s.href}
                className="group flex flex-col rounded-xl border border-blue-200 bg-white p-4 transition hover:shadow-md hover:border-blue-400">
                <div className="flex items-center gap-2 mb-2">
                  <span className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">
                    {s.step}
                  </span>
                  <span className="text-lg">{s.icon}</span>
                </div>
                <span className="text-sm font-semibold text-gray-900 group-hover:text-blue-700 transition">{s.label}</span>
                {s.desc && <span className="mt-1 text-xs text-gray-500">{s.desc}</span>}
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
