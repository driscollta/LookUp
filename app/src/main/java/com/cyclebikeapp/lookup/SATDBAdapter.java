package com.cyclebikeapp.lookup;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.cyclebikeapp.lookup.Constants.CDEEP;
import static com.cyclebikeapp.lookup.Constants.CELESTRAK_STATUS_WEBSITE;
import static com.cyclebikeapp.lookup.Constants.CGEO;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_NORAD_NUM;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_OTHER;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_BRIEF_DESC;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_CATEGORY;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_INFO_LINK;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_KIND;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_NAME;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_SUB_CATEGORY;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_TLE1;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_TLE2;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_TLE_TIMESTAMP;
import static com.cyclebikeapp.lookup.Constants.DEEP;
import static com.cyclebikeapp.lookup.Constants.DOUBLE_QUOTE;
import static com.cyclebikeapp.lookup.Constants.FUTURE_EPOCH;
import static com.cyclebikeapp.lookup.Constants.GEO;
import static com.cyclebikeapp.lookup.Constants.LEO;
import static com.cyclebikeapp.lookup.Constants.PREFS_NAME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_ASSET_TLES_READ_SUCCESS;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_READING_SATFILES;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_SATFILES_READ_SUCCESS;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_SATFILE_DOC_NUM;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATED_CELESTRAK_STATUS_TIME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATED_CELESTRAK_TLES_TIME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATED_MCCANTS_TLES_TIME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATING_STATUS;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATING_TLES;
import static com.cyclebikeapp.lookup.Constants.SAT_DB;
import static com.cyclebikeapp.lookup.Constants.SAT_TABLE;
import static com.cyclebikeapp.lookup.Constants.STILL_LOADING_SATELLITE_FILES;
import static com.cyclebikeapp.lookup.Constants.STRING_ZERO;
import static com.cyclebikeapp.lookup.Constants.WITH_TLES;
import static com.cyclebikeapp.lookup.Constants.mccantsFile_classfd_zip;
import static com.cyclebikeapp.lookup.Constants.mccantsFile_inttles_zip;
import static com.cyclebikeapp.lookup.Constants.mccantsURL1;
import static com.cyclebikeapp.lookup.Constants.mccantsURL2;
import static com.cyclebikeapp.lookup.Constants.planet_labs;
import static com.cyclebikeapp.lookup.Constants.tleInfoURL1;
import static com.cyclebikeapp.lookup.Constants.tleinfo_zip;
import static com.cyclebikeapp.lookup.Util.deleteAllCacheFolderFiles;
import static com.cyclebikeapp.lookup.Util.deleteCacheFolderFile;
import static com.cyclebikeapp.lookup.Util.hasStoragePermission;
import static com.cyclebikeapp.lookup.Util.limitSatelliteNameLength;

class SATDBAdapter {
    private final String[] tleAssetFileList;
    private final Context mContext;
    private DBHelper mDBHelper;
    private SQLiteDatabase satelliteDB;
    private final GetTLEsFromCelestrak mCGetter;
    private final GetSatelliteStatusFromCelestrak mCSSGetter;

    // we only build GEO satellites when loading a new data file, updating TLEs or receiveing a new Location
    private boolean rebuildGEOSatellites;
    private boolean rebuildCGEOSatellites;
    private final String[] celestrakURLList;

    SATDBAdapter(Context context) {
        this.mContext = context;
        mCGetter = new GetTLEsFromCelestrak();
        mCSSGetter = new GetSatelliteStatusFromCelestrak();
        rebuildGEOSatellites = false;
        tleAssetFileList = new String[]{Constants.SAT_FILES_CLASSFD_TLE, Constants.SAT_FILES_INTTLES_TLE,
                Constants.SAT_FILES_SPACETRACK_COMPLETE_TLES_1, Constants.SAT_FILES_SPACETRACK_COMPLETE_TLES_2,
                Constants.SAT_FILES_SPACETRACK_COMPLETE_TLES_3, Constants.SAT_FILES_SPACETRACK_COMPLETE_TLES_4};
        celestrakURLList = new String[]{Constants.celestrakURL1, Constants.celestrakURL2, Constants.celestrakURL3, Constants.celestrakURL4, Constants.celestrakURL5,
                Constants.celestrakURL6, Constants.celestrakURL7, Constants.celestrakURL8, Constants.celestrakURL9, Constants.celestrakURL10,
                Constants.celestrakURL11, Constants.celestrakURL12, Constants.celestrakURL13, Constants.celestrakURL14, Constants.celestrakURL15,
                Constants.celestrakURL16, Constants.celestrakURL17, Constants.celestrakURL18, Constants.celestrakURL19, Constants.celestrakURL20,
                Constants.celestrakURL21, Constants.celestrakURL22, Constants.celestrakURL23, Constants.celestrakURL24, Constants.celestrakURL25,
                Constants.celestrakURL26, Constants.celestrakURL27, Constants.celestrakURL28, Constants.celestrakURL29, Constants.celestrakURL30,
                Constants.celestrakURL31, Constants.celestrakURL32, Constants.celestrakURL33, Constants.celestrakURL34, Constants.celestrakURL35,
                Constants.celestrakURL36, Constants.celestrakURL37, Constants.celestrakURL38, Constants.celestrakURL39, Constants.celestrakURL40,
                Constants.celestrakURL41, Constants.celestrakURL42, Constants.celestrakURL43, Constants.celestrakURL44, Constants.celestrakURL45,
                Constants.celestrakURL46, Constants.celestrakURL47, Constants.celestrakURL48, Constants.celestrakURL49, Constants.celestrakURL50,
                Constants.amsat_bare};
    }

