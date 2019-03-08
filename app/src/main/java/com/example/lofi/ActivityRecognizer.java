package com.example.lofi;
import org.tensorflow.lite.Interpreter;


import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.lang.Math;

public class ActivityRecognizer {

    private static final String MODEL_PATH = "activity_recognizer_4.tflite";
    //private Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    protected Interpreter tflite;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();


    protected float[][] inputWindow;
    protected float[][] outputProbabilities;

    private final static int NUM_OUTPUTS = 4;
    private final static int NUM_SAMPLES_PER_WINDOW = 60;
    private final static int NUM_AXES = 3;

    private static final String INPUT_NODE = "input";
    private static final String[] OUTPUT_NODES = {"k2tfout_"};
    private static final String OUTPUT_NODE = "k2tfout_";

    public static int num_samples_in_this_window = 0;

    public ActivityRecognizer(Activity activity) {
        try {

            tfliteModel = loadModelFile(activity);
            tflite = new Interpreter(tfliteModel, tfliteOptions);

            inputWindow = new float[NUM_SAMPLES_PER_WINDOW][NUM_AXES];
            outputProbabilities = new float[1][NUM_OUTPUTS];
        }
        catch (IOException e) {
            Log.e("ARC", "Model failed to load");
        }
    }

    public void setInputWindow(float[][] standardizedVals) {
        /*
        Use after standardizing a window of data
         */
        this.inputWindow = standardizedVals;
        ActivityRecognizer.num_samples_in_this_window = 0;
    }

    public void setSample(float[][] vals, float x, float y, float z) {
        vals[num_samples_in_this_window][0] = x;
        vals[num_samples_in_this_window][1] = y;
        vals[num_samples_in_this_window][2] = z;
        ActivityRecognizer.num_samples_in_this_window++;
    }

    public float[][] getOutputProbabilities() {

        return outputProbabilities;
    }

    public float getMean(float[] vals) {
        int num = vals.length;
        int sum = 0;

        for (int i = 0; i < num; i++) {
            sum += vals[i];
        }

        return sum/num;
    }

    public float getStdDev(float[] vals, float mean) {
        int num = vals.length;
        float sum = 0.0f;
        float thisTerm;

        for (int i = 0; i < num; i++) {
            thisTerm = vals[i] - mean;
            sum += thisTerm*thisTerm;
        }

        return (float)Math.sqrt(sum / num);
    }

    public float[][] standardize(float[][] thisWindow) {
        float[] x_vals = new float[NUM_SAMPLES_PER_WINDOW];
        float[] y_vals = new float[NUM_SAMPLES_PER_WINDOW];
        float[] z_vals = new float[NUM_SAMPLES_PER_WINDOW];

        for (int i = 0; i < NUM_SAMPLES_PER_WINDOW; i++) {
            x_vals[i] = thisWindow[i][0];
            y_vals[i] = thisWindow[i][1];
            z_vals[i] = thisWindow[i][2];
        }

        float x_mean = getMean(x_vals);
        float y_mean = getMean(y_vals);
        float z_mean = getMean(z_vals);

        float x_stddev = getStdDev(x_vals, x_mean);
        float y_stddev = getStdDev(y_vals, y_mean);
        float z_stddev = getStdDev(z_vals, z_mean);

        for (int i = 0; i < NUM_SAMPLES_PER_WINDOW; i++) {
            x_vals[i] = (x_vals[i] - x_mean) / x_stddev;
            y_vals[i] = (y_vals[i] - y_mean) / y_stddev;
            z_vals[i] = (z_vals[i] - z_mean) / z_stddev;
        }

        float[][] output = new float[NUM_SAMPLES_PER_WINDOW][NUM_AXES];

        for (int i = 0; i < NUM_SAMPLES_PER_WINDOW; i++) {
            output[i][0] = x_vals[i];
            output[i][1] = y_vals[i];
            output[i][2] = x_vals[i];
        }

        return output;

    }


    protected void runInference() {

        tflite.run(this.inputWindow, this.outputProbabilities);
    }


    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }



}
