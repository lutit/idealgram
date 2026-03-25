package org.telegram.ui;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Chronometer;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;

public class AllahCallActivity extends Activity {

    private MediaPlayer mediaPlayer;
    private Thread audioThread;
    private volatile boolean isDestroyed = false;

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
        
        startAudioLoop();
    }

    private void startAudioLoop() {
        audioThread = new Thread(() -> {
            String[] urls = new String[]{
                "https://f003.backblazeb2.com/file/cdn-lutit/r/allah1.ogg",
                "https://f003.backblazeb2.com/file/cdn-lutit/r/allah2.ogg",
                "https://f003.backblazeb2.com/file/cdn-lutit/r/%D0%A3%D0%B7%D0%B1%D0%B5%D0%BA%D0%93%D1%80%D0%B0%D0%BC.mp3",
                "https://f003.backblazeb2.com/file/cdn-lutit/r/Flappy+bird+September+edition+Nasheed.mp3"
            };
            
            List<File> cachedFiles = new ArrayList<>();
            for (String urlStr : urls) {
                if (isDestroyed) return;
                try {
                    String fileName = Uri.parse(urlStr).getLastPathSegment();
                    File cacheFile = new File(getCacheDir(), fileName);
                    if (!cacheFile.exists()) {
                        URL url = new URL(urlStr);
                        URLConnection connection = url.openConnection();
                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(10000);
                        connection.connect();
                        InputStream in = connection.getInputStream();
                        FileOutputStream out = new FileOutputStream(cacheFile);
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            if (isDestroyed) {
                                in.close();
                                out.close();
                                cacheFile.delete();
                                return;
                            }
                            out.write(buffer, 0, read);
                        }
                        out.flush();
                        out.close();
                        in.close();
                    }
                    cachedFiles.add(cacheFile);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            
            if (cachedFiles.isEmpty() || isDestroyed) return;
            
            File firstFile = null;
            for (File f : cachedFiles) {
                if (f.getName().equals("allah1.ogg")) {
                    firstFile = f;
                    break;
                }
            }
            if (firstFile == null) firstFile = cachedFiles.get(0);
            
            playAudioSync(firstFile);
            
            Random random = new Random();
            while (!isDestroyed) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
                if (isDestroyed) break;
                
                File nextFile = cachedFiles.get(random.nextInt(cachedFiles.size()));
                playAudioSync(nextFile);
            }
        });
        audioThread.start();
    }
    
    private void playAudioSync(File file) {
        if (isDestroyed || file == null) return;
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            while (!isDestroyed && mediaPlayer != null && mediaPlayer.isPlaying()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.release();
                    mediaPlayer = null;
                } catch (Exception ex) {
                    FileLog.e(ex);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (audioThread != null) {
            audioThread.interrupt();
        }
    }
}