package org.reactnative.camera.events;

import androidx.core.util.Pools;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.reactnative.camera.CameraViewManager;
import org.reactnative.camera.utils.ImageDimensions;

import java.nio.ByteBuffer;

public class ModelProcessedEvent extends Event<ModelProcessedEvent> {
    private static final Pools.SynchronizedPool<ModelProcessedEvent> EVENTS_POOL =
            new Pools.SynchronizedPool<>(3);

    private double mScaleX;
    private double mScaleY;
    private ByteBuffer mData;
    private ImageDimensions mImageDimensions;


    public static ModelProcessedEvent obtain(
            int viewTag,
            ByteBuffer data,
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
            ByteBuffer data,
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

    private WritableMap createEvent() {
        WritableMap event = Arguments.createMap();
        event.putString("type", "textBlock");
        event.putInt("target", getViewTag());
        return event;
    }
}
