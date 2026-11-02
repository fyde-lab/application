"""Navigation graph of every reference screen in ../screens.

usage: python3 screen_map.py   (needs Graphviz `dot`)
Writes ../sheets/carte-mobile.{svg,png} and ../sheets/carte-bureau.{svg,png}.
Edit NODES / EDGES here when a screen or a link changes; design/screen-map.md describes the same graph.
"""
import pathlib
import subprocess

HERE = pathlib.Path(__file__).resolve().parent
SCREENS = HERE.parent / "screens"
OUT = HERE.parent / "sheets"

# id: (label, group). Ids that are not a PNG in ../screens are entry points or pages still to draw.
NODES = {
    # phone
    "O1": ("Bienvenue", "onb"), "O2": ("Connexion", "onb"), "O3": ("Créer le coffre", "onb"),
    "O4": ("Stockage indisponible", "onb"), "O5": ("Trop d’appareils", "onb"), "O6": ("Kit de récupération", "onb"),
    "O7": ("Choisir ses sources", "onb"), "O7b": ("Limite de l’offre", "onb"), "O8": ("Notifications", "onb"),
    "O9": ("Ouverture", "onb"),
    "V1": ("Coffre", "vault"), "V2": ("Coffre en remplissage", "vault"), "V3": ("Catégorie", "vault"),
    "V4": ("Recherche", "vault"), "V5": ("Résultats", "vault"), "V6": ("Document", "vault"), "V7": ("Lecteur", "vault"),
    "V8": ("Actions", "vault"), "V9": ("Reclasser", "vault"), "V10": ("Cartes par type", "vault"),
    "V11": ("Coffre sombre", "vault"), "X1": ("Hors ligne", "vault"),
    "K1": ("Scanner", "capture"), "K2": ("Vérifier le scan", "capture"), "K3": ("Depuis une autre app", "capture"),
    "K4": ("Ajout en cours", "capture"),
    "R1": ("Arrivées", "arrivals"), "R2": ("Tout est rangé", "arrivals"), "R3": ("Historique", "arrivals"),
    "R4": ("Notification de code", "arrivals"),
    "S1": ("Sources", "sources"), "S2": ("Catalogue", "sources"), "S3": ("Connecter", "sources"),
    "S4": ("Récupération", "sources"), "S5": ("Détail", "sources"), "S6": ("En échec", "sources"),
    "S7": ("Retirer", "sources"),
    "M1": ("Moi", "moi"), "M2": ("Sécurité", "moi"), "M3": ("Ce que voit le serveur", "moi"),
    "M4": ("Vérifier le mot de passe", "moi"), "M5": ("Appareils", "moi"), "M6": ("Offre et usage", "moi"),
    "M7": ("Apparence et langue", "moi"), "M8": ("Se déconnecter", "moi"),
    "TABS": ("Barre d’onglets", "entry"), "OS": ("Autre app (partage)", "entry"),
    "LOCK": ("Écran verrouillé", "entry"), "PLANS": ("Offres", "todo"), "HELP": ("Aide", "todo"),
    # desktop / tablet
    "DO1": ("Bienvenue", "donb"), "DO2": ("Connexion", "donb"), "DO2b": ("Connexion en cours", "donb"),
    "DO2c": ("Échec de connexion", "donb"), "DO3": ("Créer le coffre", "donb"), "DO3b": ("Créer — erreurs", "donb"),
    "DO4b": ("Trousseau absent", "donb"),
    "DO5": ("Trop d’appareils", "donb"), "DO6": ("Kit de récupération", "donb"), "DO7": ("Choisir ses sources", "donb"),
    "DO7h": ("Limite de l’offre", "donb"), "DO7b": ("Connecter une source", "donb"),
    "DO7c": ("Récupération en cours", "donb"), "DO7d": ("Code reçu (SMS ou e-mail)", "donb"),
    "DO7f": ("Mot de passe refusé", "donb"),
    "DO7g": ("Sources connectées", "donb"), "DO8": ("Notifications", "donb"), "DO9": ("Ouverture", "donb"),
    "SIDE": ("Barre latérale", "dentry"), "T1": ("Tablette — Coffre", "dapp"), "D1": ("Catégorie", "dapp"),
    "D2": ("Document", "dapp"), "D3": ("Arrivées", "dapp"), "D4": ("Sources", "dapp"),
    "DMOI": ("Moi (menu de l’avatar)", "dtodo"),
}

