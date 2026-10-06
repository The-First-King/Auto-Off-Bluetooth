package com.mine.autooffbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class WifiStateReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isAppEnabled = prefs.getBoolean(MainActivity.PREF_MASTER_SWITCH, true);

        if (!isAppEnabled) {
            Log.d(TAG, "App logic is disabled via Master Switch. Ignoring broadcast.");
            return;
        }

        String action = intent.getAction();
        WifiInactivityTimer wifiInactivityTimer = WifiInactivityTimer.getInstance(context);

        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            if (isConnectedToAnySSID(context)) {
                wifiInactivityTimer.cancelTimer();
            } else {
                wifiInactivityTimer.startTimer();
            }
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            if (state == WifiManager.WIFI_STATE_DISABLED) {
                wifiInactivityTimer.cancelTimer();
            } else if (state == WifiManager.WIFI_STATE_ENABLED && !isConnectedToAnySSID(context)) {
                wifiInactivityTimer.startTimer();
            }
        }
    }

    /** True only when Wi-Fi is on and actively associated with an SSID. */
    public static boolean isConnectedToAnySSID(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) return false;

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null || wifiInfo.getNetworkId() == -1) return false;

        String ssid = wifiInfo.getSSID();
        return ssid != null && !ssid.isEmpty() && !WifiManager.UNKNOWN_SSID.equals(ssid);
    }
}
