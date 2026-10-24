// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Settings → Update QuietHome, the only network use of QuietHome. It reads the latest release from
 * the GitHub API and, when it is newer, downloads quiethome.apk, checks its SHA-256 digest and its
 * signature, and gives it to Android's package installer, which asks the user to confirm.
 * Only in the GitHub build: in the Play build, src-play has a stub, because Google Play updates the app.
 */
final class Updater {
    /** Settings shows Update QuietHome. */
    static final boolean AVAILABLE = true;

    private static final String LATEST = "https://api.github.com/repos/fballiano/quiethome/releases/latest";
    private static final String ASSET = "quiethome.apk";
    private static final int TIMEOUT_MS = 20000;
    /** How long a message about the update stays: long enough for a slow download step. */
    private static final long STATUS_MS = 60000;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> new Thread(r, "QuietHome update"));
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** An update is in progress; the main thread reads and writes it. */
    private static boolean running;
    private static String lastStatus;

    /** A failure with the string id of the message for the screen. */
    private static final class Failure extends Exception {
        final int message;

        Failure(int message, String detail) {
            super(detail);
            this.message = message;
        }
    }

    private Updater() {
    }

    /** Starts the update from Settings. Call on the main thread. */
    static void start(Stage stage) {
        if (running) {
            stage.toast(lastStatus);
            return;
        }
        Context context = stage.getContext().getApplicationContext();
        // Android allows the installation only after the user lets QuietHome install apps.
        if (!context.getPackageManager().canRequestPackageInstalls()) {
            try {
                stage.getContext().startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName())));
            } catch (ActivityNotFoundException | SecurityException e) {
                Log.w(Home.TAG, "No settings screen to allow installs", e);
            }
            stage.toast(stage.text(R.string.update_allow));
            return;
        }
        running = true;
        show(stage, stage.text(R.string.update_checking));
        WORKER.execute(() -> {
            try {
                run(stage, context);
            } catch (Failure f) {
                Log.w(Home.TAG, "Update: " + f.getMessage());
                MAIN.post(() -> stage.toast(stage.text(f.message)));
            } catch (IOException | JSONException e) {
                Log.w(Home.TAG, "Update: no connection to GitHub", e);
                MAIN.post(() -> stage.toast(stage.text(R.string.update_no_network)));
            } finally {
                MAIN.post(() -> running = false);
            }
        });
    }

    /** Runs on the worker thread. */
    private static void run(Stage stage, Context context) throws Failure, IOException, JSONException {
        PackageManager pm = context.getPackageManager();
        PackageInfo installed;
        try {
            installed = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            throw new Failure(R.string.update_failed, "QuietHome is not installed");
        }

        JSONObject release = new JSONObject(new String(read(LATEST), "UTF-8"));
        String tag = release.getString("tag_name");
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        if (code(version) <= code(installed)) {
            MAIN.post(() -> stage.toast(stage.text(R.string.update_latest, installed.versionName)));
            return;
        }
        JSONObject asset = null;
        JSONArray assets = release.getJSONArray("assets");
        for (int i = 0; i < assets.length(); i++) {
            if (ASSET.equals(assets.getJSONObject(i).getString("name"))) asset = assets.getJSONObject(i);
        }
        if (asset == null) throw new Failure(R.string.update_failed, tag + " has no " + ASSET);

        File apk = new File(context.getCacheDir(), "update.apk");
        String digest = download(stage, asset.getString("browser_download_url"), asset.optLong("size"), apk, version);
        String expected = asset.optString("digest", "");
        if (!expected.isEmpty() && !expected.equalsIgnoreCase("sha256:" + digest)) {
            apk.delete();
            throw new Failure(R.string.update_bad_file, "digest " + digest + " is not " + expected);
        }

        // Android installs an update only with the same signature; say so before its own error.
        PackageInfo archive = pm.getPackageArchiveInfo(apk.getPath(), PackageManager.GET_SIGNATURES);
        if (archive == null || !context.getPackageName().equals(archive.packageName) || code(archive) <= code(installed)) {
            apk.delete();
            throw new Failure(R.string.update_bad_file, "not a newer QuietHome APK");
        }
        if (!Arrays.equals(archive.signatures, installed.signatures)) {
            apk.delete();
            throw new Failure(R.string.update_other_key, "the installed QuietHome has another signature");
        }

        MAIN.post(() -> show(stage, stage.text(R.string.update_installing, version)));
        install(context, apk);
    }

    /** The version code that the release workflow gives a version: 1.2.3 is 10203. */
    static long code(String version) {
        String[] parts = version.split("-")[0].split("\\.");
        long code = 0;
        for (int i = 0; i < 3; i++) {
            long part = 0;
            if (i < parts.length) {
                try {
                    part = Long.parseLong(parts[i]);
                } catch (NumberFormatException ignored) {
                }
            }
            code = code * 100 + (i == 0 ? part : Math.min(part, 99));
        }
        return code;
    }

    @SuppressWarnings("deprecation")
    private static long code(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setRequestProperty("User-Agent", "QuietHome");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
            c.disconnect();
            throw new IOException(url + ": HTTP " + c.getResponseCode());
        }
        return c;
    }

    private static byte[] read(String url) throws IOException {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    /** Downloads to the file and shows the progress. Returns the SHA-256 digest in hex. */
    private static String download(Stage stage, String url, long size, File file, String version) throws IOException {
        HttpURLConnection c = open(url);
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        long total = c.getContentLengthLong() > 0 ? c.getContentLengthLong() : size;
        try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(file)) {
            byte[] buffer = new byte[65536];
            long done = 0;
            int shown = -1;
            for (int n; (n = in.read(buffer)) > 0; ) {
                out.write(buffer, 0, n);
                sha.update(buffer, 0, n);
                done += n;
                int percent = total > 0 ? (int) Math.min(100, done * 100 / total) : 0;
                if (percent != shown) {
                    shown = percent;
                    MAIN.post(() -> show(stage, stage.text(R.string.update_downloading, version, percent)));
                }
            }
        } finally {
            c.disconnect();
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : sha.digest()) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    /** Gives the APK to the package installer. {@link UpdateStatus} receives the result. */
    private static void install(Context context, File apk) throws IOException {
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(context.getPackageName());
        int id = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(id)) {
            try (InputStream in = new FileInputStream(apk); OutputStream out = session.openWrite(ASSET, 0, apk.length())) {
                byte[] buffer = new byte[65536];
                for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
                session.fsync(out);
            }
            // The installer adds the result to this intent, so it must be mutable.
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            PendingIntent result = PendingIntent.getBroadcast(context, id, new Intent(context, UpdateStatus.class), flags);
            context.getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE).edit().putBoolean(Prefs.UPDATING, true).apply();
            session.commit(result.getIntentSender());
        } finally {
            apk.delete();
        }
    }

    private static void show(Stage stage, String message) {
        lastStatus = message;
        stage.toast(message, STATUS_MS);
    }
}
