# FBState Utility — Core Module

**Implementation status: standalone parser/validator/exporter implemented; in-app UI and Android integration remain separate.**

## Files

- `utilities/fbstate/fbstate.js` — dependency-free CommonJS module.
- `utilities/fbstate/fbstate.test.js` — Node built-in test suite; no third-party packages.

## Supported operations

- Parse AppState JSON arrays (also accepts an object containing a `cookies` array).
- Parse a pasted Cookie header when the user supplies the intended Facebook/Messenger domain explicitly.
- Parse Netscape cookie files, including HttpOnly rows.
- Validate required `c_user` and `xs` entries, duplicate identities and supported domains.
- Normalize cookie fields and export AppState JSON or Netscape format.
- Produce masked previews; raw values are not included in preview output.

## Safety boundary

- The module performs local string/data transformation only.
- It has no network, browser-cookie-store, filesystem, clipboard or account-login access.
- It does not apply cookies to a browser session or bypass authentication.
- Only facebook.com subdomains and messenger.com subdomains are accepted.
- Tests use synthetic values only.

## Test

Run from repository root:

```sh
node --test utilities/fbstate/fbstate.test.js
```

The in-app screen, Android document-picker integration and permission prompts still need to call this module from the app layer.


## Uploaded extension review — c3c-fbstate-1.1.zip

The supplied archive is a Manifest V2 Chrome extension. Its popup uses the `cookies` permission and calls `chrome.cookies.getAll`, `chrome.cookies.remove`, and `chrome.cookies.set` to export the active Facebook cookie jar or replace cookies during import. Its encrypted mode encrypts/decrypts the same cookie data; encryption does not change the underlying session-credential behavior.

The Android app is a native WebView browser, not a Chrome extension host. The extension's `chrome.*` APIs and Manifest V2 popup cannot be installed as a normal Android extension in this app. The active-cookie export and cookie injection behavior are intentionally not ported.

The app-facing adaptation retains the FBState Utility name and the native, user-selected-file workflow already present in the app. No active WebView cookie-store extraction or cookie injection is added. This is a compatibility/safety decision, not a claim that the original extension has been fully ported.
