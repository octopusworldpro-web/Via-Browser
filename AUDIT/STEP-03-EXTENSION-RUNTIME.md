# Roadmap Step 3 — Chrome Extension Runtime + Extension Manager

**Status: BLOCKED — engine feasibility evaluated; no runtime is integrated into the OmegA APK.**

## Reused work (do not duplicate)

- AUDIT/STEP-05-EXTENSION-MANAGER.md defines manager screens, lifecycle, metadata, permission policy, runtime adapter contract, security constraints and acceptance tests.
- AUDIT/STEP-06-RUNTIME-RECONNAISSANCE.md records the decompilation inventory.
- AUDIT/STEP-07-RUNTIME-IMPLEMENTATION.md records missing runtime evidence and the source-project blocker.
- AUDIT/STEP-08-ENGINE-DECISION.md records the engine feasibility decision and verified GeckoView compatibility boundary.
- config/extension-manager.json is the feature gate.

## Verified current boundary

The inspected Via APK has a userscript subsystem, but static reconnaissance found no Chrome/WebExtension runtime indicators. Userscripts are not Chrome extensions. The repository contains a compiled APK and resource-repackaging workflows, not the original Android Studio/Gradle project.

## Feasibility outcome

GeckoView is a documented embedded WebExtension engine candidate, but it is not a drop-in Chrome MV3 runtime. Firefox's background implementation does not support Chrome's background.service_worker key and instead uses background scripts/event pages. Integrating GeckoView would replace the existing Via WebView engine and require browser-core migration and regression work. This is not approved as a silent change because it risks Via feature preservation and the lightweight requirement.

A custom extension runtime on the existing WebView would require implementing isolated script contexts, background lifecycle, browser APIs, permission brokerage, storage and navigation integration. It is not a safe resource-only/DEX patch.

## Current safety gate

- Extension manager remains disabled.
- ZIP/CRX/Web Store are planned sources, not supported import features.
- No package is labelled installed/runnable merely because metadata is parsed.
- No extension code executes during import or permission review.
- No broad cookie, password, history or private-tab access is exposed.
- Existing Via userscripts remain separate and untouched.

## What is complete in Step 3

- Existing manager architecture reused; no duplicate manager design created.
- APK reconnaissance evidence checked and referenced.
- Engine feasibility evaluated and decision documented.
- GeckoView's real WebExtension support and MV3 background incompatibility recorded.
- Safety gate CI is in place and remains closed.

## What remains before Step 3 can be marked complete

1. Obtain a buildable, licensed Via Android source/Gradle project, or approve a separate browser shell.
2. Decide whether replacing Via WebView with GeckoView is acceptable.
3. Build and measure a GeckoView proof-of-concept if engine replacement is approved.
4. Implement a tested WebExtension compatibility subset and report unsupported APIs.
5. Integrate isolated content scripts, background lifecycle, messaging, storage and permission broker.
6. Wire the manager UI to the actual runtime.
7. Pass automated tests and Android emulator/physical-device regression tests.

**Honest status:** Step 3 feasibility work is complete, but the requested runtime feature is not. The extension runtime must remain disabled until the missing source/engine decision is resolved.
