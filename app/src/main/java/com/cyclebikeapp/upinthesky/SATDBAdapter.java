package com.cyclebikeapp.upinthesky;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import static android.support.v4.content.ContextCompat.checkSelfPermission;
import static com.cyclebikeapp.upinthesky.Constants.DATE_TIME_Z_FORMAT;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_NORAD_NUM;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_OTHER;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_BRIEF_DESC;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_CATEGORY;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_INFO_LINK;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_KIND;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_NAME;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_SUB_CATEGORY;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_TLE1;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_TLE2;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_TLE_TIMESTAMP;
import static com.cyclebikeapp.upinthesky.Constants.DEEP;
import static com.cyclebikeapp.upinthesky.Constants.DOUBLE_QUOTE;
import static com.cyclebikeapp.upinthesky.Constants.GEO;
import static com.cyclebikeapp.upinthesky.Constants.JSON_KEY_EPOCH;
import static com.cyclebikeapp.upinthesky.Constants.JSON_KEY_NORAD_CAT_ID;
import static com.cyclebikeapp.upinthesky.Constants.JSON_KEY_TLE_LINE_0;
import static com.cyclebikeapp.upinthesky.Constants.JSON_KEY_TLE_LINE_1;
import static com.cyclebikeapp.upinthesky.Constants.JSON_KEY_TLE_LINE_2;
import static com.cyclebikeapp.upinthesky.Constants.LEO;
import static com.cyclebikeapp.upinthesky.Constants.MAX_NUM_TLE_QUERY;
import static com.cyclebikeapp.upinthesky.Constants.OLD_DATE;
import static com.cyclebikeapp.upinthesky.Constants.ONE_WEEK;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_NAME;
import static com.cyclebikeapp.upinthesky.Constants.PREF_KEY_READING_SATFILES;
import static com.cyclebikeapp.upinthesky.Constants.PREF_KEY_SATFILES_READ_SUCCESS;
import static com.cyclebikeapp.upinthesky.Constants.PREF_KEY_SATFILE_DOC_NUM;
import static com.cyclebikeapp.upinthesky.Constants.PREF_KEY_UPDATED_TLES_TIME;
import static com.cyclebikeapp.upinthesky.Constants.SAT_DB;
import static com.cyclebikeapp.upinthesky.Constants.SAT_TABLE;
import static com.cyclebikeapp.upinthesky.Constants.SIX_WEEKS;
import static com.cyclebikeapp.upinthesky.Constants.STILL_LOADING_SATELLITE_FILES;
import static com.cyclebikeapp.upinthesky.Constants.WITH_TLES;

public class SATDBAdapter {


    private final Context mContext;
	private DBHelper mDBHelper;
	private SQLiteDatabase satelliteDB;
    private final GetTLEs jParser;
    // we only build GEO satellites when loading a new data file, updating TLEs or receiveing a new Location
    private boolean rebuildGEOSatellites;


    public SATDBAdapter(Context context) {
		this.mContext = context;
        jParser = new GetTLEs();
        rebuildGEOSatellites = false;
	}

