# Step 03 — OmegA Branding, Logo & UI

**Status:** Design system and reusable Android resource templates complete  
**Product:** OmegA Browser  
**Customization credit:** NirZon

## Direction

Keep the original Via icon's recognizable three forward-leaning color strokes, but redraw them as a distinct OmegA mark on a deep-navy rounded-square field. The new mark uses blue, coral and gold ribbons with a small light accent. It is not a copy of the original white circular app tile.

The original APK was inspected directly: its launcher icon is a white circular tile with blue, orange-red and yellow angled strokes; its bundled `assets/logo.svg` uses the same three-color motif. This OmegA treatment deliberately retains that visual idea while changing the silhouette, background and palette balance.

## Brand tokens

| Token | Hex | Use |
|---|---|---|
| OmegA blue | `#19A7F5` | Primary actions, links, focus |
| OmegA coral | `#FF5A45` | Secondary accent |
| OmegA gold | `#FFC247` | Highlights and selected details |
| Deep navy | `#101827` | Dark surfaces and launcher background |
| Light surface | `#F7F9FC` | Light theme background |
| Light text | `#172033` | Main light-theme text |
| Dark text | `#F2F5FA` | Main dark-theme text |

## UI rules

1. Preserve Via's familiar browser workflow and compact navigation; branding must not make the browser heavier or add decorative screens.
2. Use blue for primary actions, coral/gold sparingly for secondary emphasis, and neutral surfaces for content.
3. Keep both light and dark theme tokens available.
4. Keep extension management, local extension import, Web Store entry and Device Identity settings visually consistent with the browser's existing compact settings style.
5. Keep “Customized by NirZon” in the About area, not permanently overlaid on browsing pages.
6. Avoid bundled fonts, large illustrations, animation libraries and extra UI frameworks. Use Android system sans fonts and vector assets.
7. The launcher icon must have an adaptive foreground/background for API 26+, plus a raster fallback generated at build time for API 19–25.

## Files added

- `branding/omega-browser-logo.svg` — editable master logo.
- `branding/android-res/drawable/omega_launcher_foreground.xml` — vector foreground.
- `branding/android-res/drawable/omega_launcher_background.xml` — lightweight background.
- `branding/android-res/mipmap-anydpi-v26/ic_launcher.xml` — adaptive launcher icon.
- `branding/android-res/values/colors.xml` — brand and theme tokens.
- `branding/android-res/values/strings.xml` — app/About/feature labels.
- `branding/android-res/values/styles.xml` — API-19-compatible lightweight base theme.

## Lightweight constraints

- No new runtime library is introduced by this design kit.
- Logo is vector-based; no bundled bitmap set or custom font is added.
- System sans font is used.
- No remote font, analytics, animation or image-loading dependency is added.

## Integration notes / remaining validation

This repository still contains the original APK rather than a Gradle Android source project. Therefore these are build-ready resource templates, not yet merged into a compiled application. At integration time:
- Copy resources into the reconstructed Android resource tree.
- Generate legacy launcher PNGs from the SVG/vector for API 19–25.
- Apply `@string/app_name` and the launcher icon in the manifest.
- Confirm adaptive icon rendering on API 26+ and legacy icon rendering on API 19–25.
- Verify status-bar behavior, dark/light contrast and launcher masking on physical/emulated devices.

**Step 03 is complete as a branding/UI design and resource package. APK-level application and device verification remain part of the later rebuild/test step; this report does not claim the APK itself has already changed.**
