# Step 3 — Extension Engine Feasibility Decision

**Decision date:** 2026-11-24  
**Status:** Evaluated; production integration remains blocked.

## Executive decision

Do not attempt to bolt a Chrome extension runtime onto the current Via WebView APK by DEX/resource patching. The repository has no buildable Via Android source project, and Android WebView is not a Chrome browser embedder API. Keep extension execution disabled until the app source and an engine migration decision are available.

**GeckoView is the only evidence-backed candidate identified for a real embedded WebExtension runtime**, but it is not a drop-in Chrome extension engine and it changes the browser engine. It must not be silently inserted into OmegA Browser because that would replace Via's existing WebView-based browsing stack and may conflict with the lightweight/preservation goals.

## Candidate assessment

| Candidate | Evidence | Decision |
|---|---|---|
| Existing Via Android WebView | Chromium-based rendering does not expose Chrome's browser extension runtime or chrome.* extension host. | Reject as a ready-made Chrome extension engine. |
| GeckoView | Mozilla documents embedded WebExtension installation, content scripts and native messaging. | Viable engine-replacement prototype only; not approved for production integration yet. |
| Custom WebExtension runtime on WebView | Would require isolated worlds, manifest parsing, background lifecycle, API shims, permission broker, storage and browser event integration. | Reject as a shortcut; this is a substantial browser-engine project. |

## GeckoView compatibility boundary

Verified from Mozilla documentation:

- GeckoView can host WebExtensions and install bundled extensions through its WebExtension controller.
- It provides isolated content-script integration and native messaging between an extension and the Android app.
- Firefox's WebExtension background model is not Chrome's MV3 service-worker model: MDN documents that Firefox does not support background.service_worker and uses background scripts/event pages instead.
- Therefore GeckoView must not be advertised as full Chrome MV3 compatibility. A compatibility layer, explicit unsupported-API reporting and real extension test corpus would still be required.
- GeckoView is a standalone bundled engine, not a drop-in WebView patch. A migration would need app-level navigation, tabs, downloads, cookies, permissions, history, bookmarks, private browsing, and lifecycle integration to be rebuilt and regression-tested.

References:
- https://mozilla.github.io/geckoview/consumer/docs/web-extensions
- https://mozilla.github.io/geckoview/consumer/docs/geckoview-quick-start
- https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/manifest.json/background
- https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/Background_scripts
- https://developer.chrome.com/docs/webview

## Selected path

1. Keep current Via WebView engine and all extension execution/import toggles disabled.
2. Do not add a custom JS bridge that claims to emulate Chrome APIs.
3. Obtain the buildable, licensed Via Android source/Gradle project (or decide to build a separate browser shell).
4. If an engine replacement is approved, build a separate GeckoView proof-of-concept and measure APK size, cold start, memory, page compatibility and supported WebExtension APIs before deciding whether to migrate.
5. Define Chrome MV3 compatibility as a tested subset, not blanket support. Reject or clearly mark unsupported service-worker/API features.
6. Only after engine approval, implement the runtime adapter, manager UI and device regression suite.

## Required input to unblock production Step 3

- Buildable Via Android Studio/Gradle source project and its license/provenance.
- Confirmation whether replacing Via's WebView engine with GeckoView is acceptable, knowing this is a major browser-core change and not guaranteed to preserve the lightweight footprint.
- Target extension compatibility scope (for example, a tested subset of MV3 rather than all Chrome extensions).

## Acceptance status

This feasibility decision is complete. **Roadmap Step 3 is not complete**: there is no runtime integrated into the OmegA APK, no functioning extension manager, and no emulator/device extension test. The safety gate remains closed.
