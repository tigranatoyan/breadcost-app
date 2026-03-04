'use client';
import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import Link from 'next/link';
import { isLoggedIn, getUsername, getRole, getUserInfo, clearCredentials, type Role } from '@/lib/auth';

// ─── nav definition ─────────────────────────────────────────────────────────

type NavItem = { href: string; label: string };
type NavSection = { title?: string; items: NavItem[]; roles: Role[] };

const SECTIONS: NavSection[] = [
  // ── admin / management / viewer ──────────────────────────────────────────
  {
    items: [{ href: '/dashboard', label: '📊 Dashboard' }],
    roles: ['admin', 'management', 'viewer', 'finance'],
  },
  {
    title: 'Operations',
    items: [
      { href: '/orders', label: '📦 Orders' },
      { href: '/production-plans', label: '📅 Production Plans' },
    ],
    roles: ['admin', 'management'],
  },
  {
    title: 'Warehouse',
    items: [{ href: '/inventory', label: '🏬 Inventory' }],
    roles: ['admin', 'management', 'warehouse'],
  },
  {
    title: 'Sales',
    items: [{ href: '/pos', label: '🛒 Point of Sale' }],
    roles: ['admin', 'management', 'cashier'],
  },
  {
    title: 'Reports',
    items: [{ href: '/reports', label: '📈 Reports' }],
    roles: ['admin', 'management', 'viewer', 'finance'],
  },
  // ── floor ─────────────────────────────────────────────────────────────────
  {
    title: 'My Shift',
    items: [{ href: '/floor', label: '🏭 Production Floor' }],
    roles: ['floor'],
  },
  {
    title: 'Operations',
    items: [{ href: '/production-plans', label: '📅 Production Plans' }],
    roles: ['floor'],
  },
  // ── technologist ─────────────────────────────────────────────────────────
  {
    title: 'Workshop',
    items: [
      { href: '/recipes', label: '📋 Recipes & Steps' },
      { href: '/products', label: '🍞 Products' },
    ],
    roles: ['technologist'],
  },
  {
    title: 'Analysis',
    items: [{ href: '/technologist', label: '🔬 Technologist View' }],
    roles: ['technologist'],
  },
  // ── admin only ────────────────────────────────────────────────────────────
  {
    title: 'Floor',
    items: [{ href: '/floor', label: '🏭 Production Floor' }],
    roles: ['admin'],
  },
  {
    title: 'Analysis',
    items: [{ href: '/technologist', label: '🔬 Technologist' }],
    roles: ['admin'],
  },
  {
    title: 'Configuration',
    items: [
      { href: '/admin', label: '⚙️ Admin Panel' },
      { href: '/departments', label: '   🏢 Departments' },
      { href: '/products', label: '   🍞 Products' },
      { href: '/recipes', label: '   📋 Recipes & Steps' },
    ],
    roles: ['admin'],
  },
];

const ROLE_LABELS: Record<Role, string> = {
  admin: 'Administrator',
  floor: 'Floor Staff',
  management: 'Management',
  viewer: 'Viewer',
  finance: 'Finance',
  warehouse: 'Warehouse',
  cashier: 'Cashier',
  technologist: 'Technologist',
};

/** Default landing route per role */
const ROLE_DEFAULT: Partial<Record<Role, string>> = {
  floor: '/floor',
  cashier: '/pos',
  warehouse: '/inventory',
  technologist: '/recipes',
  finance: '/reports',
};

export default function AuthShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [checked, setChecked] = useState(false);
  const [user, setUser] = useState('');
  const [role, setRole] = useState<Role>('viewer');

  useEffect(() => {
    if (!isLoggedIn() && pathname !== '/login') {
      router.replace('/login');
    } else {
      const u = getUsername();
      const r = getRole();
      setUser(u);
      setRole(r);
      setChecked(true);
      // Role-based default redirect — only fires when landing on root or dashboard
      const defaultRoute = ROLE_DEFAULT[r];
      if (defaultRoute && (pathname === '/' || pathname === '/dashboard')) {
        router.replace(defaultRoute);
      }
    }
  }, [pathname, router]);

  if (!checked && pathname !== '/login') {
    // Show a minimal loading screen instead of blank
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }
  if (pathname === '/login') return <>{children}</>;

  const logout = () => {
    clearCredentials();
    router.push('/login');
  };

  return (
    <div className="flex h-screen bg-gray-50 text-gray-900 overflow-hidden">
      {/* Sidebar */}
      <aside className="w-56 shrink-0 bg-slate-900 text-white flex flex-col">
        <div className="px-5 py-4 border-b border-slate-700">
          <div className="font-bold text-lg tracking-tight">🍞 BreadCost</div>
          <div className="text-xs text-slate-400 mt-0.5">{ROLE_LABELS[role]}</div>
        </div>

        <nav className="flex-1 p-3 overflow-y-auto space-y-4">
          {SECTIONS.filter((s) => s.roles.includes(role)).map((section, si) => (
            <div key={si}>
              {section.title && (
                <div className="px-3 mb-1 text-xs font-semibold uppercase tracking-widest text-slate-500">
                  {section.title}
                </div>
              )}
              <div className="space-y-0.5">
                {section.items.map((item) => {
                  const active = pathname === item.href || (item.href !== '/dashboard' && pathname.startsWith(item.href + '/'));
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      className={`block px-3 py-2 rounded text-sm transition-colors ${
                        active
                          ? 'bg-slate-600 text-white font-medium'
                          : 'text-slate-300 hover:bg-slate-700 hover:text-white'
                      }`}
                    >
                      {item.label}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        <div className="px-4 py-3 border-t border-slate-700 flex items-center justify-between">
          <div className="min-w-0">
            <div className="text-xs text-white font-medium truncate">{user}</div>
            <div className="text-xs text-slate-400">{getUserInfo()?.tenantId ?? 'tenant1'}</div>
          </div>
          <button
            onClick={logout}
            className="text-xs text-slate-400 hover:text-white transition-colors px-2 py-1 rounded hover:bg-slate-700 shrink-0 ml-1"
          >
            Sign out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto p-6">{children}</main>
    </div>
  );
}
