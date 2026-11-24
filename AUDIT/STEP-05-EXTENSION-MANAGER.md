# Step 05 — OmegA Extension Manager

**Status:** Manager design and integration contract complete; APK implementation is blocked by the missing Via source/build project  
**Product:** OmegA Browser  
**Planned integration window:** Steps 05–07  
**Base:** Via Browser 7.4.1 / Android WebView

## 1. Goal and boundary

Provide one in-browser place to inspect and control installed extensions without replacing Via's existing userscript manager or changing the browser engine.

The manager is the control plane. It owns extension records, user actions, permission decisions, lifecycle state and storage references. It does **not** itself make WebView execute Chrome extensions. A compatible runtime/bridge is a separate dependency and must be delivered and validated in Steps 06–07 before an extension can be marked runnable.

No extension catalog/server is required. The manager will support local sources and, when the compatibility layer is ready, a Chrome Web Store handoff/installation flow.

## 2. Manager screens

### Extension list
Each installed item displays:
- Name and version (from validated package metadata).
- Publisher/author (untrusted display metadata, clearly labeled).
- Source: local ZIP, CRX, or Web Store.
- State: Installed, Enabled, Disabled, Needs permission, Incompatible, or Error.
- A short capability/permission summary.
- Last error, only when an actionable error exists.

Actions: open details, enable/disable, update, uninstall. The manager must not show an Enable action for an incompatible package/runtime.

### Extension details
- Description, version, source and package ID.
- Requested permissions and host access patterns.
- Granted permissions separated from requested permissions.
- Per-extension site access: on click / selected sites / all sites, only where supported by the runtime.
- Runtime compatibility and unsupported API warnings.
- Storage usage and clear extension data.
- Update and uninstall controls.

### Add extension
- Local ZIP/CRX picker entry point.
- Chrome Web Store entry point is shown only when the Web Store flow and compatibility layer are available; otherwise show a disabled row with a clear “not available in this build” explanation.
- No built-in extension catalog and no account sign-in requirement.

## 3. Core components and responsibilities

| Component | Responsibility |
|---|---|
| `ExtensionManager` | Single application-facing API for list, inspect, enable, disable, update and uninstall |
| `ExtensionRepository` | Persist metadata/state in app-private storage; transaction-safe writes |
| `PackageInspector` | Parse package metadata without executing package code |
| `PermissionBroker` | Explain, request and persist explicit user grants |
| `ExtensionRuntimeAdapter` | Narrow interface to the future WebExtension compatibility runtime |
| `ExtensionStorage` | Allocate isolated per-extension persistent storage and enforce quotas |
| `ExtensionImportCoordinator` | Receive SAF document URI, copy to staging, validate, then hand off to installer |
| `ExtensionUpdateCoordinator` | Compare versions and package identity; require confirmation before replacing |
| `ExtensionErrorReporter` | Surface actionable parse, permission, runtime and migration failures |

The browser UI must call the manager API; it must not directly mutate extension files or permission records.

## 4. Lifecycle state machine

```text
Not installed
   |
   | import + validate
   v
Staged -> Rejected (invalid / unsupported / unsafe package)
   |
   | user confirms permissions
   v
Installed / Disabled
   |
   | runtime supports required APIs + user enables
   v
Enabled
   |  \
   |   \ runtime/API failure
   |    v
   |  Error / Incompatible
   |
   | user disables
   v
Installed / Disabled
   |
   | user uninstalls
   v
Removed (metadata + package + owned storage deleted)
```

Rules:
1. Package parsing and validation happen before any extension code can execute.
2. Import never silently enables an extension.
3. Enabling requires a compatible runtime and an explicit user action.
4. Updating preserves the extension ID only when package identity/signature policy validates; otherwise treat it as a different package and ask the user.
5. Uninstall removes only files and data owned by that extension. Never touch browser bookmarks, history, cookies or downloads.
6. Disable must stop future extension event delivery and content-script injection. If the runtime cannot guarantee that, disable is not considered complete.

## 5. Persistent data model

Store one record per installed extension in app-private storage. Do not store executable package bytes inside the metadata database.

