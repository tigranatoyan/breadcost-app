'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success } from '@/components/ui';

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

/* ── page ──────────────────────────────────────────────── */
export default function SuppliersPage() {
  const t = useT();

  /* tabs */
  const [tab, setTab] = useState<'suppliers' | 'purchase-orders'>('suppliers');

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
  const [catForm, setCatForm] = useState({ ingredientId: '', ingredientName: '', unitPrice: '', currency: 'UZS', leadTimeDays: '', moq: '', unit: 'kg' });
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

  useEffect(() => { loadSuppliers(); }, [loadSuppliers]);
  useEffect(() => { if (tab === 'purchase-orders') loadPOs(); }, [tab, loadPOs]);

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
    if (!confirm(t('suppliers.confirmDelete'))) return;
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
      setCatForm({ ingredientId: '', ingredientName: '', unitPrice: '', currency: 'UZS', leadTimeDays: '', moq: '', unit: 'kg' });
      openCatalog(catalogSup);
    } catch (e) { setError(String(e)); } finally { setCatSaving(false); }
  };

  /* ── PO CRUD ────────────────────────────────────────── */
  const addPOLine = () => setPoLines([...poLines, { ingredientId: '', ingredientName: '', qty: '', unit: 'kg', unitPrice: '', currency: 'UZS' }]);
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
      const res = await fetch(`http://localhost:8080/v2/purchase-orders/${id}/export?tenantId=${TENANT_ID}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('bc_token')}` },
      });
      if (!res.ok) throw new Error('Export failed');
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

  /* ── render ─────────────────────────────────────────── */
  return (
    <div className="max-w-6xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">{t('suppliers.title')}</h1>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* tabs */}
      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {(['suppliers', 'purchase-orders'] as const).map(k => (
          <button key={k} onClick={() => setTab(k)}
            className={`px-4 py-2 text-sm font-medium -mb-px ${tab === k ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {t(`suppliers.tab_${k.replace('-', '_')}`)}
          </button>
        ))}
      </div>

      {/* ───────── SUPPLIERS TAB ───────── */}
      {tab === 'suppliers' && (
        <>
          <div className="flex justify-end mb-4">
            <button className="btn btn-primary text-sm" onClick={() => setShowCreate(true)}>+ {t('suppliers.add')}</button>
          </div>
          {loading ? <Spinner /> : (
            <Table
              cols={[t('common.name'), t('suppliers.email'), t('suppliers.phone'), t('common.actions')]}
              rows={suppliers.map(s => [
                s.name,
                s.contactEmail || '—',
                s.contactPhone || '—',
                <div key={s.supplierId} className="flex gap-1 flex-wrap">
                  <button className="btn btn-xs" onClick={() => openCatalog(s)}>{t('suppliers.catalog')}</button>
                  <button className="btn btn-xs" onClick={() => { setEditSup(s); setEditForm({ name: s.name, contactEmail: s.contactEmail || '', contactPhone: s.contactPhone || '', notes: s.notes || '' }); }}>{t('common.edit')}</button>
                  <button className="btn btn-xs btn-danger" onClick={() => deleteSupplier(s.supplierId)}>{t('common.delete')}</button>
                </div>,
              ])}
              empty={t('suppliers.empty')}
            />
          )}
        </>
      )}

      {/* ───────── PURCHASE ORDERS TAB ───────── */}
      {tab === 'purchase-orders' && (
        <>
          <div className="flex justify-end gap-2 mb-4">
            <button className="btn btn-secondary text-sm" onClick={suggestPOs} disabled={poSaving}>{t('suppliers.suggest')}</button>
            <button className="btn btn-primary text-sm" onClick={() => { setShowCreatePO(true); addPOLine(); }}>+ {t('suppliers.createPO')}</button>
          </div>
          {poLoading ? <Spinner /> : (
            <Table
              cols={[t('suppliers.poId'), t('suppliers.supplier'), t('common.status'), t('suppliers.total'), t('common.date'), t('common.actions')]}
              rows={pos.map(po => [
                <button key={po.poId} className="text-blue-600 underline text-sm" onClick={() => openPODetail(po.poId)}>{po.poId.slice(0, 8)}</button>,
                po.supplierName || po.supplierId.slice(0, 8),
                <Badge key={`s-${po.poId}`} status={po.status} />,
                po.totalAmount != null ? `${po.totalAmount.toFixed(2)} ${po.currency || ''}` : '—',
                po.createdAt ? new Date(po.createdAt).toLocaleDateString() : '—',
                <div key={`a-${po.poId}`} className="flex gap-1 flex-wrap">
                  {po.status === 'DRAFT' && <button className="btn btn-xs btn-success" onClick={() => approvePO(po.poId)}>{t('suppliers.approve')}</button>}
                  <button className="btn btn-xs" onClick={() => exportPO(po.poId)}>{t('suppliers.export')}</button>
                </div>,
              ])}
              empty={t('suppliers.poEmpty')}
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
              <button type="button" className="btn btn-secondary" onClick={() => setShowCreate(false)}>{t('common.cancel')}</button>
              <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? t('common.saving') : t('common.save')}</button>
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
              <button type="button" className="btn btn-secondary" onClick={() => setEditSup(null)}>{t('common.cancel')}</button>
              <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? t('common.saving') : t('common.save')}</button>
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
                rows={catalog.map(c => [c.ingredientName || c.ingredientId, c.unitPrice?.toFixed(2) ?? '—', c.currency || '—', c.leadTimeDays != null ? `${c.leadTimeDays}d` : '—', c.moq ?? '—', c.unit || '—'])}
                empty={t('suppliers.catalogEmpty')}
              />
              <form onSubmit={addCatalogItem} className="mt-4 grid grid-cols-3 gap-3 items-end">
                <Field label={t('suppliers.ingredientId')}><input className="input w-full" required value={catForm.ingredientId} onChange={e => setCatForm({ ...catForm, ingredientId: e.target.value })} /></Field>
                <Field label={t('suppliers.ingredientName')}><input className="input w-full" value={catForm.ingredientName} onChange={e => setCatForm({ ...catForm, ingredientName: e.target.value })} /></Field>
                <Field label={t('suppliers.unitPrice')}><input className="input w-full" type="number" step="0.01" required value={catForm.unitPrice} onChange={e => setCatForm({ ...catForm, unitPrice: e.target.value })} /></Field>
                <Field label={t('suppliers.currency')}><input className="input w-full" value={catForm.currency} onChange={e => setCatForm({ ...catForm, currency: e.target.value })} /></Field>
                <Field label={t('suppliers.leadTime')}><input className="input w-full" type="number" value={catForm.leadTimeDays} onChange={e => setCatForm({ ...catForm, leadTimeDays: e.target.value })} /></Field>
                <button type="submit" className="btn btn-primary h-10" disabled={catSaving}>{catSaving ? '…' : `+ ${t('common.add')}`}</button>
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
              <div key={i} className="grid grid-cols-6 gap-2 items-end">
                <Field label={t('suppliers.ingredientId')}><input className="input w-full" required value={line.ingredientId} onChange={e => updatePOLine(i, 'ingredientId', e.target.value)} /></Field>
                <Field label={t('suppliers.ingredientName')}><input className="input w-full" value={line.ingredientName} onChange={e => updatePOLine(i, 'ingredientName', e.target.value)} /></Field>
                <Field label={t('suppliers.qty')}><input className="input w-full" type="number" step="0.01" required value={line.qty} onChange={e => updatePOLine(i, 'qty', e.target.value)} /></Field>
                <Field label={t('suppliers.unit')}><input className="input w-full" value={line.unit} onChange={e => updatePOLine(i, 'unit', e.target.value)} /></Field>
                <Field label={t('suppliers.unitPrice')}><input className="input w-full" type="number" step="0.01" required value={line.unitPrice} onChange={e => updatePOLine(i, 'unitPrice', e.target.value)} /></Field>
                <button type="button" className="btn btn-xs btn-danger mb-1" onClick={() => removePOLine(i)}>✕</button>
              </div>
            ))}
            <button type="button" className="btn btn-secondary text-sm" onClick={addPOLine}>+ {t('suppliers.addLine')}</button>
            <div className="flex justify-end gap-2 pt-4">
              <button type="button" className="btn btn-secondary" onClick={() => { setShowCreatePO(false); setPoLines([]); }}>{t('common.cancel')}</button>
              <button type="submit" className="btn btn-primary" disabled={poSaving}>{poSaving ? t('common.saving') : t('common.save')}</button>
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
    </div>
  );
}
