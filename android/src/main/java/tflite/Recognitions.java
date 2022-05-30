package tflite;

import java.util.List;

public class Recognitions {
    List<Detector.Recognition> recognitions;
    long timeTaken;

    public Recognitions() {}

    public Recognitions(List<Detector.Recognition> recognitions, long timeTaken) {
        this.recognitions = recognitions;
        this.timeTaken = timeTaken;
    }

    public List<Detector.Recognition> getRecognitions() {
        return recognitions;
    }

    public void setRecognitions(List<Detector.Recognition> recognitions) {
        this.recognitions = recognitions;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(long timeTaken) {
        this.timeTaken = timeTaken;
    }
}
