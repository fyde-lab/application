"""Navigation graph of every reference screen in ../screens.

usage: python3 screen_map.py   (needs Graphviz `dot`)
Writes ../sheets/carte-mobile.{svg,png} and ../sheets/carte-bureau.{svg,png}.
Edit NODES / EDGES here when a screen or a link changes; design/screen-map.md describes the same graph.
Fails unless every PNG in ../screens has a node here and a row in design-system.md's Screens tables, and the reverse.
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
    "O4": ("Stockage indisponible", "onb"), "O5": ("Trop d’appareils", "onb"), "O6": ("Kit de récupération", "onb"), "O6b": ("Vérifier le kit", "onb"), "O11": ("Vos dossiers", "onb"),
    "O7": ("Choisir ses sources", "onb"), "O7b": ("Limite de l’offre", "onb"), "O8": ("Notifications", "onb"),
    "O9": ("Ouverture", "onb"), "O10": ("Récupérer mon coffre", "onb"), "O10b": ("Nouveau mot de passe", "onb"),
    "V1": ("Coffre", "vault"), "V2": ("Coffre en remplissage", "vault"), "V3": ("Dossier", "vault"),
    "V4": ("Recherche", "vault"), "V5": ("Résultats", "vault"), "V6": ("Document", "vault"), "V6b": ("Document sombre", "vault"), "V6c": ("Document, similaires", "vault"), "V7": ("Lecteur", "vault"),
    "V8": ("Actions", "vault"), "V8b": ("Actions sombres", "vault"), "V9": ("Déplacer le document", "vault"), "V10": ("Supprimer le document", "vault"),
    "V16": ("Documents similaires", "vault"), "V16b": ("Sélection", "vault"), "V9b": ("Déplacer les documents", "vault"), "V10b": ("Supprimer les documents", "vault"),
    "V12": ("Nouveau dossier", "vault"), "V13": ("Actions du dossier", "vault"), "V14": ("Déplacer le dossier", "vault"), "V15": ("Supprimer le dossier", "vault"),
    "V11": ("Coffre sombre", "vault"), "X1": ("Hors ligne", "vault"), "X2": ("Texte à 200 %", "vault"),
    "K1": ("Scanner", "capture"), "K2": ("Vérifier le scan", "capture"), "K3": ("Depuis une autre app", "capture"),
    "K4": ("Ajout en cours", "capture"),
    "S1": ("Sources", "sources"), "S2": ("Catalogue", "sources"), "S3": ("Connecter", "sources"), "S3b": ("Autre compte", "sources"),
    "S4": ("Récupération", "sources"), "S4b": ("Récupération arrêtée", "sources"), "S5": ("Détail", "sources"), "S5b": ("Détail, en cours", "sources"), "S5c": ("Un compte parmi plusieurs", "sources"), "S6": ("En échec", "sources"), "S6b": ("Site changé", "sources"), "S8": ("Vérification demandée", "sources"),
    "S7": ("Retirer", "sources"), "S7b": ("Retirer un compte", "sources"),
    "M1": ("Moi", "moi"), "M2": ("Sécurité", "moi"), "M2b": ("Sécurité sombre", "moi"), "M3": ("Ce que voit le serveur", "moi"),
    "M4": ("Vérifier le mot de passe", "moi"), "M5": ("Appareils", "moi"), "M6": ("Offre et usage", "moi"),
    "M7": ("Apparence et langue", "moi"), "M8": ("Se déconnecter", "moi"), "M9": ("Offres", "moi"), "M10": ("Aide", "moi"), "M11": ("Changer le mot de passe", "moi"),
    "M12": ("Mes données", "moi"), "M12b": ("Supprimer mon compte", "moi"), "M8b": ("Se déconnecter, sombre", "moi"),
    "TABS": ("Barre d’onglets", "entry"), "OS": ("Autre app (partage)", "entry"),
    # desktop / tablet
    "DO1": ("Bienvenue", "donb"), "DO2": ("Connexion", "donb"), "DO2b": ("Connexion en cours", "donb"),
    "DO2c": ("Échec de connexion", "donb"), "DO3": ("Créer le coffre", "donb"), "DO3b": ("Créer — erreurs", "donb"),
    "DO4b": ("Trousseau absent", "donb"),
    "DO5": ("Trop d’appareils", "donb"), "DO6": ("Kit de récupération", "donb"), "DO6b": ("Vérifier le kit", "donb"), "DO11": ("Vos dossiers", "donb"), "DO7": ("Choisir ses sources", "donb"),
    "DO7h": ("Limite de l’offre", "donb"), "DO7b": ("Connecter une source", "donb"),
    "DO7c": ("Récupération en cours", "donb"), "DO7d": ("Code reçu (SMS ou e-mail)", "donb"),
    "DO7f": ("Mot de passe refusé", "donb"),
    "DO7g": ("Sources connectées", "donb"), "DO8": ("Notifications", "donb"), "DO9": ("Ouverture", "donb"), "DO10": ("Récupérer mon coffre", "donb"), "DO10b": ("Nouveau mot de passe", "donb"),
    "SIDE": ("Barre latérale", "dentry"), "T1": ("Tablette — Coffre", "dapp"), "T2": ("Tablette portrait", "dapp"), "D1": ("Dossier", "dapp"),
    "D2": ("Document", "dapp"), "D2b": ("Actions du document", "dapp"), "D2c": ("Supprimer le document", "dapp"), "D11": ("Documents similaires", "dapp"), "D11b": ("Supprimer les documents", "dapp"), "D4": ("Sources", "dapp"), "D4b": ("Source en cours", "dapp"),
    "D5": ("Moi", "dapp"), "D6": ("Tout le coffre", "dapp"),
    "D7": ("Actions du dossier", "dapp"), "D8": ("Déplacer le dossier", "dapp"), "D9": ("Supprimer le dossier", "dapp"), "D10": ("Nouveau dossier", "dapp"),
}

# Links that jump to a distant part of the app. On a map they're drawn as a short tag next to the
# screen they leave ("→ O1"), so they don't cross the whole drawing; the text lists them too.
JUMPS = {("V1", "O2"), ("M8", "O1"), ("M4", "O6"), ("M2", "O6"), ("O7b", "M6"), ("S1", "M6"), ("O5", "M9"),
         ("K4", "V3"), ("O9", "V1"), ("S4", "O8")}

# (from, to, label, kind): nav = a tap moves there; state = the same screen in another state;
# overlay = a sheet or dialog over the screen; todo = leads to a page not drawn yet.
MOBILE = [
    ("V6", "V6b", "sombre", "state"), ("V8", "V8b", "sombre", "state"),
    ("M2", "M2b", "sombre", "state"), ("M8", "M8b", "sombre", "state"),
    ("V1", "X2", "texte à 200 %", "state"), ("S6", "S6b", "site changé", "state"), ("S4", "S8", "vérification", "overlay"),
    ("S4", "S4b", "Arrêter", "state"), ("S8", "S4b", "Arrêter", "state"), ("S4b", "S1", "Revenir aux sources", "nav"), ("S4b", "S4", "Relancer", "nav"),
    ("S5", "S5b", "Lancer", "state"), ("S5b", "S5", "Arrêter", "state"),
    ("TABS", "V1", "Coffre", "nav"), ("TABS", "K1", "Scanner", "nav"),
    ("TABS", "S1", "Sources", "nav"), ("TABS", "M1", "Moi", "nav"),
    ("O1", "O3", "Créer mon coffre", "nav"), ("O1", "O2", "J’ai déjà un coffre", "nav"),
    ("O2", "O3", "Créer un coffre", "nav"), ("O2", "O4", "pas de trousseau", "overlay"),
    ("O3", "O4", "pas de trousseau", "overlay"), ("O2", "O5", "limite d’appareils", "nav"),
    ("O5", "O9", "appareil retiré", "nav"), ("O2", "O9", "1re ouverture ici", "nav"),
    ("O9", "V1", "Parcourir le coffre", "nav"), ("O2", "O10", "Mot de passe oublié ?", "nav"), ("O10", "O10b", "Continuer", "nav"), ("O10b", "O9", "Rouvrir mon coffre", "nav"), ("O5", "M9", "Voir les offres", "nav"),
    ("O3", "O11", "Continuer", "nav"), ("O6", "O6b", "Je l’ai noté", "nav"), ("O6b", "M2", "Terminé", "nav"), ("O11", "O7", "Continuer", "nav"), ("O7", "O7b", "limite atteinte", "state"),
    ("O7b", "M6", "Offre et usage", "nav"), ("O7", "S3", "Connecter n sources", "nav"), ("O7", "O8", "Passer", "nav"),
    ("S4", "O8", "dernière source", "nav"), ("O8", "V2", "", "nav"), ("V2", "V1", "chaque source a tourné", "state"),
    ("V1", "V3", "dossier", "nav"), ("V3", "V3", "sous-dossier", "nav"), ("V1", "V12", "Nouveau dossier", "overlay"), ("V3", "V12", "Nouveau", "overlay"),
    ("V3", "V13", "…", "overlay"), ("V13", "V12", "Nouveau sous-dossier", "overlay"), ("V13", "V14", "Déplacer", "overlay"), ("V13", "V15", "Supprimer", "overlay"), ("V15", "V3", "supprimé", "nav"), ("V1", "V4", "recherche", "nav"), ("V4", "V5", "", "nav"),
    ("V3", "V6", "", "nav"), ("V5", "V6", "", "nav"), ("V6", "V7", "fichier", "nav"), ("V6", "V8", "…", "overlay"),
    ("V6", "V6c", "fin de la page", "state"), ("V6c", "V6", "un similaire", "nav"), ("V6c", "V16", "Tout voir", "nav"), ("V16", "V6", "document", "nav"),
    ("V16", "V16b", "coché", "state"), ("V16b", "V9b", "Déplacer", "overlay"), ("V16b", "V10b", "Supprimer", "overlay"), ("V9b", "V16", "déplacés", "nav"), ("V10b", "V16", "supprimés", "nav"),
    ("V8", "V9", "Déplacer", "overlay"), ("V8", "V10", "Supprimer", "overlay"), ("V10", "V3", "supprimé", "nav"), ("V1", "V11", "sombre", "state"),
    ("V1", "X1", "sans réseau", "state"), ("V1", "M1", "avatar", "nav"),
    ("V1", "M2", "ligne « Scellé »", "nav"), ("V1", "O2", "session expirée", "nav"),
    ("K1", "K2", "Terminer", "nav"), ("K2", "K4", "Ajouter au coffre", "nav"), ("OS", "K3", "Partager", "overlay"),
    ("K3", "K4", "Ajouter au coffre", "nav"), ("K4", "V3", "", "nav"),
    ("S1", "S2", "+", "nav"), ("S2", "S3", "Ajouter", "nav"), ("S3", "S4", "Connecter", "nav"),
    ("S4", "S3", "source suivante", "nav"), ("S4", "S1", "en arrière-plan", "nav"), ("S1", "S5", "", "nav"),
    ("S1", "S6", "en échec", "nav"), ("S6", "S4", "Mettre à jour et relancer", "nav"), ("S5", "S7", "Retirer", "overlay"),
    ("S5", "S3b", "Ajouter un autre compte", "nav"), ("S2", "S3b", "Autre compte", "nav"), ("S3b", "S4", "Connecter ce compte", "nav"),
    ("S1", "S5c", "un compte", "nav"), ("S5c", "S3b", "Ajouter un autre compte", "nav"), ("S5c", "S7b", "Retirer ce compte", "overlay"),
    ("S1", "M6", "limite", "nav"),
    ("M1", "M2", "", "nav"), ("M1", "M5", "", "nav"), ("M1", "M6", "", "nav"), ("M1", "M7", "", "nav"),
    ("M1", "M8", "", "overlay"), ("M1", "M10", "Aide", "nav"), ("M1", "M12", "Mes données", "nav"), ("M12", "M12b", "Supprimer", "overlay"), ("M2", "M11", "", "nav"), ("M2", "M3", "", "nav"), ("M2", "M4", "", "nav"),
    ("M2", "O6", "kit", "nav"), ("M2", "M5", "tuiles", "nav"), ("M4", "O6", "oublié ?", "nav"),
    ("M6", "M9", "Voir les offres", "nav"), ("M8", "O1", "confirmé", "nav"),
]
DESKTOP = [
    ("DO1", "DO2", "J’ai déjà un coffre", "nav"), ("DO1", "DO3", "Créer mon coffre", "nav"),
    ("DO2", "DO2b", "Ouvrir le coffre", "state"), ("DO2b", "DO2c", "échec", "state"),
    ("DO2", "DO4b", "pas de trousseau", "overlay"), ("DO2b", "DO5", "limite d’appareils", "nav"),
    ("DO2b", "DO9", "1re ouverture ici", "nav"), ("DO5", "DO9", "appareil retiré", "nav"),
    ("DO5", "M9", "Voir les offres", "nav"), ("DO2c", "DO10", "kit de récupération", "nav"), ("DO2", "DO10", "Mot de passe oublié ?", "nav"), ("DO10", "DO10b", "Continuer", "nav"), ("DO10b", "DO9", "Rouvrir mon coffre", "nav"),
    ("DO3", "DO3b", "erreurs", "state"), ("DO3", "DO4b", "pas de trousseau", "overlay"), ("DO3", "DO11", "Continuer", "nav"),
    ("D5", "DO6", "Kit de récupération", "nav"), ("DO6", "DO6b", "Je l’ai noté", "nav"), ("DO6b", "D5", "Terminé", "nav"), ("DO11", "DO7", "Continuer", "nav"), ("DO7", "DO7h", "limite atteinte", "state"), ("DO7", "DO7b", "Connecter n sources", "nav"),
    ("DO7", "DO8", "Passer", "nav"), ("DO7b", "DO7c", "Connecter", "nav"), ("DO7b", "DO7f", "refusé", "state"),
    ("DO7c", "DO7d", "code SMS ou e-mail", "overlay"),
    ("DO7c", "DO7b", "Source suivante", "nav"), ("DO7c", "DO7g", "dernière source", "nav"),
    ("DO7f", "DO7g", "Passer (dernière)", "nav"), ("DO7g", "DO8", "Continuer", "nav"), ("DO8", "SIDE", "", "nav"),
    ("DO9", "SIDE", "Parcourir le coffre", "nav"),
    ("SIDE", "D6", "Tout le coffre", "nav"), ("D6", "T1", "sur tablette", "state"), ("T1", "T2", "portrait", "state"), ("D6", "D1", "dossier", "nav"), ("SIDE", "D1", "dossier", "nav"), ("SIDE", "D10", "+", "overlay"), ("D1", "D10", "Nouveau dossier", "overlay"), ("D1", "D7", "… ou clic droit", "overlay"), ("D7", "D8", "Déplacer…", "overlay"), ("D7", "D9", "Supprimer", "overlay"), ("D9", "D1", "supprimé", "nav"),
    ("SIDE", "D4", "Sources", "nav"), ("D4", "D4b", "Lancer", "state"), ("D4b", "D4", "Arrêter", "state"), ("SIDE", "D5", "pierre (avatar)", "nav"), ("D1", "D2", "", "nav"), ("D2", "D2b", "… ou clic droit", "overlay"), ("D2b", "D2c", "Supprimer", "overlay"), ("D2", "D2c", "⌘⌫", "overlay"), ("D2c", "D2", "document suivant", "nav"), ("D2", "D11", "Tout voir", "nav"), ("D11", "D11b", "Supprimer", "overlay"), ("T1", "D1", "dossier", "nav"),
]

GROUPS = {
    "entry": ("Entrées", "#FFFFFF"), "onb": ("Accueil et accès (O)", "#DCE8E0"), "vault": ("Coffre (V, X)", "#E8DCC6"),
    "capture": ("Capture (K)", "#F0E1CF"), "sources": ("Sources (S)", "#D4E6E3"),
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

# Cross-checks, so the PNGs, the map and the spec's Screens tables can't drift apart.
import re
pngs = {p.name.split("-")[0] for p in SCREENS.glob("*.png")}
screens = {nid for nid, (_, group) in NODES.items() if group not in ("entry", "dentry", "todo", "dtodo")}
spec = (HERE.parent.parent / "design-system.md").read_text()
rows = set(re.findall(r"^\| ([A-Z]+\d+[a-z]?) \|", spec, re.M))
problems = [f"{label}: {sorted(ids)}" for label, ids in [
    ("PNG without a map node", pngs - screens), ("map node without a PNG", screens - pngs),
    ("PNG without a row in design-system.md", pngs - rows), ("row in design-system.md without a PNG", rows - pngs)] if ids]
if problems:
    raise SystemExit("\n".join(problems))
print(f"{len(pngs)} screens: every PNG has a map node and a spec row")
