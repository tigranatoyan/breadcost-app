'use client';
import { useState, useEffect, useCallback, useRef } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Modal, Spinner, Alert, Field, useConfirm } from '@/components/ui';
import { Badge, Button, SectionTitle } from '@/components/design-system';
import { Plus, ChevronDown, ChevronUp, Clock3, Check } from 'lucide-react';
import { useT } from '@/lib/i18n';

interface Product {
  productId: string;
  name: string;
  departmentId: string;
}
interface Dept {
  departmentId: string;
  name: string;
}
interface RecipeIngredient {
  ingredientId: string;
  itemId: string;
  itemName: string;
  unitMode: string;
  recipeQty: number;
  recipeUom: string;
  purchasingUnitSize: number;
  purchasingUom: string;
  wasteFactor: number;
}
interface Recipe {
  recipeId: string;
  productId: string;
  version: number;
  status: string;
  batchSize: number;
  batchSizeUom: string;
  expectedYield: number;
  yieldUom: string;
  leadTimeHours: number | null;
  productionNotes: string | null;
  ingredients: RecipeIngredient[];
}

interface TechnologyStep {
  stepId: string;
  recipeId: string;
  stepNumber: number;
  name: string;
  activities: string | null;
  instruments: string | null;
  durationMinutes: number | null;
  temperatureCelsius: number | null;
}

const newStepForm = (recipeId: string, nextNum: number) => ({
  recipeId, stepNumber: nextNum, name: '', activities: '',
  instruments: '', durationMinutes: 30, temperatureCelsius: '' as string | number,
});

const newIng = () => ({
  itemId: '',
  itemName: '',
  unitMode: 'WEIGHT',
  recipeQty: 1000,
  recipeUom: 'G',
  purchasingUnitSize: 25000,
  purchasingUom: 'G',
  wasteFactor: 0.02,
});

