# Roadmap Step 3 — Lightweight Extension Runtime + Extension Manager

**Status: IN PROGRESS — lightweight content-script execution is integrated; full Chrome compatibility is explicitly out of scope.**

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

1. CI compile and utility tests for the current implementation.
2. Add focused tests for match-pattern behavior and manifest/script path validation.
3. Test install, enable, disable, matching-site injection and removal on an Android device.
4. Measure final main APK and installed extension storage size.

**Honest status:** a minimal page-context content-script feature is now wired into the OmegA WebView app. Step 3 is not fully complete until CI and device tests pass.