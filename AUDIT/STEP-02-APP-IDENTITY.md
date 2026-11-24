# Step 02 — OmegA Browser App Identity

**Status:** Identity contract and cloud repack workflow implemented; APK build/install validation pending  
**Base APK:** Via Browser 7.4.1 (`mark.via.gp`)

## Approved identity

| Field | Value |
|---|---|
| Product/app label | `OmegA Browser` |
| New Android application ID | `com.octopusworldpro.omega.browser` |
| Original Via application ID | `mark.via.gp` |
| Base version | `7.4.1` |
| Initial OmegA version name | `7.4.1-omega` |
| Initial OmegA version code | `2026112401` |
| Minimum SDK to preserve | `19` |
| Target SDK from audited APK | `36` |
| Install strategy | Side-by-side; original Via remains untouched |

## Identity and migration rules

1. OmegA Browser will use a distinct Android application ID so it can coexist with the original Via Browser.
2. The original Java/Kotlin code namespace (`mark.via.*`) will not be mass-renamed just to change the app ID. Application ID and code namespace are separate concerns.
3. During repackaging, all manifest package references must be reviewed: provider authorities, custom permissions, deep links, shortcuts/widgets and hard-coded package-ID strings.
4. OmegA will use a new signing identity. The original Via signing key is not available and must not be impersonated.
5. The keystore and passwords must be stored as protected CI secrets, never committed to this public repository.
6. Preserve minSdk 19 unless later compatibility tests justify a change.

## Machine-readable configuration

The approved values are stored in `config/app-identity.json`.

## Cloud build workflow

`.github/workflows/omega-step2-identity.yml` now provides a manually triggered GitHub Actions pipeline that:
- installs Java, Android Build Tools 36.0.0 and Apktool 2.11.1;
- decodes the checked-in Via 7.4.1 APK;
- applies the OmegA application ID, version name/code and app label, and migrates package-scoped manifest strings;
- rebuilds, zipaligns, signs with a temporary test-only key, and verifies the package identity;
- uploads the test APK and build evidence as a short-lived Actions artifact.

The temporary key is deliberately **not** a production release key. A stable protected OmegA keystore must be configured as GitHub Actions secrets before a distributable release build.

## Scope boundary

The identity contract and cloud build workflow are committed. The workflow has not yet been dispatched from this session, so a successful rebuilt APK, full manifest-reference migration, and side-by-side device installation are **not yet verified**. Do not mark APK-level Step 2 complete until the Actions run passes and the artifact is inspected/installed. The base APK has no matching Android source project in this repository, so this is a controlled APK repackaging path, not a source-based rebuild.
