# QuietHome

To install, configure or remove QuietHome on a TV, follow `.claude/skills/install-quiethome/SKILL.md`.

Build with `sh build.sh`. To release, push a tag such as `v0.2.0`: `.github/workflows/build.yml` builds, signs and publishes `quiethome.apk`. All drawing happens in `Stage` on a 1920 × 1080 design canvas; each screen is a `Screen` subclass in `src/`.

Keep the README and its screenshots current in the same commit as the change. When a change alters what the user sees, retake the affected images in `docs/images/` on the emulator: 1600 px wide JPEG, with stub apps that use real brand package names (the List, Dock, All apps, Settings and button picker screens).
