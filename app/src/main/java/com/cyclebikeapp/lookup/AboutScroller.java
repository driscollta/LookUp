package com.cyclebikeapp.lookup;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.cyclebikeapp.lookup.Constants.PAID_VERSION;
import static com.cyclebikeapp.lookup.MainActivity.version;
/*
 * Copyright  2013 cyclebikeapp. All Rights Reserved.
*/

/**
 * just pop-up the XML layout that has a scrollable list of text; the text sections 
 * are defined in a series of strings. The only other thing is to change the custom title
 */

@SuppressWarnings("ConstantConditions")
public class AboutScroller extends AppCompatActivity {
    private TextView link1;
    private TextView link2;
    private TextView link3;
    private TextView link4;
    private TextView link5;
    private TextView link6;
    private TextView predict4java_link;
    private TextView celestrak_link;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_scroller);
        setupActionBar();
        getWidgetIDs();
    }
    private void getWidgetIDs() {
        Button upgradeButton = findViewById(R.id.upgrade_button);
        if (version == PAID_VERSION) {

            TextView aboutAppText = findViewById(R.id.free_version_ad_to_upgrade);
            assert aboutAppText != null;
            aboutAppText.setText(R.string.paid_version_about_app_text);
        } else{
            // in Free version, set sat color text hidden
            findViewById(R.id.sat_color_layout).setVisibility(version == PAID_VERSION?GONE:VISIBLE);
        }
        assert upgradeButton != null;
        upgradeButton.setVisibility(version == PAID_VERSION?GONE:VISIBLE);
        upgradeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.cyclebikeapp.lookup")));
                } catch (android.content.ActivityNotFoundException anfe) {
                    Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.cyclebikeapp.lookup"));
                    //https://play.google.com/store/apps/details?id=com.cyclebikeapp.lookup
                    PackageManager packageManager = AboutScroller.this.getPackageManager();
                    if (browseIntent.resolveActivity(packageManager) != null) {
                        startActivity(browseIntent);
                    } else {
                        Log.w(this.getClass().getName(), getString(R.string.no_browser));
                    }
                }
            }
        });
        celestrak_link = findViewById(R.id.celestrack_link);
        celestrak_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse((String) celestrak_link.getText()));
                PackageManager packageManager = AboutScroller.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        });
        predict4java_link = findViewById(R.id.sat_calc_link);
        predict4java_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse((String) predict4java_link.getText()));
                PackageManager packageManager = AboutScroller.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        });
        link1 = findViewById(R.id.link1_link);
        link1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse((String) link1.getText()));
                PackageManager packageManager = AboutScroller.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        });
        link2 = findViewById(R.id.link2_link);
        link2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse((String) link2.getText()));
                PackageManager packageManager = AboutScroller.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        });
        link3 = findViewById(R.id.link3_link);
        link3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse((String) link3.getText()));
                PackageManager packageManager = AboutScroller.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        });
        link4 = findViewById(R.id.link4_link);
        link4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse((String) link4.getText()));
                PackageManager packageManager = AboutScroller.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        });
        link5 = findViewById(R.id.link5_link);
        link5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse((String) link5.getText()));
                PackageManager packageManager = AboutScroller.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        });
        link6 = findViewById(R.id.link6_link);
        link6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                browseIntent.setData(Uri.parse((String) link6.getText()));
                PackageManager packageManager = AboutScroller.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        });

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.about_lookup);
            actionBar.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                this.overridePendingTransition(0, 0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
