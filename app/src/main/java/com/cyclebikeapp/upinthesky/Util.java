package com.cyclebikeapp.upinthesky;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.cyclebikeapp.upinthesky.Constants.LOCATION_STATUS_NONE;
import static com.cyclebikeapp.upinthesky.Constants.LOCATION_STATUS_OKAY;
import static com.cyclebikeapp.upinthesky.Constants.LOCATION_STATUS_OLD;
import static com.cyclebikeapp.upinthesky.Constants.LOCATION_STATUS_UNKNOWN;
import static com.cyclebikeapp.upinthesky.Constants.MOBILE_DATA_SETTING_KEY;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_ALTITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_LATITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_LONGITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_TIME;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_ALTITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_LATITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_LONGITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_TIME;
import static com.cyclebikeapp.upinthesky.Constants.PREF_KEY_SATFILES_READ_SUCCESS;
import static com.cyclebikeapp.upinthesky.Constants.PREF_KEY_UPDATED_TLES_TIME;
import static com.cyclebikeapp.upinthesky.Constants.TWENTY_FOUR_HOURS;

/**
 * Created by TommyD on 4/18/2016.
 *
 */
public class Util {

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean hasInternetPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && hasInternetPermission;
    }

    public static boolean hasMobileDataPermission(SharedPreferences settings) {
        return Integer.parseInt(settings.getString(MOBILE_DATA_SETTING_KEY, "0")) == 1;
    }

    public static boolean hasMobileInternetPermission(SharedPreferences settings) {
        return Integer.parseInt(settings.getString(MOBILE_DATA_SETTING_KEY, "0")) == 1;
    }

    public static boolean hasWifiInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean hasInternetPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && hasInternetPermission
                && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public static boolean isGPSLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (SecurityException ignored) {
        }
        return gps_enabled;
    }

    public static boolean isNetworkLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean network_enabled = false;
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException ignored) {
        }
        return network_enabled;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable(Context context) {
        String state = Environment.getExternalStorageState();
        boolean hasWritePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return Environment.MEDIA_MOUNTED.equals(state) && hasWritePermission;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static String returnStringLocationStatus(int locationStatus) {
        String status = LOCATION_STATUS_UNKNOWN;
        switch (locationStatus) {
            case GridSatView.LOCATION_STATUS_NONE:
                status = LOCATION_STATUS_NONE;
                break;
            case GridSatView.LOCATION_STATUS_OLD:
                status = LOCATION_STATUS_OLD;
                break;
            case GridSatView.LOCATION_STATUS_OKAY:
                status = LOCATION_STATUS_OKAY;
                break;
            case GridSatView.LOCATION_STATUS_UNKNOWN:
                status = LOCATION_STATUS_UNKNOWN;
                break;
        }
        return status;
    }

    /**
     * Test if the Location is the program default, meaning we have never received a Location
     *
     * @param aLoc the location to test
     * @return true if the location is the default
     */

    public static boolean locationIsDefault(Location aLoc) {
        return aLoc.getTime() == PREFS_DEFAULT_TIME
                && aLoc.getLatitude() == Double.parseDouble(PREFS_DEFAULT_LATITUDE)
                && aLoc.getLongitude() == Double.parseDouble(PREFS_DEFAULT_LONGITUDE)
                && aLoc.getAltitude() == Double.parseDouble(PREFS_DEFAULT_ALTITUDE);
    }

    public static Location getLocFromSharedPrefs(SharedPreferences settings) {
        // last location is stored in Shared Prefs in LocationHelper
        Location aLoc = new Location(LocationManager.NETWORK_PROVIDER);
        aLoc.setLongitude(Double.parseDouble(settings.getString(PREFS_KEY_LONGITUDE, PREFS_DEFAULT_LONGITUDE)));
        aLoc.setLatitude(Double.parseDouble(settings.getString(PREFS_KEY_LATITUDE, PREFS_DEFAULT_LATITUDE)));
        aLoc.setAltitude(Double.parseDouble(settings.getString(PREFS_KEY_ALTITUDE, PREFS_DEFAULT_ALTITUDE)));
        // this is just temporary until we get a location from LocationHelper
        aLoc.setTime(settings.getLong(PREFS_KEY_TIME, PREFS_DEFAULT_TIME));
        return aLoc;
    }

    public static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean hasFineLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    public static boolean satelliteFilesWereRead(SharedPreferences settings) {
        return settings.getBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false);
    }
    public static  void setReadSatFilesFlag(SharedPreferences settings) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false).apply();
    }
    public static boolean checkTLEsNeedUpdating(SharedPreferences settings) {
        // UPDATED_TLES_TIME is set when successfully updating TLEs in dataBaseAdapter.updateTLEsAsync()
        return System.currentTimeMillis() - settings.getLong(PREF_KEY_UPDATED_TLES_TIME, 0) > TWENTY_FOUR_HOURS;
    }
    public static String convertMsec2Date(long time){
        Date date = new Date();
        date.setTime(time);
        return new SimpleDateFormat("MM dd, yyyy").format(date);
    }
}
