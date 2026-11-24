# Extension Runtime — Step 2: GeckoView Engine Integration Prototype

**Status:** Implementation committed; CI build and APK verification pending  
**Date:** 2026-11-24  
**Branch:** `feature/via-style-search-home`

## Objective

Verify that the project can resolve and embed a current stable GeckoView engine without destabilizing the existing API-19 WebView browser. The GeckoView work is isolated in a separate Android application module so the main OmegA Browser keeps its existing package, minimum SDK and WebView implementation.

## Selected engine

- Maven coordinate: `org.mozilla.geckoview:geckoview:160.0.20261116130455`
- Channel: Stable
- Java/Kotlin compatibility: Java 17
- GeckoView minimum Android requirement: API 26 (Android 8.0)
- GeckoView documentation: https://mozilla.github.io/geckoview/consumer/docs/geckoview-quick-start
- Mozilla minimum system requirements: https://wiki.mozilla.org/Mobile/GeckoView

The dependency is declared only in `:gecko-poc`; it is not included in the main `:app` APK.

## Implementation added

- New `:gecko-poc` Android application module.
- Separate package ID: `com.octopusworldpro.omega.browser.geckopoc`.
- API 26 minimum SDK, leaving the main browser's API 19 floor unchanged.
- GeckoRuntime singleton for the POC process.
- GeckoSession open/close lifecycle.
- GeckoView host with URL/search input.
- Basic navigation using `session.loadUri()`.
- Back/forward via GeckoSession navigation controller.
- Reload support.
- Explicit session/view cleanup in Activity destruction.
- CI builds both the existing WebView APK and the isolated GeckoView POC APK, then uploads them as separate artifacts.

## Isolation and compatibility decision

The main browser has **not** been migrated. This avoids silently dropping Android 4.4–7.1 support or changing the user's default browser engine before the GeckoView POC is validated.

The GeckoView POC is a separate installable test application and is not yet an extension runtime. It does not yet install or execute WebExtensions, implement a Chrome API compatibility layer, or migrate downloads, file chooser, identity settings, bookmarks, history, tabs or private browsing.

## Acceptance checklist

- [x] Verified official GeckoView embedding workflow and Java 17 requirement.
- [x] Selected a current stable GeckoView artifact.
- [x] Kept GeckoView in an isolated module with API 26 minimum.
- [x] Preserved the main app's API 19 configuration and WebView code.
- [x] Added GeckoRuntime and GeckoSession lifecycle.
- [x] Added basic load/back/forward/reload UI.
- [x] Added CI build and artifact upload steps for both APKs.
- [ ] CI dependency resolution and compilation pass.
- [ ] GeckoView APK archive verification and artifact upload pass.
- [ ] Physical-device install, first launch and real-page navigation test.
- [ ] APK size and cold-start measurements.

## Current result

The code and CI pipeline for the isolated engine prototype are committed. **Step 2 is not accepted as complete until CI proves that both APKs build and the GeckoView artifact is uploaded.** Device validation and performance measurements remain later acceptance gates; static CI alone cannot establish runtime behavior.
