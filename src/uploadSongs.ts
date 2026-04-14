import { collection, writeBatch, doc } from 'firebase/firestore';
import { db } from './firebase';
import { Song } from './types';

export const uploadSongsToFirestore = async (songs: Song[]) => {
  const batch = writeBatch(db);
  const songsCollection = collection(db, 'songs');

  songs.forEach((song) => {
    const songRef = doc(songsCollection, song.id);
    batch.set(songRef, song);
  });

  await batch.commit();
  console.log('Successfully uploaded all songs to Firestore');
};
