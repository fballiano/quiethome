// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apps on Home: the Home order on the left, everything else on the right. OK on an app name picks
 * it up, Up and Down move it, OK places it. Every completed change is saved at once and can be undone.
 */
final class EditorScreen extends Screen {
    private static final int HEADER = 0, NAME = 1, REMOVE = 2, OTHER = 3;
    private static final float TOP = 290, ROW = 76, RIGHT_LEFT = 1000;
    private static final int VISIBLE = 9;

    private List<String> home;
    private final List<String> removed = new ArrayList<>();
    private final Map<String, Integer> removedAt = new HashMap<>();
    private final List<List<String>> undo = new ArrayList<>();
    private final List<String> others = new ArrayList<>();
    private final Ui.Anim scroll = new Ui.Anim(Ui.SCROLL_MS);
    private final RectF r = new RectF();
    private final Path arrow = new Path();
    private int zone = NAME, index, button = 1;
    private boolean lifted;
    private List<String> beforeLift;

    EditorScreen(Stage stage) {
        super(stage);
        home = stage.homeScreen.ids();
        others();
        if (home.isEmpty()) zone = OTHER;
    }

    @Override
    void refresh() {
        List<String> installed = new ArrayList<>();
        for (String id : home) if (stage.catalog.get(id) != null) installed.add(id);
        home = installed;
        others();
        clampIndex();
    }

    /** Recently removed apps first, then the rest by name. */
    private void others() {
        others.clear();
        for (String id : removed) if (!home.contains(id) && stage.catalog.get(id) != null) others.add(id);
        for (Catalog.Entry e : stage.catalog.all()) if (!home.contains(e.id) && !others.contains(e.id)) others.add(e.id);
    }

    private void save(List<String> before) {
        undo.add(before);
        stage.prefs.setHome(home);
        stage.homeScreen.refresh();
        others();
    }

    private void clampIndex() {
        int size = zone == OTHER ? others.size() : home.size();
        if (size == 0 && zone != HEADER) {
            zone = zone == OTHER ? (home.isEmpty() ? HEADER : NAME) : (others.isEmpty() ? HEADER : OTHER);
            size = zone == OTHER ? others.size() : home.size();
        }
        index = Math.max(0, Math.min(index, size - 1));
        if (zone == OTHER) {
            float s = scroll.target;
            if (index < s) s = index;
            if (index > s + VISIBLE - 1) s = index - VISIBLE + 1;
            scroll.to(Math.max(0, Math.min(s, Math.max(0, others.size() - VISIBLE))));
        }
    }

    @Override
    void move(int dx, int dy) {
        if (lifted) {
            int to = index + dy;
            if (dy != 0 && to >= 0 && to < home.size()) {
                home.add(to, home.remove(index));
                index = to;
                stage.homeScreen.refresh();
            }
            return;
        }
        switch (zone) {
            case HEADER:
                if (dx != 0) button = Math.max(0, Math.min(1, button + dx));
                if (dy > 0) {
                    zone = home.isEmpty() ? OTHER : NAME;
                    index = 0;
                }
                break;
            case NAME:
            case REMOVE:
                if (dy != 0) {
                    if (index + dy < 0) zone = HEADER;
                    else index = Math.min(home.size() - 1, index + dy);
                } else if (dx > 0) {
                    if (zone == NAME) zone = REMOVE;
                    else if (!others.isEmpty()) {
                        zone = OTHER;
                        index = Math.round(scroll.target) + Math.min(index, VISIBLE - 1);
                    }
                } else if (dx < 0 && zone == REMOVE) {
                    zone = NAME;
                }
                break;
            case OTHER:
                if (dy != 0) {
                    if (index + dy < 0) zone = HEADER;
                    else index = Math.min(others.size() - 1, index + dy);
                } else if (dx < 0 && !home.isEmpty()) {
                    index = Math.max(0, index - Math.round(scroll.target));
                    zone = NAME;
                }
                break;
        }
        clampIndex();
    }

    @Override
    void ok() {
        switch (zone) {
            case HEADER:
                if (button == 1) stage.close(this);
                else if (!undo.isEmpty()) {
                    home = undo.remove(undo.size() - 1);
                    stage.prefs.setHome(home);
                    stage.homeScreen.refresh();
                    others();
                    clampIndex();
                }
                break;
            case NAME:
                if (lifted) {
                    lifted = false;
                    if (!home.equals(beforeLift)) save(beforeLift);
                } else {
                    lifted = true;
                    beforeLift = new ArrayList<>(home);
                }
                break;
            case REMOVE:
                if (home.size() <= 1) {
                    stage.toast(text(R.string.home_needs_one));
                    break;
                }
                List<String> before = new ArrayList<>(home);
                String id = home.remove(index);
                removed.remove(id);
                removed.add(0, id);
                removedAt.put(id, index);
                save(before);
                zone = OTHER;
                index = others.indexOf(id);
                break;
            case OTHER:
                if (home.size() >= Home.MAX_HOME) {
                    stage.toast(text(R.string.home_full));
                    break;
                }
                before = new ArrayList<>(home);
                id = others.get(index);
                Integer at = removedAt.get(id);
                int position = at == null ? home.size() : Math.min(at, home.size());
                home.add(position, id);
                removed.remove(id);
                save(before);
                zone = REMOVE;
                index = position;
                break;
        }
        clampIndex();
    }

    @Override
    void back() {
        if (lifted) {
            lifted = false;
            home = beforeLift;
            stage.homeScreen.refresh();
            clampIndex();
            return;
        }
        stage.close(this);
    }

