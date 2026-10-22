---
name: install-quiethome
description: Installs and configures QuietHome on an Android TV or Google TV over ADB. Use when the user wants QuietHome on a TV, wants to change its configuration or button mapping on a TV, or wants to remove it and go back to the stock launcher.
---

# Install QuietHome on a TV

You work over ADB on the user's TV. The user holds the remote: some steps need them to press a button or confirm a prompt on the TV. Ask, then wait for their answer.

Keep a **recovery record** from step 2 onwards: the facts that let you put the TV back as it was. Show it to the user at the end.

## Steps

### 1. Connect

1. Ask for the TV's IP address (TV Settings → Network) or a USB cable.
2. If developer options are off, tell the user: Settings → System (or Device Preferences) → About → select **Build** seven times. Then turn on **USB debugging** in Developer options. Google TV and Chromecast also need **Wireless debugging**: run `adb pair <ip>:<pairing-port>` with the code the TV shows, then `adb connect <ip>:<port>`.
3. Otherwise run `adb connect <ip>:5555`. If `adb devices` says `unauthorized`, ask the user to select **Allow** on the TV.

Done when `adb devices` lists the TV as `device`. With several devices, pass `-s <serial>` to every command below.

### 2. Check the TV and record its state

```sh
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell pm list features | grep -c leanback
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1
adb shell settings get secure enabled_accessibility_services
```

QuietHome needs SDK 26 or higher and the `leanback` feature (a TV). If either check fails, stop and tell the user.

Done when the recovery record holds: model, Android version, SDK, the current Home activity, and the current accessibility services value (`null` means none).

### 3. Get the APK

Use the latest release, which GitHub builds and signs with the release key:

```sh
curl -fL -o quiethome.apk https://github.com/fballiano/quiethome/releases/latest/download/quiethome.apk
```

Build it instead (`sh build.sh`, which writes `out/quiethome.apk`) only when the user asks for their own changes, or the download fails. A local build has a different signing key from the releases.

Done when the APK file exists.

### 4. Install

```sh
adb install -r quiethome.apk
```

If the output contains `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, the installed QuietHome has a different signing key (a local build). Tell the user that its settings will be lost, and ask before you run `adb uninstall io.github.fballiano.quiethome` and install again.

Done when the output ends with `Success`.

### 5. Make QuietHome the home screen

Ask the user to confirm first: this changes what Home shows.

```sh
adb shell cmd role add-role-holder --user 0 android.app.role.HOME io.github.fballiano.quiethome   # SDK 29+
adb shell cmd package set-home-activity io.github.fballiano.quiethome/.Home
adb shell input keyevent KEYCODE_HOME
sleep 2
adb shell dumpsys activity activities | grep -E 'ResumedActivity' | head -1
```

If the resumed activity is `io.github.fballiano.quiethome/.Home`, Home works without the service. On most Android TV and Google TV devices the stock launcher still wins, because it is a system app with a higher priority. Step 6 fixes that.

### 6. Turn on the QuietHome service

The service gives QuietHome the Home button, button mapping and "When the TV turns on". Grant QuietHome the right to turn it on itself, then open QuietHome:

```sh
adb shell pm grant io.github.fballiano.quiethome android.permission.WRITE_SECURE_SETTINGS
adb shell am start -n io.github.fballiano.quiethome/.Home -a android.intent.action.MAIN -c android.intent.category.HOME
sleep 3
adb shell settings get secure enabled_accessibility_services
```

QuietHome appends its service to that setting, so the services that were already on stay on. It also turns the service on again after a force stop.

If `pm grant` fails or the service is not in the list, QuietHome shows its setup screen on the TV. Ask the user to select **Open Accessibility settings**, then **QuietHome**, and turn it on.

`adb shell input keyevent` cannot test the Home button: injected keys skip accessibility services. Ask the user to open any app and press Home on the remote.

Done when the setting lists `io.github.fballiano.quiethome/io.github.fballiano.quiethome.RemoteButtons` and the user confirms that Home on the remote opens QuietHome.

### 7. Configure

Ask the user what they want. Offer the defaults and change only what they choose. Read the current state first:

- Configuration: `adb shell am broadcast -n io.github.fballiano.quiethome/.Config --ez dump true`. The JSON after `data=` uses the keys in **Configuration**. If QuietHome was already installed, add it to the recovery record.
- Apps: `adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER`
- HDMI inputs: QuietHome logs them each time it reads the app list. Open QuietHome once, run `adb shell "am broadcast -n io.github.fballiano.quiethome/.Config --es config '{}'"`, then `adb logcat -d -s QuietHome | grep 'HDMI input'`. Each line gives the id to use (for example `hdmi:HW5`) and the TV's name for it. No lines means the TV reports no HDMI inputs.

Send everything in one configuration. See **Configuration** below for the keys and the quoting.

Ask whether the TV only shows a box on one HDMI port (for example an Nvidia Shield). If so, set `startOn` to that input's id: QuietHome then opens it at boot and on each wake.

For remote buttons (for example a Netflix key), the user maps them on the TV: Settings → Button mapping → Add a button, press the button, choose the app.

Done when `adb logcat -d -s QuietHome | tail -5` shows `Configuration applied` after your last command, and no `Ignoring` line.

### 8. Verify and hand over

```sh
adb shell input keyevent KEYCODE_HOME
adb exec-out screencap -p > quiethome.png
```

Look at the screenshot. Check that Home shows the chosen apps, layout and background. Then give the user the recovery record and the matching commands from **Recovery**.

Done when the screenshot matches the chosen configuration and the user has the recovery commands.

## Configuration

```sh
adb shell "am broadcast -n io.github.fballiano.quiethome/.Config --es config '{\"layout\":\"dock\",\"home\":[\"com.netflix.ninja\",\"hdmi:HW5\"]}'"
```

Wrap the whole remote command in double quotes. Without them the TV's shell expands `{a,b}` and QuietHome receives broken JSON.

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
| `home` | Up to nine package names or input ids, in order |
| `inputNames` | Input id to a name; an empty name restores the TV's name |
| `buttons` | Android keycode to a package, an input id, `action.none`, `action.home` or `action.settings`; an empty value removes it |

## Safety

- Change only QuietHome's own state, the Home role and the accessibility value. Leave system apps enabled and installed.
- A force stop turns the QuietHome service off. With the permission from step 6, QuietHome turns it on again the next time it opens.
- Give SharedPreferences a second to save before you restart anything after a configuration.

## Recovery

Use the values from the recovery record.

```sh
# QuietHome's configuration back to the recorded JSON (only when QuietHome was installed before)
adb shell am broadcast -n io.github.fballiano.quiethome/.Config --es config "'<recorded JSON>'"
# Home button back to Android
adb shell "am broadcast -n io.github.fballiano.quiethome/.Config --es config '{\"homeKey\":\"system\"}'"
# Stop the service self-repair, then set accessibility services back to the recorded value ("null" means: delete the setting)
adb shell pm revoke io.github.fballiano.quiethome android.permission.WRITE_SECURE_SETTINGS
adb shell settings put secure enabled_accessibility_services "<recorded value>"
adb shell settings delete secure enabled_accessibility_services   # when the recorded value was null
# Home back to the recorded launcher
adb shell cmd package set-home-activity <recorded home activity>
# Remove QuietHome completely (Home falls back to another launcher)
adb uninstall io.github.fballiano.quiethome
```
