package com.cyclebikeapp.lookup;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;

import satellite.GroundStationPosition;
import satellite.SatPos;
import satellite.Satellite;
import satellite.SatelliteFactory;
import satellite.TLE;
import static com.cyclebikeapp.lookup.Constants.*;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Created by TommyD on 3/7/2016.
 * helper class to build satellites and calculate look angles using routines from g4dpz library
 */

class SatelliteTabulator {

    private String[] categoryKeyWords;
    private final SharedPreferences settings;
    private final SharedPreferences defaultSettings;


    SatelliteTabulator(Context context) {
        initializeSatelliteCategoryList();
        // need to read in SharedPrefs allowed categories; only need context for this
        // bad practice to keep context as a Class element
        settings = context.getSharedPreferences(PREFS_NAME, 0);
        defaultSettings = getDefaultSharedPreferences(context);
    }

    /**
     * These are the labels for the satellite categories in the satellite lists
     */
    private void initializeSatelliteCategoryList() {
        categoryKeyWords = new String[]{"Tv", "Radio", "Weather", "Military",
                "HAM", "Communications", "Earth Observing", "Science", "Data", "GPS", "Space Station"};
    }
    /**
     * 1) read TLE Strings from DB for each satellite and create the real TLEs
     * 2) pass TLEs plus my location and time to makeSatellites() which creates the satellite &
     * calculates sat position
     * 3) store look angles in TLEs so we can retrieve them later
     * 4) if the satellite is visible from myLocation, add it to the return list
     *
     * @param myLocation    user Location lat/long of user position, either from GPS, or WiFi, cell. Only need coarse location
     * @param time          the current time in milliseconds
     * @param myDBAdapter   a reference to the database to query
     * @param noradNumber the int describing this satellite
     */
    Satellite buildSatelliteForNoradNumber(Location myLocation, long time, SATDBAdapter myDBAdapter, int noradNumber) {
        // need a Location to calc look angles; use a WatchDog to check how current Location is and how current TLEs are...
        // Assemble TLE String array from database for satellite "kind"; checks for valid Strings
        long startTime = System.nanoTime();// just used for debugging
        String[] mTLEString = myDBAdapter.fetchTLEsByNoradNumber(noradNumber);
        //Now create TLE list (also does some orbit pre-calculations) and create a list of visible satellites
        DateTime timeNow = new DateTime(new Date(time));
        return makeOneSatellite(mTLEString, myLocation, timeNow);
    }
    private Satellite makeOneSatellite(String[] mTLEString, Location myLocation, DateTime time) {
        Date nowDate = time.toDate();
        GroundStationPosition groundStationPosition = calcGroundStation(myLocation);
        // read Shared Prefs to find out which categories user has checked
        boolean[] allowedCategories = getAllowedCategories();
        // too many satellites to show, default is not to show inactive satellites
        Satellite aSat = null;
        // also make sure we have TLE1 and TLE2
        //FREE version only has active GEO satellites, so the active / inactive switch makes no difference
        if (mTLEString[2] != null && mTLEString[1] != null && mTLEString[1].length() > 3 && mTLEString[2].length() > 3) {
            try {
                TLE aTLE = new TLE(mTLEString);
                aSat = SatelliteFactory.createSatellite(aTLE);
                // now calculate SatPos, a class in g4dpz library
                SatPos satellitePosition = aSat.getPosition(groundStationPosition, nowDate);
                // set look angles in TLEs - Degrees
                //az angles go from 0 to 360, el angles -90 to 90
                aSat.getTLE().setLookAngleAz((int) Math.toDegrees(satellitePosition.getAzimuth()));
                aSat.getTLE().setLookAngleEl((int) Math.toDegrees(satellitePosition.getElevation()));

                // Exception thrown if TLE Strings are mal-formed
            } catch (IllegalArgumentException e) {
                if (MainActivity.DEBUG) {e.printStackTrace();}
            }
        }

        return aSat;
    }
    /**
     * 1) read TLE Strings from DB for each satellite and create the real TLEs
     * 2) pass TLEs plus my location and time to makeSatellites() which creates the satellite &
     * calculates sat position
     * 3) store look angles in TLEs so we can retrieve them later
     * 4) if the satellite is visible from myLocation, add it to the return list
     *
     * @param myLocation    user Location lat/long of user position, either from GPS, or WiFi, cell. Only need coarse location
     * @param time          the current time in milliseconds
     * @param myDBAdapter   a reference to the database to query
     * @param satelliteKind a String of satellite type to build "GEO", "LEO", "DEEP", "CGEO", or "CDEEP"
     */
    ArrayList<Satellite> buildSatellitesByKind(Location myLocation, long time, SATDBAdapter myDBAdapter, String satelliteKind) {
        // need a Location to calc look angles; use a WatchDog to check how current Location is and how current TLEs are...
        // Assemble TLE String array from database for satellite "kind"; checks for valid Strings
        long startTime = System.nanoTime();// just used for debugging
        ArrayList<String[]> mTLEString = myDBAdapter.fetchTLEsByKind(satelliteKind);
        //Now create TLE list (also does some orbit pre-calculations) and create a list of visible satellites
        DateTime timeNow = new DateTime(new Date(time));
        ArrayList<Satellite> returnList = makeSatellites(mTLEString, myLocation, timeNow);
        if (MainActivity.DEBUG) {
            Log.i(this.getClass().getName(), "buildSatellitesByKind() takes " + String.format("%4.2f", (System.nanoTime() - startTime) / 1000000.)
                    + " msec to make " + (returnList.size()) + " " + satelliteKind + " satellites from a list of " + mTLEString.size() + " dataBase entries");
        }
        // now update satellite Kind in the database, depending on what type SatelliteFactory has determined
        for (Satellite aSat : returnList) {
            ContentValues tleContent = new ContentValues();
            if (!satelliteKind.equals(GEO)
                    && !satelliteKind.equals(CGEO)
                    && !satelliteKind.equals(CDEEP)) {
                tleContent.put(DB_KEY_SAT_KIND, (aSat.getTLE().isDeepspace() ? DEEP : LEO));
                myDBAdapter.updateSatelliteRecord(aSat.getTLE().getCatnum(), tleContent);
            }
        }
        return returnList;
        // return a List of visible Satellites that we will search when user clicks on Canvas
    }


