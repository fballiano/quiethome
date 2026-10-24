// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

/**
 * The result of an update from {@link Updater}. Android first asks for the user's confirmation; this
 * opens its dialog. After a good installation Android restarts QuietHome, so only failures remain.
 */
public final class UpdateStatus extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (status == PackageInstaller.STATUS_SUCCESS) return;
        context.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE).edit().putBoolean(Prefs.UPDATING, false).apply();
        Log.w(Home.TAG, "Update: " + status + " " + intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
        Home home = Home.shown();
        if (home != null) {
            home.toast(context.getString(status == PackageInstaller.STATUS_FAILURE_ABORTED
                ? R.string.update_cancelled : R.string.update_failed));
        }
    }
}
