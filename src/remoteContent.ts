import type { Song } from './types';

/* ------------------------------------------------------------------ *
 * Remote content ("server-side" additions)
 *
 * Songs and PDF books live in content/manifest.json in the GitHub
 * repo. The app fetches it on launch (cached in localStorage for
 * offline), so new entries appear for every user WITHOUT an app
 * update. Bundled content always stays as the offline base.
 * ------------------------------------------------------------------ */

export interface RemoteBook {
  id: string;
  title: string;
  author: string;
  /** Absolute https URL, or a path relative to the repo raw root */
  url: string;
  pages?: number;
  note?: string;
}

export interface ContentManifest {
  version: number;
  updatedAt?: string;
  songs: Partial<Song>[];
  books: RemoteBook[];
}

const REPO = 'Pumyk/FFWPU-Holy-Songs-';
const MANIFEST_PATH = 'content/manifest.json';

const MANIFEST_URLS = [
  `https://raw.githubusercontent.com/${REPO}/main/${MANIFEST_PATH}`,
  `https://cdn.jsdelivr.net/gh/${REPO}@main/${MANIFEST_PATH}`,
];

const CACHE_KEY = 'hphc:content-manifest';

const normalizeBookUrl = (url: string): string => {
  if (/^https?:/i.test(url)) return url;
  return `https://raw.githubusercontent.com/${REPO}/main/${url.replace(/^\//, '')}`;
};

const isValidManifest = (m: unknown): m is ContentManifest => {
  const v = m as ContentManifest;
  return !!v && Array.isArray(v.songs) && Array.isArray(v.books);
};

/** Load the last-known-good manifest from localStorage (instant, offline). */
export function cachedManifest(): ContentManifest | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return isValidManifest(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

async function tryFetch(url: string, timeoutMs: number): Promise<ContentManifest | null> {
  try {
    const res = await fetch(url, { signal: AbortSignal.timeout(timeoutMs), cache: 'no-store' });
    if (!res.ok) return null;
    const parsed = await res.json();
    return isValidManifest(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

/** Fetch the freshest manifest; falls back to the cached copy offline. */
export async function fetchManifest(): Promise<ContentManifest | null> {
  // debug/testing override: ?contentUrl=<url>
  const override = new URLSearchParams(window.location.search).get('contentUrl');
  const urls = override ? [override] : MANIFEST_URLS;

  for (const u of urls) {
    const m = await tryFetch(u, 6000);
    if (m) {
      try { localStorage.setItem(CACHE_KEY, JSON.stringify(m)); } catch { /* storage full */ }
      return m;
    }
  }
  return cachedManifest();
}

/** Manifest songs, sanitized to full Song objects. */
export function manifestSongs(m: ContentManifest | null): Song[] {
  if (!m?.songs) return [];
  return m.songs
    .filter((s): s is Partial<Song> & { id: string; title: string; lyrics: string } =>
      !!s?.id && !!s?.title && !!s?.lyrics)
    .map((s) => ({
      id: s.id,
      title: s.title,
      number: s.number,
      lyrics: s.lyrics,
      artist: s.artist,
      album: s.album ?? 'Remote',
      category: s.category ?? 'holy-songs',
    }));
}

/** Manifest books, with resolved absolute URLs. */
export function manifestBooks(m: ContentManifest | null): RemoteBook[] {
  if (!m?.books) return [];
  return m.books
    .filter((b) => !!b?.id && !!b?.title && !!b?.url)
    .map((b) => ({ ...b, url: normalizeBookUrl(b.url) }));
}

/** Merge remote songs into the bundled list (remote overrides by id, new ones append). */
export function mergeSongs(local: Song[], remote: Song[]): Song[] {
  if (remote.length === 0) return local;
  const byId = new Map(local.map((s) => [s.id, s]));
  const added: Song[] = [];
  for (const r of remote) {
    if (byId.has(r.id)) byId.set(r.id, r);
    else added.push(r);
  }
  return [...byId.values(), ...added];
}
