package com.mine.autooffbluetooth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class InactivityTimer {

    private static final String TAG = "InactivityTimer";
    private static final String PREFS_NAME = "inactivity_prefs";
    private static final String PREF_INACTIVITY_ENABLED = "inactivity_enabled";
    private static final String PREF_INACTIVITY_TIME = "inactivity_time";
    private static final String PREF_INACTIVITY_START_TIME = "inactivity_start_time";
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

    public boolean isMasterEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(MainActivity.PREF_MASTER_SWITCH, true);
    }

    /** Call on a genuine disconnect (or BT-on-with-nothing-connected) event. Resets the reference point that the countdown is measured from. */
    public void startTimer() {
        if (!isMasterEnabled()) {
            Log.d(TAG, "Master Switch is OFF. Blocking timer start.");
            return;
        }

        if (!isInactivityEnabled()) {
            Log.d(TAG, "Inactivity timer is disabled in settings. Skipping.");
            return;
        }

        long startTime = System.currentTimeMillis();
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(PREF_INACTIVITY_START_TIME, startTime).apply();

        scheduleFrom(startTime);
    }

    /**
     * Call when the configured duration changes while a disconnect window may
     * already be in progress. Keeps the existing disconnect reference time
     * (instead of restarting the countdown from now), so shortening the
     * duration past the elapsed disconnect time takes effect right away
     * instead of requiring a fresh full wait.
     */
    public void rescheduleTimer() {
        if (!isMasterEnabled() || !isInactivityEnabled()) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long startTime = prefs.getLong(PREF_INACTIVITY_START_TIME, System.currentTimeMillis());
        scheduleFrom(startTime);
    }

    private void scheduleFrom(long startTime) {
        cancelTimer(false);

        int minutes = getInactivityTime();
        long now = System.currentTimeMillis();
        long triggerAtMillis = startTime + (minutes * 60L * 1000L);
        if (triggerAtMillis <= now) {
            // Already overdue under the (possibly just-shortened) duration - fire almost immediately.
            triggerAtMillis = now + 1000L;
        }

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
            String when = new SimpleDateFormat("HH:mm:ss", Locale.US).format(triggerAtMillis);
            Log.d(TAG, "Scheduled Bluetooth shutdown at " + triggerAtMillis + " (" + minutes + "m window).");
            Toast.makeText(context, "DEBUG: BT shutoff alarm scheduled for " + when, Toast.LENGTH_LONG).show();
        }
    }

    public void cancelTimer() {
        cancelTimer(true);
    }

    private void cancelTimer(boolean clearStartTime) {
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

        if (clearStartTime) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().remove(PREF_INACTIVITY_START_TIME).apply();
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
