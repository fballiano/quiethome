# QuietHome

To install, configure or remove QuietHome on a TV, follow `.claude/skills/install-quiethome/SKILL.md`.

Build with `sh build.sh`. To release, push an annotated tag such as `v1.2.0` whose message is the release notes: `git tag -a v1.2.0 --cleanup=verbatim -F notes.md`. Write the notes like the earlier releases: `### Added`, `### Changed`, `### Fixed` and `### Removed`, with only the changes that users see. `--cleanup=verbatim` keeps the `###` lines. `.github/workflows/build.yml` builds, signs and publishes `quiethome.apk` with these notes. A tag without a message stops the release. There are two builds. The GitHub build is the APK with Settings → Update QuietHome. The Play build (`STORE=play sh build.sh`, `out/quiethome.aab`, needs bundletool) has no update and no network, because Play rejects apps that update themselves. Code for one build only goes in `src-github/` or `src-play/`, with the same class in both. Manifest lines for the GitHub build only go between `<!-- github -->` and `<!-- /github -->`. CI builds both; upload the bundle from the tag's run to the Play Console. All drawing happens in `Stage` on a 1920 × 1080 design canvas; each screen is a `Screen` subclass in `src/`.

Keep the README and its screenshots current in the same commit as the change. When a change alters what the user sees, retake the affected images in `docs/images/` on the emulator: 1600 px wide JPEG, with stub apps that use real brand package names (the List, Dock, All apps, Settings and button picker screens).

## Conventions

- All text that the user sees is in `res/values/strings.xml`. Add each new or changed string to every `res/values-*/strings.xml` in the same commit, and keep each translation as short as the English.
- New source files start with the two SPDX lines: `SPDX-FileCopyrightText: 2026 Fabrizio Balliano` and `SPDX-License-Identifier: GPL-3.0-or-later`.
- Write the README, `SECURITY.md` and other docs in ASD-STE100 Simplified Technical English.
- Smoothness comes first: `Stage` draws only while something animates. Keep per-frame work small (no blurred shadows on text that scales) and do slow work, such as the icon filter, off the main thread. Animation speeds are `Ui.FOCUS_MS` and `Ui.SCROLL_MS`.
- Google Play asks for a disclosure and consent before an app's accessibility service is turned on. `SetupScreen` is that disclosure: open it, not the Accessibility settings, from every place that turns the service on.
- Brand marks are Simple Icons (CC0) or our own drawings, each listed in `tools/icons/SOURCES.md` and `THIRD_PARTY_NOTICES.md`; regenerate with `python3 -I tools/svg2vector.py`. Brands that Disney owns get no mark (`mark` 0): Home then shows the app's own icon through the one-colour filter, like every app without a mark.

## Testing

- `adb shell input keyevent` skips accessibility services, so it cannot test the Home button or button mapping; an injected Home opens the stock launcher. Use the real remote or a virtual HID keyboard (`adb shell hid`).
- The emulator does not count graphics memory, so it shows much less than a real TV (on a Xiaomi Google TV box, an empty app already uses 20 MB). Check memory claims on a real TV, after a restart, and after `adb shell cmd package compile -m speed-profile -f io.github.fballiano.quiethome`: a fresh install runs uncompiled and uses a few MB more.
- A TV with a release build accepts only release-signed APKs. On the maintainer's computer, sign a test build with the release key and a higher version code, so it installs over the release and keeps the settings: `KEYSTORE=~/.config/quiethome/release.jks KEYSTORE_PASS="$(cat ~/.config/quiethome/release-password.txt)" VERSION_CODE=<n> VERSION_NAME=<x.y.z-dev> sh build.sh`.
- Screenshot stub apps are manifest-only APKs: `aapt2 link` a manifest with `android:hasCode="false"` and one LEANBACK_LAUNCHER activity, under the real package name, signed with `key.jks`.
