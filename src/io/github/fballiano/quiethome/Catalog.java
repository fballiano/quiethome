// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** What Home can open (installed apps, HDMI inputs and button actions), and how to open and close it. */
final class Catalog {
    static final class Entry {
        /** A package name, an HDMI input ("hdmi:" + port) or a button action ("action." + name). */
        final String id;
        final String name;
        final Brands.Brand brand;
        /** The TvInputManager id, for HDMI inputs only. */
        final String inputId;

        Entry(String id, String name, Brands.Brand brand, String inputId) {
            this.id = id;
            this.name = name;
            this.brand = brand;
            this.inputId = inputId;
        }

        boolean isInput() {
            return inputId != null;
        }
    }

    static final String HDMI = "hdmi:";

    /** Things a mapped button can do besides opening an app or an input. */
    static final String ACTION_NONE = "action.none", ACTION_HOME = "action.home", ACTION_SETTINGS = "action.settings";

    /** HOME activities with these words in their package are system tools, not home screens to switch to. */
    private static final Pattern TOOL_HOMES = Pattern.compile("setup|provision|settings|factory|wraith");

    private final Context context;
    private final PackageManager pm;
    private final Prefs prefs;
    private final Map<String, Entry> byId = new LinkedHashMap<>();
    private final Map<String, Bitmap> icons = new HashMap<>();

    Catalog(Context context, Prefs prefs) {
        this.context = context;
        this.pm = context.getPackageManager();
        this.prefs = prefs;
    }

    static List<Entry> actions() {
        List<Entry> out = new ArrayList<>();
        out.add(new Entry(ACTION_NONE, "Nothing", null, null));
        out.add(new Entry(ACTION_HOME, "QuietHome", null, null));
        out.add(new Entry(ACTION_SETTINGS, "TV settings", null, null));
        return out;
    }

    static boolean isAction(String id) {
        return id.startsWith("action.");
    }

    static boolean isHdmi(String id) {
        return id.startsWith(HDMI);
    }

    // ---- Reading ----

    /** Reads the apps and the HDMI inputs again, sorted by name. */
    void load() {
        List<Entry> found = new ArrayList<>();
        for (ResolveInfo app : launchableApps()) {
            String pkg = app.activityInfo.packageName;
            found.add(new Entry(pkg, label(app.loadLabel(pm), pkg), Brands.forPackage(pkg), null));
        }
        found.addAll(hdmiInputs());
        Collections.sort(found, (a, b) -> a.name.compareToIgnoreCase(b.name));
        byId.clear();
        for (Entry e : found) byId.put(e.id, e);
        // Updated apps can have new icons; they are drawn again when Home needs them.
        icons.clear();
    }

