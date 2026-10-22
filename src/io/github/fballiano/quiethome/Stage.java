// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The only view. It scales the 1920 x 1080 design canvas to the screen, keeps the photo, draws the
 * screens and turns remote keys into moves. Frames are drawn only while something changes.
 */
final class Stage extends View {
    static final long HOLD_MS = 650;
    private static final long STATUS_MS = 2600;

    final Home home;
    final Prefs prefs;
    final Catalog catalog;
    final Ui ui;
    final HomeScreen homeScreen;
    private final List<Screen> screens = new ArrayList<>();
    private final RectF full = new RectF(0, 0, Ui.W, Ui.H);

    private Bitmap photo, blur;
    private String photoId;
    private long lastFrame;
    private String status;
    private long statusUntil;
    private boolean started;

    private boolean okPending, okLongDone;
    private int swallowUntilUp;

    private long rotationSince, rotationSpent;

    private final Runnable longPress = () -> {
        okPending = false;
        okLongDone = true;
        top().okLong();
        invalidate();
    };

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            invalidate();
            scheduleTick();
        }
    };

    private final Runnable rotate = this::rotateNow;

    Stage(Home home, Prefs prefs, Catalog catalog) {
        super(home);
        this.home = home;
        this.prefs = prefs;
        this.catalog = catalog;
        this.ui = new Ui(home);
        setFocusable(true);
        setFocusableInTouchMode(true);
        homeScreen = new HomeScreen(this);
        screens.add(homeScreen);
    }

    // ---- Life cycle ----

    void start() {
        started = true;
        ensurePhoto();
        scheduleTick();
        updateRotation();
        requestFocus();
        invalidate();
    }

    void stop() {
        started = false;
        removeCallbacks(tick);
        cancelOk();
        updateRotation();
    }

    /** Back from another app. "Each time Home opens" rotation moves on here. */
    void returned() {
        if (prefs.is(Prefs.BG, "photo") && prefs.is(Prefs.ROTATION, "open")) {
            prefs.set(Prefs.WALLPAPER, Wallpapers.next(prefs.get(Prefs.WALLPAPER)).id);
        }
    }

    /**
     * Keeps the QuietHome service on: QuietHome turns it on itself when ADB allowed that, and
     * otherwise shows the setup screen until the user turns it on or chooses Later.
     */
    void checkService() {
        Screen setup = null;
        for (Screen s : screens) if (s instanceof SetupScreen) setup = s;
        if (ServiceSetup.isOn(getContext()) || ServiceSetup.turnOnItself(getContext())) {
            if (setup != null) {
                close(setup);
                toast("The QuietHome service is on.");
            }
            return;
        }
        if (setup == null && !SetupScreen.later && top() == homeScreen) open(new SetupScreen(this));
    }

    /** The Home button while QuietHome is in front: close panels and Photo mode. */
    void goHome() {
        while (screens.size() > 1) close(top());
        homeScreen.exitPhotoMode();
        homeScreen.reset();
        invalidate();
    }

    /** Apps changed, or a configuration arrived from ADB. */
    void refresh() {
        catalog.load();
        for (Screen s : new ArrayList<>(screens)) s.refresh();
        ensurePhoto();
        updateRotation();
        invalidate();
    }

    // ---- Screens ----

    Screen top() {
        return screens.get(screens.size() - 1);
    }

    void open(Screen s) {
        cancelOk();
        screens.add(s);
        s.opened();
        updateRotation();
        invalidate();
    }

    /** Closes a screen and every screen above it. */
    void close(Screen s) {
        int i = screens.indexOf(s);
        if (i <= 0) return;
        cancelOk();
        while (screens.size() > i) screens.remove(screens.size() - 1).closed();
        updateRotation();
        invalidate();
    }

    boolean isOpen(Screen s) {
        return screens.contains(s);
    }

    void toast(String message) {
        if (message == null) return;
        status = message;
        statusUntil = SystemClock.uptimeMillis() + STATUS_MS;
        invalidate();
        postDelayed(this::invalidate, STATUS_MS + 50);
    }

    /** Saves a choice and applies what it changes. */
    void apply(String key, String value) {
        if (value.equals(prefs.get(key))) return;
        if (!prefs.set(key, value)) return;
        if (Prefs.WALLPAPER.equals(key) || Prefs.ROTATION.equals(key)) {
            rotationSpent = 0;
            rotationSince = 0;
        }
        if (Prefs.LAYOUT.equals(key) || Prefs.APPS.equals(key) || Prefs.FONT.equals(key)) homeScreen.layoutChanged();
        ensurePhoto();
        updateRotation();
        invalidate();
    }

    // ---- Photo ----

    Bitmap photo() {
        return photo;
    }

    Bitmap blur() {
        return blur;
    }

    /** Decodes the chosen photo when the background is a photo, and frees it when it is not. */
    void ensurePhoto() {
        if (!prefs.is(Prefs.BG, "photo")) {
            photo = null;
            blur = null;
            photoId = null;
            return;
        }
        String id = prefs.get(Prefs.WALLPAPER);
        if (id.equals(photoId) && photo != null) return;
        Point size = screenSize();
        Bitmap decoded = Wallpapers.decode(getContext().getAssets(), Wallpapers.find(id), size.x, size.y);
        if (decoded == null) return;
        blur = Wallpapers.blur(decoded);
        // A hardware bitmap keeps the pixels in graphics memory only, not also in the app's heap.
        Bitmap gpu = decoded.copy(Bitmap.Config.HARDWARE, false);
        if (gpu != null) {
            decoded.recycle();
            decoded = gpu;
        }
        photo = decoded;
        photoId = id;
        Log.i(Home.TAG, "Photo " + id + ": " + photo.getWidth() + "x" + photo.getHeight() + " " + photo.getConfig());
    }

    Point screenSize() {
        Point p = new Point();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealSize(p);
        if (p.x < p.y) p.set(p.y, p.x);
        return p;
    }

    /** The background of Home: the photo, or a flat colour. Also used, scaled, by the Settings preview. */
    void drawBackground(Canvas c, Bitmap photo) {
        String bg = prefs.get(Prefs.BG);
        if ("photo".equals(bg) && photo != null) ui.photo(c, photo, full);
        else c.drawColor("light".equals(bg) ? Ui.LIGHT_BG : Ui.DARK_BG);
    }

    /** A brand mark, else the app's own icon, else initials; fitted into the box. */
    void drawMark(Canvas c, Catalog.Entry e, float left, float top, float w, float h, int ink, float alpha) {
        if (e.brand != null) {
            ui.mark(c, e.brand.mark, left, top, w, h, ink, alpha);
            return;
        }
        if (Catalog.isAction(e.id)) {
            int mark = Catalog.ACTION_HOME.equals(e.id) ? R.drawable.icon_home
                : Catalog.ACTION_SETTINGS.equals(e.id) ? R.drawable.icon_settings : R.drawable.icon_none;
            float size = Math.min(w, h) * 0.8f;
            ui.mark(c, mark, left + (w - size) / 2, top + (h - size) / 2, size, size, ink, alpha);
            return;
        }
        float size = Math.min(w, h);
        Bitmap icon = catalog.icon(e, Math.round(size));
        if (icon != null) ui.bitmap(c, icon, left + (w - size) / 2, top + (h - size) / 2, size, size, alpha);
        else ui.monogram(c, Catalog.title(e), left + w / 2, top + h / 2, size * 0.86f, ink, alpha);
    }

    // ---- Drawing ----

    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 16 : Math.min(64, now - lastFrame);
        lastFrame = now;
        canvas.drawColor(0xFF000000);
        float scale = Math.min(getWidth() / Ui.W, getHeight() / Ui.H);
        canvas.save();
        canvas.translate((getWidth() - Ui.W * scale) / 2, (getHeight() - Ui.H * scale) / 2);
        canvas.scale(scale, scale);
        canvas.clipRect(full);

        int first = screens.size() - 1;
        while (first > 0 && !screens.get(first).opaque()) first--;
        boolean moving = false;
        if (first == 0) drawBackground(canvas, photo);
        for (int i = first; i < screens.size(); i++) moving |= screens.get(i).draw(canvas, dt);
        drawStatus(canvas, now);
        canvas.restore();

        if (moving) postInvalidateOnAnimation();
        else lastFrame = 0;
    }

    private void drawStatus(Canvas c, long now) {
        if (status == null || now > statusUntil) return;
        float alpha = Math.min(1, (statusUntil - now) / 300f);
        ui.text(ui.font("manrope", 500), 30, -0.3f, Ui.PHOTO_INK, alpha);
        float w = ui.width(status);
        RectF r = new RectF(Ui.W / 2 - w / 2 - 32, 960, Ui.W / 2 + w / 2 + 32, 1024);
        ui.fill.setShader(null);
        ui.fill.setColor(0xB3101214);
        ui.fill.setAlpha(Math.round(0xB3 * alpha));
        c.drawRoundRect(r, 32, 32, ui.fill);
        ui.line(c, status, Ui.W / 2, ui.baseline(992), 0, 0);
        if (alpha < 1) postInvalidateOnAnimation();
    }

    private void scheduleTick() {
        removeCallbacks(tick);
        if (!started) return;
        long now = System.currentTimeMillis();
        postDelayed(tick, 60000 - now % 60000 + 50);
    }

    // ---- Rotation: counts only the time Home shows a photo, then moves to the next one ----

    private void rotateNow() {
        rotationSpent = 0;
        rotationSince = 0;
        prefs.set(Prefs.WALLPAPER, Wallpapers.next(prefs.get(Prefs.WALLPAPER)).id);
        ensurePhoto();
        updateRotation();
        invalidate();
    }

    private void updateRotation() {
        removeCallbacks(rotate);
        long now = SystemClock.uptimeMillis();
        String mode = prefs.get(Prefs.ROTATION);
        boolean timed = !"fixed".equals(mode) && !"open".equals(mode);
        boolean counting = started && timed && top() == homeScreen && prefs.is(Prefs.BG, "photo");
        if (!counting) {
            if (rotationSince != 0) rotationSpent += now - rotationSince;
            rotationSince = 0;
            return;
        }
        if (rotationSince == 0) rotationSince = now;
        long period = Long.parseLong(mode) * 60000L;
        long left = period - rotationSpent - (now - rotationSince);
        postDelayed(rotate, Math.max(1000, left));
    }

    // ---- Keys ----

    boolean key(KeyEvent e) {
        int code = e.getKeyCode();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        if (swallowUntilUp != 0 && code == swallowUntilUp) {
            if (!down) swallowUntilUp = 0;
            return true;
        }
        // Photo mode: any key returns, without doing its usual job.
        if (top() == homeScreen && homeScreen.photoMode && down) {
            homeScreen.exitPhotoMode();
            if (e.getRepeatCount() == 0) swallowUntilUp = code;
            invalidate();
            return true;
        }
        switch (code) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
                if (down && e.getRepeatCount() == 0) top().back();
                invalidate();
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                okKey(down, e.getRepeatCount(), e.isLongPress());
                invalidate();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                return move(down, 0, -1);
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return move(down, 0, 1);
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return move(down, -1, 0);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return move(down, 1, 0);
            default:
                return false;
        }
    }

    private boolean move(boolean down, int dx, int dy) {
        if (down && !okPending) {
            top().move(dx, dy);
            invalidate();
        }
        return true;
    }

    /** OK opens an app when it is released; holding it closes the app. Other items act at once. */
    private void okKey(boolean down, int repeat, boolean held) {
        if (down) {
            // Android marks the hold itself on the first repeat; act on that or on our timer, whichever comes first.
            if (repeat > 0) {
                if (held && okPending) {
                    removeCallbacks(longPress);
                    longPress.run();
                }
                return;
            }
            okLongDone = false;
            if (top().holdable()) {
                okPending = true;
                postDelayed(longPress, HOLD_MS);
            } else {
                top().ok();
            }
            return;
        }
        if (okPending) {
            cancelOk();
            if (!okLongDone) top().ok();
        }
    }

    private void cancelOk() {
        okPending = false;
        removeCallbacks(longPress);
    }
}
