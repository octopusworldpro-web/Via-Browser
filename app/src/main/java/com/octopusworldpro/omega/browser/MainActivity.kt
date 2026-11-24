package com.octopusworldpro.omega.browser

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.text.Editable
import android.text.TextWatcher
import android.os.Handler
import android.os.Looper
import android.graphics.drawable.GradientDrawable
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

private class OmegaMarkView(context: Context, private val markColor: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = markColor
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 7f * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val path = Path().apply {
            moveTo(w * 0.08f, h * 0.82f)
            lineTo(w * 0.27f, h * 0.82f)
            lineTo(w * 0.27f, h * 0.63f)
            cubicTo(w * 0.27f, h * 0.27f, w * 0.37f, h * 0.10f, w * 0.50f, h * 0.10f)
            cubicTo(w * 0.63f, h * 0.10f, w * 0.73f, h * 0.27f, w * 0.73f, h * 0.63f)
            lineTo(w * 0.73f, h * 0.82f)
            lineTo(w * 0.92f, h * 0.82f)
        }
        canvas.drawPath(path, paint)
    }
}

class MainActivity : Activity() {
    private val bg = Color.rgb(9, 8, 23)
    private val panel = Color.rgb(25, 20, 43)
    private val accent = Color.rgb(168, 85, 247)
    private val ink = Color.rgb(239, 243, 255)
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var web: WebView
    private var contentView: View? = null
    private lateinit var progress: ProgressBar
    private lateinit var suggestionPanel: LinearLayout
    private val suggestionHandler = Handler(Looper.getMainLooper())
    private var suppressSuggestions = false
    private var suggestionTask: Runnable? = null
    private var suggestionTarget: EditText? = null
    private val tabs = mutableListOf<String>()
    private val history = mutableListOf<String>()
    private val bookmarks = mutableListOf<String>()
    private var currentUrl = "https://www.google.com"
    private var identityLanguage = ""
    private var identityUserAgent = ""
    private var searchEngine = "Google"
    private var javascriptEnabled = true
    private var desktopMode = false
    private lateinit var prefs: android.content.SharedPreferences
    private var filePathCallback: android.webkit.ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = bg
            window.navigationBarColor = bg
        }
        prefs = getSharedPreferences("omega_browser", Context.MODE_PRIVATE)
        loadSavedData()
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        buildShell()
        showHome()
    }

    private fun buildShell() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(14, 6, 14, 6); setBackgroundColor(bg) }
        top.addView(OmegaMarkView(this, accent), LinearLayout.LayoutParams(48, 54).apply {
            gravity = Gravity.CENTER
            marginEnd = (8 * resources.displayMetrics.density).toInt()
        })
        address = EditText(this).apply {
            hint = "Search or enter address"; textSize = 15f; setSingleLine(true)
            setTextColor(ink); setHintTextColor(Color.LTGRAY); setPadding(16, 8, 12, 8)
            setBackgroundColor(panel); imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { loadAddress(text.toString()); true } else false
            }
        }
        top.addView(address, LinearLayout.LayoutParams(0, 70, 1f).apply {
            marginEnd = (10 * resources.displayMetrics.density).toInt()
        })
        top.addView(button("⋮") { showMainMenu() }, LinearLayout.LayoutParams(38, 70))
        root.addView(top)
        suggestionPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 4, 12, 8); setBackgroundColor(panel); visibility = View.GONE }
        root.addView(suggestionPanel, LinearLayout.LayoutParams(-1, -2))
        address.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { if (!suppressSuggestions) requestSuggestions(s?.toString().orEmpty(), address) }
            override fun afterTextChanged(s: Editable?) {}
        })
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; if (Build.VERSION.SDK_INT >= 21) progressTintList = android.content.res.ColorStateList.valueOf(accent); visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, 3))
        web = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = javascriptEnabled
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            // Keep web content isolated from local files and block mixed-content downgrades.
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            if (Build.VERSION.SDK_INT >= 21) {
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
            if (Build.VERSION.SDK_INT >= 26) {
                settings.safeBrowsingEnabled = true
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    if (url != null) { currentUrl = url; suppressSuggestions = true; address.setText(url); suppressSuggestions = false }
                    this@MainActivity.progress.visibility = View.VISIBLE
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    this@MainActivity.progress.visibility = View.GONE
                    if (url != null && !url.startsWith("about:")) {
                        currentUrl = url
                        if (history.lastOrNull() != url) { history.add(url); persistSavedData() }
                    }
                }
            }
            if (identityUserAgent.isNotEmpty()) settings.userAgentString = identityUserAgent
            if (desktopMode) settings.userAgentString = desktopUserAgent()
            settings.allowFileAccess = false
            settings.allowContentAccess = true
            if (Build.VERSION.SDK_INT >= 16) {
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
            }
            settings.javaScriptCanOpenWindowsAutomatically = false
            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                try {
                    val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimeType)
                        addRequestHeader("User-Agent", userAgent)
                        setTitle(fileName)
                        setDescription("OmegA Browser download")
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    }
                    (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                    Toast.makeText(this@MainActivity, "Download started", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this@MainActivity, "Download could not start", Toast.LENGTH_LONG).show()
                }
            })
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(view: WebView?, callback: android.webkit.ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback
                    return try {
                        startActivityForResult(params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }, REQUEST_FILE)
                        true
                    } catch (_: Exception) {
                        filePathCallback = null
                        false
                    }
                }
                override fun onProgressChanged(view: WebView?, newProgress: Int) { this@MainActivity.progress.progress = newProgress }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(4, 6, 4, 8); setBackgroundColor(panel) }
        bottom.addView(button("‹") { if (web.canGoBack()) web.goBack() }, weight())
        bottom.addView(button("›") { if (web.canGoForward()) web.goForward() }, weight())
        bottom.addView(button("⌂") { showHome() }, weight())
        bottom.addView(button("＋") { newTab() }, weight())
        bottom.addView(button("▢") { showTabs() }, weight())
        root.addView(bottom)
        root.isFocusableInTouchMode = true
        setContentView(root)
        root.requestFocus()
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = radiusDp * resources.displayMetrics.density
        setStroke((1 * resources.displayMetrics.density).toInt().coerceAtLeast(1), Color.argb(35, 170, 150, 255))
    }

    private fun requestSuggestions(raw: String, source: EditText = address) {
        suggestionTarget = source
        suggestionTask?.let { suggestionHandler.removeCallbacks(it) }
        val query = raw.trim()
        if (query.length < 2 || query.startsWith("http://") || query.startsWith("https://")) {
            suggestionPanel.visibility = View.GONE
            return
        }
        suggestionTask = Runnable {
            Thread {
                val results = mutableListOf<String>()
                try {
                    val encoded = Uri.encode(query)
                    val endpoint = when (searchEngine) {
                        "Bing" -> URL("https://api.bing.com/osjson.aspx?query=$encoded")
                        "DuckDuckGo" -> URL("https://duckduckgo.com/ac/?q=$encoded")
                        else -> URL("https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded")
                    }
                    val connection = endpoint.openConnection() as HttpURLConnection
                    try {
                        connection.connectTimeout = 2500
                        connection.readTimeout = 2500
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                        val json = connection.inputStream.bufferedReader().use { it.readText() }
                        if (searchEngine == "DuckDuckGo") {
                            val array = JSONArray(json)
                            for (i in 0 until array.length()) {
                                val phrase = array.optJSONObject(i)?.optString("phrase").orEmpty()
                                if (phrase.isNotBlank()) results.add(phrase)
                            }
                        } else {
                            val array = JSONArray(json).optJSONArray(1)
                            if (array != null) for (i in 0 until array.length()) results.add(array.optString(i))
                        }
                    } finally {
                        connection.disconnect()
                    }
                } catch (_: Exception) {}
                runOnUiThread {
                    if (suggestionTarget?.text?.toString()?.trim() != query || results.isEmpty()) return@runOnUiThread
                    suggestionPanel.removeAllViews()
                    results.take(6).forEach { value ->
                        val row = TextView(this).apply {
                            text = "⌕   $value"; textSize = 14f; setTextColor(ink)
                            setPadding(12, 11, 12, 11); isClickable = true
                            setOnClickListener { loadAddress(value) }
                        }
                        suggestionPanel.addView(row, LinearLayout.LayoutParams(-1, -2))
                        suggestionPanel.addView(View(this).apply { setBackgroundColor(Color.argb(30, 210, 205, 240)) }, LinearLayout.LayoutParams(-1, 1))
                    }
                    suggestionPanel.visibility = View.VISIBLE
                }
            }.start()
        }
        suggestionHandler.postDelayed(suggestionTask!!, 280)
    }

    private fun weight() = LinearLayout.LayoutParams(0, 48, 1f)
    private fun button(label: String, action: () -> Unit): TextView = TextView(this).apply {
        text = label; textSize = 22f; gravity = Gravity.CENTER; setTextColor(ink); setOnClickListener { action() }
    }

    private fun loadAddress(raw: String) {
        var value = raw.trim()
        if (value.isEmpty()) return
        val looksLikeUrl = value.contains(".") && !value.contains(" ")
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = if (looksLikeUrl) "https://$value" else searchUrl(value)
        }
        showBrowser()
        suppressSuggestions = true
        address.setText(value)
        suppressSuggestions = false
        suggestionPanel.visibility = View.GONE
        if (identityLanguage.isNotBlank()) web.loadUrl(value, mapOf("Accept-Language" to identityLanguage))
        else web.loadUrl(value)
    }

    private fun showHome() {
        web.setVisibility(View.GONE)
        suppressSuggestions = true
        address.setText("")
        suppressSuggestions = false
        suggestionPanel.visibility = View.GONE
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(18, 20, 18, 20); setBackgroundColor(bg) }
        page.addView(OmegaMarkView(this, accent).apply {
            translationY = (-8 * resources.displayMetrics.density)
        }, LinearLayout.LayoutParams(104, 78).apply { gravity = Gravity.CENTER_HORIZONTAL })
        page.addView(TextView(this).apply {
            text = "OmegA Browser"; textSize = 32f; setTextColor(ink); gravity = Gravity.CENTER
            includeFontPadding = true
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 4 })
        page.addView(TextView(this).apply {
            text = "Fast • Private • Lightweight"; textSize = 16f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
            includeFontPadding = true
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 2 })
        val search = EditText(this).apply {
            hint = "Search or enter website"; setSingleLine(true); textSize = 16f; setTextColor(ink); setHintTextColor(Color.LTGRAY)
            setPadding(18, 8, 18, 8); background = rounded(panel, 18); imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { loadAddress(text.toString()); true } else false }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { requestSuggestions(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        val searchParams = LinearLayout.LayoutParams(-1, 70); searchParams.topMargin = 34; page.addView(search, searchParams)
        val grid = GridLayout(this).apply { columnCount = 3; useDefaultMargins = false; alignmentMode = GridLayout.ALIGN_BOUNDS }
        val shortcuts = listOf(
            Triple("▣", "Bookmarks", { showBookmarks() }), Triple("◷", "History", { showHistory() }),
            Triple("↓", "Downloads", { showDownloads() }), Triple("🧩", "Extensions", { showExtensions() }),
            Triple("f", "Facebook", { loadAddress("https://www.facebook.com/") }),
            Triple("◎", "Instagram", { loadAddress("https://www.instagram.com/") }),
            Triple("⌁", "Device Identity", { showIdentity() }), Triple("⚙", "Settings", { showSettings() })
        )
        shortcuts.forEach { item ->
            val tile = TextView(this).apply {
                text = "${item.first}\n${item.second}"
                textSize = 16f
                setLineSpacing(3f, 1f)
                gravity = Gravity.CENTER
                setTextColor(ink)
                includeFontPadding = true
                maxLines = 2
                background = rounded(Color.argb(42, 168, 85, 247), 20)
                setPadding(8, 12, 8, 12)
                isClickable = true
                isFocusable = true
                setOnClickListener { item.third() }
            }
            val lp = GridLayout.LayoutParams().apply { width = 0; height = 140; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(5, 7, 5, 7) }
            grid.addView(tile, lp)
        }
        val gridParams = LinearLayout.LayoutParams(-1, -2); gridParams.topMargin = 20; page.addView(grid, gridParams)
        page.addView(TextView(this).apply { text = "Customized by NirZon"; textSize = 11f; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, 22, 0, 0) })
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(page)
        }
        replaceWebWith(scroll)
    }

    private fun replaceWebWith(view: View) {
        val current = contentView ?: web
        val existingIndex = root.indexOfChild(current)
        val index = if (existingIndex >= 0) existingIndex else root.indexOfChild(progress) + 1
        if (existingIndex >= 0) root.removeViewAt(existingIndex)
        contentView = view
        root.addView(view, index.coerceAtLeast(3).coerceAtMost(root.childCount), LinearLayout.LayoutParams(-1, 0, 1f))
    }
    private fun showBrowser() {
        val current = contentView
        if (current !== web) {
            val oldIndex = current?.let { root.indexOfChild(it) } ?: -1
            val contentIndex = if (oldIndex >= 0) oldIndex else root.indexOfChild(progress) + 1
            if (oldIndex >= 0) root.removeViewAt(oldIndex)
            contentView = web
            root.addView(web, contentIndex.coerceAtLeast(3).coerceAtMost(root.childCount), LinearLayout.LayoutParams(-1, 0, 1f))
        }
        web.visibility = View.VISIBLE
        suggestionPanel.visibility = View.GONE
    }

    private fun showMainMenu() {
        val options = arrayOf("New tab", "Tabs", "Bookmarks", "History", "Downloads", "Extensions", "Device Identity Manager", "Settings", "About")
        AlertDialog.Builder(this).setTitle("OmegA Browser").setItems(options) { _, which ->
            when (which) {
                0 -> newTab(); 1 -> showTabs(); 2 -> showBookmarks(); 3 -> showHistory()
                4 -> showDownloads(); 5 -> showExtensions(); 6 -> showIdentity(); 7 -> showSettings(); 8 -> showAbout()
            }
        }.show()
    }
    private fun newTab() {
        if (tabs.size >= 19) {
            Toast.makeText(this, "Maximum 20 tabs reached", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUrl.isNotBlank()) tabs.add(currentUrl)
        currentUrl = "https://www.google.com"
        persistSavedData()
        suppressSuggestions = true
        address.setText("")
        suppressSuggestions = false
        showBrowser()
        web.loadUrl(currentUrl)
    }
    private fun showTabs() {
        val entries = (tabs + currentUrl).toTypedArray()
        AlertDialog.Builder(this).setTitle("Tabs (${entries.size}/20)")
            .setItems(entries) { _, which ->
                if (which < tabs.size) {
                    val selected = tabs[which]
                    tabs[which] = currentUrl
                    currentUrl = selected
                } else {
                    currentUrl = entries[which]
                }
                persistSavedData()
                showBrowser()
                web.loadUrl(currentUrl)
            }
            .setPositiveButton("New tab") { _, _ -> newTab() }
            .setNeutralButton("Manage") { _, _ -> manageTabs() }
            .setNegativeButton("Close", null).show()
    }

    private fun manageTabs() {
        val entries = tabs.toTypedArray()
        if (entries.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Manage tabs").setMessage("No background tabs.")
                .setPositiveButton("OK", null).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Close a background tab")
            .setItems(entries) { _, which ->
                tabs.removeAt(which)
                persistSavedData()
                manageTabs()
            }
            .setNeutralButton("Close all background tabs") { _, _ ->
                tabs.clear()
                persistSavedData()
                Toast.makeText(this, "Background tabs closed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Done", null).show()
    }
    private fun showBookmarks() = showList("Bookmarks", bookmarks, true)
    private fun showHistory() = showList("History", history, false)
    private fun showList(title: String, items: MutableList<String>, allowAdd: Boolean) {
        val values = items.distinct().toTypedArray()
        val builder = AlertDialog.Builder(this).setTitle(title)
        if (values.isEmpty()) {
            builder.setMessage("Nothing here yet.")
        } else {
            builder.setItems(values) { _, which -> loadAddress(values[which]) }
                .setNeutralButton("Manage") { _, _ -> showManageList(title, items, allowAdd) }
        }
        if (allowAdd) builder.setPositiveButton("Bookmark current page") { _, _ ->
            if (currentUrl.isNotBlank() && !bookmarks.contains(currentUrl)) {
                bookmarks.add(currentUrl); persistSavedData()
                Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Close", null).show()
    }

    private fun showManageList(title: String, items: MutableList<String>, bookmarksList: Boolean) {
        val values = items.distinct().toTypedArray()
        if (values.isEmpty()) { showList(title, items, bookmarksList); return }
        AlertDialog.Builder(this).setTitle("Manage $title")
            .setItems(values) { _, which ->
                val selected = values[which]
                AlertDialog.Builder(this).setTitle(if (bookmarksList) "Bookmark" else "History entry")
                    .setMessage(selected)
                    .setPositiveButton("Open") { _, _ -> loadAddress(selected) }
                    .setNegativeButton("Remove") { _, _ ->
                        items.remove(selected); persistSavedData()
                        showManageList(title, items, bookmarksList)
                    }.setNeutralButton("Done", null).show()
            }
            .setNeutralButton("Clear all") { _, _ ->
                AlertDialog.Builder(this).setTitle("Clear $title?")
                    .setPositiveButton("Clear") { _, _ -> items.clear(); persistSavedData(); Toast.makeText(this, "$title cleared", Toast.LENGTH_SHORT).show() }
                    .setNegativeButton("Cancel", null).show()
            }.setNegativeButton("Close", null).show()
    }
    private fun showDownloads() {
        try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) }
        catch (_: Exception) { AlertDialog.Builder(this).setTitle("Downloads").setMessage("Open the Android Downloads app to view saved files.").setPositiveButton("OK", null).show() }
    }
    private fun desktopUserAgent(): String =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private fun searchUrl(query: String): String {
        val encoded = Uri.encode(query)
        return when (searchEngine) {
            "Bing" -> "https://www.bing.com/search?q=$encoded"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    private fun showSettings() {
        val items = arrayOf(
            "Search engine: $searchEngine",
            "JavaScript: ${if (javascriptEnabled) "On" else "Off"}",
            "Desktop site: ${if (desktopMode) "On" else "Off"}",
            "Clear browsing data",
            "About"
        )
        AlertDialog.Builder(this).setTitle("Settings").setItems(items) { _, which ->
            when (which) {
                0 -> AlertDialog.Builder(this).setTitle("Search engine")
                    .setItems(arrayOf("Google", "Bing", "DuckDuckGo")) { _, choice ->
                        searchEngine = arrayOf("Google", "Bing", "DuckDuckGo")[choice]
                        persistSavedData()
                        Toast.makeText(this, "Search engine: $searchEngine", Toast.LENGTH_SHORT).show()
                    }.show()
                1 -> { javascriptEnabled = !javascriptEnabled; web.settings.javaScriptEnabled = javascriptEnabled; persistSavedData(); showSettings() }
                2 -> {
                    desktopMode = !desktopMode
                    web.settings.userAgentString = if (desktopMode) desktopUserAgent() else identityUserAgent.ifBlank { android.webkit.WebSettings.getDefaultUserAgent(this) }
                    persistSavedData()
                    web.reload()
                    showSettings()
                }
                3 -> AlertDialog.Builder(this).setTitle("Clear browsing data?")
                    .setMessage("Clear local history, bookmarks, cookies and cached website files?")
                    .setPositiveButton("Clear") { _, _ ->
                        history.clear(); bookmarks.clear(); persistSavedData()
                        web.clearCache(true); web.clearHistory()
                        android.webkit.CookieManager.getInstance().removeAllCookies(null)
                        android.webkit.CookieManager.getInstance().flush()
                        Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show()
                    }.setNegativeButton("Cancel", null).show()
                4 -> showAbout()
            }
        }.setNegativeButton("Done", null).show()
    }

    private fun showIdentity() {
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        val ua = EditText(this).apply { hint = "Custom User-Agent (optional)"; setText(identityUserAgent) }
        val lang = EditText(this).apply { hint = "Accept-Language (e.g. en-US)"; setText(identityLanguage) }
        fields.addView(ua); fields.addView(lang)
        AlertDialog.Builder(this).setTitle("Device Identity Manager")
            .setMessage("Only browser-exposed values are configurable. Hardware IDs are never accessed.")
            .setView(fields).setPositiveButton("Save") { _, _ ->
                identityUserAgent = ua.text.toString().trim(); identityLanguage = lang.text.toString().trim()
                if (identityUserAgent.isNotEmpty()) web.settings.userAgentString = identityUserAgent
                persistSavedData()
                Toast.makeText(this, "Profile saved for browser session.", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }
    @Deprecated("Legacy activity result retained for API 19 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE) {
            val result = if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
            return
        }
        if (requestCode == REQUEST_EXTENSION_ZIP && resultCode == RESULT_OK && data?.data != null) {
            installExtensionZip(data.data!!)
        }
    }

    private fun showExtensions() {
        val base = File(filesDir, "extensions")
        if (!base.exists()) base.mkdirs()
        val installed = base.listFiles()?.filter { it.isDirectory && File(it, "manifest.json").isFile } ?: emptyList()
        val actions = arrayOf("Install extension from ZIP", "Manage installed extensions (${installed.size})")
        AlertDialog.Builder(this).setTitle("Extension Manager")
            .setItems(actions) { _, which ->
                if (which == 0) {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/zip"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    }
                    startActivityForResult(intent, REQUEST_EXTENSION_ZIP)
                } else showInstalledExtensions(installed)
            }.setNegativeButton("Close", null).show()
    }

    private fun showInstalledExtensions(installed: List<File>) {
        if (installed.isEmpty()) {
            AlertDialog.Builder(this).setTitle("Installed extensions")
                .setMessage("No extensions installed yet. Choose Install extension from ZIP to add one.")
                .setPositiveButton("OK", null).show()
            return
        }
        val labels = installed.map { dir ->
            try {
                val manifest = org.json.JSONObject(File(dir, "manifest.json").readText())
                "${manifest.optString("name", dir.name)} • v${manifest.optString("version", "?")}"
            } catch (_: Exception) { dir.name }
        }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Installed extensions")
            .setItems(labels) { _, which ->
                val dir = installed[which]
                AlertDialog.Builder(this).setTitle(labels[which])
                    .setMessage("Extension package is stored locally. This build supports ZIP installation and management; Chrome extension scripts/APIs are not executed.")
                    .setPositiveButton("Remove") { _, _ ->
                        dir.deleteRecursively()
                        Toast.makeText(this, "Extension removed", Toast.LENGTH_SHORT).show()
                        showExtensions()
                    }.setNegativeButton("Back") { _, _ -> showExtensions() }.show()
            }.setNegativeButton("Close", null).show()
    }

    private fun installExtensionZip(uri: Uri) {
        val base = File(filesDir, "extensions")
        if (!base.exists()) base.mkdirs()
        val staging = File(base, "install-${System.currentTimeMillis()}")
        try {
            staging.mkdirs()
            var totalBytes = 0L
            var fileCount = 0
            contentResolver.openInputStream(uri)?.use { input ->
                java.util.zip.ZipInputStream(java.io.BufferedInputStream(input)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        fileCount++
                        if (fileCount > 2000) throw IllegalArgumentException("ZIP contains too many files")
                        val name = entry.name.replace('\\', '/')
                        if (name.startsWith("/") || name.split('/').any { it == ".." } || name.contains(":")) {
                            throw IllegalArgumentException("Unsafe ZIP entry path")
                        }
                        if (name.isNotBlank()) {
                            val target = File(staging, name)
                            if (!target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                                throw IllegalArgumentException("Unsafe ZIP path")
                            }
                            if (entry.isDirectory) target.mkdirs() else {
                                target.parentFile?.mkdirs()
                                target.outputStream().use { output ->
                                    val buffer = ByteArray(8192)
                                    while (true) {
                                        val count = zip.read(buffer)
                                        if (count < 0) break
                                        totalBytes += count
                                        if (totalBytes > 20L * 1024L * 1024L) throw IllegalArgumentException("Extension ZIP exceeds 20 MB")
                                        output.write(buffer, 0, count)
                                    }
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: throw IllegalArgumentException("Could not open selected ZIP")
            var manifestFile = File(staging, "manifest.json")
            if (!manifestFile.isFile) {
                val nested = staging.listFiles()?.filter { it.isDirectory }?.singleOrNull()
                if (nested != null && File(nested, "manifest.json").isFile) {
                    nested.listFiles()?.forEach { it.renameTo(File(staging, it.name)) }
                    nested.delete()
                }
            }
            manifestFile = File(staging, "manifest.json")
            if (!manifestFile.isFile) throw IllegalArgumentException("manifest.json not found in ZIP")
            val manifest = org.json.JSONObject(manifestFile.readText())
            val manifestVersion = manifest.optInt("manifest_version", 0)
            if (manifestVersion !in 2..3) throw IllegalArgumentException("Only Manifest V2/V3 packages are recognized")
            val name = manifest.optString("name").trim()
            val version = manifest.optString("version").trim()
            if (name.isBlank() || version.isBlank()) throw IllegalArgumentException("Manifest name/version is missing")
            val id = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(48).ifBlank { "extension" }
            val destination = File(base, id)
            if (destination.exists()) destination.deleteRecursively()
            if (!staging.renameTo(destination)) throw IllegalStateException("Could not finalize extension install")
            AlertDialog.Builder(this).setTitle("Extension installed")
                .setMessage("$name v$version package files were installed. Extension execution is not yet enabled in this build.")
                .setPositiveButton("OK", null).show()
        } catch (e: Exception) {
            staging.deleteRecursively()
            AlertDialog.Builder(this).setTitle("Extension install failed")
                .setMessage(e.message ?: "Invalid or unsupported extension ZIP")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun loadSavedData() {
        fun readList(key: String): MutableList<String> {
            return try {
                val a = JSONArray(prefs.getString(key, "[]") ?: "[]")
                MutableList(a.length()) { i -> a.getString(i) }
            } catch (_: Exception) { mutableListOf() }
        }
        bookmarks.addAll(readList("bookmarks"))
        history.addAll(readList("history"))
        tabs.addAll(readList("tabs").take(19))
        identityUserAgent = prefs.getString("identity_ua", "") ?: ""
        identityLanguage = prefs.getString("identity_lang", "") ?: ""
        searchEngine = prefs.getString("search_engine", "Google") ?: "Google"
        javascriptEnabled = prefs.getBoolean("javascript_enabled", true)
        desktopMode = prefs.getBoolean("desktop_mode", false)
    }

    private fun persistSavedData() {
        prefs.edit()
            .putString("bookmarks", JSONArray(bookmarks.distinct().takeLast(500)).toString())
            .putString("history", JSONArray(history.distinct().takeLast(500)).toString())
            .putString("tabs", JSONArray(tabs.takeLast(19)).toString())
            .putString("identity_ua", identityUserAgent)
            .putString("identity_lang", identityLanguage)
            .putString("search_engine", searchEngine)
            .putBoolean("javascript_enabled", javascriptEnabled)
            .putBoolean("desktop_mode", desktopMode)
            .apply()
    }

    private fun showAbout() {
        AlertDialog.Builder(this).setTitle("OmegA Browser").setMessage("Version 1.0.0\nCustomized by NirZon\nIndependent lightweight browser project.")
            .setPositiveButton("OK", null).show()
    }
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack() && web.visibility == View.VISIBLE) web.goBack() else showHome()
    }
    companion object { private const val REQUEST_FILE = 4311; private const val REQUEST_EXTENSION_ZIP = 4313 }
}
