package com.mine.autooffbluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Keeps BTReceiver alive as a DYNAMICALLY registered receiver.
 * Manages Wi-Fi state monitoring via NetworkCallback for Doze-mode resistance.
 */
public class MonitorService extends Service {

    private static final String CHANNEL_ID = "monitor_service_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "MonitorService";

    private BTReceiver btReceiver;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
        registerBluetoothReceiver();
        registerWifiNetworkCallback();
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

    private void registerWifiNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Wi-Fi Network Available. Cancelling timer.");
                WifiInactivityTimer.getInstance(MonitorService.this).cancelTimer();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d(TAG, "Wi-Fi Network Lost. Checking preferences to start timer.");

                // Validate against your MainActivity's SharedPreferences setup
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MonitorService.this);
                boolean isAppEnabled = prefs.getBoolean("master_switch_enabled", true);
                WifiInactivityTimer wifiTimer = WifiInactivityTimer.getInstance(MonitorService.this);

                if (isAppEnabled && wifiTimer.isInactivityEnabled()) {
                    wifiTimer.startTimer();
                    Log.d(TAG, "Conditions met. Wi-Fi inactivity timer started.");
                } else {
                    Log.d(TAG, "Master switch or Wi-Fi timer is disabled. Timer not started.");
                }
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);
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
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
