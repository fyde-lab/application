//! `fyde.browser`'s webview engine across the FFI (see
//! [`fyde_sdk::browser::WebviewEngine`], whose docs are the contract).
//!
//! On Android and iOS the app implements [`WebviewEngine`] over the
//! platform's own webview and passes it to `FydeClient::init`; elsewhere
//! (desktop) the bindings run `fyde-browser-wry`'s engine themselves. The
//! engine only hosts webviews: every decision is the SDK's, asked through
//! the [`FfiWebviewHandler`] each session gets.

use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};

use fyde_sdk::browser as sdk;

/// A webview engine's failure, thrown by the app's [`WebviewEngine`]: the
/// SDK reports it as `BrowserFailed`, its message only logged.
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum FfiBrowserError {
    #[error("{reason}")]
    Failed { reason: String },
}

impl From<uniffi::UnexpectedUniFFICallbackError> for FfiBrowserError {
    fn from(err: uniffi::UnexpectedUniFFICallbackError) -> Self {
        Self::Failed { reason: err.reason }
    }
}

fn to_sdk_error(err: FfiBrowserError) -> fyde_sdk::Error {
    match err {
        FfiBrowserError::Failed { reason } => fyde_sdk::Error::browser(reason),
    }
}

/// The platform's webview, implemented by the app (Android, iOS) and
/// called from scraper runs' own blocking threads, never the UI thread: an
/// implementation hops to its UI thread itself and may block until it's
/// done there. Each session is named by the `session_id` `open_session`
/// got, unique for this engine. See [`fyde_sdk::browser::WebviewEngine`]
/// for what an engine must guarantee (an isolated, ephemeral data store per
/// session above all: refuse to open one rather than share one).
#[uniffi::export(callback_interface)]
pub trait WebviewEngine: Send + Sync {
    /// Opens session `session_id`'s webview, loads `config`'s cookies into
    /// it and returns once it's ready, asking `handler` about everything it
    /// does until it's closed.
    fn open_session(
        &self,
        session_id: u64,
        config: FfiWebviewConfig,
        handler: Arc<FfiWebviewHandler>,
    ) -> Result<(), FfiBrowserError>;

    /// Starts loading `url`, without waiting for it to load.
    fn load_url(&self, session_id: u64, url: String) -> Result<(), FfiBrowserError>;

    /// Runs `script` in the current page, without waiting for it: it
    /// reports back through `window.ipc.postMessage` itself.
    fn evaluate_script(&self, session_id: u64, script: String) -> Result<(), FfiBrowserError>;

    /// Every cookie the session's webview holds, whatever its host.
    fn cookies(&self, session_id: u64) -> Result<Vec<FfiWebviewCookie>, FfiBrowserError>;

    /// Closes the session's webview and deletes its data store.
    fn close_session(&self, session_id: u64);
}

/// The SDK's half of a session, which the app's [`WebviewEngine`] calls
/// back into from any thread. Every method is cheap and never blocks.
#[derive(uniffi::Object)]
pub struct FfiWebviewHandler {
    inner: Arc<dyn sdk::WebviewHandler>,
}

#[uniffi::export]
impl FfiWebviewHandler {
    /// A page posted `body` through `window.ipc.postMessage`.
    pub fn on_message(&self, body: String) {
        self.inner.on_message(body)
    }

    /// Whether the webview may navigate (a frame or the page) to `url`:
    /// block it on `false`.
    pub fn allow_navigation(&self, url: String) -> bool {
        self.inner.allow_navigation(&url)
    }

    /// Whether the webview may open a popup on `url`: never open it on
    /// `false`.
    pub fn allow_new_window(&self, url: String) -> bool {
        self.inner.allow_new_window(&url)
    }

    /// The page started a native download of `url`, which the engine
    /// cancelled before writing anything.
    pub fn on_download(&self, url: String) {
        self.inner.on_download(&url)
    }

    /// The webview went away on its own (the system killed it, say).
    pub fn on_closed(&self) {
        self.inner.on_closed()
    }
}

