package com.mystro256.autooffbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Set;

public class BTReceiver extends BroadcastReceiver {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final Runnable shutdownTask = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            // Final check right before turning off.
            if (adapter != null && adapter.isEnabled() && !isAnyDeviceConnected(adapter)) {
                adapter.disable();
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null || !adapter.isEnabled()) {
            return;
        }

        // If connected to something, CANCEL the timer immediately.
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            handler.removeCallbacks(shutdownTask);
            return;
        }

        // If disconnected, or something else happened, check if ANYTHING is still alive.
        if (isAnyDeviceConnected(adapter)) {
            handler.removeCallbacks(shutdownTask);
        } else {
            // Nothing found. Start the countdown.
            handler.removeCallbacks(shutdownTask);
            handler.postDelayed(shutdownTask, 20000); // 20 seconds
        }
    }

    // Helper to check connections.
    private static boolean isAnyDeviceConnected(BluetoothAdapter adapter) {
        // Check Standard Profiles (Headsets, Audio).
        int[] profiles = {BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.HEALTH};
        for (int profileId : profiles) {
            if (adapter.getProfileConnectionState(profileId) == BluetoothProfile.STATE_CONNECTED) {
                return true;
            }
        }

        // Check Bonded Devices. This catches devices that don't report standard profiles.
        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if (bondedDevices != null) {
            for (BluetoothDevice device : bondedDevices) {
                if (isConnectedReflection(device)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isConnectedReflection(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected");
            return (boolean) m.invoke(device);
        } catch (Exception e) {
            return false;
        }
    }
}
