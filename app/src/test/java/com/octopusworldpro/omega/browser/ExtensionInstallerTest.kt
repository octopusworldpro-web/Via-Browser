package com.octopusworldpro.omega.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExtensionInstallerTest {
    @Test
    fun preparesNestedManifestV2PopupPackageAndCommitsItDisabledByCaller() {
        val root = Files.createTempDirectory("omega-extension-test").toFile()
        try {
            val zip = packageZip(
                "sample/manifest.json" to """{"name":"C3C Sample","version":"1.1","manifest_version":2,"permissions":["activeTab","https://*.example.com/"],"browser_action":{"default_popup":"popup.html","default_icon":{"16":"logo/16.png"}},"background":{"scripts":["background.js"],"persistent":false}}""",
                "sample/popup.html" to "<html><script src='popup.js'></script></html>",
                "sample/popup.js" to "document.title='ready';",
                "sample/background.js" to "",
                "sample/logo/16.png" to "image"
            )
            val prepared = ExtensionInstaller.prepare(ByteArrayInputStream(zip), File(root, "extensions"))
            assertEquals("C3C Sample", prepared.name)
            assertEquals("1.1", prepared.version)
            assertEquals(2, prepared.manifestVersion)
            assertEquals(listOf("https://*.example.com/"), prepared.hostPatterns)
            assertTrue(prepared.stagingDirectory.isDirectory)

            ExtensionInstaller.commit(prepared)
            assertTrue(File(prepared.destinationDirectory, "manifest.json").isFile)
            assertTrue(File(prepared.destinationDirectory, "popup.html").isFile)
            assertFalse(prepared.stagingDirectory.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsZipSlipPaths() {
        val root = Files.createTempDirectory("omega-extension-test").toFile()
        try {
            val zip = packageZip(
                "../outside.txt" to "bad",
                "manifest.json" to """{"name":"Bad","version":"1.0","manifest_version":2}"""
            )
            val error = runCatching {
                ExtensionInstaller.prepare(ByteArrayInputStream(zip), File(root, "extensions"))
            }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)
            assertFalse(File(root, "outside.txt").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsManifestWithMissingPopupResource() {
        val root = Files.createTempDirectory("omega-extension-test").toFile()
        try {
            val zip = packageZip(
                "manifest.json" to """{"name":"Broken","version":"1.0","manifest_version":2,"browser_action":{"default_popup":"missing.html"}}"""
            )
            val error = runCatching {
                ExtensionInstaller.prepare(ByteArrayInputStream(zip), File(root, "extensions"))
            }.exceptionOrNull()
            assertTrue(error?.message?.contains("Popup file not found") == true)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun packageZip(vararg entries: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, value) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }
}
