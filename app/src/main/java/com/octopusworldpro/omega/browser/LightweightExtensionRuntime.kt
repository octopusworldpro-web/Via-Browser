package com.octopusworldpro.omega.browser

import android.content.Context
import android.net.Uri
import android.webkit.WebView
import org.json.JSONObject
import java.io.File

/**
 * Lightweight, content-script-only adapter for Android WebView.
 *
 * Scripts run in the page's JavaScript world. No native/Chrome API bridge is
 * exposed, so this is intentionally not a full or isolated WebExtension runtime.
 */
internal object LightweightExtensionRuntime {
    private const val MAX_SCRIPT_BYTES = 1024 * 1024

    fun injectEnabled(context: Context, webView: WebView, pageUrl: String) {
        val page = Uri.parse(pageUrl)
        if (page.scheme != "http" && page.scheme != "https") return
        val root = File(context.filesDir, "extensions")
        val extensions = root.listFiles()?.filter { it.isDirectory } ?: return
        val prefs = context.getSharedPreferences("omega_browser", Context.MODE_PRIVATE)

        extensions.forEach { directory ->
            if (!prefs.getBoolean(enabledKey(directory.name), false)) return@forEach
            val manifestFile = File(directory, "manifest.json")
            if (!manifestFile.isFile) return@forEach
            try {
                val manifest = JSONObject(manifestFile.readText())
                val contentScripts = manifest.optJSONArray("content_scripts") ?: return@forEach
                for (i in 0 until contentScripts.length()) {
                    val entry = contentScripts.optJSONObject(i) ?: continue
                    val matches = entry.optJSONArray("matches") ?: continue
                    if (!(0 until matches.length()).any { matches.optString(it).let { pattern -> matchesUrl(pageUrl, pattern) } }) continue
                    if (entry.optBoolean("all_frames", false)) continue // top-frame only in this lightweight adapter
                    val files = entry.optJSONArray("js") ?: continue
                    for (j in 0 until files.length()) {
                        val relative = files.optString(j)
                        if (relative.isBlank()) continue
                        val script = File(directory, relative)
                        if (!script.canonicalPath.startsWith(directory.canonicalPath + File.separator) ||
                            !script.isFile || script.length() > MAX_SCRIPT_BYTES) continue
                        val source = script.readText()
                        val encoded = JSONObject.quote(source)
                        webView.evaluateJavascript(
                            "(function(){try{(0,eval)($encoded)}catch(e){console.error('OmegA extension script failed',e)}})();",
                            null
                        )
                    }
                }
            } catch (_: Exception) {
                // A broken extension must not interrupt page navigation.
            }
        }
    }

    fun enabledKey(id: String) = "extension_enabled_$id"

    private fun matchesUrl(url: String, pattern: String): Boolean {
        if (pattern == "<all_urls>") return url.startsWith("http://") || url.startsWith("https://")
        val separator = pattern.indexOf("://")
        if (separator <= 0) return false
        val schemePattern = pattern.substring(0, separator)
        val rest = pattern.substring(separator + 3)
        val slash = rest.indexOf('/')
        val hostPattern = if (slash < 0) rest else rest.substring(0, slash)
        val pathPattern = if (slash < 0) "/*" else rest.substring(slash)
        val uri = Uri.parse(url)
        val scheme = uri.scheme ?: return false
        val host = uri.host ?: return false
        if (schemePattern != "*" && schemePattern != scheme) return false
        val hostMatches = when {
            hostPattern == "*" -> true
            hostPattern.startsWith("*.") -> host == hostPattern.substring(2) || host.endsWith(".${hostPattern.substring(2)}")
            else -> host.equals(hostPattern, ignoreCase = true)
        }
        if (!hostMatches) return false
        val pathRegex = Regex("^" + Regex.escape(pathPattern).replace("\\\\*", ".*") + "$")
        val path = (uri.path ?: "/") + (uri.encodedQuery?.let { "?$it" } ?: "")
        return pathRegex.matches(path)
    }
}
