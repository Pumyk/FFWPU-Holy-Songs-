import { useEffect, useMemo, useRef, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  ChevronLeft, ZoomIn, ZoomOut, RotateCcw, Search, X, Loader2, BookOpen,
} from 'lucide-react';
import type { PDFDocumentProxy } from 'pdfjs-dist';

const TAP = { type: 'spring', stiffness: 700, damping: 26 } as const;

const MIN_ZOOM = 0.6;
const MAX_ZOOM = 3;

interface PageDims { width: number; height: number }

interface FindHit { page: number; snippet: string }

export default function PdfReader({
  src, title, author, progressKey, onClose,
}: {
  src: string;
  title: string;
  author: string;
  progressKey?: string;
  onClose: () => void;
}) {
  const storeKey = `hphc:pdf-progress:${progressKey ?? src}`;
  const [doc, setDoc] = useState<PDFDocumentProxy | null>(null);
  const [loadError, setLoadError] = useState(false);
  const [dims, setDims] = useState<PageDims[]>([]);
  const dimsRef = useRef<PageDims[]>([]);
  useEffect(() => { dimsRef.current = dims; }, [dims]);
  const [scale, setScale] = useState(() => {
    const z = Number(localStorage.getItem(storeKey + ':zoom'));
    return z >= MIN_ZOOM && z <= MAX_ZOOM ? z : 1;
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [findOpen, setFindOpen] = useState(false);
  const [findQuery, setFindQuery] = useState('');
  const [indexing, setIndexing] = useState(false);
  const [hits, setHits] = useState<FindHit[] | null>(null);

  const containerRef = useRef<HTMLDivElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const pageRefs = useRef<Map<number, HTMLDivElement>>(new Map());
  const canvasRefs = useRef<Map<number, HTMLCanvasElement>>(new Map());
  const renderedAt = useRef<Map<number, number>>(new Map()); // page -> rendered scale
  const pageTextCache = useRef<Map<number, string>>(new Map());
  const findInputRef = useRef<HTMLInputElement>(null);
  const observerRef = useRef<IntersectionObserver | null>(null);
  const pdfjsRef = useRef<typeof import('pdfjs-dist') | null>(null);
  const resumedRef = useRef(false);
  const pinchDist = useRef<number | null>(null);

  /* ------------------------------ load document ---------------------------- */
  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const pdfjs = await import('pdfjs-dist');
        pdfjs.GlobalWorkerOptions.workerSrc = new URL(
          'pdfjs-dist/build/pdf.worker.min.mjs',
          import.meta.url,
        ).toString();
        if (!alive) return;
        pdfjsRef.current = pdfjs;
        const d = await pdfjs.getDocument({
          url: src,
          standardFontDataUrl: new URL('./pdfjs/standard_fonts/', document.baseURI).toString(),
          useSystemFonts: false,
        }).promise;
        if (!alive) { void d.destroy(); return; }
        setDoc(d);
      } catch (e) {
        console.error('pdf load failed', e);
        if (alive) setLoadError(true);
      }
    })();
    return () => { alive = false; };
  }, [src]);

  /* --------------------- first-page dims + resume position ----------------- */
  useEffect(() => {
    if (!doc) return;
    let alive = true;
    (async () => {
      const p = await doc.getPage(1);
      const v = p.getViewport({ scale: 1 });
      if (!alive) return;
      setDims([{ width: v.width, height: v.height }]);

      // resume last position (page + zoom persisted in localStorage)
      const saved = Number(localStorage.getItem(storeKey + ':page'));
      if (Number.isFinite(saved) && saved > 1 && saved <= doc.numPages) {
        requestAnimationFrame(() => {
          const sc = scrollRef.current;
          if (!sc) { resumedRef.current = true; return; }
          const avg = (sc.scrollHeight || 0) / doc.numPages;
          sc.scrollTop = Math.max(0, (saved - 1) * avg);
          setCurrentPage(saved);
          resumedRef.current = true;
        });
      } else {
        resumedRef.current = true;
      }
    })();
    return () => { alive = false; };
  }, [doc, storeKey]);

  /* ------------------------------ render pages ----------------------------- */
  const renderPage = useCallback(async (i: number) => {
    if (!doc) return;
    const canvas = canvasRefs.current.get(i);
    const wrap = pageRefs.current.get(i);
    if (!canvas || !wrap) return;
    const at = renderedAt.current.get(i);
    if (at === scale) return;
    renderedAt.current.set(i, scale);
    try {
      const page = await doc.getPage(i);
      const v1 = page.getViewport({ scale: 1 });
      const cur = dimsRef.current[i - 1];
      if (!cur || Math.abs(cur.height - v1.height) > 2 || Math.abs(cur.width - v1.width) > 2) {
        setDims((prev) => {
          const next = prev.length >= i ? [...prev] : Array.from({ length: i }, (_, k) => prev[k] ?? prev[0]);
          next[i - 1] = { width: v1.width, height: v1.height };
          return next;
        });
      }
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const v = page.getViewport({ scale: scale * dpr });
      canvas.width = Math.floor(v.width);
      canvas.height = Math.floor(v.height);
      const ctx = canvas.getContext('2d');
      if (!ctx) return;
      await page.render({ canvasContext: ctx, viewport: v }).promise;
    } catch {
      renderedAt.current.delete(i);
    }
  }, [doc, scale]);

  // re-render on scale change
  useEffect(() => {
    renderedAt.current.clear();
    observerRef.current?.disconnect();
    if (!doc || dims.length === 0) return;
    const io = new IntersectionObserver((entries) => {
      for (const en of entries) {
        if (en.isIntersecting) {
          const i = Number((en.target as HTMLElement).dataset.page);
          void renderPage(i);
        }
      }
    }, { root: scrollRef.current, rootMargin: '1200px 0px' });
    observerRef.current = io;
    pageRefs.current.forEach((el) => io.observe(el));
    return () => io.disconnect();
  }, [doc, dims, scale, renderPage]);

  /* ------------------------------ progress saving -------------------------- */
  useEffect(() => {
    if (!doc || !resumedRef.current) return;
    localStorage.setItem(storeKey + ':page', String(currentPage));
  }, [currentPage, doc, storeKey]);
  useEffect(() => {
    localStorage.setItem(storeKey + ':zoom', String(scale));
  }, [scale, storeKey]);

  /* ------------------------------ page tracking ---------------------------- */
  useEffect(() => {
    const sc = scrollRef.current;
    if (!sc) return;
    let raf = 0;
    const onScroll = () => {
      cancelAnimationFrame(raf);
      raf = requestAnimationFrame(() => {
        const mid = sc.scrollTop + sc.clientHeight / 2;
        const els = [...pageRefs.current.values()];
        for (const el of els) {
          const top = el.offsetTop;
          if (mid >= top && mid <= top + el.offsetHeight) {
            setCurrentPage(Number(el.dataset.page));
            break;
          }
        }
      });
    };
    sc.addEventListener('scroll', onScroll, { passive: true });
    return () => { sc.removeEventListener('scroll', onScroll); cancelAnimationFrame(raf); };
  }, [dims]);

  /* ------------------------------ pinch to zoom ---------------------------- */
  useEffect(() => {
    const sc = scrollRef.current;
    if (!sc) return;
    const onTouchStart = (e: TouchEvent) => {
      if (e.touches.length === 2) {
        pinchDist.current = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY,
        );
      }
    };
    const onTouchMove = (e: TouchEvent) => {
      if (e.touches.length === 2 && pinchDist.current) {
        const d = Math.hypot(
          e.touches[0].clientX - e.touches[1].clientX,
          e.touches[0].clientY - e.touches[1].clientY,
        );
        setScale((s) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, +(s * (d / pinchDist.current!)).toFixed(2))));
      }
    };
    const onTouchEnd = () => { pinchDist.current = null; };
    sc.addEventListener('touchstart', onTouchStart, { passive: true });
    sc.addEventListener('touchmove', onTouchMove, { passive: true });
    sc.addEventListener('touchend', onTouchEnd);
    return () => {
      sc.removeEventListener('touchstart', onTouchStart);
      sc.removeEventListener('touchmove', onTouchMove);
      sc.removeEventListener('touchend', onTouchEnd);
    };
  }, []);

  /* ------------------------------ find (fast index) ------------------------- */
  const pageText = useCallback(async (i: number): Promise<string> => {
    const c = pageTextCache.current.get(i);
    if (c != null) return c;
    if (!doc) return '';
    const page = await doc.getPage(i);
    const tc = await page.getTextContent();
    let t = '';
    for (const item of tc.items as Array<{ str: string; hasEOL?: boolean }>) {
      t += item.str + (item.hasEOL ? '\n' : ' ');
    }
    pageTextCache.current.set(i, t);
    return t;
  }, [doc]);

  const runFind = useCallback(async (q: string) => {
    if (!doc || q.trim().length < 2) { setHits(null); return; }
    setIndexing(true);
    setHits([]);
    const needle = q.trim().toLowerCase();
    const out: FindHit[] = [];
    const BATCH = 24;
    for (let i = 1; i <= doc.numPages; i += BATCH) {
      const texts = await Promise.all(
        Array.from({ length: Math.min(BATCH, doc.numPages - i + 1) }, (_, k) => pageText(i + k)),
      );
      texts.forEach((t, k) => {
        const low = t.toLowerCase();
        let idx = low.indexOf(needle);
        let found = 0;
        while (idx >= 0 && found < 3) {
          out.push({
            page: i + k,
            snippet: t.slice(Math.max(0, idx - 60), idx + needle.length + 90).replace(/\s+/g, ' '),
          });
          found++;
          idx = low.indexOf(needle, idx + needle.length);
        }
      });
      setHits([...out].slice(0, 100));
      if (out.length >= 100) break;
    }
    setIndexing(false);
  }, [doc, pageText]);

  useEffect(() => {
    const t = setTimeout(() => { void runFind(findQuery); }, findQuery ? 350 : 0);
    return () => clearTimeout(t);
  }, [findQuery, runFind]);

  const scrollToPage = (i: number) => {
    const el = pageRefs.current.get(i);
    if (el && scrollRef.current) {
      scrollRef.current.scrollTo({ top: el.offsetTop - 8, behavior: 'smooth' });
      setCurrentPage(i);
    }
  };

  /* ------------------------------ helpers ----------------------------------- */
  const goPage = (delta: number) => scrollToPage(Math.min(doc?.numPages ?? 1, Math.max(1, currentPage + delta)));
  const changeZoom = (d: number) => setScale((z) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, +(z + d).toFixed(2))));
  const firstDims = dims[0] ?? { width: 612, height: 792 };

  /* ------------------------------ render ----------------------------------- */
  return (
    <div className="fixed inset-0 z-30 bg-cream-50 dark:bg-amoled flex flex-col pt-[env(safe-area-inset-top)]">
      {/* chrome */}
      <div className="border-b border-cream-200/60 dark:border-white/5 bg-cream-50 dark:bg-amoled px-4 py-2 flex-shrink-0">
        <div className="max-w-2xl mx-auto flex items-center gap-2">
          <motion.button
            whileTap={{ scale: 0.9 }} transition={TAP}
            onClick={onClose}
            className="flex-shrink-0 flex items-center gap-1.5 py-2 px-3 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-xs font-black text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700 transition-all"
            aria-label="Back to books"
          >
            <ChevronLeft className="w-4 h-4" /> Books
          </motion.button>
          <div className="flex-1 min-w-0 text-center">
            <p className="text-[9px] font-black uppercase tracking-[0.18em] text-ink-700 dark:text-muted-warm truncate">{author}</p>
            <p className="text-xs font-extrabold text-ink-900 dark:text-creamtext truncate">{title}</p>
          </div>
          <motion.button
            whileTap={{ scale: 0.88 }} transition={TAP} onClick={() => { setFindOpen(true); requestAnimationFrame(() => findInputRef.current?.focus()); }}
            className="p-2.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700"
            aria-label="Find in document"
          >
            <Search className="w-[18px] h-[18px]" />
          </motion.button>
        </div>
        {/* slim zoom row */}
        <div className="max-w-2xl mx-auto flex items-center justify-center gap-1 pt-2">
          <button onClick={() => changeZoom(-0.2)} disabled={scale <= MIN_ZOOM}
            className="p-1.5 rounded-full text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800 disabled:opacity-25 transition-colors" aria-label="Zoom out">
            <ZoomOut className="w-4 h-4" />
          </button>
          <span className="text-[11px] font-black font-mono text-ink-900 dark:text-creamtext w-11 text-center tabular-nums">{Math.round(scale * 100)}%</span>
          <button onClick={() => changeZoom(0.2)} disabled={scale >= MAX_ZOOM}
            className="p-1.5 rounded-full text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800 disabled:opacity-25 transition-colors" aria-label="Zoom in">
            <ZoomIn className="w-4 h-4" />
          </button>
          <button onClick={() => setScale(1)} disabled={scale === 1}
            className="p-1.5 rounded-full text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800 disabled:opacity-25 transition-colors" aria-label="Reset zoom">
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* loading / error */}
      {!doc && !loadError && (
        <div className="flex-1 flex flex-col items-center justify-center gap-4 text-ink-700 dark:text-muted-warm">
          <Loader2 className="w-8 h-8 animate-spin text-amber-brand" />
          <p className="text-sm font-bold">Opening PDF…</p>
        </div>
      )}
      {loadError && (
        <div className="flex-1 flex flex-col items-center justify-center gap-4 text-ink-700 dark:text-muted-warm px-6 text-center">
          <p className="text-sm font-bold">Could not open this PDF.</p>
          <button onClick={onClose} className="px-5 py-2.5 rounded-full bg-amber-brand text-black text-sm font-bold">Go back</button>
        </div>
      )}

      {/* pages */}
      {doc && (
        <div ref={scrollRef} className="flex-1 overflow-y-auto overscroll-contain">
          <div className="max-w-2xl mx-auto px-4 py-4 space-y-4 pb-[calc(2rem+env(safe-area-inset-bottom))]">
            {Array.from({ length: doc.numPages }, (_, k) => k + 1).map((i) => {
              const d = dims[i - 1] ?? firstDims;
              return (
                <div
                  key={i}
                  data-page={i}
                  ref={(el) => { if (el) pageRefs.current.set(i, el); else pageRefs.current.delete(i); }}
                  style={{ aspectRatio: `${d.width} / ${d.height}` }}
                  className="relative w-full bg-white dark:bg-night-800 rounded-lg overflow-hidden shadow-md border border-cream-200 dark:border-white/[0.04]"
                >
                  <canvas
                    ref={(el) => { if (el) canvasRefs.current.set(i, el); else canvasRefs.current.delete(i); }}
                    className="w-full h-full block"
                    style={{ touchAction: 'pan-y' }}
                  />
                  {!renderedAt.current.has(i) && (
                    <div className="absolute inset-0 flex items-center justify-center">
                      <span className="text-[10px] font-black uppercase tracking-[0.18em] text-ink-700/50 dark:text-muted-warm/50">
                        Page {i}
                      </span>
                    </div>
                  )}
                </div>
              );
            })}
            <p className="text-center text-[10px] font-bold uppercase tracking-[0.18em] text-ink-700/60 dark:text-muted-warm/60 pb-2 flex items-center justify-center gap-1.5">
              <BookOpen className="w-3 h-3" /> End of document · {doc.numPages} pages
            </p>
          </div>
        </div>
      )}

      {/* page counter + page nav, in-flow bottom (reserved space keeps it off text) */}
      {doc && (
        <div className="border-t border-cream-200/60 dark:border-white/5 bg-cream-50 dark:bg-amoled px-4 pt-2 pb-[calc(0.5rem+env(safe-area-inset-bottom))] flex-shrink-0">
          <div className="max-w-2xl mx-auto flex items-center justify-center gap-3">
            <button onClick={() => goPage(-1)} disabled={currentPage <= 1}
              className="p-2 rounded-full text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800 disabled:opacity-25" aria-label="Previous page">
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="text-xs font-black font-mono text-ink-900 dark:text-creamtext tabular-nums">
              {currentPage} / {doc.numPages}
            </span>
            <button onClick={() => goPage(1)} disabled={currentPage >= doc.numPages}
              className="p-2 rounded-full text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800 disabled:opacity-25" aria-label="Next page">
              <ChevronLeft className="w-4 h-4 rotate-180" />
            </button>
          </div>
        </div>
      )}

      {/* find overlay */}
      <AnimatePresence>
        {findOpen && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} transition={{ duration: 0.12 }}
            className="fixed inset-0 z-40 bg-cream-50 dark:bg-amoled overflow-y-auto px-6 pt-8 pb-10"
          >
            <div className="max-w-2xl mx-auto">
              <div className="flex items-center justify-between gap-3 mb-6">
                <h3 className="text-2xl font-black text-ink-900 dark:text-creamtext tracking-tight">Find</h3>
                <motion.button whileTap={{ scale: 0.88 }} transition={TAP} onClick={() => setFindOpen(false)}
                  className="p-3 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext" aria-label="Close find">
                  <X className="w-5 h-5" />
                </motion.button>
              </div>
              <input
                ref={findInputRef}
                value={findQuery}
                onChange={(e) => setFindQuery(e.target.value)}
                placeholder={`Search ${title}…`}
                className="w-full rounded-full bg-white dark:bg-night-800 border border-cream-200 dark:border-white/10 px-5 py-3.5 text-sm font-semibold text-ink-900 dark:text-creamtext placeholder:text-ink-700/60 dark:placeholder:text-muted-warm/60 focus:outline-none focus:border-amber-brand/50"
              />
              <p className="mt-3 text-[10px] font-black uppercase tracking-[0.18em] text-ink-700/70 dark:text-muted-warm/70">
                {findQuery.trim().length < 2
                  ? 'Type at least two letters'
                  : indexing
                    ? 'Indexing…'
                    : `${hits?.length ?? 0} match${hits?.length === 1 ? '' : 'es'}${(hits?.length ?? 0) >= 100 ? '+' : ''}`}
              </p>
              <div className="mt-5 space-y-2">
                {(hits ?? []).map((h, i) => (
                  <button key={i} onClick={() => { scrollToPage(h.page); setFindOpen(false); }}
                    className="w-full text-left rounded-2xl bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] hover:border-amber-brand/40 p-4 transition-all">
                    <p className="text-[10px] font-black uppercase tracking-[0.16em] text-ember dark:text-amber-brand mb-1.5">
                      Page {h.page}
                    </p>
                    <p className="text-[13px] text-ink-900 dark:text-creamtext leading-relaxed">…{h.snippet}…</p>
                  </button>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
