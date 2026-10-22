package io.github.fballiano.quiethome;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

/**
 * The QuietHome service (RemoteButtons) is needed for the Home button, button mapping and
 * "When the TV turns on". This checks that it is on, turns it on when ADB granted
 * WRITE_SECURE_SETTINGS, and otherwise opens the settings where the user turns it on.
 */
final class ServiceSetup {
    private ServiceSetup() {
    }

    private static ComponentName service(Context context) {
        return new ComponentName(context, RemoteButtons.class);
    }

    /** Whether the service is on in Android's settings (it binds a moment later). */
    static boolean isOn(Context context) {
        String list = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (list == null) return false;
        ComponentName me = service(context);
        for (String entry : list.split(":")) if (me.equals(ComponentName.unflattenFromString(entry))) return true;
        return false;
    }

    /**
     * Turns the service on without the user, which needs WRITE_SECURE_SETTINGS:
     * adb shell pm grant io.github.fballiano.quiethome android.permission.WRITE_SECURE_SETTINGS
     * The other services that are on stay on. Returns false when QuietHome may not do this.
     */
    static boolean turnOnItself(Context context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) return false;
        ContentResolver resolver = context.getContentResolver();
        String list = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        String me = service(context).flattenToString();
        try {
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                list == null || list.isEmpty() ? me : list + ":" + me);
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            Log.i(Home.TAG, "Turned the QuietHome service on");
            return true;
        } catch (SecurityException e) {
            Log.w(Home.TAG, "Could not turn the QuietHome service on", e);
            return false;
        }
    }

    /** Opens QuietHome's app info, where Android 13+ can "allow restricted settings" for sideloaded apps. */
    static String openAppInfo(Context context) {
        try {
            context.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return null;
        } catch (ActivityNotFoundException | SecurityException e) {
            return "Open Settings → Apps → QuietHome on the TV.";
        }
    }

    /** Opens the service's own page in Accessibility (Android 12+), or the Accessibility list. Returns null or a message. */
    static String openSettings(Context context) {
        Intent details = new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS")
            .putExtra(Intent.EXTRA_COMPONENT_NAME, service(context).flattenToString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(details);
            return null;
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // Older TVs, and TV settings apps without that page.
        }
        try {
            context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return null;
        } catch (ActivityNotFoundException | SecurityException e) {
            return "Open Settings → System → Accessibility on the TV.";
        }
    }
}
