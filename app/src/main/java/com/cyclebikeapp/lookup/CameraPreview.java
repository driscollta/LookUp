package com.cyclebikeapp.lookup;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by TommyD on 2/18/2016.
 *
 */
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final int DEF_FOV = 60;
    private static final int JPEG_QUALITY = 60;
    private static float desiredExposureCompensationValue = -1.5f;
    private SurfaceHolder mHolder;
    Camera mCamera;
    // a list of available zoom ratios for this camera
    public List<Integer> zoomRatios;
    private boolean hasCamera;
    private YuvImage previewImage;

    CameraPreview(Context context) {
        super(context);
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        hasCamera = checkCameraHardware(context);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where to draw.
        connectCamera(holder);
    }

    public void connectCamera(SurfaceHolder holder) {
        if (hasCamera) {
            mCamera = getCameraInstance();
            configureCamera();
            try {
                if (mCamera != null) {
                    mCamera.setPreviewDisplay(holder);
                }
            } catch (IOException e) {
                if (MainActivity.DEBUG) { e.printStackTrace(); }
            }
        }
    }

    /**
     * set some preliminary camera parameters like focus (infinity) and exposure compensation(a bit dark)
     */
    public void configureCamera() {
        if (mCamera == null) {
            return;
        }
        //TODO set JPEG EXIF location
        Camera.Parameters p = mCamera.getParameters();
        float exposureCompStepValue = p.getExposureCompensationStep();
        int compensationIndex = (int) (desiredExposureCompensationValue / exposureCompStepValue);
        if (p.getMinExposureCompensation() > compensationIndex) {
            compensationIndex = p.getMinExposureCompensation();
        }
        p.setExposureCompensation(compensationIndex);
        List<String> focusModes = p.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
            p.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        }
        if (p.isZoomSupported()){
            zoomRatios = p.getZoomRatios();
        }
        mCamera.setParameters(p);
        logCameraParameters();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Because the CameraDevice object is not a shared resource, it's very
        // important to release it when the activity is paused.
        // release the camera here so menu activity can capture sharing image from camera
        stopPreview();
        releaseCamera();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    // let MainActivity handle other surfaceChanged tasks
        if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "camera surfaceChanged() - width: " + w + "height: " + h);}
    }

    /**
     * Called on a preview frame to retain the image for sharing. Called when Menu button clicked
     * or in LiveMode when Play/Pause clicked
     */
    public void startPreviewCallback(final GridSatView mGridSatView) {
        if (mCamera == null){return;}
        if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "startPreviewCallback()");}
            mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "setOneShotPreviewCallback()");}
                    Camera.Parameters parameters = camera.getParameters();
                    Camera.Size size = parameters.getPreviewSize();
                    previewImage = new YuvImage(data, parameters.getPreviewFormat(),
                            size.width, size.height, null);
                    // save the zoom scale factor at which this image was taken
                    double cameraZoomFactor = 1.;
                    if (parameters.isZoomSupported()) {
                        int cameraZoomNumber = parameters.getZoom();
                        cameraZoomFactor = parameters.getZoomRatios().get(cameraZoomNumber) / 100.;
                    }
                    mGridSatView.setPreviewImageZoomScaleFactor(cameraZoomFactor);
                    saveSharingImage(mGridSatView, previewImage);
                    stopPreview();
                }
            });
    }

    public void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance(){
        Camera c = null;
        int cameraId = findBackFacingCamera();
        int cameraPermissionCheck = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.CAMERA);
        boolean hasCameraPermission = cameraPermissionCheck == PackageManager.PERMISSION_GRANTED;
        if (hasCameraPermission) {
            try {
                c = Camera.open(cameraId); // attempt to get a Camera instance
            } catch (Exception e) {
                // Camera is not available (in use or does not exist)
                if (MainActivity.DEBUG) { e.printStackTrace(); }
            }
        }
        return c; // returns null if camera is unavailable
    }
    private int findBackFacingCamera() {
        int cameraId = 0;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }
    public float[] getFOV(){
        float[] fieldOfView = new float[2];
        fieldOfView[0] = DEF_FOV;
        fieldOfView[1] = DEF_FOV;
        if(mCamera == null)return fieldOfView;
        Camera.Parameters p = mCamera.getParameters();
        fieldOfView[0] = (p.getVerticalViewAngle());
        fieldOfView[1] = (p.getHorizontalViewAngle());
        return fieldOfView;
    }

    public int getCameraRotationOffset(){
        int returnCamInfoOrientation = 0;
        try {
            Camera.CameraInfo camInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(findBackFacingCamera(), camInfo);
            if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "getCameraRotationOffset()" + camInfo.orientation);}
            returnCamInfoOrientation =  camInfo.orientation;
        }catch (Exception e){
            e.printStackTrace();
        }
        return returnCamInfoOrientation;
    }
    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
    public void setDisplayOrientation(int displayOrientation) {
        if (mCamera != null) {
            mCamera.setDisplayOrientation(displayOrientation);
        }
    }
    private void logCameraParameters(){
        Camera.Parameters p = mCamera.getParameters();
//        Log.i(this.getClass().getName(), "ExposureCompensation: " + p.getExposureCompensation());
//        Log.i(this.getClass().getName(), "ExposureCompensationStep: " + p.getExposureCompensationStep());
//        Log.i(this.getClass().getName(), "MaxExposureCompensation: " + p.getMaxExposureCompensation());
//        Log.i(this.getClass().getName(), "MinExposureCompensation: " + p.getMinExposureCompensation());
//        Log.i(this.getClass().getName(), "PreviewSize: " + "height - " +  p.getPreviewSize().height +" width - " + p.getPreviewSize().width);
        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "Zoom: " + p.getZoom());}
        String mZRatioStr = "";
        if (zoomRatios != null) {
            for (Integer zr : zoomRatios) {
                mZRatioStr = mZRatioStr + ", " + zr.toString();
            }
        }
        if (MainActivity.DEBUG) {
            Log.i(this.getClass().getName(), "SupportedZoomRatios: " + mZRatioStr);
            //        Log.i(this.getClass().getName(), "PreviewFormat: " + p.getPreviewFormat());
            Log.i(this.getClass().getName(), "Zoom supported? " + (p.isZoomSupported() ? "yes" : "no"));
            Log.i(this.getClass().getName(), "max Zoom " + p.getMaxZoom());
            Log.i(this.getClass().getName(), "ViewAngle (v): " + p.getVerticalViewAngle());
            Log.i(this.getClass().getName(), "ViewAngle (h): " + p.getHorizontalViewAngle());
            Log.i(this.getClass().getName(), "focal length (mm): " + p.getFocalLength());
        }
        List<Camera.Size> mSize = p.getSupportedPreviewSizes();
        String mCSizeStr = "";
        for(Camera.Size cs:mSize){
            String aCS = " ["+cs.height + "," + cs.width+"] ";
            mCSizeStr = mCSizeStr + aCS;
        }
        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "SupportedPreviewSizes: " + mCSizeStr);}
    }

    /**
     * match camera preview size to holder dimensions using available preview dimensions.
     * Calculate holder aspect ratio and pick best matching camera preview size.
     */
    public void setPreviewSize() {
        if (mCamera == null) {return;}
        Camera.Parameters p = mCamera.getParameters();
        List<Camera.Size> mSize = p.getSupportedPreviewSizes();
        double holderAspect = mHolder.getSurfaceFrame().width() / (double) mHolder.getSurfaceFrame().height();
        if (holderAspect > 1) {
            holderAspect = 1 / holderAspect;
        }
        double bestAspect = 99.;
        Camera.Size bestSize = p.getPreviewSize();
        for (Camera.Size s : mSize) {
            double testAspect = s.width / (double) s.height;
            if (testAspect > 1) {
                testAspect = 1 / testAspect;
            }
            if (Math.abs((testAspect - holderAspect) / holderAspect) < Math.abs((bestAspect - holderAspect) / holderAspect)) {
                bestAspect = testAspect;
                bestSize = s;
            }
        }
        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "holder aspect: " + holderAspect + " bestAspect: " + bestAspect);}
        p.setPreviewSize(bestSize.width, bestSize.height);

        mCamera.setParameters(p);
        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "PreviewSize: " + "height - " + p.getPreviewSize().height + " width - " + p.getPreviewSize().width);}
    }

    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private void saveSharingImage(GridSatView mGridSatView, YuvImage previewImage) {
        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "saveSharingImage() ");}
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        previewImage.compressToJpeg(new Rect(0, 0, previewImage.getWidth(), previewImage.getHeight()), JPEG_QUALITY, out);
        byte[] imageBytes = out.toByteArray();
        try {
            out.close();
        } catch (IOException e) {
            if (MainActivity.DEBUG) { e.printStackTrace(); }
        }
        Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        mGridSatView.previewImage = image.copy(Bitmap.Config.RGB_565, true);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        mGridSatView.rotatedPreviewImage = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        mGridSatView.saveSharingImage = true;
        mGridSatView.invalidate();
        Bitmap mutableImage = image.copy(Bitmap.Config.RGB_565, true);
        if (Util.isExternalStorageWritable(mGridSatView.getContext())) {
            File mySharingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sharingImageZoomedPreviewImage.jpg");
            try {
                FileOutputStream mFileOutStream = new FileOutputStream(mySharingFile);
                mutableImage.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, mFileOutStream);
                mFileOutStream.flush();
                mFileOutStream.close();
            } catch (Exception e) {
                if (MainActivity.DEBUG) { e.printStackTrace(); }
            }
        }
    }

    public double findNewZoomFactor(double desiredZoomFactor) {
        // get Camera parameters
        // if zoom not supported, return 1.0
        // find index to zoom ratios for zoomRatio closest, but less than desiredZoomFactor
        // set camera zoom parameter to that index
        // return zoomRatio at that index
        if (mCamera == null){return 0;}
        Camera.Parameters parameters = mCamera.getParameters();
        double cameraZoomFactor = 1.;
        if (parameters.isZoomSupported()) {
            for (int cameraZoomNumber = parameters.getMaxZoom(); cameraZoomNumber >= 0; cameraZoomNumber--){
                cameraZoomFactor = parameters.getZoomRatios().get(cameraZoomNumber) / 100.;
                if (cameraZoomFactor < desiredZoomFactor){
                    parameters.setZoom(cameraZoomNumber);
                    mCamera.setParameters(parameters);
                    return cameraZoomFactor;
                }
            }
        }
        return cameraZoomFactor;
    }
}
