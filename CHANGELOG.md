# Changelog

All releases of QuietHome, with the newest first. The release workflow uses the section of a version as the notes of its GitHub release.

## 1.0.1 – 2026-10-22

### Fixed

- Settings: the arrow of a row is now in the vertical centre of the row.
- Home: when the controls show icons only, the name of the focused control no longer touches its Glass plate.

## 1.0.0 – 2026-10-22

### Added

- QuietHome uses the language of the TV. It has 13 translations: Czech, Danish, Dutch, Finnish, French, German, Italian, Norwegian, Polish, Portuguese, Spanish, Swedish and Turkish. Other languages get English.
- Settings → Update QuietHome opens Downloader, where the code 9591315 gets the latest release. If Downloader is not on the TV, it opens Downloader in the Play Store. QuietHome still has no network access.

### Fixed

- Two notes in Settings were too long and ended with "…". They are shorter now.

## 0.5.1 – 2026-10-22

### Added

- Settings shows the installed version under its title.

## 0.5.0 – 2026-10-22

### Added

- After a wake from standby, Google TV shows its own home screen. The QuietHome service now sees when a stock launcher comes to the front, and then it opens QuietHome. For this, the service receives the name of the app in front. It still cannot read the screen.
- When the TV turns on → Show Home now opens QuietHome at boot, also on TVs where the stock launcher has a higher priority.

## 0.4.0 – 2026-10-22

### Changed

- All apps starts with an Edit Home tile. The button picker starts with Default (the button's own job), and When the TV turns on starts with Show Home. Before, these were buttons outside the grid.
- A picker opens on the current choice, which has a Current badge.

## 0.3.2 – 2026-10-22

### Changed

- Settings shows Button mapping on its first page, not under Android TV.
- QuietHome frees old photos at once, not at the next garbage collection. This lowers the memory use.

## 0.3.1 – 2026-10-22

### Changed

- The focus and the lists move faster on every screen.

## 0.3.0 – 2026-10-22

### Added

- OK on an app makes the app dip and spring back. A message, for example "Opening Netflix", shows until the app opens.

### Changed

- The Dock is as wide as its tiles. Six tiles fit with no scrolling. With seven to nine apps, the Dock scrolls.

### Removed

- Photo mode. Home has three controls: All apps, Settings and TV settings.

## 0.2.0 – 2026-10-22

### Added

- Apps without a built-in mark show their own icon as a mark in one colour.
- 12 new brand marks: Kodi, VLC, Emby, Crunchyroll, Paramount+, DAZN, MUBI, Steam Link, YouTube Music, YouTube Kids, Tidal and Deezer.
- A TV settings control (the gear) opens Android's settings.
- The setup screen has an App info button, for the restricted settings of Android 13 and newer.
- Install with the remote only: in Downloader, type the code 9591315.

### Changed

- Disney+ and Hulu show their own app icon, because Disney does not allow its marks in icon sets.

### Security

- Each release APK has a build provenance attestation. The GitHub actions are pinned to commit SHAs. `SECURITY.md` tells how to report a problem.

## 0.1.0 – 2026-10-22

- The first release: a quiet, native home screen for Android TV and Google TV.
