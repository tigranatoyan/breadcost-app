'use client';
import { ReactNode, ButtonHTMLAttributes, InputHTMLAttributes, SelectHTMLAttributes } from 'react';
import { useT } from '@/lib/i18n';

/* ── Utility ── */
export function cn(...parts: (string | false | null | undefined)[]) {
  return parts.filter(Boolean).join(' ');
}

/* ── Button ── */
const buttonVariants = {
  primary: 'bg-blue-600 text-white hover:bg-blue-700',
  secondary: 'bg-gray-100 text-gray-700 hover:bg-gray-200',
  danger: 'bg-red-600 text-white hover:bg-red-700',
  success: 'bg-green-600 text-white hover:bg-green-700',
  ghost: 'bg-transparent text-gray-700 hover:bg-gray-100',
} as const;

const buttonSizes = {
  xs: 'px-2.5 py-1.5 text-xs',
  sm: 'px-3 py-1.5 text-sm',
  lg: 'px-5 py-2.5 text-sm',
} as const;

export type ButtonVariant = keyof typeof buttonVariants;
export type ButtonSize = keyof typeof buttonSizes;

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

export function Button({ variant = 'primary', size = 'sm', className, children, ...props }: ButtonProps) {
  return (
    <button
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-md font-medium transition disabled:cursor-not-allowed disabled:opacity-50',
        buttonVariants[variant],
        buttonSizes[size],
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}

/* ── Card ── */
interface CardProps {
  title?: string;
  action?: ReactNode;
  className?: string;
  children: ReactNode;
}

export function Card({ title, action, className, children }: CardProps) {
  return (
    <div className={cn('rounded-2xl border border-gray-200 bg-white p-4 shadow-sm', className)}>
      {(title || action) && (
        <div className="mb-4 flex items-center justify-between gap-3">
          <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
          {action}
        </div>
      )}
      {children}
    </div>
  );
}

/* ── StatCard ── */
interface StatCardProps {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string | number;
  hint?: string;
}

export function StatCard({ icon: Icon, label, value, hint }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-4 shadow-sm">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</p>
          <p className="mt-2 text-2xl font-bold text-gray-900">{value}</p>
          {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
        </div>
        <div className="rounded-xl bg-blue-50 p-2 text-blue-600">
          <Icon className="h-5 w-5" />
        </div>
      </div>
    </div>
  );
}

/* ── SectionTitle ── */
interface SectionTitleProps {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  action?: ReactNode;
}

export function SectionTitle({ eyebrow, title, subtitle, action }: SectionTitleProps) {
  return (
    <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <div>
        {eyebrow && <div className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-600">{eyebrow}</div>}
        <h1 className="mt-1 text-2xl font-bold text-gray-900">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-gray-500">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

/* ── Input ── */
interface InputFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  label?: string;
  hint?: string;
  error?: string;
  rightIcon?: React.ComponentType<{ className?: string }>;
}

export function InputField({ label, hint, error, rightIcon: Icon, className, ...props }: InputFieldProps) {
  return (
    <label className="block">
      {label && <div className="mb-1 text-sm font-medium text-gray-700">{label}</div>}
      <div className="relative">
        <input
          className={cn(
            'w-full rounded-md border px-3 py-2 text-sm outline-none ring-0 transition bg-white',
            error
              ? 'border-red-400 focus:border-red-500 focus:ring-2 focus:ring-red-500'
              : 'border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-500',
            className,
          )}
          {...props}
        />
        {Icon && <Icon className="pointer-events-none absolute right-3 top-2.5 h-4 w-4 text-gray-400" />}
      </div>
      {error && <div className="mt-1 text-xs text-red-600">{error}</div>}
      {hint && !error && <div className="mt-1 text-xs text-gray-500">{hint}</div>}
    </label>
  );
}

/* ── SelectField ── */
interface SelectFieldProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  label?: string;
  options: { value: string; label: string }[];
}

export function SelectField({ label, options, className, ...props }: SelectFieldProps) {
  return (
    <label className="block">
      {label && <div className="mb-1 text-sm font-medium text-gray-700">{label}</div>}
      <select
        className={cn(
          'w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-500',
          className,
        )}
        {...props}
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </label>
  );
}

/* ── Progress ── */
interface ProgressProps {
  value: number;
  className?: string;
}

export function Progress({ value, className }: ProgressProps) {
  const clamped = Math.max(0, Math.min(100, value));
  return (
    <div className={cn('h-2 w-full overflow-hidden rounded-full bg-gray-100', className)}>
      <div className="h-full rounded-full bg-blue-600 transition-all" style={{ width: `${clamped}%` }} />
    </div>
  );
}

/* ── Badge ── */
const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-100 text-gray-700',
  ACTIVE: 'bg-green-100 text-green-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  IN_PRODUCTION: 'bg-amber-100 text-amber-800',
  IN_PROGRESS: 'bg-amber-100 text-amber-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  READY: 'bg-teal-100 text-teal-800',
  DELIVERED: 'bg-green-100 text-green-800',
  OUT_FOR_DELIVERY: 'bg-blue-100 text-blue-800',
  PUBLISHED: 'bg-teal-100 text-teal-800',
  APPROVED: 'bg-purple-100 text-purple-800',
  GENERATED: 'bg-orange-100 text-orange-800',
  OVERDUE: 'bg-red-100 text-red-800',
  PENDING: 'bg-yellow-100 text-yellow-800',
  NOT_STARTED: 'bg-gray-100 text-gray-700',
  ARCHIVED: 'bg-yellow-100 text-yellow-700',
  INACTIVE: 'bg-gray-100 text-gray-500',
  STARTED: 'bg-blue-100 text-blue-700',
};

interface BadgeProps {
  status: string;
  children?: ReactNode;
}

export function Badge({ status, children }: BadgeProps) {
  const t = useT();
  const cls = STATUS_COLORS[status] ?? 'bg-gray-100 text-gray-700';
  const key = `statusLabels.${status}`;
  const translated = t(key as any);
  const label = children || (translated !== key ? translated : status.replace(/_/g, ' '));
  return (
    <span className={cn('inline-flex rounded-full px-2 py-0.5 text-xs font-medium', cls)}>
      {label}
    </span>
  );
}

/* ── SidebarItem ── */
interface SidebarItemProps {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  active?: boolean;
  accent?: boolean;
  onClick?: () => void;
}

export function SidebarItem({ icon: Icon, label, active, accent, onClick }: SidebarItemProps) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition',
        active
          ? accent ? 'bg-emerald-600 text-white shadow-sm' : 'bg-slate-800 text-white shadow-sm'
          : accent ? 'text-emerald-400 hover:bg-emerald-900/40 hover:text-emerald-300' : 'text-slate-300 hover:bg-slate-800 hover:text-white',
      )}
    >
      <Icon className={cn('h-4 w-4', accent && !active && 'text-emerald-400')} />
      <span>{label}</span>
    </button>
  );
}

