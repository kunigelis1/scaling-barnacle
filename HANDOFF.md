# Handoff: Night Sky & Wings

Status as of 2026-11-06: the project **builds, passes lint and passes its unit tests** with the real
Android toolchain (AGP 8.7.3, compileSdk 35), and produces a debug APK. It has **not been run on a
phone or emulator**, so none of its actual behaviour — screen, notifications, background jobs,
location — has been observed yet. That is the next step.

## What the app does

| Feature | When | Source |
| --- | --- | --- |
| Stargazing alert: notifies if tonight (21:00 to 03:00) averages 30% cloud cover or less, with moon phase and a viewing tip | Daily, 16:00 | Open-Meteo + on-device moon calculation |
| Rare bird alert: new notable sightings anywhere in Lithuania (last 3 days), up to 3 per check | Every 3 hours | eBird |
| Likely birds today: up to 5 species reported within 25 km in the last 7 days | Daily, 07:30 | eBird |
| Bird of the day: notification, plus a card in the app with a picture, English and Lithuanian names, a Wikipedia summary and a play button for its song | Daily, 07:30, and each time the app opens | eBird, Wikipedia, Xeno-canto |

eBird reports what people have **already seen**. It is not a forecast: "likely today" means
"seen near you this past week" and "rare" means "just reported".

## Project structure

```
settings.gradle.kts, build.gradle.kts    Gradle setup (AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1 wrapper)
app/build.gradle.kts                      App module; reads API keys from local.properties into BuildConfig
app/src/main/AndroidManifest.xml          Permissions: internet, notifications, coarse location
app/src/main/res/values/                  App name and AppCompat theme
app/src/main/java/com/example/nightskywings/
  Config.kt        Keys (from BuildConfig), fallback location (Klaipeda), cloud threshold
  MoonPhase.kt     Moon age, % lit, phase name, viewing tip (pure Kotlin)
  Parsers.kt       All JSON parsing for every API (pure Kotlin, unit-tested)
  TimeMath.kt      "Milliseconds until next HH:MM" for scheduling (pure Kotlin, unit-tested)
  Net.kt           OkHttp GET helper; returns null instead of throwing on errors
  WeatherApi.kt    Open-Meteo request
  BirdApi.kt       eBird requests + cached Lithuanian name list
  BirdMedia.kt     Wikipedia picture/summary + Xeno-canto song lookup
  Location.kt      Location lookup with a saved last-known location
  Notifier.kt      Notification channel and posting
  Workers.kt       SkyWorker, RareBirdWorker, MorningBirdWorker (WorkManager jobs)
  Scheduler.kt     Schedules the three jobs
  MainActivity.kt  Asks permissions, starts schedules, shows the bird card (UI built in code)
app/src/test/java/com/example/nightskywings/
  MoonPhaseTest.kt, ParsersTest.kt, TimeMathTest.kt   JVM unit tests
```

## API keys and configuration

Two free keys are needed. They go in `local.properties` at the project root, which is git-ignored and
must never be committed. Android Studio creates that file (with `sdk.dir`) on first open; add:

```
ebird.key=YOUR_EBIRD_KEY
xenocanto.key=YOUR_XENO_CANTO_KEY
```

- **eBird**: sign in at ebird.org, then request a key at https://ebird.org/api/keygen. Without it the app
  shows "No eBird key set" and the bird alerts send nothing.
- **Xeno-canto**: create an account at xeno-canto.org and get an API key from your account page.
  Without it the song button stays disabled; everything else works.
- Open-Meteo and Wikipedia need no key.

Rebuild after changing keys (they are compiled into `BuildConfig`). Other settings are in `Config.kt`:
fallback location (Klaipeda, 55.7033, 21.1443), `CLEAR_THRESHOLD` (30%) and how often the name list
refreshes (30 days). Alert times are in `Scheduler.kt`.

Note: keys in `BuildConfig` end up inside the APK. That's fine for a personal app; don't publish an
APK built with your keys.

## Build and run (Android Studio + POCO X6 Pro)

1. Install Android Studio (Ladybug 2024.2 or newer) from https://developer.android.com/studio.
2. File > Open, choose this repository folder. Let Gradle sync finish (it downloads the Gradle wrapper,
   Android SDK 35 and the libraries). Accept any SDK licence prompts.
3. Add your keys to `local.properties` as above, then sync again.
4. On the POCO X6 Pro: Settings > About phone > tap **OS version** (HyperOS) seven times to enable
   developer options, then Settings > Additional settings > Developer options > turn on
   **USB debugging** (and **Install via USB** if HyperOS asks).
5. Plug the phone in, pick it in the device menu, press **Run**. Accept the location and notification
   prompts on the phone.
6. On the phone, allow **Autostart** and set battery to **No restrictions** for Night Sky & Wings
   (Settings > Apps > Manage apps > Night Sky & Wings). Xiaomi phones otherwise stop background jobs
   and alerts won't arrive.

Command line, from the project root: `./gradlew testDebugUnitTest` (unit tests) and
`./gradlew assembleDebug` (APK in `app/build/outputs/apk/debug/`).

**Testing the alerts without waiting:** temporarily add to the end of `MainActivity.onCreate`:

