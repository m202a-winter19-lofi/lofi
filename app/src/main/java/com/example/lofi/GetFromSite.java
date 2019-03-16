package com.example.lofi;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class GetFromSite extends AsyncTask<String, Void, String> {

    private Activity activity;

    public GetFromSite(Activity a) {
        this.activity = a;
    }

    @Override
    protected String doInBackground(String... site_urls) {
        String result = "";
        String line;
        BufferedReader input;
        URL site;
        InputStreamReader siteISR = null;


        try {
            Log.e("URL", "About to initialize URL");
            site = new URL(site_urls[0]);
            Log.e("URL", "Initialized URL. Initializing Input stream");
            InputStream siteStream = site.openStream();
            Log.e("URL", "Initialized input stream. Initializing Input stream reader");
            siteISR = new InputStreamReader(siteStream);
            Log.e("URL", "Initialized input stream reader. Initializing buffered reader");
            input = new BufferedReader(siteISR);

            Log.e("URL", "Initialized BufferedReader.");
            while ((line = input.readLine()) != null) {
                result += line;
            }
        }
        catch (Exception e) {
            Log.e("URL", "Failed to open stream to site, " + e.toString());
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        TextView txt = activity.findViewById(R.id.retrieved_VA);
        txt.setText(result);
    }

}