    private ArrayList<Satellite> makeSatellites(ArrayList<String[]> mTLEStrings, Location myLocation, DateTime time) {
        ArrayList<Satellite> returnSatelliteList = new ArrayList<>();
        Date nowDate = time.toDate();
        GroundStationPosition groundStationPosition = calcGroundStation(myLocation);
        // read Shared Prefs to find out which categories user has checked
        boolean[] allowedCategories = getAllowedCategories();
        // too many satellites to show, default is not to show inactive satellites
        boolean showInactiveSatellites = defaultSettings.getBoolean(SHOW_INACTIVE_SATS_KEY, false);
        for (String[] tleStrings : mTLEStrings) {
            // get category, included in TLE[3] fetched from dataBase, see if that should be built
            // also make sure we have TLE1 and TLE2
            // if Preference is to show inactive satellites, or satellite is active, make it
            boolean satelliteIsActive = tleStrings[4].toUpperCase().equals(ACTIVE);
            //FREE version only has active GEO satellites, so the active / inactive switch makes no difference
            String category = tleStrings[3];
             if (inFilteredCategoryList(category, allowedCategories)
                    && (showInactiveSatellites || satelliteIsActive)
                    && tleStrings[1].length() > 3
                    && tleStrings[2].length() > 3) {

                try {
                    TLE aTLE = new TLE(tleStrings);
                    Satellite aSat = SatelliteFactory.createSatellite(aTLE);
                    // now calculate SatPos, a class in g4dpz library
                    SatPos satellitePosition = aSat.getPosition(groundStationPosition, nowDate);
                    // set look angles in TLEs - Degrees
                    //az angles go from 0 to 360, el angles -90 to 90
                    aSat.getTLE().setLookAngleAz((int) Math.toDegrees(satellitePosition.getAzimuth()));
                    aSat.getTLE().setLookAngleEl((int) Math.toDegrees(satellitePosition.getElevation()));
                    // then determine if satellite is visible from user Location;
                    // only add visible satellites to the return list
                    if (satellitePosition.isAboveHorizon()) {
                        returnSatelliteList.add(aSat);
                    }
                    // Exception thrown if TLE Strings are mal-formed
                } catch (IllegalArgumentException e) {
                    if (MainActivity.DEBUG) {e.printStackTrace();}
                }
            }
        }
        return returnSatelliteList;
    }

