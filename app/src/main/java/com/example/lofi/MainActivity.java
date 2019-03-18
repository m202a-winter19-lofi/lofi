package com.example.lofi;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.tensorflow.lite.Interpreter;


import android.content.res.AssetManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.lang.Math;
import android.view.View.OnClickListener;

import java.net.*;

public class MainActivity extends AppCompatActivity{

    private SensorManager sensorManager;
    private Sensor sensor;

    private SensorManager sensorManagerStep;
    private Sensor sensorStep;
    private long lastStepTime = 0;

    private final int SAMPLE_RATE_US = 50000; // Sample every 50ms for 20Hz

    private final int STRIDES_PER_CALC = 16;
    private double[] strides = new double[STRIDES_PER_CALC];
    // make effective stride rate average of last STRIDES_PER_CALC stride rates
    private int numStrides = 0; // increment on each new step
    private double effectiveStrideRate = 0.0;

    // Fetch from http site cherrypy
    public String site_url = "http://192.168.43.194:8080/"; //"http://172.16.17.49:8080/";
    //"http://192.168.43.194:8080/";//"http://169.254.14.4:8080/"; //
    public String valFrom_retrieved_VA_tv;

    // Key points for quadrant-based approximation
    public int kp_rows = 5;
    public int kp_cols = 5;
    public float[][][] key_points = new float[kp_rows][kp_cols][2]; // num rows by num cols by V,A

    public float v_begin = -1.5f;
    public float a_begin = 1.5f;
    public float va_increment = 0.75f; // how much to change each adjacent element by

    // "Global" VA and button adjustment values
    public float v_global;
    public float a_global;
    public float t_global;
    public float v_adjust = 0.05f;
    public float a_adjust = 0.05f;
    public float t_adjust = 2.0f;

    // Populate key_points array
    public void populateKeyPoints() {
        for (int i = 0; i < kp_rows; i++) {
            for (int j = 0; j < kp_cols; j++) {
                key_points[i][j][0] = v_begin + j*va_increment;
                key_points[i][j][1] = a_begin - i*va_increment;
                //Log.e("VA", Float.toString(key_points[i][j][0]) + " " + Float.toString(key_points[i][j][1]));
            }
        }
    }

    public double getMean(double[] vals) {
        int num = vals.length;
        double sum = 0;

        for (int i = 0; i < num; i++) {
            sum += vals[i];
        }

        return sum/num;
    }

    public void zeroStrideArray() {
        for (int i = 0; i < STRIDES_PER_CALC; i++) {
            strides[i] = 0.0;
        }
    }

    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                activityRecognizer.setSample(inputWindow, event.values[0], event.values[1], event.values[2]);

