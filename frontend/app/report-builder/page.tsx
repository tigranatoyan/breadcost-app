'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, API_BASE, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success, useConfirm } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';

/* ── types ─────────────────────────────────────────────── */
interface KpiBlock {
  blockId: string;
  name: string;
  description?: string;
  category?: string;
  queryKey: string;
}
interface CustomReport {
  reportId: string;
  tenantId: string;
  name: string;
  description?: string;
  blocks: string[];
  createdAt?: string;
}
interface ReportResult {
  reportId: string;
  name: string;
  data: Record<string, unknown>[];
  generatedAt?: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function ReportBuilderPage() {
  const t = useT();
  const [askConfirm, confirmModal] = useConfirm({ confirmLabel: t('common.confirm'), cancelLabel: t('common.cancel') });

  const [tab, setTab] = useState<'catalog' | 'reports'>('reports');

  /* catalog */
  const [blocks, setBlocks] = useState<KpiBlock[]>([]);
  const [blocksLoading, setBlocksLoading] = useState(false);

  /* reports */
  const [reports, setReports] = useState<CustomReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* create report */
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ name: '', description: '' });
  const [selectedBlocks, setSelectedBlocks] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);

  /* edit */
  const [editReport, setEditReport] = useState<CustomReport | null>(null);
  const [editForm, setEditForm] = useState({ name: '', description: '' });
  const [editBlocks, setEditBlocks] = useState<string[]>([]);

  /* run result */
  const [result, setResult] = useState<ReportResult | null>(null);
  const [running, setRunning] = useState(false);

  /* loaders */
  const loadBlocks = useCallback(async () => {
    try {
      setBlocksLoading(true);
      const data = await apiFetch<KpiBlock[]>(`/v2/reports/kpi-blocks?tenantId=${TENANT_ID}`);
      setBlocks(data);
    } catch (e) { setError(String(e)); } finally { setBlocksLoading(false); }
  }, []);

  const loadReports = useCallback(async () => {
    try {
      setLoading(true);
      const data = await apiFetch<CustomReport[]>(`/v2/reports?tenantId=${TENANT_ID}`);
      setReports(data);
    } catch (e) { setError(String(e)); } finally { setLoading(false); }
  }, []);

  useEffect(() => { loadReports(); loadBlocks(); }, [loadReports, loadBlocks]);

  /* toggle block selection */
  const toggleBlock = (id: string, list: string[], setList: (_v: string[]) => void) => {
    setList(list.includes(id) ? list.filter(b => b !== id) : [...list, id]);
  };

  /* create */
  const createReport = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await apiFetch('/v2/reports', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, name: form.name, description: form.description || undefined, blocks: selectedBlocks }),
      });
      setSuccess(t('reportBuilder.created'));
      setShowCreate(false);
      setForm({ name: '', description: '' });
      setSelectedBlocks([]);
      loadReports();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  /* update */
  const updateReport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editReport) return;
    try {
      setSaving(true);
      await apiFetch(`/v2/reports/${editReport.reportId}?tenantId=${TENANT_ID}`, {
        method: 'PUT',
        body: JSON.stringify({ name: editForm.name, description: editForm.description || undefined, blocks: editBlocks }),
      });
      setSuccess(t('reportBuilder.updated'));
      setEditReport(null);
      loadReports();
    } catch (e) { setError(String(e)); } finally { setSaving(false); }
  };

  /* delete */
  const deleteReport = async (id: string) => {
    if (!await askConfirm(t('reportBuilder.confirmDelete'))) return;
    try {
      await apiFetch(`/v2/reports/${id}?tenantId=${TENANT_ID}`, { method: 'DELETE' });
      setSuccess(t('reportBuilder.deleted'));
      loadReports();
    } catch (e) { setError(String(e)); }
  };

  /* run */
  const runReport = async (id: string) => {
    try {
      setRunning(true);
      const data = await apiFetch<ReportResult>(`/v2/reports/${id}/run?tenantId=${TENANT_ID}`);
      setResult(data);
    } catch (e) { setError(String(e)); } finally { setRunning(false); }
  };

  /* export */
  const exportReport = async (id: string) => {
    try {
      const res = await fetch(`${API_BASE}/v2/reports/${id}/export?tenantId=${TENANT_ID}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('bc_token')}` },
      });
      if (!res.ok) throw new Error('Export failed');
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `report-${id}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) { setError(String(e)); }
  };

  /* block picker */
  const BlockPicker = ({ selected, setSelected }: { selected: string[]; setSelected: (_v: string[]) => void }) => (
    <div className="max-h-60 overflow-y-auto border rounded p-2 space-y-1">
      {blocks.map(b => (
        <label key={b.blockId} className="flex items-center gap-2 text-sm cursor-pointer hover:bg-gray-50 p-1 rounded">
          <input type="checkbox" checked={selected.includes(b.blockId)} onChange={() => toggleBlock(b.blockId, selected, setSelected)} />
          <span className="font-medium">{b.name}</span>
          {b.category && <Badge status={b.category} />}
          {b.description && <span className="text-gray-400 text-xs">— {b.description}</span>}
        </label>
      ))}
      {blocks.length === 0 && <p className="text-sm text-gray-400">{t('reportBuilder.noBlocks')}</p>}
    </div>
  );

  return (
    <div className="max-w-[1800px]">
      <SectionTitle eyebrow="Analytics" title={t('reportBuilder.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {(['reports', 'catalog'] as const).map(k => (
          <button key={k} onClick={() => setTab(k)}
            className={`px-4 py-2 text-sm font-medium -mb-px ${tab === k ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {t(`reportBuilder.tab_${k}`)}
          </button>
        ))}
      </div>

      {/* ───────── REPORTS TAB ───────── */}
      {tab === 'reports' && (
        <>
          <div className="flex justify-end mb-4">
            <Button variant="primary" size="sm" onClick={() => setShowCreate(true)}>+ {t('reportBuilder.create')}</Button>
          </div>
          {loading ? <Spinner /> : (
            <Table
              cols={[t('common.name'), t('common.description'), t('reportBuilder.blockCount'), t('common.date'), t('common.actions')]}
              rows={reports.map(r => [
                r.name,
                r.description || '—',
                r.blocks.length.toString(),
                r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—',
                <div key={r.reportId} className="flex gap-1 flex-wrap">
                  <Button variant="primary" size="xs" className="bg-green-600 hover:bg-green-700" onClick={() => runReport(r.reportId)} disabled={running}>{t('reportBuilder.run')}</Button>
                  <Button variant="secondary" size="xs" onClick={() => exportReport(r.reportId)}>{t('reportBuilder.export')}</Button>
                  <Button variant="secondary" size="xs" onClick={() => { setEditReport(r); setEditForm({ name: r.name, description: r.description || '' }); setEditBlocks([...r.blocks]); }}>{t('common.edit')}</Button>
                  <Button variant="danger" size="xs" onClick={() => deleteReport(r.reportId)}>{t('common.delete')}</Button>
                </div>,
              ])}
              empty={t('reportBuilder.empty')}
            />
          )}
        </>
      )}

      {/* ───────── CATALOG TAB ───────── */}
      {tab === 'catalog' && (
        <>
          {blocksLoading ? <Spinner /> : (
            <Table
              cols={[t('common.name'), t('common.description'), t('reportBuilder.category'), t('reportBuilder.queryKey')]}
              rows={blocks.map(b => [b.name, b.description || '—', b.category ? <Badge key={b.blockId} status={b.category} /> : '—', <span key={`q-${b.blockId}`} className="font-mono text-xs">{b.queryKey}</span>])}
              empty={t('reportBuilder.noBlocks')}
            />
          )}
        </>
      )}

      {/* create modal */}
      {showCreate && (
        <Modal title={t('reportBuilder.create')} onClose={() => { setShowCreate(false); setSelectedBlocks([]); }} wide>
          <form onSubmit={createReport} className="space-y-4">
            <Field label={t('common.name')}><input className="input w-full" required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></Field>
            <Field label={t('common.description')}><textarea className="input w-full" rows={2} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></Field>
            <Field label={t('reportBuilder.selectBlocks')}><BlockPicker selected={selectedBlocks} setSelected={setSelectedBlocks} /></Field>
            <p className="text-sm text-gray-500">{selectedBlocks.length} {t('reportBuilder.blocksSelected')}</p>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => { setShowCreate(false); setSelectedBlocks([]); }}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving || selectedBlocks.length === 0}>{saving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* edit modal */}
      {editReport && (
        <Modal title={t('reportBuilder.edit')} onClose={() => setEditReport(null)} wide>
          <form onSubmit={updateReport} className="space-y-4">
            <Field label={t('common.name')}><input className="input w-full" required value={editForm.name} onChange={e => setEditForm({ ...editForm, name: e.target.value })} /></Field>
            <Field label={t('common.description')}><textarea className="input w-full" rows={2} value={editForm.description} onChange={e => setEditForm({ ...editForm, description: e.target.value })} /></Field>
            <Field label={t('reportBuilder.selectBlocks')}><BlockPicker selected={editBlocks} setSelected={setEditBlocks} /></Field>
            <p className="text-sm text-gray-500">{editBlocks.length} {t('reportBuilder.blocksSelected')}</p>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setEditReport(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={saving || editBlocks.length === 0}>{saving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* result modal */}
      {result && (
        <Modal title={`${t('reportBuilder.result')} — ${result.name}`} onClose={() => setResult(null)} wide>
          {result.data.length === 0 ? (
            <p className="text-sm text-gray-500">{t('reportBuilder.noData')}</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr>{Object.keys(result.data[0]).map(k => <th key={k} className="text-left p-2 border-b font-medium">{k}</th>)}</tr>
                </thead>
                <tbody>
                  {result.data.map((row, i) => (
                    <tr key={i} className="border-b">
                      {Object.values(row).map((v, j) => <td key={j} className="p-2">{String(v ?? '—')}</td>)}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {result.generatedAt && <p className="text-xs text-gray-400 mt-2">{t('reportBuilder.generatedAt')}: {new Date(result.generatedAt).toLocaleString()}</p>}
        </Modal>
      )}
      {confirmModal}
    </div>
  );
}
