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
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Keeps BTReceiver alive as a DYNAMICALLY registered receiver.
 * Manages Wi-Fi state monitoring via BroadcastReceiver for Doze-mode resistance.
 */
public class MonitorService extends Service {

    private static final String CHANNEL_ID = "monitor_service_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "MonitorService";

    private BTReceiver btReceiver;
    private BroadcastReceiver wifiStateReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, buildNotification());
        registerBluetoothReceiver();
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
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION); 

        wifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean isAppEnabled = prefs.getBoolean("master_switch_enabled", true);
                
                if (!isAppEnabled) {
                    Log.d(TAG, "Master switch is disabled. Ignoring Wi-Fi broadcast.");
                    return;
                }

                WifiInactivityTimer timer = WifiInactivityTimer.getInstance(context);
                if (!timer.isInactivityEnabled()) {
                    Log.d(TAG, "Wi-Fi timer is disabled in settings. Ignoring broadcast.");
                    return;
                }

                WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm == null) return;

                if (!wm.isWifiEnabled()) {
                    timer.cancelTimer();
                    Log.d(TAG, "Wi-Fi radio is OFF. Timer cancelled.");
                    return;
                }

                if (isConnectedToAnySSID(wm)) {
                    timer.cancelTimer();
                    Log.d(TAG, "Wi-Fi is connected to an SSID. Timer cancelled.");
                } else {
                    timer.startTimer();
                    Log.d(TAG, "Wi-Fi is ON but disconnected from SSID. Timer started.");
                }
            }
        };
        registerReceiver(wifiStateReceiver, filter);
    }

    private boolean isConnectedToAnySSID(WifiManager wm) {
        WifiInfo wifiInfo = wm.getConnectionInfo();
        if (wifiInfo == null) return false;

        if (wifiInfo.getNetworkId() == -1) return false;

        return wifiInfo.getSupplicantState() == SupplicantState.COMPLETED;
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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
