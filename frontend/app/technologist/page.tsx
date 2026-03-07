'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Spinner, Badge, Alert } from '@/components/ui';
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
  workOrders: { productName: string; status: string; batchCount?: number }[];
}

interface ProductRecipe {
  product: Product;
  recipe: RecipeSummary | null;
  error: boolean;
}

// ─── helpers ──────────────────────────────────────────────────────────────────
function RecipeHealthBadge({ recipe }: { recipe: RecipeSummary | null }) {
  const t = useT();
  if (!recipe) return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700">{t('technologist.noActiveRecipeLabel')}</span>;
  return <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-700">{t('technologist.activeVersion', { version: recipe.versionNumber })}</span>;
}

// ─── main component ───────────────────────────────────────────────────────────
export default function TechnologistPage() {
  const t = useT();
  const [productRecipes, setProductRecipes] = useState<ProductRecipe[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expanded, setExpanded] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [products, allPlans] = await Promise.all([
        apiFetch<Product[]>(`/v1/products?tenantId=${TENANT_ID}`),
        apiFetch<Plan[]>(`/v1/production-plans?tenantId=${TENANT_ID}`),
      ]);
      setPlans(allPlans);

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
  const withRecipe = productRecipes.filter((pr) => pr.recipe !== null).length;
  const withLeadTime = productRecipes.filter((pr) => pr.recipe?.leadTimeHours).length;
  const noRecipe = productRecipes.length - withRecipe;
  const avgLeadTime =
    productRecipes.filter((pr) => pr.recipe?.leadTimeHours)
      .reduce((sum, pr) => sum + (pr.recipe?.leadTimeHours ?? 0), 0) /
    (withLeadTime || 1);

  // Work order breakdown by product across all completed/in-progress plans
  const productionByProduct: Record<string, { count: number; batches: number }> = {};
  for (const plan of plans) {
    for (const wo of plan.workOrders ?? []) {
      if (!productionByProduct[wo.productName]) productionByProduct[wo.productName] = { count: 0, batches: 0 };
      productionByProduct[wo.productName].count++;
      productionByProduct[wo.productName].batches += wo.batchCount ?? 0;
    }
  }
  const topProducts = Object.entries(productionByProduct)
    .sort((a, b) => b[1].count - a[1].count)
    .slice(0, 8);

  // Recent plans for analysis
  const recentPlans = [...plans]
    .sort((a, b) => b.planDate.localeCompare(a.planDate))
    .slice(0, 10);

  if (loading) return <Spinner />;

  return (
    <div className="max-w-5xl space-y-8">
      <div>
        <h1 className="text-2xl font-semibold">{t('technologist.title')}</h1>
        <p className="text-sm text-gray-400 mt-0.5">{t('technologist.subtitle')}</p>
      </div>

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
          {productRecipes.length === 0 ? (
            <div className="px-5 py-10 text-center text-sm text-gray-400">
              {t('technologist.noProducts')} <Link href="/products" className="text-blue-600 hover:underline">{t('technologist.addProducts')}</Link>
            </div>
          ) : (
            <div className="divide-y">
              {productRecipes.map(({ product, recipe }) => {
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
                              {recipe.ingredients.map((ing, i) => (
                                <tr key={i}>
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
