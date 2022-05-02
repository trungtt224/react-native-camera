package org.reactnative.camera.events;

import android.graphics.RectF;

import androidx.core.util.Pools;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.reactnative.camera.CameraViewManager;
import org.reactnative.camera.utils.ImageDimensions;

import java.nio.ByteBuffer;

import tflite.Detector;

public class ModelProcessedEvent extends Event<ModelProcessedEvent> {
    private static final Pools.SynchronizedPool<ModelProcessedEvent> EVENTS_POOL =
            new Pools.SynchronizedPool<>(3);

    private double mScaleX;
    private double mScaleY;
//    private ByteBuffer mData;
    private Detector.Recognition mData;
    private ImageDimensions mImageDimensions;


    public static ModelProcessedEvent obtain(
            int viewTag,
            Detector.Recognition data,
            ImageDimensions dimensions,
            double scaleX,
            double scaleY) {
        ModelProcessedEvent event = EVENTS_POOL.acquire();
        if (event == null) {
            event = new ModelProcessedEvent();
        }
        event.init(viewTag, data, dimensions, scaleX, scaleY);
        return event;
    }

    private void init(
            int viewTag,
            Detector.Recognition data,
            ImageDimensions dimensions,
            double scaleX,
            double scaleY) {
        super.init(viewTag);
        mData = data;
        mImageDimensions = dimensions;
        mScaleX = scaleX;
        mScaleY = scaleY;
    }

    @Override
    public String getEventName() {
        return CameraViewManager.Events.EVENT_ON_MODEL_PROCESSED.toString();
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), createEvent());
    }

//    private WritableMap createEvent() {
//        mData.rewind();
//        byte[] byteArray = new byte[mData.capacity()];
//        mData.get(byteArray);
//        WritableArray dataList = Arguments.createArray();
//        for (byte b : byteArray) {
//            dataList.pushInt((int)b);
//        }
//
//        WritableMap event = Arguments.createMap();
//        event.putString("type", "textBlock");
//        event.putArray("data", dataList);
//        event.putInt("target", getViewTag());
//        return event;
//    }

    private WritableMap createEvent() {
        WritableMap event = Arguments.createMap();
        if (mData != null) {
            RectF location = mData.getLocation();
            String title = mData.getTitle();
            Float confidence = mData.getConfidence();
            event.putBoolean("detected", true);
            event.putString("title", title);
            event.putDouble("confidence", confidence);
            WritableMap locationMap = Arguments.createMap();
            locationMap.putDouble("top", location.top);
            locationMap.putDouble("bottom", location.bottom);
            locationMap.putDouble("left", location.left);
            locationMap.putDouble("right", location.right);
            event.putMap("location", locationMap);
        } else {
            event.putBoolean("detected", false);
        }

        return event;
    }
}
