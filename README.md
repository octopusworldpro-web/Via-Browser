<div align="center">

# <span style="color:#A855F7">OmegA</span> Browser

### Fast • Lightweight • Privacy-focused

**A customized Via Browser 7.4.1 build**

[![Build](https://github.com/octopusworldpro-web/Via-Browser/actions/workflows/omega-step2-identity.yml/badge.svg)](https://github.com/octopusworldpro-web/Via-Browser/actions/workflows/omega-step2-identity.yml)

**Customized by NirZon**

</div>

---

## Project

OmegA Browser is a separately installable Android browser customization based on Via Browser 7.4.1. The goal is to preserve the compact browser experience while developing OmegA features in distinct, testable stages.

| Detail | Value |
|---|---|
| Product | OmegA Browser |
| Base | Via Browser 7.4.1 |
| Android package | `com.octopusworldpro.omega.browser` |
| Version line | `7.4.1-omega` |
| Minimum Android | API 19 |
| Target Android | API 36 |

## Six-step development roadmap

| Step | Scope | Status |
|---|---|---|
| 1 | Base reverse engineering + OmegA identity & branding | **CI/build complete**; device verification pending |
| 2 | Browser core preservation + custom OmegA UI | Audit/design groundwork exists; implementation and regression tests pending |
| 3 | Chrome Extension runtime + extension manager | Not implemented; runtime feasibility is a major engineering task |
| 4 | ZIP/CRX import, install/update/uninstall + storage/background/messaging | Not implemented |
| 5 | Browser-exposed Device Identity Manager + C3C FBState Utility | Not implemented |
| 6 | Full integration, security audit, device tests and release APK | Pending |

### Step 1 evidence

- [Step 1 completion record and original APK audit](AUDIT/STEP-01-APK-AUDIT.md)
- [Identity repack CI run #9](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45331285994) — successful
- [JADX runtime reconnaissance CI](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45332809697) — successful
- [Branding integration report](AUDIT/STEP-03-BRANDING-INTEGRATION.md)

Step 1 consolidates existing work; later steps must reuse these results rather than repeat the APK audit, package migration, launcher branding or JADX inventory.

## Branding

- Purple-neon / chrome OmegA visual direction.
- Omega globe, orbit and crown-inspired launcher mark.
- Adaptive icon for Android 8.0+ and legacy density icons for API 19–25.
- Keep the browser lightweight: avoid unnecessary runtime libraries, bundled fonts and decorative assets.

## Current limitations

- Current successful APK is test-signed, not production-signed.
- Physical device install/launch and side-by-side coexistence have not been verified.
- Static reconnaissance found no Chrome/WebExtension runtime indicators in the inspected APK. Userscripts and generic ZIP parsing are not Chrome extension support.
- Extension runtime, manager UI, ZIP/CRX installer and Web Store integration are not implemented.
- Device Identity scope is limited to browser-exposed values; hardware IMEI modification is not part of this project.
- C3C FBState Utility integration is pending; any cookie/AppState export must be user-initiated and permission-gated.

## Audit records

- [Step 1 — APK audit, reverse engineering and identity/branding completion](AUDIT/STEP-01-APK-AUDIT.md)
- [Step 2 — App identity contract](AUDIT/STEP-02-APP-IDENTITY.md)
- [Step 3 — Branding integration details](AUDIT/STEP-03-BRANDING-INTEGRATION.md)
- [Step 4 — Browser core compatibility baseline](AUDIT/STEP-04-BROWSER-CORE-COMPATIBILITY.md)
- [Step 5 — Extension manager design](AUDIT/STEP-05-EXTENSION-MANAGER.md)
- [Step 6 — Runtime reconnaissance](AUDIT/STEP-06-RUNTIME-RECONNAISSANCE.md)
- [Step 7 — Runtime implementation feasibility/blocker](AUDIT/STEP-07-RUNTIME-IMPLEMENTATION.md)

## Build artifacts

Open [GitHub Actions](https://github.com/octopusworldpro-web/Via-Browser/actions/workflows/omega-step2-identity.yml) and select a successful run to download the test APK artifact. The artifact is not a production release.