/// How to open a session, mirroring [`sdk::WebviewConfig`].
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct FfiWebviewConfig {
    /// Whether the user is shown the webview. Hidden, it must still run as
    /// if it were on screen (in the view hierarchy, not throttled).
    pub visible: bool,
    pub cookies: Vec<FfiWebviewCookie>,
}

impl From<sdk::WebviewConfig> for FfiWebviewConfig {
    fn from(config: sdk::WebviewConfig) -> Self {
        Self {
            visible: config.visible,
            cookies: config.cookies.into_iter().map(Into::into).collect(),
        }
    }
}

/// A cookie, mirroring [`sdk::WebviewCookie`]: always set as a domain
/// cookie on `domain` (no leading dot). An engine that can't read some
/// attribute back (Android's `CookieManager` only gives names and values)
/// reports the default: `/`, not secure, not HTTP-only, no `SameSite`, no
/// expiry.
#[derive(Clone, PartialEq, Eq, uniffi::Record)]
pub struct FfiWebviewCookie {
    pub name: String,
    pub value: String,
    pub domain: String,
    pub path: String,
    pub secure: bool,
    pub http_only: bool,
    pub same_site: Option<FfiWebviewSameSite>,
    /// Unix seconds; `None` for a session cookie.
    pub expires: Option<i64>,
}

/// Cookies carry sessions: never print their values.
impl std::fmt::Debug for FfiWebviewCookie {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("FfiWebviewCookie")
            .field("name", &self.name)
            .field("value", &"<redacted>")
            .field("domain", &self.domain)
            .field("path", &self.path)
            .field("secure", &self.secure)
            .field("http_only", &self.http_only)
            .field("same_site", &self.same_site)
            .field("expires", &self.expires)
            .finish()
    }
}

impl From<sdk::WebviewCookie> for FfiWebviewCookie {
    fn from(cookie: sdk::WebviewCookie) -> Self {
        Self {
            name: cookie.name,
            value: cookie.value,
            domain: cookie.domain,
            path: cookie.path,
            secure: cookie.secure,
            http_only: cookie.http_only,
            same_site: cookie.same_site.map(Into::into),
            expires: cookie.expires,
        }
    }
}

impl From<FfiWebviewCookie> for sdk::WebviewCookie {
    fn from(cookie: FfiWebviewCookie) -> Self {
        Self {
            name: cookie.name,
            value: cookie.value,
            domain: cookie.domain,
            path: cookie.path,
            secure: cookie.secure,
            http_only: cookie.http_only,
            same_site: cookie.same_site.map(Into::into),
            expires: cookie.expires,
        }
    }
}

/// A cookie's `SameSite`, mirroring [`sdk::WebviewSameSite`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum FfiWebviewSameSite {
    Strict,
    Lax,
    None,
}

impl From<sdk::WebviewSameSite> for FfiWebviewSameSite {
    fn from(same_site: sdk::WebviewSameSite) -> Self {
        match same_site {
            sdk::WebviewSameSite::Strict => Self::Strict,
            sdk::WebviewSameSite::Lax => Self::Lax,
            sdk::WebviewSameSite::None => Self::None,
        }
    }
}

impl From<FfiWebviewSameSite> for sdk::WebviewSameSite {
    fn from(same_site: FfiWebviewSameSite) -> Self {
        match same_site {
            FfiWebviewSameSite::Strict => Self::Strict,
            FfiWebviewSameSite::Lax => Self::Lax,
            FfiWebviewSameSite::None => Self::None,
        }
    }
}

/// The engine `FydeClient::init` hands the SDK: the app's, or else this
/// platform's own (`fyde-browser-wry` on desktop, none on mobile).
pub(crate) fn engine(
    app_engine: Option<Box<dyn WebviewEngine>>,
) -> Option<Arc<dyn sdk::WebviewEngine>> {
    match app_engine {
        Some(app_engine) => Some(Arc::new(ForeignEngine {
            app_engine: Arc::from(app_engine),
            next_session_id: AtomicU64::new(1),
        })),
        None => platform_engine(),
    }
}

