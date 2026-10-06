package com.mine.autooffbluetooth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public class WifiInactivityTimer {

    private static final String TAG = "WifiInactivityTimer";
    private static final String PREFS_NAME = "wifi_inactivity_prefs";
    private static final String PREF_WIFI_INACTIVITY_ENABLED = "wifi_inactivity_enabled";
    private static final String PREF_WIFI_INACTIVITY_TIME = "wifi_inactivity_time";
    private static final int DEFAULT_INACTIVITY_MINUTES = 20;
    private static final int MIN_INACTIVITY_MINUTES = 1;
    private static final int MAX_INACTIVITY_MINUTES = 720;

    private static WifiInactivityTimer instance;
    private final Context context;

    private WifiInactivityTimer(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized WifiInactivityTimer getInstance(Context context) {
        if (instance == null) {
            instance = new WifiInactivityTimer(context);
        }
        return instance;
    }

    public boolean isMasterEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(MainActivity.PREF_MASTER_SWITCH, true);
    }

    public void startTimer() {
        if (!isMasterEnabled()) {
            Log.d(TAG, "Master Switch is OFF. Blocking timer start.");
            return;
        }

        if (!isInactivityEnabled()) {
            Log.d(TAG, "Wi-Fi inactivity timer is disabled in settings. Skipping.");
            return;
        }

        cancelTimer();

        int minutes = getInactivityTime();
        long triggerAtMillis = System.currentTimeMillis() + (minutes * 60L * 1000L);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WifiInactivityAlarmReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, flags);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Log.d(TAG, "Scheduled Wi-Fi shutdown in " + minutes + " minutes.");
        }
    }

    public void cancelTimer() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WifiInactivityAlarmReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, flags);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelled existing Wi-Fi inactivity alarm.");
        }
    }

    public boolean isInactivityEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_WIFI_INACTIVITY_ENABLED, false);
    }

    public void setInactivityEnabled(boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_WIFI_INACTIVITY_ENABLED, enabled).apply();
        if (!enabled) cancelTimer();
    }

    public int getInactivityTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_WIFI_INACTIVITY_TIME, DEFAULT_INACTIVITY_MINUTES);
    }

    public boolean setInactivityTime(int minutes) {
        if (minutes < MIN_INACTIVITY_MINUTES || minutes > MAX_INACTIVITY_MINUTES) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_WIFI_INACTIVITY_TIME, minutes).apply();
        return true;
    }

    public static int getMinInactivityMinutes() { return MIN_INACTIVITY_MINUTES; }
    public static int getMaxInactivityMinutes() { return MAX_INACTIVITY_MINUTES; }
}
