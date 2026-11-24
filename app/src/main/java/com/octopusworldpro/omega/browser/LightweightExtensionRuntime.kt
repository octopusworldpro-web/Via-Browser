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
            val extensionId = directory.name
            val origin = "${page.scheme}://${page.authority}"
            val mode = prefs.getString(siteModeKey(extensionId), "blocked") ?: "blocked"
            val allowed = when (mode) {
                "all_sites" -> true
                "selected_sites" -> prefs.getStringSet(selectedSitesKey(extensionId), emptySet())?.contains(origin) == true
                "on_click" -> {
                    val grants = prefs.getStringSet(oneTimeSitesKey(extensionId), emptySet())?.toMutableSet() ?: mutableSetOf()
                    val hasGrant = grants.remove(origin)
                    if (hasGrant) prefs.edit().putStringSet(oneTimeSitesKey(extensionId), grants).apply()
                    hasGrant
                }
                else -> false
            }
            if (!allowed) return@forEach
            val manifestFile = File(directory, "manifest.json")
            if (!manifestFile.isFile) return@forEach
            try {
                val manifest = JSONObject(manifestFile.readText())
                val contentScripts = manifest.optJSONArray("content_scripts") ?: return@forEach
                for (i in 0 until contentScripts.length()) {
                    val entry = contentScripts.optJSONObject(i) ?: continue
                    val matches = entry.optJSONArray("matches") ?: continue
                    if (!(0 until matches.length()).any { matches.optString(it).let { pattern -> ExtensionRuntimePolicy.matchesUrl(pageUrl, pattern) } }) continue
                    if (entry.optBoolean("all_frames", false)) continue // top-frame only in this lightweight adapter
                    val files = entry.optJSONArray("js") ?: continue
                    for (j in 0 until files.length()) {
                        val relative = files.optString(j)
                        if (relative.isBlank()) continue
                        if (!ExtensionRuntimePolicy.isSafeScript(directory, relative, MAX_SCRIPT_BYTES.toLong())) continue
                        val script = File(directory, relative)
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
    fun siteModeKey(id: String) = "extension_site_mode_$id"
    fun selectedSitesKey(id: String) = "extension_selected_sites_$id"
    fun oneTimeSitesKey(id: String) = "extension_one_time_sites_$id"

}
