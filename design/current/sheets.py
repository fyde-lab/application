"""Builds labelled contact sheets of current/screens/ into current/sheets/: one per area and size.

usage: python3 design/current/sheets.py   (capture.sh runs it; needs Pillow)
"""
import pathlib

from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).resolve().parent
SCREENS = HERE / "screens"
OUT = HERE / "sheets"
SIZES = ["compact", "medium", "expanded"]
# Area of a screen, by the first prefix its name starts with (in this order).
AREAS = [
    ("sign-in", ["welcome", "login", "register", "onboarding-", "connect-source", "source-fetching",
                 "session-limit", "opening", "insecure-storage", "already-running", "password-meter"]),
    ("vault", ["vault-"]),
    ("sources", ["sources-tab"]),
    ("settings", ["settings-"]),
    ("design-system", ["design-system"]),
]
THUMB_H = {"compact": 600, "medium": 600, "expanded": 450}
COLUMNS = {"compact": 6, "medium": 5, "expanded": 3}
BG, INK, GAP, LABEL_H = "#E4E2DD", "#3d3a33", 32, 44


def area_of(name):
    for area, prefixes in AREAS:
        if any(name.startswith(p) for p in prefixes):
            return area
    return "other"


def font():
    for candidate in [HERE.parents[1] / "shared/src/commonMain/composeResources/font/public_sans_semibold.ttf", "DejaVuSans-Bold.ttf"]:
        try:
            return ImageFont.truetype(str(candidate), 22)
        except OSError:
            continue
    return ImageFont.load_default()


def main():
    OUT.mkdir(exist_ok=True)
    for old in OUT.glob("*.png"):
        old.unlink()
    label_font = font()
    for size in SIZES:
        groups = {}
        for png in sorted(SCREENS.glob(f"*-{size}.png")):
            groups.setdefault(area_of(png.name[: -len(f"-{size}.png")]), []).append(png)
        for area, pngs in groups.items():
            thumbs = []
            for png in pngs:
                img = Image.open(png).convert("RGB")
                h = THUMB_H[size]
                thumbs.append((png.name[: -len(f"-{size}.png")], img.resize((round(img.width * h / img.height), h))))
            cols = min(COLUMNS[size], len(thumbs))
            w = max(t.width for _, t in thumbs)
            h = THUMB_H[size] + LABEL_H
            rows = -(-len(thumbs) // cols)
            sheet = Image.new("RGB", (GAP + cols * (w + GAP), GAP + rows * (h + GAP)), BG)
            draw = ImageDraw.Draw(sheet)
            for i, (name, thumb) in enumerate(thumbs):
                x, y = GAP + (i % cols) * (w + GAP), GAP + (i // cols) * (h + GAP)
                draw.text((x, y + 6), name, fill=INK, font=label_font)
                sheet.paste(thumb, (x, y + LABEL_H))
            target = OUT / f"{area}-{size}.png"
            sheet.save(target, optimize=True)
            print("wrote", target.relative_to(HERE.parent))


main()
