import type { Metadata } from 'next';
import AuthShell from '@/components/AuthShell';
import { I18nProvider } from '@/lib/i18n';
import { ToastProvider } from '@/components/ui';
import './globals.css';

export const metadata: Metadata = {
  title: 'BreadCost',
  description: 'Bread Factory Management',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body className="bg-gray-50 text-gray-900">
        <I18nProvider>
          <ToastProvider>
            <AuthShell>{children}</AuthShell>
          </ToastProvider>
        </I18nProvider>
      </body>
    </html>
  );
}
