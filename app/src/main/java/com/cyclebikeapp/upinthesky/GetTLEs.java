package com.cyclebikeapp.upinthesky;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class GetTLEs {

    public static final String JSON_KEY_NORAD_CAT_ID = "NORAD_CAT_ID";
    public static final String baseURL = "https://www.space-track.org";
    public static final String authPath = "/auth/login";
    public static final String userName = "";
    public static final String password = "";
    public GetTLEs() {

    }

    public JSONArray fetchTLEs(String query, String satelliteKind) {
        JSONArray response = new JSONArray();
        try {
            CookieManager manager = new CookieManager();
            manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(manager);
            URL url = new URL(baseURL + authPath);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            String input = "identity=" + userName + "&password=" + password;
            OutputStream os = conn.getOutputStream();
            os.write(input.getBytes());
            os.flush();
            // read log-in response from server
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            //System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                //System.out.println(output);
                // flush buffered reader
            }
            // should be logged-in now, send query
            // todo parse log-in response to see if we're really logged-in before sending query
            url = new URL(baseURL + query);
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            while ((output = br.readLine()) != null) {
                //System.out.println(output);
                int noradNumber = 0;
                try {
                    response = new JSONArray(output);
                    // now test if we have a valid JSONArray by requesting the Norad Number
                    JSONObject mJSONObj = response.getJSONObject(0);
                    noradNumber = mJSONObj.getInt(JSON_KEY_NORAD_CAT_ID);
                } catch (JSONException e) {
                    if (MainActivity.DEBUG){
                        e.printStackTrace();
                        Log.w(this.getClass().getName(), "noradNumber = " + noradNumber);
                    }
                }
            }
// todo don't log out after each query; also need to escape this task with a time-out in case SpaceTrack is not responding
            url = new URL(baseURL + "/ajaxauth/logout");
            br = new BufferedReader(new InputStreamReader((url.openStream())));
            //System.out.println("logout response Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
            conn.disconnect();
        } catch (Exception e) {
            if (MainActivity.DEBUG){e.printStackTrace();}
        }
        return response;
    }
 
}
