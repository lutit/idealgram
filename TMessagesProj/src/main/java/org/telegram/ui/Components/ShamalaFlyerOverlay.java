package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.text.TextPaint;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class ShamalaFlyerOverlay extends View {

    private static final int MAX_PARTICLES = 240;
    private static final long SPAWN_INTERVAL_MS = 350;
    private static final int BASE_PARTICLES = 6;

    private final Random random = new Random();
    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final TextPaint emojiPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap logoBitmap;
    private boolean started;
    private long startedAt;
    private long lastUpdateTime;
    private int activeCount;
    private int maxActive;

    public ShamalaFlyerOverlay(Context context) {
        super(context);
        setVisibility(GONE);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        emojiPaint.setTextAlign(Paint.Align.CENTER);
        emojiPaint.setAlpha(220);

        loadLogoBitmap(context);

        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle();
        }
    }

    private void loadLogoBitmap(Context context) {
        try (InputStream stream = context.getAssets().open("uzbekogram.png")) {
            logoBitmap = BitmapFactory.decodeStream(stream);
        } catch (IOException ignore) {
            logoBitmap = null;
        }
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        startedAt = SystemClock.uptimeMillis();
        lastUpdateTime = startedAt;
        activeCount = 0;
        setVisibility(VISIBLE);
        invalidate();
    }

    public void stop() {
        if (!started) {
            return;
        }
        started = false;
        setVisibility(GONE);
        activeCount = 0;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int area = Math.max(1, w * h);
        float base = AndroidUtilities.dp(90);
        int estimate = (int) (area / (base * base) * 6f);
        maxActive = Math.min(MAX_PARTICLES, Math.max(80, estimate));
        resetAll(w, h);
    }

    private void resetAll(int w, int h) {
        for (Particle particle : particles) {
            particle.reset(w, h, true, random, logoBitmap != null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!started) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        long elapsed = now - startedAt;
        int target = Math.min(maxActive, BASE_PARTICLES + (int) (elapsed / SPAWN_INTERVAL_MS));
        if (target > activeCount) {
            int w = getWidth();
            int h = getHeight();
            for (int i = activeCount; i < target; i++) {
                particles[i].reset(w, h, false, random, logoBitmap != null);
            }
            activeCount = target;
        }

        int dt = (int) Math.min(32, Math.max(0, now - lastUpdateTime));
        lastUpdateTime = now;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            postInvalidateOnAnimation();
            return;
        }

        float move = dt / 1000f;
        for (int i = 0; i < activeCount; i++) {
            Particle particle = particles[i];
            particle.x += particle.vx * move;
            particle.y += particle.vy * move;
            particle.rotation += particle.vr * move;

            float buffer = AndroidUtilities.dp(120);
            if (particle.x < -buffer || particle.x > w + buffer || particle.y < -buffer || particle.y > h + buffer) {
                particle.reset(w, h, false, random, logoBitmap != null);
            }

            drawParticle(canvas, particle);
        }

        postInvalidateOnAnimation();
    }

    private void drawParticle(Canvas canvas, Particle particle) {
        if (particle.isEmoji) {
            float textSize = particle.size;
            emojiPaint.setTextSize(textSize);
            canvas.save();
            canvas.translate(particle.x, particle.y);
            canvas.rotate(particle.rotation);
            canvas.drawText("✅", 0, textSize * 0.35f, emojiPaint);
            canvas.restore();
            return;
        }

        if (logoBitmap == null || logoBitmap.isRecycled()) {
            return;
        }

        float half = particle.size * 0.5f;
        float scale = particle.size / Math.max(1f, Math.max(logoBitmap.getWidth(), logoBitmap.getHeight()));
        canvas.save();
        canvas.translate(particle.x, particle.y);
        canvas.rotate(particle.rotation);
        canvas.scale(scale, scale);
        canvas.drawBitmap(logoBitmap, -logoBitmap.getWidth() / 2f, -logoBitmap.getHeight() / 2f, bitmapPaint);
        canvas.restore();
    }

    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float rotation;
        float vr;
        float size;
        boolean isEmoji;

        void reset(int w, int h, boolean randomStart, Random random, boolean hasBitmap) {
            float sizeBase = AndroidUtilities.dp(28);
            float sizeSpread = AndroidUtilities.dp(52);
            size = sizeBase + random.nextFloat() * sizeSpread;
            rotation = random.nextFloat() * 360f;
            vr = (random.nextFloat() * 2f - 1f) * 70f;
            isEmoji = !hasBitmap || random.nextFloat() < 0.35f;

            float speed = AndroidUtilities.dp(40 + random.nextFloat() * 140);
            float angle = (float) (random.nextFloat() * Math.PI * 2);
            vx = (float) Math.cos(angle) * speed;
            vy = (float) Math.sin(angle) * speed;

            if (w <= 0 || h <= 0) {
                x = 0;
                y = 0;
                return;
            }

            float buffer = AndroidUtilities.dp(120);
            if (randomStart) {
                x = random.nextFloat() * w;
                y = random.nextFloat() * h;
            } else {
                int edge = random.nextInt(4);
                if (edge == 0) {
                    x = -buffer;
                    y = random.nextFloat() * h;
                } else if (edge == 1) {
                    x = w + buffer;
                    y = random.nextFloat() * h;
                } else if (edge == 2) {
                    x = random.nextFloat() * w;
                    y = -buffer;
                } else {
                    x = random.nextFloat() * w;
                    y = h + buffer;
                }
            }
        }
    }
}
