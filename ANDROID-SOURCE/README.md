# Native OmegA Browser Source — Initial Implementation

## What was added

A new Android Studio/Gradle project now exists in the repository:

- Root Gradle project and Android application module
- Kotlin Android entry point and Android manifest
- Independent application ID: `com.octopusworldpro.omega.browser`
- OmegA dark home screen and browser navigation shell
- WebView-based page loading, address/search input, back/forward/home, basic tab list
- Home shortcuts for Bookmarks, History, Downloads, C3C FBState and Device Identity
- Consent-gated local document selection for FBState import, with masked cookie-name preview and c_user/xs presence check
- Browser-exposed User-Agent setting; no hardware identifier access
- Android Actions workflow to build a debug APK and upload it as an artifact

## Important scope

This is a newly authored lightweight browser foundation, **not a source recovery or feature-for-feature clone of Via Browser 7.4.1**. Existing Via-specific functions have not been ported into this new project.

The first pass contains functional navigation and an initial OmegA screen, but several screens are still placeholders or in-memory prototypes:
- Bookmarks/history are not yet persisted across app restarts.
- Tab switching is a simple URL list, not isolated multi-WebView tab sessions.
- Downloads manager is not connected to Android DownloadManager yet.
- Appearance/search-engine/JavaScript settings are not fully wired.
- Device Identity language/locale/time-zone adapter is incomplete.
- FBState import currently validates cookie names only; full parser parity, normalization, export formats, and secure local persistence still need integration.
- No extension runtime is included.
- No physical-device QA or production signing is complete.

## Build

Open the repository in Android Studio, or run:

```bash
gradle :app:assembleDebug
```

The CI workflow is `.github/workflows/android-source-build.yml`. A debug APK is considered buildable only after this workflow passes and uploads `OmegA-Browser-native-debug`.

## Security boundaries

- FBState import is initiated by the user through Android's document picker and requires an explicit confirmation.
- No automatic WebView cookie extraction, background session collection, credential collection, cloud sync or upload is implemented.
- Session cookie values are not displayed in the preview.
- Device identity is limited to browser-exposed values; IMEI, serial, Android ID, MAC and other hardware identifiers are out of scope.
