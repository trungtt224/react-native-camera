package org.reactnative.camera.tasks;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;
import java.util.List;

import tflite.Detector;
import tflite.Recognitions;

public interface ModelProcessorAsyncTaskDelegate {
    //    void onModelProcessed(ByteBuffer data, int sourceWidth, int sourceHeight, int sourceRotation);
    void onModelProcessed(Recognitions recognitions, byte[] imageData, Bitmap rgbImgBitmap, int sourceWidth, int sourceHeight, int sourceRotation);
    void onModelProcessorTaskCompleted();
}