    /**
     * Used in buildSatellitesByKind(), returns all TLEs of the specified Kind from the dataBase
     * @param kind the type of satellite to fetchTLEs for; used to build or rebuild a list of visible satellites
     * @return a List of TLEs in a string vector
     */
    public ArrayList<String[]> fetchTLEsByKind(String kind) {
        ArrayList<String[]> returnTLEs = new ArrayList<>();
        String filter = DB_KEY_SAT_KIND + "= '" + kind + "'";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME, DB_KEY_SAT_TLE1, DB_KEY_SAT_TLE2, DB_KEY_SAT_CATEGORY, DB_KEY_SAT_SUB_CATEGORY};
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        String[] aTLEString = new String[5];
                        aTLEString[0] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_NAME));
                        aTLEString[1] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE1));
                        aTLEString[2] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE2));
                        aTLEString[3] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_CATEGORY));
                        aTLEString[4] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_SUB_CATEGORY));
                        // if we haven't yet gotten TLEs from Space-Track since loading the satellite file...
                        if (!aTLEString[1].equals("") && !aTLEString[2].equals("")) {
                            returnTLEs.add(aTLEString);
                         }
                    } while (mCursor.moveToNext());
                } else {
                    if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "no satellites in database");}
                }
            }
        } catch (Exception e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return returnTLEs;
    }

    public boolean shouldRebuildGEOSatellites() {
        return rebuildGEOSatellites;
    }

    public void setRebuildGEOSatellites(boolean rebuild) {
        rebuildGEOSatellites = rebuild;
    }

    /**
     * This is the primary call from onCreate() in MainActivity after openning the database. From here the
     * asset satellite data files are loaded, database updated and TLEs refreshed, all in Background Tasks.
     * @param applicationContext needed to load files from asset
     */
    public void readSatFilesFromAssetAsync(Context applicationContext) {
        new ReadSatFilesBackground().execute(applicationContext);
    }

    /**
     * Only called internally from readSatFilesFromAssetAsync()
     * Read satellite files from assets and update the satellite database.
     * When finished, update the Two Line Element sets from SpaceTrack.org
     * pass the application Context so we can access the assets folder.
     * Returns whether we have write permission only so that the development method can write the GEO _withTLEs file
     */
    private class ReadSatFilesBackground extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... params) {
            SharedPreferences settings = params[0].getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            //GridSatView will read this and display a Lodaing Satellites message
            editor.putBoolean(PREF_KEY_READING_SATFILES, true).apply();
            // assume we will succeed
            editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, true).apply();
            try {
                // first read-in GEO satellites file with_tles so they show-up first
                if (!readSatFileAndUpdateDB(params[0], "satFiles/GEOsatellites_withTLEs.csv")){
                    editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false).apply();
                }
                //read-in any .csv file in assets folder
                String[] fileList = params[0].getAssets().list("satFiles");
                // test for .csv extension, open files in for loop
                for (String satFileName : fileList) {
                    if (MainActivity.DEBUG) {
                        Log.w(this.getClass().getName(), "adding file " + satFileName + " to dataBase");
                    }
                    if (satFileName.endsWith(".csv") && !satFileName.contains("withTLEs")) {
                        if (!readSatFileAndUpdateDB(params[0], "satFiles/" + satFileName)) {
                            if (MainActivity.DEBUG) {
                                Log.w(this.getClass().getName(), "readSatFile failed: " + satFileName);
                            }
                            // if any SatFile isn't read, set a SharedPref flag that we can easily test in LookAfterStuff to repeat this Task
                            // a file may not be read if the user navigates away while files are being read
                            editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false).apply();
                        }
                    }
                }
                editor.putBoolean(PREF_KEY_READING_SATFILES, false).apply();
            } catch (IOException e) {
                if (MainActivity.DEBUG) {
                    e.printStackTrace();
                }
            }
            return checkSelfPermission(params[0], android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        @Override
        protected void onPostExecute(Boolean canWrite) {
            super.onPostExecute(canWrite);
            if (canWrite) {
                new WriteSatFilesBackground().execute();
            }
        }
    }
    public void readGEOwithTLEsFileFromAssetAsync(Context applicationContext) {
        new ReadGEOwithTLEsFileBackground().execute(applicationContext);
    }

    /**
     * Only called internally from readSatFilesFromAssetAsync()
     * Read satellite files from assets and update the satellite database.
     * When finished, update the Two Line Element sets from SpaceTrack.org
     * pass the application Context so we can access the assets folder.
     * Returns whether we have write permission only so that the development method can write the GEO _withTLEs file
     */
    private class ReadGEOwithTLEsFileBackground extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... params) {
            // first read-in GEO satellites file with_tles so they show-up first
            readSatFileAndUpdateDB(params[0], "satFiles/GEOsatellites_withTLEs.csv");
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (MainActivity.DEBUG) {
                Log.w(this.getClass().getName(), "finished reading GEO_withTLEs file");
            }
        }
    }

    /**
     * After we've read in all the satellite files in the assets folder, write
     * a file of GEO satellite data including the TLEs in a background task, during development
     * This "_withTLEs" file will be added to the assets folder so the free version has GEO satellites with TLEs
     * also write the entire database to a file for error checking during debug
     */
    private class WriteSatFilesBackground extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // as development method save .csv file including TLEs for GEO satellites; set name to GEOsatellites_withTLEs.csv
            if (MainActivity.DEBUG) {
                Log.w(this.getClass().getName(), "writing GEO_withTLEs file");
                newGEOCSVFile("GEOsatellites_withTLEs.csv");
                writeGEOValues(fetchAllDataByKind(GEO));
            }
            if (MainActivity.DEBUG) {
                Log.w(this.getClass().getName(), "writing ALLsatellites_withTLEs file");
                newALLCSVFile("ALLsatellites_withTLEs.csv");
                writeALLValues(fetchAllDataByKind(GEO));
                writeALLValues(fetchAllDataByKind(LEO));
                writeALLValues(fetchAllDataByKind(DEEP));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (myGEOOutWriter != null) {
                try {
                    myGEOOutWriter.close();
                } catch (IOException e) {
                    if (MainActivity.DEBUG) { e.printStackTrace(); }
                }
            }
            if (myALLOutWriter != null) {
                try {
                    myALLOutWriter.close();
                } catch (IOException e) {
                    if (MainActivity.DEBUG) { e.printStackTrace(); }
                }
            }
        }
    }
    private OutputStreamWriter myGEOOutWriter;
    private OutputStreamWriter myALLOutWriter;
    /**
     * Create a new _withTLEs file to be added later to the assets folder. This file contains TLEs
     * for GEO satellites so we don't have to update them in the free version
     */
    private void newGEOCSVFile(String filename) {
        File myFile;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String timeStampDB = sdf.format(cal.getTime());
        myFile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), filename);
        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            myGEOOutWriter = new OutputStreamWriter(fOut);
            myGEOOutWriter.write(timeStampDB);
            myGEOOutWriter.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Create a new "ALLsatellites_withTLEs.csv" file for debugging the database.
     */
    private void newALLCSVFile(String filename) {
        File myFile;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String timeStampDB = sdf.format(cal.getTime());
        myFile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), filename);
        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            myALLOutWriter = new OutputStreamWriter(fOut);
            myALLOutWriter.write(timeStampDB);
            myALLOutWriter.append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write all the data for GEO satellites to the _withTLEs file; development method
     *
     * @param strings all the GEO satellite data in the dB
     */
    private void writeGEOValues(ArrayList<String[]> strings) {
        for (String[] dataString : strings) {
            StringBuilder outStr = new StringBuilder();
            for (String aString : dataString) {
                outStr.append(aString);
                outStr.append("\t");
            }
            try {
                myGEOOutWriter.append(outStr);
                myGEOOutWriter.append("\n");
            } catch (IOException ignored) {
            }
        }
    }
    /**
     * Write all the data for all satellites in the DB, for debugging
     *
     * @param strings all the satellite data in the dB
     */
    private void writeALLValues(ArrayList<String[]> strings) {
        for (String[] dataString : strings) {
            StringBuilder outStr = new StringBuilder();
            for (String aString : dataString) {
                outStr.append(aString);
                outStr.append("\t");
            }
            try {
                myALLOutWriter.append(outStr);
                myALLOutWriter.append("\n");
            } catch (IOException ignored) {
            }
        }
    }
    /**
     * @param query the search text with wildcard characters
     * @return a Cursor containing all the matches
     */
    public Cursor searchDataBaseByName(String query) {
        String selection = DB_KEY_SAT_NAME + " LIKE ? ";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME};
        String[] selectionArgs = new String[] {query};

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(SAT_TABLE);
        Cursor cursor = builder.query(satelliteDB,
                columns, selection, selectionArgs, null, null, DB_KEY_SAT_NAME);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }
    /**
     * @param kind the type of satellite to fetchTLEs for; used to build or rebuild a list of visible satellites
     * @return a ArrayList of all data in a string vector
     */
    private ArrayList<String[]> fetchAllDataByKind(String kind) {
        ArrayList<String[]> returnData = new ArrayList<>();
        String filter = DB_KEY_SAT_KIND + "= '" + kind + "'";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME, DB_KEY_SAT_KIND,
                DB_KEY_SAT_CATEGORY, DB_KEY_SAT_SUB_CATEGORY, DB_KEY_SAT_INFO_LINK, DB_KEY_SAT_BRIEF_DESC,
                DB_KEY_SAT_TLE1, DB_KEY_SAT_TLE2};
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        String[] aDataString = new String[9];
                        aDataString[0] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_KIND));
                        aDataString[1] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_INFO_LINK));
                        aDataString[2] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_CATEGORY));
                        aDataString[3] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_SUB_CATEGORY));
                        aDataString[4] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_NAME));
                        aDataString[5] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_NORAD_NUM));
                        aDataString[6] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_BRIEF_DESC));
                        aDataString[7] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE1));
                        aDataString[8] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE2));
                        returnData.add(aDataString);
                    } while (mCursor.moveToNext());
                } else {
                    if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "no satellites in database");}
                }
            }
        } catch (Exception e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return returnData;
    }
    /**
     * Called from TimerTask LookAfterStuff testTLEsAreCurrent(). In any case, check if the TLEs need to be updated.
     * Since we are using Space-Track's API over Internet do this in a Background Task.
     * Ask user for permission to use Data, if they haven't opted-in; also check for Data connection
     * and warn that TLEs may be out-dated.
     */
    public void updateTLEsAsync(Context applicationContext) {

        new UpdateTLEsBackground().execute(applicationContext);
    }

    private class UpdateTLEsBackground extends AsyncTask<Context, Void, Void> {
        /**
         * 1) query the satDB to get Norad satellite numbers for Two Line Element data sets that are old
         * 2) convert list to a series of limited quantity satellite numbers in comma-delimited format
         * 3) pass this comma-delimited String to a web-site query from Space-Track.org to retrieve TLEs
         * 4) pass an ArrayList of ContentValues to a routine that updates the satDB database with TLEs and a timestamp
         */
        // todo log-in to SpaceTrack, then run series of queries, then log-out; this should speed-up the process, especially for 1st -time start-up
        // todo for 1st time getting TLEs this takes a couple minutes, should not block AsyncTask for this long. Split this up into three calls with Kind as parameter
        @Override
        protected Void doInBackground(Context... params) {
            SharedPreferences settings = params[0].getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            // get LEOs first, since GEOs already have TLEs and don't need updating as often
            ArrayList<Integer> numbersLEO = fetchNoradNumbersToUpdate(LEO);
            if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "LEO noradNumbers fetched " + numbersLEO.size());}
            int numQueries = (numbersLEO.size() / MAX_NUM_TLE_QUERY) + 1;
            // if MainActivity is stopped, but TimerTask is still running, but DB is closed we may think we updated TLEs
            // because we didn't get any TLEs to update. updated will be false if it is closed
            boolean updated = (satelliteDB != null && !isClosed());
            for (int i = 0; i < numQueries; i++) {
                String noradNumberString = convertArrayListToString(numbersLEO, i);
                boolean success = true;
                // if there was nothing to update, String will be ""
                if (!("").equals(noradNumberString)) {
                    success = updateDBTLEs(retrieveTLEs(noradNumberString, LEO));
                    if (MainActivity.DEBUG){
                        if (success) {
                            Log.w(this.getClass().getName(), "successfully updated LEO TLEs");
                        } else {
                            Log.w(this.getClass().getName(), "didn't update LEO TLEs");
                        }
                    }
                }
                updated = updated && success;
            }
            ArrayList<Integer> numbersDEEP = fetchNoradNumbersToUpdate(DEEP);
            if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "DEEP noradNumbers fetched " + numbersDEEP.size());}
            numQueries = (numbersDEEP.size() / MAX_NUM_TLE_QUERY) + 1;
            for (int i = 0; i < numQueries; i++) {
                String noradNumberString = convertArrayListToString(numbersDEEP, i);
                boolean success = true;
                // if there was nothing to update, String will be ""
                if (!("").equals(noradNumberString)) {
                    success = updateDBTLEs(retrieveTLEs(noradNumberString, DEEP));
                    if (MainActivity.DEBUG){
                        if (success) {
                            Log.w(this.getClass().getName(), "successfully updated DEEP TLEs");
                        } else {
                            Log.w(this.getClass().getName(), "didn't update DEEP TLEs");
                        }
                    }
                }
                updated = updated && success;
            }
            ArrayList<Integer> numbersGEO = fetchNoradNumbersToUpdate(GEO);
            if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "GEO noradNumbers fetched " + numbersGEO.size());}
            // how many times do we have to query?
            numQueries = (numbersGEO.size() / MAX_NUM_TLE_QUERY) + 1;
            for (int i = 0; i < numQueries; i++) {
                boolean success = true;
                String noradNumberString = convertArrayListToString(numbersGEO, i);
                // if there was nothing to update, String will be ""
                if (!("").equals(noradNumberString)) {
                    success = updateDBTLEs(retrieveTLEs(noradNumberString, GEO));
                    if (MainActivity.DEBUG){
                        if (success) {
                            Log.w(this.getClass().getName(), "successfully updated GEO TLEs");
                        } else {
                            Log.w(this.getClass().getName(), "didn't update GEO TLEs");
                        }
                    }
                    // if we've received new TLEs set rebuildGEOSatellites = true
                    rebuildGEOSatellites = true;
                }
                updated = updated && success;
            }
            // now if we've succeeded, store the current time in SharedPrefs; we'll test this in LookAfterStuff to see if we have to update TLEs
            if (updated) {
                if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "successfully updated all TLEs");}
                editor.putLong(PREF_KEY_UPDATED_TLES_TIME, System.currentTimeMillis()).apply();
            } else if (MainActivity.DEBUG){
                Log.w(this.getClass().getName(), "didn't update all TLEs db closed? " + (isClosed()?"yes":"no"));
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //TODO
            // assuming we had a data connection before retrieving TLEs, now test for out-of-date TLEs
            // if one is found, it means that it isn't available at Space-Tracks.org
            // just replace the Epoch in our database with the current date so we don't keep looking for it.
        }
    }

    /**
     * After we've retrived the new TLEs from Space-Track in the Background AsyncTask "UpdateTLEsBackground",
     * update the satDB with the new ContentValues for each satellite.
     *
     * @param contentList a List of ContentValues containing new TLE1, TLE2, SAT_NAME, TLE_time-stamp data
     * @return true if all Content was written to the database
     */
    private boolean updateDBTLEs(List<ContentValues> contentList) {
    boolean updated = true;
        for (ContentValues content : contentList) {
            boolean success = updateSatelliteRecord(content.getAsInteger(DB_KEY_NORAD_NUM), content);
            updated = updated && success;
        }
        return updated;
    }

    /**
     * We've found satellites whose data needs to be updated. The numbers are arranged in an ArrayList,
     * because we didn't know how many there might be.
     * The Space-Track API requires these numbers in a comma-delimited String. Also we should only
     * fetch a limited quantity of data at one time from the Space-Tracks web-site. The constant
     * MAX_NUM_TLE_QUERY specifies how much data we should ask for. Don't really know what this should be...
     *
     * @param noradNumbers is an ArrayList of satellite numbers we want to fetch new data
     * @param index        is the starting point in the ArrayList to extract Norad numbers from
     * @return a comma delimited String of Norad numbers
     */
    private String convertArrayListToString(ArrayList<Integer> noradNumbers, int index) {
        int startIndex = index * MAX_NUM_TLE_QUERY;
        int endIndex = (index + 1) * MAX_NUM_TLE_QUERY;
        if (endIndex > noradNumbers.size()) {
            endIndex = noradNumbers.size();
        }
        String noradNumbersString;
        if (endIndex > startIndex) {
            noradNumbersString = Integer.toString(noradNumbers.get(startIndex));
        } else {
            return "";
        }
        for (int j = startIndex + 1; j < endIndex; j++) {
            noradNumbersString = noradNumbersString + "," + noradNumbers.get(j);
        }
        return noradNumbersString;
    }

    /**
     * Using the current time and TLE time-tag column, determine which satellites need to update the TLE
     * A LEO satellite will need to update TLE more often than GEO or DEEP
     *
     * @param kind is the "Kind" column in the satDB
     * @return an ArrayList of Integer Norad numbers with old TLEs
     */
    public ArrayList<Integer> fetchNoradNumbersToUpdate(String kind) {
        // using an ArrayList so we can expand the list as needed
        ArrayList<Integer> noradNumbers = new ArrayList<>();
        // 1) get Cursor containing TLE Epoch from satDB query
        // 2) step thru Cursor and build noradNumber ArrayList if TLE Epoch time-stamp is old
        // 3) release Cursor
        // System.currentTimeMillis() is accurate enough to decide on refreshing TLEs
        long currentTime = System.currentTimeMillis();
        long refreshTime;
        switch (kind) {
            case GEO:
                refreshTime = SIX_WEEKS;
                break;
            case LEO:
                refreshTime = ONE_WEEK;
                break;
            default:
                refreshTime = ONE_WEEK;
                break;
        }
        String filter = DB_KEY_SAT_KIND + "= '" + kind + "'";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_TLE_TIMESTAMP};
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter,  null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_Z_FORMAT, Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String satDbTimeString;
                    long satEpochTimeInMilliseconds = 0;
                    do {
                        satDbTimeString = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE_TIMESTAMP));
                        if (satDbTimeString.equals("0")) {
                            // epoch has never been set, set it to be old
                            satDbTimeString = OLD_DATE;
                        }
                        try {
                            Date mDate = sdf.parse(satDbTimeString);
                            satEpochTimeInMilliseconds = mDate.getTime();
                        } catch (ParseException e) {
                            if (MainActivity.DEBUG) { e.printStackTrace(); }
                        }
                        if (currentTime - refreshTime > satEpochTimeInMilliseconds) {
                            noradNumbers.add(mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_NORAD_NUM)));
                        }
                    } while (mCursor.moveToNext());
                } else {
                    if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "no satellites in database");}
                }
            }
        } catch (Exception e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return noradNumbers;
    }

    /**
     * Given a limited list of Norad numbers of satellites with obsolete data, get the data from Space-Track
     * and assemble JSON Array into ContentValues that can be used to update the satDB database.
     *
     * @param noradNumbers a comma-delimited String containing the satellite Norad numbers to fetch TLE
     * @return ArrayList of ContentValues containing TLEs, the timestamp (Epoch)
     */
    private ArrayList<ContentValues> retrieveTLEs(String noradNumbers, String satelliteKind) {
        // get most recent TLES from SpaceTrack website; parse return to extract TLE1 and TLE2 Strings, add to ContentValues list
        ArrayList<ContentValues> tleContentList = new ArrayList<>();
        if (noradNumbers.length() < 3) {
            // nothing to retrieve
            return tleContentList;
        }
        JSONArray tleJSONArray = getTLEsFromSpaceTrack(noradNumbers, satelliteKind);
        for (int i = 0; i < tleJSONArray.length(); i++) {
            try {
                JSONObject mJSONObj = tleJSONArray.getJSONObject(i);
                ContentValues tleContent = new ContentValues();
                tleContent.put(DB_KEY_NORAD_NUM, mJSONObj.getInt(JSON_KEY_NORAD_CAT_ID));
                tleContent.put(DB_KEY_SAT_TLE_TIMESTAMP, mJSONObj.getString(JSON_KEY_EPOCH));
                // tle0 start with "0 ", extract name starting at index 2
                tleContent.put(DB_KEY_SAT_NAME, mJSONObj.getString(JSON_KEY_TLE_LINE_0).substring(2));
                tleContent.put(DB_KEY_SAT_TLE1, mJSONObj.getString(JSON_KEY_TLE_LINE_1));
                tleContent.put(DB_KEY_SAT_TLE2, mJSONObj.getString(JSON_KEY_TLE_LINE_2));
                tleContentList.add(tleContent);
            } catch (JSONException e) {
                if (MainActivity.DEBUG) { e.printStackTrace(); }
            }
        }
        return tleContentList;
    }

    /**
     * Called from retrieveTLEs().
     * Assembles the query and calls fetchTLEs() in the GetTLEs Class, which does all the HTTP work.
     * The fetchTLEs() method just returns the raw JSONArray received from Space-Track.
     *
     * @param noradNumbers a comma-delimited String of the satellite numbers to retrieve
     * @return a JSON array containing the data
     */
    private JSONArray getTLEsFromSpaceTrack(String noradNumbers, String satelliteKind) {
        String query = "/basicspacedata/query/class/tle_latest/ORDINAL/1/NORAD_CAT_ID/"
                + noradNumbers + "/orderby/NORAD_CAT_ID";
        return jParser.fetchTLEs(query, satelliteKind);
    }

    /**
     * This method is called to handle updating the satellite database by loading satellite file from assets,
     * and calls addSatToDBFromFile with the content. It is called from the AsyncTask ReadSatFilesBackground
     * so this process is handled in a background task.
     *
     * @param mContext    needed to access file from assets
     * @param satFileName the name of the file containing satellite data in a csv format;
     *                    the first line of the file is a document number spec'd as yyyymmdd
     *                    as creation date to indicate new information. The file has "fields" for
     *                    satellite kind, info-link, category, sub-category, name, Norad-number, brief-description
     * @return true if the data base was updated
     */
    private boolean readSatFileAndUpdateDB(Context mContext, String satFileName) {
        ContentValues satContent = new ContentValues();
        InputStream is = null;
        boolean success = true;
        try {
            is = mContext.getAssets().open(satFileName);
        } catch (IOException e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        }
        BufferedReader reader = null;
        if (is != null) {
            reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        }
        String line;
        StringTokenizer st;
        int documentNum = 0;
        try {
            if (reader != null) {
                SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
                //read sharedPrefs for current document num for satFileName file
                int currentDocumentNum = settings.getInt(PREF_KEY_SATFILE_DOC_NUM + satFileName, 0);
                // to force satDB to update from file, set currentDocumentNum to 0; for debugging
                //currentDocumentNum = 0;
                //read first line as document number to compare to database document number
                // if the file doesn't have a valid integer on the first line as the document number
                // catch the error and just set the document number to 0, which will force it to be read
                if ((line = reader.readLine()) != null) {
                    try {
                        documentNum = Integer.parseInt(line.trim());
                    } catch (NumberFormatException e){
                        documentNum = 0;
                    }
                    // if documentNum is same, return
                    if (currentDocumentNum == documentNum) {
                        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "document not new - doc#:" + documentNum + " saved doc#: " + currentDocumentNum);}
                        return true;
                    }
                }
                // loaded in new data, so re-build GEO satellites
                rebuildGEOSatellites = true;
                while ((line = reader.readLine()) != null) {
                   // tab-delimited text file
                    st = new StringTokenizer(line, "\t");
                    satContent.clear();
                    // withTLEs file has different format; read all satContent here
                    if (satFileName.contains(WITH_TLES)) {
                        satContent.put(DB_KEY_SAT_KIND, GEO);
                        satContent.put(DB_KEY_SAT_INFO_LINK, "");
                        satContent.put(DB_KEY_SAT_CATEGORY, st.nextToken());
                        satContent.put(DB_KEY_SAT_SUB_CATEGORY, st.nextToken());
                        satContent.put(DB_KEY_SAT_NAME, st.nextToken());
                        // catch NumberFormatException if a field was missing and Norad number is not a number
                        try {
                            int noradNumber = Integer.valueOf(st.nextToken());
                            satContent.put(DB_KEY_NORAD_NUM, noradNumber);
                        } catch (NumberFormatException e){
                            if (MainActivity.DEBUG) { e.printStackTrace(); }
                            satContent.put(DB_KEY_NORAD_NUM, 0);
                        }
                        satContent.put(DB_KEY_SAT_BRIEF_DESC, STILL_LOADING_SATELLITE_FILES);
                        try {
                            satContent.put(DB_KEY_SAT_TLE1, st.nextToken());
                            satContent.put(DB_KEY_SAT_TLE2, st.nextToken());
                        } catch (NoSuchElementException e) {
                            if (MainActivity.DEBUG) {
                                e.printStackTrace();
                                Log.i(this.getClass().getName(), "no TLEs for Norad# " + satContent.getAsString(DB_KEY_NORAD_NUM));
                            }
                        }
                    } else {
                        // read all other files here
                        String satKind = st.nextToken().replaceAll(DOUBLE_QUOTE, "");
                        satContent.put(DB_KEY_SAT_KIND, satKind);
                        satContent.put(DB_KEY_SAT_INFO_LINK, st.nextToken());
                        satContent.put(DB_KEY_SAT_CATEGORY, st.nextToken().replaceAll(DOUBLE_QUOTE, ""));
                        satContent.put(DB_KEY_SAT_SUB_CATEGORY, st.nextToken().replaceAll(DOUBLE_QUOTE, ""));
                        satContent.put(DB_KEY_SAT_NAME, st.nextToken().replaceAll(DOUBLE_QUOTE, ""));
                        // catch NumberFormatException if a field was missing and Norad number is not a number
                        try {
                            satContent.put(DB_KEY_NORAD_NUM, Integer.valueOf(st.nextToken()));
                        } catch (NumberFormatException e){
                            if (MainActivity.DEBUG) { e.printStackTrace(); }
                            satContent.put(DB_KEY_NORAD_NUM, 0);
                        }
                        // catch NoSuchElementException if there wasn't a description in the file
                        try {
                            satContent.put(DB_KEY_SAT_BRIEF_DESC, st.nextToken().replaceAll(DOUBLE_QUOTE, ""));
                        } catch (NoSuchElementException e) {
                            if (MainActivity.DEBUG) {
                                e.printStackTrace();
                                Log.i(this.getClass().getName(), line);
                            }
                            // if brief description was empty, just add " "
                            satContent.put(DB_KEY_SAT_BRIEF_DESC, " ");
                        }
                    }
                    // when successfully added satellite data, store the document number so we won't read it again
                    success = addSatToDBFromFile(satContent);
                    if (success){
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt(PREF_KEY_SATFILE_DOC_NUM + satFileName, documentNum).apply();
                    }
                }
            }
        } catch (IOException e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        }
        return success;
    }

    /**
     * Read the data pertaining to a particular noradNum
     * Include the satellite name, brief description and link to add content to popup window
     * todo include satellite category for displaying a different icon
     *
     * @param noradNum the  number of the satellite to read
     * @return a String[] containing the data
     */
    public String[] fetchDeviceData(long noradNum) {
        String[] returnString = new String[3];
        String[] columns = {DB_KEY_SAT_NAME, DB_KEY_SAT_BRIEF_DESC, DB_KEY_SAT_INFO_LINK};
        String filter = DB_KEY_NORAD_NUM + "= '" + Integer.toString((int) noradNum) + "'";
        Cursor mCursor = null;
        if (satelliteDB != null && !isClosed()) {
            try {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        returnString[0] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_NAME));
                        returnString[1] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_BRIEF_DESC));
                        returnString[2] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_INFO_LINK)).replace(DOUBLE_QUOTE, "");
                    } while (mCursor.moveToNext());
                }
            } catch (Exception e) {
                if (MainActivity.DEBUG) {e.printStackTrace();}
            } finally {
                if (mCursor != null) {
                    mCursor.close();
                }
            }
        }
        return returnString;
    }

    /**
     * Determines if a satellite is stored in the data base
     *
     * @param noradNumber the satellite in question
     * @return true if that satellite is in the data base
     */
    private boolean isSatelliteInDataBase(int noradNumber) {
        boolean found = false;
        if (satelliteDB != null && !isClosed()) {
            String devNumFilter = DB_KEY_NORAD_NUM + "= '" + Integer.toString(noradNumber) + "'";
            String[] columns = {DB_KEY_NORAD_NUM};
            Cursor mCursor = null;
            try {
                mCursor = satelliteDB.query(SAT_TABLE, columns, devNumFilter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    int devNum;
                    do {
                        devNum = mCursor.getInt(mCursor.getColumnIndexOrThrow(DB_KEY_NORAD_NUM));
                        found = (devNum == noradNumber);
                    } while (mCursor.moveToNext());
                }
            } catch (Exception e) {
                if (MainActivity.DEBUG) {e.printStackTrace();}
            } finally {
                if (mCursor != null) {
                    mCursor.close();
                }
            }
        }
        return found;
    }

    /**
     * When loading file of satellites add satellite to the data base if its not already there. If
     * satellite is already in the database, this must be new information because the document number is different,
     * so replace the information. (TLEs and Epoch time-stamp are retained)
     *
     * @param content has the satellite kind, moreInfoLink, satellite category,
     *                satellite sub-category, satellite name, Norad number, and brief description
     */
    private boolean addSatToDBFromFile(ContentValues content) {
        int noradNum = content.getAsInteger(DB_KEY_NORAD_NUM);
        boolean returnSuccess = true;
        try {
            if (satelliteDB != null && !isClosed()) {
                // see if this noradNum is already in dB
                boolean inDB = isSatelliteInDataBase(noradNum);
                if (!inDB) {
                    // if this satellite is not from file "withTLEs" it doesn't have TLEs, so initialize the content
                    if (content.getAsString(DB_KEY_SAT_TLE1) == null) {
                        // initialize all the DB columns not specified in the satellite file
                        content.put(DB_KEY_SAT_TLE1, "");
                        content.put(DB_KEY_SAT_TLE2, "");
                    }
                    content.put(DB_KEY_SAT_TLE_TIMESTAMP, 0);
                    // (OTHER field is not used as of now)
                    content.put(DB_KEY_OTHER, "");
                    long rowID = satelliteDB.insertWithOnConflict(
                            SAT_TABLE, "", content,
                            SQLiteDatabase.CONFLICT_IGNORE);
                } else if (content.getAsString(DB_KEY_SAT_TLE1) == null) {
                    // satellite is already in database and new data is not from a withTLEs file
                    // update content because file has new information
                    // we've checked that it's a new file before calling this method
                    // we should load withTLEs file first just to have satellite position to display
                    // if this satellite is already in database it may have TLEs already. No need to update.
                    String[] whereArgs = {String.valueOf(noradNum)};
                    satelliteDB.update(SAT_TABLE, content, DB_KEY_NORAD_NUM + "=?", whereArgs);
                }
            } else {
                // database was closed; maybe user navigated away before finishing reading satellite documents
                // this will reset SharedPrefs document number so it will load again
                returnSuccess = false;
                if (MainActivity.DEBUG) {
                    Log.w(this.getClass().getName(), "couldn't add to DB for deviceNum = "
                            + content.getAsInteger(DB_KEY_NORAD_NUM)
                            + ((satelliteDB == null) ? "satDB is null" : "satDB is closed"));
                }
            }
        } catch (IllegalStateException ignored) {
        }
        return returnSuccess;
    }

    /**
     * When retreiving TLE or calculating new information, update the data base
     *
     * @param noradNum the satellite number of the new information
     * @param content  an Object containing the new information
     * @return true if the update occurred
     */
    public boolean updateSatelliteRecord(int noradNum, ContentValues content) {
        String[] whereArgs = {String.valueOf(noradNum)};
        boolean success = true;
        try {
            if (satelliteDB != null && satelliteDB.isOpen()) {
                satelliteDB.update(SAT_TABLE, content, DB_KEY_NORAD_NUM + "=?", whereArgs);
            } else {
                success = false;
                if (MainActivity.DEBUG) {
                    Log.w(this.getClass().getName(), "couldn't update DB for noradNum = " + noradNum);
                }
            }
        } catch (SQLException e1) {
            if (MainActivity.DEBUG) {e1.printStackTrace();}
            success = false;
        }
        return success;
    }

    public class DBHelper extends SQLiteOpenHelper {
		static final String TEXT_NOT_NULL = " TEXT NOT NULL";
		static final String INTEGER_NOT_NULL = " INTEGER NOT NULL";
		static final String INTEGER_UNIQUE = " INTEGER UNIQUE";
		private static final int DB_VERSION = 1;

		public DBHelper(Context context) {
			super(context, SAT_DB, null, DB_VERSION);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			String dropString = "DROP TABLE IF EXISTS satTable;";
			db.execSQL(dropString);
			onCreate(db);
		}

		@Override
		public void onCreate(SQLiteDatabase db) throws SQLException {
			String createString = "CREATE TABLE IF NOT EXISTS satTable "
					+ "( _id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ DB_KEY_NORAD_NUM + INTEGER_UNIQUE + ", "
					+ DB_KEY_SAT_NAME + TEXT_NOT_NULL + ", "
					+ DB_KEY_SAT_KIND + TEXT_NOT_NULL + ", "
					+ DB_KEY_SAT_CATEGORY + TEXT_NOT_NULL + ", "
					+ DB_KEY_SAT_SUB_CATEGORY + TEXT_NOT_NULL + ", "
					+ DB_KEY_SAT_INFO_LINK + TEXT_NOT_NULL + ", "
					+ DB_KEY_SAT_BRIEF_DESC + TEXT_NOT_NULL + ", "
					+ DB_KEY_SAT_TLE1 + TEXT_NOT_NULL + ", "
					+ DB_KEY_SAT_TLE2 + TEXT_NOT_NULL + ", "
					+ DB_KEY_SAT_TLE_TIMESTAMP + INTEGER_NOT_NULL + ", "
					+ DB_KEY_OTHER + TEXT_NOT_NULL + ");";
			db.execSQL(createString);
		}
	}

    /**
     * This call creates a new DBHelper, creates the satDB if it doesn't exist and opens a writeable database instance.
     * Should be called in MainActivity.onCreate() before trying to use the database.
     *
     * @return a SATDBAdapter
     * @throws SQLiteException
     */
    public SATDBAdapter open() throws SQLiteException {
        mDBHelper = new DBHelper(mContext);
        satelliteDB = mDBHelper.getWritableDatabase();
        return this;
    }
    /**
     * This call creates a new DBHelper, creates the satDB if it doesn't exist and opens a writeable database instance.
     * Should be called in MainActivity.onCreate() before trying to use the database.
     *
     * @return a SATDBAdapter
     * @throws SQLiteException
     */
    public SATDBAdapter openAsReadable() throws SQLiteException {
        mDBHelper = new DBHelper(mContext);
        satelliteDB = mDBHelper.getReadableDatabase();
        return this;
    }
    /**
     * close the satTable database
     */
    public void close() {
        try {
            if (mDBHelper != null) {
                mDBHelper.close();
            }
        } catch (IllegalStateException e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        }
    }

	public boolean isClosed() {
		return satelliteDB == null || !satelliteDB.isOpen();
	}

}
