package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

public class UspdmpshmOverlay extends View {

    private static final long PHASE_1_MS = 650;
    private static final long PHASE_2_MS = 950;
    private static final long PHASE_3_MS = 650;
    private static final long PHASE_4_MS = 5500;

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pixelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmpRect = new RectF();

    private long startedAt;
    private boolean started;

    public UspdmpshmOverlay(Context context) {
        super(context);

        setVisibility(GONE);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        dimPaint.setColor(0xD0000000);

        orbPaint.setStyle(Paint.Style.FILL);
        orbPaint.setShader(new RadialGradient(0, 0, dp(70),
                new int[]{0xFFFFFFFF, 0x66FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.35f, 1f},
                Shader.TileMode.CLAMP));

        pixelPaint.setStyle(Paint.Style.FILL);
        pixelPaint.setAlpha(160);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(2));
        ringPaint.setAlpha(220);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(16));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        startedAt = SystemClock.uptimeMillis();
        setVisibility(VISIBLE);
        invalidate();
    }

    public void stop() {
        started = false;
        startedAt = 0;
        setVisibility(GONE);
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!started) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            postInvalidateOnAnimation();
            return;
        }

        long now = SystemClock.uptimeMillis();
        long t = now - startedAt;
        long total = PHASE_1_MS + PHASE_2_MS + PHASE_3_MS + PHASE_4_MS;
        if (t >= total) {
            // Keep last phase dim a bit then stop.
            stop();
            return;
        }

        canvas.drawRect(0, 0, w, h, dimPaint);

        float cx = w * 0.5f;
        float cy = h * 0.45f;

        if (t < PHASE_1_MS) {
            float p = t / (float) PHASE_1_MS;
            drawOrb(canvas, w, h, p);
            drawFlash(canvas, cx, cy, 0.30f + 0.80f * p, p);
            drawPixels(canvas, w, h, 1.0f - 0.25f * p, p);
            drawText(canvas, LocaleController.getString(R.string.UspdmpshmPhase1), w, h, p);
        } else if (t < PHASE_1_MS + PHASE_2_MS) {
            float p = (t - PHASE_1_MS) / (float) PHASE_2_MS;
            drawPixels(canvas, w, h, 1.0f - 0.85f * p, 1.0f);
            drawSingularity(canvas, cx, cy, w, h, p);
            drawText(canvas, LocaleController.getString(R.string.UspdmpshmPhase2), w, h, p);
        } else if (t < PHASE_1_MS + PHASE_2_MS + PHASE_3_MS) {
            float p = (t - PHASE_1_MS - PHASE_2_MS) / (float) PHASE_3_MS;
            drawBurst(canvas, cx, cy, w, h, p);
            drawOrb(canvas, w, h, 1.0f - p);
            drawText(canvas, LocaleController.getString(R.string.UspdmpshmPhase3), w, h, p);
        } else {
            float p = (t - PHASE_1_MS - PHASE_2_MS - PHASE_3_MS) / (float) PHASE_4_MS;
            drawChaos(canvas, cx, cy, w, h, p);
        }

        postInvalidateOnAnimation();
    }

    private void drawText(Canvas canvas, @NonNull String text, int w, int h, float p) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        float alpha;
        if (p < 0.15f) {
            alpha = p / 0.15f;
        } else if (p > 0.85f) {
            alpha = (1f - p) / 0.15f;
        } else {
            alpha = 1f;
        }
        textPaint.setAlpha((int) (235 * Math.max(0f, Math.min(1f, alpha))));
        canvas.drawText(text, w * 0.5f, h * 0.22f, textPaint);
    }

    private void drawOrb(Canvas canvas, int w, int h, float p) {
        float t = SystemClock.uptimeMillis() / 450.0f;
        float x = (float) (w * (0.15 + 0.70 * (0.5 + 0.5 * Math.sin(t))));
        float y = (float) (h * (0.20 + 0.55 * (0.5 + 0.5 * Math.cos(t * 0.9))));
        float r = dp(55) * (0.9f + 0.2f * (1f - p));

        canvas.save();
        canvas.translate(x, y);
        canvas.scale(0.9f + 0.35f * p, 0.9f + 0.35f * p);
        // tint orb between red and blue
        int c = Color.HSVToColor(210, new float[]{(p < 0.5f ? 220 : 5), 0.85f, 1.0f});
        orbPaint.setColorFilter(new android.graphics.PorterDuffColorFilter(c, android.graphics.PorterDuff.Mode.SRC_ATOP));
        canvas.drawCircle(0, 0, r, orbPaint);
        orbPaint.setColorFilter(null);
        canvas.restore();
    }

    private void drawFlash(Canvas canvas, float cx, float cy, float strength, float p) {
        float radius = (float) (Math.hypot(getWidth(), getHeight()) * strength);
        int c1 = Color.HSVToColor((int) (230 * (1f - p)), new float[]{(p < 0.5f ? 210 : 0), 0.9f, 1.0f});
        int c2 = 0x00000000;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new RadialGradient(cx, cy, radius, new int[]{c1, c2}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }

    private void drawPixels(Canvas canvas, int w, int h, float scale, float alphaP) {
        int step = dp(18);
        float cx = w * 0.5f;
        float cy = h * 0.5f;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);
        canvas.translate(-cx, -cy);
        int alpha = (int) (140 * Math.max(0f, Math.min(1f, alphaP)));
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                if (((x + y) / step) % 5 != 0) {
                    continue;
                }
                float hue = (x * 1.0f / Math.max(1, w)) * 360f;
                pixelPaint.setColor(Color.HSVToColor(alpha, new float[]{hue, 0.9f, 1.0f}));
                tmpRect.set(x, y, x + step * 0.9f, y + step * 0.9f);
                canvas.drawRect(tmpRect, pixelPaint);
            }
        }
        canvas.restore();
    }

    private void drawSingularity(Canvas canvas, float cx, float cy, int w, int h, float p) {
        float maxR = (float) Math.hypot(w, h) * 0.75f;
        float r = dp(8) + maxR * p;
        Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);
        hole.setShader(new RadialGradient(cx, cy, r, new int[]{0xFF000000, 0xD0000000, 0x00000000}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, hole);

        int c = Color.HSVToColor(220, new float[]{210f + 140f * p, 0.9f, 1.0f});
        ringPaint.setShader(new LinearGradient(cx - r, cy, cx + r, cy, new int[]{c, 0xFFFFFFFF, c}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        ringPaint.setAlpha((int) (220 * (1f - p)));
        canvas.drawCircle(cx, cy, r * 0.72f, ringPaint);
        ringPaint.setShader(null);
    }

    private void drawBurst(Canvas canvas, float cx, float cy, int w, int h, float p) {
        float radius = (float) Math.hypot(w, h) * (0.15f + 0.75f * p);
        int c1 = Color.HSVToColor(225, new float[]{0f, 0.95f, 1.0f});
        int c2 = Color.HSVToColor(225, new float[]{210f, 0.95f, 1.0f});
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new RadialGradient(cx, cy, radius, new int[]{c1, c2, 0x00000000}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
    }

    private void drawChaos(Canvas canvas, float cx, float cy, int w, int h, float p) {
        float pulse = 0.85f + 0.15f * (float) Math.sin(SystemClock.uptimeMillis() / 220.0);
        float radius = (float) Math.hypot(w, h) * (0.55f + 0.35f * pulse);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new RadialGradient(cx, cy, radius,
                new int[]{
                        Color.HSVToColor(120, new float[]{(SystemClock.uptimeMillis() / 8f) % 360f, 1f, 1f}),
                        0x2200D5FF,
                        0x00000000
                },
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);

        // quick rainbow streaks
        Paint streak = new Paint(Paint.ANTI_ALIAS_FLAG);
        streak.setAlpha(160);
        int count = 14;
        for (int i = 0; i < count; i++) {
            float x = (float) (Math.random() * w);
            float len = dp(120 + (float) Math.random() * 420);
            float y2 = (float) (Math.random() * h);
            float y1 = y2 - len;
            float hue = (i / (float) count) * 360f + (SystemClock.uptimeMillis() / 10f);
            int c = Color.HSVToColor(180, new float[]{hue % 360f, 0.95f, 1.0f});
            streak.setStrokeWidth(dp(2f + (float) Math.random() * 2.5f));
            streak.setShader(new LinearGradient(x, y1, x, y2, new int[]{0x00FFFFFF, c, 0x00FFFFFF}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawLine(x, y1, x, y2, streak);
        }
        streak.setShader(null);
    }
}

