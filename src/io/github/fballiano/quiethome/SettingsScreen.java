// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Settings: groups on the left, the committed Home on the right. OK applies a choice at once and
 * saves it; there is no Save button. Back leaves one level at a time.
 */
final class SettingsScreen extends Screen {
    private static final int CHOICE = 0, LINK = 1, OPTION = 2, ACTION = 3;
    private static final float LEFT = 140, COLUMN = 680, TOP = 236;
    private static final RectF PREVIEW = new RectF(900, 236, 1780, 731);

    private final class Row {
        final int type;
        final String label;
        String key;
        String[] values, names;
        String value;
        Runnable run;
        Supplier<String> note;
        BooleanSupplier active;
        String inactive;
        String font;
        Wallpapers.Wallpaper wallpaper;

        Row(int type, String label) {
            this.type = type;
            this.label = label;
        }

        boolean isActive() {
            return active == null || active.getAsBoolean();
        }

        float height() {
            switch (type) {
                case CHOICE:
                    return note != null || !isActive() ? 168 : 132;
                case OPTION:
                    return wallpaper != null ? 112 : 80;
                default:
                    return 108;
            }
        }

        int applied() {
            String current = stage.prefs.get(key);
            for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return i;
            return 0;
        }
    }

    private final class Page {
        final String title;
        final List<Row> rows = new ArrayList<>();
        final Ui.Anim scroll = new Ui.Anim(Ui.SCROLL_MS);
        Supplier<List<Row>> builder;
        boolean wallpapers;
        int focus, choice;

        Page(String title) {
            this.title = title;
        }
    }

    private final List<Page> pages = new ArrayList<>();
    private final RectF r = new RectF();
    private final Bitmap[] thumbs = new Bitmap[Wallpapers.ALL.length];
    private Bitmap candidate, candidateBlur;
    private String candidateId;
    private final String version;
    private final Runnable loadCandidate = this::loadCandidate;

    SettingsScreen(Stage stage) {
        super(stage);
        version = "QuietHome " + versionName();
        push(root());
    }

