package org.reactnative.camera.events;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.util.Pools;

import com.facebook.common.util.Hex;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.reactnative.camera.CameraViewManager;
import org.reactnative.camera.utils.CommonUtil;
import org.reactnative.camera.utils.ImageDimensions;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.List;

import tflite.Detector;
import tflite.Recognitions;

public class ModelProcessedEvent extends Event<ModelProcessedEvent> {
    private static final Pools.SynchronizedPool<ModelProcessedEvent> EVENTS_POOL =
            new Pools.SynchronizedPool<>(3);

    private double mScaleX;
    private double mScaleY;
    //    private ByteBuffer mData;
//    private List<Detector.Recognition> recognitions;
    private Recognitions recognitions;
    private byte[] imageData;
    private Bitmap rgbImgBitmap;
    private ImageDimensions mImageDimensions;
    private String filepath;

    public static ModelProcessedEvent obtain(
            int viewTag,
            Recognitions data,
            String filepath,
            byte[] imageData,
            Bitmap rgbImgBitmap,
            ImageDimensions dimensions,
            double scaleX,
            double scaleY) {
        ModelProcessedEvent event = EVENTS_POOL.acquire();
        if (event == null) {
            event = new ModelProcessedEvent();
        }
        event.init(viewTag, data, filepath, imageData, rgbImgBitmap, dimensions, scaleX, scaleY);
        return event;
    }

    private void init(
            int viewTag,
            Recognitions data,
            String filepath,
            byte[] imageData,
            Bitmap rgbImgBitmap,
            ImageDimensions dimensions,
            double scaleX,
            double scaleY) {
        super.init(viewTag);
        recognitions = data;
        this.filepath = filepath;
        this.imageData = imageData;
        this.rgbImgBitmap = rgbImgBitmap;
        mImageDimensions = dimensions;
        mScaleX = scaleX;
        mScaleY = scaleY;
    }

    @Override
    public String getEventName() {
        return CameraViewManager.Events.EVENT_ON_MODEL_PROCESSED.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), createEvent());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private WritableMap createEvent() {

        WritableArray dataRecognition = Arguments.createArray();
        WritableMap event = Arguments.createMap();

        Log.d(CommonUtil.TAG, imageData.length + "");

        List<Detector.Recognition> detectorRecognitions = recognitions.getRecognitions();

        for (Detector.Recognition recognition : detectorRecognitions) {
            WritableMap recognitionEvent = Arguments.createMap();
            RectF location = recognition.getLocation();
            String title = recognition.getTitle();
            Float confidence = recognition.getConfidence();

            recognitionEvent.putString("title", title);
            recognitionEvent.putDouble("confidence", confidence);

            WritableMap locationMap = Arguments.createMap();
            locationMap.putDouble("top", location.top);
            locationMap.putDouble("bottom", location.bottom);
            locationMap.putDouble("left", location.left);
            locationMap.putDouble("right", location.right);
            recognitionEvent.putMap("location", locationMap);
            dataRecognition.pushMap(recognitionEvent);
        }

        event.putArray("recognitions", dataRecognition);
        event.putString("filepath", filepath);
        event.putString("imageDataResize", Base64.getEncoder().encodeToString(bitmapToArray(rgbImgBitmap)));
        event.putInt("processTime", (int) recognitions.getTimeTaken());
        return event;
    }

    public static byte[] bitmapToArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();
        return byteArray;
    }
}
