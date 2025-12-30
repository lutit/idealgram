package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.TextPaint;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class ShamalaScreamerOverlay extends View {

    private static final long TEXT_PHASE_MS = 1200;
    private static final long IMAGE_PHASE_MS = 2000;
    private static final long TOTAL_MS = TEXT_PHASE_MS + IMAGE_PHASE_MS;

    private static final String[] PHRASES = new String[]{
            "WAKE UP",
            "DONT LOOK",
            "NO EXIT",
            "BEHIND YOU",
            "STAY STILL",
            "ITS HERE"
    };

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF imageRect = new RectF();
    private final Random random = new Random();

    private Bitmap screamerBitmap;
    private long startedAt;
    private boolean started;

    public ShamalaScreamerOverlay(Context context) {
        super(context);

        setVisibility(GONE);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        dimPaint.setColor(0xE0000000);
        textPaint.setColor(0xFFF2F2F2);
        textPaint.setTextSize(dp(26));
        textPaint.setFakeBoldText(true);

        loadScreamerBitmap(context);
    }

    private void loadScreamerBitmap(Context context) {
        try (InputStream stream = context.getAssets().open("hamza.jpg")) {
            screamerBitmap = BitmapFactory.decodeStream(stream);
        } catch (IOException ignore) {
            screamerBitmap = null;
        }
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
        if (!started) {
            return;
        }
        started = false;
        setVisibility(GONE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!started) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        long t = now - startedAt;
        if (t >= TOTAL_MS) {
            stop();
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            postInvalidateOnAnimation();
            return;
        }

        if (t < TEXT_PHASE_MS) {
            float p = t / (float) TEXT_PHASE_MS;
            int alpha = (int) (120 + 100 * Math.min(1f, p * 1.6f));
            dimPaint.setAlpha(alpha);
            canvas.drawRect(0, 0, w, h, dimPaint);
            drawHallucinations(canvas, w, h, now);
        } else {
            canvas.drawRect(0, 0, w, h, dimPaint);
            drawScreamer(canvas, w, h);
        }

        postInvalidateOnAnimation();
    }

    private void drawHallucinations(Canvas canvas, int w, int h, long now) {
        int steps = 5;
        long seed = now / 80;
        random.setSeed(seed);
        for (int i = 0; i < steps; i++) {
            String text = PHRASES[random.nextInt(PHRASES.length)];
            float x = dp(24) + random.nextFloat() * (w - dp(48));
            float y = dp(80) + random.nextFloat() * (h - dp(160));
            float jitter = dp(3) * (random.nextFloat() - 0.5f);
            textPaint.setAlpha(180 + random.nextInt(75));
            canvas.drawText(text, x + jitter, y + jitter, textPaint);
        }
    }

    private void drawScreamer(Canvas canvas, int w, int h) {
        if (screamerBitmap == null || screamerBitmap.isRecycled()) {
            return;
        }
        float bw = screamerBitmap.getWidth();
        float bh = screamerBitmap.getHeight();
        float scale = Math.max(w / bw, h / bh);
        float dw = bw * scale;
        float dh = bh * scale;
        float left = (w - dw) * 0.5f;
        float top = (h - dh) * 0.5f;
        imageRect.set(left, top, left + dw, top + dh);
        canvas.drawBitmap(screamerBitmap, null, imageRect, imagePaint);
    }
}
