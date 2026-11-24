# Step 07 — Lightweight Runtime Implementation

**Status: IN PROGRESS — a restricted content-script adapter is integrated; device validation remains pending.**

## Implemented in OmegA

- Existing Android WebView remains the only browser engine.
- ZIP packages are extracted to app-private storage with traversal and archive limits.
- Extensions are installed disabled and require explicit user enablement.
- Manifest V2/V3 `content_scripts` entries can run matching top-frame JavaScript files after page load.
- URL match patterns are limited to HTTP/HTTPS scheme, host and path wildcards.
- Individual script files are limited to 1 MiB.
- No JavaScript-to-native bridge or privileged Chrome API is exposed.

## Deliberate limitations

- Scripts run in the page JavaScript world, not an isolated execution world.
- No `chrome.*` APIs, extension messaging, background pages, MV3 service workers, all-frame injection or Web Store installation.
- `run_at` timing is not implemented; injection is a document-idle approximation.
- This is not full Chrome extension compatibility.

Users should enable only extensions from sources they trust. The content script can read and modify the page DOM because it executes in the page context, but it receives no privileged native bridge.

## Build and validation

The normal Android workflow now builds only the main WebView APK. GeckoView has been removed from the Gradle project graph and the standard build/artifact workflow to keep the app lightweight.

Pending:
1. CI build and test confirmation for the new adapter.
2. Automated URL matching and path-safety tests.
3. Android device validation for install/enable/match/disable/remove.
4. Final APK-size comparison.

Do not label this a full Chrome/WebExtension runtime.