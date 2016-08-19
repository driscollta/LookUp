package com.cyclebikeapp.upinthesky;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class AskWriteStoragePermission extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_LOCATION = 51;
    private static final int PERMISSIONS_REQUEST_STORAGE = 22;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MainActivity.DEBUG){
            Log.w(this.getClass().getName(), "asking for location permission in new Activity");}
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_permission);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.request_storage_permission_dialog_message))
                .setTitle(getString(R.string.request_storage_permission_dialog_title))
                .setIcon(R.drawable.ic_menu_camera)
                // Add the buttons
                .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Allow, show permissions dialog
                        ActivityCompat.requestPermissions(AskWriteStoragePermission.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                PERMISSIONS_REQUEST_STORAGE);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (MainActivity.DEBUG){Log.w(this.getClass().getName(), "onRequestPermissionsResult - allowed Location");}
                    // All required changes were successfully made user changed location settings
                    finish();
                } else{
                    if (MainActivity.DEBUG){Log.w(this.getClass().getName(), "onRequestPermissionsResult - denied Location");}
                }
            }
            break;
            case PERMISSIONS_REQUEST_STORAGE:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (MainActivity.DEBUG){Log.w(this.getClass().getName(), "onRequestPermissionsResult - allowed Storage");}
                    // All required changes were successfully made user changed location settings
                    finish();
                } else{
                    if (MainActivity.DEBUG){Log.w(this.getClass().getName(), "onRequestPermissionsResult - denied Storage");}
                }
            }
            break;
        }
    }
}
