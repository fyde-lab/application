"""Renders every [data-shot] artboard of the mockup pages to PNG.

usage: python3 render.py [page.html ...]   (default: every *.html here)
Screenshots land in ../screenshots/<data-shot>.png at 2x.
"""
import pathlib
import sys

from playwright.sync_api import sync_playwright

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent / "screenshots"
OUT.mkdir(exist_ok=True)
ICONS = (HERE / "icons.svg").read_text()

pages = [pathlib.Path(p).resolve() for p in sys.argv[1:]] or sorted(HERE.glob("*.html"))

with sync_playwright() as p:
    browser = p.chromium.launch()
    ctx = browser.new_context(viewport={"width": 1600, "height": 1000}, device_scale_factor=2)
    for page_path in pages:
        built = HERE / f".built-{page_path.name}"
        built.write_text(page_path.read_text().replace("<!--ICONS-->", ICONS))
        page = ctx.new_page()
        page.goto(built.as_uri())
        page.wait_for_load_state("networkidle")
        page.evaluate("document.fonts.ready")
        for el in page.query_selector_all("[data-shot]"):
            name = el.get_attribute("data-shot")
            el.screenshot(path=str(OUT / f"{name}.png"))
            print("wrote", name)
        page.close()
        built.unlink()
    browser.close()
