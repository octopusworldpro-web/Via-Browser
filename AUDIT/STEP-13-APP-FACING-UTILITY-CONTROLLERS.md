# Step 13 — App-Facing Utility Controller Layer

**Status: dependency-free controller layer implemented and CI-tested; Android Activity, document picker and WebView adapter wiring remain pending.**

## Implemented

`utilities/android-integration/feature-controller.js` exposes one small controller intended for the Android app layer to call:

- FBState import paths for AppState JSON, pasted Cookie header and Netscape file contents.
- Mandatory per-import consent and separate export confirmation.
- Masked preview only; raw values are returned only through an explicitly confirmed export operation.
- Device Identity profile validation, consent-gated save callback and exact-origin scoped apply callback.
- Temporary in-memory state clearing with an app-layer cleanup callback.

The controller accepts adapters rather than reaching into Android APIs. The eventual Activity must supply Android Storage Access Framework results and a narrowly scoped WebView settings adapter. It must not expose cookie-store access or inject imported cookies into a browser session.

## Required Android adapter behavior

1. Use ACTION_OPEN_DOCUMENT for user-selected files and persist only URI permissions required for the operation.
2. Read the selected file into a bounded in-memory string; enforce a size limit before parsing.
3. Ask for consent before import and confirmation before export.
4. Use ACTION_CREATE_DOCUMENT for the user-selected output destination.
5. Store saved identity profiles only in app-private storage; apply only supported WebView/browser-exposed settings.
6. Call clearTemporaryData when the utility screen closes or the app process is finished.
7. Never log cookie values, include them in crash reports, or send them to network services.

## Validation

Run:

```sh
node --test utilities/fbstate/fbstate.test.js
node --test utilities/device-identity/device-identity.test.js
node --test utilities/android-integration/feature-controller.test.js
```

## Integration boundary

This controller is app-facing business logic, not an Android screen and not a live WebView integration. The current repository still lacks the Via Android Studio/Gradle source project, so it cannot yet be wired into the shipped APK or device-tested here.
