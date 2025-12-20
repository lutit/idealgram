package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

public class ChaosOverlay extends View {

    private static final int FLAKES_COUNT = 70;
    private static final int STREAKS_COUNT = 18;

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint burstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flakePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint streakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Flake[] flakes = new Flake[FLAKES_COUNT];
    private final Streak[] streaks = new Streak[STREAKS_COUNT];

    private long lastUpdateTime;
    private boolean started;

    public ChaosOverlay(Context context) {
        super(context);
        init();
    }

    private void init() {
        setVisibility(GONE);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        dimPaint.setColor(0x3A00152A);

        burstPaint.setStrokeCap(Paint.Cap.ROUND);
        burstPaint.setStrokeWidth(dp(2));
        burstPaint.setAlpha(210);
        if (Build.VERSION.SDK_INT >= 29) {
            burstPaint.setBlendMode(BlendMode.SCREEN);
        } else {
            //noinspection deprecation
            burstPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        }

        flakePaint.setColor(Color.WHITE);
        flakePaint.setAlpha(210);
        flakePaint.setStrokeCap(Paint.Cap.ROUND);
        flakePaint.setStrokeWidth(dp(1));
        if (Build.VERSION.SDK_INT >= 29) {
            flakePaint.setBlendMode(BlendMode.SCREEN);
        } else {
            //noinspection deprecation
            flakePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        }

        streakPaint.setAlpha(180);
        if (Build.VERSION.SDK_INT >= 29) {
            streakPaint.setBlendMode(BlendMode.SCREEN);
        } else {
            //noinspection deprecation
            streakPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        }

        for (int i = 0; i < flakes.length; i++) {
            flakes[i] = new Flake();
        }
        for (int i = 0; i < streaks.length; i++) {
            streaks[i] = new Streak();
        }
        randomizeAll();
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        lastUpdateTime = SystemClock.uptimeMillis();
        setVisibility(VISIBLE);
        invalidate();
    }

    public void stop() {
        if (!started) {
            return;
        }
        started = false;
        setVisibility(GONE);
        lastUpdateTime = 0;
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        return false;
    }

    private void randomizeAll() {
        for (Flake flake : flakes) {
            flake.reset(getWidth(), getHeight(), true);
        }
        for (Streak streak : streaks) {
            streak.reset(getWidth(), getHeight(), true);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        randomizeAll();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!started) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        int dt = (int) Math.min(32, Math.max(0, now - lastUpdateTime));
        lastUpdateTime = now;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            postInvalidateOnAnimation();
            return;
        }

        canvas.drawRect(0, 0, w, h, dimPaint);

        float cx = w * 0.5f;
        float cy = h * 0.45f;
        float baseRadius = (float) Math.hypot(w, h) * 0.75f;

        float pulse = 0.85f + 0.15f * (float) Math.sin(now / 280.0);
        RadialGradient vignette = new RadialGradient(
                cx,
                cy,
                baseRadius,
                new int[]{0x2200D5FF, 0x16006BFF, 0x00000000},
                new float[]{0.0f, 0.55f * pulse, 1.0f},
                Shader.TileMode.CLAMP
        );
        Paint vignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vignettePaint.setShader(vignette);
        canvas.drawRect(0, 0, w, h, vignettePaint);

        drawBurst(canvas, now, cx, cy, baseRadius);
        drawStreaks(canvas, dt, w, h, now);
        drawFlakes(canvas, dt, w, h, now);