export default function RecipesPage() {
  const t = useT();
  const [askConfirm, confirmModal] = useConfirm({ confirmLabel: t('common.confirm'), cancelLabel: t('common.cancel') });
  const [products, setProducts] = useState<Product[]>([]);
  const [depts, setDepts] = useState<Dept[]>([]);
  const [selectedDeptId, setSelectedDeptId] = useState('');
  const [selectedPid, setSelectedPid] = useState('');
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(false);
  const [initLoading, setInitLoading] = useState(true);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activating, setActivating] = useState('');
  const [expanded, setExpanded] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<Record<string, 'ingredients' | 'steps'>>({});
  const [steps, setSteps] = useState<Record<string, TechnologyStep[]>>({});
  const [loadingSteps, setLoadingSteps] = useState('');
  const [stepOpen, setStepOpen] = useState(false);
  const [savingStep, setSavingStep] = useState(false);
  const [editStepId, setEditStepId] = useState<string | null>(null);
  const [stepForm, setStepForm] = useState(newStepForm('', 1));
  const stepsRef = useRef(steps);
  useEffect(() => { stepsRef.current = steps; }, [steps]);

  // ── Ingredient inline editing state ──────────────────────────────────────────
  const [editIngRecipeId, setEditIngRecipeId] = useState<string | null>(null);
  const [editIngRows, setEditIngRows] = useState<ReturnType<typeof newIng>[]>([]);
  const [savingIngredients, setSavingIngredients] = useState(false);

  const startEditIngredients = (r: Recipe) => {
    setEditIngRecipeId(r.recipeId);
    setEditIngRows(
      (r.ingredients ?? []).map((ing) => ({
        itemId: ing.itemId,
        itemName: ing.itemName ?? '',
        unitMode: ing.unitMode ?? 'WEIGHT',
        recipeQty: ing.recipeQty,
        recipeUom: ing.recipeUom,
        purchasingUnitSize: ing.purchasingUnitSize,
        purchasingUom: ing.purchasingUom,
        wasteFactor: ing.wasteFactor,
      }))
    );
  };

  const cancelEditIngredients = () => {
    setEditIngRecipeId(null);
    setEditIngRows([]);
  };

  const saveIngredients = async (recipeId: string) => {
    try {
      setSavingIngredients(true);
      await apiFetch(`/v1/recipes/${recipeId}/ingredients?tenantId=${TENANT_ID}`, {
        method: 'PUT',
        body: JSON.stringify(editIngRows),
      });
      cancelEditIngredients();
      // Reload recipes for current product
      if (selectedPid) {
        const updated = await apiFetch<Recipe[]>(`/v1/recipes?tenantId=${TENANT_ID}&productId=${selectedPid}`);
        setRecipes(updated);
      }
    } catch (e) {
      setError(String(e));
    } finally {
      setSavingIngredients(false);
    }
  };
  const [form, setForm] = useState({
    batchSize: 50,
    batchSizeUom: 'PCS',
    expectedYield: 50,
    yieldUom: 'PCS',
    productionNotes: '',
    leadTimeHours: 8,
    ingredients: [newIng()],
  });

  useEffect(() => {
    Promise.all([
      apiFetch<Product[]>(`/v1/products?tenantId=${TENANT_ID}`),
      apiFetch<Dept[]>(`/v1/departments?tenantId=${TENANT_ID}`),
    ])
      .then(([prods, deps]) => { setProducts(prods); setDepts(deps); })
      .catch((e) => setError(String(e)))
      .finally(() => setInitLoading(false));
  }, []);

  const loadRecipes = useCallback(async (pid: string) => {
    if (!pid) {
      setRecipes([]);
      return;
    }
    try {
      setLoading(true);
      setError('');
      const data = await apiFetch<Recipe[]>(
        `/v1/recipes?tenantId=${TENANT_ID}&productId=${pid}`
      );
      setRecipes(data);
    } catch (e) {
      setError(String(e));
      setRecipes([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const selectProduct = (id: string) => {
    setSelectedPid(id);
    setExpanded(null);
    loadRecipes(id);
  };

  const loadSteps = useCallback(async (recipeId: string, force = false) => {
    if (stepsRef.current[recipeId] && !force) return;
    try {
      setLoadingSteps(recipeId);
      const data = await apiFetch<TechnologyStep[]>(
        `/v1/technology-steps?tenantId=${TENANT_ID}&recipeId=${recipeId}`
      );
      setSteps((prev) => ({ ...prev, [recipeId]: data }));
    } catch {
      setSteps((prev) => ({ ...prev, [recipeId]: [] }));
    } finally {
      setLoadingSteps('');
    }
  }, []);

  const toggleExpand = (recipeId: string) => {
    if (expanded === recipeId) {
      setExpanded(null);
    } else {
      setExpanded(recipeId);
      loadSteps(recipeId);
      setActiveTab((prev) => ({ ...prev, [recipeId]: prev[recipeId] ?? 'ingredients' }));
    }
  };

  const openAddStep = (recipeId: string) => {
    const existing = stepsRef.current[recipeId] ?? [];
    const nextNum = existing.length > 0 ? Math.max(...existing.map((s) => s.stepNumber)) + 1 : 1;
    setStepForm(newStepForm(recipeId, nextNum));
    setEditStepId(null);
    setStepOpen(true);
  };

  const openEditStep = (step: TechnologyStep) => {
    setStepForm({
      recipeId: step.recipeId,
      stepNumber: step.stepNumber,
      name: step.name,
      activities: step.activities ?? '',
      instruments: step.instruments ?? '',
      durationMinutes: step.durationMinutes ?? 30,
      temperatureCelsius:
        step.temperatureCelsius !== null && step.temperatureCelsius !== undefined
          ? step.temperatureCelsius
          : '',
    });
    setEditStepId(step.stepId);
    setStepOpen(true);
  };

  const submitStep = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSavingStep(true);
      const payload = {
        ...stepForm,
        durationMinutes: stepForm.durationMinutes || null,
        temperatureCelsius: stepForm.temperatureCelsius !== '' ? Number(stepForm.temperatureCelsius) : null,
      };
      if (editStepId) {
        await apiFetch(`/v1/technology-steps/${editStepId}?tenantId=${TENANT_ID}`, {
          method: 'PUT', body: JSON.stringify(payload),
        });
      } else {
        await apiFetch(`/v1/technology-steps?tenantId=${TENANT_ID}`, {
          method: 'POST', body: JSON.stringify(payload),
        });
      }
      setStepOpen(false);
      loadSteps(stepForm.recipeId, true);
    } catch (e) {
      setError(String(e));
    } finally {
      setSavingStep(false);
    }
  };

  const deleteStep = async (stepId: string, recipeId: string) => {
    if (!await askConfirm(t('recipes.deleteStep'))) return;
    try {
      await apiFetch(`/v1/technology-steps/${stepId}?tenantId=${TENANT_ID}`, { method: 'DELETE' });
      loadSteps(recipeId, true);
    } catch (e) {
      setError(String(e));
    }
  };

  const activate = async (recipeId: string) => {
    if (!await askConfirm(t('recipes.confirmActivate'))) return;
    try {
      setActivating(recipeId);
      await apiFetch(`/v1/recipes/${recipeId}/activate?tenantId=${TENANT_ID}`, {
        method: 'POST',
      });
      loadRecipes(selectedPid);
    } catch (e) {
      setError(String(e));
    } finally {
      setActivating('');
    }
  };

  const addIng = () =>
    setForm((f) => ({ ...f, ingredients: [...f.ingredients, newIng()] }));
  const removeIng = (i: number) =>
    setForm((f) => ({
      ...f,
      ingredients: f.ingredients.filter((_, j) => j !== i),
    }));
  const updateIng = (i: number, field: string, value: unknown) =>
    setForm((f) => ({
      ...f,
      ingredients: f.ingredients.map((ing, j) =>
        j === i ? { ...ing, [field]: value } : ing
      ),
    }));

  const openForm = () => {
    setForm({
      batchSize: 50,
      batchSizeUom: 'PCS',
      expectedYield: 50,
      yieldUom: 'PCS',
      productionNotes: '',
      leadTimeHours: 8,
      ingredients: [newIng()],
    });
    setOpen(true);
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v1/recipes', {
        method: 'POST',
        body: JSON.stringify({
          tenantId: TENANT_ID,
          productId: selectedPid,
          ...form,
        }),
      });
      setOpen(false);
      loadRecipes(selectedPid);
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-[1800px]">
      <SectionTitle
        eyebrow={t('recipes.workshop') ?? 'Workshop'}
        title={t('recipes.title')}
        subtitle={t('recipes.subtitle') ?? 'Select a product above to view its recipes.'}
        action={
          <Button
            variant="primary"
            size="sm"
            disabled={!selectedPid}
            onClick={openForm}
          >
            <Plus className="h-4 w-4" /> {t('recipes.newRecipe')}
          </Button>
        }
      />

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {initLoading ? (
        <Spinner />
      ) : (
        <>
          {/* ── Department selector (BC-1707) ── */}
          <div className="flex gap-4 mb-5">
            <div className="max-w-xs flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('recipes.selectDepartment')}</label>
              <select className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500" value={selectedDeptId}
                onChange={(e) => { setSelectedDeptId(e.target.value); setSelectedPid(''); setRecipes([]); }}>
                <option value="">{t('recipes.allDepartments')}</option>
                {depts.map((d) => <option key={d.departmentId} value={d.departmentId}>{d.name}</option>)}
              </select>
            </div>
            <div className="max-w-xs flex-1">
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('recipes.selectProduct')}</label>
              <select className="w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500" value={selectedPid} onChange={(e) => selectProduct(e.target.value)}>
                <option value="">{t('recipes.chooseProduct')}</option>
                {products
                  .filter((p) => !selectedDeptId || p.departmentId === selectedDeptId)
                  .map((p) => <option key={p.productId} value={p.productId}>{p.name}</option>)}
              </select>
            </div>
          </div>

          {loading ? (
            <Spinner />
          ) : selectedPid ? (
            <div className="space-y-2">
              {recipes.length === 0 ? (
                <div className="text-sm text-gray-400 py-12 text-center rounded-2xl border border-gray-200 bg-white shadow-sm">
                  {t('recipes.noRecipes')}
                </div>
              ) : (
                recipes.map((r) => {
                  const tab = activeTab[r.recipeId] ?? 'ingredients';
                  const recipeSteps = steps[r.recipeId] ?? [];
                  return (
                  <div key={r.recipeId} className="rounded-2xl border border-gray-200 bg-white shadow-sm">
                    <div
                      className="flex items-center gap-4 px-4 py-3 cursor-pointer hover:bg-gray-50 rounded-t-2xl"
                      onClick={() => toggleExpand(r.recipeId)}
                    >
                      <span className="font-medium text-sm">v{r.version}</span>
                      <Badge status={r.status} />
                      <span className="text-sm text-gray-500">
                        {t('recipes.batch')}: {r.batchSize} {r.batchSizeUom}
                      </span>
                      <span className="text-sm text-gray-500">
                        {t('recipes.yield')}: {r.expectedYield} {r.yieldUom}
                      </span>
                      {r.leadTimeHours && (
                        <span className="inline-flex items-center gap-1 text-sm text-blue-600 font-medium">
                          <Clock3 className="h-3.5 w-3.5" /> {r.leadTimeHours}h
                        </span>
                      )}
                      <span className="text-xs text-gray-400 ml-auto">
                        {r.ingredients?.length ?? 0} {t('recipes.ingr')} · {recipeSteps.length} steps
                      </span>
                      {r.status === 'DRAFT' && (
                        <Button
                          variant="success"
                          size="xs"
                          disabled={activating === r.recipeId}
                          onClick={(ev) => { ev.stopPropagation(); activate(r.recipeId); }}
                        >
                          <Check className="h-3.5 w-3.5" />
                          {activating === r.recipeId ? t('recipes.activating') : t('recipes.activate')}
                        </Button>
                      )}
                      <span className="text-gray-400">
                        {expanded === r.recipeId ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                      </span>
                    </div>

                    {expanded === r.recipeId && (
                      <div className="border-t">
                        {/* Tab bar */}
                        <div className="flex border-b px-4 bg-gray-50">
                          {(['ingredients', 'steps'] as const).map((tabKey) => (
                            <button
                              key={tabKey}
                              className={`px-4 py-2 text-xs font-semibold border-b-2 transition-colors ${
                                tab === tabKey
                                  ? 'border-blue-600 text-blue-700'
                                  : 'border-transparent text-gray-500 hover:text-gray-700'
                              }`}
                              onClick={() => setActiveTab((prev) => ({ ...prev, [r.recipeId]: tabKey }))}
                            >
                              {tabKey === 'ingredients'
                                ? `${t('recipes.ingredients')} (${r.ingredients?.length ?? 0})`
                                : `${t('recipes.steps')} (${recipeSteps.length})`}
                            </button>
                          ))}
                        </div>

                        {/* Ingredients tab */}
                        {tab === 'ingredients' && (
                          <div className="px-4 py-3">
                            {editIngRecipeId === r.recipeId ? (
                              /* ── EDIT MODE ── */
                              <div>
                                <div className="flex items-center justify-between mb-2">
                                  <span className="text-xs font-semibold text-gray-600">{t('recipes.editingIngredients')}</span>
                                  <div className="flex gap-2">
                                    <Button variant="secondary" size="xs" onClick={cancelEditIngredients}>
                                      {t('common.cancel')}
                                    </Button>
                                    <Button
                                      variant="primary" size="xs" className="bg-green-600 hover:bg-green-700"
                                      disabled={savingIngredients}
                                      onClick={() => saveIngredients(r.recipeId)}
                                    >
                                      {savingIngredients ? t('common.saving') : t('common.save')}
                                    </Button>
                                  </div>
                                </div>
                                <table className="w-full text-xs mb-2">
                                  <thead>
                                    <tr className="text-gray-500">
                                      <th className="text-left py-1 pr-2 w-32">{t('recipes.itemId')}</th>
                                      <th className="text-left py-1 pr-2 w-28">{t('common.name')}</th>
                                      <th className="text-left py-1 pr-2 w-20">{t('recipes.qtyPerBatch')}</th>
                                      <th className="text-left py-1 pr-2 w-16">{t('recipes.uom')}</th>
                                      <th className="text-left py-1 pr-2 w-20">{t('recipes.buySize')}</th>
                                      <th className="text-left py-1 pr-2 w-16">{t('recipes.buyUom')}</th>
                                      <th className="text-left py-1 pr-2 w-16">{t('recipes.waste')}</th>
                                      <th />
                                    </tr>
                                  </thead>
                                  <tbody className="divide-y divide-gray-100">
                                    {editIngRows.map((row, idx) => (
                                      <tr key={idx}>
                                        <td className="py-1 pr-2">
                                          <input className="input text-xs py-0.5" value={row.itemId}
                                            onChange={(e) => setEditIngRows((rows) => rows.map((r2, i) => i === idx ? { ...r2, itemId: e.target.value } : r2))} />
                                        </td>
                                        <td className="py-1 pr-2">
                                          <input className="input text-xs py-0.5" value={row.itemName}
                                            onChange={(e) => setEditIngRows((rows) => rows.map((r2, i) => i === idx ? { ...r2, itemName: e.target.value } : r2))} />
                                        </td>
                                        <td className="py-1 pr-2">
                                          <input className="input text-xs py-0.5" type="number" value={row.recipeQty}
                                            onChange={(e) => setEditIngRows((rows) => rows.map((r2, i) => i === idx ? { ...r2, recipeQty: Number(e.target.value) } : r2))} />
                                        </td>
                                        <td className="py-1 pr-2">
                                          <input className="input text-xs py-0.5" value={row.recipeUom}
                                            onChange={(e) => setEditIngRows((rows) => rows.map((r2, i) => i === idx ? { ...r2, recipeUom: e.target.value } : r2))} />
                                        </td>
                                        <td className="py-1 pr-2">
                                          <input className="input text-xs py-0.5" type="number" value={row.purchasingUnitSize}
                                            onChange={(e) => setEditIngRows((rows) => rows.map((r2, i) => i === idx ? { ...r2, purchasingUnitSize: Number(e.target.value) } : r2))} />
                                        </td>
                                        <td className="py-1 pr-2">
                                          <input className="input text-xs py-0.5" value={row.purchasingUom}
                                            onChange={(e) => setEditIngRows((rows) => rows.map((r2, i) => i === idx ? { ...r2, purchasingUom: e.target.value } : r2))} />
                                        </td>
                                        <td className="py-1 pr-2">
                                          <input className="input text-xs py-0.5" type="number" step="0.01" value={row.wasteFactor}
                                            onChange={(e) => setEditIngRows((rows) => rows.map((r2, i) => i === idx ? { ...r2, wasteFactor: Number(e.target.value) } : r2))} />
                                        </td>
                                        <td className="py-1">
                                          <button className="text-red-500 hover:text-red-700 text-xs"
                                            onClick={() => setEditIngRows((rows) => rows.filter((_, i) => i !== idx))}>
                                            ✕
                                          </button>
                                        </td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                                <Button
                                  variant="secondary" size="xs" className="bg-blue-100 text-blue-700 hover:bg-blue-200"
                                  onClick={() => setEditIngRows((rows) => [...rows, newIng()])}
                                >
                                  {t('recipes.addRow')}
                                </Button>
                              </div>
                            ) : (
                              /* ── READ MODE ── */
                              <div>
                                <div className="flex items-center justify-between mb-2">
                                  <span />
                                  {r.status === 'DRAFT' && (
                                    <Button
                                      variant="primary" size="xs" className="bg-amber-500 hover:bg-amber-600"
                                      onClick={() => startEditIngredients(r)}
                                    >
                                      {t('recipes.editIngredients')}
                                    </Button>
                                  )}
                                </div>
                                <table className="w-full text-xs">
                                  <thead>
                                    <tr className="text-gray-500">
                                      <th className="text-left py-1 pr-4">{t('recipes.item')}</th>
                                      <th className="text-left py-1 pr-4">{t('recipes.unitMode')}</th>
                                      <th className="text-left py-1 pr-4">{t('recipes.qtyPerBatch')}</th>
                                      <th className="text-left py-1 pr-4">{t('recipes.uom')}</th>
                                      <th className="text-left py-1 pr-4">{t('recipes.waste')}</th>
                                      <th className="text-left py-1 pr-4">{t('recipes.purchaseSize')}</th>
                                      <th className="text-left py-1">{t('recipes.purchaseUom')}</th>
                                    </tr>
                                  </thead>
                                  <tbody className="divide-y divide-gray-100">
                                    {r.ingredients?.map((ing) => (
                                      <tr key={ing.ingredientId}>
                                        <td className="py-1 pr-4 font-medium">{ing.itemName || ing.itemId}</td>
                                        <td className="py-1 pr-4">{ing.unitMode}</td>
                                        <td className="py-1 pr-4">{ing.recipeQty}</td>
                                        <td className="py-1 pr-4">{ing.recipeUom}</td>
                                        <td className="py-1 pr-4">{(ing.wasteFactor * 100).toFixed(0)}%</td>
                                        <td className="py-1 pr-4">{ing.purchasingUnitSize}</td>
                                        <td className="py-1">{ing.purchasingUom}</td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                                {r.productionNotes && (
                                  <div className="mt-3 bg-amber-50 border border-amber-200 rounded-lg p-3 text-xs text-gray-700 whitespace-pre-wrap">
                                    <span className="font-semibold block mb-1 text-amber-700">{t('recipes.processNotes')}</span>
                                    {r.productionNotes}
                                  </div>
                                )}
                              </div>
                            )}
                          </div>
                        )}

                        {/* Technology Steps tab */}
                        {tab === 'steps' && (
                          <div className="px-4 py-3">
                            <div className="flex items-center justify-between mb-3">
                              <span className="text-xs text-gray-500">{t('recipes.stepGuide')}</span>
                              <Button
                                variant="primary" size="xs"
                                onClick={() => openAddStep(r.recipeId)}
                              >
                                {t('recipes.addStep')}
                              </Button>
                            </div>
                            {loadingSteps === r.recipeId ? (
                              <p className="text-xs text-gray-400">{t('common.loading')}</p>
                            ) : recipeSteps.length === 0 ? (
                              <p className="text-xs text-gray-400 py-4 text-center border rounded-lg bg-gray-50">
                                {t('recipes.noSteps')}
                              </p>
                            ) : (
                              <div className="space-y-2">
                                {recipeSteps.map((step) => (
                                  <div key={step.stepId} className="flex gap-3 border rounded-lg p-3 bg-gray-50 text-xs">
                                    <div className="flex-shrink-0 w-7 h-7 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-sm">
                                      {step.stepNumber}
                                    </div>
                                    <div className="flex-1 min-w-0">
                                      <div className="font-semibold text-gray-800">{step.name}</div>
                                      {step.activities && (
                                        <div className="text-gray-600 mt-0.5 whitespace-pre-line">{step.activities}</div>
                                      )}
                                      <div className="flex gap-3 mt-1 text-gray-400">
                                        {step.instruments && <span>🔧 {step.instruments}</span>}
                                        {step.durationMinutes && <span>⏱ {step.durationMinutes} min</span>}
                                        {step.temperatureCelsius !== null && step.temperatureCelsius !== undefined && <span>🌡 {step.temperatureCelsius}°C</span>}
                                      </div>
                                    </div>
                                    <div className="flex gap-1 flex-shrink-0">
                                      <Button variant="secondary" size="xs" onClick={() => openEditStep(step)}>{t('common.edit')}</Button>
                                      <Button variant="danger" size="xs" onClick={() => deleteStep(step.stepId, step.recipeId)}>×</Button>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                  );
                })
              )}
            </div>
          ) : (
            <div className="text-sm text-gray-400 py-12 text-center border rounded-xl bg-white">
              Select a product above to view its recipes.
            </div>
          )}
        </>
      )}

      {open && (
        <Modal title={t('recipes.newRecipeTitle')} onClose={() => setOpen(false)} wide>
          <form onSubmit={submit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('recipes.batchSize')}>
                <div className="flex gap-2">
                  <input
                    className="input"
                    type="number"
                    min={1}
                    step="any"
                    required
                    value={form.batchSize}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, batchSize: +e.target.value }))
                    }
                  />
                  <input
                    className="input w-20"
                    placeholder={t('recipes.batchSizeUom')}
                    value={form.batchSizeUom}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, batchSizeUom: e.target.value }))
                    }
                  />
                </div>
              </Field>
              <Field label={t('recipes.expectedYield')}>
                <div className="flex gap-2">
                  <input
                    className="input"
                    type="number"
                    min={1}
                    step="any"
                    required
                    value={form.expectedYield}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, expectedYield: +e.target.value }))
                    }
                  />
                  <input
                    className="input w-20"
                    placeholder={t('recipes.yieldUom')}
                    value={form.yieldUom}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, yieldUom: e.target.value }))
                    }
                  />
                </div>
              </Field>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Field label={t('recipes.leadTimeHours')}>
                <input
                  type="number"
                  min={0}
                  className="input"
                  placeholder="e.g. 8"
                  value={form.leadTimeHours}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, leadTimeHours: +e.target.value }))
                  }
                />
              </Field>
            </div>

            <Field label={t('recipes.productionNotes')}>
              <textarea
                className="input min-h-[80px] resize-y"
                placeholder="Step-by-step process, temperatures, timings, quality checkpoints..."
                value={form.productionNotes}
                onChange={(e) =>
                  setForm((f) => ({ ...f, productionNotes: e.target.value }))
                }
              />
            </Field>

            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-sm font-medium text-gray-700">
                  {t('recipes.recipeIngredients')} ({form.ingredients.length})
                </span>
                <button
                  type="button"
                  className="text-xs text-blue-600 hover:underline"
                  onClick={addIng}
                >
                  {t('recipes.addIngredient')}
                </button>
              </div>
              <div className="space-y-3">
                {form.ingredients.map((ing, i) => (
                  <div key={i} className="border rounded-lg p-3 bg-gray-50">
                    <div className="flex justify-between items-center mb-2">
                      <span className="text-xs font-medium text-gray-600">
                        Ingredient {i + 1}
                      </span>
                      {form.ingredients.length > 1 && (
                        <button
                          type="button"
                          className="text-xs text-red-500 hover:underline"
                          onClick={() => removeIng(i)}
                        >
                          {t('recipes.removeIngredient')}
                        </button>
                      )}
                    </div>
                    <div className="grid grid-cols-2 gap-2 mb-2">
                      <div>
                        <label className="text-xs text-gray-500">{t('recipes.itemId')} *</label>
                        <input
                          className="input"
                          placeholder="e.g. flour-001"
                          required
                          value={ing.itemId}
                          onChange={(e) => updateIng(i, 'itemId', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">{t('recipes.itemName')}</label>
                        <input
                          className="input"
                          placeholder="e.g. Wheat Flour"
                          value={ing.itemName}
                          onChange={(e) => updateIng(i, 'itemName', e.target.value)}
                        />
                      </div>
                    </div>
                    <div className="grid grid-cols-4 gap-2">
                      <div>
                        <label className="text-xs text-gray-500">{t('recipes.qtyPerBatch')}</label>
                        <input
                          className="input"
                          type="number"
                          min={0}
                          step="any"
                          value={ing.recipeQty}
                          onChange={(e) =>
                            updateIng(i, 'recipeQty', +e.target.value)
                          }
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">{t('recipes.uom')}</label>
                        <input
                          className="input"
                          placeholder="G, KG…"
                          value={ing.recipeUom}
                          onChange={(e) => updateIng(i, 'recipeUom', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">
                          {t('recipes.purchaseSize')}
                        </label>
                        <input
                          className="input"
                          type="number"
                          min={0}
                          step="any"
                          value={ing.purchasingUnitSize}
                          onChange={(e) =>
                            updateIng(i, 'purchasingUnitSize', +e.target.value)
                          }
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">{t('recipes.purchaseUom')}</label>
                        <input
                          className="input"
                          placeholder="G, BAG…"
                          value={ing.purchasingUom}
                          onChange={(e) =>
                            updateIng(i, 'purchasingUom', e.target.value)
                          }
                        />
                      </div>
                    </div>
                    <div className="mt-2 max-w-xs">
                      <label className="text-xs text-gray-500">
                        {t('recipes.wasteFactor')}
                      </label>
                      <input
                        className="input"
                        type="number"
                        min={0}
                        max={1}
                        step="0.01"
                        value={ing.wasteFactor}
                        onChange={(e) =>
                          updateIng(i, 'wasteFactor', +e.target.value)
                        }
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="secondary" size="sm" onClick={() => setOpen(false)}>
                {t('common.cancel')}
              </Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>
                {saving ? t('common.saving') : t('common.create')}
              </Button>
            </div>
          </form>
        </Modal>
      )}

      {/* ── Technology Step modal ─────────────────────────────────────────────── */}
      {stepOpen && (
        <Modal title={editStepId ? t('recipes.editStepTitle') : t('recipes.addStepTitle')} onClose={() => setStepOpen(false)}>
          <form onSubmit={submitStep} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('recipes.stepNumber')}>
                <input className="input" type="number" min={1} required
                  value={stepForm.stepNumber}
                  onChange={(e) => setStepForm((f) => ({ ...f, stepNumber: +e.target.value }))} />
              </Field>
              <Field label={`${t('recipes.stepName')} *`}>
                <input className="input" placeholder="e.g. Mixing, Baking, Cooling" required
                  value={stepForm.name}
                  onChange={(e) => setStepForm((f) => ({ ...f, name: e.target.value }))} />
              </Field>
            </div>

            <Field label={t('recipes.activities')}>
              <textarea className="input min-h-[80px] resize-y"
                placeholder="What to do: temperatures, actions, quality checks…"
                value={stepForm.activities}
                onChange={(e) => setStepForm((f) => ({ ...f, activities: e.target.value }))} />
            </Field>

            <Field label={t('recipes.instruments')}>
              <input className="input" placeholder="e.g. Spiral mixer, Proofing cabinet, Deck oven"
                value={stepForm.instruments}
                onChange={(e) => setStepForm((f) => ({ ...f, instruments: e.target.value }))} />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label={t('recipes.duration')}>
                <input className="input" type="number" min={0}
                  value={stepForm.durationMinutes}
                  onChange={(e) => setStepForm((f) => ({ ...f, durationMinutes: +e.target.value }))} />
              </Field>
              <Field label={t('recipes.temperature')}>
                <input className="input" type="number" placeholder="e.g. 220"
                  value={stepForm.temperatureCelsius}
                  onChange={(e) => setStepForm((f) => ({ ...f, temperatureCelsius: e.target.value }))} />
              </Field>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="secondary" size="sm" onClick={() => setStepOpen(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={savingStep}>
                {savingStep ? t('common.saving') : editStepId ? t('common.save') : t('recipes.addStep')}
              </Button>
            </div>
          </form>
        </Modal>
      )}
      {confirmModal}
    </div>
  );
}
