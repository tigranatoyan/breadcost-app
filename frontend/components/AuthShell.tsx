'use client';
import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import Link from 'next/link';
import { isLoggedIn, getUsername, getRole, getUserInfo, clearCredentials, type Role } from '@/lib/auth';
import { useI18n, type Locale } from '@/lib/i18n';
import {
  LayoutDashboard, ShoppingCart, CreditCard, Factory, HardHat, BookOpen,
  Package, Building2, Warehouse, BarChart3, Settings, MessageCircle,
  Sparkles, TrendingUp, Truck, FileText, Users, Star, MapPin, Smartphone,
  ArrowLeftRight, Crown, Bell, User, LogOut, Globe, Menu, X, ChefHat,
  type LucideIcon,
} from 'lucide-react';
import { cn, SidebarItem } from './design-system';

// ─── nav definition ─────────────────────────────────────────────────────────

type NavItem = { href: string; labelKey: string; icon: LucideIcon };
type NavSection = { titleKey?: string; items: NavItem[]; roles: Role[] };

const SECTIONS: NavSection[] = [
  {
    items: [{ href: '/dashboard', labelKey: 'nav.dashboard', icon: LayoutDashboard }],
    roles: ['admin', 'management', 'viewer', 'finance'],
  },
  {
    titleKey: 'nav.operations',
    items: [
      { href: '/orders', labelKey: 'nav.orders', icon: ShoppingCart },
      { href: '/production-plans', labelKey: 'nav.productionPlans', icon: Factory },
    ],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.warehouse',
    items: [{ href: '/inventory', labelKey: 'nav.inventory', icon: Warehouse }],
    roles: ['admin', 'management', 'warehouse'],
  },
  {
    titleKey: 'nav.sales',
    items: [{ href: '/pos', labelKey: 'nav.pos', icon: CreditCard }],
    roles: ['admin', 'management', 'cashier'],
  },
  {
    titleKey: 'nav.reportsSection',
    items: [{ href: '/reports', labelKey: 'nav.reports', icon: BarChart3 }],
    roles: ['admin', 'management', 'viewer', 'finance'],
  },
  {
    titleKey: 'nav.myShift',
    items: [{ href: '/floor', labelKey: 'nav.floor', icon: HardHat }],
    roles: ['floor'],
  },
  {
    titleKey: 'nav.operations',
    items: [{ href: '/production-plans', labelKey: 'nav.productionPlans', icon: Factory }],
    roles: ['floor'],
  },
  {
    titleKey: 'nav.workshop',
    items: [
      { href: '/recipes', labelKey: 'nav.recipes', icon: BookOpen },
      { href: '/products', labelKey: 'nav.products', icon: Package },
    ],
    roles: ['technologist'],
  },
  {
    titleKey: 'nav.analysis',
    items: [{ href: '/technologist', labelKey: 'nav.technologistView', icon: TrendingUp }],
    roles: ['technologist'],
  },
  {
    titleKey: 'nav.floorSection',
    items: [{ href: '/floor', labelKey: 'nav.floor', icon: HardHat }],
    roles: ['admin'],
  },
  {
    titleKey: 'nav.analysis',
    items: [{ href: '/technologist', labelKey: 'nav.technologist', icon: TrendingUp }],
    roles: ['admin'],
  },
  {
    titleKey: 'nav.supplyChain',
    items: [
      { href: '/suppliers', labelKey: 'nav.suppliers', icon: Package },
      { href: '/deliveries', labelKey: 'nav.deliveries', icon: Truck },
    ],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.finance',
    items: [
      { href: '/invoices', labelKey: 'nav.invoices', icon: FileText },
      { href: '/customers', labelKey: 'nav.customers', icon: Users },
    ],
    roles: ['admin', 'management', 'finance'],
  },
  {
    titleKey: 'nav.loyalty',
    items: [{ href: '/loyalty', labelKey: 'nav.loyalty', icon: Star }],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.analytics',
    items: [{ href: '/report-builder', labelKey: 'nav.reportBuilder', icon: BarChart3 }],
    roles: ['admin', 'management', 'finance'],
  },
  {
    titleKey: 'nav.aiSection',
    items: [
      { href: '/ai-whatsapp', labelKey: 'nav.aiWhatsapp', icon: MessageCircle },
      { href: '/ai-suggestions', labelKey: 'nav.aiSuggestions', icon: Sparkles },
      { href: '/ai-pricing', labelKey: 'nav.aiPricing', icon: TrendingUp },
    ],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.driverSection',
    items: [{ href: '/driver', labelKey: 'nav.driver', icon: MapPin }],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.platform',
    items: [
      { href: '/notification-templates', labelKey: 'nav.notificationTemplates', icon: Bell },
      { href: '/subscriptions', labelKey: 'nav.subscriptions', icon: Crown },
      { href: '/exchange-rates', labelKey: 'nav.exchangeRates', icon: ArrowLeftRight },
      { href: '/mobile-admin', labelKey: 'nav.mobileAdmin', icon: Smartphone },
    ],
    roles: ['admin'],
  },
  {
    titleKey: 'nav.configuration',
    items: [
      { href: '/admin', labelKey: 'nav.admin', icon: Settings },
      { href: '/departments', labelKey: 'nav.departments', icon: Building2 },
      { href: '/products', labelKey: 'nav.navProducts', icon: Package },
      { href: '/recipes', labelKey: 'nav.navRecipes', icon: BookOpen },
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
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    if (pathname.startsWith('/customer')) return; // customer portal has its own auth
    if (!isLoggedIn() && pathname !== '/login') {
      router.replace('/login');
    } else {
      const u = getUsername();
      const r = getRole();
      setUser(u);
      setRole(r);
      setChecked(true);
      const defaultRoute = ROLE_DEFAULT[r];
      if (defaultRoute && (pathname === '/' || pathname === '/dashboard')) {
        router.replace(defaultRoute);
      }
    }
  }, [pathname, router]);

  // Close sidebar on navigation (mobile)
  useEffect(() => {
    setSidebarOpen(false);
  }, [pathname]);

  if (!checked && pathname !== '/login' && !pathname.startsWith('/customer')) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }
  if (pathname === '/login' || pathname.startsWith('/customer')) return <>{children}</>;

  const logout = () => {
    clearCredentials();
    router.push('/login');
  };

  const filteredSections = SECTIONS.filter((s) => s.roles.includes(role));

  const sidebarContent = (
    <>
      {/* Brand block */}
      <div className="mb-4 rounded-2xl bg-slate-800 p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 text-white shadow-lg">
            <ChefHat className="h-5 w-5" />
          </div>
          <div>
            <div className="font-semibold text-white">BreadCost</div>
            <div className="text-xs text-slate-400">ERP · {t(`roles.${role}`)}</div>
          </div>
        </div>
      </div>

      {/* Navigation sections */}
      <nav className="flex-1 space-y-4 overflow-y-auto">
        {filteredSections.map((section, si) => (
          <div key={si}>
            {section.titleKey && (
              <div className="mb-2 px-3 text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                {t(section.titleKey)}
              </div>
            )}
            <div className="space-y-1">
              {section.items.map((item) => {
                const active = pathname === item.href || (item.href !== '/dashboard' && pathname.startsWith(item.href + '/'));
                return (
                  <Link key={item.href + si} href={item.href} className="block">
                    <SidebarItem
                      icon={item.icon}
                      label={t(item.labelKey)}
                      active={active}
                    />
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      {/* Footer: language switcher */}
      <div className="mt-4 flex items-center justify-center gap-1 border-t border-slate-700 pt-3">
        {LOCALES.map((l) => (
          <button
            key={l.code}
            onClick={() => setLocale(l.code)}
            className={cn(
              'rounded px-2.5 py-1 text-xs font-medium transition-colors',
              locale === l.code
                ? 'bg-blue-600 text-white'
                : 'text-slate-400 hover:bg-slate-700 hover:text-white',
            )}
          >
            {l.label}
          </button>
        ))}
      </div>

      {/* Footer: user info + logout */}
      <div className="mt-3 flex items-center justify-between border-t border-slate-700 pt-3">
        <div className="min-w-0">
          <div className="truncate text-xs font-medium text-white">{user}</div>
          <div className="text-xs text-slate-400">{getUserInfo()?.tenantId ?? 'tenant1'}</div>
        </div>
        <button
          onClick={logout}
          className="ml-1 shrink-0 rounded p-1.5 text-slate-400 transition-colors hover:bg-slate-700 hover:text-white"
          title={t('nav.signOut')}
        >
          <LogOut className="h-4 w-4" />
        </button>
      </div>
    </>
  );

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900">
      {/* ── Top navigation bar ── */}
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto flex max-w-[1800px] items-center justify-between gap-3 px-6 py-3">
          {/* Left: hamburger (mobile) + logo */}
          <div className="flex items-center gap-3">
            <button
              className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 lg:hidden"
              onClick={() => setSidebarOpen(!sidebarOpen)}
            >
              {sidebarOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
            </button>
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-blue-600 text-white shadow">
                <ChefHat className="h-5 w-5" />
              </div>
              <div className="hidden sm:block">
                <div className="text-sm font-bold leading-tight">BreadCost</div>
                <div className="text-xs text-gray-500">{t(`roles.${role}`)}</div>
              </div>
            </div>
          </div>

          {/* Right: language, bell, user, logout */}
          <div className="flex items-center gap-2">
            <div className="hidden items-center gap-1 rounded-lg border border-gray-200 bg-gray-50 px-2 py-1.5 md:flex">
              <Globe className="h-3.5 w-3.5 text-gray-400" />
              {LOCALES.map((l) => (
                <button
                  key={l.code}
                  onClick={() => setLocale(l.code)}
                  className={cn(
                    'rounded px-1.5 py-0.5 text-xs font-medium transition-colors',
                    locale === l.code
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-500 hover:text-gray-900',
                  )}
                >
                  {l.label}
                </button>
              ))}
            </div>
            <button className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 transition-colors">
              <Bell className="h-4 w-4" />
            </button>
            <div className="hidden items-center gap-2 sm:flex">
              <div className="flex h-7 w-7 items-center justify-center rounded-full bg-blue-100 text-blue-700">
                <User className="h-3.5 w-3.5" />
              </div>
              <span className="text-sm font-medium text-gray-700">{user}</span>
            </div>
            <button
              onClick={logout}
              className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 transition-colors"
              title={t('nav.signOut')}
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </header>

      {/* ── Body: sidebar + main ── */}
      <div className="mx-auto grid max-w-[1800px] gap-6 p-6 lg:grid-cols-[260px_minmax(0,1fr)]">
        {/* Mobile sidebar overlay */}
        {sidebarOpen && (
          <div
            className="fixed inset-0 z-30 bg-black/40 lg:hidden"
            onClick={() => setSidebarOpen(false)}
          />
        )}

        {/* Sidebar */}
        <aside
          className={cn(
            'fixed inset-y-0 left-0 z-40 flex w-72 flex-col rounded-r-[28px] bg-slate-900 p-4 text-white shadow-xl transition-transform lg:static lg:z-auto lg:w-auto lg:rounded-[28px] lg:translate-x-0',
            sidebarOpen ? 'translate-x-0' : '-translate-x-full',
          )}
        >
          {sidebarContent}
        </aside>

        {/* Main content */}
        <main className="min-w-0">{children}</main>
      </div>
    </div>
  );
}
