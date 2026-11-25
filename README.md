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

For development, use Gradle 8.9 and run `gradle -p native assembleDebug`.
The debug APK uses Android’s normal debug key and has a separate app ID.

For public releases, create a **new** Android signing key in Android Studio
and store the keystore outside the checkout. Keep a secure backup. Never use the
retired key or commit a keystore, password, or other signing credential.

Set these variables privately on the machine you use to produce a release:

- `HPHC_RELEASE_KEYSTORE`: absolute path to the new keystore
- `HPHC_RELEASE_STORE_PASSWORD`: new keystore password
- `HPHC_RELEASE_KEY_ALIAS`: alias for the new key
- `HPHC_RELEASE_KEY_PASSWORD`: new key password

Then run `gradle -p native assembleRelease`. Verify the signed APK’s certificate
and app ID (`com.hphc.library.android`) before sharing it. Android installs it
alongside the retired app, not as an update to it. GitHub Actions builds debug
APKs only; it does not receive or use the release key.
