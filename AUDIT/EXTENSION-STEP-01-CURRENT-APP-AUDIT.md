# Extension Runtime — Step 1: Current Android App Audit

**Status:** Complete (repository/source inspection only)  
**Date:** 2026-11-24  
**Repository:** `octopusworldpro-web/Via-Browser`  
**Branch audited:** `feature/via-style-search-home`  
**Audited revision:** `ae24f0bf8969cd2b86230ecde077fb8651d86306`

## 1. Project structure and build

The active branch contains a buildable, independently authored Android Studio/Gradle project under `app/`. This is not the original Via Browser source tree and is not a feature-for-feature reconstruction of Via 7.4.1.

| Item | Verified value |
|---|---|
| Android application module | `:app` |
| Application ID | `com.octopusworldpro.omega.browser` |
| Namespace | `com.octopusworldpro.omega.browser` |
| Language | Kotlin |
| Android Gradle Plugin | 8.6.1 |
| Kotlin Android plugin | 2.0.21 |
| CI Gradle | 8.9 |
| Compile SDK | 35 |
| Target SDK | 28 |
| Minimum SDK | 19 |
| Java/Kotlin target | 17 |
| Main activity | `MainActivity.kt` |
| Manifest permissions | INTERNET, ACCESS_NETWORK_STATE |

The project currently resolves dependencies from Google Maven and Maven Central. No GeckoView dependency or Mozilla Maven repository is configured.

## 2. Current browser engine and architecture

The active app creates one Android `WebView` directly inside `MainActivity.onCreate/buildShell`. It configures `WebSettings`, `WebViewClient`, `WebChromeClient` and a `DownloadListener` inline.

The main activity is a monolithic UI/controller file (about 38.7 KB at the audited revision). It owns:
- Address/search input and suggestions.
- Home screen and shortcut grid.
- Page navigation and page loading callbacks.
- URL-list tab model (not independent WebView sessions).
- Bookmark/history in-memory lists and SharedPreferences persistence.
- DownloadManager handoff.
- File chooser callback and legacy `onActivityResult` flow.
- Device Identity settings and browser preferences.
- Extension ZIP picker, extraction, validation, listing and removal.

There is no browser-engine abstraction, no separate tab engine/session class and no extension runtime adapter in the current source.

## 3. WebView-specific integration points to migrate

| Area | Current implementation | Migration impact |
|---|---|---|
| Engine creation | `WebView(this)` in `buildShell()` | Replace with GeckoView + GeckoSession lifecycle |
| Settings | `web.settings.*` | Map only supported settings to GeckoView APIs |
| Navigation | `WebViewClient` page callbacks | Rebuild using GeckoSession navigation/progress delegates |
| Browser chrome | `WebChromeClient` | Reimplement file chooser, permission and popup flows using GeckoView delegates |
| Downloads | `DownloadListener` + Android DownloadManager | Add GeckoView content/download delegate integration |
| File upload | `onShowFileChooser` + `REQUEST_FILE` | Reconnect GeckoView file-prompt delegate to Android document picker |
| JavaScript setting | `settings.javaScriptEnabled` | Verify available GeckoView setting/API and persist preference |
| Desktop mode | User-Agent override | Verify supported GeckoView setting and behavior |
| Language identity | `loadUrl(url, headers)` | Rework using GeckoView-supported request/header or locale controls |
| Back navigation | `web.canGoBack()/goBack()` | Use GeckoSession navigation session/history APIs |
| Tab switching | One WebView + URL strings | Define one GeckoSession per real tab or document a single-session model |
| Cleanup | No engine-specific lifecycle handling | Add session/view pause, close and Activity lifecycle handling |

## 4. Existing extension code — exact capability boundary

The current Extension Manager:
- Opens a ZIP using Android `ACTION_OPEN_DOCUMENT`.
- Extracts files to app-private `filesDir/extensions`.
- Applies a 20 MB extracted-size limit and 2,000-entry limit.
- Rejects path traversal and unsafe archive paths.
- Requires `manifest.json`, Manifest V2 or V3, name and version.
- Lists and removes extracted package directories.

It does **not**:
- Register an extension with a browser engine.
- Execute background scripts or service workers.
- Inject content scripts.
- Render extension popup pages.
- Implement `browser.*` or `chrome.*` extension APIs.
- Broker extension permissions or extension messaging.

The current UI explicitly tells the user that extension scripts/APIs are not executed. Keep this truthful boundary until the real runtime is integrated.

## 5. Existing features that must be preserved

- OmegA dark/violet home screen, branding and navigation shell.
- Search engine selection and suggestions.
- Back/forward/home/reload navigation behavior.
- Current URL and basic tab list.
- Bookmarks and history persistence.
- DownloadManager integration and downloads screen.
- Web file chooser.
- JavaScript preference.
- Desktop-site preference.
- Device Identity User-Agent and Accept-Language behavior.
- Clear browsing data flow.
- Safe WebView defaults currently present: file access disabled, content access enabled, mixed content blocked, Safe Browsing enabled where available.

Important: the current URL-list tab model does not provide isolated tab sessions. Do not describe it as full multi-session tabs.

## 6. GeckoView migration constraints discovered

1. GeckoView is a browser-engine replacement, not a drop-in extension library for Android WebView.
2. The app's current minimum SDK is 19. A GeckoView migration may require raising the supported Android floor; this must be verified against the selected GeckoView release before changing Gradle configuration.
3. The current app uses only two manifest permissions. Additional permissions must be added only when a concrete GeckoView/browser feature requires them.
4. Existing WebView callbacks and settings cannot be carried over mechanically; they require explicit GeckoView delegate/API equivalents.
5. Firefox/Gecko WebExtensions are not identical to Chrome extensions. Chrome MV3 service-worker and API compatibility must be treated as a tested subset, not promised universally.
6. The original Via APK audit and this native source audit are separate evidence sets. The current source project is available, but it is not the original Via source project.

## 7. Step 1 acceptance checklist

- [x] Confirmed current branch and audited revision.
- [x] Located Android Gradle project, app module and build configuration.
- [x] Inspected Android manifest and declared permissions.
- [x] Inspected main activity and current engine ownership.
- [x] Mapped WebView-specific navigation, settings, downloads and file-picker integrations.
- [x] Inspected extension ZIP install/manage implementation and documented that it does not execute extensions.
- [x] Recorded existing browser features and regression requirements.
- [x] Recorded GeckoView migration constraints and unknowns.
- [ ] No code migration performed in this audit step.
- [ ] No claim of extension runtime functionality or device validation.

## 8. Step 1 result and handoff

**Step 1 is complete as a source and architecture audit.** The repository does contain a buildable native Android project, so the older audit statement that no Android Studio/Gradle project exists is no longer true for the new OmegA source project. However, this source is an independent lightweight browser foundation, not the original Via source.

Step 2 can now add GeckoView dependencies and create an isolated engine-integration layer on this feature branch. It must first verify the selected GeckoView artifact, supported Android API floor and API surface, then migrate the current WebView integration without deleting the existing working implementation until the GeckoView path builds and passes basic navigation tests.
