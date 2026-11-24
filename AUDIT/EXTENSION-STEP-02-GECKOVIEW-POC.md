# Extension Runtime — Step 2: GeckoView Engine Integration Prototype

**Status:** CI build and both APK artifact uploads passed; physical-device validation pending  
**Date:** 2026-11-24  
**Branch:** `feature/via-style-search-home`

## Objective

Verify that the project can resolve and embed GeckoView without destabilizing the existing API-19 WebView browser. GeckoView remains isolated in a separate Android application module.

## Selected engine

- Maven coordinate: `org.mozilla.geckoview:geckoview:140.0.20250630155423`
- Channel: Stable
- Java/Kotlin compatibility: Java 17
- GeckoView minimum Android requirement: API 26 (Android 8.0)
- GeckoView documentation: https://mozilla.github.io/geckoview/consumer/docs/geckoview-quick-start
- Mozilla minimum system requirements: https://wiki.mozilla.org/Mobile/GeckoView

The dependency is declared only in `:gecko-poc`; it is not included in the main `:app` APK. Version 160 was rejected because its transitive AndroidX/Media3 requirements exceeded this repository's current compile SDK and Android Gradle Plugin baseline. Version 140 resolved and compiled successfully under the existing toolchain.

## Implementation added

- Separate `:gecko-poc` Android application module.
- Separate package ID: `com.octopusworldpro.omega.browser.geckopoc`.
- API 26 minimum SDK; main browser API 19 floor unchanged.
- GeckoRuntime singleton and GeckoSession lifecycle.
- GeckoView host with URL/search input.
- Basic navigation using `session.loadUri()`.
- Back/forward using `GeckoSession.goBack()` and `goForward()`, with availability tracked by NavigationDelegate callbacks.
- Reload using `session.reload()`.
- Session/view cleanup in Activity destruction.
- CI builds both the existing WebView APK and isolated GeckoView POC APK and uploads them separately.

## Isolation and compatibility decision

The main browser has **not** been migrated. This avoids silently dropping Android 4.4–7.1 support or changing the default browser engine before the POC is validated.

The GeckoView POC is a separate test application, not yet a complete extension runtime. It does not yet install or execute WebExtensions, implement a Chrome API compatibility layer, or migrate downloads, file chooser, identity settings, bookmarks, history, tabs or private browsing.

## Acceptance checklist

- [x] Verified GeckoView embedding workflow and Java 17 requirement.
- [x] Selected a stable GeckoView artifact compatible with the current project toolchain.
- [x] Kept GeckoView in an isolated module with API 26 minimum.
- [x] Preserved the main app's API 19 configuration and WebView implementation.
- [x] Added GeckoRuntime and GeckoSession lifecycle.
- [x] Added basic load/back/forward/reload UI.
- [x] Added CI build and artifact upload steps for both APKs.
- [x] CI dependency resolution and compilation passed.
- [x] Both APKs passed workflow verification and were uploaded.
- [ ] Physical-device install, first launch and real-page navigation test.
- [ ] APK size and cold-start measurements.

## CI verification

- Workflow run: [#63 — successful](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45410405687)
- Commit: `c6b9f87caeeb8430b7cb9852c162e31d47fbcf1c`
- Main WebView APK build: passed.
- GeckoView POC APK build: passed.
- APK verification steps: passed.
- Artifact uploads: passed.
- Uploaded artifacts:
  - `OmegA-Browser-native-debug`
  - `OmegA-GeckoView-POC-debug`
- GeckoView artifact archive size: 309,440,460 bytes. This is the uploaded artifact archive size, not a measured installed APK size.

## Result and remaining gates

**CI acceptance for Step 2 is complete.** Both APKs built and both artifacts were uploaded successfully. Runtime behavior is not yet confirmed because no physical-device test has been performed. Device installation/navigation and size/cold-start measurements remain explicit follow-up gates.
