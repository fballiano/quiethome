// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/**
 * A new name for an HDMI input, typed with the TV's keyboard. Stage draws the text; the keyboard
 * edits it through an input connection. Done on the keyboard saves, Back cancels, and an empty
 * name gives the input the TV's name again.
 */
final class RenameScreen extends Screen {
    private static final int MAX_LENGTH = 40;
    private static final float LEFT = 140;
    private static final RectF FIELD = new RectF(LEFT - 24, 236, LEFT + 1100, 340);
    private static final int[] BUTTONS = {R.string.save, R.string.use_tv_name};

    private final Catalog.Entry input;
    private final SpannableStringBuilder name;
    private final InputMethodManager keyboard;
    private final RectF r = new RectF();
    /** 0 is the name field; 1 and up are the buttons. */
    private int focus;

    RenameScreen(Stage stage, Catalog.Entry input) {
        super(stage);
        this.input = input;
        name = new SpannableStringBuilder(input.name);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_LENGTH)});
        Selection.setSelection(name, name.length());
        keyboard = (InputMethodManager) stage.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    void opened() {
        // The connection is made when the keyboard asks for it, after this screen is on top.
        stage.post(this::showKeyboard);
    }

    @Override
    void closed() {
        keyboard.hideSoftInputFromWindow(stage.getWindowToken(), 0);
        keyboard.restartInput(stage);
    }

    private void showKeyboard() {
        if (!stage.isOpen(this)) return;
        keyboard.restartInput(stage);
        keyboard.showSoftInput(stage, 0);
    }

    @Override
    boolean editsText() {
        return true;
    }

    @Override
    InputConnection input(EditorInfo info) {
        info.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        info.imeOptions = EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
        info.initialSelStart = Selection.getSelectionStart(name);
        info.initialSelEnd = Selection.getSelectionEnd(name);
        return new BaseInputConnection(stage, true) {
            private int batch;

            @Override
            public Editable getEditable() {
                return name;
            }

            @Override
            public boolean beginBatchEdit() {
                batch++;
                return true;
            }

            @Override
            public boolean endBatchEdit() {
                if (batch > 0 && --batch == 0) changed();
                return batch > 0;
            }

            @Override
            public boolean setSelection(int start, int end) {
                boolean done = super.setSelection(start, end);
                if (batch == 0) changed();
                return done;
            }

            @Override
            public boolean performEditorAction(int action) {
                stage.post(() -> save(name.toString()));
                return true;
            }
        };
    }

    /** The text or the cursor changed: draw it, and tell the keyboard where the cursor is now. */
    private void changed() {
        stage.invalidate();
        keyboard.updateSelection(stage, Selection.getSelectionStart(name), Selection.getSelectionEnd(name),
            BaseInputConnection.getComposingSpanStart(name), BaseInputConnection.getComposingSpanEnd(name));
    }

    /** Delete and letters from a keyboard that sends keys, not text. */
    @Override
    boolean key(KeyEvent e) {
        if (e.getAction() != KeyEvent.ACTION_DOWN) return e.getKeyCode() == KeyEvent.KEYCODE_DEL || e.getUnicodeChar() != 0;
        int start = Math.max(0, Selection.getSelectionStart(name)), end = Math.max(0, Selection.getSelectionEnd(name));
        if (e.getKeyCode() == KeyEvent.KEYCODE_DEL) {
            if (start != end) name.delete(Math.min(start, end), Math.max(start, end));
            else if (start > 0) name.delete(start - 1, start);
        } else if (e.getUnicodeChar() != 0) {
            name.replace(Math.min(start, end), Math.max(start, end), String.valueOf((char) e.getUnicodeChar()));
        } else {
            return false;
        }
        changed();
        return true;
    }

    @Override
    void move(int dx, int dy) {
        if (dy > 0 && focus == 0) focus = 1;
        else if (dy < 0) focus = 0;
        else if (dx != 0 && focus > 0) focus = Math.max(1, Math.min(BUTTONS.length, focus + dx));
    }

    @Override
    void ok() {
        if (focus == 0) showKeyboard();
        else save(BUTTONS[focus - 1] == R.string.save ? name.toString() : "");
    }

    private void save(String value) {
        if (!stage.isOpen(this)) return;
        stage.prefs.setInputName(input.id, value);
        stage.close(this);
        stage.refresh();
        Catalog.Entry now = stage.catalog.get(input.id);
        stage.toast(value.trim().isEmpty() ? text(R.string.input_tv_name_again)
            : text(R.string.renamed, now == null ? value.trim() : Catalog.title(now)));
    }

    @Override
    boolean draw(Canvas c, float dt) {
        String bg = stage.prefs.get(Prefs.BG);
        int ink = Ui.ink(bg);
        ui.sheet(c, stage.blur(), bg);
        Typeface regular = ui.font("manrope", 500);
        ui.text(regular, 56, -1.6f, ink, 1);
        ui.line(c, text(R.string.rename_input, Catalog.title(input)), LEFT, ui.baseline(104), 1640, -1);
        ui.text(regular, 26, 0, ink, 0.5f);
        ui.line(c, text(R.string.rename_note), LEFT, ui.baseline(158), 1640, -1);

        ui.plate(c, FIELD, 32, bg, focus == 0 ? 1 : 0.5f);
        field(c, regular, ink);

        float x = LEFT;
        for (int i = 0; i < BUTTONS.length; i++) {
            String label = text(BUTTONS[i]);
            ui.text(regular, 32, -0.3f, ink, 1);
            float w = ui.width(label) + 64;
            r.set(x - 24, 380, x - 24 + w, 452);
            if (focus == i + 1) ui.plate(c, r, 36, bg, 1);
            ui.text(regular, 32, -0.3f, ink, focus == i + 1 ? 1 : 0.62f);
            ui.line(c, label, r.centerX(), ui.baseline(r.centerY()), 0, 0);
            x += w + 16;
        }
        return false;
    }

    /** The name with a steady cursor; a long name slides left so the cursor stays in view. */
    private void field(Canvas c, Typeface face, int ink) {
        float left = FIELD.left + 28, right = FIELD.right - 28, cy = FIELD.centerY();
        ui.text(face, 44, -0.8f, ink, 1);
        int cursor = Math.max(0, Math.min(name.length(), Selection.getSelectionEnd(name)));
        float cursorX = ui.width(name.subSequence(0, cursor));
        float shift = Math.max(0, cursorX - (right - left));
        c.save();
        c.clipRect(left - 4, FIELD.top, right + 4, FIELD.bottom);
        ui.line(c, name, left - shift, ui.baseline(cy), 0, -1);
        int from = BaseInputConnection.getComposingSpanStart(name), to = BaseInputConnection.getComposingSpanEnd(name);
        if (from >= 0 && to > from) {
            float u0 = left - shift + ui.width(name.subSequence(0, from)), u1 = left - shift + ui.width(name.subSequence(0, to));
            r.set(u0, cy + 30, u1, cy + 33);
            ui.round(c, r, 1.5f, ink);
        }
        if (focus == 0) {
            r.set(left - shift + cursorX, cy - 30, left - shift + cursorX + 3, cy + 30);
            ui.round(c, r, 1.5f, ink);
        }
        c.restore();
    }
}
