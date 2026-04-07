import { useState, useEffect, useMemo } from 'react';
import { Search, Moon, Sun, ChevronLeft, Book } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from './lib/utils';
import { Song } from './types';
import songsData from './songs.json';

// Use the parsed songs from the PDF
const INITIAL_SONGS: Song[] = songsData as Song[];

const LyricsDisplay = ({ lyrics }: { lyrics: string }) => {
  // Pre-process lyrics to ensure Chorus is on its own line
  const processedLyrics = lyrics
    .replace(/Chorus:/gi, '\n\nChorus:\n')
    .replace(/Verse (\d+):/gi, '\n\nVerse $1:\n');

  // Split into paragraphs by double newlines
  const paragraphs = processedLyrics.split(/\n\s*\n/).filter(p => p.trim() !== '');

  return (
    <div className="font-serif leading-relaxed text-lg md:text-xl text-gray-800 dark:text-gray-200 space-y-6">
      {paragraphs.map((paragraph, index) => {
        const isChorus = paragraph.toLowerCase().includes('chorus:');
        
        if (isChorus) {
          const chorusText = paragraph.replace(/Chorus:/gi, '').trim();
          return (
            <div key={index} className="pl-4 md:pl-6 border-l-4 border-[#003366]/40 dark:border-[#D4AF37]/40 py-2 my-6 bg-[#003366]/5 dark:bg-[#D4AF37]/5 rounded-r-lg">
              <p className="font-bold text-[#003366] dark:text-[#D4AF37] mb-2 text-sm uppercase tracking-wider">Chorus</p>
              <div className="whitespace-pre-wrap italic">{chorusText}</div>
            </div>
          );
        }

        return (
          <div key={index} className="whitespace-pre-wrap">
            {paragraph.trim()}
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
  const [songs, setSongs] = useState<Song[]>(INITIAL_SONGS);

  useEffect(() => {
    if (isDarkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [isDarkMode]);

  const filteredSongs = useMemo(() => {
    if (!searchQuery.trim()) return songs;
    const query = searchQuery.toLowerCase();
    return songs.filter(song => 
      song.title.toLowerCase().includes(query) || 
      (song.number && song.number.includes(query)) ||
      song.lyrics.toLowerCase().includes(query)
    );
  }, [searchQuery, songs]);

  const toggleTheme = () => setIsDarkMode(!isDarkMode);

  return (
    <div className={isDarkMode ? 'dark' : ''}>
      <div className="min-h-screen bg-gray-50 dark:bg-[#001529] text-gray-900 dark:text-gray-100 font-sans transition-colors duration-200 selection:bg-[#003366]/20 dark:selection:bg-[#D4AF37]/30">
        {/* Header */}
        <header className="sticky top-0 z-10 bg-[#003366] text-white shadow-md safe-top">
          <div className="max-w-3xl mx-auto px-4 h-16 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <AnimatePresence mode="popLayout">
                {selectedSong ? (
                  <motion.button
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -10 }}
                    onClick={() => setSelectedSong(null)}
                    className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
                    aria-label="Go back"
                  >
                    <ChevronLeft className="w-6 h-6" />
                  </motion.button>
                ) : (
                  <motion.div
                    initial={{ opacity: 0, x: 10 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 10 }}
                    className="flex items-center justify-center w-9 h-9 -ml-1 bg-white rounded-full p-1 shadow-sm"
                  >
                    <img 
                      src="https://static.wikia.nocookie.net/logopedia/images/c/c4/Family_Federation_for_World_Peace_and_Unification.svg/revision/latest?cb=20230814040140" 
                      alt="FFWPU Logo" 
                      className="w-full h-full object-contain"
                      referrerPolicy="no-referrer"
                    />
                  </motion.div>
                )}
              </AnimatePresence>
              <h1 className="text-xl font-semibold tracking-tight truncate">
                {selectedSong ? (
                  <span className="flex items-center gap-2">
                    {selectedSong.number && <span className="text-[#D4AF37]">#{selectedSong.number}</span>}
                    {selectedSong.title}
                  </span>
                ) : (
                  "FFWPU Holy Songs"
                )}
              </h1>
            </div>
          <button
            onClick={toggleTheme}
            className="p-2 rounded-full hover:bg-white/10 transition-colors"
            aria-label="Toggle theme"
          >
            {isDarkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>
        </div>
      </header>

      <main className="max-w-3xl mx-auto relative">
        <AnimatePresence mode="wait">
          {selectedSong ? (
            <motion.div
              key="detail"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.2 }}
              className="p-6 md:p-8 pb-24"
            >
              <div className="prose prose-lg dark:prose-invert max-w-none">
                <LyricsDisplay lyrics={selectedSong.lyrics} />
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
              <div className="p-4 sticky top-16 z-10 bg-gray-50/95 dark:bg-[#001529]/95 backdrop-blur-sm">
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Search className="h-5 w-5 text-gray-400" />
                  </div>
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search by title, number, or lyrics..."
                    className="block w-full pl-10 pr-3 py-3 border border-gray-200 dark:border-[#002A52] rounded-2xl leading-5 bg-white dark:bg-[#001A33] placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-[#003366] focus:border-[#003366] dark:focus:ring-[#D4AF37] dark:focus:border-[#D4AF37] sm:text-sm transition-shadow shadow-sm"
                  />
                </div>
              </div>

              {/* Song List */}
              <div className="flex-1 px-4 pb-24">
                {filteredSongs.length > 0 ? (
                  <div className="space-y-2">
                    {filteredSongs.map((song) => (
                      <button
                        key={song.id}
                        onClick={() => setSelectedSong(song)}
                        className="w-full text-left p-4 rounded-2xl bg-white dark:bg-[#001A33] border border-gray-100 dark:border-[#002A52] hover:border-[#003366]/30 dark:hover:border-[#D4AF37]/50 hover:shadow-md transition-all active:scale-[0.98] group flex items-center gap-4"
                      >
                        {song.number && (
                          <div className="flex-shrink-0 w-12 h-12 rounded-full bg-[#003366]/10 dark:bg-[#D4AF37]/10 flex items-center justify-center text-[#003366] dark:text-[#D4AF37] font-semibold group-hover:bg-[#003366]/20 dark:group-hover:bg-[#D4AF37]/20 transition-colors">
                            {song.number}
                          </div>
                        )}
                        <div className="flex-1 min-w-0">
                          <h3 className="text-lg font-medium text-gray-900 dark:text-gray-100 truncate">
                            {song.title}
                          </h3>
                          <p className="text-sm text-gray-500 dark:text-gray-400 truncate mt-0.5">
                            {song.lyrics.split('\n')[0]}...
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
      </main>
      </div>
    </div>
  );
}
