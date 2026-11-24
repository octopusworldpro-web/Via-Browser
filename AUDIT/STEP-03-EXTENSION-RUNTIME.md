# Roadmap Step 3 — Chrome Extension Runtime + Extension Manager

**Status:** Manager architecture exists; runtime implementation blocked pending a buildable Android source project and a selected compatible engine.

## Reused work (do not duplicate)

- `AUDIT/STEP-05-EXTENSION-MANAGER.md` already defines manager screens, responsibilities, lifecycle, metadata, permission policy, runtime adapter contract, security constraints and acceptance tests.
- `AUDIT/STEP-06-RUNTIME-RECONNAISSANCE.md` records the decompilation inventory.
- `AUDIT/STEP-07-RUNTIME-IMPLEMENTATION.md` records the missing runtime evidence and implementation blocker.
- `config/extension-manager.json` is the feature gate. It now explicitly distinguishes planned import sources from implemented support.

## Current verified boundary

The inspected Via APK has a userscript subsystem, but the static inventory found no Chrome/WebExtension runtime indicators. Userscripts are not Chrome extensions. The manager must remain disabled until an actual runtime adapter is integrated and tested.

The repository contains a compiled APK and resource-repackaging workflows, not the original Android Studio/Gradle project. Resource repackaging cannot safely add the required runtime, UI event wiring, isolated script contexts or persistent lifecycle.

## Runtime work required

1. Select and document an engine strategy compatible with the product's lightweight constraint and required Chrome API coverage.
2. Implement Manifest V3 parsing and safe package validation.
3. Implement isolated content-script contexts and supported navigation injection phases.
4. Implement background worker/event lifecycle and extension messaging.
5. Implement a scoped `chrome.*` API adapter with explicit unsupported-API reporting.
6. Implement per-extension storage, permission brokerage and atomic lifecycle operations.
7. Wire the existing manager contract to the runtime adapter.
8. Preserve Via userscripts, browser cookies, history, bookmarks, downloads and incognito isolation.
9. Add JVM/unit/instrumentation tests and Android emulator/device regression tests.

## Feature safety gate

Until those requirements pass:
- Manager remains disabled.
- ZIP/CRX/Web Store are planned sources, not supported import features.
- No package may be labelled installed/runnable merely because metadata was parsed.
- No extension code executes during import or permission review.
- No broad cookie, password, history or private-tab access is exposed.
- Existing Via userscripts remain separate and untouched.

## Completion criteria

Step 3 is complete only when a real runtime is integrated into a buildable Android project, the manager is wired to it, supported APIs are tested, unsupported APIs are disclosed, and a test APK passes regression tests. A design document or CI feature gate alone does not satisfy this acceptance gate.
