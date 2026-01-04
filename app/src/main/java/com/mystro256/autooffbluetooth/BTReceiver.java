package com.mystro256.autooffbluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.lang.reflect.Method;
import java.util.Set;

public class BTReceiver extends BroadcastReceiver {

    private static final String TAG = "BTReceiver";
    private static final Handler handler = new Handler(Looper.getMainLooper());

    // Task that actually turns off Bluetooth
    private static final Runnable shutdownTask = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                // Final double-check: ensure nothing connected in the last 20 seconds
                Log.d(TAG, "Countdown finished. Disabling Bluetooth...");
                try {
                    adapter.disable();
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException: Cannot disable BT. Ensure permission is granted.");
                }
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null || !adapter.isEnabled()) return;

        // Security Check for Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Missing BLUETOOTH_CONNECT permission. Cannot run logic.");
                return; 
            }
        }

        // Stop timer if Bluetooth is already being turned off manually
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
                handler.removeCallbacks(shutdownTask);
                return;
            }
        }

        Log.d(TAG, "Action received: " + action);

        // Check if any device is currently active
        if (isAnyDeviceConnected(context, adapter)) {
            Log.d(TAG, "Device(s) currently connected. Cancelling countdown.");
            handler.removeCallbacks(shutdownTask);
        } else {
            Log.d(TAG, "No active connections detected. Starting 20s countdown.");
            handler.removeCallbacks(shutdownTask);
            handler.postDelayed(shutdownTask, 20000);
        }
    }

    private static boolean isAnyDeviceConnected(Context context, BluetoothAdapter adapter) {
        // Check Standard Profiles
        int[] profiles = {
            BluetoothProfile.A2DP,      // 2: Standard Media Audio
            BluetoothProfile.HEADSET,   // 1: Phone Calls
            BluetoothProfile.GATT,      // 7: Data Sync (Watches/Apps)
            4,                          // HID_HOST: Keyboards/Headphone Buttons
            5                           // PAN: Personal Area Networking
        };
        
        for (int profileId : profiles) {
            try {
                int state = adapter.getProfileConnectionState(profileId);
                // If connected or connecting, consider a device present
                if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING) {
                    Log.d(TAG, "Connection active on profile ID: " + profileId);
                    return true;
                }
            } catch (Exception e) {
                // Profile not supported on this specific hardware
            }
        }

        // Check Bonded Devices via Reflection
        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            if (bondedDevices != null) {
                for (BluetoothDevice device : bondedDevices) {
                    if (isConnectedReflection(device)) {
                        Log.d(TAG, "Device connected (Reflection): " + device.getName());
                        return true;
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException checking bonded devices.");
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
