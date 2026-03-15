'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';
import { Plus } from 'lucide-react';

interface Dept {
  departmentId: string;
  name: string;
  leadTimeHours: number;
  warehouseMode: string;
  status: string;
}

export default function DepartmentsPage() {
  const t = useT();
  const [depts, setDepts] = useState<Dept[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: '',
    leadTimeHours: 8,
    warehouseMode: 'ISOLATED',
  });

  // Edit modal (BC-1705)
  const [editDept, setEditDept] = useState<Dept | null>(null);
  const [editForm, setEditForm] = useState({ name: '', leadTimeHours: 8, warehouseMode: 'ISOLATED', status: 'ACTIVE' });

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const data = await apiFetch<Dept[]>(`/v1/departments?tenantId=${TENANT_ID}`);
      setDepts(data);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v1/departments', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, ...form }),
      });
      setOpen(false);
      setForm({ name: '', leadTimeHours: 8, warehouseMode: 'ISOLATED' });
      setSuccess(t('departments.created'));
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  // ── Edit department (BC-1705) ────────────────────────────────────────────────
  const openEdit = (d: Dept) => {
    setEditDept(d);
    setEditForm({ name: d.name, leadTimeHours: d.leadTimeHours, warehouseMode: d.warehouseMode, status: d.status ?? 'ACTIVE' });
  };

  const saveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editDept) return;
    try {
      setSaving(true);
      await apiFetch(`/v1/departments/${editDept.departmentId}?tenantId=${TENANT_ID}`, {
        method: 'PUT',
        body: JSON.stringify(editForm),
      });
      setEditDept(null);
      setSuccess(t('departments.updated'));
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-[1800px]">
      <SectionTitle
        eyebrow={t('departments.eyebrow')}
        title={t('departments.title')}
        subtitle={t('departments.subtitle')}
        action={
          <Button variant="primary" size="sm" onClick={() => setOpen(true)}>
            <Plus className="h-4 w-4" /> {t('departments.newDepartment')}
          </Button>
        }
      />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {loading ? (
        <Spinner />
      ) : (
        <Table
          cols={[t('departments.cols.name'), t('departments.cols.leadTime'), t('departments.cols.warehouseMode'), t('departments.cols.status'), t('common.actions')]}
          rows={depts.map((d) => [
            <span key={`name-${d.departmentId}`} className="font-medium">{d.name}</span>,
            `${d.leadTimeHours} hrs`,
            d.warehouseMode,
            <Badge key={`status-${d.departmentId}`} status={d.status ?? 'ACTIVE'} />,
            <button key={`edit-${d.departmentId}`} className="text-xs text-blue-600 hover:underline" onClick={() => openEdit(d)}>{t('common.edit')}</button>,
          ])}
          empty={t('departments.noDepartments')}
        />
      )}

      {open && (
        <Modal title={t('departments.newDepartmentTitle')} onClose={() => setOpen(false)}>
          <form onSubmit={submit} className="space-y-4">
            <Field label={t('common.name')}>
              <input
                className="input"
                required
                placeholder={t('departments.namePlaceholder')}
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              />
            </Field>
            <Field
              label={t('departments.leadTime')}
              hint={t('departments.leadTimeHint')}
            >
              <input
                className="input"
                type="number"
                min={0}
                value={form.leadTimeHours}
                onChange={(e) =>
                  setForm((f) => ({ ...f, leadTimeHours: +e.target.value }))
                }
              />
            </Field>
            <Field label={t('departments.warehouseMode')}>
              <select
                className="input"
                value={form.warehouseMode}
                onChange={(e) =>
                  setForm((f) => ({ ...f, warehouseMode: e.target.value }))
                }
              >
                <option value="ISOLATED">{t('departments.isolated')}</option>
                <option value="SHARED">{t('departments.shared')}</option>
              </select>
            </Field>
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

      {/* ── Edit Department Modal (BC-1705) ─────────────────────────────── */}
      {editDept && (
        <Modal title={t('departments.editTitle')} onClose={() => setEditDept(null)}>
          <form onSubmit={saveEdit} className="space-y-4">
            <Field label={t('common.name')}>
              <input className="input" required value={editForm.name}
                onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))} />
            </Field>
            <Field label={t('departments.leadTime')} hint={t('departments.leadTimeHint')}>
              <input className="input" type="number" min={0} value={editForm.leadTimeHours}
                onChange={(e) => setEditForm((f) => ({ ...f, leadTimeHours: +e.target.value }))} />
            </Field>
            <Field label={t('departments.warehouseMode')}>
              <select className="input" value={editForm.warehouseMode}
                onChange={(e) => setEditForm((f) => ({ ...f, warehouseMode: e.target.value }))}>
                <option value="ISOLATED">{t('departments.isolated')}</option>
                <option value="SHARED">{t('departments.shared')}</option>
              </select>
            </Field>
            <Field label={t('common.status')}>
              <select className="input" value={editForm.status}
                onChange={(e) => setEditForm((f) => ({ ...f, status: e.target.value }))}>
                <option value="ACTIVE">{t('admin.active')}</option>
                <option value="INACTIVE">{t('admin.inactive')}</option>
              </select>
            </Field>
            <div className="flex justify-end gap-2 pt-2 border-t">
              <Button variant="secondary" size="sm" onClick={() => setEditDept(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving}>
                {saving ? t('common.saving') : t('common.save')}
              </Button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