# Links that jump to a distant part of the app. On a map they're drawn as a short tag next to the
# screen they leave ("→ O1"), so they don't cross the whole drawing; the text lists them too.
JUMPS = {("V1", "O2"), ("M8", "O1"), ("M4", "O6"), ("M2", "O6"), ("O7b", "M6"), ("S1", "M6"), ("O5", "PLANS"),
         ("M6", "PLANS"), ("R1", "V9"), ("K4", "V3"), ("O9", "V1"), ("S4", "O8")}

# (from, to, label, kind): nav = a tap moves there; state = the same screen in another state;
# overlay = a sheet or dialog over the screen; todo = leads to a page not drawn yet.
MOBILE = [
    ("TABS", "V1", "Coffre", "nav"), ("TABS", "R1", "Arrivées", "nav"), ("TABS", "K1", "Scanner", "nav"),
    ("TABS", "S1", "Sources", "nav"), ("TABS", "M1", "Moi", "nav"),
    ("O1", "O3", "Créer mon coffre", "nav"), ("O1", "O2", "J’ai déjà un coffre", "nav"),
    ("O2", "O3", "Créer un coffre", "nav"), ("O2", "O4", "pas de trousseau", "overlay"),
    ("O3", "O4", "pas de trousseau", "overlay"), ("O2", "O5", "limite d’appareils", "nav"),
    ("O5", "O9", "appareil déconnecté", "nav"), ("O2", "O9", "1re ouverture ici", "nav"),
    ("O9", "V1", "Parcourir le coffre", "nav"), ("O5", "PLANS", "Voir les offres", "todo"),
    ("O3", "O6", "Continuer", "nav"), ("O6", "O7", "", "nav"), ("O7", "O7b", "limite atteinte", "state"),
    ("O7b", "M6", "Offre et usage", "nav"), ("O7", "S3", "Connecter n sources", "nav"), ("O7", "O8", "Passer", "nav"),
    ("S4", "O8", "dernière source", "nav"), ("O8", "V2", "", "nav"), ("V2", "V1", "chaque source a tourné", "state"),
    ("V1", "V3", "carte", "nav"), ("V1", "V4", "recherche", "nav"), ("V4", "V5", "", "nav"),
    ("V3", "V6", "", "nav"), ("V5", "V6", "", "nav"), ("V6", "V7", "fichier", "nav"), ("V6", "V8", "…", "overlay"),
    ("V8", "V9", "Reclasser", "overlay"), ("V6", "V10", "selon le type", "state"), ("V1", "V11", "sombre", "state"),
    ("V1", "X1", "sans réseau", "state"), ("V1", "R1", "bannière", "nav"), ("V1", "M1", "avatar", "nav"),
    ("V1", "M2", "ligne « Scellé »", "nav"), ("V1", "O2", "session expirée", "nav"),
    ("K1", "K2", "Terminer", "nav"), ("K2", "K4", "Mettre au coffre", "nav"), ("OS", "K3", "Partager", "overlay"),
    ("K3", "K4", "Ajouter au coffre", "nav"), ("K4", "V3", "", "nav"), ("K2", "R1", "à classer", "nav"),
    ("R1", "R2", "tout traité", "state"), ("R1", "R3", "Historique", "nav"), ("R1", "V9", "Modifier", "overlay"),
    ("LOCK", "R4", "", "nav"), ("R4", "R1", "saisir le code", "nav"),
    ("S1", "S2", "+", "nav"), ("S2", "S3", "Ajouter", "nav"), ("S3", "S4", "Connecter", "nav"),
    ("S4", "S3", "source suivante", "nav"), ("S4", "S1", "en arrière-plan", "nav"), ("S1", "S5", "", "nav"),
    ("S1", "S6", "en échec", "nav"), ("S6", "S4", "Mettre à jour et relancer", "nav"), ("S5", "S7", "Retirer", "overlay"),
    ("S1", "M6", "limite", "nav"),
    ("M1", "M2", "", "nav"), ("M1", "M5", "", "nav"), ("M1", "M6", "", "nav"), ("M1", "M7", "", "nav"),
    ("M1", "M8", "", "overlay"), ("M1", "HELP", "Aide", "todo"), ("M2", "M3", "", "nav"), ("M2", "M4", "", "nav"),
    ("M2", "O6", "kit", "nav"), ("M2", "M5", "tuiles", "nav"), ("M4", "O6", "oublié ?", "nav"),
    ("M6", "PLANS", "Voir les offres", "todo"), ("M8", "O1", "confirmé", "nav"),
]
DESKTOP = [
    ("DO1", "DO2", "J’ai déjà un coffre", "nav"), ("DO1", "DO3", "Créer mon coffre", "nav"),
    ("DO2", "DO2b", "Ouvrir le coffre", "state"), ("DO2b", "DO2c", "échec", "state"),
    ("DO2", "DO4b", "pas de trousseau", "overlay"), ("DO2b", "DO5", "limite d’appareils", "nav"),
    ("DO2b", "DO9", "1re ouverture ici", "nav"), ("DO5", "DO9", "appareil déconnecté", "nav"),
    ("DO5", "PLANS", "Voir les offres", "todo"), ("DO2c", "DO6", "kit (à venir)", "todo"),
    ("DO3", "DO3b", "erreurs", "state"), ("DO3", "DO4b", "pas de trousseau", "overlay"), ("DO3", "DO6", "Continuer", "nav"),
    ("DO6", "DO7", "", "nav"), ("DO7", "DO7h", "limite atteinte", "state"), ("DO7", "DO7b", "Connecter n sources", "nav"),
    ("DO7", "DO8", "Passer", "nav"), ("DO7b", "DO7c", "Connecter", "nav"), ("DO7b", "DO7f", "refusé", "state"),
    ("DO7c", "DO7d", "code SMS ou e-mail", "overlay"),
    ("DO7c", "DO7b", "Source suivante", "nav"), ("DO7c", "DO7g", "dernière source", "nav"),
    ("DO7f", "DO7g", "Passer (dernière)", "nav"), ("DO7g", "DO8", "Continuer", "nav"), ("DO8", "SIDE", "", "nav"),
    ("DO9", "SIDE", "Parcourir le coffre", "nav"),
    ("SIDE", "T1", "Tout le coffre", "nav"), ("SIDE", "D1", "catégorie", "nav"), ("SIDE", "D3", "Arrivées", "nav"),
    ("SIDE", "D4", "Sources", "nav"), ("SIDE", "DMOI", "avatar", "todo"), ("D1", "D2", "", "nav"), ("T1", "D1", "carte", "nav"),
]

