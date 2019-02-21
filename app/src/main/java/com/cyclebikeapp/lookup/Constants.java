package com.cyclebikeapp.lookup;

/**
 * Created by TommyD on 8/4/2016.
 *
 */
public final class Constants {
    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";

    static final int MAX_SAT_NAME_LENGTH = 32;
    static final String SAT_FILES_CLASSFD_TLE = "satFiles/classfd.tle";
    static final String SAT_FILES_INTTLES_TLE = "satFiles/inttles.tle";
    static final String SAT_FILES_SPACETRACK_COMPLETE_TLES_1 = "satFiles/spacetrack_complete_1.tle";
    static final String SAT_FILES_SPACETRACK_COMPLETE_TLES_2 = "satFiles/spacetrack_complete_2.tle";
    static final String SAT_FILES_SPACETRACK_COMPLETE_TLES_3 = "satFiles/spacetrack_complete_3.tle";
    static final String SAT_FILES_SPACETRACK_COMPLETE_TLES_4 = "satFiles/spacetrack_complete_4.tle";

    private Constants() {}
    static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 0 meters
    static final long MIN_TIME_BW_UPDATES = 100; //.1 sec
    static final String SHARING_IMAGE_NAME = "LookUpInTheSky.jpg";
    static final String SAT_TABLE = "satTable";
    static final String SAT_DB = "satDataBase";
    //database key for the satellite kind from the file; GEO, LEO, DEEP
    static final String DB_KEY_SAT_KIND = "satellite_kind";
    //database key for the Norad satellite number
    static final String DB_KEY_NORAD_NUM = "norad_num";
    //database key for the satellite name as labeled in the file
    static final String DB_KEY_SAT_NAME = "satellite_name";
    //database key for the satellite category from the file; TV, Communication, Weather, etc
    static final String DB_KEY_SAT_CATEGORY = "satellite_category";
    //database key for the satellite sub-catgory from the file; use to indicate if a satellite is active or inactive
    static final String DB_KEY_SAT_SUB_CATEGORY = "satellite_sub_category";
    //database key for the satellite information link
    static final String DB_KEY_SAT_INFO_LINK = "satellite_info_link";
    //database key for the satellite brief description from file; to be displayed when user clicks on satellite
    static final String DB_KEY_SAT_BRIEF_DESC = "satellite_brief_description";
    //database key for the satellite Two Line Element set; retrieved from Space-Track.Org
    static final String DB_KEY_SAT_TLE1 = "satellite_tle_1";
    //database key for the satellite Two Line Element set, second String
    static final String DB_KEY_SAT_TLE2 = "satellite_tle_2";
    //database key for the timestamp when the TLE is retrieved, used to determine if TLE should be updated (in seconds)
    static final String DB_KEY_SAT_TLE_TIMESTAMP = "satellite_tle_timestamp";
    //database key for an undefined column that may be used later
    static final String DB_KEY_OTHER = "satellite_other";
    static final String SDF_KEY_TITLE = "sdf_key_title";
    static final String SDF_KEY_NUM_SEARCH_RESULTS = "sdf_key_num_search_results";
    static final String SDF_KEY_STATUS = "sdf_key_status";
    static final String SDF_KEY_MESSAGE = "sdf_key_message";
    static final String SDF_KEY_NORAD_NUMBER = "sdf_key_norad_number";
    static final String SDF_KEY_LINK = "sdf_key_link";
    // strip all double quote characters from the KIND, Category, Sub-Category, etc data in the file
    static final String DOUBLE_QUOTE = "\"";
    static final String CELESTRAK_STATUS_WEBSITE = "https://www.celestrak.com/pub/satcat.txt";
    // 3-line TLEs
    static final String celestrakURL1 = "https://celestrak.com/NORAD/elements/cubesat.txt";
    static final String celestrakURL2 = "https://celestrak.com/NORAD/elements/amateur.txt";
    static final String celestrakURL3 = "https://celestrak.com/NORAD/elements/supplemental/gps.txt";
    static final String celestrakURL4 = "https://celestrak.com/NORAD/elements/supplemental/glonass.txt";
    static final String celestrakURL5 = "https://celestrak.com/NORAD/elements/supplemental/meteosat.txt";
    static final String celestrakURL6 = "https://celestrak.com/NORAD/elements/supplemental/intelsat.txt";
    static final String celestrakURL7 = "https://celestrak.com/NORAD/elements/supplemental/ses.txt";
    static final String celestrakURL8 = "https://celestrak.com/NORAD/elements/supplemental/orbcomm.txt";
    static final String celestrakURL9 = "https://celestrak.com/NORAD/elements/supplemental/cpf.txt";
    static final String celestrakURL10 = "https://celestrak.com/NORAD/elements/science.txt";
    static final String celestrakURL11 = "https://celestrak.com/NORAD/elements/engineering.txt";
    static final String celestrakURL12 = "https://celestrak.com/NORAD/elements/nnss.txt";
    static final String celestrakURL13 = "https://celestrak.com/NORAD/elements/sbas.txt";
    static final String celestrakURL14 = "https://celestrak.com/NORAD/elements/beidou.txt";
    static final String celestrakURL15 = "https://celestrak.com/NORAD/elements/globalstar.txt";
    static final String celestrakURL16 = "https://celestrak.com/NORAD/elements/molniya.txt";
    static final String celestrakURL17 = "https://celestrak.com/NORAD/elements/raduga.txt";
    static final String celestrakURL18 = "https://celestrak.com/NORAD/elements/tdrss.txt";
    static final String celestrakURL19 = "https://celestrak.com/NORAD/elements/sarsat.txt";
    static final String celestrakURL20 = "https://celestrak.com/NORAD/elements/resource.txt";
    static final String celestrakURL21 = "https://celestrak.com/NORAD/elements/noaa.txt";
    static final String celestrakURL22 = "https://celestrak.com/NORAD/elements/goes.txt";
    static final String celestrakURL23 = "https://celestrak.com/NORAD/elements/weather.txt";
    static final String celestrakURL24 = "https://celestrak.com/NORAD/elements/gorizont.txt";
    static final String celestrakURL25 = "https://celestrak.com/NORAD/elements/iridium.txt";
    static final String celestrakURL26 = "https://celestrak.com/NORAD/elements/education.txt";
    static final String celestrakURL27 = "https://celestrak.com/NORAD/elements/geodetic.txt";
    static final String celestrakURL28 = "https://celestrak.com/NORAD/elements/musson.txt";
    static final String celestrakURL29 = "https://celestrak.com/NORAD/elements/galileo.txt";
    static final String celestrakURL30 = "https://celestrak.com/NORAD/elements/glo-ops.txt";
    static final String celestrakURL31 = "https://celestrak.com/NORAD/elements/gps-ops.txt";
    static final String celestrakURL32 = "https://celestrak.com/NORAD/elements/other-comm.txt";
    static final String celestrakURL33 = "https://celestrak.com/NORAD/elements/x-comm.txt";
    static final String celestrakURL34 = "https://celestrak.com/NORAD/elements/geo.txt";
    static final String celestrakURL35 = "https://celestrak.com/NORAD/elements/argos.txt";
    static final String celestrakURL36 = "https://celestrak.com/NORAD/elements/dmc.txt";
    static final String celestrakURL37 = "https://celestrak.com/NORAD/elements/sarsat.txt";
    static final String celestrakURL38 = "https://celestrak.com/NORAD/elements/intelsat.txt";
    static final String celestrakURL39 = "https://celestrak.com/NORAD/elements/orbcomm.txt";
    static final String celestrakURL40 = "https://celestrak.com/NORAD/elements/military.txt";
    static final String celestrakURL41 = "https://celestrak.com/NORAD/elements/radar.txt";
    static final String celestrakURL42 = "https://celestrak.com/NORAD/elements/other.txt";
    static final String celestrakURL43 = "https://celestrak.com/NORAD/elements/stations.txt";
    static final String celestrakURL44 = "https://www.celestrak.com/NORAD/elements/visual.txt";
    static final String celestrakURL45 = "https://www.celestrak.com/NORAD/elements/planet.txt";
    static final String celestrakURL46 = "https://www.celestrak.com/NORAD/elements/spire.txt";
    static final String celestrakURL47 = "https://www.celestrak.com/NORAD/elements/ses.txt";
    static final String celestrakURL48 = "https://www.celestrak.com/NORAD/elements/iridium-NEXT.txt";
    static final String celestrakURL49 = "https://celestrak.com/NORAD/elements/active.txt";
    static final String celestrakURL50 = "https://celestrak.com/NORAD/elements/tle-new.txt";
    // 3-line TLEs
    static final String amsat_bare = "https://www.amsat.org/tle/current/nasabare.txt";
    static final String planet_labs = "https://ephemerides.planet-labs.com/planet_mc.tle";
    static final String tleInfoURL1 = "https://www.tle.info/data/TLE.ZIP";
    static final String mccantsURL1 = "https://www.prismnet.com/~mmccants/tles/inttles.zip";
    static final String mccantsURL2 = "https://www.prismnet.com/~mmccants/tles/classfd.zip";
    static final String nssdcLinkBase = "https://nssdc.gsfc.nasa.gov/nmc/spacecraft/display.action?id=";
    static final String badLink = "BADLINK";
    // 3-line TLEs
    static final String mccantsFile_inttles_zip = "inttles.zip";
    // 3-line TLEs
    static final String mccantsFile_classfd_zip = "classfd.zip";
    // 3-line TLEs
    static final String tleinfo_zip = "TLE.ZIP";
    // 2-line TLEs
    // for some reason this URL returns a JSON version of all current tles
    static final String OLD_DATE = "1970-01-01 00:00:00";
    static final String STRING_ZERO = "0";

