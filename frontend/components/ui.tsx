'use client';
import { ReactNode } from 'react';

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
