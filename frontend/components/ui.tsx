'use client';
import { ReactNode, createContext, useContext, useState, useCallback, useRef } from 'react';

/* ── Skeleton ──────────────────────────────────────────────────────── */
export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse rounded-lg bg-gray-200 ${className}`} />;
}

/** A ready-made page skeleton: 4 stat cards + table placeholder */
export function PageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <Skeleton className="h-3 w-20 mb-2" />
          <Skeleton className="h-7 w-48" />
        </div>
        <Skeleton className="h-9 w-28 rounded-md" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm space-y-3">
            <Skeleton className="h-3 w-24" />
            <Skeleton className="h-7 w-16" />
            <Skeleton className="h-2 w-32" />
          </div>
        ))}
      </div>
      <div className="rounded-xl border bg-white shadow-sm p-4 space-y-3">
        {[0, 1, 2, 3, 4].map((i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    </div>
  );
}

/* ── Toast system ──────────────────────────────────────────────────── */
type ToastType = 'success' | 'error' | 'info';
interface Toast { id: number; message: string; type: ToastType }

interface ToastCtx {
  toast: (_message: string, _type?: ToastType) => void;
  toastSuccess: (_message: string) => void;
  toastError: (_message: string) => void;
}

const ToastContext = createContext<ToastCtx>({
  toast: () => {},
  toastSuccess: () => {},
  toastError: () => {},
});

export const useToast = () => useContext(ToastContext);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(0);

  const toast = useCallback((message: string, type: ToastType = 'info') => {
    const id = nextId.current++;
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 4000);
  }, []);

  const toastSuccess = useCallback((msg: string) => toast(msg, 'success'), [toast]);
  const toastError = useCallback((msg: string) => toast(msg, 'error'), [toast]);

  const dismiss = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const colors: Record<ToastType, string> = {
    success: 'bg-green-600',
    error: 'bg-red-600',
    info: 'bg-blue-600',
  };

  const icons: Record<ToastType, string> = {
    success: '✓',
    error: '⚠',
    info: 'ℹ',
  };

  return (
    <ToastContext.Provider value={{ toast, toastSuccess, toastError }}>
      {children}
      {/* Toast container — fixed bottom-right */}
      <div className="fixed bottom-4 right-4 z-[9999] flex flex-col-reverse gap-2 pointer-events-none">
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`pointer-events-auto flex items-center gap-2 rounded-lg px-4 py-3 text-sm font-medium text-white shadow-lg transition-all animate-[slideUp_0.3s_ease-out] ${colors[t.type]}`}
          >
            <span>{icons[t.type]}</span>
            <span className="flex-1 max-w-xs break-words">{t.message}</span>
            <button onClick={() => dismiss(t.id)} className="ml-2 text-white/70 hover:text-white">×</button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

/* ── Pagination ────────────────────────────────────────────────────── */
interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (_page: number) => void;
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-center gap-2 py-4 text-sm">
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 0}
        className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        ←
      </button>
      <span className="text-gray-600">
        {page + 1} / {totalPages}
      </span>
      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="rounded-md border border-gray-300 bg-white px-3 py-1.5 text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        →
      </button>
    </div>
  );
}

/* ── Modal ─────────────────────────────────────────────────────────── */
export function Modal({
  title,
  onClose,
  wide,
  children,
}: {
  title: string;
  onClose: () => void;
  wide?: boolean;
  children: ReactNode;
}) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-start justify-center z-50 p-4 pt-12 overflow-y-auto">
      <div
        className={`bg-white rounded-xl shadow-2xl w-full flex flex-col ${
          wide ? 'max-w-3xl' : 'max-w-xl'
        }`}
      >
        <div className="flex items-center justify-between px-5 py-4 border-b shrink-0">
          <h2 className="font-semibold text-base">{title}</h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-700 text-2xl leading-none"
          >
            ×
          </button>
        </div>
        <div className="p-5 overflow-y-auto">{children}</div>
      </div>
    </div>
  );
}

/* ── Confirm Modal ─────────────────────────────────────────────────── */
export function ConfirmModal({
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  onConfirm,
  onCancel,
}: {
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-sm">
        <div className="p-5 text-sm text-gray-700">{message}</div>
        <div className="flex justify-end gap-2 px-5 pb-4">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50"
          >
            {cancelLabel}
          </button>
          <button
            onClick={onConfirm}
            className="px-4 py-2 text-sm rounded-lg bg-blue-600 text-white hover:bg-blue-700"
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

/** Hook that replaces `window.confirm()` with a styled modal.
 *  Returns `[askConfirm, ConfirmModalElement]`.
 *  Call `const ok = await askConfirm(msg)` — it resolves `true`/`false`.
 *  Render `{ConfirmModalElement}` once at the bottom of the page JSX. */
export function useConfirm(opts?: { confirmLabel?: string; cancelLabel?: string }) {
  const [state, setState] = useState<{
    message: string;
    resolve: (v: boolean) => void;
  } | null>(null);

  const ask = useCallback(
    (message: string) =>
      new Promise<boolean>((resolve) => {
        setState({ message, resolve });
      }),
    [],
  );

  const handleConfirm = useCallback(() => {
    state?.resolve(true);
    setState(null);
  }, [state]);

  const handleCancel = useCallback(() => {
    state?.resolve(false);
    setState(null);
  }, [state]);

  const element = state ? (
    <ConfirmModal
      message={state.message}
      confirmLabel={opts?.confirmLabel}
      cancelLabel={opts?.cancelLabel}
      onConfirm={handleConfirm}
      onCancel={handleCancel}
    />
  ) : null;

  return [ask, element] as const;
}

/* ── Spinner ───────────────────────────────────────────────────────── */
export function Spinner() {
  return (
    <div className="flex justify-center py-16">
      <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
    </div>
  );
}

/* ── Alert ─────────────────────────────────────────────────────────── */
export function Alert({ msg, onClose }: { msg: string; onClose: () => void }) {
  return (
    <div className="mb-4 flex items-start gap-2 bg-red-50 border border-red-200 text-red-800 rounded-lg px-4 py-3 text-sm">
      <span className="flex-1 break-words min-w-0">⚠ {msg}</span>
      <button
        onClick={onClose}
        className="shrink-0 text-red-400 hover:text-red-600 text-xl leading-none"
      >
        ×
      </button>
    </div>
  );
}

/* ── Success ───────────────────────────────────────────────────────── */
export function Success({ msg, onClose }: { msg: string; onClose: () => void }) {
  return (
    <div className="mb-4 flex items-center gap-2 bg-green-50 border border-green-200 text-green-800 rounded-lg px-4 py-3 text-sm">
      <span className="flex-1">✓ {msg}</span>
      <button
        onClick={onClose}
        className="text-green-400 hover:text-green-600 text-xl leading-none"
      >
        ×
      </button>
    </div>
  );
}

/* ── Badge ─────────────────────────────────────────────────────────── */
const BADGE_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-600',
  ACTIVE: 'bg-green-100 text-green-700',
  ARCHIVED: 'bg-yellow-100 text-yellow-700',
  INACTIVE: 'bg-gray-100 text-gray-500',
  CONFIRMED: 'bg-blue-100 text-blue-700',
  CANCELLED: 'bg-red-100 text-red-700',
  PUBLISHED: 'bg-purple-100 text-purple-700',
  IN_PROGRESS: 'bg-orange-100 text-orange-700',
  COMPLETED: 'bg-green-100 text-green-700',
  PENDING: 'bg-slate-100 text-slate-600',
  STARTED: 'bg-blue-100 text-blue-700',
  IN_PRODUCTION: 'bg-orange-100 text-orange-700',
  READY: 'bg-teal-100 text-teal-700',
  OUT_FOR_DELIVERY: 'bg-sky-100 text-sky-700',
  DELIVERED: 'bg-emerald-100 text-emerald-700',
};

export function Badge({ status }: { status: string }) {
  const cls = BADGE_COLORS[status] ?? 'bg-gray-100 text-gray-600';
  return (
    <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${cls}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
}

/* ── Field ─────────────────────────────────────────────────────────── */
export function Field({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      {children}
      {hint && <p className="mt-1 text-xs text-gray-400">{hint}</p>}
    </div>
  );
}

/* ── Table ─────────────────────────────────────────────────────────── */
export function Table({
  cols,
  rows,
  empty = 'No records found',
}: {
  cols: string[];
  rows: ReactNode[][];
  empty?: string;
}) {
  if (rows.length === 0) {
    return (
      <div className="text-center py-16 text-sm text-gray-400 border rounded-xl bg-white">
        {empty}
      </div>
    );
  }
  return (
    <div className="overflow-x-auto rounded-xl border bg-white shadow-sm">
      <table className="min-w-full text-sm">
        <thead className="bg-gray-50 border-b">
          <tr>
            {cols.map((c) => (
              <th
                key={c}
                className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide whitespace-nowrap"
              >
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {rows.map((row, i) => (
            <tr key={i} className="hover:bg-gray-50 transition-colors">
              {row.map((cell, j) => (
                <td key={j} className="px-4 py-3 text-gray-700 align-top">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
