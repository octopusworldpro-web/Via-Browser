const fs = require("node:fs");
const path = require("node:path");
const assert = require("node:assert/strict");

const root = path.resolve(__dirname, "..");
const extensionDir = path.join(root, "gecko-poc/src/main/assets/omega_poc_extension");
const manifest = JSON.parse(fs.readFileSync(path.join(extensionDir, "manifest.json"), "utf8"));
const contentScript = fs.readFileSync(path.join(extensionDir, "content.js"), "utf8");

assert.equal(manifest.manifest_version, 2, "POC manifest must use MV2");
assert.equal(manifest.name, "OmegA Gecko POC Extension");
assert.equal(manifest.version, "1.0.0");
assert.equal(
  manifest.browser_specific_settings?.gecko?.id,
  "omega-gecko-poc@example.invalid",
  "manifest ID must match ensureBuiltIn()"
);

const contentScripts = manifest.content_scripts;
assert.ok(Array.isArray(contentScripts) && contentScripts.length > 0, "content script entry is required");
assert.ok(
  contentScripts.some(entry =>
    Array.isArray(entry.matches) &&
    entry.matches.length === 1 &&
    entry.matches[0] === "https://example.com/*" &&
    Array.isArray(entry.js) &&
    entry.js.length === 1 &&
    entry.js[0] === "content.js" &&
    entry.run_at === "document_idle"
  ),
  "POC must only target example.com and load content.js at document_idle"
);

const declaredPermissions = [
  ...(Array.isArray(manifest.permissions) ? manifest.permissions : []),
  ...(Array.isArray(manifest.host_permissions) ? manifest.host_permissions : [])
];
assert.ok(
  !declaredPermissions.some(permission =>
    ["<all_urls>", "http://*/*", "https://*/*", "*://*/*"].includes(permission)
  ),
  "POC must not request broad host access"
);
assert.equal(manifest.host_permissions, undefined, "MV2 POC must not declare MV3 host_permissions");

assert.match(contentScript, /OmegA WebExtension POC active/, "visible execution marker is missing");
assert.match(contentScript, /OmegA Extension Active/, "observable document-title marker is missing");

const vm = require("node:vm");
const appended = [];
const document = {
  title: "Example Domain",
  documentElement: {
    dataset: {},
    appendChild(element) { appended.push(element); }
  },
  createElement(tag) {
    assert.equal(tag, "div");
    return { style: {}, setAttribute(name, value) { this[name] = value; } };
  }
};
vm.runInNewContext(contentScript, { document });
assert.equal(document.documentElement.dataset.omegaGeckoExtension, "active");
assert.equal(document.title, "Example Domain | OmegA Extension Active");
assert.equal(appended.length, 1);
assert.equal(appended[0].textContent, "OmegA WebExtension POC active");
assert.equal(appended[0].role, "status");

console.log("GeckoView WebExtension POC manifest, permissions and content-script execution checks passed.");
