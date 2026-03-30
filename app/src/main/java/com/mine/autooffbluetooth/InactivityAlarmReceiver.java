package com.mine.autooffbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class InactivityAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isAppEnabled = prefs.getBoolean(MainActivity.PREF_MASTER_SWITCH, true);
        
        if (!isAppEnabled) {
            Log.d("InactivityAlarm", "Master switch is OFF. Ignoring alarm trigger.");
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            if (!BTReceiver.isAnyDeviceConnected(adapter)) {
                Log.d("InactivityAlarm", "Timeout reached with no connections. Disabling Bluetooth.");
                try {
                    adapter.disable();
                } catch (SecurityException e) {
                    Log.e("InactivityAlarm", "Failed to disable BT: " + e.getMessage());
                }
            } else {
                Log.d("InactivityAlarm", "Timeout reached but device is connected. Aborting.");
            }
        }
    }
}
