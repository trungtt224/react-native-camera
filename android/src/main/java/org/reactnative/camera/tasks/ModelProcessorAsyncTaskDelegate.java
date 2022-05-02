package org.reactnative.camera.tasks;

import java.nio.ByteBuffer;

import tflite.Detector;

public interface ModelProcessorAsyncTaskDelegate {
//    void onModelProcessed(ByteBuffer data, int sourceWidth, int sourceHeight, int sourceRotation);
    void onModelProcessed(Detector.Recognition data, int sourceWidth, int sourceHeight, int sourceRotation);
    void onModelProcessorTaskCompleted();
}
