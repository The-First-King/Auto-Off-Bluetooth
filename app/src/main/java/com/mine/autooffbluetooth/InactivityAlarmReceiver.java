package com.mine.autooffbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class InactivityAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "DEBUG: Inactivity alarm FIRED", Toast.LENGTH_LONG).show();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isAppEnabled = prefs.getBoolean(MainActivity.PREF_MASTER_SWITCH, true);

        if (!isAppEnabled) {
            Log.d("InactivityAlarm", "Master switch is OFF. Ignoring alarm trigger.");
            Toast.makeText(context, "DEBUG: Master switch OFF, ignoring", Toast.LENGTH_LONG).show();
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(context, "DEBUG: adapter is null", Toast.LENGTH_LONG).show();
            return;
        }
        if (!adapter.isEnabled()) {
            Toast.makeText(context, "DEBUG: adapter.isEnabled()==false, nothing to do", Toast.LENGTH_LONG).show();
            return;
        }

        if (!BTReceiver.isAnyDeviceConnected(adapter)) {
            Log.d("InactivityAlarm", "Timeout reached with no connections. Disabling Bluetooth.");
            try {
                adapter.disable();
                Toast.makeText(context, "DEBUG: adapter.disable() called, no exception", Toast.LENGTH_LONG).show();
            } catch (SecurityException e) {
                Log.e("InactivityAlarm", "Failed to disable BT: " + e.getMessage());
                Toast.makeText(context, "DEBUG: disable() threw SecurityException: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d("InactivityAlarm", "Timeout reached but device is connected. Aborting.");
            Toast.makeText(context, "DEBUG: isAnyDeviceConnected()==true, aborting", Toast.LENGTH_LONG).show();
        }
    }
}