Required fields:
- `id`: validated extension identifier (or a stable OmegA-generated ID for package types without a stable ID).
- `name`, `version`, `description`, `author`: parsed metadata; untrusted display strings.
- `sourceType`: `local_zip`, `local_crx`, `web_store`.
- `packagePath`: relative path under the extension-owned private directory; never an arbitrary external path.
- `packageDigest`: SHA-256 of the installed package payload.
- `enabled`: boolean; defaults to false.
- `requestedPermissions`, `grantedPermissions`: separate lists.
- `hostAccessMode`: `on_click`, `selected_sites`, or `all_sites`, where supported.
- `runtimeCompatibility`: `unknown`, `compatible`, `partial`, `incompatible`.
- `installTime`, `updateTime`: UTC epoch milliseconds.
- `lastErrorCode`: nullable stable error code; no secrets or page content.

Database changes must be atomic. If metadata migration fails, retain the previous known-good record and package.

## 6. Permission and security policy

- Default to least privilege. Never auto-grant broad host access.
- Explain permissions in plain language before enabling.
- Separate API permissions from website/host permissions.
- Ask again when an update adds permissions or expands host access.
- Keep extension data isolated by extension ID and separate from browser profile databases.
- Do not expose raw browser cookies, saved passwords, autofill contents, browsing history or private tabs through a generic extension bridge.
- Any future cookie API must be explicitly designed, permission-gated and restricted; no ambient cookie access.
- Validate ZIP entry paths; reject absolute paths, traversal (`../`), duplicate/conflicting entries, symlinks and decompression bombs.
- Enforce package size, expanded-size, file-count and metadata limits before extraction.
- Store packages in app-private storage; never execute from arbitrary shared-storage paths.
- Treat name, description, author, icons and update metadata as untrusted input. Escape UI strings and avoid loading remote icons automatically.
- A package's own metadata is not proof of publisher identity. Do not display a “verified” badge without a verifiable trust mechanism.
- No extension code runs during import, preview or permission review.

## 7. WebView/runtime contract

The current Via APK is WebView-based and the static audit found userscript APIs, but no confirmed full `chrome.runtime` / `chrome-extension://` implementation. Therefore the manager must use an adapter boundary rather than calling Chrome APIs directly.

Minimum adapter contract:
- `capabilities()`: runtime version and supported API names.
- `validate(package)`: static validation result and unsupported requirements.
- `install(package, grants)`: install validated package in isolated storage.
- `enable(id)` / `disable(id)`: lifecycle control.
- `remove(id, clearData)`: remove package and optionally its owned storage.
- `update(id, package, grants)`: atomic update/rollback.
- `status(id)`: runtime state and last error.

If no adapter is installed, the manager may display imported/inspected package metadata as “Not runnable in this build”; it must not claim successful installation/execution. Existing Via userscripts remain managed by Via's existing userscript subsystem and must not be migrated or deleted by this manager.

## 8. Performance and UX constraints

- Load the extension list on demand; do not scan package archives at every browser startup.
- No permanent foreground service, polling loop or background network check.
- Keep metadata small and cache parsed manifests.
- Run archive inspection off the UI thread with bounded memory.
- Lazy-load details and icons.
- Do not block page rendering while opening the manager.
- Respect OmegA light/dark theme tokens and compact Via navigation patterns.

## 9. Acceptance checklist

These are implementation acceptance criteria, not claimed runtime test results:

- [ ] Empty state and Add Extension entry render correctly.
- [ ] Invalid/corrupt archive is rejected without partial install.
- [ ] Valid package metadata can be previewed without executing code.
- [ ] Import does not auto-enable.
- [ ] Requested and granted permissions are visibly distinct.
- [ ] Enable is blocked when runtime or required APIs are unavailable.
- [ ] Enable/disable state persists after process death and device restart.
- [ ] Update with added permissions prompts again.
- [ ] Failed update rolls back to the last working package.
- [ ] Uninstall clears only extension-owned files/data.
- [ ] Existing Via userscripts still work and remain untouched.
- [ ] Browser startup time and memory remain within the baseline budget.
- [ ] No extension manager action exposes cookies/passwords/history by default.

## 10. Step completion statement

**Completed here:** manager screens, responsibilities, lifecycle, persistent data contract, permission/security rules, runtime adapter boundary, performance constraints and acceptance criteria are defined and committed.

**Not completed/claimed:** Android UI code, database implementation, archive importer, extension runtime or APK integration. The repository currently has the original compiled APK and documentation/resources, not the Via source project; the current toolchain also lacks the Android reverse-engineering/build tools needed to safely inject these components. Steps 06–07 must implement and test the package/runtime path before this manager can be called operational.

Step 05 is complete as the manager design and integration contract. Its runnable APK implementation remains blocked on obtaining/reconstructing a buildable Android source tree.
