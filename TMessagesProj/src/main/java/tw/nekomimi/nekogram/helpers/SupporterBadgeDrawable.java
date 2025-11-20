package tw.nekomimi.nekogram.helpers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.AndroidUtilities;

public class SupporterBadgeDrawable extends Drawable {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint symbolPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path diamondPath = new Path();
    private final Path symbolPath = new Path();
    private final Path symbolPathSecondary = new Path();

    private int alpha = 255;

    public SupporterBadgeDrawable() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(AndroidUtilities.dp(1f));
        borderPaint.setColor(0x88FFFFFF);

        symbolPaint.setStyle(Paint.Style.FILL);
        symbolPaint.setColor(Color.WHITE);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }

        fillPaint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                0xFFFF8A3D, 0xFF7F4CFF, Shader.TileMode.CLAMP));
        fillPaint.setAlpha(alpha);
        borderPaint.setAlpha(alpha);
        symbolPaint.setAlpha(alpha);

        float cx = bounds.exactCenterX();
        float cy = bounds.exactCenterY();
        float halfWidth = bounds.width() / 2f;
        float halfHeight = bounds.height() / 2f;

        diamondPath.reset();
        diamondPath.moveTo(cx, bounds.top);
        diamondPath.lineTo(bounds.right, cy);
        diamondPath.lineTo(cx, bounds.bottom);
        diamondPath.lineTo(bounds.left, cy);
        diamondPath.close();

        canvas.drawPath(diamondPath, fillPaint);
        canvas.drawPath(diamondPath, borderPaint);

        float outer = Math.min(halfWidth, halfHeight) * 0.95f;
        float inner = outer * 0.45f;

        symbolPath.reset();
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 4d * i;
            float radius = (i % 2 == 0) ? outer : inner;
            float x = cx + (float) (Math.cos(angle) * radius);
            float y = cy + (float) (Math.sin(angle) * radius);
            if (i == 0) {
                symbolPath.moveTo(x, y);
            } else {
                symbolPath.lineTo(x, y);
            }
        }
        symbolPath.close();

        canvas.drawPath(symbolPath, symbolPaint);

        symbolPathSecondary.reset();
        symbolPathSecondary.moveTo(cx, cy - inner);
        symbolPathSecondary.lineTo(cx + inner, cy);
        symbolPathSecondary.lineTo(cx, cy + inner);
        symbolPathSecondary.lineTo(cx - inner, cy);
        symbolPathSecondary.close();
        symbolPaint.setAlpha((int) (alpha * 0.45f));
        canvas.drawPath(symbolPathSecondary, symbolPaint);
        symbolPaint.setAlpha(alpha);
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        borderPaint.setColorFilter(colorFilter);
        symbolPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return alpha == 255 ? android.graphics.PixelFormat.OPAQUE : android.graphics.PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return AndroidUtilities.dp(22);
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(22);
    }
}
