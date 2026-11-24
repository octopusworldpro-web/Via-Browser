package com.octopusworldpro.omega.browser

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import android.os.Handler
import android.os.Looper
import android.graphics.drawable.GradientDrawable
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private val bg = Color.rgb(12, 16, 28)
    private val panel = Color.rgb(24, 31, 48)
    private val accent = Color.rgb(110, 140, 255)
    private val ink = Color.rgb(239, 243, 255)
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var suggestionPanel: LinearLayout
    private val suggestionHandler = Handler(Looper.getMainLooper())
    private var suppressSuggestions = false
    private var suggestionTask: Runnable? = null
    private val tabs = mutableListOf<String>()
    private val history = mutableListOf<String>()
    private val bookmarks = mutableListOf<String>()
    private var currentUrl = "https://www.google.com"
    private var identityLanguage = ""
    private var identityUserAgent = ""
    private var pendingImport = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = bg
            window.navigationBarColor = bg
        }
        buildShell()
        showHome()
    }

    private fun buildShell() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(14, 12, 14, 8); setBackgroundColor(bg) }
        top.addView(TextView(this).apply { text = "Ω"; textSize = 25f; setTextColor(accent); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(38, 48))
        address = EditText(this).apply {
            hint = "Search or enter address"; textSize = 15f; setSingleLine(true)
            setTextColor(ink); setHintTextColor(Color.LTGRAY); setPadding(16, 4, 12, 4)
            setBackgroundColor(panel); imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { loadAddress(text.toString()); true } else false
            }
        }
        top.addView(address, LinearLayout.LayoutParams(0, 48, 1f))
        top.addView(button("⋮") { showMainMenu() }, LinearLayout.LayoutParams(42, 48))
        root.addView(top)
        suggestionPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 4, 12, 8); setBackgroundColor(panel); visibility = View.GONE }
        root.addView(suggestionPanel, LinearLayout.LayoutParams(-1, -2))
        address.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { if (!suppressSuggestions) requestSuggestions(s?.toString().orEmpty()) }
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
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    if (url != null) { currentUrl = url; address.setText(url) }
                    this@MainActivity.progress.visibility = View.VISIBLE
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    this@MainActivity.progress.visibility = View.GONE
                    if (url != null && !url.startsWith("about:")) {
                        currentUrl = url
                        if (history.lastOrNull() != url) history.add(url)
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
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
        setContentView(root)
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = radiusDp * resources.displayMetrics.density
        setStroke((1 * resources.displayMetrics.density).toInt().coerceAtLeast(1), Color.argb(35, 170, 150, 255))
    }

    private fun requestSuggestions(raw: String) {
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
                    if (address.text.toString().trim() != query || results.isEmpty()) return@runOnUiThread
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
            value = if (looksLikeUrl) "https://$value" else "https://www.google.com/search?q=" + Uri.encode(value)
        }
        showBrowser()
        suppressSuggestions = true
        address.setText(value)
        suppressSuggestions = false
        suggestionPanel.visibility = View.GONE
        web.loadUrl(value)
    }

    private fun showHome() {
        web.setVisibility(View.GONE)
        suppressSuggestions = true
        address.setText("")
        suppressSuggestions = false
        suggestionPanel.visibility = View.GONE
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(18, 22, 18, 18); setBackgroundColor(bg) }
        page.addView(TextView(this).apply { text = "Ω"; textSize = 64f; setTextColor(accent); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, 88))
        page.addView(TextView(this).apply { text = "OmegA Browser"; textSize = 25f; setTextColor(ink); gravity = Gravity.CENTER })
        page.addView(TextView(this).apply { text = "Fast • Private • Lightweight"; textSize = 13f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER })
        val search = EditText(this).apply {
            hint = "Search or enter website"; setSingleLine(true); textSize = 16f; setTextColor(ink); setHintTextColor(Color.LTGRAY)
            setPadding(18, 8, 18, 8); background = rounded(panel, 18); imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_GO) { loadAddress(text.toString()); true } else false }
        }
        val searchParams = LinearLayout.LayoutParams(-1, 54); searchParams.topMargin = 28; page.addView(search, searchParams)
        val grid = GridLayout(this).apply { columnCount = 3; rowCount = 2; useDefaultMargins = false }
        val shortcuts = listOf(
            Triple("▣", "Bookmarks", { showBookmarks() }), Triple("◷", "History", { showHistory() }),
            Triple("↓", "Downloads", { showDownloads() }), Triple("◉", "C3C FBState", { showFbState() }),
            Triple("⌁", "Device Identity", { showIdentity() }), Triple("⚙", "Settings", { showSettings() })
        )
        shortcuts.forEach { item ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(4, 12, 4, 10)
                background = rounded(panel, 16); isClickable = true; isFocusable = true
                addView(TextView(this@MainActivity).apply { text = item.first; textSize = 25f; gravity = Gravity.CENTER; setTextColor(accent) }, LinearLayout.LayoutParams(-1, 34))
                addView(TextView(this@MainActivity).apply { text = item.second; textSize = 11f; gravity = Gravity.CENTER; setTextColor(ink); maxLines = 1 }, LinearLayout.LayoutParams(-1, 25))
                setOnClickListener { item.third() }
            }
            val lp = GridLayout.LayoutParams().apply { width = 0; height = 78; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(4, 5, 4, 5) }
            grid.addView(tile, lp)
        }
        val gridParams = LinearLayout.LayoutParams(-1, -2); gridParams.topMargin = 16; page.addView(grid, gridParams)
        page.addView(TextView(this).apply { text = "Customized by NirZon"; textSize = 11f; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, 22, 0, 0) })
        replaceWebWith(page)
    }

    private fun replaceWebWith(view: View) {
        val index = root.indexOfChild(web)
        if (index >= 0) root.removeViewAt(index)
        root.addView(view, index.coerceAtLeast(3), LinearLayout.LayoutParams(-1, 0, 1f))
    }
    private fun showBrowser() {
        val index = root.indexOfChild(web)
        if (index < 0) {
            val contentIndex = root.indexOfChild(progress) + 1
            if (contentIndex < root.childCount - 1) root.removeViewAt(contentIndex)
            root.addView(web, contentIndex, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        web.setVisibility(View.VISIBLE)
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
    private fun newTab() { tabs.add(currentUrl); currentUrl = "https://www.google.com"; address.setText(""); showBrowser(); web.loadUrl(currentUrl) }
    private fun showTabs() {
        val entries = (tabs + currentUrl).toTypedArray()
        AlertDialog.Builder(this).setTitle("Tabs (${entries.size})").setItems(entries) { _, which -> currentUrl = entries[which]; showBrowser(); web.loadUrl(currentUrl) }
            .setPositiveButton("New tab") { _, _ -> newTab() }.setNegativeButton("Close", null).show()
    }
    private fun showBookmarks() = showList("Bookmarks", bookmarks, true)
    private fun showHistory() = showList("History", history, false)
    private fun showList(title: String, items: MutableList<String>, allowAdd: Boolean) {
        val values = items.distinct().toTypedArray()
        val builder = AlertDialog.Builder(this).setTitle(title)
        if (values.isEmpty()) builder.setMessage("Nothing here yet.") else builder.setItems(values) { _, which -> loadAddress(values[which]) }
        if (allowAdd) builder.setPositiveButton("Bookmark current page") { _, _ -> if (!bookmarks.contains(currentUrl)) bookmarks.add(currentUrl) }
        builder.setNegativeButton("Close", null).show()
    }
    private fun showDownloads() {
        AlertDialog.Builder(this).setTitle("Downloads").setMessage("Download tracking and file handling are not yet connected.")
            .setPositiveButton("OK", null).show()
    }
    private fun showSettings() {
        val items = arrayOf("Appearance: Dark", "Search engine: Google", "JavaScript: Enabled", "Clear browsing data", "About")
        AlertDialog.Builder(this).setTitle("Settings").setItems(items) { _, which ->
            when (which) {
                3 -> AlertDialog.Builder(this).setTitle("Clear browsing data?").setMessage("This clears local history and WebView cache.")
                    .setPositiveButton("Clear") { _, _ -> history.clear(); web.clearCache(true); web.clearHistory() }.setNegativeButton("Cancel", null).show()
                4 -> showAbout()
                else -> Toast.makeText(this, "Setting control will be expanded in the next pass.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Profile saved for browser session.", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }
    private fun showFbState() {
        val options = arrayOf("Import AppState JSON", "Import Cookie Header", "Import Netscape file")
        AlertDialog.Builder(this).setTitle("C3C FBState Utility")
            .setMessage("Manual, consent-based and local-only. Session cookies are sensitive; preview is masked.")
            .setItems(options) { _, which ->
                AlertDialog.Builder(this).setTitle("Import sensitive session data?")
                    .setMessage("You are choosing a local file. Cookies can grant account access. OmegA does not upload or auto-read browser cookies.")
                    .setPositiveButton("I understand") { _, _ ->
                        pendingImport = which
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }
                        startActivityForResult(intent, REQUEST_IMPORT)
                    }.setNegativeButton("Cancel", null).show()
            }.setNegativeButton("Close", null).show()
    }
    @Deprecated("Legacy activity result retained for API 19 compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_IMPORT || resultCode != RESULT_OK || data?.data == null) return
        try {
            val source = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { it.readText() } ?: ""
            val names = when (pendingImport) {
                0 -> Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").findAll(source).map { it.groupValues[1] }.toList()
                1 -> source.split(";").map { it.trim().substringBefore("=") }.filter { it.isNotEmpty() }
                else -> source.lines().filter { !it.startsWith("#") && it.split(Regex("\\s+")).size >= 7 }.map { it.split(Regex("\\s+"))[5] }
            }.distinct()
            val valid = names.contains("c_user") && names.contains("xs")
            val preview = names.joinToString("\n") { "$it = ••••••••" }
            AlertDialog.Builder(this).setTitle(if (valid) "FBState validated" else "Validation incomplete")
                .setMessage("Cookie names: ${names.size}\nRequired c_user + xs: ${if (valid) "present" else "missing"}\n\n$preview")
                .setPositiveButton("OK", null).show()
        } catch (_: Exception) { Toast.makeText(this, "Could not read selected file.", Toast.LENGTH_LONG).show() }
    }
    private fun showAbout() {
        AlertDialog.Builder(this).setTitle("OmegA Browser").setMessage("Version 1.0.0\nCustomized by NirZon\nIndependent lightweight browser project.")
            .setPositiveButton("OK", null).show()
    }
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack() && web.visibility == View.VISIBLE) web.goBack() else showHome()
    }
    companion object { private const val REQUEST_IMPORT = 4310 }
}
