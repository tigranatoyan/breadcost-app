'use client';
import { useState, useEffect, useCallback, useRef } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Modal, Spinner, Alert, Badge, Field } from '@/components/ui';

interface Product {
  productId: string;
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
  const [products, setProducts] = useState<Product[]>([]);
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
    apiFetch<Product[]>(`/v1/products?tenantId=${TENANT_ID}`)
      .then(setProducts)
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
      temperatureCelsius: step.temperatureCelsius != null ? step.temperatureCelsius : '',
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
    if (!confirm('Delete this technology step?')) return;
    try {
      await apiFetch(`/v1/technology-steps/${stepId}?tenantId=${TENANT_ID}`, { method: 'DELETE' });
      loadSteps(recipeId, true);
    } catch (e) {
      setError(String(e));
    }
  };

  const activate = async (recipeId: string) => {
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
    <div className="max-w-5xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">Recipes</h1>
        <button
          className="btn-primary"
          disabled={!selectedPid}
          onClick={openForm}
        >
          + New Recipe
        </button>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {initLoading ? (
        <Spinner />
      ) : (
        <>
          <div className="mb-5 max-w-xs">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Select Product
            </label>
            <select
              className="input"
              value={selectedPid}
              onChange={(e) => selectProduct(e.target.value)}
            >
              <option value="">-- choose a product --</option>
              {products.map((p) => (
                <option key={p.productId} value={p.productId}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>

          {loading ? (
            <Spinner />
          ) : selectedPid ? (
            <div className="space-y-2">
              {recipes.length === 0 ? (
                <div className="text-sm text-gray-400 py-12 text-center border rounded-xl bg-white">
                  No recipes for this product yet.
                </div>
              ) : (
                recipes.map((r) => {
                  const tab = activeTab[r.recipeId] ?? 'ingredients';
                  const recipeSteps = steps[r.recipeId] ?? [];
                  return (
                  <div key={r.recipeId} className="border rounded-xl bg-white shadow-sm">
                    <div
                      className="flex items-center gap-4 px-4 py-3 cursor-pointer hover:bg-gray-50"
                      onClick={() => toggleExpand(r.recipeId)}
                    >
                      <span className="font-medium text-sm">v{r.version}</span>
                      <Badge status={r.status} />
                      <span className="text-sm text-gray-500">
                        Batch: {r.batchSize} {r.batchSizeUom}
                      </span>
                      <span className="text-sm text-gray-500">
                        Yield: {r.expectedYield} {r.yieldUom}
                      </span>
                      {r.leadTimeHours && (
                        <span className="text-sm text-blue-600 font-medium">
                          ⏱ {r.leadTimeHours}h
                        </span>
                      )}
                      <span className="text-xs text-gray-400 ml-auto">
                        {r.ingredients?.length ?? 0} ingr · {recipeSteps.length} steps
                      </span>
                      {r.status === 'DRAFT' && (
                        <button
                          className="btn-xs bg-green-600 text-white hover:bg-green-700 ml-2"
                          disabled={activating === r.recipeId}
                          onClick={(ev) => { ev.stopPropagation(); activate(r.recipeId); }}
                        >
                          {activating === r.recipeId ? 'Activating…' : 'Activate'}
                        </button>
                      )}
                      <span className="text-gray-400 text-xs">
                        {expanded === r.recipeId ? '▲' : '▼'}
                      </span>
                    </div>

                    {expanded === r.recipeId && (
                      <div className="border-t">
                        {/* Tab bar */}
                        <div className="flex border-b px-4 bg-gray-50">
                          {(['ingredients', 'steps'] as const).map((t) => (
                            <button
                              key={t}
                              className={`px-4 py-2 text-xs font-semibold border-b-2 transition-colors ${
                                tab === t
                                  ? 'border-blue-600 text-blue-700'
                                  : 'border-transparent text-gray-500 hover:text-gray-700'
                              }`}
                              onClick={() => setActiveTab((prev) => ({ ...prev, [r.recipeId]: t }))}
                            >
                              {t === 'ingredients'
                                ? `Ingredients (${r.ingredients?.length ?? 0})`
                                : `Technology Steps (${recipeSteps.length})`}
                            </button>
                          ))}
                        </div>

                        {/* Ingredients tab */}
                        {tab === 'ingredients' && (
                          <div className="px-4 py-3">
                            <table className="w-full text-xs">
                              <thead>
                                <tr className="text-gray-500">
                                  <th className="text-left py-1 pr-4">Item</th>
                                  <th className="text-left py-1 pr-4">Qty/batch</th>
                                  <th className="text-left py-1 pr-4">UoM</th>
                                  <th className="text-left py-1 pr-4">Waste</th>
                                  <th className="text-left py-1 pr-4">Purchase size</th>
                                  <th className="text-left py-1">Purchase UoM</th>
                                </tr>
                              </thead>
                              <tbody className="divide-y divide-gray-100">
                                {r.ingredients?.map((ing) => (
                                  <tr key={ing.ingredientId}>
                                    <td className="py-1 pr-4 font-medium">{ing.itemName || ing.itemId}</td>
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
                                <span className="font-semibold block mb-1 text-amber-700">Process Notes</span>
                                {r.productionNotes}
                              </div>
                            )}
                          </div>
                        )}

                        {/* Technology Steps tab */}
                        {tab === 'steps' && (
                          <div className="px-4 py-3">
                            <div className="flex items-center justify-between mb-3">
                              <span className="text-xs text-gray-500">Step-by-step process shown to floor workers</span>
                              <button
                                className="btn-xs bg-blue-600 text-white hover:bg-blue-700"
                                onClick={() => openAddStep(r.recipeId)}
                              >
                                + Add Step
                              </button>
                            </div>
                            {loadingSteps === r.recipeId ? (
                              <p className="text-xs text-gray-400">Loading…</p>
                            ) : recipeSteps.length === 0 ? (
                              <p className="text-xs text-gray-400 py-4 text-center border rounded-lg bg-gray-50">
                                No technology steps yet. Click &quot;+ Add Step&quot; to define the production process.
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
                                        {step.temperatureCelsius != null && <span>🌡 {step.temperatureCelsius}°C</span>}
                                      </div>
                                    </div>
                                    <div className="flex gap-1 flex-shrink-0">
                                      <button className="btn-xs border text-gray-500 hover:bg-gray-100" onClick={() => openEditStep(step)}>Edit</button>
                                      <button className="btn-xs bg-red-50 text-red-600 hover:bg-red-100" onClick={() => deleteStep(step.stepId, step.recipeId)}>×</button>
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
        <Modal title="New Recipe" onClose={() => setOpen(false)} wide>
          <form onSubmit={submit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Batch Size">
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
                    placeholder="UoM"
                    value={form.batchSizeUom}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, batchSizeUom: e.target.value }))
                    }
                  />
                </div>
              </Field>
              <Field label="Expected Yield">
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
                    placeholder="UoM"
                    value={form.yieldUom}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, yieldUom: e.target.value }))
                    }
                  />
                </div>
              </Field>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Lead Time (hours)" hint="Hours from production start to delivery-ready">
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

            <Field label="Technological Process / Production Notes">
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
                  Ingredients ({form.ingredients.length})
                </span>
                <button
                  type="button"
                  className="text-xs text-blue-600 hover:underline"
                  onClick={addIng}
                >
                  + Add ingredient
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
                          Remove
                        </button>
                      )}
                    </div>
                    <div className="grid grid-cols-2 gap-2 mb-2">
                      <div>
                        <label className="text-xs text-gray-500">Item ID *</label>
                        <input
                          className="input"
                          placeholder="e.g. flour-001"
                          required
                          value={ing.itemId}
                          onChange={(e) => updateIng(i, 'itemId', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">Item Name</label>
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
                        <label className="text-xs text-gray-500">Qty / batch</label>
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
                        <label className="text-xs text-gray-500">UoM</label>
                        <input
                          className="input"
                          placeholder="G, KG…"
                          value={ing.recipeUom}
                          onChange={(e) => updateIng(i, 'recipeUom', e.target.value)}
                        />
                      </div>
                      <div>
                        <label className="text-xs text-gray-500">
                          Purchase unit size
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
                        <label className="text-xs text-gray-500">Purchase UoM</label>
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
                        Waste factor (0.02 = 2%)
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
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setOpen(false)}
              >
                Cancel
              </button>
              <button type="submit" className="btn-primary" disabled={saving}>
                {saving ? 'Saving…' : 'Create Recipe'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ── Technology Step modal ─────────────────────────────────────────────── */}
      {stepOpen && (
        <Modal title={editStepId ? 'Edit Technology Step' : 'Add Technology Step'} onClose={() => setStepOpen(false)}>
          <form onSubmit={submitStep} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Step Number">
                <input className="input" type="number" min={1} required
                  value={stepForm.stepNumber}
                  onChange={(e) => setStepForm((f) => ({ ...f, stepNumber: +e.target.value }))} />
              </Field>
              <Field label="Step Name *">
                <input className="input" placeholder="e.g. Mixing, Baking, Cooling" required
                  value={stepForm.name}
                  onChange={(e) => setStepForm((f) => ({ ...f, name: e.target.value }))} />
              </Field>
            </div>

            <Field label="Activities / Instructions">
              <textarea className="input min-h-[80px] resize-y"
                placeholder="What to do: temperatures, actions, quality checks…"
                value={stepForm.activities}
                onChange={(e) => setStepForm((f) => ({ ...f, activities: e.target.value }))} />
            </Field>

            <Field label="Instruments / Equipment">
              <input className="input" placeholder="e.g. Spiral mixer, Proofing cabinet, Deck oven"
                value={stepForm.instruments}
                onChange={(e) => setStepForm((f) => ({ ...f, instruments: e.target.value }))} />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Duration (minutes)">
                <input className="input" type="number" min={0}
                  value={stepForm.durationMinutes}
                  onChange={(e) => setStepForm((f) => ({ ...f, durationMinutes: +e.target.value }))} />
              </Field>
              <Field label="Temperature (°C, optional)">
                <input className="input" type="number" placeholder="e.g. 220"
                  value={stepForm.temperatureCelsius}
                  onChange={(e) => setStepForm((f) => ({ ...f, temperatureCelsius: e.target.value }))} />
              </Field>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <button type="button" className="btn-secondary" onClick={() => setStepOpen(false)}>Cancel</button>
              <button type="submit" className="btn-primary" disabled={savingStep}>
                {savingStep ? 'Saving…' : editStepId ? 'Save Changes' : 'Add Step'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
