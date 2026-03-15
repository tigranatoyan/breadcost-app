'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Modal, Spinner, Alert, Success, Badge, Field, useConfirm } from '@/components/ui';
import { SectionTitle, Button, SelectField, InputField } from '@/components/design-system';
import { useT } from '@/lib/i18n';
import { Plus } from 'lucide-react';

interface WorkOrder {
  workOrderId: string;
  productName: string;
  departmentName: string;
  targetQty: number;
  uom: string;
  batchCount: number;
  status: string;
  startedAt: string | null;
  completedAt: string | null;
  startOffsetHours: number;
  durationHours: number | null;
  actualYield: number | null;
}
interface Plan {
  planId: string;
  planDate: string;
  shift: string;
  status: string;
  notes: string;
  workOrders: WorkOrder[];
}

interface MatReq {
  itemName: string;
  purchasingUnitsNeeded: number;
  purchasingUom: string;
}

interface PlanScheduleEntry {
  workOrderId: string;
  productName: string;
  departmentName: string;
  status: string;
  startOffsetHours: number;
  durationHours: number | null;
  endOffsetHours: number | null;
  parallel: boolean;
  criticalPath: boolean;
  batchCount: number;
  targetQty: number;
  uom: string;
}

interface PlanSchedule {
  planId: string;
  planDate: string;
  shift: string;
  status: string;
  totalLeadTimeHours: number | null;
  workOrders: PlanScheduleEntry[];
}

