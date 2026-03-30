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
    private static final Runnable deviceDisconnectShutdownTask = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled() && !isAnyDeviceConnected(adapter)) {
                Log.d(TAG, "20s disconnect window expired. Turning off Bluetooth.");
                try { adapter.disable(); } catch (SecurityException e) {}
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return;

        InactivityTimer inactivityTimer = InactivityTimer.getInstance(context);

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothAdapter.STATE_ON) {
                inactivityTimer.startTimer();
            } else if (state == BluetoothAdapter.STATE_OFF) {
                inactivityTimer.cancelTimer();
                handler.removeCallbacks(deviceDisconnectShutdownTask);
            }
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            inactivityTimer.cancelTimer();
            handler.removeCallbacks(deviceDisconnectShutdownTask);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            inactivityTimer.startTimer();
            handler.postDelayed(deviceDisconnectShutdownTask, 20000);
        }
    }

    public static boolean isAnyDeviceConnected(BluetoothAdapter adapter) {
        int[] profiles = {1, 2, 7, 4, 5}; // Headset, A2DP, GATT, HID, PAN
        for (int profileId : profiles) {
            try {
                if (adapter.getProfileConnectionState(profileId) == BluetoothProfile.STATE_CONNECTED) return true;
            } catch (Exception e) { }
        }
        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            if (bondedDevices != null) {
                for (BluetoothDevice device : bondedDevices) {
                    if (isConnectedReflection(device)) return true;
                }
            }
        } catch (SecurityException e) { }
        return false;
    }

    private static boolean isConnectedReflection(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected");
            return (boolean) m.invoke(device);
        } catch (Exception e) { return false; }
    }
}
