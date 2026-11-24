# C3C FBState Utility — Core Module

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
