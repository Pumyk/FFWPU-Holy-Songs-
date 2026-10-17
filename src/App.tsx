import { useState, useEffect, useMemo, useRef, type TouchEvent as ReactTouchEvent } from 'react';
import { Search, Moon, Sun, ChevronLeft, ChevronRight, Book, X, Music2, Sparkles, ZoomIn, ZoomOut, RotateCcw, Palette, HeartHandshake } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { Song } from './types';
import songsHtml from './songs.html?raw';

/* ---------- Local songbook, bundled at build time (instant, offline) ---------- */

const parseSongs = (html: string): Song[] => {
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const songElements = doc.querySelectorAll('.song');
  return Array.from(songElements).map((el) => {
    const article = el as HTMLElement;
    return {
      id: article.dataset.id || '',
      title: article.querySelector('.title')?.textContent || '',
      number: article.dataset.number,
      lyrics: article.querySelector('.lyrics')?.textContent || '',
      artist: article.dataset.artist,
      album: article.dataset.album,
      category: article.dataset.category,
    };
  });
};

const LOCAL_SONGS = parseSongs(songsHtml);
const MIN_ZOOM = 1;
const MAX_ZOOM = 2.5;

/* ------------------------ Material Motion: expressive springs ------------------------ */

/** Snappy entrance — quick ease-out tween, no spring settle time */
const FAST = { duration: 0.16, ease: [0.2, 0, 0, 1] } as const;
/** Button press feedback — tight, quick spring */
const TAP = { type: 'spring', stiffness: 700, damping: 26 } as const;
/** Soft bouncy spring — splash only (one-shot) */
const SPRING_BOUNCY = { type: 'spring', stiffness: 210, damping: 17, mass: 0.9 } as const;

/* ---------------------------- Material You (dynamic color) ---------------------------- */

interface MaterialYouColors {
  accent1: string;
  accent1Deep: string;
}

const materialYouFetch = async (): Promise<MaterialYouColors | null> => {
  try {
    const cached = window.localStorage.getItem('holy-songs-my-colors');
    if (cached) return JSON.parse(cached) as MaterialYouColors;
    const { Capacitor } = await import('@capacitor/core');
    if (!Capacitor.isNativePlatform() || !Capacitor.isPluginAvailable('MaterialYou')) return null;
    const plugins = (Capacitor as unknown as { Plugins: Record<string, { getColors: () => Promise<{ available: boolean; accent1?: string; accent1Deep?: string }> }> }).Plugins;
    const plugin = plugins.MaterialYou;
    if (!plugin) return null;
    const res = await plugin.getColors();
    if (!res?.available || !res.accent1 || !res.accent1Deep) return null;
    const colors = { accent1: res.accent1, accent1Deep: res.accent1Deep };
    window.localStorage.setItem('holy-songs-my-colors', JSON.stringify(colors));
    return colors;
  } catch {
    return null;
  }
};

const applyDynamicColors = (colors: MaterialYouColors | null) => {
  const root = document.documentElement;
  if (colors) {
    root.style.setProperty('--color-amber-brand', colors.accent1);
    root.style.setProperty('--color-ember', colors.accent1Deep);
  } else {
    root.style.removeProperty('--color-amber-brand');
    root.style.removeProperty('--color-ember');
  }
};

/* -------------------------------- Lyrics view -------------------------------- */

