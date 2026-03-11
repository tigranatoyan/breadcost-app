'use client';
import Link from 'next/link';
import { useI18n } from '@/lib/i18n';
import { Button } from '@/components/design-system';

export default function NotFound() {
  const { t } = useI18n();

  return (
    <div className="flex min-h-[520px] items-center justify-center">
      <div className="max-w-xl text-center">
        <div className="text-7xl font-black text-slate-900">404</div>
        <div className="mt-3 text-2xl font-bold text-gray-900">
          {t('notFound.title')}
        </div>
        <p className="mt-3 text-sm text-gray-500">
          {t('notFound.description')}
        </p>
        <div className="mt-6 flex justify-center gap-2">
          <Link href="/dashboard">
            <Button>{t('notFound.backToDashboard')}</Button>
          </Link>
          <Link href="/products">
            <Button variant="secondary">{t('notFound.openCatalog')}</Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
