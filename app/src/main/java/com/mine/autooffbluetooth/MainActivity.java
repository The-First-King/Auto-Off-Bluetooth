package com.mine.autooffbluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private Switch inactivitySwitch;
    private EditText inactivityTimeInput;
    private InactivityTimer inactivityTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inactivityTimer = InactivityTimer.getInstance(this);
        initializeInactivityUI();
        checkAndRequestPermissions();
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
                    Toast.makeText(MainActivity.this, "Inactivity timer enabled", Toast.LENGTH_SHORT).show();
                    Log.d("MainActivity", "Inactivity timer feature ENABLED");
                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    if (adapter != null && adapter.isEnabled()) {
                        if (!BTReceiver.isAnyDeviceConnected(adapter)) {
                            Log.d("MainActivity", "BT is ON and disconnected. Starting timer immediately.");
                            inactivityTimer.startTimer();
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Inactivity timer disabled", Toast.LENGTH_SHORT).show();
                    inactivityTimer.cancelTimer();
                    Log.d("MainActivity", "Inactivity timer feature DISABLED");
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
                            if (!inactivityTimer.setInactivityTime(minutes)) {
                                Toast.makeText(MainActivity.this,
                                        "Value must be between " + InactivityTimer.getMinInactivityMinutes() +
                                                " and " + InactivityTimer.getMaxInactivityMinutes(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("MainActivity", "Inactivity time updated to " + minutes);
                                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                                if (inactivityTimer.isInactivityEnabled() && adapter != null && adapter.isEnabled()) {
                                    if (!BTReceiver.isAnyDeviceConnected(adapter)) {
                                        inactivityTimer.startTimer();
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            Log.e("MainActivity", "Invalid number format in input");
                        }
                    }
                }
            });
        }
    }

    public void disableBatteryOptimization(View view) {
        requestIgnoreBatteryOptimizations();
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
             if (ContextCompat.checkSelfPermission(this, "android.permission.SCHEDULE_EXACT_ALARM") != PackageManager.PERMISSION_GRANTED) {
             }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(this, "Battery optimization is already disabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions are required for background tasks.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
