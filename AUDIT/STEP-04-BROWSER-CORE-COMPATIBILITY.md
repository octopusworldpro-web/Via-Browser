# Step 04 — Browser Core Compatibility Baseline

**Status:** Static compatibility baseline complete; reproducible CI preservation audit added but not yet run  
**Base artifact:** Via Browser 7.4.1 APK  
**Target identity:** `com.octopusworldpro.omega.browser`  
**Compatibility floor:** Android API 19 (Android 4.4), subject to feature-specific runtime support

## 1. What was inspected

The original APK was re-inspected from the repository artifact. The package is a compiled APK, not an Android Studio/Gradle source project. Its DEX and assets expose the following implementation evidence:

- Android `WebView`, `WebViewClient`, `WebChromeClient`, `WebSettings` and WebKit support-library boundary APIs.
- WebView cookie management, service-worker controller/settings, web-message ports/listeners, proxy controller and resource-request interfaces.
- Download manager integration and external download-manager handoff identifiers.
- Browser history database/schema, bookmark import/export markers, incognito-mode identifiers and custom-tab connection service.
- Reader-mode JavaScript/CSS, read-aloud-related app components and the bundled ad/filter list at `assets/simple.txt`.
- A userscript manager with a Tampermonkey-compatible API surface, including `GM_getValue`, `GM_setValue`, `GM_registerMenuCommand`, `GM_xmlhttpRequest`, `GM_download`, `GM_addStyle` and `GM_openInTab`.

This is static string/structure evidence. It does not prove that every feature passes a live device test.

## 2. Core-preservation contract

The OmegA fork must retain the Via browsing engine and user-facing browser behavior. New OmegA features must be layered around the existing browser rather than replacing or destabilizing it.

| Core area | Evidence in base APK | Compatibility requirement |
|---|---|---|
| Page rendering/navigation | Android WebView classes and WebView client interfaces | Preserve normal navigation, redirects, page lifecycle and rendering settings |
| JavaScript / web UI | WebView and WebChromeClient interfaces | Preserve site JavaScript, dialogs, file chooser and web permission flows |
| Cookies and sessions | CookieManager and WebKit cookie boundary | Preserve cookie persistence and per-site session behavior |
| Downloads | DownloadManager and external download-manager integration strings | Preserve download detection, destination handling and external handoff |
| Bookmarks | Bookmark HTML/import-export markers and bookmark UI strings | Preserve bookmark data and import/export compatibility |
| History | History database schema and WebHistoryItem references | Preserve history records, ordering and clear-history behavior |
| Private browsing | Incognito-mode identifiers | Preserve separation of private-session data from regular browsing data |
| Reader mode | Reader-mode scripts, styles and text extraction strings | Preserve reader extraction and custom reader styling |
| Proxy | AndroidX WebKit ProxyController boundary | Keep proxy support feature-gated by the installed WebView provider |
| Service workers | Android WebKit ServiceWorkerController boundary | Preserve site behavior where the provider supports the relevant API |
| Web messaging | WebMessage / WebMessagePort boundary references | Do not assume every provider/API level supports all messaging methods |
| Custom tabs | Via custom-tabs connection service and AndroidX support references | Preserve external app/custom-tab handoff |
| Userscripts | GM_* APIs and userscript metadata markers | Keep the existing userscript engine distinct from the future Chrome extension runtime |

## 3. Android API compatibility rules

1. Keep the approved `minSdkVersion=19` until a documented feature or dependency makes that impossible.
2. Guard newer Android framework and AndroidX WebKit calls with runtime SDK checks and/or `WebViewFeature.isFeatureSupported(...)` checks where applicable.
3. A missing optional WebView feature must degrade gracefully; it must not prevent the browser shell or ordinary pages from opening.
4. Do not assume that the Android OS version alone determines WebView capabilities. The installed WebView provider/version is also relevant.
5. Avoid raising the minimum SDK just to enable extension-manager UI or optional WebKit APIs.
6. Preserve the existing Via code namespace (`mark.via.*`) unless verified source-level references require a specific migration. The Android application ID remains a separate OmegA identity.

