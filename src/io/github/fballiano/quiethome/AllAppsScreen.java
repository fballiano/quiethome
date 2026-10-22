package io.github.fballiano.quiethome;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.List;

/**
 * Every installed app and HDMI input in a five-column grid. OK opens, holding OK closes, and Up
 * from the first row reaches Apps on Home. As a picker it chooses what a mapped button does:
 * the button actions come first, then the apps and inputs.
 */
final class AllAppsScreen extends Screen {
    interface Picker {
        void pick(String id);

        void remove();
    }

    private static final int COLS = 5, VISIBLE_ROWS = 3;
    private static final float LEFT = 140, TOP = 236, TILE_W = 312, TILE_H = 208, PITCH_X = 328, PITCH_Y = 232;

    private final Picker picker;
    private final boolean actions;
    private final String title;
    private final String assigned;
    private final List<Catalog.Entry> list = new ArrayList<>();
    private final Ui.Anim scroll = new Ui.Anim(120), plateX = new Ui.Anim(80), plateY = new Ui.Anim(80);
    private final RectF r = new RectF();
    private int focus, lastFocus;
    private boolean snap = true;

    AllAppsScreen(Stage stage, Picker picker) {
        this(stage, picker, "All apps", null);
    }

    /** A picker; assigned is the current choice, which gets a Remove button. */
    AllAppsScreen(Stage stage, Picker picker, String title, String assigned) {
        this(stage, picker, title, assigned, true);
    }

    /** A picker that offers only apps and inputs, without the button actions. */
    AllAppsScreen(Stage stage, Picker picker, String title, String assigned, boolean actions) {
        super(stage);
        this.actions = actions;
        this.picker = picker;
        this.title = title;
        this.assigned = assigned;
        refresh();
        if (assigned != null) for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(assigned)) focus = i;
        targets();
    }

    private boolean hasButton() {
        return picker == null || assigned != null;
    }

    @Override
    void refresh() {
        String focused = focus >= 0 && focus < list.size() ? list.get(focus).id : null;
        list.clear();
        if (picker != null && actions) list.addAll(Catalog.actions());
        list.addAll(stage.catalog.all());
        if (focus >= list.size()) focus = list.size() - 1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).id.equals(focused)) focus = i;
        if (list.isEmpty()) focus = hasButton() ? -1 : 0;
        targets();
    }

    @Override
    void closed() {
        stage.catalog.trimIcons(stage.homeScreen.ids());
    }

    @Override
    void move(int dx, int dy) {
        int n = list.size();
        if (focus < 0) {
            if (dy > 0 && n > 0) focus = Math.min(lastFocus, n - 1);
        } else if (dx != 0) {
            int next = focus + dx;
            if (next >= 0 && next < n && next / COLS == focus / COLS) focus = next;
        } else if (dy < 0) {
            if (focus >= COLS) focus -= COLS;
            else if (hasButton()) {
                lastFocus = focus;
                focus = -1;
            }
        } else if (dy > 0) {
            if (focus + COLS < n) focus += COLS;
            else if ((n - 1) / COLS > focus / COLS) focus = n - 1;
        }
        targets();
    }

    private void targets() {
        if (focus < 0) return;
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
        return picker == null && focus >= 0;
    }

    @Override
    void ok() {
        if (focus < 0) {
            if (picker != null) picker.remove();
            else stage.open(new EditorScreen(stage));
            return;
        }
        if (list.isEmpty()) return;
        Catalog.Entry e = list.get(focus);
        if (picker != null) {
            picker.pick(e.id);
            return;
        }
        String error = stage.catalog.launch(e.id);
        if (error != null) stage.toast(error);
    }

    @Override
    void okLong() {
        if (focus < 0 || list.isEmpty()) return;
        Catalog.Entry e = list.get(focus);
        String error = stage.catalog.close(e.id);
        stage.toast(error != null ? error : "Closed " + Catalog.title(e) + ".");
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
        if (picker == null) ui.line(c, String.valueOf(list.size()), LEFT + w + 20, ui.baseline(108), 0, -1);
        if (hasButton()) {
            String label = picker == null ? "Apps on Home" : "Remove";
            ui.text(regular, 30, -0.3f, ink, 1);
            float bw = ui.width(label) + 60;
            r.set(LEFT + 1640 - bw, 72, LEFT + 1640, 136);
            if (focus < 0) ui.plate(c, r, 32, bg, 1);
            ui.text(regular, 30, -0.3f, ink, focus < 0 ? 1 : 0.7f);
            ui.line(c, label, r.centerX(), ui.baseline(r.centerY()), 0, 0);
        }

        c.save();
        c.clipRect(0, TOP - 24, Ui.W, Ui.H);
        float sy = scroll.value;
        if (focus >= 0) {
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
            String state = picker == null ? (home.contains(e.id) ? "On Home" : null) : (e.id.equals(assigned) ? "Assigned" : null);
            if (state != null) {
                ui.text(regular, 24, 0, ink, 0.6f * a);
                ui.line(c, state, cx, ui.baseline(y + 182), 0, 0);
            }
        }
        if (list.isEmpty()) {
            ui.text(regular, 36, -0.6f, ink, 0.7f);
            ui.line(c, "No apps found.", LEFT, ui.baseline(TOP + 60), 0, -1);
        }
        c.restore();
        return moving;
    }
}
