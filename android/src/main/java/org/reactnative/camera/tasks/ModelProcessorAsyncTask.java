package org.reactnative.camera.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.reactnative.camera.utils.CommonUtil;
import org.reactnative.frame.RNFrame;
import org.reactnative.frame.RNFrameFactory;
import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import tflite.Detector;

public class ModelProcessorAsyncTask extends AsyncTask<Void, Void, List<Detector.Recognition>> {
    private ModelProcessorAsyncTaskDelegate mDelegate;
    private int mModelMaxFreqms;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private byte[] imageData;
    private Detector detector;
    private Bitmap rgbFrameBitmap;

    public ModelProcessorAsyncTask(
            ModelProcessorAsyncTaskDelegate delegate,
            Bitmap bitmap,
            Detector detector,
            byte[] imageData,
            int modelMaxFreqms,
            int width,
            int height,
            int rotation
    ) {
        mDelegate = delegate;
        this.detector = detector;
        this.rgbFrameBitmap = bitmap;
        this.imageData = imageData;
        mModelMaxFreqms = modelMaxFreqms;
        mWidth = width;
        mHeight = height;
        mRotation = rotation;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected List<Detector.Recognition> doInBackground(Void... voids) {
        if (isCancelled() || mDelegate == null || detector == null) {
            return null;
        }
        long startTime = SystemClock.uptimeMillis();
        List<Detector.Recognition> recognitions = null;
        try {

            recognitions = detector.recognizeImage(rgbFrameBitmap);
            recognitions.sort((a, b) -> (int) (b.getConfidence() - a.getConfidence()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (mModelMaxFreqms > 0) {
                long endTime = SystemClock.uptimeMillis();
                long timeTaken = endTime - startTime;
                if (timeTaken < mModelMaxFreqms) {
                    TimeUnit.MILLISECONDS.sleep(mModelMaxFreqms - timeTaken);
                }
            }
        } catch (Exception e) {}
        return recognitions;
    }

    @Override
    protected void onPostExecute(List<Detector.Recognition> data) {
        super.onPostExecute(data);

        if (data != null) {
            Bitmap resized = Bitmap.createScaledBitmap(rgbFrameBitmap, 381, 514, true);
            mDelegate.onModelProcessed(data, imageData, resized, mWidth, mHeight, mRotation);
        }
        mDelegate.onModelProcessorTaskCompleted();
    }
}
