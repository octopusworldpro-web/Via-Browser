# Step 08 — Lightweight Chrome Extension Compatibility Plan

**Status: IN PROGRESS — sample package inspected; implementation scope narrowed for a lightweight APK.**

## User requirement

Support locally supplied Chrome extension ZIP files so their normal UI and supported behavior work, while keeping OmegA Browser lightweight. Do not turn this project into a full desktop Chromium port.

## Sample package inspected

Archive: `c3c-fbstate-1.1(1).zip`

Manifest facts:
- Manifest V2
- `browser_action.default_popup` points to `popup.html`
- popup loads local JavaScript libraries (`aes.js`, `popup.js`)
- declares `cookies`, `activeTab` and Facebook host access
- declares a non-persistent background script (`background.js`, empty in this sample)
- no `content_scripts` declaration

Popup structure and behavior relevant to a generic extension host:
- render the extension's own HTML/CSS/JS in a popup surface
- load local relative JS assets in declared order
- support popup DOM interactions and file input controls
- provide a Chrome-compatible API bridge only for explicitly implemented APIs
- request/grant permissions before exposing privileged APIs
- show a useful error for unsupported APIs or manifest features

The sample's cookie export/import behavior is not part of the implementation requirements. Its manifest and popup are used only to identify the extension packaging and runtime features the browser needs to host.

## Lightweight implementation target

Keep Android WebView as the page engine for now. Build a bounded extension host around it instead of embedding a full Chromium browser engine.

### Phase A — install and package handling
- accept local ZIP packages with `manifest.json` at the root or one top-level folder
- safely extract with path traversal, duplicate path, file count, per-file and total-size limits
- validate manifest version, name, version, popup paths, script paths and referenced resources
- retain package files in app-private storage; persist enabled state and package identity
- support update, disable, enable and remove without losing the previous package on failed update
- show requested permissions and compatibility status before enabling

### Phase B — popup host
- read `action.default_popup` (MV3) and `browser_action.default_popup` (MV2)
- open the extension's actual popup HTML in a small themed WebView surface
- resolve relative assets from the extension directory without exposing arbitrary filesystem access
- support local CSS/JS/images, DOM events, forms, file selection and downloads where safely supported
- apply a restrictive WebView configuration and a strict app-controlled origin/resource loader
- close popup on outside tap/back and handle popup sizing/scrolling

### Phase C — small Chrome API compatibility layer
- expose `chrome.runtime.getManifest`, `chrome.runtime.getURL`, basic runtime messaging and `chrome.storage.local`
- persist extension storage separately per extension
- support `chrome.tabs` only for safe browser-owned tab operations that the app can implement
- implement permission checks and host access grants before any privileged operation
- do not expose APIs that are not implemented; return documented errors instead of fake success
- explicitly classify `cookies`, `webRequest`, `debugger`, native messaging, identity and other sensitive/unsupported APIs as unsupported unless separately reviewed and implemented safely

### Phase D — content scripts
- parse MV2/MV3 `content_scripts` match patterns and exclude matches
- support `run_at`, JS/CSS file ordering and top-frame injection
- keep a strict script size limit and handle script errors without breaking page navigation
- current WebView `evaluateJavascript` runs in the page world; it is not an isolated extension world. Do not claim isolation or full Chrome compatibility.

### Phase E — background and lifecycle
- support only a bounded event/background model that can be maintained reliably within Android lifecycle limits
- determine which MV2 event-page patterns can be supported without persistent background processes
- MV3 service workers are not equivalent to a normal Android WebView JavaScript context; unsupported lifecycle APIs must be reported
- do not claim general background service-worker support until implemented and tested

## Sample-derived compatibility checklist

- [ ] Import this ZIP and detect its MV2 manifest.
- [ ] Read and display the declared popup path.
- [ ] Render the popup HTML and its local JS/CSS dependencies.
- [ ] Make popup file input and ordinary DOM interactions work.
- [ ] Parse and display requested permissions.
- [ ] Ensure unsupported privileged APIs fail clearly rather than silently.
- [ ] Enable/disable/remove and persist package state.

## Lightweight constraints and non-goals

- No full Chromium engine or desktop Chrome Web Store integration in this phase.
- No promise that every Chrome extension will run; desktop-only APIs, unsupported permissions, native messaging, service workers and browser-internal APIs may require a different engine.
- Keep APK size, startup time and RAM use as first-class acceptance criteria.
- Preserve existing OmegA browser tabs, navigation, downloads, bookmarks, history, settings and UI.

## Acceptance criteria

- ZIP import succeeds safely and survives app restart.
- The sample's popup UI and local assets load in the browser's extension popup surface.
- Generic popup extensions using the implemented API subset can execute without modifying their source.
- Unsupported APIs/manifest features are identified before enablement and produce explicit errors.
- Package install/update/remove and permission decisions persist.
- Existing browser regression tests and Android CI pass.
- Measure APK size, cold start and memory before/after the extension host.
- Test on a physical Android device before claiming completion.

**Compatibility promise:** target a documented, lightweight subset of Chrome extensions, beginning with popup-based MV2/MV3 packages and safe common APIs. Do not claim support for arbitrary Chrome extensions until their required APIs and lifecycle are actually implemented.