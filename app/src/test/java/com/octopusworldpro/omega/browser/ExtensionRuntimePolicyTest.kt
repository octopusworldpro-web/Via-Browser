package com.octopusworldpro.omega.browser

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ExtensionRuntimePolicyTest {
    @Test fun matchesExactAndWildcardHosts() {
        assertTrue(ExtensionRuntimePolicy.matchesUrl("https://example.com/a", "https://example.com/*"))
        assertTrue(ExtensionRuntimePolicy.matchesUrl("https://sub.example.com/a", "https://*.example.com/*"))
        assertFalse(ExtensionRuntimePolicy.matchesUrl("https://badexample.com/a", "https://*.example.com/*"))
        assertFalse(ExtensionRuntimePolicy.matchesUrl("http://example.com/a", "https://example.com/*"))
    }

    @Test fun rejectsUnsupportedSchemesAndMalformedPatterns() {
        assertFalse(ExtensionRuntimePolicy.matchesUrl("file:///tmp/a", "<all_urls>"))
        assertFalse(ExtensionRuntimePolicy.matchesUrl("javascript:alert(1)", "<all_urls>"))
        assertFalse(ExtensionRuntimePolicy.matchesUrl("https://example.com/", "https://*bad.example/*"))
        assertFalse(ExtensionRuntimePolicy.matchesUrl("not a url", "https://example.com/*"))
    }

    @Test fun checksScriptPathAndSize() {
        val root = createTempDir(prefix = "omega-extension-test")
        try {
            File(root, "scripts").mkdirs()
            File(root, "scripts/content.js").writeText("console.log('ok')")
            assertTrue(ExtensionRuntimePolicy.isSafeScript(root, "scripts/content.js", 1024))
            assertFalse(ExtensionRuntimePolicy.isSafeScript(root, "../outside.js", 1024))
            assertFalse(ExtensionRuntimePolicy.isSafeScript(root, "/etc/passwd", 1024))
            assertFalse(ExtensionRuntimePolicy.isSafeScript(root, "scripts/content.js", 2))
            assertFalse(ExtensionRuntimePolicy.isSafeScript(root, "missing.js", 1024))
        } finally { root.deleteRecursively() }
    }
}
