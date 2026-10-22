// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/** One layer of the interface: Home at the bottom, panels such as All apps and Settings above it. */
abstract class Screen {
    final Stage stage;
    final Ui ui;

    Screen(Stage stage) {
        this.stage = stage;
        this.ui = stage.ui;
    }

    /** A string from res/values, in the TV's language. */
    String text(int id, Object... args) {
        return stage.text(id, args);
    }

    /** Draws on the 1920 x 1080 design canvas. Returns true while something still animates. */
    abstract boolean draw(Canvas c, float dt);

    /** An arrow key: dx and dy are -1, 0 or 1. */
    void move(int dx, int dy) {
    }

    /** OK. For a holdable item it comes when OK is released before the hold time. */
    void ok() {
    }

    /** Whether OK on the focused item waits for release, because holding it does something else. */
    boolean holdable() {
        return false;
    }

    void okLong() {
    }

    void back() {
        stage.close(this);
    }

    /** Whether the screen covers everything below it, so the layers below need no drawing. */
    boolean opaque() {
        return true;
    }

    void opened() {
    }

    void closed() {
    }

    /** Apps were installed, removed or updated, or the configuration changed. */
    void refresh() {
    }

    /** Whether the screen has a text field, so the TV's keyboard can type into it. */
    boolean editsText() {
        return false;
    }

    /** The keyboard's connection to the text field, or null. */
    InputConnection input(EditorInfo info) {
        return null;
    }

    /** A key that Stage does not use, for example Delete. Returns true when the screen used it. */
    boolean key(KeyEvent e) {
        return false;
    }
}
