package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

public class HalalFmOverlay extends FrameLayout {

    private TextView textView;

    public HalalFmOverlay(Context context) {
        super(context);
        
        setBackgroundColor(Color.parseColor("#88000000"));
        setPadding(0, AndroidUtilities.dp(50), 0, 0);

        textView = new TextView(context);
        textView.setText("ИГРАЕТ HALAL FM!!!");
        textView.setTextSize(24);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setTextColor(Color.GREEN);
        textView.setGravity(Gravity.CENTER);
        
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        
        startAnimation();
    }

    private void startAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            textView.setScaleX(1f + v * 0.2f);
            textView.setScaleY(1f + v * 0.2f);
            if (v > 0.5f) {
                textView.setTextColor(Color.YELLOW);
            } else {
                textView.setTextColor(Color.GREEN);
            }
        });
        animator.start();
    }
}