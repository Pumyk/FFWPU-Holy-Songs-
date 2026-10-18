import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { Newspaper, Youtube, CalendarDays, Eye, RefreshCw, ExternalLink, Play, WifiOff } from 'lucide-react';
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
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

const fmtViews = (n: number) =>
  n >= 1000 ? `${(n / 1000).toFixed(1)}K views` : `${n} views`;

export default function NewsPage() {
  const [data, setData] = useState<NewsData>(
    () => loadCached() ?? (bundledNews as unknown as NewsData)
  );
  const [refreshing, setRefreshing] = useState(true);
  const [playing, setPlaying] = useState(false);

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

  const latest = data.items[0];

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

      {/* Latest edition, playable in-app */}
      {latest && (
        <div className="px-5 md:px-8 max-w-2xl mx-auto mt-10">
          <motion.div
            key={latest.id}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={FAST}
            className="rounded-[28px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] overflow-hidden shadow-lg shadow-black/5"
          >
            {/* Player */}
            <div className="relative w-full bg-black" style={{ aspectRatio: '16 / 9' }}>
              {playing ? (
                <iframe
                  key={latest.id}
                  className="absolute inset-0 w-full h-full"
                  src={`https://www.youtube-nocookie.com/embed/${latest.id}?autoplay=1&playsinline=1&rel=0`}
                  title={latest.title}
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
                  allowFullScreen
                />
              ) : (
                <motion.button
                  whileTap={{ scale: 0.98 }}
                  transition={TAP}
                  onClick={() => setPlaying(true)}
                  className="absolute inset-0 w-full h-full group"
                  aria-label="Play in app"
                >
                  <img
                    src={latest.thumbnail.replace('hqdefault', 'maxresdefault')}
                    alt=""
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      const img = e.target as HTMLImageElement;
                      if (!img.dataset.fallback) {
                        img.dataset.fallback = '1';
                        img.src = latest.thumbnail;
                      }
                    }}
                  />
                  <span className="absolute inset-0 bg-black/25 group-hover:bg-black/15 transition-colors" />
                  <span className="absolute inset-0 flex items-center justify-center">
                    <span className="flex items-center justify-center w-16 h-16 rounded-full bg-amber-brand text-black shadow-2xl group-hover:scale-110 transition-transform">
                      <Play className="w-7 h-7 ml-0.5 fill-black" />
                    </span>
                  </span>
                  {!refreshing && navigator.onLine === false && (
                    <span className="absolute top-3 left-3 inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-black/70 text-[10px] font-black uppercase tracking-[0.14em] text-cream-100">
                      <WifiOff className="w-3 h-3" />
                      Offline
                    </span>
                  )}
                </motion.button>
              )}
            </div>

            {/* Meta */}
            <div className="p-6">
              <h3 className="text-lg font-extrabold text-ink-900 dark:text-creamtext leading-snug">
                {latest.title}
              </h3>
              <p className="text-[11px] font-bold text-ink-700 dark:text-muted-warm mt-2.5 inline-flex flex-wrap items-center gap-x-3 uppercase tracking-[0.12em]">
                <span className="inline-flex items-center gap-1.5">
                  <CalendarDays className="w-3 h-3" />
                  {fmtDate(latest.date)}
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Eye className="w-3 h-3" />
                  {fmtViews(latest.views)}
                </span>
              </p>
              <div className="mt-5 flex items-center gap-2">
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-cream-100 dark:bg-night-700 text-[10px] font-black uppercase tracking-[0.14em] text-ink-700 dark:text-muted-warm">
                  <Youtube className="w-3.5 h-3.5 text-ember dark:text-amber-brand" />
                  {data.channel.title}
                </span>
                <a
                  href={latest.url}
                  target="_blank"
                  rel="noreferrer"
                  className="ml-auto inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-cream-200 dark:border-white/10 text-[10px] font-black uppercase tracking-[0.14em] text-ink-700 dark:text-muted-warm hover:text-ember dark:hover:text-amber-brand hover:border-amber-brand/40 transition-colors"
                >
                  <ExternalLink className="w-3 h-3" />
                  YouTube
                </a>
              </div>
            </div>
          </motion.div>

          <p className="text-center text-[10px] font-bold uppercase tracking-[0.16em] text-ink-700/70 dark:text-muted-warm/70 pt-8 leading-relaxed">
            The latest English edition, refreshed every Saturday night
          </p>
        </div>
      )}
    </div>
  );
}
