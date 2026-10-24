#!/bin/sh
# SPDX-FileCopyrightText: 2026 Fabrizio Balliano
# SPDX-License-Identifier: GPL-3.0-or-later

# Takes the Home images of the README on the emulator with the screenshot stub apps (see AGENTS.md).
# For each setup below, it applies the configuration over ADB and writes:
#   out/screenshots/<name>.png   the plain 1920 × 1080 screen, also for the Play listing
#   docs/images/<name>.jpg       the screen on the TV in docs/images/room.jpg, 1600 px wide
# and docs/images/home.webp, which shows the "hero" setups in turn on the TV in the whole room.
# At the end, the configuration of the emulator is as before. With REUSE=1, it takes no screenshots
# and makes the images again from out/screenshots, for example after a change to the room or a crop.
# Needs adb, ImageMagick and img2webp (brew install imagemagick webp).
set -e
REUSE=${REUSE:-}
cd "$(dirname "$0")/.."

OUT=out/screenshots
ROOM=docs/images/room.jpg
# The TV screen in room.jpg, inside its bezel. The TV is 16:9.
SCREEN=549x307+749+294
# Gallery images show the TV and the top of the console; the hero shows the whole console.
CLOSE=826x428+612+226
WIDE=1390x662+330+205

# name | where | configuration. "hero" setups also go in the animation.
SETUPS='
golden|hero|{"wallpaper":"golden","bg":"photo","layout":"list","apps":"both","controls":"both","focus":"glass","material":"soft","font":"manrope","clock":"24","date":"off"}
night|hero|{"wallpaper":"night","bg":"photo","layout":"list","apps":"text","controls":"icons","focus":"type","font":"manrope","clock":"24","date":"on"}
night-dock|hero|{"wallpaper":"night","bg":"photo","layout":"dock","apps":"both","controls":"both","focus":"glass","material":"frost","font":"manrope","clock":"24","date":"off"}
light|hero|{"bg":"light","layout":"list","apps":"both","controls":"text","focus":"type","font":"newsreader","clock":"12","date":"on"}
icons|gallery|{"wallpaper":"cove","bg":"photo","layout":"list","apps":"icons","controls":"icons","focus":"glass","material":"clear","font":"manrope","clock":"24","date":"off"}
dark-dock|gallery|{"bg":"dark","layout":"dock","apps":"both","controls":"text","focus":"glass","material":"soft","font":"geist","clock":"24","date":"on"}
'

# Crops the room, scales the crop to 1600 px wide, then puts the screen in at that scale, so the
# screenshot is resized only once and stays sharp. Usage: place <screen.png> <crop> <out.png>
place() {
  # shellcheck disable=SC2046 # the geometry, as separate numbers
  set -- "$1" "$2" "$3" $(echo "$SCREEN $2" | tr 'x+' '  ' | awk '{
    s = 1600 / $5
    printf "%dx%d %d %d %d", $1 * s + 0.5, $2 * s + 0.5, ($3 - $7) * s + 0.5, ($4 - $8) * s + 0.5, $6 * s + 0.5 }')
  magick "$ROOM" -crop "$2" +repage -resize "1600x$7!" \( "$1" -resize "$4!" \) -geometry "+$5+$6" -composite "$3"
}

# adb reads its input, so each call gets none: in the loop below, it would take the next setups.
config() {
  adb shell "am broadcast -n io.github.fballiano.quiethome/.Config --es config '$1'" < /dev/null > /dev/null
}

mkdir -p "$OUT"
rm -f "$OUT/hero.txt.new"
if [ -z "$REUSE" ]; then
  saved=$(adb shell am broadcast -n io.github.fballiano.quiethome/.Config --ez dump true | sed -n 's/.*data="\(.*\)"$/\1/p')
  [ -n "$saved" ] || { echo "QuietHome does not answer over ADB." >&2; exit 1; }
  trap 'config "$saved"' EXIT
fi

echo "$SETUPS" | while IFS='|' read -r name where json; do
  [ -n "$name" ] || continue
  if [ -z "$REUSE" ]; then
    config "$json"
    adb shell am start -n io.github.fballiano.quiethome/.Home -a android.intent.action.MAIN -c android.intent.category.HOME < /dev/null > /dev/null 2>&1
    # Long enough for the wallpaper and the focus animation.
    sleep 3
    adb exec-out screencap -p < /dev/null > "$OUT/$name.png"
  fi
  place "$OUT/$name.png" "$CLOSE" "$OUT/$name-close.png"
  magick "$OUT/$name-close.png" -strip -interlace JPEG -quality 85 "docs/images/$name.jpg"
  echo "Made $name"
  [ "$where" = hero ] || continue
  place "$OUT/$name.png" "$WIDE" "$OUT/hero-$name.png"
  echo "$name" >> "$OUT/hero.txt.new"
done
mv "$OUT/hero.txt.new" "$OUT/hero.txt"

# The animation: each setup stays 3 s, then fades into the next in 10 steps of 50 ms. Only the
# screen changes between frames, so WebP stores small rectangles and the file stays small. Each
# still frame is a key frame (one in 10 frames): otherwise the small errors of the fade frames add
# up and stay on the screen as blocks.
set --
first=
previous=
for name in $(cat "$OUT/hero.txt") ""; do
  [ -n "$name" ] || name=$first
  [ -n "$first" ] || first=$name
  if [ -n "$previous" ]; then
    for step in 1 2 3 4 5 6 7 8 9; do
      magick "$OUT/hero-$previous.png" "$OUT/hero-$name.png" -compose blend \
        -define compose:args=$((step * 10)) -composite "$OUT/fade-$previous-$step.png"
      set -- "$@" -d 50 "$OUT/fade-$previous-$step.png"
    done
  fi
  [ "$name" = "$first" ] && [ -n "$previous" ] && break
  set -- "$@" -d 3000 "$OUT/hero-$name.png"
  previous=$name
done
img2webp -loop 0 -kmin 9 -kmax 10 -lossy -q 82 -m 6 "$@" -o docs/images/home.webp
echo "Wrote docs/images/home.webp ($(($(wc -c < docs/images/home.webp) / 1024)) KB)"
