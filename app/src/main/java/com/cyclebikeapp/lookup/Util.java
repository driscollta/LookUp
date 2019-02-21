package com.cyclebikeapp.lookup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.cyclebikeapp.lookup.Constants.DB_KEY_NORAD_NUM;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_NAME;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_SUB_CATEGORY;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_TLE1;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_TLE2;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_TLE_TIMESTAMP;
import static com.cyclebikeapp.lookup.Constants.FUTURE_EPOCH;
import static com.cyclebikeapp.lookup.Constants.KEY_REQUESTING_LOCATION_UPDATES;
import static com.cyclebikeapp.lookup.Constants.LOCATION_STATUS_NONE;
import static com.cyclebikeapp.lookup.Constants.LOCATION_STATUS_OKAY;
import static com.cyclebikeapp.lookup.Constants.LOCATION_STATUS_OLD;
import static com.cyclebikeapp.lookup.Constants.LOCATION_STATUS_UNKNOWN;
import static com.cyclebikeapp.lookup.Constants.MAX_SAT_NAME_LENGTH;
import static com.cyclebikeapp.lookup.Constants.MOBILE_DATA_SETTING_KEY;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_ALTITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_LATITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_LONGITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_MAG_DECLINATION;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_TIME;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_ALTITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_LATITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_LONGITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_MAG_DECLINATION;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_TIME;
import static com.cyclebikeapp.lookup.Constants.PREFS_NAME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_READING_SATFILES;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_SATFILES_READ_SUCCESS;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATED_CELESTRAK_STATUS_TIME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATED_CELESTRAK_TLES_TIME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATED_MCCANTS_TLES_TIME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATING_TLES;
import static com.cyclebikeapp.lookup.Constants.STRING_ZERO;
import static com.cyclebikeapp.lookup.Constants.TWENTY_FOUR_HOURS;

/**
 * Created by TommyD on 4/18/2016.
 *
 */
