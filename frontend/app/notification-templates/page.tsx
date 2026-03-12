'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Spinner, Alert, Success } from '@/components/ui';
import { Bell, Mail, MessageCircle, Smartphone, Edit, Eye, X } from 'lucide-react';

interface Template {
  templateId: string;
  tenantId: string;
  type: string;
  channel: string;
  subject: string;
  bodyTemplate: string;
  active: boolean;
}

const TYPES = [
  'ORDER_CONFIRMATION', 'PRODUCTION_STARTED', 'READY_FOR_DELIVERY',
  'OUT_FOR_DELIVERY', 'DELIVERED', 'PAYMENT_REMINDER', 'STOCK_ALERT', 'PROMOTIONAL',
] as const;

const CHANNELS = ['PUSH', 'EMAIL', 'WHATSAPP', 'SMS'] as const;

const CHANNEL_ICON: Record<string, typeof Bell> = {
  PUSH: Bell, EMAIL: Mail, WHATSAPP: MessageCircle, SMS: Smartphone,
};

const VARIABLES = ['{{orderNumber}}', '{{customerName}}', '{{status}}'];

const typeLabel = (t: string) => t.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());

export default function NotificationTemplatesPage() {
  const t = useT();
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [editing, setEditing] = useState<Template | null>(null);
  const [preview, setPreview] = useState<{ subject: string; body: string } | null>(null);

  // Edit form state
  const [editSubject, setEditSubject] = useState('');
  const [editBody, setEditBody] = useState('');
  const [editChannel, setEditChannel] = useState('PUSH');
  const [editActive, setEditActive] = useState(true);
  const [editType, setEditType] = useState('ORDER_CONFIRMATION');
  const [isNew, setIsNew] = useState(false);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const data = await apiFetch<Template[]>(`/v3/notifications/templates?tenantId=${TENANT_ID}`);
      setTemplates(data);
    } catch (e) { setError(String(e)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const openEdit = (tpl: Template) => {
    setEditing(tpl);
    setEditSubject(tpl.subject ?? '');
    setEditBody(tpl.bodyTemplate ?? '');
    setEditChannel(tpl.channel);
    setEditActive(tpl.active);
    setEditType(tpl.type);
    setIsNew(false);
  };

  const openNew = () => {
    setEditing({} as Template);
    setEditSubject('');
    setEditBody('');
    setEditChannel('PUSH');
    setEditActive(true);
    setEditType('ORDER_CONFIRMATION');
    setIsNew(true);
  };

  const saveTemplate = async () => {
    try {
      if (isNew) {
        await apiFetch(`/v3/notifications/templates`, {
          method: 'POST',
          body: JSON.stringify({
            tenantId: TENANT_ID,
            type: editType,
            channel: editChannel,
            subject: editSubject,
            bodyTemplate: editBody,
          }),
        });
      } else if (editing?.templateId) {
        await apiFetch(`/v3/notifications/templates/${editing.templateId}`, {
          method: 'PUT',
          body: JSON.stringify({
            subject: editSubject,
            bodyTemplate: editBody,
            active: editActive,
          }),
        });
      }
      setEditing(null);
      setSuccess(t('notificationTemplates.saved'));
      setTimeout(() => setSuccess(''), 3000);
      load();
    } catch (e) { setError(String(e)); }
  };

  const previewTemplate = async (tpl: Template) => {
    try {
      const data = await apiFetch<{ subject: string; body: string }>(
        `/v3/notifications/templates/${tpl.templateId}/preview`,
        { method: 'POST', body: JSON.stringify({}) },
      );
      setPreview(data);
    } catch (e) { setError(String(e)); }
  };

  const insertVar = (v: string) => {
    setEditBody(editBody + v);
  };

  // Group templates by type for the grid display
  const typeMap = new Map<string, Template[]>();
  templates.forEach(tpl => {
    if (!typeMap.has(tpl.type)) typeMap.set(tpl.type, []);
    typeMap.get(tpl.type)!.push(tpl);
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t('notificationTemplates.title')}</h1>
        <button onClick={openNew}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
          {t('notificationTemplates.addTemplate')}
        </button>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {loading ? <Spinner /> : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {TYPES.map(type => {
            const tpls = typeMap.get(type) ?? [];
            const Icon = CHANNEL_ICON[tpls[0]?.channel] ?? Bell;
            return (
              <div key={type} className="rounded-xl border bg-white p-4 shadow-sm">
                <div className="flex items-start justify-between mb-2">
                  <div>
                    <h3 className="text-sm font-semibold text-gray-900">{typeLabel(type)}</h3>
                    <div className="flex gap-1 mt-1">
                      {tpls.map(tpl => {
                        const ChIcon = CHANNEL_ICON[tpl.channel] ?? Bell;
                        return (
                          <span key={tpl.templateId} title={tpl.channel}
                            className={`inline-flex h-6 w-6 items-center justify-center rounded ${tpl.active ? 'bg-blue-50 text-blue-600' : 'bg-gray-100 text-gray-400'}`}>
                            <ChIcon className="h-3.5 w-3.5" />
                          </span>
                        );
                      })}
                    </div>
                  </div>
                </div>
                <p className="text-xs text-gray-500 line-clamp-2 mb-3">
                  {tpls[0]?.bodyTemplate ?? 'No template configured'}
                </p>
                <div className="flex gap-2">
                  {tpls[0] && (
                    <>
                      <button onClick={() => openEdit(tpls[0])}
                        className="flex items-center gap-1 rounded border px-2 py-1 text-xs font-medium text-gray-700 hover:bg-gray-50">
                        <Edit className="h-3 w-3" /> {t('common.edit')}
                      </button>
                      <button onClick={() => previewTemplate(tpls[0])}
                        className="flex items-center gap-1 rounded border px-2 py-1 text-xs font-medium text-gray-700 hover:bg-gray-50">
                        <Eye className="h-3 w-3" /> {t('notificationTemplates.preview')}
                      </button>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Edit modal */}
      {editing && (
        <Modal title={isNew ? t('notificationTemplates.addTemplate') : t('notificationTemplates.editTemplate')} onClose={() => setEditing(null)}>
          <div className="space-y-4">
            {isNew && (
              <>
                <div>
                  <label className="text-sm font-medium text-gray-700">{t('notificationTemplates.typeLabel')}</label>
                  <select value={editType} onChange={(e) => setEditType(e.target.value)}
                    className="mt-1 w-full rounded border px-3 py-2 text-sm">
                    {TYPES.map(tp => <option key={tp} value={tp}>{typeLabel(tp)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-700">{t('notificationTemplates.channelLabel')}</label>
                  <div className="mt-1 flex gap-2">
                    {CHANNELS.map(ch => (
                      <label key={ch} className="flex items-center gap-1 text-sm">
                        <input type="radio" name="channel" value={ch} checked={editChannel === ch}
                          onChange={() => setEditChannel(ch)} />
                        {ch}
                      </label>
                    ))}
                  </div>
                </div>
              </>
            )}
            <div>
              <label className="text-sm font-medium text-gray-700">{t('notificationTemplates.subject')}</label>
              <input value={editSubject} onChange={(e) => setEditSubject(e.target.value)}
                className="mt-1 w-full rounded border px-3 py-2 text-sm" />
            </div>
            <div>
              <label className="text-sm font-medium text-gray-700">{t('notificationTemplates.body')}</label>
              <textarea value={editBody} onChange={(e) => setEditBody(e.target.value)}
                rows={4} className="mt-1 w-full rounded border px-3 py-2 text-sm font-mono" />
              <div className="mt-1 flex gap-1">
                {VARIABLES.map(v => (
                  <button key={v} onClick={() => insertVar(v)}
                    className="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600 hover:bg-gray-200">
                    {v}
                  </button>
                ))}
              </div>
            </div>
            {!isNew && (
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={editActive} onChange={(e) => setEditActive(e.target.checked)} />
                {t('notificationTemplates.active')}
              </label>
            )}
            <div className="flex justify-end gap-2">
              <button onClick={() => setEditing(null)} className="rounded border px-4 py-2 text-sm">{t('common.cancel')}</button>
              <button onClick={saveTemplate} className="rounded bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700">{t('common.save')}</button>
            </div>
          </div>
        </Modal>
      )}

      {/* Preview modal */}
      {preview && (
        <Modal title={t('notificationTemplates.preview')} onClose={() => setPreview(null)}>
          <div className="space-y-3">
            <div>
              <label className="text-xs font-medium text-gray-500">{t('notificationTemplates.subject')}</label>
              <div className="rounded bg-gray-50 px-3 py-2 text-sm">{preview.subject || '(empty)'}</div>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500">{t('notificationTemplates.body')}</label>
              <div className="rounded bg-gray-50 px-3 py-2 text-sm whitespace-pre-wrap">{preview.body || '(empty)'}</div>
            </div>
            <div className="flex justify-end">
              <button onClick={() => setPreview(null)} className="rounded border px-4 py-2 text-sm">{t('common.close')}</button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
