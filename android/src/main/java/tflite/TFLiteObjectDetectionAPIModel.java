package tflite;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Environment;
import android.os.Trace;
import android.util.Log;
import android.view.TextureView;


import com.google.android.gms.common.internal.service.Common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.reactnative.camera.utils.CommonUtil;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * github.com/tensorflow/models/tree/master/research/object_detection
 */
public class TFLiteObjectDetectionAPIModel implements Detector {

    // Only return this many results.
    private static final int NUM_DETECTIONS = 10;
    // Float model
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    // Config values.
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    private ByteBuffer imgData;

    private Interpreter tfLite;

    private MappedByteBuffer tfLiteModel;
    private Interpreter.Options tfLiteOptions;

    private TFLiteObjectDetectionAPIModel() {}

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param modelFilename The model file path relative to the assets folder
     * @param labelFilename The label file path relative to the assets folder
     * @param inputSize The size of image input
     * @param isQuantized Boolean representing model is quantized or not
     */
    public static Detector create(
            final Context context,
            final String modelFilename,
            final String labelFilename,
            final int inputSize,
            final boolean isQuantized)
            throws IOException {
        final TFLiteObjectDetectionAPIModel d = new TFLiteObjectDetectionAPIModel();

        try {
            InputStream labelsInput = context.getAssets().open(labelFilename);
            Log.d(CommonUtil.TAG, labelFilename);
            BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
            String line;
            while ((line = br.readLine()) != null) {
                d.labels.add(line);
            }
        } catch (Exception ex) {
            Log.d(CommonUtil.TAG, "TFLITE: " + ex.getMessage());
        }
        d.inputSize = inputSize;

        MappedByteBuffer modelFile = loadModelFile(context.getAssets(), modelFilename);
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(NUM_THREADS);
            options.setUseXNNPACK(true);
            d.tfLite = new Interpreter(modelFile, options);
            d.tfLiteModel = modelFile;
            d.tfLiteOptions = options;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];
        return d;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap, final int cameraViewWidth, final int cameraViewHeight) {
        // Log this method so that it can be analyzed with systrace.
        Log.d("recognizeImage",
                String.format("bitmap size: width=%d, height=%d", bitmap.getWidth(), bitmap.getHeight()));

        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                        .build();
        TensorImage tensorImage = new TensorImage(DataType.UINT8);
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        Log.d(CommonUtil.TAG,
                String.format("recognizeImage: image tensor size: width=%d, height=%d",
                        tensorImage.getWidth(), tensorImage.getHeight()));
        Log.d(CommonUtil.TAG,
                String.format("recognizeImage: image tensor's bitmap size: width=%d, height=%d",
                        tensorImage.getBitmap().getWidth(), tensorImage.getBitmap().getHeight()));

        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");

//        storeImage(bitmap);
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        try {
            int width = tensorImage.getWidth();
            int height = tensorImage.getHeight();

            tensorImage.getBitmap().getPixels(intValues, 0, width, 0, 0, width, height);
//            bitmap.getPixels(intValues, 0, width, 0, 0, width, height);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Trace.endSection(); // preprocessBitmap

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");
        outputLocations = new float[1][NUM_DETECTIONS][4];
        outputClasses = new float[1][NUM_DETECTIONS];
        outputScores = new float[1][NUM_DETECTIONS];
        numDetections = new float[1];

        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(1, outputLocations);
        outputMap.put(3, outputClasses);
        outputMap.put(0, outputScores);
        outputMap.put(2, numDetections);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Trace.endSection();

        // Show the best detections.
        // after scaling them back to the input size.
        final ArrayList<Recognition> recognitions = new ArrayList<>(NUM_DETECTIONS);
        for (int i = 0; i < NUM_DETECTIONS; ++i) {
//            int originalWidth = bitmap.getWidth();
//            int originalHeight = bitmap.getHeight();

            Log.d(CommonUtil.TAG, "recognizeImage: " + cameraViewWidth + " x " + cameraViewHeight);
            int ymin = (int) Math.max(1, outputLocations[0][i][0] * cameraViewHeight);
            int xmin = (int) Math.max(1, outputLocations[0][i][1] * cameraViewWidth);
            int ymax = (int) Math.min(cameraViewHeight, outputLocations[0][i][2] * cameraViewHeight);
            int xmax = (int) Math.min(cameraViewWidth, outputLocations[0][i][3] * cameraViewWidth);

            final RectF detection =
                    new RectF(xmin, ymin, xmax, ymax);
            Log.d(CommonUtil.TAG, String.format("Detection: top [%s] - left [%s] - bottom [%s] - right [%s]",
                    detection.top, detection.left, detection.bottom, detection.right));

//            final RectF detection =
//                    new RectF(
//                            outputLocations[0][i][1],
//                            outputLocations[0][i][0],
//                            outputLocations[0][i][3],
//                            outputLocations[0][i][2]);

            // SSD Mobilenet V1 Model assumes class 0 is background class
            // in label file and class labels start from 1 to number_of_classes+1,
            // while outputClasses correspond to class index from 0 to number_of_classes
            int labelOffset = 0;
            recognitions.add(
                    new Recognition(
                            "" + i,
                            labels.get((int) outputClasses[0][i] + labelOffset),
                            outputScores[0][i],
                            detection));
        }
        Trace.endSection(); // "recognizeImage"
        return recognitions;
    }

    @Override
    public void enableStatLogging(final boolean logStats) {}

    @Override
    public String getStatString() {
        return "";
    }

    @Override
    public void close() {}

    @Override
    public void setNumThreads(int numThreads) {
        if (tfLite != null) {
            tfLiteOptions.setNumThreads(numThreads);
            recreateInterpreter();
        }
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        if (tfLite != null) {
            tfLiteOptions.setUseNNAPI(isChecked);
            recreateInterpreter();
        }
    }

    private void recreateInterpreter() {
        tfLite.close();
        tfLite = new Interpreter(tfLiteModel, tfLiteOptions);
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(CommonUtil.TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(CommonUtil.TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(CommonUtil.TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Download");

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
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }
}
