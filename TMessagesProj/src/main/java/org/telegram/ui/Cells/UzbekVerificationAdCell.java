package org.telegram.ui.Cells;

import android.animation.ObjectAnimator;
import android.content.Context;
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

public class UzbekVerificationAdCell extends FrameLayout {

    private final FrameLayout adContainer;
    private final View flashOverlay;
    private final TextView actionButton;
    private final ObjectAnimator flashAnimator;
    private final ObjectAnimator pulseAnimator;

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

        TextView badge = new TextView(context);
        badge.setText("Реклама");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        contentLayout.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText("ВНИМАНИЕ: нам не удалось подтвердить, что вы узбек!!!!!!");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        title.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        contentLayout.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView subtitle = new TextView(context);
        subtitle.setText("Нажмите на кнопку \"НАЧАТЬ ПРОВЕРКУ\" для подтверждения.");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitle.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(10));
        contentLayout.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        actionButton = new TextView(context);
        actionButton.setText("НАЧАТЬ ПРОВЕРКУ");
        actionButton.setGravity(Gravity.CENTER);
        actionButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        actionButton.setTypeface(AndroidUtilities.bold());
        actionButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        actionButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                AndroidUtilities.dp(8),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)
        ));
        actionButton.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(9), AndroidUtilities.dp(14), AndroidUtilities.dp(9));
        contentLayout.addView(actionButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        flashOverlay = new View(context);
        flashOverlay.setAlpha(0f);
        flashOverlay.setBackgroundColor(0x33FF0000);
        adContainer.addView(flashOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        flashAnimator = ObjectAnimator.ofFloat(flashOverlay, View.ALPHA, 0f, 0.6f, 0f);
        flashAnimator.setDuration(420);
        flashAnimator.setRepeatMode(ObjectAnimator.RESTART);
        flashAnimator.setRepeatCount(ObjectAnimator.INFINITE);

        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                adContainer,
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.02f),
                android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.02f)
        );
        pulseAnimator.setDuration(560);
        pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void setOnStartCheckClickListener(OnClickListener listener) {
        actionButton.setOnClickListener(listener);
        adContainer.setOnClickListener(listener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!flashAnimator.isStarted()) {
            flashAnimator.start();
        }
        if (!pulseAnimator.isStarted()) {
            pulseAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        flashAnimator.cancel();
        pulseAnimator.cancel();
        adContainer.setScaleX(1f);
        adContainer.setScaleY(1f);
    }
}
