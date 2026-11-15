#!/usr/bin/env python3
"""Refresh public/news.json from the HJ PeaceTV YouTube channel.

Keeps only the English 'HJ Global News' editions (titles like
'HJ Global News (10. 17. 2026)' — foreign editions carry a language
suffix before the date, e.g. 'HJ Global News Português (...)').

YouTube intermittently answers GitHub Actions runner IPs with 404/429, so
the feed is retried and falls back to parsing the channel page. A total
failure keeps the existing news.json and exits 0 so the workflow stays
green and the app keeps serving the last good digest.
"""
import json
import re
import sys
import time
import urllib.request
import datetime
import xml.etree.ElementTree as ET

CHANNEL_ID = "UCKBcbApQ73-PJJfF77Dz4EQ"
CHANNEL_URL = "https://www.youtube.com/@hjpeacetv8814"
FEED = f"https://www.youtube.com/feeds/videos.xml?channel_id={CHANNEL_ID}"
OUT = "public/news.json"
MAX_ITEMS = 12

UAS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15",
]

NS = {
    "a": "http://www.w3.org/2005/Atom",
    "m": "http://search.yahoo.com/mrss/",
    "yt": "http://www.youtube.com/xml/schemas/2015",
}
# English edition: 'HJ Global News' followed ONLY by digits/punctuation (a date)
ENGLISH = re.compile(r"^HJ Global News[\s\d\.\,\(\)/\-]+$", re.IGNORECASE)


def fetch(url: str, attempts: int = 3) -> bytes:
    last = None
    for i in range(attempts):
        ua = UAS[i % len(UAS)]
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": ua,
                "Accept": "application/xml,text/xml,text/html,*/*",
                "Accept-Language": "en-US,en;q=0.9",
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                return resp.read()
        except Exception as exc:  # noqa: BLE001 - retry any transport error
            last = exc
            print(f"attempt {i + 1}/{attempts} failed for {url}: {exc}", file=sys.stderr)
            time.sleep(4 * (i + 1))
    raise RuntimeError(f"all attempts failed for {url}: {last}")


def from_rss() -> list[dict]:
    root = ET.fromstring(fetch(FEED))
    items = []
    for e in root.findall("a:entry", NS):
        title = (e.find("a:title", NS).text or "").strip()
        if not ENGLISH.match(title):
            continue
        vid = e.find("yt:videoId", NS).text
        stats = e.find("m:group/m:community/m:statistics", NS)
        views = int(stats.get("views")) if stats is not None else 0
        items.append(
            {
                "id": vid,
                "title": title,
                "url": f"https://www.youtube.com/watch?v={vid}",
                "date": e.find("a:published", NS).text,
                "views": views,
                "thumbnail": f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg",
            }
        )
    return items


def from_rss2json() -> list[dict]:
    """Fallback: same feed fetched server-side by api.rss2json.com.

    Useful when YouTube refuses our runner's IP directly.
    """
    import urllib.parse

    url = "https://api.rss2json.com/v1/api.json?rss_url=" + urllib.parse.quote(FEED, safe="")
    data = json.loads(fetch(url).decode("utf-8", "replace"))
    if data.get("status") != "ok":
        raise RuntimeError(f"rss2json status: {data.get('status')}")
    items = []
    for it in data.get("items", []):
        title = (it.get("title") or "").strip()
        if not ENGLISH.match(title):
            continue
        m = re.search(r"(?:v=|/shorts/|youtu\.be/)([A-Za-z0-9_-]{11})", it.get("link") or "")
        if not m:
            continue
        vid = m.group(1)
        try:
            date = (
                datetime.datetime.strptime(it.get("pubDate", ""), "%Y-%m-%d %H:%M:%S")
                .replace(tzinfo=datetime.timezone.utc)
                .isoformat()
            )
        except Exception:  # noqa: BLE001 - keep going with now()
            date = datetime.datetime.now(datetime.timezone.utc).isoformat()
        items.append(
            {
                "id": vid,
                "title": title,
                "url": f"https://www.youtube.com/watch?v={vid}",
                "date": date,
                "views": 0,
                "thumbnail": it.get("thumbnail") or f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg",
            }
        )
    return items


def main() -> None:
    items: list[dict] = []
    for label, fn in (("rss", from_rss), ("rss2json", from_rss2json)):
        try:
            items = fn()
            if items:
                print(f"fetched {len(items)} English editions via {label}")
                break
        except Exception as exc:  # noqa: BLE001 - try next source
            print(f"{label} source failed: {exc}", file=sys.stderr)

    if not items:
        print("no source reachable; keeping existing news.json", file=sys.stderr)
        return

    # merge with history so older editions (beyond the RSS window) accumulate
    try:
        with open(OUT) as f:
            prev = json.load(f)
        seen = {i["id"] for i in items}
        for old_item in prev.get("items", []):
            if old_item["id"] not in seen:
                items.append(old_item)
    except Exception:  # noqa: BLE001 - first run / unreadable file
        pass

    items.sort(key=lambda i: i["date"], reverse=True)
    payload = {
        "channel": {
            "id": CHANNEL_ID,
            "title": "HJ PeaceTV",
            "url": CHANNEL_URL,
        },
        "updated": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "items": items[:MAX_ITEMS],
    }
    with open(OUT, "w") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"wrote {OUT}: {len(payload['items'])} English editions, latest: {items[0]['title']}")


if __name__ == "__main__":
    main()
