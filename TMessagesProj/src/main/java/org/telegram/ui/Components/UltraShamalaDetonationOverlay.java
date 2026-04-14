package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

public class UltraShamalaDetonationOverlay extends FrameLayout {

    private static final String DETONATION_URL = "https://f003.backblazeb2.com/file/cdn-lutit/r/ass2.html";
    private static final long DETONATION_DURATION_MS = 20_000L;

    private final WebView webView;
    private final TextView timerView;
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            long left = endUptimeMs - SystemClock.uptimeMillis();
            if (left <= 0) {
                stop();
                return;
            }
            int secondsLeft = (int) Math.ceil(left / 1000.0d);
            timerView.setText("ДО ДЕТОНАЦИИ: " + secondsLeft);
            AndroidUtilities.runOnUIThread(this, 100);
        }
    };

    private long endUptimeMs;
    private boolean running;

    public UltraShamalaDetonationOverlay(Context context) {
        super(context);
        setVisibility(GONE);
        setBackgroundColor(Color.BLACK);
        setClickable(true);
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        webView = new WebView(context);
        webView.setBackgroundColor(Color.BLACK);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        addView(webView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        timerView = new TextView(context);
        timerView.setTextColor(Color.RED);
        timerView.setTextSize(20);
        timerView.setTypeface(AndroidUtilities.bold());
        timerView.setGravity(Gravity.CENTER);
        timerView.setShadowLayer(dp(3), 0, 0, 0xFF000000);
        timerView.setPadding(dp(16), dp(18), dp(16), dp(8));
        addView(timerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
    }

    public void start() {
        if (running) {
            stop();
        }
        running = true;
        endUptimeMs = SystemClock.uptimeMillis() + DETONATION_DURATION_MS;
        timerView.setText("ДО ДЕТОНАЦИИ: 20");
        webView.loadUrl(DETONATION_URL);
        setVisibility(VISIBLE);
        bringToFront();
        AndroidUtilities.runOnUIThread(timerRunnable);
    }

    public void stop() {
        if (!running && getVisibility() == GONE) {
            return;
        }
        running = false;
        AndroidUtilities.cancelRunOnUIThread(timerRunnable);
        webView.stopLoading();
        webView.loadUrl("about:blank");
        setVisibility(GONE);
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        webView.destroy();
        super.onDetachedFromWindow();
    }
}
