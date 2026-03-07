'use client';
import { useState, useEffect, useCallback, useRef } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Spinner, Badge, Alert } from '@/components/ui';
import { useT } from '@/lib/i18n';

// â”€â”€â”€ interfaces â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

interface Ingredient {
  itemName: string;
  unitMode: string;
  recipeQty: number;
  recipeUom: string;
  pieceQty: number;
  wasteFactor: number;
}

interface RecipeDetail {
  recipeId: string;
  versionNumber: number;
  batchSize: number;
  batchSizeUom: string;
  expectedYield: number;
  yieldUom: string;
  leadTimeHours: number | null;
  productionNotes: string | null;
  ingredients: Ingredient[];
}

interface TechnologyStep {
  stepId: string;
  stepNumber: number;
  name: string;
  activities: string | null;
  instruments: string | null;
  durationMinutes: number | null;
  temperatureCelsius: number | null;
}

interface WorkOrder {
  workOrderId: string;
  productId: string;
  productName: string;
  status: string;
  batchCount: number;
  targetQty: number;
  targetUom: string;
  recipeId: string | null;
}

interface Plan {
  planId: string;
  planDate: string;
  shift: string;
  status: string;
  workOrders: WorkOrder[];
}

// â”€â”€â”€ step key helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

const stepKey = (woId: string, stepNum: number) => `step_${woId}_${stepNum}`;

function getConfirmed(woId: string, steps: TechnologyStep[]): Record<number, boolean> {
  const out: Record<number, boolean> = {};
  if (typeof window === 'undefined') return out;
  steps.forEach((s) => {
    out[s.stepNumber] = localStorage.getItem(stepKey(woId, s.stepNumber)) === '1';
  });
  return out;
}

// â”€â”€â”€ shift colours â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

const shiftBg: Record<string, string> = {
  MORNING: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  AFTERNOON: 'bg-orange-100 text-orange-800 border-orange-300',
  NIGHT: 'bg-indigo-100 text-indigo-800 border-indigo-300',
};
const woStatusIcon: Record<string, string> = {
  PENDING: 'â³', STARTED: 'â–¶ï¸', IN_PROGRESS: 'â–¶ï¸', COMPLETED: 'âœ…', CANCELLED: 'âŒ',
};

// â”€â”€â”€ Work Order Detail Panel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

