import { useRef, useState, type TouchEvent as ReactTouchEvent } from 'react';
import { motion } from 'motion/react';
import { ScrollText, ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';
import { PLEDGE_VERSES } from '../data/pledge';

const TAP = { type: 'spring', stiffness: 700, damping: 26 } as const;
const MIN_ZOOM = 1;
const MAX_ZOOM = 2.5;

export default function PledgePage() {
  const [zoom, setZoom] = useState(1);
  const pinchState = useRef<{ dist: number; zoom: number } | null>(null);

  const touchDist = (t: ReactTouchEvent['touches']) =>
    Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);

  const handleTouchStart = (e: ReactTouchEvent) => {
    if (e.touches.length === 2) pinchState.current = { dist: touchDist(e.touches), zoom };
  };

  const handleTouchMove = (e: ReactTouchEvent) => {
    if (e.touches.length === 2 && pinchState.current) {
      const d = touchDist(e.touches);
      const next = pinchState.current.zoom * (d / pinchState.current.dist);
      setZoom(Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, next)));
    }
  };

  const changeZoom = (delta: number) =>
    setZoom((z) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, +(z + delta).toFixed(2))));

  return (
    <div className="pb-[calc(9rem+env(safe-area-inset-bottom))]">
      {/* Page header */}
      <div className="px-7 md:px-10 pt-12 md:pt-16 max-w-xl mx-auto relative">
        <div
          className="absolute -top-4 left-1/2 -translate-x-1/2 w-80 h-44 pointer-events-none"
          style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 18%, transparent), transparent)' }}
        />
        <div className="relative">
          <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-brand/15 dark:bg-amber-brand/10 text-[10px] font-black tracking-[0.18em] uppercase text-ember dark:text-amber-brand mb-7">
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

      {/* Zoom toolbar */}
      <div className="px-7 md:px-10 max-w-xl mx-auto sticky top-[calc(4rem+env(safe-area-inset-top))] z-20 flex justify-end -mt-2 mb-6">
        <div className="flex items-center gap-0.5 rounded-full bg-ink-900 dark:bg-night-800 border border-white/10 shadow-xl px-1 py-1">
          <motion.button
            whileTap={{ scale: 0.85 }}
            transition={TAP}
            onClick={() => changeZoom(-0.15)}
            disabled={zoom <= MIN_ZOOM}
            className="p-2 rounded-full text-cream-100 hover:bg-white/10 disabled:opacity-25 transition-colors"
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
            className="p-2 rounded-full text-cream-100 hover:bg-white/10 disabled:opacity-25 transition-colors"
            aria-label="Zoom in"
          >
            <ZoomIn className="w-4 h-4" />
          </motion.button>
          <motion.button
            whileTap={{ scale: 0.85 }}
            transition={TAP}
            onClick={() => setZoom(1)}
            disabled={zoom === 1}
            className="p-2 rounded-full text-cream-100 hover:bg-white/10 disabled:opacity-25 transition-colors"
            aria-label="Reset zoom"
          >
            <RotateCcw className="w-3.5 h-3.5" />
          </motion.button>
        </div>
      </div>

      {/* Verses (pinch to zoom) */}
      <div
        style={{ fontSize: `${1.02 * zoom}rem`, touchAction: 'pan-y' }}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        className="px-7 md:px-10 max-w-xl mx-auto space-y-6"
      >
        {PLEDGE_VERSES.map((verse) => (
          <motion.div
            key={verse.number}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.18 }}
            className="relative rounded-[28px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] p-7 md:p-9 overflow-hidden group"
          >
            <div
              className="absolute -top-10 -right-10 w-44 h-44 pointer-events-none"
              style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 12%, transparent), transparent)' }}
            />
            <div className="relative flex items-start gap-5 md:gap-6">
              <div className="flex-shrink-0 flex items-center justify-center w-10 h-10 rounded-full bg-cream-100 dark:bg-black border border-cream-200 dark:border-white/5 text-sm font-black font-mono text-ember dark:text-amber-brand group-hover:bg-amber-brand group-hover:text-black transition-all duration-300">
                {verse.number}
              </div>
              <p className="font-serif font-medium leading-[1.9] text-ink-900 dark:text-creamtext tracking-[0.012em]">
                {verse.text}
              </p>
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
