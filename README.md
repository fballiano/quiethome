<div align="center">
<img src="docs/images/logo.svg" width="128" alt="QuietHome" />
<h1>QuietHome</h1>
<p><strong>A quiet home screen for Android TV and Google TV.</strong></p>
<p>
<a href="https://github.com/fballiano/quiethome/releases/latest"><img src="https://img.shields.io/github/v/release/fballiano/quiethome?style=for-the-badge&color=3D5A66&labelColor=1B1F23" alt="Latest release" /></a>
<a href="https://github.com/fballiano/quiethome/releases"><img src="https://img.shields.io/github/downloads/fballiano/quiethome/total?style=for-the-badge&color=3D5A66&labelColor=1B1F23" alt="Downloads" /></a>
<img src="https://img.shields.io/badge/Android%20TV-8.0%2B-3D5A66?style=for-the-badge&labelColor=1B1F23&logo=android&logoColor=white" alt="Android TV 8.0 or later" />
<a href="LICENSE"><img src="https://img.shields.io/badge/licence-GPL--3.0-3D5A66?style=for-the-badge&labelColor=1B1F23" alt="GPL-3.0 licence" /></a>
</p>
<p><a href="https://github.com/fballiano/quiethome/releases/latest"><img src="https://img.shields.io/badge/Download%20for%20Android%20TV-3D5A66?style=for-the-badge&labelColor=3D5A66&logo=data:image/svg%2Bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZmZmIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCI+PHBhdGggZD0iTTQgMTd2MmEyIDIgMCAwIDAgMiAyaDEyYTIgMiAwIDAgMCAyIC0ydi0yIi8+PHBhdGggZD0iTTcgMTFsNSA1bDUgLTUiLz48cGF0aCBkPSJNMTIgNGwwIDEyIi8+PC9zdmc+" alt="Download for Android TV" height="36" /></a></p>
</div>

<table align=center><tr><td align=center>
<strong>If you find my work valuable, please consider sponsoring</strong><br />
<a href="https://github.com/sponsors/fballiano" target=_blank title="Sponsor me on GitHub"><img src="https://img.shields.io/badge/sponsor-30363D?style=for-the-badge&logo=GitHub-Sponsors&logoColor=#white" alt="Sponsor me on GitHub" /></a>
<a href="https://www.buymeacoffee.com/fballiano" target=_blank title="Buy me a coffee"><img src="https://img.shields.io/badge/Buy_Me_A_Coffee-FFDD00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black" alt="Buy me a coffee" /></a>
<a href="https://www.paypal.com/paypalme/fabrizioballiano" target=_blank title="Donate via PayPal"><img src="https://img.shields.io/badge/PayPal-00457C?style=for-the-badge&logo=paypal&logoColor=white" alt="Donate via PayPal" /></a>
</td></tr></table>

---

QuietHome shows your apps, your HDMI inputs, a photograph and the time. It is made for the TV remote.

