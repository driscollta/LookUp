package com.cyclebikeapp.lookup;

import android.content.ContentValues;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static com.cyclebikeapp.lookup.Util.readTLEsFromFile;

/**
 * Created by TommyD on 10/12/2016.
 * Contact Celestrak website and retrieve text containing TLEs. Parse the return to extract ContentValues
 * that can be used to update the database.
 */

class GetTLEsFromCelestrak {
    ArrayList<ContentValues> getTLEsFromCelestrak(String webSite) {
        ArrayList<ContentValues> returnList = new ArrayList<>();
        try {
            returnList.addAll(downloadUrl(webSite));
        } catch (IOException ignored) {
            // bad URL link goes here and we won't return any TLE data
            if (MainActivity.DEBUG){Log.i(this.getClass().getName(), "bad URL? - " + webSite);}

        }
        return returnList;
    }

    ArrayList<ContentValues> getNewSatellites(String webSite) {
        ArrayList<ContentValues> returnList = new ArrayList<>();
        try {
            returnList.addAll(downloadUrl(webSite));
        } catch (IOException ignored) {
            // bad URL link goes here and we won't return any TLE data
            if (MainActivity.DEBUG){Log.i(this.getClass().getName(), "bad URL? - " + webSite);}

        }
        return returnList;
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // an ArrayList of ContentValues.
    ArrayList<ContentValues> downloadUrl(String myurl) throws IOException {
        if (MainActivity.DEBUG){Log.i(this.getClass().getName(),"retrieving from link: " + myurl);}
        InputStream is = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(myurl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(35000 /* milliseconds */);
            conn.setConnectTimeout(35000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            if (MainActivity.DEBUG){Log.i(this.getClass().getName(), "The response code is: " + response);}
            is = conn.getInputStream();
            boolean readName = true;
            // Convert the InputStream into anArrayList of ContentValues
            return readTLEsFromFile(is, 3, readName);
            // Makes sure that the InputStream is closed after the app is finished using it.
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored){

                }
            }
        }
    }

}
