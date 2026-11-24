# Step 07 — Runtime Implementation and APK Integration

**Status: BLOCKED — no buildable Android source project or compatible WebExtension engine is available in this repository.**  
**Decision date:** 2026-11-24

## Step 06 evidence used

The successful [runtime reconnaissance workflow](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45332809697) decompiled 4,285 Java source files. Its report found:

- 9 files matching WebView construction/configuration.
- 12 files matching JavaScript bridge patterns.
- 10 files matching the existing userscript subsystem.
- 6 files matching generic extension package/import patterns.
- **0 files matching Chrome/WebExtension runtime indicators.**

This is static evidence, not proof of runtime behavior. Generic ZIP/manifest handling and WebView service-worker APIs do not provide Chrome extension execution.

## Implementation decision

A real extension manager is not useful unless a compatible runtime can execute the extension. Implementing only a manager UI or ZIP/CRX importer would create a misleading feature: packages could appear installed but could not run. Therefore this step does not label imported packages as installed/runnable and does not claim Chrome Web Store compatibility.

The repository currently contains a compiled Via APK and audit/build workflows, not the original Android source/Gradle project. The existing APK build workflow can repackage resources and identity, but it does not provide a safe, maintainable path to add a complete extension engine and application UI. Blind DEX patching is rejected because the exact app-specific call sites and runtime behavior have not been verified.

## Required implementation components

A future implementation must provide and test all of the following before Chrome extension support is enabled:

1. Manifest V3 parsing and package validation with traversal, archive-size and decompression-bomb protections.
2. Isolated content-script execution and deterministic injection at supported navigation phases.
3. Background service-worker/event lifecycle and extension messaging.
4. Explicitly scoped `chrome.*` API compatibility, with unsupported APIs reported accurately.
5. Permission broker for API and host permissions; no ambient access to cookies, passwords, history or private tabs.
6. Per-extension persistent storage and atomic install/update/rollback/uninstall.
7. Local ZIP/CRX import and a truthful Web Store handoff policy.
8. Manager UI wired to the actual runtime adapter.
9. Regression tests for existing Via browsing, userscripts, downloads, cookies, bookmarks and history.
10. Install/launch/functional tests on an Android emulator and physical device.

## Current build capability

| Capability | Verified state |
|---|---|
| Separate OmegA application identity | CI build passed |
| OmegA launcher branding | CI build passed |
| Existing Via browser code retained in repackaged APK | Static build/repack only |
| Runtime decompilation inventory | CI passed |
| Extension manager design | Documented only |
| Chrome extension runtime | Not implemented |
| ZIP/CRX importer integrated into APK | Not implemented |
| Chrome Web Store install flow | Not implemented |
| Device install/launch regression test | Not run |
| Production signing | Not configured; current artifact uses temporary test key |

## Completion gate

Step 07 can be marked complete only when a buildable implementation exists, CI produces the updated APK, and device/emulator tests prove the manager and supported extension APIs work. Until then, the honest project status is **blocked**, not complete.

## What unblocks this step

The viable next input is a buildable Via Android source/Gradle project (or an independently licensed WebExtension engine plus a maintainable integration project). Without that, this repository can continue static APK/resource repackaging, but cannot responsibly deliver a real Chrome-compatible extension runtime.
