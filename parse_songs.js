import fs from 'fs';

const text = fs.readFileSync('./songs.txt', 'utf8');

const songs = [];
let currentSong = null;

const lines = text.split('\n');
let parsingLyrics = false;

for (let i = 0; i < lines.length; i++) {
  const line = lines[i].trim();
  
  // Match song title like "1. Blessing of Glory" or "21a. Suffering"
  const titleMatch = line.match(/^(\d+[a-z]?)\.\s+(.+)$/);
  
  if (titleMatch) {
    if (currentSong) {
      currentSong.lyrics = currentSong.lyrics.trim();
      songs.push(currentSong);
    }
    
    currentSong = {
      id: titleMatch[1],
      number: titleMatch[1],
      title: titleMatch[2],
      lyrics: ''
    };
    parsingLyrics = false;
    continue;
  }
  
  if (currentSong) {
    if (line.startsWith('(Click here to listen') || line.startsWith('(Listen to Midi')) {
      parsingLyrics = true;
      continue;
    }
    
    if (parsingLyrics) {
      if (line !== '') {
        currentSong.lyrics += line + '\n';
      } else {
        currentSong.lyrics += '\n';
      }
    }
  }
}

if (currentSong) {
  currentSong.lyrics = currentSong.lyrics.trim();
  songs.push(currentSong);
}

fs.writeFileSync('./src/songs.json', JSON.stringify(songs, null, 2));
console.log(`Parsed ${songs.length} songs.`);
