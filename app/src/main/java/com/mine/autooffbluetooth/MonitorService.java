package com.mine.autooffbluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MonitorService extends Service {

    private static final String CHANNEL_ID = "monitor_service_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "MonitorService";

    private BTReceiver btReceiver;
    private BroadcastReceiver wifiStateReceiver;
    
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private boolean isWifiConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
        
        registerBluetoothReceiver();
        registerWifiNetworkCallback();
        registerWifiStateReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void registerBluetoothReceiver() {
        btReceiver = new BTReceiver();
        IntentFilter btFilter = new IntentFilter();
        btFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        btFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        btFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED");
        btFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btReceiver, btFilter);
    }

    private void registerWifiStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        wifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                WifiInactivityTimer timer = WifiInactivityTimer.getInstance(context);

                if (state == WifiManager.WIFI_STATE_DISABLED) {
                    timer.cancelTimer();
                    Log.d(TAG, "Wi-Fi radio turned OFF. Timer cancelled.");
                } else if (state == WifiManager.WIFI_STATE_ENABLED) {
                    if (!isWifiConnected) {
                        checkAndStartTimer();
                        Log.d(TAG, "Wi-Fi radio turned ON, but no network. Timer started.");
                    }
                }
            }
        };
        registerReceiver(wifiStateReceiver, filter);
    }

    private void registerWifiNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                isWifiConnected = true;
                WifiInactivityTimer.getInstance(MonitorService.this).cancelTimer();
                Log.d(TAG, "NetworkCallback: Connected to Wi-Fi AP. Timer cancelled.");
            }

            @Override
            public void onLost(@NonNull Network network) {
                isWifiConnected = false;
                Log.d(TAG, "NetworkCallback: Disconnected from Wi-Fi AP.");
                checkAndStartTimer();
            }
        };

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback);
            
            if (!isWifiConnectedToNetwork()) {
                checkAndStartTimer();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to register NetworkCallback", e);
        }
    }

    private boolean isWifiConnectedToNetwork() {
        if (connectivityManager == null) return false;
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities cap = connectivityManager.getNetworkCapabilities(network);
        return cap != null && cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private void checkAndStartTimer() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAppEnabled = prefs.getBoolean("master_switch_enabled", true);
        WifiInactivityTimer timer = WifiInactivityTimer.getInstance(this);

        if (!isAppEnabled || !timer.isInactivityEnabled()) return;

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        
        if (wm != null && wm.isWifiEnabled()) {
            timer.startTimer();
            Log.d(TAG, "Timer started: Device disconnected, but Wi-Fi radio is still ON.");
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Background monitoring", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps Bluetooth/Wi-Fi inactivity monitoring running");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Monitoring Bluetooth/Wi-Fi activity")
                .setSmallIcon(R.drawable.ic_bluetooth_status) 
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btReceiver != null) {
            unregisterReceiver(btReceiver);
        }
        if (wifiStateReceiver != null) {
            unregisterReceiver(wifiStateReceiver);
        }
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering callback", e);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
