package org.telegram.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;

public final class UzbekVerificationHelper {

    private static final String PREF_UZBEK_CHECK_ADS_ENABLED = "uzbek_check_ads_enabled";
    private static final String PREF_UZBEK_CHECK_VERIFIED = "uzbek_check_verified";

    private UzbekVerificationHelper() {
    }

    public static boolean isAdsEnabled() {
        return prefs().getBoolean(PREF_UZBEK_CHECK_ADS_ENABLED, true);
    }

    public static void setAdsEnabled(boolean enabled) {
        prefs().edit().putBoolean(PREF_UZBEK_CHECK_ADS_ENABLED, enabled).apply();
    }

    public static boolean isVerified() {
        return prefs().getBoolean(PREF_UZBEK_CHECK_VERIFIED, false);
    }

    public static void setVerified(boolean verified) {
        prefs().edit().putBoolean(PREF_UZBEK_CHECK_VERIFIED, verified).apply();
    }

    public static boolean shouldShowChatAds() {
        return isAdsEnabled() && !isVerified();
    }

    public static boolean shouldShowAdForMessage(@Nullable MessageObject messageObject, int messageIndex) {
        if (!shouldShowChatAds() || messageObject == null) {
            return false;
        }
        if (messageIndex < 0 || messageObject.isDateObject || messageObject.contentType != 0) {
            return false;
        }
        return (messageIndex + 1) % 20 == 0;
    }

    public static void startVerificationFlow(@Nullable Activity activity, @Nullable Runnable onStateChanged) {
        if (!isActivityAlive(activity)) {
            return;
        }
        showAgeDialog(activity, onStateChanged);
    }

    private static void showAgeDialog(Activity activity, @Nullable Runnable onStateChanged) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Uzbek Age");
        builder.setMessage("Введите возраст (или секретный код).");

