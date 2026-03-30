package com.mine.autooffbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class InactivityAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
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
