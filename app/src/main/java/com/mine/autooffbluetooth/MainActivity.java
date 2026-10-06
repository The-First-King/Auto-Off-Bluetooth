package com.mine.autooffbluetooth;

import android.Manifest;
import android.app.AlarmManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String PREF_MASTER_SWITCH = "master_switch_enabled";

    // Sequential permission flow
    private static final int STEP_RUNTIME_PERMS = 0;
    private static final int STEP_BATTERY = 1;
    private static final int REQ_RUNTIME_PERMS = 42;

    private int pendingPermissionStep = -1;

    private SwitchCompat masterSwitch;
    private Switch inactivitySwitch;
    private EditText inactivityTimeInput;
    private InactivityTimer inactivityTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inactivityTimer = InactivityTimer.getInstance(this);
        initializeMasterUI();
        initializeInactivityUI();

        // Ask for everything the app needs, one prompt at a time
        startPermissionStep(STEP_RUNTIME_PERMS);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Continue the permission flow if a step was waiting for the user to come back from a Settings screen
        if (pendingPermissionStep != -1) {
            int step = pendingPermissionStep;
            pendingPermissionStep = -1;
            startPermissionStep(step);
        }
    }

    private void initializeMasterUI() {
        masterSwitch = findViewById(R.id.master_switch);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (masterSwitch != null) {
            boolean isEnabled = prefs.getBoolean(PREF_MASTER_SWITCH, true);
            masterSwitch.setChecked(isEnabled);

            masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(PREF_MASTER_SWITCH, isChecked).apply();
                
                if (isChecked) {
                    Log.d("MainActivity", "App Logic ENABLED.");
                    refreshTimerIfNecessary();
                } else {
                    Log.d("MainActivity", "App Logic DISABLED. Cancelling timers.");
                    inactivityTimer.cancelTimer();
                }
            });
        }
    }

    private void initializeInactivityUI() {
        inactivitySwitch = findViewById(R.id.inactivitySwitch);
        inactivityTimeInput = findViewById(R.id.inactivityTimeInput);

        if (inactivitySwitch != null && inactivityTimeInput != null) {
            boolean isEnabled = inactivityTimer.isInactivityEnabled();
            int inactivityMinutes = inactivityTimer.getInactivityTime();
            
            inactivitySwitch.setChecked(isEnabled);
            inactivityTimeInput.setText(String.valueOf(inactivityMinutes));
            inactivityTimeInput.setEnabled(isEnabled);
            
            inactivitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                inactivityTimer.setInactivityEnabled(isChecked);
                inactivityTimeInput.setEnabled(isChecked);

                if (isChecked) {
                    Log.d("MainActivity", "Inactivity feature ENABLED.");
                    refreshTimerIfNecessary();
                } else {
                    Log.d("MainActivity", "Inactivity feature DISABLED.");
                    inactivityTimer.cancelTimer();
                }
            });

            inactivityTimeInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        try {
                            int minutes = Integer.parseInt(s.toString());
                            if (inactivityTimer.setInactivityTime(minutes)) {
                                Log.d("MainActivity", "Time updated to " + minutes + "m. Refreshing timer...");
                                refreshTimerIfNecessary();
                            } else {
                                Toast.makeText(MainActivity.this, 
                                    "Min: " + InactivityTimer.getMinInactivityMinutes() + " Max: " + InactivityTimer.getMaxInactivityMinutes(), 
                                    Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Log.e("MainActivity", "Invalid input");
                        }
                    }
                }
            });
        }
    }

    private void refreshTimerIfNecessary() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isAppEnabled = prefs.getBoolean(PREF_MASTER_SWITCH, true);
        
        if (!isAppEnabled) return;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (inactivityTimer.isInactivityEnabled() && adapter != null && adapter.isEnabled()) {
            if (!BTReceiver.isAnyDeviceConnected(adapter)) {
                inactivityTimer.startTimer();
                Log.d("MainActivity", "Timer (re)started successfully.");
            } else {
                Log.d("MainActivity", "Device connected; timer not started.");
            }
        }
    }

    private void startPermissionStep(int step) {
        switch (step) {
            case STEP_RUNTIME_PERMS: {
                List<String> needed = new ArrayList<>();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.BLUETOOTH_CONNECT);
                    }
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                            != PackageManager.PERMISSION_GRANTED) {
                        needed.add(Manifest.permission.BLUETOOTH_SCAN);
                    }
                }
                if (!needed.isEmpty()) {
                    ActivityCompat.requestPermissions(this, needed.toArray(new String[0]),
                            REQ_RUNTIME_PERMS);
                    return;
                }
                startPermissionStep(STEP_BATTERY);
                break;
            }

            case STEP_BATTERY: {
                ensureBatteryExemption();
                break;
            }
        }
    }

    private void continueAfterDialog(int nextStep) {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            startPermissionStep(nextStep);
        } else {
            pendingPermissionStep = nextStep;
        }
    }

    private void ensureBatteryExemption() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm == null || pm.isIgnoringBatteryOptimizations(getPackageName())) {
            return;
        }
        try {
            Intent intent = new Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Log.e("AutoOffBluetooth", "Battery exemption request failed", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RUNTIME_PERMS) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.BLUETOOTH_CONNECT.equals(permissions[i])) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this,
                                "Bluetooth permissions are needed for the app to work.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
            startPermissionStep(STEP_BATTERY);
        }
    }
}
