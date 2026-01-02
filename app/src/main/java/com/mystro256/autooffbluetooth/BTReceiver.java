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

        // Check standard profiles
        int[] profiles = {BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.HEALTH};
        
        for (int profileId : profiles) {
            int state = adapter.getProfileConnectionState(profileId);
            
            // IMPROVEMENT 1: Also check if we are currently "CONNECTING"
            // This prevents cutting off the radio while the car is trying to handshake
            if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING) {
                isConnectedOrConnecting = true;
                break;
            }
        }

        if (isConnectedOrConnecting) {
            // IMPROVEMENT 2: Connection found! Cancel any pending shutdown command.
            handler.removeCallbacks(shutdownTask);
        } else {
            // IMPROVEMENT 3: No connection found. 
            // Don't turn off immediately. Wait 20 seconds (20000ms).
            // First, remove any existing callbacks to reset the timer (debounce)
            handler.removeCallbacks(shutdownTask);
            handler.postDelayed(shutdownTask, 20000);
        }
    }
}
