"""Builds labelled contact sheets from ../screens into ../sheets (one per area).

usage: python3 sheets.py   (run render.py first; needs Pillow)
"""
import glob
import os
import pathlib
import re

from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).resolve().parent
SCREENS = HERE.parent / "screens"
OUT = HERE.parent / "sheets"
OUT.mkdir(exist_ok=True)

LABELS = {
    "O1": "Bienvenue", "O2": "Connexion", "O3": "Créer le coffre", "O4": "Stockage indisponible",
    "O5": "Trop d’appareils", "O6": "Kit de récupération", "O6b": "Vérifier le kit", "O7": "Choisir ses sources", "O8": "Notifications",
    "O7b": "Limite de l’offre", "O9": "Ouverture", "O10": "Récupérer mon coffre", "O10b": "Nouveau mot de passe", "V1": "Coffre", "V2": "Coffre en remplissage", "V3": "Catégorie", "V4": "Recherche",
    "V5": "Résultats", "V6": "Document", "V6b": "Document sombre", "V7": "Lecteur", "V8": "Actions", "V8b": "Actions sombres", "V9": "Reclasser",
    "V10": "Cartes par type", "V11": "Coffre sombre", "K1": "Scanner", "K2": "Vérifier le scan",
    "K3": "Depuis une autre app", "K4": "Ajout en cours", "R1": "Arrivées", "R1c": "Code expiré", "R1b": "Arrivées sombre", "R2": "Tout est rangé",
    "R3": "Historique", "R4": "Notification de code", "S1": "Sources", "S2": "Catalogue", "S3": "Connecter",
    "S4": "Récupération", "S5": "Détail", "S6": "En échec", "S6b": "Site changé", "S8": "Vérification demandée", "S7": "Retirer", "M1": "Moi", "M2": "Sécurité", "M2b": "Sécurité sombre",
    "M3": "Ce que voit le serveur", "M4": "Vérifier le mot de passe", "M5": "Appareils", "M6": "Offre et usage",
    "M7": "Apparence et langue", "M8": "Se déconnecter", "M9": "Offres", "M10": "Aide", "M11": "Changer le mot de passe", "M12": "Mes données", "M12b": "Supprimer mon compte", "M8b": "Se déconnecter, sombre", "X1": "Hors ligne", "X2": "Texte à 200 %", "D1": "Bureau — Catégorie",
    "DO1": "Bienvenue", "DO2": "Connexion", "DO2b": "Connexion en cours", "DO2c": "Échec de connexion",
    "DO3": "Créer le coffre", "DO3b": "Créer — erreurs",
    "DO4b": "Trousseau absent", "DO5": "Trop d’appareils", "DO6": "Kit de récupération", "DO6b": "Vérifier le kit",
    "DO7": "Choisir ses sources", "DO7b": "Connecter une source", "DO7c": "Récupération en cours",
    "DO7d": "Code reçu par SMS ou e-mail", "DO7f": "Mot de passe refusé",
    "DO7g": "Sources connectées", "DO7h": "Limite de l’offre", "DO8": "Notifications", "DO9": "Ouverture", "DO10": "Récupérer mon coffre", "DO10b": "Nouveau mot de passe",
    "D2": "Bureau — Document", "D3": "Bureau — Arrivées", "D4": "Bureau — Sources", "T1": "Tablette — Coffre", "T2": "Tablette portrait", "D5": "Bureau — Moi", "D6": "Bureau — Tout le coffre",
}
GROUPS = {"onboarding": "O", "coffre": "V", "capture-arrivees": "KR", "sources": "S", "moi": "MX"}
BG, INK, GAP, PAD, LABEL_H = "#E4E2DD", "#3d3a33", 28, 36, 50


def font():
    for candidate in ["/usr/share/fonts/opentype/inter/Inter-SemiBold.otf", "DejaVuSans-Bold.ttf"]:
        try:
            return ImageFont.truetype(candidate, 26)
        except OSError:
            continue
    return ImageFont.load_default()


def screen_id(path):
    return re.match(r"([A-Z]+\d+[a-z]?)(?=-)", os.path.basename(path)).group(1)


def order(path):
    letters, number, suffix = re.match(r"([A-Z]+)(\d+)([a-z]?)", screen_id(path)).groups()
    return letters, int(number), suffix


def sheet(name, files, width, per_row):
    ims = [Image.open(f).convert("RGB") for f in files]
    ims = [i.resize((width, int(i.height * width / i.width)), Image.LANCZOS) for i in ims]
    rows = [list(range(i, min(i + per_row, len(ims)))) for i in range(0, len(ims), per_row)]
    w = PAD * 2 + per_row * width + (per_row - 1) * GAP
    h = PAD * 2 + sum(max(ims[i].height for i in r) + LABEL_H + GAP for r in rows) - GAP
    canvas = Image.new("RGB", (w, h), BG)
    draw, f, y = ImageDraw.Draw(canvas), font(), PAD
    for r in rows:
        x = PAD
        for i in r:
            canvas.paste(ims[i], (x, y))
            sid = screen_id(files[i])
            draw.text((x + 4, y + ims[i].height + 12), f"{sid} · {LABELS[sid]}", fill=INK, font=f)
            x += width + GAP
        y += max(ims[i].height for i in r) + LABEL_H + GAP
    canvas.save(OUT / f"{name}.png", optimize=True)
    print("wrote", name)


for name, prefixes in GROUPS.items():
    files = sorted((f for f in glob.glob(str(SCREENS / "*.png"))
                    if os.path.basename(f)[0] in prefixes and not os.path.basename(f).startswith("V10")), key=order)
    sheet(name, files, 390, 5)
sheet("bureau-acces", sorted(glob.glob(str(SCREENS / "DO*.png")), key=order), 720, 2)
sheet("bureau-tablette", sorted(glob.glob(str(SCREENS / "D[0-9]*.png")), key=order) + [str(SCREENS / "T1-tablette-coffre.png"), str(SCREENS / "T2-tablette-portrait.png")], 720, 2)
