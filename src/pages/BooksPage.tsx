import { useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Library, BookOpen, ChevronLeft, ChevronRight, FileText, ListTree, Maximize2, Minimize2, Search, ZoomIn, ZoomOut, RotateCcw, X } from 'lucide-react';
import exposition from '../data/books/exposition.json';
import PdfReader from './PdfReader';
import { FileUp, Feather } from 'lucide-react';

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

/** 'Part 1 · Chapter 1: The Principle of Creation' -> 'Ch 1: The Principle of Creation' */
const chapterShort = (t: string) =>
  t.startsWith('Part ')
    ? t.split(' · ')[1].replace(/^Chapter /, 'Ch ')
    : t;

/** group label for the chapter currently open */
const groupOf = (groups: ChapterGroup[], idx: number) =>
  groups.find((g) => g.chapters.includes(idx));

/* ------------------------------ Find (fast index) ------------------------- */

interface SearchHit {
  chapter: number;
  block: number;
  snippet: { before: string; match: string; after: string };
}

/* Builds a token -> block index once per book (a few milliseconds),
   so every search afterwards is an instant set intersection. */
function buildIndex(book: Book): Map<string, Set<string>> {
  const idx = new Map<string, Set<string>>();
  book.chapters.forEach((ch, c) =>
    ch.blocks.forEach((b, bi) => {
      const key = `${c}:${bi}`;
      const words = b.text.toLowerCase().match(/[a-z]+/g) ?? [];
      for (const w of words) {
        let set = idx.get(w);
        if (!set) {
          set = new Set();
          idx.set(w, set);
        }
        set.add(key);
      }
    })
  );
  return idx;
}

const snippetAround = (text: string, q: string): SearchHit['snippet'] => {
  const i = text.toLowerCase().indexOf(q.toLowerCase());
  if (i < 0) return { before: text.slice(0, 90), match: '', after: text.slice(90, 160) };
  const start = Math.max(0, i - 70);
  return {
    before: (start > 0 ? '\u2026' : '') + text.slice(start, i),
    match: text.slice(i, i + q.length),
    after: text.slice(i + q.length, i + q.length + 90),
  };
};

/* --------------------------------- Shelf --------------------------------- */