    @Override
    boolean draw(Canvas c, float dt) {
        boolean moving = scroll.step(dt);
        String bg = stage.prefs.get(Prefs.BG);
        int ink = Ui.ink(bg);
        ui.sheet(c, stage.blur(), bg);
        Typeface regular = ui.font("manrope", 500);

        ui.text(regular, 56, -1.6f, ink, 1);
        ui.line(c, text(R.string.apps_on_home), 140, ui.baseline(104), 0, -1);
        ui.text(regular, 28, -0.2f, ink, 0.66f);
        ui.line(c, text(lifted ? R.string.editor_hint_lifted : R.string.editor_hint), 140, ui.baseline(176), 1200, -1);

        String[] buttons = {text(R.string.undo), text(R.string.done)};
        float x = 1780;
        for (int i = 1; i >= 0; i--) {
            ui.text(regular, 30, -0.3f, ink, 1);
            float w = ui.width(buttons[i]) + 60;
            r.set(x - w, 72, x, 136);
            if (zone == HEADER && button == i) ui.plate(c, r, 32, bg, 1);
            boolean dim = i == 0 && undo.isEmpty();
            ui.text(regular, 30, -0.3f, ink, dim ? 0.35f : zone == HEADER && button == i ? 1 : 0.7f);
            ui.line(c, buttons[i], r.centerX(), ui.baseline(r.centerY()), 0, 0);
            x -= w + 12;
        }

        ui.text(regular, 26, 0, ink, 0.55f);
        ui.line(c, text(R.string.on_home_count, home.size(), Home.MAX_HOME), 140, ui.baseline(TOP - 30), 0, -1);
        ui.line(c, text(R.string.not_on_home), RIGHT_LEFT, ui.baseline(TOP - 30), 0, -1);

        String remove = text(R.string.remove);
        ui.text(regular, 28, -0.2f, ink, 1);
        float removeWidth = ui.width(remove) + 48;
        // Remove starts at 816. A longer word moves it left, so it never reaches the list on the right.
        float removeLeft = Math.min(816, RIGHT_LEFT - 30 - removeWidth);
        float nameRight = Math.min(800, removeLeft - 16);
        for (int i = 0; i < home.size(); i++) {
            Catalog.Entry e = stage.catalog.get(home.get(i));
            if (e == null) continue;
            float y = TOP + i * ROW;
            boolean focused = zone == NAME && index == i;
            if (focused) {
                r.set(116, y + 4, nameRight, y + ROW - 4);
                ui.plate(c, r, 28, bg, 1);
                if (lifted) {
                    ui.fill.setShader(null);
                    ui.fill.setColor("light".equals(bg) ? 0x14000000 : 0x2EFFFFFF);
                    c.drawRoundRect(r, 28, 28, ui.fill);
                    arrows(c, 140, y + ROW / 2, ink);
                }
            }
            ui.text(regular, 36, -0.6f, ink, focused || zone != NAME ? 1 : 0.8f);
            float nameLeft = lifted && focused ? 180 : 148;
            ui.line(c, Catalog.title(e), nameLeft, ui.baseline(y + ROW / 2), Math.min(600, nameRight - 52 - nameLeft), -1);

            r.set(removeLeft, y + 8, removeLeft + removeWidth, y + ROW - 8);
            boolean removeFocused = zone == REMOVE && index == i;
            if (removeFocused) ui.plate(c, r, 30, bg, 1);
            ui.text(regular, 28, -0.2f, ink, removeFocused ? 1 : 0.45f);
            ui.line(c, remove, r.centerX(), ui.baseline(r.centerY()), 0, 0);
        }

        c.save();
        c.clipRect(RIGHT_LEFT - 30, TOP, Ui.W, TOP + VISIBLE * ROW + 8);
        float sy = scroll.value;
        for (int i = 0; i < others.size(); i++) {
            float y = TOP + (i - sy) * ROW;
            if (y < TOP - ROW || y > TOP + VISIBLE * ROW) continue;
            Catalog.Entry e = stage.catalog.get(others.get(i));
            if (e == null) continue;
            boolean focused = zone == OTHER && index == i;
            if (focused) {
                r.set(RIGHT_LEFT - 24, y + 4, 1804, y + ROW - 4);
                ui.plate(c, r, 28, bg, 1);
            }
            ui.text(regular, 36, -0.6f, ink, focused ? 1 : 0.7f);
            ui.line(c, Catalog.title(e), RIGHT_LEFT + 8, ui.baseline(y + ROW / 2), 560, -1);
            int state = removed.contains(e.id) ? R.string.removed : e.isInput() ? R.string.input : 0;
            if (state != 0) {
                ui.text(regular, 24, 0, ink, 0.5f);
                ui.line(c, text(state), 1780, ui.baseline(y + ROW / 2), 0, 1);
            }
        }
        c.restore();
        return moving;
    }

    /** Small up and down triangles beside a picked-up app. */
    private void arrows(Canvas c, float cx, float cy, int ink) {
        arrow.reset();
        arrow.moveTo(cx, cy - 22);
        arrow.lineTo(cx + 10, cy - 8);
        arrow.lineTo(cx - 10, cy - 8);
        arrow.close();
        arrow.moveTo(cx, cy + 22);
        arrow.lineTo(cx + 10, cy + 8);
        arrow.lineTo(cx - 10, cy + 8);
        arrow.close();
        ui.fill.setShader(null);
        ui.fill.setColor(ink);
        c.drawPath(arrow, ui.fill);
    }
}