#[cfg(not(any(target_os = "android", target_os = "ios")))]
fn platform_engine() -> Option<Arc<dyn sdk::WebviewEngine>> {
    Some(Arc::new(fyde_browser_wry::WryEngine))
}

#[cfg(any(target_os = "android", target_os = "ios"))]
fn platform_engine() -> Option<Arc<dyn sdk::WebviewEngine>> {
    None
}

/// The app's [`WebviewEngine`] as the SDK's.
struct ForeignEngine {
    app_engine: Arc<dyn WebviewEngine>,
    next_session_id: AtomicU64,
}

impl sdk::WebviewEngine for ForeignEngine {
    fn open_session(
        &self,
        config: sdk::WebviewConfig,
        handler: Arc<dyn sdk::WebviewHandler>,
    ) -> fyde_sdk::Result<Box<dyn sdk::WebviewSession>> {
        let session_id = self.next_session_id.fetch_add(1, Ordering::Relaxed);
        self.app_engine
            .open_session(
                session_id,
                config.into(),
                Arc::new(FfiWebviewHandler { inner: handler }),
            )
            .map_err(to_sdk_error)?;
        Ok(Box::new(ForeignSession {
            app_engine: self.app_engine.clone(),
            session_id,
        }))
    }
}

/// One session of the app's [`WebviewEngine`].
struct ForeignSession {
    app_engine: Arc<dyn WebviewEngine>,
    session_id: u64,
}

impl sdk::WebviewSession for ForeignSession {
    fn load_url(&self, url: &str) -> fyde_sdk::Result<()> {
        self.app_engine
            .load_url(self.session_id, url.to_string())
            .map_err(to_sdk_error)
    }

    fn evaluate_script(&self, script: &str) -> fyde_sdk::Result<()> {
        self.app_engine
            .evaluate_script(self.session_id, script.to_string())
            .map_err(to_sdk_error)
    }

    fn cookies(&self) -> fyde_sdk::Result<Vec<sdk::WebviewCookie>> {
        self.app_engine
            .cookies(self.session_id)
            .map(|cookies| cookies.into_iter().map(Into::into).collect())
            .map_err(to_sdk_error)
    }

    fn close(&self) {
        self.app_engine.close_session(self.session_id)
    }
}

#[cfg(test)]
mod tests {
    use std::sync::Mutex;

    use fyde_sdk::ErrorCode;

    use super::*;

    /// An app engine recording every call, failing `load_url`.
    #[derive(Default)]
    struct RecordingEngine {
        calls: Mutex<Vec<String>>,
        handler: Mutex<Option<Arc<FfiWebviewHandler>>>,
    }

    impl WebviewEngine for Arc<RecordingEngine> {
        fn open_session(
            &self,
            session_id: u64,
            config: FfiWebviewConfig,
            handler: Arc<FfiWebviewHandler>,
        ) -> Result<(), FfiBrowserError> {
            self.calls.lock().unwrap().push(format!(
                "open {session_id} visible={} cookies={}",
                config.visible, config.cookies[0].value
            ));
            *self.handler.lock().unwrap() = Some(handler);
            Ok(())
        }

        fn load_url(&self, _session_id: u64, _url: String) -> Result<(), FfiBrowserError> {
            Err(FfiBrowserError::Failed {
                reason: "no WebView".to_string(),
            })
        }

        fn evaluate_script(&self, session_id: u64, script: String) -> Result<(), FfiBrowserError> {
            self.calls
                .lock()
                .unwrap()
                .push(format!("eval {session_id} {script}"));
            Ok(())
        }

        fn cookies(&self, _session_id: u64) -> Result<Vec<FfiWebviewCookie>, FfiBrowserError> {
            Ok(vec![cookie()])
        }

        fn close_session(&self, session_id: u64) {
            self.calls
                .lock()
                .unwrap()
                .push(format!("close {session_id}"));
        }
    }

