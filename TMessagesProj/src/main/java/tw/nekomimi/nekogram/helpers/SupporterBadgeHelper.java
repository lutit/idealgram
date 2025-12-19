package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class SupporterBadgeHelper {

    private static final String PREFS_NAME = "idealgram_supporters";
    private static final String PREFS_KEY_JSON = "supporters_json";
    private static final String DEFAULT_BACKEND_URL = "http://127.0.0.1:8000/api/v1/supporters";

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build();

    private static final Object lock = new Object();
    private static volatile boolean initialized;
    private static volatile boolean refreshInFlight;
    private static volatile Set<Long> supporterIds = Collections.emptySet();

    private SupporterBadgeHelper() {
    }

    public static void refreshFromServerAsync() {
        ensureInitialized();
        if (refreshInFlight) {
            return;
        }
        refreshInFlight = true;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                String url = DEFAULT_BACKEND_URL;
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("SupporterBadgeHelper: refreshing supporters from " + url);
                }

                Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

                try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        if (BuildVars.LOGS_ENABLED) {
                            FileLog.e("SupporterBadgeHelper: request failed, code=" + response.code());
                        }
                        return;
                    }

                    String body = response.body().string();
                    Set<Long> ids = parseSupporters(body);
                    if (ids.isEmpty()) {
                        if (BuildVars.LOGS_ENABLED) {
                            FileLog.e("SupporterBadgeHelper: empty supporters list, ignoring");
                        }
                        return;
                    }
                    persistAndSet(ids, body);
                    if (BuildVars.LOGS_ENABLED) {
                        FileLog.d("SupporterBadgeHelper: updated supporters, size=" + ids.size());
                    }
                }
            } catch (Throwable t) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.e("SupporterBadgeHelper: refresh failed");
                    FileLog.e(t);
                }
            } finally {
                refreshInFlight = false;
            }
        });
    }

    public static boolean hasBadge(long dialogId) {
        ensureInitialized();
        long normalizedId = dialogId;
        if (dialogId < 0) {
            normalizedId = -dialogId;
        }
        return supporterIds.contains(normalizedId);
    }

    public static SupporterBadgeDrawable newDrawable() {
        return new SupporterBadgeDrawable();
    }

    public static void showInfo(Context context) {
        if (context == null) {
            return;
        }
        Toast.makeText(context.getApplicationContext(), LocaleController.getString(R.string.SupporterBadgeToast), Toast.LENGTH_LONG).show();
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        if (ApplicationLoader.applicationContext == null) {
            if (supporterIds.isEmpty()) {
                supporterIds = Collections.unmodifiableSet(new HashSet<>(getFallbackSupporters()));
            }
            return;
        }
        synchronized (lock) {
            if (initialized) {
                return;
            }
            Set<Long> loaded = loadFromCache();
            if (loaded == null || loaded.isEmpty()) {
                loaded = new HashSet<>(getFallbackSupporters());
            }
            supporterIds = Collections.unmodifiableSet(loaded);
            initialized = true;
        }
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static Set<Long> loadFromCache() {
        try {
            String json = prefs().getString(PREFS_KEY_JSON, null);
            if (TextUtils.isEmpty(json)) {
                return null;
            }
            Set<Long> ids = parseSupporters(json);
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("SupporterBadgeHelper: loaded supporters from cache, size=" + ids.size());
            }
            return ids;
        } catch (Throwable t) {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.e("SupporterBadgeHelper: failed to load supporters cache");
                FileLog.e(t);
            }
            return null;
        }
    }

    private static void persistAndSet(Set<Long> ids, String json) {
        synchronized (lock) {
            supporterIds = Collections.unmodifiableSet(new HashSet<>(ids));
        }
        prefs().edit().putString(PREFS_KEY_JSON, json).apply();
    }

    private static Set<Long> parseSupporters(String json) throws Exception {
        HashSet<Long> ids = new HashSet<>();
        if (TextUtils.isEmpty(json)) {
            return ids;
        }
        String trimmed = json.trim();
        JSONArray array;
        if (trimmed.startsWith("[")) {
            array = new JSONArray(trimmed);
        } else {
            JSONObject object = new JSONObject(trimmed);
            array = object.optJSONArray("supporters");
            if (array == null) {
                array = object.optJSONArray("ids");
            }
            if (array == null) {
                return ids;
            }
        }
        for (int i = 0; i < array.length(); i++) {
            long value = array.optLong(i, 0);
            if (value > 0) {
                ids.add(value);
            }
        }
        return ids;
    }

    private static List<Long> getFallbackSupporters() {
        ArrayList<Long> ids = new ArrayList<>();
        ids.add(8592165157L);
        ids.add(7107188595L);
        ids.add(2975680882L);
        ids.add(1203243944L);
        ids.add(2400916702L);
        ids.add(8471562589L);
        ids.add(8304905410L);
        ids.add(6784215899L);
        ids.add(6352839357L);
        ids.add(387785790L);
        ids.add(2092916097L);
        return ids;
    }
}
