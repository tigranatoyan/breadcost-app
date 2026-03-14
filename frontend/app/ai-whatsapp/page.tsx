'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Spinner, Alert, Badge, Success } from '@/components/ui';
import { SectionTitle, Button, Card } from '@/components/design-system';
import { Shield, MessageCircle, Bot, User } from 'lucide-react';

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

/* ── BC-2809: Flow reference cards ───────────────── */
function FlowCards() {
  const flows = [
    {
      title: 'New Order',
      description: 'Customer places an order via WhatsApp',
      messages: [
        { sender: 'customer', text: 'Hi, I want to order 50 baguettes for tomorrow' },
        { sender: 'bot', text: 'Got it! 50 Baguettes for delivery tomorrow morning. Here are your options:' },
        { sender: 'bot', text: '✅ Confirm  ✏️ Edit  ❌ Cancel' },
      ],
    },
    {
      title: 'Status Update',
      description: 'Automated delivery notification to customer',
      messages: [
        { sender: 'bot', text: '📦 Order #ORD-4521 Update' },
        { sender: 'bot', text: 'Your order is now Out for Delivery! Estimated arrival: 10:30 AM' },
        { sender: 'bot', text: '🚚 Driver: Armen · Track: breadcost.app/track/4521' },
      ],
    },
    {
      title: 'Escalation',
      description: 'Complex request handed off to team',
      messages: [
        { sender: 'customer', text: 'I need to change my delivery schedule for the next 3 weeks and add custom packaging' },
        { sender: 'bot', text: '🙋 This request needs a team member. Connecting you now…' },
        { sender: 'bot', text: '👤 Karen from Sales will follow up within 15 minutes.' },
      ],
    },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {flows.map((flow) => (
        <div key={flow.title} className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
          <h3 className="text-base font-semibold text-gray-900">{flow.title}</h3>
          <p className="mb-3 text-xs text-gray-500">{flow.description}</p>
          <div className="space-y-2 rounded-2xl bg-gray-50 p-3">
            {flow.messages.map((m, i) => (
              <div key={i} className={`flex ${m.sender === 'bot' ? 'justify-start' : 'justify-end'}`}>
                <div className={`rounded-2xl px-4 py-2.5 text-xs max-w-[85%] shadow-sm ${
                  m.sender === 'bot' ? 'bg-white text-gray-800' : 'bg-blue-600 text-white'
                }`}>
                  {m.text}
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

/* ── page ──────────────────────────────────────────────── */
export default function AiWhatsappPage() {
  const t = useT();

  const [tab, setTab] = useState<'all' | 'escalated' | 'flows'>('all');
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
        ? `/v3/ai/conversations/escalated?tenantId=${TENANT_ID}`
        : `/v3/ai/conversations?tenantId=${TENANT_ID}`;
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
        apiFetch<Message[]>(`/v3/ai/conversations/${conv.id}/messages?tenantId=${TENANT_ID}`),
        apiFetch<DraftOrder[]>(`/v3/ai/conversations/${conv.id}/drafts?tenantId=${TENANT_ID}`),
      ]);
      setMessages(msgs);
      setDrafts(drs);
    } catch (e) { setError(String(e)); }
    finally { setDetailLoading(false); }
  };

  const resolveEscalation = async (close: boolean) => {
    if (!selected) return;
    try {
      await apiFetch(`/v3/ai/conversations/${selected.id}/resolve?tenantId=${TENANT_ID}&close=${close}`, { method: 'POST' });
      setSuccess(t('aiWhatsapp.resolved'));
      setSelected(null);
      loadConversations();
    } catch (e) { setError(String(e)); }
  };

  const tabs: { key: typeof tab; label: string }[] = [
    { key: 'all', label: t('aiWhatsapp.allConversations') },
    { key: 'escalated', label: t('aiWhatsapp.escalated') },
    { key: 'flows', label: t('aiWhatsapp.flows') },
  ];

  return (
    <div className="space-y-6">
      <SectionTitle eyebrow="AI Tools" title={t('aiWhatsapp.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* tabs */}
      <div className="flex gap-1">
        {tabs.map(tb => (
          <button key={tb.key} onClick={() => { setTab(tb.key); setSelected(null); }}
            className={`px-4 py-2 rounded-t-xl font-medium transition ${tab === tb.key ? 'bg-white border-t border-x border-gray-200 text-blue-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
            {tb.label}
          </button>
        ))}
      </div>

      {tab === 'flows' ? (
        <FlowCards />
      ) : loading ? <Spinner /> : (
        /* ── Split-pane: Conversation List + Chat Detail ── */
        <div className="grid gap-6 xl:grid-cols-[0.85fr_1.35fr]">
          {/* LEFT: Conversation List */}
          <Card title={t('aiWhatsapp.conversations')} className="max-h-[70vh] overflow-y-auto">
            <div className="p-3 space-y-3">
              {conversations.length === 0 && (
                <p className="text-center text-sm text-gray-400 py-8">{t('aiWhatsapp.noConversations')}</p>
              )}
              {conversations.map((c) => (
                <button
                  key={c.id}
                  onClick={() => openConversation(c)}
                  className={`w-full text-left rounded-2xl border p-3 transition hover:shadow-sm ${
                    selected?.id === c.id
                      ? 'border-blue-300 bg-blue-50'
                      : 'border-gray-200 bg-white hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-semibold text-gray-900">{c.customerPhone}</span>
                    <span className="text-xs text-gray-400">
                      {c.createdAt ? new Date(c.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                    </span>
                  </div>
                  <div className="flex items-center justify-between mt-1">
                    <span className="text-xs text-gray-500 truncate">
                      {c.messageCount ? `${c.messageCount} messages` : 'No messages'}
                    </span>
                    <Badge status={c.status} />
                  </div>
                </button>
              ))}
            </div>
          </Card>

          {/* RIGHT: Chat Detail */}
          {selected ? (
            <Card title={`${t('aiWhatsapp.messageThread')} — ${selected.customerPhone}`}
                  action={<Badge status={selected.status} />}>
              {detailLoading ? (
                <div className="py-16"><Spinner /></div>
              ) : (
                <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
                  {/* Chat bubbles */}
                  <div className="rounded-2xl border border-gray-200 bg-gray-50 p-4 max-h-[55vh] overflow-y-auto space-y-3">
                    {messages.length === 0 && (
                      <p className="text-center text-sm text-gray-400 py-8">{t('aiWhatsapp.noConversations')}</p>
                    )}
                    {messages.map((m) => {
                      const isBot = m.sender === 'BOT';
                      return (
                        <div key={m.id} className={`flex ${isBot ? 'justify-start' : 'justify-end'}`}>
                          <div className="flex items-end gap-2 max-w-[85%]">
                            {isBot && <Bot className="h-5 w-5 text-gray-400 shrink-0 mb-1" />}
                            <div className={`rounded-2xl px-4 py-3 text-sm shadow-sm ${
                              isBot ? 'bg-white text-gray-800' : 'bg-blue-600 text-white'
                            }`}>
                              <p>{m.text}</p>
                              <p className={`text-[10px] mt-1 ${isBot ? 'text-gray-400' : 'text-blue-200'}`}>
                                {new Date(m.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                              </p>
                            </div>
                            {!isBot && <User className="h-5 w-5 text-gray-400 shrink-0 mb-1" />}
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Draft orders + Escalation panel */}
                  <div className="space-y-4">
                    {/* Draft Order */}
                    {drafts.length > 0 && (
                      <div className="rounded-2xl border border-gray-200 p-4">
                        <h4 className="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
                          <MessageCircle className="h-4 w-4 text-blue-600" />
                          {t('aiWhatsapp.draftOrders')}
                        </h4>
                        <div className="space-y-2">
                          {drafts.map((d) => (
                            <div key={d.id} className="flex items-center justify-between rounded-xl bg-gray-50 px-3 py-2 text-sm">
                              <span className="font-medium text-gray-700">{d.productName}</span>
                              <span className="font-semibold text-gray-900">×{d.quantity}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Escalation box */}
                    {selected.escalated && (
                      <div className="rounded-2xl border border-red-200 bg-red-50 p-4">
                        <div className="flex items-center gap-2 mb-2">
                          <Shield className="h-4 w-4 text-red-600" />
                          <span className="text-sm font-semibold text-red-800">{t('aiWhatsapp.escalationPending')}</span>
                        </div>
                        <p className="text-sm text-red-700 mb-3">
                          {t('aiWhatsapp.escalationDescription')}
                        </p>
                        <div className="flex gap-2">
                          <Button variant="secondary" size="sm" onClick={() => resolveEscalation(false)}>
                            <Shield className="h-3.5 w-3.5" /> {t('aiWhatsapp.resolve')}
                          </Button>
                          <Button variant="danger" size="sm" onClick={() => resolveEscalation(true)}>
                            {t('aiWhatsapp.resolveAndClose')}
                          </Button>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </Card>
          ) : (
            <Card className="flex items-center justify-center min-h-[400px]">
              <div className="text-center text-gray-400">
                <MessageCircle className="h-12 w-12 mx-auto mb-3 opacity-30" />
                <p className="text-sm">{t('aiWhatsapp.selectConversation')}</p>
              </div>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
