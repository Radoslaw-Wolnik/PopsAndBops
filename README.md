# PopsAndBops

PopsAndBops is a local-first Kotlin + Jetpack Compose soundboard app. The main screen is a movable dotted sound map with colourful blob buttons, bounded pinch zoom, and a fixed record button in the centre.

## What It Does

- Move around a dotted sound map in every direction.
- Pinch or use the zoom controls to zoom within a comfortable range.
- Tap a blob to play it with a small pulse animation.
- Arrange the map by dragging pinned blobs onto concentric guide rings.
- Auto-arrange pinned blobs into tidy ring slots.
- Record a new sound from the centre button.
- Recordings stop at 10 seconds max.
- While recording, a floating recording blob shows elapsed time and a live waveform.
- After recording, trim the waveform, name the sound, and save it.
- New recordings are added to the outer edge of the map so the board grows outward.
- Open the library to see every saved sound.
- Rename sounds from the editor.
- Pin or unpin sounds from the map while keeping them in the library.
- Change a blob colour from the built-in palette.
- Pick a pregenerated blob shape or edit the shape by adding/removing curve points and dragging handles.
- Revisit the waveform trim later from the editor.

## Local-Only Storage

There is no backend. The app does not request the Android `INTERNET` permission.

Recordings are stored as `.m4a` files in the app's private on-device files directory. Blob metadata such as name, colour, shape, map position, pin state, waveform preview, and trim range is stored in local `SharedPreferences` as JSON.

Android cloud backup is disabled in the manifest so app data stays device-local.

## Code Structure

- `data/` contains the sound blob model, preset palette/shape library, map positioning, and local repository.
- `audio/` contains the `MediaRecorder` wrapper and playback logic for both recorded clips and preset tones.
- `ui/map/` contains the draggable, arrangeable, and zoomable sound map.
- `ui/library/` contains the library list.
- `ui/editor/` contains the blob editor for names, pinning, colours, shape curves, and trims.
- `ui/recording/` contains the recording overlay and trim/save sheet.
- `ui/components/` contains reusable blob and waveform drawing components.
- `ui/theme/` contains the app colour scheme.

## Build

From the project root:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

To install/run from Android Studio, open the project and run the `app` configuration on a device or emulator with microphone permission available.
