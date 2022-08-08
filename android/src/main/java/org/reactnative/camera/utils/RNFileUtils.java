package org.reactnative.camera.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by jgfidelis on 23/01/18.
 */

public class RNFileUtils {

    public static String STORAGE_DIR = "Android/data/com.xsignvision/Files/Pictures";

    public static File ensureDirExists(File dir) throws IOException {
        if (!(dir.isDirectory() || dir.mkdirs())) {
            throw new IOException("Couldn't create directory '" + dir + "'");
        }
        return dir;
    }

    public static String getOutputFilePath(File directory, String extension) throws IOException {
        ensureDirExists(directory);
        String filename = UUID.randomUUID().toString();
        return directory + File.separator + filename + extension;
    }

    public static Uri uriFromFile(File file) {
        return Uri.fromFile(file);
    }

    public static void storeImage(Bitmap image, String filename) {
        File pictureFile = getOutputMediaFile(filename);
        if (pictureFile == null) {
            Log.d(CommonUtil.TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(CommonUtil.TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(CommonUtil.TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private static File getOutputMediaFile(String filename){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + File.separator + STORAGE_DIR);

        Log.d(CommonUtil.TAG, mediaStorageDir.getAbsolutePath());

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = System.currentTimeMillis() + "";
        File mediaFile;
        String mImageName= filename + ".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

}
