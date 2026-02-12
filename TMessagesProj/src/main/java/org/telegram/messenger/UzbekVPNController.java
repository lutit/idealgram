package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.tgnet.ConnectionsManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UzbekVPNController implements NotificationCenter.NotificationCenterDelegate {

    private static volatile UzbekVPNController Instance = null;

    public static UzbekVPNController getInstance() {
        UzbekVPNController localInstance = Instance;
        if (localInstance == null) {
            synchronized (UzbekVPNController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Log.d("UzbekVPN", "Creating new UzbekVPNController instance");
                    Instance = localInstance = new UzbekVPNController();
                }
            }
        }
        return localInstance;
    }

    private ArrayList<UzbekProxyInfo> proxies = new ArrayList<>();
    private volatile long lastFetchTime;
    private volatile boolean isFetching;
    public long connectionStartTime;
    private final Object sync = new Object();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class UzbekProxyInfo extends SharedConfig.ProxyInfo {
        public String country;
        public String flag;
        public String id;
        public String type; // "mtproto" or "socks5"

        public UzbekProxyInfo(String address, int port, String username, String password, String secret, String country, String flag, String id, String type) {
            super(address, port, username, password, secret);
            this.country = country;
            this.flag = flag;
            this.id = id;
            this.type = type;
        }
    }

    public UzbekVPNController() {
        Log.d("UzbekVPN", "UzbekVPNController constructor called");
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.didUpdateConnectionState);
        checkConnectionTime();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didUpdateConnectionState) {
            checkConnectionTime();
        }
    }

    private void checkConnectionTime() {
        int state = ConnectionsManager.getInstance(UserConfig.selectedAccount).getConnectionState();
        if (state == ConnectionsManager.ConnectionStateConnected) {
            if (connectionStartTime == 0) {
                connectionStartTime = System.currentTimeMillis();
            }
        } else {
            connectionStartTime = 0;
        }
    }

    public void start() {
        executor.execute(() -> {
            loadProxies();
            checkAndFetch();
        });
    }

    public void checkAndFetch() {
        executor.execute(() -> {
            Log.d("UzbekVPN", "checkAndFetch called. Aggressively fetching proxies.");
            fetchProxies();
        });
    }

    public void forceFetch() {
        Log.d("UzbekVPN", "forceFetch called");
        executor.execute(() -> fetchProxies(true));
    }

    private void fetchProxies() {
        fetchProxies(false);
    }

    private void fetchProxies(boolean force) {
        Log.d("UzbekVPN", "fetchProxies called. isFetching: " + isFetching + " force: " + force);
        if (isFetching && !force) return;
        isFetching = true;
        // Run on our own executor
        Log.d("UzbekVPN", "fetchProxies started on executor");

        try {
                URL url = new URL("https://cdn.lutit.xyz/uzbekgram/proxy.json");
                Log.d("UzbekVPN", "Connecting to " + url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000); // Increased timeout
                connection.setReadTimeout(10000);
                connection.setRequestMethod("GET");
                connection.setUseCaches(false); // Disable cache
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d("UzbekVPN", "Response code: " + responseCode);

                if (responseCode == 200) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    String json = sb.toString();
                    Log.d("UzbekVPN", "Fetched JSON (length: " + json.length() + ")");
                    parseAndSave(json);
                }
            } catch (Exception e) {
                Log.e("UzbekVPN", "Error fetching proxies", e);
                FileLog.e(e);
            } finally {
                isFetching = false;
                Log.d("UzbekVPN", "fetchProxies finished");
            }
    }

    private void parseAndSave(String json) {
        Log.d("UzbekVPN", "parseAndSave called");
        try {
            JSONObject root = new JSONObject(json);
            JSONObject servers = root.optJSONObject("servers");
            if (servers == null) {
                 Log.e("UzbekVPN", "No 'servers' object in JSON");
                 return;
            }
            
            ArrayList<UzbekProxyInfo> newProxies = new ArrayList<>();

            // MTProto
            JSONArray mtproto = servers.optJSONArray("mtproto");
            if (mtproto != null) {
                for (int i = 0; i < mtproto.length(); i++) {
                    JSONObject p = mtproto.getJSONObject(i);
                    String link = p.optString("tg_link");
                    SharedConfig.ProxyInfo info = null;
                    try {
                        info = SharedConfig.ProxyInfo.fromUrl(link);
                    } catch (Exception ignore) {}
                    
                    if (info != null) {
                        newProxies.add(new UzbekProxyInfo(
                                info.address, info.port, info.username, info.password, info.secret,
                                p.optString("country_code"), p.optString("flag"), p.optString("id"), "mtproto"
                        ));
                    }
                }
            }

            synchronized (sync) {
                proxies.clear();
                proxies.addAll(newProxies);
            }
            saveProxies();
            lastFetchTime = System.currentTimeMillis();
            saveConfig();

            Log.d("UzbekVPN", "Parsed " + newProxies.size() + " proxies. Saving...");
            AndroidUtilities.runOnUIThread(() -> {
                Log.d("UzbekVPN", "Posting NotificationCenter.proxySettingsChanged");
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
            });

            checkProxies();

        } catch (Exception e) {
            Log.e("UzbekVPN", "Error parsing proxies", e);
            FileLog.e(e);
        }
    }

    public ArrayList<UzbekProxyInfo> getProxies() {
        synchronized (sync) {
            return new ArrayList<>(proxies);
        }
    }
    
    public void checkProxies() {
        executor.execute(() -> {
            Log.d("UzbekVPN", "Starting checkProxies in background");
            ArrayList<UzbekProxyInfo> checkList;
            synchronized (sync) {
                checkList = new ArrayList<>(proxies);
            }
            for (UzbekProxyInfo info : checkList) {
                if (info.checking || SystemClock.elapsedRealtime() - info.availableCheckTime < (info.available ? 20 : 5) * 1000) {
                    continue;
                }
                info.checking = true;
                info.proxyCheckPingId = ConnectionsManager.getInstance(UserConfig.selectedAccount).checkProxy(info.address, info.port, info.username, info.password, info.secret, time -> AndroidUtilities.runOnUIThread(() -> {
                    info.availableCheckTime = SystemClock.elapsedRealtime();
                    info.checking = false;
                    if (time == -1) {
                        info.available = false;
                        info.ping = 0;
                    } else {
                        info.ping = time;
                        info.available = true;
                    }
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxyCheckDone, info);
                }));
            }
        });
    }

    public void enableProxy(UzbekProxyInfo info) {
        Log.d("UzbekVPN", "Enabling proxy: " + info.address);
        SharedConfig.setCurrentProxy(info);
        boolean enabled = true;
        ConnectionsManager.setProxySettings(enabled, info.address, info.port, info.username, info.password, info.secret);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    public void disableProxy() {
        Log.d("UzbekVPN", "Disabling proxy");
        SharedConfig.setProxyEnable(false);
        ConnectionsManager.setProxySettings(false, "", 0, "", "", "");
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }

    private void saveProxies() {
        try {
            JSONArray arr = new JSONArray();
            synchronized (sync) {
                for (UzbekProxyInfo info : proxies) {
                    JSONObject obj = new JSONObject();
                    obj.put("addr", info.address);
                    obj.put("port", info.port);
                    obj.put("user", info.username);
                    obj.put("pass", info.password);
                    obj.put("sec", info.secret);
                    obj.put("cc", info.country);
                    obj.put("flag", info.flag);
                    obj.put("id", info.id);
                    obj.put("type", info.type);
                    arr.put(obj);
                }
            }
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("uzbek_vpn_prefs", Context.MODE_PRIVATE);
            prefs.edit().putString("proxies", arr.toString()).apply();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private void loadProxies() {
        Log.d("UzbekVPN", "loadProxies started");
        try {
            if (ApplicationLoader.applicationContext == null) {
                Log.e("UzbekVPN", "ApplicationLoader.applicationContext is null!");
                return;
            }
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("uzbek_vpn_prefs", Context.MODE_PRIVATE);
            long localLastFetchTime = prefs.getLong("last_fetch", 0);
            String json = prefs.getString("proxies", null);
            ArrayList<UzbekProxyInfo> loadedProxies = new ArrayList<>();

            if (json != null) {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    loadedProxies.add(new UzbekProxyInfo(
                            obj.optString("addr"),
                            obj.optInt("port"),
                            obj.optString("user"),
                            obj.optString("pass"),
                            obj.optString("sec"),
                            obj.optString("cc"),
                            obj.optString("flag"),
                            obj.optString("id"),
                            obj.optString("type", "mtproto")
                    ));
                }
            }
            
            synchronized (sync) {
                proxies.clear();
                if (!loadedProxies.isEmpty()) {
                    proxies.addAll(loadedProxies);
                    lastFetchTime = localLastFetchTime;
                    Log.d("UzbekVPN", "Loaded " + proxies.size() + " proxies from cache");
                } else {
                    lastFetchTime = 0;
                    Log.d("UzbekVPN", "No proxies in cache or empty list");
                }
            }
        } catch (Throwable e) {
            Log.e("UzbekVPN", "Error loading proxies", e);
            FileLog.e(e);
            synchronized (sync) {
                proxies.clear();
                lastFetchTime = 0;
            }
        }
    }

    private void saveConfig() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("uzbek_vpn_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_fetch", lastFetchTime).apply();
    }
}