function WOPanel({
  wo, planId, recipe, techSteps, stepsLoading,
  onClose, onAction, actionBusy,
}: {
  wo: WorkOrder;
  planId: string;
  recipe: RecipeDetail | null;
  techSteps: TechnologyStep[];
  stepsLoading: boolean;
  onClose: () => void;
  onAction: (planId: string, woId: string, action: string) => void;
  actionBusy: boolean;
}) {
  const t = useT();
  const [tab, setTab] = useState<'steps' | 'recipe'>(techSteps.length > 0 ? 'steps' : 'recipe');
  const [confirmed, setConfirmed] = useState<Record<number, boolean>>(() => getConfirmed(wo.workOrderId, techSteps));

  // Update confirmed map when steps arrive
  useEffect(() => {
    setConfirmed(getConfirmed(wo.workOrderId, techSteps));
    if (techSteps.length > 0) setTab('steps');
  }, [wo.workOrderId, techSteps]);

  const toggleStep = (stepNum: number) => {
    const next = !confirmed[stepNum];
    localStorage.setItem(stepKey(wo.workOrderId, stepNum), next ? '1' : '0');
    setConfirmed((prev) => ({ ...prev, [stepNum]: next }));
  };

  const confirmedCount = Object.values(confirmed).filter(Boolean).length;
  const allStepsDone = techSteps.length > 0 && confirmedCount === techSteps.length;

  return (
    <div className="fixed inset-0 bg-black/60 flex items-stretch justify-end z-50">
      <div className="bg-white w-full max-w-xl flex flex-col shadow-2xl">
        {/* Header */}
        <div className="flex items-start justify-between px-5 py-4 border-b bg-slate-800 text-white">
          <div className="flex-1 min-w-0">
            <div className="text-xs uppercase tracking-wide opacity-60 mb-0.5">{t('floor.workOrder')}</div>
            <div className="font-semibold text-lg truncate">{wo.productName}</div>
            <div className="text-sm opacity-70 mt-0.5">
              {t('floor.target')}: {wo.targetQty} {wo.targetUom}
              {wo.batchCount ? ` · ${t('floor.batches', {count: wo.batchCount})}` : ''}
              {recipe && ` · ${t('floor.batchSizeLabel')}: ${recipe.batchSize} ${recipe.batchSizeUom}`}
            </div>
          </div>
          <div className="flex items-center gap-2 ml-3">
            <Badge status={wo.status} />
            <button onClick={onClose} className="text-white opacity-60 hover:opacity-100 text-2xl leading-none">Ã—</button>
          </div>
        </div>

        {/* Tabs */}
        <div className="flex border-b bg-gray-50 px-5">
          {(['steps', 'recipe'] as const).map((tb) => (
            <button
              key={tb}
              className={`px-4 py-2.5 text-xs font-semibold border-b-2 transition-colors ${
                tab === tb
                  ? 'border-blue-600 text-blue-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
              onClick={() => setTab(tb)}
            >
              {tb === 'steps'
                ? `${t('floor.technologySteps')}${techSteps.length > 0 ? ` (${confirmedCount}/${techSteps.length})` : ''}`
                : `${t('floor.recipe')}${recipe ? ` v${recipe.versionNumber}` : ''}`}
            </button>
          ))}
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-5">
          {/* â”€â”€ Technology Steps â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
          {tab === 'steps' && (
            <>
              {stepsLoading ? (
                <div className="flex justify-center py-12"><Spinner /></div>
              ) : techSteps.length === 0 ? (
                <div className="text-center py-12">
                  <div className="text-3xl mb-2">ðŸ“‹</div>
                  <div className="text-sm font-medium text-gray-600">{t('floor.noStepsDefined')}</div>
                  <div className="text-xs text-gray-400 mt-1">{t('floor.askTechnologistSteps')}</div>
                  {recipe?.productionNotes && (
                    <div className="mt-4 text-left bg-amber-50 border border-amber-200 rounded-lg p-4 text-sm text-gray-700 whitespace-pre-wrap">
                      <div className="font-semibold text-amber-700 mb-1">{t('recipes.processNotes')}</div>
                      {recipe.productionNotes}
                    </div>
                  )}
                </div>
              ) : (
                <div className="space-y-3">
                  {allStepsDone && (
                    <div className="bg-green-50 border border-green-200 rounded-lg px-4 py-2 text-sm text-green-700 font-medium">
                      {t('floor.allStepsConfirmed')}
                    </div>
                  )}
                  {techSteps.map((step) => {
                    const done = confirmed[step.stepNumber] ?? false;
                    return (
                      <div
                        key={step.stepId}
                        className={`border rounded-lg p-4 transition-colors ${done ? 'bg-green-50 border-green-200' : 'bg-white border-gray-200'}`}
                      >
                        <div className="flex items-start gap-3">
                          <button
                            onClick={() => toggleStep(step.stepNumber)}
                            className={`flex-shrink-0 mt-0.5 w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                              done
                                ? 'bg-green-500 border-green-500 text-white'
                                : 'border-gray-300 hover:border-green-400'
                            }`}
                          >
                            {done && <span className="text-xs">âœ“</span>}
                          </button>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-bold text-gray-400">{t('recipes.stepNumber')} {step.stepNumber}</span>
                              <span className={`font-semibold text-sm ${done ? 'text-green-700 line-through' : 'text-gray-800'}`}>
                                {step.name}
                              </span>
                            </div>
                            {step.activities && (
                              <div className="text-sm text-gray-600 mt-1 whitespace-pre-line">{step.activities}</div>
                            )}
                            <div className="flex flex-wrap gap-3 mt-1.5 text-xs text-gray-400">
                              {step.instruments && (
                                <span className="flex items-center gap-1">
                                  <span>ðŸ”§</span> {step.instruments}
                                </span>
                              )}
                              {step.durationMinutes && (
                                <span className="flex items-center gap-1">
                                  <span>â±</span> {step.durationMinutes} min
                                </span>
                              )}
                              {step.temperatureCelsius != null && (
                                <span className="flex items-center gap-1">
                                  <span>ðŸŒ¡</span> {step.temperatureCelsius}Â°C
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </>
          )}

          {/* â”€â”€ Recipe / Ingredients â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
          {tab === 'recipe' && (
            <>
              {!recipe ? (
                <div className="text-center py-12">
                  <div className="text-3xl mb-2">âŒ</div>
                  <div className="text-sm font-medium text-gray-600">{t('floor.noActiveRecipe')}</div>
                  <div className="text-xs text-gray-400 mt-1">{t('floor.askTechnologistRecipe')}</div>
                </div>
              ) : (
                <div className="space-y-4">
                  <div className="grid grid-cols-3 gap-3 text-xs">
                    {[
                      { label: t('floor.batchSizeValue'), value: `${recipe.batchSize} ${recipe.batchSizeUom}` },
                      { label: t('floor.yieldValue'), value: `${recipe.expectedYield} ${recipe.yieldUom}` },
                      { label: t('floor.leadTimeValue'), value: recipe.leadTimeHours ? `${recipe.leadTimeHours}h` : '—' },
                    ].map((kv) => (
                      <div key={kv.label} className="bg-gray-50 border rounded-lg p-3">
                        <div className="text-gray-400">{kv.label}</div>
                        <div className="font-semibold text-gray-800 mt-0.5">{kv.value}</div>
                      </div>
                    ))}
                  </div>
                  {recipe.productionNotes && (
                    <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 text-sm text-gray-700 whitespace-pre-wrap">
                      <div className="font-semibold text-amber-700 mb-1">{t('recipes.processNotes')}</div>
                      {recipe.productionNotes}
                    </div>
                  )}
                  {recipe.ingredients.length > 0 && (
                    <div>
                      <div className="text-xs font-semibold uppercase tracking-wide text-gray-400 mb-2">
                        {t('floor.ingredientsPerBatch', {count: wo.batchCount})}
                      </div>
                      <table className="w-full text-sm">
                        <thead>
                          <tr className="text-left text-xs text-gray-400 border-b">
                            <th className="pb-2">{t('floor.ingredient')}</th>
                            <th className="pb-2 text-right">{t('floor.perBatch')}</th>
                            <th className="pb-2 text-right">{t('floor.totalCol')}</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y">
                          {recipe.ingredients.map((ing, i) => {
                            const perBatch = ing.unitMode === 'PIECE' ? `${ing.pieceQty} pcs` : `${ing.recipeQty} ${ing.recipeUom}`;
                            const totalAmt = ing.unitMode === 'PIECE'
                              ? `${(ing.pieceQty * wo.batchCount).toFixed(0)} pcs`
                              : `${(ing.recipeQty * wo.batchCount).toFixed(1)} ${ing.recipeUom}`;
                            return (
                              <tr key={i}>
                                <td className="py-2 font-medium">{ing.itemName || 'â€”'}</td>
                                <td className="py-2 text-right text-gray-500">{perBatch}</td>
                                <td className="py-2 text-right font-semibold">{totalAmt}</td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer actions */}
        <div className="border-t px-5 py-3 flex items-center gap-2 bg-gray-50">
          {wo.status === 'PENDING' && (
            <button
              className="btn-primary bg-blue-600 hover:bg-blue-700"
              disabled={actionBusy}
              onClick={() => onAction(planId, wo.workOrderId, 'start')}
            >
              {actionBusy ? 'â€¦' : t('floor.startWorkOrder')}
            </button>
          )}
          {wo.status === 'STARTED' && (
            <button
              className="btn-primary bg-green-600 hover:bg-green-700"
              disabled={actionBusy}
              onClick={() => onAction(planId, wo.workOrderId, 'complete')}
            >
              {actionBusy ? 'â€¦' : t('floor.completeWorkOrder')}
            </button>
          )}
          {(wo.status === 'PENDING' || wo.status === 'STARTED') && (
            <button
              className="btn-xs bg-red-100 text-red-700 hover:bg-red-200 px-3 py-1.5"
              disabled={actionBusy}
              onClick={() => onAction(planId, wo.workOrderId, 'cancel')}
            >
              {t('common.cancel')}
            </button>
          )}
          <button className="ml-auto btn-secondary" onClick={onClose}>{t('common.close')}</button>
        </div>
      </div>
    </div>
  );
}

// â”€â”€â”€ main page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

export default function FloorPage() {
  const t = useT();
  const [plans, setPlans] = useState<Plan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [now, setNow] = useState(new Date());

  // Selected WO panel
  const [selectedWo, setSelectedWo] = useState<{ wo: WorkOrder; planId: string } | null>(null);
  const [recipe, setRecipe] = useState<RecipeDetail | null>(null);
  const [recipeLoading, setRecipeLoading] = useState(false);
  const [techSteps, setTechSteps] = useState<TechnologyStep[]>([]);
  const [stepsLoading, setStepsLoading] = useState(false);
  const [actionBusy, setActionBusy] = useState(false);

  // Cache recipe + steps so we don't re-fetch on every open
  const recipeCache = useRef<Record<string, RecipeDetail>>({});
  const stepsCache = useRef<Record<string, TechnologyStep[]>>({});

  const today = now.toISOString().substring(0, 10);

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(id);
  }, []);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const all = await apiFetch<Plan[]>(`/v1/production-plans?tenantId=${TENANT_ID}`);
      // Today's plans + any still IN_PROGRESS
      const relevant = all.filter((p) => p.planDate === today || p.status === 'IN_PROGRESS');
      setPlans(relevant);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, [today]);

  useEffect(() => { load(); }, [load]);

  const reloadPlan = async (planId: string) => {
    try {
      const updated = await apiFetch<Plan>(`/v1/production-plans/${planId}?tenantId=${TENANT_ID}`);
      setPlans((prev) => prev.map((p) => (p.planId === planId ? updated : p)));
      // Also refresh selectedWo reference
      if (selectedWo?.planId === planId) {
        const refreshedWo = updated.workOrders?.find((w) => w.workOrderId === selectedWo.wo.workOrderId);
        if (refreshedWo) setSelectedWo({ wo: refreshedWo, planId });
      }
    } catch (e) {
      setError(String(e));
    }
  };

  const openWo = async (wo: WorkOrder, planId: string) => {
    setSelectedWo({ wo, planId });

    // Load recipe
    if (wo.productId) {
      if (recipeCache.current[wo.productId]) {
        setRecipe(recipeCache.current[wo.productId]);
        setRecipeLoading(false);
      } else {
        setRecipe(null);
        setRecipeLoading(true);
        try {
          const r = await apiFetch<RecipeDetail>(
            `/v1/recipes/active?tenantId=${TENANT_ID}&productId=${wo.productId}`
          );
          recipeCache.current[wo.productId] = r;
          setRecipe(r);
        } catch {
          setRecipe(null);
        } finally {
          setRecipeLoading(false);
        }
      }
    }

    // Load technology steps
    const rid = wo.recipeId;
    if (rid) {
      if (stepsCache.current[rid]) {
        setTechSteps(stepsCache.current[rid]);
        setStepsLoading(false);
      } else {
        setTechSteps([]);
        setStepsLoading(true);
        try {
          const s = await apiFetch<TechnologyStep[]>(
            `/v1/technology-steps?tenantId=${TENANT_ID}&recipeId=${rid}`
          );
          stepsCache.current[rid] = s;
          setTechSteps(s);
        } catch {
          setTechSteps([]);
        } finally {
          setStepsLoading(false);
        }
      }
    } else {
      setTechSteps([]);
    }
  };

  const handleWoAction = async (planId: string, woId: string, action: string) => {
    try {
      setActionBusy(true);
      await apiFetch(`/v1/production-plans/work-orders/${woId}/${action}?tenantId=${TENANT_ID}`, { method: 'POST' });
      await reloadPlan(planId);
    } catch (e) {
      setError(String(e));
    } finally {
      setActionBusy(false);
    }
  };

  if (loading) return <Spinner />;

  const activePlan = plans.find((p) => p.status === 'IN_PROGRESS');

  return (
    <div className="max-w-4xl space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-semibold">{t('floor.title')}</h1>
        <p className="text-sm text-gray-400 mt-0.5">
          {now.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
          {activePlan && (
            <span className="ml-3 text-amber-600 font-medium">
              {t('floor.activeShift', {shift: activePlan.shift})}
            </span>
          )}
        </p>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {plans.length === 0 ? (
        <div className="bg-white border rounded-xl p-12 text-center text-gray-400">
          <div className="text-4xl mb-3">ðŸ­</div>
          <div className="font-medium">{t('floor.noPlansToday')}</div>
          <div className="text-sm mt-1">{t('floor.plansCreatedBySupervisor')}</div>
        </div>
      ) : (
        plans.map((plan) => {
          const workOrders = plan.workOrders ?? [];
          const doneCount = workOrders.filter((w) => w.status === 'COMPLETED').length;
          const total = workOrders.length;
          const pct = total > 0 ? Math.round((doneCount / total) * 100) : 0;
          const isActive = plan.status === 'IN_PROGRESS';

          return (
            <div
              key={plan.planId}
              className={`bg-white border rounded-xl shadow-sm overflow-hidden ${isActive ? 'ring-2 ring-blue-500' : ''}`}
            >
              {/* Plan header */}
              <div className={`px-5 py-3 flex items-center gap-3 border-b ${isActive ? 'bg-blue-600 text-white' : 'bg-slate-50'}`}>
                <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${shiftBg[plan.shift] ?? 'bg-gray-100 text-gray-700 border-gray-200'}`}>
                  {plan.shift}
                </span>
                <span className={`font-semibold ${isActive ? 'text-white' : 'text-gray-800'}`}>{plan.planDate}</span>
                {isActive && <span className="text-xs bg-white/20 px-2 py-0.5 rounded-full">â–¶ IN PROGRESS</span>}
                <div className="ml-auto flex items-center gap-3">
                  <span className={`text-sm ${isActive ? 'text-blue-100' : 'text-gray-500'}`}>
                    {t('floor.progress', {done: doneCount, total})}
                  </span>
                  <Badge status={plan.status} />
                </div>
              </div>

              {/* Progress bar */}
              {total > 0 && (
                <div className="h-1.5 bg-gray-100">
                  <div
                    className={`h-1.5 transition-all ${isActive ? 'bg-blue-500' : 'bg-green-500'}`}
                    style={{ width: `${pct}%` }}
                  />
                </div>
              )}

              {/* Work order cards */}
              {total === 0 ? (
                <div className="px-5 py-4 text-sm text-gray-400">No work orders yet.</div>
              ) : (
                <div className="divide-y">
                  {workOrders.map((wo) => (
                    <div
                      key={wo.workOrderId}
                      className="px-5 py-3 flex items-center gap-3 hover:bg-gray-50 cursor-pointer"
                      onClick={() => openWo(wo, plan.planId)}
                    >
                      <span className="text-lg flex-shrink-0">{woStatusIcon[wo.status] ?? 'â€¢'}</span>
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium">{wo.productName}</div>
                        <div className="text-xs text-gray-400">
                          {t('floor.target')}: {wo.targetQty} {wo.targetUom}
                          {wo.batchCount ? ` · ${t('floor.batches', {count: wo.batchCount})}` : ''}
                        </div>
                      </div>
                      <Badge status={wo.status} />
                      <span className="text-xs text-blue-600 font-medium">View â†’</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })
      )}

      {/* Work Order Detail Panel */}
      {selectedWo && (
        <WOPanel
          wo={selectedWo.wo}
          planId={selectedWo.planId}
          recipe={recipe}
          techSteps={techSteps}
          stepsLoading={stepsLoading || recipeLoading}
          onClose={() => { setSelectedWo(null); setRecipe(null); setTechSteps([]); }}
          onAction={handleWoAction}
          actionBusy={actionBusy}
        />
      )}
    </div>
  );
}
