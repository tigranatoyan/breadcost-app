'use client';
import { useState, useEffect, useCallback, useRef } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Modal, Table, Spinner, Alert, Badge, Field } from '@/components/ui';

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
  status: string;
}

export default function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [depts, setDepts] = useState<Dept[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const defaultForm = {
    departmentId: '',
    name: '',
    description: '',
    saleUnit: 'PIECE',
    baseUom: 'PCS',
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
        <h1 className="text-2xl font-semibold">Products</h1>
        <button className="btn-primary" onClick={openForm}>
          + New Product
        </button>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {loading ? (
        <Spinner />
      ) : (
        <Table
          cols={['Name', 'Department', 'Sale Unit', 'Base UoM', 'Status', 'ID']}
          rows={products.map((p) => [
            <span className="font-medium">{p.name}</span>,
            p.departmentName ??
              depts.find((d) => d.departmentId === p.departmentId)?.name ??
              p.departmentId,
            p.saleUnit,
            p.baseUom,
            <Badge status={p.status ?? 'ACTIVE'} />,
            <code className="text-xs text-gray-400">{p.productId}</code>,
          ])}
          empty="No products yet."
        />
      )}

      {open && (
        <Modal title="New Product" onClose={() => setOpen(false)}>
          <form onSubmit={submit} className="space-y-4">
            <Field label="Department">
              <select
                className="input"
                required
                value={form.departmentId}
                onChange={(e) => setForm((f) => ({ ...f, departmentId: e.target.value }))}
              >
                <option value="">-- select a department --</option>
                {depts.map((d) => (
                  <option key={d.departmentId} value={d.departmentId}>
                    {d.name}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Product Name">
              <input
                className="input"
                required
                placeholder="e.g. White Bread 500g"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              />
            </Field>
            <Field label="Description">
              <input
                className="input"
                placeholder="Optional"
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              />
            </Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Sale Unit">
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
              <Field label="Base UoM" hint="e.g. PCS, KG, L">
                <input
                  className="input"
                  required
                  placeholder="PCS"
                  value={form.baseUom}
                  onChange={(e) => setForm((f) => ({ ...f, baseUom: e.target.value }))}
                />
              </Field>
            </div>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <button type="button" className="btn-secondary" onClick={() => setOpen(false)}>
                Cancel
              </button>
              <button
                type="submit"
                className="btn-primary"
                disabled={saving || !form.departmentId}
              >
                {saving ? 'Saving…' : 'Create'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
