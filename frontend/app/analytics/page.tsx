'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { PageSkeleton } from '@/components/ui';

/** BC-309: Analytics merged into Dashboard widgets. Redirect for bookmarks. */
export default function AnalyticsPage() {
  const router = useRouter();
  useEffect(() => { router.replace('/dashboard'); }, [router]);
  return <PageSkeleton />;
}
