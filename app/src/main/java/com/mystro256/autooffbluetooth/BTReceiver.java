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
import androidx.core.content.ContextCompat;
import java.lang.reflect.Method;
import java.util.Set;

public class BTReceiver extends BroadcastReceiver {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final Runnable shutdownTask = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                try {
                    adapter.disable();
                } catch (SecurityException e) {
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

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            handler.removeCallbacks(shutdownTask);
            return;
        }

        if (isAnyDeviceConnected(context, adapter)) {
            handler.removeCallbacks(shutdownTask);
        } else {
            handler.removeCallbacks(shutdownTask);
            handler.postDelayed(shutdownTask, 20000);
        }
    }

    private static boolean isAnyDeviceConnected(Context context, BluetoothAdapter adapter) {
        // Standard Profiles
        int[] profiles = {BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.HEALTH};
        for (int profileId : profiles) {
            try {
                if (adapter.getProfileConnectionState(profileId) == BluetoothProfile.STATE_CONNECTED) {
                    return true;
                }
            } catch (SecurityException e) { return false; }
        }

        // Bonded Devices
        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            if (bondedDevices != null) {
                for (BluetoothDevice device : bondedDevices) {
                    if (isConnectedReflection(device)) return true;
                }
            }
        } catch (SecurityException e) { return false; }
        
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
