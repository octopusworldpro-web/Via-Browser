# Device Identity Manager — Safe Core

**Status: standalone profile validation and scoped resolution implemented; Android WebView UI/runtime wiring remains pending.**

## Supported browser-exposed values

- User-Agent string
- Accept-Language
- Locale
- IANA time zone
- Optional exact HTTP(S) origin allowlist (maximum 50 origins)

Profiles are validated before use. A non-empty site allowlist restricts overrides to those exact origins; an unmatched site retains device defaults. An empty allowlist means the caller has explicitly chosen an unscoped profile.

## Explicit exclusions

IMEI, MEID, serial number, Android ID, hardware/device IDs, advertising IDs, MAC addresses and fingerprint values are rejected. This utility does not read device hardware identifiers, modify Android identity, patch WebView internals, or promise fingerprint resistance.

## Runtime boundary

This is a dependency-free data/profile core. The Android app layer must obtain user consent, persist profiles in app-private storage, and apply only supported WebView/browser-exposed settings. No setting is applied to a live browser by this module.

## Tests

```sh
node --test utilities/device-identity/device-identity.test.js
```
