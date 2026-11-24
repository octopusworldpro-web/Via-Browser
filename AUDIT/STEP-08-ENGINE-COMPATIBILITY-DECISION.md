# Step 08 — Extension Engine Compatibility Decision

**Status: IN PROGRESS — compatibility research completed; engine choice is a blocking product decision.**

## User-facing target

The requested behavior is comparable to Kiwi/Lemur:
- install locally supplied extensions,
- show extension toolbar entries and open extension popup UI,
- run content scripts and background logic,
- expose extension APIs and messaging,
- manage enable/disable, remove, permissions and per-site access.

The current app is an Android WebView browser. Its existing ZIP importer and `LightweightExtensionRuntime` are not a Chrome/WebExtension runtime.

## Engine research

### GeckoView

Mozilla's current GeckoView API provides a real WebExtension controller with install, list, enable, disable and uninstall operations. It provides isolated content-script execution and extension messaging. This is materially better than the current WebView adapter.

However, GeckoView is **not a drop-in Chromium extension engine**:
- It implements Firefox's WebExtensions platform, not Chromium's complete extension API surface.
- GeckoView's ordinary `install(uri)` flow validates Mozilla signatures; arbitrary unsigned ZIP folders are not equivalent to installable add-ons.
- The documented `installBuiltIn` flow is for extensions bundled in the APK under `resource://android/`, not a general user ZIP installer.
- Firefox for Android currently documents important MV3 limitations; notably background service workers are unsupported and event pages are the alternative.
- Chrome Web Store extensions may depend on Chromium-specific APIs and cannot be promised to work unchanged.

Sources:
- https://firefox-source-docs.mozilla.org/mobile/android/geckoview/consumer/web-extensions.html
- https://mozilla.github.io/geckoview/javadoc/mozilla-central/org/mozilla/geckoview/WebExtensionController.html
- https://mozilla.github.io/geckoview/consumer/docs/geckoview-quick-start
- https://extensionworkshop.com/documentation/develop/developing-extensions-for-firefox-for-android/

### Chromium / Kiwi-style target

For high compatibility with Chrome Web Store packages and Chromium `chrome.*` APIs, the engine needs a Chromium-based implementation with an extension subsystem (as Kiwi maintains). Android System WebView does not provide this subsystem. This is a substantial browser-engine integration/fork project, not a ZIP-importer or UI-only change.

## Current code facts

- `MainActivity` is tightly coupled to `android.webkit.WebView` for navigation, history, downloads, file chooser, settings, page lifecycle and tabs.
- `LightweightExtensionRuntime` injects scripts in the page JavaScript world and does not implement isolated worlds, extension background execution, extension messaging or `chrome.*` APIs.
- The existing ZIP importer stores unpacked folders under app-private `filesDir/extensions`; those folders cannot simply be passed to GeckoView's public `installBuiltIn` API.
- `app/build.gradle` currently has `minSdk 19`. GeckoView's documented integration uses Java 17 and a separate Mozilla Maven repository; the app's current minimum Android version and APK size would need a deliberate compatibility review before migration.

## Decision gate

Do not silently replace WebView with GeckoView and then advertise Kiwi-level Chrome compatibility. That would change the extension ecosystem and still leave key Chrome MV3 behavior unsupported.

Before selecting an engine, the product requirement must be one of:

1. **Chrome extension compatibility is mandatory:** pursue a Chromium-based engine/fork with an extension subsystem. This is the path aligned with Kiwi/Lemur expectations, but has substantially greater integration and maintenance cost.
2. **Firefox WebExtensions are acceptable:** evaluate GeckoView as a separate browser-engine migration, with Mozilla-signed installation constraints and Android API limitations clearly surfaced.

## Acceptance criteria after engine selection

- Engine builds for supported Android ABIs and minimum Android version.
- Real extension package install succeeds or reports a specific signature/compatibility error.
- Popup action opens the extension's actual popup document.
- Content scripts run in isolated worlds.
- Background/event lifecycle and extension messaging work.
- API and host permissions are prompted and enforced.
- Enable/disable/remove persist across app restarts.
- Tabs, downloads, file chooser, cookies, history, bookmarks, desktop mode, navigation and search regressions are tested.
- APK size and startup/memory impact are measured.
- CI build and automated tests pass; then validate on a physical Android device.

**No full extension compatibility is claimed by this research step.**
