package org.telegram.ui.Cells;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Random;

public class UzbekVerificationAdCell extends FrameLayout {

    private final FrameLayout adContainer;
    private final View flashOverlay;
    private final TextView actionButton;
    private final TextView title;
    private final TextView subtitle;
    private final TextView badge;
    private ObjectAnimator flashAnimator;
    private ObjectAnimator pulseAnimator;
    private ValueAnimator lsdAnimator;
    private boolean isShown = false;

    public UzbekVerificationAdCell(Context context) {
        super(context);
        setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));

        adContainer = new FrameLayout(context);
        GradientDrawable cardDrawable = new GradientDrawable();
        cardDrawable.setColor(Theme.getColor(Theme.key_chat_inBubble));
        cardDrawable.setCornerRadius(AndroidUtilities.dp(14));
        adContainer.setBackground(cardDrawable);
        addView(adContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        adContainer.addView(contentLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        badge = new TextView(context);
        badge.setText("РЕКЛАМА БЛЯТЬ");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setTextColor(Color.RED);
        contentLayout.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        title = new TextView(context);
        title.setText("ВНИМАНИЕ: нам не удалось подтвердить, что вы узбек!!!!!!");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        title.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        contentLayout.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        subtitle = new TextView(context);
        subtitle.setText("Нажмите на кнопку \"НАЧАТЬ ПРОВЕРКУ\" ИНАЧЕ ПИЗДЕЦ.");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitle.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(10));
        contentLayout.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        actionButton = new TextView(context);
        actionButton.setText("НАЧАТЬ ПРОВЕРКУ!!!!!!");
        actionButton.setGravity(Gravity.CENTER);
        actionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        actionButton.setTypeface(AndroidUtilities.bold());
        actionButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        actionButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(8),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)
        ));
        actionButton.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(9), AndroidUtilities.dp(14), AndroidUtilities.dp(9));
        contentLayout.addView(actionButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView closeButton = new TextView(context);
        closeButton.setText("СБЕЖАТЬ (ЗАКРЫТЬ)");
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        closeButton.setTypeface(AndroidUtilities.bold());
        closeButton.setTextColor(Color.WHITE);
        closeButton.setBackgroundColor(Color.DKGRAY);
        closeButton.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(9), AndroidUtilities.dp(14), AndroidUtilities.dp(9));
        contentLayout.addView(closeButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 10, 0, 0));
        
        closeButton.setOnClickListener(v -> {
            setVisibility(GONE);
            if (getLayoutParams() != null) {
                getLayoutParams().height = 0;
                requestLayout();
            }
        });

        flashOverlay = new View(context);
        flashOverlay.setAlpha(0f);
        flashOverlay.setBackgroundColor(0x77FF0000);
        adContainer.addView(flashOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        adContainer.setAlpha(0f);
        setVisibility(GONE);
    }

    public void setOnStartCheckClickListener(OnClickListener listener) {
        actionButton.setOnClickListener(listener);
        adContainer.setOnClickListener(listener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isShown) {
            AndroidUtilities.runOnUIThread(() -> {
                setVisibility(VISIBLE);
                adContainer.animate().alpha(1f).setDuration(100).start();
                isShown = true;
                startTrashAnimations();
            }, 3000);
        } else {
            startTrashAnimations();
        }
    }

    private void startTrashAnimations() {
        if (flashAnimator == null) {
            flashAnimator = ObjectAnimator.ofFloat(flashOverlay, View.ALPHA, 0f, 0.8f, 0f);
            flashAnimator.setDuration(200);
            flashAnimator.setRepeatMode(ObjectAnimator.RESTART);
            flashAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        }
        if (!flashAnimator.isStarted()) {
            flashAnimator.start();
        }

        if (pulseAnimator == null) {
            pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                    adContainer,
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.1f),
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.1f),
                    android.animation.PropertyValuesHolder.ofFloat(View.ROTATION, -5f, 5f)
            );
            pulseAnimator.setDuration(150);
            pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        }
        if (!pulseAnimator.isStarted()) {
            pulseAnimator.start();
        }

        if (lsdAnimator == null) {
            lsdAnimator = ValueAnimator.ofFloat(0f, 1f);
            lsdAnimator.setDuration(100);
            lsdAnimator.setRepeatCount(ValueAnimator.INFINITE);
            Random rand = new Random();
            lsdAnimator.addUpdateListener(animation -> {
                title.setTextColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                subtitle.setTextColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                badge.setTextColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                actionButton.setBackgroundColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                actionButton.setTextColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
            });
        }
        if (!lsdAnimator.isStarted()) {
            lsdAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (flashAnimator != null) flashAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
        if (lsdAnimator != null) lsdAnimator.cancel();
        adContainer.setScaleX(1f);
        adContainer.setScaleY(1f);
        adContainer.setRotation(0f);
    }
}