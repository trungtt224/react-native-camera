package org.reactnative.camera.tasks;

import java.nio.ByteBuffer;
import java.util.List;

import tflite.Detector;

public interface ModelProcessorAsyncTaskDelegate {
//    void onModelProcessed(ByteBuffer data, int sourceWidth, int sourceHeight, int sourceRotation);
    void onModelProcessed(List<Detector.Recognition> data, int sourceWidth, int sourceHeight, int sourceRotation);
    void onModelProcessorTaskCompleted();
}
