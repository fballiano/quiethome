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

    private static final String[] LINES = {
        "QuietHome needs its service for these features:",
        "·  the Home button, on TVs that keep their own launcher",
        "·  button mapping, for example for a Netflix key",
        "·  opening an HDMI input when the TV turns on",
        "",
        "In Accessibility, select QuietHome and turn it on.",
        "If it is grey or says \"Restricted setting\", open App info, allow restricted settings, and try again.",
        "The service sees only key presses. It cannot read the screen and sends nothing.",
    };
    private static final String[] BUTTONS = {"Open Accessibility settings", "App info", "Later"};

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
        ui.line(c, "Turn on the QuietHome service", 140, ui.baseline(220), 0, -1);
        float y = 320;
        for (String line : LINES) {
            ui.text(regular, 30, -0.3f, ink, line.startsWith("·") ? 0.9f : 0.7f);
            ui.line(c, line, 140, ui.baseline(y), 1640, -1);
            y += 54;
        }
        float x = 140;
        for (int i = 0; i < BUTTONS.length; i++) {
            ui.text(regular, 32, -0.3f, ink, 1);
            float w = ui.width(BUTTONS[i]) + 64;
            r.set(x - 8, 760, x - 8 + w, 832);
            if (i == button) ui.plate(c, r, 36, bg, 1);
            ui.text(regular, 32, -0.3f, ink, i == button ? 1 : 0.62f);
            ui.line(c, BUTTONS[i], r.centerX(), ui.baseline(r.centerY()), 0, 0);
            x += w + 16;
        }
        return false;
    }
}
