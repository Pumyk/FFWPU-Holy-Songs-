import { useEffect, useState } from 'react';
import { motion } from 'motion/react';
import { Library, BookOpen, ChevronLeft, ChevronRight, FileText } from 'lucide-react';

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
              {book ? `${book.chapters.length} chapters · fully offline` : 'Loading…'}
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

function Reader({ book, onClose }: { book: Book; onClose: () => void }) {
  const [chapter, setChapter] = useState(0);
  const ch = book.chapters[chapter];

  const goChapter = (i: number) => {
    if (i < 0 || i >= book.chapters.length) return;
    setChapter(i);
    window.scrollTo(0, 0);
  };

  return (
    <div className="pb-[calc(9rem+env(safe-area-inset-bottom))]">
      {/* Chapter chips */}
      <div className="sticky top-[calc(4rem+env(safe-area-inset-top))] z-20 bg-cream-50 dark:bg-amoled -mt-2 pt-3 pb-3">
        <div className="max-w-2xl mx-auto px-5 flex items-center gap-3">
          <motion.button
            whileTap={{ scale: 0.9 }}
            transition={TAP}
            onClick={onClose}
            className="flex-shrink-0 flex items-center gap-1.5 py-2 px-3 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-xs font-black text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700 transition-all"
            aria-label="Back to books"
          >
            <ChevronLeft className="w-4 h-4" />
            Books
          </motion.button>
          <div className="flex-1 overflow-x-auto no-scrollbar">
            <div className="flex items-center gap-1.5 w-max">
              {book.chapters.map((c, i) => (
                <button
                  key={c.id}
                  onClick={() => goChapter(i)}
                  className={`whitespace-nowrap px-3 py-2 rounded-full text-[11px] font-black transition-all ${
                    i === chapter
                      ? 'bg-amber-brand text-black'
                      : 'bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-muted-warm hover:bg-cream-200 dark:hover:bg-night-700'
                  }`}
                >
                  {c.title.split(' · ')[0]}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Chapter text */}
      <div className="px-5 md:px-8 max-w-2xl mx-auto mt-8">
        <h2 className="text-3xl md:text-4xl font-black text-ink-900 dark:text-creamtext leading-[1.1] tracking-tight mb-10">
          {ch.title}
        </h2>

        <div className="font-serif text-ink-900 dark:text-creamtext space-y-6">
          {ch.blocks.map((b, i) =>
            b.type === 'p' ? (
              <p key={i} className="leading-[1.85] tracking-[0.01em] text-[1.02em]">
                {b.text}
              </p>
            ) : (
              <h3
                key={i}
                className={
                  b.type === 'h1'
                    ? 'text-xl font-black pt-6 text-ember dark:text-amber-brand tracking-tight'
                    : 'text-base font-black pt-5 text-ember dark:text-amber-brand tracking-tight'
                }
              >
                {b.text}
              </h3>
            )
          )}
        </div>

        {/* Chapter nav */}
        <div className="sticky bottom-[calc(1.5rem+env(safe-area-inset-bottom))] z-10 mt-14">
          <div className="flex items-center gap-3 p-2 rounded-[28px] bg-ink-900 dark:bg-night-800 border border-white/10 shadow-2xl shadow-black/40">
            <motion.button
              whileTap={{ scale: 0.95 }}
              transition={TAP}
              onClick={() => goChapter(chapter - 1)}
              disabled={chapter === 0}
              className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-[22px] text-cream-100 font-bold text-sm hover:bg-white/10 disabled:opacity-25 disabled:cursor-not-allowed transition-all"
            >
              <ChevronLeft className="w-4 h-4" />
              Previous
            </motion.button>
            <div className="w-px h-8 bg-white/10" />
            <motion.button
              whileTap={{ scale: 0.95 }}
              transition={TAP}
              onClick={() => goChapter(chapter + 1)}
              disabled={chapter >= book.chapters.length - 1}
              className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-[22px] bg-amber-brand text-black font-bold text-sm hover:bg-amber-brand/90 disabled:opacity-25 disabled:cursor-not-allowed transition-all"
            >
              Next chapter
              <ChevronRight className="w-4 h-4" />
            </motion.button>
          </div>
        </div>

        <p className="text-center text-[10px] font-bold uppercase tracking-[0.18em] text-ink-700/60 dark:text-muted-warm/60 pt-10 flex items-center justify-center gap-1.5">
          <BookOpen className="w-3 h-3" />
          Source: {book.source}
        </p>
      </div>
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