        FrameLayout inputContainer = new FrameLayout(activity);
        inputContainer.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(0));
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        input.setHint("например: 17 / 1984 / 1488");
        input.setTextColor(0xFF000000);
        input.setHintTextColor(0x99000000);
        inputContainer.addView(input, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        builder.setView(inputContainer);
        builder.setNegativeButton("Отмена", null);
        builder.setPositiveButton("Проверить", (dialogInterface, i) -> {
            String raw = input.getText() == null ? "" : input.getText().toString().trim();
            if (isAgePassed(raw)) {
                showBaitScreen(activity, onStateChanged);
            } else {
                new AlertDialog.Builder(activity)
                        .setTitle("Ошибка")
                        .setMessage("huyina age ❌")
                        .setPositiveButton("Ок", null)
                        .show();
            }
        });
        AlertDialog dialog = builder.show();
        if (dialog != null) {
            AndroidUtilities.runOnUIThread(() -> {
                if (isActivityAlive(activity)) {
                    input.requestFocus();
                    AndroidUtilities.showKeyboard(input);
                }
            }, 120);
        }
    }

    private static boolean isAgePassed(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return false;
        }
        if ("1984".equals(raw) || "1488".equals(raw)) {
            return true;
        }
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (Exception e) {
            return false;
        }
        return value >= 16;
    }

    private static void showBaitScreen(Activity activity, @Nullable Runnable onStateChanged) {
        if (!isActivityAlive(activity)) {
            return;
        }
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(0xFFEEF2F3);

        LinearLayout flagBackground = new LinearLayout(activity);
        flagBackground.setOrientation(LinearLayout.VERTICAL);
        root.addView(flagBackground, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        // Uzbekistan flag stripes.
        View stripeBlue = new View(activity);
        stripeBlue.setBackgroundColor(0xFF00A6E9);
        flagBackground.addView(stripeBlue, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 320f));
        View stripeRedTop = new View(activity);
        stripeRedTop.setBackgroundColor(0xFFDC1F26);
        flagBackground.addView(stripeRedTop, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(6)));
        View stripeWhite = new View(activity);
        stripeWhite.setBackgroundColor(0xFFFFFFFF);
        flagBackground.addView(stripeWhite, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 320f));
        View stripeRedBottom = new View(activity);
        stripeRedBottom.setBackgroundColor(0xFFDC1F26);
        flagBackground.addView(stripeRedBottom, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(6)));
        View stripeGreen = new View(activity);
        stripeGreen.setBackgroundColor(0xFF1EB53A);
        flagBackground.addView(stripeGreen, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 320f));

        TextView uzMark = new TextView(activity);
        uzMark.setText("☪ ★★★★★★★★★★★★");
        uzMark.setTextColor(0xFFFFFFFF);
        uzMark.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        uzMark.setTypeface(AndroidUtilities.bold());
        uzMark.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(66), 0, 0);
        root.addView(uzMark, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

        TextView topWarning = new TextView(activity);
        topWarning.setText("ВАШ TELEGRAM МОЖЕТ БЫТЬ ЗАБЛОКИРОВАН!");
        topWarning.setTypeface(AndroidUtilities.bold());
        topWarning.setTextColor(0xFFFFFFFF);
        topWarning.setGravity(Gravity.CENTER);
        topWarning.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        topWarning.setBackgroundColor(0xE6008CCF);
        topWarning.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        root.addView(topWarning, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        FrameLayout center = new FrameLayout(activity);
        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        centerParams.topMargin = AndroidUtilities.dp(60);
        centerParams.bottomMargin = AndroidUtilities.dp(230);
        root.addView(center, centerParams);

        FrameLayout phone = new FrameLayout(activity);
        GradientDrawable phoneBg = new GradientDrawable();
        phoneBg.setColor(0xCC1B1D22);
        phoneBg.setCornerRadius(AndroidUtilities.dp(26));
        phone.setBackground(phoneBg);
        FrameLayout.LayoutParams phoneParams = new FrameLayout.LayoutParams(AndroidUtilities.dp(160), AndroidUtilities.dp(300), Gravity.CENTER);
        center.addView(phone, phoneParams);

        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.msg_policy);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        phone.addView(logo, new FrameLayout.LayoutParams(AndroidUtilities.dp(84), AndroidUtilities.dp(84), Gravity.CENTER));

        TextView bigCross = new TextView(activity);
        bigCross.setText("X");
        bigCross.setTextColor(0xD6FF2020);
        bigCross.setTypeface(AndroidUtilities.bold());
        bigCross.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 220);
        bigCross.setGravity(Gravity.CENTER);
        center.addView(bigCross, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        FrameLayout card = new FrameLayout(activity);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFFFDFDFD);
        cardBg.setCornerRadius(AndroidUtilities.dp(20));
        card.setBackground(cardBg);
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        cardParams.leftMargin = AndroidUtilities.dp(24);
        cardParams.rightMargin = AndroidUtilities.dp(24);
        cardParams.bottomMargin = AndroidUtilities.dp(30);
        root.addView(card, cardParams);

        View redHeader = new View(activity);
        redHeader.setBackgroundColor(0xFFE92020);
        FrameLayout.LayoutParams redHeaderParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56), Gravity.TOP);
        card.addView(redHeader, redHeaderParams);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        card.addView(content, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView alertEmoji = new TextView(activity);
        alertEmoji.setText("⚠");
        alertEmoji.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
        alertEmoji.setGravity(Gravity.CENTER);
        content.addView(alertEmoji, new LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView body = new TextView(activity);
        body.setText("ТОТАЛЬНАЯ ЧИСТКА АККАУНТОВ!\nНЕМЕДЛЕННО ПОДТВЕРДИТЕ,\nЧТО ВЫ ИЗ УЗБЕКИСТАНА");
        body.setTypeface(AndroidUtilities.bold());
        body.setTextColor(0xFF161616);
        body.setGravity(Gravity.CENTER);
        body.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        body.setLineSpacing(AndroidUtilities.dp(2), 1f);
        body.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(12));
        content.addView(body, new LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        TextView confirmButton = new TextView(activity);
        confirmButton.setText("Я УЗБЕК ✅");
        confirmButton.setGravity(Gravity.CENTER);
        confirmButton.setTypeface(AndroidUtilities.bold());
        confirmButton.setTextColor(Color.WHITE);
        confirmButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        confirmButton.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(10), AndroidUtilities.dp(18), AndroidUtilities.dp(10));
        confirmButton.setBackgroundColor(0xFF1F9D43);
        content.addView(confirmButton, new LinearLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        View redFlash = new View(activity);
        redFlash.setBackgroundColor(0x73FF0000);
        redFlash.setAlpha(0f);
        root.addView(redFlash, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        ObjectAnimator flashAnimator = ObjectAnimator.ofFloat(redFlash, View.ALPHA, 0.0f, 0.65f, 0.0f);
        flashAnimator.setDuration(350);
        flashAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        flashAnimator.setRepeatMode(ObjectAnimator.RESTART);
        flashAnimator.start();

        ObjectAnimator zoomAnimator = ObjectAnimator.ofPropertyValuesHolder(
                body,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f)
        );
        zoomAnimator.setDuration(560);
        zoomAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        zoomAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        zoomAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        zoomAnimator.start();

        confirmButton.setOnClickListener(v -> {
            flashAnimator.cancel();
            zoomAnimator.cancel();
            dialog.dismiss();
            showCheckingAndResult(activity, onStateChanged);
        });

        dialog.setOnDismissListener(d -> {
            flashAnimator.cancel();
            zoomAnimator.cancel();
        });
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        dialog.show();

        topWarning.setScaleX(0.96f);
        topWarning.setScaleY(0.96f);
        topWarning.animate().scaleX(1f).scaleY(1f).setDuration(260).start();
        card.setScaleX(0.92f);
        card.setScaleY(0.92f);
        card.setAlpha(0f);
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private static void showCheckingAndResult(Activity activity, @Nullable Runnable onStateChanged) {
        if (!isActivityAlive(activity)) {
            return;
        }
        AlertDialog progressDialog = new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.setMessage("Проверка...");
        progressDialog.show();

        AndroidUtilities.runOnUIThread(() -> {
            if (!isActivityAlive(activity)) {
                return;
            }
            progressDialog.dismiss();
            setVerified(true);
            if (onStateChanged != null) {
                onStateChanged.run();
            }
            new AlertDialog.Builder(activity)
                    .setTitle("Готово")
                    .setMessage("Мы успешно подтвердили, что вы узбек! ✅ 📱")
                    .setPositiveButton("Ок", null)
                    .show();
        }, 1300);
    }

    private static boolean isActivityAlive(@Nullable Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static SharedPreferences prefs() {
        return MessagesController.getGlobalMainSettings();
    }
}
