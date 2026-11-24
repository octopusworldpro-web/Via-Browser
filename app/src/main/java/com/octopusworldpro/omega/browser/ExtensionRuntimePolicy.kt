package com.octopusworldpro.omega.browser

import java.io.File
import java.net.URI

internal object ExtensionRuntimePolicy {
    fun matchesUrl(url: String, pattern: String): Boolean = try {
        val target = URI(url)
        if (target.scheme !in listOf("http", "https") || target.host.isNullOrBlank()) return false
        if (pattern == "<all_urls>") return true
        val separator = pattern.indexOf("://")
        if (separator <= 0) return false
        val schemePattern = pattern.substring(0, separator)
        val rest = pattern.substring(separator + 3)
        val slash = rest.indexOf('/')
        val hostPattern = if (slash < 0) rest else rest.substring(0, slash)
        val pathPattern = if (slash < 0) "/*" else rest.substring(slash)
        if (schemePattern != "*" && schemePattern != target.scheme) return false
        if (hostPattern.isBlank() || hostPattern.contains(':') || (hostPattern.startsWith("*") && !hostPattern.startsWith("*."))) return false
        val host = target.host.lowercase()
        val hostMatches = when {
            hostPattern == "*" -> true
            hostPattern.startsWith("*.") -> { val suffix = hostPattern.substring(2).lowercase(); suffix.isNotBlank() && (host == suffix || host.endsWith("." + suffix)) }
            else -> host.equals(hostPattern, ignoreCase = true)
        }
        if (!hostMatches) return false
        val path = (target.rawPath?.ifEmpty { "/" } ?: "/") + (target.rawQuery?.let { "?" + it } ?: "")
        Regex("^" + pathPattern.map { if (it == '*') ".*" else Regex.escape(it.toString()) }.joinToString("") + "$").matches(path)
    } catch (_: Exception) { false }

    fun isSafeScript(directory: File, relativePath: String, maxBytes: Long): Boolean {
        if (relativePath.isBlank() || relativePath.startsWith("/") || relativePath.contains(92.toChar())) return false
        if (relativePath.split('/').any { it.isBlank() || it == "." || it == ".." }) return false
        return try {
            val root = directory.canonicalFile
            val candidate = File(root, relativePath).canonicalFile
            candidate.path.startsWith(root.path + File.separator) && candidate.isFile && candidate.length() in 1..maxBytes
        } catch (_: Exception) { false }
    }
}
