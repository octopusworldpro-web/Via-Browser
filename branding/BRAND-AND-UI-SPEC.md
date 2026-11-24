# Step 03 — OmegA Branding, Logo & UI

**Status:** Approved purple-neon/chrome design system and Android vector templates aligned  
**Product:** OmegA Browser  
**Customization credit:** NirZon

## Final approved direction

The authoritative brand direction is the premium purple-neon / chrome OmegA mark defined in `FINAL-LOGO-DIRECTION.md` and `omega-browser-logo.svg`. This supersedes the earlier blue/coral/gold Via-inspired concept in this document.

- Deep near-black violet background with restrained purple glow.
- Metallic Omega-inspired browser ring and globe grid.
- Violet orbit swoosh and crown detail.
- Chrome OmegA wordmark with violet accent on the final A.
- “BROWSER” subtitle; “Customized by NirZon” belongs in About, not over browsing pages.

Do not reuse the original Via three-stroke launcher motif.

## Brand tokens

| Token | Hex | Use |
|---|---|---|
| OmegA violet | `#A855F7` | Primary actions, links, focus |
| OmegA magenta | `#D946EF` | Secondary accent |
| Deep violet | `#100B2B` | Dark surfaces and launcher background |
| Near-black | `#090817` | App background |
| Chrome light | `#F5F3FF` | Light text/highlights |
| Light surface | `#F7F5FC` | Light theme background |
| Light text | `#211A32` | Main light-theme text |
| Dark text | `#F2EDFF` | Main dark-theme text |

## UI rules

1. Preserve Via's familiar browser workflow and compact navigation; branding must not make the browser heavier or add decorative screens.
2. Use violet for primary actions and focus, magenta sparingly for secondary emphasis, and neutral surfaces for content.
3. Keep both light and dark theme tokens available.
4. Keep extension management, local extension import, Web Store entry and Device Identity settings visually consistent with the browser's existing compact settings style.
5. Keep “Customized by NirZon” in the About area, not permanently overlaid on browsing pages.
6. Avoid bundled fonts, large illustrations, animation libraries and extra UI frameworks. Use Android system sans fonts and vector assets.
7. Use an adaptive foreground/background for API 26+, plus a raster fallback generated at build time for API 19–25.

## Files

- `omega-browser-logo.svg` — editable 1024×1024 master.
- `android-res/drawable/omega_launcher_foreground.xml` — lightweight Omega/globe/orbit/crown vector symbol.
- `android-res/drawable/omega_launcher_background.xml` — deep-violet launcher background.
- `android-res/mipmap-anydpi-v26/ic_launcher.xml` — adaptive launcher icon.
- `android-res/values/colors.xml` — brand and theme tokens.
- `android-res/values/strings.xml` — app/About/feature labels.
- `android-res/values/styles.xml` — API-19-compatible lightweight base theme.

## Lightweight constraints

- No new runtime library, bundled font, remote asset, analytics or animation dependency.
- System sans font only.
- Vector launcher artwork; legacy density PNGs are generated at build time.

## Integration and validation boundary

These are reusable resource templates, not yet integrated into a compiled APK. During APK integration:
- Copy resources into the decoded/reconstructed Android resource tree.
- Generate legacy launcher PNGs for API 19–25.
- Apply `@string/app_name`, theme and launcher icon in the manifest.
- Verify adaptive and legacy icons, launcher masking, status-bar behavior and light/dark contrast on API 19 and API 26+.

**Step 03 design/resource alignment is complete. APK integration and device verification remain pending.**