    private String versionName() {
        try {
            Context c = stage.getContext();
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    // ---- Pages ----

    private Page root() {
        Page p = new Page("Settings");
        p.rows.add(link("Background", this::backgroundSummary, () -> push(background())));
        p.rows.add(link("Layout", this::layoutSummary, () -> push(layout())));
        p.rows.add(link("Typography", () -> name(Prefs.FONT), () -> push(typography())));
        p.rows.add(link("Focus", this::focusSummary, () -> push(focus())));
        p.rows.add(link("Apps on Home", () -> stage.homeScreen.items.size() + " of " + Home.MAX_HOME + " apps",
            () -> stage.open(new EditorScreen(stage))));
        p.rows.add(buttonMapping());
        p.rows.add(link("Android TV", () -> "The service, the Home button and when the TV turns on", () -> push(android())));
        return p;
    }

    private Page background() {
        Page p = new Page("Background");
        p.rows.add(choice("Background", Prefs.BG, "Photo", "Light", "Dark"));
        Row wallpaper = link("Wallpaper", () -> Wallpapers.find(stage.prefs.get(Prefs.WALLPAPER)).name, () -> push(wallpapers()));
        wallpaper.active = this::isPhoto;
        wallpaper.inactive = "Choose Photo to pick a wallpaper.";
        p.rows.add(wallpaper);
        Row rotation = link("Rotation", () -> name(Prefs.ROTATION), () -> push(rotation()));
        rotation.active = this::isPhoto;
        rotation.inactive = "Choose Photo to rotate wallpapers.";
        p.rows.add(rotation);
        return p;
    }

    private Page wallpapers() {
        Page p = new Page("Wallpaper");
        p.wallpapers = true;
        for (Wallpapers.Wallpaper w : Wallpapers.ALL) {
            Row row = option(w.name, Prefs.WALLPAPER, w.id);
            row.wallpaper = w;
            p.rows.add(row);
        }
        return p;
    }

    private Page rotation() {
        Page p = new Page("Rotation");
        String[] values = Prefs.CHOICES.get(Prefs.ROTATION);
        for (int i = 0; i < values.length; i++) p.rows.add(option(ROTATION_NAMES[i], Prefs.ROTATION, values[i]));
        return p;
    }

    private Page layout() {
        Page p = new Page("Layout");
        p.rows.add(choice("Layout", Prefs.LAYOUT, "List", "Dock"));
        p.rows.add(choice("Apps", Prefs.APPS, "Both", "Names", "Icons"));
        p.rows.add(choice("Controls", Prefs.CONTROLS, "Both", "Names", "Icons"));
        p.rows.add(choice("Clock", Prefs.CLOCK, "24-hour", "12-hour"));
        return p;
    }

    private Page typography() {
        Page p = new Page("Typography");
        String[] values = Prefs.CHOICES.get(Prefs.FONT);
        for (String v : values) {
            Row row = option(FONT_NAMES[indexOf(values, v)], Prefs.FONT, v);
            row.font = v;
            p.rows.add(row);
        }
        return p;
    }

    private Page focus() {
        Page p = new Page("Focus");
        p.rows.add(choice("Focus", Prefs.FOCUS, "Type", "Glass"));
        Row material = choice("Material", Prefs.MATERIAL, "Soft", "Frost", "Clear");
        material.active = () -> isPhoto() && (stage.prefs.is(Prefs.LAYOUT, "dock") || stage.prefs.is(Prefs.FOCUS, "glass"));
        material.inactive = "Used on a photo, by the Dock or by Glass focus.";
        p.rows.add(material);
        return p;
    }

    private Page android() {
        Page p = new Page("Android TV");
        p.rows.add(action("QuietHome service", () -> ServiceSetup.isOn(stage.getContext())
                ? "On: Home button, button mapping and start input work" : "Off · select to turn it on",
            () -> stage.toast(ServiceSetup.isOn(stage.getContext()) ? "The QuietHome service is on."
                : ServiceSetup.openSettings(stage.getContext()))));
        p.rows.add(action("TV settings", () -> "Android's own settings", () -> stage.toast(stage.catalog.openSettings())));
        Row homeKey = choice("Home button", Prefs.HOME_KEY, "QuietHome", "Android");
        homeKey.note = () -> !RemoteButtons.running ? "Needs the QuietHome service."
            : stage.prefs.is(Prefs.HOME_KEY, "quiethome") ? "Home always opens QuietHome." : "Home opens what Android chooses.";
        p.rows.add(homeKey);
        p.rows.add(link("When the TV turns on", this::startOnSummary, this::pickStartOn));
        p.rows.add(action("Previous launcher", () -> "Opens the TV's other home screen",
            () -> stage.toast(stage.catalog.openPreviousLauncher())));
        return p;
    }

    private Row buttonMapping() {
        return link("Button mapping", () -> RemoteButtons.running
            ? stage.prefs.buttons().size() + " assigned" : "Needs the QuietHome service", () -> push(buttons()));
    }

    private Page buttons() {
        Page p = new Page("Button mapping");
        p.builder = () -> {
            List<Row> rows = new ArrayList<>();
            if (!RemoteButtons.running) {
                rows.add(action("Turn on the QuietHome service", () -> "Button mapping needs it",
                    () -> stage.toast(ServiceSetup.openSettings(stage.getContext()))));
            }
            for (Map.Entry<Integer, String> b : stage.prefs.buttons().entrySet()) {
                int code = b.getKey();
                Catalog.Entry target = stage.catalog.get(b.getValue());
                String opens = target == null ? b.getValue() : Catalog.title(target);
                String does = Catalog.ACTION_NONE.equals(b.getValue()) ? "Does nothing" : "Opens " + opens;
                rows.add(link(RemoteButtons.name(code), () -> does, () -> pick(code)));
            }
            Row add = action("Add a button", () -> "Press it, then choose what it does",
                () -> stage.open(new LearnScreen(stage, this::pick)));
            add.active = () -> RemoteButtons.running;
            add.inactive = "Turn on the QuietHome service first.";
            rows.add(add);
            return rows;
        };
        return p;
    }

    private String startOnSummary() {
        String id = stage.prefs.get(Prefs.START_ON);
        if (id.isEmpty()) return "Show Home";
        Catalog.Entry e = stage.catalog.get(id);
        String open = "Open " + (e == null ? id : Catalog.title(e));
        return RemoteButtons.running ? open : open + " · needs the QuietHome service";
    }

    /** For a TV that only shows a box on HDMI: open that input (or an app) at boot and on wake. */
    private void pickStartOn() {
        String current = stage.prefs.get(Prefs.START_ON);
        List<Catalog.Entry> lead = Collections.singletonList(new Catalog.Entry(Catalog.ACTION_HOME, "Show Home", null, null));
        AllAppsScreen[] picker = new AllAppsScreen[1];
        picker[0] = new AllAppsScreen(stage, id -> {
            boolean home = Catalog.ACTION_HOME.equals(id);
            stage.prefs.set(Prefs.START_ON, home ? "" : id);
            stage.close(picker[0]);
            Catalog.Entry e = stage.catalog.get(id);
            stage.toast(home ? "The TV shows Home when it turns on."
                : "The TV opens " + (e == null ? id : Catalog.title(e)) + " when it turns on.");
        }, "Open when the TV turns on", lead, current.isEmpty() ? Catalog.ACTION_HOME : current);
        stage.open(picker[0]);
    }

    /** Chooses, changes or removes what a mapped button does. */
    private void pick(int code) {
        String name = RemoteButtons.name(code);
        String current = stage.prefs.buttons().get(code);
        List<Catalog.Entry> lead = new ArrayList<>();
        lead.add(new Catalog.Entry(Catalog.ACTION_DEFAULT, "Default", null, null));
        lead.addAll(Catalog.actions());
        AllAppsScreen[] picker = new AllAppsScreen[1];
        picker[0] = new AllAppsScreen(stage, id -> {
            boolean free = Catalog.ACTION_DEFAULT.equals(id);
            stage.prefs.setButton(code, free ? null : id);
            stage.close(picker[0]);
            Catalog.Entry e = stage.catalog.get(id);
            stage.toast(free ? name + " does its own job again."
                : Catalog.ACTION_NONE.equals(id) ? name + " now does nothing."
                : name + " opens " + (e == null ? id : Catalog.title(e)) + ".");
        }, "Choose what " + name + " does", lead, current == null ? Catalog.ACTION_DEFAULT : current);
        stage.open(picker[0]);
    }

    private static final String[] ROTATION_NAMES = {"Fixed", "Each time Home opens", "Every 5 minutes", "Every 15 minutes", "Every hour"};
    private static final String[] FONT_NAMES = {"Manrope", "Geist", "Newsreader"};

    private Row choice(String label, String key, String... names) {
        Row row = new Row(CHOICE, label);
        row.key = key;
        row.values = Prefs.CHOICES.get(key);
        row.names = names;
        return row;
    }

    private Row option(String label, String key, String value) {
        Row row = new Row(OPTION, label);
        row.key = key;
        row.value = value;
        return row;
    }

    private Row link(String label, Supplier<String> note, Runnable run) {
        Row row = new Row(LINK, label);
        row.note = note;
        row.run = run;
        return row;
    }

    private Row action(String label, Supplier<String> note, Runnable run) {
        Row row = new Row(ACTION, label);
        row.note = note;
        row.run = run;
        return row;
    }

    private boolean isPhoto() {
        return stage.prefs.is(Prefs.BG, "photo");
    }

    private String name(String key) {
        String[] values = Prefs.CHOICES.get(key);
        int i = indexOf(values, stage.prefs.get(key));
        if (Prefs.FONT.equals(key)) return FONT_NAMES[i];
        if (Prefs.ROTATION.equals(key)) return ROTATION_NAMES[i];
        return values[i];
    }

    private String backgroundSummary() {
        String bg = stage.prefs.get(Prefs.BG);
        if ("photo".equals(bg)) return "Photo · " + Wallpapers.find(stage.prefs.get(Prefs.WALLPAPER)).name;
        return "light".equals(bg) ? "Light" : "Dark";
    }

    private String layoutSummary() {
        String apps = stage.prefs.get(Prefs.APPS);
        return (stage.prefs.is(Prefs.LAYOUT, "dock") ? "Dock" : "List") + " · "
            + ("both".equals(apps) ? "Names and icons" : "text".equals(apps) ? "Names" : "Icons")
            + " · " + stage.prefs.get(Prefs.CLOCK) + "-hour clock";
    }

    private String focusSummary() {
        if (stage.prefs.is(Prefs.FOCUS, "type")) return "Type";
        String m = stage.prefs.get(Prefs.MATERIAL);
        return "Glass · " + Character.toUpperCase(m.charAt(0)) + m.substring(1);
    }

    private static int indexOf(String[] values, String v) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(v)) return i;
        return 0;
    }

