# OmegA Browser — Final Logo Direction

The approved visual direction is the premium purple-neon / chrome OmegA mark.

## Master artwork
- `omega-browser-logo.svg` — editable vector master, 1024×1024.
- Dark near-black violet background with restrained purple glow.
- Metallic Omega-inspired browser ring, globe grid, orbit swoosh and crown.
- OmegA wordmark with violet accent on the final A; BROWSER subtitle.
- No external fonts, scripts, or raster dependencies.

## App icon usage
Use the symbol and globe as the launcher crop; do not shrink the full wordmark into a tiny launcher icon. Keep the full master for splash/about/branding. Export Android density-specific PNG/WebP assets from this SVG during APK resource integration and verify legibility at small launcher sizes.

## Lightweight constraints
The SVG is standalone and uses gradients, paths, circles and text. No external image, font, or runtime library is required. Convert text to paths or use a bundled/system-safe font at final export to avoid device font variation.

## Status
Approved visual direction committed as the current OmegA Browser master logo. Final Android launcher assets will be generated and tested in the branding integration/build stage.
