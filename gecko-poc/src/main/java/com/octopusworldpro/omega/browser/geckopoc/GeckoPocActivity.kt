package com.octopusworldpro.omega.browser.geckopoc

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * Isolated GeckoView proof-of-concept.
 *
 * This is intentionally a separate APK/module. It does not replace the API-19
 * WebView app until compatibility, feature parity, size and device gates pass.
 */
class GeckoPocActivity : Activity() {
    private lateinit var session: GeckoSession
    private lateinit var geckoView: GeckoView
    private lateinit var address: EditText
    private lateinit var status: TextView
    private lateinit var backButton: Button
    private lateinit var forwardButton: Button
    private var canGoBack = false
    private var canGoForward = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activeRuntime = runtime ?: GeckoRuntime.create(this).also { runtime = it }
        session = GeckoSession()
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, value: Boolean) {
                canGoBack = value
                if (::backButton.isInitialized) backButton.isEnabled = value
            }

            override fun onCanGoForward(session: GeckoSession, value: Boolean) {
                canGoForward = value
                if (::forwardButton.isInitialized) forwardButton.isEnabled = value
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                if (!url.isNullOrBlank() && ::address.isInitialized) address.setText(url)
                if (::status.isInitialized) status.text = "GeckoView: " + (url ?: "Loading")
            }
        }
        session.open(activeRuntime)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 8, 23))
        }
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 8, 10, 8)
        }
        address = EditText(this).apply {
            hint = "Search or enter address"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(35, 29, 54))
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    navigate(address.text.toString())
                    true
                } else false
            }
        }
        toolbar.addView(address, LinearLayout.LayoutParams(0, 52, 1f))
        toolbar.addView(Button(this).apply {
            text = "Go"
            setOnClickListener { navigate(address.text.toString()) }
        })
        root.addView(toolbar)

        status = TextView(this).apply {
            text = "GeckoView engine initialized"
            setTextColor(Color.LTGRAY)
            setPadding(12, 4, 12, 4)
        }
        root.addView(status)

        geckoView = GeckoView(this)
        geckoView.setSession(session)
        root.addView(geckoView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(25, 20, 43))
        }
        backButton = Button(this).apply {
            text = "Back"
            isEnabled = canGoBack
            setOnClickListener { if (canGoBack) session.goBack() }
        }
        controls.addView(backButton)
        forwardButton = Button(this).apply {
            text = "Forward"
            isEnabled = canGoForward
            setOnClickListener { if (canGoForward) session.goForward() }
        }
        controls.addView(forwardButton)
        controls.addView(Button(this).apply {
            text = "Reload"
            setOnClickListener { session.reload() }
        })
        root.addView(controls)
        setContentView(root)
    }

    private fun navigate(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        val uri = when {
            input.startsWith("https://") || input.startsWith("http://") -> input
            input.contains('.') && !input.contains(' ') -> "https://$input"
            else -> "https://www.google.com/search?q=" +
                android.net.Uri.encode(input)
        }
        address.setText(uri)
        status.text = "Loading with GeckoView"
        session.loadUri(uri)
    }

    override fun onDestroy() {
        if (::session.isInitialized) {
            geckoView.releaseSession()
            session.close()
        }
        super.onDestroy()
    }

    companion object {
        @Volatile private var runtime: GeckoRuntime? = null
    }
}
