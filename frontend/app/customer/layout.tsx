'use client';
import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import Link from 'next/link';
import { User, LogOut, ChefHat, Search, Package } from 'lucide-react';
import {
  isCustomerLoggedIn, getCustomerInfo, clearCustomerSession,
  type CustomerInfo,
} from '@/lib/customer-auth';

const NAV = [
  { href: '/customer/catalog', label: 'Catalog', icon: Search },
  { href: '/customer/orders', label: 'My Orders', icon: Package },
  { href: '/customer/profile', label: 'My Account', icon: User },
];

export default function CustomerLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [customer, setCustomer] = useState<CustomerInfo | null>(null);

  useEffect(() => {
    // login page is always accessible
    if (pathname === '/customer/login') return;
    if (!isCustomerLoggedIn()) {
      router.replace('/customer/login');
    } else {
      setCustomer(getCustomerInfo());
    }
  }, [pathname, router]);

  // Login page renders without shell
  if (pathname === '/customer/login') return <>{children}</>;

  const logout = () => {
    clearCustomerSession();
    router.push('/customer/login');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top navigation bar */}
      <header className="sticky top-0 z-30 border-b border-gray-200 bg-white shadow-sm">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
          <Link href="/customer/catalog" className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-600 text-white">
              <ChefHat className="h-4 w-4" />
            </div>
            <span className="text-sm font-bold text-gray-900">BreadCost</span>
          </Link>

          <nav className="flex items-center gap-1">
            {NAV.map((item) => {
              const active = pathname === item.href || pathname.startsWith(item.href + '/');
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                    active
                      ? 'bg-amber-50 text-amber-700'
                      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                  }`}
                >
                  <item.icon className="h-4 w-4" />
                  <span className="hidden sm:inline">{item.label}</span>
                </Link>
              );
            })}

            {customer && (
              <div className="ml-2 flex items-center gap-2 border-l pl-3">
                <span className="hidden text-sm text-gray-600 sm:inline">{customer.name}</span>
                <button
                  onClick={logout}
                  className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                  title="Sign out"
                >
                  <LogOut className="h-4 w-4" />
                </button>
              </div>
            )}
          </nav>
        </div>
      </header>

      {/* Main content */}
      <main className="mx-auto max-w-6xl px-4 py-6 sm:px-6">{children}</main>
    </div>
  );
}
