package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.browser.Browser;

import java.io.InputStream;
import java.net.URL;
import java.util.Random;

public class UzbekAdView extends FrameLayout {

    private ImageView imageView;
    private TextView titleView;
    private TextView textView;
    private TextView buttonView;
    private TextView closeButtonView;
    private int currentAd = 0;
    private LinearLayout container;

    private static final String[][] ADS = {
            {"https://cdn.lutit.xyz/r/c95fcec21d2f4db55ce3f1728971f901.png", "НОВЫЙ ПРОЦЕССОР ОТ UZBEK COMPANY", "ТОЛЬКО СЕЙЧАС ПО СКИДКЕ ВСЕГО 1337 СОМ !!!!!", "ПРОДАТЬ ПОЧКУ", "https://t.me/uzbekgram_client"},
            {"https://cdn.lutit.xyz/r/my/linus.png", "ВАС ПОСЛАЛ НАХУЙ ТОРВАЛЬДС!", "да", "ПОСЛАТЬ НАХУЙ В ОТВЕТ", ""},
            {"https://cdn.lutit.xyz/r/my/uzbekgram.jpg", "УСТАНОВИТЕ ОБНОВЛЕНИЕ УЗБЕКГРАМ", "PORN TV FEATURE NEW DA UZBEKISTAN ALOOOOOOOO", "СКАЧАТЬ МЕССЕНДЖЕР АЛЛАХА", "https://uzbekgram.lutit.xyz"},
            {"https://cdn.lutit.xyz/r/my/sepi.jpg", "ВАС ПРИГЛАСИЛИ В СЕКС ЧАТ СЕПИ", "ВЫ НЕ МОЖЕТЕ ОТКАЗАТЬСЯ \u274C ТАК КАК Я УЗБЕК", "принять", "https://t.me/thebesttexteditor"},
            {"https://cdn.lutit.xyz/r/my/photo_2026-03-25_18-09-09.jpg", "АЛЬКАФОН БИЗНЕС", "ОТКРОЙТЕ ВОЗМОЖНОСТИ СВОЕГО ПОТОЛКА НА ВСЕ 1000000000% ЗА 3000 СОМ", "ЗАПУСТИТЬ РАКЕТУ", "https://t.me/alkafon_bizness"}
    };

    public UzbekAdView(Context context, int type) {
        super(context);
        setVisibility(GONE);

        AndroidUtilities.runOnUIThread(() -> {
            setVisibility(VISIBLE);
            Random random = new Random();
            currentAd = random.nextInt(ADS.length);
            String[] ad = ADS[currentAd];

            container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            
            if (type == 0) { // Chat style
                container.setBackgroundColor(Color.parseColor("#44000000"));
                container.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
            } else if (type == 1) { // Banner style
                container.setBackgroundColor(Color.RED);
                container.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
            } else { // Fullscreen style
                setBackgroundColor(Color.parseColor("#88000000")); // Dimmed background
                container.setBackgroundColor(Color.RED);
                container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20));
            }

            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            container.addView(imageView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, type == 2 ? 250 : 150));

            titleView = new TextView(context);
            titleView.setTextSize(type == 2 ? 24 : 18);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setTextColor(Color.YELLOW);
            titleView.setText(ad[1]);
            container.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

            textView = new TextView(context);
            textView.setTextSize(type == 2 ? 20 : 16);
            textView.setTextColor(Color.WHITE);
            textView.setText(ad[2]);
            container.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

            buttonView = new TextView(context);
            buttonView.setTextSize(type == 2 ? 22 : 18);
            buttonView.setTypeface(AndroidUtilities.bold());
            buttonView.setTextColor(Color.WHITE);
            buttonView.setBackgroundColor(Color.BLUE);
            buttonView.setGravity(Gravity.CENTER);
            buttonView.setPadding(0, AndroidUtilities.dp(15), 0, AndroidUtilities.dp(15));
            buttonView.setText(ad[3]);
            container.addView(buttonView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 15, 0, 0));

            closeButtonView = new TextView(context);
            closeButtonView.setTextSize(18);
            closeButtonView.setTypeface(AndroidUtilities.bold());
            closeButtonView.setTextColor(Color.LTGRAY);
            closeButtonView.setBackgroundColor(Color.DKGRAY);
            closeButtonView.setGravity(Gravity.CENTER);
            closeButtonView.setPadding(0, AndroidUtilities.dp(15), 0, AndroidUtilities.dp(15));
            closeButtonView.setText("ЗАКРЫТЬ НАХУЙ (если сможешь)");
            container.addView(closeButtonView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 15, 0, 0));
            closeButtonView.setOnClickListener(v -> AndroidUtilities.removeFromParent(this));

            if (type == 2) {
                addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 20, 20, 20));
            } else {
                addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }

            buttonView.setOnClickListener(v -> {
                if (ad[4].length() > 0) {
                    Browser.openUrl(context, ad[4]);
                }
            });

            new Thread(() -> {
                try {
                    URL url = new URL(ad[0]);
                    InputStream in = url.openStream();
                    final Bitmap bmp = BitmapFactory.decodeStream(in);
                    AndroidUtilities.runOnUIThread(() -> imageView.setImageBitmap(bmp));
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }).start();

            startCrazyAnimations();
        }, 3000);
    }

    private void startCrazyAnimations() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(150); // EVEN FASTER
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        Random rand = new Random();
        animator.addUpdateListener(animation -> {
            if (container == null) return;
            float v = (float) animation.getAnimatedValue();
            
            // LSD scale & rotation
            container.setScaleX(0.8f + v * 0.4f);
            container.setScaleY(0.8f + v * 0.4f);
            container.setRotation(-15f + v * 30f);
            
            // Random translation (shaking)
            container.setTranslationX((rand.nextFloat() - 0.5f) * 50);
            container.setTranslationY((rand.nextFloat() - 0.5f) * 50);

            // LSD Colors
            container.setBackgroundColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
            titleView.setTextColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
            textView.setTextColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
            buttonView.setBackgroundColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
            closeButtonView.setBackgroundColor(Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
        });
        animator.start();
    }
}