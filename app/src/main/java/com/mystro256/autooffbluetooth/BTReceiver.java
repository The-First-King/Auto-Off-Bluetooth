package com.mystro256.autooffbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Method;
import java.util.Set;

public class BTReceiver extends BroadcastReceiver {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final Runnable shutdownTask = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                adapter.disable();
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null || !adapter.isEnabled()) {
            return;
        }

        boolean activeConnectionFound = false;

        // Check Classic Profiles (Audio/Headset)
        int[] profiles = {BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.HEALTH};
        for (int profileId : profiles) {
            int state = adapter.getProfileConnectionState(profileId);
            if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING) {
                activeConnectionFound = true;
                break;
            }
        }

        // Iterate Paired Devices to catch BLE
        if (!activeConnectionFound) {
            Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
            if (pairedDevices != null) {
                for (BluetoothDevice device : pairedDevices) {
                    if (isDeviceConnected(device)) {
                        activeConnectionFound = true;
                        break;
                    }
                }
            }
        }

        if (activeConnectionFound) {
            // Device found! Cancel shutdown.
            handler.removeCallbacks(shutdownTask);
        } else {
            // No active device found. Schedule shutdown in 20s.
            handler.removeCallbacks(shutdownTask);
            handler.postDelayed(shutdownTask, 20000);
        }
    }

    private boolean isDeviceConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected");
            return (boolean) m.invoke(device);
        } catch (Exception e) {
            return false;
        }
    }
}
