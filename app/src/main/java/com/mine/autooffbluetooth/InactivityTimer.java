package com.mine.autooffbluetooth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class InactivityTimer {

    private static final String TAG = "InactivityTimer";
    private static final String PREFS_NAME = "inactivity_prefs";
    private static final String PREF_INACTIVITY_ENABLED = "inactivity_enabled";
    private static final String PREF_INACTIVITY_TIME = "inactivity_time";
    private static final int DEFAULT_INACTIVITY_MINUTES = 20;
    private static final int MIN_INACTIVITY_MINUTES = 1;
    private static final int MAX_INACTIVITY_MINUTES = 1440;

    private static InactivityTimer instance;
    private final Context context;

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
            Log.d(TAG, "Inactivity timer is disabled in settings. Skipping.");
            return;
        }

        cancelTimer();

        int minutes = getInactivityTime();
        long triggerAtMillis = System.currentTimeMillis() + (minutes * 60L * 1000L);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, InactivityAlarmReceiver.class);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Log.d(TAG, "Scheduled Bluetooth shutdown in " + minutes + " minutes.");
        }
    }

    public void cancelTimer() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, InactivityAlarmReceiver.class);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelled existing inactivity alarm.");
        }
    }

    public boolean isInactivityEnabled() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_INACTIVITY_ENABLED, false);
    }

    public void setInactivityEnabled(boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_INACTIVITY_ENABLED, enabled).apply();
        if (!enabled) cancelTimer();
    }

    public int getInactivityTime() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_INACTIVITY_TIME, DEFAULT_INACTIVITY_MINUTES);
    }

    public boolean setInactivityTime(int minutes) {
        if (minutes < MIN_INACTIVITY_MINUTES || minutes > MAX_INACTIVITY_MINUTES) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(PREF_INACTIVITY_TIME, minutes).apply();
        return true;
    }

    public static int getMinInactivityMinutes() { return MIN_INACTIVITY_MINUTES; }
    public static int getMaxInactivityMinutes() { return MAX_INACTIVITY_MINUTES; }
}
