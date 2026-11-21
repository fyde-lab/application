package com.peltoche.fyde

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSDate
import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSHTTPCookieDomain
import platform.Foundation.NSHTTPCookieExpires
import platform.Foundation.NSHTTPCookieName
import platform.Foundation.NSHTTPCookiePath
import platform.Foundation.NSHTTPCookieSameSiteLax
import platform.Foundation.NSHTTPCookieSameSitePolicy
import platform.Foundation.NSHTTPCookieSameSiteStrict
import platform.Foundation.NSHTTPCookieSecure
import platform.Foundation.NSHTTPCookieValue
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.WebKit.WKWindowFeatures
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time
import uniffi.fyde_sdk.FfiBrowserException
import uniffi.fyde_sdk.FfiWebviewConfig
import uniffi.fyde_sdk.FfiWebviewCookie
import uniffi.fyde_sdk.FfiWebviewHandler
import uniffi.fyde_sdk.FfiWebviewSameSite
import uniffi.fyde_sdk.WebviewEngine

/**
 * `fyde.browser`'s webview on iOS (see the bindings' `WebviewEngine`): one
 * [WKWebView] per scraper run, each on a non-persistent
 * [WKWebsiteDataStore] of its own, so concurrent runs never share cookies,
 * storage or cache, and nothing is left on disk once it's gone.
 *
 * The SDK makes every decision: navigations, popups and downloads are asked
 * of the session's [FfiWebviewHandler]. Calls come from scraper runs' own
 * threads and are carried out on the main queue, waited for here.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosWebviewEngine : WebviewEngine {
    /** Only touched on the main queue. */
    private val sessions = mutableMapOf<ULong, Session>()

    override fun openSession(sessionId: ULong, config: FfiWebviewConfig, handler: FfiWebviewHandler) {
        val session = onMain { Session(config.visible, handler).also { sessions[sessionId] = it } }
        // The cookies are set before returning, so no navigation races them.
        for (cookie in config.cookies) {
            val nsCookie = cookie.toNsCookie() ?: continue
            awaitMain<Unit> { done -> session.dataStore.httpCookieStore.setCookie(nsCookie) { done(Unit) } }
        }
    }

    override fun loadUrl(sessionId: ULong, url: String) {
        onMain {
            val nsUrl = NSURL.URLWithString(url) ?: throw FfiBrowserException.Failed(reason = "invalid url")
            session(sessionId).webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
        }
    }

    override fun evaluateScript(sessionId: ULong, script: String) {
        onMain { session(sessionId).webView.evaluateJavaScript(script, completionHandler = null) }
    }

    override fun cookies(sessionId: ULong): List<FfiWebviewCookie> {
        val store = onMain { session(sessionId).dataStore.httpCookieStore }
        return awaitMain { done ->
            store.getAllCookies { cookies ->
                done(cookies.orEmpty().mapNotNull { (it as? NSHTTPCookie)?.toFfiCookie() })
            }
        }
    }

    override fun closeSession(sessionId: ULong) {
        dispatch_async(dispatch_get_main_queue()) { sessions.remove(sessionId)?.destroy() }
    }

    private fun session(sessionId: ULong): Session =
        sessions[sessionId] ?: throw FfiBrowserException.Failed(reason = "the browser window was closed")

    /** One run's webview; only ever touched on the main queue. */
    private inner class Session(visible: Boolean, val handler: FfiWebviewHandler) {
        val dataStore: WKWebsiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
        private val delegate = Delegate()
        val webView: WKWebView

        init {
            val configuration = WKWebViewConfiguration().apply {
                websiteDataStore = dataStore
                userContentController.addScriptMessageHandler(delegate, name = IPC_HANDLER)
                userContentController.addUserScript(
                    WKUserScript(
                        source = IPC_SHIM,
                        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                        forMainFrameOnly = false,
                    ),
                )
            }
            webView = WKWebView(frame = CGRectMake(0.0, 0.0, 1.0, 1.0), configuration = configuration)
            webView.navigationDelegate = delegate
            webView.UIDelegate = delegate
            // In the key window so its pages run as if on screen: full size
            // on top when visible, 1×1 and transparent otherwise.
            keyWindow()?.let { window ->
                if (visible) {
                    webView.setFrame(window.bounds)
                } else {
                    webView.alpha = 0.01
                    webView.accessibilityElementsHidden = true
                }
                window.addSubview(webView)
            }
        }

        fun destroy() {
            webView.stopLoading()
            webView.removeFromSuperview()
            webView.configuration.userContentController.removeScriptMessageHandlerForName(IPC_HANDLER)
            webView.navigationDelegate = null
            webView.UIDelegate = null
        }

        private inner class Delegate :
            NSObject(),
            WKNavigationDelegateProtocol,
            WKUIDelegateProtocol,
            WKScriptMessageHandlerProtocol {
            override fun userContentController(
                userContentController: WKUserContentController,
                didReceiveScriptMessage: WKScriptMessage,
            ) {
                val body = didReceiveScriptMessage.body as? String ?: return
                // Only pages the SDK would let the webview navigate to may
                // answer it (a third-party frame can't).
                val origin = didReceiveScriptMessage.frameInfo.securityOrigin
                val port = if (origin.port == 0L) "" else ":${origin.port}"
                if (handler.allowNavigation("${origin.protocol}://${origin.host}$port/")) handler.onMessage(body)
            }

            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationAction: WKNavigationAction,
                decisionHandler: (WKNavigationActionPolicy) -> Unit,
            ) {
                val url = decidePolicyForNavigationAction.request.URL?.absoluteString
                when {
                    url == null -> decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                    decidePolicyForNavigationAction.shouldPerformDownload -> {
                        handler.onDownload(url)
                        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                    }
                    handler.allowNavigation(url) -> decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
                    else -> decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                }
            }

            /** A response the webview would download rather than show is cancelled and reported. */
            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationResponse: WKNavigationResponse,
                decisionHandler: (WKNavigationResponsePolicy) -> Unit,
            ) {
                val response = decidePolicyForNavigationResponse.response
                val disposition = (response as? NSHTTPURLResponse)?.allHeaderFields?.entries
                    ?.firstOrNull { (it.key as? String).equals("Content-Disposition", ignoreCase = true) }
                    ?.value as? String
                val isDownload = !decidePolicyForNavigationResponse.canShowMIMEType ||
                    disposition?.trimStart()?.startsWith("attachment", ignoreCase = true) == true
                val url = response.URL?.absoluteString
                if (isDownload && url != null) {
                    handler.onDownload(url)
                    decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel)
                } else {
                    decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
                }
            }

            override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
                sessions.entries.firstOrNull { it.value === this@Session }?.let { sessions.remove(it.key) }
                destroy()
                handler.onClosed()
            }

            /** An allowed popup loads in the session's own webview: there's no second window here. */
            override fun webView(
                webView: WKWebView,
                createWebViewWithConfiguration: WKWebViewConfiguration,
                forNavigationAction: WKNavigationAction,
                windowFeatures: WKWindowFeatures,
            ): WKWebView? {
                val request = forNavigationAction.request
                val url = request.URL?.absoluteString ?: return null
                if (handler.allowNewWindow(url)) webView.loadRequest(request)
                return null
            }
        }
    }

    private companion object {
        const val IPC_HANDLER = "ipc"
        const val MAIN_QUEUE_TIMEOUT_SECONDS = 30L

        /** The `window.ipc.postMessage` the SDK's scripts reply through. */
        const val IPC_SHIM =
            "window.ipc={postMessage:function(m){window.webkit.messageHandlers.$IPC_HANDLER.postMessage(String(m));}};"

        fun keyWindow(): UIWindow? =
            UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow

        /** Runs [block] on the main queue and waits for it. */
        fun <T> onMain(block: () -> T): T = awaitMain { done -> done(block()) }

        /**
         * Runs [start] on the main queue and waits until it calls `done`: for
         * WebKit's completion handlers, which call back on the main queue.
         */
        fun <T> awaitMain(start: (done: (T) -> Unit) -> Unit): T {
            var result: Result<T>? = null
            if (NSThread.isMainThread) {
                start { result = Result.success(it) }
                return result?.getOrThrow() ?: throw FfiBrowserException.Failed(reason = "called on the main thread")
            }
            val semaphore = dispatch_semaphore_create(0)
            dispatch_async(dispatch_get_main_queue()) {
                try {
                    start { value ->
                        result = Result.success(value)
                        dispatch_semaphore_signal(semaphore)
                    }
                } catch (err: Throwable) {
                    result = Result.failure(err)
                    dispatch_semaphore_signal(semaphore)
                }
            }
            val timeout = dispatch_time(DISPATCH_TIME_NOW, MAIN_QUEUE_TIMEOUT_SECONDS * NSEC_PER_SEC.toLong())
            if (dispatch_semaphore_wait(semaphore, timeout) != 0L) {
                throw FfiBrowserException.Failed(reason = "the main thread didn't answer in time")
            }
            return result!!.getOrElse { err ->
                throw err as? FfiBrowserException ?: FfiBrowserException.Failed(reason = err.toString())
            }
        }

        fun FfiWebviewCookie.toNsCookie(): NSHTTPCookie? {
            val properties = mutableMapOf<Any?, Any?>(
                NSHTTPCookieName to name,
                NSHTTPCookieValue to value,
                // A leading dot: a domain cookie, sent to subdomains too.
                NSHTTPCookieDomain to ".$domain",
                NSHTTPCookiePath to path,
            )
            if (secure) properties[NSHTTPCookieSecure] = "TRUE"
            if (httpOnly) properties["HttpOnly"] = "TRUE"
            when (sameSite) {
                FfiWebviewSameSite.STRICT -> properties[NSHTTPCookieSameSitePolicy] = NSHTTPCookieSameSiteStrict
                FfiWebviewSameSite.LAX -> properties[NSHTTPCookieSameSitePolicy] = NSHTTPCookieSameSiteLax
                FfiWebviewSameSite.NONE, null -> {}
            }
            expires?.let { properties[NSHTTPCookieExpires] = NSDate.dateWithTimeIntervalSince1970(it.toDouble()) }
            return NSHTTPCookie.cookieWithProperties(properties)
        }

        fun NSHTTPCookie.toFfiCookie(): FfiWebviewCookie = FfiWebviewCookie(
            name = name,
            value = value,
            domain = domain.trimStart('.'),
            path = path,
            secure = isSecure(),
            httpOnly = isHTTPOnly(),
            sameSite = when (sameSitePolicy) {
                NSHTTPCookieSameSiteStrict -> FfiWebviewSameSite.STRICT
                NSHTTPCookieSameSiteLax -> FfiWebviewSameSite.LAX
                else -> null
            },
            expires = expiresDate?.timeIntervalSince1970?.toLong(),
        )
    }
}
