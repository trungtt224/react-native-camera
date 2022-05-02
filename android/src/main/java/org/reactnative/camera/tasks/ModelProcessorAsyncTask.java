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

public class ModelProcessorAsyncTask extends AsyncTask<Void, Void, Detector.Recognition> {
    private byte[] mImageData;
    private ModelProcessorAsyncTaskDelegate mDelegate;
    private Interpreter mModelProcessor;
    private ByteBuffer mInputBuf;
    private ByteBuffer mOutputBuf;
    private int mModelMaxFreqms;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private Detector detector;
    private Bitmap croppedBitmap = null;
    private Bitmap rgbFrameBitmap = null;

    public ModelProcessorAsyncTask(Detector detector) {
        this.detector = detector;

    }

    public ModelProcessorAsyncTask(
            ModelProcessorAsyncTaskDelegate delegate,
            Interpreter modelProcessor,
            ByteBuffer inputBuf,
            ByteBuffer outputBuf,
            int modelMaxFreqms,
            int width,
            int height,
            int rotation
    ) {
        mDelegate = delegate;
        mModelProcessor = modelProcessor;
        mInputBuf = inputBuf;
        mOutputBuf = outputBuf;
        mModelMaxFreqms = modelMaxFreqms;
        mWidth = width;
        mHeight = height;
        mRotation = rotation;
    }

    public ModelProcessorAsyncTask(
            ModelProcessorAsyncTaskDelegate delegate,
            Bitmap bitmap,
            Detector detector,
            Interpreter modelProcessor,
            ByteBuffer inputBuf,
            ByteBuffer outputBuf,
            int modelMaxFreqms,
            int width,
            int height,
            int rotation
    ) {
        mDelegate = delegate;
        this.detector = detector;
        this.rgbFrameBitmap = bitmap;
        mModelProcessor = modelProcessor;
        mInputBuf = inputBuf;
        mOutputBuf = outputBuf;
        mModelMaxFreqms = modelMaxFreqms;
        mWidth = width;
        mHeight = height;
        mRotation = rotation;
    }

    public ModelProcessorAsyncTask(
            ModelProcessorAsyncTaskDelegate delegate,
            byte[] data,
            Detector detector,
            Interpreter modelProcessor,
            ByteBuffer inputBuf,
            ByteBuffer outputBuf,
            int modelMaxFreqms,
            int width,
            int height,
            int rotation
    ) {
        mDelegate = delegate;
        this.detector = detector;
        mImageData = data;
        mModelProcessor = modelProcessor;
        mInputBuf = inputBuf;
        mOutputBuf = outputBuf;
        mModelMaxFreqms = modelMaxFreqms;
        mWidth = width;
        mHeight = height;
        mRotation = rotation;
    }

    public ModelProcessorAsyncTask(
            ModelProcessorAsyncTaskDelegate delegate,
            Detector detector,
            Interpreter modelProcessor,
            ByteBuffer inputBuf,
            ByteBuffer outputBuf,
            int modelMaxFreqms,
            int width,
            int height,
            int rotation
    ) {
        this.detector = detector;
        mDelegate = delegate;
        mModelProcessor = modelProcessor;
        mInputBuf = inputBuf;
        mOutputBuf = outputBuf;
        mModelMaxFreqms = modelMaxFreqms;
        mWidth = width;
        mHeight = height;
        mRotation = rotation;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected Detector.Recognition doInBackground(Void... voids) {
        if (isCancelled() || mDelegate == null || mModelProcessor == null || detector == null) {
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
        if (recognitions != null && !recognitions.isEmpty()) {
            return recognitions.get(0);
        } else {
            return null;
        }
    }

    @Override
    protected void onPostExecute(Detector.Recognition data) {
        super.onPostExecute(data);

        if (data != null) {
            mDelegate.onModelProcessed(data, mWidth, mHeight, mRotation);
        }
        mDelegate.onModelProcessorTaskCompleted();
    }
}
