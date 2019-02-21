package com.cyclebikeapp.lookup;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SoundEffectConstants;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupMenu;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import satellite.Satellite;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.cyclebikeapp.lookup.Constants.CC_MAP_KEY_NORAD_NUMBER;
import static com.cyclebikeapp.lookup.Constants.CC_MAP_KEY_SAT_NAME;
import static com.cyclebikeapp.lookup.Constants.CDEEP;
import static com.cyclebikeapp.lookup.Constants.CGEO;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_INFO_LINK;
import static com.cyclebikeapp.lookup.Constants.DEEP;
import static com.cyclebikeapp.lookup.Constants.DEF_LOS_AZIMUTH;
import static com.cyclebikeapp.lookup.Constants.DEF_LOS_ELEV;
import static com.cyclebikeapp.lookup.Constants.FREE_VERSION;
import static com.cyclebikeapp.lookup.Constants.GEO;
import static com.cyclebikeapp.lookup.Constants.LEO;
import static com.cyclebikeapp.lookup.Constants.LOOKAFTERSTUFF_INITIAL_DELAY_TIME;
import static com.cyclebikeapp.lookup.Constants.LOOKAFTERSTUFF_REPEAT_TIME;
import static com.cyclebikeapp.lookup.Constants.MAX_CLICK_DISTANCE;
import static com.cyclebikeapp.lookup.Constants.MAX_CLICK_DURATION;
import static com.cyclebikeapp.lookup.Constants.MAX_ELEV;
import static com.cyclebikeapp.lookup.Constants.MAX_LIVE_ZOOM;
import static com.cyclebikeapp.lookup.Constants.MAX_TOTAL_ZOOM;
import static com.cyclebikeapp.lookup.Constants.MCCANTS_DATA_SIZE;
import static com.cyclebikeapp.lookup.Constants.MIN_TOTAL_ZOOM;
import static com.cyclebikeapp.lookup.Constants.MOBILE_DATA_SETTING_KEY;
import static com.cyclebikeapp.lookup.Constants.NAV_DRAWER_LIVE_MODE_KEY;
import static com.cyclebikeapp.lookup.Constants.ONE_MINUTE;
import static com.cyclebikeapp.lookup.Constants.PAID_VERSION;
import static com.cyclebikeapp.lookup.Constants.PERMISSIONS_REQUEST_CAMERA;
import static com.cyclebikeapp.lookup.Constants.PERMISSIONS_REQUEST_LOCATION;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_LIVE_MODE;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_LOSAZ;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_LOSEL;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_PANAZ;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_PANEL;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_SHOWN_LIVEMODE_HINT;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_SHOWN_PAUSEDMODE_HINT;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_TEMP_PANAZ;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_TEMP_PANEL;
import static com.cyclebikeapp.lookup.Constants.PREFS_NAME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATED_CELESTRAK_TLES_TIME;
import static com.cyclebikeapp.lookup.Constants.PREF_KEY_UPDATING_TLES;
import static com.cyclebikeapp.lookup.Constants.RC_NAV_DRAWER;
import static com.cyclebikeapp.lookup.Constants.RECALC_LOOKANGLES_INITIAL_DELAY_TIME;
import static com.cyclebikeapp.lookup.Constants.RECALC_LOOKANGLES_REPEAT_TIME;
import static com.cyclebikeapp.lookup.Constants.REQUEST_CHANGE_LOCATION_SETTINGS;
import static com.cyclebikeapp.lookup.Constants.REQUEST_CHANGE_WIFI_SETTINGS;
import static com.cyclebikeapp.lookup.Constants.REQUEST_CHECK_SETTINGS;
import static com.cyclebikeapp.lookup.Constants.REQUEST_RESOLVE_ERROR;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_LINK;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_MESSAGE;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_NORAD_NUMBER;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_NUM_SEARCH_RESULTS;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_STATUS;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_TITLE;
import static com.cyclebikeapp.lookup.Constants.SPACETRACK_DATA_SIZE;
import static com.cyclebikeapp.lookup.Constants.TWENTY_FOUR_HOURS;
import static com.cyclebikeapp.lookup.Constants.badLink;
import static com.cyclebikeapp.lookup.Constants.nssdcLinkBase;
import static com.cyclebikeapp.lookup.SatelliteDialogFragment.MAIN;
import static com.cyclebikeapp.lookup.Util.getDeclinationFromSharedPrefs;
import static com.cyclebikeapp.lookup.Util.readingSatFiles;
import static com.cyclebikeapp.lookup.Util.satelliteFilesWereRead;
import static com.cyclebikeapp.lookup.Util.updatingTLEs;

