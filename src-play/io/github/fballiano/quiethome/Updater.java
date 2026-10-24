// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

/**
 * The Play build has no Settings → Update QuietHome: Google Play updates the app, and Play does not
 * accept an app that installs its own updates. The GitHub build has the real one, in src-github.
 */
final class Updater {
    /** Settings shows Update QuietHome. */
    static final boolean AVAILABLE = false;

    private Updater() {
    }

    static void start(Stage stage) {
    }
}