                if (activityRecognizer.num_samples_in_this_window == NUM_SAMPLES_PER_WINDOW) {
                    float[][] standardizedWindow = activityRecognizer.standardize(inputWindow);

                    activityRecognizer.setInputWindow(standardizedWindow);

                    //Log.i("DEBUG", "About to run inference");
                    activityRecognizer.runInference();
                    //Log.i("DEBUG", "Ran inference, getting probabilities");
                    outputProbabilities = activityRecognizer.getOutputProbabilities();
                    //Log.i("DEBUG", "Got probabilities, displaying");

                    TextView activity1p_tv = findViewById(R.id.activity1p);
                    TextView activity2p_tv = findViewById(R.id.activity2p);
                    TextView activity3p_tv = findViewById(R.id.activity3p);
                    TextView activity4p_tv = findViewById(R.id.activity4p);
                    activity1p_tv.setText(String.format("%.2f", outputProbabilities[0][0]));
                    activity2p_tv.setText(String.format("%.2f", outputProbabilities[0][1]));
                    activity3p_tv.setText(String.format("%.2f", outputProbabilities[0][2]));
                    activity4p_tv.setText(String.format("%.2f", outputProbabilities[0][3]));

                    if (outputProbabilities[0][0] >= outputProbabilities[0][1] &&
                            outputProbabilities[0][0] >= outputProbabilities[0][2] &&
                            outputProbabilities[0][0] >= outputProbabilities[0][3]) {
                        activity1p_tv.setTextColor(Color.RED);
                        activity2p_tv.setTextColor(Color.BLACK);
                        activity3p_tv.setTextColor(Color.BLACK);
                        activity4p_tv.setTextColor(Color.BLACK);
                        stateMachine.update(0);

                    } else if (outputProbabilities[0][1] >= outputProbabilities[0][0] &&
                            outputProbabilities[0][1] >= outputProbabilities[0][2] &&
                            outputProbabilities[0][1] >= outputProbabilities[0][3]) {
                        activity1p_tv.setTextColor(Color.BLACK);
                        activity2p_tv.setTextColor(Color.RED);
                        activity3p_tv.setTextColor(Color.BLACK);
                        activity4p_tv.setTextColor(Color.BLACK);
                        stateMachine.update(1);

                    } else if (outputProbabilities[0][2] >= outputProbabilities[0][0] &&
                            outputProbabilities[0][2] >= outputProbabilities[0][1] &&
                            outputProbabilities[0][2] >= outputProbabilities[0][3]) {
                        activity1p_tv.setTextColor(Color.BLACK);
                        activity2p_tv.setTextColor(Color.BLACK);
                        activity3p_tv.setTextColor(Color.RED);
                        activity4p_tv.setTextColor(Color.BLACK);
                        stateMachine.update(1);

                    } else {
                        activity1p_tv.setTextColor(Color.BLACK);
                        activity2p_tv.setTextColor(Color.BLACK);
                        activity3p_tv.setTextColor(Color.BLACK);
                        activity4p_tv.setTextColor(Color.RED);
                        stateMachine.update(0);

                    }

                    TextView activity_state = findViewById(R.id.activity_state);

                    if (stateMachine.state == 0) {
                        activity_state.setText("non-active");
                    }

                    else {
                        activity_state.setText("active");
                    }

                }
            }

            else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                long timestamp = event.timestamp;
                long delta = timestamp - lastStepTime;
                double strideRate = 0.0;

                if (lastStepTime == 0) {
                    lastStepTime = timestamp;
                }
                else {
                    strideRate = 60.0 / (delta / 1000000000.0);
                    lastStepTime = timestamp;
                }

                TextView stride_tv = findViewById(R.id.stride);
                String toStride_tv = "Running stride rate: " + String.format("%.2f", strideRate) + " s/m";
                stride_tv.setText(toStride_tv);

                strides[numStrides++] = strideRate;

                if (numStrides == STRIDES_PER_CALC) {
                    effectiveStrideRate = getMean(strides);
                    TextView eff_stride_tv = findViewById(R.id.eff_stride);
                    String toEff_stride_tv = "Effective stride rate: " + String.format("%.2f", effectiveStrideRate) + " s/m";
                    eff_stride_tv.setText(toEff_stride_tv);
                    numStrides = 0;
                }

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            /* TODO:
            Implement?
             */
        }
    };
    private final static int NUM_OUTPUTS = 4;
    private final static int NUM_SAMPLES_PER_WINDOW = 60;
    private final static int NUM_AXES = 3;

    private float[][] inputWindow = new float[NUM_SAMPLES_PER_WINDOW][NUM_AXES];
    private float[][] outputProbabilities = new float[1][NUM_OUTPUTS];

    private ActivityRecognizer activityRecognizer;
    private MusicRecommender musicRecommender;
    private StateMachine stateMachine;

    private final static int K = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        populateKeyPoints();

        activityRecognizer = new ActivityRecognizer(this);

        TextView activity1_tv = findViewById(R.id.activity1);
        activity1_tv.setText(R.string.activity1str); //set text for text view

        TextView activity2_tv = findViewById(R.id.activity2);
        activity2_tv.setText(R.string.activity2str); //set text for text view

        TextView activity3_tv = findViewById(R.id.activity3);
        activity3_tv.setText(R.string.activity3str); //set text for text view

        TextView activity4_tv = findViewById(R.id.activity4);
        activity4_tv.setText(R.string.activity4str); //set text for text view



        try {
            musicRecommender = new MusicRecommender(this);

            TextView rec1 = findViewById(R.id.rec1);
            TextView rec2 = findViewById(R.id.rec2);
            TextView rec3 = findViewById(R.id.rec3);
            TextView rec4 = findViewById(R.id.rec4);
            TextView rec5 = findViewById(R.id.rec5);

            TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

            Song[] songs = musicRecommender.knn(5, -1.3f, 1.3f);
            for (int i = 0; i < 5; i++) {
                String toTextView = "";
                toTextView += songs[i].artist + " - " + songs[i].title + " (V = " +
                        String.format("%.2f", songs[i].v) + ", A = " +
                        String.format("%.2f", songs[i].a) + ")";
                recs[i].setText(toTextView);
            }
        }
        catch (IOException e) {
            Log.e("OnCreate", "Music recommender instantiation failed");
        }

        // Manual input for song recommendation
        final EditText input_valence = findViewById(R.id.input_valence);
        final EditText input_arousal = findViewById(R.id.input_arousal);
        final EditText input_stride = findViewById(R.id.input_stride);
        Button get_songs = findViewById(R.id.get_songs);

        input_valence.setHint("val");
        input_arousal.setHint("ar");
        input_stride.setHint("str");

        get_songs.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                boolean valenceGiven = input_valence.getText().toString().trim().length() > 0;
                boolean arousalGiven = input_arousal.getText().toString().trim().length() > 0;
                //boolean strideGiven = input_stride.getText().toString().trim().length() > 0;
                float thisValence = 0.0f;
                float thisArousal = 0.0f;
                //float thisStride = 0.0f;

                if (valenceGiven) thisValence = Float.parseFloat(input_valence.getText().toString());
                if (arousalGiven) thisArousal = Float.parseFloat(input_arousal.getText().toString());
                //if (strideGiven)  thisStride = Float.parseFloat(input_stride.getText().toString());

                float thisStride = (float)effectiveStrideRate;

                TextView rec1 = findViewById(R.id.rec1);
                TextView rec2 = findViewById(R.id.rec2);
                TextView rec3 = findViewById(R.id.rec3);
                TextView rec4 = findViewById(R.id.rec4);
                TextView rec5 = findViewById(R.id.rec5);

                TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

                // If non-active, search based on VA
                if (stateMachine.state == 0) {
                    Song[] recommendations = musicRecommender.knn(K, thisValence, thisArousal);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                else {
                    Song[] recommendations = musicRecommender.knn(K, thisValence, thisArousal, thisStride);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ", T " +
                                String.format("%.2f", recommendations[i].t) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                // Set global values
                v_global = thisValence;
                a_global = thisArousal;
                t_global = thisStride;
            }
                                     });

        // Get VA from the Pi-hosted site
        final TextView retrieved_VA_tv = findViewById(R.id.retrieved_VA);
        Button get_http_btn = findViewById(R.id.get_http);
        final Activity thisActivity = this;

        get_http_btn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final AsyncTask<String, Void, String> myTask = new GetFromSite(thisActivity);
                myTask.execute(site_url);
                setVA(retrieved_VA_tv.getText().toString());
                retrieved_VA_tv.setText(getVA());
                Log.e("DEBUG", "VA value is " + getVA());

                int VA = 33;
                try {
                    VA = Integer.parseInt(getVA());
                }
                catch (Exception e) {
                    Log.e("DEBUG", "Couldn't parse an integer value from site");
                }
                int thisRow = (VA / 10) - 1; //kp_rows - 1 - ((VA / 10) - 1);
                int thisCol = kp_cols - 1 - ((VA % 10) - 1);

                float thisValence = key_points[thisCol][thisRow][0];
                float thisArousal = key_points[thisCol][thisRow][1];

                TextView estimated_VA = findViewById(R.id.estimated_VA);
                estimated_VA.setText(Float.toString(thisValence) + ", " + Float.toString(thisArousal));

                float thisStride = (float)effectiveStrideRate;

                TextView rec1 = findViewById(R.id.rec1);
                TextView rec2 = findViewById(R.id.rec2);
                TextView rec3 = findViewById(R.id.rec3);
                TextView rec4 = findViewById(R.id.rec4);
                TextView rec5 = findViewById(R.id.rec5);

                TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

                // If non-active, search based on VA
                if (stateMachine.state == 0) {
                    Song[] recommendations = musicRecommender.knn(K, thisValence, thisArousal);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                else {
                    Song[] recommendations = musicRecommender.knn(K, thisValence, thisArousal, thisStride);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ", T " +
                                String.format("%.2f", recommendations[i].t) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                // Set global values
                v_global = thisValence;
                a_global = thisArousal;
                t_global = thisStride;

            }
        });

        Button adjust_upbeat = findViewById(R.id.adjust_upbeat);
        Button adjust_downer = findViewById(R.id.adjust_downer);
        Button adjust_hype = findViewById(R.id.adjust_hype);
        Button adjust_chill = findViewById(R.id.adjust_chill);

        adjust_upbeat.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView rec1 = findViewById(R.id.rec1);
                TextView rec2 = findViewById(R.id.rec2);
                TextView rec3 = findViewById(R.id.rec3);
                TextView rec4 = findViewById(R.id.rec4);
                TextView rec5 = findViewById(R.id.rec5);

                TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

                v_global += v_adjust;

                TextView estimated_VA = findViewById(R.id.estimated_VA);
                estimated_VA.setText(String.format("%.2f", v_global) + ", " + String.format("%.2f", a_global));

                // If non-active, search based on VA
                if (stateMachine.state == 0) {
                    Song[] recommendations = musicRecommender.knn(K, v_global, a_global);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                else {
                    Song[] recommendations = musicRecommender.knn(K, v_global, a_global, t_global);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ", T " +
                                String.format("%.2f", recommendations[i].t) + ")";
                        recs[i].setText(toTextView);
                    }
                }

            }
        });

        adjust_downer.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView rec1 = findViewById(R.id.rec1);
                TextView rec2 = findViewById(R.id.rec2);
                TextView rec3 = findViewById(R.id.rec3);
                TextView rec4 = findViewById(R.id.rec4);
                TextView rec5 = findViewById(R.id.rec5);

                TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

                v_global -= v_adjust;

                TextView estimated_VA = findViewById(R.id.estimated_VA);
                estimated_VA.setText(String.format("%.2f", v_global) + ", " + String.format("%.2f", a_global));

                // If non-active, search based on VA
                if (stateMachine.state == 0) {
                    Song[] recommendations = musicRecommender.knn(K, v_global, a_global);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                else {
                    Song[] recommendations = musicRecommender.knn(K, v_global, a_global, t_global);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ", T " +
                                String.format("%.2f", recommendations[i].t) + ")";
                        recs[i].setText(toTextView);
                    }
                }

            }
        });

        adjust_hype.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView rec1 = findViewById(R.id.rec1);
                TextView rec2 = findViewById(R.id.rec2);
                TextView rec3 = findViewById(R.id.rec3);
                TextView rec4 = findViewById(R.id.rec4);
                TextView rec5 = findViewById(R.id.rec5);

                TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

                a_global += a_adjust;

                TextView estimated_VA = findViewById(R.id.estimated_VA);
                estimated_VA.setText(String.format("%.2f", v_global) + ", " + String.format("%.2f", a_global));

                // If non-active, search based on VA
                if (stateMachine.state == 0) {
                    Song[] recommendations = musicRecommender.knn(K, v_global, a_global);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                else {
                    Song[] recommendations = musicRecommender.knn(K, v_global, a_global, t_global);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ", T " +
                                String.format("%.2f", recommendations[i].t) + ")";
                        recs[i].setText(toTextView);
                    }
                }

            }
        });

        adjust_chill.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView rec1 = findViewById(R.id.rec1);
                TextView rec2 = findViewById(R.id.rec2);
                TextView rec3 = findViewById(R.id.rec3);
                TextView rec4 = findViewById(R.id.rec4);
                TextView rec5 = findViewById(R.id.rec5);

                TextView[] recs = {rec1, rec2, rec3, rec4, rec5};

                a_global -= a_adjust;

                TextView estimated_VA = findViewById(R.id.estimated_VA);
                estimated_VA.setText(String.format("%.2f", v_global) + ", " + String.format("%.2f", a_global));

                // If non-active, search based on VA
                if (stateMachine.state == 0) {
                    Song[] recommendations = musicRecommender.knn(K, v_global, a_global);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ")";
                        recs[i].setText(toTextView);
                    }
                }

                else {
                    Song[] recommendations = musicRecommender.knn(K, v_global, a_global, t_global);
                    for (int i = 0; i < K; i++) {
                        String toTextView = "";
                        toTextView += recommendations[i].artist + " - " + recommendations[i].title + " (V " +
                                String.format("%.2f", recommendations[i].v) + ", A " +
                                String.format("%.2f", recommendations[i].a) + ", T " +
                                String.format("%.2f", recommendations[i].t) + ")";
                        recs[i].setText(toTextView);
                    }
                }

            }
        });

        stateMachine = new StateMachine();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {

            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(sensorEventListener, sensor, SAMPLE_RATE_US);

        } else {
            Log.e("ACC", "No accelerometer found");
        }

        sensorManagerStep = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManagerStep.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {

            sensorStep = sensorManagerStep.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            sensorManagerStep.registerListener(sensorEventListener, sensorStep, SensorManager.SENSOR_DELAY_FASTEST);

        } else {
            Log.e("ACC", "No step detector found");
        }

        zeroStrideArray();

    }

    public void setVA(String VA) {
        valFrom_retrieved_VA_tv = VA;
    }

    public String getVA() {
        return valFrom_retrieved_VA_tv;
    }


    /* TODO:
    implement active/non-active mode detection
    implement different recommendation functions depending on mode
     */

}