GROUPS = {
    "entry": ("Entrées", "#FFFFFF"), "onb": ("Accueil et accès (O)", "#DCE8E0"), "vault": ("Coffre (V, X)", "#E8DCC6"),
    "capture": ("Capture (K)", "#F0E1CF"), "arrivals": ("Arrivées (R)", "#D8E3EF"), "sources": ("Sources (S)", "#D4E6E3"),
    "moi": ("Moi (M)", "#E3DCEC"), "todo": ("À dessiner", "#F2F0EA"),
    "dentry": ("Entrées", "#FFFFFF"), "donb": ("Accueil et accès au bureau (DO)", "#DCE8E0"),
    "dapp": ("Coffre au bureau et sur tablette (D, T)", "#E8DCC6"), "dtodo": ("À dessiner", "#F2F0EA"),
}
STYLE = {
    "nav": 'color="#1F4D3A" penwidth=1.6',
    "state": 'color="#8C877B" style=dotted penwidth=1.6 arrowhead=open',
    "overlay": 'color="#2C5582" style=dashed penwidth=1.4',
    "todo": 'color="#B3261E" style=dashed penwidth=1.2 arrowhead=open',
}


LEGEND = """subgraph cluster_legend { label="Légende"; fontname="Inter SemiBold"; fontsize=14; style=rounded; color="#CFCAC0";
  node [shape=plaintext fontsize=12 margin=0];
  la [label=""]; lb [label="on touche, l’écran change"]; lc [label=""]; ld [label="même écran, autre état"];
  le [label=""]; lf [label="feuille ou dialogue par-dessus"]; lg [label=""]; lh [label="vers une page à dessiner"];
  la -> lb [color="#1F4D3A" penwidth=1.6]; lc -> ld [color="#8C877B" style=dotted penwidth=1.6 arrowhead=open];
  le -> lf [color="#2C5582" style=dashed penwidth=1.4]; lg -> lh [color="#B3261E" style=dashed penwidth=1.2 arrowhead=open];
}"""