export default function ProductionPlansPage() {
  const t = useT();
  const [askConfirm, confirmModal] = useConfirm({ confirmLabel: t('common.confirm'), cancelLabel: t('common.cancel') });
  const [plans, setPlans] = useState<Plan[]>([]);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [dateFilter, setDateFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [info, setInfo] = useState('');
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [actionId, setActionId] = useState('');
  const [expanded, setExpanded] = useState<string | null>(null);
  const [mats, setMats] = useState<Record<string, MatReq[]>>({});
  const [loadingMats, setLoadingMats] = useState('');
  const [schedules, setSchedules] = useState<Record<string, PlanSchedule>>({});
  const [loadingSchedule, setLoadingSchedule] = useState('');
  const [form, setForm] = useState({
    planDate: new Date().toISOString().substring(0, 10),
    shift: 'MORNING',
    notes: '',
  });

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const data = await apiFetch<Plan[]>(
        `/v1/production-plans?tenantId=${TENANT_ID}`
      );
      setPlans(data);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const reloadPlan = async (planId: string) => {
    try {
      const updated = await apiFetch<Plan>(
        `/v1/production-plans/${planId}?tenantId=${TENANT_ID}`
      );
      setPlans((prev) =>
        prev.map((p) => (p.planId === planId ? updated : p))
      );
    } catch (e) {
      setError(String(e));
    }
  };

  const planAction = async (planId: string, action: string) => {
    if (actionId) return;
    if (action === 'approve' && !await askConfirm(t('productionPlans.confirmApprove'))) return;
    try {
      setActionId(`${planId}-${action}`);
      const result = await apiFetch<Plan>(
        `/v1/production-plans/${planId}/${action}?tenantId=${TENANT_ID}`,
        { method: 'POST' }
      );
      if (action === 'generate') {
        const count = result.workOrders?.length ?? 0;
        if (count === 0) {
          setInfo(t('productionPlans.zeroWorkOrders'));
        } else {
          setInfo(t('productionPlans.workOrdersGenerated').replace('{count}', String(count)));
        }
      }
      await reloadPlan(planId);
    } catch (e) {
      setError(String(e));
    } finally {
      setActionId('');
    }
  };

  const [yieldInputs, setYieldInputs] = useState<Record<string, string>>({});

  const woAction = async (
    planId: string,
    woId: string,
    action: string
  ) => {
    try {
      setActionId(woId);
      const body: Record<string, unknown> = {};
      if (action === 'complete' && yieldInputs[woId]) {
        body.actualYield = parseFloat(yieldInputs[woId]);
      }
      await apiFetch(
        `/v1/production-plans/work-orders/${woId}/${action}?tenantId=${TENANT_ID}`,
        { method: 'POST', body: Object.keys(body).length > 0 ? JSON.stringify(body) : undefined }
      );
      await reloadPlan(planId);
    } catch (e) {
      setError(String(e));
    } finally {
      setActionId('');
    }
  };

  const loadMats = async (planId: string) => {
    if (mats[planId]) return;
    try {
      setLoadingMats(planId);
      const data = await apiFetch<MatReq[]>(
        `/v1/production-plans/${planId}/material-requirements?tenantId=${TENANT_ID}`
      );
      setMats((prev) => ({ ...prev, [planId]: data }));
    } catch (e) {
      setError(String(e));
    } finally {
      setLoadingMats('');
    }
  };

  const loadSchedule = async (planId: string, force = false) => {
    if (schedules[planId] && !force) return;
    try {
      setLoadingSchedule(planId);
      const data = await apiFetch<PlanSchedule>(
        `/v1/production-plans/${planId}/schedule?tenantId=${TENANT_ID}`
      );
      setSchedules((prev) => ({ ...prev, [planId]: data }));
    } catch {
      // schedule may not be available yet — silently ignore
    } finally {
      setLoadingSchedule('');
    }
  };

  const patchSchedule = async (
    planId: string,
    woId: string,
    startOffsetHours: number,
    durationHours: number | null
  ) => {
    try {
      await apiFetch(
        `/v1/production-plans/work-orders/${woId}/schedule?tenantId=${TENANT_ID}`,
        {
          method: 'PATCH',
          body: JSON.stringify({ startOffsetHours, durationHours }),
        }
      );
      await loadSchedule(planId, true);
    } catch (e) {
      setError(String(e));
    }
  };

  const toggleExpand = (planId: string) => {
    if (expanded === planId) {
      setExpanded(null);
    } else {
      setExpanded(planId);
      loadMats(planId);
      loadSchedule(planId);
    }
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (saving) return;
    try {
      setSaving(true);
      await apiFetch('/v1/production-plans', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, siteId: 'main', ...form }),
      });
      setOpen(false);
      setForm({
        planDate: new Date().toISOString().substring(0, 10),
        shift: 'MORNING',
        notes: '',
      });
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  const planBtns = (p: Plan) => {
    const btns: { label: string; action: string; cls: string }[] = [];
    const busy = actionId.startsWith(p.planId);
    if (p.status === 'DRAFT') {
      btns.push({
        label: t('productionPlans.generateWorkOrders'),
        action: 'generate',
        cls: 'bg-indigo-600 text-white hover:bg-indigo-700',
      });
    }
    if (p.status === 'GENERATED') {
      btns.push({
        label: t('productionPlans.approve'),
        action: 'approve',
        cls: 'bg-purple-600 text-white hover:bg-purple-700',
      });
    }
    if (p.status === 'APPROVED' || p.status === 'PUBLISHED') {
      btns.push({
        label: t('productionPlans.start'),
        action: 'start',
        cls: 'bg-orange-600 text-white hover:bg-orange-700',
      });
    }
    if (p.status === 'IN_PROGRESS') {
      btns.push({
        label: t('productionPlans.complete'),
        action: 'complete',
        cls: 'bg-green-600 text-white hover:bg-green-700',
      });
    }
    return btns.map((b) => (
      <button
        key={b.action}
        className={`btn-xs ${b.cls}`}
        disabled={busy}
        onClick={(e) => {
          e.stopPropagation();
          planAction(p.planId, b.action);
        }}
      >
        {busy ? '…' : b.label}
      </button>
    ));
  };

  const filteredPlans = plans.filter((p) => {
    if (statusFilter !== 'ALL' && p.status !== statusFilter) return false;
    if (dateFilter && p.planDate !== dateFilter) return false;
    return true;
  });

  return (
    <div className="max-w-[1800px]">
      <div className="mb-4">
        <SectionTitle
          eyebrow="Production"
          title={t('productionPlans.title')}
          subtitle={t('productionPlans.subtitle')}
          action={<Button variant="primary" size="sm" onClick={() => setOpen(true)}><Plus className="h-4 w-4" /> {t('productionPlans.newPlan')}</Button>}
        />
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 mb-4">
        <SelectField
          className="w-44"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          options={[
            { value: 'ALL', label: t('common.allStatuses') },
            ...['DRAFT', 'GENERATED', 'APPROVED', 'PUBLISHED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].map((s) => ({ value: s, label: s })),
          ]}
        />
        <InputField
          className="w-44"
          type="date"
          value={dateFilter}
          onChange={(e) => setDateFilter(e.target.value)}
        />
        {(statusFilter !== 'ALL' || dateFilter) && (
          <button
            className="text-xs text-gray-500 hover:text-gray-700 underline"
            onClick={() => { setStatusFilter('ALL'); setDateFilter(''); }}
          >
            {t('common.clearFilters')}
          </button>
        )}
        <span className="ml-auto text-xs text-gray-400 self-center">
          {t('productionPlans.planCount', {count: filteredPlans.length})}
        </span>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {info && <Success msg={info} onClose={() => setInfo('')} />}

      {loading ? (
        <Spinner />
      ) : (
        <div className="space-y-2">
          {filteredPlans.length === 0 && (
            <div className="text-center py-16 text-sm text-gray-400 rounded-2xl border border-gray-200 bg-white">
              {plans.length === 0 ? t('productionPlans.noPlans') : t('productionPlans.noPlansMatch')}
            </div>
          )}
          {filteredPlans.map((p) => (
            <div key={p.planId} className="rounded-2xl border border-gray-200 bg-white shadow-sm">
              {/* Plan header row */}
              <div
                className="flex items-center gap-3 px-4 py-3 cursor-pointer hover:bg-gray-50"
                onClick={() => toggleExpand(p.planId)}
              >
                <span className="font-medium text-sm">{p.planDate}</span>
                <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
                  {t(`productionPlans.shifts.${p.shift}` as any) || p.shift}
                </span>
                <Badge status={p.status} />
                <span className="text-xs text-gray-400">
                  {t('productionPlans.workOrderCount', { count: p.workOrders?.length ?? 0 })}
                </span>
                {p.notes && (
                  <span className="text-xs text-gray-400 italic truncate max-w-xs">
                    {p.notes}
                  </span>
                )}
                <div
                  className="flex gap-1 ml-auto shrink-0"
                  onClick={(e) => e.stopPropagation()}
                >
                  {planBtns(p)}
                </div>
                <span className="text-gray-400 text-xs shrink-0">
                  {expanded === p.planId ? '▲' : '▼'}
                </span>
              </div>

              {/* Expanded: work orders + materials */}
              {expanded === p.planId && (
                <div className="border-t">
                  {/* Work orders */}
                  <div className="px-4 py-3">
                    <h3 className="text-xs font-semibold text-gray-500 uppercase mb-2">
                      {t('productionPlans.workOrders')}
                    </h3>
                    {!p.workOrders || p.workOrders.length === 0 ? (
                      <p className="text-xs text-gray-400">
                        {t('productionPlans.noWorkOrders')}
                      </p>
                    ) : (
                      <table className="w-full text-xs">
                        <thead>
                          <tr className="text-gray-500">
                            <th className="text-left py-1 pr-3">{t('productionPlans.cols.product')}</th>
                            <th className="text-left py-1 pr-3">{t('productionPlans.cols.department')}</th>
                            <th className="text-left py-1 pr-3">{t('productionPlans.cols.qty')}</th>
                            <th className="text-left py-1 pr-3">{t('productionPlans.cols.batches')}</th>
                            <th className="text-left py-1 pr-3">{t('productionPlans.cols.status')}</th>
                            <th className="text-left py-1 pr-3">{t('productionPlans.cols.yield')}</th>
                            <th className="text-left py-1">{t('productionPlans.cols.actions')}</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                          {p.workOrders.map((wo) => (
                            <tr key={wo.workOrderId}>
                              <td className="py-1.5 pr-3 font-medium">
                                {wo.productName}
                              </td>
                              <td className="py-1.5 pr-3">{wo.departmentName}</td>
                              <td className="py-1.5 pr-3">
                                {wo.targetQty} {wo.uom}
                              </td>
                              <td className="py-1.5 pr-3">{wo.batchCount}</td>
                              <td className="py-1.5 pr-3">
                                <Badge status={wo.status} />
                              </td>
                              <td className="py-1.5 pr-3">
                                {wo.status === 'STARTED' && (
                                  <input
                                    type="number"
                                    min={0}
                                    step="0.1"
                                    className="w-20 border rounded px-1.5 py-0.5 text-xs"
                                    placeholder={t('productionPlans.yieldPlaceholder')}
                                    value={yieldInputs[wo.workOrderId] ?? ''}
                                    onChange={(e) => setYieldInputs((prev) => ({ ...prev, [wo.workOrderId]: e.target.value }))}
                                  />
                                )}
                                {wo.completedAt && wo.actualYield != null && (
                                  <span className="text-xs text-gray-600">
                                    {wo.actualYield} {wo.uom}
                                  </span>
                                )}
                              </td>
                              <td className="py-1.5">
                                <div className="flex gap-1">
                                  {wo.status === 'PENDING' && (
                                    <button
                                      className="btn-xs bg-blue-600 text-white hover:bg-blue-700"
                                      disabled={actionId === wo.workOrderId}
                                      onClick={() =>
                                        woAction(p.planId, wo.workOrderId, 'start')
                                      }
                                    >
                                      {t('productionPlans.start')}
                                    </button>
                                  )}
                                  {wo.status === 'STARTED' && (
                                    <button
                                      className="btn-xs bg-green-600 text-white hover:bg-green-700"
                                      disabled={actionId === wo.workOrderId}
                                      onClick={() =>
                                        woAction(
                                          p.planId,
                                          wo.workOrderId,
                                          'complete'
                                        )
                                      }
                                    >
                                      {t('productionPlans.complete')}
                                    </button>
                                  )}
                                  {(wo.status === 'PENDING' ||
                                    wo.status === 'STARTED') && (
                                    <button
                                      className="btn-xs bg-red-100 text-red-700 hover:bg-red-200"
                                      disabled={actionId === wo.workOrderId}
                                      onClick={() =>
                                        woAction(p.planId, wo.workOrderId, 'cancel')
                                      }
                                    >
                                      {t('common.cancel')}
                                    </button>
                                  )}
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>

                  {/* Schedule / Gantt timeline */}
                  {(() => {
                    const sched = schedules[p.planId];
                    const total = sched?.totalLeadTimeHours ?? 0;
                    const entries = sched?.workOrders ?? [];
                    const activEntries = entries.filter(
                      (e) => e.status !== 'CANCELLED' && e.durationHours !== null && e.durationHours !== undefined
                    );
                    return (
                      <div className="border-t px-4 py-3">
                        <div className="flex items-center justify-between mb-2">
                          <h3 className="text-xs font-semibold text-gray-500 uppercase">
                            {t('productionPlans.scheduleLeadTime')}
                          </h3>
                          {loadingSchedule === p.planId ? (
                            <span className="text-xs text-gray-400">{t('common.loading')}</span>
                          ) : total > 0 ? (
                            <span className="text-xs font-medium text-indigo-700 bg-indigo-50 border border-indigo-200 px-2 py-0.5 rounded-full">
                              {t('common.total')}: {total}h
                            </span>
                          ) : null}
                        </div>
                        {activEntries.length === 0 ? (
                          <p className="text-xs text-gray-400">
                            {loadingSchedule === p.planId
                              ? ''
                              : t('productionPlans.noSchedule')}
                          </p>
                        ) : (
                          <div className="space-y-2">
                            {/* Time axis labels */}
                            <div className="relative h-4 text-xs text-gray-400 select-none">
                              <span className="absolute left-0">0h</span>
                              <span className="absolute left-1/2 -translate-x-1/2">{Math.round(total / 2)}h</span>
                              <span className="absolute right-0">{total}h</span>
                            </div>
                            {activEntries.map((entry) => {
                              const startPct = total > 0 ? (entry.startOffsetHours / total) * 100 : 0;
                              const widthPct = total > 0 && entry.durationHours
                                ? (entry.durationHours / total) * 100
                                : 0;
                              const barCls = entry.criticalPath
                                ? 'bg-blue-500'
                                : entry.parallel
                                ? 'bg-emerald-500'
                                : 'bg-gray-400';
                              return (
                                <div key={entry.workOrderId} className="flex items-center gap-2">
                                  {/* Label */}
                                  <div className="w-32 shrink-0 text-right">
                                    <div className="text-xs font-medium truncate">{entry.productName}</div>
                                    <div className="text-xs text-gray-400 truncate">{entry.departmentName}</div>
                                  </div>
                                  {/* Gantt bar */}
                                  <div className="flex-1 relative h-7 bg-gray-100 rounded overflow-hidden">
                                    {widthPct > 0 && (
                                      <div
                                        className={`absolute top-0 h-full rounded ${barCls} opacity-85 flex items-center px-1`}
                                        style={{
                                          left: `${startPct}%`,
                                          width: `${Math.max(widthPct, 3)}%`,
                                        }}
                                        title={`Start: +${entry.startOffsetHours}h  Duration: ${entry.durationHours}h  End: +${entry.endOffsetHours}h`}
                                      >
                                        <span className="text-white text-xs truncate">
                                          {entry.durationHours}h
                                        </span>
                                      </div>
                                    )}
                                  </div>
                                  {/* Offset editor */}
                                  <div className="flex items-center gap-1 shrink-0">
                                    <label className="text-xs text-gray-400">+</label>
                                    <input
                                      type="number"
                                      min={0}
                                      className="w-14 border rounded px-1 py-0.5 text-xs text-center"
                                      value={entry.startOffsetHours}
                                      title="Start offset in hours"
                                      onChange={async (ev) => {
                                        const val = parseInt(ev.target.value, 10);
                                        if (!isNaN(val) && val >= 0) {
                                          await patchSchedule(p.planId, entry.workOrderId, val, entry.durationHours);
                                        }
                                      }}
                                    />
                                    <span className="text-xs text-gray-400">{t('productionPlans.hOffset')}</span>
                                  </div>
                                </div>
                              );
                            })}
                            {/* Legend */}
                            <div className="flex gap-4 pt-1 text-xs text-gray-500">
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 rounded bg-blue-500" /> {t('productionPlans.criticalPath')}
                              </span>
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 rounded bg-emerald-500" /> {t('productionPlans.parallel')}
                              </span>
                              <span className="flex items-center gap-1">
                                <span className="inline-block w-3 h-3 rounded bg-gray-400" /> {t('productionPlans.sequential')}
                              </span>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })()}

                  {/* Material requirements */}
                  <div className="border-t px-4 py-3 bg-gray-50">
                    <h3 className="text-xs font-semibold text-gray-500 uppercase mb-2">
                      {t('productionPlans.materialRequirements')}
                    </h3>
                    {loadingMats === p.planId ? (
                      <p className="text-xs text-gray-400">{t('common.loading')}</p>
                    ) : mats[p.planId] ? (
                      mats[p.planId].length === 0 ? (
                        <p className="text-xs text-gray-400">
                          {t('productionPlans.noMaterials')}
                        </p>
                      ) : (
                        <div className="flex flex-wrap gap-3">
                          {mats[p.planId].map((m, i) => (
                            <div
                              key={i}
                              className="bg-white border rounded-lg px-3 py-2 text-xs"
                            >
                              <div className="font-medium">{m.itemName}</div>
                              <div className="text-gray-500">
                                {m.purchasingUnitsNeeded.toFixed(4)}{' '}
                                {m.purchasingUom}
                              </div>
                            </div>
                          ))}
                        </div>
                      )
                    ) : (
                      <p className="text-xs text-gray-400">
                        {t('productionPlans.expandToLoad')}
                      </p>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {open && (
        <Modal title={t('productionPlans.newPlanTitle')} onClose={() => setOpen(false)}>
          <form onSubmit={submit} className="space-y-4">
            <Field label={t('productionPlans.planDate')}>
              <input
                className="input"
                type="date"
                required
                value={form.planDate}
                onChange={(e) =>
                  setForm((f) => ({ ...f, planDate: e.target.value }))
                }
              />
            </Field>
            <Field label={t('productionPlans.shift')}>
              <select
                className="input"
                value={form.shift}
                onChange={(e) =>
                  setForm((f) => ({ ...f, shift: e.target.value }))
                }
              >
                <option value="MORNING">{t('productionPlans.shifts.MORNING')}</option>
                <option value="AFTERNOON">{t('productionPlans.shifts.AFTERNOON')}</option>
                <option value="NIGHT">{t('productionPlans.shifts.NIGHT')}</option>
              </select>
            </Field>
            <Field label={t('common.notes')}>
              <input
                className="input"
                placeholder={t('productionPlans.notesPlaceholder')}
                value={form.notes}
                onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
              />
            </Field>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="secondary" size="sm" type="button" onClick={() => setOpen(false)}>
                {t('common.cancel')}
              </Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>
                {saving ? t('common.saving') : t('common.create')}
              </Button>
            </div>
          </form>
        </Modal>
      )}
      {confirmModal}
    </div>
  );
}