function Shelf({
  book, onOpen, onOpenPdf, onPickDevicePdf,
}: {
  book: Book | null;
  onOpen: () => void;
  onOpenPdf: (src: string, title: string, author: string) => void;
  onPickDevicePdf: () => void;
}) {
  return (
    <div className="pb-[calc(6rem+env(safe-area-inset-bottom))]">
      <div className="px-6 md:px-10 pt-10 md:pt-16 max-w-2xl mx-auto relative">
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

      <div className="px-6 md:px-10 max-w-2xl mx-auto mt-10 space-y-4">
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
              {book ? '2 parts · 17 chapters · full-text search' : 'Loading…'}
            </p>
          </div>
          <ChevronRight className="w-5 h-5 flex-shrink-0 text-ember/50 dark:text-amber-brand/50 group-hover:text-ember dark:group-hover:text-amber-brand group-hover:translate-x-0.5 transition-all" />
        </motion.button>

        <motion.button
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={FAST}
          whileTap={{ scale: 0.98 }}
          onClick={() => onOpenPdf('books/mother-of-peace.pdf', 'Mother of Peace', 'Dr. Hak Ja Han Moon')}
          className="w-full text-left rounded-[28px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] hover:border-amber-brand/40 hover:shadow-lg hover:shadow-amber-brand/5 dark:hover:bg-night-700/60 transition-all p-6 flex items-center gap-5 group"
        >
          <div className="flex-shrink-0 w-16 h-20 rounded-2xl bg-amber-brand/90 border border-black/5 flex items-center justify-center shadow-lg group-hover:bg-amber-brand transition-colors">
            <Feather className="w-7 h-7 text-black" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-extrabold text-ink-900 dark:text-creamtext leading-snug">
              Mother of Peace
            </h3>
            <p className="text-xs font-bold text-ink-700 dark:text-muted-warm mt-1.5 uppercase tracking-[0.12em]">
              Dr. Hak Ja Han Moon · Memoir (2020)
            </p>
            <p className="text-xs font-semibold text-ink-700 dark:text-muted-warm mt-2">
              PDF · 369 pages · in-app PDF reader with search
            </p>
          </div>
          <ChevronRight className="w-5 h-5 flex-shrink-0 text-ember/50 dark:text-amber-brand/50 group-hover:text-ember dark:group-hover:text-amber-brand group-hover:translate-x-0.5 transition-all" />
        </motion.button>

        <motion.button
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={FAST}
          whileTap={{ scale: 0.98 }}
          onClick={() => onOpenPdf('books/way-of-tradition-vol1.pdf', 'The Way of Tradition, Vol. I', 'Rev. Sun Myung Moon')}
          className="w-full text-left rounded-[28px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] hover:border-amber-brand/40 hover:shadow-lg hover:shadow-amber-brand/5 dark:hover:bg-night-700/60 transition-all p-6 flex items-center gap-5 group"
        >
          <div className="flex-shrink-0 w-16 h-20 rounded-2xl bg-ink-900 dark:bg-amoled border border-white/10 flex items-center justify-center shadow-lg group-hover:bg-amber-brand transition-colors">
            <FileText className="w-7 h-7 text-amber-brand group-hover:text-black transition-colors" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="text-lg font-extrabold text-ink-900 dark:text-creamtext leading-snug">
              The Way of Tradition, Volume I
            </h3>
            <p className="text-xs font-bold text-ink-700 dark:text-muted-warm mt-1.5 uppercase tracking-[0.12em]">
              Rev. Sun Myung Moon · Selected passages
            </p>
            <p className="text-xs font-semibold text-ink-700 dark:text-muted-warm mt-2">
              PDF · 315 pages · in-app PDF reader with search
            </p>
          </div>
          <ChevronRight className="w-5 h-5 flex-shrink-0 text-ember/50 dark:text-amber-brand/50 group-hover:text-ember dark:group-hover:text-amber-brand group-hover:translate-x-0.5 transition-all" />
        </motion.button>

        <motion.button
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={FAST}
          whileTap={{ scale: 0.98 }}
          onClick={onPickDevicePdf}
          className="w-full text-left rounded-[28px] border border-dashed border-cream-300 dark:border-white/[0.12] hover:border-amber-brand/50 hover:bg-cream-100/60 dark:hover:bg-night-800/60 transition-all p-5 flex items-center gap-4 group"
        >
          <div className="flex-shrink-0 w-12 h-12 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 flex items-center justify-center group-hover:bg-amber-brand/15">
            <FileUp className="w-5 h-5 text-ember dark:text-amber-brand" />
          </div>
          <div className="flex-1 min-w-0">
            <h3 className="text-sm font-extrabold text-ink-900 dark:text-creamtext leading-snug">
              Open a PDF from your device
            </h3>
            <p className="text-xs font-semibold text-ink-700 dark:text-muted-warm mt-1">
              Read any PDF you have saved, right inside the app
            </p>
          </div>
          <ChevronRight className="w-5 h-5 flex-shrink-0 text-ember/50 dark:text-amber-brand/50 group-hover:text-ember dark:group-hover:text-amber-brand group-hover:translate-x-0.5 transition-all" />
        </motion.button>
      </div>
    </div>
  );
}

/* -------------------------------- Reader ---------------------------------- */

const MIN_ZOOM = 0.85;
const MAX_ZOOM = 1.7;

