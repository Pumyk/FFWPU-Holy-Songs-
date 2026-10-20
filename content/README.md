# Remote Content

Songs and books listed in `manifest.json` are fetched by the app on launch
(cached for offline). **Users get new content without updating the app.**

## How to add a song

1. Open `manifest.json`.
2. Add an entry to `songs`:

```json
{
  "id": "my-new-song",
  "title": "My New Song",
  "number": "",
  "artist": "Author / source",
  "album": "Holy Songs",
  "category": "holy-songs",
  "lyrics": "First line here.\nSecond line.\n\nSecond verse here."
}
```

3. Use `\n` for line breaks and a blank line (`\n\n`) between verses.
4. Commit to `main`. Everyone's app picks it up on next launch.

`id` must be unique (letters, numbers, hyphens). If an `id` matches a bundled
song, the remote version replaces it (useful for lyric corrections).

## How to add a book (PDF)

1. Upload the PDF into `content/books/`.
2. Add an entry to `books`:

```json
{
  "id": "my-book",
  "title": "My Book Title",
  "author": "Author Name",
  "url": "content/books/my-book.pdf",
  "pages": 123,
  "note": "Optional short description"
}
```

3. Commit to `main`. The book appears on the Books shelf; after a user opens
   it once, it is cached and works offline.

Keep PDFs under ~20 MB. The app always keeps its bundled content as the
offline base, so a bad manifest can never break the app.
