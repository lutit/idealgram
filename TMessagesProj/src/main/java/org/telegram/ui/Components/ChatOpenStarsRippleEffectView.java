package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

import java.util.Random;

public class ChatOpenStarsRippleEffectView extends View {

    public static final long DURATION_MS = 2000L;

    private static final int[] STAR_COLORS = new int[]{
            0xFFFFFFFF,
            0xFFFFF3B0,
            0xFFB6F3FF,
            0xFFFFB6F1,
            0xFFB8FFCB,
            0xFFE5D4FF
    };

    private static final float TWO_PI = (float) (Math.PI * 2);

    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rippleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path unitStar = new Path();
    private final Random random = new Random();

    private Star[] stars;
    private float progress;
    private ValueAnimator animator;
    private boolean started;
    private @Nullable Runnable onEnd;

    private float centerX;
    private float centerY;
    private float maxRippleRadius;

    private static class Star {
        float startX;
        float startY;
        float velocityX;
        float velocityY;
        float size;
        float rotation;
        float rotationSpeed;
        float alpha;
        float phase;
        float twinkleSpeed;
        int color;
        boolean glow;
    }

    public ChatOpenStarsRippleEffectView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setEnabled(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        starPaint.setStyle(Paint.Style.FILL);
        glowPaint.setStyle(Paint.Style.FILL);

        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeCap(Paint.Cap.ROUND);
        ripplePaint.setStrokeJoin(Paint.Join.ROUND);

        rippleFillPaint.setStyle(Paint.Style.FILL);
        sparklePaint.setStyle(Paint.Style.FILL);

        buildUnitStarPath();
    }

