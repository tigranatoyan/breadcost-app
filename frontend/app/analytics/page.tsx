'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Spinner } from '@/components/ui';
import { SectionTitle, StatCard, Card, Table, Button } from '@/components/design-system';
import { useT } from '@/lib/i18n';
import {
  DollarSign, Users, ShoppingCart, TrendingUp,
  AlertTriangle, BarChart3, RefreshCw, Package,
} from 'lucide-react';

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

  if (loading) return <Spinner />;

  return (
    <div className="space-y-8">
      <SectionTitle
        eyebrow="D1"
        title={t('nav.analytics')}
        subtitle="Key performance indicators and business insights"
        action={
          <Button variant="secondary" onClick={load}>
            <RefreshCw className="w-4 h-4 mr-1" /> Refresh
          </Button>
        }
      />

      {/* ── Revenue & Financial KPIs ──────────────────────────────────────── */}
      <Card title="Financial Overview">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            icon={DollarSign}
            label="Revenue (Month)"
            value={revenue.month != null ? `${Number(revenue.month).toLocaleString()} ${revenue.currency || ''}` : '—'}
          />
          <StatCard
            icon={TrendingUp}
            label="Gross Margin"
            value={`${kpiValue('gross_margin_pct')}%`}
          />
          <StatCard
            icon={ShoppingCart}
            label="Avg Order Value"
            value={kpiValue('avg_order_value')}
          />
          <StatCard
            icon={AlertTriangle}
            label="Overdue Invoices"
            value={kpiValue('overdue_invoices')}
          />
        </div>
      </Card>

      {/* ── Customer Insights ─────────────────────────────────────────────── */}
      <Card title="Customer Insights">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            icon={Users}
            label="Active Customers"
            value={kpiValue('active_customers')}
          />
          <StatCard
            icon={ShoppingCart}
            label="Order Frequency"
            value={`${kpiValue('order_frequency')}x`}
          />
          <StatCard
            icon={DollarSign}
            label="Customer LTV"
            value={kpiValue('customer_lifetime_value')}
          />
          <StatCard
            icon={AlertTriangle}
            label="Dispute Rate"
            value={`${kpiValue('disputed_invoice_rate')}%`}
          />
        </div>
      </Card>

      {/* ── Operations ────────────────────────────────────────────────────── */}
      <Card title="Operations">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            icon={BarChart3}
            label="Production Efficiency"
            value={`${kpiValue('production_efficiency')}%`}
          />
          <StatCard
            icon={Package}
            label="Stock Turnover"
            value={`${kpiValue('stock_turnover')}x`}
          />
          <StatCard
            icon={TrendingUp}
            label="Delivery Rate"
            value={`${kpiValue('delivery_completion_rate')}%`}
          />
          <StatCard
            icon={ShoppingCart}
            label="Today Orders"
            value={String(ordersSummary.todayCount ?? '—')}
            hint={ordersSummary.todayValue != null ? `Value: ${Number(ordersSummary.todayValue).toLocaleString()}` : undefined}
          />
        </div>
      </Card>

      {/* ── Top Products ──────────────────────────────────────────────────── */}
      <Card title="Top Products">
        <Table
          cols={['Product', 'Qty Sold', 'Revenue', 'Orders']}
          rows={topProducts.map((p) => [
            String(p.name || p.productName || ''),
            String(p.totalQty ?? 0),
            Number(p.totalRevenue ?? 0).toLocaleString(),
            String(p.orderCount ?? 0),
          ])}
          empty="No product data"
        />
      </Card>
    </div>
  );
}
