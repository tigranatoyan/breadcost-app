'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';

/* ── types ─────────────────────────────────────────────── */
interface SubTier {
  tierId: string;
  name: string;
  monthlyPrice?: number;
  currency?: string;
  features: string[];
  maxUsers?: number;
}
interface TenantSub {
  tenantId: string;
  tierId: string;
  tierName?: string;
  status: string;
  startDate?: string;
  endDate?: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function SubscriptionsPage() {
  const t = useT();

  const [tab, setTab] = useState<'tiers' | 'assignment'>('tiers');

  /* tiers */
  const [tiers, setTiers] = useState<SubTier[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* assignment */
  const [tenantSub, setTenantSub] = useState<TenantSub | null>(null);
  const [subLoading, setSubLoading] = useState(false);
  const [showAssign, setShowAssign] = useState(false);
  const [assignTier, setAssignTier] = useState('');
  const [saving, setSaving] = useState(false);

  /* feature check */
  const [featureKey, setFeatureKey] = useState('');
  const [featureResult, setFeatureResult] = useState<boolean | null>(null);

  /* loaders */
  const loadTiers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await apiFetch<SubTier[]>(`/v2/subscriptions/tiers?tenantId=${TENANT_ID}`);
      setTiers(data);
    } catch (e) { setError(String(e)); } finally { setLoading(false); }
  }, []);

  const loadAssignment = useCallback(async () => {
    try {
      setSubLoading(true);
      const data = await apiFetch<TenantSub>(`/v2/subscriptions/current?tenantId=${TENANT_ID}`);
      setTenantSub(data);
    } catch { setTenantSub(null); } finally { setSubLoading(false); }
  }, []);

  useEffect(() => { loadTiers(); }, [loadTiers]);
  useEffect(() => { if (tab === 'assignment') loadAssignment(); }, [tab, loadAssignment]);

  const assignSubscription = async () => {
    if (!assignTier) return;
    try {
      setSaving(true);
      await apiFetch('/v2/subscriptions/assign', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID, tierId: assignTier }) });
      setSuccess(t('subscriptions.assigned'));
      setShowAssign(false);
      loadAssignment();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  const checkFeature = async () => {
    if (!featureKey) return;
    try {
      const data = await apiFetch<{ allowed: boolean }>(`/v2/subscriptions/features/${featureKey}?tenantId=${TENANT_ID}`);
      setFeatureResult(data.allowed);
    } catch (e) { setError(String(e)); }
  };

  return (
    <div className="max-w-[1800px]">
      <SectionTitle eyebrow="Billing" title={t('subscriptions.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {(['tiers', 'assignment'] as const).map(k => (
          <button key={k} onClick={() => setTab(k)}
            className={`px-4 py-2 text-sm font-medium -mb-px ${tab === k ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {t(`subscriptions.tab_${k}`)}
          </button>
        ))}
      </div>

      {/* ───────── TIERS ───────── */}
      {tab === 'tiers' && (
        <>
          {loading ? <Spinner /> : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {tiers.map(ti => (
                <div key={ti.tierId} className="border rounded-lg p-4 bg-white shadow-sm">
                  <h3 className="text-lg font-bold mb-1">{ti.name}</h3>
                  {ti.monthlyPrice != null && <p className="text-2xl font-bold text-blue-600 mb-2">{ti.monthlyPrice.toFixed(2)} <span className="text-sm text-gray-500">{ti.currency || ''}/mo</span></p>}
                  {ti.maxUsers && <p className="text-sm text-gray-500 mb-2">{t('subscriptions.maxUsers')}: {ti.maxUsers}</p>}
                  <ul className="space-y-1">
                    {ti.features.map((f, i) => <li key={i} className="text-sm flex items-center gap-1"><span className="text-green-500">✓</span> {f}</li>)}
                  </ul>
                </div>
              ))}
              {tiers.length === 0 && <p className="text-sm text-gray-500 col-span-3">{t('subscriptions.noTiers')}</p>}
            </div>
          )}
        </>
      )}

      {/* ───────── ASSIGNMENT ───────── */}
      {tab === 'assignment' && (
        <div className="space-y-6">
          {subLoading ? <Spinner /> : tenantSub ? (
            <div className="bg-white shadow rounded p-4 space-y-2 text-sm">
              <div><strong>{t('subscriptions.currentTier')}:</strong> {tenantSub.tierName || tenantSub.tierId}</div>
              <div><strong>{t('common.status')}:</strong> <Badge status={tenantSub.status} /></div>
              {tenantSub.startDate && <div><strong>{t('subscriptions.start')}:</strong> {tenantSub.startDate}</div>}
              {tenantSub.endDate && <div><strong>{t('subscriptions.end')}:</strong> {tenantSub.endDate}</div>}
              <Button variant="primary" size="sm" className="mt-2" onClick={() => setShowAssign(true)}>{t('subscriptions.change')}</Button>
            </div>
          ) : (
            <div className="text-sm text-gray-500">
              <p>{t('subscriptions.noSubscription')}</p>
              <Button variant="primary" size="sm" className="mt-2" onClick={() => setShowAssign(true)}>{t('subscriptions.assign')}</Button>
            </div>
          )}

          {/* feature check */}
          <div className="bg-white shadow rounded p-4">
            <h3 className="font-semibold text-sm mb-2">{t('subscriptions.featureCheck')}</h3>
            <div className="flex gap-2 items-end">
              <Field label={t('subscriptions.featureKey')}><input className="input" value={featureKey} onChange={e => setFeatureKey(e.target.value)} placeholder="e.g. advanced_reports" /></Field>
              <Button variant="primary" size="sm" onClick={checkFeature}>{t('subscriptions.check')}</Button>
            </div>
            {featureResult !== null && (
              <p className={`text-sm mt-2 font-medium ${featureResult ? 'text-green-600' : 'text-red-600'}`}>
                {featureResult ? t('subscriptions.featureAllowed') : t('subscriptions.featureDenied')}
              </p>
            )}
          </div>
        </div>
      )}

      {/* assign modal */}
      {showAssign && (
        <Modal title={t('subscriptions.assignTier')} onClose={() => setShowAssign(false)}>
          <div className="space-y-4">
            <Field label={t('subscriptions.selectTier')}>
              <select className="input w-full" value={assignTier} onChange={e => setAssignTier(e.target.value)}>
                <option value="">— {t('common.select')} —</option>
                {tiers.map(ti => <option key={ti.tierId} value={ti.tierId}>{ti.name}</option>)}
              </select>
            </Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowAssign(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" disabled={!assignTier || saving} onClick={assignSubscription}>{saving ? t('common.saving') : t('subscriptions.assign')}</Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
