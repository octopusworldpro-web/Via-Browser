# Roadmap Step 3 — Chrome Extension Runtime + Extension Manager

**Status: BLOCKED — lightweight engine decision made; runtime is not integrated.**

## Reused work

- AUDIT/STEP-05-EXTENSION-MANAGER.md defines the manager architecture and security contract.
- AUDIT/STEP-06-RUNTIME-RECONNAISSANCE.md records the decompilation inventory.
- AUDIT/STEP-07-RUNTIME-IMPLEMENTATION.md records the missing runtime evidence and source-project blocker.
- AUDIT/STEP-08-ENGINE-DECISION.md records the earlier GeckoView feasibility review.
- AUDIT/STEP-09-LIGHTWEIGHT-ENGINE-DECISION.md records the current decision: retain WebView and reject GeckoView bundling for the lightweight target.
- config/extension-manager.json remains the feature gate.

## Verified current boundary

The inspected Via APK has a userscript subsystem, but static reconnaissance found no Chrome/WebExtension runtime indicators. Userscripts are not Chrome extensions. The repository contains a compiled APK and resource-repackaging workflows, not the original Android Studio/Gradle project.

## Engine decision

Keep the existing Via WebView engine. Do not bundle GeckoView: the reviewed ARM64 package measurement is approximately 90 MB, its minimum Android requirement is API 26, and Firefox Android does not support Chrome MV3 background service workers. This would increase size, drop support for API 19–25 and still not deliver full Chrome compatibility.

WebView shares Chromium rendering code but is not the Chrome browser extension host. A real extension-compatible layer requires app-source integration; it cannot be safely added by resource repackaging or blind DEX patching.

## Lightweight implementation target (not yet implemented)

Investigate a deliberately limited WebExtension-compatible subset on the existing WebView:
- Local-only staged package validation.
- Content scripts with isolated execution contexts and explicit origin match patterns.
- Small allowlisted APIs, with unsupported Chrome APIs clearly rejected.
- Per-extension storage quotas and atomic lifecycle.
- Explicit least-privilege host permission prompts.
- No general background service worker, arbitrary native bridge, broad cookie/password/history access or Web Store compatibility claim.
- Existing Via userscripts remain separate and unchanged.

This is not full Chrome extension support. If safe isolated execution cannot be achieved using the actual source and supported platform APIs, do not ship extension execution.

## APK size policy

- No second browser engine or duplicate native browser libraries.
- No bundled fonts, analytics, remote code or heavyweight UI framework.
- Keep user extension packages outside the base APK and enforce archive quotas.
- Measure per-ABI APK size, install size, startup and memory before/after dependencies.
- Reject heavyweight dependencies without measured feature benefit.

## What is complete

- Existing manager architecture reused.
- APK reconnaissance evidence checked.
- GeckoView cost and compatibility tradeoffs reviewed.
- Lightweight engine decision recorded: retain WebView; do not bundle GeckoView.
- Runtime safety gate remains closed.

## What remains

1. Obtain the buildable Via Android Studio/Gradle source project.
2. Prototype a minimal isolated content-script runtime on a separate branch.
3. Implement the restricted API and permission broker only if isolation is sound.
4. Add storage quotas, atomic install/update/remove and manager UI.
5. Run security, browser-regression, APK-size and Android device tests.

**Honest status:** feasibility decision complete; runtime and manager UI are not complete. Do not mark Step 3 done until a real test APK passes the acceptance gates.
