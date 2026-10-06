package com.mine.autooffbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class WifiInactivityAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiInactivityAlarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isAppEnabled = prefs.getBoolean(MainActivity.PREF_MASTER_SWITCH, true);

        if (!isAppEnabled) {
            Log.d(TAG, "Master switch is OFF. Ignoring alarm trigger.");
            return;
        }

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) return;

        if (WifiStateReceiver.isConnectedToAnySSID(context)) {
            Log.d(TAG, "Timeout reached but Wi-Fi is connected to an SSID. Aborting.");
            return;
        }

        Log.d(TAG, "Timeout reached with no SSID connection. Disabling Wi-Fi.");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Pre-Android 10: third-party apps can still toggle Wi-Fi directly.
            try {
                wifiManager.setWifiEnabled(false);
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to disable Wi-Fi: " + e.getMessage());
            }
        } else {
            // Android 10+: Google blocks WifiManager.setWifiEnabled() for third-party apps - root needed.
            if (!RootUtils.executeRootCommand("svc wifi disable")) {
                Log.e(TAG, "Failed to disable Wi-Fi via root.");
            }
        }
    }
}
