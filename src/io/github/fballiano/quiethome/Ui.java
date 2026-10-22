// SPDX-FileCopyrightText: 2026 Fabrizio Balliano
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.fballiano.quiethome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared drawing: fonts, colours, text, marks and glass. Everything is drawn on a 1920 x 1080
 * design canvas that the Stage scales to the screen.
 */
final class Ui {
    static final float W = 1920, H = 1080;
    static final int LIGHT_BG = 0xFFE7E5DF, DARK_BG = 0xFF15191C;
    static final int PHOTO_INK = 0xFFF6F1E7, LIGHT_INK = 0xFF1C1C1A, DARK_INK = 0xFFF4F4F1;
    static final int WARN = 0xFFFFD9A8;

    private static final Map<String, String> FONT_FILES = new HashMap<>();

    static {
        FONT_FILES.put("manrope", "fonts/Manrope-Variable.ttf");
        FONT_FILES.put("geist", "fonts/Geist-Variable.ttf");
        FONT_FILES.put("newsreader", "fonts/Newsreader-Variable.ttf");
    }

    final Context context;
    final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint image = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint mask = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    final Paint glass = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<String, Typeface> fonts = new HashMap<>();
    private final SparseArray<Drawable> marks = new SparseArray<>();
    private final RectF tmp = new RectF();
    private final Rect tmpRect = new Rect();
    private final Matrix matrix = new Matrix();
    private Bitmap glassSource;
    private BitmapShader glassShader;

    Ui(Context context) {
        this.context = context;
        stroke.setStyle(Paint.Style.STROKE);
    }

    /** A weight of a variable font from assets/fonts; the family stays mapped from the APK, not copied. */
    Typeface font(String family, int weight) {
        String key = family + weight;
        Typeface face = fonts.get(key);
        if (face != null) return face;
        String file = FONT_FILES.get(family);
        try {
            String axes = "'wght' " + weight + ("newsreader".equals(family) ? ", 'opsz' 48" : "");
            face = new Typeface.Builder(context.getAssets(), file).setFontVariationSettings(axes).build();
        } catch (RuntimeException e) {
            Log.w(Home.TAG, "Font " + family + " is not available", e);
        }
        if (face == null) face = Typeface.create("sans-serif", weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
        fonts.put(key, face);
        return face;
    }

    /** Sets the text paint. Letter spacing is in pixels, like the CSS it comes from. */
    TextPaint text(Typeface face, float size, float spacing, int color, float alpha) {
        text.setTypeface(face);
        text.setTextSize(size);
        text.setLetterSpacing(spacing / size);
        text.setColor(color);
        text.setAlpha(Math.round(255 * clamp(alpha)));
        text.clearShadowLayer();
        return text;
    }

    void shadow(boolean on) {
        if (on) text.setShadowLayer(8, 0, 2, 0x660E0A06);
        else text.clearShadowLayer();
    }

    /** Draws one line, cut with an ellipsis to maxWidth. align: -1 left, 0 centre, 1 right. Returns its width. */
    float line(Canvas c, CharSequence s, float x, float baseline, float maxWidth, int align) {
        CharSequence fit = maxWidth > 0 ? TextUtils.ellipsize(s, text, maxWidth, TextUtils.TruncateAt.END) : s;
        float w = text.measureText(fit, 0, fit.length());
        float left = align < 0 ? x : align == 0 ? x - w / 2 : x - w;
        c.drawText(fit, 0, fit.length(), left, baseline, text);
        return w;
    }

    float width(CharSequence s) {
        return text.measureText(s, 0, s.length());
    }

    /** Baseline that centres the current font's capitals and lower case on centerY. */
    float baseline(float centerY) {
        Paint.FontMetrics m = text.getFontMetrics();
        return centerY - (m.ascent + m.descent) / 2;
    }

    /** A bundled vector mark, tinted, fitted into the box without changing its aspect ratio. */
    void mark(Canvas c, int res, float left, float top, float width, float height, int color, float alpha) {
        Drawable d = marks.get(res);
        if (d == null) {
            d = context.getDrawable(res).mutate();
            marks.put(res, d);
        }
        float iw = d.getIntrinsicWidth(), ih = d.getIntrinsicHeight();
        float scale = Math.min(width / iw, height / ih);
        float w = iw * scale, h = ih * scale;
        int l = Math.round(left + (width - w) / 2), t = Math.round(top + (height - h) / 2);
        d.setBounds(l, t, l + Math.round(w), t + Math.round(h));
        d.setTint(color);
        d.setAlpha(Math.round(255 * clamp(alpha)));
        d.draw(c);
    }

    /** Draws an alpha mask (see {@link #silhouette}) in one colour, fitted into the box like a mark. */
    void silhouette(Canvas c, Bitmap b, float left, float top, float width, float height, int color, float alpha) {
        float scale = Math.min(width / b.getWidth(), height / b.getHeight());
        float w = b.getWidth() * scale, h = b.getHeight() * scale;
        tmp.set(left + (width - w) / 2, top + (height - h) / 2, left + (width + w) / 2, top + (height + h) / 2);
        mask.setColor(color);
        mask.setAlpha(Math.round(255 * clamp(alpha)));
        c.drawBitmap(b, null, tmp, mask);
    }

    /**
     * An app icon as a one-colour mark, like the bundled brand marks: the parts of the icon that
     * differ from its background, as an ALPHA_8 mask cropped to them. Null when the result would
     * not read as a mark: almost nothing stands out, or one solid block fills it.
     */
    static Bitmap silhouette(Bitmap icon) {
        int w = icon.getWidth(), h = icon.getHeight();
        int[] px = new int[w * h];
        icon.getPixels(px, 0, w, 0, 0, w, h);

        // The background is read from a band along the edge. Mostly transparent: no background,
        // so the icon's shape is the mark. Flat: the mark is what differs from its colour.
        // A gradient: the mark is what is lighter (or darker) than all of it.
        int band = Math.max(1, w / 8);
        int[] lums = new int[px.length];
        int[] count = new int[4096];
        int edge = 0, solid = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (x >= band && x < w - band && y >= band && y < h - band) continue;
                edge++;
                int p = px[y * w + x];
                if (p >>> 24 < 128) continue;
                lums[solid++] = lum(p);
                count[bin(p)]++;
            }
        }
        int mode = SHAPE, lo = 0, hi = 0, bg = 0;
        if (solid * 4 >= edge) {
            Arrays.sort(lums, 0, solid);
            lo = lums[solid / 20];
            hi = lums[solid * 19 / 20];
            if (hi - lo <= 32) {
                mode = COLOUR;
                int top = 0;
                for (int i = 1; i < count.length; i++) if (count[i] > count[top]) top = i;
                bg = meanOfBin(px, w, h, band, top);
            } else {
                int light = 0, dark = 0;
                for (int p : px) {
                    if (p >>> 24 < 128) continue;
                    int l = lum(p);
                    if (l > hi + 32) light++;
                    else if (l < lo - 32) dark++;
                }
                mode = light >= dark ? LIGHTER : DARKER;
            }
        }

