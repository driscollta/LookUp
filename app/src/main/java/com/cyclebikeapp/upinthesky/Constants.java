package com.cyclebikeapp.upinthesky;

/**
 * Created by TommyD on 8/4/2016.
 */
public final class Constants {
        private Constants() {}

    public static final String SAT_TABLE = "satTable";
    public static final String SAT_DB = "satDataBase";
    //database key for the satellite kind from the file; GEO, LEO, DEEP
    public static final String DB_KEY_SAT_KIND = "satellite_kind";
    //database key for the Norad satellite number
    public static final String DB_KEY_NORAD_NUM = "norad_num";
    //database key for the satellite name as labeled in the file
    public static final String DB_KEY_SAT_NAME = "satellite_name";
    //database key for the satellite category from the file; TV, Communication, Weather, etc
    public static final String DB_KEY_SAT_CATEGORY = "satellite_category";
    //database key for the satellite sub-catgory from the file; use to indicate if a satellite is active or inactive
    public static final String DB_KEY_SAT_SUB_CATEGORY = "satellite_sub_category";
    //database key for the satellite information link
    public static final String DB_KEY_SAT_INFO_LINK = "satellite_info_link";
    //database key for the satellite brief description from file; to be displayed when user clicks on satellite
    public static final String DB_KEY_SAT_BRIEF_DESC = "satellite_brief_description";
    //database key for the satellite Two Line Element set; retrieved from Space-Track.Org
    public static final String DB_KEY_SAT_TLE1 = "satellite_tle_1";
    //database key for the satellite Two Line Element set, second String
    public static final String DB_KEY_SAT_TLE2 = "satellite_tle_2";
    //database key for the timestamp when the TLE is retrieved, used to determine if TLE should be updated (in seconds)
    public static final String DB_KEY_SAT_TLE_TIMESTAMP = "satellite_tle_timestamp";
    //database key for an undefined column that may be used later
    public static final String DB_KEY_OTHER = "satellite_other";
    public static final String DATE_TIME_Z_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // strip all double quote characters from the KIND, Category, Sub-Category, etc data in the file
    public static final String DOUBLE_QUOTE = "\"";
    // maximum number of satellites to retrieve TLEs for from SpaceTrack API
    public static final int MAX_NUM_TLE_QUERY = 400;

    public static final String OLD_DATE = "2001-01-01 00:00:00";
    public static final String JSON_KEY_NORAD_CAT_ID = "NORAD_CAT_ID";
    public static final String JSON_KEY_TLE_LINE_0 = "TLE_LINE0";
    public static final String JSON_KEY_TLE_LINE_1 = "TLE_LINE1";
    public static final String JSON_KEY_TLE_LINE_2 = "TLE_LINE2";
    public static final String JSON_KEY_EPOCH = "EPOCH";
    public static final String WITH_TLES = "withTLEs";
    // SharedPrefs key for the satellite asset file document number to determine if document is new
    public static final String PREF_KEY_SATFILE_DOC_NUM = "pref_key_satfile_docnum";
    // SharedPrefs key to indicate we've read all the asset satellite files. If not, we'll try again the next time
    public static final String PREF_KEY_SATFILES_READ_SUCCESS = "pref_key_satfiles_read_success";
    //once we've successfully updated TLEs, store the time in SharedPrefs
    public static final String PREF_KEY_UPDATED_TLES_TIME = "pref_key_updated_tles_time";
    public static final String PREF_KEY_READING_SATFILES = "pref_key_reading_satfiles";
    public static final String STILL_LOADING_SATELLITE_FILES = "Still loading satellite files...";
    public static final String PREFS_KEY_MAG_DECLINATION = "prefs_key_mag_declination";
    public static final float PREFS_DEFAULT_MAG_DECLINATION = 0f;
    public static final String PREFS_KEY_ALTITUDE = "prefs_key_altitude";
    public static final String PREFS_KEY_LATITUDE = "prefs_key_latitude";
    public static final String PREFS_KEY_LONGITUDE = "prefs_key_longitude";
    public static final String PREFS_KEY_TIME = "prefs_key_time";
    public static final String PREFS_DEFAULT_ALTITUDE = "-621.1";
    public static final String PREFS_DEFAULT_LATITUDE = "37.1";
    public static final String PREFS_DEFAULT_LONGITUDE = "-122.1";
    public static final long PREFS_DEFAULT_TIME = 123456;
    public static final String LOCATION_STATUS_UNKNOWN = "LOCATION_STATUS_UNKNOWN";
    public static final String LOCATION_STATUS_NONE = "LOCATION_STATUS_NONE";
    public static final String LOCATION_STATUS_OLD = "LOCATION_STATUS_OLD";
    public static final String LOCATION_STATUS_OKAY = "LOCATION_STATUS_OKAY";
    // name of the album for storing sharing image
    public static final String LOOK_UP = "LookUp";
    // key word in sub-category field of database indicating satellite is active
    public static final String ACTIVE = "ACTIVE";
    //database key for the satellite kind from the file; GEO, LEO, DEEP
    public static final String GEO = "GEO";
    public static final String LEO = "LEO";
    public static final String DEEP = "DEEP";