    private Page page() {
        return pages.get(pages.size() - 1);
    }

    private void push(Page p) {
        if (p.builder != null) p.rows.addAll(p.builder.get());
        pages.add(p);
        p.focus = -1;
        focusRow(p, 0, 1);
        // A list of options opens on the applied one.
        for (int i = 0; i < p.rows.size(); i++) {
            Row row = p.rows.get(i);
            if (row.type == OPTION && stage.prefs.is(row.key, row.value)) p.focus = i;
        }
        if (p.wallpapers) {
            for (int i = 0; i < thumbs.length; i++) {
                thumbs[i] = Wallpapers.decodeSmall(stage.getContext().getAssets(), Wallpapers.ALL[i].thumb(), 160);
            }
        }
        p.scroll.snap(scrollFor(p));
        stage.invalidate();
    }

    private void pop() {
        Page p = pages.remove(pages.size() - 1);
        if (p.wallpapers) {
            stage.removeCallbacks(loadCandidate);
            for (int i = 0; i < thumbs.length; i++) {
                Ui.free(thumbs[i]);
                thumbs[i] = null;
            }
            Ui.free(candidate);
            Ui.free(candidateBlur);
            candidate = null;
            candidateBlur = null;
            candidateId = null;
        }
    }

    /** Moves focus to the next active row from start in direction dir (start included). */
    private void focusRow(Page p, int start, int dir) {
        for (int i = start; i >= 0 && i < p.rows.size(); i += dir) {
            if (p.rows.get(i).isActive()) {
                p.focus = i;
                Row row = p.rows.get(i);
                if (row.type == CHOICE) p.choice = row.applied();
                return;
            }
        }
    }