def node_line(nid):
    label, group = NODES[nid]
    drawn = any(SCREENS.glob(f"{nid}-*.png"))
    shape = "box"
    if group in ("entry", "dentry"):
        attrs = 'shape=box style="rounded,bold" color="#1C1B17" fillcolor="#FFFFFF"'
        text = label
    elif not drawn:
        attrs = 'shape=box style="rounded,dashed,filled" color="#8C877B" fillcolor="#F2F0EA" fontcolor="#5B574F"'
        text = f"{label}\\n(à dessiner)"
    else:
        attrs = f'shape={shape} style="rounded,filled" color="#8C877B" fillcolor="{GROUPS[group][1]}"'
        text = f"{nid}\\n{label}"
    return f'"{nid}" [label="{text}" {attrs}];'


def graph(name, edges, title, rankdir="TB"):
    used = sorted({n for a, b, _, _ in edges for n in ((a,) if (a, b) in JUMPS else (a, b))}, key=list(NODES).index)
    by_group = {}
    for n in used:
        by_group.setdefault(NODES[n][1], []).append(n)
    lines = [f'digraph "{name}" {{', f'rankdir={rankdir}; newrank=true; compound=true; nodesep=0.35; ranksep=0.55;',
             f'label="{title}"; labelloc=t; labeljust=l; fontsize=26; fontname="Inter SemiBold"; pad=0.4; bgcolor="#F2F0EA";',
             'node [fontname="Inter" fontsize=13 margin="0.18,0.08" penwidth=1.2];',
             'edge [fontname="Inter" fontsize=11 fontcolor="#3d3a33" arrowsize=0.8];']
    for g, nodes in by_group.items():
        if g in ("entry", "dentry", "todo", "dtodo"):
            lines += [node_line(n) for n in nodes]
            continue
        lines.append(f'subgraph "cluster_{g}" {{ label="{GROUPS[g][0]}"; fontname="Inter SemiBold"; fontsize=15; '
                     f'style="rounded"; color="#CFCAC0"; penwidth=1.2; margin=14;')
        lines += ["  " + node_line(n) for n in nodes]
        lines.append("}")
    for a, b, label, kind in edges:
        lab = f' label="{label}"' if label else ""
        if (a, b) in JUMPS:
            tag = f"{a}>{b}"
            target = "Offres, à dessiner" if b == "PLANS" else f"{b} · {NODES[b][0]}"
            color = "#B3261E" if kind == "todo" else "#1F4D3A"
            lines.append(f'"{tag}" [label="→ {target}" shape=box style="rounded,filled" fillcolor="#FFFFFF" '
                         f'color="{color}" fontcolor="{color}" fontsize=11 margin="0.10,0.04" penwidth=1];')
            lines.append(f'"{a}" -> "{tag}" [{STYLE[kind]}{lab} arrowhead=none];')
            continue
        lines.append(f'"{a}" -> "{b}" [{STYLE[kind]}{lab}];')
    lines.append(LEGEND)
    lines.append("}")
    dot = "\n".join(lines)
    (HERE / f"{name}.dot").write_text(dot)
    for fmt in ("svg", "png"):
        args = ["dot", f"-T{fmt}", "-o", str(OUT / f"{name}.{fmt}")] + (["-Gdpi=144"] if fmt == "png" else [])
        subprocess.run(args, input=dot.encode(), check=True)
    print("wrote", name)


graph("carte-mobile", MOBILE, "Fyde — carte des écrans, téléphone")
graph("carte-bureau", DESKTOP, "Fyde — carte des écrans, bureau et tablette")

missing = sorted({p.name.split("-")[0] for p in SCREENS.glob("*.png")} - set(NODES))
if missing:
    raise SystemExit(f"screens missing from the map: {missing}")
