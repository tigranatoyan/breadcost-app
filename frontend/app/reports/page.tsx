'use client';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Spinner, Badge } from '@/components/ui';
import { useT } from '@/lib/i18n';

// ─── types ────────────────────────────────────────────────────────────────────

interface OrderLine {
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
  orderPlacedAt: string;
  lines: OrderLine[];
}

interface StockPosition {
  id: string;
  itemId: string;
  siteId: string;
  locationId: string;
  lotId: string;
  onHandQty: number;
  uom: string;
  avgUnitCost: number;
  valuationAmount: number;
}

interface StockItem {
  itemId: string;
  name: string;
  type: string;
  baseUom: string;
  minStockThreshold: number;
}

interface WorkOrderSummary {
  status: string;
}

interface RevenueSummary {
  today: number;
  week: number;
  month: number;
  allTime: number;
  currency: string;
}

interface TopProduct {
  productId: string;
  productName: string;
  totalQty: number;
  totalRevenue: number;
  orderCount: number;
}

interface Plan {
  planId: string;
  planDate: string;
  shift: string;
  status: string;
  workOrders: WorkOrderSummary[];
}

// ─── helpers ──────────────────────────────────────────────────────────────────

function fmt2(n: number) {
  return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fmtMoney(n: number) {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(2)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return fmt2(n);
}

const STATUS_ORDER = ['DRAFT', 'CONFIRMED', 'IN_PRODUCTION', 'READY', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

function pct(part: number, total: number) {
  if (!total) return 0;
  return Math.round((part / total) * 100);
}

function StatBox({ label, value, sub, color = 'text-gray-800' }: { label: string; value: string | number; sub?: string; color?: string }) {
  return (
    <div className="bg-white border rounded-xl p-5 shadow-sm">
      <div className={`text-3xl font-bold ${color}`}>{value}</div>
      <div className="text-sm text-gray-600 mt-0.5 font-medium">{label}</div>
      {sub && <div className="text-xs text-gray-400 mt-0.5">{sub}</div>}
    </div>
  );
}

// ─── Orders Report ────────────────────────────────────────────────────────────

function OrdersReport({ orders }: { orders: Order[] }) {
  const t = useT();
  const total = orders.length;
  const delivered = orders.filter((o) => o.status === 'DELIVERED').length;
  const cancelled = orders.filter((o) => o.status === 'CANCELLED').length;
  const active = orders.filter((o) => !['DELIVERED', 'CANCELLED'].includes(o.status)).length;
  const revenue = orders
    .filter((o) => !['CANCELLED', 'DRAFT'].includes(o.status))
    .reduce((s, o) => s + (o.totalAmount ?? 0), 0);
  const rushCount = orders.filter((o) => o.rushOrder || o.isRushOrder).length;

  // revenue by status
  const byStatus = STATUS_ORDER.map((s) => {
    const group = orders.filter((o) => o.status === s);
    return { status: s, count: group.length, revenue: group.reduce((sum, o) => sum + (o.totalAmount ?? 0), 0) };
  }).filter((r) => r.count > 0);

  // top customers
  const custMap: Record<string, { count: number; revenue: number }> = {};
  orders.forEach((o) => {
    if (!custMap[o.customerName]) custMap[o.customerName] = { count: 0, revenue: 0 };
    custMap[o.customerName].count++;
    if (!['CANCELLED'].includes(o.status)) custMap[o.customerName].revenue += o.totalAmount ?? 0;
  });
  const topCustomers = Object.entries(custMap)
    .sort((a, b) => b[1].revenue - a[1].revenue)
    .slice(0, 10);

  return (
    <div className="space-y-6">
      {/* KPIs */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <StatBox label={t('reports.totalOrders')} value={total} />
        <StatBox label={t('reports.activeOrders')} value={active} color="text-blue-600" />
        <StatBox label={t('reports.delivered')} value={delivered} color="text-green-600" sub={t('reports.completionPct', { pct: pct(delivered, total) })} />
        <StatBox label={t('reports.cancelled')} value={cancelled} color="text-red-500" />
        <StatBox label={t('reports.rushOrders')} value={rushCount} color="text-orange-500" sub={t('reports.ofTotal', { pct: pct(rushCount, total) })} />
        <StatBox label={t('reports.recordedRevenue')} value={fmtMoney(revenue)} color="text-emerald-600" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Revenue by status */}
        <div>
          <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{t('reports.revenueByStatus')}</h3>
          <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  {[t('reports.cols.status'), t('reports.cols.orders'), t('reports.cols.revenue'), '%'].map((h) => (
                    <th key={h} className="px-4 py-2.5 text-left text-xs font-semibold text-gray-500 uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y">
                {byStatus.map((r) => (
                  <tr key={r.status} className="hover:bg-gray-50">
                    <td className="px-4 py-2.5"><Badge status={r.status} /></td>
                    <td className="px-4 py-2.5 font-medium">{r.count}</td>
                    <td className="px-4 py-2.5">{fmtMoney(r.revenue)}</td>
                    <td className="px-4 py-2.5 text-gray-400 text-xs">{pct(r.count, total)}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Top customers */}
        <div>
          <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{t('reports.topCustomers')}</h3>
          <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
            {topCustomers.length === 0 ? (
              <div className="py-8 text-center text-sm text-gray-400">{t('reports.noCustomerData')}</div>
            ) : (
              <table className="min-w-full text-sm">
                <thead className="bg-gray-50 border-b">
                  <tr>
                    {[t('reports.cols.customer'), t('reports.cols.orders'), t('reports.cols.revenue')].map((h) => (
                      <th key={h} className="px-4 py-2.5 text-left text-xs font-semibold text-gray-500 uppercase">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {topCustomers.map(([name, data], i) => (
                    <tr key={name} className="hover:bg-gray-50">
                      <td className="px-4 py-2.5 flex items-center gap-2">
                        <span className="text-xs text-gray-400 w-4">{i + 1}.</span>
                        <span className="font-medium truncate max-w-xs">{name}</span>
                      </td>
                      <td className="px-4 py-2.5">{data.count}</td>
                      <td className="px-4 py-2.5 font-semibold">{fmtMoney(data.revenue)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      {/* Order listing summary */}
      <div>
        <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{t('reports.allOrders')}</h3>
        <div className="bg-white border rounded-xl shadow-sm overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                {[t('reports.cols.orderId'), t('reports.cols.customer'), t('reports.cols.status'), t('reports.cols.deliveryDate'), t('reports.cols.amount'), t('reports.cols.lines')].map((h) => (
                  <th key={h} className="px-4 py-2.5 text-left text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y">
              {[...orders].sort((a, b) => new Date(b.orderPlacedAt || 0).getTime() - new Date(a.orderPlacedAt || 0).getTime()).map((o) => (
                <tr key={o.orderId} className="hover:bg-gray-50">
                  <td className="px-4 py-2.5 font-mono text-xs text-gray-500">{o.orderId.slice(0, 8).toUpperCase()}</td>
                  <td className="px-4 py-2.5 font-medium">
                    {o.customerName}
                    {(o.rushOrder || o.isRushOrder) && <span className="ml-1 text-orange-500 text-xs">⚡</span>}
                  </td>
                  <td className="px-4 py-2.5"><Badge status={o.status} /></td>
                  <td className="px-4 py-2.5 text-gray-500 text-xs whitespace-nowrap">
                    {o.requestedDeliveryTime ? new Date(o.requestedDeliveryTime).toLocaleDateString() : '—'}
                  </td>
                  <td className="px-4 py-2.5 font-semibold">{fmtMoney(o.totalAmount ?? 0)}</td>
                  <td className="px-4 py-2.5 text-gray-400">{(o.lines ?? []).length}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {orders.length === 0 && (
            <div className="py-8 text-center text-sm text-gray-400">{t('reports.noOrdersFound')}</div>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── Inventory Report ─────────────────────────────────────────────────────────

function InventoryReport({ positions, items }: { positions: StockPosition[]; items: StockItem[] }) {
  const t = useT();
  const itemMap: Record<string, StockItem> = {};
  items.forEach((i) => { itemMap[i.itemId] = i; });

  const totalValue = positions.reduce((s, p) => s + (p.valuationAmount ?? 0), 0);
  const belowThreshold = positions.filter((p) => {
    const thr = itemMap[p.itemId]?.minStockThreshold ?? 0;
    return thr > 0 && p.onHandQty < thr;
  });

  // By type
  const byType: Record<string, { count: number; value: number }> = {};
  positions.forEach((p) => {
    const t = itemMap[p.itemId]?.type ?? 'UNKNOWN';
    if (!byType[t]) byType[t] = { count: 0, value: 0 };
    byType[t].count++;
    byType[t].value += p.valuationAmount ?? 0;
  });

  const TYPE_BADGE: Record<string, string> = {
    INGREDIENT: 'bg-blue-100 text-blue-700',
    PACKAGING: 'bg-purple-100 text-purple-700',
    FG: 'bg-green-100 text-green-700',
    BYPRODUCT: 'bg-yellow-100 text-yellow-700',
    WIP: 'bg-orange-100 text-orange-700',
  };

  return (
    <div className="space-y-6">
      {/* KPIs */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatBox label={t('reports.totalPositions')} value={positions.length} />
        <StatBox label={t('reports.totalStockValue')} value={fmtMoney(totalValue)} color="text-emerald-600" />
        <StatBox label={t('reports.uniqueItems')} value={items.length} />
        <StatBox label={t('reports.lowStockAlerts')} value={belowThreshold.length} color={belowThreshold.length > 0 ? 'text-red-500' : 'text-gray-800'} />
      </div>

      {/* Breakdown by type */}
      {Object.keys(byType).length > 0 && (
        <div>
          <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{t('reports.valuationByType')}</h3>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
            {Object.entries(byType).sort((a, b) => b[1].value - a[1].value).map(([type, data]) => (
              <div key={type} className="bg-white border rounded-xl p-4 shadow-sm">
                <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium mb-2 ${TYPE_BADGE[type] ?? 'bg-gray-100 text-gray-600'}`}>{type}</span>
                <div className="text-xl font-bold text-gray-800">{fmtMoney(data.value)}</div>
                <div className="text-xs text-gray-400">{data.count} position{data.count !== 1 ? 's' : ''}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Low stock items */}
      {belowThreshold.length > 0 && (
        <div>
          <h3 className="text-sm font-semibold text-red-500 uppercase tracking-wide mb-3">{t('reports.lowStockAlertsTitle')}</h3>
          <div className="bg-red-50 border border-red-200 rounded-xl overflow-hidden">
            <table className="min-w-full text-sm">
              <thead className="bg-red-100 border-b border-red-200">
                <tr>
                  {[t('reports.cols.item'), t('reports.cols.type'), t('reports.onHand'), t('reports.minThreshold'), t('reports.deficit')].map((h) => (
                    <th key={h} className="px-4 py-2.5 text-left text-xs font-semibold text-red-700 uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-red-100">
                {belowThreshold.map((p) => {
                  const item = itemMap[p.itemId];
                  const deficit = (item?.minStockThreshold ?? 0) - p.onHandQty;
                  return (
                    <tr key={p.id}>
                      <td className="px-4 py-2.5 font-medium">{item?.name ?? p.itemId}</td>
                      <td className="px-4 py-2.5">
                        <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${TYPE_BADGE[item?.type ?? ''] ?? 'bg-gray-100 text-gray-600'}`}>
                          {item?.type ?? '—'}
                        </span>
                      </td>
                      <td className="px-4 py-2.5 font-semibold text-red-600">{fmt2(p.onHandQty)} {p.uom}</td>
                      <td className="px-4 py-2.5 text-gray-600">{fmt2(item?.minStockThreshold ?? 0)} {p.uom}</td>
                      <td className="px-4 py-2.5 font-semibold text-red-700">{fmt2(deficit)} {p.uom}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Full positions table */}
      <div>
        <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{t('reports.stockPositionsDetail')}</h3>
        <div className="bg-white border rounded-xl shadow-sm overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                {[t('reports.cols.item'), t('reports.cols.type'), t('reports.cols.site'), t('reports.cols.location'), t('reports.cols.lot'), t('reports.onHand'), t('reports.cols.avgCost'), t('reports.cols.totalValue')].map((h) => (
                  <th key={h} className="px-4 py-2.5 text-left text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y">
              {positions.length === 0 ? (
                <tr><td colSpan={8} className="px-4 py-8 text-center text-gray-400">{t('reports.noStockPositions')}</td></tr>
              ) : (
                positions.map((p) => {
                  const item = itemMap[p.itemId];
                  return (
                    <tr key={p.id} className="hover:bg-gray-50">
                      <td className="px-4 py-2.5 font-medium">{item?.name ?? p.itemId}</td>
                      <td className="px-4 py-2.5">
                        {item?.type && (
                          <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${TYPE_BADGE[item.type] ?? 'bg-gray-100 text-gray-600'}`}>
                            {item.type}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-2.5 text-gray-500">{p.siteId}</td>
                      <td className="px-4 py-2.5 text-gray-500">{p.locationId}</td>
                      <td className="px-4 py-2.5 font-mono text-xs text-gray-400">{p.lotId ? p.lotId.slice(0, 8) + '…' : '—'}</td>
                      <td className="px-4 py-2.5 font-semibold">{fmt2(p.onHandQty)} <span className="text-gray-400 font-normal text-xs">{p.uom}</span></td>
                      <td className="px-4 py-2.5 text-gray-600">{fmt2(p.avgUnitCost)}</td>
                      <td className="px-4 py-2.5 font-semibold">{fmtMoney(p.valuationAmount)}</td>
                    </tr>
                  );
                })
              )}
            </tbody>
            {positions.length > 0 && (
              <tfoot className="border-t bg-gray-50">
                <tr>
                  <td colSpan={7} className="px-4 py-2 text-right text-xs font-semibold text-gray-500 uppercase">{t('common.total')}</td>
                  <td className="px-4 py-2 font-bold">{fmtMoney(totalValue)}</td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      </div>
    </div>
  );
}

// ─── Production Report ────────────────────────────────────────────────────────

function ProductionReport({ plans }: { plans: Plan[] }) {
  const t = useT();
  const total = plans.length;
  const completed = plans.filter((p) => p.status === 'COMPLETED').length;
  const inProgress = plans.filter((p) => p.status === 'IN_PROGRESS').length;
  const draft = plans.filter((p) => p.status === 'DRAFT').length;
  const cancelled = plans.filter((p) => p.status === 'CANCELLED').length;

  const allWOs = plans.flatMap((p) => p.workOrders ?? []);
  const woTotal = allWOs.length;
  const woCompleted = allWOs.filter((w) => w.status === 'COMPLETED').length;
  const woInProgress = allWOs.filter((w) => ['STARTED', 'IN_PROGRESS'].includes(w.status)).length;
  const woCancelled = allWOs.filter((w) => w.status === 'CANCELLED').length;

  const shiftColor: Record<string, string> = {
    MORNING: 'bg-yellow-100 text-yellow-800',
    AFTERNOON: 'bg-orange-100 text-orange-800',
    NIGHT: 'bg-indigo-100 text-indigo-800',
  };

  // plan status breakdown
  const planByStatus = [
    { status: 'DRAFT', count: draft, color: 'bg-gray-200' },
    { status: 'PUBLISHED', count: plans.filter((p) => p.status === 'PUBLISHED').length, color: 'bg-purple-400' },
    { status: 'IN_PROGRESS', count: inProgress, color: 'bg-blue-400' },
    { status: 'COMPLETED', count: completed, color: 'bg-green-400' },
    { status: 'CANCELLED', count: cancelled, color: 'bg-red-300' },
  ].filter((r) => r.count > 0);

  return (
    <div className="space-y-6">
      {/* KPIs */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatBox label={t('reports.productionPlans')} value={total} />
        <StatBox label={t('reports.completedPlans')} value={completed} color="text-green-600" sub={t('reports.completionRate', { pct: pct(completed, total) })} />
        <StatBox label={t('reports.workOrdersTotal')} value={woTotal} />
        <StatBox label={t('reports.wosCompleted')} value={woCompleted} color="text-green-600" sub={t('reports.completionPct', { pct: pct(woCompleted, woTotal) })} />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Plan status breakdown */}
        <div>
          <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{t('reports.plansByStatus')}</h3>
          <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  {[t('reports.cols.status'), t('reports.productionPlans'), t('reports.cols.pctOfTotal')].map((h) => (
                    <th key={h} className="px-4 py-2.5 text-left text-xs font-semibold text-gray-500 uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y">
                {planByStatus.map((r) => (
                  <tr key={r.status} className="hover:bg-gray-50">
                    <td className="px-4 py-2.5"><Badge status={r.status} /></td>
                    <td className="px-4 py-2.5 font-medium">{r.count}</td>
                    <td className="px-4 py-2.5">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 bg-gray-100 rounded-full h-2 w-24">
                          <div className={`h-2 rounded-full ${r.color}`} style={{ width: `${pct(r.count, total)}%` }} />
                        </div>
                        <span className="text-xs text-gray-500 w-8">{pct(r.count, total)}%</span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* WO status breakdown */}
        <div>
          <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{t('reports.woSummary')}</h3>
          <div className="bg-white border rounded-xl shadow-sm p-5 space-y-3">
            {[
              { label: t('common.total'), value: woTotal, color: 'bg-gray-300' },
              { label: t('reports.inProgress'), value: woInProgress, color: 'bg-blue-400' },
              { label: t('reports.cols.completed'), value: woCompleted, color: 'bg-green-400' },
              { label: t('reports.cancelled'), value: woCancelled, color: 'bg-red-300' },
              { label: t('reports.pending'), value: woTotal - woCompleted - woInProgress - woCancelled, color: 'bg-gray-200' },
            ].map((row) => (
              <div key={row.label} className="flex items-center gap-3 text-sm">
                <div className={`w-3 h-3 rounded-full shrink-0 ${row.color}`} />
                <span className="text-gray-600 w-24">{row.label}</span>
                <span className="font-semibold">{row.value}</span>
                {woTotal > 0 && (
                  <div className="flex-1 bg-gray-100 rounded-full h-1.5 ml-2">
                    <div className={`h-1.5 rounded-full ${row.color}`} style={{ width: `${pct(row.value, woTotal)}%` }} />
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Plans table */}
      <div>
        <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">{t('reports.allProductionPlans')}</h3>
        <div className="bg-white border rounded-xl shadow-sm overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                {[t('reports.date'), t('reports.shift'), t('reports.cols.status'), t('reports.cols.workOrders'), t('reports.cols.completed'), t('reports.progress')].map((h) => (
                  <th key={h} className="px-4 py-2.5 text-left text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y">
              {plans.length === 0 ? (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">{t('reports.noProductionPlans')}</td></tr>
              ) : (
                [...plans]
                  .sort((a, b) => b.planDate.localeCompare(a.planDate))
                  .map((p) => {
                    const wos = p.workOrders ?? [];
                    const done = wos.filter((w) => w.status === 'COMPLETED').length;
                    const ppct = pct(done, wos.length);
                    return (
                      <tr key={p.planId} className="hover:bg-gray-50">
                        <td className="px-4 py-2.5 font-medium">{p.planDate}</td>
                        <td className="px-4 py-2.5">
                          <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${shiftColor[p.shift] ?? 'bg-gray-100 text-gray-600'}`}>
                            {p.shift}
                          </span>
                        </td>
                        <td className="px-4 py-2.5"><Badge status={p.status} /></td>
                        <td className="px-4 py-2.5">{wos.length}</td>
                        <td className="px-4 py-2.5 text-green-600 font-medium">{done}</td>
                        <td className="px-4 py-2.5">
                          <div className="flex items-center gap-2">
                            <div className="w-20 bg-gray-100 rounded-full h-2">
                              <div
                                className={`h-2 rounded-full ${p.status === 'COMPLETED' ? 'bg-green-400' : 'bg-blue-400'}`}
                                style={{ width: `${ppct}%` }}
                              />
                            </div>
                            <span className="text-xs text-gray-400 w-8">{ppct}%</span>
                          </div>
                        </td>
                      </tr>
                    );
                  })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

// ─── page ─────────────────────────────────────────────────────────────────────

type ReportTab = 'orders' | 'inventory' | 'production' | 'revenue';

export default function ReportsPage() {
  const t = useT();
  const [tab, setTab] = useState<ReportTab>('orders');
  const [orders, setOrders] = useState<Order[]>([]);
  const [positions, setPositions] = useState<StockPosition[]>([]);
  const [stockItems, setStockItems] = useState<StockItem[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [revenue, setRevenue] = useState<RevenueSummary | null>(null);
  const [topProducts, setTopProducts] = useState<TopProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lastRefresh, setLastRefresh] = useState<Date>(new Date());

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [o, pos, itms, p, rev, top] = await Promise.all([
        apiFetch<Order[]>(`/v1/orders?tenantId=${TENANT_ID}`),
        apiFetch<StockPosition[]>(`/v1/inventory/positions?tenantId=${TENANT_ID}`).catch(() => [] as StockPosition[]),
        apiFetch<StockItem[]>(`/v1/items?tenantId=${TENANT_ID}`).catch(() => [] as StockItem[]),
        apiFetch<Plan[]>(`/v1/production-plans?tenantId=${TENANT_ID}`),
        apiFetch<RevenueSummary>(`/v1/reports/revenue-summary?tenantId=${TENANT_ID}`).catch(() => null),
        apiFetch<TopProduct[]>(`/v1/reports/top-products?tenantId=${TENANT_ID}&limit=8`).catch(() => [] as TopProduct[]),
      ]);
      setOrders(o);
      setPositions(pos);
      setStockItems(itms);
      setPlans(p);
      setRevenue(rev);
      setTopProducts(top);
      setLastRefresh(new Date());
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // ── Revenue summary banner (always visible) ──────────────────────────────
  const revBanner = revenue ? (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
      {[
        { label: t('reports.today'), value: revenue.today },
        { label: t('reports.last7Days'), value: revenue.week },
        { label: t('reports.last30Days'), value: revenue.month },
        { label: t('reports.allTime'), value: revenue.allTime },
      ].map(({ label, value }) => (
        <div key={label} className="bg-white border rounded-xl p-4 shadow-sm">
          <div className="text-xs text-gray-400 mb-1">{label}</div>
          <div className="text-xl font-bold text-gray-800">{fmtMoney(value)}</div>
          <div className="text-xs text-gray-400">{revenue.currency}</div>
        </div>
      ))}
    </div>
  ) : null;

  // ── Top products sidebar ──────────────────────────────────────────────────
  const topProductsPanel = topProducts.length > 0 ? (
    <div className="mt-6 bg-white border rounded-xl shadow-sm p-5">
      <div className="text-sm font-semibold text-gray-700 mb-3">{t('reports.topProductsWeek')}</div>
      <table className="w-full text-xs">
        <thead>
          <tr className="text-gray-400 border-b">
            <th className="text-left py-1 pr-4">#</th>
            <th className="text-left py-1 pr-4">Product</th>
            <th className="text-right py-1 pr-4">Qty</th>
            <th className="text-right py-1">Revenue</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {topProducts.map((tp, i) => (
            <tr key={tp.productId}>
              <td className="py-1.5 pr-4 text-gray-400">{i + 1}</td>
              <td className="py-1.5 pr-4 font-medium">{tp.productName}</td>
              <td className="py-1.5 pr-4 text-right">{tp.totalQty.toFixed(1)}</td>
              <td className="py-1.5 text-right text-green-700 font-semibold">{fmtMoney(tp.totalRevenue)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  ) : null;

  const tabs: { id: ReportTab; label: string; count?: number }[] = [
    { id: 'orders', label: t('reports.ordersTab'), count: orders.length },
    { id: 'inventory', label: t('reports.inventoryTab'), count: positions.length },
    { id: 'production', label: t('reports.productionTab'), count: plans.length },
    { id: 'revenue', label: t('reports.revenueTab') },
  ];

  return (
    <div className="max-w-6xl">
      <div className="flex items-center justify-between mb-5">
        <div>
          <h1 className="text-2xl font-semibold">{t('reports.title')}</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            {t('reports.lastRefreshed', { time: lastRefresh.toLocaleTimeString() })}
          </p>
        </div>
        <button className="btn-secondary" onClick={load} disabled={loading}>
          {loading ? t('common.loading') : `↻ ${t('common.refresh')}`}
        </button>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {revBanner}

      {/* Tab bar */}
      <div className="flex gap-1 mb-6 bg-gray-100 rounded-xl p-1 w-fit">
        {tabs.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`px-5 py-2 rounded-lg text-sm font-medium transition-colors ${
              tab === t.id ? 'bg-white shadow text-gray-900' : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {t.label}
            {t.count !== undefined && (
              <span className="ml-1.5 bg-gray-200 text-gray-600 text-xs px-1.5 py-0.5 rounded-full">{t.count}</span>
            )}
          </button>
        ))}
      </div>

      {loading ? (
        <Spinner />
      ) : (
        <>
          {tab === 'orders' && <OrdersReport orders={orders} />}
          {tab === 'inventory' && <InventoryReport positions={positions} items={stockItems} />}
          {tab === 'production' && <ProductionReport plans={plans} />}
          {tab === 'revenue' && (
            <div>
              {topProductsPanel ?? (
                <div className="text-sm text-gray-400 py-8 text-center">{t('reports.noSalesData')}</div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