    /**
     *
      * @return a boolean list of whether the user has chosen that category to display
     */
    private boolean[] getAllowedCategories() {
        boolean[] allowedCategories = new boolean[categoryKeyWords.length];
        allowedCategories[0] = settings.getBoolean(SHOW_TELEVISION_SETTING_KEY, true);
        allowedCategories[1] = settings.getBoolean(SHOW_RADIO_SETTING_KEY, true);
        allowedCategories[2] = settings.getBoolean(SHOW_WEATHER_SETTING_KEY, true);
        allowedCategories[3] = settings.getBoolean(SHOW_MILITARY_SETTING_KEY, true);
        allowedCategories[4] = settings.getBoolean(SHOW_HAM_SETTING_KEY, true);
        allowedCategories[5] = settings.getBoolean(SHOW_COMMUNICATION_SETTING_KEY, true);
        allowedCategories[6] = settings.getBoolean(SHOW_EARTHOBSERVING_SETTING_KEY, true);
        allowedCategories[7] = settings.getBoolean(SHOW_SCIENCE_SETTING_KEY, true);
        allowedCategories[8] = settings.getBoolean(SHOW_DATA_SETTING_KEY, true);
        allowedCategories[9] = settings.getBoolean(SHOW_GPS_SETTING_KEY, true);
        allowedCategories[10] = settings.getBoolean(SHOW_ISS_SETTING_KEY, true);
        return allowedCategories;
    }

    /**
     * For a satellite candidate to be included in the satellite list we are building, it must be in the allowed categories list
     * @param category the category of the satellite we might include in the satellite list
     * @param allowedCategories user specified categories to display
     * @return if the category is in the allowed list
     */
    private boolean inFilteredCategoryList(String category, boolean[] allowedCategories) {
        int i = 0;
       // {"Tv", "Radio", "Weather", "Military", "HAM", "Communications", "Earth Observing", "Science", "Data", "GPS", "Space Station"};
        for (String keyWord : categoryKeyWords) {
            if (category.toUpperCase().contains(keyWord.toUpperCase()) && allowedCategories[i]) {
                return true;
            }
            i++;
        }
        return false;
    }

    /**
     * Periodically calculate position of LEO and DEEP satellites as they transit user field of view.
     * The "look-angles", azimuth, elevation from the user location are stored in the Satellite structures'
     * TLE field so we can access it when drawing satellites
     *
     * @param satList    the list of satellites to recalculate look angles for; usually this will be LEO satellites
     * @param myLocation lat/long of user position, either from GPS, or WiFi, cell. Only need coarse location
     */
    void recalculateLookAngles(ArrayList<Satellite> satList, Location myLocation, long time) {
        if (myLocation == null) {
            return;
        }
        GroundStationPosition groundStationPosition = calcGroundStation(myLocation);
        DateTime timeNow = new DateTime(new Date(time));
        Date nowDate = timeNow.toDate();
        for (Satellite aSat : satList) {
            SatPos satellitePosition = aSat.getPosition(groundStationPosition, nowDate);
            // set look angles in TLEs - Degrees
            //az angles go from 0 to 360, el angles -90 to 90
            aSat.getTLE().setLookAngleAz(Math.toDegrees(satellitePosition.getAzimuth()));
            aSat.getTLE().setLookAngleEl(Math.toDegrees(satellitePosition.getElevation()));
        }
    }

    /**
     * Create a GroundStation for calculating satellite look-angles
     * @param myLocation user Location from GPS, WiFi or cell. Coarse precision is adequate
     * @return the GroundStation instance
     */
    private GroundStationPosition calcGroundStation(Location myLocation) {
        return new GroundStationPosition(myLocation.getLatitude(),
                myLocation.getLongitude(),
                myLocation.getAltitude());
    }

}
