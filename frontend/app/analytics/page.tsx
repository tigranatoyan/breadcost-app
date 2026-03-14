'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { PageSkeleton } from '@/components/ui';
import { SectionTitle, StatCard, Card, Table, Button } from '@/components/design-system';
import { useT } from '@/lib/i18n';
import {
  DollarSign, Users, ShoppingCart, TrendingUp,
  AlertTriangle, BarChart3, RefreshCw, Package,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell, Legend,
} from 'recharts';

interface KpiResult {
  blockKey: string;
  name: string;
  value: unknown;
  unit: string;
}

export default function AnalyticsPage() {
  const t = useT();
  const [loading, setLoading] = useState(true);
  const [revenue, setRevenue] = useState<Record<string, unknown>>({});
  const [topProducts, setTopProducts] = useState<Array<Record<string, unknown>>>([]);
  const [kpis, setKpis] = useState<KpiResult[]>([]);
  const [ordersSummary, setOrdersSummary] = useState<Record<string, unknown>>({});

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [rev, top, orders] = await Promise.all([
        apiFetch(`/v1/reports/revenue-summary?tenantId=${TENANT_ID}&period=month`),
        apiFetch(`/v1/reports/top-products?tenantId=${TENANT_ID}&limit=10`),
        apiFetch(`/v1/reports/orders-summary?tenantId=${TENANT_ID}`),
      ]);
      setRevenue(rev as Record<string, unknown>);
      setTopProducts(top as Array<Record<string, unknown>>);
      setOrdersSummary(orders as Record<string, unknown>);

      // Fetch KPI blocks
      const blockKeys = [
        'total_revenue', 'gross_margin_pct', 'avg_order_value',
        'active_customers', 'order_frequency', 'customer_lifetime_value',
        'overdue_invoices', 'disputed_invoice_rate', 'production_efficiency',
        'stock_turnover', 'delivery_completion_rate',
      ];
      const kpiResults: KpiResult[] = [];
      for (const key of blockKeys) {
        try {
          const val = await apiFetch(
            `/v2/reports/kpi?blockKey=${key}&tenantId=${TENANT_ID}`
          ) as Record<string, unknown>;
          kpiResults.push({ blockKey: key, name: key, value: val?.value ?? val, unit: String(val?.unit ?? '') });
        } catch {
          kpiResults.push({ blockKey: key, name: key, value: 'N/A', unit: '' });
        }
      }
      setKpis(kpiResults);
    } catch (e) {
      console.error('Analytics load failed', e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const kpiValue = (key: string) => {
    const k = kpis.find((x) => x.blockKey === key);
    if (!k || k.value === 'N/A') return '—';
    if (typeof k.value === 'number') return k.value.toLocaleString();
    return String(k.value);
  };

  if (loading) return <PageSkeleton />;

  return (
    <div className="space-y-8">
      <SectionTitle
        eyebrow="D1"
        title={t('nav.analytics')}
        subtitle={t('analytics.subtitle')}
        action={
          <Button variant="secondary" onClick={load}>
            <RefreshCw className="w-4 h-4 mr-1" /> {t('common.refresh')}
          </Button>
        }
      />

      {/* ── Revenue & Financial KPIs ──────────────────────────────────────── */}
      <Card title={t('analytics.financialOverview')}>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            icon={DollarSign}
            label={t('analytics.revenueMonth')}
            value={
              revenue.month !== null && revenue.month !== undefined
                ? `${Number(revenue.month).toLocaleString()} ${revenue.currency || ''}`
                : '—'
            }
          />
          <StatCard
            icon={TrendingUp}
            label={t('analytics.grossMargin')}
            value={`${kpiValue('gross_margin_pct')}%`}
          />
          <StatCard
            icon={ShoppingCart}
            label={t('analytics.avgOrderValue')}
            value={kpiValue('avg_order_value')}
          />
          <StatCard
            icon={AlertTriangle}
            label={t('analytics.overdueInvoices')}
            value={kpiValue('overdue_invoices')}
          />
        </div>
      </Card>

      {/* ── Customer Insights ─────────────────────────────────────────────── */}
      <Card title={t('analytics.customerInsights')}>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            icon={Users}
            label={t('analytics.activeCustomers')}
            value={kpiValue('active_customers')}
          />
          <StatCard
            icon={ShoppingCart}
            label={t('analytics.orderFrequency')}
            value={`${kpiValue('order_frequency')}x`}
          />
          <StatCard
            icon={DollarSign}
            label={t('analytics.customerLtv')}
            value={kpiValue('customer_lifetime_value')}
          />
          <StatCard
            icon={AlertTriangle}
            label={t('analytics.disputeRate')}
            value={`${kpiValue('disputed_invoice_rate')}%`}
          />
        </div>
      </Card>

      {/* ── Operations ────────────────────────────────────────────────────── */}
      <Card title={t('analytics.operations')}>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            icon={BarChart3}
            label={t('analytics.productionEfficiency')}
            value={`${kpiValue('production_efficiency')}%`}
          />
          <StatCard
            icon={Package}
            label={t('analytics.stockTurnover')}
            value={`${kpiValue('stock_turnover')}x`}
          />
          <StatCard
            icon={TrendingUp}
            label={t('analytics.deliveryRate')}
            value={`${kpiValue('delivery_completion_rate')}%`}
          />
          <StatCard
            icon={ShoppingCart}
            label={t('analytics.todayOrders')}
            value={String(ordersSummary.todayCount ?? '—')}
            hint={
              ordersSummary.todayValue !== null && ordersSummary.todayValue !== undefined
                ? `Value: ${Number(ordersSummary.todayValue).toLocaleString()}`
                : undefined
            }
          />
        </div>
      </Card>

      {/* ── Top Products Chart ─────────────────────────────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title={t('analytics.topProductsRevenue')}>
          {topProducts.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={topProducts.slice(0, 8).map((p) => ({
                name: String(p.name || p.productName || '').slice(0, 15),
                revenue: Number(p.totalRevenue ?? 0),
                qty: Number(p.totalQty ?? 0),
              }))}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" fontSize={12} />
                <YAxis fontSize={12} />
                <Tooltip />
                <Bar dataKey="revenue" fill="#3b82f6" name="Revenue" radius={[4,4,0,0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : <p className="text-gray-400 text-sm py-8 text-center">{t('common.noData')}</p>}
        </Card>

        <Card title={t('analytics.topProductsDistribution')}>
          {topProducts.length > 0 ? (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={topProducts.slice(0, 6).map((p) => ({
                    name: String(p.name || p.productName || ''),
                    value: Number(p.totalRevenue ?? 0),
                  }))}
                  cx="50%" cy="50%" outerRadius={100}
                  dataKey="value" label={({ name }) => name?.slice(0, 12)}
                >
                  {topProducts.slice(0, 6).map((_, i) => (
                    <Cell key={i} fill={['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#ec4899'][i % 6]} />
                  ))}
                </Pie>
                <Legend />
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          ) : <p className="text-gray-400 text-sm py-8 text-center">{t('common.noData')}</p>}
        </Card>
      </div>

      {/* ── Top Products Table ────────────────────────────────────────────── */}
      <Card title={t('analytics.topProducts')}>
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
      </Card>
    </div>
  );
}
