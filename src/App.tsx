import { useState, useEffect, useMemo } from 'react';
import { Search, Moon, Sun, ChevronLeft, ChevronRight, Book, X, Music2, Sparkles } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from './lib/utils';
import { Song } from './types';
import { db, auth } from './firebase';
import { collection, onSnapshot, query, orderBy } from 'firebase/firestore';
import { onAuthStateChanged, User } from 'firebase/auth';

const LyricsDisplay = ({ lyrics }: { lyrics: string }) => {
  // Pre-process lyrics to ensure Chorus markers are consistent and attached to their content
  const processedLyrics = lyrics
    .replace(/Chorus:\s*/gi, '[CHORUS]\n')
    .replace(/Verse (\d+):\s*/gi, '[VERSE $1]\n');

  // Split into paragraphs by double newlines
  const paragraphs = processedLyrics.split(/\n\s*\n/).filter(p => p.trim() !== '');

  // Extract the first chorus text found to repeat it
  const chorusParagraph = paragraphs.find(p => p.includes('[CHORUS]'));
  const chorusText = chorusParagraph ? chorusParagraph.replace('[CHORUS]', '').trim() : null;

  const items: { type: 'verse' | 'chorus'; text: string; number?: string }[] = [];
  let verseCounter = 0;

  paragraphs.forEach((paragraph) => {
    const isChorus = paragraph.includes('[CHORUS]');
    const explicitVerseMatch = paragraph.match(/\[VERSE (\d+)\]/);

    if (isChorus) {
      const text = paragraph.replace('[CHORUS]', '').trim();
      if (text) {
        items.push({ type: 'chorus', text });
      }
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

      if (verseText) {
        items.push({ type: 'verse', text: verseText, number: verseNumber });
      }
    }
  });

  return (
    <div className="font-serif leading-relaxed text-[17px] md:text-xl text-ink-700 dark:text-creamtext/90 space-y-12 pb-12">
      {items.map((item, index) => {
        // If it's a chorus item, we skip it in the main loop because we'll
        // inject it after each verse if it exists.
        if (item.type === 'chorus') return null;

        return (
          <div key={index} className="space-y-12">
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.45, ease: 'easeOut' }}
              className="relative group pl-12 md:pl-16"
            >
              {/* Floating verse number bubble */}
              <div className="absolute left-0 top-0 flex items-center justify-center w-9 h-9 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-xs font-bold font-mono text-amber-brand dark:text-amber-brand group-hover:scale-110 group-hover:bg-amber-brand group-hover:text-black dark:group-hover:bg-amber-brand dark:group-hover:text-black transition-all duration-300 shadow-sm">
                {item.number}
              </div>

              <div className="whitespace-pre-wrap leading-[1.8] tracking-[0.01em]">
                {item.text}
              </div>
            </motion.div>

            {/* Auto-inject Chorus after each verse */}
            {chorusText && (
              <motion.div
                initial={{ opacity: 0, y: 14 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.45, ease: 'easeOut' }}
                className="relative rounded-[28px] bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 p-6 md:p-8 overflow-hidden"
              >
                {/* Soft amber glow in the corner */}
                <div className="absolute -top-10 -right-10 w-36 h-36 rounded-full bg-amber-brand/10 dark:bg-amber-brand/[0.07] blur-2xl pointer-events-none" />

                <div className="relative">
                  <div className="flex items-center gap-3 mb-5">
                    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-brand/10 dark:bg-amber-brand/10 text-[10px] font-black tracking-[0.18em] uppercase text-ember dark:text-amber-brand">
                      <Music2 className="w-3 h-3" />
                      Chorus
                    </span>
                    <div className="h-px flex-1 bg-amber-brand/15 dark:bg-amber-brand/10" />
                  </div>
                  <div className="whitespace-pre-wrap italic font-medium text-ember dark:text-creamtext/95 text-lg md:text-xl leading-relaxed tracking-tight">
                    {chorusText}
                  </div>
                </div>
              </motion.div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default function App() {
  const [isDarkMode, setIsDarkMode] = useState(() => {
    if (typeof window !== 'undefined') {
      return document.documentElement.classList.contains('dark') ||
             window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    return false;
  });

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSong, setSelectedSong] = useState<Song | null>(null);
  const [songs, setSongs] = useState<Song[]>([]);
  const [isTOCOpen, setIsTOCOpen] = useState(false);
  const [tocSearchQuery, setTocSearchQuery] = useState('');
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const unsubscribeAuth = onAuthStateChanged(auth, (user) => {
      setUser(user);
    });
    return () => unsubscribeAuth();
  }, []);

  useEffect(() => {
    // First, try to fetch from Firestore for real-time updates
    const q = query(collection(db, 'songs'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      if (!snapshot.empty) {
        const firestoreSongs = snapshot.docs.map(doc => doc.data() as Song);
        setSongs(firestoreSongs);
      } else {
        // Fallback to local HTML if Firestore is empty
        fetchLocalSongs();
      }
    }, (error) => {
      console.error("Firestore error, falling back to local:", error);
      fetchLocalSongs();
    });

    const fetchLocalSongs = async () => {
      try {
        const response = await fetch('/songs.html');
        if (!response.ok) throw new Error('Failed to fetch songs');
        const html = await response.text();
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        const songElements = doc.querySelectorAll('.song');

        const parsedSongs: Song[] = Array.from(songElements).map((el) => {
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

        setSongs(parsedSongs);
      } catch (error) {
        console.error('Error fetching local songs:', error);
      }
    };

    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkMode]);

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
      // Songs without numbers go to the top
      if (!a.number && b.number) return -1;
      if (a.number && !b.number) return 1;
      if (a.number && b.number) {
        return a.number.localeCompare(b.number, undefined, { numeric: true, sensitivity: 'base' });
      }
      return 0;
    });
  }, [searchQuery, songs]);

  const toggleTheme = () => setIsDarkMode(!isDarkMode);

  const navigationList = useMemo(() => {
    // If we have multiple search results, navigate within them
    if (filteredSongs.length > 1) return filteredSongs;
    // If we have 1 result or no results (though no results shouldn't happen if a song is selected),
    // navigate the full list
    return songs;
  }, [filteredSongs, songs]);

  const currentSongIndex = useMemo(() => {
    if (!selectedSong) return -1;
    return navigationList.findIndex(s => s.id === selectedSong.id);
  }, [selectedSong, navigationList]);

  const handleNext = () => {
    if (currentSongIndex < navigationList.length - 1) {
      setSelectedSong(navigationList[currentSongIndex + 1]);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handlePrevious = () => {
    if (currentSongIndex > 0) {
      setSelectedSong(navigationList[currentSongIndex - 1]);
      window.scrollTo({ top: 0, behavior: 'smooth' });
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

  return (
    <div className={isDarkMode ? 'dark' : ''}>
      <div className="min-h-screen bg-cream-50 dark:bg-amoled text-ink-900 dark:text-creamtext font-sans transition-colors duration-300 selection:bg-amber-brand/20 dark:selection:bg-amber-brand/30">
        {/* Header */}
        <header className="sticky top-0 z-30 bg-cream-50/85 dark:bg-amoled/85 backdrop-blur-xl border-b border-cream-200/60 dark:border-white/5 pt-[env(safe-area-inset-top)] transition-all duration-300">
          <div className="max-w-3xl mx-auto px-5 h-16 flex items-center justify-between">
            <div className="flex items-center gap-3.5">
              <AnimatePresence mode="popLayout">
                {selectedSong ? (
                  <motion.button
                    initial={{ opacity: 0, x: -6 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -6 }}
                    onClick={() => setSelectedSong(null)}
                    className="p-2.5 -ml-2 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 hover:bg-cream-200 dark:hover:bg-night-700 transition-colors text-ink-700 dark:text-creamtext"
                    aria-label="Go back"
                  >
                    <ChevronLeft className="w-5 h-5" />
                  </motion.button>
                ) : (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.9 }}
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
              <button
                onClick={() => setIsTOCOpen(true)}
                className="p-2.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 hover:bg-cream-200 dark:hover:bg-night-700 transition-colors text-ink-700 dark:text-creamtext"
                aria-label="Table of Contents"
              >
                <Book className="w-[18px] h-[18px]" />
              </button>
              <button
                onClick={toggleTheme}
                className="p-2.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 hover:bg-cream-200 dark:hover:bg-night-700 transition-colors text-ink-700 dark:text-creamtext"
                aria-label="Toggle theme"
              >
                {isDarkMode ? <Sun className="w-[18px] h-[18px] text-amber-brand" /> : <Moon className="w-[18px] h-[18px]" />}
              </button>
            </div>
          </div>
        </header>

        <main className="max-w-3xl mx-auto relative">
          <AnimatePresence mode="wait">
            {selectedSong ? (
              <motion.div
                key="detail"
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -12 }}
                transition={{ duration: 0.3 }}
                className="pb-[calc(9rem+env(safe-area-inset-bottom))]"
              >
                {/* Song Detail Header */}
                <div className="px-5 md:px-8 pt-12 md:pt-20 max-w-2xl mx-auto relative">
                  {/* Ambient glow */}
                  <div className="absolute -top-6 left-1/2 -translate-x-1/2 w-72 h-32 bg-amber-brand/15 dark:bg-amber-brand/10 blur-3xl rounded-full pointer-events-none" />

                  <div className="relative">
                    <div className="flex flex-wrap items-center gap-2.5 mb-6">
                      {selectedSong.number && (
                        <span className="px-3.5 py-1.5 rounded-full bg-black dark:bg-amber-brand text-amber-brand dark:text-black font-mono font-black text-sm tracking-tight shadow-lg">
                          {selectedSong.number}
                        </span>
                      )}
                      {selectedSong.category && (
                        <span className="px-3 py-1.5 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-[10px] font-black uppercase tracking-[0.15em] text-ink-500 dark:text-muted-warm">
                          {selectedSong.category.replace(/-/g, ' ')}
                        </span>
                      )}
                    </div>
                    <h2 className="text-4xl md:text-6xl font-black text-ink-900 dark:text-creamtext mb-8 leading-[1.02] tracking-tight">
                      {selectedSong.title}
                    </h2>
                    <div className="flex flex-wrap gap-x-8 gap-y-2 text-[11px] text-ink-500 dark:text-muted-warm font-bold uppercase tracking-[0.15em]">
                      {selectedSong.artist && (
                        <div className="flex items-center gap-2">
                          <Sparkles className="w-3.5 h-3.5 text-amber-brand" />
                          <span className="opacity-60">By</span>
                          <span className="text-ink-700 dark:text-creamtext/80">{selectedSong.artist}</span>
                        </div>
                      )}
                      {selectedSong.album && (
                        <div className="flex items-center gap-2">
                          <span className="opacity-60">Album</span>
                          <span className="text-ink-700 dark:text-creamtext/80">{selectedSong.album}</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                <div className="px-5 md:px-8 max-w-2xl mx-auto">
                  <div className="prose max-w-none">
                    <LyricsDisplay lyrics={selectedSong.lyrics} />
                  </div>

                  {/* Floating Navigation */}
                  <div className="sticky bottom-[calc(1.5rem+env(safe-area-inset-bottom))] z-20 mt-16">
                    <div className="flex items-center gap-3 p-2 rounded-[28px] bg-ink-900/95 dark:bg-night-800/95 backdrop-blur-xl border border-white/10 shadow-2xl shadow-black/40">
                      <button
                        onClick={handlePrevious}
                        disabled={currentSongIndex <= 0}
                        className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-[22px] text-cream-100 font-bold text-sm hover:bg-white/10 disabled:opacity-25 disabled:cursor-not-allowed transition-all"
                      >
                        <ChevronLeft className="w-4 h-4" />
                        Previous
                      </button>
                      <div className="w-px h-8 bg-white/10" />
                      <button
                        onClick={handleNext}
                        disabled={currentSongIndex >= navigationList.length - 1}
                        className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-[22px] bg-amber-brand text-black font-bold text-sm hover:bg-amber-brand/90 disabled:opacity-25 disabled:cursor-not-allowed transition-all"
                      >
                        Next
                        <ChevronRight className="w-4 h-4" />
                      </button>
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
                transition={{ duration: 0.2 }}
                className="flex flex-col min-h-[calc(100vh-4rem)]"
              >
                {/* Hero */}
                <div className="px-5 pt-10 pb-6 max-w-2xl mx-auto w-full relative">
                  <div className="absolute top-4 right-2 w-56 h-24 bg-amber-brand/15 dark:bg-amber-brand/10 blur-3xl rounded-full pointer-events-none" />
                  <h2 className="relative text-[1.75rem] md:text-[2.25rem] font-black tracking-tight leading-[1.15] text-ink-900 dark:text-creamtext">
                    Let's sing to glorify our <span className="text-ember dark:text-amber-brand italic font-serif">Heavenly Parent</span>
                  </h2>
                  <p className="relative mt-2 text-sm font-medium text-ink-500 dark:text-muted-warm">
                    {songs.length > 0 ? (
                      <>
                        <span className="font-black text-amber-brand dark:text-amber-brand">{songs.length}</span> songs ready
                      </>
                    ) : (
                      'Loading the songbook...'
                    )}
                  </p>
                </div>

                {/* Search Bar */}
                <div className="px-5 pb-4 sticky top-[calc(4rem+env(safe-area-inset-top))] z-20 bg-cream-50/90 dark:bg-amoled/90 backdrop-blur-xl">
                  <div className="relative max-w-2xl mx-auto w-full">
                    <div className="absolute inset-y-0 left-5 flex items-center pointer-events-none">
                      <Search className="h-[18px] w-[18px] text-ink-500 dark:text-muted-warm" />
                    </div>
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Search title, number, or lyrics..."
                      className="block w-full pl-13 pr-14 py-4 rounded-full border border-cream-200 dark:border-white/[0.07] leading-5 bg-cream-100 dark:bg-night-800 text-ink-900 dark:text-creamtext placeholder-ink-500/70 dark:placeholder-muted-warm/70 focus:outline-none focus:border-amber-brand/50 focus:ring-4 ring-amber-brand/10 transition-all sm:text-sm font-medium"
                    />
                    {searchQuery && (
                      <button
                        onClick={() => setSearchQuery('')}
                        className="absolute inset-y-0 right-4 flex items-center text-ink-500 dark:text-muted-warm hover:text-ink-900 dark:hover:text-creamtext transition-colors"
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
                      {filteredSongs.map((song, index) => (
                        <motion.button
                          key={song.id}
                          initial={{ opacity: 0, y: 10 }}
                          animate={{ opacity: 1, y: 0 }}
                          transition={{ duration: 0.3, delay: Math.min(index * 0.015, 0.3) }}
                          onClick={() => setSelectedSong(song)}
                          className="w-full text-left p-4 rounded-[24px] bg-white dark:bg-night-800 border border-cream-200 dark:border-white/[0.04] hover:border-amber-brand/40 dark:hover:border-amber-brand/30 hover:shadow-lg hover:shadow-amber-brand/5 dark:hover:bg-night-700/60 active:scale-[0.99] transition-all group flex items-center gap-4"
                        >
                          {song.number && (
                            <div className="flex-shrink-0 w-11 h-11 rounded-2xl bg-cream-100 dark:bg-black flex items-center justify-center text-ink-500 dark:text-amber-brand/80 font-mono font-black text-xs group-hover:bg-amber-brand group-hover:text-black dark:group-hover:bg-amber-brand dark:group-hover:text-black transition-all">
                              {song.number}
                            </div>
                          )}
                          <div className="flex-1 min-w-0">
                            <h3 className="text-[15px] font-bold text-ink-900 dark:text-creamtext truncate">
                              {song.title}
                            </h3>
                            <p className="text-xs text-ink-500 dark:text-muted-warm truncate mt-1 font-medium">
                              {song.lyrics.split('\n')[0]}
                            </p>
                          </div>
                          <ChevronRight className="w-4 h-4 flex-shrink-0 text-ink-500/40 dark:text-muted-warm/40 group-hover:text-amber-brand group-hover:translate-x-0.5 transition-all" />
                        </motion.button>
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-20 px-4">
                      <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-cream-100 dark:bg-night-800 mb-5">
                        <Search className="h-7 w-7 text-ink-500 dark:text-muted-warm" />
                      </div>
                      <h3 className="text-lg font-bold text-ink-900 dark:text-creamtext mb-1.5">No songs found</h3>
                      <p className="text-sm text-ink-500 dark:text-muted-warm">
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
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-50 bg-cream-50 dark:bg-amoled overflow-y-auto pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)]"
              >
                <div className="max-w-3xl mx-auto px-5 py-12">
                  <div className="flex items-center justify-between mb-10">
                    <div>
                      <h2 className="text-3xl font-black text-ink-900 dark:text-creamtext tracking-tight">
                        Contents
                      </h2>
                      <p className="text-xs font-bold uppercase tracking-[0.2em] text-ink-500 dark:text-muted-warm mt-1.5">
                        Browse by category
                      </p>
                    </div>
                    <button
                      onClick={() => {
                        setIsTOCOpen(false);
                        setTocSearchQuery('');
                      }}
                      className="p-3 rounded-full bg-cream-100 dark:bg-night-800 border border-cream-200 dark:border-white/5 text-ink-700 dark:text-creamtext hover:bg-cream-200 dark:hover:bg-night-700 transition-all"
                      aria-label="Close contents"
                    >
                      <X className="w-5 h-5" />
                    </button>
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
                      className="block w-full pl-13 pr-4 py-4 rounded-full border border-cream-200 dark:border-white/[0.07] leading-5 bg-cream-100 dark:bg-night-800 text-ink-900 dark:text-creamtext placeholder-ink-500/70 dark:placeholder-muted-warm/70 focus:outline-none focus:border-amber-brand/50 focus:ring-4 ring-amber-brand/10 transition-all sm:text-sm font-medium"
                    />
                  </div>

                  <div className="space-y-16">
                    {Object.entries(filteredSongsForTOC).map(([category, categorySongs], catIndex) => (
                      <motion.div
                        key={category}
                        initial={{ opacity: 0, y: 24 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: catIndex * 0.08, duration: 0.5, ease: "easeOut" }}
                        className="space-y-8"
                      >
                        <div className="flex items-center gap-5">
                          <h3 className="inline-flex items-center gap-2 text-xs font-black uppercase tracking-[0.3em] text-ember dark:text-amber-brand whitespace-nowrap">
                            <span className="w-1.5 h-1.5 rounded-full bg-ember dark:bg-amber-brand" />
                            {category.replace(/-/g, ' ')}
                          </h3>
                          <div className="h-px w-full bg-cream-200 dark:bg-white/[0.06]" />
                        </div>
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-12 gap-y-1">
                          {(categorySongs as Song[]).sort((a, b) => (a.number || '').localeCompare(b.number || '', undefined, { numeric: true })).map((song) => (
                            <button
                              key={song.id}
                              onClick={() => {
                                setSelectedSong(song);
                                setIsTOCOpen(false);
                                setSearchQuery('');
                                setTocSearchQuery('');
                              }}
                              className="flex items-baseline gap-4 py-3 group text-left transition-all border-b border-transparent hover:border-amber-brand/30 rounded-lg px-2 -mx-2 hover:bg-amber-brand/[0.06]"
                            >
                              <span className="text-xs font-mono font-black text-ink-500/60 dark:text-creamtext/30 group-hover:text-ember dark:group-hover:text-amber-brand transition-colors w-8">
                                {song.number?.padStart(2, '0')}
                              </span>
                              <span className="text-[15px] font-bold text-ink-700 dark:text-creamtext/75 group-hover:text-ink-900 dark:group-hover:text-creamtext transition-colors">
                                {song.title}
                              </span>
                            </button>
                          ))}
                        </div>
                      </motion.div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
