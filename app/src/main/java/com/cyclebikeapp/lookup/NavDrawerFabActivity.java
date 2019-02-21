package com.cyclebikeapp.lookup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static com.cyclebikeapp.lookup.Constants.LOOK_UP;
import static com.cyclebikeapp.lookup.Constants.NAV_DRAWER_LIVE_MODE_KEY;
import static com.cyclebikeapp.lookup.Constants.PAID_VERSION;
import static com.cyclebikeapp.lookup.Constants.SHARING_IMAGE_NAME;
import static com.cyclebikeapp.lookup.Util.getAlbumStorageDir;


/**
 * Created by TommyD on 3/16/2016.
 *
 */
@SuppressWarnings("ConstantConditions")
public class NavDrawerFabActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{
    private static final int REQUEST_CODE_SETTINGS = 902;
    private static final int REQUEST_CODE_SEARCH = 903;
    private static final int PERMISSIONS_REQUEST_STORAGE = 22;
    private NavigationView navigationView;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navdrawer_fab_main);

        FloatingActionButton fabToggle = (FloatingActionButton) findViewById(R.id.fab_toggle);
        assert fabToggle != null;
        fabToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleNavDrawer();
            }
        });
        // Disable all animations
        getWindow().setWindowAnimations(0);
        final FloatingActionButton fabPlayPause = (FloatingActionButton) findViewById(R.id.fab_play);
        assert fabPlayPause != null;
        fabPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPlayPauseClick(fabPlayPause);
            }
        });

        navigationView = (NavigationView) findViewById(R.id.navigation_drawer_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(MainActivity.version == PAID_VERSION) {
            hideUpgradeItem();
        }
        toggleNavDrawer();
        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                Intent i = getIntent();
                i.putExtra(NAV_DRAWER_LIVE_MODE_KEY, false);
                setResult(RESULT_OK, i);
                finish();
            }
        });

    }
    @Override
    protected void onPause() {
        super.onPause();
        Intent i = getIntent();
        i.putExtra(NAV_DRAWER_LIVE_MODE_KEY, false);
        setResult(RESULT_OK, i);
    }

    private boolean hideUpgradeItem(){
        Menu menu = navigationView.getMenu();
        MenuItem item = menu.findItem(R.id.nav_upgrade);
        item.setVisible(false);
        return true;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case REQUEST_CODE_SETTINGS:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onActivityResult() - result ok ");}
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    assert drawer != null;
                    if (drawer.isDrawerOpen(navigationView)) {
                        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onActivityResult() - Drawer open");}
                       // drawer.closeDrawer(GravityCompat.START);
                    } else {
                        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onActivityResult() - Drawer closed");}
                        //drawer.openDrawer(GravityCompat.START);
                    }
                    //toggleNavDrawer();
                }
                break;
            case REQUEST_CODE_SEARCH:
                if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onActivityResult() - returned from Search");}
                if (resultCode == RESULT_OK) {
                    if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onActivityResult() - result ok ");}
                    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                    assert drawer != null;
                    if (drawer.isDrawerOpen(navigationView)) {
                        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onActivityResult() - Drawer open");}
                        // drawer.closeDrawer(GravityCompat.START);
                    } else {
                        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onActivityResult() - Drawer closed");}
                        drawer.openDrawer(GravityCompat.START);
                    }
                    //toggleNavDrawer();
                }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case PERMISSIONS_REQUEST_STORAGE: {
            }
        }
    }

    private void doPlayPauseClick(FloatingActionButton fabPlayPause) {
        //set fabicon to pause; we don't use this code any more
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fabPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_fab_pause_cropped, getApplicationContext().getTheme()));
        } else {
            fabPlayPause.setImageDrawable(ContextCompat.getDrawable(NavDrawerFabActivity.this, R.drawable.ic_fab_pause_cropped));
        }
        if (MainActivity.DEBUG) {
            Log.i(this.getClass().getName(), "playPauseButton onClick() ");
        }
        Intent i = getIntent();
        i.putExtra(NAV_DRAWER_LIVE_MODE_KEY, true);
        setResult(RESULT_OK, i);
        finish();
    }

    private void toggleNavDrawer() {
        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "toggleNavDrawer() ");}
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(navigationView)) {
            drawer.closeDrawer(GravityCompat.START);
            Intent i = getIntent();
            i.putExtra(NAV_DRAWER_LIVE_MODE_KEY, false);
            setResult(RESULT_OK, i);
        } else {
            drawer.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public void onBackPressed() {
        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onBackPressed()");}
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Intent i = getIntent();
            i.putExtra(NAV_DRAWER_LIVE_MODE_KEY, false);
            setResult(RESULT_OK, i);
            finish();
            super.onBackPressed();
        }
    }
    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();

        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                Intent i = getIntent();
                i.putExtra(NAV_DRAWER_LIVE_MODE_KEY, false);
                setResult(RESULT_OK, i);
            }
        });
        toggleNavDrawer();
        if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "onSearchRequested() ");}
        startSearch(null, false, appData, false);
        return super.onSearchRequested();
    }
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "settings() ");}
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityForResult(settingsIntent, REQUEST_CODE_SETTINGS);
        } else if (id == R.id.nav_share) {
            // if Marshmallow and no write permission, show Alert Dialog to complain with button to "Allow"
            // put this in a new Activity to show dialog asking for write storage permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Util.hasStoragePermission(this)){
                startActivity(new Intent(this, AskWriteStoragePermission.class));
            } else if (Util.isExternalStorageWritable(this)) {
                File sharingFileDir = getAlbumStorageDir(LOOK_UP, getApplicationContext());
                // Create the new Intent using the 'Send' action.
                Intent fileShare = new Intent(Intent.ACTION_SEND);
                fileShare.setFlags(FLAG_GRANT_READ_URI_PERMISSION);
                File sharingFile = new File(sharingFileDir.toString(), SHARING_IMAGE_NAME);
                if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "sharing file: " + sharingFile.toString()
                        + " length: " + sharingFile.length() + " can read: " + (sharingFile.canRead()?"yes":"no"));}
                Uri fileUri = FileProvider.getUriForFile(
                        getApplicationContext(),
                        getApplicationContext().getPackageName() + ".provider", sharingFile);
                fileShare.setDataAndType(fileUri, "image/jpeg");
                // Broadcast the Intent.
                startActivity(Intent.createChooser(fileShare, getString(R.string.share_file)));
                if (MainActivity.DEBUG){
                    String[] fileList = sharingFileDir.list();
                    StringBuilder files = new StringBuilder("");
                    for (String s:fileList){
                        files.append(s);
                        files.append("\n");
                    }
                    Log.i(this.getClass().getName(), "sharing files in dir: " + files.toString());
                    Log.i(this.getClass().getName(), "sharing file from Uri: " + fileShare.getData().getPath());
                }

            }
        } else if (id == R.id.nav_satellite_filter) {
            startActivity(new Intent(this, SatelliteFilterSettings.class));
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(this, AboutScroller.class));
        } else if (id == R.id.search_help) {
            startActivity(new Intent(this, SearchHelpScroller.class));
        } else if (id == R.id.nav_search){
            if (MainActivity.DEBUG) {Log.i(this.getClass().getName(), "search() ");}
            //todo could startActivity for search, where we'll do onSearchRequested during on Resume()
            onSearchRequested();
        } else if (id == R.id.nav_upgrade){

            final String appPackageName = "com.cyclebikeapp.lookup";
            if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "packageName: " + appPackageName);}
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
                PackageManager packageManager = NavDrawerFabActivity.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        }
        else if (id == R.id.nav_up2){

            final String appPackageName = "com.cyclebikeapp.lookup.airplanes";
            if (MainActivity.DEBUG) {Log.w(this.getClass().getName(), "packageName: " + appPackageName);}
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
                PackageManager packageManager = NavDrawerFabActivity.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        assert drawer != null;
        //drawer.closeDrawer(GravityCompat.START);
        return true;
    }

}
