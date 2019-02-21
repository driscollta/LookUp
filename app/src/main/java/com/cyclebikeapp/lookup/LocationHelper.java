package com.cyclebikeapp.lookup;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import static com.cyclebikeapp.lookup.Constants.LOCATION_INTERVAL;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_TIME;


/**
 * Created by TommyD on 3/4/2016.
 * Use Google Play Service FusedLocationApi to get coarse user Location for satellite look angle calculations
 * Only need one coarse Location; altitude doesn't matter. First get the Last Known Location and save it in my Prefs.
 * If we don't have data connection, at least we'll have a location.
 * If user has Location Services set to allow fine Location using GPS, we'll try to use that. Otherwise try to get Location
 * over WiFi or Mobile. Once we have a new Location, save it in my Prefs and call stopLocationUpdates.
 */
class LocationHelper {
    private final Context mContext;
    private Location myLocation;
    private float magDeclination;
    private final LocationCallback mLocationCallback;
    private final FusedLocationProviderClient mFusedLocationClient;

    LocationHelper(Context context) {
        mContext = context;
        magDeclination = Util.getDeclinationFromSharedPrefs(context);
        setMyLocation(Util.getLocFromSharedPrefs(context));
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                long thisLocationTime = PREFS_DEFAULT_TIME;
                for (Location location : locationResult.getLocations()) {
                    // if more than one location returned only use latest one
                    if (location.getTime() > thisLocationTime) {
                        setMagDeclination(location);
                        thisLocationTime = location.getTime();
                        setMyLocation(location);
                    }
                }
            }
        };
    }

    /**
     * Use Google Location API to get a user location
     */
    void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(createLocationRequest(),
                mLocationCallback,
                null);
    }

    /**
     * stop location updates in onStop() and before starting location updates
     */
    void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(LOCATION_INTERVAL / 2);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    Location getMyLocation() {
        return myLocation;
    }

    private void setMyLocation(Location loc) {
        myLocation = loc;
        Util.saveLocSharedPrefs(loc, mContext);
    }

    float getMagDeclination() {
        return magDeclination;
    }

    private void setMagDeclination(Location location) {
        GeomagneticField gmf = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(),
                location.getTime());
        magDeclination = gmf.getDeclination();
    }

}
