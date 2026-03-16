'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Badge, Alert, PageSkeleton } from '@/components/ui';
import { SectionTitle } from '@/components/design-system';
import Link from 'next/link';
import { useT } from '@/lib/i18n';

// ─── interfaces ───────────────────────────────────────────────────────────────
interface Product {
  productId: string;
  name: string;
  departmentId: string;
  departmentName: string;
  baseUom: string;
}

interface RecipeSummary {
  recipeId: string;
  productId: string;
  versionNumber: number;
  status: string;
  batchSize: number;
  batchSizeUom: string;
  expectedYield: number;
  yieldUom: string;
  leadTimeHours: number | null;
  productionNotes: string | null;
  ingredients: { itemName: string; recipeQty: number; recipeUom: string; wasteFactor: number }[];
}

interface Plan {
  planId: string;
  planDate: string;
  shift: string;
  status: string;
  workOrders: { productName: string; status: string; batchCount?: number; actualYield?: number }[];
}

interface Dept {
  departmentId: string;
  name: string;
}

interface ProductRecipe {
  product: Product;
  recipe: RecipeSummary | null;
  error: boolean;
}

// ─── helpers ──────────────────────────────────────────────────────────────────
function RecipeHealthBadge({ recipe }: Readonly<{ recipe: RecipeSummary | null }>) {
  const t = useT();
  if (!recipe) return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700">{t('technologist.noActiveRecipeLabel')}</span>;
  return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">{t('technologist.activeVersion', { version: recipe.versionNumber })}</span>;
}

function buildProductionBreakdown(plans: Plan[]): Record<string, { count: number; batches: number }> {
  const result: Record<string, { count: number; batches: number }> = {};
  for (const plan of plans) {
    for (const wo of plan.workOrders ?? []) {
      if (!result[wo.productName]) result[wo.productName] = { count: 0, batches: 0 };
      result[wo.productName].count++;
      result[wo.productName].batches += wo.batchCount ?? 0;
    }
  }
  return result;
}
}