    static final String WITH_TLES = "withTLEs";
    // SharedPrefs key for the satellite asset file document number to determine if document is new
    static final String PREF_KEY_SATFILE_DOC_NUM = "pref_key_satfile_docnum";
    // SharedPrefs key to indicate we've read all the asset satellite files. If not, we'll try again the next time
    static final String PREF_KEY_SATFILES_READ_SUCCESS = "pref_key_satfiles_read_success";
    static final String PREF_KEY_ASSET_TLES_READ_SUCCESS = "pref_key_mccants_tles_read_success";
    //once we've successfully updated TLEs, store the time in SharedPrefs
    static final String PREF_KEY_UPDATED_CELESTRAK_STATUS_TIME = "pref_key_updated_celestrak_status_time";
    static final String PREF_KEY_UPDATING_STATUS = "pref_key_updating_status";
    static final String PREF_KEY_UPDATED_MCCANTS_TLES_TIME = "pref_key_updated_mccants_tles_time";
    static final String PREF_KEY_UPDATED_CELESTRAK_TLES_TIME = "pref_key_updated_celestrak_tles_time";
    static final String PREF_KEY_READING_SATFILES = "pref_key_reading_satfiles";
    static final String PREF_KEY_UPDATING_TLES = "pref_key_updating_tles";
    static final String STILL_LOADING_SATELLITE_FILES = "Still loading satellite files...";
    static final String PREFS_KEY_MAG_DECLINATION = "prefs_key_mag_declination";
    static final float PREFS_DEFAULT_MAG_DECLINATION = 0f;
    static final String PREFS_KEY_ALTITUDE = "prefs_key_altitude";
    static final String PREFS_KEY_LATITUDE = "prefs_key_latitude";
    static final String PREFS_KEY_LONGITUDE = "prefs_key_longitude";
    static final String PREFS_KEY_TIME = "prefs_key_time";
    static final String PREFS_DEFAULT_ALTITUDE = "-621.1";
    static final String PREFS_DEFAULT_LATITUDE = "37.1";
    static final String PREFS_DEFAULT_LONGITUDE = "-122.1";
    static final long PREFS_DEFAULT_TIME = 123456;
    static final String LOCATION_STATUS_UNKNOWN = "LOCATION_STATUS_UNKNOWN";
    static final String LOCATION_STATUS_NONE = "LOCATION_STATUS_NONE";
    static final String LOCATION_STATUS_OLD = "LOCATION_STATUS_OLD";
    static final String LOCATION_STATUS_OKAY = "LOCATION_STATUS_OKAY";
    // name of the album for storing sharing image
    static final String LOOK_UP = "LookUp";
    // key word in sub-category field of database indicating satellite is active
    static final String ACTIVE = "ACTIVE";
    //database key for the satellite kind from the file; GEO, LEO, DEEP
    public static final String GEO = "GEO";
    public static final String LEO = "LEO";
    public static final String DEEP = "DEEP";
    static final String CDEEP = "CDEEP";
    static final String CGEO = "CGEO";