@SuppressWarnings("ConstantConditions")
public class MainActivity extends AppCompatActivity
        implements SurfaceHolder.Callback, SensorEventListener, PopupMenu.OnMenuItemClickListener {
    // ** change these values depending on APK type (DEVELOPMENT OR PRODUCTION)
    public static final int version = PAID_VERSION;
    public static final boolean DEBUG = true;
    public static final boolean WRITE_ALLSATSFILE = false;
    public static final boolean WRITE_ALLTLES = true;
    // Timers and Handlers for periodic tasks
    private Handler lookAfterStuffHandler;
    private Timer lookAfterStuffTimer;
    private TimerTask lookAfterStuff;
    private Handler recalcLookAnglesHandler;
    private Timer recalcLookAnglesTimer;
    private TimerTask recalcLookAnglesTask;
    // all the database functions
    private SATDBAdapter dataBaseAdapter = null;
    // Class that interfaces to g4pz library for satellite calculations
    private SatelliteTabulator mSatCalculator;
    // all the Location functions
    private LocationHelper mLocationHelper;
    // A handle to the View in which the satellites are plotted
    private GridSatView mGridSatView;
    // this is where we put the camera preview
    private SurfaceHolder mHolder;
    // a Class to set-up and maintain the camera preview in LiveMode
    private CameraPreview mCameraPreview;
    private SensorManager mSensorManager;
    private Sensor sensorGravity;
    private Sensor sensorAccel;
    private Sensor sensorMagField;
    // data from Sensors
    private float[] mGravityVector;
    private float[] mMagFieldValues;
    // some Android devices don't have a compass
    private boolean hasMagSensor;
    // gesture detector to handle pinch zooming for camera and satellite canvas
    private ScaleGestureDetector mScaleDetector;

    // low pass filter parameter, depending on gravity- or accelerometer-type sensor
    private double accelFilterAlpha;
    // display once-per-app-launch user-requests about Services' configuration
    private boolean complainedLocationOld;
    private boolean haveCheckedForUpdatedTLEs;
    private boolean complainedMagSensor;
    private boolean complainedCameraPermission;
    // pop-up windows directing user to make System Settings changes
    private Snackbar mMobileDataPermissionSnackbar;
    private Snackbar mWirelessSettingsSnackbar;
    private Snackbar mHintSnackBar;
    private Snackbar mLocationSettingsSnackBar;
    private Snackbar mCameraPermissionSnackBar;
    // prevent multiple access to AsyncTask for rebuilding satellites
    private boolean rebuildingSatellites;
    private ArrayList<Snackbar> snackBarList;

    /**
     * Invoked when the Activity is created.
     *
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG){Log.w(this.getClass().getName(), "onCreate()");}
        mLocationHelper = new LocationHelper(getApplicationContext());
        // create satDB
        dataBaseAdapter = new SATDBAdapter(getApplicationContext());
        dataBaseAdapter.open();
        snackBarList = new ArrayList<>();
        //test if GooglePlay Services is available and up to date
        googlePlayAvailable(getApplicationContext());
        mSatCalculator = new SatelliteTabulator(this);
        // Handlers and Timers for repeating tasks
        lookAfterStuffHandler = new Handler();
        recalcLookAnglesHandler = new Handler();
        lookAfterStuffTimer = new Timer();
        recalcLookAnglesTimer = new Timer();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGridSatView = new GridSatView(this, null);

        mCameraPreview = new CameraPreview(this);
        mHolder = mCameraPreview.getHolder();
        // we handle all the changes to the camera preview surface from MainActivity
        mHolder.addCallback(this);
        setContentView(R.layout.activity_main);
        addContentView(mCameraPreview, new FrameLayout.LayoutParams
                (FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        addContentView(mGridSatView, new FrameLayout.LayoutParams
                (FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        rebuildingSatellites = false;

        // load GEO_withTLEs satDB data from file and update DB; wait before executing so database opens
        mGridSatView.postDelayed(new Runnable() {
            @Override
            public void run() {
                new InitGEOwithTLESatsBackground().execute();
            }
        }, 50);
        // When apk is first loaded we have to read the _withTLEs file and build the dataBase for GEO satellites.
        // rebuild the GEO satellites just before we start LookAfterStuff, so we show the user something while waiting
        // to read other satellite files and retrieving TLEs (in paid version).
        mGridSatView.invalidate();
        // "complaining" means showing a Snackbar dialog
        complainedLocationOld = false;
        // we don't update TLEs in Free Version, so don't ever complain
        haveCheckedForUpdatedTLEs = (version == FREE_VERSION);
        // some devices don't have a magnetic compass; we'll pop-up a dialog to complain that Live Mode doesn't work
        complainedMagSensor = false;
        complainedCameraPermission = false;
        //handles pinch zoom input
        mScaleDetector = new ScaleGestureDetector(getApplicationContext(), scaleGestureListener());
        // re-read sat files from assets in case there's a new one
        Util.setReadSatFilesFlag(getSharedPreferences(PREFS_NAME, 0));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
    private boolean askLocationPermission() {
        if (Util.hasFineLocationPermission(getApplicationContext())) {
            if (DEBUG) Log.i(this.getClass().getName(), "ask Location permission: has location permission");
            return true;
        } else {
            // Request permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationHelper.stopLocationUpdates();
                    mLocationHelper.startLocationUpdates();
                }
            }//location permissions case
            break;
            case PERMISSIONS_REQUEST_CAMERA: {
                // restart camera
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCameraPreview.stopPreview();
                    if (mCameraPreview.mCamera == null) {
                        mCameraPreview.connectCamera(mHolder);
                    }
                    mCameraPreview.setPreviewSize();
                    // we're told to find camera zoom ratios after changing the Preview Size; zoom ratios may be different for different sizes
                    mCameraPreview.configureCamera();
                    if (mGridSatView.isLiveMode()) {
                        if (DEBUG) {Log.w(this.getClass().getName(), "starting camera preview");}
                        mCameraPreview.startPreview();
                    }
                }
            }// camera permissions case
            break;
        }// switch
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case RC_NAV_DRAWER:
                if (DEBUG) {
                    Log.w(this.getClass().getName(), "returning from Nav_Drawer Activity");
                }
                // user may have changed satellite categories, re-build satellites, but wait until DB re-opens
                if (!rebuildingSatellites) {
                    // have to set this flag to rebuild GEOs too
                    dataBaseAdapter.setRebuildGEOSatellites(true);
                    dataBaseAdapter.setRebuildCGEOSatellites(true);
                    mGridSatView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //new RebuildSatellitesBackground().execute();
                            rebuildSatellites();
                        }
                    }, 100);
                }
                if (resultCode == RESULT_OK) {
                    boolean liveMode = data.getExtras().getBoolean(NAV_DRAWER_LIVE_MODE_KEY);
                    mGridSatView.setLiveMode(liveMode);
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
                    editor.putBoolean(PREFS_KEY_LIVE_MODE, mGridSatView.isLiveMode()).apply();
                }
                break;
            case REQUEST_RESOLVE_ERROR:

                break;
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made user changed location settings
                        if (!rebuildingSatellites) {
                            dataBaseAdapter.setRebuildGEOSatellites(true);
                            dataBaseAdapter.setRebuildCGEOSatellites(true);
                            mGridSatView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //new RebuildSatellitesBackground().execute();
                                    rebuildSatellites();
                                }
                            }, 100);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change Location settings, but chose not to
                         break;
                    default:
                        break;
                }
                break;

            case REQUEST_CHANGE_LOCATION_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        mLocationHelper.stopLocationUpdates();
                        mLocationHelper.startLocationUpdates();
                        // All required changes were successfully made user changed location settings
                        if (!rebuildingSatellites) {
                            dataBaseAdapter.setRebuildGEOSatellites(true);
                            dataBaseAdapter.setRebuildCGEOSatellites(true);
                            mGridSatView.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //new RebuildSatellitesBackground().execute();
                                    rebuildSatellites();
                                }
                            }, 100);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change Location settings, but chose not to
                         break;
                    default:
                        break;
                }
                break;
            case REQUEST_CHANGE_WIFI_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        // now that we have a data connection & presumably user has given permission, update TLEs
                        if (version == PAID_VERSION){dataBaseAdapter.updateTLEsAsync(getApplicationContext());}
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change WiFi settings, but chose not to
                        // pretend that we've updated TLEs today so LookAfterStuff doesn't keep asking
                        SharedPreferences.Editor editor1 = getSharedPreferences(PREFS_NAME, 0).edit();
                        editor1.putLong(PREF_KEY_UPDATED_CELESTRAK_TLES_TIME, System.currentTimeMillis()).apply();
                         break;
                    default:
                        break;
                }
                break;
        }
    }

    /**
     * User has clicked in the Menu floating action button;
     * 1) save a previewImage if we're in LiveMode
     * 2) save the sharingImage that looks like the screen without FABs
     * 3) start the NavigationDrawer Activity
     */
    private void doMenuClick() {
        //start NavigationDrawerActivity
        if (mGridSatView.isLiveMode()) {
            // we are live, capture previewImage and save sharingImage
            mCameraPreview.startPreviewCallback(mGridSatView);
            // set flag if previewImage is portrait mode
            setPreviewImageRotation();
        } else {
            // we are paused, just save the sharingImage, don't have to capture a new one
            mGridSatView.saveSharingImage = true;
            mGridSatView.invalidate();
        }
        mGridSatView.setLiveMode(false);
        final Intent navigationDrawerIntent = new Intent(this, NavDrawerFabActivity.class);
        // delay navigationDrawer activity until sharing image is saved
        mGridSatView.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigationDrawerIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(navigationDrawerIntent, RC_NAV_DRAWER);
            }
        }, 100);
    }

    /**
     * If we're in Portrait Mode, the previewImage is rotated on its side
     * we'll have to correct this when using the preview image as background or for the sharingImage
     */
    private void setPreviewImageRotation() {
        boolean previewImageRotation = false;
        if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_0) {
            previewImageRotation = true;
        }
        mGridSatView.setPreviewImageRotation(previewImageRotation);
    }

    /**
     * User clicked in the Play (or Pause) button
     * If we were Live, save a previewImage. If we were Paused,
     * start the camera Preview Mode and reset pan and zoom parameters
     */
    private void doPlayPauseClick() {
         if (mGridSatView.isLiveMode()) {
            //capture a previewImage to use for sharingImage and under canvas when panning and zooming
            mCameraPreview.startPreviewCallback(mGridSatView);
            // set flag if previewImage is portrait mode
            setPreviewImageRotation();
            mGridSatView.setLiveMode(false);
        } else {// we will be Live
            mCameraPreview.startPreview();
            mGridSatView.setLiveMode(true);
            // now in liveMode where we don't pan, so reset pan parameters
            mGridSatView.resetPan();
            // now in LiveMode only zoom using the camera; set pausedZoom = 1;
            mGridSatView.setPausedZoomFactor(1.);
            mGridSatView.invalidate();
        }
    }

    @Override
    protected void onDestroy() {
        if (DEBUG){Log.w(this.getClass().getName(), "onDestroy()");}
        super.onDestroy();
        mLocationHelper.stopLocationUpdates();
    }
    @Override
    protected void onStart() {
        if (DEBUG) { Log.w(this.getClass().getName(), "onStart()"); }
        super.onStart();
    }
    @Override
    protected void onResume() {
        if (DEBUG){Log.w(this.getClass().getName(), "onResume()");}
        super.onResume();
        // if we don't have an internet connection, WiFi or Mobile, but we have fine location permission and GPS location enabled,
        // try to get location from GPS; default is location from Network provider.
        // If that is disabled and can't get GPS Location, we'll ask to correct later.
        if (askLocationPermission()) {
            mLocationHelper.stopLocationUpdates();
            mLocationHelper.startLocationUpdates();
        }
        initializeSnackBars();
        // we close the database during onStop to release resources; reconnect to DB here if it's closed
        try {
            if (dataBaseAdapter != null && dataBaseAdapter.isClosed()) {
                dataBaseAdapter.open();
            }
        } catch (SQLException ignored) {
        }
        // may have to connect camera if mCamera is null? or disconnected
/*        if (mCameraPreview.mCamera == null){
            mCameraPreview.connectCamera(mHolder);
        }*/
        setLocationStatus();
        startLookingAfterStuff();
        startRecalculatingLookAngles();
        restoreSharedPreferences();
        registerSensorListeners(SensorManager.SENSOR_DELAY_GAME);
    }

    private void restoreSharedPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mGridSatView.setLiveMode(settings.getBoolean(PREFS_KEY_LIVE_MODE, true));
        // GEO satellites will be at 180 for Northern Latitudes; use that as default
        mGridSatView.setLosAzDeg(settings.getFloat(PREFS_KEY_LOSAZ, DEF_LOS_AZIMUTH));
        mGridSatView.setLosElDeg(settings.getFloat(PREFS_KEY_LOSEL, DEF_LOS_ELEV));
        mGridSatView.tempPanAz = settings.getFloat(PREFS_KEY_TEMP_PANAZ, 0f);
        mGridSatView.tempPanEl = settings.getFloat(PREFS_KEY_TEMP_PANEL, 0f);
        mGridSatView.panAz = settings.getFloat(PREFS_KEY_PANAZ, 0f);
        mGridSatView.panEl = settings.getFloat(PREFS_KEY_PANEL, 0f);
        // copy magDeclination to GridSatView so it can correct longitude line labels
        mGridSatView.magDeclination = getDeclinationFromSharedPrefs(getApplicationContext());
    }

    /**
     * This is how we'll find satellites given a click on the canvas.
     * If there is only one satellite within the click-tolerance, we'll pop-up the satellite dialog.
     * If there are more than one, pop-up a context menu listing the satellite names. When user chooses,
     * get Norad Number and fetchDeviceData to pop-up satelliteDialog.
     * Feed this from onTouchListener where lookAngAz, lookAngEl are provided. tolerance depends on screen pixel density
     */
    private void handleSatelliteCanvasClick(float lookAngAz, float lookAngEl, float tolerance) {
        final ArrayList<HashMap<String, String>> clickSatList = getCanvasClickSatellites(lookAngAz, lookAngEl, tolerance);
        // how many satellites did I find?  print Name, Norad num
        if (clickSatList.size() > 0) {
            if (clickSatList.size() > 1){
                //situate a View in the Layout to anchor this pop-up
                final View aView = findViewById(R.id.popup_anchor);
                if (aView != null) {
                    aView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showPopupMenu(aView, clickSatList);
                        }
                    },50);
                }
            } else {
                int selectedItem = 0;
                int noradNumber = Integer.valueOf(clickSatList.get(selectedItem).get(CC_MAP_KEY_NORAD_NUMBER));
                String[] satString = dataBaseAdapter.fetchDeviceData(noradNumber);
                // now using Norad num show satellite dialog with name, brief description and link
                String title = satString[0];
                String message = satString[1];
                String link = satString[2];
                String status = satString[5];
                showSatelliteDialogFragment(status, title, message, link, noradNumber);
            }
        }
    }

    private void showPopupMenu(View v, ArrayList<HashMap<String, String>> clickSatList) {
        // Don't leave a pop-up on screen if we're leaving MainActivity
        if (MainActivity.this.isFinishing()) {
            return;
        }
        PopupMenu popup = new PopupMenu(MainActivity.this, v);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(MainActivity.this);
        //add items to pop-up depending on # satellites
        int order = 1;
        // only 1 group
        int groupId = 0;
        for (HashMap<String, String> hm : clickSatList) {
            // use "itemId" entry to store Norad number.
            // When user clicks on List we're given itemId and we can then retrieve Norad Number
            int itemId = Integer.parseInt(hm.get(CC_MAP_KEY_NORAD_NUMBER));
            popup.getMenu().add(groupId, itemId, order, hm.get(CC_MAP_KEY_SAT_NAME));
            order++;
        }
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.actions, popup.getMenu());
        popup.show();
    }

    /**
     * User clicked on the pop-up list of satellites where user clicked on canvas
     *
     * @param item the item number clicked
     * @return true to say this pop-up has been dismissed
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int noradNumber = item.getItemId();
        String[] satString = dataBaseAdapter.fetchDeviceData(noradNumber);
        // now using Norad num show pop-up dialog with name, brief description and link
        String title = satString[0];
        String message = satString[1];
        String link = satString[2];
        String status = satString[5];
        showSatelliteDialogFragment(status, title, message, link, noradNumber);
        return true;
    }

    /**
     * Return a list of hashmaps containing satellite info when a user clicks on the canvas.
     * The hashmap has the satellite name, Norad number. The satellite name will appear in a
     * drop-down list if there is more than one target in the click region. The Norad number
     * will be used to extract info from the database for the satellite dialog.
     *
     * @param lookAngAz is the azimuth angle of the canvas click spot
     * @param lookAngEl is the elevation angle of the canvas click spot
     * @param tol       is the angle tolerance of the canvas click;
     *                  this depends on the canvas scale and zoom factor
     * @return the ArrayList; could be null if no satellites in the click spot
     */
    //todo move this to GridSatView
    private ArrayList<HashMap<String, String>> getCanvasClickSatellites(float lookAngAz, float lookAngEl, float tol) {
        ArrayList<HashMap<String, String>> returnList = new ArrayList<>();
        if (mGridSatView.mGEOSatellites != null) {
            for (Satellite aSat : mGridSatView.mGEOSatellites) {
                addSatToList(lookAngAz, lookAngEl, tol, returnList, aSat);
            }
        }
        if (mGridSatView.mLEOSatellites != null && version == PAID_VERSION) {
            for (Satellite aSat : mGridSatView.mLEOSatellites) {
                addSatToList(lookAngAz, lookAngEl, tol, returnList, aSat);
            }
        }
        if (mGridSatView.mDeepSatellites != null && version == PAID_VERSION) {
            for (Satellite aSat : mGridSatView.mDeepSatellites) {
                addSatToList(lookAngAz, lookAngEl, tol, returnList, aSat);
            }
        }
        if (mGridSatView.mCDeepSatellites != null && version == PAID_VERSION) {
            for (Satellite aSat : mGridSatView.mCDeepSatellites) {
                addSatToList(lookAngAz, lookAngEl, tol, returnList, aSat);
            }
        }
        if (mGridSatView.mCGEOSatellites != null && version == PAID_VERSION) {
            for (Satellite aSat : mGridSatView.mCGEOSatellites) {
                addSatToList(lookAngAz, lookAngEl, tol, returnList, aSat);
            }
        }
        return returnList;
    }

    /**
     * See if we should add this satellite to the pop-up list when user clicks on the canvas
     *
     * @param lookAngAz  horizontal angle of screen center
     * @param lookAngEl  vertical angle of screen center
     * @param tol        a tolerance in angle such that we can't distinguish between two overlapping satellites
     * @param returnList an ArrayList of Hashmaps with satellite name and Norad number
     * @param aSat       the satellite to possibly add to the ArrayList
     */
    //todo move this to GridSatView
    private void addSatToList(float lookAngAz, float lookAngEl, float tol, ArrayList<HashMap<String, String>> returnList, Satellite aSat) {
        HashMap<String, String> hmItem = new HashMap<>();
        if (aSat.getTLE().getLookAngleAz() < lookAngAz + tol
                && aSat.getTLE().getLookAngleAz() > lookAngAz - tol
                && aSat.getTLE().getLookAngleEl() < lookAngEl + tol
                && aSat.getTLE().getLookAngleEl() > lookAngEl - tol) {
            hmItem.put(CC_MAP_KEY_SAT_NAME, aSat.getTLE().getName());
            hmItem.put(CC_MAP_KEY_NORAD_NUMBER, String.valueOf(aSat.getTLE().getCatnum()));
            returnList.add(hmItem);
        }
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        if (DEBUG){Log.w(this.getClass().getName(), "onPause()");}
        super.onPause();
        unRegisterSensorListeners();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PREFS_KEY_LIVE_MODE, mGridSatView.isLiveMode());
        editor.putFloat(PREFS_KEY_LOSAZ, mGridSatView.getLosAzDeg());
        editor.putFloat(PREFS_KEY_LOSEL,mGridSatView.getLosElDeg());
        editor.putFloat(PREFS_KEY_TEMP_PANAZ, mGridSatView.tempPanAz);
        editor.putFloat(PREFS_KEY_TEMP_PANEL, mGridSatView.tempPanEl);
        editor.putFloat(PREFS_KEY_PANAZ, mGridSatView.panAz);
        editor.putFloat(PREFS_KEY_PANEL, mGridSatView.panEl).apply();
    }

    @Override
    protected void onStop() {
        if (DEBUG){Log.w(this.getClass().getName(), "onStop()");}
        super.onStop();
        if (dataBaseAdapter != null) {
            dataBaseAdapter.close();
        }
        mLocationHelper.stopLocationUpdates();
        stopLookingAfterStuff();
        stopRecalculatingLookAngles();
        mCameraPreview.stopPreview();
        mCameraPreview.releaseCamera();
    }
    private void showSatelliteDialogFragment(String status,
            String dialog_title,
            String dialog_message,
            final String link,
            final int noradNumber) {
        String satelliteLocationString = getString(R.string.location_string, noradNumber);
        Bundle dialogBundle = new Bundle();
        dialogBundle.putCharSequence(SDF_KEY_TITLE, dialog_title);
        dialogBundle.putCharSequence(SDF_KEY_MESSAGE, satelliteLocationString + dialog_message);
        dialogBundle.putCharSequence(SDF_KEY_LINK, link);
        dialogBundle.putCharSequence(SDF_KEY_STATUS, status);
        dialogBundle.putInt(SDF_KEY_NORAD_NUMBER, noradNumber);
        dialogBundle.putInt(SDF_KEY_NUM_SEARCH_RESULTS, 0);
        SatelliteDialogFragment newFragment = SatelliteDialogFragment.newInstance(dialogBundle);
        newFragment.show(getFragmentManager(), MAIN);
    }

    public void doSatelliteDialogPostiveClick() {
        // User clicked OK button, just exit dialog
    }

    public void doSatelliteDialogNegativeClick(Bundle bundle) {
        if (MainActivity.version == Constants.FREE_VERSION) {
            // User clicked Upgrade button; start Intent to go to Play Store
            String appPackageName = "com.cyclebikeapp.lookup";
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                //Play Store app not found, use Browser
                Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
                PackageManager packageManager = this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        } else {
            String link = bundle.getString(Constants.SDF_KEY_LINK);
            int noradNumber = bundle.getInt(Constants.SDF_KEY_NORAD_NUMBER);
            // User clicked More Info button; start Intent to navigate to provided link
            String url = link.trim();
            if (!link.startsWith(getString(R.string.http_)) && !link.startsWith(getString(R.string.https_))) {
                url = getString(R.string.http_) + link;
            }
            // if link.contains "badLink", use NSSDC link based on Int'l code
            if (url.toUpperCase().contains(badLink)) {
                if (DEBUG) {
                    Log.w(this.getClass().getName(), "substituting NSSDC URL for bad link");
                }
                String[] someData = dataBaseAdapter.fetchDeviceData(noradNumber);
                String intlCode = someData[4];
                url = nssdcLinkBase + intlCode;
            }
            // also run link tester and label this as a bad link if error; replace dblink with "badLink"
            new TestURL().execute(url, String.valueOf(noradNumber));
            // if !hasWiFiInternetConnection && !hasMobileInternetPermission ask for mobileInternetPermission
            // else must be okay to send browseIntent. If no internet connection, browser will complain
            final Intent browseIntent = new Intent(Intent.ACTION_VIEW);
            browseIntent.setData(Uri.parse(url));
            final PackageManager packageManager = MainActivity.this.getPackageManager();
            if (!Util.hasWifiInternetConnection(getApplicationContext())
                    && !Util.hasMobileInternetPermission(getDefaultSharedPreferences(getApplicationContext()))) {
                // doesn't have Mobile data Permission

                mMobileDataPermissionSnackbar.setAction(R.string.allow, new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        //"Allow" clicked, change hasMobileDataPermission
                        SharedPreferences settings = getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(MOBILE_DATA_SETTING_KEY, "1").apply();
                        // now have permission to browse
                        if (browseIntent.resolveActivity(packageManager) != null) {
                            startActivity(browseIntent);
                        } else {
                            Log.w(this.getClass().getName(), getString(R.string.no_browser));
                        }
                    }
                }).show();
            } else { //already have Mobile Data permission, or WiFi connection
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        }
    }

    /**
     * recalculate look angles for fast-moving satellites; only in paid version
     */
    private void startRecalculatingLookAngles() {
        if (recalcLookAnglesTask != null){return;}
        recalcLookAnglesTask = new TimerTask() {
            @Override
            public void run() {
                recalcLookAnglesHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //it's nice to update locationStatus quickly here
                        mGridSatView.setLocationStatus(testSharedPrefsLocationIsCurrent());
                        if (version == PAID_VERSION) {
                            // if we're still loading satellites tell GridSatView to display message
                            mGridSatView.loadingSatellites = readingSatFiles(getSharedPreferences(PREFS_NAME, 0))
                                    || !satelliteFilesWereRead(getSharedPreferences(PREFS_NAME, 0));
                            mGridSatView.updatingTLEs = updatingTLEs(getSharedPreferences(PREFS_NAME, 0));

                            long startTime =  System.nanoTime();
                            long timeNow = Util.getNowTime();
                            int numSatellites = 0;
                            if (mGridSatView.getLocationStatus() == GridSatView.LOCATION_STATUS_OKAY
                                    || mGridSatView.getLocationStatus() == GridSatView.LOCATION_STATUS_OLD) {
                                if (mGridSatView.mLEOSatellites != null) {
                                    numSatellites += mGridSatView.mLEOSatellites.size();
                                    mSatCalculator.recalculateLookAngles(mGridSatView.mLEOSatellites,
                                            mLocationHelper.getMyLocation(), timeNow);
                                }
                                if (mGridSatView.mDeepSatellites != null) {
                                    numSatellites += mGridSatView.mDeepSatellites.size();
                                    mSatCalculator.recalculateLookAngles(mGridSatView.mDeepSatellites,
                                            mLocationHelper.getMyLocation(), timeNow);
                                }
                                if (mGridSatView.mCDeepSatellites != null) {
                                    numSatellites += mGridSatView.mCDeepSatellites.size();
                                    mSatCalculator.recalculateLookAngles(mGridSatView.mCDeepSatellites,
                                            mLocationHelper.getMyLocation(), timeNow);
                                }

                            }
                        }
                        // redraw the satellites
                        mGridSatView.invalidate();
                    }
                });// Runnable
            }
        };
        // repeat time should be fast enough to track LEO satellites
        // a Leo sat with period of 90 minutes that traverses 90 deg of my sky goes at 1 deg per minute
        // with a scale factor of 50 pixels per degree the sat moves 50 pixels per minute; update every 20 sec
        // initially wait a minute before doing this for the first time to let TLEs come in from Space-Track.org
        recalcLookAnglesTimer.schedule(recalcLookAnglesTask, RECALC_LOOKANGLES_INITIAL_DELAY_TIME, RECALC_LOOKANGLES_REPEAT_TIME);
    }

    private void stopRecalculatingLookAngles() {
        recalcLookAnglesHandler.removeCallbacksAndMessages(null);
        if (recalcLookAnglesTask != null) {
            recalcLookAnglesTask.cancel();
            recalcLookAnglesTask = null;
        }
    }

    private class InitGEOwithTLESatsBackground extends AsyncTask<Void, Void, Void> {
        /**
         * On first execution of app, load the GEOwithTLEs file into the database, then build satellites so
         * something shows up quickly. After this we can load the other satellite files and fetch TLEs.
         */
        @Override
        protected Void doInBackground(Void... params) {
            dataBaseAdapter.readGEOwithTLEsFileFromAsset(getApplicationContext());
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (DEBUG){Log.w(this.getClass().getName(), "finished reading GEOsatellites_withTLEs file");}
            rebuildSatellites();
        }
    }

    /**
     * Check url for a bad link when clicking the "More Info" button in a satellite Dialog
     * Use the same routine as downloading TLEs from Celestrak. It will throw an IOException
     * if the link is bad, so then replace the link in the dataBase with "badLink". The next time
     * the user clicks on the MoreInfo button we'll re-direct to the default link to NSSDC
     */
    public class TestURL extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... url) {
            GetTLEsFromCelestrak ctfc = new GetTLEsFromCelestrak();
            try {
                ctfc.downloadUrl(url[0]);
            } catch (IOException e) {
                if (DEBUG) { Log.w(this.getClass().getName(), "testing URL... bad link"); }
                int noradNumber = Integer.parseInt(url[1]);
                ContentValues badLinkContent = new ContentValues();
                badLinkContent.put(DB_KEY_SAT_INFO_LINK, badLink);
                dataBaseAdapter.updateSatelliteRecord(noradNumber, badLinkContent);
                return null;
            }
            if (DEBUG) { Log.w(this.getClass().getName(), "testing URL... seems okay"); }
            return null;
        }
    }

    private void rebuildSatellites() {
        SerialExecutor rebuildSats = new SerialExecutor();
        rebuildSats.execute(rebuildSatellitesRunnable);
        rebuildSats.execute(setNotRebuildingFlagRunnable);
    }

    private Runnable rebuildSatellitesRunnable = new Runnable() {

        @Override
        public void run() {
            //if mGEOSatellites size == 0, rebuild them because we didn't when we started-up, maybe because Location was null
            // (if we have an asset file _withTLEs_ this shouldn't happen)
            //if LocationHelper.rebuildSatellites, rebuild GEO satellites because we have a new Location
            //if dataBaseAdapter.rebuildSatellites, rebuild GEO satellites because we have new GEO TLEs
            // reset flag to rebuild GEOs
            // always rebuild LEO and DEEP satellites, except in free version
            // these may be null if the app quits before doInBackground finishes
            if (mLocationHelper == null || dataBaseAdapter == null || mLocationHelper.getMyLocation() == null) {
                return;
            }
            rebuildingSatellites = true;
            //use adjusted time to rebuild satellites, not System.currentTimeMillis()
            long timeNow = Util.getNowTime();
            if (mGridSatView.mGEOSatellites == null
                    || dataBaseAdapter.shouldRebuildGEOSatellites()) {
                mGridSatView.mGEOSatellites = mSatCalculator.buildSatellitesByKind(mLocationHelper.getMyLocation(),
                        timeNow, dataBaseAdapter, GEO);
                dataBaseAdapter.setRebuildGEOSatellites(false);
            }
            if (mGridSatView.mCGEOSatellites == null
                    || dataBaseAdapter.shouldRebuildCGEOSatellites()
                    && (version == PAID_VERSION)) {
                mGridSatView.mCGEOSatellites = mSatCalculator.buildSatellitesByKind(mLocationHelper.getMyLocation(),
                        timeNow, dataBaseAdapter, CGEO);
                dataBaseAdapter.setRebuildCGEOSatellites(false);
            }
            // only build LEO and DEEP satellites in paid version
            if (version == PAID_VERSION) {
                mGridSatView.mLEOSatellites = mSatCalculator.buildSatellitesByKind(mLocationHelper.getMyLocation(),
                        timeNow, dataBaseAdapter, LEO);
                mGridSatView.mDeepSatellites = mSatCalculator.buildSatellitesByKind(mLocationHelper.getMyLocation(),
                        timeNow, dataBaseAdapter, DEEP);
                mGridSatView.mCDeepSatellites = mSatCalculator.buildSatellitesByKind(mLocationHelper.getMyLocation(),
                        timeNow, dataBaseAdapter, CDEEP);
            }

        }
    };

    private Runnable setNotRebuildingFlagRunnable = new Runnable() {

        @Override
        public void run() {
            rebuildingSatellites = false;
        }
    };

    /**
     * 1) check that we have updated TLEs and if not, that we have a data connection
     * 2) Location is current and if not, that we have a Provider connection
     * 3) rebuild LEO & DEEP satellites so we catch rising-, and drop setting-satellites
     * 4) rebuild GEOs if we have a new Location or new TLEs
     */
    private void startLookingAfterStuff() {
        if (lookAfterStuff != null) {
            return;
        }
        lookAfterStuff = new TimerTask() {

            @Override
            public void run() {
                lookAfterStuffHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        rebuildSatellites();
                        // only read satellite files if we've read-in the GEO_withTLEs file and built those satellites; mGEOSatellites.size() > 1
                        // otherwise, we have to wait several minutes to read sat files and this blocks AsyncTask from building initial GEO satellites
                        if (DEBUG) {Log.w(this.getClass().getName(), "lookAfterStuff -sat files were read: "
                                + (satelliteFilesWereRead(getSharedPreferences(PREFS_NAME, 0))?"yes":"no")
                                + " reading  sat files: "
                                + (readingSatFiles(getSharedPreferences(PREFS_NAME, 0))?"yes":"no"));}
                        if (!satelliteFilesWereRead(getSharedPreferences(PREFS_NAME, 0))
                                && !readingSatFiles(getSharedPreferences(PREFS_NAME, 0))) {
                            // reload satDB data from file and update DB, but wait until reading GEO_withTLEs file
                            if (DEBUG) {Log.w(this.getClass().getName(), "re-reading SatFiles...");}
                            dataBaseAdapter.readSatFilesFromAssetAsync();
                        }
                        testLocationIsCurrent();
                        setLocationStatus();
                        testTLEsAreCurrent();
                        complainCameraPermission();
                        showSnackBarHints();
                    }
                });//Runnable
            }

            private void complainCameraPermission() {
                // if not Android M or greater, return
                // if we have camera permission, return
                // if any SnackBar is showing, return
                // if we've already complained, return
                // if we haven't complained about current Location or current TLEs, return
                if (DEBUG) {
                    Log.w(this.getClass().getName(), "BuildVersion: " + Build.VERSION.SDK_INT
                            + " version < M: " + (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? "yes" : "no")
                            + " hasCameraPermission: " + (Util.hasCameraPermission(MainActivity.this) ? "yes" : "no")
                            + " complainedCameraPermission: " + (complainedCameraPermission ? "yes" : "no")
                            + " haveCheckedForUpdatedTLEs: " + (haveCheckedForUpdatedTLEs ? "yes" : "no")
                            + " complainedLocationOld: " + (complainedLocationOld ? "yes" : "no"));
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Util.hasCameraPermission(MainActivity.this)) {
                    complainedCameraPermission = true;
                    return;
                }
                // wait until we've complained about Location and have updated TLEs
                // if we've already complained once since the app was first launched
                if (!complainedLocationOld || !haveCheckedForUpdatedTLEs || complainedCameraPermission
                        || Util.isAnySnackBarVisible(snackBarList)) {
                    return;
                }
                complainedCameraPermission = true;
                // show snack bar explaining why camera is needed, ask permission
                if (DEBUG) { Log.w(this.getClass().getName(), "Complaining about camera Permission"); }
                mCameraPermissionSnackBar.setAction(getString(R.string.allow), new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Show the request permissions dialog
                        // and check the result in onRequestPermissionsResult()
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSIONS_REQUEST_CAMERA);
                    }
                }).show();
            }

            private void testTLEsAreCurrent() {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor settingsEditor = settings.edit();
                // We don't update TLEs in free version
                if ((version == FREE_VERSION)) {
                    settingsEditor.putBoolean(PREF_KEY_UPDATING_TLES, false).apply();
                    return;
                }
                // 1) test if TLE update time is old (> 1 day)
                //      a) or we haven't loaded all satellites initially
                //      b) or we haven't built all the satellites;
                //      updateTLEsAsync would then return true even tho' we didn't really update anything
                // 2) check for WiFi data connection
                // 3) if no Wifi connection, ask user permission to update sat database:
                //      (1) estimate data size using ArrayLists size (2) don't ask again option
                // 4) if TLEs not current and have update permission and no data connection complain;
                // 5) call updateTLEsAsync if conditions have been met (have data connection & permission)

                // checkSpaceTrackTLEsNeedUpdating just tests the time of the last check for updates. We'll only check every day.
                boolean shouldUpdateTLEs = Util.checkCelestrakStatusNeedsUpdating(settings)
                        || mGridSatView.mDeepSatellites == null
                        || mGridSatView.mLEOSatellites == null
                        || mGridSatView.mLEOSatellites.size() < 1
                        || mGridSatView.mDeepSatellites.size() < 1;
                if (DEBUG) {
                    Log.w(this.getClass().getName(), "shouldUpdateTLEs: " + (shouldUpdateTLEs? "yes":"no")
                            + " TLEs updated: "
                            + Util.convertMsec2Date(settings.getLong(PREF_KEY_UPDATED_CELESTRAK_TLES_TIME, 0))
                            + ", or "
                            + String.format("%3.1f",
                            (System.currentTimeMillis()
                                    - settings.getLong(PREF_KEY_UPDATED_CELESTRAK_TLES_TIME, 0))/(60.*ONE_MINUTE))
                            + " hours ago" + " haveCheckedForUpdatedTLEs: " + (haveCheckedForUpdatedTLEs ? "yes":"no"));
                }
                if (shouldUpdateTLEs
                        && !updatingTLEs(settings)
                        && !readingSatFiles(settings)
                        && !haveCheckedForUpdatedTLEs) {
                    if (Util.hasWifiInternetConnection(getApplicationContext())) {
                        // update TLEs over WiFi without asking for permission
                        dataBaseAdapter.updateTLEsAsync(getApplicationContext());
                        haveCheckedForUpdatedTLEs = true;
                    } else if (Util.hasInternetConnection(getApplicationContext())) {
                        if (!Util.hasMobileDataPermission(getDefaultSharedPreferences(getApplicationContext()))) {
                            // doesn't have WiFi data, but has Internet, ask for permission
                            // get size of required data to alert user
                            Util.closeAllSnackbars(snackBarList);
                            double dataSize = MCCANTS_DATA_SIZE + SPACETRACK_DATA_SIZE;
                            StringBuilder snackBarString = new StringBuilder(getString(R.string.ask_mobile_data_permission));
                            snackBarString.append(String.format(Locale.getDefault(), "%3.3f", dataSize));
                            snackBarString.append(" MB");
                            // show snackbar to ask permission to update TLEs over mobile given data size
                            mMobileDataPermissionSnackbar = Snackbar.make(
                                    mGridSatView,
                                    snackBarString,
                                    Snackbar.LENGTH_INDEFINITE);
                            mMobileDataPermissionSnackbar.setAction(getString(R.string.allow), new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    SharedPreferences settings = getDefaultSharedPreferences(getApplicationContext());
                                    SharedPreferences.Editor editor = settings.edit();
                                    //change hasMobileDataPermission
                                    editor.putString(MOBILE_DATA_SETTING_KEY, "1").apply();
                                    // now have permission to update TLEs over mobile network
                                    dataBaseAdapter.updateTLEsAsync(getApplicationContext());
                                    haveCheckedForUpdatedTLEs = true;
                                }
                            }).show();
                        } else { //have Mobile Data & permission
                            dataBaseAdapter.updateTLEsAsync(getApplicationContext());
                            haveCheckedForUpdatedTLEs = true;
                        }
                    } else {
                        Util.closeAllSnackbars(snackBarList);
                        // no data connection-> complain and show snackbar with wirelessSettings Intent
                        mWirelessSettingsSnackbar.setAction(getString(R.string.open), new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                Intent viewIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                                startActivityForResult(viewIntent, REQUEST_CHANGE_WIFI_SETTINGS);
                            }
                        }).show();
                    }
                } else { // tles don't need updating
                    settingsEditor.putBoolean(PREF_KEY_UPDATING_TLES, false).apply();
                }
            }


            private void showSnackBarHints() {
                // 1a) if we've shown liveModeHint and we're in liveMode return
                // 1b) if we've shown pausedModeHint and we're in pausedMode return
                // 2) if we haven't complained that the location is old with a snackBar, return
                // 3) if we haven't complained that TLEs are old, return
                // 3a) if we haven't complained about camera permission in Android M, return
                // 4) if liveMode, show liveModeHintText, set liveModeHint SharedPref = true
                // 5) if pausedMode, show pausedModeHintText, set pausedModeHint SharedPref = true
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                if ((settings.getBoolean(PREFS_KEY_SHOWN_LIVEMODE_HINT, false)
                        && mGridSatView.isLiveMode())
                        || (settings.getBoolean(PREFS_KEY_SHOWN_PAUSEDMODE_HINT, false)
                        && !mGridSatView.isLiveMode())) {
                    return;
                }
                if (!complainedLocationOld || !haveCheckedForUpdatedTLEs || !complainedCameraPermission) {
                    return;
                }
                // don't over-write any other snackBars that haven't been dismissed
                if (Util.isAnySnackBarVisible(snackBarList)) {
                    return;
                }
                SharedPreferences.Editor editor = settings.edit();
                String snackBarText;
                if (mGridSatView.isLiveMode()) {
                    editor.putBoolean(PREFS_KEY_SHOWN_LIVEMODE_HINT, true).apply();
                    snackBarText = getResources().getString(R.string.livemode_hint);
                } else {
                    editor.putBoolean(PREFS_KEY_SHOWN_PAUSEDMODE_HINT, true).apply();
                    snackBarText = getResources().getString(R.string.pausedmode_hint);
                }
                // pass GridSatView so snackBar shows over surface
                mHintSnackBar = Snackbar.make(
                        mGridSatView, snackBarText,
                        Snackbar.LENGTH_INDEFINITE);
                mHintSnackBar.setAction(getString(R.string.ok), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mHintSnackBar.dismiss();
                    }
                }).show();
            }
        };
        // repeat time should be fast enough that we catch rising satellites.
        // a Leo sat with period of 90 minutes that traverses 90 deg of my sky goes at 1 deg per minute.
        // We probably don't care if its below 5 deg in elevation, so we can do this TimerTask every 1.5 minutes or so.
        // Initially wait about 5 sec before doing this for the first time to let dataBase build from asset files
        // and build the first iteration of satellites
        lookAfterStuffTimer.schedule(lookAfterStuff, LOOKAFTERSTUFF_INITIAL_DELAY_TIME, LOOKAFTERSTUFF_REPEAT_TIME);
    }

    private void stopLookingAfterStuff() {
        lookAfterStuffHandler.removeCallbacksAndMessages(null);
        if (lookAfterStuff != null) {
            lookAfterStuff.cancel();
            lookAfterStuff = null;
        }
    }

    private void googlePlayAvailable(Context context) {
        if (!Util.hasWifiInternetConnection(context)){
            return;
        }
        int googlePlayAvailableResponse = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (googlePlayAvailableResponse != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, googlePlayAvailableResponse, 0).show();
        }
    }

    private void testLocationIsCurrent() {

        if (askLocationPermission()) {
            if (DEBUG) {
                Log.i(this.getClass().getName(), "checkFusedLocationService()"
                        + (Util.requestingLocationUpdates(getApplicationContext()) ? " are already requesting" : " not requesting"));
            }
        }
    }

    private boolean areLocationServicesAvailable(Context context) {
        return Util.isGPSLocationEnabled(context)
                || Util.isNetworkLocationEnabled(context);
    }
    private int setLocationStatus() {
        int locationStatus;
        if (mLocationHelper.getMyLocation() == null) {
            // somehow LocationHelper doesn't have a Location; testing this prevents nullPointerException for other tests
            locationStatus = testSharedPrefsLocationIsCurrent();
        } else if (Util.locationIsDefault(mLocationHelper.getMyLocation())) {
            locationStatus = GridSatView.LOCATION_STATUS_NONE;
        } else if (System.currentTimeMillis() - mLocationHelper.getMyLocation().getTime() > TWENTY_FOUR_HOURS) {
            locationStatus = GridSatView.LOCATION_STATUS_OLD;
        } else {
            locationStatus = GridSatView.LOCATION_STATUS_OKAY;
        }
        mGridSatView.setLocationStatus(locationStatus);
        if (DEBUG) {
            Log.i(this.getClass().getName(), "setLocationStatus(): long - " +
                    String.format("%7.4f", mLocationHelper.getMyLocation().getLongitude())
                    + " lat - " + String.format("%7.4f", mLocationHelper.getMyLocation().getLatitude())
                    + " alt - " + String.format("%3.1f", mLocationHelper.getMyLocation().getAltitude())
                    + " time - " + new Date(mLocationHelper.getMyLocation().getTime()));
        }
        return locationStatus;
    }

    /**
     * Query the condition of the last known Location: "none", "old", or "okay"
     *
     * @return the integer value of LocationStatus
     */
    private int testSharedPrefsLocationIsCurrent() {
        Location aLoc = Util.getLocFromSharedPrefs(getSharedPreferences(PREFS_NAME, 0));
        int locationStatus;
        if (Util.locationIsDefault(aLoc)) {
            locationStatus = GridSatView.LOCATION_STATUS_NONE;
        } else if (System.currentTimeMillis() - aLoc.getTime() > TWENTY_FOUR_HOURS) {
            locationStatus = GridSatView.LOCATION_STATUS_OLD;
        } else {
            locationStatus = GridSatView.LOCATION_STATUS_OKAY;
        }
        return locationStatus;
    }

    /**
     * This is called immediately after the surface is first created.
     * Implementations of this should start up whatever rendering code
     * they desire.  Note that only one thread can ever draw into
     * a {@link }, so you should not draw into the Surface here
     * if your normal rendering will be in another thread.
     *
     * @param holder The SurfaceHolder whose surface is being created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mGridSatView.setSurfaceSize(holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
        mGridSatView.setFOVScale(mCameraPreview.getFOV());
    }

    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  You should at this point update
     * the imagery in the surface.  This method is always called at least
     * once, after {@link #surfaceCreated}.
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width  The new width of the surface.
     * @param height The new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        int cameraSensorOrientation = 90;
//use CameraInfo21 to see if camera sensor is upside down and modify surface orientation; write debug code to display parameters
//todo does surfaceChanged get called after onRequestPermissionsResult? if not, we'll have to save cameraSensorOrientation
        // todo and adjust setAzelConfigCorrection() when camera is on/off
        // if Build number > Lollipop can use CameraInfo21; get cameraSensorOrientation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            CameraInfo21 mCamInfo21 = new CameraInfo21(getApplicationContext());
            cameraSensorOrientation = mCamInfo21.findBackFacingCameraOrientation();
            if (DEBUG) {
                Log.e(this.getClass().getName(), "reading cameraSensorOrientation for Android M "
                        + " cameraSensorOrientation: " + cameraSensorOrientation);
            }
        }
        mGridSatView.setSurfaceSize(width, height);
        mGridSatView.setFOVScale(mCameraPreview.getFOV());
        mCameraPreview.stopPreview();
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int displayRotationDegrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                displayRotationDegrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                displayRotationDegrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                displayRotationDegrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                displayRotationDegrees = 270;
                break;// Landscape right
        }
        int displayRotationChange = (mCameraPreview.getCameraRotationOffset() - displayRotationDegrees + 360) % 360;
        mCameraPreview.setDisplayOrientation(displayRotationChange);
        mGridSatView.setAzelConfigCorrection(displayRotationChange - cameraSensorOrientation);
        if (DEBUG) {
            Log.e(this.getClass().getName(), "surfaceChanged() liveMode?" + (mGridSatView.isLiveMode() ? " yes" : " no")
                    + " displayRotation: " + displayRotationDegrees
                    + " displayRotationChange: " + displayRotationChange
                    + " cameraSensorOrientation: " + cameraSensorOrientation);
        }
            // if sensor orientation is not 0, add "so: " + cameraSensorOrientation to debug text
            // also add " dispRot: " + displayRotationDegrees
            // also add " dispRotChange: " + displayRotationChange
            mGridSatView.debugText = "camSO: " + cameraSensorOrientation
                    + " dispRot: " + displayRotationDegrees
                    + " dispRotChange: " + displayRotationChange;

        if (!mGridSatView.isLiveMode()){
            // gravity sensor would determine this in LiveMode
            //todo need to preserve screenOrientation from accel sensor so grid lines are the same as in LiveMode
            mGridSatView.setScreenRotation(displayRotationDegrees);
        }
        try {
            mCameraPreview.mCamera.setPreviewDisplay(mHolder);
        // Exception is most likely no camera permission
        } catch (Exception ignore) {

        }
        mCameraPreview.setPreviewSize();
        // we're told to find camera zoom ratios after changing the Preview Size; zoom ratios may be different for different sizes
        mCameraPreview.configureCamera();
        if (mGridSatView.isLiveMode()) {
            mCameraPreview.startPreview();
        }
        // we've re-oriented the screen, re-draw the grid and satellites.
        mGridSatView.invalidate();
    }

    /**
     * This is called immediately before a surface is being destroyed. After
     * returning from this call, you should no longer try to access this
     * surface.  If you have a rendering thread that directly accesses
     * the surface, you must ensure that thread is no longer touching the
     * Surface before returning from this function.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    /**
     * Need magnetic field sensor to determine which way screen is pointing.
     * Need either a gravity sensor or an accelerometer sensor to find screen orientation and elevation angle of z-axis
     * If we find a gravity sensor, use that. If there is no gravity sensor, use an accelerometer
     *
     * @param sensorSpeed desired update rate for sensors
     */
    private void registerSensorListeners(int sensorSpeed) {
        mGravityVector = new float[3];
        sensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        // the Gravity Sensor already filters raw data, so only use a mild low-pass filter
        accelFilterAlpha = .4;
        if (sensorGravity == null) {
            sensorAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sensorAccel != null) {
                mSensorManager.registerListener(this, sensorAccel, sensorSpeed);
                // the Accelerometer Sensor is noisy, so use an agressive low-pass filter
                accelFilterAlpha = .96;
            }
        } else {
            // prefer to use the Gravity Sensor
            mSensorManager.registerListener(this, sensorGravity, sensorSpeed);
        }
        mMagFieldValues = new float[3];
        sensorMagField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (sensorMagField != null) {
            hasMagSensor = mSensorManager.registerListener(this, sensorMagField, sensorSpeed);
        }
        if (!hasMagSensor && !complainedMagSensor) {
            complainedMagSensor = true;
            if (mGridSatView.isLiveMode()) {
                mGridSatView.setLosAzDeg(180f);
            }
            noCompassDialog(getResources().getString(R.string.no_compass_title), getResources().getString(R.string.no_compass_message));
        }
    }

    private void unRegisterSensorListeners() {
        mGravityVector = null;
        mMagFieldValues = null;
        mSensorManager.unregisterListener(this);
        if (sensorGravity == null) {
            mSensorManager.unregisterListener(this, sensorAccel);
        } else {
            mSensorManager.unregisterListener(this, sensorGravity);
        }
        if (sensorMagField != null) {
            mSensorManager.unregisterListener(this, sensorMagField);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // dismiss any snackbars showing when touchEvent occurs
        if (mWirelessSettingsSnackbar != null && mWirelessSettingsSnackbar.isShown()){
            mWirelessSettingsSnackbar.dismiss();
        } else if (mMobileDataPermissionSnackbar != null && mMobileDataPermissionSnackbar.isShown()){
            mMobileDataPermissionSnackbar.dismiss();
        } else  if (mLocationSettingsSnackBar != null && mLocationSettingsSnackBar.isShown()){
            mLocationSettingsSnackBar.dismiss();
        } else  if (mCameraPermissionSnackBar != null && mCameraPermissionSnackBar.isShown()){
            mCameraPermissionSnackBar.dismiss();
        }
        //handle a zoom event with the ScaleDetector
        mScaleDetector.onTouchEvent(event);
        // don't try to pan or click if we're zooming
        if (!mScaleDetector.isInProgress()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mGridSatView.pressStartTime = System.currentTimeMillis();
                    mGridSatView.stayedWithinClickDistance = true;
                    mGridSatView.touchDownX = event.getX();
                    mGridSatView.touchDownY = event.getY();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    //this event is intercepted by mScaleDetector
                    mGridSatView.isZooming = true;
                    break;
                case MotionEvent.ACTION_UP:
                    // detect click if ACTION_DOWN coords are not much different than ACTION_UP coords and timing is short
                    long pressDuration = System.currentTimeMillis() - mGridSatView.pressStartTime;
                    if (pressDuration < MAX_CLICK_DURATION && mGridSatView.stayedWithinClickDistance) {
                        // Click event has occurred. Just call these to perform feedback that user has specified
                        handleClickEvent();
                    } else {
                        //was not a click event, handle the end of a pan event
                        if (!mGridSatView.isZooming) {
                            handlePanEventEnd(event);
                        }
                    }
                    mGridSatView.isZooming = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    //if I was within clickDistance and now I've moved too far, reject this as a click
                    if (mGridSatView.stayedWithinClickDistance
                            && distanceSwiped(mGridSatView.touchDownX, mGridSatView.touchDownY,
                            event.getX(), event.getY()) > MAX_CLICK_DISTANCE) {
                        mGridSatView.stayedWithinClickDistance = false;
                    }
                    pressDuration = System.currentTimeMillis() - mGridSatView.pressStartTime;
                    if (pressDuration > MAX_CLICK_DURATION
                            || !mGridSatView.stayedWithinClickDistance) {
                        // either we've moved out of the "click distance"
                        // or waited until we're sure this is not a click event, then start panning if not in LiveMode
                        if (!mGridSatView.isZooming) {
                            handleOngoingPanEvent(event);
                        }
                    }
                    break;
            }
        } else {
            // scaleDetector is handling the touch event
            mGridSatView.isZooming = true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Touch event was click, handle menu button, play/pause button or satellite clicks
     */
    private void handleClickEvent() {
        mGridSatView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        mGridSatView.playSoundEffect(SoundEffectConstants.CLICK);
        if (mGridSatView.clickIsInPlayPauseButton()) {
            doPlayPauseClick();
        } else if (mGridSatView.clickIsInMenuButton()) {
            doMenuClick();
        } else {
            // click was somewhere in canvas, maybe in a satellite
            // convert click coords to az,el coords - depends on scale and screenRotation
            float[] clickAzEl = mGridSatView.convertClickXYToAzEl();
            float clickTolerance = 1.5f * GridSatView.ICON_SIZE / mGridSatView.getPixelPerDegree();
            //clicking is shakier in liveMode
            if (mGridSatView.isLiveMode()) {
                clickTolerance *= 2;
            }
            // find what satellites are at this click location, and either present popup, or satellite dialog
            handleSatelliteCanvasClick(clickAzEl[0], clickAzEl[1], clickTolerance);
        }
    }

    /**
     * Touch event was pan, calculate az, el pan distance and re-draw canvas
     *
     * @param event the data associated with this Touch Event
     */
    private void handleOngoingPanEvent(MotionEvent event) {
        // do panning if not LiveMode, so set panX, panY and call invalidate()
        if (mGridSatView.isLiveMode()) {
            return;
        }
        // calculate delta between current coords and ACTION_DOWN coords to set GRIDSatView panAz and panEl
        mGridSatView.panAz = (mGridSatView.tempPanAz + (mGridSatView.touchDownX - event.getX()) / mGridSatView.getPixelPerDegree());
        // restrict total panning in elevation so that el > 90 or el < -10 doesn't show on screen
        float proposedTempPanEl = (event.getY() - mGridSatView.touchDownY) / mGridSatView.getPixelPerDegree();
        float newLosEl = mGridSatView.getLosElDeg() + mGridSatView.tempPanEl + proposedTempPanEl;
        float halfCanvasHDeg = mGridSatView.mCanvasHeight / (2 * mGridSatView.getPixelPerDegree());
        if (newLosEl > halfCanvasHDeg - 10 && newLosEl < MAX_ELEV - halfCanvasHDeg) {
            mGridSatView.panEl = mGridSatView.tempPanEl + proposedTempPanEl;
        }
        // in LiveMode sensor event calls invalidate; need to do it here to update canvas during pan
        mGridSatView.invalidate();
    }

    /**
     * We're finished with this pan event. Save the distance panned so the next pan event
     * starts where we finished this pan. Also limit elevation pan to -10 to +90 degrees
     *
     * @param event the pan event to handle
     */
    private void handlePanEventEnd(MotionEvent event) {
        if (mGridSatView.isLiveMode()) {
            return;
        }
        float halfCanvasHDeg = mGridSatView.mCanvasHeight / (2 * mGridSatView.getPixelPerDegree());
        mGridSatView.tempPanAz += (mGridSatView.touchDownX - event.getX()) / mGridSatView.getPixelPerDegree();
        mGridSatView.tempPanAz = (mGridSatView.tempPanAz + 360) % 360;
        float proposedTempPanEl = (event.getY() - mGridSatView.touchDownY) / mGridSatView.getPixelPerDegree();
        float newLosEl = mGridSatView.getLosElDeg() + mGridSatView.tempPanEl + proposedTempPanEl;
        if (newLosEl > halfCanvasHDeg - 10 && newLosEl < MAX_ELEV - halfCanvasHDeg) {
            mGridSatView.tempPanEl += proposedTempPanEl;
            //restrict total panning in elevation so that el > 90 doesn't show on screen
        } else if (newLosEl >= MAX_ELEV - halfCanvasHDeg) {
            mGridSatView.tempPanEl += (MAX_ELEV - mGridSatView.getLosElDeg() - mGridSatView.tempPanEl - halfCanvasHDeg);
        } else {
            mGridSatView.tempPanEl += (-mGridSatView.getLosElDeg() - mGridSatView.tempPanEl + halfCanvasHDeg - 10);
        }
    }

    /**
     * Calculate distance swiped to see if we stayed within a small distance. If so
     * it's a click, if not it's a pan
     *
     * @param x1 start horizontal location in pixels
     * @param y1 start vertical location in pixels
     * @param x2 end horizontal location in pixels
     * @param y2 end vertical location in pixels
     * @return total distance in density dependent pixels
     */
    private float distanceSwiped(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float distanceInPixels = (float) Math.sqrt(dx * dx + dy * dy);
        return pixelToDp(distanceInPixels);
    }

    /**
     * convert a distance in pixels to a distance in dp: density dependent pixels
     * to determine whether touch event is a click or a pan
     *
     * @param pixel distance the touch event moved, in pixels
     * @return equivalent touch distance in dp
     */
    private float pixelToDp(float pixel) {
        return pixel / getResources().getDisplayMetrics().density;
    }

    /**
     * Handle a pinch zoom event to change the camera zoom factor and grid/satellite view
      * @return false to say we've handled the event
     */
    @NonNull
    private ScaleGestureDetector.OnScaleGestureListener scaleGestureListener() {
        return new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

                if (mGridSatView.isLiveMode()) {
                    mGridSatView.setPreviewImageZoomScaleFactor(mGridSatView.getTempCameraZoomFactor());
                } else{
                    mGridSatView.setPausedZoomFactor(mGridSatView.getTempPausedZoomFactor());
                }
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mGridSatView.setTempCameraZoomFactor(MIN_TOTAL_ZOOM);
                mGridSatView.setTempPausedZoomFactor(MIN_TOTAL_ZOOM);
                return true;
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // if in liveMode set maximum zoom factor to be about 3.33 because azimuth mag field shakes too much
                // if camera doesn't zoom and we're in LiveMode, just return
                if (mGridSatView.isLiveMode() && mCameraPreview.zoomRatios == null) {
                    return false;
                }
                // in LiveMode
                // 0) set pausedZoomFactor = 1;
                // 1) multiply previous cameraZoomFactor by currentScaleFactor
                // 2) if newZoomFactor > 3.3 newZoomFactor = 3.3; if newZoomFactor < 1, newZoomFactor = 1
                // 3) find corresponding zoomNumber for the camera, set zoomNumber and cameraZoomFactor;
                // set camera parameters, store cameraZoomFactor in GridSatView
                // detector.getScaleFactor is a relative # from 1 - 3 for this onScale event
                double detectorScaleFactor = detector.getScaleFactor();

                if (mGridSatView.isLiveMode()) {
                    mGridSatView.setPausedZoomFactor(MIN_TOTAL_ZOOM);
                    double newZoomFactor = mGridSatView.getPreviewImageZoomScaleFactor() * detectorScaleFactor;
                    if (newZoomFactor > MAX_LIVE_ZOOM) {
                        newZoomFactor = MAX_LIVE_ZOOM;
                    }
                    if (newZoomFactor < MIN_TOTAL_ZOOM) {
                        newZoomFactor = MIN_TOTAL_ZOOM;
                    }
                    double newCameraZoomFactor = mCameraPreview.findNewZoomFactor(newZoomFactor);
                    mGridSatView.setTempCameraZoomFactor(newCameraZoomFactor);

                    // recalculate the total zoomFactor with new camera zoom factor
                    mGridSatView.setZoomScaleFactor(newCameraZoomFactor);
                    mGridSatView.setFOVScale(mCameraPreview.getFOV());
                } else {
                    //in PausedMode
                    // 1) newPausedZoomFactor = pausedZoomFactor * currentScaleFactor; newZoomFactor = newPausedZoomFactor*cameraZoomFactor
                    // 2) if newZoomFactor > 6 newZoomFactor = 6; if newZoomFactor < 1, newZoomFactor = 1
                    // 3) pausedZoomFactor = newZoomFactor / cameraZoomFactor
                    // save pausedZoomFactor in GridSatView; save zoomScaleFactor in GridSatView (= newZoomFactor)
                    double newPausedZoomFactor = mGridSatView.getPausedZoomFactor()* detectorScaleFactor;
                    if (newPausedZoomFactor * mGridSatView.getPreviewImageZoomScaleFactor() > MAX_TOTAL_ZOOM) {
                        newPausedZoomFactor = MAX_TOTAL_ZOOM / mGridSatView.getPreviewImageZoomScaleFactor();
                    }
                    // total zoom can't be less than 1
                    if (newPausedZoomFactor * mGridSatView.getPreviewImageZoomScaleFactor() < MIN_TOTAL_ZOOM) {
                        newPausedZoomFactor = MIN_TOTAL_ZOOM / mGridSatView.getPreviewImageZoomScaleFactor();
                    }
                    mGridSatView.setTempPausedZoomFactor(newPausedZoomFactor);
                    mGridSatView.setZoomScaleFactor(newPausedZoomFactor * mGridSatView.getPreviewImageZoomScaleFactor());
                    mGridSatView.setFOVScale(mCameraPreview.getFOV());
                    mGridSatView.invalidate();
                }
                return false;
            }
        };
    }

    /**
     * Called when sensor values have changed.
     * <p>See {@link SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link SensorEvent SensorEvent}.
     * <p/>
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the {@link SensorEvent SensorEvent}.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                 if (mGridSatView.isLiveMode()) {
                    lowPassFilterMag(event.values.clone());
                         // correct for declination
                     mGridSatView.setLosAzDeg(calcAz() - mLocationHelper.getMagDeclination());
                }
                break;
            case Sensor.TYPE_ACCELEROMETER:
            case Sensor.TYPE_GRAVITY:
                if (mGridSatView.isLiveMode()) {
                    lowPassFilterAccel(event.values.clone());
                    calcRotEl();
                    //only redraw screen when we get new gravity values, not both mag & gravity
                    mGridSatView.invalidate();
                }
                break;
        }//switch
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * <p/>
     * <p>See the SENSOR_STATUS_* constants in
     * {@link SensorManager SensorManager} for details.
     *
     * @param sensor the sensor whose accuracy changed
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String sensorAccuracy;
        switch (accuracy){
            case 3:
                sensorAccuracy = "SENSOR_STATUS_ACCURACY_HIGH";
                break;
            case 2:
                sensorAccuracy = "SENSOR_STATUS_ACCURACY_MED";
                break;
            case 1:
                sensorAccuracy = "SENSOR_STATUS_ACCURACY_LOW";
                break;
            default:
                sensorAccuracy = "SENSOR_STATUS_UNRELIABLE";
        }
        //TODO if magnetic field sensor accuracy is low, alert user to re-calibrate
        if (DEBUG){Log.e(this.getClass().getName(), "onAccuracyChanged()- sensor: " + sensor.getName() + " new accuracy: " + sensorAccuracy);}
    }

    private void calcRotEl() {
        // this only depends on gravity (or accelerometer) so just call it when those sensors give a new value
        float gravNorm = (float) Math.sqrt(mGravityVector[0] * mGravityVector[0]
                + mGravityVector[1] * mGravityVector[1]
                + mGravityVector[2] * mGravityVector[2]);
        float screenRotation = (float) (Math.toDegrees(Math.atan2(mGravityVector[0] / gravNorm, mGravityVector[1] / gravNorm)));
        mGridSatView.setScreenRotation(screenRotation);
        float losEl = (float) (Math.toDegrees(Math.acos(mGravityVector[2] / gravNorm) - Math.PI / 2));
        mGridSatView.setLosElDeg(losEl);
    }

    private void lowPassFilterAccel(float[] accelValues) {
        mGravityVector[0] = (float) (accelFilterAlpha * mGravityVector[0] + (1-accelFilterAlpha) * accelValues[0]);
        mGravityVector[1] = (float) (accelFilterAlpha * mGravityVector[1] + (1-accelFilterAlpha) * accelValues[1]);
        mGravityVector[2] = (float) (accelFilterAlpha * mGravityVector[2] + (1-accelFilterAlpha) * accelValues[2]);
    }

    private void lowPassFilterMag(float[] magValues) {
        final double alpha = 0.97;
        mMagFieldValues[0] = (float) (alpha * mMagFieldValues[0] + (1-alpha) * magValues[0]);
        mMagFieldValues[1] = (float) (alpha * mMagFieldValues[1] + (1-alpha) * magValues[1]);
        mMagFieldValues[2] = (float) (alpha * mMagFieldValues[2] + (1-alpha) * magValues[2]);
    }

    private float calcAz() {
        //this only depends on magnetic field value (and screen rotation & losEl) but only call this when we get a new mag field value
        // since the losAz shouldn't change when we change the elevation or screen rotation
        // use for losEl between +/- 70 degrees
        double cos = Math.cos(Math.toRadians(mGridSatView.getScreenRotation()));
        double sin = Math.sin(Math.toRadians(mGridSatView.getScreenRotation()));
        // correct screen rotation by rotating x-y axes around z;
        // this puts x in plane containing magnetic field
        double x = mMagFieldValues[0]*cos - mMagFieldValues[1]*sin;
        double y = mMagFieldValues[0]*sin + mMagFieldValues[1]*cos;
        // now rotate z-azis around x to correct for los elevation
        // this put z in plane containing magnetic field
        // x mag field doesn't change
        cos = Math.cos(Math.toRadians(mGridSatView.getLosElDeg()));
        sin = Math.sin(Math.toRadians(mGridSatView.getLosElDeg()));
        double z = y*sin + mMagFieldValues[2]*cos;
        double norm = Math.sqrt(x*x + z*z);
        return (float) Math.toDegrees(Math.atan2(x / norm, z / norm) + Math.PI);
    }
    private void noCompassDialog(String dialog_title, String dialog_message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(dialog_message)
                .setTitle(dialog_title)
                .setIcon(R.drawable.ic_compass_icon)
                // Add the buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button, just exit dialog
                    }
                }).show();
    }
    private void initializeSnackBars() {
        mMobileDataPermissionSnackbar = Snackbar.make(
                mGridSatView,
                getString(R.string.ask_mobile_internet_permission),
                Snackbar.LENGTH_INDEFINITE);
        mCameraPermissionSnackBar = Snackbar.make(
                mGridSatView,
                getString(R.string.allow_camera),
                Snackbar.LENGTH_INDEFINITE);
        mLocationSettingsSnackBar = Snackbar.make(
                mGridSatView,
                getString(R.string.ask_location_permission),
                Snackbar.LENGTH_INDEFINITE);
        mWirelessSettingsSnackbar = Snackbar.make(
                mGridSatView,
                getString(R.string.open_wifi_settings),
                Snackbar.LENGTH_INDEFINITE);
        mHintSnackBar = Snackbar.make(
                mGridSatView, getString(R.string.livemode_hint),
                Snackbar.LENGTH_INDEFINITE);
        snackBarList.clear();
        snackBarList.add(mMobileDataPermissionSnackbar);
        snackBarList.add(mWirelessSettingsSnackbar);
        snackBarList.add(mHintSnackBar);
        snackBarList.add(mLocationSettingsSnackBar);
        snackBarList.add(mCameraPermissionSnackBar);
    }

}
