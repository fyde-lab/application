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
TABS = [("coffre", "i-grid", "Coffre", None), ("scan", None, None, None),
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


# Top-level folders, by name (the default layout's, less the ones this user emptied and deleted).
FOLDERS = [("achats", "Achats", "i-bag", "shop", 6), ("argent", "Argent", "i-wallet", "money", 51),
           ("impots", "Impôts", "i-landmark", "tax", 12), ("logement", "Logement", "i-home", "home", 38),
           ("sante", "Santé", "i-heart", "health", 33), ("telecom", "Télécom", "i-phone", "tel", 24),
           ("transport", "Transport", "i-train", "trans", 9), ("travail", "Travail", "i-brief", "work", 41)]
# Logement's subfolders (it also holds 2 documents of its own: 5 + 6 + 14 + 11 + 2 = 38).
SUBFOLDERS = {"logement": [("assurance", "Assurance habitation", 5), ("eau", "Eau", 6), ("energie", "Énergie", 14),
                           ("internet", "Internet", 11)]}


def sidebar(active):
    """The desktop/tablet sidebar; active = tout | aclasser | sources | moi | <folder> | <folder>/<subfolder>."""
    top = active.split("/")[0]

    def item(key, inner, count="", extra="", style=""):
        on = " on" if key == active else ""
        if on and key in ("tout", "sources", "moi", "aclasser"):
            style += ";background:var(--pwash)"
        c = f'<span class="c"{extra}>{count}</span>' if count != "" else ""
        return f'<div class="sitem{on}" style="{style}">{inner}{c}</div>'
    out = ['<div class="side">',
           '<div class="row" style="gap:10px;padding:0 10px 14px;font:600 18px var(--font)"><span style="width:30px;height:30px;'
           'border-radius:9px;background:var(--primary);color:var(--onp);display:grid;place-items:center"><svg class="ic sm">'
           '<use href="#i-lock"/></svg></span>Fyde</div>',
           item("tout", '<svg class="ic"><use href="#i-grid"/></svg>Tout le coffre', 217),
           item("aclasser", '<span class="sdot" style="background:var(--c-none);color:var(--c-none-i)"><svg class="ic sm"><use href="#i-doc"/></svg></span>À classer', 3),
           '<div class="row" style="padding:12px 4px 4px 10px;justify-content:space-between"><p style="font:600 12px var(--font);color:var(--muted);letter-spacing:.06em">DOSSIERS</p>'
           '<span class="sadd" aria-label="Nouveau dossier"><svg class="ic sm"><use href="#i-plus"/></svg></span></div>']
    for key, label, icon, var, n in FOLDERS:
        subs = SUBFOLDERS.get(key, [])
        open_ = key == top and subs
        chev = (f'<svg class="ic xs schev" style="color:var(--muted)"><use href="#i-chev-{"d" if open_ else "r"}"/></svg>' if subs
                else '<span class="schev"></span>')
        bg = f"background:var(--c-{var})" if key == active else ""
        out.append(item(key, f'{chev}<span class="sdot" style="background:{"rgba(255,255,255,.6)" if key == active else f"var(--c-{var})"};color:var(--c-{var}-i)">'
                             f'<svg class="ic sm"><use href="#{icon}"/></svg></span>{label}', n, style=bg))
        if open_:
            for skey, slabel, sn in subs:
                k = f"{key}/{skey}"
                sbg = f"background:var(--c-{var})" if k == active else ""
                out.append(item(k, f'<svg class="ic sm" style="color:var(--c-{var}-i)"><use href="#i-folder"/></svg>{slabel}', sn,
                                style=f"height:34px;padding-left:52px;font-size:14px;white-space:nowrap;{sbg}"))
    out += ['<span class="grow"></span>', item("sources", '<svg class="ic"><use href="#i-plug"/></svg>Sources', 6),
            '<div class="sitem" style="color:var(--ok)"><svg class="ic"><use href="#i-shield"/></svg>Scellé · 3 appareils</div>',
            item("moi", '<span style="width:26px;height:26px;border-radius:13px;background:var(--pwash);color:var(--primary);display:grid;place-items:center;font:600 11px var(--font)">PP</span>pierre', '<svg class="ic sm"><use href="#i-chev-r"/></svg>'),
            '</div>']
    return "".join(out)


LATEST = {"achats": "Facture Fnac · 2 sept.", "argent": "Relevé de compte · 1 oct.", "impots": "Avis d’impôt 2026",
          "logement": "Facture EDF · 12 oct.", "sante": "Remboursement · 22 sept.", "telecom": "Facture Free · 9 oct.",
          "transport": "Billet Paris → Lyon", "travail": "Bulletin de paie · 10 oct."}


def folder_cards(n):
    """The first n cards of the vault's home: À classer (when it holds documents), then the top-level folders by name."""
    def card(var, icon, count, name, latest):
        return (f'<div class="cat fold" style="--cbg:var(--c-{var});--ci:var(--c-{var}-i)"><div class="row" style="justify-content:space-between">'
                f'<span class="ico"><svg class="ic"><use href="#{icon}"/></svg></span><span class="cnt">{count}</span></div>'
                f'<div><p class="n">{name}</p><p class="l trunc">{latest}</p></div></div>')
    cards = [card("none", "i-doc", 3, "À classer", "Contrat de bail — scan")]
    cards += [card(var, icon, count, name, LATEST[key]) for key, name, icon, var, count in FOLDERS]
    return "".join(cards[:int(n)])


def expand(html):
    """Shared snippets: <!--ICONS-->, <!--SB--> status bar, <!--HB--> home bar, <!--TABS:key[:badge]--> phone tab bar, <!--SIDE:key--> sidebar,
    <!--FOLDERCARDS:n--> the first n folder cards of the vault's home."""
    import re
    html = html.replace("<!--ICONS-->", ICONS).replace("<!--SB-->", SB).replace("<!--HB-->", HB)
    html = re.sub(r"<!--FOLDERCARDS:(\d+)-->", lambda m: folder_cards(m.group(1)), html)
    html = re.sub(r"<!--SIDE:([\w/]+)-->", lambda m: sidebar(m.group(1)), html)
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
