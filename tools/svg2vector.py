#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Fabrizio Balliano
# SPDX-License-Identifier: GPL-3.0-or-later

"""Converts the monochrome icons in tools/icons/*.svg to Android vector drawables in res/drawable/.

Run it again after you add or change an icon: python3 -I tools/svg2vector.py
Only the SVG features that the icons use are supported: a viewBox, <path>, <rect> and <circle>,
a fill or a stroke on the root element, a stroke-width on a shape, and fill-rule="evenodd".
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "tools", "icons")
OUT = os.path.join(ROOT, "res", "drawable")
NS = "{http://www.w3.org/2000/svg}"

# Brand marks and interface outlines that the app uses. Other files in tools/icons are ignored.
ICONS = ["primevideo", "youtube", "netflix", "jellyfin", "twitch", "appletv", "stremio",
         "spotify", "hbomax", "tubi", "plex", "nuvio", "smarttube", "kodi", "vlc", "emby", "crunchyroll",
         "paramountplus", "dazn", "mubi", "steamlink", "youtubemusic", "youtubekids", "tidal", "deezer",
         "console", "photo", "apps", "settings", "tvsettings", "home"]


def num(value):
    text = f"{float(value):g}"
    return "0" if text == "-0" else text


def rect_path(e):
    x, y = float(e.get("x", 0)), float(e.get("y", 0))
    w, h = float(e.get("width")), float(e.get("height"))
    r = min(float(e.get("rx", e.get("ry", 0))), w / 2, h / 2)
    if r <= 0:
        return f"M{num(x)},{num(y)}h{num(w)}v{num(h)}h{num(-w)}Z"
    return (f"M{num(x + r)},{num(y)}h{num(w - 2 * r)}a{num(r)},{num(r)} 0 0 1 {num(r)},{num(r)}"
            f"v{num(h - 2 * r)}a{num(r)},{num(r)} 0 0 1 {num(-r)},{num(r)}h{num(-(w - 2 * r))}"
            f"a{num(r)},{num(r)} 0 0 1 {num(-r)},{num(-r)}v{num(-(h - 2 * r))}a{num(r)},{num(r)} 0 0 1 {num(r)},{num(-r)}Z")


def circle_path(e):
    cx, cy, r = float(e.get("cx")), float(e.get("cy")), float(e.get("r"))
    return (f"M{num(cx - r)},{num(cy)}a{num(r)},{num(r)} 0 1 0 {num(2 * r)},0"
            f"a{num(r)},{num(r)} 0 1 0 {num(-2 * r)},0Z")


def convert(name):
    root = ET.parse(os.path.join(SRC, name + ".svg")).getroot()
    minx, miny, w, h = (float(v) for v in re.split(r"[ ,]+", root.get("viewBox").strip()))
    stroke = root.get("stroke")
    stroked = stroke is not None and stroke != "none"
    paths = []
    for e in root.iter():
        tag = e.tag.replace(NS, "")
        if tag == "path":
            d = e.get("d")
        elif tag == "rect":
            d = rect_path(e)
        elif tag == "circle":
            d = circle_path(e)
        else:
            continue
        even_odd = e.get("fill-rule", root.get("fill-rule")) == "evenodd"
        paths.append((" ".join(d.split()), even_odd, e.get("stroke-width")))
    if not paths:
        sys.exit(f"{name}: no shapes")
    # The intrinsic size keeps the icon's aspect ratio; the app scales it to the box it needs.
    scale = 24 / max(w, h)
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- Generated from tools/icons/{name}.svg by tools/svg2vector.py. Do not edit. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{num(w * scale)}dp" android:height="{num(h * scale)}dp"',
        f'    android:viewportWidth="{num(w)}" android:viewportHeight="{num(h)}">',
        f'    <group android:translateX="{num(-minx)}" android:translateY="{num(-miny)}">',
    ]
    for d, even_odd, width in paths:
        attrs = [f'android:pathData="{d}"']
        if stroked:
            attrs += ['android:strokeColor="#FFFFFFFF"',
                      f'android:strokeWidth="{num(width or root.get("stroke-width", "1"))}"',
                      f'android:strokeLineCap="{root.get("stroke-linecap", "butt")}"',
                      f'android:strokeLineJoin="{root.get("stroke-linejoin", "miter")}"']
        else:
            attrs.append('android:fillColor="#FFFFFFFF"')
            if even_odd:
                attrs.append('android:fillType="evenOdd"')
        lines.append("        <path " + "\n            ".join(attrs) + " />")
    lines += ["    </group>", "</vector>", ""]
    with open(os.path.join(OUT, f"icon_{name}.xml"), "w") as f:
        f.write("\n".join(lines))


os.makedirs(OUT, exist_ok=True)
for icon in ICONS:
    convert(icon)
print(f"Wrote {len(ICONS)} vector drawables to res/drawable/")
