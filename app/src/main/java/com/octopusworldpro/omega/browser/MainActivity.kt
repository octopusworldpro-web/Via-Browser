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
    private var pendingImport = 0
    private var fbStateContent: String? = null
    private var pendingExportContent: String? = null
    private var searchEngine = "Google"
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
            settings.javaScriptEnabled = true
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
                    val endpoint = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=" + Uri.encode(query))
                    val connection = endpoint.openConnection() as HttpURLConnection
                    connection.connectTimeout = 2500; connection.readTimeout = 2500
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(json).optJSONArray(1)
                    if (array != null) for (i in 0 until array.length()) results.add(array.optString(i))
                    connection.disconnect()
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
            Triple("↓", "Downloads", { showDownloads() }), Triple("◉", "C3C FBState", { showFbState() }),
            Triple("f", "Facebook", { loadAddress("https://www.facebook.com/") }),
            Triple("◎", "Instagram", { loadAddress("https://www.instagram.com/") }),
            Triple("◉", "IG Cookie Check", { showInstagramCookieUtility() }),
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
        val options = arrayOf("New tab", "Tabs", "Bookmarks", "History", "Downloads", "C3C FBState Utility", "Device Identity Manager", "Settings", "About")
        AlertDialog.Builder(this).setTitle("OmegA Browser").setItems(options) { _, which ->
            when (which) {
                0 -> newTab(); 1 -> showTabs(); 2 -> showBookmarks(); 3 -> showHistory()
                4 -> showDownloads(); 5 -> showFbState(); 6 -> showIdentity(); 7 -> showSettings(); 8 -> showAbout()
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
                currentUrl = entries[which]
                if (which < tabs.size) tabs.removeAt(which)
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
            "JavaScript: ${if (web.settings.javaScriptEnabled) "On" else "Off"}",
            "Desktop site: ${if (web.settings.userAgentString.contains("X-Desktop")) "On" else "Off"}",
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
                1 -> { web.settings.javaScriptEnabled = !web.settings.javaScriptEnabled; showSettings() }
                2 -> {
                    val ua = web.settings.userAgentString
                    web.settings.userAgentString = if (ua.contains("X-Desktop")) ua.replace("X-Desktop", "Mobile") else ua.replace("Mobile", "X-Desktop")
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
    private fun showInstagramCookieUtility() {
        val input = EditText(this).apply {
            hint = "Paste a cookie header to check its format"
            setSingleLine(false)
            minLines = 3
            maxLines = 5
            textSize = 13f
            setTextColor(ink)
            setHintTextColor(Color.GRAY)
        }
        AlertDialog.Builder(this).setTitle("Instagram Cookie Check")
            .setMessage("Paste a cookie header you already have. Values are checked in memory only, masked in the result, and are not saved, copied, uploaded, or applied to a login session.")
            .setView(input)
            .setPositiveButton("Validate") { _, _ ->
                val raw = input.text.toString().trim()
                try {
                    if (raw.isBlank()) throw IllegalArgumentException("No cookie header provided")
                    val pairs = raw.split(";").map { part ->
                        val pair = part.trim().split("=", limit = 2)
                        if (pair.size != 2 || pair[0].isBlank() || pair[1].isBlank()) throw IllegalArgumentException("Invalid cookie pair")
                        pair[0].trim() to pair[1].trim()
                    }
                    val names = pairs.map { it.first }.distinct()
                    val hasSession = names.contains("sessionid")
                    val hasCsrf = names.contains("csrftoken")
                    val report = "Cookies found: ${names.size}\n" +
                        "sessionid: ${if (hasSession) "present (value hidden)" else "missing"}\n" +
                        "csrftoken: ${if (hasCsrf) "present (value hidden)" else "missing"}\n\n" +
                        "Cookie names:\n" + names.joinToString("\n")
                    AlertDialog.Builder(this).setTitle(if (hasSession && hasCsrf) "Cookie format looks valid" else "Required cookies missing")
                        .setMessage(report).setPositiveButton("OK", null).show()
                } catch (e: Exception) {
                    Toast.makeText(this, e.message ?: "Invalid cookie header", Toast.LENGTH_LONG).show()
                }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showFbState() {
        AlertDialog.Builder(this)
            .setTitle("C3C FBState Utility")
            .setMessage("Manual, consent-based and local-only. Session cookies are sensitive; values are masked and never uploaded.")
            .setPositiveButton("Continue") { _, _ -> showFbStateOptions() }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showFbStateOptions() {
        val options = if (fbStateContent == null) arrayOf("Import AppState JSON", "Import Cookie Header", "Import Netscape file")
            else arrayOf("Import AppState JSON", "Import Cookie Header", "Import Netscape file", "Export imported data")
        AlertDialog.Builder(this).setTitle("C3C FBState Utility")
            .setItems(options) { _, which ->
                if (which == 1) {
                    showCookieHeaderImport()
                } else if (which == 3) {
                    AlertDialog.Builder(this).setTitle("Export sensitive session data?")
                        .setMessage("The backup contains account session cookies. Store it only in a trusted private location.")
                        .setPositiveButton("Continue") { _, _ ->
                            pendingExportContent = fbStateContent
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE); type = "text/plain"
                                putExtra(Intent.EXTRA_TITLE, "omegA-fbstate-backup.txt")
                            }
                            startActivityForResult(intent, REQUEST_EXPORT)
                        }.setNegativeButton("Cancel", null).show()
                } else {
                    AlertDialog.Builder(this).setTitle("Import sensitive session data?")
                        .setMessage("You are choosing a local file. OmegA does not upload or auto-read browser cookies.")
                        .setPositiveButton("I understand") { _, _ ->
                            pendingImport = which
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }
                            startActivityForResult(intent, REQUEST_IMPORT)
                        }.setNegativeButton("Cancel", null).show()
                }
            }.setNegativeButton("Close", null).show()
    }
    private fun showCookieHeaderImport() {
        val editor = EditText(this).apply {
            hint = "c_user=...; xs=...; other_cookie=..."
            setTextColor(ink)
            setHintTextColor(Color.GRAY)
            textSize = 14f
            minLines = 3
            maxLines = 6
            setPadding(18, 12, 18, 12)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 4, 22, 0)
            addView(TextView(this@MainActivity).apply {
                text = "Paste a Cookie header you obtained yourself. Values stay in memory and are hidden in the preview."
                setTextColor(ink)
                textSize = 13f
            })
            addView(editor, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })
        }
        AlertDialog.Builder(this).setTitle("Import Cookie Header")
            .setView(container)
            .setPositiveButton("Validate") { _, _ ->
                try {
                    val raw = editor.text.toString().trim()
                    if (raw.isBlank() || raw.length > 1024 * 1024 || raw.contains('\n') || raw.contains('\r')) {
                        throw IllegalArgumentException("Cookie header is empty or too large")
                    }
                    val pairs = raw.split(";").map { part ->
                        val index = part.indexOf('=')
                        if (index <= 0) throw IllegalArgumentException("Invalid cookie pair")
                        val name = part.substring(0, index).trim()
                        val value = part.substring(index + 1).trim()
                        if (name.isBlank() || value.isBlank()) throw IllegalArgumentException("Cookie name/value is empty")
                        name to value
                    }
                    val names = pairs.map { it.first }.distinct()
                    if (!names.contains("c_user") || !names.contains("xs")) {
                        throw IllegalArgumentException("Required c_user and xs cookies are missing")
                    }
                    fbStateContent = raw
                    val preview = names.joinToString("\n") { "$it = ••••••••" }
                    AlertDialog.Builder(this).setTitle("FBState validated")
                        .setMessage("Cookie names: " + names.size + "\nRequired c_user + xs: present\n\n" + preview)
                        .setPositiveButton("OK", null).show()
                } catch (e: Exception) {
                    AlertDialog.Builder(this).setTitle("Cookie Header invalid")
                        .setMessage(e.message ?: "Could not validate Cookie header")
                        .setPositiveButton("OK", null).show()
                }
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
        if (requestCode == REQUEST_EXPORT) {
            val uri = if (resultCode == RESULT_OK) data?.data else null
            try {
                if (uri != null && pendingExportContent != null) contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingExportContent!!) }
                Toast.makeText(this, if (uri != null) "FBState backup exported locally" else "Export cancelled", Toast.LENGTH_LONG).show()
            } catch (_: Exception) { Toast.makeText(this, "Could not export backup", Toast.LENGTH_LONG).show() }
            pendingExportContent = null
            return
        }
        if (requestCode != REQUEST_IMPORT || resultCode != RESULT_OK || data?.data == null) return
        try {
            val source = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { reader ->\n                val text = reader.readText()\n                if (text.length > 1024 * 1024) throw IllegalArgumentException("Selected file exceeds 1 MB limit")\n                text\n            } ?: ""
            val names = when (pendingImport) {
                0 -> {
                    val rootJson = org.json.JSONTokener(source).nextValue()
                    val array = if (rootJson is org.json.JSONObject) rootJson.optJSONArray("cookies") else rootJson as? JSONArray
                    if (array == null) throw IllegalArgumentException("AppState must contain a cookie array")
                    (0 until array.length()).mapNotNull { i ->
                        val item = array.optJSONObject(i) ?: return@mapNotNull null
                        val name = item.optString("name", item.optString("key"))
                        val value = item.optString("value")
                        val domain = item.optString("domain", ".facebook.com").removePrefix(".")
                        if (name.isBlank() || value.isBlank()) throw IllegalArgumentException("Cookie name/value is empty")
                        if (!(domain == "facebook.com" || domain.endsWith(".facebook.com") || domain == "messenger.com" || domain.endsWith(".messenger.com"))) throw IllegalArgumentException("Unsupported cookie domain")
                        name
                    }
                }
                1 -> source.split(";").map {
                    val pair = it.trim().split("=", limit = 2)
                    if (pair.size != 2 || pair[0].isBlank() || pair[1].isBlank()) throw IllegalArgumentException("Invalid Cookie header")
                    pair[0].trim()
                }
                else -> source.lines().filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("#HttpOnly_") }.map {
                    val fields = it.split("\\t")
                    if (fields.size != 7) throw IllegalArgumentException("Invalid Netscape cookie row")
                    val domain = fields[0].removePrefix(".")
                    if (!(domain == "facebook.com" || domain.endsWith(".facebook.com") || domain == "messenger.com" || domain.endsWith(".messenger.com"))) throw IllegalArgumentException("Unsupported cookie domain")
                    if (fields[5].isBlank() || fields[6].isBlank()) throw IllegalArgumentException("Cookie name/value is empty")
                    fields[5]
                }
            }.distinct()
            val valid = names.contains("c_user") && names.contains("xs")
            if (!valid) throw IllegalArgumentException("Required c_user and xs cookies are missing")
            fbStateContent = source
            val preview = names.joinToString("\n") { "$it = ••••••••" }
            AlertDialog.Builder(this).setTitle(if (valid) "FBState validated" else "Validation incomplete")
                .setMessage("Cookie names: ${names.size}\nRequired c_user + xs: ${if (valid) "present" else "missing"}\n\n$preview")
                .setPositiveButton("OK", null).show()
        } catch (_: Exception) { Toast.makeText(this, "Could not read selected file.", Toast.LENGTH_LONG).show() }
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
    }

    private fun persistSavedData() {
        prefs.edit()
            .putString("bookmarks", JSONArray(bookmarks.distinct().takeLast(500)).toString())
            .putString("history", JSONArray(history.distinct().takeLast(500)).toString())
            .putString("tabs", JSONArray(tabs.takeLast(19)).toString())
            .putString("identity_ua", identityUserAgent)
            .putString("identity_lang", identityLanguage)
            .putString("search_engine", searchEngine)
            .apply()
    }

    private fun showAbout() {
        AlertDialog.Builder(this).setTitle("OmegA Browser").setMessage("Version 1.0.0\nCustomized by NirZon\nIndependent lightweight browser project.")
            .setPositiveButton("OK", null).show()
    }
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack() && web.visibility == View.VISIBLE) web.goBack() else showHome()
    }
    companion object { private const val REQUEST_IMPORT = 4310; private const val REQUEST_FILE = 4311; private const val REQUEST_EXPORT = 4312 }
}
