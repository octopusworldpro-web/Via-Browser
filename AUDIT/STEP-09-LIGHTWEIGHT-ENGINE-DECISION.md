# Step 3 — Lightweight Extension Engine Decision

**Decision date:** 2026-11-24  
**Priorities:** preserve Via Browser, keep APK small, retain API 19 where feasible.

## Decision

Do not bundle GeckoView into the main OmegA APK. It is a major browser-core replacement, not a small add-on. A recent ARM64 package measurement found an approximately 90 MB GeckoView AAR; Mozilla's GeckoView minimum-system-requirements page lists Android 8.0 / API 26. These conflict with OmegA's current minSdk 19 and the lightweight requirement.

GeckoView supports embedded WebExtensions, but Firefox for Android does not support MV3 background service workers. It would add substantial size and still not provide full Chrome extension compatibility.

## Why WebView is retained

The current Via browser uses Android WebView. WebView shares Chromium rendering code, but is a content embedder and does not include Chrome's browser-specific extension host. A Chrome-like extension runtime cannot be enabled by a WebView setting.

The repository lacks the buildable Via Android Studio/Gradle source project. The current resource-repackaging workflow cannot safely add isolated execution contexts or native manager screens.

## Lightweight path selected for investigation

Retain WebView and investigate a deliberately limited, opt-in extension-compatible subset once buildable source is available:

- Local-only package staging and strict manifest validation.
- Content scripts only, with isolated execution contexts and explicit match patterns.
- A small allowlisted API surface; unsupported Chrome APIs must be rejected and reported.
- No general background service worker, arbitrary native bridge, broad cookie/password/history access or Web Store claim.
- Per-extension storage with quotas and atomic lifecycle.
- Least-privilege host permission prompts.
- Existing userscripts remain separate and unchanged.

This is a **limited WebExtension-compatible runtime**, not full Chrome extension support. If safe isolated execution cannot be implemented with the actual source and supported platform APIs, do not ship extension execution.

## APK size policy

1. Do not bundle a second browser engine or duplicate native browser libraries.
2. No bundled fonts, analytics, remote code, heavyweight UI framework or background service by default.
3. Keep extension packages user-installed and outside the base APK; enforce archive/file quotas.
4. Use platform/WebView capabilities where safe; do not add a large JS engine solely to imitate Chrome.
5. Record base and per-ABI APK size, install size, cold start and memory before/after each dependency.
6. Reject heavyweight dependencies without a measured feature benefit.

## Required source and acceptance gates

The original buildable Via Android source/Gradle project is still required. Once available:
- Create a minimal prototype on a separate branch without changing the current APK.
- Measure universal and per-ABI APK sizes, install size, cold start and idle/active memory.
- Test isolation, origin matching, permissions, storage quotas, navigation lifecycle and uninstall cleanup.
- Verify Via browsing, userscripts, cookies, bookmarks, history, downloads and private mode.
- Keep the runtime disabled if any security or regression gate fails.

## References

- GeckoView WebExtensions: https://firefox-source-docs.mozilla.org/mobile/android/geckoview/consumer/web-extensions.html
- GeckoView platform minimum requirements: https://wiki.mozilla.org/Mobile/GeckoView
- Firefox Android MV3 limitations: https://extensionworkshop.com/documentation/develop/developing-extensions-for-firefox-for-android/
- Firefox background service-worker support: https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/manifest.json/background
- Chromium WebView architecture: https://chromium.googlesource.com/chromium/src/+/main/android_webview/docs/architecture.md
- Android WebView overview: https://developer.chrome.google.cn/docs/webview

## Status

**Lightweight engine choice decided: retain WebView; do not bundle GeckoView.** Step 3 feasibility/architecture is updated, but the extension runtime itself remains unimplemented and disabled until the buildable Android source project is available.
