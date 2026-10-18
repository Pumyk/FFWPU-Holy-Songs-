import { useEffect, useRef, useState, useMemo, type TouchEvent as ReactTouchEvent } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Library, BookOpen, ChevronLeft, ChevronRight, FileText, ListTree, Maximize2, Minimize2, ZoomIn, ZoomOut, RotateCcw, X } from 'lucide-react';
import exposition from '../data/books/exposition.json';

const TAP = { type: 'spring', stiffness: 700, damping: 26 } as const;
const FAST = { duration: 0.16, ease: [0.2, 0, 0, 1] } as const;

interface BookBlock {
  type: 'h1' | 'h2' | 'p';
  text: string;
}

interface BookChapter {
  id: string;
  title: string;
  blocks: BookBlock[];
}

interface Book {
  slug: string;
  title: string;
  author: string;
  source: string;
  chapters: BookChapter[];
}

/* The book (~1 MB) is code-split: it loads only when the Books section opens,
   keeping songbook startup instant. */
function useBook(): Book | null {
  const [book, setBook] = useState<Book | null>(null);
  useEffect(() => {
    let alive = true;
    import('../data/books/exposition.json').then(
      (m) => { if (alive) setBook(m.default as unknown as Book); },
      () => {},
    );
    return () => { alive = false; };
  }, []);
  return book;
}

/* ------------------------------ Part grouping ------------------------------ */

interface ChapterGroup {
  label: string;
  chapters: number[]; // indices into book.chapters
}

const buildGroups = (book: Book): ChapterGroup[] => {
  const groups: ChapterGroup[] = [
    { label: 'Front Matter', chapters: [] },
    { label: 'Part 1', chapters: [] },
    { label: 'Part 2', chapters: [] },
  ];
  book.chapters.forEach((c, i) => {
    if (c.title.startsWith('Part 2')) groups[2].chapters.push(i);
    else if (c.title.startsWith('Part 1') || c.title === 'Introduction') groups[1].chapters.push(i);
    else groups[0].chapters.push(i);
  });
  return groups.filter((g) => g.chapters.length > 0);
};

/** 'Part 1 · Chapter 1: The Principle of Creation' -> 'Ch 1 · The Principle of Creation' */
const chapterShort = (t: string) =>
  t.startsWith('Part ')
    ? t.split(' · ')[1].replace(/^Chapter /, 'Ch ')
    : t;

/** group label for the chapter currently open */
const groupOf = (book: Book, groups: ChapterGroup[], idx: number) =>
  groups.find((g) => g.chapters.includes(idx));

/* --------------------------------- Shelf --------------------------------- */

function Shelf({ book, onOpen }: { book: Book | null; onOpen: () => void }) {
  return (
    <div className="pb-[calc(6rem+env(safe-area-inset-bottom))]">
      <div className="px-5 md:px-8 pt-10 md:pt-16 max-w-2xl mx-auto relative">
        <div
          className="absolute -top-4 left-1/2 -translate-x-1/2 w-80 h-44 pointer-events-none"
          style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 18%, transparent), transparent)' }}
        />
        <div className="relative">
          <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-brand/15 dark:bg-amber-brand/10 text-[10px] font-black tracking-[0.18em] uppercase text-ember dark:text-amber-brand mb-6">
            <Library className="w-3 h-3" />
            Core teachings
          </span>
          <h2 className="text-4xl md:text-6xl font-black text-ink-900 dark:text-creamtext mb-4 leading-[1.02] tracking-tight">
            Books
          </h2>
          <p className="text-sm font-bold text-ink-700 dark:text-muted-warm uppercase tracking-[0.15em]">
            Offline reading library
          </p>
        </div>
      </div>

      <div className="px-5 md:px-8 max-w-2xl mx-auto mt-10 space-y-4">
        <motion.button
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={FAST}
          whileTap={{ scale: 0.98 }}
          onClick={onOpen}
          className="w-full text-left rounded-[28px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] hover:border-amber-brand/40 hover:shadow-lg hover:shadow-amber-brand/5 dark:hover:bg-night-700/60 transition-all p-6 flex items-center gap-5 group"
        >
          <div className="flex-shrink-0 w-16 h-20 rounded-2xl bg-ink-900 dark:bg-amoled border border-white/10 flex items-center justify-center shadow-lg group-hover:bg-amber-brand transition-colors">
            <FileText className="w-7 h-7 text-amber-brand group-hover:text-black transition-colors" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-extrabold text-ink-900 dark:text-creamtext leading-snug">
              Exposition of the Divine Principle
            </h3>
            <p className="text-xs font-bold text-ink-700 dark:text-muted-warm mt-1.5 uppercase tracking-[0.12em]">
              Hyo Won Eu · 1996 authorized translation
            </p>
            <p className="text-xs font-semibold text-ink-700 dark:text-muted-warm mt-2">
              {book ? '2 parts · 17 chapters · paged reader' : 'Loading…'}
            </p>
          </div>
          <ChevronRight className="w-5 h-5 flex-shrink-0 text-ember/50 dark:text-amber-brand/50 group-hover:text-ember dark:group-hover:text-amber-brand group-hover:translate-x-0.5 transition-all" />
        </motion.button>

        <div className="rounded-[28px] border border-dashed border-cream-200 dark:border-white/[0.08] p-6 text-center">
          <p className="text-sm font-semibold text-ink-700 dark:text-muted-warm">
            More titles will be added in future updates.
          </p>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------ Paged reader ------------------------------ */

