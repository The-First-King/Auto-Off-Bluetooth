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

    private static final Runnable shutdownTask = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                Log.d(TAG, "Countdown finished. Turning off Bluetooth...");
                try {
                    adapter.disable();
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied while disabling BT");
                }
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null || !adapter.isEnabled()) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                return; 
            }
        }

        // Handle Bluetooth manually turned off elsewhere
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
                handler.removeCallbacks(shutdownTask);
                return;
            }
        }

        Log.d(TAG, "Action received: " + action);

        // Logic check: if anything connected.
        if (isAnyDeviceConnected(context, adapter)) {
            Log.d(TAG, "Device detected. Cancelling/Preventing shutdown.");
            handler.removeCallbacks(shutdownTask);
        } else {
            Log.d(TAG, "No devices detected. Starting 20s countdown.");
            handler.removeCallbacks(shutdownTask);
            handler.postDelayed(shutdownTask, 20000);
        }
    }

    private static boolean isAnyDeviceConnected(Context context, BluetoothAdapter adapter) {
        // Check Standard Profiles (Audio, Input, Data)
        int[] profiles = {
            BluetoothProfile.A2DP,      // Standard Audio
            BluetoothProfile.HEADSET,   // Voice Calls
            BluetoothProfile.GATT,      // Low Energy Data (Watches/App-linked headphones)
            BluetoothProfile.HID_HOST   // Input (Keyboards/Media buttons)
        };
        
        for (int profileId : profiles) {
            try {
                int state = adapter.getProfileConnectionState(profileId);
                // If connected OR in the middle of connecting, consider it active
                if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING) {
                    Log.d(TAG, "Active connection found in profile: " + profileId);
                    return true;
                }
            } catch (Exception e) {
                // Profile not supported on this device, ignore
            }
        }

        // Check Bonded Devices via Reflection
        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            if (bondedDevices != null) {
                for (BluetoothDevice device : bondedDevices) {
                    if (isConnectedReflection(device)) {
                        Log.d(TAG, "Active connection detected on: " + device.getName());
                        return true;
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission error during Bonded Device check");
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
