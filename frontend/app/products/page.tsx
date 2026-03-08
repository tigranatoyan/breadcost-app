'use client';
import { useState, useEffect, useCallback, useRef } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Modal, Table, Spinner, Alert, Badge, Field, Success } from '@/components/ui';
import { useT } from '@/lib/i18n';

interface Dept {
  departmentId: string;
  name: string;
}
interface Product {
  productId: string;
  name: string;
  departmentId: string;
  departmentName: string;
  saleUnit: string;
  baseUom: string;
  price: number | null;
  vatRatePct: number;
  status: string;
}

export default function ProductsPage() {
  const t = useT();
  const [products, setProducts] = useState<Product[]>([]);
  const [depts, setDepts] = useState<Dept[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  // Search & filter (BC-1706)
  const [search, setSearch] = useState('');
  const [filterDept, setFilterDept] = useState('');

  // Edit modal (BC-1706)
  const [editProduct, setEditProduct] = useState<Product | null>(null);
  const [editForm, setEditForm] = useState({ name: '', departmentId: '', saleUnit: 'PIECE', baseUom: 'PCS', price: '', vatRatePct: '0', status: 'ACTIVE' });
  const defaultForm = {
    departmentId: '',
    name: '',
    description: '',
    saleUnit: 'PIECE',
    baseUom: 'PCS',
    price: '',
    vatRatePct: '0',
  };
  const [form, setForm] = useState(defaultForm);
  const deptsRef = useRef<Dept[]>([]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [prods, deps] = await Promise.all([
        apiFetch<Product[]>(`/v1/products?tenantId=${TENANT_ID}`),
        apiFetch<Dept[]>(`/v1/departments?tenantId=${TENANT_ID}`),
      ]);
      setProducts(prods);
      setDepts(deps);
      deptsRef.current = deps;
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openForm = () => {
    const firstDept = deptsRef.current[0];
    setForm({
      ...defaultForm,
      departmentId: firstDept?.departmentId ?? '',
    });
    setOpen(true);
  };

  // ── Edit product (BC-1706) ───────────────────────────────────────────────────
  const openEditProduct = (p: Product) => {
    setEditProduct(p);
    setEditForm({
      name: p.name,
      departmentId: p.departmentId,
      saleUnit: p.saleUnit,
      baseUom: p.baseUom,
      price: p.price != null ? String(p.price) : '',
      vatRatePct: p.vatRatePct != null ? String(p.vatRatePct) : '0',
      status: p.status ?? 'ACTIVE',
    });
  };

  const saveEditProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editProduct) return;
    try {
      setSaving(true);
      await apiFetch(`/v1/products/${editProduct.productId}?tenantId=${TENANT_ID}`, {
        method: 'PUT',
        body: JSON.stringify(editForm),
      });
      setEditProduct(null);
      setSuccess(t('products.updated'));
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  // Filtered products
  const filtered = products.filter((p) => {
    if (filterDept && p.departmentId !== filterDept) return false;
    if (search && !p.name.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v1/products', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, ...form }),
      });
      setOpen(false);
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-semibold">{t('products.title')}</h1>
        <button className="btn-primary" onClick={openForm}>
          {t('products.newProduct')}
        </button>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* ── Search & Filter (BC-1706) ─────────────────────────────────── */}
      {!loading && products.length > 0 && (
        <div className="flex gap-3 mb-4">
          <input className="input max-w-xs text-sm" placeholder={t('products.searchPlaceholder')}
            value={search} onChange={(e) => setSearch(e.target.value)} />
          <select className="input max-w-xs text-sm" value={filterDept} onChange={(e) => setFilterDept(e.target.value)}>
            <option value="">{t('products.allDepartments')}</option>
            {depts.map((d) => <option key={d.departmentId} value={d.departmentId}>{d.name}</option>)}
          </select>
        </div>
      )}

      {loading ? (
        <Spinner />
      ) : (
        <Table
          cols={[t('products.cols.name'), t('products.cols.department'), t('products.cols.saleUnit'), t('products.cols.baseUom'), t('products.cols.price'), t('products.cols.vat'), t('products.cols.status'), t('common.actions')]}
          rows={filtered.map((p) => [
            <span className="font-medium">{p.name}</span>,
            p.departmentName ?? depts.find((d) => d.departmentId === p.departmentId)?.name ?? p.departmentId,
            p.saleUnit,
            p.baseUom,
            p.price != null ? p.price.toLocaleString() : '—',
            p.vatRatePct != null ? `${p.vatRatePct}%` : '—',
            <Badge status={p.status ?? 'ACTIVE'} />,
            <button className="text-xs text-blue-600 hover:underline" onClick={() => openEditProduct(p)}>{t('common.edit')}</button>,
          ])}
          empty={t('products.noProducts')}
        />
      )}

      {open && (
        <Modal title={t('products.newProductTitle')} onClose={() => setOpen(false)}>
          <form onSubmit={submit} className="space-y-4">
            <Field label={t('products.department')}>
              <select
                className="input"
                required
                value={form.departmentId}
                onChange={(e) => setForm((f) => ({ ...f, departmentId: e.target.value }))}
              >
                <option value="">{t('products.selectDepartment')}</option>
                {depts.map((d) => (
                  <option key={d.departmentId} value={d.departmentId}>
                    {d.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label={t('products.productName')}>
              <input
                className="input"
                required
                placeholder={t('products.productPlaceholder')}
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              />
            </Field>
            <Field label={t('common.description')}>
              <input
                className="input"
                placeholder={t('products.descriptionPlaceholder')}
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('products.saleUnit')}>
                <select
                  className="input"
                  value={form.saleUnit}
                  onChange={(e) => setForm((f) => ({ ...f, saleUnit: e.target.value }))}
                >
                  <option value="PIECE">PIECE</option>
                  <option value="WEIGHT">WEIGHT</option>
                  <option value="VOLUME">VOLUME</option>
                </select>
              </Field>
              <Field label={t('products.baseUom')} hint={t('products.baseUomHint')}>
                <input
                  className="input"
                  required
                  placeholder="PCS"
                  value={form.baseUom}
                  onChange={(e) => setForm((f) => ({ ...f, baseUom: e.target.value }))}
                />
              </Field>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('products.salePrice')}>
                <input
                  className="input"
                  type="number"
                  min="0"
                  step="0.01"
                  placeholder="0.00"
                  value={form.price}
                  onChange={(e) => setForm((f) => ({ ...f, price: e.target.value }))}
                />
              </Field>
              <Field label={t('products.vatRate')}>
                <input
                  className="input"
                  type="number"
                  min="0"
                  max="100"
                  step="0.01"
                  placeholder="0"
                  value={form.vatRatePct}
                  onChange={(e) => setForm((f) => ({ ...f, vatRatePct: e.target.value }))}
                />
              </Field>
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button type="button" className="btn-secondary" onClick={() => setOpen(false)}>
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="btn-primary"
                disabled={saving || !form.departmentId}
              >
                {saving ? t('common.saving') : t('common.create')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ── Edit Product Modal (BC-1706) ──────────────────────────────── */}
      {editProduct && (
        <Modal title={t('products.editTitle')} onClose={() => setEditProduct(null)}>
          <form onSubmit={saveEditProduct} className="space-y-4">
            <Field label={t('products.productName')}>
              <input className="input" required value={editForm.name}
                onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))} />
            </Field>
            <Field label={t('products.department')}>
              <select className="input" required value={editForm.departmentId}
                onChange={(e) => setEditForm((f) => ({ ...f, departmentId: e.target.value }))}>
                <option value="">{t('products.selectDepartment')}</option>
                {depts.map((d) => <option key={d.departmentId} value={d.departmentId}>{d.name}</option>)}
              </select>
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('products.saleUnit')}>
                <select className="input" value={editForm.saleUnit}
                  onChange={(e) => setEditForm((f) => ({ ...f, saleUnit: e.target.value }))}>
                  <option value="PIECE">PIECE</option>
                  <option value="WEIGHT">WEIGHT</option>
                  <option value="VOLUME">VOLUME</option>
                </select>
              </Field>
              <Field label={t('products.baseUom')} hint={t('products.baseUomHint')}>
                <input className="input" required value={editForm.baseUom}
                  onChange={(e) => setEditForm((f) => ({ ...f, baseUom: e.target.value }))} />
              </Field>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('products.salePrice')}>
                <input className="input" type="number" min="0" step="0.01" value={editForm.price}
                  onChange={(e) => setEditForm((f) => ({ ...f, price: e.target.value }))} />
              </Field>
              <Field label={t('products.vatRate')}>
                <input className="input" type="number" min="0" max="100" step="0.01" value={editForm.vatRatePct}
                  onChange={(e) => setEditForm((f) => ({ ...f, vatRatePct: e.target.value }))} />
              </Field>
            </div>
            <Field label={t('common.status')}>
              <select className="input" value={editForm.status}
                onChange={(e) => setEditForm((f) => ({ ...f, status: e.target.value }))}>
                <option value="ACTIVE">{t('admin.active')}</option>
                <option value="INACTIVE">{t('admin.inactive')}</option>
              </select>
            </Field>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button type="button" className="btn-secondary" onClick={() => setEditProduct(null)}>{t('common.cancel')}</button>
              <button type="submit" className="btn-primary" disabled={saving}>
                {saving ? t('common.saving') : t('common.save')}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