    static final String KEY_SATELLITE_CATEGORY = "satellite_category";
    static final String KEY_THUMB = "icon_type";
    // keys to SharedPrefs boolean for which satellite categories to display
    static final String SHOW_TELEVISION_SETTING_KEY = "show_television_setting_key";
    static final String SHOW_RADIO_SETTING_KEY = "show_radio_setting_key";
    static final String SHOW_WEATHER_SETTING_KEY = "show_weather_setting_key";
    static final String SHOW_MILITARY_SETTING_KEY = "show_military_setting_key";
    static final String SHOW_HAM_SETTING_KEY = "show_ham_setting_key";
    static final String SHOW_COMMUNICATION_SETTING_KEY = "show_communication_setting_key";
    static final String SHOW_EARTHOBSERVING_SETTING_KEY = "show_earthobserving_setting_key";
    static final String SHOW_SCIENCE_SETTING_KEY = "show_science_setting_key";
    static final String SHOW_DATA_SETTING_KEY = "show_data_setting_key";
    static final String SHOW_GPS_SETTING_KEY = "show_gps_setting_key";
    static final String SHOW_ISS_SETTING_KEY = "show_iss_setting_key";
    static final String SHOW_INACTIVE_SATS_KEY = "show_inactive_sats_key";
    // Unique tag for the error dialog fragment
    static final String DIALOG_ERROR = "dialog_error";
    static final String STATE_RESOLVING_ERROR = "resolving_error";
    static final String PREFS_NAME = "look_shared_prefs";
    // Hashmap tags for satellite-click pop-up window
    static final String CC_MAP_KEY_NORAD_NUMBER = "cc_map_key_norad_number";
    static final String CC_MAP_KEY_SAT_NAME = "cc_map_key_satellite_name";
    static final long nanosPerMillis = 1000000;

