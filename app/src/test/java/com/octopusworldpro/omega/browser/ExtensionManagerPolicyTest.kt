package com.octopusworldpro.omega.browser

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ExtensionManagerPolicyTest {
    @Test fun inspectsContentScriptPackageWithoutExecutingIt() {
        val dir = createTempDir()
        try {
            File(dir, "manifest.json").writeText("""{"manifest_version":3,"name":"Sample","version":"1.2","content_scripts":[{"matches":["https://example.com/*"],"js":["content.js"]}]}""")
            val info = ExtensionManagerPolicy.inspect(dir)
            assertEquals("Sample", info.name)
            assertTrue(info.compatible)
            assertEquals("1.2", info.version)
            assertEquals(64, info.digest.length)
        } finally { dir.deleteRecursively() }
    }

    @Test fun marksBackgroundRuntimeAsIncompatible() {
        val dir = createTempDir()
        try {
            File(dir, "manifest.json").writeText("""{"manifest_version":3,"name":"Background","version":"1","background":{"service_worker":"bg.js"},"content_scripts":[{"matches":["<all_urls>"],"js":["content.js"]}]}""")
            val info = ExtensionManagerPolicy.inspect(dir)
            assertFalse(info.compatible)
            assertTrue(info.incompatibility!!.contains("Background"))
        } finally { dir.deleteRecursively() }
    }
}