function Reader({ book, onClose }: { book: Book; onClose: () => void }) {
  const [chapter, setChapter] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [immersive, setImmersive] = useState(false);
  const [contentsOpen, setContentsOpen] = useState(false);
  const [findOpen, setFindOpen] = useState(false);
  const [findQuery, setFindQuery] = useState('');
  const [activeBlock, setActiveBlock] = useState<number | null>(null);
  const pendingScroll = useRef<number | null>(null);
  const blockRefs = useRef<Map<number, HTMLDivElement>>(new Map());
  const findInputRef = useRef<HTMLInputElement>(null);

  const groups = useMemo(() => buildGroups(book), [book]);
  const searchIndex = useMemo(() => buildIndex(book), [book]);
  const ch = book.chapters[chapter];
  const grp = groupOf(groups, chapter);

  // immersive mode hides the app header
  useEffect(() => {
    document.documentElement.classList.toggle('reader-immersive', immersive);
    return () => document.documentElement.classList.remove('reader-immersive');
  }, [immersive]);

  // after a chapter change, jump to a find target if one is pending
  useEffect(() => {
    if (pendingScroll.current != null) {
      const bi = pendingScroll.current;
      pendingScroll.current = null;
      requestAnimationFrame(() => {
        const el = blockRefs.current.get(bi);
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
          setActiveBlock(bi);
          window.setTimeout(() => setActiveBlock((b) => (b === bi ? null : b)), 1400);
        }
      });
    } else {
      window.scrollTo(0, 0);
    }
  }, [chapter]);

  // search over the index: intersection of word -> block sets
  const hits = useMemo<SearchHit[]>(() => {
    const q = findQuery.trim().toLowerCase();
    if (q.length < 2) return [];
    const words = q.match(/[a-z]+/g) ?? [];
    if (words.length === 0) return [];
    let sets = words.map((w) => searchIndex.get(w));
    if (sets.some((s) => !s)) return [];
    let keys = sets[0]!;
    for (const s of sets.slice(1)) keys = new Set([...keys].filter((k) => s!.has(k)));
    const out: SearchHit[] = [];
    for (const key of keys) {
      const [c, b] = key.split(':').map(Number);
      out.push({ chapter: c, block: b, snippet: snippetAround(book.chapters[c].blocks[b].text, findQuery.trim()) });
    }
    out.sort((a, x) => a.chapter - x.chapter || a.block - x.block);
    return out.slice(0, 60);
  }, [findQuery, searchIndex, book]);

  const goToHit = (hit: SearchHit) => {
    setFindOpen(false);
    if (hit.chapter !== chapter) {
      pendingScroll.current = hit.block;
      setChapter(hit.chapter);
    } else {
      const el = blockRefs.current.get(hit.block);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        setActiveBlock(hit.block);
        window.setTimeout(() => setActiveBlock((b) => (b === hit.block ? null : b)), 1400);
      }
    }
  };

  const changeZoom = (delta: number) =>
    setZoom((z) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, +(z + delta).toFixed(2))));

  const goChapter = (i: number) => {
    setContentsOpen(false);
    if (i >= 0 && i < book.chapters.length) setChapter(i);
  };

  const openFind = () => {
    setFindOpen(true);
    requestAnimationFrame(() => findInputRef.current?.focus());
  };

  return (
    <div className="relative">
      {/* Chrome bar: navigation + zoom, sticky so text never scrolls under it */}
      <div
        className={`sticky ${immersive ? 'top-[env(safe-area-inset-top)]' : 'top-[calc(4rem+env(safe-area-inset-top))]'} z-20 bg-cream-50 dark:bg-amoled border-b border-cream-200/60 dark:border-white/5 pt-2 pb-2.5 px-6`}
      >
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
            onClick={openFind}
            className="flex-shrink-0 p-2.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700"
            aria-label="Find in book"
          >
            <Search className="w-[18px] h-[18px]" />
          </motion.button>
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
        {/* Slim zoom row, part of the chrome so it never covers text */}
        <div className="max-w-2xl mx-auto flex items-center justify-center gap-1 pt-2">
          <button
            onClick={() => changeZoom(-0.15)}
            disabled={zoom <= MIN_ZOOM}
            className="p-1.5 rounded-full text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800 disabled:opacity-25 transition-colors"
            aria-label="Zoom out"
          >
            <ZoomOut className="w-4 h-4" />
          </button>
          <span className="text-[11px] font-black font-mono text-ink-900 dark:text-creamtext w-11 text-center tabular-nums">
            {Math.round(zoom * 100)}%
          </span>
          <button
            onClick={() => changeZoom(0.15)}
            disabled={zoom >= MAX_ZOOM}
            className="p-1.5 rounded-full text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800 disabled:opacity-25 transition-colors"
            aria-label="Zoom in"
          >
            <ZoomIn className="w-4 h-4" />
          </button>
          <button
            onClick={() => setZoom(1)}
            disabled={zoom === 1}
            className="p-1.5 rounded-full text-ink-700 dark:text-muted-warm hover:bg-cream-100 dark:hover:bg-night-800 disabled:opacity-25 transition-colors"
            aria-label="Reset zoom"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Chapter text: seamless vertical scroll */}
      <div
        style={{ fontSize: `${1.02 * zoom}rem` }}
        className="px-6 md:px-10 max-w-2xl mx-auto pt-8 pb-6 font-serif text-ink-900 dark:text-creamtext"
      >
        {ch.blocks.map((b, i) => (
          <div
            key={i}
            ref={(el) => {
              if (el) blockRefs.current.set(i, el);
              else blockRefs.current.delete(i);
            }}
            className={`rounded-lg -mx-3 px-3 transition-colors duration-700 ${activeBlock === i ? 'bg-amber-brand/15 dark:bg-amber-brand/10' : ''}`}
          >
            {b.type === 'p' ? (
              <p style={{ lineHeight: 1.85, marginBottom: '1.15em', textAlign: 'justify', hyphens: 'auto' }}>
                {b.text}
              </p>
            ) : (
              <h3
                style={{ fontSize: b.type === 'h1' ? `${1.5 * zoom}em` : `${1.22 * zoom}em` }}
                className={`font-black pt-5 pb-1 text-ember dark:text-amber-brand ${i === 0 ? 'mt-0' : ''}`}
              >
                {b.text}
              </h3>
            )}
          </div>
        ))}

        {/* End-of-chapter navigation */}
        <div className="grid grid-cols-2 gap-3 pt-8 pb-[calc(2rem+env(safe-area-inset-bottom))]">
          <motion.button
            whileTap={{ scale: 0.97 }}
            transition={TAP}
            disabled={chapter === 0}
            onClick={() => setChapter(chapter - 1)}
            className="p-4 rounded-[22px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] text-left disabled:opacity-30 transition-all"
          >
            <span className="text-[9px] font-black uppercase tracking-[0.16em] text-ink-700 dark:text-muted-warm flex items-center gap-1">
              <ChevronLeft className="w-3 h-3" /> Previous
            </span>
            <span className="text-xs font-extrabold text-ink-900 dark:text-creamtext leading-snug line-clamp-2 mt-1.5">
              {chapter > 0 ? chapterShort(book.chapters[chapter - 1].title) : 'Start of book'}
            </span>
          </motion.button>
          <motion.button
            whileTap={{ scale: 0.97 }}
            transition={TAP}
            disabled={chapter >= book.chapters.length - 1}
            onClick={() => setChapter(chapter + 1)}
            className="p-4 rounded-[22px] bg-amber-brand text-black text-right disabled:opacity-30 transition-all"
          >
            <span className="text-[9px] font-black uppercase tracking-[0.16em] flex items-center justify-end gap-1">
              Next <ChevronRight className="w-3 h-3" />
            </span>
            <span className="text-xs font-extrabold leading-snug line-clamp-2 mt-1.5">
              {chapter < book.chapters.length - 1 ? chapterShort(book.chapters[chapter + 1].title) : 'End of book'}
            </span>
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
            className="fixed inset-0 z-40 bg-cream-50 dark:bg-amoled overflow-y-auto px-6 pt-8 pb-10"
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

      {/* Find overlay with fast full-text search */}
      <AnimatePresence>
        {findOpen && (
          <motion.div
            key="find"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.12 }}
            className="fixed inset-0 z-40 bg-cream-50 dark:bg-amoled overflow-y-auto px-6 pt-8 pb-10"
          >
            <div className="max-w-2xl mx-auto">
              <div className="flex items-center justify-between gap-3 mb-6">
                <h3 className="text-2xl font-black text-ink-900 dark:text-creamtext tracking-tight">Find</h3>
                <motion.button
                  whileTap={{ scale: 0.88 }}
                  transition={TAP}
                  onClick={() => setFindOpen(false)}
                  className="p-3 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext"
                  aria-label="Close find"
                >
                  <X className="w-5 h-5" />
                </motion.button>
              </div>
              <input
                ref={findInputRef}
                value={findQuery}
                onChange={(e) => setFindQuery(e.target.value)}
                placeholder="Search the whole book…"
                className="w-full rounded-full bg-white dark:bg-night-800 border border-cream-200 dark:border-white/10 px-5 py-3.5 text-sm font-semibold text-ink-900 dark:text-creamtext placeholder:text-ink-700/60 dark:placeholder:text-muted-warm/60 focus:outline-none focus:border-amber-brand/50"
              />
              <p className="mt-3 text-[10px] font-black uppercase tracking-[0.18em] text-ink-700/70 dark:text-muted-warm/70">
                {findQuery.trim().length < 2
                  ? 'Type at least two letters'
                  : `${hits.length} match${hits.length === 1 ? '' : 'es'}${hits.length >= 60 ? '+' : ''}`}
              </p>
              <div className="mt-5 space-y-2">
                {hits.map((hit, i) => (
                  <button
                    key={`${hit.chapter}:${hit.block}`}
                    onClick={() => goToHit(hit)}
                    className="w-full text-left rounded-2xl bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] hover:border-amber-brand/40 p-4 transition-all"
                  >
                      <p className="text-[10px] font-black uppercase tracking-[0.16em] text-ember dark:text-amber-brand flex items-center gap-1.5 mb-1.5">
                        <FileText className="w-3 h-3" />
                        {chapterShort(book.chapters[hit.chapter].title).slice(0, 44)}
                      </p>
                      <p className="text-[13px] text-ink-900 dark:text-creamtext leading-relaxed font-serif">
                        {hit.snippet.before}
                        <mark className="bg-amber-brand/30 dark:bg-amber-brand/20 rounded px-0.5">{hit.snippet.match}</mark>
                        {hit.snippet.after}
                      </p>
                    </button>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Attribution */}
      <p className="text-center text-[10px] font-bold uppercase tracking-[0.18em] text-ink-700/60 dark:text-muted-warm/60 pb-8 flex items-center justify-center gap-1.5">
        <BookOpen className="w-3 h-3" />
        Source: {book.source}
      </p>
    </div>
  );
}

export default function BooksPage() {
  const [reading, setReading] = useState(false);
  const [pdf, setPdf] = useState<{ src: string; title: string; author: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const book = useBook();

  const handleFile = (e: ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) return;
    setPdf({
      src: URL.createObjectURL(f),
      title: f.name.replace(/\.pdf$/i, ''),
      author: 'From your device',
    });
    e.target.value = '';
  };

  if (pdf) {
    return <PdfReader src={pdf.src} title={pdf.title} author={pdf.author} onClose={() => setPdf(null)} />;
  }
  return reading && book ? (
    <Reader book={book} onClose={() => setReading(false)} />
  ) : (
    <>
      <input ref={fileInputRef} type="file" accept="application/pdf" className="hidden" onChange={handleFile} />
      <Shelf
        book={book}
        onOpen={() => setReading(true)}
        onOpenPdf={(src, title, author) => setPdf({ src, title, author })}
        onPickDevicePdf={() => fileInputRef.current?.click()}
      />
    </>
  );
}
