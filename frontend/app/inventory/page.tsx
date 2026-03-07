'use client';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Modal, Spinner, Alert, Badge, Field } from '@/components/ui';
import { useT } from '@/lib/i18n';

const SITE_ID = 'MAIN';

// ─── types ────────────────────────────────────────────────────────────────────

interface Item {
  itemId: string;
  name: string;
  type: string;
  baseUom: string;
  description: string | null;
  minStockThreshold: number;
  active: boolean;
}

interface StockPosition {
  id: string;
  tenantId: string;
  siteId: string;
  itemId: string;
  lotId: string;
  locationId: string;
  onHandQty: number;
  uom: string;
  avgUnitCost: number;
  valuationAmount: number;
}

const ITEM_TYPES = ['INGREDIENT', 'PACKAGING', 'FG', 'BYPRODUCT', 'WIP'];

const TYPE_BADGE: Record<string, string> = {
  INGREDIENT: 'bg-blue-100 text-blue-700',
  PACKAGING:  'bg-purple-100 text-purple-700',
  FG:         'bg-green-100 text-green-700',
  BYPRODUCT:  'bg-yellow-100 text-yellow-700',
  WIP:        'bg-orange-100 text-orange-700',
};

function typeBadge(t: string) {
  const cls = TYPE_BADGE[t] ?? 'bg-gray-100 text-gray-600';
  return (
    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${cls}`}>
      {t}
    </span>
  );
}

function fmt(n: number | null | undefined, decimals = 2) {
  return (n ?? 0).toLocaleString(undefined, {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

// ─── main component ───────────────────────────────────────────────────────────

export default function InventoryPage() {
  const t = useT();
  const [tab, setTab] = useState<'stock' | 'items'>('stock');

  const [positions, setPositions] = useState<StockPosition[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // filters
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [alertsOnly, setAlertsOnly] = useState(false);

  // receive stock modal
  const [receiveOpen, setReceiveOpen] = useState(false);
  const [receiveSaving, setReceiveSaving] = useState(false);
  const [receiveForm, setReceiveForm] = useState({
    itemId: '',
    qty: '',
    unitCostBase: '',
    supplierRef: '',
  });

  // transfer modal
  const [transferOpen, setTransferOpen] = useState(false);
  const [transferSaving, setTransferSaving] = useState(false);
  const [transferForm, setTransferForm] = useState({
    itemId: '',
    lotId: '',
    qty: '',
    fromLocationId: '',
    toLocationId: '',
  });

  // item modal (create / edit)
  const [itemOpen, setItemOpen] = useState(false);
  const [itemSaving, setItemSaving] = useState(false);
  const [editItemId, setEditItemId] = useState<string | null>(null);
  const [itemForm, setItemForm] = useState({
    name: '',
    type: 'INGREDIENT',
    baseUom: 'KG',
    description: '',
    minStockThreshold: '0',
  });

  // ─── load ─────────────────────────────────────────────────────────────────

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [pos, itms] = await Promise.all([
        apiFetch<StockPosition[]>(`/v1/inventory/positions?tenantId=${TENANT_ID}`),
        apiFetch<Item[]>(`/v1/items?tenantId=${TENANT_ID}`),
      ]);
      setPositions(pos);
      setItems(itms);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // ─── item name lookup ─────────────────────────────────────────────────────

  const itemMap = useMemo(() => {
    const m: Record<string, Item> = {};
    items.forEach((i) => { m[i.itemId] = i; });
    return m;
  }, [items]);

  // ─── filtered positions ───────────────────────────────────────────────────

  const filteredPositions = useMemo(() => {
    return positions.filter((p) => {
      const item = itemMap[p.itemId];
      if (typeFilter !== 'ALL' && item?.type !== typeFilter) return false;
      if (search) {
        const name = item?.name ?? p.itemId;
        if (!name.toLowerCase().includes(search.toLowerCase())) return false;
      }
      if (alertsOnly) {
        const threshold = item?.minStockThreshold ?? 0;
        if (p.onHandQty >= threshold) return false;
      }
      return true;
    });
  }, [positions, itemMap, typeFilter, search, alertsOnly]);

  // ─── filtered items ───────────────────────────────────────────────────────

  const filteredItems = useMemo(() => {
    return items.filter((i) => {
      if (typeFilter !== 'ALL' && i.type !== typeFilter) return false;
      if (search && !i.name.toLowerCase().includes(search.toLowerCase())) return false;
      return true;
    });
  }, [items, typeFilter, search]);

  // ─── alert count ─────────────────────────────────────────────────────────

  const alertCount = useMemo(() => {
    return positions.filter((p) => {
      const threshold = itemMap[p.itemId]?.minStockThreshold ?? 0;
      return threshold > 0 && p.onHandQty < threshold;
    }).length;
  }, [positions, itemMap]);

  // ─── receive stock ────────────────────────────────────────────────────────

  const openReceive = () => {
    setReceiveForm({
      itemId: items.length > 0 ? items[0].itemId : '',
      qty: '',
      unitCostBase: '',
      supplierRef: '',
    });
    setReceiveOpen(true);
  };

  const submitReceive = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setReceiveSaving(true);
      const item = itemMap[receiveForm.itemId];
      const now = new Date().toISOString();
      await apiFetch('/v1/inventory/receipts', {
        method: 'POST',
        body: JSON.stringify({
          tenantId: TENANT_ID,
          siteId: SITE_ID,
          receiptId: crypto.randomUUID(),
          itemId: receiveForm.itemId,
          lotId: crypto.randomUUID(),
          qty: parseFloat(receiveForm.qty),
          uom: item?.baseUom ?? 'PCS',
          unitCostBase: parseFloat(receiveForm.unitCostBase),
          occurredAtUtc: now,
          idempotencyKey: crypto.randomUUID(),
        }),
      });
      setReceiveOpen(false);
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setReceiveSaving(false);
    }
  };

  // ─── transfer ─────────────────────────────────────────────────────────────

  const openTransfer = (pos?: StockPosition) => {
    setTransferForm({
      itemId: pos?.itemId ?? (items.length > 0 ? items[0].itemId : ''),
      lotId: pos?.lotId ?? '',
      qty: '',
      fromLocationId: pos?.locationId ?? '',
      toLocationId: '',
    });
    setTransferOpen(true);
  };

  const submitTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setTransferSaving(true);
      await apiFetch('/v1/inventory/transfers', {
        method: 'POST',
        body: JSON.stringify({
          tenantId: TENANT_ID,
          siteId: SITE_ID,
          itemId: transferForm.itemId,
          lotId: transferForm.lotId || null,
          qty: parseFloat(transferForm.qty),
          fromLocationId: transferForm.fromLocationId,
          toLocationId: transferForm.toLocationId,
          occurredAtUtc: new Date().toISOString(),
          idempotencyKey: crypto.randomUUID(),
        }),
      });
      setTransferOpen(false);
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setTransferSaving(false);
    }
  };

  // ─── item CRUD ────────────────────────────────────────────────────────────

  const openCreateItem = () => {
    setEditItemId(null);
    setItemForm({ name: '', type: 'INGREDIENT', baseUom: 'KG', description: '', minStockThreshold: '0' });
    setItemOpen(true);
  };

  const openEditItem = (item: Item) => {
    setEditItemId(item.itemId);
    setItemForm({
      name: item.name,
      type: item.type,
      baseUom: item.baseUom,
      description: item.description ?? '',
      minStockThreshold: String(item.minStockThreshold),
    });
    setItemOpen(true);
  };

  const submitItem = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setItemSaving(true);
      const body = {
        name: itemForm.name,
        type: itemForm.type,
        baseUom: itemForm.baseUom,
        description: itemForm.description || null,
        minStockThreshold: parseFloat(itemForm.minStockThreshold) || 0,
      };
      if (editItemId) {
        await apiFetch(`/v1/items/${editItemId}?tenantId=${TENANT_ID}`, {
          method: 'PUT',
          body: JSON.stringify(body),
        });
      } else {
        await apiFetch(`/v1/items?tenantId=${TENANT_ID}`, {
          method: 'POST',
          body: JSON.stringify(body),
        });
      }
      setItemOpen(false);
      load();
    } catch (e) {
      setError(String(e));
    } finally {
      setItemSaving(false);
    }
  };

  // ─── render ───────────────────────────────────────────────────────────────

  return (
    <div className="max-w-6xl">
      {/* header */}
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold">{t('inventory.title')}</h1>
          {alertCount > 0 && (
            <span className="bg-red-100 text-red-700 text-xs font-semibold px-2.5 py-1 rounded-full">
              {t('inventory.alerts', { count: alertCount })}
            </span>
          )}
        </div>
        <div className="flex gap-2">
          {tab === 'stock' && (
            <>
              <button className="btn-secondary" onClick={() => openTransfer()}>
                {t('inventory.transfer')}
              </button>
              <button className="btn-primary" onClick={openReceive}>
                {t('inventory.receiveStock')}
              </button>
            </>
          )}
          {tab === 'items' && (
            <button className="btn-primary" onClick={openCreateItem}>
              {t('inventory.newItem')}
            </button>
          )}
        </div>
      </div>

      {error && <Alert msg={error} onClose={() => setError('')} />}

      {/* tabs */}
      <div className="flex gap-1 mb-4 bg-gray-100 rounded-xl p-1 w-fit">
        {(['stock', 'items'] as const).map((tb) => (
          <button
            key={tb}
            className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${
              tab === tb ? 'bg-white shadow text-gray-900' : 'text-gray-500 hover:text-gray-700'
            }`}
            onClick={() => setTab(tb)}
          >
            {tb === 'stock' ? t('inventory.stockLevels', { count: positions.length }) : t('inventory.items', { count: items.length })}
          </button>
        ))}
      </div>

      {/* filters */}
      <div className="flex flex-wrap gap-3 mb-4">
        <select
          className="input w-44"
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
        >
          <option value="ALL">{t('inventory.allTypes')}</option>
          {ITEM_TYPES.map((tp) => <option key={tp} value={tp}>{tp}</option>)}
        </select>
        <input
          className="input w-52"
          placeholder={t('inventory.searchItem')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        {tab === 'stock' && (
          <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
            <input
              type="checkbox"
              className="w-4 h-4 accent-red-500"
              checked={alertsOnly}
              onChange={(e) => setAlertsOnly(e.target.checked)}
            />
            {t('inventory.belowThresholdOnly')}
          </label>
        )}
      </div>

      {/* content */}
      {loading ? (
        <Spinner />
      ) : tab === 'stock' ? (
        /* ── Stock Levels ─────────────────────────────────────────────── */
        filteredPositions.length === 0 ? (
          <div className="text-center py-16 text-sm text-gray-400 border rounded-xl bg-white">
            {positions.length === 0
              ? t('inventory.noStockOnHand')
              : t('inventory.noPositionsMatch')}
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border bg-white shadow-sm">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  {[t('inventory.cols.item'), t('inventory.cols.type'), t('inventory.cols.location'), t('inventory.cols.lot'), t('inventory.cols.onHand'), t('inventory.cols.avgCost'), t('inventory.cols.totalValue'), ''].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide whitespace-nowrap">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredPositions.map((p) => {
                  const item = itemMap[p.itemId];
                  const threshold = item?.minStockThreshold ?? 0;
                  const isAlert = threshold > 0 && p.onHandQty < threshold;
                  return (
                    <tr key={p.id} className={`hover:bg-gray-50 ${isAlert ? 'bg-red-50' : ''}`}>
                      <td className="px-4 py-3 font-medium">
                        {item?.name ?? p.itemId}
                        {isAlert && <span className="ml-2 text-red-500 text-xs">{t('inventory.low')}</span>}
                      </td>
                      <td className="px-4 py-3">{item ? typeBadge(item.type) : '—'}</td>
                      <td className="px-4 py-3 text-gray-600">{p.locationId}</td>
                      <td className="px-4 py-3 text-gray-400 text-xs font-mono">
                        {p.lotId ? p.lotId.slice(0, 8) + '…' : '—'}
                      </td>
                      <td className="px-4 py-3 font-semibold">
                        {fmt(p.onHandQty, 3)} <span className="text-gray-400 font-normal text-xs">{p.uom}</span>
                      </td>
                      <td className="px-4 py-3 text-gray-600">{fmt(p.avgUnitCost, 4)}</td>
                      <td className="px-4 py-3 font-semibold">{fmt(p.valuationAmount)}</td>
                      <td className="px-4 py-3">
                        <button
                          className="text-xs text-indigo-600 hover:underline"
                          onClick={() => openTransfer(p)}
                        >
                          {t('inventory.transferBtn')}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
              <tfoot className="border-t bg-gray-50">
                <tr>
                  <td colSpan={6} className="px-4 py-2 text-xs text-right text-gray-500 font-medium">
                    {t('inventory.totalValuation')}
                  </td>
                  <td className="px-4 py-2 font-bold text-sm">
                    {fmt(filteredPositions.reduce((s, p) => s + (p.valuationAmount ?? 0), 0))}
                  </td>
                  <td />
                </tr>
              </tfoot>
            </table>
          </div>
        )
      ) : (
        /* ── Items Master Data ────────────────────────────────────────── */
        filteredItems.length === 0 ? (
          <div className="text-center py-16 text-sm text-gray-400 border rounded-xl bg-white">
            {items.length === 0
              ? t('inventory.noItemsDefined')
              : t('inventory.noItemsMatch')}
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border bg-white shadow-sm">
            <table className="min-w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  {[t('inventory.cols.name'), t('inventory.cols.type'), t('inventory.cols.uom'), t('inventory.cols.minStockCol'), t('inventory.cols.statusCol'), t('inventory.cols.descriptionCol'), ''].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide whitespace-nowrap">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredItems.map((item) => (
                  <tr key={item.itemId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium">{item.name}</td>
                    <td className="px-4 py-3">{typeBadge(item.type)}</td>
                    <td className="px-4 py-3 font-mono text-xs">{item.baseUom}</td>
                    <td className="px-4 py-3 text-gray-600">
                      {item.minStockThreshold > 0 ? `${item.minStockThreshold} ${item.baseUom}` : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <Badge status={item.active ? 'ACTIVE' : 'INACTIVE'} />
                    </td>
                    <td className="px-4 py-3 text-gray-400 text-xs max-w-xs truncate">
                      {item.description ?? '—'}
                    </td>
                    <td className="px-4 py-3">
                      <button
                        className="text-xs text-blue-600 hover:underline"
                        onClick={() => openEditItem(item)}
                      >
                        {t('common.edit')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {/* ─── Receive Stock Modal ────────────────────────────────────────────── */}
      {receiveOpen && (
        <Modal title={t('inventory.receiveTitle')} onClose={() => setReceiveOpen(false)}>
          <form onSubmit={submitReceive} className="space-y-4">
            <Field label={t('inventory.cols.item')}>
              <select
                className="input"
                required
                value={receiveForm.itemId}
                onChange={(e) => setReceiveForm((f) => ({ ...f, itemId: e.target.value }))}
              >
                {items.filter((i) => i.active).map((i) => (
                  <option key={i.itemId} value={i.itemId}>
                    {i.name} ({i.baseUom})
                  </option>
                ))}
              </select>
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label={t('inventory.quantityLabel', { uom: itemMap[receiveForm.itemId]?.baseUom ?? '' })}>
                <input
                  className="input"
                  type="number"
                  min={0.001}
                  step="0.001"
                  required
                  placeholder="e.g. 100"
                  value={receiveForm.qty}
                  onChange={(e) => setReceiveForm((f) => ({ ...f, qty: e.target.value }))}
                />
              </Field>
              <Field label={t('inventory.costPerUnit')}>
                <input
                  className="input"
                  type="number"
                  min={0}
                  step="0.0001"
                  required
                  placeholder="e.g. 5.50"
                  value={receiveForm.unitCostBase}
                  onChange={(e) => setReceiveForm((f) => ({ ...f, unitCostBase: e.target.value }))}
                />
              </Field>
            </div>

            <Field label={t('inventory.supplierRef')}>
              <input
                className="input"
                placeholder={t('inventory.supplierRefPlaceholder')}
                value={receiveForm.supplierRef}
                onChange={(e) => setReceiveForm((f) => ({ ...f, supplierRef: e.target.value }))}
              />
            </Field>

            <div className="bg-blue-50 border border-blue-100 rounded-lg px-4 py-2 text-xs text-blue-700">
              {t('inventory.newLotNote', { site: SITE_ID })}
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <button type="button" className="btn-secondary" onClick={() => setReceiveOpen(false)}>
                {t('common.cancel')}
              </button>
              <button type="submit" className="btn-primary" disabled={receiveSaving || items.length === 0}>
                {receiveSaving ? t('inventory.recording') : t('inventory.recordReceipt')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ─── Transfer Modal ─────────────────────────────────────────────────── */}
      {transferOpen && (
        <Modal title={t('inventory.transferTitle')} onClose={() => setTransferOpen(false)}>
          <form onSubmit={submitTransfer} className="space-y-4">
            <Field label={t('inventory.cols.item')}>
              <select
                className="input"
                required
                value={transferForm.itemId}
                onChange={(e) => setTransferForm((f) => ({ ...f, itemId: e.target.value }))}
              >
                {items.filter((i) => i.active).map((i) => (
                  <option key={i.itemId} value={i.itemId}>{i.name}</option>
                ))}
              </select>
            </Field>

            <Field label={t('inventory.lotIdLabel')}>
              <input
                className="input"
                placeholder={t('inventory.lotIdPlaceholder')}
                value={transferForm.lotId}
                onChange={(e) => setTransferForm((f) => ({ ...f, lotId: e.target.value }))}
              />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label={t('inventory.fromLocation')}>
                <input
                  className="input"
                  required
                  placeholder="e.g. RECEIVING"
                  value={transferForm.fromLocationId}
                  onChange={(e) => setTransferForm((f) => ({ ...f, fromLocationId: e.target.value }))}
                />
              </Field>
              <Field label={t('inventory.toLocation')}>
                <input
                  className="input"
                  required
                  placeholder="e.g. PRODUCTION"
                  value={transferForm.toLocationId}
                  onChange={(e) => setTransferForm((f) => ({ ...f, toLocationId: e.target.value }))}
                />
              </Field>
            </div>

            <Field label={t('inventory.quantityLabel', { uom: itemMap[transferForm.itemId]?.baseUom ?? '' })}>
              <input
                className="input"
                type="number"
                min={0.001}
                step="0.001"
                required
                value={transferForm.qty}
                onChange={(e) => setTransferForm((f) => ({ ...f, qty: e.target.value }))}
              />
            </Field>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <button type="button" className="btn-secondary" onClick={() => setTransferOpen(false)}>
                {t('common.cancel')}
              </button>
              <button type="submit" className="btn-primary" disabled={transferSaving}>
                {transferSaving ? t('inventory.transferring') : t('inventory.transferBtn')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ─── Item Create / Edit Modal ───────────────────────────────────────── */}
      {itemOpen && (
        <Modal
          title={editItemId ? t('inventory.editItemTitle') : t('inventory.createItemTitle')}
          onClose={() => setItemOpen(false)}
        >
          <form onSubmit={submitItem} className="space-y-4">
            <Field label={t('common.name')}>
              <input
                className="input"
                required
                placeholder="e.g. Wheat Flour"
                value={itemForm.name}
                onChange={(e) => setItemForm((f) => ({ ...f, name: e.target.value }))}
              />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label={t('inventory.itemType')}>
                <select
                  className="input"
                  value={itemForm.type}
                  onChange={(e) => setItemForm((f) => ({ ...f, type: e.target.value }))}
                >
                  {ITEM_TYPES.map((tp) => <option key={tp} value={tp}>{tp}</option>)}
                </select>
              </Field>
              <Field label={t('inventory.cols.uom')}>
                <input
                  className="input"
                  required
                  placeholder="KG / PCS / L"
                  value={itemForm.baseUom}
                  onChange={(e) => setItemForm((f) => ({ ...f, baseUom: e.target.value.toUpperCase() }))}
                />
              </Field>
            </div>

            <Field label={t('inventory.minStock')}>
              <input
                className="input"
                type="number"
                min={0}
                step="0.001"
                value={itemForm.minStockThreshold}
                onChange={(e) => setItemForm((f) => ({ ...f, minStockThreshold: e.target.value }))}
              />
            </Field>

            <Field label={`${t('common.description')} (${t('common.optional')})`}>
              <textarea
                className="input h-16 resize-none"
                placeholder={t('common.description')}
                value={itemForm.description}
                onChange={(e) => setItemForm((f) => ({ ...f, description: e.target.value }))}
              />
            </Field>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <button type="button" className="btn-secondary" onClick={() => setItemOpen(false)}>
                {t('common.cancel')}
              </button>
              <button type="submit" className="btn-primary" disabled={itemSaving}>
                {itemSaving ? t('common.saving') : editItemId ? t('common.save') : t('common.create')}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
