package com.octopusworldpro.omega.browser

import org.junit.Assert.*
import org.junit.Test

class BrowserCorePolicyTest {
    @Test fun allowsOnlyWellFormedHttpAndHttpsNavigation() {
        assertTrue(BrowserCorePolicy.isWebNavigation("https://example.com/path"))
        assertTrue(BrowserCorePolicy.isWebNavigation("http://example.com"))
        assertFalse(BrowserCorePolicy.isWebNavigation("file:///sdcard/test.html"))
        assertFalse(BrowserCorePolicy.isWebNavigation("javascript:alert(1)"))
        assertFalse(BrowserCorePolicy.isWebNavigation("https:///missing-host"))
    }

    @Test fun identifiesExternalSchemesWithoutTreatingWebUrlsAsExternal() {
        assertEquals("mailto", BrowserCorePolicy.externalScheme("mailto:test@example.com"))
        assertEquals("tel", BrowserCorePolicy.externalScheme("tel:+123456"))
        assertNull(BrowserCorePolicy.externalScheme("https://example.com"))
        assertNull(BrowserCorePolicy.externalScheme("file:///sdcard/a"))
    }
}
