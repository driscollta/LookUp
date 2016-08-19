package com.cyclebikeapp.upinthesky;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import static com.cyclebikeapp.upinthesky.Constants.PAID_VERSION;
/*
 * Copyright  2013 cyclebikeapp. All Rights Reserved.
*/

/**
 * just pop-up the XML layout that has a scrollable list of text; the text sections 
 * are defined in a series of strings. The only other thing is to change the custom title
 */

public class AboutScroller extends AppCompatActivity {
    TextView link1;
    TextView link2;
    TextView link3;
    TextView link4;
    TextView link5;
    TextView link6;
    TextView predict4java_link;
    TextView celestrak_link;
    Button upgradeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_scroller);
        setupActionBar();
        getWidgetIDs();
    }
    private void getWidgetIDs() {
        upgradeButton = (Button) findViewById(R.id.upgrade_button);
        int upgrade_visibility = View.VISIBLE;
        if (MainActivity.version == PAID_VERSION) {
            upgrade_visibility = View.GONE;
            TextView aboutAppText = (TextView) findViewById(R.id.free_version_ad_to_upgrade);
            assert aboutAppText != null;
            aboutAppText.setText(R.string.paid_version_about_app_text);
        } else{
            // in Free version, set sat color text hidden
            TextView satColorText = (TextView) findViewById(R.id.sat_color_header);
            satColorText.setVisibility(View.GONE);
        }
        assert upgradeButton != null;
        upgradeButton.setVisibility(upgrade_visibility);
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
        celestrak_link = (TextView) findViewById(R.id.celestrack_link);
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
        predict4java_link = (TextView) findViewById(R.id.sat_calc_link);
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
        link1 = (TextView) findViewById(R.id.link1_link);
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
        link2 = (TextView) findViewById(R.id.link2_link);
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
        link3 = (TextView) findViewById(R.id.link3_link);
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
        link4 = (TextView) findViewById(R.id.link4_link);
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
        link5 = (TextView) findViewById(R.id.link5_link);
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
        link6 = (TextView) findViewById(R.id.link6_link);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
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