    /**
     * Used in buildSatellitesByKind(), returns all TLEs of the specified Kind from the dataBase
     *
     * @param noradNumber the satellite number
     * @return a TLEs in a string vector
     */
    String[] fetchTLEsByNoradNumber(int noradNumber) {
        String[] aTLEString = new String[5];
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME, DB_KEY_SAT_TLE1, DB_KEY_SAT_TLE2, DB_KEY_SAT_CATEGORY, DB_KEY_SAT_SUB_CATEGORY};
        String filter = DB_KEY_NORAD_NUM + "= '" + Integer.toString(noradNumber) + "'";
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        aTLEString[0] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_NAME));
                        aTLEString[1] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE1));
                        aTLEString[2] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE2));
                        aTLEString[3] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_CATEGORY));
                        aTLEString[4] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_SUB_CATEGORY));
                        // if we haven't yet gotten TLEs from Space-Track since loading the satellite file...
                    } while (mCursor.moveToNext());
                } else {
                    if (MainActivity.DEBUG) { Log.w(this.getClass().getName(), "no satellites in database"); }
                }
            }
        } catch (Exception e) {
            if (MainActivity.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return aTLEString;
    }

    /**
     * Used in buildSatellitesByKind(), returns all TLEs of the specified Kind from the dataBase
     *
     * @param kind the type of satellite to fetchTLEs for; used to build or rebuild a list of visible satellites
     * @return a List of TLEs in a string vector
     */
    ArrayList<String[]> fetchTLEsByKind(String kind) {
        ArrayList<String[]> returnTLEs = new ArrayList<>();
        String filter = DB_KEY_SAT_KIND + "= '" + kind + "'";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME, DB_KEY_SAT_TLE1, DB_KEY_SAT_TLE2, DB_KEY_SAT_CATEGORY, DB_KEY_SAT_SUB_CATEGORY};
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                satelliteDB.beginTransaction();
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                satelliteDB.setTransactionSuccessful();
                satelliteDB.endTransaction();
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        String[] aTLEString = new String[5];
                        aTLEString[0] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_NAME));
                        aTLEString[1] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE1));
                        aTLEString[2] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE2));
                        aTLEString[3] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_CATEGORY));
                        aTLEString[4] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_SUB_CATEGORY));
                        // if we haven't yet gotten TLEs from Space-Track since loading the satellite file...
                        if (!aTLEString[1].equals("") && !aTLEString[2].equals("")) {
                            returnTLEs.add(aTLEString);
                        }
                    } while (mCursor.moveToNext());
                } else {
                    if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "no satellites in database"); }
                }
            }
        } catch (Exception e) {
            if (MainActivity.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return returnTLEs;
    }

    boolean shouldRebuildGEOSatellites() {
        return rebuildGEOSatellites;
    }

    void setRebuildGEOSatellites(boolean rebuild) {
        rebuildGEOSatellites = rebuild;
    }

    boolean shouldRebuildCGEOSatellites() {
        return rebuildCGEOSatellites;
    }

    void setRebuildCGEOSatellites(boolean rebuild) {
        rebuildCGEOSatellites = rebuild;
    }


    /**
     * This is the primary call from onCreate() in MainActivity after openning the database. From here the
     * asset satellite data files are loaded, database updated and TLEs refreshed, all in Background Tasks.
     *
     */
    void readSatFilesFromAssetAsync() {
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        //GridSatView will read this and display a Loading Satellites message
        editor.putBoolean(PREF_KEY_READING_SATFILES, true).apply();
        SerialExecutor readSatFilesFromAssetEx = new SerialExecutor();
        readSatFilesFromAssetEx.execute(readSatFilesFromAssetsRunnable);
        // don't read-in the tles until the sat files have been read
        readSatFilesFromAssetEx.execute(readTLEFromAssetRunnable);
    }

    void readGEOwithTLEsFileFromAsset(Context applicationContext) {
        // first read-in GEO satellites file with_tles so they show-up first
        if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "loading GEOsatellites_withTLEs");}
        readSatFileAndUpdateDB(applicationContext, "satFiles/GEOsatellites_withTLEs.csv");
    }
    /**
     * After we've read in all the satellite files in the assets folder, write
     * a file of GEO satellite data including the TLEs in a background task, during development
     * This "_withTLEs" file will be added to the assets folder so the free version has GEO satellites with TLEs
     * also write the entire database to a file for error checking during debug
     */
    private class WriteTLEsBackground extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // as development method save .csv file including TLEs for GEO satellites;
            if (MainActivity.WRITE_ALLTLES && hasStoragePermission(mContext)) {
                Log.w(this.getClass().getName(), "writing AllTLEs file");
                newALLTLECSVFile("AllTLEs.csv");
                writeALLTLEs(fetchAllTLEs());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (myALLOutWriter != null) {
                try {
                    myALLOutWriter.close();
                } catch (IOException e) {
                    if (MainActivity.DEBUG) {
                        e.printStackTrace();
                    }
                }
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
                //only fetch active in weather, tv, radio categories
                writeGEOValues(fetchGEOwithTLEData());
            }
            if (MainActivity.WRITE_ALLSATSFILE) {
                Log.w(this.getClass().getName(), "writing ALLsatellites_withTLEs file");
                newALLCSVFile("ALLsatellites_withTLEs.csv");
                writeALLValues(fetchAllDataByKind(GEO));
                writeALLValues(fetchAllDataByKind(LEO));
                writeALLValues(fetchAllDataByKind(DEEP));
                writeALLValues(fetchAllDataByKind(CGEO));
                writeALLValues(fetchAllDataByKind(CDEEP));
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
                    if (MainActivity.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
            if (myALLOutWriter != null) {
                try {
                    myALLOutWriter.close();
                } catch (IOException e) {
                    if (MainActivity.DEBUG) {
                        e.printStackTrace();
                    }
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
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
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
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
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
     * Create a new "ALLTLEs.csv" file for debugging the database.
     */
    private void newALLTLECSVFile(String filename) {
        File myFile;
        myFile = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), filename);
        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            myALLOutWriter = new OutputStreamWriter(fOut);

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
     * Write all the data for all satellites in the DB, for debugging
     *
     * @param strings all the satellite data in the dB
     */
    private void writeALLTLEs(ArrayList<String[]> strings) {
        for (String[] dataString : strings) {
            try {
                for (String aString : dataString) {
                    myALLOutWriter.append(aString);
                    myALLOutWriter.append("\n");
            }
            } catch (IOException ignored) {
            }
        }
    }
    /**
     * @param query the search text of a Norad Number
     * @return a Cursor containing the exact match; could be null
     */
    Cursor searchDataBaseByNoradNumber(String query) {
        String selection = DB_KEY_NORAD_NUM + " = ? ";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME};
        String[] selectionArgs = new String[]{query};

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(SAT_TABLE);
        Cursor cursor = builder.query(satelliteDB, columns, selection, selectionArgs, null, null, null);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    /**
     * @param query the search text of a Int'l Code in the Other column
     * @return a Cursor containing the exact match; could be null
     */
    Cursor searchDataBaseByIntlCode(String query) {
        String selection = DB_KEY_OTHER + " = ? ";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME};
        String[] selectionArgs = new String[]{query};

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(SAT_TABLE);
        Cursor cursor = builder.query(satelliteDB, columns, selection, selectionArgs, null, null, null);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    /**
     * @param query the search text of a Int'l Code in the Other column, containing a wildcard character at the end
     * @return a Cursor containing the exact match; could be null
     */
    Cursor searchDataBaseByYear(String query) {
        String selection = DB_KEY_OTHER + " LIKE ? ";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME};
        String[] selectionArgs = new String[]{query};

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(SAT_TABLE);
        String orderBy = DB_KEY_NORAD_NUM;
        Cursor cursor = builder.query(satelliteDB, columns, selection, selectionArgs, null, null, orderBy);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    /**
     * @param query the search text with wildcard characters
     * @return a Cursor containing all the matches; could be null
     */
    Cursor searchDataBaseByName(String query) {
        String alternateNameQuery = "%(" + query + "%";
        String selection = DB_KEY_SAT_NAME + " LIKE ? OR " + DB_KEY_SAT_NAME + " LIKE ?";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME};
        String[] selectionArgs = new String[]{query, alternateNameQuery};
        String orderBy = DB_KEY_SAT_NAME;
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(SAT_TABLE);
        Cursor cursor = builder.query(satelliteDB, columns, selection, selectionArgs, null, null, orderBy);
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    /**
     * Fetch active GEO satellite data to write the file GEO_withTLEs
     *
     * @return a ArrayList of all data in a string vector
     */
    private ArrayList<String[]> fetchGEOwithTLEData() {
        ArrayList<String[]> returnData = new ArrayList<>();
        String filter = DB_KEY_SAT_KIND + "= '" + GEO + "'";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_NAME,
                DB_KEY_SAT_CATEGORY, DB_KEY_SAT_SUB_CATEGORY,
                DB_KEY_SAT_TLE1, DB_KEY_SAT_TLE2};
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        String[] aDataString = new String[columns.length];
                        //only add sub_category = "active" satellites
                        String satCategory = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_CATEGORY));
                        boolean allowedCategory = satCategory.toUpperCase().contains("TV")
                                || satCategory.toUpperCase().contains("WEATHER")
                                || satCategory.toUpperCase().contains("RADIO");
                        boolean satIsActive = ("ACTIVE").equals(mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_SUB_CATEGORY)).toUpperCase().trim());
                        if (satIsActive && allowedCategory) {
                            aDataString[0] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_CATEGORY));
                            aDataString[1] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_SUB_CATEGORY));
                            aDataString[2] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_NAME));
                            aDataString[3] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_NORAD_NUM));
                            aDataString[4] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE1));
                            aDataString[5] = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE2));
                            returnData.add(aDataString);
                        }
                    } while (mCursor.moveToNext());
                } else {
                    if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "no satellites in database");}
                }
            }
        } catch (Exception e) {
            if (MainActivity.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return returnData;
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
                DB_KEY_SAT_TLE1, DB_KEY_SAT_TLE2, DB_KEY_OTHER};
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                satelliteDB.beginTransaction();
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                satelliteDB.setTransactionSuccessful();
                satelliteDB.endTransaction();
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        String[] aDataString = new String[columns.length];
                        aDataString[0] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_KIND));
                        aDataString[1] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_INFO_LINK));
                        aDataString[2] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_CATEGORY));
                        aDataString[3] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_SUB_CATEGORY));
                        aDataString[4] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_NAME));
                        aDataString[5] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_NORAD_NUM));
                        aDataString[6] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_BRIEF_DESC));
                        aDataString[7] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE1));
                        aDataString[8] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE2));
                        aDataString[9] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_OTHER));
                        returnData.add(aDataString);
                    } while (mCursor.moveToNext());
                } else {
                    if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "no satellites in database");}
                }
            }
        } catch (Exception e) {
            if (MainActivity.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return returnData;
    }

    private ArrayList<String[]> fetchAllTLEs() {
        ArrayList<String[]> returnData = new ArrayList<>();
        String[] columns = {DB_KEY_SAT_NAME, DB_KEY_SAT_TLE1, DB_KEY_SAT_TLE2};
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                satelliteDB.beginTransaction();
                mCursor = satelliteDB.query(SAT_TABLE, columns, null, null, null, null, null);
                satelliteDB.setTransactionSuccessful();
                satelliteDB.endTransaction();
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        String[] aDataString = new String[columns.length + 1];
                        aDataString[0] = "0 " + mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_NAME));
                        aDataString[1] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE1));
                        aDataString[2] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE2));
                        returnData.add(aDataString);
                    } while (mCursor.moveToNext());
                } else {
                    if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "no satellites in database");}
                }
            }
        } catch (Exception e) {
            if (MainActivity.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            if (mCursor != null) {
                mCursor.close();
            }
        }
        return returnData;
    }

    /**
     * This is only done in the Paid Version. testTLEsAreCurrent() returns for Free Version
     * First read Celestrack pages and update TLEs, then run UpdateTLEsFromSpaceTrackBackground() to get TLEs from SpaceTrack
     * Called from TimerTask LookAfterStuff testTLEsAreCurrent(). In any case, check if the TLEs need to be updated.
     * Since we are using Space-Track's API over Internet do this in a Background Task.
     * Ask user for permission to use Data, if they haven't opted-in; also check for Data connection
     * and warn that TLEs may be out-dated.
     */
    void updateTLEsAsync(Context applicationContext) {
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREF_KEY_UPDATING_TLES, true).apply();
        readMcCantsTLEsToCache(applicationContext);
        SerialExecutor updateTLEs = new SerialExecutor();
        updateTLEs.execute(updateTLEsFromCelestrak);
        editor.putBoolean(PREF_KEY_UPDATING_STATUS, true).apply();
        updateTLEs.execute(updateSatelliteStatusFromCelestrak);
        if (MainActivity.WRITE_ALLSATSFILE){updateTLEs.execute(writeAllSatellites2File);}
        if (MainActivity.WRITE_ALLTLES){updateTLEs.execute(writeAllTLEs2File);}

        if (MainActivity.DEBUG){
            Log.i(this.getClass().getName(), "after updating TLEs, still have "
                    + howManyZeroEpochSatellites(GEO) + " 0-Epoch GEOs "
                    + howManyZeroEpochSatellites(LEO) + " 0-Epoch LEOs "
                    + howManyZeroEpochSatellites(DEEP) + " 0-Epoch DEEPs "
                    + howManyZeroEpochSatellites(CDEEP) + " 0-Epoch CDEEPs "
                    + howManyZeroEpochSatellites(CGEO) + " 0-Epoch CGEOs"
            );

        }
    }

    private void readMcCantsTLEsToCache(Context applicationContext) {
        // 1) only read once per day via SharedPref timetag
        // 2) return if not Paid Version or if no internet
        // 3) compose filename for the two McCant's and TLE.info files and execute the Background AsyncTask
        if (!Util.checkMcCantsTLEsNeedUpdating(applicationContext.getSharedPreferences(PREFS_NAME, 0))) {
            return;
        }
        SerialExecutor downloadMcCants = new SerialExecutor();
        downloadMcCants.execute(downloadMcCantsFilesRunnable);
        downloadMcCants.execute(unPackCacheZipsRunnable);
    }

    private Runnable downloadMcCantsFilesRunnable = new Runnable() {
        /**
         * Read McCants tle files from assets and update the satellite database.
         * Have this in case McCant's URLs are dead we'll still have some TLEs.
         * Pass the application Context so we can access the assets folder.
         * Return if we've read the files once and noted it in the SharedPref.
         * On success, set the SharedPref to true
         */
        @Override
        public void run() {
            int count;
            File cDir = mContext.getCacheDir();
            if (deleteAllCacheFolderFiles(mContext)){
                if (MainActivity.DEBUG) { Log.w(this.getClass().getName(), "deleted all cache files"); }
            }
            String[] f_urls = {mccantsURL1, mccantsURL2, tleInfoURL1, planet_labs};
            String[] tempFileCacheNames = {mccantsFile_inttles_zip, mccantsFile_classfd_zip, tleinfo_zip, "planet_mc.tle"};
            for (int i = 0; i < f_urls.length; i++) {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(f_urls[i]);
                    connection = (HttpURLConnection) url.openConnection();
                    //connection.connect();
                    // download the file
                    InputStream input = new BufferedInputStream(connection.getInputStream());
                    OutputStream output = new FileOutputStream(cDir.getPath() + "/" + tempFileCacheNames[i]);
                    byte data[] = new byte[1024];
                    while ((count = input.read(data)) != -1) {
                        // writing data to file
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "successfully read McCants TLEs from " + f_urls[i]);}
            }
        }

    };

    private Runnable unPackCacheZipsRunnable = new Runnable() {
        /**
         * Read McCants tle files from cache and update the satellite database.
         */
        @Override
        public void run() {
            String root = mContext.getCacheDir().getPath();
            String[] tempFileCacheNames = mContext.getCacheDir().list();
            for (String tempFileCacheName : tempFileCacheNames) {
                long startTime = System.currentTimeMillis();
                InputStream is;
                ZipInputStream zis;
                String filename = null;
                if (!tempFileCacheName.contains("planet")) {
                    try {
                        is = new FileInputStream(root + "/" + tempFileCacheName);
                        zis = new ZipInputStream(new BufferedInputStream(is));
                        ZipEntry zipEntry;
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((zipEntry = zis.getNextEntry()) != null) {
                            filename = zipEntry.getName();
                            // Need to create directories if not exists, or it will generate an Exception...
                            if (zipEntry.isDirectory()) {
                                File fmd = new File(root + "/" + filename);
                                fmd.mkdirs();
                                continue;
                            }
                            FileOutputStream fout = new FileOutputStream(root + "/" + filename);
                            while ((count = zis.read(buffer)) != -1) {
                                fout.write(buffer, 0, count);
                            }
                            fout.close();
                            zis.closeEntry();
                        }
                        zis.close();
                        if (deleteCacheFolderFile(root + "/" + tempFileCacheName)) {
                            if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "deleted cache file: " + tempFileCacheName);}
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                } else {
                    filename = tempFileCacheName;
                }
                // now update database with McCants tles for this filename using readTLEsFromFile(inputstream)
                ArrayList<ContentValues> tleContentList = new ArrayList<>();
                FileInputStream fis = null;
                try {
                    if (filename == null) {
                        return;
                    }
                    // the filename should be the one in the .zip archive
                    fis = new FileInputStream(root + "/" + filename);
                    boolean readName = false;
                    tleContentList.addAll(Util.readTLEsFromFile(fis, 3, readName));
                    if (MainActivity.DEBUG) {
                        Log.w(this.getClass().getName(), "found " + tleContentList.size()
                                + "  " + filename + " tles to update from McCants");
                    }
                    boolean success = updateDBTLEs(tleContentList);
                    deleteCacheFolderFile(root + "/" + filename);
                    if (success) {
                        rebuildCGEOSatellites = true;
                        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
                        settings.edit().putLong(PREF_KEY_UPDATED_MCCANTS_TLES_TIME, System.currentTimeMillis()).apply();
                        if (MainActivity.DEBUG) {
                            Log.w(this.getClass().getName(), "successfully updated McCants TLEs " + tempFileCacheName);
                        }
                    } else {
                        if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "didn't update McCants TLEs");}
                    }
                    if (MainActivity.DEBUG) {
                        Log.w(this.getClass().getName(), "updating McCants TLEs took "
                                + String.format("%3.1f", (System.currentTimeMillis() - startTime) / 1000.) + " sec");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        }
    };
    private Runnable writeAllSatellites2File = new Runnable() {

        @Override
        public void run() {
            new WriteSatFilesBackground().execute();
        }
    };
    private Runnable writeAllTLEs2File = new Runnable() {

        @Override
        public void run() {
            new WriteTLEsBackground().execute();
        }
    };
    private Runnable readTLEFromAssetRunnable = new Runnable() {
        /**
         * Read tle files from assets and update the satellite database.
         * Have this in case URLs are dead we'll still have some TLEs.
         * Need the application Context so we can access the assets folder.
         * Return if we've read the files once and noted it in the SharedPref.
         * On success, set the SharedPref to true
         */
        @Override
        public void run() {
            boolean updated = (satelliteDB != null && !isClosed());
            SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            if (settings.getBoolean(PREF_KEY_ASSET_TLES_READ_SUCCESS, false)) {return;}
            //GridSatView will read this and display a Loading Satellites message
            editor.putBoolean(PREF_KEY_UPDATING_TLES, true).apply();
            InputStream is;
            boolean readName = false;
            ArrayList<ContentValues> tleContentList = new ArrayList<>();
            try {
                for (String file:tleAssetFileList) {
                    is = mContext.getAssets().open(file);
                    tleContentList.addAll(Util.readTLEsFromFile(is, 3, readName));
                    if (MainActivity.DEBUG) {
                        Log.w(this.getClass().getName(), "reading " + file
                                + " tle asset file, found " + tleContentList.size() + " tles to update");
                    }
                    updated = updated && updateDBTLEs(tleContentList);
                }
            } catch (IOException e) {
                editor.putBoolean(PREF_KEY_ASSET_TLES_READ_SUCCESS, false).apply();
                if (MainActivity.DEBUG) {e.printStackTrace();}
            } finally {
                settings.edit().putBoolean(PREF_KEY_UPDATING_TLES, false).apply();
            }
            if (updated) {
                rebuildCGEOSatellites = true;
                rebuildGEOSatellites = true;
                settings.edit().putBoolean(PREF_KEY_ASSET_TLES_READ_SUCCESS, true).apply();
            }
        }
    };

    private Runnable readSatFilesFromAssetsRunnable = new Runnable() {
        /**
         * Only called internally from readSatFilesFromAssetAsync()
         * Read satellite files from assets and update the satellite database.
         * When finished, update the Two Line Element sets from SpaceTrack.org
         * pass the application Context so we can access the assets folder.
         * Returns whether we have write permission only so that the development method can write the GEO _withTLEs file
         */
        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            // assume we will succeed
            editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, true).apply();
            try {
                // first read-in GEO satellites file with_tles so they show-up first
                if (!readSatFileAndUpdateDB(mContext, "satFiles/GEOsatellites_withTLEs.csv")) {
                    editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false).apply();
                }
                // read-in any .csv file in assets folder
                String[] fileList = mContext.getAssets().list("satFiles");
                // test for .csv extension, open files in for loop
                for (String satFileName : fileList != null ? fileList : new String[0]) {
                    if (MainActivity.DEBUG) {
                        Log.w(this.getClass().getName(), "adding file " + satFileName + " to dataBase");
                    }
                    if (satFileName.endsWith(".csv") && !satFileName.contains("withTLEs")) {
                        if (!readSatFileAndUpdateDB(mContext, "satFiles/" + satFileName)) {
                            if (MainActivity.DEBUG) {
                                Log.w(this.getClass().getName(), "readSatFile failed: " + satFileName);
                            }
                            // If any SatFile isn't read, set a SharedPref flag that we can easily test in LookAfterStuff
                            // to repeat this Task.
                            // A file may not be read if the user navigates away while files are being read
                            editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false).apply();
                        }
                    }
                }
            } catch (IOException e) {
                editor.putBoolean(PREF_KEY_SATFILES_READ_SUCCESS, false).apply();
                if (MainActivity.DEBUG) {e.printStackTrace();}
            } finally {
                settings.edit().putBoolean(PREF_KEY_READING_SATFILES, false).apply();
            }
            if (MainActivity.DEBUG) {
                Log.w(this.getClass().getName(), "reading sat files Runnable took "
                        + String.format("%3.1f",(System.currentTimeMillis() - startTime) / 1000.) + " sec");
            }
        }
    };

    private Runnable updateSatelliteStatusFromCelestrak = new Runnable(){
        /**
         * 1) See if we've contacted Celestrak in the past month by reading a SharedPref value
         * 2) contact various Celestrak web pages to read a .txt response containing a bunch of status data
         * 3) pass an ArrayList of ContentValues to a routine that updates the satDB database with status
         */
        @Override
        public void run() {
            SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            //only read Celestrack data once a week. Store last retrieved time in SharedPrefs.
            if (!Util.checkCelestrakStatusNeedsUpdating(settings)){
                if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "Celestrak status doesn't need updating");}
                editor.putBoolean(PREF_KEY_UPDATING_STATUS, false).apply();
                return;
            }
            long startTime = System.currentTimeMillis();
            // if MainActivity is stopped, but TimerTask is still running, but DB is closed we may think we updated TLEs
            // because we didn't get any TLEs to update. updated will be false if it is closed
            boolean updated = (satelliteDB != null && !isClosed());
            boolean success = updateDBTLEs(retrieveStatusFromCelestrak());
            if (MainActivity.DEBUG) {
                if (success) {
                    Log.w(this.getClass().getName(), "successfully updated Celestrak status");
                } else {
                    Log.w(this.getClass().getName(), "didn't update Celestrak status");
                }
                Log.w(this.getClass().getName(), "updating Celestrak status Runnable took "
                        + String.format("%3.1f",(System.currentTimeMillis() - startTime) / 1000.) + " sec");
            }
            editor.putBoolean(PREF_KEY_UPDATING_STATUS, false).apply();
            updated = updated && success;
            // now if we've succeeded, store the current time in SharedPrefs; we'll test this here next time to see if we have to update TLEs
            if (updated) {
                if (MainActivity.DEBUG) { Log.w(this.getClass().getName(), "successfully updated Celestrak status"); }
                editor.putLong(PREF_KEY_UPDATED_CELESTRAK_STATUS_TIME, System.currentTimeMillis()).apply();
            } else if (MainActivity.DEBUG) {
                Log.w(this.getClass().getName(), "didn't update Celestrak status db closed? " + (isClosed() ? "yes" : "no"));
            }
        }
    };

    private Runnable updateTLEsFromCelestrak = new Runnable() {
        /**
         * 1) See if we've contacted Celestrak in the past day by reading a SharedPref value
         * 2) contact various Celestrak web pages to read a .txt response containing a bunch of TLEs
         * 3) pass an ArrayList of ContentValues to a routine that updates the satDB database with TLEs and a timestamp
         */
        @Override
        public void run() {
            SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            //only read Celestrack data once a week. Store last retrieved time in SharedPrefs.
            if (!Util.checkCelestrakTLEsNeedUpdating(settings)){
                if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "Celestrak TLEs don't need updating");}
                editor.putBoolean(PREF_KEY_UPDATING_TLES, false).apply();
                return;
            }
            long startTime = System.currentTimeMillis();
            // if MainActivity is stopped, but TimerTask is still running, but DB is closed we may think we updated TLEs
            // because we didn't get any TLEs to update. updated will be false if it is closed
            boolean updated = (satelliteDB != null && !isClosed());
            boolean success = updateDBTLEs(retrieveTLEsFromCelestrak());
            if (MainActivity.DEBUG) {
                if (success) {
                    Log.w(this.getClass().getName(), "successfully updated Celestrak TLEs");
                } else {
                    Log.w(this.getClass().getName(), "didn't update Celestrak TLEs");
                }
                Log.w(this.getClass().getName(), "updating Celestrak TLEs Runnable took "
                        + String.format("%3.1f",(System.currentTimeMillis() - startTime) / 1000.) + " sec");
            }
            editor.putBoolean(PREF_KEY_UPDATING_TLES, false).apply();
            updated = updated && success;
            // now if we've succeeded, store the current time in SharedPrefs; we'll test this here next time to see if we have to update TLEs
            if (updated) {
                if (MainActivity.DEBUG) { Log.w(this.getClass().getName(), "successfully updated Celestrak TLEs"); }
                editor.putLong(PREF_KEY_UPDATED_CELESTRAK_TLES_TIME, System.currentTimeMillis()).apply();
            } else if (MainActivity.DEBUG) {
                Log.w(this.getClass().getName(), "didn't update Celestrak TLEs db closed? " + (isClosed() ? "yes" : "no"));
            }
        }
    };

    /**
     * After we've retrived the new TLEs from Space-Track or Celestrack or McCants,
     * or from Celestrak in the AsyncTask "UpdateTLEsFromCelestrakBackground", update the satDB with the new ContentValues for each satellite.
     *
     * @param contentList a List of ContentValues containing new TLE1, TLE2, SAT_NAME, TLE_time-stamp data, Norad Number
     * @return true if all Content was written to the database
     */
    private boolean updateDBTLEs(List<ContentValues> contentList) {
        // if URL is bad, or format changed, we may not retrieve any tles
        boolean updated = contentList.size() > 0;
        boolean success = true;
        if (!isClosed()) {
            try {
                satelliteDB.beginTransaction();
                for (ContentValues content : contentList) {
                    int noradNum = content.getAsInteger(DB_KEY_NORAD_NUM);
                    if ((noradNum < 43700 && noradNum > 43500) && MainActivity.DEBUG) {
                        showDBTLEContent(noradNum);
                    }
                    // 1) get TLE Epoch for NoradNum from database: Get TLE1, TLE2, find Epoch
                    // 2) test this content's TLE Epoch is greater, then update
                    if (isContentNewer(content)) {
                        if ((noradNum < 43700 && noradNum > 43500) && MainActivity.DEBUG) {
                            Log.w(this.getClass().getName(), "Content is newer Norad # " +noradNum);
                        }
                        String[] whereArgs = {String.valueOf(noradNum)};
                        satelliteDB.update(SAT_TABLE, content, DB_KEY_NORAD_NUM + "=?", whereArgs);
                    }
                }
                satelliteDB.setTransactionSuccessful();
            } catch (Exception e1) {
                if (MainActivity.DEBUG) {
                    e1.printStackTrace();
                }
                success = false;
            } finally {
                satelliteDB.endTransaction();
            }
        } else {
            success = false;
        }

        return updated;
    }

    /**
     * TLE data from different sources may have different Epoch. We only want to update TLEs if they are newer.
     * If there is no TLE data, or TLE_TIMESTAMP is "0" the new data is newer. Convert TIMESTAMP into a Date to compare.
     *
     * @param content is new data downloaded from SpaceTrack, Celestrak or McCant's
     * @return true if the new data has a later epoch than what's in the dataBase
     */
    private boolean isContentNewer(ContentValues content) {
        String[] dbData = fetchDeviceTLEData(content.getAsInteger(DB_KEY_NORAD_NUM));
        boolean contentIsNewer;
        if (dbData[3] == null) {
            return true;
        }
        //if database epoch is "0" whatever content we receive is newer
        if (STRING_ZERO.equals(dbData[3])) {
            //            if (MainActivity.DEBUG){Log.w(this.getClass().getName(), "norad#: "
            //                    + content.getAsInteger(DB_KEY_NORAD_NUM)
            //                    + " no content");}
            return true;
        }
        try {
            Double dbEpoch = Double.parseDouble(dbData[3]);
            String contentEpochString = content.getAsString(DB_KEY_SAT_TLE_TIMESTAMP);
            Double contentEpoch = Double.parseDouble(contentEpochString);
            contentIsNewer = contentEpoch > dbEpoch || (FUTURE_EPOCH).equals(contentEpochString);
        } catch (NumberFormatException e) {
            contentIsNewer = true;
        }
        return contentIsNewer;
    }

    private void showDBTLEContent(Integer noradNumber) {
        String[] theData = fetchDeviceTLEData(noradNumber);
        Log.w(this.getClass().getName(), "norad#: " + noradNumber
                + " TLE1: " + theData[1] + " TLE2: " + theData[2] + " epoch: " + theData[3]);
    }


    /**
     * Using the current time and TLE time-tag column, determine which satellites need to update the TLE
     * A LEO satellite will need to update TLE more often than GEO or DEEP
     *
     * @param kind is the "Kind" column in the satDB
     * @return an ArrayList of Integer Norad numbers with old TLEs
     */
    private int howManyZeroEpochSatellites(String kind) {
        int count = 0;
        // 1) get Cursor containing TLE Epoch from satDB query
        // 2) step thru Cursor and increment count if TLE Epoch time-stamp is zero or null
        // 3) release Cursor

        String filter = DB_KEY_SAT_KIND + "= '" + kind + "'";
        String[] columns = {DB_KEY_NORAD_NUM, DB_KEY_SAT_TLE_TIMESTAMP};
        Cursor mCursor = null;
        try {
            if (satelliteDB != null && !isClosed()) {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter,  null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    String satDbTimeString;
                    do {
                        satDbTimeString = mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_SAT_TLE_TIMESTAMP));
                        if (satDbTimeString == null || satDbTimeString.equals("0")) {
                            // epoch has never been set, set it to be old
                            //Log.i(this.getClass().getName(), "no TLEs for Norad# " + mCursor.getString(mCursor.getColumnIndexOrThrow(DB_KEY_NORAD_NUM)));
                            count++;
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
        return count;
    }

    /**
     * Contact a number of pages at Celestrak and read the TLEs from the response.
     * Could check that the URL is valid, get the response and parse into ContentValues
     *
     * @return ArrayList of ContentValues containing TLEs, the timestamp (Epoch)
     */
    private ArrayList<ContentValues> retrieveTLEsFromCelestrak() {
        // get most recent TLES from Celestrak website; parse return to extract TLE1 and TLE2 Strings, add to ContentValues list
        ArrayList<ContentValues> tleContentList = new ArrayList<>();
        for (String webSite : celestrakURLList) {
            tleContentList.addAll(mCGetter.getTLEsFromCelestrak(webSite));
        }
        if (MainActivity.DEBUG) { Log.w(this.getClass().getName(), "retrieved " + tleContentList.size() + " TLEs from Celestrak");}
        return tleContentList;
    }


    /**
     * Contact a pages at Celestrak and read the status from the response.
     * Could check that the URL is valid, get the response and parse into ContentValues
     *
     * @return ArrayList of ContentValues containing TLEs, the timestamp (Epoch)
     */
    private ArrayList<ContentValues> retrieveStatusFromCelestrak() {
        // get most recent status from Celestrak website; parse return to extract TLE1 and TLE2 Strings, add to ContentValues list
        ArrayList<ContentValues> tleContentList = new ArrayList<>(mCSSGetter.getStatusFromCelestrak(CELESTRAK_STATUS_WEBSITE));
        if (MainActivity.DEBUG) { Log.w(this.getClass().getName(), "retrieved " + tleContentList.size() + " status from Celestrak");}
        return tleContentList;
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
        ContentValues satContent;
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
        ArrayList<ContentValues> satContentList = new ArrayList<>();
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
                    //documentNum = 0;
                    // if documentNum is same, return
                    if (currentDocumentNum == documentNum) {
                        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "document not new - doc#:" + documentNum + " saved doc#: " + currentDocumentNum);}
                        return true;
                    }
                }
                // loaded in new data, so re-build GEO satellites
                rebuildGEOSatellites = true;
                rebuildCGEOSatellites = true;
                while ((line = reader.readLine()) != null) {
                   // tab-delimited text file
                    st = new StringTokenizer(line, "\t");
                    satContent = new ContentValues();
                    // withTLEs file has different format; read all satContent here
                    if (satFileName.contains(WITH_TLES)) {
                        satContent.put(DB_KEY_SAT_KIND, GEO);
                        satContent.put(DB_KEY_SAT_INFO_LINK, "");
                        satContent.put(DB_KEY_SAT_CATEGORY, st.nextToken());
                        satContent.put(DB_KEY_SAT_SUB_CATEGORY, st.nextToken());
                        satContent.put(DB_KEY_SAT_NAME, limitSatelliteNameLength(st.nextToken()));
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
                        satContent.put(DB_KEY_OTHER, "");
                    } else {
                        // read all other files here
                        String satKind = st.nextToken();
                        satContent.put(DB_KEY_SAT_KIND, satKind);
                        satContent.put(DB_KEY_SAT_INFO_LINK, st.nextToken());
                        satContent.put(DB_KEY_SAT_CATEGORY, st.nextToken());
                        satContent.put(DB_KEY_SAT_SUB_CATEGORY, st.nextToken());
                        satContent.put(DB_KEY_SAT_NAME, limitSatelliteNameLength(st.nextToken()));
                        // catch NumberFormatException if a field was missing and Norad number is not a number
                        try {
                            satContent.put(DB_KEY_NORAD_NUM, Integer.valueOf(st.nextToken()));
                        } catch (NumberFormatException e){
                            if (MainActivity.DEBUG) { e.printStackTrace(); }
                            satContent.put(DB_KEY_NORAD_NUM, 0);
                        }
                        // catch NoSuchElementException if there wasn't a description in the file
                        try {
                            satContent.put(DB_KEY_SAT_BRIEF_DESC, st.nextToken());
                        } catch (NoSuchElementException e) {
                            if (MainActivity.DEBUG) {
                                e.printStackTrace();
                                Log.i(this.getClass().getName(), line);
                            }
                            // if brief description was empty, just add " "
                            satContent.put(DB_KEY_SAT_BRIEF_DESC, " ");
                        }
                        satContent.put(DB_KEY_OTHER, st.nextToken());
                    }
                    if (satContent.getAsString(DB_KEY_SAT_TLE1) == null) {
                        // initialize all the DB columns not specified in the satellite file
                        satContent.put(DB_KEY_SAT_TLE1, "");
                        satContent.put(DB_KEY_SAT_TLE2, "");
                    }
                    satContent.put(DB_KEY_SAT_TLE_TIMESTAMP, 0);
                    satContentList.add(satContent);
                }
                if (MainActivity.DEBUG) {
                    Log.i(this.getClass().getName(), String.format("read %d satellites from file %s", satContentList.size(), satFileName));
                }
                success = addSatToDBFromFile(satContentList);
                if (success){
                    // when successfully added satellite data, store the document number so we won't read it again
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt(PREF_KEY_SATFILE_DOC_NUM + satFileName, documentNum).apply();
                }
            }
        } catch (IOException e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        }
        return success;
    }

    /**
     * Read the data pertaining to a particular noradNum
     * Include the satellite name, TLE1 & TLE2
     *
     * @param noradNum the  number of the satellite to read
     * @return a String[] containing the data
     */
    private String[] fetchDeviceTLEData(long noradNum) {
        String[] returnString = new String[4];
        String[] columns = {DB_KEY_SAT_NAME, DB_KEY_SAT_TLE1, DB_KEY_SAT_TLE2, DB_KEY_SAT_TLE_TIMESTAMP};
        String filter = DB_KEY_NORAD_NUM + "= '" + Integer.toString((int) noradNum) + "'";
        Cursor mCursor = null;
        if (satelliteDB != null && !isClosed()) {
            try {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        returnString[0] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_NAME));
                        returnString[1] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE1));
                        returnString[2] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE2));
                        returnString[3] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_TLE_TIMESTAMP));
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
     * Read the data pertaining to a particular noradNum
     * Include the satellite name, brief description and link to add content to popup window
     * also include satellite category for displaying a different icon and Int'l code
     * in "Other" category to replace dead link in Satellite Dialog
     *
     * @param noradNum the  number of the satellite to read
     * @return a String[] containing the data
     */
    String[] fetchDeviceData(long noradNum) {
        String[] returnString = new String[6];
        String[] columns = {DB_KEY_SAT_NAME, DB_KEY_SAT_BRIEF_DESC, DB_KEY_SAT_INFO_LINK, DB_KEY_SAT_CATEGORY, DB_KEY_OTHER, DB_KEY_SAT_SUB_CATEGORY};
        String filter = DB_KEY_NORAD_NUM + "= '" + Integer.toString((int) noradNum) + "'";
        Cursor mCursor = null;
        if (satelliteDB != null && !isClosed()) {
            try {
                mCursor = satelliteDB.query(SAT_TABLE, columns, filter, null, null, null, null);
                if (mCursor != null && mCursor.moveToFirst()) {
                    do {
                        returnString[0] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_NAME));
                        returnString[1] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_BRIEF_DESC));
                        returnString[2] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_INFO_LINK)).replace(DOUBLE_QUOTE, "");
                        returnString[3] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_CATEGORY));
                        returnString[4] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_OTHER));
                        returnString[5] = mCursor.getString(mCursor.getColumnIndex(DB_KEY_SAT_SUB_CATEGORY));
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
     * @param contentList has the satellite kind, moreInfoLink, satellite category,
     *                satellite sub-category, satellite name, Norad number, brief description, and Other (Int'l Code)
     */
    private boolean addSatToDBFromFile(ArrayList<ContentValues> contentList) {
        boolean returnSuccess = true;
        try {
            if (satelliteDB != null && satelliteDB.isOpen()) {
                satelliteDB.beginTransaction();
                for (ContentValues content : contentList) {
                    if (satelliteDB.insertWithOnConflict(SAT_TABLE, "",
                            content, SQLiteDatabase.CONFLICT_IGNORE) == -1){
                        int noradNum = content.getAsInteger(DB_KEY_NORAD_NUM);
                        String[] whereArgs = {String.valueOf(noradNum)};
                        satelliteDB.update(SAT_TABLE, content, DB_KEY_NORAD_NUM + "=?", whereArgs);
                    }
                }
                satelliteDB.setTransactionSuccessful();
            } else {
                // database was closed; maybe user navigated away before finishing reading satellite documents
                // this will reset SharedPrefs document number so it will load again
                returnSuccess = false;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            if (satelliteDB != null && !isClosed()) {
                satelliteDB.endTransaction();
                if (MainActivity.DEBUG) {
                    long count = DatabaseUtils.queryNumEntries(satelliteDB, SAT_TABLE);
                    Log.w(this.getClass().getName(), String.format("added %d rows to DB ", count));
                }
            }
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
    boolean updateSatelliteRecord(int noradNum, ContentValues content) {
        String[] whereArgs = {String.valueOf(noradNum)};
        boolean success = true;
        int rows;
        try {
            if (satelliteDB != null && satelliteDB.isOpen()) {
                rows = satelliteDB.update(SAT_TABLE, content, DB_KEY_NORAD_NUM + "=?", whereArgs);
            } else {
                success = false;
            }
        } catch (Exception e1) {
            if (MainActivity.DEBUG) {e1.printStackTrace();}
            success = false;
        }
        return success;
    }

    private class DBHelper extends SQLiteOpenHelper {
		static final String TEXT_NOT_NULL = " TEXT NOT NULL";
		static final String INTEGER_NOT_NULL = " INTEGER NOT NULL";
		static final String INTEGER_UNIQUE = " INTEGER UNIQUE";
		private static final int DB_VERSION = 1;

		DBHelper(Context context) {
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
     * @throws SQLiteException a bad thing
     */
    public SATDBAdapter open() throws SQLiteException {
        mDBHelper = new DBHelper(mContext);
        satelliteDB = mDBHelper.getWritableDatabase();
        return this;
    }

    /**
     * close the satTable database
     */
    void close() {
        try {
            if (mDBHelper != null) {
                mDBHelper.close();
            }
        } catch (IllegalStateException e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        }
    }

	boolean isClosed() {
		return satelliteDB == null || !satelliteDB.isOpen();
	}

}
