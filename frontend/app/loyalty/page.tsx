'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT, useDateTimeFmt } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success, useConfirm } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';

/* ── types ─────────────────────────────────────────────── */
interface LoyaltyTier {
  tierId: string;
  tenantId: string;
  name: string;
  minPoints: number;
  discountPercent?: number;
  perks?: string;
}
interface LoyaltyBalance {
  customerId: string;
  customerName?: string;
  currentPoints: number;
  lifetimePoints: number;
  tier?: string;
}
interface LoyaltyTx {
  txId: string;
  customerId: string;
  type: string;
  points: number;
  reason?: string;
  createdAt?: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function LoyaltyPage() {
  const t = useT();
  const fmtDateTime = useDateTimeFmt();
  const [askConfirm, confirmModal] = useConfirm({ confirmLabel: t('common.confirm'), cancelLabel: t('common.cancel') });

  const [tab, setTab] = useState<'tiers' | 'balances' | 'history'>('tiers');

  /* tiers */
  const [tiers, setTiers] = useState<LoyaltyTier[]>([]);
  const [tiersLoading, setTiersLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showCreateTier, setShowCreateTier] = useState(false);
  const [tierForm, setTierForm] = useState({ name: '', minPoints: '', discountPercent: '', perks: '' });
  const [saving, setSaving] = useState(false);
  const [editTier, setEditTier] = useState<LoyaltyTier | null>(null);
  const [editTierForm, setEditTierForm] = useState({ name: '', minPoints: '', discountPercent: '', perks: '' });

  /* balance lookup */
  const [balanceCustId, setBalanceCustId] = useState('');
  const [balance, setBalance] = useState<LoyaltyBalance | null>(null);
  const [balLoading, setBalLoading] = useState(false);

  /* award / redeem */
  const [showAward, setShowAward] = useState(false);
  const [awardForm, setAwardForm] = useState({ customerId: '', points: '', reason: '' });
  const [showRedeem, setShowRedeem] = useState(false);
  const [redeemForm, setRedeemForm] = useState({ customerId: '', points: '', reason: '' });

  /* history */
  const [historyCustId, setHistoryCustId] = useState('');
  const [history, setHistory] = useState<LoyaltyTx[]>([]);
  const [histLoading, setHistLoading] = useState(false);

  /* loaders */
  const loadTiers = useCallback(async () => {
    try {
      setTiersLoading(true);
      const data = await apiFetch<LoyaltyTier[]>(`/v2/loyalty/tiers?tenantId=${TENANT_ID}`);
      setTiers(data);
    } catch (e) { setError(String(e)); } finally { setTiersLoading(false); }
  }, []);

  useEffect(() => { loadTiers(); }, [loadTiers]);

  /* tier CRUD */
  const createTier = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v2/loyalty/tiers', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, name: tierForm.name, minPoints: Number(tierForm.minPoints), discountPercent: tierForm.discountPercent ? Number(tierForm.discountPercent) : undefined, perks: tierForm.perks || undefined }),
      });
      setSuccess(t('loyalty.tierCreated'));
      setShowCreateTier(false);
      setTierForm({ name: '', minPoints: '', discountPercent: '', perks: '' });
      loadTiers();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  const updateTier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editTier) return;
    try {
      setSaving(true);
      await apiFetch(`/v2/loyalty/tiers/${editTier.tierId}?tenantId=${TENANT_ID}`, {
        method: 'PUT',
        body: JSON.stringify({ name: editTierForm.name, minPoints: Number(editTierForm.minPoints), discountPercent: editTierForm.discountPercent ? Number(editTierForm.discountPercent) : undefined, perks: editTierForm.perks || undefined }),
      });
      setSuccess(t('loyalty.tierUpdated'));
      setEditTier(null);
      loadTiers();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  const deleteTier = async (id: string) => {
    if (!await askConfirm(t('loyalty.confirmDelete'))) return;
    try {
      await apiFetch(`/v2/loyalty/tiers/${id}?tenantId=${TENANT_ID}`, { method: 'DELETE' });
      setSuccess(t('loyalty.tierDeleted'));
      loadTiers();
    } catch (e) { setError(String(e)); }
  };

  /* balance */
  const lookupBalance = async () => {
    if (!balanceCustId) return;
    try {
      setBalLoading(true);
      const data = await apiFetch<LoyaltyBalance>(`/v2/loyalty/balance/${balanceCustId}?tenantId=${TENANT_ID}`);
      setBalance(data);
    } catch (e) { setError(String(e)); } finally { setBalLoading(false); }
  };

  /* award / redeem */
  const awardPoints = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v2/loyalty/award', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID, customerId: awardForm.customerId, points: Number(awardForm.points), reason: awardForm.reason || undefined }) });
      setSuccess(t('loyalty.awarded'));
      setShowAward(false);
      setAwardForm({ customerId: '', points: '', reason: '' });
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  const redeemPoints = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v2/loyalty/redeem', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID, customerId: redeemForm.customerId, points: Number(redeemForm.points), reason: redeemForm.reason || undefined }) });
      setSuccess(t('loyalty.redeemed'));
      setShowRedeem(false);
      setRedeemForm({ customerId: '', points: '', reason: '' });
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  /* history */
  const loadHistory = async () => {
    if (!historyCustId) return;
    try {
      setHistLoading(true);
      const data = await apiFetch<LoyaltyTx[]>(`/v2/loyalty/history/${historyCustId}?tenantId=${TENANT_ID}`);
      setHistory(data);
    } catch (e) { setError(String(e)); } finally { setHistLoading(false); }
  };

  return (
    <div className="max-w-[1800px]">
      <SectionTitle eyebrow="Engagement" title={t('loyalty.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {(['tiers', 'balances', 'history'] as const).map(k => (
          <button key={k} onClick={() => setTab(k)}
            className={`px-4 py-2 text-sm font-medium -mb-px ${tab === k ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {t(`loyalty.tab_${k}`)}
          </button>
        ))}
      </div>

      {/* ───────── TIERS ───────── */}
      {tab === 'tiers' && (
        <>
          <div className="flex justify-end mb-4">
            <Button variant="primary" size="sm" onClick={() => setShowCreateTier(true)}>+ {t('loyalty.addTier')}</Button>
          </div>
          {tiersLoading ? <Spinner /> : (
            <Table
              cols={[t('common.name'), t('loyalty.minPoints'), t('loyalty.discountPct'), t('loyalty.perks'), t('common.actions')]}
              rows={tiers.map(ti => [
                ti.name,
                ti.minPoints.toString(),
                ti.discountPercent !== null && ti.discountPercent !== undefined ? `${ti.discountPercent}%` : '—',
                ti.perks || '—',
                <div key={ti.tierId} className="flex gap-1">
                  <Button variant="secondary" size="xs" onClick={() => { setEditTier(ti); setEditTierForm({ name: ti.name, minPoints: String(ti.minPoints), discountPercent: ti.discountPercent !== null && ti.discountPercent !== undefined ? String(ti.discountPercent) : '', perks: ti.perks || '' }); }}>{t('common.edit')}</Button>
                  <Button variant="danger" size="xs" onClick={() => deleteTier(ti.tierId)}>{t('common.delete')}</Button>
                </div>,
              ])}
              empty={t('loyalty.noTiers')}
            />
          )}
        </>
      )}

      {/* ───────── BALANCES ───────── */}
      {tab === 'balances' && (
        <>
          <div className="flex gap-2 items-end mb-4">
            <Field label={t('loyalty.customerId')}><input className="input" value={balanceCustId} onChange={e => setBalanceCustId(e.target.value)} /></Field>
            <Button variant="primary" size="sm" onClick={lookupBalance}>{t('common.load')}</Button>
            <Button variant="primary" size="sm" className="bg-green-600 hover:bg-green-700" onClick={() => setShowAward(true)}>{t('loyalty.award')}</Button>
            <Button variant="secondary" size="sm" onClick={() => setShowRedeem(true)}>{t('loyalty.redeem')}</Button>
          </div>
          {balLoading ? <Spinner /> : balance && (
            <div className="bg-white shadow rounded p-4 space-y-2 text-sm">
              <div><strong>{t('loyalty.currentPoints')}:</strong> {balance.currentPoints}</div>
              <div><strong>{t('loyalty.lifetimePoints')}:</strong> {balance.lifetimePoints}</div>
              <div><strong>{t('loyalty.tier')}:</strong> {balance.tier || '—'}</div>
            </div>
          )}
        </>
      )}

      {/* ───────── HISTORY ───────── */}
      {tab === 'history' && (
        <>
          <div className="flex gap-2 items-end mb-4">
            <Field label={t('loyalty.customerId')}><input className="input" value={historyCustId} onChange={e => setHistoryCustId(e.target.value)} /></Field>
            <Button variant="primary" size="sm" onClick={loadHistory}>{t('common.load')}</Button>
          </div>
          {histLoading ? <Spinner /> : (
            <Table
              cols={[t('loyalty.txType'), t('loyalty.points'), t('loyalty.reason'), t('common.date')]}
              rows={history.map(h => [
                <Badge key={h.txId} status={h.type} />,
                h.points.toString(),
                h.reason || '—',
                fmtDateTime(h.createdAt),
              ])}
              empty={t('loyalty.noHistory')}
            />
          )}
        </>
      )}

      {/* create tier modal */}
      {showCreateTier && (
        <Modal title={t('loyalty.addTier')} onClose={() => setShowCreateTier(false)}>
          <form onSubmit={createTier} className="space-y-4">
            <Field label={t('common.name')}><input className="input w-full" required value={tierForm.name} onChange={e => setTierForm({ ...tierForm, name: e.target.value })} /></Field>
            <Field label={t('loyalty.minPoints')}><input className="input w-full" type="number" required value={tierForm.minPoints} onChange={e => setTierForm({ ...tierForm, minPoints: e.target.value })} /></Field>
            <Field label={t('loyalty.discountPct')}><input className="input w-full" type="number" step="0.1" value={tierForm.discountPercent} onChange={e => setTierForm({ ...tierForm, discountPercent: e.target.value })} /></Field>
            <Field label={t('loyalty.perks')}><textarea className="input w-full" rows={2} value={tierForm.perks} onChange={e => setTierForm({ ...tierForm, perks: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowCreateTier(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{saving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* edit tier modal */}
      {editTier && (
        <Modal title={t('loyalty.editTier')} onClose={() => setEditTier(null)}>
          <form onSubmit={updateTier} className="space-y-4">
            <Field label={t('common.name')}><input className="input w-full" required value={editTierForm.name} onChange={e => setEditTierForm({ ...editTierForm, name: e.target.value })} /></Field>
            <Field label={t('loyalty.minPoints')}><input className="input w-full" type="number" required value={editTierForm.minPoints} onChange={e => setEditTierForm({ ...editTierForm, minPoints: e.target.value })} /></Field>
            <Field label={t('loyalty.discountPct')}><input className="input w-full" type="number" step="0.1" value={editTierForm.discountPercent} onChange={e => setEditTierForm({ ...editTierForm, discountPercent: e.target.value })} /></Field>
            <Field label={t('loyalty.perks')}><textarea className="input w-full" rows={2} value={editTierForm.perks} onChange={e => setEditTierForm({ ...editTierForm, perks: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setEditTier(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{saving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* award modal */}
      {showAward && (
        <Modal title={t('loyalty.awardPoints')} onClose={() => setShowAward(false)}>
          <form onSubmit={awardPoints} className="space-y-4">
            <Field label={t('loyalty.customerId')}><input className="input w-full" required value={awardForm.customerId} onChange={e => setAwardForm({ ...awardForm, customerId: e.target.value })} /></Field>
            <Field label={t('loyalty.points')}><input className="input w-full" type="number" required value={awardForm.points} onChange={e => setAwardForm({ ...awardForm, points: e.target.value })} /></Field>
            <Field label={t('loyalty.reason')}><input className="input w-full" value={awardForm.reason} onChange={e => setAwardForm({ ...awardForm, reason: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowAward(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{saving ? t('common.saving') : t('loyalty.award')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* redeem modal */}
      {showRedeem && (
        <Modal title={t('loyalty.redeemPoints')} onClose={() => setShowRedeem(false)}>
          <form onSubmit={redeemPoints} className="space-y-4">
            <Field label={t('loyalty.customerId')}><input className="input w-full" required value={redeemForm.customerId} onChange={e => setRedeemForm({ ...redeemForm, customerId: e.target.value })} /></Field>
            <Field label={t('loyalty.points')}><input className="input w-full" type="number" required value={redeemForm.points} onChange={e => setRedeemForm({ ...redeemForm, points: e.target.value })} /></Field>
            <Field label={t('loyalty.reason')}><input className="input w-full" value={redeemForm.reason} onChange={e => setRedeemForm({ ...redeemForm, reason: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowRedeem(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{saving ? t('common.saving') : t('loyalty.redeem')}</Button>
            </div>
          </form>
        </Modal>
      )}
      {confirmModal}
    </div>
  );
}
