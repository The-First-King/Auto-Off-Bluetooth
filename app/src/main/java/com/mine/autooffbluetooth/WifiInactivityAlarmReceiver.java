package com.mine.autooffbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class WifiInactivityAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiInactivityAlarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Wi-Fi inactivity alarm triggered.");

        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        // Verify if the device is still authenticated.
        boolean isHardwareConnected = wifiInfo != null
                && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED
                && wifiInfo.getNetworkId() != -1;

        if (isHardwareConnected) {
            Log.d(TAG, "Device is still authenticated. Aborting Wi-Fi shutdown.");
            return;
        }

        Log.d(TAG, "Device disconnected. Attempting to disable Wi-Fi.");

        boolean apiAccepted = wifiManager.setWifiEnabled(false);
        if (apiAccepted) {
            Log.d(TAG, "Wi-Fi disable requested via WifiManager.");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean rootModeEnabled = prefs.getBoolean(MainActivity.PREF_WIFI_ROOT_MODE, false);

        if (!rootModeEnabled) {
            Log.w(TAG, "WifiManager rejected the request; root mode is not enabled.");
            return;
        }

        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"su", "-c", "svc wifi disable"});
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                Log.d(TAG, "Wi-Fi disabled via root shell command.");
            } else {
                Log.e(TAG, "Root Wi-Fi command failed with exit code " + exitCode + ".");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable Wi-Fi via root.", e);
        }
    }
}
