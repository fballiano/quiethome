// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Typeface;

/** Shown over Home while the QuietHome service is off: why it is needed, and a way to turn it on. */
final class SetupScreen extends Screen {
    /** "Later" hides this screen until QuietHome starts again. */
    static boolean later;

    /** The text, line by line; 0 is an empty line. */
    private static final int[] LINES = {
        R.string.service_needed, R.string.service_home, R.string.service_buttons, R.string.service_start, 0,
        R.string.service_how, R.string.service_restricted, R.string.service_privacy,
    };
    /** These lines are the features, shown as a list. */
    private static final int FEATURES_FROM = 1, FEATURES_TO = 3;
    private static final int[] BUTTONS = {R.string.open_accessibility, R.string.app_info, R.string.later};

    private final RectF r = new RectF();
    private int button;

    SetupScreen(Stage stage) {
        super(stage);
    }

    @Override
    void move(int dx, int dy) {
        if (dx != 0) button = Math.max(0, Math.min(BUTTONS.length - 1, button + dx));
    }

    @Override
    void ok() {
        if (button == 0) stage.toast(ServiceSetup.openSettings(stage.getContext()));
        else if (button == 1) stage.toast(ServiceSetup.openAppInfo(stage.getContext()));
        else back();
    }

    @Override
    void back() {
        later = true;
        stage.close(this);
    }

    @Override
    boolean draw(Canvas c, float dt) {
        String bg = stage.prefs.get(Prefs.BG);
        int ink = Ui.ink(bg);
        ui.sheet(c, stage.blur(), bg);
        Typeface regular = ui.font("manrope", 500);
        ui.text(regular, 56, -1.6f, ink, 1);
        ui.line(c, text(R.string.service_title), 140, ui.baseline(220), 0, -1);
        float y = 320;
        for (int i = 0; i < LINES.length; i++) {
            if (LINES[i] != 0) {
                boolean feature = i >= FEATURES_FROM && i <= FEATURES_TO;
                ui.text(regular, 30, -0.3f, ink, feature ? 0.9f : 0.7f);
                ui.line(c, (feature ? "·  " : "") + text(LINES[i]), 140, ui.baseline(y), 1640, -1);
            }
            y += 54;
        }
        float x = 140;
        for (int i = 0; i < BUTTONS.length; i++) {
            String label = text(BUTTONS[i]);
            ui.text(regular, 32, -0.3f, ink, 1);
            float w = ui.width(label) + 64;
            r.set(x - 8, 760, x - 8 + w, 832);
            if (i == button) ui.plate(c, r, 36, bg, 1);
            ui.text(regular, 32, -0.3f, ink, i == button ? 1 : 0.62f);
            ui.line(c, label, r.centerX(), ui.baseline(r.centerY()), 0, 0);
            x += w + 16;
        }
        return false;
    }
}