class Util {

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }
    static float getDeclinationFromSharedPrefs(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getFloat(PREFS_KEY_MAG_DECLINATION, PREFS_DEFAULT_MAG_DECLINATION);
    }
    static void saveLocSharedPrefs(Location location, Context mContext) {
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PREFS_KEY_TIME, location.getTime());
        editor.putString(PREFS_KEY_ALTITUDE, String.valueOf(location.getAltitude()));
        editor.putString(PREFS_KEY_LATITUDE, String.valueOf(location.getLatitude()));
        editor.putString(PREFS_KEY_LONGITUDE, String.valueOf(location.getLongitude())).apply();
        // save magnetic declination to sharedPrefs for this Location
        saveMagDeclinationSharedPrefs(location, mContext);
        // only call saveLocSharedPrefs when new Location received, so we should indicate rebuildGEOSatellites with new Location
    }
    static Location getLocFromSharedPrefs(Context mContext) {
        Location aLoc = new Location(LocationManager.NETWORK_PROVIDER);
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        aLoc.setLongitude(Double.parseDouble(settings.getString(PREFS_KEY_LONGITUDE, PREFS_DEFAULT_LONGITUDE)));
        aLoc.setLatitude(Double.parseDouble(settings.getString(PREFS_KEY_LATITUDE, PREFS_DEFAULT_LATITUDE)));
        aLoc.setAltitude(Double.parseDouble(settings.getString(PREFS_KEY_ALTITUDE, PREFS_DEFAULT_ALTITUDE)));
        // this is just temporary until we get a location from LocationHelper
        aLoc.setTime(settings.getLong(PREFS_KEY_TIME, PREFS_DEFAULT_TIME));
        return aLoc;
    }

    private static void saveMagDeclinationSharedPrefs(Location location, Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        GeomagneticField gmf = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(),
                location.getTime());
        editor.putFloat(PREFS_KEY_MAG_DECLINATION, gmf.getDeclination()).apply();
    }
    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    static String getLocationText(Location location) {
        return "Tracking location...";
    }

    static String getLocationTitle(Context context) {
        return context.getString(R.string.location_updated,
                DateFormat.getDateTimeInstance().format(new Date()));
    }

    static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        boolean hasInternetPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && hasInternetPermission;
    }

    static boolean hasMobileDataPermission(SharedPreferences settings) {
        return Integer.parseInt(settings.getString(MOBILE_DATA_SETTING_KEY, STRING_ZERO)) == 1;
    }

    static boolean hasMobileInternetPermission(SharedPreferences settings) {
        return Integer.parseInt(settings.getString(MOBILE_DATA_SETTING_KEY, STRING_ZERO)) == 1;
    }

    static boolean hasWifiInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = null;
        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        }
        boolean hasInternetPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting() && hasInternetPermission
                && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }

    static boolean isGPSLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        try {
            if (lm != null) {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }
        } catch (SecurityException ignored) {
        }
        return gps_enabled && hasLocationPermission(context);
    }

    static boolean isNetworkLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean network_enabled = false;
        try {
            if (lm != null) {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (SecurityException ignored) {
        }
        return network_enabled;
    }

    /* Checks if external storage is available for read and write */
    static boolean isExternalStorageWritable(Context context) {
        String state = Environment.getExternalStorageState();
        boolean hasWritePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (MainActivity.DEBUG) { Log.i("Util", "isExternalStorageWritable? "
                + (Environment.MEDIA_MOUNTED.equals(state) && hasWritePermission?"yes":"no"));}
        return Environment.MEDIA_MOUNTED.equals(state) && hasWritePermission;
    }

    static String returnStringLocationStatus(int locationStatus) {
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

    static boolean locationIsDefault(Location aLoc) {
        return aLoc.getTime() == PREFS_DEFAULT_TIME
                && aLoc.getLatitude() == Double.parseDouble(PREFS_DEFAULT_LATITUDE)
                && aLoc.getLongitude() == Double.parseDouble(PREFS_DEFAULT_LONGITUDE)
                && aLoc.getAltitude() == Double.parseDouble(PREFS_DEFAULT_ALTITUDE);
    }

    static Location getLocFromSharedPrefs(SharedPreferences settings) {
        // last location is stored in Shared Prefs in LocationHelper
        Location aLoc = new Location(LocationManager.NETWORK_PROVIDER);
        aLoc.setLongitude(Double.parseDouble(settings.getString(PREFS_KEY_LONGITUDE, PREFS_DEFAULT_LONGITUDE)));
        aLoc.setLatitude(Double.parseDouble(settings.getString(PREFS_KEY_LATITUDE, PREFS_DEFAULT_LATITUDE)));
        aLoc.setAltitude(Double.parseDouble(settings.getString(PREFS_KEY_ALTITUDE, PREFS_DEFAULT_ALTITUDE)));
        // this is just temporary until we get a location from LocationHelper
        aLoc.setTime(settings.getLong(PREFS_KEY_TIME, PREFS_DEFAULT_TIME));
        return aLoc;
    }

    static boolean hasCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasFineLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean satelliteFilesWereRead(SharedPreferences settings) {
        return settings.getBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false);
    }
    static boolean readingSatFiles(SharedPreferences settings) {
        return settings.getBoolean(PREF_KEY_READING_SATFILES, false);
    }

    static boolean updatingTLEs(SharedPreferences settings) {
        return settings.getBoolean(PREF_KEY_UPDATING_TLES, false);
    }

    static void setReadSatFilesFlag(SharedPreferences settings) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false).apply();
    }

    private static double getEpochFromTLE(String tle1) throws IllegalArgumentException {
        Integer year = Integer.parseInt((tle1.substring(18, 20)).trim());
        double refEpoch = Double.parseDouble(tle1.substring(20, 32));
        return (1000.0 * year) + refEpoch;
    }

    private static int getCatnumFromTLE(String tle1) throws IllegalArgumentException {
        return Integer.parseInt((tle1.substring(2, 7)).trim());
    }
