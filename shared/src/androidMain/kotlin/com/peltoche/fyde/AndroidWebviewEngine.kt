package com.peltoche.fyde

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import uniffi.fyde_sdk.FfiBrowserException
import uniffi.fyde_sdk.FfiWebviewConfig
import uniffi.fyde_sdk.FfiWebviewCookie
import uniffi.fyde_sdk.FfiWebviewHandler
import uniffi.fyde_sdk.FfiWebviewSameSite
import uniffi.fyde_sdk.WebviewEngine

/**
 * `fyde.browser`'s webview on Android (see the bindings' `WebviewEngine`):
 * one [WebView] per scraper run, each in a WebView profile of its own
 * (androidx.webkit's multi-profile API), so concurrent runs never share
 * cookies, storage or cache, and deleted with it once the run ends.
 *
 * Without multi-profile support (WebView older than 110) it refuses every
 * session rather than fall back to the app-wide data store, which every
 * run, and the app's own WebViews, would share and keep on disk.
 *
 * The SDK makes every decision: navigations, popups and downloads are asked
 * of the session's [FfiWebviewHandler]. Calls come from scraper runs' own
 * threads and are carried out on the main thread, waited for here.
 */
internal class AndroidWebviewEngine(private val context: Context) : WebviewEngine {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val sessions = ConcurrentHashMap<ULong, Session>()

    init {
        ResumedActivity.onResumed { activity -> sessions.values.forEach { it.attachTo(activity) } }
        // Profiles a previous process left behind (it was killed mid-run).
        mainHandler.post { if (isSupported()) deleteLeftoverProfiles() }
    }

    override fun openSession(sessionId: ULong, config: FfiWebviewConfig, handler: FfiWebviewHandler) {
        onMain {
            if (!isSupported()) {
                throw FfiBrowserException.Failed(
                    reason = "this device's WebView can't isolate a run's data (needs multi-profile support, WebView 110+)",
                )
            }
            val session = Session(sessionId, config.visible, handler)
            config.cookies.forEach { session.setCookie(it) }
            session.cookieManager.flush()
            sessions[sessionId] = session
            ResumedActivity.activity?.let { session.attachTo(it) }
        }
    }

    override fun loadUrl(sessionId: ULong, url: String) {
        onMain { session(sessionId).load(url) }
    }

    override fun evaluateScript(sessionId: ULong, script: String) {
        onMain { session(sessionId).webView.evaluateJavascript(IPC_SHIM + script, null) }
    }

    override fun cookies(sessionId: ULong): List<FfiWebviewCookie> = onMain { session(sessionId).cookies() }

    override fun closeSession(sessionId: ULong) {
        mainHandler.post { sessions.remove(sessionId)?.destroy() }
    }

    private fun session(sessionId: ULong): Session =
        sessions[sessionId] ?: throw FfiBrowserException.Failed(reason = "the browser window was closed")

