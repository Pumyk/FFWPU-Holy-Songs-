import { motion } from 'motion/react';
import { ScrollText } from 'lucide-react';
import { PLEDGE_VERSES } from '../data/pledge';

const TAP = { type: 'spring', stiffness: 700, damping: 26 } as const;

export default function PledgePage() {
  return (
    <div className="pb-[calc(9rem+env(safe-area-inset-bottom))]">
      {/* Page header */}
      <div className="px-5 md:px-8 pt-10 md:pt-16 max-w-2xl mx-auto relative">
        <div
          className="absolute -top-4 left-1/2 -translate-x-1/2 w-80 h-44 pointer-events-none"
          style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 18%, transparent), transparent)' }}
        />
        <div className="relative">
          <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-brand/15 dark:bg-amber-brand/10 text-[10px] font-black tracking-[0.18em] uppercase text-ember dark:text-amber-brand mb-6">
            <ScrollText className="w-3 h-3" />
            Recited daily by our family
          </span>
          <h2 className="text-4xl md:text-6xl font-black text-ink-900 dark:text-creamtext mb-4 leading-[1.02] tracking-tight">
            Family Pledge
          </h2>
          <p className="text-sm font-bold text-ink-700 dark:text-muted-warm uppercase tracking-[0.15em]">
            Eight pledges · centering on true love
          </p>
        </div>
      </div>

      {/* Verses */}
      <div className="px-5 md:px-8 max-w-2xl mx-auto mt-10 space-y-5">
        {PLEDGE_VERSES.map((verse) => (
          <motion.div
            key={verse.number}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.18 }}
            className="relative rounded-[28px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] p-6 md:p-8 overflow-hidden group"
          >
            <div
              className="absolute -top-10 -right-10 w-44 h-44 pointer-events-none"
              style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 12%, transparent), transparent)' }}
            />
            <div className="relative flex items-start gap-5">
              <div className="flex-shrink-0 flex items-center justify-center w-10 h-10 rounded-full bg-cream-100 dark:bg-black border border-cream-200 dark:border-white/5 text-sm font-black font-mono text-ember dark:text-amber-brand group-hover:bg-amber-brand group-hover:text-black transition-all duration-300">
                {verse.number}
              </div>
              <p className="font-serif font-medium text-[1.02em] leading-[1.8] text-ink-900 dark:text-creamtext whitespace-pre-wrap">
                {verse.text}
              </p>
            </div>
          </motion.div>
        ))}

        <p className="text-center text-[11px] font-bold uppercase tracking-[0.18em] text-ink-700 dark:text-muted-warm pt-6">
          Sincerely recited, our family becomes the owner of Cheon Il Guk
        </p>
      </div>
    </div>
  );
}
