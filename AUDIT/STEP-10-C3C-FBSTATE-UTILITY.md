# Step 5 — C3C FBState Utility

**Status: SPECIFICATION READY — Android app integration is blocked by the missing buildable Via source project.**

## Goal

Provide a small, built-in utility for the user to manually import, inspect/validate, normalize and export Facebook session-state data. This is a local file/text utility, not an automatic browser-cookie collector and not a login bypass.

## Supported user flows

1. User opens Tools → C3C FBState Utility.
2. User chooses one input explicitly:
   - Select an AppState JSON file.
   - Paste a Cookie header/string.
   - Select a Netscape cookie file.
3. Show a clear warning that Facebook session cookies are equivalent to account credentials.
4. Validate structure and supported Facebook/Messenger domains; reject malformed or unsupported input.
5. Show a masked preview by default. Never print full values to logs or analytics.
6. User chooses an explicit output:
   - AppState JSON.
   - Netscape cookie file.
7. Ask for confirmation before writing/exporting. Use Android's document picker / Storage Access Framework rather than broad storage permission.

## Security and privacy rules

- No automatic extraction from the browser's WebView cookie store.
- No background polling, collection or export.
- No network upload, cloud sync, telemetry or credential logging.
- No password collection or password storage.
- Require consent for each import and each export.
- Keep temporary working data in app-private storage and clear it when the utility closes.
- Mask c_user, xs and all other cookie values in previews; reveal only after an explicit user action.
- Accept only the supported Facebook/Messenger cookie domains; reject unrelated domains.
- Do not inject imported cookies into the active browser session automatically. Any future session-apply action must be separate, explicit, origin-scoped and confirmed.
- Do not bypass Facebook authentication, checkpoints, 2FA or security controls.
- Do not claim that imported state is valid or logged in until the user verifies it through the official site.

## Lightweight implementation

- Reuse Android's built-in file picker and existing JSON utilities where available.
- No new browser engine, native cookie interception layer, analytics SDK or third-party UI framework.
- Keep the utility independent of the extension runtime.
- Avoid copying session data into clipboard automatically.
- Do not include real session values in tests, fixtures, screenshots or CI logs.

## Acceptance criteria

- AppState JSON, Cookie header and Netscape cookie inputs are parsed and validated.
- Missing c_user or xs, malformed JSON, invalid cookie syntax and unsupported domains produce clear errors.
- Import/export always requires a direct user action and per-operation confirmation.
- Preview is masked by default.
- No automatic WebView cookie extraction or network transfer is present.
- Temporary data is cleared on exit.
- Tests use synthetic cookie values only.
- APK size and regression checks pass on supported Android versions.

## Current blocker

The repository currently contains a compiled Via APK and repackaging workflows, not a buildable Android Studio/Gradle source project. The specification and safety configuration can be committed now, but a working in-app utility cannot be honestly claimed or safely integrated through resource-only APK repackaging. The source project is required for implementation and device testing.
