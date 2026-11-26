package com.octopusworldpro.omega.browser

import android.app.Activity
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject

/** Narrow, consent-gated Chrome API adapter for the FBState popup. */
internal class ExtensionBrowserApiBridge(
    private val activity: Activity,
    private val extensionId: String,
    private val currentTabUrl: () -> String,
    private val updateCurrentTab: (String) -> Unit
) {
    private val prefs = activity.getSharedPreferences("omega_browser", Activity.MODE_PRIVATE)
    private fun allowed(permission: String): Boolean =
        prefs.getBoolean("extension_permission_${extensionId}_${permission}", false)
    private fun facebookUrl(url: String): Boolean = runCatching {
        val host = android.net.Uri.parse(url).host.orEmpty().lowercase()
        host == "facebook.com" || host.endsWith(".facebook.com")
    }.getOrDefault(false)
    private fun requireCookieAccess() {
        check(allowed("cookies")) { "cookies permission has not been approved" }
        check(prefs.getBoolean("extension_host_${extensionId}_facebook", false)) {
            "Facebook host access has not been approved"
        }
    }

    @JavascriptInterface fun cookiesGetAll(detailsJson: String?): String {
        requireCookieAccess()
        val details = runCatching { JSONObject(detailsJson ?: "{}") }.getOrDefault(JSONObject())
        val domain = details.optString("domain", "facebook.com").removePrefix(".").lowercase()
        require(domain == "facebook.com" || domain.endsWith(".facebook.com")) { "Only Facebook cookies are available" }
        val cm = CookieManager.getInstance()
        val result = JSONArray()
        val hosts = listOf("https://facebook.com", "https://www.facebook.com", "https://m.facebook.com")
        val seen = mutableSetOf<String>()
        hosts.forEach { url ->
            val raw = cm.getCookie(url).orEmpty()
            raw.split(";").forEach { pair ->
                val split = pair.trim().split("=", limit = 2)
                if (split.size == 2 && seen.add(split[0])) {
                    result.put(JSONObject().apply {
                        put("name", split[0]); put("key", split[0]); put("value", split[1])
                        put("domain", "facebook.com"); put("path", "/")
                        put("hostOnly", false); put("secure", true); put("httpOnly", false)
                        put("session", true)
                    })
                }
            }
        }
        return result.toString()
    }

    @JavascriptInterface fun cookiesSet(detailsJson: String): String {
        requireCookieAccess()
        val d = JSONObject(detailsJson)
        val name = d.optString("name", d.optString("key"))
        val value = d.optString("value")
        val url = d.optString("url", "https://facebook.com/")
        require(name.matches(Regex("[A-Za-z0-9_!#$%&'*+.^\u0060|~-]{1,256}"))) { "Invalid cookie name" }
        require(facebookUrl(url)) { "Only Facebook cookies can be set" }
        val domain = d.optString("domain", "facebook.com").removePrefix(".")
        require(domain == "facebook.com" || domain.endsWith(".facebook.com")) { "Invalid cookie domain" }
        val path = d.optString("path", "/").ifBlank { "/" }
        val secure = if (d.optBoolean("secure", true)) "; Secure" else ""
        val httpOnly = if (d.optBoolean("httpOnly", false)) "; HttpOnly" else ""
        val expires = if (d.has("expirationDate")) "; Max-Age=" + (d.optDouble("expirationDate") - System.currentTimeMillis()/1000.0).toLong().coerceAtLeast(0) else ""
        CookieManager.getInstance().setCookie(url, "$name=$value; Domain=.$domain; Path=$path$secure$httpOnly$expires")
        CookieManager.getInstance().flush()
        return JSONObject().put("name", name).put("value", value).put("domain", ".$domain").put("path", path).toString()
    }

    @JavascriptInterface fun cookiesRemove(detailsJson: String): String {
        requireCookieAccess()
        val d = JSONObject(detailsJson)
        val name = d.optString("name")
        val url = d.optString("url", "https://facebook.com/")
        require(name.isNotBlank() && facebookUrl(url)) { "Invalid Facebook cookie removal request" }
        CookieManager.getInstance().setCookie(url, "$name=; Max-Age=0; Path=/; Domain=.facebook.com; Secure")
        CookieManager.getInstance().flush()
        return JSONObject().put("name", name).put("url", url).toString()
    }

    @JavascriptInterface fun tabsQuery(queryJson: String?): String {
        check(allowed("tabs") || allowed("activeTab")) { "tabs permission has not been approved" }
        val url = currentTabUrl()
        return JSONArray().put(JSONObject().put("id", 1).put("active", true).put("url", url)).toString()
    }

    @JavascriptInterface fun tabsUpdate(tabId: Int, updateJson: String): String {
        check(allowed("tabs") || allowed("activeTab")) { "tabs permission has not been approved" }
        require(tabId == 1) { "Only the active browser tab is available" }
        val update = JSONObject(updateJson)
        val url = update.optString("url")
        if (url.isNotBlank()) {
            require(facebookUrl(url)) { "This extension may only navigate the active Facebook tab" }
            activity.runOnUiThread { updateCurrentTab(url) }
        }
        return JSONObject().put("id", 1).put("url", url.ifBlank { currentTabUrl() }).toString()
    }
}
