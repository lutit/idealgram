package org.telegram.ui.Components;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.LaunchActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class HalalFmManager {

    private static HalalFmManager instance;
    private boolean isPlaying = false;
    private MediaPlayer mediaPlayer;
    private Thread audioThread;
    private List<File> audioFiles = new ArrayList<>();
    private volatile boolean isDestroyed = false;
    private HalalFmOverlay overlay;

    public static HalalFmManager getInstance() {
        if (instance == null) {
            instance = new HalalFmManager();
        }
        return instance;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void toggle(Context context) {
        if (isPlaying) {
            String currentTheme = org.telegram.messenger.MessagesController.getGlobalMainSettings().getString("theme", null);
            if ("Sex".equals(currentTheme)) {
                org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(context);
                builder.setTitle("ОШИБКА");
                builder.setMessage("В теме 'Sex' Uzbek FM (HALAL FM) выключить НЕВОЗМОЖНО! Слушай!");
                builder.setPositiveButton("Понял", null);
                builder.show();
                return;
            }
            stop();
            if (overlay != null) {
                AndroidUtilities.removeFromParent(overlay);
                overlay = null;
            }
        } else {
            startFlow(context);
        }
    }

    private void startFlow(Context context) {
        File cacheDir = new File(context.getCacheDir(), "halal_fm");
        File timestampFile = new File(cacheDir, "timestamp.txt");
        
        boolean needsDownload = true;
        if (cacheDir.exists() && timestampFile.exists()) {
            try {
                byte[] bytes = new byte[(int) timestampFile.length()];
                FileInputStream fis = new FileInputStream(timestampFile);
                fis.read(bytes);
                fis.close();
                long timestamp = Long.parseLong(new String(bytes));
                if (System.currentTimeMillis() - timestamp < 3L * 24L * 60L * 60L * 1000L) {
                    needsDownload = false;
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }

        if (needsDownload) {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Качаем HALAL FM, подождите братья...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(() -> {
                try {
                    if (cacheDir.exists()) {
                        deleteRecursive(cacheDir);
                    }
                    cacheDir.mkdirs();

                    File zipFile = new File(cacheDir, "fm.zip");
                    URL url = new URL("https://f003.backblazeb2.com/file/cdn-lutit/halal-fm/fm.zip");
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    InputStream in = new BufferedInputStream(url.openStream(), 8192);
                    FileOutputStream out = new FileOutputStream(zipFile);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    out.close();
                    in.close();

                    unzip(zipFile, cacheDir);
                    zipFile.delete();

                    FileOutputStream fos = new FileOutputStream(timestampFile);
                    fos.write(String.valueOf(System.currentTimeMillis()).getBytes());
                    fos.close();

                    AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                        playFromCache(context, cacheDir);
                    });

                } catch (Exception e) {
                    FileLog.e(e);
                    AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                    });
                }
            }).start();
        } else {
            playFromCache(context, cacheDir);
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void unzip(File zipFile, File targetDirectory) throws Exception {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            File file = new File(targetDirectory, ze.getName());
            File dir = ze.isDirectory() ? file : file.getParentFile();
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new Exception("Failed to ensure directory: " + dir.getAbsolutePath());
            }
            if (ze.isDirectory()) {
                continue;
            }
            FileOutputStream fout = new FileOutputStream(file);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = zis.read(buffer)) != -1) {
                fout.write(buffer, 0, count);
            }
            fout.close();
            zis.closeEntry();
        }
        zis.close();
    }

    private void playFromCache(Context context, File cacheDir) {
        audioFiles.clear();
        findAudioFiles(cacheDir);
        if (audioFiles.isEmpty()) return;

        Collections.shuffle(audioFiles);
        isPlaying = true;
        isDestroyed = false;

        if (context instanceof LaunchActivity) {
            LaunchActivity la = (LaunchActivity) context;
            if (overlay == null) {
                overlay = new HalalFmOverlay(context);
                la.getFrameLayout().addView(overlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }
        }

        audioThread = new Thread(() -> {
            int index = 0;
            while (!isDestroyed) {
                if (index >= audioFiles.size()) index = 0;
                playAudioSync(audioFiles.get(index));
                index++;
            }
        });
        audioThread.start();
        
        MessagesController.getGlobalMainSettings().edit().putBoolean("halal_fm_enabled", true).apply();
    }

    private void findAudioFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findAudioFiles(file);
                } else {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".m4a") || name.endsWith(".wav")) {
                        audioFiles.add(file);
                    }
                }
            }
        }
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

    public void stop() {
        isPlaying = false;
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
            audioThread = null;
        }
        MessagesController.getGlobalMainSettings().edit().putBoolean("halal_fm_enabled", false).apply();
    }
}