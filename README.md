# PopsAndBops

PopsAndBops is a local-first Android soundboard for short playful sounds. It lets people play colorful blob buttons, record new sounds, trim them, rename them, arrange them on a dotted map, and customize each blob's color and vector-like shape.

The app is intentionally private and offline. There is no backend, no account system, and no `INTERNET` permission.

## What The App Does

- Shows pinned sounds as draggable blob buttons on a pan-and-zoom map.
- Plays built-in preset tones and saved recordings.
- Records short clips through the centered record flow.
- Saves recordings as private `.m4a` files.
- Stores sound metadata locally in `SharedPreferences`.
- Provides a library view for every preset and recorded sound.
- Lets users edit names, colors, trim ranges, pin state, shape presets, and custom blob curves.
- Supports arrange mode, auto-arrange, overlap avoidance, and map recentering.

## Tech Stack

- Kotlin
- Android SDK 24+
- Jetpack Compose
- Material 3
- Android `MediaRecorder` for recording
- Android `MediaPlayer` and `AudioTrack` for playback
- JVM unit tests for pure logic

## Project Structure

```text
app/src/main/java/com/example/popsandbops/
  MainActivity.kt
  audio/
    AudioRecorder.kt
    SoundPlayer.kt
    WaveformNormalizer.kt
  data/
    BlobAssetLibrary.kt
    SoundBlob.kt
    SoundBlobRepository.kt
  ui/
    PopsAndBopsApp.kt
    SoundboardUiState.kt
    SoundboardViewModel.kt
    components/
      BlobButton.kt
      PressFeedback.kt
      WaveformView.kt
    editor/
      SoundEditorScreen.kt
    library/
      SoundLibraryScreen.kt
    map/
      SoundMapScreen.kt
    recording/
      RecordingOverlay.kt
    theme/
      Color.kt
      Theme.kt
      Type.kt
```

## Architecture

The app uses a simple Compose + ViewModel architecture.

`MainActivity` starts Compose and applies the app theme.

`PopsAndBopsApp` is the top-level UI coordinator. It reads `SoundboardUiState`, decides whether the user is on the map, library, editor, or recording flow, and wires callbacks into the ViewModel.

`SoundboardViewModel` owns app behavior. It loads blobs, handles playback, records audio, edits metadata, manages selection, saves recordings, updates map positions, and persists changes through the repository.

`SoundboardUiState` is the screen state object. Compose screens receive state and callbacks rather than reaching into persistence or audio APIs directly.

`SoundBlobRepository` is the local persistence boundary. It loads and saves `SoundBlob` metadata as JSON in `SharedPreferences`, creates private recording files, and provides default preset blobs when there is no saved data.

`audio/` wraps Android audio APIs. The ViewModel calls these wrappers so UI code stays platform-light.

## Data Model

`SoundBlob` is the core model. It contains:

- identity and display name
- map position
- color
- preset/custom shape data
- waveform preview
- trim range
- recording path or built-in tone
- pin state

Blob shapes are stored as `BlobShapeNode` values: each node has an anchor, an incoming handle, and an outgoing handle. This makes custom blobs editable like a small vector shape instead of only using fixed radial points.

Older/simple shape data is still represented as radial `shapePoints`; `effectiveShapeNodes()` converts or chooses the best available editable node list.

## Important Implementations

### Blob Drawing

`ui/components/BlobButton.kt` contains the shared blob rendering code. It builds a cubic `Path` from editable shape nodes, fills it with the blob color, draws a darker outline, and optionally overlays the blob name. Both map buttons and previews use this component so visual changes stay consistent.

### Shape Editing

`ui/editor/SoundEditorScreen.kt` contains the vector-style shape editor. Users can:

- drag anchor points freely
- drag curve handles
- mirror or free curve handles
- smooth or corner a selected point
- double-tap the curve to insert a point without changing the visible outline

Point insertion uses De Casteljau cubic splitting, so the curve keeps its current shape when a new node is added.

### Map Layout

`data/BlobMapLayout` defines map layout constants, suggested ring positions, overlap checks, and overlap resolution. `SoundMapScreen` uses this logic for arrange mode and drag/drop placement.

Map positions are stored in density-independent world coordinates. The map converts them to screen coordinates with the current pan and zoom.

### Recording Flow

`RecordingOverlay.kt` shows the recording UI. While recording, it displays elapsed time and a live waveform-like preview. After recording, it shows a save flow with name, waveform, trim slider, discard, and save actions.

`AudioRecorder` writes `.m4a` files with AAC audio into the app's private files directory. `WaveformNormalizer` creates lightweight waveform samples used by the UI.

### Playback

`SoundPlayer` plays recorded clips with `MediaPlayer` and generated preset tones with `AudioTrack`. Playback respects each blob's trim range.

## Build

Open the project in Android Studio and run the `app` configuration on an emulator or Android device.

From the project root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/
```

## Tests

Run JVM unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

Build the debug app:

```powershell
.\gradlew.bat assembleDebug
```

Run Android instrumentation tests on a connected device or emulator:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Contributing

Before changing code, find the owner area:

- map behavior: `ui/map/` and `data/BlobMapLayout`
- blob drawing: `ui/components/BlobButton.kt`
- shape editing: `ui/editor/SoundEditorScreen.kt`
- recording: `ui/recording/`, `audio/AudioRecorder.kt`, and `SoundboardViewModel`
- persistence: `data/SoundBlobRepository.kt`
- playback: `audio/SoundPlayer.kt`

Keep UI components state-light. Screens should receive state and callbacks from `PopsAndBopsApp` or the ViewModel.

Keep persistence changes backward-compatible. Existing users may have saved JSON without newer fields, so repository parsing should always provide safe defaults.

When changing map placement or collision rules, add or update tests in `app/src/test/java/com/example/popsandbops/data/BlobMapLayoutTest.kt`.

When changing trims, recording limits, or persistence rules, prefer pure helper functions that can be tested with JVM unit tests.

Before committing, run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

## Privacy Notes

PopsAndBops keeps user data on the device. Recordings are stored in app-private files, and metadata is stored in app-private preferences. Android backup is disabled in the manifest configuration, and the app does not request network access.
