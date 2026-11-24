# Step 06 — Runtime Reconnaissance for Extension Integration

**Status:** Automated decompilation reconnaissance added; findings pending CI  
**Base:** Via Browser 7.4.1 compiled APK  
**Purpose:** Identify the actual browser UI, WebView bridge, storage, file-picker and userscript integration points before attempting APK-level implementation.

## Why this step exists

The repository does not contain the original Android source project. The current APK can be decoded and decompiled, but adding a real extension manager or Chrome-compatible runtime requires locating stable application entry points and understanding the existing WebView architecture first. Guessing class names or patching arbitrary DEX methods would risk breaking the browser.

## Automated inspection

Workflow: `.github/workflows/omega-step6-runtime-recon.yml`

It uses pinned JADX v1.5.7 to decompile the supplied APK and inventories source matches for Activity/Fragment entry points, WebView construction/navigation/JavaScript bridges, document picker flows, persistent storage, downloads, existing userscripts, Chrome/WebExtension indicators, package import, service workers and WebView messaging.

The report is uploaded as a GitHub Actions artifact. No decompiled source is committed.

## Implementation gates

1. Identify the browser Activity and WebView owner.
2. Locate the existing settings/menu navigation used to add a manager entry.
3. Determine whether file selection and app-private package storage can be safely reused.
4. Locate the userscript subsystem boundary so it remains untouched.
5. Confirm whether a viable WebExtension runtime is already embedded or must be added.
6. Only then implement a small manager/import vertical slice and rebuild.

## Safety rules

- Decompiled code is evidence, not a source project or proof of buildability.
- Do not claim Chrome extension support from userscript APIs or ZIP/CRX parsing alone.
- Never execute imported package code during inspection.
- Keep extension permissions and storage isolated from browser cookies, passwords, history and private tabs.
- Do not commit generated decompiled source or original app secrets.

## Completion criteria

- [ ] CI decompilation completes.
- [ ] Runtime inventory artifact is generated.
- [ ] Actual app entry points and integration boundaries are identified from source evidence.
- [ ] Feasibility decision is documented for APK-level UI/runtime implementation.
- [ ] No feature is marked runnable until an installed APK is exercised on a device/emulator.

**Current claim:** This step adds the inspection pipeline only. It does not yet implement the extension manager or Chrome extension runtime.