        // How far each pixel is from the background, on the scale of the mode. A shape uses
        // brightness: it keeps holes where it is much lighter than itself, such as a play button.
        int[] dist = lums;
        int t0 = mode == COLOUR ? 32 : 24;
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            if (mode == COLOUR) {
                dist[i] = Math.max(Math.abs((p >> 16 & 0xFF) - (bg >> 16 & 0xFF)),
                    Math.max(Math.abs((p >> 8 & 0xFF) - (bg >> 8 & 0xFF)), Math.abs((p & 0xFF) - (bg & 0xFF))));
            } else if (mode == LIGHTER) {
                dist[i] = lum(p) - hi;
            } else if (mode == DARKER) {
                dist[i] = lo - lum(p);
            } else {
                dist[i] = lum(p);
            }
        }

        // The median distance of the clear logo pixels (for a shape, its median brightness).
        // Coverage is full at 3/5 of the way to it, so a pixel half on an edge stays partly
        // covered and the edges stay smooth.
        int[] histogram = new int[256];
        int counted = 0;
        for (int i = 0; i < px.length; i++) {
            if (px[i] >>> 24 < 128) continue;
            int d = mode == SHAPE ? dist[i] : dist[i] - t0;
            if (d <= 0 && mode != SHAPE) continue;
            histogram[Math.min(255, d)]++;
            counted++;
        }
        int median = 0, seen = histogram[0];
        while (median < 255 && seen * 2 < counted) seen += histogram[++median];
        int span = Math.max(16, median * 3 / 5);
        int cut = Math.max(median + 64, 180);

        int left = w, top = h, right = -1, bottom = -1, strong = 0;
        long sum = 0;
        for (int i = 0; i < px.length; i++) {
            int a = px[i] >>> 24;
            if (mode == SHAPE) a = a * (255 - ramp((dist[i] - cut) * 10)) / 255;
            else a = a * ramp((dist[i] - t0) * 255 / span) / 255;
            px[i] = a << 24;
            if (a > 64) {
                strong++;
                int x = i % w, y = i / w;
                left = Math.min(left, x);
                right = Math.max(right, x);
                top = Math.min(top, y);
                bottom = Math.max(bottom, y);
            }
        }
        if (strong < px.length / 200) return null;
        int cw = right - left + 1, ch = bottom - top + 1;
        for (int y = top; y <= bottom; y++) for (int x = left; x <= right; x++) sum += px[y * w + x] >>> 24;
        if (sum > cw * ch * 255L * 4 / 5) return null;
        Bitmap out = Bitmap.createBitmap(cw, ch, Bitmap.Config.ALPHA_8);
        out.setPixels(px, top * w + left, w, 0, 0, cw, ch);
        return out;
    }

    private static final int SHAPE = 0, COLOUR = 1, LIGHTER = 2, DARKER = 3;

    private static int lum(int p) {
        return ((p >> 16 & 0xFF) * 299 + (p >> 8 & 0xFF) * 587 + (p & 0xFF) * 114) / 1000;
    }

    /** The colour in 4 bits per channel. */
    private static int bin(int p) {
        return (p >> 12 & 0xF00) | (p >> 8 & 0xF0) | (p >> 4 & 0xF);
    }

    private static int ramp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** The mean colour of the opaque edge pixels in one bin. */
    private static int meanOfBin(int[] px, int w, int h, int band, int bin) {
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (x >= band && x < w - band && y >= band && y < h - band) continue;
                int p = px[y * w + x];
                if (p >>> 24 < 128 || bin(p) != bin) continue;
                r += p >> 16 & 0xFF;
                g += p >> 8 & 0xFF;
                b += p & 0xFF;
                n++;
            }
        }
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /** Draws a 16:9 photo so it covers the rectangle. */
    void photo(Canvas c, Bitmap b, RectF dst) {
        if (b == null) return;
        Rect src = Wallpapers.crop(b.getWidth(), b.getHeight());
        image.setAlpha(255);
        c.drawBitmap(b, src, dst, image);
    }

    /** Initials in an outlined rounded square, for apps and inputs without a mark or an icon. */
    void monogram(Canvas c, String name, float cx, float cy, float size, int color, float alpha) {
        String letters = initials(name);
        tmp.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
        stroke.setColor(color);
        stroke.setAlpha(Math.round(255 * 0.38f * clamp(alpha)));
        stroke.setStrokeWidth(Math.max(2, size / 36));
        c.drawRoundRect(tmp, size * 0.24f, size * 0.24f, stroke);
        text(font("manrope", 600), size * 0.4f, 0, color, alpha);
        line(c, letters, cx, baseline(cy), 0, 0);
    }

    static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty() && Character.isLetterOrDigit(w.charAt(0))) out.appendCodePoint(w.codePointAt(0));
            if (out.length() >= 2) break;
        }
        return out.length() == 0 ? "?" : out.toString().toUpperCase();
    }

    void round(Canvas c, RectF r, float radius, int color) {
        fill.setShader(null);
        fill.setColor(color);
        c.drawRoundRect(r, radius, radius, fill);
    }

    /**
     * A focus plate or panel. On a photo it shows the blurred photo behind it, then a veil:
     * soft (light), frost (stronger light) or clear (dark tint). On flat backgrounds it is a quiet tint.
     * The blurred bitmap is mapped to the 1920 x 1080 design canvas, so plates line up with the photo.
     */
    void glass(Canvas c, RectF r, float radius, Bitmap blur, String bg, String material, float alpha, boolean edge) {
        int a = Math.round(255 * clamp(alpha));
        if (a == 0) return;
        if (!"photo".equals(bg) || blur == null) {
            fill.setShader(null);
            fill.setColor("light".equals(bg) ? 0x14000000 : 0x24FFFFFF);
            fill.setAlpha(Math.round(fill.getAlpha() * clamp(alpha)));
            c.drawRoundRect(r, radius, radius, fill);
            return;
        }
        if (glassSource != blur) {
            glassSource = blur;
            glassShader = new BitmapShader(blur, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        }
        matrix.setScale(W / blur.getWidth(), H / blur.getHeight());
        glassShader.setLocalMatrix(matrix);
        glass.setShader(glassShader);
        glass.setAlpha(a);
        c.drawRoundRect(r, radius, radius, glass);
        // Over a bright part of the photo, shade the glass first, so light text on it stays readable.
        float shade = clamp((luma(blur, r) - 0.5f) / 0.4f);
        fill.setShader(null);
        if (shade > 0) {
            fill.setColor(0xFF0E0C0A);
            fill.setAlpha(Math.round(255 * (0.1f + 0.28f * shade) * clamp(alpha)));
            c.drawRoundRect(r, radius, radius, fill);
        }
        float veil = 1 - 0.6f * shade;
        switch (material) {
            case "frost":
                fill.setColor(0x66FFFFFF);
                break;
            case "clear":
                fill.setColor(0x6B0E0C0A);
                break;
            default:
                fill.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0x4DFFFFFF, 0x29FFFFFF, Shader.TileMode.CLAMP));
                fill.setColor(0xFFFFFFFF);
        }
        fill.setAlpha(Math.round(fill.getAlpha() * clamp(alpha) * ("clear".equals(material) ? 1 : veil)));
        c.drawRoundRect(r, radius, radius, fill);
        fill.setShader(null);
        if (edge) {
            stroke.setStrokeWidth(1.5f);
            stroke.setShader(new LinearGradient(0, r.top, 0, r.top + radius, 0x57FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            stroke.setAlpha(a);
            tmp.set(r.left + 0.75f, r.top + 0.75f, r.right - 0.75f, r.bottom - 0.75f);
            c.drawRoundRect(tmp, radius, radius, stroke);
            stroke.setShader(null);
        }
    }

    /** Average brightness (0 to 1) of the blurred photo under a rectangle of the design canvas. */
    static float luma(Bitmap blur, RectF r) {
        float sum = 0;
        int n = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 3; j++) {
                float x = r.left + r.width() * (i + 0.5f) / 5, y = r.top + r.height() * (j + 0.5f) / 3;
                int px = (int) clamp(x / W * blur.getWidth(), 0, blur.getWidth() - 1);
                int py = (int) clamp(y / H * blur.getHeight(), 0, blur.getHeight() - 1);
                int p = blur.getPixel(px, py);
                sum += (0.2126f * ((p >> 16) & 0xff) + 0.7152f * ((p >> 8) & 0xff) + 0.0722f * (p & 0xff)) / 255f;
                n++;
            }
        }
        return sum / n;
    }

    /** A focus plate on a panel (All apps, Settings, the editor): a light tint with a top edge. */
    void plate(Canvas c, RectF r, float radius, String bg, float alpha) {
        fill.setShader(null);
        fill.setColor("light".equals(bg) ? 0x17000000 : 0x2EFFFFFF);
        fill.setAlpha(Math.round(fill.getAlpha() * clamp(alpha)));
        c.drawRoundRect(r, radius, radius, fill);
        if ("light".equals(bg)) return;
        stroke.setStrokeWidth(1.5f);
        stroke.setShader(new LinearGradient(0, r.top, 0, r.top + radius, 0x47FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        stroke.setAlpha(Math.round(255 * clamp(alpha)));
        tmp.set(r.left + 0.75f, r.top + 0.75f, r.right - 0.75f, r.bottom - 0.75f);
        c.drawRoundRect(tmp, radius, radius, stroke);
        stroke.setShader(null);
    }

    /** The backdrop of full-screen panels: the blurred photo, darkened, or the flat colour. */
    void sheet(Canvas c, Bitmap blur, String bg) {
        if ("photo".equals(bg) && blur != null) {
            tmp.set(0, 0, W, H);
            image.setAlpha(255);
            c.drawBitmap(blur, null, tmp, image);
            c.drawColor(0x8C0C0E10);
        } else {
            c.drawColor("light".equals(bg) ? LIGHT_BG : DARK_BG);
        }
    }

    /** A check mark that fits a size x size box. */
    void check(Canvas c, float left, float top, float size, int color, float alpha) {
        stroke.setColor(color);
        stroke.setAlpha(Math.round(255 * clamp(alpha)));
        stroke.setStrokeWidth(size / 9);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        float[] p = {left + size * 0.18f, top + size * 0.52f, left + size * 0.42f, top + size * 0.76f, left + size * 0.84f, top + size * 0.28f};
        c.drawLine(p[0], p[1], p[2], p[3], stroke);
        c.drawLine(p[2], p[3], p[4], p[5], stroke);
    }

    static int ink(String bg) {
        return "light".equals(bg) ? LIGHT_INK : "dark".equals(bg) ? DARK_INK : PHOTO_INK;
    }

    static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    static float clamp(float v, float min, float max) {
        return v < min ? min : v > max ? max : v;
    }

    /**
     * A value that eases towards its target. step() returns true while it still moves, so the
     * Stage keeps drawing frames only during an animation.
     */
    static final class Anim {
        float value, target;
        private final float tau;

        Anim(float tauMs) {
            this.tau = tauMs;
        }

        void to(float t) {
            target = t;
        }

        void snap(float t) {
            target = t;
            value = t;
        }

        boolean step(float dtMs) {
            if (value == target) return false;
            value += (target - value) * (1 - (float) Math.exp(-dtMs / tau));
            if (Math.abs(target - value) < 0.002f * Math.max(1, Math.abs(target))) value = target;
            return value != target;
        }
    }
}
