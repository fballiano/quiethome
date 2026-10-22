// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every installed app and HDMI input in a five-column grid. OK opens. Holding OK closes an app and
 * renames an input. As a picker it chooses one item, for example what a mapped button does.
 * Actions are tiles at the start of the grid, never buttons outside it, so the arrows reach
 * everything the same way.
 */
final class AllAppsScreen extends Screen {
    interface Picker {
        void pick(String id);
    }

    private static final int COLS = 5, VISIBLE_ROWS = 3;
    private static final float LEFT = 140, TOP = 236, TILE_W = 312, TILE_H = 208, PITCH_X = 328, PITCH_Y = 232;

    private final Picker picker;
    /** The tiles before the apps: Edit Home in All apps, the choices that are not apps in a picker. */
    private final List<Catalog.Entry> lead;
    private final String title;
    /** The picker's current choice: it gets the Current badge, and the focus starts on it. */
    private final String current;
    private final List<Catalog.Entry> list = new ArrayList<>();
    private final Ui.Anim scroll = new Ui.Anim(Ui.SCROLL_MS), plateX = new Ui.Anim(Ui.FOCUS_MS), plateY = new Ui.Anim(Ui.FOCUS_MS);
    private final RectF r = new RectF();
    private int focus;
    private boolean snap = true;

    /** All apps: Edit Home, then the apps. The focus starts on the first app. */
    AllAppsScreen(Stage stage) {
        this(stage, null, stage.text(R.string.all_apps),
            Collections.singletonList(new Catalog.Entry(Catalog.ACTION_EDIT, stage.text(R.string.edit_home), null, null)), null);
        if (list.size() > 1) focus = 1;
        targets();
    }

    /** A picker: the lead tiles, then the apps and inputs. */
    AllAppsScreen(Stage stage, Picker picker, String title, List<Catalog.Entry> lead, String current) {
        super(stage);
        this.picker = picker;
        this.title = title;
        this.lead = lead;
        this.current = current;
        refresh();
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(current)) focus = i;
        targets();
    }

    @Override
    void refresh() {
        String focused = focus < list.size() ? list.get(focus).id : null;
        list.clear();
        list.addAll(lead);
        list.addAll(stage.catalog.all());
        if (focus >= list.size()) focus = list.size() - 1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(focused)) focus = i;
        targets();
    }

    @Override
    void closed() {
        stage.catalog.trimIcons(stage.homeScreen.ids());
    }

    @Override
    void move(int dx, int dy) {
        int n = list.size();
        if (dx != 0) {
            int next = focus + dx;
            if (next >= 0 && next < n && next / COLS == focus / COLS) focus = next;
        } else if (dy < 0) {
            if (focus >= COLS) focus -= COLS;
        } else if (dy > 0) {
            if (focus + COLS < n) focus += COLS;
            else if ((n - 1) / COLS > focus / COLS) focus = n - 1;
        }
        targets();
    }

    private void targets() {
        int row = focus / COLS, rows = (list.size() + COLS - 1) / COLS;
        float s = scroll.target;
        if (row < s) s = row;
        if (row > s + VISIBLE_ROWS - 1) s = row - VISIBLE_ROWS + 1;
        scroll.to(Math.max(0, Math.min(s, Math.max(0, rows - VISIBLE_ROWS))));
        plateX.to(focus % COLS);
        plateY.to(row);
    }

    @Override
    boolean holdable() {
        return picker == null && focus < list.size() && !Catalog.isAction(list.get(focus).id);
    }

    @Override
    void ok() {
        if (list.isEmpty()) return;
        Catalog.Entry e = list.get(focus);
        if (picker != null) {
            picker.pick(e.id);
            return;
        }
        if (Catalog.ACTION_EDIT.equals(e.id)) {
            stage.open(new EditorScreen(stage));
            return;
        }
        String error = stage.catalog.launch(e.id);
        if (error != null) stage.toast(error);
        else stage.opening(e);
    }

    @Override
    void okLong() {
        if (!holdable()) return;
        Catalog.Entry e = list.get(focus);
        if (e.isInput()) {
            stage.open(new RenameScreen(stage, e));
            return;
        }
        String error = stage.catalog.close(e.id);
        stage.toast(error != null ? error : text(R.string.closed, Catalog.title(e)));
    }

    @Override
    boolean draw(Canvas c, float dt) {
        boolean moving = false;
        if (snap) {
            snap = false;
            scroll.snap(scroll.target);
            plateX.snap(plateX.target);
            plateY.snap(plateY.target);
        } else {
            moving |= scroll.step(dt);
            moving |= plateX.step(dt);
            moving |= plateY.step(dt);
        }
        String bg = stage.prefs.get(Prefs.BG);
        int ink = Ui.ink(bg);
        ui.sheet(c, stage.blur(), bg);
        Typeface regular = ui.font("manrope", 500);

        ui.text(regular, 56, -1.6f, ink, 1);
        float w = ui.line(c, title, LEFT, ui.baseline(104), 1200, -1);
        ui.text(regular, 26, 0, ink, 0.55f);
        if (picker == null) ui.line(c, String.valueOf(list.size() - lead.size()), LEFT + w + 20, ui.baseline(108), 0, -1);

        c.save();
        c.clipRect(0, TOP - 24, Ui.W, Ui.H);
        float sy = scroll.value;
        if (!list.isEmpty()) {
            float x = LEFT + plateX.value * PITCH_X, y = TOP + (plateY.value - sy) * PITCH_Y;
            r.set(x, y, x + TILE_W, y + TILE_H);
            ui.plate(c, r, 32, bg, 1);
        }
        List<String> home = stage.homeScreen.ids();
        for (int i = 0; i < list.size(); i++) {
            float y = TOP + (i / COLS - sy) * PITCH_Y;
            if (y > Ui.H || y + TILE_H < TOP - 24) continue;
            float x = LEFT + (i % COLS) * PITCH_X, cx = x + TILE_W / 2;
            Catalog.Entry e = list.get(i);
            float a = i == focus ? 1 : 0.78f;
            stage.drawMark(c, e, cx - 60, y + 30, 120, 72, ink, a);
            ui.text(regular, 32, -0.6f, ink, a);
            ui.line(c, Catalog.title(e), cx, ui.baseline(y + 140), TILE_W - 24, 0);
            int state = picker == null ? (home.contains(e.id) ? R.string.on_home : 0)
                : e.id.equals(current) ? R.string.current : Catalog.ACTION_DEFAULT.equals(e.id) ? R.string.its_own_job : 0;
            if (state != 0) {
                ui.text(regular, 24, 0, ink, 0.6f * a);
                ui.line(c, text(state), cx, ui.baseline(y + 182), 0, 0);
            }
        }
        c.restore();
        return moving;
    }
}