    // ---- Keys ----

    @Override
    void move(int dx, int dy) {
        Page p = page();
        if (p.focus < 0) return;
        Row row = p.rows.get(p.focus);
        if (dy != 0) {
            int before = p.focus;
            focusRow(p, p.focus + dy, dy);
            if (before != p.focus) {
                p.scroll.to(scrollFor(p));
                if (p.wallpapers) {
                    stage.removeCallbacks(loadCandidate);
                    stage.postDelayed(loadCandidate, 220);
                }
            }
        } else if (row.type == CHOICE && row.isActive()) {
            p.choice = Math.max(0, Math.min(row.values.length - 1, p.choice + dx));
        }
    }

    @Override
    void ok() {
        Page p = page();
        if (p.focus < 0) return;
        Row row = p.rows.get(p.focus);
        if (!row.isActive()) return;
        switch (row.type) {
            case CHOICE:
                stage.apply(row.key, row.values[p.choice]);
                break;
            case OPTION:
                stage.apply(row.key, row.value);
                break;
            default:
                row.run.run();
        }
    }

    @Override
    void back() {
        if (pages.size() > 1) pop();
        else stage.close(this);
    }

    @Override
    void closed() {
        while (!pages.isEmpty()) pop();
    }

    @Override
    void refresh() {
        Page p = page();
        if (p.builder != null) rebuild(p);
    }

    private void rebuild(Page p) {
        p.rows.clear();
        p.rows.addAll(p.builder.get());
        if (p.focus >= p.rows.size()) p.focus = p.rows.size() - 1;
        if (p.focus >= 0 && !p.rows.get(p.focus).isActive()) focusRow(p, 0, 1);
    }

