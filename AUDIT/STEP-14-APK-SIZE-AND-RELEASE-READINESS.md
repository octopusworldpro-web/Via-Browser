# Step 14 — APK Size and Release Readiness Audit

**Audit date:** 2026-11-24  
**Status:** Existing branded test APK measured and archive-checked; final app integration and release validation remain pending.

## 1. Artifact inspected

- Workflow: [OmegA Browser identity/branding build](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45331285994)
- Artifact: `OmegA-Browser-step4-ui-branding-test-build`
- Artifact ID: `14891148875`
- Workflow artifact ZIP: **3,301,832 bytes** (3.15 MiB)
- Extracted APK: **4,700,000 bytes** (4.48 MiB; 4.70 MB decimal)
- APK SHA-256: `58dc99c0e2b138992dd97d38a0aad6aaf2e1179e071ac5618edc94ca6bdfc034`

The artifact was downloaded from the recorded GitHub Actions artifact and inspected directly.

## 2. APK structural checks

| Check | Observed result |
|---|---|
| ZIP/APK archive integrity | PASS — 366 entries; no corrupt entry |
| Package ID | `com.octopusworldpro.omega.browser` |
| App label | `OmegA Browser` |
| Version name | `7.4.1-omega` |
| Version code | `2026112401` |
| minSdk | 19 |
| targetSdk | 36 |
| DEX files | 1 (`classes.dex`) |
| Native libraries | None found under `lib/` |
| APK size | 4,700,000 bytes |

The originating build workflow also ran `apksigner verify --verbose` and `aapt dump badging` before uploading the artifact.

## 3. Comparison with base APK

The base Via Browser 7.4.1 APK audit recorded **4,356,517 bytes**. The OmegA branded test APK is **343,483 bytes larger**, approximately **7.88%** above that base artifact.

This difference reflects the current repackaging/branding build; it is not a measured size for the future FBState or Device Identity Android screens.

## 4. Lightweight size budget

- Current measured APK: **4.70 MB**
- Preferred target: **around or below 5.00 MB**
- Review threshold: **5.20 MB**
- Any increase beyond the review threshold should be justified by measured functionality and tested on-device.

These are project targets, not guarantees. The final APK must be measured again after native UI and adapter integration.

## 5. Release-readiness disposition

**Verified now**
- A real OmegA branded test APK exists in CI artifact storage.
- Its exact size, SHA-256, archive integrity, package identity and SDK levels were inspected.
- The APK currently contains one DEX and no packaged native libraries.

**Still not verified / not included in this APK**
- FBState Android screen, document picker and consent UI.
- Device Identity Android screen and live WebView adapter.
- Functional regression testing of browser features on emulator/physical device.
- Production signing key and production-signed APK.
- Final release artifact size after feature integration.

## 6. Build-input limitation

The current `main` tree contains the repackaging workflow and project utilities but does not expose the base APK at the path expected by that workflow (`Via+Browser+-+Fast+&+Light_7.4.1_APKPure.apk`). The measured artifact is therefore the previously successful CI output, not a fresh build from the current `main` tree.

The repository also does not contain the original buildable Via Android Studio/Gradle source project. Adding native Activities, Android document-picker wiring or WebView adapters cannot be truthfully completed by editing the standalone JavaScript controller layer or by binary-patching the APK without a verified integration project.

## 7. Final conclusion

**APK size audit: complete.** The currently available OmegA branded test APK measures **4.70 MB** and passes the archive integrity check.

**Full Step 6 / release: not complete.** Native feature integration, device regression tests and production signing remain release gates. No production-ready APK is claimed by this report.
