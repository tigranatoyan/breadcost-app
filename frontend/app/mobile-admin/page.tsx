'use client';
import { useState, useEffect, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { Table, Spinner, Alert, Modal, Field, Success } from '@/components/ui';

/* ── types ─────────────────────────────────────────────── */
interface DeviceRegistration {
  id: string;
  customerId: string;
  deviceToken: string;
  platform: string;
  deviceName: string;
  registeredAt: string;
}
interface PushNotification {
  id: string;
  customerId: string;
  title: string;
  body: string;
  notificationType: string;
  referenceId?: string;
  sentAt: string;
}

/* ── page ──────────────────────────────────────────────── */
export default function MobileAdminPage() {
  const t = useT();

  const [tab, setTab] = useState<'devices' | 'notifications'>('devices');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  /* devices */
  const [devices, setDevices] = useState<DeviceRegistration[]>([]);
  const [devicesLoading, setDevicesLoading] = useState(false);
  const [custFilter, setCustFilter] = useState('');

  /* notifications */
  const [notifs, setNotifs] = useState<PushNotification[]>([]);
  const [notifsLoading, setNotifsLoading] = useState(false);
  const [notifCust, setNotifCust] = useState('');

  /* send modal */
  const [sendModal, setSendModal] = useState(false);
  const [nf, setNf] = useState({ customerId: '', title: '', body: '', notificationType: 'GENERAL', referenceId: '' });

  /* ── loaders ─────────────────────────────────────────── */
  const loadDevices = useCallback(async () => {
    setDevicesLoading(true);
    try {
      const q = custFilter.trim() ? `&customerId=${custFilter.trim()}` : '';
      setDevices(await apiFetch<DeviceRegistration[]>(`/v3/mobile/devices?tenantId=${TENANT_ID}${q}`));
    } catch (e) { setError(String(e)); }
    finally { setDevicesLoading(false); }
  }, [custFilter]);

  const removeDevice = async (id: string) => {
    try {
      await apiFetch(`/v3/mobile/devices/${id}?tenantId=${TENANT_ID}`, { method: 'DELETE' });
      setSuccess(t('mobileAdmin.deviceRemoved'));
      loadDevices();
    } catch (e) { setError(String(e)); }
  };

  const loadNotifs = useCallback(async () => {
    if (!notifCust.trim()) return;
    setNotifsLoading(true);
    try {
      setNotifs(await apiFetch<PushNotification[]>(`/v3/mobile/notifications?tenantId=${TENANT_ID}&customerId=${notifCust.trim()}`));
    } catch (e) { setError(String(e)); }
    finally { setNotifsLoading(false); }
  }, [notifCust]);

  const sendNotification = async () => {
    try {
      await apiFetch('/v3/mobile/notifications', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID, ...nf }),
      });
      setSuccess(t('mobileAdmin.notificationSent'));
      setSendModal(false);
      setNf({ customerId: '', title: '', body: '', notificationType: 'GENERAL', referenceId: '' });
    } catch (e) { setError(String(e)); }
  };

  /* ── effects ─────────────────────────────────────────── */
  useEffect(() => { if (tab === 'devices') loadDevices(); }, [tab, loadDevices]);

  const tabs: { key: typeof tab; label: string }[] = [
    { key: 'devices', label: t('mobileAdmin.devices') },
    { key: 'notifications', label: t('mobileAdmin.notifications') },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">{t('mobileAdmin.title')}</h1>

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* tabs */}
      <div className="flex gap-1">
        {tabs.map(tb => (
          <button key={tb.key} onClick={() => setTab(tb.key)}
            className={`px-4 py-2 rounded-t font-medium ${tab === tb.key ? 'bg-white border-t border-x text-blue-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}>
            {tb.label}
          </button>
        ))}
      </div>

      {/* ── Devices ────────────────────────────────────── */}
      {tab === 'devices' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-end">
            <Field label={t('mobileAdmin.customerId')}><input className="input w-full" value={custFilter} onChange={e => setCustFilter(e.target.value)} /></Field>
            <button onClick={loadDevices} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 h-10">{t('mobileAdmin.search')}</button>
          </div>
          {devicesLoading ? <Spinner /> : (
            <Table
              cols={[t('mobileAdmin.customerId'), t('mobileAdmin.deviceName'), t('mobileAdmin.platform'), t('mobileAdmin.registeredAt'), '']}
              rows={devices.map(d => [
                d.customerId,
                d.deviceName,
                d.platform,
                new Date(d.registeredAt).toLocaleString(),
                <button key={d.id} onClick={() => removeDevice(d.id)} className="text-red-600 hover:underline text-sm">{t('mobileAdmin.remove')}</button>,
              ])}
              empty={t('mobileAdmin.noDevices')}
            />
          )}
        </div>
      )}

      {/* ── Notifications ──────────────────────────────── */}
      {tab === 'notifications' && (
        <div className="space-y-4">
          <div className="flex gap-2 items-end">
            <Field label={t('mobileAdmin.customerId')}><input className="input w-full" value={notifCust} onChange={e => setNotifCust(e.target.value)} /></Field>
            <button onClick={loadNotifs} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 h-10">{t('mobileAdmin.search')}</button>
            <button onClick={() => setSendModal(true)} className="rounded bg-green-600 px-4 py-2 text-white hover:bg-green-700 h-10 ml-auto">{t('mobileAdmin.sendNotification')}</button>
          </div>
          {notifsLoading ? <Spinner /> : (
            <Table
              cols={[t('mobileAdmin.customerId'), t('mobileAdmin.notifTitle'), t('mobileAdmin.body'), t('mobileAdmin.type'), t('mobileAdmin.sentAt')]}
              rows={notifs.map(n => [
                n.customerId,
                n.title,
                <span key={n.id} className="max-w-xs truncate block text-sm">{n.body}</span>,
                n.notificationType,
                new Date(n.sentAt).toLocaleString(),
              ])}
              empty={t('mobileAdmin.noNotifications')}
            />
          )}
        </div>
      )}

      {/* ── Send Notification Modal ────────────────────── */}
      {sendModal && (
        <Modal title={t('mobileAdmin.sendNotification')} onClose={() => setSendModal(false)}>
          <div className="space-y-3">
            <Field label={t('mobileAdmin.customerId')}><input className="input w-full" value={nf.customerId} onChange={e => setNf({ ...nf, customerId: e.target.value })} /></Field>
            <Field label={t('mobileAdmin.notifTitle')}><input className="input w-full" value={nf.title} onChange={e => setNf({ ...nf, title: e.target.value })} /></Field>
            <Field label={t('mobileAdmin.body')}><input className="input w-full" value={nf.body} onChange={e => setNf({ ...nf, body: e.target.value })} /></Field>
            <Field label={t('mobileAdmin.type')}><input className="input w-full" value={nf.notificationType} onChange={e => setNf({ ...nf, notificationType: e.target.value })} /></Field>
            <Field label={t('mobileAdmin.referenceId')}><input className="input w-full" value={nf.referenceId} onChange={e => setNf({ ...nf, referenceId: e.target.value })} /></Field>
            <button onClick={sendNotification} className="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 w-full">{t('mobileAdmin.send')}</button>
          </div>
        </Modal>
      )}
    </div>
  );
}