    private float scrollFor(Page p) {
        float y = 0, focusTop = 0, focusBottom = 0, total = 0;
        for (int i = 0; i < p.rows.size(); i++) {
            float h = p.rows.get(i).height();
            if (i == p.focus) {
                focusTop = y;
                focusBottom = y + h;
            }
            y += h;
        }
        total = y;
        float window = Ui.H - TOP - 40;
        float s = p.scroll.target;
        if (focusTop - s < 0) s = focusTop;
        if (focusBottom - s > window) s = focusBottom - window;
        return Math.max(0, Math.min(s, Math.max(0, total - window)));
    }

    private void loadCandidate() {
        Page p = page();
        if (!p.wallpapers || p.focus < 0) return;
        Wallpapers.Wallpaper w = p.rows.get(p.focus).wallpaper;
        if (w.id.equals(candidateId)) return;
        Bitmap oldCandidate = candidate, oldBlur = candidateBlur;
        candidate = Wallpapers.decodeSmall(stage.getContext().getAssets(), w.photo(), (int) PREVIEW.width());
        candidateBlur = candidate == null ? null : Wallpapers.blur(candidate);
        candidateId = w.id;
        Ui.free(oldCandidate);
        Ui.free(oldBlur);
        stage.invalidate();
    }

    // ---- Drawing ----

    @Override
    boolean draw(Canvas c, float dt) {
        Page p = page();
        // Button mapping can change while Settings is open (the service turns on, a button is assigned).
        if (p.builder != null) rebuild(p);
        boolean moving = p.scroll.step(dt);
        String bg = stage.prefs.get(Prefs.BG);
        int ink = Ui.ink(bg);
        ui.sheet(c, stage.blur(), bg);
        Typeface regular = ui.font("manrope", 500);

        ui.text(regular, 56, -1.6f, ink, 1);
        ui.line(c, p.title, LEFT, ui.baseline(104), 0, -1);
        // Under the title: the parent page, or on the first page the version.
        ui.text(regular, 26, 0, ink, 0.5f);
        ui.line(c, pages.size() > 1 ? pages.get(pages.size() - 2).title : version, LEFT, ui.baseline(158), 0, -1);

        preview(c, p, ink);

        c.save();
        c.clipRect(0, TOP - 16, PREVIEW.left - 24, Ui.H);
        float y = TOP - p.scroll.value;
        for (int i = 0; i < p.rows.size(); i++) {
            Row row = p.rows.get(i);
            float h = row.height();
            if (y + h > TOP - 16 && y < Ui.H) drawRow(c, row, i == p.focus, p.choice, y, h, bg, ink);
            y += h;
        }
        c.restore();
        return moving;
    }

