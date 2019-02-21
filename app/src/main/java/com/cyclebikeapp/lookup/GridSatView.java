/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyclebikeapp.lookup;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import satellite.Satellite;

import static com.cyclebikeapp.lookup.Constants.PAID_VERSION;
import static com.cyclebikeapp.lookup.Constants.SHARING_IMAGE_NAME;
import static com.cyclebikeapp.lookup.Util.getAlbumStorageDir;


/**
 * View that draws the camera preview overlay, keeps track of touch parameters, etc.
 *
 */
@SuppressWarnings("ConstantConditions")
public class GridSatView extends View {

    public static final int LOCATION_STATUS_UNKNOWN = 0;
    public static final int LOCATION_STATUS_NONE = 1;
    public static final int LOCATION_STATUS_OLD = 2;
    public static final int LOCATION_STATUS_OKAY = 3;
    private static final String DEG_SYMBOL = "\u00B0";
    public static final int ICON_SIZE = 48;
    private static final int SATELLITE_TEXT_FONT_SIZE = 24;
    private static final int DRAW_DATA_TEXT_SIZE = 24;
    private static final int DRAW_LOCATION_STATUS_TEXT_SIZE = 24;
    private static final int DRAW_SATELLITE_WARNING_TEXT_SIZE = 24;
    private static final int FAB_MARGIN = 16;
    private static final int FAB_DIAMETER = 56;
    private static final String LOOK_UP = "LookUp";
    private static final int _360 = 360;
    private static final String TRUE_NORTH = "True North";
    private static final String FORMAT2_1 = "%2.1f";
    private static final String FORMAT3_1 = "%3.1f";
    private static final String X_FOV = "x";
    private static final String ZOOM = "Zoom: ";
    private final int orangeColor;
    private int statusBarHeight = 50;
    private float losElDeg;
    private float losAzDeg;
    private float pixPerDeg;
    private float screenRotation;

    public float touchDownX = 0;
    public float touchDownY = 0;
    public boolean isZooming = false;
    public long pressStartTime;
    public boolean stayedWithinClickDistance;
    private final Drawable playButtonDrawable;
    private final Drawable pauseButtonDrawable;
    private final Drawable menuButtonDrawable;
    private final Drawable lookTitleKomicaWideY;
    private final Drawable grayCameraPreviewBackground;
    public Bitmap previewImage;
    public Bitmap rotatedPreviewImage;
    private final Drawable geoSatelliteDrawable;
    private final Drawable leoSatelliteDrawable;
    private final Drawable deepSatelliteDrawable;
    private final Drawable cDeepSatelliteDrawable;
    private final Drawable cGeoSatelliteDrawable;
    ArrayList<Satellite> mGEOSatellites;
    ArrayList<Satellite> mLEOSatellites;
    ArrayList<Satellite> mDeepSatellites;
    ArrayList<Satellite> mCGEOSatellites;
    ArrayList<Satellite> mCDeepSatellites;
    /**
     * Current height of the surface/canvas.
     *
     * @see #setSurfaceSize
     */
    public int mCanvasHeight = 1;
    /**
     * Current width of the surface/canvas.
     */
    private int mCanvasWidth = 1;
    private final int fabMarginPixel;
    private final int fabDiameterPixel;
    public int locationStatus;
    private int azIncrement;
    private int elIncrement;
    // when config changes to portrait or landscape the definition of x,y changes
    private float azelConfigCorrection = 0;
    public float panAz = 0;
    public float panEl = 0;
    float tempPanEl = 0;
    float tempPanAz = 0;
    private boolean liveMode = true;
    private double tempCameraZoomFactor;
    private double tempPausedZoomFactor;
    // the zoomFactor user sets via ScaleDetector
    private double pausedZoomFactor;
    // the total zoomFactor, product of pausedZoomFactor and previewImageZoomScaleFactor
    // this is a number from 1.0 to 10.0
    private double zoomScaleFactor;
    // the scale factor at which the previewImage was captured
    // a number from 1.0 to 3.3
    private double previewImageZoomScaleFactor;
    private boolean rotatePreviewImage = false;
    public boolean saveSharingImage;
    private File mySharingFile;
    public float magDeclination;
    private Snackbar mWriteSettingsSnackbar;
    public String debugText;
    public boolean loadingSatellites;
    public boolean updatingTLEs;

