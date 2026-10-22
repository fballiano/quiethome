// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The QuietHome service. An accessibility service that filters key events receives each key
 * before the system and the app in front, so it can give a remote button (for example a Netflix
 * key) a new job, turn it off, or make Home open QuietHome. Keys without a mapping pass through.
 * It also receives the window-changed event, only to see when a stock launcher's home screen
 * comes back (after a wake from standby, for example) and to show QuietHome instead. It cannot
 * read the screen.
 */
public final class RemoteButtons extends AccessibilityService {
    interface Capture {
        void captured(int code);
    }

    /** Whether the service is connected now. Settings reads it. */
    static volatile boolean running;
    /** Set by Previous launcher: the stock launcher may stay in front until QuietHome shows again. */
    static volatile boolean visitingOtherLauncher;

    /** A stock launcher can report its window more than once as it appears; one replacement is enough. */
    private static final long REPLACE_GAP_MS = 1500;
    /** The service starts again within seconds of an update; later starts are not about the update. */
    private static final long UPDATE_WINDOW_MS = 2 * 60_000;

    // A capture gives the next mappable key to Settings instead of its usual job. Key events and
    // Settings both run on the main thread, so these fields need no lock.
    private static Capture capture;
    private static long captureUntil;

    /** Keys this service took on key down. It also takes their repeats and key up, so the TV never sees half a press. */
    private final SparseBooleanArray taken = new SparseBooleanArray();
    private SharedPreferences store;
    private Catalog catalog;
    private Map<Integer, String> mapping;
    private boolean ownHomeKey;
    /** The home activities of the other launchers, as "package/class". */
    private final Set<String> otherHomes = new HashSet<>();
    private long lastReplaced;

    private final SharedPreferences.OnSharedPreferenceChangeListener onChange = (sp, key) -> reload();

    @Override
    protected void onServiceConnected() {
        store = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE);
        catalog = new Catalog(this, new Prefs(this));
        reload();
        for (ResolveInfo r : catalog.otherLaunchers()) {
            // The window event names the real activity; the HOME entry can be an alias of it.
            String target = r.activityInfo.targetActivity;
            otherHomes.add(r.activityInfo.packageName + "/" + (target != null ? target : r.activityInfo.name));
        }
        store.registerOnSharedPreferenceChangeListener(onChange);
        running = true;
        StartOn.register(this);
        Log.i(Home.TAG, "Button mapping on, " + mapping.size() + " assigned");
        returnAfterUpdate();
    }

    /**
     * An update ends the process with the service, and Android then shows the stock launcher, which
     * has a higher priority. The service starts again a moment later and too late to see it come.
     * If Home was on screen, or the update came from Settings, it opens Home again.
     */
    private void returnAfterUpdate() {
        boolean wanted = store.getBoolean(Prefs.IN_FRONT, false) || store.getBoolean(Prefs.UPDATING, false);
        store.edit().putBoolean(Prefs.UPDATING, false).apply();
        if (!wanted || !ownHomeKey || Home.shown() != null) return;
        try {
            long updated = getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime;
            if (System.currentTimeMillis() - updated > UPDATE_WINDOW_MS) return;
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        Log.i(Home.TAG, "Updated: back to QuietHome");
        catalog.launch(Catalog.ACTION_HOME);
    }

    private void reload() {
        mapping = Prefs.buttons(store);
        ownHomeKey = !"system".equals(store.getString(Prefs.HOME_KEY, "quiethome"));
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            if (!taken.get(code)) return false;
            taken.delete(code);
            if (code == KeyEvent.KEYCODE_HOME) catalog.launch(Catalog.ACTION_HOME);
            return true;
        }
        if (taken.get(code)) return true;
        if (capture != null && isMappable(code)) {
            Capture c = capture;
            capture = null;
            if (SystemClock.uptimeMillis() > captureUntil) return false;
            taken.put(code, true);
            c.captured(code);
            return true;
        }
        if (code == KeyEvent.KEYCODE_HOME && ownHomeKey) {
            // Home opens QuietHome when it is released, like the system Home.
            taken.put(code, true);
            return true;
        }
        String target = mapping.get(code);
        if (target == null) return false;
        taken.put(code, true);
        if (Catalog.isHdmi(target) && catalog.get(target) == null) catalog.load();
        String problem = catalog.launch(target);
        if (problem != null) Log.w(Home.TAG, name(this, code) + " → " + target + ": " + problem);
        return true;
    }

    /** A stock launcher's home screen came to the front, for example after a wake: show QuietHome instead. */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !ownHomeKey || visitingOtherLauncher) return;
        CharSequence pkg = event.getPackageName(), cls = event.getClassName();
        if (pkg == null || cls == null || !otherHomes.contains(pkg + "/" + cls)) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastReplaced < REPLACE_GAP_MS) return;
        lastReplaced = now;
        Log.i(Home.TAG, "Replacing " + pkg + " with QuietHome");
        catalog.launch(Catalog.ACTION_HOME);
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public boolean onUnbind(Intent intent) {
        running = false;
        capture = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        running = false;
        if (store != null) store.unregisterOnSharedPreferenceChangeListener(onChange);
        super.onDestroy();
    }

    /** Navigation, OK, Back, Home, volume and power keep their jobs, so the remote always works. */
    static boolean isMappable(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_UNKNOWN:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN_LEFT:
            case KeyEvent.KEYCODE_DPAD_DOWN_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_TV_POWER:
            case KeyEvent.KEYCODE_SLEEP:
            case KeyEvent.KEYCODE_WAKEUP:
                return false;
            default:
                return true;
        }
    }

    /** Gives the next mappable key, pressed within timeoutMs, to the capture. Call on the main thread. */
    static void startCapture(long timeoutMs, Capture c) {
        captureUntil = SystemClock.uptimeMillis() + timeoutMs;
        capture = c;
    }

    static void stopCapture() {
        capture = null;
    }

    /** The key's name in title case ("Prog Red", "F5"); keys without a name become "Key 192". */
    static String name(Context context, int code) {
        String symbol = KeyEvent.keyCodeToString(code);
        String bare = symbol.startsWith("KEYCODE_") ? symbol.substring("KEYCODE_".length()) : symbol;
        if (bare.isEmpty() || Character.isDigit(bare.charAt(0))) return context.getString(R.string.key_number, code);
        StringBuilder out = new StringBuilder();
        for (String word : bare.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            boolean acronym = word.length() <= 2;
            out.append(acronym ? word : word.charAt(0) + word.substring(1).toLowerCase());
        }
        return out.toString();
    }
}
