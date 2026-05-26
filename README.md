# PopsAndBops

PopsAndBops is a local-first Android soundboard built with Kotlin and Jetpack Compose. It lets you play preset sounds, record short custom clips, arrange sounds on a dotted map, and edit each sound's name, color, shape, pin state, and trim range.

The app is intentionally device-local: there is no backend, no account system, and no `INTERNET` permission.

## Features

- Interactive sound map with colorful blob buttons.
- Pan, pinch zoom, zoom buttons, and quick recentering.
- Built-in preset tones for a useful board on first launch.
- Tap any blob to play it with visual playback feedback.
- Record new sounds with the centered record button.
- Recording limit of 10 seconds, with elapsed time and a live waveform preview.
- Trim and name a recording before saving it.
- Library view for all preset and recorded sounds.
- Detail view with waveform, duration, playback, and edit access.
- Editor for renaming sounds, pinning or unpinning them from the map, changing colors, choosing shape presets, editing blob curve points, and adjusting trims.
- Arrange mode for dragging pinned sounds onto ring slots.
- Auto-arrange for quickly tidying pinned sounds into concentric rings.

## How To Use

1. Launch the app and use the map as the main soundboard.
2. Tap a blob to play that sound.
3. Tap the center record button to start recording, then tap stop when finished.
4. Name and trim the recording in the bottom sheet, then save it.
5. Open the library from the map to browse every saved sound.
6. Select a sound to preview it, or tap edit to customize it.
7. Use Arrange on the map to move pinned sounds manually, or Auto to place them into tidy ring slots.

Android will ask for microphone permission before the first recording. If permission is denied, playback and library browsing still work.

## Storage And Privacy

Recordings are saved as `.m4a` files in the app's private files directory. Sound metadata is stored in `SharedPreferences` as JSON, including names, colors, shapes, waveform previews, map positions, pin states, and trim ranges.

Android backup is disabled, so app data stays on the device unless the user exports it outside the app through system tools.

## Project Structure

- `app/src/main/java/com/example/popsandbops/audio/` handles recording and playback.
- `app/src/main/java/com/example/popsandbops/data/` contains sound models, defaults, persistence, trim safety, and map layout logic.
- `app/src/main/java/com/example/popsandbops/ui/map/` contains the sound map and arrange mode.
- `app/src/main/java/com/example/popsandbops/ui/library/` contains the library grid and detail view.
- `app/src/main/java/com/example/popsandbops/ui/editor/` contains sound editing controls.
- `app/src/main/java/com/example/popsandbops/ui/recording/` contains the recording overlay and trim/save sheet.
- `app/src/main/java/com/example/popsandbops/ui/components/` contains shared blob, waveform, and press-feedback UI.
- `app/src/test/` contains JVM unit tests for pure app logic.
- `app/src/androidTest/` contains Android instrumentation smoke tests.

## Build And Run

Open the project in Android Studio and run the `app` configuration on an emulator or device running Android 7.0/API 24 or newer.

From the project root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is written under `app/build/outputs/apk/debug/`.

## Tests And Checks

Run local JVM unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Run Android lint:

```powershell
.\gradlew.bat :app:lintDebug
```

Run instrumentation tests on a connected device or running emulator:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Current Quality Notes

The project builds, lint runs successfully, and the JVM unit test suite covers the sound trim rules, default blob data, and map layout logic. Lint may still report dependency-version warnings as the Android Gradle Plugin, Kotlin, and Compose BOM move forward; those are best handled as an explicit dependency upgrade pass.
