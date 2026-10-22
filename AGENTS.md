# QuietHome

To install, configure or remove QuietHome on a TV, follow `.claude/skills/install-quiethome/SKILL.md`.

Build with `sh build.sh`. To release, push a tag such as `v0.2.0`: `.github/workflows/build.yml` builds, signs and publishes `quiethome.apk`. All drawing happens in `Stage` on a 1920 × 1080 design canvas; each screen is a `Screen` subclass in `src/`.
