# Step 08 — Chrome Extension Engine Decision

**Status: IN PROGRESS — target confirmed; Chromium engine integration remains.**

## Confirmed product requirement

The user confirmed: **Chrome-compatible extensions only**. Firefox `.xpi` / GeckoView support is out of scope.

Expected behavior:
- import a local Chrome extension package (ZIP; `.crx` where its signature/package format can be validated),
- install and persist it,
- display its toolbar/menu entry and open its real popup,
- run content scripts in an isolated extension world,
- support background/event execution and extension messaging,
- enforce declared API/host permissions,
- enable, disable, update and remove extensions.

The goal is a practical Kiwi-style Chrome extension experience, not merely extracting ZIP files and injecting scripts.

## Engine decision

**Choose a Chromium-based browser engine with a real Chromium extension subsystem. Do not use GeckoView.**

Reasons:
- Android System WebView does not expose Chrome's extension subsystem or `chrome.*` API runtime.
- GeckoView implements Firefox WebExtensions, not Chromium's complete extension API surface.
- A ZIP importer plus JavaScript injection cannot provide Chrome extension semantics.
- Chromium's own platform documentation identifies Android WebView as a separate embedder; it is not the full Chrome browser embedder.

## Reference implementations researched

### Kiwi Browser source
- https://github.com/kiwibrowser/src
- Kiwi's source repository documents Android Chromium with Chrome extension support.
- The repository is archived/read-only since January 23, 2025, so it is a reference for implementation and patches, not an assumed actively maintained upstream.

### Elixir Browser
- https://github.com/SF-FLAM/ElixirBrowser
- Its README describes an Android Chromium fork with desktop Chrome Web Store extension support.
- The repository README currently says the source code is not yet published. It cannot be treated as an available source dependency.

### Chromium platform documentation
- https://chromium.googlesource.com/chromium/src/+/HEAD/docs/supported_platforms.md
- Android WebView is a distinct Chromium embedder; it does not mean WebView provides Chrome browser extension functionality.

## Current app constraints

- `MainActivity` is tightly coupled to `android.webkit.WebView` for navigation, history, downloads, file chooser, settings, page lifecycle and tabs.
- `LightweightExtensionRuntime` injects scripts in the page JavaScript world and does not implement isolated worlds, extension background execution, extension messaging or `chrome.*` APIs.
- The current ZIP importer stores unpacked folders under app-private `filesDir/extensions`; this is not a functioning Chrome extension runtime.
- The app currently declares `minSdk 19`. A Chromium-based engine integration will require reviewing supported Android versions, ABIs, APK size, native build toolchain and update strategy.
- Existing browser features must be preserved during migration; replacing WebView is not a drop-in change.

## Implementation direction

1. Inventory the Chromium extension-related code and build requirements from available open-source Android Chromium references; verify license and maintenance status before reuse.
2. Establish a buildable Chromium-based engine strategy for this app and supported Android devices. Do not introduce a fake WebView compatibility layer and label it Chrome support.
3. Define a narrow native browser-engine adapter so tabs/navigation/downloads/file chooser can migrate without discarding the existing OmegA UI and browser features.
4. Replace the lightweight injector with the engine's actual extension service.
5. Add local ZIP import first; validate manifest, archive paths, package limits and permissions. Support `.crx` only through a correctly validated CRX installer, not by renaming/extracting it as ZIP.
6. Add extension list, toolbar/menu, popup, permissions, enable/disable and remove flows.
7. Add automated tests with representative MV2 and MV3 extensions, then test on a physical Android device.

## Acceptance criteria

- Engine builds for the declared Android ABIs and minimum Android version.
- Local Chrome extension ZIP installs and persists across app restarts.
- Invalid, unsupported or unsafe packages fail with a clear error.
- A real extension popup opens from the extension menu.
- Content scripts execute in isolated worlds and respect match patterns/run-at rules.
- Supported background/event lifecycle and extension messaging work.
- API and host permissions are surfaced and enforced.
- Enable/disable/remove persist correctly.
- Existing tabs, navigation, downloads, file chooser, cookies, history, bookmarks, desktop mode, search and settings pass regression tests.
- CI build and automated tests pass; physical-device validation is completed.

**Scope note:** “Any Chrome extension” cannot be guaranteed: extensions may rely on desktop-only APIs, unsupported permissions, native messaging or platform-specific capabilities. The app will target broad Chromium extension compatibility and report unsupported requirements clearly.

**No full Chrome extension compatibility is claimed until the engine and acceptance tests above are implemented.**