// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/**
 * "When the TV turns on": opens the chosen input or app at boot and each time the TV wakes from
 * standby. For a TV that only shows a box on one HDMI port. Home still shows QuietHome.
 *
 * Android lets an app start an activity from the background only while it shows a window or has
 * a bound accessibility service, so this works from Home in front or with button mapping on.
 */
final class StartOn {
    /** QuietHome starts this soon after boot when it belongs to the boot; later starts do not open the target. */
    private static final long BOOT_WINDOW_MS = 3 * 60_000;
    /** TV inputs can appear some seconds after boot: try again this often, this many times. */
    private static final long RETRY_MS = 2000;
    private static final int BOOT_TRIES = 15, WAKE_TRIES = 3;
    /** Home and the button mapping service can both see the same boot or wake; one opening is enough. */
    private static final long SAME_EVENT_MS = 10_000;

    private static final String HANDLED_BOOT = "startOnBoot";

    private static boolean registered;
    private static long lastOpened = -SAME_EVENT_MS;

    private StartOn() {
    }

    /** Called by Home and by the button mapping service; the first call in a process sets everything up. */
    static void register(Context context) {
        if (registered) return;
        registered = true;
        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        app.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                main.postDelayed(() -> open(app, main, WAKE_TRIES), 1000);
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_ON));
        if (isNewBoot(app)) main.postDelayed(() -> open(app, main, BOOT_TRIES), 1500);
    }

    /**
     * True once per boot, soon after it. Android counts boots; the count handled last is saved,
     * so a QuietHome restart later in the same boot does not open the target again.
     */
    private static boolean isNewBoot(Context app) {
        if (SystemClock.elapsedRealtime() > BOOT_WINDOW_MS) return false;
        int boot = Settings.Global.getInt(app.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        SharedPreferences store = app.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE);
        if (boot < 0 || store.getInt(HANDLED_BOOT, -1) == boot) return false;
        store.edit().putInt(HANDLED_BOOT, boot).apply();
        return true;
    }

    private static void open(Context app, Handler main, int tries) {
        Prefs prefs = new Prefs(app);
        String target = prefs.get(Prefs.START_ON);
        if (target.isEmpty() || SystemClock.elapsedRealtime() - lastOpened < SAME_EVENT_MS) return;
        Catalog catalog = new Catalog(app, prefs);
        catalog.load();
        if (catalog.get(target) == null) {
            if (tries > 1) main.postDelayed(() -> open(app, main, tries - 1), RETRY_MS);
            else Log.w(Home.TAG, "Start on: " + target + " is not on the TV");
            return;
        }
        String problem = catalog.launch(target);
        if (problem != null) {
            Log.w(Home.TAG, "Start on: " + target + ": " + problem);
            return;
        }
        lastOpened = SystemClock.elapsedRealtime();
        Log.i(Home.TAG, "Start on: opened " + target);
    }
}