    static final long ONE_SECOND = 1000;
    static final long ONE_MINUTE = 60 * ONE_SECOND;
    static final long TWENTY_FOUR_HOURS = 24 * 60 * ONE_MINUTE;
    static final long LOCATION_INTERVAL = ONE_MINUTE;

    // TLE refresh Times for LEO, GEO and DEEP satellite kinds
    static final long ONE_WEEK = 7 * TWENTY_FOUR_HOURS;
    static final long SIX_WEEKS = 6 * ONE_WEEK;
    static final long LOOKAFTERSTUFF_INITIAL_DELAY_TIME = ONE_SECOND;
    static final long LOOKAFTERSTUFF_REPEAT_TIME = 60 * ONE_SECOND;
    static final long RECALC_LOOKANGLES_REPEAT_TIME = 300;
    static final long RECALC_LOOKANGLES_INITIAL_DELAY_TIME = ONE_SECOND;
    // Max allowed duration for a "click", in milliseconds.
    static final int MAX_CLICK_DURATION = 650;
    //Max allowed distanceSwiped to move during a "click", in DP.
    static final int MAX_CLICK_DISTANCE = 15;

    static final String PREFS_KEY_SHOWN_PAUSEDMODE_HINT = "prefs_key_shown_pausedmode_hint";
    static final String PREFS_KEY_SHOWN_LIVEMODE_HINT = "prefs_key_shown_livemode_hint";
    static final String MOBILE_DATA_SETTING_KEY = "mobile_data_setting_key";
    static final String NAV_DRAWER_LIVE_MODE_KEY = "nav_drawer_live_mode_key";
    static final String PREFS_KEY_LIVE_MODE = "prefs_key_livemode";
    static final String PREFS_KEY_LOSAZ = "prefs_key_losaz";
    static final String PREFS_KEY_LOSEL = "prefs_key_losel";
    static final String PREFS_KEY_PANAZ = "prefs_key_panaz";
    static final String PREFS_KEY_PANEL = "prefs_key_panel";
    static final String PREFS_KEY_TEMP_PANAZ = "prefs_key_temp_panaz";
    static final String PREFS_KEY_TEMP_PANEL = "prefs_key_temp_panel";

    // codes indicating which Activity was launched and returned
    static final int PERMISSIONS_REQUEST_LOCATION = 51;
    static final int PERMISSIONS_REQUEST_CAMERA = 52;
    static final int REQUEST_CHECK_SETTINGS = 94;
    static final int REQUEST_CHANGE_LOCATION_SETTINGS = 92;
    static final int REQUEST_CHANGE_WIFI_SETTINGS = 93;
    // Request code to use when launching the resolution activity
    static final int REQUEST_RESOLVE_ERROR = 1001;
    // Request code to use when launching the navigation drawer activity
    static final int RC_NAV_DRAWER = 89;

    static final int PAID_VERSION = 5431;
    static final int FREE_VERSION = 1345;
    static final String FORMAT_3_1F = "%3.1f";
    static final String FORMAT_4_3F = "%4.3f";

    static final int MAX_ELEV = 90;
    static final double MAX_TOTAL_ZOOM = 6.;
    static final double MIN_TOTAL_ZOOM = 1.;
    static final double MAX_LIVE_ZOOM = 3.3;
    static final float DEF_LOS_AZIMUTH = 180f;
    static final float DEF_LOS_ELEV = 55f;
    static final double SPACETRACK_DATA_SIZE = 4.646;
    static final double MCCANTS_DATA_SIZE = .014 + .0235;
    static final String FUTURE_EPOCH = "999";

}
