<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/c9f30a00-1ea8-469d-b540-6b94b818a3f1

## Run Locally

**Prerequisites:**  Node.js


1. Install dependencies:
   `npm install`
2. Set the `GEMINI_API_KEY` in [.env.local](.env.local) to your Gemini API key
3. Run the app:
   `npm run dev`

## Native Android app (`native/`)

Jetpack Compose build of the library: Holy Songs, Family Pledge, Books (PDF +
Divine Principle reader) and HJ Global News.

```bash
cd native
gradle assembleRelease        # APK at app/build/outputs/apk/release/
```

Release APKs are signed with `native/hphc-release.keystore` (alias `hphc`,
PKCS#12 store password `hphc-library-2026`). The key is committed on purpose:
local builds and CI sign with the same certificate, so a new APK always
installs over an older one without a signature mismatch. Back the file up -
without it, installed copies can never be updated again.
