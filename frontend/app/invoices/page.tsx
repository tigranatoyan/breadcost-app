'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Modal, Table, Spinner, Alert, Badge, Field, Success, useConfirm } from '@/components/ui';
import { SectionTitle, Button } from '@/components/design-system';

/* ── types ─────────────────────────────────────────────── */
interface Invoice {
  invoiceId: string;
  tenantId: string;
  customerId: string;
  customerName?: string;
  status: string;
  totalAmount: number;
  currency?: string;
  dueDate?: string;
  paidAmount?: number;
  issuedAt?: string;
  notes?: string;
}
interface InvoiceLine {
  id: string;
  description: string;
  qty: number;
  unitPrice: number;
  total: number;
}
interface DiscountRule {
  ruleId: string;
  customerId: string;
  productId?: string;
  categoryId?: string;
  discountPercent: number;
  minQty?: number;
  validFrom?: string;
  validTo?: string;
}
interface CreditInfo {
  customerId: string;
  creditLimit: number;
  currentBalance: number;
  available: number;
  currency?: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function InvoicesPage() {
  const t = useT();
  const [askConfirm, confirmModal] = useConfirm({ confirmLabel: t('common.confirm'), cancelLabel: t('common.cancel') });

  const [tab, setTab] = useState<'invoices' | 'discounts'>('invoices');

  /* invoices */
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  /* invoice detail */
  const [detailInv, setDetailInv] = useState<Invoice | null>(null);
  const [lines, setLines] = useState<InvoiceLine[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);

  /* credit check */
  const [creditModal, setCreditModal] = useState<CreditInfo | null>(null);

  /* record payment */
  const [payInv, setPayInv] = useState<Invoice | null>(null);
  const [payAmount, setPayAmount] = useState('');
  const [paySaving, setPaySaving] = useState(false);

  /* credit limit */
  const [limitModal, setLimitModal] = useState<string | null>(null);
  const [limitVal, setLimitVal] = useState('');

  /* dispute / resolve (A1.1) */
  const [disputeInv, setDisputeInv] = useState<Invoice | null>(null);
  const [disputeReason, setDisputeReason] = useState('');
  const [disputeSaving, setDisputeSaving] = useState(false);
  const [resolveInv, setResolveInv] = useState<Invoice | null>(null);
  const [resolveNotes, setResolveNotes] = useState('');
  const [resolveSaving, setResolveSaving] = useState(false);

  /* discount rules */
  const [discountCustomerId, setDiscountCustomerId] = useState('');
  const [discountRules, setDiscountRules] = useState<DiscountRule[]>([]);
  const [discRulesLoading, setDiscRulesLoading] = useState(false);
  const [showAddDisc, setShowAddDisc] = useState(false);
  const [discForm, setDiscForm] = useState({ productId: '', categoryId: '', discountPercent: '', minQty: '', validFrom: '', validTo: '' });
  const [discSaving, setDiscSaving] = useState(false);

  /* lookup data for dropdowns */
  const [customers, setCustomers] = useState<{ customerId: string; name: string }[]>([]);
  const [products, setProducts] = useState<{ productId: string; name: string }[]>([]);

  /* loaders */
  const loadInvoices = useCallback(async () => {
    try {
      setLoading(true);
      const q = statusFilter ? `&status=${statusFilter}` : '';
      const data = await apiFetch<Invoice[]>(`/v2/invoices?tenantId=${TENANT_ID}${q}`);
      setInvoices(data);
    } catch (e) { setError(String(e)); } finally { setLoading(false); }
  }, [statusFilter]);

  const loadDropdowns = useCallback(async () => {
    try {
      const [c, p] = await Promise.all([
        apiFetch<{ customerId: string; name: string }[]>(`/v2/customers?tenantId=${TENANT_ID}`),
        apiFetch<{ productId: string; name: string }[]>(`/v1/products?tenantId=${TENANT_ID}`),
      ]);
      setCustomers(c);
      setProducts(p);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { loadInvoices(); loadDropdowns(); }, [loadInvoices, loadDropdowns]);

  const openDetail = async (inv: Invoice) => {
    setDetailInv(inv);
    setDetailLoading(true);
    try {
      const data = await apiFetch<{ invoice: Invoice; lines: InvoiceLine[] }>(`/v2/invoices/${inv.invoiceId}?tenantId=${TENANT_ID}`);
      setDetailInv(data.invoice || inv);
      setLines(data.lines || []);
    } catch {
      setLines([]);
    } finally { setDetailLoading(false); }
  };

  const recordPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!payInv) return;
    try {
      setPaySaving(true);
      await apiFetch(`/v2/invoices/${payInv.invoiceId}/payments?tenantId=${TENANT_ID}`, { method: 'POST', body: JSON.stringify({ amount: Number(payAmount) }) });
      setSuccess(t('invoices.paymentRecorded'));
      setPayInv(null);
      setPayAmount('');
      loadInvoices();
    } catch (e) { setError(String(e)); } finally { setPaySaving(false); }
  };

  const creditCheck = async (custId: string) => {
    try {
      const data = await apiFetch<CreditInfo>(`/v2/customers/${custId}/credit-check?tenantId=${TENANT_ID}`);
      setCreditModal(data);
    } catch (e) { setError(String(e)); }
  };

  const setCreditLimit = async () => {
    if (!limitModal) return;
    try {
      await apiFetch(`/v2/customers/${limitModal}/credit-limit?tenantId=${TENANT_ID}`, { method: 'PUT', body: JSON.stringify({ creditLimit: Number(limitVal) }) });
      setSuccess(t('invoices.creditLimitSet'));
      setLimitModal(null);
    } catch (e) { setError(String(e)); }
  };

  const voidInvoice = async (id: string) => {
    if (!await askConfirm(t('invoices.confirmVoid'))) return;
    try {
      await apiFetch(`/v2/invoices/${id}/void?tenantId=${TENANT_ID}`, { method: 'PUT' });
      setSuccess(t('invoices.voided'));
      loadInvoices();
    } catch (e) { setError(String(e)); }
  };

  /* A1.1 — dispute workflow */
  const disputeInvoice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!disputeInv) return;
    try {
      setDisputeSaving(true);
      await apiFetch(`/v2/invoices/${disputeInv.invoiceId}/dispute?tenantId=${TENANT_ID}`, { method: 'PUT', body: JSON.stringify({ reason: disputeReason }) });
      setSuccess(t('invoices.disputed'));
      setDisputeInv(null);
      setDisputeReason('');
      loadInvoices();
    } catch (e) { setError(String(e)); } finally { setDisputeSaving(false); }
  };

  const resolveInvoice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resolveInv) return;
    try {
      setResolveSaving(true);
      await apiFetch(`/v2/invoices/${resolveInv.invoiceId}/resolve?tenantId=${TENANT_ID}`, { method: 'PUT', body: JSON.stringify({ notes: resolveNotes }) });
      setSuccess(t('invoices.resolved'));
      setResolveInv(null);
      setResolveNotes('');
      loadInvoices();
    } catch (e) { setError(String(e)); } finally { setResolveSaving(false); }
  };

  /* discount rules */
  const loadDiscountRules = async () => {
    if (!discountCustomerId) return;
    try {
      setDiscRulesLoading(true);
      const data = await apiFetch<DiscountRule[]>(`/v2/customers/${discountCustomerId}/discount-rules?tenantId=${TENANT_ID}`);
      setDiscountRules(data);
    } catch (e) { setError(String(e)); } finally { setDiscRulesLoading(false); }
  };

  const addDiscountRule = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setDiscSaving(true);
      await apiFetch(`/v2/customers/${discountCustomerId}/discount-rules?tenantId=${TENANT_ID}`, {
        method: 'POST',
        body: JSON.stringify({
          ...discForm,
          discountPercent: Number(discForm.discountPercent),
          minQty: discForm.minQty ? Number(discForm.minQty) : undefined,
          productId: discForm.productId || undefined,
          categoryId: discForm.categoryId || undefined,
          validFrom: discForm.validFrom || undefined,
          validTo: discForm.validTo || undefined,
        }),
      });
      setSuccess(t('invoices.discountAdded'));
      setShowAddDisc(false);
      setDiscForm({ productId: '', categoryId: '', discountPercent: '', minQty: '', validFrom: '', validTo: '' });
      loadDiscountRules();
    } catch (e) { setError(String(e)); } finally { setDiscSaving(false); }
  };

  return (
    <div className="max-w-[1800px]">
      <SectionTitle eyebrow={t('invoices.eyebrow')} title={t('invoices.title')} />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* tabs */}
      <div className="flex gap-2 mb-6 border-b border-gray-200">
        {(['invoices', 'discounts'] as const).map(k => (
          <button key={k} onClick={() => setTab(k)}
            className={`px-4 py-2 text-sm font-medium -mb-px ${tab === k ? 'border-b-2 border-blue-600 text-blue-600' : 'text-gray-500 hover:text-gray-700'}`}>
            {t(`invoices.tab_${k}`)}
          </button>
        ))}
      </div>

      {/* ───────── INVOICES TAB ───────── */}
      {tab === 'invoices' && (
        <>
          <div className="flex gap-3 mb-4 items-center">
            <label className="text-sm font-medium">{t('common.status')}:</label>
            <select className="input w-40" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
              <option value="">{t('common.all')}</option>
              <option value="ISSUED">ISSUED</option>
              <option value="PARTIALLY_PAID">PARTIALLY_PAID</option>
              <option value="PAID">PAID</option>
              <option value="OVERDUE">OVERDUE</option>
              <option value="DISPUTED">DISPUTED</option>
              <option value="VOIDED">VOIDED</option>
            </select>
          </div>
          {loading ? <Spinner /> : (
            <Table
              cols={[t('invoices.id'), t('invoices.customer'), t('common.status'), t('invoices.total'), t('invoices.paid'), t('invoices.due'), t('common.actions')]}
              rows={invoices.map(inv => [
                <button key={inv.invoiceId} className="text-blue-600 underline text-sm" onClick={() => openDetail(inv)}>{inv.invoiceId.slice(0, 8)}</button>,
                inv.customerName || inv.customerId.slice(0, 8),
                <Badge key={`s-${inv.invoiceId}`} status={inv.status} />,
                `${inv.totalAmount.toFixed(2)} ${inv.currency || ''}`,
                inv.paidAmount !== null && inv.paidAmount !== undefined ? inv.paidAmount.toFixed(2) : '0.00',
                inv.dueDate || '—',
                <div key={`a-${inv.invoiceId}`} className="flex gap-1 flex-wrap">
                  {inv.status !== 'PAID' && inv.status !== 'VOIDED' && (
                    <Button variant="primary" size="xs" className="bg-green-600 hover:bg-green-700" onClick={() => { setPayInv(inv); setPayAmount(''); }}>{t('invoices.pay')}</Button>
                  )}
                  <Button variant="secondary" size="xs" onClick={() => creditCheck(inv.customerId)}>{t('invoices.credit')}</Button>
                  <Button variant="secondary" size="xs" onClick={() => { setLimitModal(inv.customerId); setLimitVal(''); }}>{t('invoices.setLimit')}</Button>
                  {(inv.status === 'ISSUED' || inv.status === 'PARTIALLY_PAID' || inv.status === 'OVERDUE') && (
                    <Button variant="secondary" size="xs" className="bg-amber-500 hover:bg-amber-600 text-white border-amber-500" onClick={() => { setDisputeInv(inv); setDisputeReason(''); }}>{t('invoices.dispute')}</Button>
                  )}
                  {inv.status === 'DISPUTED' && (
                    <Button variant="primary" size="xs" onClick={() => { setResolveInv(inv); setResolveNotes(''); }}>{t('invoices.resolve')}</Button>
                  )}
                  {inv.status !== 'VOIDED' && <Button variant="danger" size="xs" onClick={() => voidInvoice(inv.invoiceId)}>{t('invoices.void')}</Button>}
                </div>,
              ])}
              empty={t('invoices.empty')}
            />
          )}
        </>
      )}

      {/* ───────── DISCOUNTS TAB ───────── */}
      {tab === 'discounts' && (
        <>
          <div className="flex gap-2 mb-4 items-end">
            <Field label={t('invoices.customerId')}>
              <select className="input" value={discountCustomerId} onChange={e => setDiscountCustomerId(e.target.value)}>
                <option value="">— {t('common.select')} —</option>
                {customers.map(c => <option key={c.customerId} value={c.customerId}>{c.name}</option>)}
              </select>
            </Field>
            <Button variant="primary" size="sm" onClick={loadDiscountRules}>{t('common.load')}</Button>
            {discountCustomerId && <Button variant="secondary" size="sm" onClick={() => setShowAddDisc(true)}>+ {t('invoices.addDiscount')}</Button>}
          </div>
          {discRulesLoading ? <Spinner /> : (
            <Table
              cols={[t('invoices.product'), t('invoices.category'), t('invoices.discountPct'), t('invoices.minQty'), t('invoices.validFrom'), t('invoices.validTo')]}
              rows={discountRules.map(r => [r.productId || '—', r.categoryId || '—', `${r.discountPercent}%`, r.minQty ?? '—', r.validFrom || '—', r.validTo || '—'])}
              empty={t('invoices.noDiscounts')}
            />
          )}
        </>
      )}

      {/* detail modal */}
      {detailInv && (
        <Modal title={`${t('invoices.detail')} — ${detailInv.invoiceId.slice(0, 8)}`} onClose={() => setDetailInv(null)} wide>
          {detailLoading ? <Spinner /> : (
            <>
              <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
                <div><span className="font-medium">{t('invoices.customer')}:</span> {detailInv.customerName || detailInv.customerId}</div>
                <div><span className="font-medium">{t('common.status')}:</span> <Badge status={detailInv.status} /></div>
                <div><span className="font-medium">{t('invoices.total')}:</span> {detailInv.totalAmount.toFixed(2)} {detailInv.currency}</div>
                <div><span className="font-medium">{t('invoices.due')}:</span> {detailInv.dueDate || '—'}</div>
              </div>
              <Table
                cols={[t('invoices.description'), t('invoices.qty'), t('invoices.unitPrice'), t('invoices.lineTotal')]}
                rows={lines.map(l => [l.description, l.qty, l.unitPrice.toFixed(2), l.total.toFixed(2)])}
                empty={t('invoices.noLines')}
              />
            </>
          )}
        </Modal>
      )}

      {/* payment modal */}
      {payInv && (
        <Modal title={`${t('invoices.recordPayment')} — ${payInv.invoiceId.slice(0, 8)}`} onClose={() => setPayInv(null)}>
          <form onSubmit={recordPayment} className="space-y-4">
            <p className="text-sm">{t('invoices.outstanding')}: <strong>{(payInv.totalAmount - (payInv.paidAmount || 0)).toFixed(2)} {payInv.currency}</strong></p>
            <Field label={t('invoices.paymentAmount')}><input className="input w-full" type="number" step="0.01" required value={payAmount} onChange={e => setPayAmount(e.target.value)} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setPayInv(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={paySaving}>{paySaving ? t('common.saving') : t('invoices.pay')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* credit check modal */}
      {creditModal && (
        <Modal title={t('invoices.creditCheck')} onClose={() => setCreditModal(null)}>
          <div className="space-y-2 text-sm">
            <div><strong>{t('invoices.creditLimit')}:</strong> {creditModal.creditLimit.toFixed(2)}</div>
            <div><strong>{t('invoices.currentBalance')}:</strong> {creditModal.currentBalance.toFixed(2)}</div>
            <div><strong>{t('invoices.available')}:</strong> {creditModal.available.toFixed(2)}</div>
          </div>
        </Modal>
      )}

      {/* set credit limit modal */}
      {limitModal && (
        <Modal title={t('invoices.setCreditLimit')} onClose={() => setLimitModal(null)}>
          <div className="space-y-4">
            <Field label={t('invoices.newLimit')}><input className="input w-full" type="number" step="0.01" value={limitVal} onChange={e => setLimitVal(e.target.value)} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setLimitModal(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" onClick={setCreditLimit}>{t('common.save')}</Button>
            </div>
          </div>
        </Modal>
      )}

      {/* dispute modal (A1.1) */}
      {disputeInv && (
        <Modal title={`${t('invoices.disputeTitle')} — ${disputeInv.invoiceId.slice(0, 8)}`} onClose={() => setDisputeInv(null)}>
          <form onSubmit={disputeInvoice} className="space-y-4">
            <p className="text-sm text-gray-600">{t('invoices.disputeHint')}</p>
            <Field label={t('invoices.disputeReason')}><textarea className="input w-full" rows={3} required value={disputeReason} onChange={e => setDisputeReason(e.target.value)} placeholder={t('invoices.disputeReasonPlaceholder')} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setDisputeInv(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={disputeSaving} className="bg-amber-500 hover:bg-amber-600 border-amber-500">{disputeSaving ? t('common.saving') : t('invoices.dispute')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* resolve modal (A1.1) */}
      {resolveInv && (
        <Modal title={`${t('invoices.resolveTitle')} — ${resolveInv.invoiceId.slice(0, 8)}`} onClose={() => setResolveInv(null)}>
          <form onSubmit={resolveInvoice} className="space-y-4">
            <p className="text-sm text-gray-600">{t('invoices.resolveHint')}</p>
            <Field label={t('invoices.resolveNotes')}><textarea className="input w-full" rows={3} required value={resolveNotes} onChange={e => setResolveNotes(e.target.value)} placeholder={t('invoices.resolveNotesPlaceholder')} /></Field>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setResolveInv(null)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={resolveSaving}>{resolveSaving ? t('common.saving') : t('invoices.resolve')}</Button>
            </div>
          </form>
        </Modal>
      )}

      {/* add discount rule modal */}
      {showAddDisc && (
        <Modal title={t('invoices.addDiscount')} onClose={() => setShowAddDisc(false)}>
          <form onSubmit={addDiscountRule} className="space-y-4">
            <Field label={t('invoices.product')}>
              <select className="input w-full" value={discForm.productId} onChange={e => setDiscForm({ ...discForm, productId: e.target.value })}>
                <option value="">— {t('common.select')} —</option>
                {products.map(p => <option key={p.productId} value={p.productId}>{p.name}</option>)}
              </select>
            </Field>
            <Field label={t('invoices.category')}><input className="input w-full" value={discForm.categoryId} onChange={e => setDiscForm({ ...discForm, categoryId: e.target.value })} /></Field>
            <Field label={t('invoices.discountPct')}><input className="input w-full" type="number" step="0.1" required value={discForm.discountPercent} onChange={e => setDiscForm({ ...discForm, discountPercent: e.target.value })} /></Field>
            <Field label={t('invoices.minQty')}><input className="input w-full" type="number" value={discForm.minQty} onChange={e => setDiscForm({ ...discForm, minQty: e.target.value })} /></Field>
            <div className="grid grid-cols-2 gap-4">
              <Field label={t('invoices.validFrom')}><input className="input w-full" type="date" value={discForm.validFrom} onChange={e => setDiscForm({ ...discForm, validFrom: e.target.value })} /></Field>
              <Field label={t('invoices.validTo')}><input className="input w-full" type="date" value={discForm.validTo} onChange={e => setDiscForm({ ...discForm, validTo: e.target.value })} /></Field>
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="secondary" size="sm" onClick={() => setShowAddDisc(false)}>{t('common.cancel')}</Button>
              <Button variant="primary" size="sm" type="submit" disabled={discSaving}>{discSaving ? t('common.saving') : t('common.save')}</Button>
            </div>
          </form>
        </Modal>
      )}
      {confirmModal}
    </div>
  );
}