// ─── main component ───────────────────────────────────────────────────────────
export default function TechnologistPage() {
  const t = useT();
  const [productRecipes, setProductRecipes] = useState<ProductRecipe[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [depts, setDepts] = useState<Dept[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expanded, setExpanded] = useState<string | null>(null);
  const [deptFilter, setDeptFilter] = useState('ALL');

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [products, allPlans, allDepts] = await Promise.all([
        apiFetch<Product[]>(`/v1/products?tenantId=${TENANT_ID}`),
        apiFetch<Plan[]>(`/v1/production-plans?tenantId=${TENANT_ID}`),
        apiFetch<Dept[]>(`/v1/departments?tenantId=${TENANT_ID}`).catch(() => [] as Dept[]),
      ]);
      setPlans(allPlans);
      setDepts(allDepts);

      // Fetch active recipe for each product (best-effort: errors become null)
      const entries: ProductRecipe[] = await Promise.all(
        products.map(async (p) => {
          try {
            const recipe = await apiFetch<RecipeSummary>(
              `/v1/recipes/active?tenantId=${TENANT_ID}&productId=${p.productId}`
            );
            return { product: p, recipe, error: false };
          } catch {
            return { product: p, recipe: null, error: true };
          }
        })
      );
      setProductRecipes(entries);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // ── derived stats ──────────────────────────────────────────────────────────
  const filteredPR = deptFilter === 'ALL' ? productRecipes : productRecipes.filter((pr) => pr.product.departmentId === deptFilter);
  const withRecipe = filteredPR.filter((pr) => pr.recipe !== null).length;
  const withLeadTime = filteredPR.filter((pr) => pr.recipe?.leadTimeHours).length;
  const noRecipe = filteredPR.length - withRecipe;
  const avgLeadTime =
    filteredPR.filter((pr) => pr.recipe?.leadTimeHours)
      .reduce((sum, pr) => sum + (pr.recipe?.leadTimeHours ?? 0), 0) /
    (withLeadTime || 1);

  // Work order breakdown by product across all completed/in-progress plans
  const productionByProduct = buildProductionBreakdown(plans);
  const topProducts = Object.entries(productionByProduct)
    .sort((a, b) => b[1].count - a[1].count)
    .slice(0, 8);

  // Recent plans for analysis
  const recentPlans = [...plans]
    .sort((a, b) => b.planDate.localeCompare(a.planDate))
    .slice(0, 10);

  // Yield variance analysis (BC-1805)
  const yieldVariance: { product: string; expected: number; actual: number; variance: number; uom: string }[] = [];
  for (const pr of filteredPR) {
    if (!pr.recipe) continue;
    const wos = plans.flatMap((p) => p.workOrders ?? []).filter((w) => w.productName === pr.product.name && w.status === 'COMPLETED');
    if (wos.length === 0) continue;
    const totalBatches = wos.reduce((s, w) => s + (w.batchCount ?? 1), 0);
    const expectedTotal = pr.recipe.expectedYield * totalBatches;
    const actualTotal = wos.reduce((s, w) => s + (w.actualYield ?? pr.recipe!.expectedYield * (w.batchCount ?? 1)), 0);
    const variance = expectedTotal > 0 ? ((actualTotal - expectedTotal) / expectedTotal) * 100 : 0;
    yieldVariance.push({ product: pr.product.name, expected: expectedTotal, actual: actualTotal, variance, uom: pr.recipe.yieldUom });
  }

  if (loading) return <PageSkeleton />;

  return (
    <div className="max-w-[1800px] space-y-8">
      <SectionTitle
        eyebrow={t('technologist.eyebrow')}
        title={t('technologist.title')}
        subtitle={t('technologist.subtitle')}
        action={
          <select className="input w-48" value={deptFilter} onChange={(e) => setDeptFilter(e.target.value)}>
            <option value="ALL">{t('technologist.allDepartments')}</option>
            {depts.map((d) => <option key={d.departmentId} value={d.departmentId}>{d.name}</option>)}
          </select>
        }
      />

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {/* KPI row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="border rounded-xl p-4 bg-green-50 border-green-200">
          <div className="text-2xl font-bold text-gray-800">{withRecipe}</div>
          <div className="text-sm text-gray-600 mt-0.5">{t('technologist.withActiveRecipe')}</div>
        </div>
        <div className={`border rounded-xl p-4 ${noRecipe > 0 ? 'bg-red-50 border-red-300' : 'bg-gray-50 border-gray-200'}`}>
          <div className="text-2xl font-bold text-gray-800">{noRecipe}</div>
          <div className="text-sm text-gray-600 mt-0.5">{t('technologist.missingRecipe')}</div>
        </div>
        <div className="border rounded-xl p-4 bg-blue-50 border-blue-200">
          <div className="text-2xl font-bold text-gray-800">{withLeadTime}</div>
          <div className="text-sm text-gray-600 mt-0.5">{t('technologist.withLeadTime')}</div>
        </div>
        <div className="border rounded-xl p-4 bg-amber-50 border-amber-200">
          <div className="text-2xl font-bold text-gray-800">{avgLeadTime.toFixed(1)}h</div>
          <div className="text-sm text-gray-600 mt-0.5">{t('technologist.avgLeadTime')}</div>
        </div>
      </div>

      {/* Recipe health table */}
      <div>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-400">{t('technologist.recipeHealth')}</h2>
          <Link href="/recipes" className="text-xs text-blue-600 hover:underline">{t('technologist.manageRecipes')}</Link>
        </div>
        <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
          {filteredPR.length === 0 ? (
            <div className="px-5 py-10 text-center text-sm text-gray-400">
              {t('technologist.noProducts')} <Link href="/products" className="text-blue-600 hover:underline">{t('technologist.addProducts')}</Link>
            </div>
          ) : (
            <div className="divide-y">
              {filteredPR.map(({ product, recipe }) => {
                const isOpen = expanded === product.productId;
                return (
                  <div key={product.productId}>
                    <button
                      className="w-full text-left px-5 py-3 flex items-center gap-3 hover:bg-gray-50 transition-colors"
                      onClick={() => setExpanded(isOpen ? null : product.productId)}
                    >
                      <span className="text-sm font-medium flex-1">{product.name}</span>
                      <span className="text-xs text-gray-400">{product.departmentName}</span>
                      <RecipeHealthBadge recipe={recipe} />
                      {recipe?.leadTimeHours && (
                        <span className="text-xs text-blue-600 font-medium">⏱ {recipe.leadTimeHours}h</span>
                      )}
                      <span className="text-gray-300 ml-1">{isOpen ? '▲' : '▼'}</span>
                    </button>

                    {isOpen && recipe && (
                      <div className="px-5 py-4 bg-gray-50 border-t text-sm space-y-3">
                        <div className="flex flex-wrap gap-4 text-gray-600">
                          <span>{t('recipes.batch')}: <strong>{recipe.batchSize} {recipe.batchSizeUom}</strong></span>
                          <span>{t('recipes.yield')}: <strong>{recipe.expectedYield} {recipe.yieldUom}</strong></span>
                          {recipe.leadTimeHours && <span>{t('floor.leadTimeValue')}: <strong>{recipe.leadTimeHours}h</strong></span>}
                        </div>
                        {recipe.productionNotes && (
                          <div className="bg-amber-50 border border-amber-200 rounded p-3 text-xs whitespace-pre-wrap leading-relaxed">
                            <div className="font-semibold text-amber-800 mb-1 uppercase tracking-wide text-[10px]">{t('technologist.technologicalProcess')}</div>
                            {recipe.productionNotes}
                          </div>
                        )}
                        {recipe.ingredients.length > 0 && (
                          <table className="w-full text-xs">
                            <thead className="text-gray-400 border-b">
                              <tr>
                                <th className="pb-1 text-left">{t('floor.ingredient')}</th>
                                <th className="pb-1 text-right">{t('orders.qty')}</th>
                                <th className="pb-1 text-right">{t('recipes.waste')}</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                              {recipe.ingredients.map((ing) => (
                                <tr key={ing.itemName}>
                                  <td className="py-1">{ing.itemName}</td>
                                  <td className="py-1 text-right text-gray-600">{ing.recipeQty} {ing.recipeUom}</td>
                                  <td className="py-1 text-right text-gray-400">{ing.wasteFactor ? `${(ing.wasteFactor * 100).toFixed(1)}%` : '—'}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        )}
                        {!recipe.productionNotes && <p className="text-gray-400 text-xs">{t('technologist.noProductionNotes')}</p>}
                      </div>
                    )}

                    {isOpen && !recipe && (
                      <div className="px-5 py-4 bg-red-50 border-t text-sm text-red-600">
                        {t('technologist.noActiveRecipeMsg')} <Link href="/recipes" className="underline">{t('technologist.createAndActivate')}</Link> {t('technologist.toEnableDetection')}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Yield Variance Analysis (BC-1805) */}
      {yieldVariance.length > 0 && (
        <div>
          <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-3">{t('technologist.yieldVariance')}</h2>
          <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  {[t('technologist.product'), t('technologist.expectedYield'), t('technologist.actualYield'), t('technologist.variancePct')].map((h) => (
                    <th key={h} className="px-4 py-2.5 text-left text-xs font-semibold text-gray-500 uppercase">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y">
                {yieldVariance.map((v) => (
                  <tr key={v.product} className="hover:bg-gray-50">
                    <td className="px-4 py-2.5 font-medium">{v.product}</td>
                    <td className="px-4 py-2.5">{v.expected.toFixed(1)} {v.uom}</td>
                    <td className="px-4 py-2.5">{v.actual.toFixed(1)} {v.uom}</td>
                    <td className={`px-4 py-2.5 font-semibold ${v.variance >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {v.variance >= 0 ? '+' : ''}{v.variance.toFixed(1)}%
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Production frequency analysis */}
      {topProducts.length > 0 && (
        <div>
          <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-3">{t('technologist.productionFrequency')}</h2>
          <div className="bg-white border rounded-xl shadow-sm p-5">
            <div className="space-y-3">
              {topProducts.map(([name, stats]) => {
                const maxCount = topProducts[0]?.[1].count ?? 1;
                const pct = Math.round((stats.count / maxCount) * 100);
                return (
                  <div key={name} className="flex items-center gap-3">
                    <div className="w-40 text-sm truncate" title={name}>{name}</div>
                    <div className="flex-1 bg-gray-100 rounded-full h-3 overflow-hidden">
                      <div className="h-3 bg-blue-500 rounded-full" style={{ width: `${pct}%` }} />
                    </div>
                    <div className="text-xs text-gray-500 w-24 text-right">
                      {t('technologist.woCount', { count: stats.count })}
                      {stats.batches ? ` ${t('technologist.batchCount', { count: stats.batches })}` : ''}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Recent production plans */}
      {recentPlans.length > 0 && (
        <div>
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-xs font-semibold uppercase tracking-wide text-gray-400">{t('technologist.recentPlans')}</h2>
            <Link href="/production-plans" className="text-xs text-blue-600 hover:underline">{t('common.viewAll')}</Link>
          </div>
          <div className="bg-white border rounded-xl shadow-sm overflow-hidden">
            <div className="divide-y">
              {recentPlans.map((plan) => {
                const total = plan.workOrders?.length ?? 0;
                const done = plan.workOrders?.filter((w) => w.status === 'COMPLETED').length ?? 0;
                const pct = total > 0 ? Math.round((done / total) * 100) : 0;
                return (
                  <div key={plan.planId} className="px-5 py-3 flex items-center gap-3">
                    <div className="flex-1">
                      <div className="text-sm font-medium">{plan.planDate} · {plan.shift}</div>
                      <div className="text-xs text-gray-400 mt-0.5">
                        {t('technologist.workOrdersPct', { total, pct })}
                        {total > 0 && (
                          <span className="ml-2 inline-block bg-gray-100 rounded-full h-1.5 w-20 overflow-hidden align-middle">
                            <span className="block h-1.5 bg-green-500" style={{ width: `${pct}%` }} />
                          </span>
                        )}
                      </div>
                    </div>
                    <Badge status={plan.status} />
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
