package com.mystro256.autooffbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class BTReceiver extends BroadcastReceiver {

    // A static handler ensures the timer persists across multiple broadcast triggers
    private static final Handler handler = new Handler(Looper.getMainLooper());
    
    // The "Runnable" defines the task to turn off Bluetooth
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

        // Safety check: if BT is already off or invalid, do nothing
        if (adapter == null || !adapter.isEnabled()) {
            return;
        }

        boolean isConnectedOrConnecting = false;

        // Add GATT (Low Energy) profiles to the check list.
        int[] profiles = {
            BluetoothProfile.A2DP, 
            BluetoothProfile.HEADSET, 
            BluetoothProfile.HEALTH,
            BluetoothProfile.GATT,       // Added for BLE devices (Watches)
            BluetoothProfile.GATT_SERVER // Added for BLE server role
        };
        
        for (int profileId : profiles) {
            // Note: getProfileConnectionState is deprecated in API 31+, 
            // but is the correct method for this approach on older/mixed APIs.
            int state = adapter.getProfileConnectionState(profileId);
            
            if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING) {
                isConnectedOrConnecting = true;
                break;
            }
        }

        if (isConnectedOrConnecting) {
            // Connection found! Cancel any pending shutdown command.
            handler.removeCallbacks(shutdownTask);
        } else {
            // No connection found. Wait 20 seconds.
            // First, remove any existing callbacks to reset the timer (debounce)
            handler.removeCallbacks(shutdownTask);
            handler.postDelayed(shutdownTask, 20000);
        }
    }
}
