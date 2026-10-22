// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * The bundled photographs (Unsplash License) and the work done on them.
 * Each photo is decoded once at screen size. A tiny blurred copy gives the glass effect: it is
 * drawn scaled up behind plates and panels, so there is no live blur and no large extra bitmap.
 */
final class Wallpapers {
    static final class Wallpaper {
        final String id;
        /** The string id of its name. */
        final int name;
        final String credit;

        Wallpaper(String id, int name, String credit) {
            this.id = id;
            this.name = name;
            this.credit = credit;
        }

        String photo() {
            return "wallpapers/" + id + ".jpg";
        }

        String thumb() {
            return "wallpapers/" + id + "-thumb.jpg";
        }
    }

    static final Wallpaper[] ALL = {
        new Wallpaper("misty", R.string.wallpaper_misty, "Ganapathy Kumar"),
        new Wallpaper("cove", R.string.wallpaper_cove, "Joshua Wordel"),
        new Wallpaper("forest", R.string.wallpaper_forest, "Dejan Zakic"),
        new Wallpaper("valley", R.string.wallpaper_valley, "Aniket Deole"),
        new Wallpaper("night", R.string.wallpaper_night, "Mohammad Alizade"),
        new Wallpaper("peaks", R.string.wallpaper_peaks, "John Rodenn Castillo"),
        new Wallpaper("bridge", R.string.wallpaper_bridge, "Justin Wolff"),
        new Wallpaper("ridge", R.string.wallpaper_ridge, "Cristina Gottardi"),
        new Wallpaper("golden", R.string.wallpaper_golden, "Karthik Sreenivas"),
        new Wallpaper("summit", R.string.wallpaper_summit, "Rudra Gupta"),
        new Wallpaper("goldcoast", R.string.wallpaper_goldcoast, "City of Gold Coast"),
        new Wallpaper("sydney", R.string.wallpaper_sydney, "Caleb"),
        new Wallpaper("amber", R.string.wallpaper_amber, "Chris Barbalis"),
        new Wallpaper("mint", R.string.wallpaper_mint, "MagicPattern"),
        new Wallpaper("alpine", R.string.wallpaper_alpine, "Cristina Gottardi"),
    };

    /** Width of the blurred copy. 1920 / 16: enough for soft glass, and only 30 KB. */
    static final int BLUR_WIDTH = 120;

    private Wallpapers() {
    }

    static Wallpaper find(String id) {
        if (id == null) return null;
        for (Wallpaper w : ALL) if (w.id.equals(id)) return w;
        return null;
    }

    static Wallpaper next(String id) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].id.equals(id)) return ALL[(i + 1) % ALL.length];
        return ALL[0];
    }

    /**
     * Decodes the 16:9 centre of a photo for a width x height screen. Only that region is decoded.
     * Big screens get the photo's 1920 x 1080: the interface of a 4K TV is drawn at 1080p.
     */
    static Bitmap decode(AssetManager assets, Wallpaper w, int width, int height) {
        try (InputStream in = assets.open(w.photo())) {
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(in, false);
            Rect crop = crop(decoder.getWidth(), decoder.getHeight());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 1;
            while (crop.width() / (options.inSampleSize * 2) >= width && crop.height() / (options.inSampleSize * 2) >= height) {
                options.inSampleSize *= 2;
            }
            Bitmap bitmap = decoder.decodeRegion(crop, options);
            decoder.recycle();
            // A 720p interface needs less than half the pixels of the 1080p photo.
            if (bitmap != null && bitmap.getWidth() > width * 1.2f) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);
                bitmap.recycle();
                bitmap = scaled;
            }
            return bitmap;
        } catch (IOException | RuntimeException e) {
            Log.w(Home.TAG, "Could not decode " + w.id, e);
            return null;
        }
    }

    /** A small copy for thumbnails and previews, decoded with subsampling (no full-size step). */
    static Bitmap decodeSmall(AssetManager assets, String path, int minWidth) {
        try (InputStream in = assets.open(path)) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, bounds);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (bounds.outWidth / (options.inSampleSize * 2) >= minWidth) options.inSampleSize *= 2;
            try (InputStream again = assets.open(path)) {
                return BitmapFactory.decodeStream(again, null, options);
            }
        } catch (IOException | RuntimeException e) {
            Log.w(Home.TAG, "Could not decode " + path, e);
            return null;
        }
    }

    /** The largest centred 16:9 region of a width x height image. */
    static Rect crop(int width, int height) {
        if (width * 9 > height * 16) {
            int w = height * 16 / 9;
            return new Rect((width - w) / 2, 0, (width - w) / 2 + w, height);
        }
        int h = width * 9 / 16;
        return new Rect(0, (height - h) / 2, width, (height - h) / 2 + h);
    }

    /** A 120 x 68 blurred copy of the photo's 16:9 centre (three box blur passes approximate a Gaussian). */
    static Bitmap blur(Bitmap photo) {
        int w = BLUR_WIDTH, h = BLUR_WIDTH * 9 / 16;
        Rect crop = crop(photo.getWidth(), photo.getHeight());
        Bitmap small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        new Canvas(small).drawBitmap(photo, crop, new Rect(0, 0, w, h), paint);
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        int[] tmp = new int[w * h];
        for (int pass = 0; pass < 3; pass++) {
            boxBlur(px, tmp, w, h, 3, true);
            boxBlur(tmp, px, w, h, 3, false);
        }
        small.setPixels(px, 0, w, 0, 0, w, h);
        return small;
    }

    private static void boxBlur(int[] src, int[] dst, int w, int h, int r, boolean horizontal) {
        int lines = horizontal ? h : w, length = horizontal ? w : h;
        for (int line = 0; line < lines; line++) {
            for (int i = 0; i < length; i++) {
                int rs = 0, gs = 0, bs = 0, n = 0;
                for (int k = -r; k <= r; k++) {
                    int j = Math.max(0, Math.min(length - 1, i + k));
                    int p = horizontal ? src[line * w + j] : src[j * w + line];
                    rs += (p >> 16) & 0xff;
                    gs += (p >> 8) & 0xff;
                    bs += p & 0xff;
                    n++;
                }
                int out = 0xff000000 | ((rs / n) << 16) | ((gs / n) << 8) | (bs / n);
                if (horizontal) dst[line * w + i] = out;
                else dst[i * w + line] = out;
            }
        }
    }
}