## 4. Extension boundary

The base APK contains a userscript manager, but this inspection found no `chrome.runtime`, `chrome-extension://` or identifiable WebExtension runtime markers.

Therefore:

- Existing userscript support is a browser-core feature to preserve.
- ZIP/CRX import, extension lifecycle, background execution, permissions, content scripts and Chrome Web Store compatibility are a separate compatibility layer planned for Steps 05–07.
- Do not label userscript execution as Chrome extension support.
- Do not replace the WebView engine or silently remove Via userscript support to make room for extensions.
- Extension APIs must be bridged only through a deliberately scoped runtime layer; unsupported APIs must be reported rather than falsely advertised as compatible.

## 5. Security and data-integrity guardrails

- Review the base manifest's `usesCleartextTraffic`, backup, storage and install-package settings before release; do not blindly copy permissive settings into the final manifest.
- Keep browser cookies, history, downloads and private-session storage intact during any package-identity migration.
- Do not share regular-profile cookies with extension background contexts by default.
- Keep private/incognito browsing isolated from persistent extension storage unless the user explicitly enables a narrowly scoped permission.
- Treat external download-manager intents and file chooser results as untrusted input; validate URI schemes and granted URI permissions.
- Do not introduce a new network service, analytics SDK, font package or always-running service as part of the core compatibility layer.

## 6. Required regression checks before APK release

These checks are acceptance criteria for the later rebuild/device-test stage; they are **not marked as executed** by this static audit.

- [ ] Cold launch and first page load on API 19-compatible environment (where practical) and a current Android release.
- [ ] HTTP/HTTPS navigation, redirects, back/forward and tab lifecycle.
- [ ] JavaScript-heavy page, DOM storage and file chooser.
- [ ] Cookie set/read/restart persistence and sign-in session continuity.
- [ ] Download start, completion, notification and external-manager handoff.
- [ ] Bookmark create/edit/import/export and history add/clear.
- [ ] Incognito session isolation and cleanup.
- [ ] Reader mode and read-aloud entry points.
- [ ] Proxy and service-worker graceful fallback when provider features are unavailable.
- [ ] Custom-tab launch/return behavior.
- [ ] Existing userscript install, execution, menu command and GM storage behavior.
- [ ] Side-by-side install with original Via; verify OmegA does not overwrite it.

## 7. Reproducible CI preservation audit

Added `.github/workflows/omega-step4-core-audit.yml`. It can be run manually from GitHub Actions and also runs when the base APK or this workflow changes. The workflow:
- Verifies the checked-in APK SHA-256 and ZIP integrity.
- Decodes the APK with Apktool.
- Checks static DEX/asset markers for WebView, cookies, downloads, history/bookmarks, incognito, reader mode and userscript APIs.
- Confirms the base package and minSdk 19 remain unchanged.
- Uploads decoded manifest and audit evidence as a short-lived artifact.

This is a static regression guard, not an emulator test. Marker checks establish presence only, not successful feature execution. The workflow has been committed but has not yet been dispatched or observed passing.

## 8. Scope and completion statement

**Completed in Step 04:** static core-feature inventory, Android/WebView compatibility boundaries, feature-preservation contract, extension/userscript separation, security guardrails, concrete runtime regression acceptance checklist, and a committed reproducible static CI preservation audit.

**Not claimed:** source-level code changes, a rebuilt APK, live device compatibility, or passing regression tests. The repository currently contains the original compiled APK and audit/resource files, not the original Via source tree. The available environment also lacks the Android APK reverse-engineering/build tools needed to safely patch and rebuild this binary. Those implementation and runtime-test claims require the source/build toolchain and an Android test environment.

Step 04 is complete as a static compatibility specification and CI guard setup. The workflow result is pending. Actual browser-core code changes and runtime compatibility remain blocked until the original Android source project or a reliable decompilation/build toolchain and Android test runtime are available. The next planned stage is Step 05: extension manager architecture and integration.
