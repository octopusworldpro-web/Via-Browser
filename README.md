<div align="center">

# <span style="color:#A855F7">OmegA</span> Browser

### Fast • Lightweight • Privacy-focused

**A customized Via Browser 7.4.1 build**

[![Branding Build](https://github.com/octopusworldpro-web/Via-Browser/actions/workflows/omega-step2-identity.yml/badge.svg)](https://github.com/octopusworldpro-web/Via-Browser/actions/workflows/omega-step2-identity.yml)

**Customized by NirZon**

</div>

---

## Project

OmegA Browser is a separately branded Android APK based on Via Browser 7.4.1. The project aims to preserve the original browser's compact, lightweight experience while developing OmegA identity and carefully scoped additions.

| Detail | Value |
|---|---|
| Product | OmegA Browser |
| Base | Via Browser 7.4.1 |
| Android package | `com.octopusworldpro.omega.browser` |
| Version line | `7.4.1-omega` |
| Minimum Android | API 19 |
| Target Android | API 36 |

## OmegA identity

- Purple-neon and chrome visual direction.
- Omega globe, orbit and crown-inspired launcher mark.
- Adaptive launcher icon for Android 8.0+.
- Density-specific legacy launcher icons for Android 4.4–7.1.
- Compact browser-first design; avoid unnecessary runtime dependencies.

## Build status

| Area | Status |
|---|---|
| APK identity migration | CI build passed |
| Adaptive launcher resources | CI build passed |
| Legacy launcher resources (API 19–25) | CI build passed |
| Device install / launch validation | Pending |
| In-app About attribution | Pending |
| Full in-app theme / splash integration | Pending |
| Chrome extension runtime | Blocked — no compatible engine in inspected APK |\n| Extension manager UI / ZIP-CRX importer | Design only; not integrated |\n| Device install / launch test | Not run |

> CI currently produces a test-signed APK. It is not a production-signed release. A successful build does not replace physical-device installation and behavior testing.

## Build artifact

Open [GitHub Actions](https://github.com/octopusworldpro-web/Via-Browser/actions/workflows/omega-step2-identity.yml) and select the latest successful run to retrieve the temporary test APK artifact.

## Audit records

- [Step 01 — Base APK audit](AUDIT/STEP-01-APK-AUDIT.md)
- [Step 02 — Application identity](AUDIT/STEP-02-APP-IDENTITY.md)
- [Step 03 — Branding integration](AUDIT/STEP-03-BRANDING-INTEGRATION.md)
- [Step 04 — Browser core compatibility](AUDIT/STEP-04-BROWSER-CORE-COMPATIBILITY.md)
- [Step 05 — Extension manager design](AUDIT/STEP-05-EXTENSION-MANAGER.md)\n- [Step 06 — Runtime reconnaissance](AUDIT/STEP-06-RUNTIME-RECONNAISSANCE.md)\n- [Step 07 — Runtime implementation status](AUDIT/STEP-07-RUNTIME-IMPLEMENTATION.md)

## Important

OmegA Browser is an independent customization project. The current APK build is based on the supplied Via Browser binary; deeper browser-engine features require implementation and runtime testing, not documentation alone.