const MIN_ZOOM = 0.85;
const MAX_ZOOM = 1.7;
const PAGE_PAD = 20; // px breathing room on each side of a page

function Reader({ book, onClose }: { book: Book; onClose: () => void }) {
  const [chapter, setChapter] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [immersive, setImmersive] = useState(false);
  const [contentsOpen, setContentsOpen] = useState(false);
  const [pageNum, setPageNum] = useState(1);
  const [pageCount, setPageCount] = useState(1);

  const chromeRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const pagerRef = useRef<HTMLDivElement>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  const [pagerH, setPagerH] = useState(0);
  const settleTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const groups = useMemo(() => buildGroups(book), [book]);
  const ch = book.chapters[chapter];
  const grp = groupOf(book, groups, chapter);

  const pinchState = useRef<{ dist: number; zoom: number } | null>(null);
  const touchDist = (t: ReactTouchEvent['touches']) =>
    Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);

  /* ---- layout: pager fills the remaining viewport, pages via CSS columns ---- */
  const recompute = () => {
    const pager = pagerRef.current;
    if (!pager) return;
    const top = chromeRef.current ? chromeRef.current.getBoundingClientRect().bottom : 64;
    const barH = bottomRef.current ? bottomRef.current.getBoundingClientRect().height : 64;
    const h = Math.max(220, window.innerHeight - top - barH - 20);
    setPagerH(h);
    const w = pager.clientWidth || window.innerWidth;
    pager.scrollTo({ left: 0 });
    requestAnimationFrame(() => {
      const cols = contentRef.current;
      if (!cols) return;
      cols.style.columnWidth = `${w - PAGE_PAD * 2}px`;
      cols.style.columnGap = `${PAGE_PAD * 2}px`;
      const total = Math.max(1, Math.ceil((pager.scrollWidth - w) / w) + 1);
      setPageCount(total);
      setPageNum(1);
    });
  };

  useEffect(() => {
    recompute();
    const onResize = () => recompute();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chapter, book]);

  // reflow after zoom change
  useEffect(() => {
    const t = setTimeout(recompute, 50);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [zoom, immersive, pagerH]);

  // immersive mode hides the app header
  useEffect(() => {
    document.documentElement.classList.toggle('reader-immersive', immersive);
    return () => document.documentElement.classList.remove('reader-immersive');
  }, [immersive]);

  const pageWidth = () => pagerRef.current?.clientWidth || window.innerWidth;

  const goPage = (n: number) => {
    const pager = pagerRef.current;
    if (!pager) return;
    const w = pageWidth();
    const target = Math.max(0, Math.min(pageCount - 1, n - 1));
    pager.scrollTo({ left: target * w, behavior: 'smooth' });
    setPageNum(target + 1);
  };

  const nextPage = () => {
    if (pageNum < pageCount) goPage(pageNum + 1);
    else if (chapter < book.chapters.length - 1) setChapter(chapter + 1);
  };
  const prevPage = () => {
    if (pageNum > 1) goPage(pageNum - 1);
    else if (chapter > 0) setChapter(chapter - 1);
  };

  const onScroll = () => {
    const pager = pagerRef.current;
    if (!pager) return;
    const w = pageWidth();
    const current = Math.round(pager.scrollLeft / w) + 1;
    if (current !== pageNum) setPageNum(Math.min(current, pageCount));
    if (settleTimer.current) clearTimeout(settleTimer.current);
    settleTimer.current = setTimeout(() => {
      const snap = Math.round(pager.scrollLeft / w);
      if (Math.abs(pager.scrollLeft - snap * w) > 2) {
        pager.scrollTo({ left: snap * w, behavior: 'smooth' });
      }
      setPageNum(Math.min(snap + 1, pageCount));
    }, 140);
  };

  const changeZoom = (delta: number) =>
    setZoom((z) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, +(z + delta).toFixed(2))));

  const goChapter = (i: number) => {
    setContentsOpen(false);
    if (i >= 0 && i < book.chapters.length) setChapter(i);
  };

  return (
    <div className="relative">
      {/* Chrome bar */}
      <div ref={chromeRef} className="sticky top-0 z-30 bg-cream-50 dark:bg-amoled pt-[calc(env(safe-area-inset-top)+0.5rem)] pb-3 px-5">
        <div className="max-w-2xl mx-auto flex items-center gap-2">
          <motion.button
            whileTap={{ scale: 0.9 }}
            transition={TAP}
            onClick={() => { setImmersive(false); onClose(); }}
            className="flex-shrink-0 flex items-center gap-1.5 py-2 px-3 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-xs font-black text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700 transition-all"
            aria-label="Back to books"
          >
            <ChevronLeft className="w-4 h-4" />
            Books
          </motion.button>
          <div className="flex-1 min-w-0 text-center">
            <p className="text-[9px] font-black uppercase tracking-[0.2em] text-ink-700 dark:text-muted-warm truncate">
              {grp?.label ?? ''}
            </p>
            <p className="text-xs font-extrabold text-ink-900 dark:text-creamtext truncate">
              {chapterShort(ch.title)}
            </p>
          </div>
          <motion.button
            whileTap={{ scale: 0.88 }}
            transition={TAP}
            onClick={() => setContentsOpen(true)}
            className="flex-shrink-0 p-2.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700"
            aria-label="Contents"
          >
            <ListTree className="w-[18px] h-[18px]" />
          </motion.button>
          <motion.button
            whileTap={{ scale: 0.88 }}
            transition={TAP}
            onClick={() => setImmersive((v) => !v)}
            className="flex-shrink-0 p-2.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700"
            aria-label={immersive ? 'Exit fullscreen' : 'Fullscreen'}
          >
            {immersive ? <Minimize2 className="w-[18px] h-[18px]" /> : <Maximize2 className="w-[18px] h-[18px]" />}
          </motion.button>
        </div>
      </div>

      {/* Paged text (CSS columns = pages; horizontal scroll = page turns) */}
      <div
        ref={pagerRef}
        onScroll={onScroll}
        onTouchStart={(e) => { if (e.touches.length === 2) pinchState.current = { dist: touchDist(e.touches), zoom }; }}
        onTouchMove={(e) => {
          if (e.touches.length === 2 && pinchState.current) {
            const d = touchDist(e.touches);
            setZoom(Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, pinchState.current.zoom * (d / pinchState.current.dist))));
          }
        }}
        className="overflow-x-auto overflow-y-hidden reader-pager"
        style={{ height: pagerH ? `${pagerH}px` : '60vh' }}
      >
        <div
          ref={contentRef}
          style={{ fontSize: `${1.02 * zoom}rem`, paddingLeft: PAGE_PAD }}
          className="h-full font-serif text-ink-900 dark:text-creamtext reader-columns"
        >
          {ch.blocks.map((b, i) =>
            b.type === 'p' ? (
              <p key={i} style={{ lineHeight: 1.75, marginBottom: '1.1em', textAlign: 'justify', hyphens: 'auto' }}>
                {b.text}
              </p>
            ) : (
              <h3
                key={i}
                style={{ fontSize: b.type === 'h1' ? `${1.5 * zoom}em` : `${1.22 * zoom}em` }}
                className={`font-black pt-6 pb-1 text-ember dark:text-amber-brand ${i === 0 ? 'mt-0' : ''}`}
              >
                {b.text}
              </h3>
            )
          )}
        </div>
      </div>

      {/* Bottom bar: page turner */}
      <div ref={bottomRef} className="sticky bottom-[calc(1rem+env(safe-area-inset-bottom))] z-20 px-5 pb-[env(safe-area-inset-bottom)]">
        <div className="max-w-2xl mx-auto flex items-center gap-3 p-2 rounded-[28px] bg-ink-900 dark:bg-night-800 border border-white/10 shadow-2xl shadow-black/40">
          <motion.button
            whileTap={{ scale: 0.95 }}
            transition={TAP}
            onClick={prevPage}
            className="flex-1 flex items-center justify-center gap-2 py-3 rounded-[22px] text-cream-100 font-bold text-sm hover:bg-white/10 transition-all"
            aria-label="Previous page"
          >
            <ChevronLeft className="w-4 h-4" />
          </motion.button>
          <div className="flex items-center gap-2 px-2">
            <div className="flex items-center gap-1 rounded-full bg-white/10 px-1 py-1">
              <motion.button
                whileTap={{ scale: 0.85 }}
                transition={TAP}
                onClick={() => changeZoom(-0.15)}
                disabled={zoom <= MIN_ZOOM}
                className="p-2 rounded-full text-cream-100 hover:bg-white/10 disabled:opacity-25"
                aria-label="Zoom out"
              >
                <ZoomOut className="w-4 h-4" />
              </motion.button>
              <span className="text-[11px] font-black font-mono text-amber-brand w-11 text-center tabular-nums">
                {Math.round(zoom * 100)}%
              </span>
              <motion.button
                whileTap={{ scale: 0.85 }}
                transition={TAP}
                onClick={() => changeZoom(0.15)}
                disabled={zoom >= MAX_ZOOM}
                className="p-2 rounded-full text-cream-100 hover:bg-white/10 disabled:opacity-25"
                aria-label="Zoom in"
              >
                <ZoomIn className="w-4 h-4" />
              </motion.button>
              <motion.button
                whileTap={{ scale: 0.85 }}
                transition={TAP}
                onClick={() => setZoom(1)}
                disabled={zoom === 1}
                className="p-2 rounded-full text-cream-100 hover:bg-white/10 disabled:opacity-25"
                aria-label="Reset zoom"
              >
                <RotateCcw className="w-3.5 h-3.5" />
              </motion.button>
            </div>
            <span className="text-[11px] font-black font-mono text-cream-100/70 tabular-nums whitespace-nowrap">
              {pageNum} / {pageCount}
            </span>
          </div>
          <motion.button
            whileTap={{ scale: 0.95 }}
            transition={TAP}
            onClick={nextPage}
            className="flex-1 flex items-center justify-center gap-2 py-3 rounded-[22px] bg-amber-brand text-black font-bold text-sm hover:bg-amber-brand/90 transition-all"
            aria-label="Next page"
          >
            <ChevronRight className="w-4 h-4" />
          </motion.button>
        </div>
      </div>

      {/* Contents overlay */}
      <AnimatePresence>
        {contentsOpen && (
          <motion.div
            key="contents"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.12 }}
            className="absolute inset-0 z-40 bg-cream-50 dark:bg-amoled overflow-y-auto px-5 pt-8 pb-10"
          >
            <div className="max-w-2xl mx-auto">
              <div className="flex items-center justify-between mb-8">
                <div>
                  <h3 className="text-2xl font-black text-ink-900 dark:text-creamtext tracking-tight">Contents</h3>
                  <p className="text-[10px] font-black uppercase tracking-[0.2em] text-ink-700 dark:text-muted-warm mt-1">
                    Exposition of the Divine Principle
                  </p>
                </div>
                <motion.button
                  whileTap={{ scale: 0.88 }}
                  transition={TAP}
                  onClick={() => setContentsOpen(false)}
                  className="p-3 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext"
                  aria-label="Close contents"
                >
                  <X className="w-5 h-5" />
                </motion.button>
              </div>
              <div className="space-y-10">
                {groups.map((g) => (
                  <div key={g.label}>
                    <div className="flex items-center gap-4 mb-4">
                      <h4 className="text-xs font-black uppercase tracking-[0.28em] text-ember dark:text-amber-brand whitespace-nowrap">
                        {g.label}
                      </h4>
                      <div className="h-px w-full bg-cream-200 dark:bg-white/[0.06]" />
                    </div>
                    <div className="space-y-1.5">
                      {g.chapters.map((i) => {
                        const active = i === chapter;
                        return (
                          <button
                            key={book.chapters[i].id}
                            onClick={() => goChapter(i)}
                            className={`w-full text-left px-4 py-3 rounded-2xl text-sm font-bold transition-all ${
                              active
                                ? 'bg-amber-brand/15 dark:bg-amber-brand/10 text-ink-900 dark:text-creamtext'
                                : 'text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800'
                            }`}
                          >
                            {chapterShort(book.chapters[i].title)}
                            {active && (
                              <span className="float-right text-[10px] font-black uppercase tracking-[0.16em] text-ember dark:text-amber-brand">
                                Reading
                              </span>
                            )}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Chapter attribution (small, below pager flow) */}
      <p className="text-center text-[10px] font-bold uppercase tracking-[0.18em] text-ink-700/60 dark:text-muted-warm/60 pt-3 flex items-center justify-center gap-1.5">
        <BookOpen className="w-3 h-3" />
        Source: {book.source}
      </p>
    </div>
  );
}

export default function BooksPage() {
  const [reading, setReading] = useState(false);
  const book = useBook();
  return reading && book ? (
    <Reader book={book} onClose={() => setReading(false)} />
  ) : (
    <Shelf book={book} onOpen={() => setReading(true)} />
  );
}