    public void start(@Nullable Runnable onEnd) {
        if (started) {
            return;
        }
        started = true;
        this.onEnd = onEnd;
        setAlpha(1f);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progress = 1f;
                invalidate();
                Runnable callback = ChatOpenStarsRippleEffectView.this.onEnd;
                ChatOpenStarsRippleEffectView.this.onEnd = null;
                if (callback != null) {
                    callback.run();
                }
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        onEnd = null;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        maxRippleRadius = (float) Math.hypot(w, h) * 0.58f;
        initStars(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        final float t = progress;
        final float easedOut = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(t);
        final float timeSeconds = (DURATION_MS / 1000f) * easedOut;

        final float fadeOut = 1f - CubicBezierInterpolator.EASE_OUT.getInterpolation(t);
        final float fadeOutFast = 1f - CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(t);

        drawRipples(canvas, t, fadeOut);
        drawStars(canvas, t, timeSeconds, fadeOut, fadeOutFast);
        drawSparkles(canvas, t, timeSeconds, fadeOut);
    }

    private void drawRipples(Canvas canvas, float t, float fadeOut) {
        final int baseAlpha = (int) (110 * fadeOut);
        rippleFillPaint.setColor((baseAlpha << 24) | 0xFFFFFF);

        // Subtle center glow.
        canvas.drawCircle(centerX, centerY, AndroidUtilities.dp(36) + t * AndroidUtilities.dp(64), rippleFillPaint);

        final int rings = 7;
        for (int i = 0; i < rings; i++) {
            float start = i * 0.085f;
            float p = (t - start) / (1f - start);
            if (p <= 0f || p >= 1f) {
                continue;
            }
            p = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(p);

            float radius = maxRippleRadius * p;
            float ringAlpha = (1f - p) * fadeOut;

            ripplePaint.setStrokeWidth(AndroidUtilities.dp(1.5f) + (1f - p) * AndroidUtilities.dp(10));
            int a = (int) (150 * ringAlpha);
            ripplePaint.setColor((a << 24) | 0xFFFFFF);
            canvas.drawCircle(centerX, centerY, radius, ripplePaint);
        }
    }

    private void drawStars(Canvas canvas, float t, float timeSeconds, float fadeOut, float fadeOutFast) {
        Star[] localStars = stars;
        if (localStars == null) {
            return;
        }

        final float gravity = AndroidUtilities.dp(190);
        final float drift = AndroidUtilities.dp(30) * (t - 0.5f);

        for (Star s : localStars) {
            float x = s.startX + s.velocityX * timeSeconds + drift;
            float y = s.startY + s.velocityY * timeSeconds + gravity * timeSeconds * timeSeconds * 0.12f;

            float baseFade = s.glow ? fadeOutFast : fadeOut;
            float a = s.alpha * baseFade * twinkle(t, s.phase, s.twinkleSpeed);
            if (a <= 0.01f) {
                continue;
            }

            float rotation = s.rotation + s.rotationSpeed * timeSeconds;
            float size = s.size * (0.95f + 0.15f * (1f - t));

            if (s.glow) {
                int glowAlpha = (int) (90 * a);
                glowPaint.setColor((glowAlpha << 24) | (s.color & 0x00FFFFFF));
                canvas.save();
                canvas.translate(x, y);
                canvas.rotate(rotation);
                canvas.scale(size * 1.9f, size * 1.9f);
                canvas.drawPath(unitStar, glowPaint);
                canvas.restore();
            }

            int alpha = (int) (255 * a);
            starPaint.setColor((alpha << 24) | (s.color & 0x00FFFFFF));
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(rotation);
            canvas.scale(size, size);
            canvas.drawPath(unitStar, starPaint);
            canvas.restore();
        }
    }

    private void drawSparkles(Canvas canvas, float t, float timeSeconds, float fadeOut) {
        // Extra "a lot of stuff": tiny points flying around to fill the screen.
        final int count = 220;
        final float spread = maxRippleRadius * (0.35f + 0.65f * t);
        final float baseAlpha = 170 * fadeOut;

        for (int i = 0; i < count; i++) {
            float p = (i / (float) count);
            float angle = (p * 18f + t * 7f) * TWO_PI;
            float r = spread * (0.15f + 0.85f * fract(p * 13.37f + t * 1.7f));

            float x = centerX + (float) Math.cos(angle) * r + (float) Math.sin(angle * 3) * AndroidUtilities.dp(10);
            float y = centerY + (float) Math.sin(angle) * r + (float) Math.cos(angle * 2) * AndroidUtilities.dp(10);

            float size = AndroidUtilities.dp(0.9f) + AndroidUtilities.dp(1.9f) * fract(p * 9.11f + t * 2.1f);
            int a = (int) (baseAlpha * (0.35f + 0.65f * fract(p * 6.77f + t * 4.2f)));
            sparklePaint.setColor((a << 24) | 0xFFFFFF);
            canvas.drawCircle(x, y, size, sparklePaint);
        }
    }

    private void initStars(int w, int h) {
        // "A lot": scaled a bit by area, but capped to keep it smooth.
        int area = w * h;
        int count = (int) (420 + Math.min(420, (area / (float) (AndroidUtilities.dp(6) * AndroidUtilities.dp(6))) * 0.12f));
        count = Math.max(520, Math.min(840, count));

        Star[] localStars = new Star[count];
        float jitter = AndroidUtilities.dp(24);
        float minSpeed = AndroidUtilities.dp(140);
        float maxSpeed = AndroidUtilities.dp(980);

        for (int i = 0; i < count; i++) {
            Star s = new Star();

            float angle = random.nextFloat() * TWO_PI;
            float speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);
            float directional = 0.4f + 0.6f * random.nextFloat();

            s.velocityX = (float) Math.cos(angle) * speed * directional;
            s.velocityY = (float) Math.sin(angle) * speed * directional - AndroidUtilities.dp(90) * random.nextFloat();

            s.startX = centerX + (random.nextFloat() - 0.5f) * jitter;
            s.startY = centerY + (random.nextFloat() - 0.5f) * jitter;

            float sizeDp = 1.6f + random.nextFloat() * 6.3f;
            s.size = AndroidUtilities.dp(sizeDp);

            s.rotation = random.nextFloat() * 360f;
            s.rotationSpeed = (random.nextFloat() - 0.5f) * 880f;

            s.alpha = 0.35f + random.nextFloat() * 0.65f;
            s.phase = random.nextFloat();
            s.twinkleSpeed = 2.0f + random.nextFloat() * 5.2f;

            s.color = STAR_COLORS[random.nextInt(STAR_COLORS.length)];
            s.glow = random.nextFloat() < 0.18f;

            localStars[i] = s;
        }

        stars = localStars;
    }

    private void buildUnitStarPath() {
        unitStar.reset();

        // 5-point star centered at (0, 0), outer radius = 1.
        final int points = 5;
        final float outer = 1f;
        final float inner = 0.46f;
        final float startAngle = -90f;

        for (int i = 0; i < points * 2; i++) {
            float a = (startAngle + i * (360f / (points * 2))) * (float) Math.PI / 180f;
            float r = (i % 2 == 0) ? outer : inner;
            float x = (float) Math.cos(a) * r;
            float y = (float) Math.sin(a) * r;
            if (i == 0) {
                unitStar.moveTo(x, y);
            } else {
                unitStar.lineTo(x, y);
            }
        }
        unitStar.close();
    }

    private static float twinkle(float t, float phase, float speed) {
        // A cheap triangle-wave twinkle in [0.55..1.0].
        float p = fract(t * speed + phase);
        float tri = 1f - Math.abs(p * 2f - 1f);
        return 0.55f + 0.45f * tri;
    }

    private static float fract(float x) {
        return x - (float) Math.floor(x);
    }
}