    fn cookie() -> FfiWebviewCookie {
        FfiWebviewCookie {
            name: "session".to_string(),
            value: "secret".to_string(),
            domain: "example.com".to_string(),
            path: "/".to_string(),
            secure: true,
            http_only: false,
            same_site: Some(FfiWebviewSameSite::Lax),
            expires: Some(1_900_000_000),
        }
    }

    /// Records what the SDK's handler was asked.
    #[derive(Default)]
    struct RecordingHandler(Mutex<Vec<String>>);

    impl sdk::WebviewHandler for RecordingHandler {
        fn on_message(&self, body: String) {
            self.0.lock().unwrap().push(format!("message {body}"));
        }
        fn allow_navigation(&self, url: &str) -> bool {
            url.starts_with("https://example.com")
        }
        fn allow_new_window(&self, _url: &str) -> bool {
            false
        }
        fn on_download(&self, url: &str) {
            self.0.lock().unwrap().push(format!("download {url}"));
        }
        fn on_closed(&self) {
            self.0.lock().unwrap().push("closed".to_string());
        }
    }

    #[test]
    fn the_apps_engine_runs_each_session_under_its_own_id() {
        let app = Arc::new(RecordingEngine::default());
        let engine = engine(Some(Box::new(app.clone()))).unwrap();
        let handler = Arc::new(RecordingHandler::default());
        let config = sdk::WebviewConfig {
            visible: true,
            cookies: vec![cookie().into()],
        };

        let first = engine
            .open_session(config.clone(), handler.clone())
            .unwrap();
        let second = engine.open_session(config, handler.clone()).unwrap();
        first.evaluate_script("1 + 1").unwrap();
        second.close();

        assert_eq!(
            *app.calls.lock().unwrap(),
            vec![
                "open 1 visible=true cookies=secret",
                "open 2 visible=true cookies=secret",
                "eval 1 1 + 1",
                "close 2",
            ]
        );
        assert_eq!(
            first.cookies().unwrap(),
            vec![sdk::WebviewCookie::from(cookie())]
        );
    }

    #[test]
    fn an_app_engine_failure_is_browser_failed() {
        let app = Arc::new(RecordingEngine::default());
        let engine = engine(Some(Box::new(app))).unwrap();
        let session = engine
            .open_session(
                sdk::WebviewConfig {
                    visible: false,
                    cookies: vec![cookie().into()],
                },
                Arc::new(RecordingHandler::default()),
            )
            .unwrap();

        let err = session.load_url("https://example.com/").unwrap_err();

        assert_eq!(err.code(), ErrorCode::BrowserFailed);
    }

    #[test]
    fn the_handler_the_app_gets_forwards_to_the_sdks() {
        let app = Arc::new(RecordingEngine::default());
        let engine = engine(Some(Box::new(app.clone()))).unwrap();
        let sdk_handler = Arc::new(RecordingHandler::default());
        let _session = engine
            .open_session(
                sdk::WebviewConfig {
                    visible: false,
                    cookies: vec![cookie().into()],
                },
                sdk_handler.clone(),
            )
            .unwrap();
        let handler = app.handler.lock().unwrap().clone().unwrap();

        handler.on_message("{}".to_string());
        handler.on_download("https://example.com/doc.pdf".to_string());
        handler.on_closed();

        assert!(handler.allow_navigation("https://example.com/login".to_string()));
        assert!(!handler.allow_navigation("https://evil.example/".to_string()));
        assert!(!handler.allow_new_window("https://example.com/".to_string()));
        assert_eq!(
            *sdk_handler.0.lock().unwrap(),
            vec![
                "message {}",
                "download https://example.com/doc.pdf",
                "closed"
            ]
        );
    }

    #[test]
    fn a_cookie_round_trips_and_never_prints_its_value() {
        let original = cookie();

        assert_eq!(
            FfiWebviewCookie::from(sdk::WebviewCookie::from(original.clone())),
            original
        );
        assert!(!format!("{original:?}").contains("secret"));
    }

    #[cfg(not(any(target_os = "android", target_os = "ios")))]
    #[test]
    fn desktop_runs_its_own_engine_without_the_apps() {
        assert!(engine(None).is_some());
    }
}
