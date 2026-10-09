package com.mine.autooffbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

public class WifiInactivityAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiInactivityAlarm";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Wifi Inactivity Alarm triggered.");

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            
            // Verify if the hardware is still authenticated
            boolean isHardwareConnected = wifiInfo != null 
                    && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED 
                    && wifiInfo.getNetworkId() != -1;

            if (isHardwareConnected) {
                Log.d(TAG, "Device is still authenticated to router (likely in Doze state). Aborting Wi-Fi shutdown.");
                return;
            }

            Log.d(TAG, "Device genuinely disconnected. Disabling Wi-Fi.");
            
            // Execute Wi-Fi shutdown.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) Root needed to control Wi-Fi
                try {
                    Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "svc wifi disable"});
                    process.waitFor();
                    Log.d(TAG, "Wi-Fi disabled via ROOT shell command.");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to disable Wi-Fi via ROOT. Ensure app has SU permissions.", e);
                }
            } else {
                // Android 9- using API
                wifiManager.setWifiEnabled(false);
                Log.d(TAG, "Wi-Fi disabled via standard API.");
            }
        }
    }
}