    private fun isSupported(): Boolean =
        WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) &&
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)

    private fun deleteLeftoverProfiles() {
        val store = ProfileStore.getInstance()
        store.allProfileNames.filter { it.startsWith(PROFILE_PREFIX) }.forEach { name ->
            runCatching { store.deleteProfile(name) }
        }
    }

    /**
     * Runs [block] on the main thread and waits for it. Never called from the
     * main thread itself (the SDK calls from scraper runs' threads), but runs
     * it inline there rather than deadlock if it ever is.
     */
    private fun <T> onMain(block: () -> T): T {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val done = CountDownLatch(1)
        var result: Result<T>? = null
        mainHandler.post {
            result = runCatching(block)
            done.countDown()
        }
        if (!done.await(MAIN_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw FfiBrowserException.Failed(reason = "the main thread didn't answer in time")
        }
        return result!!.getOrElse { err ->
            throw err as? FfiBrowserException ?: FfiBrowserException.Failed(reason = err.toString())
        }
    }

    /** One run's WebView and profile; only ever touched on the main thread. */
    @SuppressLint("SetJavaScriptEnabled")
    private inner class Session(sessionId: ULong, private val visible: Boolean, val handler: FfiWebviewHandler) {
        private val profileName = "$PROFILE_PREFIX$sessionId-${System.nanoTime()}"
        val cookieManager: CookieManager
        val webView: WebView

        /** Every host a page or frame of this session loaded from, to read cookies back for. */
        private val hosts = mutableSetOf<String>()
        private var closed = false

        init {
            ProfileStore.getInstance().getOrCreateProfile(profileName)
            webView = newWebView()
            cookieManager = WebViewCompat.getProfile(webView).cookieManager
            WebViewCompat.addWebMessageListener(webView, IPC_OBJECT, setOf("*")) { _, message, sourceOrigin, _, _ ->
                // Only pages the SDK would let the webview navigate to may
                // answer it (a third-party frame can't).
                val body = message.data ?: return@addWebMessageListener
                if (handler.allowNavigation(sourceOrigin.toString())) handler.onMessage(body)
            }
            webView.webViewClient = SessionClient()
            webView.webChromeClient = SessionChromeClient()
            // The WebView never downloads anything itself: it only reports it.
            webView.setDownloadListener { url, _, _, _, _ -> handler.onDownload(url) }
        }

        /** A WebView on this session's profile, set before anything else touches it. */
        fun newWebView(): WebView = WebView(context).also { view ->
            WebViewCompat.setProfile(view, profileName)
            view.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
            }
        }

        fun load(url: String) {
            trackHost(url)
            webView.loadUrl(url)
        }

        /**
         * Puts the WebView in [activity]'s window: full size on top when
         * [visible], otherwise 1×1 and transparent, still laid out and
         * rendered so its pages run as if on screen.
         */
        fun attachTo(activity: Activity) {
            if (closed) return
            (webView.parent as? ViewGroup)?.removeView(webView)
            val root = activity.window.decorView as ViewGroup
            if (visible) {
                root.addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            } else {
                webView.alpha = 0f
                webView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                root.addView(webView, FrameLayout.LayoutParams(1, 1))
            }
        }

        fun setCookie(cookie: FfiWebviewCookie) {
            cookieManager.setCookie("https://${cookie.domain}${cookie.path}", cookie.toSetCookie())
            hosts += cookie.domain
        }

        /**
         * The cookies of every host this session saw. `CookieManager` only
         * gives names and values: every other attribute reads as its default,
         * and a cookie seen from several hosts is kept once, on the shortest.
         */
        fun cookies(): List<FfiWebviewCookie> {
            val byName = linkedMapOf<Pair<String, String>, FfiWebviewCookie>()
            for (host in hosts.sortedBy { it.length }) {
                val header = cookieManager.getCookie("https://$host/") ?: continue
                for (pair in header.split(";")) {
                    val name = pair.substringBefore("=").trim()
                    if (name.isEmpty()) continue
                    val value = pair.substringAfter("=", "").trim()
                    byName.getOrPut(name to value) {
                        FfiWebviewCookie(
                            name = name,
                            value = value,
                            domain = host,
                            path = "/",
                            secure = false,
                            httpOnly = false,
                            sameSite = null,
                            expires = null,
                        )
                    }
                }
            }
            return byName.values.toList()
        }

        fun destroy() {
            closed = true
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
            // A profile can only be deleted once no WebView uses it any more,
            // which `destroy` only gets to after this frame.
            mainHandler.post { runCatching { ProfileStore.getInstance().deleteProfile(profileName) } }
        }

        private fun trackHost(url: String) {
            android.net.Uri.parse(url).host?.let { hosts += it }
        }

        private inner class SessionClient : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (!handler.allowNavigation(url)) return true
                trackHost(url)
                return false
            }

            // `shouldOverrideUrlLoading` skips form POSTs and frames: their
            // document requests are refused here instead.
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                val dest = request.requestHeaders.entries
                    .firstOrNull { it.key.equals("Sec-Fetch-Dest", ignoreCase = true) }?.value
                val isDocument = request.isForMainFrame || dest in setOf("document", "iframe", "frame")
                if (!isDocument) return null
                if (!handler.allowNavigation(url)) {
                    return WebResourceResponse("text/plain", "utf-8", 403, "Forbidden", emptyMap(), ByteArrayInputStream(ByteArray(0)))
                }
                trackHost(url)
                return null
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                trackHost(url)
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                sessions.entries.firstOrNull { it.value === this@Session }?.let { sessions.remove(it.key) }
                closed = true
                (webView.parent as? ViewGroup)?.removeView(webView)
                webView.destroy()
                mainHandler.post { runCatching { ProfileStore.getInstance().deleteProfile(profileName) } }
                handler.onClosed()
                return true
            }
        }

        /**
         * A popup (`window.open`, `target="_blank"`) only gets its URL once it
         * starts loading, so it opens in a throwaway WebView on this session's
         * profile that reports its first URL and is destroyed. An allowed popup
         * then loads in the session's own WebView: there's no second window
         * here.
         */
        private inner class SessionChromeClient : WebChromeClient() {
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                val popup = newWebView()
                popup.webViewClient = object : WebViewClient() {
                    private var decided = false

                    private fun decide(url: String): Boolean {
                        if (decided) return true
                        decided = true
                        if (handler.allowNewWindow(url)) load(url)
                        mainHandler.post { popup.destroy() }
                        return true
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        decide(request.url.toString())

                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        view.stopLoading()
                        decide(url)
                    }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView = popup
                resultMsg.sendToTarget()
                return true
            }
        }
    }

    private companion object {
        const val PROFILE_PREFIX = "fyde-run-"
        const val IPC_OBJECT = "fydeIpc"
        const val MAIN_THREAD_TIMEOUT_SECONDS = 30L

        /** The `window.ipc.postMessage` the SDK's scripts reply through. */
        const val IPC_SHIM =
            "if(!window.ipc){window.ipc={postMessage:function(m){$IPC_OBJECT.postMessage(String(m));}};}\n"

        val COOKIE_DATE = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }

        fun FfiWebviewCookie.toSetCookie(): String = buildString {
            append("$name=$value; Domain=$domain; Path=$path")
            if (secure) append("; Secure")
            if (httpOnly) append("; HttpOnly")
            when (sameSite) {
                FfiWebviewSameSite.STRICT -> append("; SameSite=Strict")
                FfiWebviewSameSite.LAX -> append("; SameSite=Lax")
                FfiWebviewSameSite.NONE -> append("; SameSite=None")
                null -> {}
            }
            expires?.let { append("; Expires=${synchronized(COOKIE_DATE) { COOKIE_DATE.format(Date(it * 1000)) }}") }
        }
    }
}
