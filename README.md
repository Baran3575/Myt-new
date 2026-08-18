# Myt — personal music player

A Spotify-style music player built from scratch for personal use. Streams and
downloads royalty-free music, and plays the songs already on your phone.

## Features

- **Spotify-style UI** — dark theme, "good evening" home grid, category tiles,
  mini player bar, full-screen now playing with artwork + seekbar
- **On-device music** — scans MediaStore: tracks, albums, artwork, favorites
- **Online backend** — search, stream and download royalty-free tracks from
  two providers:
  - [Jamendo](https://devs.jamendo.com) — thousands of CC-licensed tracks
  - [Pixabay Music](https://pixabay.com/api/docs/) — professional stock music
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

Free API keys (add either or both; more providers = better catalog):

1. **Jamendo** — https://devs.jamendo.com → repo secret `JAMENDO_CLIENT_ID`
2. **Pixabay Music** — https://pixabay.com/api/docs/ (key shown on the page)
   → repo secret `PIXABAY_API_KEY`

Then re-run the workflow. The app merges results from all configured providers.
Without keys, the app still works fully for on-device music.

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