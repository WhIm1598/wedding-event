import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { searchApi } from '@/api/system.api';
import { Badge, type BadgeVariant } from '@/components/ui/Badge';
import type { SearchResult } from '@/types';

const KIND: Record<SearchResult['kind'], { label: string; variant: BadgeVariant }> = {
  customer: { label: 'Khách hàng', variant: 'default' },
  lead: { label: 'Lead', variant: 'info' },
  contract: { label: 'Hợp đồng', variant: 'brand' },
};

export function GlobalSearch() {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const [focused, setFocused] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);

  // Ctrl+K / Cmd+K focuses the search box
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  // Debounced search
  useEffect(() => {
    if (!focused) return;
    const t = setTimeout(() => searchApi.search(query).then(setResults), 200);
    return () => clearTimeout(t);
  }, [query, focused]);

  const open = (r: SearchResult) => navigate(`/customers?view=${r.kind === 'lead' ? 'kanban' : 'table'}`);

  return (
    <div className="relative w-full max-w-md hidden sm:block">
      <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
      <input
        ref={inputRef}
        type="text"
        value={query}
        placeholder="Tìm kiếm nhanh HĐ, SĐT khách (Ctrl+K)..."
        className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500 focus:bg-white transition-all"
        onFocus={() => setFocused(true)}
        onBlur={() => setTimeout(() => setFocused(false), 200)}
        onChange={(e) => setQuery(e.target.value)}
      />

      {focused && (
        <div className="absolute top-full mt-2 w-full bg-white rounded-xl shadow-xl border border-slate-200 p-2 z-50 animate-in fade-in slide-in-from-top-2">
          <p className="text-xs font-bold text-slate-400 uppercase tracking-wider px-3 py-2">{query ? 'Kết quả tìm kiếm' : 'Kết quả gần đây'}</p>
          {results.length === 0 && <p className="px-3 py-2 text-sm text-slate-400">Không tìm thấy kết quả.</p>}
          {results.map((r) => (
            <button
              key={`${r.kind}-${r.id}`}
              onMouseDown={() => open(r)}
              className="w-full p-2 hover:bg-slate-50 rounded-lg flex justify-between items-center gap-2 group text-left"
            >
              <span className="text-sm font-medium text-slate-700 group-hover:text-rose-600 truncate">{r.label}</span>
              <Badge variant={KIND[r.kind].variant}>{KIND[r.kind].label}</Badge>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
