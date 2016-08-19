package com.cyclebikeapp.upinthesky;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.Date;

import static com.cyclebikeapp.upinthesky.Constants.ONE_MINUTE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_ALTITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_LATITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_LONGITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_MAG_DECLINATION;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_DEFAULT_TIME;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_ALTITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_LATITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_LONGITUDE;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_MAG_DECLINATION;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_KEY_TIME;
import static com.cyclebikeapp.upinthesky.Constants.PREFS_NAME;

/**
 * Created by TommyD on 3/4/2016.
 * Use Google Play Service FusedLocationApi to get coarse user Location for satellite look angle calculations
 * Only need one coarse Location; altitude doesn't matter. First get the Last Known Location and save it in my Prefs.
 * If we don't have data connection, at least we'll have a location.
 * If user has Location Services set to allow fine Location using GPS, we'll try to use that. Otherwise try to get Location
 * over WiFi or Mobile. Once we have a new Location, save it in my Prefs and call stopLocationUpdates.
 */
public class LocationHelper implements GoogleApiClient.OnConnectionFailedListener,
                                       GoogleApiClient.ConnectionCallbacks,
                                       com.google.android.gms.location.LocationListener {


    // we only build GEO satellites when loading a new data file, updating TLEs or receiveing a new Location
    private boolean rebuildGEOSatellites;
    public boolean mResolvingError;
    public ConnectionResult connectionFailureResult;
    public Status locationSettingsResultStatus;
    // difference between Location time and System time - use to correct time when calculating satellite position
    public long systemCurrentTimeOffset;
    int priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
    private Location myLocation;
    final GoogleApiClient mGoogleApiClient;
    public float magDeclination;
    public LocationHelper(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        rebuildGEOSatellites = false;
        mResolvingError = false;
        magDeclination = getDeclinationFromSharedPrefs();
    }

    /**
     * Use Google Location API to get a user location
     *
     * @param mLocationRequest specifies update interval and accuracy
     */
    protected void startLocationUpdates(LocationRequest mLocationRequest) {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException ignore) {
        }
    }

    /**
     * Once we have received a coarse location disable locationListener
     */
    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(ONE_MINUTE);
        mLocationRequest.setFastestInterval(10000);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setPriority(priority);
        return mLocationRequest;
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            myLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } catch (SecurityException ignore) {

        }
        if (myLocation != null) {
           saveLocSharedPrefs(myLocation);
            magDeclination = getDeclinationFromSharedPrefs();
        } else {
            if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "last known Location: null");}
            setMyLocation(getLocFromSharedPrefs());
        }

        final LocationRequest mLocationRequest = createLocationRequest();
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        startLocationUpdates(mLocationRequest);
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location requests here.
                        try {
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                                    mLocationRequest, LocationHelper.this);
                        } catch (SecurityException ignore) {
                        }
                        connectionFailureResult = null;
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "location settings result callback() - RESOLUTION_REQUIRED");}
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        locationSettingsResultStatus = status;
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        locationSettingsResultStatus = status;
                        if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "SETTINGS_CHANGE_UNAVAILABLE");}
                        break;
                }
            }
        });
    }

    private Location getLocFromSharedPrefs() {
        Location aLoc = new Location(LocationManager.NETWORK_PROVIDER);
        Context mContext = mGoogleApiClient.getContext();
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        aLoc.setLongitude(Double.parseDouble(settings.getString(PREFS_KEY_LONGITUDE, PREFS_DEFAULT_LONGITUDE)));
        aLoc.setLatitude(Double.parseDouble(settings.getString(PREFS_KEY_LATITUDE, PREFS_DEFAULT_LATITUDE)));
        aLoc.setAltitude(Double.parseDouble(settings.getString(PREFS_KEY_ALTITUDE, PREFS_DEFAULT_ALTITUDE)));
        // this is just temporary until we get a location from LocationHelper
        aLoc.setTime(settings.getLong(PREFS_KEY_TIME, PREFS_DEFAULT_TIME));
        return aLoc;
    }
    public float getDeclinationFromSharedPrefs() {
        Context mContext = mGoogleApiClient.getContext();
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        return settings.getFloat(PREFS_KEY_MAG_DECLINATION, PREFS_DEFAULT_MAG_DECLINATION);
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "ConnectionSuspended");}
    }

    @Override
    public void onLocationChanged(Location location) {
        // received new Location, rebuild GEO satellites
        rebuildGEOSatellites = true;
        myLocation = location;
        systemCurrentTimeOffset = System.currentTimeMillis() - location.getTime();
        saveLocSharedPrefs(location);
        magDeclination = getDeclinationFromSharedPrefs();
        if (MainActivity.DEBUG) {
            Log.w(this.getClass().getName(), "onLocationChanged(): long - " +
                    String.format("%7.4f", myLocation.getLongitude())
                    + " lat - " + String.format("%7.4f", myLocation.getLatitude())
                    + " alt - " + String.format("%7.1f", myLocation.getAltitude())
                    + " time - " + new Date(myLocation.getTime())
                    + " current time offset " + String.format("%4.2f", systemCurrentTimeOffset / 1000.) + " sec");
        }
        // only need one Location update, then stop service
        stopLocationUpdates();
    }

    public void saveLocSharedPrefs(Location location) {
        Context mContext = mGoogleApiClient.getContext();
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(PREFS_KEY_TIME, location.getTime());
        editor.putString(PREFS_KEY_ALTITUDE, String.valueOf(location.getAltitude()));
        editor.putString(PREFS_KEY_LATITUDE, String.valueOf(location.getLatitude()));
        editor.putString(PREFS_KEY_LONGITUDE, String.valueOf(location.getLongitude())).apply();
        // save magnetic declination to sharedPrefs for this Location
        saveMagDeclinationSharedPrefs(location, editor);
        // only call saveLocSharedPrefs when new Location received, so we should indicate rebuildGEOSatellites with new Location
    }

    private void saveMagDeclinationSharedPrefs(Location location, SharedPreferences.Editor editor) {
        GeomagneticField gmf = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(),
                location.getTime());
        editor.putFloat(PREFS_KEY_MAG_DECLINATION, gmf.getDeclination()).apply();
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), connectionResult.getErrorMessage());}
        // we'll handle this in TimerTask LookAfterStuff
        connectionFailureResult = connectionResult;
    }

    public Location getMyLocation() {
        return myLocation;
    }
    public void setMyLocation(Location loc) {
        myLocation = loc;
    }

    public boolean shouldRebuildGEOSatellites() {
        return rebuildGEOSatellites;
    }

    public void setShouldRebuildGEOSatellites(boolean rebuildSatellites) {
        this.rebuildGEOSatellites = rebuildSatellites;
    }
}
