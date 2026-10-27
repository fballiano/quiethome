#!/bin/sh
# SPDX-FileCopyrightText: 2026 Fabrizio Balliano
# SPDX-License-Identifier: GPL-3.0-or-later

# Builds out/quiethome.apk with the Android SDK alone: no Gradle, no libraries.
# Needs a JDK (17 or newer) and the Android SDK with a platform (36+) and build-tools.
#   macOS: brew install openjdk && brew install --cask android-commandlinetools
#          sdkmanager "platforms;android-36" "build-tools;36.1.0"
# Choose other versions with PLATFORM=android-37 BUILD_TOOLS=37.0.0 sh build.sh
#
# STORE=play builds out/quiethome.aab for Google Play instead: an App Bundle without Settings → Update
# QuietHome, because Play updates the app. It also needs bundletool: brew install bundletool, or
# BUNDLETOOL=/path/to/bundletool-all.jar
set -e
cd "$(dirname "$0")"

STORE=${STORE:-github}
case $STORE in
  github | play) ;;
  *) echo "STORE is github or play." >&2; exit 1 ;;
esac
if [ "$STORE" = play ]; then
  [ -z "${BUNDLETOOL:-}" ] || bundletool() { java -jar "$BUNDLETOOL" "$@"; }
  command -v bundletool > /dev/null || { echo "bundletool not found. Install it, or set BUNDLETOOL." >&2; exit 1; }
fi

# Release builds set both from the tag (see .github/workflows/build.yml).
VERSION_CODE=${VERSION_CODE:-1}
# A local build is named after the last tag and commit, for example 0.5.0-2-gd91b0fe.
VERSION_NAME=${VERSION_NAME:-$(git describe --tags --always --dirty 2>/dev/null | sed 's/^v//')}
VERSION_NAME=${VERSION_NAME:-dev}

SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
[ -n "$SDK" ] || for d in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" /opt/homebrew/share/android-commandlinetools; do
  [ -d "$d" ] && SDK=$d && break
done
[ -n "$SDK" ] || { echo "Android SDK not found. Set ANDROID_HOME." >&2; exit 1; }
PLATFORM=${PLATFORM:-$(ls "$SDK/platforms" 2>/dev/null | grep -E '^android-[0-9]+$' | sort -t- -k2 -n | tail -1)}
BUILD_TOOLS=${BUILD_TOOLS:-$(ls "$SDK/build-tools" 2>/dev/null | sort -V | tail -1)}
JAR=$SDK/platforms/$PLATFORM/android.jar
BT=$SDK/build-tools/$BUILD_TOOLS
[ -f "$JAR" ] && [ -x "$BT/aapt2" ] || { echo "Install a platform and build-tools in $SDK." >&2; exit 1; }
echo "Using $PLATFORM and build-tools $BUILD_TOOLS"

rm -rf out
mkdir -p out/gen out/classes out/dex

# The Play build has no update: no lines from <!-- github --> to <!-- /github -->, no src-github.
if [ "$STORE" = play ]; then
  sed '/<!-- github/,/<!-- \/github -->/d' AndroidManifest.xml > out/AndroidManifest.xml
else
  cp AndroidManifest.xml out/AndroidManifest.xml
fi

# Resources and assets. Fonts stay uncompressed, so Android maps them from the APK instead of copying them.
"$BT/aapt2" compile --dir res -o out/res.zip
link() {
  "$BT/aapt2" link -I "$JAR" --manifest out/AndroidManifest.xml -A assets \
    --java out/gen --min-sdk-version 26 --target-sdk-version 36 \
    --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" "$@" out/res.zip
}
# A bundle needs the resources in proto format; bundle.json keeps the fonts uncompressed there.
if [ "$STORE" = play ]; then link --proto-format -o out/base.zip; else link -0 ttf -0 jpg -o out/base.apk; fi

# Java 8 source against android.jar; d8 turns the classes into dex.
# shellcheck disable=SC2046 # one argument per file, on purpose
javac -nowarn -Xlint:-options -encoding UTF-8 -source 8 -target 8 -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" \
  -d out/classes $(find src "src-$STORE" out/gen -name '*.java')
# shellcheck disable=SC2046
"$BT/d8" --min-api 26 --release --lib "$JAR" --output out/dex $(find out/classes -name '*.class')

# Without KEYSTORE, a local test key (key.jks) is made on the first build. Keep it: an update installs
# over an earlier APK only with the same key. Releases on GitHub use their own key (KEYSTORE secrets).
KS=${KEYSTORE:-key.jks}
PASS=${KEYSTORE_PASS:-quiethome}
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass "$PASS" -keypass "$PASS" -alias quiethome \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=QuietHome" >/dev/null 2>&1

# The bundle module has the manifest and the dex in their own folders. Play makes the APKs from the
# bundle and signs them with the app signing key; this key is the upload key.
if [ "$STORE" = play ]; then
  mkdir -p out/module/manifest out/module/dex
  (cd out/module && unzip -q ../base.zip && mv AndroidManifest.xml manifest/)
  cp out/dex/classes.dex out/module/dex/
  (cd out/module && zip -q -r ../module.zip .)
  bundletool build-bundle --modules=out/module.zip --config=bundle.json --output=out/quiethome.aab
  jarsigner -keystore "$KS" -storepass "$PASS" out/quiethome.aab "${KEY_ALIAS:-quiethome}" > /dev/null
  echo "Built out/quiethome.aab ($(du -h out/quiethome.aab | cut -f1))"
  exit 0
fi

cp out/base.apk out/unsigned.apk
(cd out/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 out/unsigned.apk out/aligned.apk

"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --out out/quiethome.apk out/aligned.apk
echo "Built out/quiethome.apk ($(du -h out/quiethome.apk | cut -f1))"
