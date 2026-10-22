// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;

import java.lang.ref.WeakReference;

/** The home screen activity. It holds one Stage view and passes it the remote keys. */
public final class Home extends Activity {
    static final String TAG = "QuietHome";
    static final int MAX_HOME = 9;

    /** The running Home, so a configuration from a computer ({@link Config}) can apply at once. */
    private static WeakReference<Home> shown = new WeakReference<>(null);

    private Stage stage;

    private final BroadcastReceiver packages = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stage.refresh();
        }
    };

    static Home shown() {
        return shown.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs prefs = new Prefs(this);
        Catalog catalog = new Catalog(this, prefs);
        catalog.load();
        stage = new Stage(this, prefs, catalog);
        setContentView(stage);
        StartOn.register(this);
        shown = new WeakReference<>(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        registerReceiver(packages, filter);
    }

    /** Settings changed outside the activity: read apps, inputs and choices again. */
    void configChanged() {
        stage.refresh();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while QuietHome is in front: back to the plain Home screen.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) stage.goHome();
    }

    @Override
    protected void onStart() {
        super.onStart();
        stage.start();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        stage.returned();
    }

    @Override
    protected void onResume() {
        super.onResume();
        stage.checkService();
    }

    @Override
    protected void onPause() {
        // Leaving Home ends "Press a remote button".
        RemoteButtons.stopCapture();
        super.onPause();
    }

    @Override
    protected void onStop() {
        stage.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packages);
        if (shown.get() == this) shown.clear();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return stage.key(event) || super.dispatchKeyEvent(event);
    }
}
