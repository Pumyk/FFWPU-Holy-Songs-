import { useState, useEffect, useMemo } from 'react';
import { Search, Moon, Sun, ChevronLeft, Book } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from './lib/utils';
import { Song } from './types';

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
    <div className="font-serif leading-relaxed text-base md:text-lg text-gray-800 dark:text-gray-200 space-y-10 pb-12">
      {items.map((item, index) => {
        // If it's a chorus item, we skip it in the main loop because we'll 
        // inject it after each verse if it exists.
        if (item.type === 'chorus') return null;

        return (
          <div key={index} className="space-y-10">
            <motion.div 
              initial={{ opacity: 0, y: 10 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              className="relative group"
            >
              {/* Desktop Verse Number */}
              <div className="absolute -left-14 top-1 hidden lg:flex items-center justify-center w-8 h-8 rounded-lg border border-slate-200 dark:border-gray-800 text-xs font-bold font-mono text-slate-300 group-hover:text-ffwpu-blue dark:group-hover:text-ffwpu-gold group-hover:border-ffwpu-blue/20 dark:group-hover:border-ffwpu-gold/20 transition-all duration-300">
                {item.number}
              </div>
              
              {/* Mobile/Tablet Verse Number */}
              <div className="lg:hidden flex items-center gap-2 mb-2">
                <span className="text-[9px] font-bold font-mono text-slate-400 dark:text-gray-500 uppercase tracking-widest">
                  Verse {item.number}
                </span>
                <div className="h-px flex-1 bg-slate-200 dark:bg-gray-800" />
              </div>

              <div className="whitespace-pre-wrap text-gray-700 dark:text-gray-300 leading-relaxed">
                {item.text}
              </div>
            </motion.div>

            {/* Auto-inject Chorus after each verse */}
            {chorusText && (
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                className="relative pl-6 md:pl-10 py-10 my-10"
              >
                {/* Decorative glass-like background for Chorus */}
                <div className="absolute inset-0 bg-white dark:bg-ffwpu-gold/5 rounded-3xl -ml-4 md:-ml-6 border border-slate-200 dark:border-ffwpu-gold/10 shadow-xl shadow-slate-200/40 dark:shadow-none" />
                <div className="absolute left-0 top-6 bottom-6 w-1 bg-ffwpu-blue dark:bg-ffwpu-gold rounded-full shadow-sm" />
                
                <div className="relative">
                  <div className="flex items-center gap-3 mb-6">
                    <span className="text-[10px] font-black tracking-[0.2em] uppercase text-ffwpu-blue dark:text-ffwpu-gold opacity-40">
                      Chorus
                    </span>
                    <div className="h-px flex-1 bg-ffwpu-blue/10 dark:bg-ffwpu-gold/10" />
                  </div>
                  <div className="whitespace-pre-wrap italic font-medium text-ffwpu-blue dark:text-ffwpu-gold text-lg md:text-xl leading-relaxed tracking-tight">
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

  useEffect(() => {
    const fetchSongs = async () => {
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
        console.error('Error fetching songs:', error);
      }
    };

    fetchSongs();
  }, []);

  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkMode]);

  const filteredSongs = useMemo(() => {
    if (!searchQuery.trim()) return songs;
    const query = searchQuery.toLowerCase().trim();
    
    // Check if query is a number (or number + letter like 21a)
    const isNumberQuery = /^\d+[a-z]?$/.test(query);

    return songs.filter(song => {
      if (isNumberQuery && song.number) {
        return song.number.toLowerCase() === query;
      }
      return song.title.toLowerCase().includes(query) || 
             (song.number && song.number.toLowerCase().includes(query)) ||
             song.lyrics.toLowerCase().includes(query);
    }).sort((a, b) => {
      // Sort by number if available
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

  return (
    <div className={isDarkMode ? 'dark' : ''}>
      <div className="min-h-screen bg-white dark:bg-[#001529] text-slate-900 dark:text-gray-100 font-sans transition-colors duration-200 selection:bg-ffwpu-blue/10 dark:selection:bg-ffwpu-gold/30">
        {/* Header */}
        <header className="sticky top-0 z-30 bg-white/95 dark:bg-[#003366]/95 backdrop-blur-md border-b border-slate-100 dark:border-[#002A52] pt-[env(safe-area-inset-top)] transition-all duration-300">
          <div className="max-w-3xl mx-auto px-6 h-16 flex items-center justify-between">
            <div className="flex items-center gap-4">
              <AnimatePresence mode="popLayout">
                {selectedSong ? (
                  <motion.button
                    initial={{ opacity: 0, x: -5 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -5 }}
                    onClick={() => setSelectedSong(null)}
                    className="p-2 -ml-2 rounded-xl hover:bg-slate-50 dark:hover:bg-white/10 transition-colors text-slate-500 dark:text-white"
                    aria-label="Go back"
                  >
                    <ChevronLeft className="w-5 h-5" />
                  </motion.button>
                ) : (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.9 }}
                    className="flex items-center justify-center w-8 h-8 -ml-1 bg-white rounded-lg shadow-sm border border-slate-100 dark:border-transparent"
                  >
                    <img 
                      src="https://static.wikia.nocookie.net/logopedia/images/c/c4/Family_Federation_for_World_Peace_and_Unification.svg/revision/latest?cb=20230814040140" 
                      alt="FFWPU Logo" 
                      className="w-full h-full object-contain p-1"
                      referrerPolicy="no-referrer"
                    />
                  </motion.div>
                )}
              </AnimatePresence>
              <h1 className="text-base font-bold tracking-tight truncate text-slate-900 dark:text-white">
                {selectedSong ? selectedSong.title : "Holy Songs"}
              </h1>
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setIsTOCOpen(true)}
                className="p-2 rounded-xl hover:bg-slate-50 dark:hover:bg-white/10 transition-colors text-slate-400 dark:text-white"
                aria-label="Table of Contents"
              >
                <Book className="w-5 h-5" />
              </button>
              <button
                onClick={toggleTheme}
                className="p-2 rounded-xl hover:bg-slate-50 dark:hover:bg-white/10 transition-colors text-slate-400 dark:text-white"
                aria-label="Toggle theme"
              >
                {isDarkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
              </button>
            </div>
        </div>
      </header>

      <main className="max-w-3xl mx-auto relative">
        <AnimatePresence mode="wait">
          {selectedSong ? (
            <motion.div
              key="detail"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.3 }}
              className="pb-[calc(8rem+env(safe-area-inset-bottom))]"
            >
              {/* Song Detail Header */}
              <div className="px-6 py-12 md:px-12 md:py-20 max-w-2xl mx-auto">
                <div className="flex items-center gap-3 mb-6">
                  {selectedSong.number && (
                    <span className="text-ffwpu-blue dark:text-ffwpu-gold font-mono font-bold text-lg tracking-tighter">
                      {selectedSong.number}
                    </span>
                  )}
                  {selectedSong.category && (
                    <span className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-[10px] font-bold uppercase tracking-widest text-slate-500 dark:text-slate-400">
                      {selectedSong.category.replace(/-/g, ' ')}
                    </span>
                  )}
                </div>
                <h2 className="text-4xl md:text-6xl font-extrabold text-slate-900 dark:text-white mb-8 leading-[1.05] tracking-tight">
                  {selectedSong.title}
                </h2>
                <div className="flex flex-wrap gap-x-8 gap-y-2 text-xs text-slate-400 dark:text-slate-500 font-bold uppercase tracking-widest">
                  {selectedSong.artist && (
                    <div className="flex items-center gap-2">
                      <span className="opacity-50">By</span>
                      <span className="text-slate-600 dark:text-slate-300">{selectedSong.artist}</span>
                    </div>
                  )}
                  {selectedSong.album && (
                    <div className="flex items-center gap-2">
                      <span className="opacity-50">Album</span>
                      <span className="text-slate-600 dark:text-slate-300">{selectedSong.album}</span>
                    </div>
                  )}
                </div>
              </div>

              <div className="px-6 md:px-12 max-w-2xl mx-auto">
                <div className="prose prose-slate dark:prose-invert max-w-none">
                  <LyricsDisplay lyrics={selectedSong.lyrics} />
                </div>

                {/* Navigation Buttons */}
                <div className="mt-20 pt-10 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between gap-4">
                  <button
                    onClick={handlePrevious}
                    disabled={currentSongIndex <= 0}
                    className="flex-1 flex items-center justify-center gap-2 py-4 px-6 rounded-2xl bg-slate-50 dark:bg-slate-900 text-slate-600 dark:text-slate-400 font-bold text-sm hover:bg-slate-100 dark:hover:bg-slate-800 disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                  >
                    <ChevronLeft className="w-4 h-4" />
                    Previous
                  </button>
                  <button
                    onClick={handleNext}
                    disabled={currentSongIndex >= navigationList.length - 1}
                    className="flex-1 flex items-center justify-center gap-2 py-4 px-6 rounded-2xl bg-slate-900 dark:bg-ffwpu-gold text-white dark:text-ffwpu-blue font-bold text-sm hover:bg-slate-800 dark:hover:bg-ffwpu-gold/90 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-lg shadow-slate-200/50 dark:shadow-none"
                  >
                    Next
                    <ChevronLeft className="w-4 h-4 rotate-180" />
                  </button>
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
              {/* Search Bar */}
              <div className="p-6 sticky top-[calc(4rem+env(safe-area-inset-top))] z-20 bg-white/95 dark:bg-[#001529]/95 backdrop-blur-md">
                <div className="relative max-w-2xl mx-auto w-full">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <Search className="h-4 w-4 text-slate-400" />
                  </div>
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search by title, number, or lyrics..."
                    className="block w-full pl-11 pr-12 py-3.5 border border-slate-200 dark:border-slate-800 rounded-2xl leading-5 bg-slate-50 dark:bg-slate-900 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-slate-200 dark:focus:ring-slate-800 focus:bg-white dark:focus:bg-slate-900 transition-all sm:text-sm"
                  />
                  {searchQuery && (
                    <button 
                      onClick={() => setSearchQuery('')}
                      className="absolute inset-y-0 right-0 pr-4 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                    >
                      <span className="text-xs font-bold uppercase tracking-tighter">Clear</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Song List */}
              <div className="flex-1 px-6 pb-[calc(8rem+env(safe-area-inset-bottom))] max-w-2xl mx-auto w-full">
                {filteredSongs.length > 0 ? (
                  <div className="space-y-1">
                    {filteredSongs.map((song) => (
                      <button
                        key={song.id}
                        onClick={() => setSelectedSong(song)}
                        className="w-full text-left p-4 rounded-2xl hover:bg-slate-50 dark:hover:bg-slate-900 transition-all group flex items-center gap-5"
                      >
                        {song.number && (
                          <div className="flex-shrink-0 w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-400 dark:text-slate-500 font-mono font-bold text-xs group-hover:bg-slate-900 group-hover:text-white dark:group-hover:bg-ffwpu-gold dark:group-hover:text-ffwpu-blue transition-all">
                            {song.number}
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <h3 className="text-base font-bold text-slate-900 dark:text-white truncate">
                            {song.title}
                          </h3>
                          <p className="text-xs text-slate-400 dark:text-slate-500 truncate mt-0.5 font-medium">
                            {song.lyrics.split('\n')[0]}
                          </p>
                        </div>
                      </button>
                    ))}
                  </div>
                ) : (
                  <div className="text-center py-20 px-4">
                    <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-800 mb-4">
                      <Search className="h-8 w-8 text-gray-400" />
                    </div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 mb-1">No songs found</h3>
                    <p className="text-gray-500 dark:text-gray-400">
                      We couldn't find any songs matching "{searchQuery}"
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
              className="fixed inset-0 z-50 bg-white dark:bg-[#001529] overflow-y-auto pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)]"
            >
              <div className="max-w-3xl mx-auto px-6 py-12">
                <div className="flex items-center justify-between mb-12">
                  <h2 className="text-3xl font-black text-slate-900 dark:text-white tracking-tight">
                    Table of Contents
                  </h2>
                  <button
                    onClick={() => setIsTOCOpen(false)}
                    className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-900 text-slate-500 hover:bg-slate-100 transition-all"
                  >
                    <ChevronLeft className="w-6 h-6 rotate-90 md:rotate-0" />
                  </button>
                </div>

                <div className="space-y-20">
                  {Object.entries(songsByCategory).map(([category, categorySongs], catIndex) => (
                    <motion.div 
                      key={category} 
                      initial={{ opacity: 0, y: 30 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: catIndex * 0.1, duration: 0.5, ease: "easeOut" }}
                      className="space-y-10"
                    >
                      <div className="flex items-center gap-6">
                        <h3 className="text-xs font-black uppercase tracking-[0.5em] text-ffwpu-blue dark:text-ffwpu-gold opacity-60 whitespace-nowrap">
                          {category.replace(/-/g, ' ')}
                        </h3>
                        <div className="h-px w-full bg-slate-100 dark:bg-slate-800" />
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-16 gap-y-6">
                        {(categorySongs as Song[]).sort((a, b) => (a.number || '').localeCompare(b.number || '', undefined, { numeric: true })).map((song, songIndex) => (
                          <motion.button
                            key={song.id}
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            transition={{ delay: (catIndex * 0.1) + (songIndex * 0.02) }}
                            onClick={() => {
                              setSelectedSong(song);
                              setIsTOCOpen(false);
                              setSearchQuery('');
                            }}
                            className="flex items-baseline gap-5 py-3 group text-left transition-all border-b border-slate-50 dark:border-slate-900/50 hover:border-ffwpu-blue/20 dark:hover:border-ffwpu-gold/20"
                          >
                            <span className="text-xs font-mono font-black text-slate-300 dark:text-slate-700 group-hover:text-ffwpu-blue dark:group-hover:text-ffwpu-gold transition-colors w-8">
                              {song.number?.padStart(2, '0')}
                            </span>
                            <span className="text-base font-bold text-slate-700 dark:text-slate-300 group-hover:text-slate-900 dark:group-hover:text-white transition-colors">
                              {song.title}
                            </span>
                          </motion.button>
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