    private static Bitmap ScaleBitmap(Bitmap source, float scale) {
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale, source.getWidth() / 2, source.getHeight() / 2);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    /**
     * method called when we invoke invalidate() on the canvas
      * @param canvas the drawing surface passed to us
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // draw the content like grid lines and satellites
        long startTime = System.nanoTime();
        doDrawGrid(canvas);
        drawLookTitle(canvas);
        if (saveSharingImage) {
            // have to disable saving sharingImage right away because onDraw gets called again before File has finished writing
            saveSharingImage = false;
            this.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(this.getDrawingCache());
            this.setDrawingCacheEnabled(false);
            if (MainActivity.DEBUG) Log.i(this.getClass().getName(), "mySharingFile location:" + mySharingFile.toString());
            if (Util.isExternalStorageWritable(this.getContext())) {
                try {
                    FileOutputStream mFileOutStream = new FileOutputStream(mySharingFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, mFileOutStream);
                    mFileOutStream.flush();
                    mFileOutStream.close();
                } catch (Exception e) {
                    if (MainActivity.DEBUG) { e.printStackTrace(); }
                }
            } else{
                // show snackBar complaining about write permission
                mWriteSettingsSnackbar = Snackbar.make(
                        this,
                        getResources().getString(R.string.complain_write_permission),
                        Snackbar.LENGTH_LONG);
                mWriteSettingsSnackbar.show();
            }
        }
        //draw the simulated floating action buttons
        drawMenuButton(canvas);
        drawPlayPauseButton(canvas);
        drawDataLeft(canvas);
        drawDataRight(canvas);
        //drawDebugText(canvas);
        drawNoSatelliteWarning(canvas);
        drawLocationStatus(canvas);
        super.onDraw(canvas);
       //Log.d(this.getClass().getName(), "onDraw takes " + String.format("%4.2f",(System.nanoTime() - startTime) / 1000000.) + " msec");
    }

    private void drawNoSatelliteWarning(Canvas canvas) {
        eraseBuildingSatMessage(canvas);
        if ((mGEOSatellites == null) || (mLEOSatellites == null) || (mDeepSatellites == null)) {return;}
        if (loadingSatellites || updatingTLEs){
            //instead show loading satellites message
            drawSatelliteStatusMessage(canvas);
            return;
        }
        int numSatellites = mGEOSatellites.size() + mLEOSatellites.size() + mDeepSatellites.size();
        if (numSatellites > 0){
            // if we have satellites, just return
            return;
        }
        String satWarningText = getResources().getString(R.string.no_satellites_visible);
        Paint textPaint = new Paint();
        textPaint.setColor(orangeColor);
        textPaint.setTextSize(DRAW_SATELLITE_WARNING_TEXT_SIZE);
        int top = fabMarginPixel;
        float topOffset = top + lookTitleKomicaWideY.getMinimumHeight() + 20;
        if (locationStatus != LOCATION_STATUS_OKAY){
            topOffset += textPaint.getFontMetrics().bottom - textPaint.getFontMetrics().top;
        }
        float satWarningTextOffset = canvas.getWidth() / 2 - textPaint.measureText(satWarningText)/ 2;
        canvas.drawText(satWarningText, satWarningTextOffset, topOffset, textPaint);
    }

    private void eraseBuildingSatMessage(Canvas canvas) {
        String loadingSatText = "";
        Paint textPaint = new Paint();
        textPaint.setColor(orangeColor);
        textPaint.setTextSize(DRAW_SATELLITE_WARNING_TEXT_SIZE);
        int top = fabMarginPixel;
        float topOffset = top + lookTitleKomicaWideY.getMinimumHeight() + 20;
        if (locationStatus != LOCATION_STATUS_OKAY){
            topOffset += textPaint.getFontMetrics().bottom - textPaint.getFontMetrics().top;
        }
        float loadingSatTextOffset = canvas.getWidth() / 2 - textPaint.measureText(loadingSatText)/ 2;
        canvas.drawText(loadingSatText, loadingSatTextOffset, topOffset, textPaint);
    }

    private void drawSatelliteStatusMessage(Canvas canvas) {
        String satStatusText = getResources().getString(R.string.loading_satellites);
        if (updatingTLEs){
            satStatusText = getResources().getString(R.string.updating_tles);
        }
        Paint textPaint = new Paint();
        textPaint.setColor(orangeColor);
        textPaint.setTextSize(DRAW_SATELLITE_WARNING_TEXT_SIZE);
        int top = fabMarginPixel;
        float topOffset = top + lookTitleKomicaWideY.getMinimumHeight() + 20;
        if (locationStatus != LOCATION_STATUS_OKAY){
            topOffset += textPaint.getFontMetrics().bottom - textPaint.getFontMetrics().top;
        }
        float loadingSatTextOffset = canvas.getWidth() / 2 - textPaint.measureText(satStatusText)/ 2;
        canvas.drawText(satStatusText, loadingSatTextOffset, topOffset, textPaint);
    }

    private void drawLocationStatus(Canvas canvas) {
        String locationStatusText;
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        switch(locationStatus){
            case LOCATION_STATUS_NONE:
            case GridSatView.LOCATION_STATUS_UNKNOWN:
                locationStatusText = getResources().getString(R.string.location_unknown);
                textPaint.setColor(Color.RED);
                break;
            case LOCATION_STATUS_OLD:
                locationStatusText = getResources().getString(R.string.location_not_current);
                textPaint.setColor(orangeColor);
            break;
            // don't draw anything if locationstatus is okay
            default:
                return;
        }
        textPaint.setTextSize(DRAW_LOCATION_STATUS_TEXT_SIZE);
        int top = fabMarginPixel;
        float topOffset = top + lookTitleKomicaWideY.getMinimumHeight() + 20;
        float locationStatusTextOffset = canvas.getWidth() / 2 - textPaint.measureText(locationStatusText)/ 2;
        canvas.drawText(locationStatusText, locationStatusTextOffset, topOffset, textPaint);
    }

    private void drawDataLeft(Canvas canvas) {
        String dataLeft = ZOOM + String.format(FORMAT2_1, zoomScaleFactor) + X_FOV;
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        float topOffset = 25;
        float dataLeftTextOffset = fabMarginPixel;
        canvas.drawText(dataLeft, dataLeftTextOffset, topOffset, textPaint);
    }

    private void drawDebugText(Canvas canvas) {
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        float topOffset = canvas.getHeight() - 25;
        float dataLeftTextOffset = fabMarginPixel;
        canvas.drawText(debugText, dataLeftTextOffset, topOffset, textPaint);
    }

    private void drawDataRight(Canvas canvas) {
        String dataRight = TRUE_NORTH;
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(DRAW_DATA_TEXT_SIZE);
        float topOffset = 25;
        float dataRightTextOffset;
        dataRightTextOffset = mCanvasWidth - textPaint.measureText(dataRight);
        dataRightTextOffset -= fabMarginPixel;
        canvas.drawText(dataRight, (dataRightTextOffset), topOffset, textPaint);
    }

    private void drawLookTitle(Canvas canvas) {
        int left = canvas.getWidth() / 2 - lookTitleKomicaWideY.getMinimumWidth() / 2;
        int right = left + lookTitleKomicaWideY.getMinimumWidth();
        int top = fabMarginPixel;
        int bottom = top + lookTitleKomicaWideY.getMinimumHeight();
        lookTitleKomicaWideY.setBounds(left, top, right, bottom);
        lookTitleKomicaWideY.draw(canvas);
    }

    /**
     * Draw a background, if we want to cover the camera preview, or just draw over the preview.
     * the camera preview will erase the previous drawing
     *
     * @param canvas the drawing surface
     */
    private void doDrawGrid(Canvas canvas) {
    boolean drawPreviewImage = true;
        int cw_2 = mCanvasWidth / 2;
        int ch_2 = mCanvasHeight / 2;
        if (!liveMode) {
            Bitmap drawingBitmap;
            grayCameraPreviewBackground.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            grayCameraPreviewBackground.draw(canvas);
            try {
                // when viewing previewImage in LiveMode, Camera scales the image to fit screen because previewImage may be less than screen density
                // ie we have a [720 x 480] preview image because the ratio best fits the screen aspect.
                // here we must scale the previewImage to fit the screen when pausedZoom = 1
                float scale = 1;
                float left = 0;
                float top = 0;
                double pausedScaleFactor = zoomScaleFactor / previewImageZoomScaleFactor;
                //this scales the previewImage up to screen dimension, even when zoom = 1
                double deltaAz = (panAz + _360) % _360;
                if (deltaAz > 180) {
                    deltaAz -= _360;
                } else if (deltaAz < -180) {
                    deltaAz += _360;
                }
                if (rotatePreviewImage) { // previewImage taken in portrait

                    if (inLandscapeMode(canvas)) {
                        //todo formulate left_offset and top_offset
                        drawPreviewImage = false;
                        // if previewImage taken in portrait and now we're in landscape scale = canvas.width / previewImage.width
                        // if previewImage taken in portrait and now we're in portrait scale = canvas.width / previewImage.height
                        // match center of previewImage to center of canvas
                    } else {
                        left = (float) ((1 - pausedScaleFactor) * cw_2 - deltaAz * pixPerDeg);
                        top = (float) ((1 - pausedScaleFactor) * ch_2 + panEl * pixPerDeg);
                        scale = (float) (pausedScaleFactor) * canvas.getHeight() / rotatedPreviewImage.getHeight();
                    }

                    // if image too big, scale by 1/2 and divide left, top by 15%
                    if (rotatedPreviewImage.getWidth() * scale > 2000 || rotatedPreviewImage.getHeight() * scale > 2000){
                        // crop center half of image- generalize crop factor
                        double cropFactor = Math.floor(scale* rotatedPreviewImage.getHeight()/2000) * 1.15;
                        //create bitmap based on previewImage, which has not been scaled to the canvas
                        int width = (int) (rotatedPreviewImage.getWidth() / cropFactor);
                        int height = (int) (rotatedPreviewImage.getHeight() / cropFactor);
                        int cropLeft = 0;
                        if (left < 0){
                            cropLeft = (int) ((int) -left / scale);
                            left = 0;
                        }
                        int cropTop = 0;
                        if (top < 0){
                            cropTop = (int) ((int) -top / scale);
                            top = 0;
                        }
                        if ((width + cropLeft) > rotatedPreviewImage.getWidth()){
                            width = rotatedPreviewImage.getWidth() - cropLeft;
                        }
                        if ((height + cropTop) > rotatedPreviewImage.getHeight()){
                            height = rotatedPreviewImage.getHeight() - cropTop;
                        }

//                        if (MainActivity.DEBUG) {
//                            Log.i(this.getClass().getName(), "cropFactor: " + String.format(FORMAT3_1,cropFactor)
//                                    + " scale: " + String.format(FORMAT3_1,scale)
//                                    + " [cropLeft, cropTop] " + "["+cropLeft + ", "+cropTop + "]"
//                                    + " [width, height] " + "["+width + ", "+height + "]");
//                        }

                        drawingBitmap = Bitmap.createBitmap(rotatedPreviewImage,cropLeft,cropTop,width,height);
//                        if (MainActivity.DEBUG) {
//                            Log.i(this.getClass().getName(), "drawingBitmap: "
//                                    + " [width, height] " + "["+drawingBitmap.getWidth() + ", "+drawingBitmap.getHeight() + "]");
//                        }
                        if (inLandscapeMode(canvas)) {
                            drawPreviewImage = false;
                        } else {
                            //left = (float) ((1 - pausedScaleFactor) * cw_2 - deltaAz * pixPerDeg);
                           // top = (float) ((1 - pausedScaleFactor) * ch_2 + panEl * pixPerDeg);
                            scale = (float) (pausedScaleFactor) * canvas.getHeight() / rotatedPreviewImage.getHeight();
                        }
//                        if (MainActivity.DEBUG) {
//                            Log.i(this.getClass().getName(), "rotatedPreviewImage tooBig [left, top, scale] "
//                                    + "[" + String.format(FORMAT3_1, left) + ", " + String.format(FORMAT3_1, top) + ", "
//                                    + String.format(FORMAT3_1, scale) + "]");
//                        }
                    }else {//previewImage is not too big
//                        if (MainActivity.DEBUG) {
//                            Log.i(this.getClass().getName(), "rotatedPreviewImage okay [left, top, scale] "
//                                    + "[" + String.format(FORMAT3_1,left) + ", "+ String.format(FORMAT3_1,top) + ", "
//                                    + String.format(FORMAT3_1,scale) + "]");
//                        }
                        drawingBitmap = Bitmap.createBitmap(rotatedPreviewImage);
/*                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "drawingBitmap: "
                                    + " [width, height] " + "["+drawingBitmap.getWidth() + ", "+drawingBitmap.getHeight() + "]");
                        }*/
                    }
                    if (drawPreviewImage) {
                        canvas.drawBitmap(ScaleBitmap(drawingBitmap, scale), left, top, new Paint());
                    }
                } else {// previewImage taken in landscape

                    if (inLandscapeMode(canvas)) {
                        // if previewImage taken in landscape and now we're in landscape scale = canvas.width/previewImage.width
                        // if previewImage taken in landscape and now we're in portrait scale = canvas.width / previewImage.height
                        left = (float) ((1 - pausedScaleFactor) * cw_2 - deltaAz * pixPerDeg);
                        top = (float) ((1 - pausedScaleFactor) * ch_2 + panEl * pixPerDeg);
                        scale = (float) (pausedScaleFactor) * canvas.getWidth() / previewImage.getWidth();
                    } else {
                        drawPreviewImage = false;
                        //todo formulate left_offset and top_offset
                        // previewImage is wider than canvas now
                        scale = (float) (pausedScaleFactor) * canvas.getHeight() / previewImage.getWidth();
                        left = (float) ((cw_2 - ch_2) - deltaAz * pixPerDeg);
                        top = ((ch_2 - cw_2) + panEl * pixPerDeg);
                    }

                    // if image too big, scale and divide left, top by cropFactor
                    if ( previewImage.getWidth() * scale > 2000 || previewImage.getHeight() * scale > 2000){
                        // crop center of image- generalize crop factor
                        double cropFactor = Math.floor(scale* previewImage.getWidth()/2000) * 1.15;
                        //create bitmap based on previewImage, which has not been scaled to the canvas
                        int width = (int) (previewImage.getWidth() / cropFactor);
                        int height = (int) (previewImage.getHeight() / cropFactor);
                        int cropLeft = 0;
                        if (left < 0){
                            cropLeft = (int) ((int) -left / scale);
                        left = 0;
                        }
                        int cropTop = 0;
                        if (top < 0){
                            cropTop = (int) ((int) -top / scale);
                            top = 0;
                        }
                        if ((width + cropLeft) > previewImage.getWidth()){
                            width = previewImage.getWidth() - cropLeft;
                        }
                        if ((height + cropTop) > previewImage.getHeight()){
                            height = previewImage.getHeight() - cropTop;
                        }

/*
                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "cropFactor: " + String.format(FORMAT3_1,cropFactor)
                                    + " scale: " + String.format(FORMAT3_1,scale)
                                    + " [cropLeft, cropTop] " + "["+cropLeft + ", "+cropTop + "]"
                                    + " [width, height] " + "["+width + ", "+height + "]");
                        }
*/

                        drawingBitmap = Bitmap.createBitmap(previewImage,cropLeft,cropTop,width,height);
                        // if previewImage taken in landscape and now we're in landscape scale = canvas.width/previewImage.width
                        // if previewImage taken in landscape and now we're in portrait scale = canvas.width / previewImage.height
                        if (inLandscapeMode(canvas)) {
                            scale = (float) (pausedScaleFactor) * canvas.getWidth() / previewImage.getWidth();
                        } else {
                            drawPreviewImage = false;
                            // drawingBitmap is wider than canvas now

                        }

                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "previewImage tooBig [left, top, scale] "
                                    + "[" + String.format(FORMAT3_1,left) + ", "+ String.format(FORMAT3_1,top) + ", "
                                    + String.format("%3.2f",scale) + "]");
                        }

                    }else {//previewImage is not too big
/*                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "previewImage okay [left, top, scale] "
                                    + "[" + String.format(FORMAT3_1,left) + ", "+ String.format(FORMAT3_1,top) + ", "
                                    + String.format(FORMAT3_1,scale) + "]");
                        }*/
                        drawingBitmap = Bitmap.createBitmap(previewImage);
/*                        if (MainActivity.DEBUG) {
                            Log.i(this.getClass().getName(), "drawingBitmap: "
                                    + " [width, height] " + "["+drawingBitmap.getWidth() + ", "+drawingBitmap.getHeight() + "]");
                        }*/

                    }
                    if (drawPreviewImage) {
                        canvas.drawBitmap(ScaleBitmap(drawingBitmap, scale), left, top, new Paint());
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }// only have to manipulate the previewImage when Paused
        canvas.save();
        // rotate the canvas about canvas half-width and half-height to compensate for screen rotation
        // and the configuration change from landscape to portrait
        canvas.rotate(screenRotation + azelConfigCorrection, cw_2, ch_2);
        drawGridLines_Labels(canvas, cw_2, ch_2);
        // while we're rotated, draw the satellites at their az, el locations
        drawSatellites(canvas);
        // rotate the canvas back to normal
        canvas.restore();
    }

    private void drawGridLines_Labels(Canvas canvas, int cw_2, int ch_2) {
        //set-up elevation lines and labels
        //number of el & azimuth lines we'll need - don't have to cover 360°
        int imax = 6;
        float[] elPts = new float[imax*4];
        String[] elLabel = new String[imax];
        float losElDeg = getLosElDeg() + panEl;
        //starting el label - offscreen in case screen is rotated
        float lineElDeg = (losElDeg - losElDeg % elIncrement - 2 * elIncrement);
        if (lineElDeg < -10){
            lineElDeg = -10;
        }
        int startX = -cw_2;
        int endX = 3 * cw_2;
        for (int i = 0; i < elLabel.length; i++) {
            int startY = (int) (ch_2 + (losElDeg - lineElDeg) * pixPerDeg);
            elPts[4 * i] = startX;
            elPts[4 * i + 1] = startY;
            elPts[4 * i + 2] = endX;
            elPts[4 * i + 3] = startY;
            elLabel[i] = Integer.toString((int) lineElDeg) + DEG_SYMBOL;
            lineElDeg += elIncrement;
        }
        Paint linePaint = new Paint();
        linePaint.setColor(Color.RED);
        canvas.drawLines(elPts, linePaint);
        // Calculate pixel position for the azimuth grid lines and labels
        float losAzDeg = (getLosAzDeg() + panAz + _360) % _360;
        float[] azPts = new float[imax * 4];
        String[] azLabel = new String[imax];
        //starting az label - offscreen in case screen is rotated
        double lineAzDeg = (losAzDeg - losAzDeg % azIncrement - 2 * azIncrement);
        int startY = -ch_2;
        int endY = 3 * ch_2;
        for (int i = 0; i < azLabel.length; i++) {
            startX = (int) (cw_2 - (losAzDeg - lineAzDeg) * pixPerDeg);
            azPts[4 * i] = startX;
            azPts[4 * i + 1] = startY;
            azPts[4 * i + 2] = startX;
            azPts[4 * i + 3] = endY;
            azLabel[i] = Integer.toString((int) ((lineAzDeg + _360) % _360)) + DEG_SYMBOL;
            lineAzDeg += azIncrement;
        }
        linePaint.setColor(Color.GREEN);
        canvas.drawLines(azPts, linePaint);
        labelLines(canvas, elPts, elLabel, azPts, azLabel);
    }

    private boolean inLandscapeMode(Canvas canvas) {
        return canvas.getWidth() > canvas.getHeight();
    }

    private void drawSatellites(Canvas canvas) {
        float losElDeg = getLosElDeg() + panEl;
        float losAzDeg = (getLosAzDeg() + panAz + _360) % _360;
        Paint satTextPaint = new Paint();
        satTextPaint.setColor(Color.YELLOW);
        satTextPaint.setTextAlign(Paint.Align.LEFT);
        satTextPaint.setTextSize(SATELLITE_TEXT_FONT_SIZE);
        int halfIcon = dpToPixel(ICON_SIZE / 2);
        int cw_2 = mCanvasWidth / 2;
        int ch_2 = mCanvasHeight / 2;
        RectF canvasRect = new RectF(-halfIcon, -halfIcon, mCanvasWidth + halfIcon, mCanvasHeight + halfIcon);
        if (mGEOSatellites != null) {
            for (Satellite aSat : mGEOSatellites) {
                //the aSat look angle assumes we're referencing True North
                double deltaAz = (losAzDeg - aSat.getTLE().getLookAngleAz());
                if (deltaAz > 180) {
                    deltaAz -= _360;
                } else if (deltaAz < -180) {
                    deltaAz += _360;
                }
                int startX = (int) Math.round(cw_2 - deltaAz * pixPerDeg);
                int startY = (int) Math.round(ch_2 + (losElDeg - aSat.getTLE().getLookAngleEl()) * pixPerDeg);
                // test if point is in canvas via Rect.contains(startX, startY)
                if (canvasRect.contains(startX, startY)) {
                    geoSatelliteDrawable.setBounds(startX - halfIcon, startY - halfIcon, startX + halfIcon, startY + halfIcon);
                    // convert angle to pixel left, top, right, bottom
                    geoSatelliteDrawable.draw(canvas);
                    // label with satellite name from satellite list
                    labelSatellite(aSat.getTLE().getName(), canvas, startX - halfIcon, startY + halfIcon, satTextPaint);
                }
            }
        }
        if (mLEOSatellites != null && MainActivity.version == PAID_VERSION){
            for (Satellite aSat : mLEOSatellites) {
                double deltaAz = (losAzDeg - aSat.getTLE().getLookAngleAz());
                if (deltaAz > 180) {
                    deltaAz -= _360;
                } else if (deltaAz < -180) {
                    deltaAz += _360;
                }
                int startX = (int) Math.round(cw_2 - deltaAz * pixPerDeg);
                int startY = (int) Math.round(ch_2 + (losElDeg - aSat.getTLE().getLookAngleEl()) * pixPerDeg);
                // test if point is in canvas via Rect.contains(startX, startY)
                // may want to have different Drawable for LEOs or for different satellite Types
                if (canvasRect.contains(startX, startY)) {
                    leoSatelliteDrawable.setBounds(startX - halfIcon, startY - halfIcon, startX + halfIcon, startY + halfIcon);
                    // convert angle to pixel left, top, right, bottom
                    leoSatelliteDrawable.draw(canvas);
                    // label with satellite name from satellite list
                    labelSatellite(aSat.getTLE().getName(), canvas, startX - halfIcon, startY + halfIcon, satTextPaint);
                }
            }
        }
        if (mDeepSatellites != null && MainActivity.version == PAID_VERSION) {
            for (Satellite aSat : mDeepSatellites) {
                double deltaAz = (losAzDeg - aSat.getTLE().getLookAngleAz());
                if (deltaAz > 180) {
                    deltaAz -= _360;
                } else if (deltaAz < -180) {
                    deltaAz += _360;
                }
                int startX = (int) Math.round(cw_2 - deltaAz * pixPerDeg);
                int startY = (int) Math.round(ch_2 + (losElDeg - aSat.getTLE().getLookAngleEl()) * pixPerDeg);
                // test if point is in canvas via Rect.contains(startX, startY)
                if (canvasRect.contains(startX, startY)) {
                    deepSatelliteDrawable.setBounds(startX - halfIcon, startY - halfIcon, startX + halfIcon, startY + halfIcon);
                    // convert angle to pixel left, top, right, bottom
                    deepSatelliteDrawable.draw(canvas);
                    // label with satellite name from satellite list
                    labelSatellite(aSat.getTLE().getName(), canvas, startX - halfIcon, startY + halfIcon, satTextPaint);
                }
            }
        }
        if (mCDeepSatellites != null && MainActivity.version == PAID_VERSION) {
            for (Satellite aSat : mCDeepSatellites) {
                double deltaAz = (losAzDeg - aSat.getTLE().getLookAngleAz());
                if (deltaAz > 180) {
                    deltaAz -= _360;
                } else if (deltaAz < -180) {
                    deltaAz += _360;
                }
                int startX = (int) Math.round(cw_2 - deltaAz * pixPerDeg);
                int startY = (int) Math.round(ch_2 + (losElDeg - aSat.getTLE().getLookAngleEl()) * pixPerDeg);
                // test if point is in canvas via Rect.contains(startX, startY)
                if (canvasRect.contains(startX, startY)) {
                    cDeepSatelliteDrawable.setBounds(startX - halfIcon, startY - halfIcon, startX + halfIcon, startY + halfIcon);
                    // convert angle to pixel left, top, right, bottom
                    cDeepSatelliteDrawable.draw(canvas);
                    // label with satellite name from satellite list
                    labelSatellite(aSat.getTLE().getName(), canvas, startX - halfIcon, startY + halfIcon, satTextPaint);
                }
            }
        }
        if (mCGEOSatellites != null && MainActivity.version == PAID_VERSION) {
            for (Satellite aSat : mCGEOSatellites) {
                double deltaAz = (losAzDeg - aSat.getTLE().getLookAngleAz());
                if (deltaAz > 180) {
                    deltaAz -= _360;
                } else if (deltaAz < -180) {
                    deltaAz += _360;
                }
                int startX = (int) Math.round(cw_2 - deltaAz * pixPerDeg);
                int startY = (int) Math.round(ch_2 + (losElDeg - aSat.getTLE().getLookAngleEl()) * pixPerDeg);
                // test if point is in canvas via Rect.contains(startX, startY)
                if (canvasRect.contains(startX, startY)) {
                    cGeoSatelliteDrawable.setBounds(startX - halfIcon, startY - halfIcon, startX + halfIcon, startY + halfIcon);
                    // convert angle to pixel left, top, right, bottom
                    cGeoSatelliteDrawable.draw(canvas);
                    // label with satellite name from satellite list
                    labelSatellite(aSat.getTLE().getName(), canvas, startX - halfIcon, startY + halfIcon, satTextPaint);
                }
            }
        }
    }

    private void labelSatellite(String satLabel, Canvas canvas, int startX, int startY, Paint textPaint) {
        canvas.drawText(satLabel, startX, startY, textPaint);
    }
    private void labelLines(Canvas canvas, float[] elPts, String[] elLabel, float[] azPts, String[] azLabel) {
        // calculate position for elevation labels and labels
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(32);
        float elTextOffset;
        for (int i = 0; i < elLabel.length; i++) {
            elTextOffset = textPaint.measureText(elLabel[i]);
            canvas.drawText(elLabel[i], (mCanvasWidth - elTextOffset) / 2, elPts[4 * i + 1], textPaint);
        }
        // calculate position for azimuth labels and draw labels
        textPaint.setColor(Color.GREEN);
        float azTextOffset;
        for (int i = 0; i < azLabel.length; i++) {
            azTextOffset = textPaint.measureText(azLabel[i]) / 2;
            canvas.drawText(azLabel[i], azPts[4 * i] - azTextOffset, mCanvasHeight / 2, textPaint);
        }
    }

    /* Callback invoked when the surface dimensions change. */
    public void setSurfaceSize(int width, int height) {
        mCanvasWidth = width;
        mCanvasHeight = height;
        //calculate azIncrement so that we always have two az lines on the screen
        azIncrement = Math.round(mCanvasWidth / (5 * 2 * pixPerDeg)) * 5;
        elIncrement = Math.round(mCanvasHeight / (5 * 2 * pixPerDeg)) * 5;
    }

    /**
     * scale factor to convert world angle in degrees to pixels to match camera FOV
     */
    public void setFOVScale(float[] fov) {
        float thetaHDeg = (float) (fov[1] / zoomScaleFactor);
        float thetaVDeg = (float) (fov[0] / zoomScaleFactor);
        float scaleW = (mCanvasWidth / thetaHDeg);
        float scaleH = (mCanvasHeight / thetaVDeg);
        //depending on screen rotation have to match FOV with canvas size
        if ((mCanvasHeight > mCanvasWidth) && (thetaHDeg > thetaVDeg)){
            scaleW = (mCanvasHeight / thetaHDeg);
            scaleH = (mCanvasWidth / thetaVDeg);
        }
        //Log.i(this.getClass().getName(), "[canvasW, canvasH]: [" + mCanvasWidth + ", " + mCanvasHeight + "]" + "[scaleW, scaleH]: [" + String.format("%3.1f", scaleW) + ", " + String.format("%3.1f", scaleH) + "]");
        // assume that camera image gets cropped in narrow direction, so FOV in the wide direction is preserved
        if (scaleW > scaleH) {
            pixPerDeg = scaleW;
        } else {
            pixPerDeg = scaleH;
        }
        azIncrement = Math.round(mCanvasWidth / (5 * 2 * pixPerDeg)) * 5;
        elIncrement = Math.round(mCanvasHeight / (5 * 2 * pixPerDeg)) * 5;
        // set minimum az, el increments
        if (azIncrement == 0) {
            azIncrement = 2;
        }
        if (elIncrement == 0) {
            elIncrement = 2;
        }
    }

    /**
     * Given canvas click coordinates, convert to azimuth and elevation, taking account of screen rotation
     * and all the scale factors and pan distance
     *
     * @return a float array containing the azimuth angle and elevation angle of the click
     */
    public float[] convertClickXYToAzEl() {
        // in landscape need to correct screenRotation value using azelConfigCorrection
        double cos = Math.cos(Math.toRadians(screenRotation + azelConfigCorrection));
        double sin = Math.sin(Math.toRadians(screenRotation + azelConfigCorrection));
        // correct screen rotation by rotating x-y axes around z;
        float azClick = (float) ((((touchDownX - mCanvasWidth / 2) * cos - (mCanvasHeight / 2 - touchDownY + statusBarHeight) * sin) / pixPerDeg)
                + losAzDeg + panAz + 12* _360) % _360;
        float elClick = (float) (((touchDownX - mCanvasWidth / 2) * sin + (mCanvasHeight / 2 - touchDownY + statusBarHeight) * cos) / pixPerDeg)
                + losElDeg + panEl;
        return new float[]{azClick, elClick};
    }

    public GridSatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true); // make sure we get key events
        locationStatus = LOCATION_STATUS_OKAY;
        loadingSatellites = true;
        updatingTLEs = false;
        losElDeg = 55;
        losAzDeg = (float) 357.5;
        pixPerDeg = 50;
        zoomScaleFactor = 1.0;
        previewImageZoomScaleFactor = 1.;
        pausedZoomFactor = 1.;
        tempCameraZoomFactor = 1.;
        tempPausedZoomFactor = 1.;
        screenRotation = 5;
        statusBarHeight = getStatusBarHeight(context);
        //store these constants for drawing buttons
        fabMarginPixel = dpToPixel(FAB_MARGIN);
        fabDiameterPixel = dpToPixel(FAB_DIAMETER);
        debugText = "";
        //cache Drawables
        grayCameraPreviewBackground  = ContextCompat.getDrawable(context, R.drawable.random_gray_background_100);
        lookTitleKomicaWideY = ContextCompat.getDrawable(context, R.drawable.lookuptitlekomikawidey);
        menuButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_floating_menu_button_56_better);
        pauseButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_floating_pause_button_56_better);
        playButtonDrawable = ContextCompat.getDrawable(context, R.drawable.ic_floating_play_button_56_better);
        geoSatelliteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_saticon_yellow);
        leoSatelliteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_saticon_green);
        deepSatelliteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_saticon_red);
        cDeepSatelliteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_saticon_pink);
        cGeoSatelliteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_saticon_orange);
        orangeColor = ContextCompat.getColor(context, R.color.colorOrange);
        saveSharingImage = false;
        mySharingFile = new File(getAlbumStorageDir(LOOK_UP, context), SHARING_IMAGE_NAME);
    }

    /**
     * Status bar takes up pixels at the top of the canvas; want to draw FABs with a margin below the status bar
     *
     * @param pContext application context given to GridSatView
     * @return the size of the status bar, or 0
     */
    private static int getStatusBarHeight(Context pContext) {
        Resources resources = pContext.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public void setZoomScaleFactor(double scaleFactor) { zoomScaleFactor = scaleFactor;}
    public double getZoomScaleFactor() { return this.zoomScaleFactor;}
    public void setLosElDeg(float losElDeg){this.losElDeg = losElDeg;}
    public float getLosElDeg(){return losElDeg;}
    public void setScreenRotation(float screenRotation){this.screenRotation = screenRotation;}
    public float getScreenRotation(){return screenRotation;}
    public void setLosAzDeg(float losAzDeg){this.losAzDeg = losAzDeg;}
    public float getLosAzDeg(){return losAzDeg;}
    public  float getPixelPerDegree(){return  pixPerDeg;}
    public boolean isLiveMode() {return liveMode;}
    public void setLiveMode(boolean liveMode) {this.liveMode = liveMode;}
    public void setAzelConfigCorrection(float azelConfigCorrection) {
        this.azelConfigCorrection = azelConfigCorrection;
    }

    private int dpToPixel(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void drawPlayPauseButton(Canvas mCanvas) {
        Drawable d;
        if (liveMode) {
            d = pauseButtonDrawable;
        } else {
            d = playButtonDrawable;
        }
        int right = mCanvasWidth - fabMarginPixel;
        int left = right - fabDiameterPixel;
        int top = fabMarginPixel;
        int bottom = top + fabDiameterPixel;
        d.setBounds(left, top, right, bottom);
        d.draw(mCanvas);
    }
    private void drawMenuButton(Canvas mCanvas){
        int left = fabMarginPixel;
        int right = left + fabDiameterPixel;
        int top = fabMarginPixel;
        int bottom = top + fabDiameterPixel;
        menuButtonDrawable.setBounds(left, top, right, bottom);
        menuButtonDrawable.draw(mCanvas);
    }

    /**
     * test if the touchDown click is in the play/pause floating action button
     *
     * @return true if click is in the rectangle
     */
    public boolean clickIsInPlayPauseButton() {
        int right = mCanvasWidth - fabMarginPixel;
        int left = right - fabDiameterPixel;
        int top = fabMarginPixel+ statusBarHeight;
        int bottom = top + fabDiameterPixel;
        RectF ppButtonRect = new RectF(left, top, right, bottom);
        return ppButtonRect.contains(touchDownX, touchDownY);
    }

    /**
     * test if the touchDown click is in the menu floating action button
     *
     * @return true if click is in the rectangle
     */
    public boolean clickIsInMenuButton() {
        int left = fabMarginPixel;
        int right = left + fabDiameterPixel;
        int top = fabMarginPixel+ statusBarHeight;
        int bottom = top + fabDiameterPixel;
        RectF menuButtonRect = new RectF(left, top, right, bottom);
        return menuButtonRect.contains(touchDownX, touchDownY);
    }

    /**
     * When switching from paused mode to LiveMode, reset the pan values to zero,
     * so satellites appear at their true az/el locations
     */
    public void resetPan() {
        tempPanAz = 0;
        tempPanEl = 0;
        panEl = 0;
        panAz = 0;
    }

    public void setPreviewImageZoomScaleFactor(double previewImageZoomScaleFactor) {
        this.previewImageZoomScaleFactor = previewImageZoomScaleFactor;
    }

    public double getPreviewImageZoomScaleFactor() {
        return previewImageZoomScaleFactor;
    }

    public void setPreviewImageRotation(boolean rotatePreviewImage) {
        this.rotatePreviewImage = rotatePreviewImage;
    }

    public int getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(int locationStatus) {
        String locStatStr = "?";
        switch (locationStatus) {
            case 0:
                locStatStr = "UNKNOWN";
                break;
            case 1:
                locStatStr = "NONE";
                break;
            case 2:
                locStatStr = "OLD";
                break;
            case 3:
                locStatStr = "OKAY";
                break;
        }
        this.locationStatus = locationStatus;
    }

    public double getPausedZoomFactor() {
        return pausedZoomFactor;
    }

    public void setPausedZoomFactor(double pausedZoomFactor) {
        this.pausedZoomFactor = pausedZoomFactor;
    }

    public double getTempPausedZoomFactor() {
        return tempPausedZoomFactor;
    }

    public void setTempPausedZoomFactor(double tempPausedZoomFactor) {
        this.tempPausedZoomFactor = tempPausedZoomFactor;
    }

    public double getTempCameraZoomFactor() {
        return tempCameraZoomFactor;
    }

    public void setTempCameraZoomFactor(double tempCameraZoomFactor) {
        this.tempCameraZoomFactor = tempCameraZoomFactor;
    }
}
