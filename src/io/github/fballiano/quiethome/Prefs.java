// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All saved choices. Values are short strings, so Settings and the ADB configuration can treat them alike. */
final class Prefs {
    static final String FILE = "quiethome";

    static final String LAYOUT = "layout";         // list | dock
    static final String BG = "bg";                 // photo | light | dark
    static final String WALLPAPER = "wallpaper";   // a Wallpapers id
    static final String ROTATION = "rotation";     // fixed | open | 5 | 15 | 60
    static final String CLOCK = "clock";           // 24 | 12
    static final String FONT = "font";             // manrope | geist | newsreader
    static final String FOCUS = "focus";           // type | glass
    static final String MATERIAL = "material";     // soft | frost | clear
    static final String APPS = "apps";             // both | text | icons
    static final String CONTROLS = "controls";     // both | text | icons
    static final String HOME_KEY = "homeKey";      // quiethome | system
    static final String HOME = "home";             // app and input ids, comma separated
    static final String LAST = "last";             // the last Home app opened
    static final String BUTTONS = "buttons";       // keycode=target;keycode=target
    static final String INPUT_NAME = "inputName."; // + input id
    static final String START_ON = "startOn";      // an app or input id, or "" for Home

    /** The keys that Settings and the ADB configuration may change, with their allowed values (first is the default). */
    static final Map<String, String[]> CHOICES = new LinkedHashMap<>();

    static {
        CHOICES.put(LAYOUT, new String[]{"list", "dock"});
        CHOICES.put(BG, new String[]{"photo", "light", "dark"});
        CHOICES.put(ROTATION, new String[]{"fixed", "open", "5", "15", "60"});
        CHOICES.put(CLOCK, new String[]{"24", "12"});
        CHOICES.put(FONT, new String[]{"manrope", "geist", "newsreader"});
        CHOICES.put(FOCUS, new String[]{"type", "glass"});
        CHOICES.put(MATERIAL, new String[]{"soft", "frost", "clear"});
        CHOICES.put(APPS, new String[]{"both", "text", "icons"});
        CHOICES.put(CONTROLS, new String[]{"both", "text", "icons"});
        CHOICES.put(HOME_KEY, new String[]{"quiethome", "system"});
    }

    final SharedPreferences sp;
    private final Context context;

    Prefs(Context context) {
        this.context = context;
        sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    String get(String key) {
        String value = sp.getString(key, null);
        if (START_ON.equals(key)) return value == null ? "" : value;
        String[] allowed = CHOICES.get(key);
        if (allowed != null && (value == null || !Arrays.asList(allowed).contains(value))) return fallback(key, allowed);
        if (WALLPAPER.equals(key) && Wallpapers.find(value) == null) return Wallpapers.ALL[0].id;
        return value;
    }

    private String fallback(String key, String[] allowed) {
        if (CLOCK.equals(key)) return DateFormat.is24HourFormat(context) ? "24" : "12";
        return allowed[0];
    }

    boolean is(String key, String value) {
        return value.equals(get(key));
    }

    /** Returns false when the value is not allowed for the key. */
    boolean set(String key, String value) {
        String[] allowed = CHOICES.get(key);
        if (allowed != null && !Arrays.asList(allowed).contains(value)) return false;
        if (WALLPAPER.equals(key) && Wallpapers.find(value) == null) return false;
        sp.edit().putString(key, value).apply();
        return true;
    }

    /** The saved Home order, or null when it was never saved. */
    List<String> home() {
        String raw = sp.getString(HOME, null);
        if (raw == null) return null;
        List<String> out = new ArrayList<>();
        for (String id : raw.split(",")) if (!id.trim().isEmpty() && !out.contains(id.trim())) out.add(id.trim());
        return out;
    }

    void setHome(List<String> ids) {
        sp.edit().putString(HOME, join(ids, ",")).apply();
    }

    String inputName(String id) {
        return sp.getString(INPUT_NAME + id, null);
    }

    void setInputName(String id, String name) {
        if (name == null || name.trim().isEmpty()) sp.edit().remove(INPUT_NAME + id).apply();
        else sp.edit().putString(INPUT_NAME + id, name.trim()).apply();
    }

    /** Remote buttons: keycode to app or input id, in keycode order. */
    static Map<Integer, String> buttons(SharedPreferences sp) {
        Map<Integer, String> out = new java.util.TreeMap<>();
        String raw = sp.getString(BUTTONS, "");
        for (String pair : raw.split(";")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            try {
                int code = Integer.parseInt(pair.substring(0, eq).trim());
                String target = pair.substring(eq + 1).trim();
                if (code > 0 && !target.isEmpty()) out.put(code, target);
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    Map<Integer, String> buttons() {
        return buttons(sp);
    }

    void setButton(int code, String target) {
        Map<Integer, String> all = buttons();
        if (target == null || target.isEmpty()) all.remove(code);
        else all.put(code, target);
        StringBuilder out = new StringBuilder();
        for (Map.Entry<Integer, String> e : all.entrySet()) {
            if (out.length() > 0) out.append(';');
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        sp.edit().putString(BUTTONS, out.toString()).apply();
    }

    static String join(List<String> items, String separator) {
        StringBuilder out = new StringBuilder();
        for (String item : items) {
            if (out.length() > 0) out.append(separator);
            out.append(item);
        }
        return out.toString();
    }
}