        postInvalidateOnAnimation();
    }

    private void drawBurst(Canvas canvas, long now, float cx, float cy, float radius) {
        int rays = 80;
        float t = now / 800.0f;
        float phase = (float) (t - Math.floor(t));
        float inner = radius * (0.06f + 0.02f * (float) Math.sin(now / 210.0));
        float outerBase = radius * 0.78f;

        for (int i = 0; i < rays; i++) {
            float a = (float) (i * (Math.PI * 2.0 / rays) + now / 900.0);
            float noise = 0.45f + 0.55f * (float) Math.sin(i * 13.7 + now / 170.0);
            float outer = outerBase * (0.72f + 0.28f * noise);

            float hue = (float) ((i / (float) rays) * 360.0 + now / 12.0) % 360.0f;
            int color = Color.HSVToColor(210, new float[]{hue, 0.9f, 1.0f});
            if (Build.VERSION.SDK_INT >= 29) {
                burstPaint.setColorFilter(new BlendModeColorFilter(color, BlendMode.SRC_ATOP));
            } else {
                //noinspection deprecation
                burstPaint.setColorFilter(new android.graphics.PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            }

            float alpha = 0.55f + 0.45f * (float) Math.sin((i * 0.35f) + phase * (float) Math.PI * 2f);
            burstPaint.setAlpha((int) (190 * alpha));

            float x1 = cx + inner * (float) Math.cos(a);
            float y1 = cy + inner * (float) Math.sin(a);
            float x2 = cx + outer * (float) Math.cos(a);
            float y2 = cy + outer * (float) Math.sin(a);
            canvas.drawLine(x1, y1, x2, y2, burstPaint);
        }
        burstPaint.setColorFilter(null);
    }

    private void drawFlakes(Canvas canvas, int dt, int w, int h, long now) {
        float move = dt / 16f;
        for (Flake flake : flakes) {
            flake.x += flake.vx * move;
            flake.y += flake.vy * move;
            flake.rot += flake.vr * move;

            if (flake.y > h + dp(24) || flake.x < -dp(48) || flake.x > w + dp(48)) {
                flake.reset(w, h, false);
            }

            float size = flake.size;
            float alphaPulse = 0.65f + 0.35f * (float) Math.sin((now / 120.0) + flake.seed);
            flakePaint.setAlpha((int) (210 * alphaPulse));

            canvas.save();
            canvas.translate(flake.x, flake.y);
            canvas.rotate(flake.rot);
            canvas.drawLine(-size, 0, size, 0, flakePaint);
            canvas.drawLine(0, -size, 0, size, flakePaint);
            canvas.drawLine(-size * 0.7f, -size * 0.7f, size * 0.7f, size * 0.7f, flakePaint);
            canvas.drawLine(-size * 0.7f, size * 0.7f, size * 0.7f, -size * 0.7f, flakePaint);
            canvas.restore();
        }
    }

    private void drawStreaks(Canvas canvas, int dt, int w, int h, long now) {
        float move = dt / 16f;
        for (Streak streak : streaks) {
            streak.y += streak.vy * move;
            if (streak.y - streak.length > h + dp(48)) {
                streak.reset(w, h, false);
            }

            float hue = (streak.hueBase + (now / 8.0f)) % 360f;
            int c1 = Color.HSVToColor(190, new float[]{hue, 0.95f, 1.0f});
            int c2 = Color.HSVToColor(0, new float[]{hue, 0.95f, 1.0f});
            LinearGradient g = new LinearGradient(
                    streak.x, streak.y - streak.length,
                    streak.x, streak.y,
                    new int[]{c2, c1, c2},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
            );
            streakPaint.setShader(g);
            streakPaint.setStrokeWidth(streak.width);
            canvas.drawLine(streak.x, streak.y - streak.length, streak.x, streak.y, streakPaint);
        }
        streakPaint.setShader(null);
    }

    private class Flake {
        float x;
        float y;
        float vx;
        float vy;
        float vr;
        float rot;
        float size;
        float seed;

        void reset(int w, int h, boolean initial) {
            float width = Math.max(1, w);
            float height = Math.max(1, h);
            x = (float) (Math.random() * width);
            y = initial ? (float) (Math.random() * height) : -dp(24) - (float) (Math.random() * dp(120));
            vx = AndroidUtilities.dp(0.15f) * (float) (Math.random() * 2.0 - 1.0);
            vy = AndroidUtilities.dp(0.8f + (float) Math.random() * 1.6f);
            vr = (float) (Math.random() * 6.0 - 3.0);
            rot = (float) (Math.random() * 360.0);
            size = dp(2.0f + (float) Math.random() * 3.5f);
            seed = (float) (Math.random() * 10.0);
        }
    }

    private class Streak {
        float x;
        float y;
        float vy;
        float length;
        float width;
        float hueBase;

        void reset(int w, int h, boolean initial) {
            float widthPx = Math.max(1, w);
            float heightPx = Math.max(1, h);
            x = (float) (Math.random() * widthPx);
            y = initial ? (float) (Math.random() * heightPx) : -dp(120) - (float) (Math.random() * dp(240));
            vy = AndroidUtilities.dp(6.5f + (float) Math.random() * 9.0f);
            length = dp(90 + (float) Math.random() * 260);
            width = dp(1.2f + (float) Math.random() * 2.2f);
            hueBase = (float) (Math.random() * 360.0);
        }
    }
}
