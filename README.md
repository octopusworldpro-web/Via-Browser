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
| Android package | com.octopusworldpro.omega.browser |
| Version line | 7.4.1-omega |
| Minimum Android | API 19 |
| Target Android | API 36 |

## Six-step development roadmap

| Step | Scope | Status |
|---|---|---|
| 1 | Base reverse engineering + OmegA identity & branding | **CI/build complete**; device verification pending |
| 2 | Browser core preservation + custom OmegA UI | Static preservation contract + CI PASS; in-app UI/runtime tests blocked by missing Android source project |
| 3 | Lightweight local extension path (ZIP/CRX only) | Web Store excluded; runtime blocked by missing Android source project |
| 4 | Local package validation, install/update/uninstall + storage/runtime infrastructure | Not implemented |
| 5 | Browser-exposed Device Identity Manager + C3C FBState Utility | FBState specification defined; app integration pending |
| 6 | Full integration, security audit, device tests and release APK | Pending |

### Step 1 evidence

- [Step 1 completion record and original APK audit](AUDIT/STEP-01-APK-AUDIT.md)
- [Identity repack CI run #9](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45331285994) — successful
- [JADX runtime reconnaissance CI](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45332809697) — successful
- [Branding integration report](AUDIT/STEP-03-BRANDING-INTEGRATION.md)

Step 1 consolidates existing work; later steps must reuse these results rather than repeat the APK audit, package migration, launcher branding or JADX inventory.

## Lightweight design rules

- Keep the existing Via WebView engine; do not bundle a second browser engine.
- Avoid heavyweight libraries, bundled fonts, analytics, remote code and unnecessary UI frameworks.
- Keep optional extension packages outside the base APK.
- Do not provide Chrome Web Store integration; local ZIP/CRX is the only planned extension import path.
- FBState operations must be manual, consent-gated and local-only; never collect WebView cookies automatically or upload session data.
- Measure per-ABI APK size, install size, startup and memory before accepting dependencies.

## Current limitations

- Current successful APK is test-signed, not production-signed.
- Physical device install/launch and side-by-side coexistence have not been verified.
- Static reconnaissance found no Chrome/WebExtension runtime indicators in the inspected APK. Userscripts and generic ZIP parsing are not Chrome extension support.
- A restricted WebView content-script subset is only an implementation target; it is not yet implemented and is not full Chrome extension compatibility.
- Device Identity scope is limited to browser-exposed values; hardware IMEI modification is not part of this project.
- C3C FBState Utility is specified but not integrated. Import/export must be explicitly initiated by the user; session tokens are sensitive and must remain local.

## Audit records

- [Step 1 — APK audit, reverse engineering and identity/branding completion](AUDIT/STEP-01-APK-AUDIT.md)
- [Step 2 — App identity contract](AUDIT/STEP-02-APP-IDENTITY.md)
- [Step 2 — Browser core compatibility baseline](AUDIT/STEP-04-BROWSER-CORE-COMPATIBILITY.md)
- [Step 3 — Extension runtime implementation gate](AUDIT/STEP-03-EXTENSION-RUNTIME.md)
- [Step 3 — Engine feasibility decision](AUDIT/STEP-08-ENGINE-DECISION.md)
- [Step 3 — Lightweight engine decision](AUDIT/STEP-09-LIGHTWEIGHT-ENGINE-DECISION.md)
- [Step 3 — Extension manager design](AUDIT/STEP-05-EXTENSION-MANAGER.md)
- [Step 3 — Runtime reconnaissance](AUDIT/STEP-06-RUNTIME-RECONNAISSANCE.md)
- [Step 3 — Runtime implementation feasibility/blocker](AUDIT/STEP-07-RUNTIME-IMPLEMENTATION.md)
- [Step 5 — C3C FBState Utility specification](AUDIT/STEP-10-C3C-FBSTATE-UTILITY.md)

## Build artifacts

Open [GitHub Actions](https://github.com/octopusworldpro-web/Via-Browser/actions/workflows/omega-step2-identity.yml) and select a successful run to download the test APK artifact. The artifact is not a production release.
