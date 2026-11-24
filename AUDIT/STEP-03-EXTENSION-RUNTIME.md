# Roadmap Step 3 — Lightweight Extension Runtime + Extension Manager

**Status: IMPLEMENTATION COMPLETE — limited WebView content-script runtime and extension manager controls are implemented; final device regression belongs to Step 6. Full Chrome compatibility is explicitly out of scope.**

## Engine decision

OmegA keeps its existing Android WebView. GeckoView is not included in the application Gradle graph or the normal Android build workflow. No second browser engine or Gecko native libraries are shipped in the main APK.

## Implemented

- Existing ZIP import and app-private extension storage retained.
- ZIP traversal checks and extraction limits retained.
- Added a lightweight WebView content-script adapter.
- Manifest V2/V3 `content_scripts` entries are inspected.
- Supports HTTP/HTTPS URL match patterns, top-frame scripts and JavaScript files up to 1 MiB each.
- Newly installed extensions start disabled; the user must explicitly enable them in Extensions.
- Enable/disable and remove controls are wired to the extension manager.
- Per-extension site access is enforced before injection: blocked by default, one-time current-site access, selected-origin allow-list, or all matching HTTP/HTTPS sites.
- Site access changes reload the active page; uninstall clears the extension's enable and site-grant preferences.
- ZIP import rejects duplicate entries and oversized manifests, and preserves an existing install if replacement finalization fails.
- No native JavaScript bridge is exposed to extension scripts.
- Main APK CI builds only `:app:assembleDebug`; GeckoView POC is no longer built or uploaded.

## Runtime limitations (intentional)

This is a small content-script runner, not a full Chrome/WebExtension runtime:
- Scripts execute in the webpage JavaScript context, **not an isolated world**.
- No `chrome.*` APIs, extension messaging, background pages, MV3 service workers, all-frame injection or Chrome Web Store installation.
- Injection occurs after page load (document-idle approximation); `run_at` timing is not implemented.
- Match-pattern support is intentionally limited to HTTP/HTTPS scheme, host and path wildcards.
- Existing Via userscripts remain separate.

Because scripts share the page context, only install extensions from sources the user trusts. No privileged native bridge is available to those scripts.

## Remaining verification

1. Run Android compile and JVM unit tests for URL matching and script path policy.
2. Validate malformed/unsupported manifest combinations and archive edge cases.
3. Test install, enable, disable, matching-site injection and removal on an Android device.
4. Measure final main APK and installed extension storage size.

**Honest status:** URL matching and script-path checks now use a pure JVM-testable policy. Step 3 remains open until CI and Android device tests pass.