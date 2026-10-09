package com.mine.autooffbluetooth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

public class WifiInactivityTimer {
    private static WifiInactivityTimer instance;
    private final Context context;
    private final SharedPreferences prefs;

    private static final int MIN_MINUTES = 1;
    private static final int MAX_MINUTES = 720;

    private WifiInactivityTimer(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }

    public static synchronized WifiInactivityTimer getInstance(Context context) {
        if (instance == null) {
            instance = new WifiInactivityTimer(context);
        }
        return instance;
    }

    public void startTimer() {
        if (!isInactivityEnabled()) return;

        int minutes = getInactivityTime();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WifiInactivityAlarmReceiver.class);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        long triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000L);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public void cancelTimer() {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WifiInactivityAlarmReceiver.class);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public boolean isInactivityEnabled() {
        return prefs.getBoolean("wifi_inactivity_enabled", false);
    }

    public void setInactivityEnabled(boolean enabled) {
        prefs.edit().putBoolean("wifi_inactivity_enabled", enabled).apply();
    }

    public int getInactivityTime() {
        return prefs.getInt("wifi_inactivity_time", 20);
    }

    public boolean setInactivityTime(int minutes) {
        if (minutes >= getMinInactivityMinutes() && minutes <= getMaxInactivityMinutes()) {
            prefs.edit().putInt("wifi_inactivity_time", minutes).apply();
            return true;
        }
        return false;
    }

    public static int getMinInactivityMinutes() {
        return MIN_MINUTES;
    }

    public static int getMaxInactivityMinutes() {
        return MAX_MINUTES;
    }
}
