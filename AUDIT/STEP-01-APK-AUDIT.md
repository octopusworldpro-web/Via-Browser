# Step 01 — Via Browser APK Structural Audit

**Status:** Complete (static APK/manifest/DEX inventory)  
**Input:** `Via+Browser+-+Fast+&+Light_7.4.1_APKPure.apk`  
**Audit date:** 2026-11-24

## 1. Artifact integrity

- APK is a readable ZIP/APK archive; archive integrity check passed.
- File size: 4,356,517 bytes (about 4.16 MiB; GitHub UI rounds this to 4.36 MB).
- SHA-256: `5764435e66cdfa71bd18e01df6c794b7e1ba64a13ba147a4a216d9f245e8acc8`
- Contains `AndroidManifest.xml`, `classes.dex`, `resources.arsc`, app assets and a v1 JAR signature block.
- One DEX file; no native `lib/*.so` libraries found.

## 2. Android application metadata

| Field | Observed value |
|---|---|
| Application ID | `mark.via.gp` |
| Version name | `7.4.1` |
| Version code | `20261113` |
| minSdkVersion | 19 (Android 4.4) |
| targetSdkVersion | 36 |
| Compile SDK | 36 |
| Application class | `mark.via.BrowserApp` |
| Main browser activity | `mark.via.Shell` |
| Other app activities | `mark.via.Search`, `mark.via.CustomTab`, `mark.via.Trampoline` |

## 3. Signing certificate

- APK contains a v1 JAR signature (`META-INF/VARIOUSF.RSA`).
- Certificate subject/issuer: `CN=Various Tu, L=广西, ST=广西, C=CN` (self-signed).
- Certificate validity: 2014-08-27 through 2114-08-03.
- Certificate SHA-256 fingerprint: `3df7f89d3b8d1315f05710c914fccbcf3a4e24980afddccb8dcebde90836a390`.
- A rebuilt APK signed with a new OmegA key will be a separate signing identity; it will not be accepted as an in-place update over the original signed Via installation.

## 4. Manifest permissions and notable configuration

Observed permissions include:

- Network: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`
- Location: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`
- Storage/install: `WRITE_EXTERNAL_STORAGE` (max SDK 29), `REQUEST_INSTALL_PACKAGES`
- Device/app visibility: `QUERY_ALL_PACKAGES`
- Media and interaction: `CAMERA`, `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `VIBRATE`
- Background/notifications: `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`
- Biometrics: `USE_BIOMETRIC`, `USE_FINGERPRINT`
- Other: `CREDENTIAL_MANAGER_SET_ORIGIN`, `AD_ID`, launcher shortcut permissions and app-specific broadcast permission.

Notable application flags:
- `allowBackup=true`
- `usesCleartextTraffic=true`
- `hardwareAccelerated=true`
- `supportsRtl=true`
- Resizable activity support is enabled.

These are inventory observations, not a claim that every permission is actively used or that a vulnerability has been confirmed. They require a focused security review before release.

## 5. Components and app architecture clues

Manifest declares the browser shell, search and custom-tab activities; download and read-aloud services; download/read-aloud receivers; search widgets; AndroidX startup/profile providers; and Firebase/Google measurement components.

DEX inventory:
- One DEX with 26,728 string IDs, 9,471 type IDs and 8,262 class definitions.
- Via application classes are under `mark.via.*`; many browser implementation class names are short/obfuscated, so source-level behavior cannot be reliably inferred from class names alone.
- No native shared libraries were present in the APK.

## 6. Existing browser capabilities found in the package

Static DEX/assets inspection found evidence of:
- A WebView-based browser stack (Android WebView / AndroidX WebKit references).
- Download, bookmarks/settings-related code, reader mode, read-aloud, proxy support, cookies, custom tabs and biometric APIs.
- An ad-block/filter list at `assets/simple.txt`.
- Embedded reader-mode and clipboard JavaScript.
- A userscript subsystem: userscript metadata markers (`// ==/UserScript ==`), sample script template, `GM_getValue`, `GM_setValue`, `GM_registerMenuCommand`, `GM_xmlhttpRequest`, `GM_download`, `GM_addStyle`, `GM_openInTab`, and a Tampermonkey compatibility shim are present.

### Extension-support distinction

The APK has meaningful **userscript-manager** evidence, but this audit did not find indicators of a full Chromium extension runtime such as `chrome.runtime` or `chrome-extension://`. Therefore, local userscript installation and full Chrome/CRX extension installation must be treated as separate features. The existing userscript engine may be reusable; full extension support is not established.

## 7. Rebuild and modification risks

- The APK contains compiled DEX and compiled Android resources, not an Android Studio source project.
- The Via application namespace contains many obfuscated classes. A source-level decompilation and control-flow review is still needed before safely changing core browser behavior.
- Repackaging requires resource/manifest/DEX tooling, a new signing key, and device-level install/launch tests. Merely changing the package string in the binary is not sufficient.
- Package ID changes require reviewing provider authorities, app-specific permissions, deep links, shortcuts/widgets and any hard-coded package references.
- The existing APK signing key is not available to this project; OmegA builds must use a new key and be installed as a separate app.

## 8. Step 01 conclusion

**Completed:** artifact integrity, binary layout, Android manifest metadata, permissions/components, signing certificate fingerprint, DEX inventory and feature indicators have been recorded.

