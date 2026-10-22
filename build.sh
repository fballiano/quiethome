#!/bin/sh
# Builds out/quiethome.apk with the Android SDK alone: no Gradle, no libraries.
# Needs a JDK (17 or newer) and the Android SDK with a platform (34+) and build-tools.
#   macOS: brew install openjdk && brew install --cask android-commandlinetools
#          sdkmanager "platforms;android-36" "build-tools;36.1.0"
# Choose other versions with PLATFORM=android-35 BUILD_TOOLS=35.0.0 sh build.sh
set -e
cd "$(dirname "$0")"

# Release builds set both from the tag (see .github/workflows/build.yml).
VERSION_CODE=${VERSION_CODE:-1}
VERSION_NAME=${VERSION_NAME:-0.1.0-dev}

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

# Resources and assets. Fonts stay uncompressed, so Android maps them from the APK instead of copying them.
"$BT/aapt2" compile --dir res -o out/res.zip
"$BT/aapt2" link -o out/base.apk -I "$JAR" --manifest AndroidManifest.xml -A assets -0 ttf -0 jpg \
  --java out/gen --min-sdk-version 26 --target-sdk-version 34 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" out/res.zip

# Java 8 source against android.jar; d8 turns the classes into dex.
# shellcheck disable=SC2046 # one argument per file, on purpose
javac -nowarn -Xlint:-options -encoding UTF-8 -source 8 -target 8 -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" \
  -d out/classes $(find src out/gen -name '*.java')
# shellcheck disable=SC2046
"$BT/d8" --min-api 26 --release --lib "$JAR" --output out/dex $(find out/classes -name '*.class')

cp out/base.apk out/unsigned.apk
(cd out/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 out/unsigned.apk out/aligned.apk

# Without KEYSTORE, a local test key (key.jks) is made on the first build. Keep it: an update installs
# over an earlier APK only with the same key. Releases on GitHub use their own key (KEYSTORE secrets).
KS=${KEYSTORE:-key.jks}
PASS=${KEYSTORE_PASS:-quiethome}
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass "$PASS" -keypass "$PASS" -alias quiethome \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=QuietHome" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --out out/quiethome.apk out/aligned.apk
echo "Built out/quiethome.apk ($(du -h out/quiethome.apk | cut -f1))"
