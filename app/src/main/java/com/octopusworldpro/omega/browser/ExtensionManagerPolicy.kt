package com.octopusworldpro.omega.browser

import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

internal data class ExtensionSummary(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val requestedPermissions: List<String>,
    val hostPatterns: List<String>,
    val compatible: Boolean,
    val incompatibility: String?,
    val storageBytes: Long,
    val digest: String
)

internal object ExtensionManagerPolicy {
    fun inspect(directory: File): ExtensionSummary {
        val manifestFile = File(directory, "manifest.json")
        require(manifestFile.isFile && manifestFile.length() <= 256L * 1024L) { "Missing or oversized manifest" }
        val raw = manifestFile.readText()
        val manifest = JSONObject(raw)
        val mv = manifest.optInt("manifest_version", 0)
        require(mv in 2..3) { "Unsupported manifest version" }
        val name = manifest.optString("name").trim()
        val version = manifest.optString("version").trim()
        require(name.isNotBlank() && version.isNotBlank()) { "Name/version missing" }

        val permissions = mutableListOf<String>()
        val api = manifest.optJSONArray("permissions")
        if (api != null) for (i in 0 until api.length()) permissions += api.optString(i)
        val hosts = manifest.optJSONArray("host_permissions")
        if (hosts != null) for (i in 0 until hosts.length()) permissions += "host:${hosts.optString(i)}"
        val legacyHosts = manifest.optJSONArray("permissions")
        if (legacyHosts != null) for (i in 0 until legacyHosts.length()) {
            val p = legacyHosts.optString(i)
            if (p.contains("://") || p == "<all_urls>") permissions += "host:$p"
        }
        val contentScripts = manifest.optJSONArray("content_scripts")
        val hasScripts = contentScripts != null && contentScripts.length() > 0
        val action = manifest.optJSONObject("action") ?: manifest.optJSONObject("browser_action")
        val hasPopup = action?.optString("default_popup").orEmpty().isNotBlank()
        val background = manifest.optJSONObject("background")
        val backgroundScripts = background?.optJSONArray("scripts")
        val hasActiveBackground = background?.optString("service_worker").orEmpty().isNotBlank() ||
            background?.optString("page").orEmpty().isNotBlank() ||
            (backgroundScripts != null && backgroundScripts.length() > 0)
        val supportedPopupApis = setOf("storage", "cookies", "activeTab", "tabs")
        val unsupportedApi = permissions.any { !it.startsWith("host:") && it !in supportedPopupApis }
        val incompatible = (!hasScripts && !hasPopup) || hasActiveBackground || unsupportedApi
        val reason = when {
            !hasScripts && !hasPopup -> "No popup or content scripts declared"
            hasActiveBackground -> "Active background scripts/service workers are not supported"
            unsupportedApi -> "One or more requested Chrome APIs are not supported"
            else -> null
        }
        val md = MessageDigest.getInstance("SHA-256")
        directory.walkTopDown().filter { it.isFile }.sortedBy { it.relativeTo(directory).invariantSeparatorsPath }.forEach { file ->
            md.update(file.relativeTo(directory).invariantSeparatorsPath.toByteArray())
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    md.update(buffer, 0, count)
                }
            }
        }
        val digest = md.digest().joinToString("") { "%02x".format(it) }
        val size = directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return ExtensionSummary(
            directory.name, name, version, manifest.optString("description"), manifest.optString("author"),
            permissions.distinct(), permissions.filter { it.startsWith("host:") }.map { it.removePrefix("host:") }.distinct(),
            !incompatible, reason, size, digest
        )
    }
}
