import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { Newspaper, Youtube, CalendarDays, Eye, RefreshCw } from 'lucide-react';
import bundledNews from '../../public/news.json';

const FAST = { duration: 0.16, ease: [0.2, 0, 0, 1] } as const;
const TAP = { type: 'spring', stiffness: 700, damping: 26 } as const;

interface NewsItem {
  id: string;
  title: string;
  url: string;
  date: string;
  views: number;
  thumbnail: string;
}

interface NewsData {
  channel: { id: string; title: string; url: string };
  updated: string;
  items: NewsItem[];
}

const REMOTE_URL =
  'https://raw.githubusercontent.com/Pumyk/FFWPU-Holy-Songs-/main/public/news.json';
const CACHE_KEY = 'hphc-news-cache';

const loadCached = (): NewsData | null => {
  try {
    const raw = window.localStorage.getItem(CACHE_KEY);
    return raw ? (JSON.parse(raw) as NewsData) : null;
  } catch {
    return null;
  }
};

const fmtDate = (iso: string) =>
  new Date(iso).toLocaleDateString(undefined, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });

const fmtViews = (n: number) =>
  n >= 1000 ? `${(n / 1000).toFixed(1)}K` : String(n);

export default function NewsPage() {
  const [data, setData] = useState<NewsData>(
    () => loadCached() ?? (bundledNews as unknown as NewsData)
  );
  const [refreshing, setRefreshing] = useState(true);

  const refresh = async () => {
    setRefreshing(true);
    try {
      const res = await fetch(REMOTE_URL, { signal: AbortSignal.timeout(5000) });
      if (res.ok) {
        const fresh = (await res.json()) as NewsData;
        if (Array.isArray(fresh.items) && fresh.items.length > 0) {
          setData(fresh);
          try {
            window.localStorage.setItem(CACHE_KEY, JSON.stringify(fresh));
          } catch { /* storage full: ignore */ }
        }
      }
    } catch {
      /* offline: keep cached/bundled digest */
    } finally {
      setRefreshing(false);
    }
  };

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="pb-[calc(6rem+env(safe-area-inset-bottom))]">
      {/* Page header */}
      <div className="px-5 md:px-8 pt-10 md:pt-16 max-w-2xl mx-auto relative">
        <div
          className="absolute -top-4 left-1/2 -translate-x-1/2 w-80 h-44 pointer-events-none"
          style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 18%, transparent), transparent)' }}
        />
        <div className="relative">
          <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-brand/15 dark:bg-amber-brand/10 text-[10px] font-black tracking-[0.18em] uppercase text-ember dark:text-amber-brand mb-6">
            <Newspaper className="w-3 h-3" />
            Weekly digest
          </span>
          <h2 className="text-4xl md:text-6xl font-black text-ink-900 dark:text-creamtext mb-4 leading-[1.02] tracking-tight">
            HJ Global News
          </h2>
          <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-[11px] font-bold uppercase tracking-[0.15em] text-ink-700 dark:text-muted-warm">
            <span className="inline-flex items-center gap-1.5">
              <CalendarDays className="w-3.5 h-3.5 text-ember dark:text-amber-brand" />
              Updated {fmtDate(data.updated)}
            </span>
            <motion.button
              whileTap={{ scale: 0.9 }}
              transition={TAP}
              onClick={refresh}
              disabled={refreshing}
              className="inline-flex items-center gap-1.5 text-ink-700 dark:text-creamtext hover:text-ember dark:hover:text-amber-brand transition-colors disabled:opacity-40"
              aria-label="Refresh news"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${refreshing ? 'animate-spin' : ''}`} />
              {refreshing ? 'Checking\u2026' : 'Refresh'}
            </motion.button>
          </div>
        </div>
      </div>

      {/* Editions */}
      <div className="px-5 md:px-8 max-w-2xl mx-auto mt-10 space-y-3">
        {data.items.map((item, idx) => (
          <motion.button
            key={item.id}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ ...FAST, delay: Math.min(idx * 0.03, 0.2) }}
            whileTap={{ scale: 0.98 }}
            onClick={() => window.open(item.url, '_blank')}
            className="w-full text-left rounded-[24px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] hover:border-amber-brand/40 hover:shadow-lg hover:shadow-amber-brand/5 dark:hover:bg-night-700/60 transition-all p-3.5 flex items-center gap-4 group"
          >
            <div className="flex-shrink-0 w-[124px] h-[70px] rounded-xl overflow-hidden bg-ink-900 dark:bg-amoled relative">
              <img
                src={item.thumbnail}
                alt=""
                loading="lazy"
                className="w-full h-full object-cover"
                onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
              />
              <span className="absolute bottom-1.5 right-1.5 flex items-center justify-center w-6 h-6 rounded-lg bg-black/80 border border-white/15">
                <Youtube className="w-3.5 h-3.5 text-amber-brand" />
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="text-sm font-extrabold text-ink-900 dark:text-creamtext leading-snug line-clamp-2">
                {item.title}
              </h3>
              <p className="text-[11px] font-bold text-ink-700 dark:text-muted-warm mt-2 inline-flex items-center gap-2 uppercase tracking-[0.1em]">
                {fmtDate(item.date)}
                <span className="inline-flex items-center gap-1">
                  <Eye className="w-3 h-3" />
                  {fmtViews(item.views)}
                </span>
              </p>
            </div>
            <span className="text-[9px] font-black uppercase tracking-[0.14em] text-ink-700/50 dark:text-muted-warm/50 group-hover:text-ember dark:group-hover:text-amber-brand transition-colors">
              Watch
            </span>
          </motion.button>
        ))}

        <p className="text-center text-[10px] font-bold uppercase tracking-[0.16em] text-ink-700/70 dark:text-muted-warm/70 pt-8 leading-relaxed">
          Latest English editions from {data.channel.title}
          <br />
          Refreshed every Saturday night · opens in YouTube
        </p>
      </div>
    </div>
  );
}
