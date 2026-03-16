'use client';
import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import Link from 'next/link';
import { isLoggedIn, getUsername, getRole, getUserInfo, clearCredentials, type Role } from '@/lib/auth';
import { useI18n, type Locale } from '@/lib/i18n';
import { apiFetch } from '@/lib/api';
import {
  LayoutDashboard, ShoppingCart, CreditCard, Factory, HardHat, BookOpen,
  Package, Building2, Warehouse, BarChart3, Settings, MessageCircle,
  Sparkles, TrendingUp, Truck, FileText, Users, Star, MapPin, Smartphone,
  ArrowLeftRight, Crown, Bell, User, LogOut, Globe, Menu, X, ChefHat, Lock, ShieldCheck,
  type LucideIcon,
} from 'lucide-react';
import { cn, SidebarItem } from './design-system';

// ─── nav definition ─────────────────────────────────────────────────────────

type NavItem = { href: string; labelKey: string; icon: LucideIcon; featureKey?: string; accent?: boolean };
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
    titleKey: 'nav.aiSection',
    items: [
      { href: '/ai-whatsapp', labelKey: 'nav.aiWhatsapp', icon: MessageCircle, featureKey: 'AI_BOT', accent: true },
      { href: '/ai-suggestions', labelKey: 'nav.aiSuggestions', icon: Sparkles, featureKey: 'AI_BOT' },
      { href: '/ai-pricing', labelKey: 'nav.aiPricing', icon: TrendingUp, featureKey: 'AI_BOT' },
      { href: '/quality-predictions', labelKey: 'nav.qualityPredictions', icon: ShieldCheck, featureKey: 'AI_BOT' },
    ],
    roles: ['admin', 'management'],
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
      { href: '/suppliers', labelKey: 'nav.suppliers', icon: Package, featureKey: 'SUPPLIER' },
      { href: '/deliveries', labelKey: 'nav.deliveries', icon: Truck },
    ],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.finance',
    items: [
      { href: '/invoices', labelKey: 'nav.invoices', icon: FileText, featureKey: 'INVOICING' },
      { href: '/customers', labelKey: 'nav.customers', icon: Users },
    ],
    roles: ['admin', 'management', 'finance'],
  },
  {
    titleKey: 'nav.loyalty',
    items: [{ href: '/loyalty', labelKey: 'nav.loyalty', icon: Star, featureKey: 'LOYALTY' }],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.analytics',
    items: [
      { href: '/dashboard', labelKey: 'nav.analytics', icon: TrendingUp },
      { href: '/report-builder', labelKey: 'nav.reportBuilder', icon: BarChart3 },
    ],
    roles: ['admin', 'management', 'finance'],
  },
  {
    titleKey: 'nav.driverSection',
    items: [{ href: '/driver', labelKey: 'nav.driver', icon: MapPin }],
    roles: ['admin', 'management'],
  },
  {
    titleKey: 'nav.platform',
    items: [
      { href: '/notification-templates', labelKey: 'nav.notificationTemplates', icon: Bell, featureKey: 'SUBSCRIPTIONS' },
      { href: '/subscriptions', labelKey: 'nav.subscriptions', icon: Crown, featureKey: 'SUBSCRIPTIONS' },
      { href: '/exchange-rates', labelKey: 'nav.exchangeRates', icon: ArrowLeftRight, featureKey: 'SUBSCRIPTIONS' },
      { href: '/mobile-admin', labelKey: 'nav.mobileAdmin', icon: Smartphone, featureKey: 'SUBSCRIPTIONS' },
      { href: '/tenant-management', labelKey: 'nav.tenantManagement', icon: Building2, featureKey: 'SUBSCRIPTIONS' },
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

export default function AuthShell({ children }: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const pathname = usePathname();
  const { t, locale, setLocale } = useI18n();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [features, setFeatures] = useState<string[]>([]);
  const isCustomerRoute = pathname.startsWith('/customer/')  || pathname === '/customer';
  const isLoginRoute = pathname === '/login';
  const authenticated = isLoggedIn();
  const user = authenticated ? getUsername() : '';
  const role: Role = authenticated ? getRole() : 'viewer';

  useEffect(() => {
    if (isCustomerRoute) return; // customer portal has its own auth
    if (!authenticated && !isLoginRoute) {
      router.replace('/login');
      return;
    }
    if (!authenticated) {
      return;
    }

    apiFetch<{ features?: string[] }>('/v2/tenant/features')
      .then((data) => setFeatures(data.features ?? []))
      .catch(() => setFeatures([]));

    const defaultRoute = ROLE_DEFAULT[role];
    if (defaultRoute && (pathname === '/' || pathname === '/dashboard')) {
      router.replace(defaultRoute);
    }
  }, [authenticated, isCustomerRoute, isLoginRoute, pathname, role, router]);

  if (!authenticated && !isLoginRoute && !isCustomerRoute) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }
  if (isLoginRoute || isCustomerRoute) return <>{children}</>;

  const logout = () => {
    clearCredentials();
    setFeatures([]);
    router.push('/login');
  };

  const filteredSections = SECTIONS
    .filter((s) => s.roles.includes(role))
    .map((s) => ({
      ...s,
      items: s.items.filter((item) => !item.featureKey || features.includes(item.featureKey)),
    }))
    .filter((s) => s.items.length > 0);

  // Check if current page requires a feature the tenant doesn't have
  const allItems = SECTIONS.flatMap((s) => s.items);
  const currentItem = allItems.find(
    (item) => pathname === item.href || (item.href !== '/dashboard' && pathname.startsWith(item.href + '/'))
  );
  const isFeatureGated = currentItem?.featureKey && !features.includes(currentItem.featureKey);

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
        {filteredSections.map((section) => (
          <div key={section.titleKey ?? section.items[0]?.href ?? 'nav'}>
            {section.titleKey && (
              <div className="mb-2 px-3 text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                {t(section.titleKey)}
              </div>
            )}
            <div className="space-y-1">
              {section.items.map((item) => {
                const active = pathname === item.href || (item.href !== '/dashboard' && pathname.startsWith(item.href + '/'));
                return (
                  <Link key={item.href} href={item.href} className="block" onClick={() => setSidebarOpen(false)}>
                    <SidebarItem
                      icon={item.icon}
                      label={t(item.labelKey)}
                      active={active}
                      accent={item.accent}
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
          <button
            type="button"
            aria-label="Close sidebar"
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
        <main className="min-w-0">
          {isFeatureGated ? (
            <div className="flex flex-col items-center justify-center py-24 text-center">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-100 text-amber-600">
                <Lock className="h-8 w-8" />
              </div>
              <h2 className="text-xl font-semibold text-gray-900">{t('subscription.upgradeRequired')}</h2>
              <p className="mt-2 max-w-md text-gray-500">{t('subscription.upgradeMessage')}</p>
              <p className="mt-1 text-xs text-gray-400">{t('subscription.featureNeeded', { feature: currentItem?.featureKey ?? '' })}</p>
              <button
                onClick={() => router.push('/subscriptions')}
                className="mt-6 rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-medium text-white hover:bg-blue-700 transition-colors"
              >
                {t('subscription.upgradeCta')}
              </button>
            </div>
          ) : children}
        </main>
      </div>
    </div>
  );
}
