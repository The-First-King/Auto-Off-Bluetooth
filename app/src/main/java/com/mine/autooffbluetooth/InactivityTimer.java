package com.mine.autooffbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class InactivityTimer {

    private static final String TAG = "InactivityTimer";
    private static final String PREFS_NAME = "inactivity_prefs";
    private static final String PREF_INACTIVITY_ENABLED = "inactivity_enabled";
    private static final String PREF_INACTIVITY_TIME = "inactivity_time";
    private static final int DEFAULT_INACTIVITY_MINUTES = 20;
    private static final int MIN_INACTIVITY_MINUTES = 1;
    private static final int MAX_INACTIVITY_MINUTES = 1440;

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static InactivityTimer instance;
    private final Context context;
    private boolean isRunning = false;

    private final Runnable inactivityShutdownTask = new Runnable() {
        @Override
        public void run() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                Log.d(TAG, "Inactivity timer expired. No device connected. Disabling Bluetooth.");
                try {
                    adapter.disable();
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied for disable(): " + e.getMessage());
                }
            }
            isRunning = false;
        }
    };

    private InactivityTimer(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized InactivityTimer getInstance(Context context) {
        if (instance == null) {
            instance = new InactivityTimer(context);
        }
        return instance;
    }

    public void startTimer() {
        if (!isInactivityEnabled()) {
            Log.d(TAG, "Inactivity timer is disabled.");
            return;
        }

        cancelTimer();

        int inactivityMinutes = getInactivityTime();
        long inactivityMillis = inactivityMinutes * 60L * 1000L;

        Log.d(TAG, "Starting inactivity timer for " + inactivityMinutes + " minutes (" + inactivityMillis + "ms).");
        handler.postDelayed(inactivityShutdownTask, inactivityMillis);
        isRunning = true;
    }

    public void cancelTimer() {
        if (isRunning) {
            Log.d(TAG, "Cancelling inactivity timer (device connected or timer disabled).");
            handler.removeCallbacks(inactivityShutdownTask);
            isRunning = false;
        }
    }

    public boolean isTimerRunning() {
        return isRunning;
    }

    public boolean isInactivityEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_INACTIVITY_ENABLED, false);
    }

    public void setInactivityEnabled(boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_INACTIVITY_ENABLED, enabled).apply();
        Log.d(TAG, "Inactivity feature set to: " + enabled);

        if (!enabled) {
            cancelTimer();
        }
    }

    public int getInactivityTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_INACTIVITY_TIME, DEFAULT_INACTIVITY_MINUTES);
    }

    public boolean setInactivityTime(int minutes) {
        if (minutes < MIN_INACTIVITY_MINUTES || minutes > MAX_INACTIVITY_MINUTES) {
            Log.e(TAG, "Invalid inactivity time: " + minutes + ". Must be between " + MIN_INACTIVITY_MINUTES + " and " + MAX_INACTIVITY_MINUTES);
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_INACTIVITY_TIME, minutes).apply();
        Log.d(TAG, "Inactivity time set to: " + minutes + " minutes");
        return true;
    }

    public static int getMinInactivityMinutes() {
        return MIN_INACTIVITY_MINUTES;
    }

    public static int getMaxInactivityMinutes() {
        return MAX_INACTIVITY_MINUTES;
    }

    public static int getDefaultInactivityMinutes() {
        return DEFAULT_INACTIVITY_MINUTES;
    }
}
