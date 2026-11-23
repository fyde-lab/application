"""Checks that current/screens/ and current/design-system.md's Screens tables describe the same screens.

usage: python3 design/current/check.py   (capture.sh runs it)
Fails unless every screen rendered has a row (its name backticked in a Screens table's first
column) and every row has its three PNGs (compact, medium, expanded).
"""
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).resolve().parent
SIZES = ("compact", "medium", "expanded")

pngs = {}
for png in (HERE / "screens").glob("*.png"):
    name, _, size = png.stem.rpartition("-")
    if size in SIZES:
        pngs.setdefault(name, set()).add(size)

doc = (HERE / "design-system.md").read_text()
screens = re.search(r"^## Screens\n(.*?)(?=^## )", doc, re.S | re.M)
if not screens:
    sys.exit("check.py: design-system.md has no '## Screens' section")
rows = set(re.findall(r"^\|\s*`([a-z0-9-]+)`", screens.group(1), re.M))

errors = [f"{n}: rendered but has no row in design-system.md § Screens" for n in sorted(pngs.keys() - rows)]
errors += [f"{n}: has a row but no PNG in screens/" for n in sorted(rows - pngs.keys())]
errors += [f"{n}: missing {', '.join(sorted(set(SIZES) - s))}" for n, s in sorted(pngs.items()) if s != set(SIZES)]
if errors:
    sys.exit("check.py:\n  " + "\n  ".join(errors))
print(f"check.py: {len(rows)} screens, each with a row and its {len(SIZES)} sizes")
