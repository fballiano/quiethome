# QuietHome

To install, configure or remove QuietHome on a TV, follow `.claude/skills/install-quiethome/SKILL.md`.

Build with `sh build.sh`. To release, push a tag such as `v0.2.0`: `.github/workflows/build.yml` builds, signs and publishes `quiethome.apk`. All drawing happens in `Stage` on a 1920 × 1080 design canvas; each screen is a `Screen` subclass in `src/`.

Keep the README and its screenshots current in the same commit as the change. When a change alters what the user sees, retake the affected images in `docs/images/` on the emulator: 1600 px wide JPEG, with stub apps that use real brand package names (the List, Dock, All apps, Settings and button picker screens).

## Conventions

- New source files start with the two SPDX lines: `SPDX-FileCopyrightText: 2026 Fabrizio Balliano` and `SPDX-License-Identifier: GPL-3.0-or-later`.
- Write the README, `SECURITY.md` and other docs in ASD-STE100 Simplified Technical English.
- Smoothness comes first: `Stage` draws only while something animates. Keep per-frame work small (no blurred shadows on text that scales) and do slow work, such as the icon filter, off the main thread. Animation speeds are `Ui.FOCUS_MS` and `Ui.SCROLL_MS`.
- Brand marks are Simple Icons (CC0) or our own drawings, each listed in `tools/icons/SOURCES.md` and `THIRD_PARTY_NOTICES.md`; regenerate with `python3 -I tools/svg2vector.py`. Brands that Disney owns get no mark (`mark` 0): Home then shows the app's own icon through the one-colour filter, like every app without a mark.

## Testing

- `adb shell input keyevent` skips accessibility services, so it cannot test the Home button or button mapping; an injected Home opens the stock launcher. Use the real remote or a virtual HID keyboard (`adb shell hid`).
- The emulator shows much less memory than a real TV (28 MB against 60 MB on a Xiaomi MiTV). Check memory claims on a real TV.
- A TV with a release build accepts only release-signed APKs. Test a local build there only after an uninstall, which loses the settings.
- Screenshot stub apps are manifest-only APKs: `aapt2 link` a manifest with `android:hasCode="false"` and one LEANBACK_LAUNCHER activity, under the real package name, signed with `key.jks`.
