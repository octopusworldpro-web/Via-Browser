const fs = require("node:fs");
const path = require("node:path");
const assert = require("node:assert/strict");

const root = path.resolve(__dirname, "..");
const extensionDir = path.join(root, "gecko-poc/src/main/assets/omega_poc_extension");
const manifestPath = path.join(extensionDir, "manifest.json");
const scriptPath = path.join(extensionDir, "content.js");

const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
const contentScript = fs.readFileSync(scriptPath, "utf8");

assert.equal(manifest.manifest_version, 2, "POC manifest must use the supported MV2 test format");
assert.equal(manifest.name, "OmegA Gecko POC Extension");
assert.equal(manifest.version, "1.0.0");
assert.equal(
  manifest.browser_specific_settings?.gecko?.id,
  "omega-gecko-poc@example.invalid",
  "manifest ID must match ensureBuiltIn()"
);

const scripts = manifest.content_scripts;
assert.ok(Array.isArray(scripts) && scripts.length > 0, "at least one content script is required");
assert.ok(
  scripts.some(entry =>
    Array.isArray(entry.matches) &&
    entry.matches.includes("https://example.com/*") &&
    Array.isArray(entry.js) &&
    entry.js.includes("content.js") &&
    entry.run_at === "document_idle"
  ),
  "POC must target example.com and load content.js at document_idle"
);

assert.match(contentScript, /OmegA WebExtension POC active/, "visible execution marker is missing");
assert.match(contentScript, /OmegA Extension Active/, "observable document-title marker is missing");
assert.doesNotMatch(manifest, /<all_urls>|https?:\/\/\*\//, "POC must not request broad host access");

console.log("GeckoView WebExtension POC manifest/content-script checks passed.");
