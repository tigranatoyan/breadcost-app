const CUST_TOKEN = 'bc_cust_token';
const CUST_USER  = 'bc_cust_user';

export interface CustomerInfo {
  customerId: string;
  name: string;
  tenantId: string;
}

export function getCustomerToken(): string {
  if (typeof window === 'undefined') return '';
  return localStorage.getItem(CUST_TOKEN) ?? '';
}

export function getCustomerInfo(): CustomerInfo | null {
  if (typeof window === 'undefined') return null;
  const raw = localStorage.getItem(CUST_USER);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

export function setCustomerSession(token: string, info: CustomerInfo) {
  localStorage.setItem(CUST_TOKEN, token);
  localStorage.setItem(CUST_USER, JSON.stringify(info));
}

export function clearCustomerSession() {
  localStorage.removeItem(CUST_TOKEN);
  localStorage.removeItem(CUST_USER);
}

export function isCustomerLoggedIn(): boolean {
  if (typeof window === 'undefined') return false;
  return !!localStorage.getItem(CUST_TOKEN);
}
