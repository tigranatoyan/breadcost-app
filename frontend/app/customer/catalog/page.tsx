'use client';
import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { API_BASE, TENANT_ID } from '@/lib/api';
import { Search, ChevronLeft, ChevronRight, Package, ShoppingCart, Plus } from 'lucide-react';

interface CatalogProduct {
  productId: string;
  departmentId: string;
  name: string;
  description: string;
  saleUnit: string;
  price: number;
  vatRatePct: number;
}

interface Department {
  departmentId: string;
  name: string;
}

interface PageResult {
  content: CatalogProduct[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

const PAGE_SIZE = 12;

export default function CatalogPage() {
  const router = useRouter();
  const [products, setProducts] = useState<PageResult | null>(null);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [search, setSearch] = useState('');
  const [deptFilter, setDeptFilter] = useState('');
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState('name,asc');
  const [loading, setLoading] = useState(true);
  const [cart, setCart] = useState<Record<string, { name: string; saleUnit: string; price: number; qty: number }>>({});

  // Load cart from sessionStorage on mount
  useEffect(() => {
    if (globalThis.window === undefined) return;
    const raw = sessionStorage.getItem('bc_cart');
    if (raw) {
      try {
        const items: Array<{ productId: string; name: string; saleUnit: string; price: number; qty: number }> = JSON.parse(raw);
        const map: typeof cart = {};
        items.forEach(i => { map[i.productId] = { name: i.name, saleUnit: i.saleUnit, price: i.price, qty: i.qty }; });
        setCart(map);
      } catch { /* ignore */ }
    }
  }, []);

  // Persist cart to sessionStorage
  const saveCart = (c: typeof cart) => {
    const items = Object.entries(c).map(([productId, v]) => ({ productId, ...v }));
    sessionStorage.setItem('bc_cart', JSON.stringify(items));
  };

  const addToCart = (p: CatalogProduct) => {
    setCart(prev => {
      const next = { ...prev };
      if (next[p.productId]) {
        next[p.productId] = { ...next[p.productId], qty: next[p.productId].qty + 1 };
      } else {
        next[p.productId] = { name: p.name, saleUnit: p.saleUnit, price: p.price, qty: 1 };
      }
      saveCart(next);
      return next;
    });
  };

  const cartCount = Object.values(cart).reduce((s, i) => s + i.qty, 0);

  // Fetch departments for the filter dropdown
  useEffect(() => {
    fetch(`${API_BASE}/v1/departments?tenantId=${TENANT_ID}`)
      .then((r) => r.ok ? r.json() : [])
      .then((d) => setDepartments(Array.isArray(d) ? d : []))
      .catch(() => {});
  }, []);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    const params = new URLSearchParams({
      tenantId: TENANT_ID,
      page: String(page),
      size: String(PAGE_SIZE),
      sort,
    });
    if (search.trim()) params.set('search', search.trim());
    if (deptFilter) params.set('departmentId', deptFilter);

    try {
      const res = await fetch(`${API_BASE}/v2/products?${params}`);
      if (res.ok) {
        setProducts(await res.json());
      }
    } catch {
      // silently handle
    } finally {
      setLoading(false);
    }
  }, [page, sort, search, deptFilter]);

  useEffect(() => { fetchProducts(); }, [fetchProducts]);

  // Reset to page 0 when search/filter changes
  const handleSearch = (v: string) => { setSearch(v); setPage(0); };
  const handleDept = (v: string) => { setDeptFilter(v); setPage(0); };
  const handleSort = (v: string) => { setSort(v); setPage(0); };

  return (
    <div>
      {/* Header */}
      <div className="mb-6 flex items-end justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-amber-600 mb-1">Browse</p>
          <h1 className="text-2xl font-bold text-gray-900">Product Catalog</h1>
        </div>
        {cartCount > 0 && (
          <button
            onClick={() => router.push('/customer/checkout')}
            className="flex items-center gap-2 rounded-xl bg-amber-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-amber-700"
          >
            <ShoppingCart className="h-4 w-4" />
            Checkout ({cartCount})
          </button>
        )}
      </div>

      {/* Filters bar */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            className="input pl-9"
            type="text"
            placeholder="Search products…"
            value={search}
            onChange={(e) => handleSearch(e.target.value)}
          />
        </div>

        {/* Department filter */}
        <select
          className="input w-auto min-w-[160px]"
          value={deptFilter}
          onChange={(e) => handleDept(e.target.value)}
        >
          <option value="">All Departments</option>
          {departments.map((d) => (
            <option key={d.departmentId} value={d.departmentId}>{d.name}</option>
          ))}
        </select>

        {/* Sort */}
        <select
          className="input w-auto min-w-[140px]"
          value={sort}
          onChange={(e) => handleSort(e.target.value)}
        >
          <option value="name,asc">Name A→Z</option>
          <option value="name,desc">Name Z→A</option>
          <option value="price,asc">Price ↑</option>
          <option value="price,desc">Price ↓</option>
        </select>
      </div>

      {/* Loading */}
      {loading && (
        <div className="flex justify-center py-16">
          <div className="w-8 h-8 border-4 border-amber-600 border-t-transparent rounded-full animate-spin" />
        </div>
      )}

      {/* Products grid */}
      {!loading && products && (
        <>
          {products.content.length === 0 ? (
            <div className="text-center py-16">
              <Package className="mx-auto h-12 w-12 text-gray-300 mb-3" />
              <p className="text-gray-500">No products found.</p>
              <p className="text-sm text-gray-400 mt-1">Try adjusting your search or filters.</p>
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {products.content.map((p) => (
                <div
                  key={p.productId}
                  className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm hover:shadow-md transition-shadow"
                >
                  <div className="flex items-start justify-between mb-3">
                    <h3 className="font-semibold text-gray-900 leading-tight">{p.name}</h3>
                    <span className="shrink-0 ml-2 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-medium text-amber-700">
                      {p.saleUnit}
                    </span>
                  </div>
                  {p.description && (
                    <p className="text-sm text-gray-500 line-clamp-2 mb-3">{p.description}</p>
                  )}
                  <div className="flex items-baseline gap-1">
                    <span className="text-lg font-bold text-gray-900">
                      {p.price?.toLocaleString('en-US', { minimumFractionDigits: 0 })}
                    </span>
                    <span className="text-xs text-gray-400">AMD</span>
                    {p.vatRatePct > 0 && (
                      <span className="text-xs text-gray-400 ml-1">+{p.vatRatePct}% VAT</span>
                    )}
                  </div>
                  <button
                    onClick={() => addToCart(p)}
                    className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-lg bg-amber-50 px-3 py-2 text-sm font-medium text-amber-700 hover:bg-amber-100 transition-colors"
                  >
                    <Plus className="h-3.5 w-3.5" />
                    {cart[p.productId] ? `In cart (${cart[p.productId].qty})` : 'Add to Cart'}
                  </button>
                </div>
              ))}
            </div>
          )}

          {/* Pagination */}
          {products.totalPages > 1 && (
            <div className="mt-6 flex items-center justify-between">
              <p className="text-sm text-gray-500">
                {products.totalElements} product{products.totalElements === 1 ? '' : 's'}
                {' · '}Page {products.page + 1} of {products.totalPages}
              </p>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="btn-secondary px-2 py-1.5 disabled:opacity-40"
                >
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <button
                  onClick={() => setPage(Math.min(products.totalPages - 1, page + 1))}
                  disabled={page >= products.totalPages - 1}
                  className="btn-secondary px-2 py-1.5 disabled:opacity-40"
                >
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