QuietHome is inspired by [CleanFrame](https://github.com/nthnclrk/CleanFrame), a quiet home screen for Samsung The Frame. It is a native Android app, with no WebView and no libraries (see [Why a native app](#why-a-native-app)). It uses the network only when you ask for an update.

![QuietHome on a TV in a living room: the List and Dock layouts over photographs, then the Light background](docs/images/home.webp)

## What it does

- **List or Dock layout.** Show names, icons or both. Show a 24-hour or 12-hour clock. You can also show the date under the clock.
- **One-colour icons.** Known services use a built-in mark. For every other app, QuietHome turns the app's own icon into a mark in the same colour.
- **Backgrounds.** Choose one of 15 photographs, a light colour or a dark colour. Photos can rotate each time Home opens, or every 5, 15 or 60 minutes.
- **Two settings buttons.** Settings (the sliders) opens QuietHome's settings. TV settings (the gear) opens Android's own settings.
- **All apps.** It shows every TV app and every HDMI input. OK opens the item. Hold OK on an app to close it and free its memory. Hold OK on an HDMI input to rename it. The first tile, **Edit Home**, opens the Apps on Home editor.
- **Your own order.** Hold OK on an app on Home for a short menu: **Move**, **Remove from Home** and **Close app**. For an HDMI input, the last item is **Rename**. In move mode the arrows move the app, OK places it and Back cancels. Settings → Apps on Home adds apps, with up to nine on Home.
- **Input names.** Give an HDMI input a name that you know, for example "Nintendo Switch". Hold OK on the input, select **Rename**, and type the name with the TV keyboard. **Use the TV's name** removes your name. When the name has Switch, Xbox or PlayStation in it, Home shows a game controller mark.
- **Typography and focus.** Choose Manrope, Geist or Newsreader. Choose Type focus or Glass focus, with a Soft, Frost or Clear material.
- **Your language.** QuietHome uses the language of the TV, in 14 languages (see [Languages](#languages)).
- **Updates.** Settings shows the installed version under its title. Settings → Update QuietHome looks for a newer release on GitHub. If there is one, it downloads it, checks it, and starts the installation. Android asks you to confirm. The first time, Android asks you to let QuietHome install apps. After the update, QuietHome opens again.
- **The QuietHome service.** An accessibility service that gives QuietHome the features below. If it is off, QuietHome shows how to turn it on.
  - **Home button.** Home always opens QuietHome, also on TVs that keep their own launcher (for example Google TV). When the stock launcher comes back by itself, for example after the TV wakes from standby, QuietHome replaces it at once. Settings → Android TV → Previous launcher still lets you visit it.
  - **Button mapping.** Press a remote button, for example the Netflix key, and choose what it does: open an app or an HDMI input, open QuietHome or TV settings, or nothing. You do not need a separate button mapper app.
  - **When the TV turns on.** For a TV that only shows a box (for example an Nvidia Shield) on one HDMI port: QuietHome opens that input at boot and each time the TV wakes. It can also open an app. Home still shows QuietHome.
- **Network only for updates.** QuietHome connects to the network only when you select Settings → Update QuietHome, and only to GitHub. It never checks in the background. It has no accounts, no analytics and no ads.

## Make it yours

Each image is one combination of settings. Settings shows a live preview of Home while you change them.

<table>
<tr>
<td width="50%"><img src="docs/images/golden.jpg" alt="List layout with names and icons, Glass focus, over the Golden hour photo" /><br /><sub><b>List</b> · names and icons · Glass focus · Golden hour</sub></td>
<td width="50%"><img src="docs/images/night.jpg" alt="List layout with names only, controls as icons and the date, over the Desert stars photo" /><br /><sub><b>List</b> · names only · controls as icons · date · Desert stars</sub></td>
</tr>
<tr>
<td><img src="docs/images/night-dock.jpg" alt="Dock layout with Glass focus in the Frost material, over the Desert stars photo" /><br /><sub><b>Dock</b> · Glass focus · Frost material · Desert stars</sub></td>
<td><img src="docs/images/light.jpg" alt="List layout on the Light background, in Newsreader, with a 12-hour clock and the date" /><br /><sub><b>Light</b> background · Newsreader · 12-hour clock · date</sub></td>
</tr>
<tr>
<td><img src="docs/images/icons.jpg" alt="List layout with icons only, Clear material, over the Quiet cove photo" /><br /><sub><b>List</b> · icons only · Clear material · Quiet cove</sub></td>
<td><img src="docs/images/dark-dock.jpg" alt="Dock layout on the Dark background, in Geist, with the date" /><br /><sub><b>Dark</b> background · Dock · Geist · date</sub></td>
</tr>
</table>

| All apps | Settings, with a live preview |
| --- | --- |
| ![All apps: every app and HDMI input in a grid](docs/images/allapps.jpg) | ![Settings: the wallpaper list and a preview of Home](docs/images/settings.jpg) |

## Why a native app

QuietHome starts from the idea of CleanFrame: the List and Dock layouts, the fonts, the focus styles and the glass. CleanFrame is a web app for Tizen, and its author made it for one Samsung The Frame. Android TV needs a different app, so QuietHome is a new implementation. We made these decisions:

- **Native views, no WebView.** QuietHome draws everything in one Android view. It has no HTML, no JavaScript and no libraries, so Android starts no browser engine for it. The result is low memory use and smooth focus on slow TVs (see [Memory and speed](#memory-and-speed)).
- **A documented API for the Home button.** CleanFrame takes the Home button with undocumented native helpers for one model and one firmware. QuietHome uses an accessibility service, a standard Android API. It needs no root and no code for one TV brand.
- **The remote is the only tool.** You can install, configure and update QuietHome with the remote. You do not need developer mode, a computer or your own signing certificate.

| | CleanFrame | QuietHome |
| --- | --- | --- |
| System | Tizen (Samsung) | Android TV and Google TV 8.0 and newer |
| Made with | HTML, CSS and JavaScript | Native Android views, no libraries |
| Made for | The author's Samsung The Frame | Any Android TV (see [Compatibility](#compatibility)) |
| Home button | Undocumented native helpers | The QuietHome service (accessibility API) |
| Install | Developer mode, Samsung tools, your own certificate | Downloader and the remote, or ADB |
| Updates | Sign and install again | Settings → Update QuietHome |
| Button mapping | No | Yes |
| HDMI inputs | No | Show, rename, open when the TV turns on |
| App icons | Marks for a fixed list of services | A one-colour mark for every app |
| Glass | 60 images, made before installation | One 120 × 68 blurred copy of the photo |
| Languages | English | 14 |

CleanFrame is a good base for a Samsung TV. Its README says that it is made for one TV, and it asks you to adapt it to yours. QuietHome is for any Android TV, with no changes.

## Languages

QuietHome shows its text in the language of the TV. For a language that is not in this list, it uses English.

<table align=center>
<tr><td align=center>🇬🇧<br />English</td><td align=center>🇨🇿<br />Čeština</td><td align=center>🇩🇰<br />Dansk</td><td align=center>🇩🇪<br />Deutsch</td><td align=center>🇪🇸<br />Español</td><td align=center>🇫🇷<br />Français</td><td align=center>🇮🇹<br />Italiano</td></tr>
<tr><td align=center>🇳🇱<br />Nederlands</td><td align=center>🇳🇴<br />Norsk</td><td align=center>🇵🇱<br />Polski</td><td align=center>🇧🇷<br />Português</td><td align=center>🇫🇮<br />Suomi</td><td align=center>🇸🇪<br />Svenska</td><td align=center>🇹🇷<br />Türkçe</td></tr>
</table>

To add a language, see [Translate QuietHome](#translate-quiethome).

## Compatibility

QuietHome runs on Android TV and Google TV 8.0 (API 26) and newer, which includes Android TV 12 and newer. It has no code for one TV brand.

| Tested on | Result |
| --- | --- |
| Xiaomi Google TV box (MiTV-AFMU0), Google TV 14, a real device | Memory and speed (see [Memory and speed](#memory-and-speed)) |
| Android TV 12 emulator (API 31), 1080p | All screens, button mapping, Home button, hold to close, move mode, opening an app at boot and on wake, the service setup screen and its self-repair |
| Google TV 14 emulator (API 34), 1080p | All screens, button mapping, Home button, hold to close, the date, renaming an input with the TV keyboard, reading the configuration |

Reports from other TVs are welcome.

## Memory and speed

QuietHome draws everything in one custom view. It decodes one photo at screen size and keeps a 120 × 68 blurred copy for the glass effect. It does not blur live. It draws frames only while something moves. Text that scales with focus has no blurred shadow, because a shadow must be drawn again at each step of the scale.

Measured on a Xiaomi Google TV box (MiTV-AFMU0, Google TV 14, 1080p interface on a 4K output). Each memory figure is the PSS about 15 seconds after the app restarts, from five samples.

| Measurement | Result |
| --- | --- |
| An empty app: only a window, no code | about 20 MB |
| QuietHome, flat background | about 31 MB |
| QuietHome, photo background | about 41 MB |
| The Google TV launcher, in front | about 115 MB |
| QuietHome cold start | 420 to 445 ms |
| QuietHome frame time while focus moves | median 13 ms, 90th percentile 16 ms |

On a real device, the graphics driver runs inside each app that draws, and the graphics chip uses the normal RAM. That is why an empty app already uses about 20 MB. The photo adds about 10 MB: the photo itself in graphics memory, and the driver's work to draw it. A new app is a few MB larger until Android compiles it in the background.

The emulator does not count graphics memory, so it shows lower numbers:

| Android TV 12 emulator, host GPU, 1080p | Result |
| --- | --- |
| QuietHome, photo background | about 29 MB |
| QuietHome, flat background | about 25 MB |
| The stock Android TV launcher, in front | about 45 MB |
| QuietHome frame time while focus moves (median) | about 5 ms, no janky frames |
| QuietHome cold start | about 230 ms |

Measure on your own TV with `adb shell dumpsys meminfo io.github.fballiano.quiethome`.

QuietHome keeps the photo while another app is in front, so Home comes back at once, with no new decode.

## Install with the remote only

You need no computer for this. It works on Android TV and Google TV.

1. Install **Downloader** by AFTVnews from the Play Store on the TV.
2. Open Downloader. Allow it to install apps when the TV asks. If the TV does not ask, go to Settings → Apps → Security & restrictions → Unknown sources (the path changes a little between TVs) and turn on Downloader.
3. In Downloader's address field, type the code **9591315**, then select **Go**. The code always gives the latest release. If the code does not work, type this address instead:

   ```text
   github.com/fballiano/quiethome/releases/latest/download/quiethome.apk
   ```

4. Select **Install** when Downloader has the file, then **Open**.
5. QuietHome shows how to turn on its service. Select **Agree and open settings**, then **QuietHome**, and turn it on. If it is grey or says "Restricted setting", go back, select **App info**, allow restricted settings, and try again.

Press Home: QuietHome opens. To install a new version later, select Settings → Update QuietHome. Your settings stay. Settings shows the installed version under its title.

## Install with a coding agent

Open this repository in a coding agent with shell access, such as Claude Code or Codex. Turn on developer options on the TV (see below). Then ask:

```text
Install QuietHome on my TV at 192.168.1.50.
```

The agent follows [the install skill](.claude/skills/install-quiethome/SKILL.md). It connects over ADB, checks that the TV is supported, builds and installs the APK, and makes QuietHome the home screen. If the TV keeps its own launcher, it turns on the Home button option. Then it asks how you want Home to look. It records the TV's state first and gives you the commands to go back. You only press OK on the remote when the TV asks.

You can also ask it to change the configuration later, or to remove QuietHome.

## Install

Download `quiethome.apk` from the [latest release](https://github.com/fballiano/quiethome/releases/latest), or build it (see below).

You need a computer with [ADB](https://developer.android.com/tools/adb) and debugging enabled on the TV. To enable it, go to Settings → System (or Device Preferences) → About. Select Build seven times. Then turn on Developer options → USB debugging.

```sh
curl -fLO https://github.com/fballiano/quiethome/releases/latest/download/quiethome.apk
adb connect <tv-address>:5555
adb install -r quiethome.apk
adb shell cmd role add-role-holder --user 0 android.app.role.HOME io.github.fballiano.quiethome
adb shell cmd package set-home-activity io.github.fballiano.quiethome/.Home
adb shell pm grant io.github.fballiano.quiethome android.permission.WRITE_SECURE_SETTINGS
adb shell input keyevent KEYCODE_HOME
```

Release APKs are all signed with the same key, so a new release installs over an older one and keeps your settings. An APK that you build yourself has a different key: to change between the two, uninstall QuietHome first.

Before you install, write down the name of your current launcher:

```sh
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

### The QuietHome service

The `pm grant` command above lets QuietHome turn its service on by itself. Then QuietHome keeps it on, also after a force stop, which makes Android turn accessibility services off.

Without that command, QuietHome shows a setup screen when its service is off. Select **Agree and open settings**, select **QuietHome** and turn it on. On some TVs, the path is Settings → System → Accessibility → QuietHome.

On Android 13 and newer, an update through an installer screen, for example with Downloader, gives QuietHome restricted settings. Android then turns the service off and does not let QuietHome turn it on again, also with the `pm grant` command. QuietHome then shows its setup screen: select **App info**, allow restricted settings, and come back. Settings → Update QuietHome installs updates in a different way and does not cause this.

On most Android TV and Google TV devices, the stock launcher is a system app with a higher priority, so it keeps the Home button. The QuietHome service takes the Home button back, without disabling system apps. Long presses of Home do not reach the TV while it does this. To give Home back to Android, choose Settings → Android TV → Home button → **Android**.

### Going back

- On the TV, open Settings → Android TV → Previous launcher.
- In Settings → Android TV → Home button, choose **Android**.
- To stop the self-repair of the service: `adb shell pm revoke io.github.fballiano.quiethome android.permission.WRITE_SECURE_SETTINGS`.
- From a computer, make your previous launcher the home screen again:

  ```sh
  adb shell cmd package set-home-activity <previous-launcher-activity>
  ```

- Uninstall QuietHome: `adb uninstall io.github.fballiano.quiethome`.

## Button mapping

1. Go to Settings → Button mapping. The QuietHome service must be on.
2. Select **Add a button**. Press the remote button, then choose what it does.
3. To change a mapping, select the button in the list. To remove it, choose **Default**: the button then does its own job again.

![Choosing what a button does: Default, Nothing, QuietHome, TV settings, or an app](docs/images/picker.jpg)

You cannot map these keys: arrows, OK, Back, Home, volume, mute and power. They always keep their usual job.

Android lets an accessibility service see key presses. QuietHome acts only on Home and on the buttons that you map. All other keys pass through. The service also gets one event, "window changed", which gives the package name of the app in front. QuietHome uses it only to see when the stock launcher's home screen comes back. The service cannot read the screen, and it stores and sends nothing.

## When the TV turns on

Go to Settings → Android TV → When the TV turns on, and choose the HDMI input of your box (or an app). QuietHome opens it at boot and each time the TV wakes from standby. At boot the TV can list its inputs a few seconds late, so QuietHome tries again for about 30 seconds. To show Home again at start, choose **Show Home**, the first tile in the same screen. With Show Home, QuietHome opens itself at boot, so it is the first screen even on a TV whose stock launcher has a higher priority. Press Home on the remote to reach QuietHome at any time.

This needs the QuietHome service: Android lets an app open another screen from the background only with a service like this.

## Configure from a computer

You can set preferences with ADB. QuietHome applies them at once when it is running. Put the whole command in double quotes. If you do not, the TV's shell changes the JSON.

```sh
adb shell "am broadcast -n io.github.fballiano.quiethome/.Config --es config '{
  \"layout\": \"dock\", \"bg\": \"photo\", \"wallpaper\": \"peaks\", \"clock\": \"24\",
  \"home\": [\"com.netflix.ninja\", \"com.google.android.youtube.tv\", \"hdmi:HW5\"],
  \"inputNames\": {\"hdmi:HW5\": \"Nintendo Switch\"},
  \"buttons\": {\"191\": \"com.netflix.ninja\", \"183\": \"action.none\"}
}'"
```

| Key | Values |
| --- | --- |
| `layout` | `list`, `dock` |
| `bg` | `photo`, `light`, `dark` |
| `wallpaper` | `misty`, `cove`, `forest`, `valley`, `night`, `peaks`, `bridge`, `ridge`, `golden`, `summit`, `goldcoast`, `sydney`, `amber`, `mint`, `alpine` |
| `rotation` | `fixed`, `open`, `5`, `15`, `60` |
| `clock` | `24`, `12` |
| `date` | `off`, `on` |
| `font` | `manrope`, `geist`, `newsreader` |
| `focus` | `type`, `glass` |
| `material` | `soft`, `frost`, `clear` |
| `apps`, `controls` | `both`, `text`, `icons` |
| `homeKey` | `quiethome`, `system` |
| `startOn` | A package name or HDMI input id to open when the TV turns on; `""` shows Home |
| `home` | A list of up to nine package names or input ids |
| `inputNames` | Input id to name. An empty name restores the TV's name. |
| `buttons` | Android keycode to a package name, an input id, `action.none`, `action.home` or `action.settings`. An empty value removes the mapping. |

HDMI input ids look like `hdmi:HW5`. To see them, send an empty configuration (`--es config '{}'`), then run `adb logcat -d -s QuietHome | grep 'HDMI input'`.

### Read the configuration

Add `--ez dump true` to see the current configuration. QuietHome gives it as the result of the broadcast, on one line, with the same keys:

```sh
adb shell am broadcast -n io.github.fballiano.quiethome/.Config --ez dump true
```

```text
Broadcast completed: result=0, data="{"layout":"dock","bg":"photo", … ,"buttons":{"191":"com.netflix.ninja"}}"
```

`home` is in the result only after you change the apps on Home. Without it, each TV chooses its own apps.

To copy the configuration to a second TV, save it to a file, then send the file to the second TV (`-s` chooses the TV):

```sh
adb -s <first-tv> shell am broadcast -n io.github.fballiano.quiethome/.Config --ez dump true \
  | sed -n 's/^Broadcast completed: result=0, data="\(.*\)"$/\1/p' > quiethome.json
adb -s <second-tv> shell am broadcast -n io.github.fballiano.quiethome/.Config --es config "'$(cat quiethome.json)'"
```

A configuration adds and changes values. It does not remove input names or button mappings that are not in it. If a name has an apostrophe ('), the second command fails: change the name on the TV instead.

To map a remote button, the easiest way is on the TV: Settings → Button mapping → Add a button. The `buttons` key needs the Android keycode, which the TV's key layout decides.

## Build from source

You need a JDK (17 or newer) and the Android SDK with a platform (34 or newer) and build-tools. Gradle is not necessary.

```sh
sh build.sh
```

The script writes `out/quiethome.apk`. The first build creates a local test key, `key.jks`. Keep this file: an update installs over an earlier build only with the same key. To use your own key, set `KEYSTORE` and `KEYSTORE_PASS`.

To build the Google Play version, install bundletool (`brew install bundletool`) and run:

```sh
STORE=play sh build.sh
```

The script then writes `out/quiethome.aab`, an Android App Bundle. This version does not have Settings → Update QuietHome, because Google Play updates the app. It also does not have the `INTERNET` and `REQUEST_INSTALL_PACKAGES` permissions. All other code is the same.

GitHub Actions ([`.github/workflows/build.yml`](.github/workflows/build.yml)) runs the same script for the two versions on every push and pull request. It keeps the APK and the App Bundle as build artifacts. To publish a release, push an annotated version tag. Its message is the release notes:

```sh
git tag -a v1.2.0 --cleanup=verbatim -F notes.md
git push origin v1.2.0
```

The workflow then builds the APK with that version, signs it with the release key from the repository secrets `KEYSTORE_BASE64` and `KEYSTORE_PASS`, and publishes it as a GitHub Release. If the tag has no message, the workflow stops. Releases are immutable, and each APK has a build provenance attestation. To check that GitHub built an APK from this repository:

```sh
gh attestation verify quiethome.apk --repo fballiano/quiethome
```

The App Bundle for Google Play is not in the release. Download it from the artifacts of the same workflow run, and upload it in the Play Console. You can also send it with [fastlane](https://fastlane.tools): put it in `out/quiethome.aab` and run `fastlane release`. The Google Play listing (the text, the graphics and the TV screenshots) is in `fastlane/metadata/android/`. `fastlane metadata` sends it to Google Play. Fastlane needs a Google Play service account key in `~/.config/quiethome/play-service-account.json`.

[PRIVACY.md](PRIVACY.md) tells what data QuietHome keeps and sends. [SECURITY.md](SECURITY.md) tells how to report a security problem, what rights the app has, and how releases are made and protected.

| Location | Contents |
| --- | --- |
| `src/` | The Java source: one activity (`Home`), one view (`Stage`) and one class for each screen |
| `src-github/`, `src-play/` | The code for only one version: Settings → Update QuietHome is only in the GitHub version |
| `res/` | Strings, the theme, the icon and the vector marks |
| `assets/` | The photographs and the fonts |
| `tools/` | The source SVG marks and `svg2vector.py`, which converts them to vector drawables, and `screenshots.sh`, which takes the Home images of this README |
| `build.sh` | The build: aapt2, javac, d8, zipalign and apksigner, or bundletool for Google Play |
| `bundle.json` | The App Bundle configuration: the fonts and photographs stay uncompressed |
| `.github/workflows/` | The build on GitHub, and the releases |
| `fastlane/` | The Google Play listing, and the fastlane lanes that send it and the App Bundle |

## Translate QuietHome

All the text that QuietHome shows is in `res/values/strings.xml`. Each language has a copy in its own folder, for example `res/values-it/strings.xml` for Italian.

To add a language:

1. Copy `res/values/strings.xml` to `res/values-<code>/strings.xml`. Use the Android language code, for example `values-ro` for Romanian.
2. Remove the strings that have `translatable="false"`.
3. Translate each string. Keep `%1$s` and `%1$d`: Android puts a name or a number there.
4. Keep the text short. Each line has a fixed width on the TV, and longer text ends with "…".
5. Use only letters that the three fonts have. Manrope, Geist and Newsreader have the Latin letters. Geist has no Greek letters, and Newsreader has no Cyrillic or Greek letters.
6. Build the APK and look at each screen in the new language.

## Support QuietHome

QuietHome is free and has no ads. If it makes your TV better, you can support its work through [GitHub Sponsors](https://github.com/sponsors/fballiano), [Buy Me a Coffee](https://buymeacoffee.com/fballiano) or [PayPal](https://paypal.me/fabrizioballiano).

## Licence

Copyright (C) 2026 Fabrizio Balliano.

QuietHome is free software under the [GNU General Public License, version 3 or any later version](LICENSE) (GPL-3.0-or-later). You can share and change it. If you share it, changed or not, you must also make its source available under the same licence. QuietHome comes with no warranty.

Fonts, photographs and brand marks keep their own terms. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

QuietHome is an independent project. It has no affiliation with Google or any streaming service. Brand marks identify the services that own them.
