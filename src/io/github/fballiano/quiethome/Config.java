// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Configuration from a computer. The manifest guards this receiver with the DUMP permission,
 * which ADB's shell holds and ordinary apps do not:
 * adb shell "am broadcast -n io.github.fballiano.quiethome/.Config --es config '{\"layout\":\"dock\"}'"
 * With --ez dump true, the result of the broadcast is the current configuration, in the same format.
 */
public final class Config extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String json = intent.getStringExtra("config");
        boolean dump = intent.getBooleanExtra("dump", false);
        if (json == null) {
            if (dump) dump(context);
            else Log.w(Home.TAG, "Ignoring a configuration without the config extra");
            return;
        }
        try {
            apply(new Prefs(context), new JSONObject(json));
        } catch (JSONException | NumberFormatException e) {
            Log.w(Home.TAG, "Ignoring a configuration that is not valid: " + e.getMessage());
            return;
        }
        Log.i(Home.TAG, "Configuration applied");
        // A running Home reads everything again (this also logs the HDMI inputs); otherwise it reads it at start.
        Home home = Home.shown();
        if (home != null) home.configChanged();
        if (dump) dump(context);
    }

    /** am broadcast prints the result data, so the computer gets the configuration on one line. */
    private void dump(Context context) {
        try {
            setResultData(read(new Prefs(context)).toString());
        } catch (JSONException e) {
            Log.w(Home.TAG, "Cannot write the configuration: " + e.getMessage());
        }
    }

    /** Every key that apply() accepts. Home is left out while it was never saved: each TV then picks its own defaults. */
    static JSONObject read(Prefs prefs) throws JSONException {
        JSONObject config = new JSONObject();
        for (String key : Prefs.CHOICES.keySet()) config.put(key, prefs.get(key));
        config.put(Prefs.WALLPAPER, prefs.get(Prefs.WALLPAPER));
        config.put(Prefs.START_ON, prefs.get(Prefs.START_ON));
        List<String> home = prefs.home();
        if (home != null) config.put("home", new JSONArray(home));
        JSONObject names = new JSONObject();
        for (String key : new TreeSet<>(prefs.sp.getAll().keySet())) {
            if (key.startsWith(Prefs.INPUT_NAME)) names.put(key.substring(Prefs.INPUT_NAME.length()), prefs.sp.getString(key, ""));
        }
        config.put("inputNames", names);
        JSONObject buttons = new JSONObject();
        for (Map.Entry<Integer, String> b : prefs.buttons().entrySet()) buttons.put(String.valueOf(b.getKey()), b.getValue());
        config.put("buttons", buttons);
        return config;
    }

    /** Saves each known key. A choice outside its allowed values is skipped with a warning. */
    private static void apply(Prefs prefs, JSONObject config) throws JSONException {
        for (Iterator<String> keys = config.keys(); keys.hasNext(); ) {
            String key = keys.next();
            boolean choice = Prefs.CHOICES.containsKey(key) || Prefs.WALLPAPER.equals(key) || Prefs.START_ON.equals(key);
            if (choice && !prefs.set(key, config.getString(key))) Log.w(Home.TAG, "Ignoring " + key + ": not an allowed value");
        }
        JSONArray home = config.optJSONArray("home");
        if (home != null) {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < home.length() && ids.size() < Home.MAX_HOME; i++) ids.add(home.getString(i));
            prefs.setHome(ids);
        }
        JSONObject names = config.optJSONObject("inputNames");
        if (names != null) {
            for (Iterator<String> ids = names.keys(); ids.hasNext(); ) {
                String id = ids.next();
                prefs.setInputName(id, names.getString(id));
            }
        }
        JSONObject buttons = config.optJSONObject("buttons");
        if (buttons != null) {
            for (Iterator<String> codes = buttons.keys(); codes.hasNext(); ) {
                String code = codes.next();
                prefs.setButton(Integer.parseInt(code), buttons.getString(code));
            }
        }
    }
}
