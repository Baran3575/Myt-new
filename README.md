# Myt — personal music player

A Spotify-style music player built from scratch for personal use. Streams and
downloads royalty-free music, and plays the songs already on your phone.

## Features

- **Spotify-type UI** — dark theme, mini player bar, full-screen now playing view
- **On-device music** — scans MediaStore: tracks, albums, artwork, favorites
- **Online backend** — search, stream and download thousands of royalty-free
  (Creative Commons) tracks via the [Jamendo API](https://devs.jamendo.com)
- **Live notification** — foreground media notification with album art and
  play / pause / next / prev / seek controls, powered by AndroidX Media3
- **Downloads** — saved into the app's Music folder with a progress notification

## Building with GitHub Actions (recommended)

Every push to `main` builds a signed APK:

1. Open the **Actions** tab of this repo → select **Build APK** → **Run workflow**
   (or just push a commit).
2. When the job finishes, open the run and grab the **myt-apks** artifact.
3. Download it on your phone and install `app-release.apk`
   (enable "install unknown apps" for your browser/file manager first).

The release build is signed with a keystore generated fresh on each CI run,
so every build is directly installable.

## Optional: enable online music

1. Get a free API key at https://devs.jamendo.com (a few minutes).
2. Add it as a repository secret named `JAMENDO_CLIENT_ID`
   (repo → Settings → Secrets and variables → Actions).
3. Re-run the workflow. Without a key, the app still works fully for on-device music.

## Building locally

```bash
# one time
export ANDROID_HOME=/path/to/android-sdk
echo "MYT_JAMENDO_CLIENT_ID=your_key" >> gradle.properties   # optional

./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK (needs a keystore, see CI workflow)
```

## App structure

```
app/src/main/java/com/myt/player/
├── AppState.kt              # shared app-wide state
├── MainActivity.kt          # edge-to-edge UI host
├── data/
│   ├── local/               # MediaStore scanning + favorites/recents/download index
│   ├── model/               # Track / Album models
│   └── online/              # Jamendo API client + mp3 downloader
├── playback/                # Media3 service, media session, player controller
└── ui/                      # Compose screens & theme
```

License: MIT