package xyz.nextalone.nagram.helper;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

/**
 * Helper class for showing the Nasheed popup before playing voice messages or audio files.
 * This is a joke feature for Uzbekgram - plays a nasheed for 15 seconds like a mobile game ad.
 */
public class NasheedHelper {

    private static final int NASHEED_DURATION_SECONDS = 15;
    private static MediaPlayer nasheedPlayer;
    private static CountDownTimer countDownTimer;
    private static AlertDialog currentDialog;
    private static boolean isShowing = false;

    /**
     * Shows the nasheed popup before playing audio.
     * After 15 seconds, user can skip and the original audio will play.
     *
     * @param activity The current activity
     * @param messageObject The message to play after nasheed
     * @param onComplete Callback to play the original audio
     */
    public static void showNasheedPopup(Activity activity, MessageObject messageObject, Runnable onComplete) {
        if (activity == null || activity.isFinishing() || isShowing) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        isShowing = true;

        // Create custom view for the dialog
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(
                AndroidUtilities.dp(24),
                AndroidUtilities.dp(16),
                AndroidUtilities.dp(24),
                AndroidUtilities.dp(8)
        );
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        // Message text
        TextView messageText = new TextView(activity);
        messageText.setText(LocaleController.getString("NasheedPopupMessage", R.string.NasheedPopupMessage));
        messageText.setTextSize(16);
        messageText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        messageText.setGravity(Gravity.CENTER);
        messageText.setPadding(0, 0, 0, AndroidUtilities.dp(20));
        container.addView(messageText);

        // Progress bar
        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(NASHEED_DURATION_SECONDS);
        progressBar.setProgress(0);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(8)
        );
        progressParams.bottomMargin = AndroidUtilities.dp(12);
        container.addView(progressBar, progressParams);

        // Timer text
        TextView timerText = new TextView(activity);
        timerText.setText("🎵 " + NASHEED_DURATION_SECONDS + "s");
        timerText.setTextSize(18);
        timerText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        timerText.setGravity(Gravity.CENTER);
        timerText.setPadding(0, 0, 0, AndroidUtilities.dp(16));
        container.addView(timerText);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.getString("NasheedPopupTitle", R.string.NasheedPopupTitle));
        builder.setView(container);

        // Create buttons (initially will be disabled)
        builder.setPositiveButton(LocaleController.getString("NasheedPopupYes", R.string.NasheedPopupYes), (dialog, which) -> {
            cleanup();
            if (onComplete != null) {
                onComplete.run();
            }
        });

        // Second button - hardcoded text (not translated)
        builder.setNegativeButton("porno net 🚫", (dialog, which) -> {
            cleanup();
            if (onComplete != null) {
                onComplete.run();
            }
        });

        currentDialog = builder.create();
        currentDialog.setCancelable(false);
        currentDialog.setCanceledOnTouchOutside(false);
        currentDialog.show();

        // Disable buttons initially
        if (currentDialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAlpha(0.5f);
        }
        if (currentDialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            currentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            currentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAlpha(0.5f);
        }

        // Start playing nasheed
        startNasheed(activity);

        // Start countdown timer
        countDownTimer = new CountDownTimer(NASHEED_DURATION_SECONDS * 1000L, 1000) {
            int secondsRemaining = NASHEED_DURATION_SECONDS;

            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining = (int) (millisUntilFinished / 1000);
                int progress = NASHEED_DURATION_SECONDS - secondsRemaining;
                
                AndroidUtilities.runOnUIThread(() -> {
                    progressBar.setProgress(progress);
                    timerText.setText("🎵 " + secondsRemaining + "s");
                });
            }

            @Override
            public void onFinish() {
                AndroidUtilities.runOnUIThread(() -> {
                    progressBar.setProgress(NASHEED_DURATION_SECONDS);
                    timerText.setText("🎵 ✅ Done!");

                    // Enable buttons
                    if (currentDialog != null && currentDialog.isShowing()) {
                        if (currentDialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                            currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            currentDialog.getButton(AlertDialog.BUTTON_POSITIVE).setAlpha(1f);
                        }
                        if (currentDialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                            currentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                            currentDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAlpha(1f);
                        }
                    }

                    // Stop nasheed
                    stopNasheed();
                });
            }
        };
        countDownTimer.start();
    }

    private static void startNasheed(Activity activity) {
        try {
            stopNasheed(); // Make sure previous player is stopped
            
            nasheedPlayer = MediaPlayer.create(activity, R.raw.nasheed);
            if (nasheedPlayer != null) {
                nasheedPlayer.setLooping(false);
                nasheedPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void stopNasheed() {
        try {
            if (nasheedPlayer != null) {
                if (nasheedPlayer.isPlaying()) {
                    nasheedPlayer.stop();
                }
                nasheedPlayer.release();
                nasheedPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cleanup() {
        isShowing = false;
        
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        
        stopNasheed();
        
        if (currentDialog != null) {
            try {
                currentDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
            currentDialog = null;
        }
    }

    /**
     * Check if the nasheed popup should be shown for this message.
     * Shows for voice messages and audio files.
     */
    public static boolean shouldShowNasheed(MessageObject messageObject) {
        if (messageObject == null) {
            return false;
        }
        return messageObject.isVoice() || messageObject.isMusic();
    }

    /**
     * Check if nasheed popup is currently showing
     */
    public static boolean isNasheedShowing() {
        return isShowing;
    }

    /**
     * Temporarily disable nasheed check for the next playMessage call.
     * Used after nasheed finishes to play the original audio.
     */
    private static boolean bypassNextCheck = false;

    public static void setBypassNextCheck(boolean bypass) {
        bypassNextCheck = bypass;
    }

    public static boolean shouldBypassCheck() {
        if (bypassNextCheck) {
            bypassNextCheck = false;
            return true;
        }
        return false;
    }
}