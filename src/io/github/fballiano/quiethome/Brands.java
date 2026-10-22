// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import java.util.regex.Pattern;

/** Known services, most with a bundled monochrome mark. Their order is the default Home order. */
final class Brands {
    static final class Brand {
        final String id;
        final String name;
        /** 0 when the owner does not allow its mark here: Home then draws the app's own icon. */
        final int mark;
        /** Android packages that open the service, best first. */
        final String[] packages;
        /** For a device on an HDMI input: matches the input's name. */
        final Pattern input;

        Brand(String id, String name, int mark, Pattern input, String... packages) {
            this.id = id;
            this.name = name;
            this.mark = mark;
            this.input = input;
            this.packages = packages;
        }
    }

    static final Brand[] ALL = {
        new Brand("primevideo", "Prime Video", R.drawable.icon_primevideo, null,
            "com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"),
        new Brand("youtube", "YouTube", R.drawable.icon_youtube, null, "com.google.android.youtube.tv"),
        new Brand("youtubekids", "YouTube Kids", R.drawable.icon_youtubekids, null,
            "com.google.android.youtube.tvkids", "com.google.android.apps.youtube.kids"),
        new Brand("smarttube", "SmartTube", R.drawable.icon_smarttube, null, "org.smarttube.stable", "org.smarttube.beta",
            "app.smarttube.fdroid", "com.teamsmart.videomanager.tv", "com.liskovsoft.smarttubetv.beta"),
        new Brand("netflix", "Netflix", R.drawable.icon_netflix, null, "com.netflix.ninja"),
        new Brand("disneyplus", "Disney+", 0, null, "com.disney.disneyplus"),
        new Brand("jellyfin", "Jellyfin", R.drawable.icon_jellyfin, null,
            "org.jellyfin.androidtv", "com.github.damontecres.wholphin"),
        new Brand("plex", "Plex", R.drawable.icon_plex, null, "com.plexapp.android"),
        new Brand("emby", "Emby", R.drawable.icon_emby, null, "tv.emby.embyatv", "com.mb.android"),
        new Brand("kodi", "Kodi", R.drawable.icon_kodi, null, "org.xbmc.kodi"),
        new Brand("twitch", "Twitch", R.drawable.icon_twitch, null, "com.s0und.s0undtv", "tv.twitch.android.app"),
        new Brand("appletv", "Apple TV", R.drawable.icon_appletv, null, "com.apple.atve.androidtv.appletv"),
        new Brand("paramountplus", "Paramount+", R.drawable.icon_paramountplus, null, "com.cbs.ott", "com.cbs.app"),
        new Brand("hulu", "Hulu", 0, null, "com.hulu.livingroomplus", "com.hulu.plus"),
        new Brand("crunchyroll", "Crunchyroll", R.drawable.icon_crunchyroll, null, "com.crunchyroll.crunchyroid"),
        new Brand("dazn", "DAZN", R.drawable.icon_dazn, null, "com.dazn"),
        new Brand("mubi", "MUBI", R.drawable.icon_mubi, null, "com.mubi"),
        new Brand("stremio", "Stremio", R.drawable.icon_stremio, null, "com.stremio.one"),
        new Brand("nuvio", "Nuvio", R.drawable.icon_nuvio, null, "com.nuvio.tv", "com.nuvio.app"),
        new Brand("spotify", "Spotify", R.drawable.icon_spotify, null, "com.spotify.tv.android"),
        new Brand("youtubemusic", "YouTube Music", R.drawable.icon_youtubemusic, null,
            "com.google.android.youtube.tvmusic", "com.google.android.apps.youtube.music"),
        new Brand("tidal", "Tidal", R.drawable.icon_tidal, null, "com.aspiro.tidal"),
        new Brand("deezer", "Deezer", R.drawable.icon_deezer, null, "deezer.android.app"),
        new Brand("hbomax", "HBO Max", R.drawable.icon_hbomax, null, "com.wbd.stream", "com.hbo.hbonow"),
        new Brand("tubi", "Tubi", R.drawable.icon_tubi, null, "com.tubitv"),
        new Brand("vlc", "VLC", R.drawable.icon_vlc, null, "org.videolan.vlc"),
        new Brand("steamlink", "Steam Link", R.drawable.icon_steamlink, null, "com.valvesoftware.steamlink"),
        new Brand("console", "Game console", R.drawable.icon_console,
            Pattern.compile("switch|xbox|playstation|\\bps[345]\\b|console", Pattern.CASE_INSENSITIVE)),
    };

    private Brands() {
    }

    static Brand forPackage(String packageName) {
        for (Brand brand : ALL) for (String p : brand.packages) if (p.equals(packageName)) return brand;
        return null;
    }

    static Brand forInput(String name) {
        for (Brand brand : ALL) if (brand.input != null && brand.input.matcher(name).find()) return brand;
        return null;
    }
}