**Not claimed:** full Java/Kotlin source reconstruction, dynamic runtime testing, complete permission-to-code mapping, or confirmation that all observed features work on a physical device. Those require decompiler/toolchain and device testing during later implementation steps.

**Implementation implications for OmegA Browser:**
1. Preserve Via's browser core where legally and technically feasible.
2. Reuse/extend the existing userscript subsystem rather than assuming it is absent.
3. Design full extension/CRX support as a distinct compatibility layer, with explicit testing.
4. Keep Device Identity Manager limited to browser-exposed identity values; do not attempt to alter hardware IMEI or other protected device identifiers.
5. Use a new OmegA application ID and signing key, then validate install/launch independently.


## 9. Additional verification pass (2026-11-24)

### Archive and signature verification

- Re-ran the ZIP integrity test against the supplied APK: all 360 archive entries passed; no ZIP structural errors were reported.
- Recomputed SHA-256 and confirmed it matches the artifact fingerprint recorded above.
- Ran JDK jarsigner verification. The archive contains a v1 JAR signature, but the installed JDK rejects SHA-1 under its current disabled-algorithm policy and reports the APK as unsigned for trust purposes.
- The signature report also lists many APK entries as not covered by the JAR signature. The v1 signature check is therefore not a reliable integrity/authenticity guarantee for the whole APK. This is a release risk, not proof of malicious modification.
- The signer certificate is self-signed; no trusted publisher chain was established by this check.

### Repository artifact comparison

The directly supplied APK in the current working session matches the recorded artifact fingerprint: 5764435e66cdfa71bd18e01df6c794b7e1ba64a13ba147a4a216d9f245e8acc8. The archive contains 360 entries and passes the ZIP integrity test.

### Toolchain availability check

The current working environment has Java, keytool and jarsigner, but does not have jadx, apktool, aapt, aapt2, apksigner, zipalign, baksmali or an Android SDK build platform installed. Python packages for Androguard/APK parsing are also absent. No Android emulator/device bridge is exposed in this environment.

This pass could independently verify the archive and inspect packaged files, but could not produce a trustworthy Java/Kotlin decompilation, decode the binary Android manifest/resource table with Android tooling, rebuild the APK, or run it on a device. Attempting binary patching without those tools would risk corrupting the base browser.

## 10. Final Step 01 disposition

**Static artifact audit: complete.** The supplied APK's identity, archive integrity, signing metadata and signature weakness, manifest inventory, component inventory, DEX inventory, feature indicators, extension/userscript distinction and rebuild risks are recorded.

**Source reconstruction / dynamic analysis: blocked, not silently marked complete.** This is a separate technical subtask requiring a compatible decompiler/APK toolchain and Android runtime. It cannot be honestly completed in the current environment. The next implementation work must first provision those tools or obtain the original Via Android source project, then validate decompilation and runtime behavior before modifying the browser core.


---

# OmegA Browser — Step 1 Completion Record

**Step 1:** Base Reverse Engineering + OmegA Identity & Branding  
**Disposition:** CI/build implementation complete; device verification remains pending  
**Date:** 2026-11-24

This step consolidates work already performed in the repository. It does not repeat the earlier APK audit, identity migration, branding integration or JADX reconnaissance.

## Reused completed work

1. **Base APK audit** — `AUDIT/STEP-01-APK-AUDIT.md`
   - Recorded APK integrity, SHA-256, manifest, package/version, SDK levels, signing metadata, permissions, components, DEX inventory and existing browser/userscript capabilities.
2. **OmegA identity contract** — `AUDIT/STEP-02-APP-IDENTITY.md`
   - Package: `com.octopusworldpro.omega.browser`
   - Label: `OmegA Browser`
   - Version line: `7.4.1-omega`
   - Side-by-side install identity; original Via package remains unchanged.
3. **Branding resources and APK integration** — `AUDIT/STEP-03-BRANDING-INTEGRATION.md`
   - OmegA adaptive launcher icon and legacy density icons.
   - OmegA app label, purple/magenta accent resources and About attribution resource.
   - Existing Via layouts and browser code were not mass-renamed.
4. **Reverse-engineering reconnaissance** — `AUDIT/STEP-06-RUNTIME-RECONNAISSANCE.md`
   - JADX decompilation and static inventory of 4,285 Java source files.
   - Identified WebView, navigation, bridge, storage, picker and userscript evidence.
   - Found no Chrome/WebExtension runtime indicators; this is a known future implementation task, not part of Step 1.

## CI evidence reused

- **Identity + branding integrated APK build:** [Run 45331285994](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45331285994) — completed successfully.
- **Runtime decompilation inventory:** [Run 45332809697](https://github.com/octopusworldpro-web/Via-Browser/actions/runs/45332809697) — completed successfully.

These are existing successful runs. No duplicate rebuild was started to claim Step 1.

## Step 1 acceptance status

| Item | Status |
|---|---|
| APK structural audit and fingerprint | Done |
| Static reverse-engineering inventory | Done |
| OmegA package identity and version in CI build | Done |
| Adaptive and legacy launcher assets in CI build | Done |
| OmegA label/theme/About resource packaging | CI build passed |
| Original Via app remains a separate package by design | Build identity configured; device install not yet tested |
| Physical device install/launch and visual verification | Pending |
| Production signing key and release APK | Pending |

**Step 1 is complete for repository audit, APK repackaging and CI-verified identity/branding.** It is not a claim of physical-device validation or production release readiness. The pending device and release checks belong in the final integration/testing step; do not redo these implementation tasks in later steps.
