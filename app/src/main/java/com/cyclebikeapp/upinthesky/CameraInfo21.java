package com.cyclebikeapp.upinthesky;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.util.Log;

/**
 * Created by TommyD on 2/18/2016.
 * Some Android cameras have the camera mounted upside-down. Have to read the camera orientation,
 *  but this Class only works for Android version > Lollipop
 */
public class CameraInfo21 {

    private static final int NORMAL_CAMERA_ORIENTATION = 0;
    CameraManager mCameraManager;
    private boolean hasCamera;
    boolean hasCameraPermission;
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    CameraInfo21(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        hasCameraPermission = Util.hasCameraPermission(context);
        hasCamera = checkCameraHardware(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public int findBackFacingCameraOrientation() {
        String[] cameraList;

        if (!hasCamera){
            return NORMAL_CAMERA_ORIENTATION;
        }
        try {
            cameraList = mCameraManager.getCameraIdList();
            for (String cameraID : cameraList) {
                CameraCharacteristics aCharacteristic = mCameraManager.getCameraCharacteristics(cameraID);
                int lensFacing = aCharacteristic.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    if (MainActivity.DEBUG){
                        Log.e(this.getClass().getName(), "Success: found sensor orientation for back-facing camera");
                    }
                    return aCharacteristic.get(CameraCharacteristics.SENSOR_ORIENTATION);
                }
            }
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
        return NORMAL_CAMERA_ORIENTATION;
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

}
