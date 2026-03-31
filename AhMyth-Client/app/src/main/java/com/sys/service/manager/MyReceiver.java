package com.sys.service.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {
    public MyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            "android.intent.action.QUICKBOOT_POWERON".equals(action) || 
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "android.intent.action.REBOOT".equals(action)) {
            
            Intent serviceIntent = new Intent(context, MainService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(serviceIntent);
                } catch (Exception e) {
                    // Prevent crash on Android 12+ if background start is blocked
                    e.printStackTrace();
                }
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
