package com.octopusworldpro.omega.browser

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.util.Locale

/**
 * Small popup host for locally installed extensions.
 *
 * Only the extension's own popup origin receives the bridge. This deliberately
 * exposes runtime metadata and namespaced local storage, never cookies,
 * credentials, arbitrary native calls, or background execution.
 */
internal object ExtensionPopupHost {
    private const val HOST = "https://extension.local"

    fun show(
        activity: Activity,
        extensionDirectory: File,
        onFileChooser: (android.webkit.ValueCallback<Array<Uri>>, android.webkit.WebChromeClient.FileChooserParams?) -> Boolean
    ) {
        val manifestFile = File(extensionDirectory, "manifest.json")
        if (!manifestFile.isFile) return
        val manifest = runCatching { JSONObject(manifestFile.readText()) }.getOrNull() ?: return
        val action = manifest.optJSONObject("action") ?: manifest.optJSONObject("browser_action")
        val popup = action?.optString("default_popup").orEmpty()
        if (popup.isBlank()) {
            android.widget.Toast.makeText(activity, "This extension has no popup page", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val popupFile = safeFile(extensionDirectory, popup) ?: run {
            android.widget.Toast.makeText(activity, "Popup file is missing or unsafe", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val id = extensionDirectory.name
        val dialog = Dialog(activity)
        val density = activity.resources.displayMetrics.density
        val container = FrameLayout(activity).apply { setBackgroundColor(Color.WHITE) }
        val web = WebView(activity).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            addJavascriptInterface(StorageBridge(activity, id), "OmegaExtensionStorage")
            webViewClient = object : WebViewClient() {
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return true
                    return uri.host != "extension.local" || uri.scheme != "https" || !uri.path.orEmpty().startsWith("/$id/")
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    return uri.host != "extension.local" || uri.scheme != "https" || uri.path.orEmpty().startsWith("/$id/").not()
                }

                @Suppress("DEPRECATION")
                override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? =
                    intercept(extensionDirectory, popupFile, id, popup, manifest, runCatching { Uri.parse(url) }.getOrNull())

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
                    intercept(extensionDirectory, popupFile, id, popup, manifest, request?.url)
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView?,
                    filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (filePathCallback == null) return false
                    return onFileChooser(filePathCallback, fileChooserParams)
                }
            }
        }
        container.addView(web, FrameLayout.LayoutParams(-1, -1))
        val close = TextView(activity).apply {
            text = "×"
            textSize = 28f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setOnClickListener { dialog.dismiss() }
        }
        container.addView(close, FrameLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt(), Gravity.TOP or Gravity.END))
        dialog.setContentView(container)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * 0.92f).toInt(), (activity.resources.displayMetrics.heightPixels * 0.72f).toInt())
        web.loadUrl("$HOST/$id/$popup")
    }

    private fun intercept(
        extensionDirectory: File,
        popupFile: File,
        id: String,
        popup: String,
        manifest: JSONObject,
        uri: Uri?
    ): WebResourceResponse? {
        uri ?: return blocked()
        if (uri.scheme != "https" || uri.host != "extension.local") return null
        val requestPath = uri.path.orEmpty()
        if (!requestPath.startsWith("/$id/")) return blocked()
        val relative = requestPath.removePrefix("/$id/").ifBlank { popup }
        val file = safeFile(extensionDirectory, relative) ?: return blocked()
        if (!file.isFile || file.length() > 8L * 1024L * 1024L) return blocked()
        return try {
            var bytes = file.readBytes()
            val mime = mimeType(file.name)
            if (file.canonicalFile == popupFile.canonicalFile && mime == "text/html") {
                val html = bytes.toString(Charsets.UTF_8)
                val bootstrap = bootstrapScript(id, manifest)
                val injected = if (html.contains("<head", true)) {
                    html.replaceFirst(Regex("(?i)<head[^>]*>"), "$0<script>$bootstrap</script>")
                } else "<html><head><script>$bootstrap</script></head><body>$html</body></html>"
                bytes = injected.toByteArray(Charsets.UTF_8)
            }
            WebResourceResponse(mime, "UTF-8", ByteArrayInputStream(bytes))
        } catch (_: Exception) { blocked() }
    }

    private fun bootstrapScript(id: String, manifest: JSONObject): String {
        val manifestJson = JSONObject.quote(manifest.toString())
        val idJson = JSONObject.quote(id)
        return """
            (function(){
              const manifest = JSON.parse($manifestJson);
              const extensionId = $idJson;
              const storage = {
                get: function(keys, callback) {
                  const raw = OmegaExtensionStorage.get(keys == null ? null : JSON.stringify(keys));
                  let value = {}; try { value = JSON.parse(raw || '{}'); } catch (_) {}
                  if (typeof callback === 'function') setTimeout(() => callback(value), 0);
                  return Promise.resolve(value);
                },
                set: function(items, callback) {
                  OmegaExtensionStorage.set(JSON.stringify(items || {}));
                  if (typeof callback === 'function') setTimeout(callback, 0);
                  return Promise.resolve();
                },
                remove: function(keys, callback) {
                  OmegaExtensionStorage.remove(JSON.stringify(keys));
                  if (typeof callback === 'function') setTimeout(callback, 0);
                  return Promise.resolve();
                },
                clear: function(callback) {
                  OmegaExtensionStorage.clear();
                  if (typeof callback === 'function') setTimeout(callback, 0);
                  return Promise.resolve();
                }
              };
              window.chrome = window.chrome || {};
              window.chrome.runtime = {
                id: extensionId,
                getManifest: () => manifest,
                getURL: path => '$HOST/' + extensionId + '/' + String(path || '').replace(/^\/+/, '')
              };
              window.chrome.storage = window.chrome.storage || {};
              window.chrome.storage.local = storage;
              window.browser = window.browser || window.chrome;
            })();
        """.trimIndent()
    }

    private class StorageBridge(activity: Activity, extensionId: String) {
        private val prefs = activity.getSharedPreferences("omega_extension_storage_$extensionId", Activity.MODE_PRIVATE)

        @JavascriptInterface fun get(keysJson: String?): String {
            val all = prefs.all
            if (keysJson.isNullOrBlank() || keysJson == "null") return JSONObject(all).toString()
            return try {
                val result = JSONObject()
                when {
                    keysJson.startsWith("[") -> {
                        val array = org.json.JSONArray(keysJson)
                        for (i in 0 until array.length()) {
                            val key = array.optString(i)
                            if (all.containsKey(key)) result.put(key, all[key])
                        }
                    }
                    else -> {
                        val keys = JSONObject(keysJson)
                        val iterator = keys.keys()
                        while (iterator.hasNext()) {
                            val key = iterator.next()
                            if (all.containsKey(key)) result.put(key, all[key]) else if (!keys.isNull(key)) result.put(key, keys.get(key))
                        }
                    }
                }
                result.toString()
            } catch (_: Exception) { "{}" }
        }

        @JavascriptInterface fun set(itemsJson: String) {
            runCatching {
                val items = JSONObject(itemsJson)
                val edit = prefs.edit()
                val keys = items.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = items.get(key)
                    when (value) {
                        is String -> edit.putString(key, value)
                        is Boolean -> edit.putBoolean(key, value)
                        is Int -> edit.putInt(key, value)
                        is Long -> edit.putLong(key, value)
                        is Number -> edit.putFloat(key, value.toFloat())
                        else -> edit.putString(key, value.toString())
                    }
                }
                edit.apply()
            }
        }

        @JavascriptInterface fun remove(keysJson: String) {
            runCatching {
                val edit = prefs.edit()
                if (keysJson.startsWith("[")) {
                    val keys = org.json.JSONArray(keysJson)
                    for (i in 0 until keys.length()) edit.remove(keys.optString(i))
                } else edit.remove(keysJson.trim('"'))
                edit.apply()
            }
        }

        @JavascriptInterface fun clear() { prefs.edit().clear().apply() }
    }

    private fun safeFile(root: File, relative: String): File? {
        if (relative.isBlank() || relative.startsWith("/") || relative.contains('\\') ||
            relative.split('/').any { it.isBlank() || it == "." || it == ".." } || relative.contains(':')) return null
        val base = root.canonicalFile
        val file = File(base, relative).canonicalFile
        return file.takeIf { it.path.startsWith(base.path + File.separator) }
    }

    private fun mimeType(name: String): String = when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "html", "htm" -> "text/html"
        "js", "mjs" -> "application/javascript"
        "css" -> "text/css"
        "json" -> "application/json"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        else -> "application/octet-stream"
    }

    private fun blocked() = WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
}