const LyricsDisplay = ({ lyrics }: { lyrics: string }) => {
  const processedLyrics = lyrics
    .replace(/Chorus:\s*/gi, '[CHORUS]\n')
    .replace(/Verse (\d+):\s*/gi, '[VERSE $1]\n');

  const paragraphs = processedLyrics.split(/\n\s*\n/).filter(p => p.trim() !== '');

  const chorusParagraph = paragraphs.find(p => p.includes('[CHORUS]'));
  const chorusText = chorusParagraph ? chorusParagraph.replace('[CHORUS]', '').trim() : null;

  const items: { type: 'verse' | 'chorus'; text: string; number?: string }[] = [];
  let verseCounter = 0;

  paragraphs.forEach((paragraph) => {
    const isChorus = paragraph.includes('[CHORUS]');
    const explicitVerseMatch = paragraph.match(/\[VERSE (\d+)\]/);

    if (isChorus) {
      const text = paragraph.replace('[CHORUS]', '').trim();
      if (text) items.push({ type: 'chorus', text });
    } else {
      let verseNumber: string;
      let verseText: string;

      if (explicitVerseMatch) {
        verseNumber = explicitVerseMatch[1];
        verseText = paragraph.replace(explicitVerseMatch[0], '').trim();
      } else {
        verseCounter++;
        verseNumber = verseCounter.toString();
        verseText = paragraph.trim();
      }

      if (verseText) items.push({ type: 'verse', text: verseText, number: verseNumber });
    }
  });

  return (
    <div className="font-serif font-medium leading-[1.75] text-[1em] text-ink-900 dark:text-creamtext space-y-12 pb-12">
      {items.map((item, index) => {
        if (item.type === 'chorus') return null;

        return (
          <div key={index} className="space-y-12">
            <div
              className="relative group pl-12 md:pl-16"
            >
              {/* Floating verse number bubble */}
              <div
                className="absolute left-0 top-0 flex items-center justify-center w-9 h-9 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-xs font-black font-mono text-ember dark:text-amber-brand group-hover:scale-110 group-hover:bg-amber-brand group-hover:text-black transition-all duration-300 shadow-sm"
              >
                {item.number}
              </div>

              <div className="whitespace-pre-wrap tracking-[0.01em]">
                {item.text}
              </div>
            </div>

            {/* Auto-inject Chorus after each verse */}
            {chorusText && (
              <div
                className="relative rounded-[28px] bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 p-6 md:p-8 overflow-hidden"
              >
                <div className="absolute -top-10 -right-10 w-44 h-44 pointer-events-none" style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 14%, transparent), transparent)' }} />

                <div className="relative">
                  <div className="flex items-center gap-3 mb-5">
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-brand/15 dark:bg-amber-brand/10 text-[10px] font-black tracking-[0.18em] uppercase text-ember dark:text-amber-brand">
                      <Music2 className="w-3 h-3" />
                      Chorus
                    </span>
                    <div className="h-px flex-1 bg-amber-brand/20 dark:bg-amber-brand/10" />
                  </div>
                  <div className="whitespace-pre-wrap italic font-semibold text-ember dark:text-creamtext text-[1.08em] leading-relaxed tracking-tight">
                    {chorusText}
                  </div>
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

/* --------------------------------- Splash --------------------------------- */

const renderSplash = () => {
  return (
    <motion.div
      key="splash"
      className="fixed inset-0 z-[100] flex flex-col items-center justify-center bg-cream-50 dark:bg-amoled overflow-hidden"
      exit={{ opacity: 0, scale: 1.03 }}
      transition={{ duration: 0.28, ease: [0.4, 0, 1, 1] }}
    >
      {/* Ambient accent glow */}
      <motion.div
        className="absolute w-[22rem] h-[22rem] rounded-full pointer-events-none"
        style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 22%, transparent), transparent)' }}
        initial={{ opacity: 0, scale: 0.5 }}
        animate={{ opacity: [0, 0.9, 0.55], scale: [0.5, 1.2, 1] }}
        transition={{ duration: 1.9, times: [0, 0.35, 1] }}
      />

      {/* Logo tile */}
      <motion.div
        initial={{ scale: 0.3, rotate: -16, opacity: 0 }}
        animate={{ scale: 1, rotate: 0, opacity: 1 }}
        transition={{ ...SPRING_BOUNCY, delay: 0.15 }}
        className="relative flex items-center justify-center w-24 h-24 rounded-[28px] bg-black dark:bg-night-800 border border-white/10 shadow-2xl shadow-black/30 overflow-hidden"
      >
        <img src="/icon.svg" alt="Holy Songs" className="w-full h-full p-4" />
      </motion.div>

      {/* Title */}
      <motion.h1
        initial={{ y: 18, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ ...FAST, delay: 0.45 }}
        className="relative mt-7 text-3xl font-black tracking-tight text-ink-900 dark:text-creamtext"
      >
        Holy Songs
      </motion.h1>

      {/* Subtitle */}
      <motion.p
        initial={{ y: 12, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ ...FAST, delay: 0.6 }}
        className="relative mt-2 px-6 text-center text-sm font-bold text-ink-700 dark:text-muted-warm"
      >
        Let's sing to glorify our{' '}
        <span className="text-ember dark:text-amber-brand italic font-serif">Heavenly Parent</span>
      </motion.p>

      {/* Loading bar */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.75 }}
        className="relative mt-10 w-40 h-1 rounded-full bg-amber-brand/15 overflow-hidden"
      >
        <motion.div
          className="h-full rounded-full bg-amber-brand"
          initial={{ width: '0%' }}
          animate={{ width: '100%' }}
          transition={{ duration: 1.0, delay: 0.8, ease: [0.4, 0, 0.2, 1] }}
        />
      </motion.div>
    </motion.div>
  );
};

/* ---------------------------------- App ---------------------------------- */

type NavState = { hs: 'song'; id: string } | { hs: 'toc' } | null;

export default function App() {
  const [isDarkMode, setIsDarkMode] = useState(() => {
    if (typeof window === 'undefined') return false;
    const stored = window.localStorage.getItem('holy-songs-theme');
    const dark = stored ? stored === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.classList.toggle('dark', dark);
    return dark;
  });

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSong, setSelectedSong] = useState<Song | null>(null);
  const [songs, setSongs] = useState<Song[]>(LOCAL_SONGS);
  const [isTOCOpen, setIsTOCOpen] = useState(false);
  const [tocSearchQuery, setTocSearchQuery] = useState('');
  const [zoom, setZoom] = useState(1);
  const [showSplash, setShowSplash] = useState(true);
  const [toast, setToast] = useState<string | null>(null);
  const [dynamicColor, setDynamicColor] = useState(() =>
    window.localStorage.getItem('holy-songs-dynamic') === 'on'
  );

  const themeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const songsRef = useRef(songs);
  songsRef.current = songs;

  useEffect(() => {
    if (!showSplash) return;
    const t = setTimeout(() => setShowSplash(false), 1900);
    return () => clearTimeout(t);
  }, [showSplash]);

  const showToast = (msg: string) => {
    setToast(msg);
    if (toastTimer.current) clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 2200);
  };

  /* -------- Firestore sync (lazy) — UI already usable from bundled songbook -------- */

  useEffect(() => {
    let cancelled = false;
    let unsubscribe: (() => void) | undefined;

    const sync = async () => {
      try {
        const [{ db }, firestore] = await Promise.all([
          import('./firebase'),
          import('firebase/firestore'),
        ]);
        if (cancelled) return;
        unsubscribe = firestore.onSnapshot(
          firestore.query(firestore.collection(db, 'songs')),
          (snapshot) => {
            if (!cancelled && !snapshot.empty) {
              const remote = snapshot.docs.map(doc => doc.data() as Song);
              if (remote.length > 0) setSongs(remote);
            }
          },
          () => {/* offline: keep bundled songbook */},
        );
      } catch {
        /* offline: keep bundled songbook */
      }
    };

    const id = window.setTimeout(sync, 1200);
    return () => {
      cancelled = true;
      clearTimeout(id);
      unsubscribe?.();
    };
  }, []);

  /* --------------------------------- Theme --------------------------------- */

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDarkMode);
    window.localStorage.setItem('holy-songs-theme', isDarkMode ? 'dark' : 'light');
  }, [isDarkMode]);

  useEffect(() => () => { if (themeTimer.current) clearTimeout(themeTimer.current); }, []);

  const toggleTheme = () => {
    const el = document.documentElement;
    el.classList.add('theme-anim');
    if (themeTimer.current) clearTimeout(themeTimer.current);
    themeTimer.current = setTimeout(() => el.classList.remove('theme-anim'), 400);
    setIsDarkMode(v => !v);
  };

  /* ---------------------------- Material You toggle ---------------------------- */

  useEffect(() => {
    window.localStorage.setItem('holy-songs-dynamic', dynamicColor ? 'on' : 'off');
    if (!dynamicColor) {
      applyDynamicColors(null);
      return;
    }
    let cancelled = false;
    materialYouFetch().then(colors => {
      if (cancelled) return;
      if (colors) applyDynamicColors(colors);
      else showToast('Material You needs Android 12+');
    });
    return () => { cancelled = true; };
  }, [dynamicColor]);

  /* ----------------------------- Back navigation ----------------------------- */
  /* The app is driven by history entries: every page (list -> song -> song via
     Next, TOC overlay) pushes an entry, so hardware/browser back always walks
     to the previous page instead of closing the app. Root-level back shows a
     "press again to exit" toast on Android. */

  const applyNav = (st: NavState) => {
    if (st?.hs === 'song') {
      const s = songsRef.current.find(x => x.id === st.id) || null;
      setSelectedSong(s);
      setIsTOCOpen(false);
      window.scrollTo(0, 0);
    } else if (st?.hs === 'toc') {
      setIsTOCOpen(true);
    } else {
      setSelectedSong(null);
      setIsTOCOpen(false);
      setTocSearchQuery('');
    }
  };

  const pushNav = (st: NavState) => {
    if (!st) return;
    window.history.pushState(st, '');
    applyNav(st);
  };

  const goSong = (song: Song) => pushNav({ hs: 'song', id: song.id });
  const goTOC = () => pushNav({ hs: 'toc' });
  const goBack = () => window.history.back();

  useEffect(() => {
    const onPop = (e: PopStateEvent) => applyNav((e.state as NavState) ?? null);
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, []);

  useEffect(() => {
    let handle: { remove: () => void } | undefined;
    let lastBack = 0;
    let cancelled = false;

    const setup = async () => {
      const { Capacitor } = await import('@capacitor/core');
      if (cancelled || !Capacitor.isNativePlatform()) return;
      const { App: CapApp } = await import('@capacitor/app');
      const h = await CapApp.addListener('backButton', () => {
        if (window.history.state && (window.history.state as NavState)?.hs) {
          window.history.back();
          return;
        }
        const now = Date.now();
        if (now - lastBack < 2200) {
          CapApp.exitApp();
        } else {
          lastBack = now;
          showToast('Press back again to exit');
        }
      });
      handle = h;
    };
    setup();

    return () => {
      cancelled = true;
      handle?.remove();
    };
  }, []);

  /* ---------------------------------- Data ---------------------------------- */

  const filteredSongs = useMemo(() => {
    let list = songs;
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase().trim();
      const isNumberQuery = /^\d+[a-z]?$/.test(query);
      list = songs.filter(song => {
        if (isNumberQuery && song.number) {
          return song.number.toLowerCase() === query;
        }
        return song.title.toLowerCase().includes(query) ||
               (song.number && song.number.toLowerCase().includes(query)) ||
               song.lyrics.toLowerCase().includes(query);
      });
    }

    return [...list].sort((a, b) => {
      if (!a.number && b.number) return -1;
      if (a.number && !b.number) return 1;
      if (a.number && b.number) {
        return a.number.localeCompare(b.number, undefined, { numeric: true, sensitivity: 'base' });
      }
      return 0;
    });
  }, [searchQuery, songs]);

  const navigationList = useMemo(() => {
    if (filteredSongs.length > 1) return filteredSongs;
    return songs;
  }, [filteredSongs, songs]);

  const currentSongIndex = useMemo(() => {
    if (!selectedSong) return -1;
    return navigationList.findIndex(s => s.id === selectedSong.id);
  }, [selectedSong, navigationList]);

  const handleNext = () => {
    if (currentSongIndex < navigationList.length - 1) {
      goSong(navigationList[currentSongIndex + 1]);
    }
  };

  const handlePrevious = () => {
    if (currentSongIndex > 0) {
      goSong(navigationList[currentSongIndex - 1]);
    }
  };

  const songsByCategory = useMemo(() => {
    const groups: Record<string, Song[]> = {};
    songs.forEach(song => {
      const cat = song.category || 'Other';
      if (!groups[cat]) groups[cat] = [];
      groups[cat].push(song);
    });
    return groups;
  }, [songs]);

  const filteredSongsForTOC = useMemo(() => {
    if (!tocSearchQuery.trim()) return songsByCategory;
    const q = tocSearchQuery.toLowerCase().trim();
    const groups: Record<string, Song[]> = {};
    songs.forEach(song => {
      const matches = song.title.toLowerCase().includes(q) ||
                      (song.number && song.number.toLowerCase().includes(q));
      if (matches) {
        const cat = song.category || 'Other';
        if (!groups[cat]) groups[cat] = [];
        groups[cat].push(song);
      }
    });
    return groups;
  }, [tocSearchQuery, songsByCategory]);

  /* ------------------------------ Pinch to zoom ------------------------------ */

  const pinchState = useRef<{ dist: number; zoom: number } | null>(null);

  const touchDist = (t: ReactTouchEvent['touches']) =>
    Math.hypot(t[0].clientX - t[1].clientX, t[0].clientY - t[1].clientY);

  const handleTouchStart = (e: ReactTouchEvent) => {
    if (e.touches.length === 2) {
      pinchState.current = { dist: touchDist(e.touches), zoom };
    }
  };

  const handleTouchMove = (e: ReactTouchEvent) => {
    if (e.touches.length === 2 && pinchState.current) {
      const d = touchDist(e.touches);
      const next = pinchState.current.zoom * (d / pinchState.current.dist);
      setZoom(Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, next)));
    }
  };

  const changeZoom = (delta: number) =>
    setZoom(z => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, +(z + delta).toFixed(2))));

  /* --------------------------------- Render --------------------------------- */

  return (
    <div className="min-h-screen bg-cream-50 dark:bg-amoled text-ink-900 dark:text-creamtext font-sans selection:bg-amber-brand/20 dark:selection:bg-amber-brand/30">
      {/* Animated splash overlay */}
      <AnimatePresence>
        {showSplash && renderSplash()}
      </AnimatePresence>

      {/* Toast */}
      <AnimatePresence>
        {toast && (
          <motion.div
            key="toast"
            initial={{ opacity: 0, y: 16, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 16, scale: 0.95 }}
            transition={TAP}
            className="fixed bottom-[calc(2.5rem+env(safe-area-inset-bottom))] left-1/2 -translate-x-1/2 z-[90] px-5 py-3 rounded-full bg-ink-900 dark:bg-night-800 border border-white/10 text-cream-100 text-sm font-bold shadow-2xl whitespace-nowrap"
          >
            {toast}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Header */}
      <header className="sticky top-0 z-30 bg-cream-50 dark:bg-amoled border-b border-cream-200/60 dark:border-white/5 pt-[env(safe-area-inset-top)]">
        <div className="max-w-3xl mx-auto px-5 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3.5">
            <AnimatePresence mode="popLayout" initial={false}>
              {selectedSong ? (
                <motion.button
                  key="back"
                  initial={{ opacity: 0, x: -6 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -6 }}
                  transition={TAP}
                  whileTap={{ scale: 0.9 }}
                  onClick={goBack}
                  className="p-2.5 -ml-2 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 hover:bg-cream-200 dark:hover:bg-night-700 text-ink-700 dark:text-creamtext"
                  aria-label="Go back"
                >
                  <ChevronLeft className="w-5 h-5" />
                </motion.button>
              ) : (
                <motion.div
                  key="logo"
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.9 }}
                  transition={TAP}
                  className="flex items-center justify-center w-10 h-10 rounded-2xl bg-black dark:bg-night-800 border border-white/5 shadow-lg overflow-hidden"
                >
                  <img
                    src="/icon.svg"
                    alt="Holy Song Book Logo"
                    className="w-full h-full object-contain p-1"
                  />
                </motion.div>
              )}
            </AnimatePresence>
            <h1 className="text-base font-extrabold tracking-tight truncate text-ink-900 dark:text-creamtext">
              {selectedSong ? selectedSong.title : "Holy Songs"}
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <motion.button
              whileTap={{ scale: 0.88 }}
              transition={TAP}
              onClick={() => setDynamicColor(v => !v)}
              className={`relative p-2.5 rounded-full border transition-all ${
                dynamicColor
                  ? 'bg-amber-brand/15 border-amber-brand/40 text-ember dark:text-amber-brand'
                  : 'bg-cream-100 dark:bg-night-800 border-cream-200 dark:border-white/5 hover:bg-cream-200 dark:hover:bg-night-700 text-ink-700 dark:text-creamtext'
              }`}
              aria-label="Toggle Material You dynamic color"
              title="Material You dynamic color"
            >
              <Palette className="w-[18px] h-[18px]" />
              {dynamicColor && (
                <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-ember dark:bg-amber-brand" />
              )}
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.88 }}
              transition={TAP}
              onClick={goTOC}
              className="p-2.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 hover:bg-cream-200 dark:hover:bg-night-700 text-ink-700 dark:text-creamtext"
              aria-label="Table of Contents"
            >
              <Book className="w-[18px] h-[18px]" />
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.88 }}
              transition={TAP}
              onClick={toggleTheme}
              className="p-2.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 hover:bg-cream-200 dark:hover:bg-night-700 text-ink-700 dark:text-creamtext"
              aria-label="Toggle theme"
            >
              {isDarkMode ? <Sun className="w-[18px] h-[18px] text-amber-brand" /> : <Moon className="w-[18px] h-[18px]" />}
            </motion.button>
          </div>
        </div>
      </header>

      <main className="max-w-3xl mx-auto relative">
        <AnimatePresence mode="wait" initial={false}>
          {selectedSong ? (
            <motion.div
              key={`song-${selectedSong.id}`}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -6 }}
              transition={FAST}
              className="pb-[calc(9rem+env(safe-area-inset-bottom))]"
            >
              {/* Song Detail Header */}
              <div className="px-5 md:px-8 pt-10 md:pt-16 max-w-2xl mx-auto relative">
                <div className="absolute -top-4 left-1/2 -translate-x-1/2 w-80 h-44 pointer-events-none" style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 18%, transparent), transparent)' }} />

                <div className="relative">
                  <div className="flex flex-wrap items-center gap-2.5 mb-6">
                    {selectedSong.number && (
                      <span className="px-3.5 py-1.5 rounded-full bg-black dark:bg-amber-brand text-amber-brand dark:text-black font-mono font-black text-sm tracking-tight shadow-lg">
                        {selectedSong.number}
                      </span>
                    )}
                    {selectedSong.category && (
                      <span className="px-3 py-1.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-[10px] font-black uppercase tracking-[0.15em] text-ink-700 dark:text-muted-warm">
                        {selectedSong.category.replace(/-/g, ' ')}
                      </span>
                    )}
                  </div>
                  <h2 className="text-4xl md:text-6xl font-black text-ink-900 dark:text-creamtext mb-7 leading-[1.02] tracking-tight">
                    {selectedSong.title}
                  </h2>
                  <div className="flex flex-wrap gap-x-8 gap-y-2 text-[11px] text-ink-700 dark:text-muted-warm font-bold uppercase tracking-[0.15em]">
                    {selectedSong.artist && (
                      <div className="flex items-center gap-2">
                        <Sparkles className="w-3.5 h-3.5 text-ember dark:text-amber-brand" />
                        <span className="opacity-60">By</span>
                        <span className="text-ink-900 dark:text-creamtext/85">{selectedSong.artist}</span>
                      </div>
                    )}
                    {selectedSong.album && (
                      <div className="flex items-center gap-2">
                        <span className="opacity-60">Album</span>
                        <span className="text-ink-900 dark:text-creamtext/85">{selectedSong.album}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              <div className="px-5 md:px-8 max-w-2xl mx-auto">
                {/* Zoom toolbar */}
                <div className="sticky top-[calc(4rem+env(safe-area-inset-top))] z-20 flex justify-end -mt-2 mb-5">
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

                {/* Lyrics (pinch to zoom) */}
                <div
                  style={{ fontSize: `${1.125 * zoom}rem`, touchAction: 'pan-y' }}
                  onTouchStart={handleTouchStart}
                  onTouchMove={handleTouchMove}
                  className="prose max-w-none"
                >
                  <LyricsDisplay lyrics={selectedSong.lyrics} />
                </div>

                {/* Floating Navigation */}
                <div className="sticky bottom-[calc(1.5rem+env(safe-area-inset-bottom))] z-10 mt-16">
                  <div className="flex items-center gap-3 p-2 rounded-[28px] bg-ink-900 dark:bg-night-800 border border-white/10 shadow-2xl shadow-black/40">
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      transition={TAP}
                      onClick={handlePrevious}
                      disabled={currentSongIndex <= 0}
                      className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-[22px] text-cream-100 font-bold text-sm hover:bg-white/10 disabled:opacity-25 disabled:cursor-not-allowed transition-all"
                    >
                      <ChevronLeft className="w-4 h-4" />
                      Previous
                    </motion.button>
                    <div className="w-px h-8 bg-white/10" />
                    <motion.button
                      whileTap={{ scale: 0.95 }}
                      transition={TAP}
                      onClick={handleNext}
                      disabled={currentSongIndex >= navigationList.length - 1}
                      className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-[22px] bg-amber-brand text-black font-bold text-sm hover:bg-amber-brand/90 disabled:opacity-25 disabled:cursor-not-allowed transition-all"
                    >
                      Next
                      <ChevronRight className="w-4 h-4" />
                    </motion.button>
                  </div>
                </div>
              </div>
            </motion.div>
          ) : (
            <motion.div
              key="list"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.15 }}
              className="flex flex-col min-h-[calc(100vh-4rem)]"
            >
              {/* Hero */}
              <div className="px-5 pt-10 pb-6 max-w-2xl mx-auto w-full relative">
                <div className="absolute top-4 right-2 w-64 h-28 pointer-events-none" style={{ background: 'radial-gradient(closest-side, color-mix(in srgb, var(--color-amber-brand) 16%, transparent), transparent)' }} />
                <h2 className="relative text-[1.75rem] md:text-[2.25rem] font-black tracking-tight leading-[1.15] text-ink-900 dark:text-creamtext">
                  Let's sing to glorify our <span className="text-ember dark:text-amber-brand italic font-serif">Heavenly Parent</span>
                </h2>
                <p className="relative mt-2 text-sm font-bold text-ink-700 dark:text-muted-warm">
                  {songs.length > 0 ? (
                    <>
                      <HeartHandshake className="inline w-4 h-4 mr-1.5 -mt-0.5 text-ember dark:text-amber-brand" />
                      <span className="font-black text-ember dark:text-amber-brand">{songs.length}</span> songs ready
                    </>
                  ) : (
                    'Loading the songbook...'
                  )}
                </p>
              </div>

              {/* Search Bar */}
              <div className="px-5 pb-4 sticky top-[calc(4rem+env(safe-area-inset-top))] z-20 bg-cream-50 dark:bg-amoled">
                <div className="relative max-w-2xl mx-auto w-full">
                  <div className="absolute inset-y-0 left-5 flex items-center pointer-events-none">
                    <Search className="h-[18px] w-[18px] text-ink-500 dark:text-muted-warm" />
                  </div>
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search title, number, or lyrics..."
                    className="block w-full pl-13 pr-14 py-4 rounded-full border border-cream-200 dark:border-white/[0.07] leading-5 bg-cream-100 dark:bg-night-800 text-ink-900 dark:text-creamtext placeholder-ink-500 dark:placeholder-muted-warm/80 focus:outline-none focus:border-amber-brand/50 focus:ring-4 ring-amber-brand/10 transition-all sm:text-sm font-bold"
                  />
                  {searchQuery && (
                    <button
                      onClick={() => setSearchQuery('')}
                      className="absolute inset-y-0 right-4 flex items-center text-ink-700 dark:text-muted-warm hover:text-ink-900 dark:hover:text-creamtext transition-colors"
                      aria-label="Clear search"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>

              {/* Song List */}
              <div className="flex-1 px-5 pb-[calc(6rem+env(safe-area-inset-bottom))] max-w-2xl mx-auto w-full">
                {filteredSongs.length > 0 ? (
                  <div className="space-y-2.5">
                    {filteredSongs.map((song) => (
                      <motion.button
                        key={song.id}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={TAP}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => goSong(song)}
                        className="w-full text-left p-4 rounded-[24px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] hover:border-amber-brand/40 dark:hover:border-amber-brand/30 hover:shadow-lg hover:shadow-amber-brand/5 dark:hover:bg-night-700/60 transition-all group flex items-center gap-4"
                      >
                        {song.number && (
                          <div className="flex-shrink-0 w-11 h-11 rounded-2xl bg-cream-100 dark:bg-black flex items-center justify-center text-ink-700 dark:text-amber-brand font-mono font-black text-xs group-hover:bg-amber-brand group-hover:text-black transition-all">
                            {song.number}
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <h3 className="text-base font-extrabold text-ink-900 dark:text-creamtext truncate">
                            {song.title}
                          </h3>
                          <p className="text-xs text-ink-700 dark:text-muted-warm truncate mt-1 font-semibold">
                            {song.lyrics.split('\n')[0]}
                          </p>
                        </div>
                        <ChevronRight className="w-4 h-4 flex-shrink-0 text-ember/50 dark:text-amber-brand/50 group-hover:text-ember dark:group-hover:text-amber-brand group-hover:translate-x-0.5 transition-all" />
                      </motion.button>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-20 px-4">
                    <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-cream-100 dark:bg-night-800 mb-5">
                      <Search className="h-7 w-7 text-ink-700 dark:text-muted-warm" />
                    </div>
                    <h3 className="text-lg font-extrabold text-ink-900 dark:text-creamtext mb-1.5">No songs found</h3>
                    <p className="text-sm font-semibold text-ink-700 dark:text-muted-warm">
                      Nothing matches "{searchQuery}" — try another word or number.
                    </p>
                  </div>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Immersive TOC Overlay */}
        <AnimatePresence>
          {isTOCOpen && (
            <motion.div
              key="toc"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.15 }}
              className="fixed inset-0 z-50 bg-cream-50 dark:bg-amoled overflow-y-auto pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)]"
            >
              <div className="max-w-3xl mx-auto px-5 py-12">
                <div className="flex items-center justify-between mb-10">
                  <div>
                    <h2 className="text-3xl font-black text-ink-900 dark:text-creamtext tracking-tight">
                      Contents
                    </h2>
                    <p className="text-xs font-black uppercase tracking-[0.2em] text-ink-700 dark:text-muted-warm mt-1.5">
                      Browse by category
                    </p>
                  </div>
                  <motion.button
                    whileTap={{ scale: 0.88 }}
                    transition={TAP}
                    onClick={goBack}
                    className="p-3 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700 transition-all"
                    aria-label="Close contents"
                  >
                    <X className="w-5 h-5" />
                  </motion.button>
                </div>

                {/* TOC Search Bar */}
                <div className="mb-12 relative">
                  <div className="absolute inset-y-0 left-5 flex items-center pointer-events-none">
                    <Search className="h-[18px] w-[18px] text-ink-500 dark:text-muted-warm" />
                  </div>
                  <input
                    type="text"
                    value={tocSearchQuery}
                    onChange={(e) => setTocSearchQuery(e.target.value)}
                    placeholder="Search in contents..."
                    className="block w-full pl-13 pr-4 py-4 rounded-full border border-cream-200 dark:border-white/[0.07] leading-5 bg-cream-100 dark:bg-night-800 text-ink-900 dark:text-creamtext placeholder-ink-500 dark:placeholder-muted-warm/80 focus:outline-none focus:border-amber-brand/50 focus:ring-4 ring-amber-brand/10 transition-all sm:text-sm font-bold"
                  />
                </div>

                <div className="space-y-16">
                  {Object.entries(filteredSongsForTOC).map(([category, categorySongs]) => (
                    <div key={category} className="space-y-8">
                      <div className="flex items-center gap-5">
                        <h3 className="inline-flex items-center gap-2 text-xs font-black uppercase tracking-[0.3em] text-ember dark:text-amber-brand whitespace-nowrap">
                          <span className="w-1.5 h-1.5 rounded-full bg-ember dark:bg-amber-brand" />
                          {category.replace(/-/g, ' ')}
                        </h3>
                        <div className="h-px w-full bg-cream-200 dark:bg-white/[0.06]" />
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-12 gap-y-1">
                        {(categorySongs as Song[]).sort((a, b) => (a.number || '').localeCompare(b.number || '', undefined, { numeric: true })).map((song) => (
                          <motion.button
                            key={song.id}
                            whileTap={{ scale: 0.985 }}
                            transition={TAP}
                            onClick={() => goSong(song)}
                            className="flex items-baseline gap-4 py-3 group text-left border-b border-transparent hover:border-amber-brand/30 rounded-lg px-2 -mx-2 hover:bg-amber-brand/[0.06] transition-all"
                          >
                            <span className="text-xs font-mono font-black text-ember/70 dark:text-amber-brand/60 group-hover:text-ember dark:group-hover:text-amber-brand transition-colors w-8">
                              {song.number?.padStart(2, '0')}
                            </span>
                            <span className="text-[15px] font-bold text-ink-900 dark:text-creamtext/85 group-hover:text-ember dark:group-hover:text-amber-brand transition-colors">
                              {song.title}
                            </span>
                          </motion.button>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </main>
    </div>
  );
}
