# ArenaFit (Android, Kotlin + Jetpack Compose)

A workout app inspired by the feature set of social fitness apps: free logging, adaptive
workouts, recovery heat map, XP / levels / streaks, weekly league, video challenges (Arena),
feed, GPS walk/run, and a coach chat. All original code and data.

## Build
1. Install Android Studio (Koala or newer). Open this folder.
2. Let Gradle sync (needs internet once). Run on an emulator or phone (Android 8+).
   CLI: `./gradlew assembleDebug` -> app/build/outputs/apk/debug/

NOTE: this source was written without an Android SDK available, so it has NOT been compiled.
Expect to fix a few small compile errors on first sync.

## Structure
- data/Db.kt            Room entities, DAO, database
- domain/Exercises.kt   ~60-exercise starter library
- domain/Engines.kt     XP/levels/streaks, recovery model, adaptive generator, league, challenges, coach
- gps/GpsTracker.kt     LocationManager tracker
- ui/AppViewModel.kt    all app state and actions
- ui/*Screen*.kt        Home, Train, Progress, Arena, Feed, Coach, Activity, Profile

## What is real vs demo
Real (offline): logging, templates, PR detection, charts, XP/streak/freeze/coins, recovery map,
adaptive generator, GPS tracking, feed (local posts), rule-based coach.
Demo / needs backend: league rivals (simulated), Arena referee approval (demo button),
community feed, Pro switch (needs Play Billing), LLM coach (implement Coach interface via your server).

## Next steps
Backend (auth, sync, leaderboards, video upload + moderation), Play Billing, foreground GPS
service, Health Connect, notifications for streaks, bigger exercise dataset with demo media you own.
