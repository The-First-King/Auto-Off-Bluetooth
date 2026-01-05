package com.mine.autooffbluetooth;

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
                // If nothing has reconnected in 20 seconds, disable Bluetooth.
                if (!isAnyDeviceConnected(adapter)) {
                    Log.d(TAG, "Confirmed: No devices. Turning off Bluetooth.");
                    try {
                        adapter.disable();
                    } catch (SecurityException e) {
                        Log.e(TAG, "Permission denied for disable()");
                    }
                } else {
                    Log.d(TAG, "Device reconnected during countdown. Aborting shutdown.");
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
                != PackageManager.PERMISSION_GRANTED) return;
        }

        Log.d(TAG, "Received Broadcast: " + action);

        // If a device connects, always stop the timer immediately.
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            Log.d(TAG, "ACL_CONNECTED: Stopping timer.");
            handler.removeCallbacks(shutdownTask);
            return;
        }

        // If a device disconnects.
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) || 
            BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
            
            Log.d(TAG, "ACL_DISCONNECTED: Triggering 20s countdown.");
            
            // We wait 2 seconds before checking "isAnyDeviceConnected" to let the stack finish the disconnection process.
            handler.postDelayed(() -> {
                if (!isAnyDeviceConnected(adapter)) {
                    handler.removeCallbacks(shutdownTask);
                    handler.postDelayed(shutdownTask, 20000);
                }
            }, 2000);
        }
    }

    private static boolean isAnyDeviceConnected(BluetoothAdapter adapter) {
        // Standard Profiles: 1=Headset, 2=A2DP, 7=GATT, 4=HID, 5=PAN.
        int[] profiles = {1, 2, 7, 4, 5};
        
        for (int profileId : profiles) {
            try {
                int state = adapter.getProfileConnectionState(profileId);
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "Active Profile Found: " + profileId);
                    return true;
                }
            } catch (Exception e) { }
        }

        // Bonded Reflection.
        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            if (bondedDevices != null) {
                for (BluetoothDevice device : bondedDevices) {
                    if (isConnectedReflection(device)) {
                        Log.d(TAG, "Active Device Found (Reflection): " + device.getName());
                        return true;
                    }
                }
            }
        } catch (SecurityException e) { }
        
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
