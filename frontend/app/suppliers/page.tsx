'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, API_BASE, TENANT_ID } from '@/lib/api';
import { useT, useDateFmt } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success, useConfirm } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';

/* ── types ─────────────────────────────────────────────── */
interface Supplier {
  supplierId: string;
  tenantId: string;
  name: string;
  contactEmail?: string;
  contactPhone?: string;
  notes?: string;
  active?: boolean;
}
interface CatalogItem {
  id: string;
  supplierId: string;
  ingredientId: string;
  ingredientName?: string;
  unitPrice?: number;
  currency?: string;
  leadTimeDays?: number;
  moq?: number;
  unit?: string;
}
interface PO {
  poId: string;
  supplierId: string;
  supplierName?: string;
  status: string;
  totalAmount?: number;
  currency?: string;
  fxRate?: number;
  fxCurrencyCode?: string;
  notes?: string;
  approvedBy?: string;
  createdAt?: string;
}
interface POLine {
  id: string;
  ingredientId: string;
  ingredientName: string;
  qty: number;
  unit: string;
  unitPrice: number;
  currency?: string;
}
interface SupplierApiConfig {
  id: string;
  supplierId: string;
  apiUrl: string;
  apiKeyRef: string;
  format: string;
  enabled: boolean;
}
interface Item {
  itemId: string;
  name: string;
  type: string;
  baseUom: string;
  active: boolean;
}

