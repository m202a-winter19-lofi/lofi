package com.example.lofi;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicRecommender {

    //protected List<Song> database;
    protected Map<String, Song> db;

    // Weights, biases for distance metric
    protected static double alpha = 1.0;
    protected static double beta = 1.0;
    protected static double gamma = 3.0;

    protected static double v_bias = 0.0;
    protected static double a_bias = 0.0;

    public MusicRecommender(Activity activity) throws IOException {

        //database = new ArrayList<Song>();
        db = new HashMap<String, Song>();

        try {
            List<String[]> csvLine = new ArrayList<>();
            String[] content = null;

            AssetManager am = activity.getAssets();
            InputStream csv = am.open("merged_dataset_trimmed.csv");

            BufferedReader br = new BufferedReader(new InputStreamReader(csv));
            String line = "";
            while((line = br.readLine()) != null){
                content = line.split(","); // content is String[]
                csvLine.add(content);
            }

            /* Was the dataset read in correctly?
            int i = 1;
            for (String[] s : csvLine) {
                Log.i("DATASET", "!!!!! Song #" + i++);
                for (String ss : s) {
                    Log.i("DATASET", ss);
                }
            }
            */

            // Process the dataset by creating Song object (cast to float some elements) and make hashmap
            for (String[] row : csvLine) { // for every song, which is a Object[]

                String trackID = row[0];
                db.put(trackID, new Song(row));

            }

            /* Was the dataset processed correctly?
            int i = 1;
            for (Song s : database) {
                Log.i("DATASET", "!!!!! Song #" + i++);
                Log.i("DATASET", s.trackID.getClass().getName() +
                        s.v +
                        s.a +
                        s.artist.getClass().getName() +
                        s.title.getClass().getName() +
                        s.t);

            }
            */

            // Construct database as dictionary, db, with trackID as key, Song as value
            //for (Song s : database) {
            //    db.put(s.trackID, s);
            //}

            //Log.i("DATASET", "Songs read in: " + db.size());


        }
        catch (IOException e) {
            Log.e("MRC", "No dataset found");
        }


    }

    public Song[] knn(int k, float v, float a) {
        /* Given valence and arousal, find the k nearest neighbors (songs) in the VA space,
         * return a Song[] of k closest songs. */
        Song[] result = new Song[k];
        Map<Double, String> distanceMap = new HashMap<>();
        List<Double> distances = new ArrayList<>();

        //float distance;
        double distance;
        double vv = (double)v;
        double aa = (double)a;

        // Calculate distances
        for (Map.Entry<String, Song> entry : db.entrySet()) {
            String key = entry.getKey();
            Song song = entry.getValue();
            double song_v = (double)song.v;
            double song_a = (double)song.a;

            //distance = (float)Math.hypot(vv - song_v, aa - song_a);
            distance = Math.hypot(alpha*(vv - song_v), beta*(aa - song_a));

            // if there are duplicate distances
            if (distanceMap.containsKey(distance)) {
                double newDistance = distance + 0.001;
                while (distanceMap.containsKey(newDistance)) {
                    newDistance += 0.001;
                }
                distanceMap.put(newDistance, key);
                distances.add(newDistance);
            }
            else {
                distanceMap.put(distance, key);
                distances.add(distance);
            }


        }

        // Sort
        Collections.sort(distances);

        // Extract k closest
        for (int i = 0; i < k; i++) {
            String trackID = distanceMap.get(distances.get(i));
            result[i] = db.get(trackID);
        }




        return result;
    }

    public Song[] knn(int k, float v, float a, float t) {
        /* Given valence and arousal, find the k nearest neighbors (songs) in the VAT space,
         * return a Song[] of k closest songs. */
        Song[] result = new Song[k];

        Map<Double, String> distanceMap = new HashMap<>();
        List<Double> distances = new ArrayList<>();

        //float distance;
        double distance;
        double vv = (double)v;
        double aa = (double)a;
        double tt = (double)t;

        // Calculate distances
        for (Map.Entry<String, Song> entry : db.entrySet()) {
            String key = entry.getKey();
            Song song = entry.getValue();
            double song_v = (double)song.v;
            double song_a = (double)song.a;
            double song_t = (double)song.t;

            //distance = (float)Math.hypot(vv - song_v, aa - song_a);
            distance = Math.sqrt(Math.pow(alpha*(vv - song_v), 2) +
                                 Math.pow(beta*(aa - song_a), 2) +
                                 Math.pow(gamma*(tt - song_t) + v_bias + a_bias, 2));



            // if there are duplicate distances
            if (distanceMap.containsKey(distance)) {
                double newDistance = distance + 0.001;
                while (distanceMap.containsKey(newDistance)) {
                    newDistance += 0.001;
                }
                distanceMap.put(newDistance, key);
                distances.add(newDistance);
            }
            else {
                distanceMap.put(distance, key);
                distances.add(distance);
            }


        }

        // Sort
        Collections.sort(distances);

        // Extract k closest
        for (int i = 0; i < k; i++) {
            String trackID = distanceMap.get(distances.get(i));
            result[i] = db.get(trackID);
        }

        return result;
    }

    // for debugging only.
    public Song[] knn(int k, float t) {
        Song[] result = new Song[k];

        Map<Double, String> distanceMap = new HashMap<>();
        List<Double> distances = new ArrayList<>();

        //float distance;
        double distance;
        double vv = (double)t;

        // Calculate distances
        for (Map.Entry<String, Song> entry : db.entrySet()) {
            String key = entry.getKey();
            Song song = entry.getValue();
            double song_t = (double)song.t;

            //distance = (float)Math.hypot(vv - song_v, aa - song_a);
            distance = Math.abs(t - song_t);

            // if there are duplicate distances
            if (distanceMap.containsKey(distance)) {
                double newDistance = distance + 0.001;
                while (distanceMap.containsKey(newDistance)) {
                    newDistance += 0.001;
                }
                distanceMap.put(newDistance, key);
                distances.add(newDistance);
            }
            else {
                distanceMap.put(distance, key);
                distances.add(distance);
            }


        }

        // Sort
        Collections.sort(distances);

        // Extract k closest
        //Log.i("KNN", "Size of distances array " + distances.size());
        for (int i = 0; i < k; i++) {
            String trackID = distanceMap.get(distances.get(i));
            result[i] = db.get(trackID);
            //Log.i("KNN", "ID: " + result[i].trackID);
            //Log.i("KNN", "V: " + result[i].v)
        }

        return result;
    }

}