```kotlin
androidx.work.WorkManager.getInstance(this).enqueue(
    androidx.work.OneTimeWorkRequestBuilder<SkyWorker>().build()
)
```

(and the same for `RareBirdWorker` and `MorningBirdWorker`). Set `CLEAR_THRESHOLD` to 100 so the
stargazing alert always fires. Undo both afterwards.

## Done vs. not done

Done:
- All features from the design doc, as a complete Android Studio project with Gradle wrapper.
- The fixes listed below.
- 16 JVM unit tests for moon phase, all JSON parsing and schedule-time maths.
- Verified: `lintDebug`, `testDebugUnitTest` and `assembleDebug` all pass; a debug APK is produced.

Not done:
- Never run on a device or emulator; no behaviour has been observed.
- No instrumented (on-device) tests; the workers, notifications and UI are untested at runtime.
- No app icon (uses the Android default) and no layout/Compose UI; the screen is built in code.
- Package name is `com.example.nightskywings`. Change it before any Play Store release.

## Known issues and limits

- **Xeno-canto query format not verified.** The v3 API docs couldn't be reached from the build
  environment. `BirdMedia.song()` tries three query forms (`gen:` + `sp:` with quality and type filters,
  then without filters, then the doc's original `sp:"Genus species"`). If songs never load, check
  https://xeno-canto.org/explore/api and fix the query list.
- **Location**: the app reads your location while it's open and saves it. Background jobs use that
  saved location (or Klaipeda if the app was never opened with location allowed). If you move, open
  the app to update it. Getting live location in the background would need the background-location
  permission, which this app deliberately doesn't ask for.
- **Moon model** uses the mean lunar cycle, so phase names can be about a day off. Fine for tips.
- **Wikipedia text** is a page summary, not a short fact, and is English only.
- **WorkManager timing** is approximate: Android may run jobs late, especially on battery saver.
- **UI strings are hardcoded English.** Lint reports 6 `SetTextI18n` warnings in `MainActivity.kt`.
  Only the bird names are bilingual; the surrounding labels ("Play song", "Loading bird of the day")
  are English literals. Move them to `strings.xml` if you ever want a Lithuanian interface.
- **Rare-bird memory** (`seen_rare`) grows without limit. Small in practice; could be trimmed to recent dates.
- Library versions (AGP 8.7.3, compileSdk 35, AndroidX) were picked as known-good, not latest.
  Android Studio may suggest upgrades; they're optional.

## Fixes applied (compared with the design doc's code)

| # | Problem in the doc | Fix |
| --- | --- | --- |
| 1 | `Task.await()` used without its library, so it would not compile | Added `kotlinx-coroutines-play-services` |
| 2 | `org.json:json` added as an app dependency, though Android already includes `org.json` | Removed it from the app; kept only as `testImplementation`, because JVM unit tests need a real copy |
| 3 | Morning job comment said 07:30, code scheduled 07:00 | Scheduler takes hour and minute; now 07:30 |
| 4 | Background jobs on Android 10+ can't get location without background permission, so they silently used Klaipeda | The app saves location while open; jobs use the saved value |
| 5 | The full eBird taxonomy (thousands of species) was downloaded on every call | Cached in app storage, refreshed every 30 days |
| 6 | Xeno-canto v3 query format possibly wrong | Unverified; tries several query forms (see Known issues) |
| 7 | Moon phase names had wrong band limits: the full moon was labelled "Waxing gibbous". Found by the new unit tests | Eight equal bands centred on each phase |
| 8 | Any network error (no signal, timeout) threw an exception and could crash the app | `Net.get` returns null on errors; all JSON parsing tolerates bad responses |
| 9 | `AppCompatActivity` crashes under the non-AppCompat theme of a new Studio project | App uses an AppCompat theme |
| 10 | API keys had to be pasted into source code | Keys come from git-ignored `local.properties` |
| 11 | Card loaded before the permission dialog was answered, and no notification-permission check | Card loads after the permission answer; notifications check permission on Android 13+ |

## Checks run and not run

| Check | Result | Where |
| --- | --- | --- |
| `assembleDebug` (full Android build, AGP 8.7.3, compileSdk 35) | **Passed**, produced a 5.2 MB debug APK | Linux CI container, SDK 35 + build-tools 35.0.0 |
| `testDebugUnitTest` (16 tests, 3 classes) | **Passed**, 0 failures, 0 errors | same |
| `lintDebug` | **Passed**: 0 errors, 11 warnings | same |
| JVM unit tests compiled with warnings-as-errors | Passed | separate JVM Gradle project |
| Running on a device or emulator | **Not run** | — |
| Live calls to Open-Meteo, eBird, Wikipedia, Xeno-canto | **Not run**: Open-Meteo and Wikipedia rate-limited (HTTP 429) from the build machine; eBird needs a key; Xeno-canto docs are behind an anti-scraping gate | — |

The 11 lint warnings are all benign: 5 say newer library versions exist (optional upgrades) and 6 are
the `SetTextI18n` hardcoded-string warnings described under Known issues. None block anything.

Reproduce the build on any machine with the Android SDK installed:

```
./gradlew lintDebug testDebugUnitTest assembleDebug
```

Note: Maven Central intermittently answers HTTP 429 to CI machines. If the build fails with 429
while downloading a library, just run it again — it is a rate limit, not a code problem.
