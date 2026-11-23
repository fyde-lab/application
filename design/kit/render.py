"""Renders every [data-shot] artboard of a proposal's mockup pages to PNG at 2x.

usage: python3 design/kit/render.py design/proposals/<NNN-slug>/mockups/*.html
Each artboard lands in the proposal's screens/<data-shot>.png (the mockups/ folder's sibling).
A page can include the kit's icon sprite with <!--ICONS-->, and link ../../../kit/tokens.css (the
app's fonts and tokens) and ../../../kit/base.css (the frames).

Needs Playwright with Chromium; on NixOS:
  nix-shell -p 'python3.withPackages(p: [p.playwright])' playwright-driver.browsers --run \
    'export PLAYWRIGHT_BROWSERS_PATH=$(nix-build --no-out-link "<nixpkgs>" -A playwright-driver.browsers);
     export PLAYWRIGHT_SKIP_VALIDATE_HOST_REQUIREMENTS=true; python3 design/kit/render.py ...'
"""
import pathlib
import sys

from playwright.sync_api import sync_playwright

KIT = pathlib.Path(__file__).resolve().parent
ICONS = (KIT / "icons.svg").read_text()

pages = [pathlib.Path(p).resolve() for p in sys.argv[1:]]
if not pages:
    sys.exit(__doc__)

with sync_playwright() as p:
    browser = p.chromium.launch()
    ctx = browser.new_context(viewport={"width": 1600, "height": 1000}, device_scale_factor=2)
    for page_path in pages:
        out = page_path.parent.parent / "screens"
        out.mkdir(exist_ok=True)
        built = page_path.with_name(f".built-{page_path.name}")
        built.write_text(page_path.read_text().replace("<!--ICONS-->", ICONS))
        try:
            page = ctx.new_page()
            page.goto(built.as_uri())
            page.wait_for_load_state("networkidle")
            page.evaluate("document.fonts.ready")
            for el in page.query_selector_all("[data-shot]"):
                name = el.get_attribute("data-shot")
                el.screenshot(path=str(out / f"{name}.png"))
                print("wrote", out / f"{name}.png")
            page.close()
        finally:
            built.unlink()
    browser.close()