/* ── page ──────────────────────────────────────────────── */
export default function SuppliersPage() {
  const t = useT();
  const fmtDate = useDateFmt();
  const [askConfirm, confirmModal] = useConfirm({ confirmLabel: t('common.confirm'), cancelLabel: t('common.cancel') });

  /* tabs */
  const [tab, setTab] = useState<'suppliers' | 'purchase-orders' | 'api-config'>('suppliers');

  /* ── supplier state ─────────────────────────────────── */
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ name: '', contactEmail: '', contactPhone: '', notes: '' });
  const [editSup, setEditSup] = useState<Supplier | null>(null);
  const [editForm, setEditForm] = useState({ name: '', contactEmail: '', contactPhone: '', notes: '' });

  /* catalog */
  const [catalogSup, setCatalogSup] = useState<Supplier | null>(null);
  const [catalog, setCatalog] = useState<CatalogItem[]>([]);
  const [catLoading, setCatLoading] = useState(false);
  const [catForm, setCatForm] = useState({ ingredientId: '', ingredientName: '', unitPrice: '', currency: 'AMD', leadTimeDays: '', moq: '', unit: 'kg' });
  const [catSaving, setCatSaving] = useState(false);

  /* ── PO state ───────────────────────────────────────── */
  const [pos, setPos] = useState<PO[]>([]);
  const [poLoading, setPoLoading] = useState(false);
  const [showCreatePO, setShowCreatePO] = useState(false);
  const [poForm, setPoForm] = useState({ supplierId: '', notes: '', fxRate: '', fxCurrencyCode: '' });
  const [poLines, setPoLines] = useState<{ ingredientId: string; ingredientName: string; qty: string; unit: string; unitPrice: string; currency: string }[]>([]);
  const [poSaving, setPoSaving] = useState(false);
  const [poDetail, setPoDetail] = useState<{ po: PO; lines: POLine[] } | null>(null);
  const [poDetailLoading, setPoDetailLoading] = useState(false);

  /* ── items (ingredients) for dropdown ─────────────── */
  const [items, setItems] = useState<Item[]>([]);

  /* ── API Config state ───────────────────────────────── */
  const [apiConfigs, setApiConfigs] = useState<SupplierApiConfig[]>([]);
  const [apiLoading, setApiLoading] = useState(false);
  const [showApiForm, setShowApiForm] = useState(false);
  const [apiForm, setApiForm] = useState({ supplierId: '', apiUrl: '', apiKeyRef: '', format: 'JSON', enabled: true });
  const [apiSaving, setApiSaving] = useState(false);

  /* A1.4 — generate POs from plan */
  const [genSaving, setGenSaving] = useState(false);

  /* A1.5 — ingredient → supplier lookup */
  const [lookupId, setLookupId] = useState('');
  const [lookupResults, setLookupResults] = useState<{ supplierId: string; supplierName: string; unitPrice: number; currency: string; leadTimeDays?: number }[] | null>(null);
  const [lookupLoading, setLookupLoading] = useState(false);

  /* ── loaders ────────────────────────────────────────── */
  const loadSuppliers = useCallback(async () => {
    try {
      setLoading(true);
      const data = await apiFetch<Supplier[]>(`/v2/suppliers?tenantId=${TENANT_ID}`);
      setSuppliers(data);
    } catch (e) { setError(String(e)); } finally { setLoading(false); }
  }, []);

  const loadPOs = useCallback(async () => {
    try {
      setPoLoading(true);
      const data = await apiFetch<PO[]>(`/v2/purchase-orders?tenantId=${TENANT_ID}`);
      setPos(data);
    } catch (e) { setError(String(e)); } finally { setPoLoading(false); }
  }, []);

  const loadItems = useCallback(async () => {
    try {
      const data = await apiFetch<Item[]>(`/v1/items?tenantId=${TENANT_ID}&activeOnly=true`);
      setItems(data.filter(i => i.type === 'INGREDIENT'));
    } catch (e) { setError(String(e)); }
  }, []);

  useEffect(() => { loadSuppliers(); }, [loadSuppliers]);
  useEffect(() => { loadItems(); }, [loadItems]);
  useEffect(() => { if (tab === 'purchase-orders') loadPOs(); }, [tab, loadPOs]);

  /* ── API Config CRUD ────────────────────────────────── */
  const loadApiConfigs = useCallback(async () => {
    try {
      setApiLoading(true);
      setApiConfigs(await apiFetch<SupplierApiConfig[]>(`/v3/supplier-api/configs?tenantId=${TENANT_ID}`));
    } catch (e) { setError(String(e)); } finally { setApiLoading(false); }
  }, []);

  const saveApiConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setApiSaving(true);
      await apiFetch('/v3/supplier-api/configs', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID, ...apiForm }) });
      setSuccess(t('supplierApi.saved'));
      setShowApiForm(false);
      setApiForm({ supplierId: '', apiUrl: '', apiKeyRef: '', format: 'JSON', enabled: true });
      loadApiConfigs();
    } catch (e) { setError(String(e)); } finally { setApiSaving(false); }
  };

  const sendPO = async (poId: string) => {
    try {
      await apiFetch('/v3/supplier-api/send-po', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID, poId }) });
      setSuccess(t('supplierApi.poSent'));
    } catch (e) { setError(String(e)); }
  };

  useEffect(() => { if (tab === 'api-config') loadApiConfigs(); }, [tab, loadApiConfigs]);

  /* ── supplier CRUD ──────────────────────────────────── */
  const createSupplier = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v2/suppliers', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID, ...form }) });
      setSuccess(t('suppliers.created'));
      setShowCreate(false);
      setForm({ name: '', contactEmail: '', contactPhone: '', notes: '' });
      loadSuppliers();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  const updateSupplier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editSup) return;
    try {
      setSaving(true);
      await apiFetch(`/v2/suppliers/${editSup.supplierId}?tenantId=${TENANT_ID}`, { method: 'PUT', body: JSON.stringify(editForm) });
      setSuccess(t('suppliers.updated'));
      setEditSup(null);
      loadSuppliers();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  const deleteSupplier = async (id: string) => {
    if (!await askConfirm(t('suppliers.confirmDelete'))) return;
    try {
      await apiFetch(`/v2/suppliers/${id}?tenantId=${TENANT_ID}`, { method: 'DELETE' });
      setSuccess(t('suppliers.deleted'));
      loadSuppliers();
    } catch (e) { setError(String(e)); }
  };

  /* ── catalog ────────────────────────────────────────── */
  const openCatalog = async (sup: Supplier) => {
    setCatalogSup(sup);
    setCatLoading(true);
    try {
      const data = await apiFetch<CatalogItem[]>(`/v2/suppliers/${sup.supplierId}/catalog?tenantId=${TENANT_ID}`);
      setCatalog(data);
    } catch (e) { setError(String(e)); } finally { setCatLoading(false); }
  };

  const addCatalogItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!catalogSup) return;
    try {
      setCatSaving(true);
      await apiFetch(`/v2/suppliers/${catalogSup.supplierId}/catalog`, {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, ...catForm, unitPrice: Number(catForm.unitPrice), leadTimeDays: catForm.leadTimeDays ? Number(catForm.leadTimeDays) : undefined, moq: catForm.moq ? Number(catForm.moq) : undefined }),
      });
      setCatForm({ ingredientId: '', ingredientName: '', unitPrice: '', currency: 'AMD', leadTimeDays: '', moq: '', unit: 'kg' });
      openCatalog(catalogSup);
    } catch (e) { setError(String(e)); } finally { setCatSaving(false); }
  };

  /* ── PO CRUD ────────────────────────────────────────── */
  const addPOLine = () => setPoLines([...poLines, { ingredientId: '', ingredientName: '', qty: '', unit: 'kg', unitPrice: '', currency: 'AMD' }]);
  const removePOLine = (i: number) => setPoLines(poLines.filter((_, idx) => idx !== i));
  const updatePOLine = (i: number, field: string, val: string) => setPoLines(poLines.map((l, idx) => idx === i ? { ...l, [field]: val } : l));

  const createPO = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setPoSaving(true);
      await apiFetch('/v2/purchase-orders', {
        method: 'POST',
        body: JSON.stringify({
          tenantId: TENANT_ID,
          supplierId: poForm.supplierId,
          notes: poForm.notes || undefined,
          fxRate: poForm.fxRate ? Number(poForm.fxRate) : undefined,
          fxCurrencyCode: poForm.fxCurrencyCode || undefined,
          lines: poLines.map(l => ({ ...l, qty: Number(l.qty), unitPrice: Number(l.unitPrice) })),
        }),
      });
      setSuccess(t('suppliers.poCreated'));
      setShowCreatePO(false);
      setPoForm({ supplierId: '', notes: '', fxRate: '', fxCurrencyCode: '' });
      setPoLines([]);
      loadPOs();
    } catch (e) { setError(String(e)); } finally { setPoSaving(false); }
  };

  const approvePO = async (id: string) => {
    try {
      await apiFetch(`/v2/purchase-orders/${id}/approve?tenantId=${TENANT_ID}`, { method: 'PUT', body: '{}' });
      setSuccess(t('suppliers.poApproved'));
      loadPOs();
    } catch (e) { setError(String(e)); }
  };

  const exportPO = async (id: string) => {
    try {
      const res = await fetch(`${API_BASE}/v2/purchase-orders/${id}/export?tenantId=${TENANT_ID}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('bc_token')}` },
      });
      if (!res.ok) throw new Error(t('suppliers.exportFailed'));
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `PO-${id}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) { setError(String(e)); }
  };

  const openPODetail = async (id: string) => {
    try {
      setPoDetailLoading(true);
      const data = await apiFetch<{ po: PO; lines: POLine[] }>(`/v2/purchase-orders/${id}?tenantId=${TENANT_ID}`);
      setPoDetail(data);
    } catch (e) { setError(String(e)); } finally { setPoDetailLoading(false); }
  };

  const suggestPOs = async () => {
    try {
      setPoSaving(true);
      await apiFetch('/v2/purchase-orders/suggest', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID }) });
      setSuccess(t('suppliers.poSuggested'));
      loadPOs();
    } catch (e) { setError(String(e)); } finally { setPoSaving(false); }
  };

  /* A1.4 — generate POs from production plan */
  const generateFromPlan = async () => {
    try {
      setGenSaving(true);
      await apiFetch('/v2/purchase-orders/from-plan', { method: 'POST', body: JSON.stringify({ tenantId: TENANT_ID }) });
      setSuccess(t('suppliers.posGeneratedFromPlan'));
      loadPOs();
    } catch (e) { setError(String(e)); } finally { setGenSaving(false); }
  };

  /* A1.5 — ingredient → supplier lookup */
  const lookupSuppliers = async () => {
    if (!lookupId.trim()) return;
    try {
      setLookupLoading(true);
      const data = await apiFetch<{ supplierId: string; supplierName: string; unitPrice: number; currency: string; leadTimeDays?: number }[]>(
        `/v2/purchase-orders/ingredients/${encodeURIComponent(lookupId.trim())}/suppliers?tenantId=${TENANT_ID}`,
      );
      setLookupResults(data);
    } catch (e) { setError(String(e)); } finally { setLookupLoading(false); }
  };

  /* ── render ─────────────────────────────────────────── */
  return (
    <div className="max-w-[1800px]">
      <SectionTitle eyebrow={t('suppliers.eyebrow')} title={t('suppliers.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* tabs */}
      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {(['suppliers', 'purchase-orders', 'api-config'] as const).map(k => (
          <button key={k} onClick={() => setTab(k)}
            className={`px-4 py-2 text-sm font-medium -mb-px ${tab === k ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {t(`suppliers.tab_${k.replace('-', '_')}`)}
          </button>
        ))}
      </div>

      {/* ───────── SUPPLIERS TAB (Admin) ───────── */}
      {tab === 'suppliers' && (
        <>
          <div className="flex items-center justify-between mb-4">
            <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide">{t('suppliers.adminSection')}</div>
            <Button variant="primary" size="sm" onClick={() => setShowCreate(true)}>+ {t('suppliers.add')}</Button>
          </div>
          {loading ? <Spinner /> : (
            <Table
              cols={[t('common.name'), t('suppliers.email'), t('suppliers.phone'), t('common.actions')]}
              rows={suppliers.map(s => [
                s.name,
                s.contactEmail || '—',
                s.contactPhone || '—',
                <div key={s.supplierId} className="flex gap-1 flex-wrap">
                  <Button variant="secondary" size="xs" onClick={() => openCatalog(s)}>{t('suppliers.catalog')}</Button>
                  <Button variant="secondary" size="xs" onClick={() => { setEditSup(s); setEditForm({ name: s.name, contactEmail: s.contactEmail || '', contactPhone: s.contactPhone || '', notes: s.notes || '' }); }}>{t('common.edit')}</Button>
                  <Button variant="danger" size="xs" onClick={() => deleteSupplier(s.supplierId)}>{t('common.delete')}</Button>
                </div>,
              ])}
              empty={t('suppliers.empty')}
            />
          )}
        </>
      )}

      {/* ───────── PURCHASE ORDERS TAB (Operations) ───────── */}
      {tab === 'purchase-orders' && (
        <>
          <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">{t('suppliers.operationsSection')}</div>
          <div className="flex justify-end gap-2 mb-4">
            <Button variant="secondary" size="sm" onClick={suggestPOs} disabled={poSaving}>{t('suppliers.suggest')}</Button>
            <Button variant="success" size="sm" onClick={generateFromPlan} disabled={genSaving}>{genSaving ? t('common.saving') : t('suppliers.generateFromPlan')}</Button>
            <Button variant="primary" size="sm" onClick={() => { setShowCreatePO(true); addPOLine(); }}>+ {t('suppliers.createPO')}</Button>
          </div>
          {/* Supplier lookup tool */}
          <div className="mt-6 mb-3 border-t pt-4">
            <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">{t('suppliers.lookupSection')}</div>
          </div>
          <div className="mb-4 p-3 bg-gray-50 rounded-lg flex items-end gap-3">
            <Field label={t('suppliers.ingredientLookup')}>
              <select className="input" value={lookupId} onChange={e => setLookupId(e.target.value)}>
                <option value="">— {t('common.select')} —</option>
                {items.map(it => <option key={it.itemId} value={it.itemId}>{it.name} ({it.baseUom})</option>)}
              </select>
            </Field>
            <Button variant="secondary" size="sm" onClick={lookupSuppliers} disabled={lookupLoading}>{lookupLoading ? '…' : t('suppliers.findSuppliers')}</Button>
          </div>
          {lookupResults && (
            <div className="mb-4">
              {lookupResults.length > 0 ? (
                <Table
                  cols={[t('suppliers.supplier'), t('suppliers.unitPrice'), t('suppliers.currency'), t('suppliers.leadTime')]}
                  rows={lookupResults.map((r) => [r.supplierName || r.supplierId, r.unitPrice.toFixed(2), r.currency, r.leadTimeDays !== null && r.leadTimeDays !== undefined ? `${r.leadTimeDays}d` : '—'])}
                  empty=""
                />
              ) : <p className="text-sm text-gray-500">{t('suppliers.noSuppliersForIngredient')}</p>}
            </div>
          )}
          {poLoading ? <Spinner /> : (
            <Table
              cols={[t('suppliers.poId'), t('suppliers.supplier'), t('common.status'), t('suppliers.total'), t('common.date'), t('common.actions')]}
              rows={pos.map(po => [
                <button key={po.poId} className="text-blue-600 underline text-sm" onClick={() => openPODetail(po.poId)}>{po.poId.slice(0, 8)}</button>,
                po.supplierName || po.supplierId.slice(0, 8),
                <Badge key={`s-${po.poId}`} status={po.status} />,
                po.totalAmount !== null && po.totalAmount !== undefined ? `${po.totalAmount.toFixed(2)} ${po.currency || ''}` : '—',
                fmtDate(po.createdAt),
                <div key={`a-${po.poId}`} className="flex gap-1 flex-wrap">
                  {po.status === 'DRAFT' && <Button variant="primary" size="xs" className="bg-green-600 hover:bg-green-700" onClick={() => approvePO(po.poId)}>{t('suppliers.approve')}</Button>}
                  <Button variant="secondary" size="xs" onClick={() => exportPO(po.poId)}>{t('suppliers.export')}</Button>
                </div>,
              ])}
              empty={t('suppliers.poEmpty')}
            />
          )}
        </>
      )}

      {/* ───────── API CONFIG TAB ───────── */}
      {tab === 'api-config' && (
        <>
          <div className="flex justify-end mb-4">
            <Button variant="primary" size="sm" onClick={() => setShowApiForm(true)}>+ {t('supplierApi.addConfig')}</Button>
          </div>
          {apiLoading ? <Spinner /> : (
            <Table
              cols={[t('supplierApi.supplier'), t('supplierApi.apiUrl'), t('supplierApi.format'), t('supplierApi.enabled'), '']}
              rows={apiConfigs.map(c => [
                suppliers.find(s => s.supplierId === c.supplierId)?.name || c.supplierId.slice(0, 8),
                <span key={c.id + 'u'} className="text-sm max-w-xs truncate block">{c.apiUrl}</span>,
                c.format,
                c.enabled ? '✅' : '❌',
                <button key={c.id} onClick={() => {
                  const po = pos.find(p => p.supplierId === c.supplierId && p.status === 'APPROVED');
                  if (po) sendPO(po.poId);
                  else setError(t('supplierApi.noApprovedPO'));
                }} className="text-blue-600 hover:underline text-sm">{t('supplierApi.sendPO')}</button>,
              ])}
              empty={t('supplierApi.noConfigs')}
            />
          )}
        </>
      )}

      {/* ───────── CREATE SUPPLIER MODAL ───────── */}
      {showCreate && (
        <Modal title={t('suppliers.add')} onClose={() => setShowCreate(false)}>
          <form onSubmit={createSupplier} className="space-y-4">
            <Field label={t('common.name')}><input className="input w-full" required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></Field>
            <Field label={t('suppliers.email')}><input className="input w-full" type="email" value={form.contactEmail} onChange={e => setForm({ ...form, contactEmail: e.target.value })} /></Field>
            <Field label={t('suppliers.phone')}><input className="input w-full" value={form.contactPhone} onChange={e => setForm({ ...form, contactPhone: e.target.value })} /></Field>
            <Field label={t('common.notes')}><textarea className="input w-full" rows={2} value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowCreate(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{saving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* ───────── EDIT SUPPLIER MODAL ───────── */}
      {editSup && (
        <Modal title={t('suppliers.edit')} onClose={() => setEditSup(null)}>
          <form onSubmit={updateSupplier} className="space-y-4">
            <Field label={t('common.name')}><input className="input w-full" required value={editForm.name} onChange={e => setEditForm({ ...editForm, name: e.target.value })} /></Field>
            <Field label={t('suppliers.email')}><input className="input w-full" type="email" value={editForm.contactEmail} onChange={e => setEditForm({ ...editForm, contactEmail: e.target.value })} /></Field>
            <Field label={t('suppliers.phone')}><input className="input w-full" value={editForm.contactPhone} onChange={e => setEditForm({ ...editForm, contactPhone: e.target.value })} /></Field>
            <Field label={t('common.notes')}><textarea className="input w-full" rows={2} value={editForm.notes} onChange={e => setEditForm({ ...editForm, notes: e.target.value })} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setEditSup(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>{saving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* ───────── CATALOG MODAL ───────── */}
      {catalogSup && (
        <Modal title={`${t('suppliers.catalog')} — ${catalogSup.name}`} onClose={() => setCatalogSup(null)} wide>
          {catLoading ? <Spinner /> : (
            <>
              <Table
                cols={[t('suppliers.ingredient'), t('suppliers.unitPrice'), t('suppliers.currency'), t('suppliers.leadTime'), t('suppliers.moq'), t('suppliers.unit')]}
                rows={catalog.map(c => [c.ingredientName || c.ingredientId, c.unitPrice?.toFixed(2) ?? '—', c.currency || '—', c.leadTimeDays !== null && c.leadTimeDays !== undefined ? `${c.leadTimeDays}d` : '—', c.moq ?? '—', c.unit || '—'])}
                empty={t('suppliers.catalogEmpty')}
              />
              <form onSubmit={addCatalogItem} className="mt-4 grid grid-cols-3 gap-3 items-end">
                <Field label={t('suppliers.ingredient')}>
                  <select className="input w-full" required value={catForm.ingredientId} onChange={e => {
                    const item = items.find(it => it.itemId === e.target.value);
                    setCatForm({ ...catForm, ingredientId: e.target.value, ingredientName: item?.name || '' });
                  }}>
                    <option value="">— {t('common.select')} —</option>
                    {items.map(it => <option key={it.itemId} value={it.itemId}>{it.name} ({it.baseUom})</option>)}
                  </select>
                </Field>
                <Field label={t('suppliers.unitPrice')}><input className="input w-full" type="number" step="0.01" required value={catForm.unitPrice} onChange={e => setCatForm({ ...catForm, unitPrice: e.target.value })} /></Field>
                <Field label={t('suppliers.currency')}><input className="input w-full" value={catForm.currency} onChange={e => setCatForm({ ...catForm, currency: e.target.value })} /></Field>
                <Field label={t('suppliers.leadTime')}><input className="input w-full" type="number" value={catForm.leadTimeDays} onChange={e => setCatForm({ ...catForm, leadTimeDays: e.target.value })} /></Field>
                <Button variant="primary" size="sm" type="submit" disabled={catSaving}>{catSaving ? '…' : `+ ${t('common.add')}`}</Button>
              </form>
            </>
          )}
        </Modal>
      )}

      {/* ───────── CREATE PO MODAL ───────── */}
      {showCreatePO && (
        <Modal title={t('suppliers.createPO')} onClose={() => { setShowCreatePO(false); setPoLines([]); }} wide>
          <form onSubmit={createPO} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('suppliers.supplier')}>
                <select className="input w-full" required value={poForm.supplierId} onChange={e => setPoForm({ ...poForm, supplierId: e.target.value })}>
                  <option value="">— {t('common.select')} —</option>
                  {suppliers.map(s => <option key={s.supplierId} value={s.supplierId}>{s.name}</option>)}
                </select>
              </Field>
              <Field label={t('common.notes')}><input className="input w-full" value={poForm.notes} onChange={e => setPoForm({ ...poForm, notes: e.target.value })} /></Field>
              <Field label={t('suppliers.fxRate')}><input className="input w-full" type="number" step="0.0001" value={poForm.fxRate} onChange={e => setPoForm({ ...poForm, fxRate: e.target.value })} /></Field>
              <Field label={t('suppliers.fxCurrency')}><input className="input w-full" value={poForm.fxCurrencyCode} onChange={e => setPoForm({ ...poForm, fxCurrencyCode: e.target.value })} /></Field>
            </div>
            <h3 className="font-semibold text-sm mt-4">{t('suppliers.lines')}</h3>
            {poLines.map((line, i) => (
              <div key={i} className="grid grid-cols-5 gap-2 items-end">
                <Field label={t('suppliers.ingredient')}>
                  <select className="input w-full" required value={line.ingredientId} onChange={e => {
                    const item = items.find(it => it.itemId === e.target.value);
                    setPoLines(poLines.map((l, idx) => idx === i ? { ...l, ingredientId: e.target.value, ingredientName: item?.name || '', unit: item?.baseUom || l.unit } : l));
                  }}>
                    <option value="">— {t('common.select')} —</option>
                    {items.map(it => <option key={it.itemId} value={it.itemId}>{it.name} ({it.baseUom})</option>)}
                  </select>
                </Field>
                <Field label={t('suppliers.qty')}><input className="input w-full" type="number" step="0.01" required value={line.qty} onChange={e => updatePOLine(i, 'qty', e.target.value)} /></Field>
                <Field label={t('suppliers.unit')}><input className="input w-full" value={line.unit} onChange={e => updatePOLine(i, 'unit', e.target.value)} /></Field>
                <Field label={t('suppliers.unitPrice')}><input className="input w-full" type="number" step="0.01" required value={line.unitPrice} onChange={e => updatePOLine(i, 'unitPrice', e.target.value)} /></Field>
                <Button variant="danger" size="xs" onClick={() => removePOLine(i)}>✕</Button>
              </div>
            ))}
            <Button variant="secondary" size="sm" onClick={addPOLine}>+ {t('suppliers.addLine')}</Button>
            <div className="flex justify-end gap-2 pt-4">
              <Button variant="secondary" size="sm" onClick={() => { setShowCreatePO(false); setPoLines([]); }}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={poSaving}>{poSaving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* ───────── PO DETAIL MODAL ───────── */}
      {poDetail && (
        <Modal title={`${t('suppliers.poDetail')} — ${poDetail.po.poId.slice(0, 8)}`} onClose={() => setPoDetail(null)} wide>
          {poDetailLoading ? <Spinner /> : (
            <>
              <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
                <div><span className="font-medium">{t('common.status')}:</span> <Badge status={poDetail.po.status} /></div>
                <div><span className="font-medium">{t('suppliers.supplier')}:</span> {poDetail.po.supplierName || poDetail.po.supplierId}</div>
                {poDetail.po.notes && <div className="col-span-2"><span className="font-medium">{t('common.notes')}:</span> {poDetail.po.notes}</div>}
              </div>
              <Table
                cols={[t('suppliers.ingredient'), t('suppliers.qty'), t('suppliers.unit'), t('suppliers.unitPrice'), t('suppliers.currency')]}
                rows={poDetail.lines.map(l => [l.ingredientName || l.ingredientId, l.qty, l.unit, l.unitPrice.toFixed(2), l.currency || '—'])}
                empty={t('suppliers.noLines')}
              />
            </>
          )}
        </Modal>
      )}

      {/* ───────── API CONFIG FORM MODAL ───────── */}
      {showApiForm && (
        <Modal title={t('supplierApi.addConfig')} onClose={() => setShowApiForm(false)}>
          <form onSubmit={saveApiConfig} className="space-y-4">
            <Field label={t('supplierApi.supplier')}>
              <select className="input w-full" required value={apiForm.supplierId} onChange={e => setApiForm({ ...apiForm, supplierId: e.target.value })}>
                <option value="">— {t('common.select')} —</option>
                {suppliers.map(s => <option key={s.supplierId} value={s.supplierId}>{s.name}</option>)}
              </select>
            </Field>
            <Field label={t('supplierApi.apiUrl')}><input className="input w-full" required value={apiForm.apiUrl} onChange={e => setApiForm({ ...apiForm, apiUrl: e.target.value })} /></Field>
            <Field label={t('supplierApi.apiKeyRef')}><input className="input w-full" required value={apiForm.apiKeyRef} onChange={e => setApiForm({ ...apiForm, apiKeyRef: e.target.value })} /></Field>
            <Field label={t('supplierApi.format')}>
              <select className="input w-full" value={apiForm.format} onChange={e => setApiForm({ ...apiForm, format: e.target.value })}>
                <option value="JSON">JSON</option>
                <option value="XML">XML</option>
                <option value="EDI">EDI</option>
              </select>
            </Field>
            <label className="flex items-center gap-2">
              <input type="checkbox" checked={apiForm.enabled} onChange={e => setApiForm({ ...apiForm, enabled: e.target.checked })} />
              {t('supplierApi.enabled')}
            </label>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowApiForm(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={apiSaving}>{apiSaving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}
      {confirmModal}
    </div>
  );
}
