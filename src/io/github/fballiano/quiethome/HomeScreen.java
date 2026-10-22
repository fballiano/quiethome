// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Home: the chosen apps as a List (large names down the left) or a Dock (tiles on a glass band),
 * the clock, and three controls: All apps, Settings (QuietHome's) and TV settings.
 */
final class HomeScreen extends Screen {
    private static final int APPS = 0, CONTROLS = 1;
    private static final int[] CONTROL_TEXTS = {R.string.all_apps, R.string.settings, R.string.tv_settings};
    private static final int[] CONTROL_MARKS = {R.drawable.icon_apps, R.drawable.icon_settings, R.drawable.icon_tvsettings};

    private static final float LEFT = 140, RIGHT = 1780;
    private static final float LIST_TOP = 236, ROW = 128;
    private static final int ROWS = 6;
    /** The largest dock. A dock with fewer tiles is narrower and stays centred. */
    private static final RectF DOCK = new RectF(96, 676, 1824, 906);
    /** Six tiles fit the largest dock; more tiles scroll. PAD is the space between the dock edge and its tiles. */
    private static final float PAD = 16, TILE_TOP = 691, TILE_W = 272, TILE_H = 200, PITCH = 280;

    final List<Catalog.Entry> items = new ArrayList<>();
    private final List<Ui.Anim> emphasis = new ArrayList<>();
    private final Ui.Anim[] controls = {new Ui.Anim(Ui.FOCUS_MS), new Ui.Anim(Ui.FOCUS_MS), new Ui.Anim(Ui.FOCUS_MS)};
    /** The press on the app that is opening: 1 at the press, back to 0 as the item springs back. */
    private final Ui.Anim press = new Ui.Anim(140);
    private int pressed = -1;
    private final Ui.Anim scroll = new Ui.Anim(Ui.SCROLL_MS);
    private final RectF dockArea = new RectF();
    private final Ui.Anim plate = new Ui.Anim(Ui.FOCUS_MS);
    private final Ui.Anim plateWidth = new Ui.Anim(Ui.FOCUS_MS);
    private final Ui.Anim plateAlpha = new Ui.Anim(Ui.FOCUS_MS);
    private final RectF r = new RectF();
    private final Calendar calendar = Calendar.getInstance();
    /** The control names in the TV's language, read again by refresh() when the language changes. */
    private final String[] controlNames = new String[CONTROL_TEXTS.length];
    private Shader[] scrims;
    private int focus, zone = APPS, control;
    private boolean snapNext = true;
    /** The hold-OK menu of the focused app as string ids, or null. */
    private List<Integer> menu;
    private int menuIndex;
    /** Move mode: the arrows move the focused app; OK saves the order, Back restores it. */
    private boolean moving;
    private final RectF menuRect = new RectF();

    HomeScreen(Stage stage) {
        super(stage);
        refresh();
    }

    @Override
    void refresh() {
        for (int k = 0; k < controlNames.length; k++) controlNames[k] = text(CONTROL_TEXTS[k]);
        menu = null;
        moving = false;
        String focused = focus < items.size() ? items.get(focus).id : stage.prefs.sp.getString(Prefs.LAST, null);
        items.clear();
        List<String> ids = stage.prefs.home();
        if (ids == null) ids = stage.catalog.defaults();
        for (String id : ids) {
            Catalog.Entry e = stage.catalog.get(id);
            if (e != null && items.size() < Home.MAX_HOME) items.add(e);
        }
        if (items.isEmpty()) for (String id : stage.catalog.defaults()) items.add(stage.catalog.get(id));
        while (emphasis.size() < items.size()) emphasis.add(new Ui.Anim(Ui.FOCUS_MS));
        focus = 0;
        for (int i = 0; i < items.size(); i++) if (items.get(i).id.equals(focused)) focus = i;
        if (items.isEmpty()) zone = CONTROLS;
        snapNext = true;
        targets();
    }

    /** Ids of the apps on Home, in order. */
    List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Catalog.Entry e : items) out.add(e.id);
        return out;
    }

    void layoutChanged() {
        snapNext = true;
        targets();
    }

    /** Home pressed while QuietHome is in front: close the menu and cancel a move. */
    void reset() {
        if (menu != null || moving) refresh();
    }

    // ---- Keys ----

    @Override
    void move(int dx, int dy) {
        boolean dock = stage.prefs.is(Prefs.LAYOUT, "dock");
        int along = dock ? dx : dy;
        if (menu != null) {
            if (dy != 0) menuIndex = Math.max(0, Math.min(menu.size() - 1, menuIndex + dy));
            return;
        }
        if (moving) {
            int to = focus + along;
            if (along != 0 && to >= 0 && to < items.size()) {
                Collections.swap(items, focus, to);
                Collections.swap(emphasis, focus, to);
                focus = to;
                targets();
            }
            return;
        }
        if (zone == APPS) {
            if (along != 0) focus = Math.max(0, Math.min(items.size() - 1, focus + along));
            else if (dock ? dy < 0 : dx > 0) zone = CONTROLS;
        } else {
            if (along != 0) control = Math.max(0, Math.min(controls.length - 1, control + along));
            else if ((dock ? dy > 0 : dx < 0) && !items.isEmpty()) zone = APPS;
        }
        targets();
    }

    @Override
    boolean holdable() {
        return menu == null && !moving && zone == APPS && !items.isEmpty();
    }

    @Override
    void ok() {
        if (menu != null) {
            int choice = menu.get(menuIndex);
            menu = null;
            act(choice);
            return;
        }
        if (moving) {
            moving = false;
            stage.prefs.setHome(ids());
            targets();
            stage.toast(text(R.string.order_saved));
            return;
        }
        if (zone == APPS) {
            if (items.isEmpty()) return;
            Catalog.Entry e = items.get(focus);
            String error = stage.catalog.launch(e.id);
            if (error != null) {
                stage.toast(error);
                return;
            }
            stage.prefs.sp.edit().putString(Prefs.LAST, e.id).apply();
            stage.opening(e);
            pressed = focus;
            press.snap(1);
            press.to(0);
            return;
        }
        if (control == 0) stage.open(new AllAppsScreen(stage));
        else if (control == 1) stage.open(new SettingsScreen(stage));
        else stage.toast(stage.catalog.openSettings());
    }

    @Override
    void okLong() {
        if (zone != APPS || items.isEmpty()) return;
        menu = new ArrayList<>();
        menu.add(R.string.move);
        menu.add(R.string.remove_from_home);
        if (!items.get(focus).isInput()) menu.add(R.string.close_app);
        menuIndex = 0;
    }

    private void act(int choice) {
        Catalog.Entry e = items.get(focus);
        if (choice == R.string.move) {
            moving = true;
            targets();
        } else if (choice == R.string.remove_from_home) {
            if (items.size() <= 1) {
                stage.toast(text(R.string.home_needs_one));
                return;
            }
            List<String> ids = ids();
            ids.remove(focus);
            stage.prefs.setHome(ids);
            refresh();
            stage.toast(text(R.string.removed_from_home, Catalog.title(e)));
        } else {
            String error = stage.catalog.close(e.id);
            stage.toast(error != null ? error : text(R.string.closed, Catalog.title(e)));
        }
    }

    @Override
    void back() {
        if (menu != null) {
            menu = null;
            return;
        }
        if (moving) {
            refresh();
            return;
        }
        // Home has nowhere to go back to. From the controls, Back returns to the selected app.
        if (zone == CONTROLS && !items.isEmpty()) {
            zone = APPS;
            targets();
        }
    }

    private void targets() {
        int n = items.size();
        for (int i = 0; i < emphasis.size(); i++) {
            emphasis.get(i).to(i != focus || i >= n ? 0 : zone == APPS ? 1 : 0.5f);
        }
        for (int k = 0; k < controls.length; k++) controls[k].to(zone == CONTROLS && k == control ? 1 : 0);
        plateAlpha.to(n == 0 ? 0 : zone == APPS ? 1 : 0.5f);
        if (stage.prefs.is(Prefs.LAYOUT, "dock")) {
            float s = scroll.target;
            float visible = dockArea(n).width() - 2 * PAD;
            float peek = PITCH * 0.4f;
            float min = (focus + 1) * PITCH - (PITCH - TILE_W) + (focus < n - 1 ? peek : 0) - visible;
            float max = focus * PITCH - (focus > 0 ? peek : 0);
            s = Math.max(min, Math.min(max, s));
            s = Math.max(0, Math.min(s, Math.max(0, n * PITCH - (PITCH - TILE_W) - visible)));
            scroll.to(s);
            plate.to(focus * PITCH);
        } else {
            float s = scroll.target;
            if (focus < s + 1) s = focus - 1;
            if (focus > s + ROWS - 2) s = focus - ROWS + 2;
            scroll.to(Math.max(0, Math.min(s, Math.max(0, n - ROWS))));
            plate.to(focus);
        }
    }

    // ---- Drawing ----

    @Override
    boolean draw(Canvas c, float dt) {
        boolean moving = false;
        if (snapNext) {
            snapNext = false;
            for (Ui.Anim a : emphasis) a.snap(a.target);
            for (Ui.Anim a : controls) a.snap(a.target);
            scroll.snap(scroll.target);
            plate.snap(plate.target);
            plateAlpha.snap(plateAlpha.target);
            plateWidth.snap(0);
        } else {
            for (Ui.Anim a : emphasis) moving |= a.step(dt);
            for (Ui.Anim a : controls) moving |= a.step(dt);
            moving |= press.step(dt);
            moving |= scroll.step(dt);
            moving |= plate.step(dt);
            moving |= plateWidth.step(dt);
            moving |= plateAlpha.step(dt);
        }
        paint(c, true, stage.blur());
        return moving;
    }

    /** The committed Home, without animation, for the Settings preview. */
    void preview(Canvas c, Bitmap blur) {
        paint(c, false, blur);
    }

    /** The opening app dips and springs back, so OK shows at once that it worked. */
    private float pressScale(int i, boolean live) {
        return live && i == pressed ? 1 - 0.08f * press.value : 1;
    }

    private float v(Ui.Anim a, boolean live) {
        return live ? a.value : a.target;
    }

    private void paint(Canvas c, boolean live, Bitmap blur) {
        Prefs prefs = stage.prefs;
        String bg = prefs.get(Prefs.BG);
        boolean photo = "photo".equals(bg);
        boolean dock = prefs.is(Prefs.LAYOUT, "dock");
        int ink = Ui.ink(bg);
        if (photo) scrims(c, dock);
        if (dock) dock(c, live, blur, bg, ink);
        else list(c, live, blur, bg, ink);
        controls(c, live, blur, bg, ink, dock);
        clock(c, dock, ink, photo);
        if (live && menu != null) menu(c, dock);
        if (live && moving) hint(c, dock);
    }

    /** Soft shade where text sits on a photo: the top (clock), the bottom (controls) and, for the List, the left. */
    private void scrims(Canvas c, boolean dock) {
        if (scrims == null) {
            scrims = new Shader[]{
                new LinearGradient(0, 0, 0, 340, 0x40000000, 0, Shader.TileMode.CLAMP),
                new LinearGradient(0, 640, 0, Ui.H, 0, 0x59000000, Shader.TileMode.CLAMP),
                new LinearGradient(0, 0, 1240, 0, 0x5C000000, 0, Shader.TileMode.CLAMP),
            };
        }
        ui.fill.setColor(0xFF000000);
        ui.fill.setShader(scrims[0]);
        c.drawRect(0, 0, Ui.W, 340, ui.fill);
        ui.fill.setShader(scrims[1]);
        c.drawRect(0, 640, Ui.W, Ui.H, ui.fill);
        if (!dock) {
            ui.fill.setShader(scrims[2]);
            c.drawRect(0, 0, 1240, Ui.H, ui.fill);
        }
        ui.fill.setShader(null);
    }

    /** Alpha of an app for an emphasis of 0 (resting), 0.5 (selected, focus elsewhere) and 1 (focus). */
    private static float alpha(float e, float rest, float selected) {
        return e <= 0.5f ? rest + (selected - rest) * e * 2 : selected + (1 - selected) * (e - 0.5f) * 2;
    }

    private void list(Canvas c, boolean live, Bitmap blur, String bg, int ink) {
        Prefs prefs = stage.prefs;
        int n = items.size();
        boolean photo = "photo".equals(bg);
        boolean glass = prefs.is(Prefs.FOCUS, "glass");
        String apps = prefs.get(Prefs.APPS);
        Typeface face = ui.font(prefs.get(Prefs.FONT), 600);
        float sc = v(scroll, live);
        float rest = glass ? 0.84f : photo ? 0.84f : 0.62f;
        float selected = glass ? 1 : photo ? 0.92f : 0.78f;

        if ((glass || moving) && n > 0) {
            float width = rowWidth(items.get(focus), apps, face);
            if (plateWidth.value == 0) plateWidth.snap(width);
            else plateWidth.to(width);
            float top = LIST_TOP + (v(plate, live) - sc) * ROW;
            r.set(LEFT - 28, top - 3, LEFT - 28 + v(plateWidth, live), top + ROW + 3);
            ui.glass(c, r, 38, blur, bg, prefs.get(Prefs.MATERIAL), v(plateAlpha, live), true);
        }
        for (int i = 0; i < n; i++) {
            float pos = i - sc;
            float fade = Ui.clamp(Math.min(pos + 1, ROWS - pos));
            if (sc > 0.01f) fade = Math.min(fade, Ui.clamp(pos + 0.4f));
            if (n - ROWS - sc > 0.01f) fade = Math.min(fade, Ui.clamp(ROWS - 1 - pos + 0.4f));
            if (fade <= 0) continue;
            float e = v(emphasis.get(i), live);
            float a = alpha(e, rest, selected) * fade;
            float top = LIST_TOP + pos * ROW, cy = top + ROW / 2;
            float scale = (glass ? 1 : 1 + 0.05f * Math.max(0, e * 2 - 1)) * pressScale(i, live);
            c.save();
            c.scale(scale, scale, LEFT + 28, cy);
            Catalog.Entry entry = items.get(i);
            float x = LEFT;
            if (!"text".equals(apps)) {
                stage.drawMark(c, entry, x, cy - 42, 84, 84, ink, a);
                x += 108;
            }
            float nameAlpha = "icons".equals(apps) ? a * Ui.clamp(e * 2 - 1) : a;
            if (nameAlpha > 0) {
                // No shadow here: a blurred shadow must be drawn again at every step of the focus scale.
                // The left scrim gives the contrast instead.
                ui.text(face, 88, -1.8f, ink, nameAlpha);
                ui.line(c, Catalog.title(entry), x, ui.baseline(cy), 992, -1);
            }
            c.restore();
        }
        if (n == 0) {
            ui.text(face, 64, -1.8f, ink, 0.8f);
            ui.line(c, text(R.string.no_apps_on_home), LEFT, ui.baseline(LIST_TOP + ROW / 2), 0, -1);
        }
    }

    /** The width of the List plate behind an app: mark, name and padding. */
    private float rowWidth(Catalog.Entry e, String apps, Typeface face) {
        float w = 56;
        if (!"text".equals(apps)) w += 84;
        if (!"icons".equals(apps)) {
            if (!"text".equals(apps)) w += 24;
            ui.text(face, 88, -1.8f, 0, 1);
            w += Math.min(992, ui.width(Catalog.title(e)));
        }
        return w;
    }

    private void dock(Canvas c, boolean live, Bitmap blur, String bg, int ink) {
        Prefs prefs = stage.prefs;
        boolean photo = "photo".equals(bg);
        boolean glass = prefs.is(Prefs.FOCUS, "glass");
        String apps = prefs.get(Prefs.APPS);
        Typeface face = ui.font(prefs.get(Prefs.FONT), 500);
        Typeface bold = ui.font(prefs.get(Prefs.FONT), 600);
        float sx = v(scroll, live);
        float rest = photo || glass ? 0.84f : 0.62f;
        float selected = glass ? 1 : photo ? 0.92f : 0.78f;

        RectF area = dockArea(items.size());
        float tileLeft = area.left + PAD;
        // When the tiles do not fit, the ones cut by an edge fade with the part that is hidden.
        boolean scrolls = area.width() >= DOCK.width() && items.size() * PITCH - (PITCH - TILE_W) > area.width() - 2 * PAD;
        ui.glass(c, area, 40, blur, bg, prefs.get(Prefs.MATERIAL), 1, true);
        c.save();
        c.clipRect(area.left + 8, area.top, area.right - 8, area.bottom);
        if ((glass || moving) && !items.isEmpty()) {
            float x = tileLeft + v(plate, live) - sx;
            r.set(x, TILE_TOP, x + TILE_W, TILE_TOP + TILE_H);
            int color = "light".equals(bg) ? 0x1A000000 : 0x38FFFFFF;
            ui.fill.setShader(null);
            ui.fill.setColor(color);
            ui.fill.setAlpha(Math.round(Color.alpha(color) * v(plateAlpha, live)));
            c.drawRoundRect(r, 32, 32, ui.fill);
        }
        for (int i = 0; i < items.size(); i++) {
            float x = tileLeft + i * PITCH - sx;
            if (x > area.right || x + TILE_W < area.left) continue;
            float e = v(emphasis.get(i), live);
            float a = alpha(e, rest, selected);
            if (scrolls) a *= Ui.clamp(Math.min(x + TILE_W - tileLeft, area.right - PAD - x) / TILE_W);
            float scale = (glass ? 1 : 1 + 0.05f * Math.max(0, e * 2 - 1)) * pressScale(i, live);
            float cx = x + TILE_W / 2, y = TILE_TOP;
            Catalog.Entry entry = items.get(i);
            c.save();
            c.scale(scale, scale, cx, y + TILE_H / 2);
            if ("text".equals(apps)) {
                ui.text(bold, 40, -1, ink, a);
                ui.shadow(photo && glass);
                ui.line(c, Catalog.title(entry), cx, ui.baseline(y + TILE_H / 2), TILE_W - 24, 0);
            } else if ("icons".equals(apps)) {
                stage.drawMark(c, entry, cx - 70, y + 44, 140, 84, ink, a);
                float label = Ui.clamp(e * 2 - 1);
                if (label > 0) {
                    ui.text(face, 28, -0.4f, ink, a * label);
                    ui.shadow(photo && glass);
                    ui.line(c, Catalog.title(entry), cx, ui.baseline(y + 168), TILE_W - 24, 0);
                }
            } else {
                stage.drawMark(c, entry, cx - 60, y + 34, 120, 72, ink, a);
                ui.text(face, 36, -0.8f, ink, a);
                ui.shadow(photo && glass);
                ui.line(c, Catalog.title(entry), cx, ui.baseline(y + 150), TILE_W - 16, 0);
            }
            c.restore();
        }
        c.restore();
    }

    /** The dock for n tiles: as wide as its tiles, centred, and at most as wide as DOCK. */
    private RectF dockArea(int n) {
        float width = Math.min(DOCK.width(), Math.max(1, n) * PITCH - (PITCH - TILE_W) + 2 * PAD);
        dockArea.set(Ui.W / 2 - width / 2, DOCK.top, Ui.W / 2 + width / 2, DOCK.bottom);
        return dockArea;
    }

    private void controls(Canvas c, boolean live, Bitmap blur, String bg, int ink, boolean dock) {
        String mode = stage.prefs.get(Prefs.CONTROLS);
        boolean photo = "photo".equals(bg);
        boolean glass = stage.prefs.is(Prefs.FOCUS, "glass");
        boolean icon = !"text".equals(mode), always = !"icons".equals(mode);
        Typeface face = ui.font(stage.prefs.get(Prefs.FONT), 500);
        float x = RIGHT;
        for (int k = controls.length - 1; k >= 0; k--) {
            float e = v(controls[k], live);
            float a = 0.66f + 0.34f * e;
            ui.text(face, 28, -0.2f, ink, a);
            float textWidth = ui.width(controlNames[k]);
            float width = (icon ? 36 : 0) + (always ? textWidth + (icon ? 14 : 0) : 0);
            float cy = dock ? 104 : 936 - (controls.length - 1 - k) * 64;
            float right = dock ? x : RIGHT;
            float left = right - width;
            if (glass && e > 0) {
                r.set(left - 22, cy - 32, right + 22, cy + 32);
                ui.glass(c, r, 32, blur, bg, stage.prefs.get(Prefs.MATERIAL), e, true);
            }
            // List: the label sits left of the icon. Dock: the icon comes first.
            float iconLeft = dock ? left : right - 36;
            if (icon) ui.mark(c, CONTROL_MARKS[k], iconLeft, cy - 18, 36, 36, ink, a);
            if (always) {
                ui.text(face, 28, -0.2f, ink, a);
                ui.shadow(photo);
                if (dock) ui.line(c, controlNames[k], left + (icon ? 50 : 0), ui.baseline(cy), 0, -1);
                else ui.line(c, controlNames[k], right - (icon ? 50 : 0), ui.baseline(cy), 0, 1);
            } else if (e > 0) {
                // Icons only: the focused control shows its name.
                ui.text(face, 28, -0.2f, ink, e);
                ui.shadow(photo);
                // Dock: the name under the icon. List: left of the icon, and with Glass focus left of its plate.
                if (dock) ui.line(c, controlNames[k], iconLeft + 18, ui.baseline(cy + 60), 0, 0);
                else ui.line(c, controlNames[k], left - (glass ? 22 : 0) - 18, ui.baseline(cy), 0, 1);
            }
            x = left - (always ? 56 : 44);
        }
    }

    /** The hold-OK menu, beside the app in the List and above it in the Dock. */
    private void menu(Canvas c, boolean dock) {
        Typeface face = ui.font("manrope", 500);
        ui.text(face, 30, -0.3f, Ui.PHOTO_INK, 1);
        float w = 0;
        for (int item : menu) w = Math.max(w, ui.width(text(item)));
        w += 96;
        float h = menu.size() * 68 + 24;
        float left, top;
        if (dock) {
            float tile = dockArea(items.size()).left + PAD + focus * PITCH - scroll.value + TILE_W / 2;
            left = tile - w / 2;
            top = TILE_TOP - 24 - h;
        } else {
            left = LEFT - 28 + Math.max(plateWidth.value, rowWidth(items.get(focus), stage.prefs.get(Prefs.APPS),
                ui.font(stage.prefs.get(Prefs.FONT), 600))) + 24;
            top = LIST_TOP + (focus - scroll.value) * ROW + ROW / 2 - h / 2;
        }
        left = Ui.clamp(left, 40, Ui.W - 40 - w);
        top = Ui.clamp(top, 40, Ui.H - 40 - h);
        menuRect.set(left, top, left + w, top + h);
        ui.round(c, menuRect, 28, 0xE6141619);
        for (int i = 0; i < menu.size(); i++) {
            r.set(left + 12, top + 12 + i * 68, left + w - 12, top + 12 + i * 68 + 68);
            if (i == menuIndex) ui.round(c, r, 22, 0x33FFFFFF);
            ui.text(face, 30, -0.3f, Ui.PHOTO_INK, i == menuIndex ? 1 : 0.7f);
            ui.line(c, text(menu.get(i)), r.left + 36, ui.baseline(r.centerY()), 0, -1);
        }
    }

    private void hint(Canvas c, boolean dock) {
        String hint = text(dock ? R.string.move_hint_dock : R.string.move_hint_list);
        ui.text(ui.font("manrope", 500), 30, -0.3f, Ui.PHOTO_INK, 1);
        float w = ui.width(hint);
        r.set(Ui.W / 2 - w / 2 - 32, 960, Ui.W / 2 + w / 2 + 32, 1024);
        ui.round(c, r, 32, 0xB3101214);
        ui.line(c, hint, Ui.W / 2, ui.baseline(992), 0, 0);
    }

    private void clock(Canvas c, boolean dock, int ink, boolean photo) {
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR_OF_DAY), minute = calendar.get(Calendar.MINUTE);
        String time;
        if (stage.prefs.is(Prefs.CLOCK, "12")) time = (hour % 12 == 0 ? 12 : hour % 12) + ":" + two(minute);
        else time = two(hour) + ":" + two(minute);
        ui.text(ui.font(stage.prefs.get(Prefs.FONT), 500), 48, -1.5f, ink, 1);
        ui.shadow(photo);
        if (dock) ui.line(c, time, LEFT, ui.baseline(104), 0, -1);
        else ui.line(c, time, RIGHT, ui.baseline(145), 0, 1);
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : String.valueOf(v);
    }
}
