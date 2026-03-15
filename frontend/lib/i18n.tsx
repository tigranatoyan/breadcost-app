'use client';
import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import en from '@/locales/en';
import hy from '@/locales/hy';

export type Locale = 'en' | 'hy';

const dictionaries: Record<Locale, Record<string, unknown>> = { en, hy };

type Dict = typeof en;

// Flatten nested object keys: { a: { b: 'x' } } => 'a.b'
type FlatKeys<T, Prefix extends string = ''> = T extends Record<string, unknown>
  ? { [K in keyof T & string]: FlatKeys<T[K], Prefix extends '' ? K : `${Prefix}.${K}`> }[keyof T & string]
  : Prefix;

export type TranslationKey = FlatKeys<Dict>;

function getNestedValue(obj: unknown, path: string): string {
  const keys = path.split('.');
  let current: unknown = obj;
  for (const key of keys) {
    if ((current === null || current === undefined) || typeof current !== 'object') return path;
    current = (current as Record<string, unknown>)[key];
  }
  return typeof current === 'string' ? current : path;
}

interface I18nContextValue {
  locale: Locale;
  setLocale: (_l: Locale) => void;
  t: (_key: string, _replacements?: Record<string, string | number>) => string;
}

const I18nContext = createContext<I18nContextValue>({
  locale: 'en',
  setLocale: () => {},
  t: (k) => k,
});

const STORAGE_KEY = 'breadcost_locale';

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(() => {
    if (typeof window === 'undefined') return 'en';
    const stored = localStorage.getItem(STORAGE_KEY) as Locale | null;
    return stored && dictionaries[stored] ? stored : 'en';
  });

  const setLocale = useCallback((l: Locale) => {
    setLocaleState(l);
    localStorage.setItem(STORAGE_KEY, l);
  }, []);

  const t = useCallback(
    (key: string, replacements?: Record<string, string | number>): string => {
      let value = getNestedValue(dictionaries[locale], key);
      if (replacements) {
        for (const [k, v] of Object.entries(replacements)) {
          value = value.replace(`{${k}}`, String(v));
        }
      }
      return value;
    },
    [locale],
  );

  return (
    <I18nContext.Provider value={{ locale, setLocale, t }}>
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  return useContext(I18nContext);
}

export function useT() {
  return useContext(I18nContext).t;
}

/* ── Locale-aware date formatters ──────────────────────────────────── */
export const BCP47: Record<Locale, string> = { en: 'en-GB', hy: 'hy-AM' };

/** Format a date string/Date as DD.MM.YYYY (or locale equiv.) */
export function useDateFmt() {
  const { locale } = useContext(I18nContext);
  const bcp = BCP47[locale];
  return useCallback(
    (v: string | number | Date | null | undefined): string => {
      if (!v) return '—';
      try { return new Date(v).toLocaleDateString(bcp); }
      catch { return String(v); }
    },
    [bcp],
  );
}

/** Format a date string/Date as DD.MM.YYYY HH:mm */
export function useDateTimeFmt() {
  const { locale } = useContext(I18nContext);
  const bcp = BCP47[locale];
  return useCallback(
    (v: string | number | Date | null | undefined): string => {
      if (!v) return '—';
      try {
        const d = new Date(v);
        return d.toLocaleDateString(bcp) + ' ' + d.toLocaleTimeString(bcp, { hour: '2-digit', minute: '2-digit' });
      } catch { return String(v); }
    },
    [bcp],
  );
}
