'use client';
import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import Link from 'next/link';
import { isLoggedIn, getUsername, getRole, getUserInfo, clearCredentials, type Role } from '@/lib/auth';
import { useI18n, type Locale } from '@/lib/i18n';

// ─── nav definition ─────────────────────────────────────────────────────────

type NavItem = { href: string; labelKey: string };
type NavSection = { titleKey?: string; items: NavItem[]; roles: Role[] };

const SECTIONS: NavSection[] = [
  {
    items: [{ href: '/dashboard', labelKey: 'nav.dashboard' }],
    roles: ['admin', 'management', 'viewer', 'finance'],
  },
  {
    titleKey: 'nav.operations',
    items: [
      { href: '/orders', labelKey: 'nav.orders' },
      { href: '/production-plans', labelKey: 'nav.productionPlans' },
    ],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.warehouse',
    items: [{ href: '/inventory', labelKey: 'nav.inventory' }],
    roles: ['admin', 'management', 'warehouse'],
  },
  {
    titleKey: 'nav.sales',
    items: [{ href: '/pos', labelKey: 'nav.pos' }],
    roles: ['admin', 'management', 'cashier'],
  },
  {
    titleKey: 'nav.reportsSection',
    items: [{ href: '/reports', labelKey: 'nav.reports' }],
    roles: ['admin', 'management', 'viewer', 'finance'],
  },
  {
    titleKey: 'nav.myShift',
    items: [{ href: '/floor', labelKey: 'nav.floor' }],
    roles: ['floor'],
  },
  {
    titleKey: 'nav.operations',
    items: [{ href: '/production-plans', labelKey: 'nav.productionPlans' }],
    roles: ['floor'],
  },
  {
    titleKey: 'nav.workshop',
    items: [
      { href: '/recipes', labelKey: 'nav.recipes' },
      { href: '/products', labelKey: 'nav.products' },
    ],
    roles: ['technologist'],
  },
  {
    titleKey: 'nav.analysis',
    items: [{ href: '/technologist', labelKey: 'nav.technologistView' }],
    roles: ['technologist'],
  },
  {
    titleKey: 'nav.floorSection',
    items: [{ href: '/floor', labelKey: 'nav.floor' }],
    roles: ['admin'],
  },
  {
    titleKey: 'nav.analysis',
    items: [{ href: '/technologist', labelKey: 'nav.technologist' }],
    roles: ['admin'],
  },
  {
    titleKey: 'nav.configuration',
    items: [
      { href: '/admin', labelKey: 'nav.admin' },
      { href: '/departments', labelKey: 'nav.departments' },
      { href: '/products', labelKey: 'nav.navProducts' },
      { href: '/recipes', labelKey: 'nav.navRecipes' },
    ],
    roles: ['admin'],
  },
];

const LOCALES: { code: Locale; label: string }[] = [
  { code: 'en', label: 'EN' },
  { code: 'hy', label: 'HY' },
];

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
  const { t, locale, setLocale } = useI18n();
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
          <div className="text-xs text-slate-400 mt-0.5">{t(`roles.${role}`)}</div>
        </div>

        <nav className="flex-1 p-3 overflow-y-auto space-y-4">
          {SECTIONS.filter((s) => s.roles.includes(role)).map((section, si) => (
            <div key={si}>
              {section.titleKey && (
                <div className="px-3 mb-1 text-xs font-semibold uppercase tracking-widest text-slate-500">
                  {t(section.titleKey)}
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
                      {t(item.labelKey)}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        <div className="px-4 py-2 border-t border-slate-700 flex items-center justify-center gap-1">
          {LOCALES.map((l) => (
            <button
              key={l.code}
              onClick={() => setLocale(l.code)}
              className={`px-2 py-1 rounded text-xs font-medium transition-colors ${
                locale === l.code
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-400 hover:text-white hover:bg-slate-700'
              }`}
            >
              {l.label}
            </button>
          ))}
        </div>

        <div className="px-4 py-3 border-t border-slate-700 flex items-center justify-between">
          <div className="min-w-0">
            <div className="text-xs text-white font-medium truncate">{user}</div>
            <div className="text-xs text-slate-400">{getUserInfo()?.tenantId ?? 'tenant1'}</div>
          </div>
          <button
            onClick={logout}
            className="text-xs text-slate-400 hover:text-white transition-colors px-2 py-1 rounded hover:bg-slate-700 shrink-0 ml-1"
          >
            {t('nav.signOut')}
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto p-6">{children}</main>
    </div>
  );
}
