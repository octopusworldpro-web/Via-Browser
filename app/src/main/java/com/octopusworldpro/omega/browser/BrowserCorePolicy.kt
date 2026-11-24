package com.octopusworldpro.omega.browser

import java.net.URI

internal object BrowserCorePolicy {
    fun isWebNavigation(url: String): Boolean = try {
        val uri = URI(url)
        (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }

    fun externalScheme(url: String): String? {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme in setOf("http", "https", "about", "javascript", "data", "file", "content")) null else scheme
        } catch (_: Exception) { null }
    }
}
