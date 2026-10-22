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

/**
 * Configuration from a computer. The manifest guards this receiver with the DUMP permission,
 * which ADB's shell holds and ordinary apps do not:
 * adb shell "am broadcast -n io.github.fballiano.quiethome/.Config --es config '{\"layout\":\"dock\"}'"
 */
public final class Config extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String json = intent.getStringExtra("config");
        if (json == null) {
            Log.w(Home.TAG, "Ignoring a configuration without the config extra");
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
