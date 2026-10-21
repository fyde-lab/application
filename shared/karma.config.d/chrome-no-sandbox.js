// CI runners can't start headless Chrome's sandbox ("ChromeHeadless failed 2
// times (cannot start)"), so under CI (GitHub Actions sets CI=true) the web
// tests launch it without one. Local runs keep the default sandboxed launcher.
if (process.env.CI) {
    config.set({
        browsers: ["ChromeHeadlessNoSandbox"],
        customLaunchers: {
            ChromeHeadlessNoSandbox: {
                base: "ChromeHeadless",
                flags: ["--no-sandbox"],
            },
        },
    });
}
