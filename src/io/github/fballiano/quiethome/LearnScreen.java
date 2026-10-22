package io.github.fballiano.quiethome;

import android.graphics.Canvas;
import android.graphics.Typeface;

/** "Press a remote button": the next assignable key goes to QuietHome instead of its usual job. */
final class LearnScreen extends Screen {
    interface Learned {
        void learned(int code);
    }

    private static final long TIMEOUT_MS = 30000;

    private final Learned learned;
    private final Runnable timeout = () -> {
        stage.close(this);
        stage.toast("No button was pressed.");
    };

    LearnScreen(Stage stage, Learned learned) {
        super(stage);
        this.learned = learned;
    }

    @Override
    void opened() {
        RemoteButtons.startCapture(TIMEOUT_MS, code -> stage.post(() -> {
            if (!stage.isOpen(this)) return;
            stage.close(this);
            learned.learned(code);
        }));
        stage.postDelayed(timeout, TIMEOUT_MS);
    }

    @Override
    void closed() {
        RemoteButtons.stopCapture();
        stage.removeCallbacks(timeout);
    }

    @Override
    boolean draw(Canvas c, float dt) {
        String bg = stage.prefs.get(Prefs.BG);
        int ink = Ui.ink(bg);
        ui.sheet(c, stage.blur(), bg);
        Typeface regular = ui.font("manrope", 500);
        ui.text(regular, 64, -1.8f, ink, 1);
        ui.line(c, "Press a remote button", Ui.W / 2, ui.baseline(480), 0, 0);
        ui.text(regular, 30, -0.2f, ink, 0.66f);
        ui.line(c, "For example a button that opens the TV maker's own app.", Ui.W / 2, ui.baseline(570), 0, 0);
        ui.line(c, "Arrows, OK, Back, Home, volume and power keep their jobs. Back cancels.", Ui.W / 2, ui.baseline(620), 0, 0);
        return false;
    }
}
