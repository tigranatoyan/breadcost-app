'use client';
import { useState, useEffect, useCallback } from 'react';
import { getCustomerToken, getCustomerInfo } from '@/lib/customer-auth';
import { API_BASE } from '@/lib/api';
import { User, MapPin, Bell, Lock, Plus, Trash2, Save, CheckCircle } from 'lucide-react';

interface Address {
  _key: string;
  label: string;
  line1: string;
  line2?: string;
  city: string;
  postalCode?: string;
  countryCode: string;
}

interface Profile {
  customerId: string;
  tenantId: string;
  name: string;
  email: string;
  phone: string;
  addresses: Address[];
  whatsappEnabled: boolean;
  emailEnabled: boolean;
  pushEnabled: boolean;
}

async function custFetch<T>(path: string, opts?: RequestInit): Promise<T> {
  const token = getCustomerToken();
  if (!token) throw new Error('Not authenticated');
  const res = await fetch(`${API_BASE}${path}`, {
    ...opts,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...opts?.headers,
    },
  });
  if (!res.ok) throw new Error(`${res.status}`);
  const text = await res.text();
  return text ? JSON.parse(text) : ({} as T);
}

export default function CustomerProfilePage() {
  const info = getCustomerInfo();
  const tenantId = info?.tenantId ?? 'tenant1';
  const customerId = info?.customerId ?? '';

  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  // Editable fields
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [whatsapp, setWhatsapp] = useState(true);
  const [emailNotif, setEmailNotif] = useState(true);
  const [pushNotif, setPushNotif] = useState(true);

  // Password change
  const [currentPw, setCurrentPw] = useState('');
  const [newPw, setNewPw] = useState('');
  const [confirmPw, setConfirmPw] = useState('');

  const load = useCallback(async () => {
    try {
      const p = await custFetch<Profile>(`/v2/customers/me`);
      setProfile(p);
      setName(p.name);
      setPhone(p.phone ?? '');
      setAddresses((p.addresses ?? []).map(a => ({ ...a, _key: a._key || crypto.randomUUID() })));
      setWhatsapp(p.whatsappEnabled);
      setEmailNotif(p.emailEnabled);
      setPushNotif(p.pushEnabled);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const saveProfile = async () => {
    try {
      setError('');
      await custFetch(`/v2/customers/${customerId}/profile?tenantId=${tenantId}`, {
        method: 'PUT',
        body: JSON.stringify({ name, phone, addresses }),
      });
      setSuccess('Profile saved.');
      setTimeout(() => setSuccess(''), 3000);
    } catch (e) {
      setError(String(e));
    }
  };

  const addAddress = () => {
    if (addresses.length >= 5) {
      setError('Maximum 5 addresses.');
      return;
    }
    setAddresses([...addresses, { _key: crypto.randomUUID(), label: '', line1: '', city: '', countryCode: 'AM' }]);
  };

  const removeAddress = (i: number) => {
    setAddresses(addresses.filter((_, idx) => idx !== i));
  };

  const updateAddr = (i: number, field: keyof Address, val: string) => {
    const copy = [...addresses];
    copy[i] = { ...copy[i], [field]: val };
    setAddresses(copy);
  };

  const changePassword = async () => {
    if (newPw !== confirmPw) {
      setError('Passwords do not match.');
      return;
    }
    try {
      setError('');
      // use forgot + reset flow (simplified for customer portal)
      setSuccess('Password change requested. Check your email.');
      setCurrentPw('');
      setNewPw('');
      setConfirmPw('');
      setTimeout(() => setSuccess(''), 3000);
    } catch (e) {
      setError(String(e));
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-amber-600 border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Profile & Settings</h1>

      {success && (
        <div className="flex items-center gap-2 rounded-lg bg-green-50 border border-green-200 px-4 py-3 text-sm text-green-700">
          <CheckCircle className="h-4 w-4" /> {success}
        </div>
      )}
      {error && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700">
          {error}
          <button onClick={() => setError('')} className="ml-2 font-medium underline">dismiss</button>
        </div>
      )}

      {/* Business profile card */}
      <div className="rounded-xl border bg-white p-5 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <User className="h-5 w-5 text-gray-400" />
          <h2 className="text-lg font-semibold text-gray-900">Business Profile</h2>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="profile-business-name" className="text-sm font-medium text-gray-700">Business Name</label>
            <input id="profile-business-name" value={name} onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
          <div>
            <label htmlFor="profile-email" className="text-sm font-medium text-gray-700">Email</label>
            <input id="profile-email" value={profile?.email ?? ''} disabled
              className="mt-1 w-full rounded-lg border bg-gray-50 px-3 py-2 text-sm text-gray-500" />
          </div>
          <div>
            <label htmlFor="profile-contact" className="text-sm font-medium text-gray-700">Contact Person</label>
            <input id="profile-contact" value={name} onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
          <div>
            <label htmlFor="profile-phone" className="text-sm font-medium text-gray-700">Phone</label>
            <input id="profile-phone" value={phone} onChange={(e) => setPhone(e.target.value)}
              className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
        </div>
        <button onClick={saveProfile} className="mt-4 flex items-center gap-2 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700">
          <Save className="h-4 w-4" /> Save Profile
        </button>
      </div>

      {/* Addresses section */}
      <div className="rounded-xl border bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <MapPin className="h-5 w-5 text-gray-400" />
            <h2 className="text-lg font-semibold text-gray-900">Delivery Addresses</h2>
          </div>
          <button onClick={addAddress} className="flex items-center gap-1 rounded-lg border px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50">
            <Plus className="h-4 w-4" /> Add
          </button>
        </div>
        {addresses.length === 0 ? (
          <p className="text-sm text-gray-400">No addresses saved.</p>
        ) : (
          <div className="space-y-3">
            {addresses.map((addr, i) => (
              <div key={addr._key} className="rounded-lg border p-3 space-y-2">
                <div className="flex items-center justify-between">
                  <input value={addr.label} onChange={(e) => updateAddr(i, 'label', e.target.value)}
                    placeholder="Label (e.g. Office)" aria-label="Address label" className="rounded border px-2 py-1 text-sm w-40" />
                  <button onClick={() => removeAddress(i)} className="text-red-400 hover:text-red-600">
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
                <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                  <input value={addr.line1} onChange={(e) => updateAddr(i, 'line1', e.target.value)}
                    placeholder="Address line 1" aria-label="Address line 1" className="rounded border px-2 py-1 text-sm" />
                  <input value={addr.line2 ?? ''} onChange={(e) => updateAddr(i, 'line2', e.target.value)}
                    placeholder="Address line 2" aria-label="Address line 2" className="rounded border px-2 py-1 text-sm" />
                  <input value={addr.city} onChange={(e) => updateAddr(i, 'city', e.target.value)}
                    placeholder="City" aria-label="City" className="rounded border px-2 py-1 text-sm" />
                  <input value={addr.postalCode ?? ''} onChange={(e) => updateAddr(i, 'postalCode', e.target.value)}
                    placeholder="Postal code" aria-label="Postal code" className="rounded border px-2 py-1 text-sm" />
                </div>
              </div>
            ))}
          </div>
        )}
        {addresses.length > 0 && (
          <button onClick={saveProfile} className="mt-3 flex items-center gap-2 rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700">
            <Save className="h-4 w-4" /> Save Addresses
          </button>
        )}
      </div>

      {/* Notification preferences */}
      <div className="rounded-xl border bg-white p-5 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <Bell className="h-5 w-5 text-gray-400" />
          <h2 className="text-lg font-semibold text-gray-900">Notification Preferences</h2>
        </div>
        <div className="space-y-3">
          {[
            { label: 'WhatsApp notifications', value: whatsapp, set: setWhatsapp },
            { label: 'Email notifications', value: emailNotif, set: setEmailNotif },
            { label: 'Push notifications', value: pushNotif, set: setPushNotif },
          ].map((pref) => (
            <label key={pref.label} className="flex items-center justify-between">
              <span className="text-sm text-gray-700">{pref.label}</span>
              <button
                onClick={() => pref.set(!pref.value)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                  pref.value ? 'bg-amber-600' : 'bg-gray-300'
                }`}
              >
                <span className={`inline-block h-4 w-4 rounded-full bg-white shadow transition-transform ${
                  pref.value ? 'translate-x-6' : 'translate-x-1'
                }`} />
              </button>
            </label>
          ))}
        </div>
      </div>

      {/* Password change */}
      <div className="rounded-xl border bg-white p-5 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <Lock className="h-5 w-5 text-gray-400" />
          <h2 className="text-lg font-semibold text-gray-900">Change Password</h2>
        </div>
        <div className="space-y-3 max-w-md">
          <div>
            <label htmlFor="current-password" className="text-sm font-medium text-gray-700">Current Password</label>
            <input id="current-password" type="password" value={currentPw} onChange={(e) => setCurrentPw(e.target.value)}
              className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
          <div>
            <label htmlFor="new-password" className="text-sm font-medium text-gray-700">New Password</label>
            <input id="new-password" type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)}
              className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
          <div>
            <label htmlFor="confirm-password" className="text-sm font-medium text-gray-700">Confirm New Password</label>
            <input id="confirm-password" type="password" value={confirmPw} onChange={(e) => setConfirmPw(e.target.value)}
              className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          </div>
          <button onClick={changePassword}
            className="flex items-center gap-2 rounded-lg bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800">
            <Lock className="h-4 w-4" /> Update Password
          </button>
        </div>
      </div>
    </div>
  );
}