    /**
     * One launch activity per app. A TV app shows up through its leanback entry. An app with only
     * a phone entry shows up when the user installed it: preinstalled phone-only apps on a TV are
     * system tools.
     */
    private Collection<ResolveInfo> launchableApps() {
        Map<String, ResolveInfo> apps = new LinkedHashMap<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            if (!apps.containsKey(r.activityInfo.packageName)) apps.put(r.activityInfo.packageName, r);
        }
        Intent phone = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(phone, 0)) {
            boolean userApp = (r.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0;
            if (userApp && !apps.containsKey(r.activityInfo.packageName)) apps.put(r.activityInfo.packageName, r);
        }
        apps.remove(context.getPackageName());
        return apps.values();
    }

    /** The physical HDMI ports. Tuners and other input types, and the devices behind a port, stay out. */
    private List<Entry> hdmiInputs() {
        List<Entry> out = new ArrayList<>();
        TvInputManager inputs = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        if (inputs == null) return out;
        List<TvInputInfo> list;
        try {
            list = inputs.getTvInputList();
        } catch (RuntimeException e) {
            Log.w(Home.TAG, "The TV did not list its inputs", e);
            return out;
        }
        for (TvInputInfo info : list) {
            boolean port = info.getType() == TvInputInfo.TYPE_HDMI && info.getParentId() == null;
            if (!port) continue;
            String id = hdmiId(info.getId());
            String name = prefs.inputName(id);
            if (name == null) {
                CharSequence custom = info.loadCustomLabel(context);
                name = label(custom != null && custom.length() > 0 ? custom : info.loadLabel(context), "HDMI");
            }
            out.add(new Entry(id, name, Brands.forInput(name), info.getId()));
            // The ADB configuration refers to inputs by this id; the log is how a computer finds it.
            Log.i(Home.TAG, "HDMI input " + id + " \"" + name + "\" (" + info.getId() + ")");
        }
        return out;
    }

    /**
     * "hdmi:" and the part of the TV input id after its last slash, which names the port on the
     * TVs seen so far (".../HW5" gives "hdmi:HW5"). Separators of the saved lists become "_".
     */
    static String hdmiId(String tvInputId) {
        String port = tvInputId.substring(tvInputId.lastIndexOf('/') + 1).replaceAll("[\\s,;=]", "_");
        return HDMI + (port.isEmpty() ? tvInputId.hashCode() : port);
    }

    private static String label(CharSequence text, String fallback) {
        String s = text == null ? "" : text.toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    Collection<Entry> all() {
        return byId.values();
    }

    Entry get(String id) {
        if (isAction(id)) for (Entry e : actions()) if (e.id.equals(id)) return e;
        return byId.get(id);
    }

    /** The name shown for an entry: the service name for a known brand, otherwise the app or input name. */
    static String title(Entry e) {
        return e.brand != null && !e.isInput() ? e.brand.name : e.name;
    }

    /** Known services found on this TV in brand order, or the first apps when there is none. */
    List<String> defaults() {
        List<String> out = new ArrayList<>();
        for (Brands.Brand brand : Brands.ALL) {
            if (brand.input != null) {
                for (Entry e : byId.values()) if (e.isInput() && e.brand == brand) { out.add(e.id); break; }
                continue;
            }
            for (String p : brand.packages) if (byId.containsKey(p)) { out.add(p); break; }
        }
        Iterator<Entry> rest = byId.values().iterator();
        while (out.size() < 4 && rest.hasNext()) {
            Entry e = rest.next();
            if (!out.contains(e.id)) out.add(e.id);
        }
        while (out.size() > Home.MAX_HOME) out.remove(out.size() - 1);
        return out;
    }

    // ---- Opening and closing ----

    /** Opens an app or an HDMI input, or does a button action. Returns null, or a message for the screen. */
    String launch(String id) {
        if (ACTION_NONE.equals(id)) return null;
        if (ACTION_SETTINGS.equals(id)) return openSettings();
        if (ACTION_HOME.equals(id)) {
            return start(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .setPackage(context.getPackageName()).addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        }
        if (isHdmi(id)) {
            Entry e = byId.get(id);
            if (e == null) return "This input is not on the TV now.";
            // The passthrough channel of an input shows that input in the TV's own player.
            Uri channel = TvContract.buildChannelUriForPassthroughInput(e.inputId);
            return start(new Intent(Intent.ACTION_VIEW, channel));
        }
        Intent app = pm.getLeanbackLaunchIntentForPackage(id);
        if (app == null) app = pm.getLaunchIntentForPackage(id);
        if (app == null) return "This app is no longer on the TV.";
        return start(app);
    }

    /**
     * Stops the app's background processes and frees their memory. Android gives a home screen no
     * stronger close; an app that runs a foreground service continues.
     */
    String close(String id) {
        if (isHdmi(id) || isAction(id)) return "Only apps can close.";
        try {
            pm.getApplicationInfo(id, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return "This app is no longer on the TV.";
        }
        try {
            ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).killBackgroundProcesses(id);
            return null;
        } catch (SecurityException e) {
            Log.w(Home.TAG, "Close refused for " + id, e);
            return "The TV did not let QuietHome close this app.";
        }
    }

    String openSettings() {
        return start(new Intent(Settings.ACTION_SETTINGS));
    }

    /** Opens the best other home screen: the one with the highest priority that is not a system tool. */
    String openPreviousLauncher() {
        ResolveInfo best = null;
        for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(context.getPackageName()) || r.priority < 0 || TOOL_HOMES.matcher(pkg).find()) continue;
            if (best == null || r.priority > best.priority) best = r;
        }
        if (best == null) return start(new Intent(Settings.ACTION_HOME_SETTINGS));
        return start(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .setClassName(best.activityInfo.packageName, best.activityInfo.name));
    }

    private String start(Intent intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return null;
        } catch (RuntimeException e) {
            Log.w(Home.TAG, "Could not start " + intent, e);
            return "The TV cannot open this.";
        }
    }

    // ---- Icons ----

    /** The app's own icon at size x size pixels, drawn once and kept until {@link #trimIcons}. */
    Bitmap icon(Entry e, int size) {
        if (e.isInput() || isAction(e.id)) return null;
        String key = e.id + "@" + size;
        if (icons.containsKey(key)) return icons.get(key);
        Bitmap bitmap = null;
        try {
            Drawable d = pm.getApplicationIcon(e.id);
            bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            d.setBounds(0, 0, size, size);
            d.draw(new Canvas(bitmap));
        } catch (PackageManager.NameNotFoundException | RuntimeException ex) {
            Log.w(Home.TAG, "No icon for " + e.id);
        }
        icons.put(key, bitmap);
        return bitmap;
    }

    /** Keeps only the icons of the given ids (Home's), so a closed All apps grid frees its icons. */
    void trimIcons(Collection<String> keep) {
        Iterator<Map.Entry<String, Bitmap>> it = icons.entrySet().iterator();
        while (it.hasNext()) {
            String key = it.next().getKey();
            if (!keep.contains(key.substring(0, key.lastIndexOf('@')))) it.remove();
        }
    }
}
