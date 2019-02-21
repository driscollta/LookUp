package com.cyclebikeapp.lookup;

import android.content.ContentValues;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static com.cyclebikeapp.lookup.Util.readSatelliteStatusFromFile;

public class GetSatelliteStatusFromCelestrak {
    /*
Columns
001-011	International Designator

    Launch Year (001-004)
    Launch of the Year (006-008)
    Piece of the Launch (009-011)

014-018	NORAD Catalog Number
020-020	Multiple Name Flag ("M" if multiple names exist; alternate names found in satcat-annex.txt)
021-021	Payload Flag ("*" if payload; blank otherwise)
022-022	Operational Status Code
Note: The "U" code indicates that the satellite is considered operational according to the
+ 	Operational
- 	Nonoperational
P 	Partially Operational
B 	Backup/Standby
S 	Spare
X 	Extended Mission
D 	Decayed
? 	Unknown
*/

    ArrayList<ContentValues> getStatusFromCelestrak(String webSite) {
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
        if (MainActivity.DEBUG){
            Log.i(this.getClass().getName(),"retrieving from link: " + myurl);}
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
            // Convert the InputStream into anArrayList of ContentValues
            return readSatelliteStatusFromFile(is);
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
