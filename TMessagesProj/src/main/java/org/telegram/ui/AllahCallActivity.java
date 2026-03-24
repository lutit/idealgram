package org.telegram.ui;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Chronometer;
import android.widget.ImageButton;

import org.telegram.messenger.R;

public class AllahCallActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allah_call);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFF0A8AD4);
        }

        Chronometer timerView = findViewById(R.id.allah_call_timer);
        timerView.setBase(SystemClock.elapsedRealtime() - 1000L);
        timerView.start();

        ImageButton endCallButton = findViewById(R.id.allah_call_end_button);
        endCallButton.setOnClickListener(v -> finish());
    }
}
