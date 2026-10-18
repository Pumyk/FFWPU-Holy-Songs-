#!/usr/bin/env python3
"""Refresh public/news.json from the HJ PeaceTV YouTube channel RSS.

Keeps only the English 'HJ Global News' editions (titles like
'HJ Global News (10. 17. 2026)' — foreign editions carry a language
suffix before the date, e.g. 'HJ Global News Português (...)').
"""
import json
import re
import sys
import datetime
import urllib.request
import xml.etree.ElementTree as ET

CHANNEL_ID = "UCKBcbApQ73-PJJfF77Dz4EQ"
CHANNEL_URL = "https://www.youtube.com/@hjpeacetv8814"
FEED = f"https://www.youtube.com/feeds/videos.xml?channel_id={CHANNEL_ID}"
OUT = "public/news.json"
MAX_ITEMS = 12

NS = {
    "a": "http://www.w3.org/2005/Atom",
    "m": "http://search.yahoo.com/mrss/",
    "yt": "http://www.youtube.com/xml/schemas/2015",
}
# English edition: 'HJ Global News' followed ONLY by digits/punctuation (a date)
ENGLISH = re.compile(r"^HJ Global News[\s\d\.\,\(\)/\-]+$", re.IGNORECASE)

def main():
    req = urllib.request.Request(FEED, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        root = ET.fromstring(resp.read())
    channel_title = root.find("a:title", NS).text
    items = []
    for e in root.findall("a:entry", NS):
        title = (e.find("a:title", NS).text or "").strip()
        if not ENGLISH.match(title):
            continue
        vid = e.find("yt:videoId", NS).text
        stats = e.find("m:group/m:community/m:statistics", NS)
        views = int(stats.get("views")) if stats is not None else 0
        items.append({
            "id": vid,
            "title": title,
            "url": f"https://www.youtube.com/watch?v={vid}",
            "date": e.find("a:published", NS).text,
            "views": views,
            "thumbnail": f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg",
        })
    if not items:
        print("no English editions found in feed; keeping existing news.json", file=sys.stderr)
        return
    # merge with history so older editions (beyond the RSS window) accumulate
    try:
        with open(OUT) as f:
            prev = json.load(f)
        seen = {i["id"] for i in items}
        for old_item in prev.get("items", []):
            if old_item["id"] not in seen:
                items.append(old_item)
                seen.add(old_item["id"])
        items.sort(key=lambda i: i["date"], reverse=True)
    except (FileNotFoundError, json.JSONDecodeError):
        pass
    items = items[:MAX_ITEMS * 2]
    news = {
        "channel": {"id": CHANNEL_ID, "title": channel_title, "url": CHANNEL_URL},
        "updated": datetime.datetime.now(
            datetime.timezone(datetime.timedelta(hours=1))
        ).isoformat(timespec="seconds"),
        "items": items,
    }
    with open(OUT, "w") as f:
        json.dump(news, f, indent=1, ensure_ascii=False)
    print(f"wrote {OUT}: {len(items)} English editions, latest: {items[0]['title']}")

if __name__ == "__main__":
    main()
