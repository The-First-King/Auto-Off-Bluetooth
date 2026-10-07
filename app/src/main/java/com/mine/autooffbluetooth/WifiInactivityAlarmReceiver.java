package com.mine.autooffbluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
            
            // Execute Wi-Fi shutdown. If targeting Android 10+ (API 29), standard setWifiEnabled(false) is deprecated and fails for non-system apps.
            wifiManager.setWifiEnabled(false);
        }
    }
}