/*    private static String convertEpoch2Date(double epoch) {
        String day = String.valueOf(epoch).substring(2);
        int decimalIndex = day.indexOf(".");
        int dayOfYear = Integer.parseInt(day.substring(0,decimalIndex));
        String yearDigits = String.valueOf(epoch).substring(0,2);
        String yearPrefix = (Integer.parseInt(yearDigits) < 56)?"20":"19";
        int year = Integer.parseInt(yearPrefix + yearDigits);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        calendar.set(Calendar.YEAR, year);

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_Z_FORMAT, Locale.US);
        return sdf.format(calendar.getTime());
    }*/
    static void listCacheFiles(File cDir) {
        StringBuilder fileList = new StringBuilder();
        for (String file : cDir.list()) {
            fileList.append(file).append("\n");
        }
        Log.w("listCacheFiles", "McCants files in cache: " + fileList);
    }
    static boolean deleteCacheFolderFile(String file) {
        File fileToDelete = new File(file);
        return fileToDelete.exists() && fileToDelete.delete();
    }
    static boolean deleteAllCacheFolderFiles(Context mContext){
        boolean success = true;
        for (String file:mContext.getCacheDir().list()){
            success = success && deleteCacheFolderFile(mContext.getCacheDir().getPath() + "/" + file);
            if (MainActivity.DEBUG && success){
                Log.w("deleteCacheFiles", "deleted: " + file);
            }
        }
        return  success;
    }
    static File getAlbumStorageDir(String albumName, Context mContext) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), albumName);
        if (!file.exists()) {
            if (!file.mkdirs()){
                if (MainActivity.DEBUG) {Log.w("getAlbumStorageDir", "directory " + file.toString() + " not created");}
            } else{
                if (MainActivity.DEBUG) {Log.w("getAlbumStorageDir", "directory " + file.toString() + " created");}
            }
        } else {
            if (MainActivity.DEBUG) {Log.w("getAlbumStorageDir", "directory " + file.toString() + " already exists");}
        }
        return file;
    }
    static boolean checkCelestrakTLEsNeedUpdating(SharedPreferences settings) {
        // UPDATED_CELESTRAK_TLES_TIME is set when successfully updating TLEs in dataBaseAdapter.updateCelestrakTLEsAsync()
        return System.currentTimeMillis() - settings.getLong(PREF_KEY_UPDATED_CELESTRAK_TLES_TIME, 0) > TWENTY_FOUR_HOURS;
    }
    static boolean checkCelestrakStatusNeedsUpdating(SharedPreferences settings) {
        // UPDATED_CELESTRAK_STATUS_TIME is set when successfully updating status in dataBaseAdapter.updateSatelliteStatusAsync()
        return System.currentTimeMillis() - settings.getLong(PREF_KEY_UPDATED_CELESTRAK_STATUS_TIME, 0) > TWENTY_FOUR_HOURS;
    }

    static boolean checkMcCantsTLEsNeedUpdating(SharedPreferences settings) {
        // UPDATED_MCCANTS_TLES_TIME is set when successfully downloaded McCants TLE file
        return System.currentTimeMillis() - settings.getLong(PREF_KEY_UPDATED_MCCANTS_TLES_TIME, 0) > TWENTY_FOUR_HOURS;
    }

    static String limitSatelliteNameLength(String name){
        String returnName = name;
        if (name.length() > MAX_SAT_NAME_LENGTH){
            returnName = name.substring(0, Constants.MAX_SAT_NAME_LENGTH);
        }
        return returnName;
    }

    static boolean isAnySnackBarVisible(ArrayList<Snackbar> snackbars){
        for (Snackbar sb :snackbars){
            if (sb != null && sb.isShown()){
                return  true;
            }
        }
        return  false;
    }
    static void closeAllSnackbars(ArrayList<Snackbar> snackbars){
        for (Snackbar sb :snackbars){
            if (sb != null && sb.isShown()){
                sb.dismiss();
            }
        }
    }

    static ArrayList<ContentValues> readSatelliteStatusFromFile(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        String oneLine, status;
        ArrayList<ContentValues> returnList = new ArrayList<>();
        ContentValues statusContent;
        while ((oneLine = r.readLine()) != null) {

            try {
                if (isPayload(oneLine)) {
                    // extract Norad Number from TLE and put in DB_KEY_NORAD_NUM
                    // extract status and put in DB_KEY_SAT_SUB_CATEGORY
                    statusContent = new ContentValues();
                    statusContent.put(DB_KEY_NORAD_NUM, getCatnumFromStatusFile(oneLine));
                    status = getStatusFromStatusFile(oneLine);
                    statusContent.put(DB_KEY_SAT_SUB_CATEGORY, status);
                    statusContent.put(DB_KEY_SAT_TLE_TIMESTAMP, FUTURE_EPOCH);
                    if (("DECAYED").equals(status)) {
                        // delete tle's so we won't calculate orbit
                        statusContent.put(DB_KEY_SAT_TLE1, "");
                        statusContent.put(DB_KEY_SAT_TLE2, "");
                    }
                    if (MainActivity.DEBUG) {
                        int noradNum = statusContent.getAsInteger(DB_KEY_NORAD_NUM);
                        if (noradNum < 4300 && noradNum > 4200) {
                            Log.i("readStatusFromFile ", "norad# " + noradNum
                                    + " status: " + statusContent.get(DB_KEY_SAT_SUB_CATEGORY));
                        }
                    }
                    returnList.add(statusContent);
                }
            } catch (Exception ignored) {
                // if URL to Celestrak is bad we'll get garbage and trying to get Epoch or Catnum from TLE will end here
            }

        }
        return returnList;
    }

    private static boolean isPayload(String oneLine) {

        return ("*").equals(oneLine.substring(20, 21));
    }

    private static Integer getCatnumFromStatusFile(String oneLine) throws IllegalArgumentException {
        return Integer.parseInt((oneLine.substring(13, 18)).trim());
    }


    /*Note: The "U" code indicates that the satellite is considered operational according to the
+ 	Operational
- 	Nonoperational
P 	Partially Operational
B 	Backup/Standby
S 	Spare
X 	Extended Mission
D 	Decayed
? 	Unknown*/
    private static String getStatusFromStatusFile(String oneLine) {
        String statusCode = oneLine.substring(21, 22);
        String status;
        switch ((statusCode.toUpperCase())) {
            case "D":
                status = "DECAYED";
                break;
            case "+":
            case "U":
                status = "ACTIVE";
                break;
            default:
                status = "INACTIVE";
        }
        return status;

    }

    static ArrayList<ContentValues> readTLEsFromFile(InputStream stream, int numTLELines, boolean readName) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        String name, tle1, tle2;
        ArrayList<ContentValues> returnList = new ArrayList<>();
        ContentValues tleContent;
        while ((name = r.readLine()) != null) {
            // two-line tle from SpaceTrack doesn't have name; assign it to tle1.
            // three-line tle from McCants or Celestrak has name as first line. read next line for tle1
            if (numTLELines == 2) {
                tle1 = name;
            } else {
                tle1 = r.readLine();
            }
            tle2 = r.readLine();
            if (tle1 != null && tle2 != null){
                try {
                    // extract Norad Number from TLE and put in DB_KEY_NORAD_NUM
                    // extract Epoch and put in DB_KEY_SAT_TLE_TIMESTAMP
                    tleContent = new ContentValues();
                    tleContent.put(DB_KEY_NORAD_NUM, getCatnumFromTLE(tle1));
                    tleContent.put(DB_KEY_SAT_TLE_TIMESTAMP, getEpochFromTLE(tle1));
                    tleContent.put(DB_KEY_SAT_TLE1, tle1);
                    tleContent.put(DB_KEY_SAT_TLE2, tle2);
                    if (numTLELines == 3 && readName) {
                        tleContent.put(DB_KEY_SAT_NAME, name);
                    }
                    if (MainActivity.DEBUG && numTLELines == 2) {
                        int noradNum = tleContent.getAsInteger(DB_KEY_NORAD_NUM);
                        if (noradNum < 43000 && noradNum > 42900){
                            Log.i("readTLEsFromFile ", "norad# " + noradNum
                                    + " tle1 " + tleContent.get(DB_KEY_SAT_TLE1)
                                    + " tle2 " + tleContent.get(DB_KEY_SAT_TLE2)
                                    + " TIMESTAMP " + tleContent.get(DB_KEY_SAT_TLE_TIMESTAMP));
                        }
                    }
                    returnList.add(tleContent);
                } catch (Exception ignored){
                    // if URL to Celestrak is bad we'll get garbage and trying to get Epoch or Catnum from TLE will end here

                }
            }
        }
        return returnList;
    }

    @SuppressLint("SimpleDateFormat")
    static String convertMsec2Date(long time){
        Date date = new Date();
        date.setTime(time);
        return new SimpleDateFormat("MM dd, yyyy").format(date);
    }


    @NonNull
    /**
     * Extract substring from beginning of String until the first space character
     * to return the first word. In case the query has many words that don't match
     * can try just the first word.
     * @parameter mString the String to find the first word from
     * @return the characters up to the first space character, or the entire input String if no space found.
     */
    static String getFirstWord(String mString) {

        int endIndex = mString.indexOf(" ");
        if (endIndex == -1){
            // didn't find a space character, return the full word
            endIndex = mString.length();
        }
        return mString.substring(0,endIndex);
    }

    static Location getDefaultLocation() {
        Location aLoc = new Location(LocationManager.NETWORK_PROVIDER);
        aLoc.setLongitude(Double.parseDouble(PREFS_DEFAULT_LONGITUDE));
        aLoc.setLatitude(Double.parseDouble(PREFS_DEFAULT_LATITUDE));
        aLoc.setAltitude(Double.parseDouble(PREFS_DEFAULT_ALTITUDE));
        // this is just temporary until we get a location from LocationHelper
        aLoc.setTime(PREFS_DEFAULT_TIME);
        return aLoc;
    }

    static long getNowTime() {
        return Calendar.getInstance().getTime().getTime();
    }
}