    public static final String KEY_SATELLITE_CATEGORY = "satellite_category";
    public static final String KEY_THUMB = "icon_type";
    // keys to SharedPrefs boolean for which satellite categories to display
    public static final String SHOW_TELEVISION_SETTING_KEY = "show_television_setting_key";
    public static final String SHOW_RADIO_SETTING_KEY = "show_radio_setting_key";
    public static final String SHOW_WEATHER_SETTING_KEY = "show_weather_setting_key";
    public static final String SHOW_MILITARY_SETTING_KEY = "show_military_setting_key";
    public static final String SHOW_HAM_SETTING_KEY = "show_ham_setting_key";
    public static final String SHOW_COMMUNICATION_SETTING_KEY = "show_communication_setting_key";
    public static final String SHOW_EARTHOBSERVING_SETTING_KEY = "show_earthobserving_setting_key";
    public static final String SHOW_SCIENCE_SETTING_KEY = "show_science_setting_key";
    public static final String SHOW_DATA_SETTING_KEY = "show_data_setting_key";
    public static final String SHOW_GPS_SETTING_KEY = "show_gps_setting_key";
    public static final String SHOW_ISS_SETTING_KEY = "show_iss_setting_key";
    public static final String SHOW_INACTIVE_SATS_KEY = "show_inactive_sats_key";
    // Unique tag for the error dialog fragment
    public static final String DIALOG_ERROR = "dialog_error";
    public static final String STATE_RESOLVING_ERROR = "resolving_error";
    public static final String PREFS_NAME = "look_shared_prefs";
    // Hashmap tags for satellite-click pop-up window
    public static final String CC_MAP_KEY_NORAD_NUMBER = "cc_map_key_norad_number";
    public static final String CC_MAP_KEY_SAT_NAME = "cc_map_key_satellite_name";

    public static final long ONE_SECOND = 1000;
    public static final long ONE_MINUTE = 60 * ONE_SECOND;
    public static final long TWENTY_FOUR_HOURS = 24 * 60 * ONE_MINUTE;
    // TLE refresh Times for LEO, GEO and DEEP satellite kinds
    public static final long ONE_WEEK = 7*TWENTY_FOUR_HOURS;
    public static final long SIX_WEEKS = 6*ONE_WEEK;
    public static final long LOOKAFTERSTUFF_INITIAL_DELAY_TIME = 10 * ONE_SECOND;
    public static final long LOOKAFTERSTUFF_REPEAT_TIME = 80 * ONE_SECOND;
    public static final long RECALC_LOOKANGLES_REPEAT_TIME = 1100;
    public static final long RECALC_LOOKANGLES_INITIAL_DELAY_TIME = ONE_SECOND;
    // Max allowed duration for a "click", in milliseconds.
    public static final int MAX_CLICK_DURATION = 650;
    //Max allowed distanceSwiped to move during a "click", in DP.
    public static final int MAX_CLICK_DISTANCE = 15;

    public static final String PREFS_KEY_SHOWN_PAUSEDMODE_HINT = "prefs_key_shown_pausedmode_hint";
    public static final String PREFS_KEY_SHOWN_LIVEMODE_HINT = "prefs_key_shown_livemode_hint";
    public static final String MOBILE_DATA_SETTING_KEY = "mobile_data_setting_key";
    public static final String NAV_DRAWER_LIVE_MODE_KEY = "nav_drawer_live_mode_key";
    public static final String PREFS_KEY_LIVE_MODE = "prefs_key_livemode";
    public static final String PREFS_KEY_LOSAZ = "prefs_key_losaz";
    public static final String PREFS_KEY_LOSEL = "prefs_key_losel";
    public static final String PREFS_KEY_PANAZ = "prefs_key_panaz";
    public static final String PREFS_KEY_PANEL = "prefs_key_panel";
    public static final String PREFS_KEY_TEMP_PANAZ = "prefs_key_temp_panaz";
    public static final String PREFS_KEY_TEMP_PANEL = "prefs_key_temp_panel";

    // codes indicating which Activity was launched and returned
    public static final int PERMISSIONS_REQUEST_LOCATION = 51;
    public static final int PERMISSIONS_REQUEST_CAMERA = 52;
    public static final int REQUEST_CHECK_SETTINGS = 94;
    public static final int REQUEST_CHANGE_LOCATION_SETTINGS = 92;
    public static final int REQUEST_CHANGE_WIFI_SETTINGS = 93;
    // Request code to use when launching the resolution activity
    public static final int REQUEST_RESOLVE_ERROR = 1001;
    // Request code to use when launching the navigation drawer activity
    public static final int RC_NAV_DRAWER = 89;

    public static final int PAID_VERSION = 5431;
    public static final int FREE_VERSION = 1345;

    public static final int MAX_ELEV = 90;
    public static final double MAX_TOTAL_ZOOM = 6.;
    public static final double MIN_TOTAL_ZOOM = 1.;
    public static final double MAX_LIVE_ZOOM = 3.3;
    public static final float DEF_LOS_AZIMUTH = 180f;
    public static final float DEF_LOS_ELEV = 55f;

}