    private void drawRow(Canvas c, Row row, boolean focused, int choice, float y, float h, String bg, int ink) {
        Typeface regular = ui.font("manrope", 500);
        boolean active = row.isActive();
        float dim = active ? 1 : 0.4f;
        switch (row.type) {
            case CHOICE: {
                ui.text(regular, 26, 0, ink, 0.6f * dim);
                ui.line(c, row.label, LEFT, y + 30, 0, -1);
                int applied = row.applied();
                float x = LEFT - 4;
                for (int j = 0; j < row.names.length; j++) {
                    ui.text(regular, 30, -0.3f, ink, 1);
                    float w = ui.width(row.names[j]) + 52 + (j == applied ? 36 : 0);
                    r.set(x, y + 48, x + w, y + 112);
                    boolean on = focused && j == choice;
                    if (on) ui.plate(c, r, 32, bg, 1);
                    float a = (on || j == applied ? 1 : 0.62f) * dim;
                    float tx = x + 26;
                    if (j == applied) {
                        ui.check(c, tx - 4, r.centerY() - 14, 28, ink, a);
                        tx += 36;
                    }
                    ui.text(regular, 30, -0.3f, ink, a);
                    ui.line(c, row.names[j], tx, ui.baseline(r.centerY()), 0, -1);
                    x += w + 8;
                }
                String note = active ? row.note == null ? null : row.note.get() : row.inactive;
                if (note != null) {
                    ui.text(regular, 26, 0, ink, 0.6f);
                    ui.line(c, note, LEFT, y + 150, COLUMN, -1);
                }
                break;
            }
            case OPTION: {
                r.set(LEFT - 24, y + 4, LEFT + COLUMN + 24, y + h - 4);
                if (focused) ui.plate(c, r, 28, bg, 1);
                boolean applied = stage.prefs.is(row.key, row.value);
                if (applied) ui.check(c, LEFT - 2, y + h / 2 - 15, 30, ink, 1);
                float x = LEFT + 44;
                if (row.wallpaper != null) {
                    int i = indexOf(wallpaperIds(), row.wallpaper.id);
                    if (thumbs[i] != null) {
                        r.set(x, y + 14, x + 150, y + h - 14);
                        ui.photo(c, thumbs[i], r);
                    }
                    x += 174;
                }
                Typeface face = row.font != null ? ui.font(row.font, 500) : regular;
                ui.text(face, row.font != null ? 44 : 36, -0.6f, ink, focused || applied ? 1 : 0.72f);
                ui.line(c, row.label, x, ui.baseline(y + h / 2), LEFT + COLUMN - x, -1);
                break;
            }
            default: {
                r.set(LEFT - 24, y + 4, LEFT + COLUMN + 24, y + h - 4);
                if (focused && active) ui.plate(c, r, 28, bg, 1);
                ui.text(regular, 38, -0.6f, ink, (focused ? 1 : 0.86f) * dim);
                ui.line(c, row.label, LEFT, ui.baseline(y + 40), COLUMN - 40, -1);
                String note = active ? row.note == null ? null : row.note.get() : row.inactive;
                if (note != null) {
                    ui.text(regular, 26, 0, ink, 0.6f * (active ? 1 : 0.8f));
                    ui.line(c, note, LEFT, ui.baseline(y + 80), COLUMN - 40, -1);
                }
                if (row.type == LINK) chevron(c, LEFT + COLUMN - 6, y + 40, ink, 0.6f * dim);
            }
        }
    }

    private static String[] wallpaperIds() {
        String[] ids = new String[Wallpapers.ALL.length];
        for (int i = 0; i < ids.length; i++) ids[i] = Wallpapers.ALL[i].id;
        return ids;
    }

    private void chevron(Canvas c, float x, float cy, int ink, float alpha) {
        ui.stroke.setColor(ink);
        ui.stroke.setAlpha(Math.round(255 * alpha));
        ui.stroke.setStrokeWidth(3);
        ui.stroke.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        c.drawLine(x - 9, cy - 12, x, cy, ui.stroke);
        c.drawLine(x, cy, x - 9, cy + 12, ui.stroke);
    }

    /** The committed Home, scaled down. In Wallpaper it shows the focused photo before it is applied. */
    private void preview(Canvas c, Page p, int ink) {
        Bitmap photo = stage.photo(), blur = stage.blur();
        String caption = "Home";
        if (p.wallpapers && p.focus >= 0) {
            Wallpapers.Wallpaper w = p.rows.get(p.focus).wallpaper;
            if (w.id.equals(candidateId) && candidate != null) {
                photo = candidate;
                blur = candidateBlur;
            } else if (!w.id.equals(stage.prefs.get(Prefs.WALLPAPER))) {
                stage.removeCallbacks(loadCandidate);
                stage.postDelayed(loadCandidate, 220);
            }
            caption = w.name + " · Photo by " + w.credit + " on Unsplash";
            if (!stage.prefs.is(Prefs.WALLPAPER, w.id)) caption += " · Not applied";
        }
        c.save();
        c.clipRect(PREVIEW);
        c.translate(PREVIEW.left, PREVIEW.top);
        c.scale(PREVIEW.width() / Ui.W, PREVIEW.width() / Ui.W);
        stage.drawBackground(c, photo);
        stage.homeScreen.preview(c, blur);
        c.restore();
        ui.stroke.setColor(ink);
        ui.stroke.setAlpha(40);
        ui.stroke.setStrokeWidth(2);
        c.drawRect(PREVIEW, ui.stroke);
        ui.text(ui.font("manrope", 500), 26, 0, ink, 0.6f);
        ui.line(c, caption, PREVIEW.left, PREVIEW.bottom + 48, PREVIEW.width(), -1);
    }
}