/* ── Table ── */
interface TableProps {
  cols: string[];
  rows: ReactNode[][];
  empty?: string;
}

export function Table({ cols, rows, empty = 'No records found' }: TableProps) {
  if (rows.length === 0) {
    return <div className="text-center py-16 text-sm text-gray-400 border rounded-xl bg-white">{empty}</div>;
  }
  return (
    <>
      {/* Desktop table */}
      <div className="hidden sm:block overflow-x-auto rounded-2xl border border-gray-200 bg-white shadow-sm">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {cols.map((c) => (
                <th key={c} className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  {c}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {rows.map((row, i) => (
              <tr key={i} className="hover:bg-gray-50 transition-colors">
                {row.map((cell, j) => (
                  <td key={j} className="whitespace-nowrap px-4 py-3 text-gray-700 align-top">
                    {cell}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Mobile card view */}
      <div className="sm:hidden space-y-3">
        {rows.map((row, i) => (
          <div key={i} className="rounded-xl border border-gray-200 bg-white p-3 shadow-sm space-y-1.5 text-sm">
            {row.map((cell, j) => (
              <div key={j} className="flex items-start justify-between gap-2">
                <span className="text-xs font-medium text-gray-500 shrink-0">{cols[j]}</span>
                <span className="text-gray-700 text-right">{cell}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </>
  );
}
