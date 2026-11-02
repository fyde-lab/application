"""Renders every [data-shot] artboard of the reference pages to PNG.

usage: python3 render.py [page.html ...]   (default: every *.html here)
Screenshots land in ../screens/<data-shot>.png at 2x. Needs `pip install playwright`
and `playwright install chromium`.
"""
import pathlib
import sys

from playwright.sync_api import sync_playwright

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent / "screens"
OUT.mkdir(exist_ok=True)
ICONS = (HERE / "icons.svg").read_text()

SB = ('<div class="status"><span>9:41</span><span class="dots"><i style="width:17px;height:11px"></i>'
      '<i style="width:15px;height:11px;border-radius:3px"></i><i style="width:25px;height:12px;border-radius:4px"></i></span></div>')
HB = '<div class="homebar"></div>'
TABS = [("coffre", "i-grid", "Coffre", None), ("arrivees", "i-inbox", "Arrivées", "5"), ("scan", None, None, None),
        ("sources", "i-plug", "Sources", None), ("moi", "i-user", "Moi", None)]


def tabbar(active, badge_override=None):
    out = ['<div class="tabs">']
    for key, icon, label, badge in TABS:
        if key == "scan":
            out.append('<div class="tab"><span class="scanb" aria-label="Scanner un document">'
                       '<svg class="ic lg"><use href="#i-camera"/></svg></span></div>')
            continue
        on = " on" if key == active else ""
        if badge and badge_override is not None:
            badge = badge_override or None
        b = f'<span class="badge">{badge}</span>' if badge else ""
        out.append(f'<div class="tab{on}"><svg class="ic lg"><use href="#{icon}"/></svg>{label}{b}</div>')
    out.append("</div>")
    return "".join(out)


# In the fixed category order; categories without documents (Famille, Abonnements, Dons here) are hidden.
CATS = [("logement", "Logement", "i-home", "home", 38), ("argent", "Argent", "i-wallet", "money", 51),
        ("travail", "Travail", "i-brief", "work", 41), ("impots", "Impôts", "i-landmark", "tax", 12),
        ("sante", "Santé", "i-heart", "health", 33), ("telecom", "Télécom", "i-phone", "tel", 24),
        ("transport", "Transport", "i-train", "trans", 9), ("achats", "Achats", "i-bag", "shop", 6)]


def sidebar(active):
    """The desktop/tablet sidebar; active = arrivees | tout | sources | moi | <category key>."""
    def item(key, inner, count="", extra=""):
        on = " on" if key == active else ""
        style = ' style="background:var(--pwash)"' if on and key in ("arrivees", "tout", "sources", "moi") else ""
        c = f'<span class="c"{extra}>{count}</span>' if count != "" else ""
        return f'<div class="sitem{on}"{style}>{inner}{c}</div>'
    out = ['<div class="side">',
           '<div class="row" style="gap:10px;padding:0 10px 16px;font:600 18px var(--font)"><span style="width:30px;height:30px;'
           'border-radius:9px;background:var(--primary);color:var(--onp);display:grid;place-items:center"><svg class="ic sm">'
           '<use href="#i-lock"/></svg></span>Fyde</div>',
           item("arrivees", '<svg class="ic"><use href="#i-inbox"/></svg>Arrivées', 5, ' style="color:var(--warn);font-weight:600"'),
           item("tout", '<svg class="ic"><use href="#i-grid"/></svg>Tout le coffre', 214),
           '<p style="font:600 12px var(--font);color:var(--muted);letter-spacing:.06em;padding:16px 10px 6px">CATÉGORIES</p>']
    for key, label, icon, var, n in CATS:
        out.append(item(key, f'<span class="sdot" style="background:var(--c-{var});color:var(--c-{var}-i)">'
                             f'<svg class="ic sm"><use href="#{icon}"/></svg></span>{label}', n))
    out += ['<span class="grow"></span>', item("sources", '<svg class="ic"><use href="#i-plug"/></svg>Sources', 6),
            '<div class="sitem" style="color:var(--ok)"><svg class="ic"><use href="#i-shield"/></svg>Scellé · 3 appareils</div>',
            item("moi", '<span style="width:26px;height:26px;border-radius:13px;background:var(--pwash);color:var(--primary);display:grid;place-items:center;font:600 11px var(--font)">PP</span>pierre', '<svg class="ic sm"><use href="#i-chev-r"/></svg>'),
            '</div>']
    return "".join(out)


def expand(html):
    """Shared snippets: <!--ICONS-->, <!--SB--> status bar, <!--HB--> home bar, <!--TABS:key[:badge]--> phone tab bar, <!--SIDE:key--> sidebar."""
    import re
    html = html.replace("<!--ICONS-->", ICONS).replace("<!--SB-->", SB).replace("<!--HB-->", HB)
    html = re.sub(r"<!--SIDE:(\w+)-->", lambda m: sidebar(m.group(1)), html)
    return re.sub(r"<!--TABS:(\w+)(?::(\w*))?-->", lambda m: tabbar(m.group(1), m.group(2)), html)


pages = [pathlib.Path(p).resolve() for p in sys.argv[1:]] or sorted(HERE.glob("*.html"))

with sync_playwright() as p:
    browser = p.chromium.launch()
    ctx = browser.new_context(viewport={"width": 1600, "height": 1000}, device_scale_factor=2)
    for page_path in pages:
        built = HERE / f".built-{page_path.name}"
        built.write_text(expand(page_path.read_text()))
        page = ctx.new_page()
        page.goto(built.as_uri())
        page.wait_for_load_state("networkidle")
        page.evaluate("document.fonts.ready")
        for el in page.query_selector_all("[data-shot]"):
            name = el.get_attribute("data-shot")
            target = OUT / f"{name}.png"
            target.parent.mkdir(parents=True, exist_ok=True)
            el.screenshot(path=str(target))
            print("wrote", name)
        page.close()
        built.unlink()
    browser.close()
