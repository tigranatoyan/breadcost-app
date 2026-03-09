'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Success } from '@/components/ui';

/* ── types ─────────────────────────────────────────────── */
interface Conversation {
  id: string;
  tenantId: string;
  customerPhone: string;
  status: string;
  messageCount?: number;
  escalated?: boolean;
  createdAt?: string;
}
interface Message {
  id: string;
  conversationId: string;
  sender: string;
  text: string;
  sentAt: string;
}
interface DraftOrder {
  id: string;
  conversationId: string;
  productName: string;
  quantity: number;
}

/* ── page ──────────────────────────────────────────────── */
export default function AiWhatsappPage() {
  const t = useT();

  const [tab, setTab] = useState<'all' | 'escalated'>('all');
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* detail view */
  const [selected, setSelected] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [drafts, setDrafts] = useState<DraftOrder[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);

  const loadConversations = useCallback(async () => {
    try {
      setLoading(true);
      const endpoint = tab === 'escalated'
        ? `/v3/ai/whatsapp/conversations/escalated?tenantId=${TENANT_ID}`
        : `/v3/ai/whatsapp/conversations?tenantId=${TENANT_ID}`;
      const data = await apiFetch<Conversation[]>(endpoint);
      setConversations(data);
    } catch (e) { setError(String(e)); }
    finally { setLoading(false); }
  }, [tab]);

  useEffect(() => { loadConversations(); }, [loadConversations]);

  const openConversation = async (conv: Conversation) => {
    setSelected(conv);
    setDetailLoading(true);
    try {
      const [msgs, drs] = await Promise.all([
        apiFetch<Message[]>(`/v3/ai/whatsapp/conversations/${conv.id}/messages?tenantId=${TENANT_ID}`),
        apiFetch<DraftOrder[]>(`/v3/ai/whatsapp/conversations/${conv.id}/drafts?tenantId=${TENANT_ID}`),
      ]);
      setMessages(msgs);
      setDrafts(drs);
    } catch (e) { setError(String(e)); }
    finally { setDetailLoading(false); }
  };

  const resolveEscalation = async (close: boolean) => {
    if (!selected) return;
    try {
      await apiFetch(`/v3/ai/whatsapp/conversations/${selected.id}/resolve?tenantId=${TENANT_ID}&close=${close}`, { method: 'POST' });
      setSuccess(t('aiWhatsapp.resolved'));
      setSelected(null);
      loadConversations();
    } catch (e) { setError(String(e)); }
  };

  const tabs: { key: typeof tab; label: string }[] = [
    { key: 'all', label: t('aiWhatsapp.allConversations') },
    { key: 'escalated', label: t('aiWhatsapp.escalated') },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">{t('aiWhatsapp.title')}</h1>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* tabs */}
      <div className="flex gap-1">
        {tabs.map(tb => (
          <button key={tb.key} onClick={() => { setTab(tb.key); setSelected(null); }}
            className={`px-4 py-2 rounded-t font-medium ${tab === tb.key ? 'bg-white border-t border-x text-blue-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
            {tb.label}
          </button>
        ))}
      </div>

      {loading ? <Spinner /> : (
        <Table
          cols={[t('aiWhatsapp.phone'), t('aiWhatsapp.status'), t('aiWhatsapp.createdAt'), '']}
          rows={conversations.map(c => [
            c.customerPhone,
            <Badge key={c.id} status={c.status} />,
            c.createdAt ? new Date(c.createdAt).toLocaleString() : '-',
            <button key={c.id + 'v'} onClick={() => openConversation(c)} className="text-blue-600 hover:underline text-sm">{t('aiWhatsapp.messages')}</button>,
          ])}
          empty={t('aiWhatsapp.noConversations')}
        />
      )}

      {/* conversation detail modal */}
      {selected && (
        <Modal title={`${t('aiWhatsapp.messageThread')} — ${selected.customerPhone}`} onClose={() => setSelected(null)} wide>
          {detailLoading ? <Spinner /> : (
            <div className="space-y-4">
              {/* messages */}
              <div className="max-h-80 overflow-y-auto space-y-2 border rounded p-3 bg-gray-50">
                {messages.length === 0 && <p className="text-gray-500 text-sm">{t('aiWhatsapp.noConversations')}</p>}
                {messages.map(m => (
                  <div key={m.id} className={`flex ${m.sender === 'BOT' ? 'justify-start' : 'justify-end'}`}>
                    <div className={`rounded-lg px-3 py-2 max-w-[70%] text-sm ${m.sender === 'BOT' ? 'bg-blue-100' : 'bg-green-100'}`}>
                      <p className="font-medium text-xs text-gray-500">{m.sender} · {new Date(m.sentAt).toLocaleTimeString()}</p>
                      <p>{m.text}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* draft orders */}
              {drafts.length > 0 && (
                <div>
                  <h4 className="font-semibold mb-1">{t('aiWhatsapp.draftOrders')}</h4>
                  <Table
                    cols={[t('aiWhatsapp.productName'), t('aiWhatsapp.quantity')]}
                    rows={drafts.map(d => [d.productName, d.quantity])}
                  />
                </div>
              )}

              {/* resolve buttons */}
              {selected.escalated && (
                <div className="flex gap-2 justify-end">
                  <button onClick={() => resolveEscalation(false)} className="rounded bg-yellow-500 px-4 py-2 text-white hover:bg-yellow-600">{t('aiWhatsapp.resolve')}</button>
                  <button onClick={() => resolveEscalation(true)} className="rounded bg-red-500 px-4 py-2 text-white hover:bg-red-600">{t('aiWhatsapp.resolveAndClose')}</button>
                </div>
              )}
            </div>
          )}
        </Modal>
      )}
    </div>
  );
}
