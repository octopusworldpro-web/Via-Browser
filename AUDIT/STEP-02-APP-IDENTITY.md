# Step 02 — OmegA Browser App Identity

**Status:** Complete — application identity and side-by-side installation contract finalized  
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

## Scope boundary

This step finalizes the app identity contract. It does **not** claim that the APK has already been repackaged or signed. Applying these values to the binary and verifying side-by-side installation belongs to the APK rebuild/signing and device-test stages.
