'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field } from '@/components/ui';

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
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: '',
    leadTimeHours: 8,
    warehouseMode: 'ISOLATED',
  });

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
        <h1 className="text-2xl font-semibold">{t('departments.title')}</h1>
        <button className="btn-primary" onClick={() => setOpen(true)}>
          {t('departments.newDepartment')}
        </button>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {loading ? (
        <Spinner />
      ) : (
        <Table
          cols={[t('departments.cols.name'), t('departments.cols.leadTime'), t('departments.cols.warehouseMode'), t('departments.cols.status'), t('departments.cols.id')]}
          rows={depts.map((d) => [
            <span className="font-medium">{d.name}</span>,
            `${d.leadTimeHours} hrs`,
            d.warehouseMode,
            <Badge status={d.status ?? 'ACTIVE'} />,
            <code className="text-xs text-gray-400">{d.departmentId}</code>,
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
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setOpen(false)}
              >
                {t('common.cancel')}
              </button>
              <button type="submit" className="btn-primary" disabled={saving}>
                {saving ? t('common.saving') : t('common.create')}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
