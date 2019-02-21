package com.cyclebikeapp.lookup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import static com.cyclebikeapp.lookup.Constants.*;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;


@SuppressWarnings("ConstantConditions")
public class SatelliteFilterSettings extends AppCompatActivity {

    private ListView satCategoryList;
    private Button showAll;
    private Button clearList;
    //FREE version only has active GEO satellites, so the active / inactive switch makes no difference
    private CheckBox showInactiveSatsCB;

    private static final String BTN_CHECK_ON = "10";
    private static final String BTN_CHECK_OFF = "11";
    private static final String BTN_CHECK_DIM = "12";

    private boolean[] satCategoryShow;
    private String[] satelliteCategoryList;
    private Snackbar adSnackbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.satellite_filter_view);
        initializeSatelliteCategoryList();
        satCategoryShow = new boolean[satelliteCategoryList.length];
        getWidgetIDs();
        SharedPreferences defaultSettings = getDefaultSharedPreferences(getApplicationContext());
        // Set state of show inactive sat checkbox; too many satellites, set default to not show inactive
        //FREE version only has active GEO satellites, so the active / inactive switch makes no difference
        showInactiveSatsCB.setChecked(defaultSettings.getBoolean(SHOW_INACTIVE_SATS_KEY, false));
        adSnackbar = Snackbar.make(
                satCategoryList,
                getString(R.string.upgrade_snackbar_ad),
                Snackbar.LENGTH_INDEFINITE);
        //satCategoryList.setOnScrollListener(scrollListener);
        //set clicked list item to opposite of previous setting
        satCategoryList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // if free version and position clicked is paid category (TV & Radio are the first two positions),
                // show snackBar with paid version ad and link
                if (MainActivity.version == FREE_VERSION && position > 2) {
                    adSnackbar.setAction("upgrade", new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.cyclebikeapp.lookup")));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.cyclebikeapp.lookup")));
                            }
                        }
                    }).show();
                } else {
                    adSnackbar.dismiss();
                    satCategoryShow[position] = !satCategoryShow[position];
                    refreshVeloList();
                }
            }
        });
        showAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAllCategories();
                refreshVeloList();
            }
        });
        clearList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNoCategories();
                refreshVeloList();
            }
        });
        setupActionBar();
        loadPreferences();
    }

    private void selectNoCategories() {
        for (int i = 0; i < satelliteCategoryList.length; i++) {
            // only check valid categories if free version
            satCategoryShow[i] = false;
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.satellite_category_settings);
            actionBar.show();
        }
    }

    private void initializeSatelliteCategoryList() {
        satelliteCategoryList = new String[]{"Television", "Radio", "Weather", "Military",
                "Amateur Radio", "Communications", "Earth Observing", "Science", "Data", "GPS", "Space Station"};
    }
    //{"Tv", "Radio", "Weather", "Military",
    //"HAM", "Communications", "Earth Observing", "Science", "Data", "GPS", "Space Station"}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                saveState();
                NavUtils.navigateUpFromSameTask(this);
                this.overridePendingTransition(0, 0);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectAllCategories() {
        for (int i = 0; i < satelliteCategoryList.length; i++) {
            // only check valid categories if free version
            satCategoryShow[i] = !(MainActivity.version == FREE_VERSION && i > 2);
        }
    }

    private void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        satCategoryShow[0] = settings.getBoolean(SHOW_TELEVISION_SETTING_KEY, true);
        satCategoryShow[1] = settings.getBoolean(SHOW_RADIO_SETTING_KEY, true);
        satCategoryShow[2] = settings.getBoolean(SHOW_WEATHER_SETTING_KEY, true);
        satCategoryShow[3] = settings.getBoolean(SHOW_MILITARY_SETTING_KEY, true);
        satCategoryShow[4] = settings.getBoolean(SHOW_HAM_SETTING_KEY, true);
        satCategoryShow[5] = settings.getBoolean(SHOW_COMMUNICATION_SETTING_KEY, true);
        satCategoryShow[6] = settings.getBoolean(SHOW_EARTHOBSERVING_SETTING_KEY, true);
        satCategoryShow[7] = settings.getBoolean(SHOW_SCIENCE_SETTING_KEY, true);
        satCategoryShow[8] = settings.getBoolean(SHOW_DATA_SETTING_KEY, true);
        satCategoryShow[9] = settings.getBoolean(SHOW_GPS_SETTING_KEY, true);
        satCategoryShow[10] = settings.getBoolean(SHOW_ISS_SETTING_KEY, true);
        refreshVeloList();
    }

    private void refreshVeloList() {
        // remember scroll position and restore it after refresh
        int index = satCategoryList.getFirstVisiblePosition();
        View v = satCategoryList.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - satCategoryList.getPaddingTop());
        ArrayList<HashMap<String, String>> satCategoryList = new ArrayList<>();
        for (int i = 0; i < satelliteCategoryList.length; i++) {
            HashMap<String, String> map = new HashMap<>();
            map.put(KEY_SATELLITE_CATEGORY, satelliteCategoryList[i]);
            //imageLevel refers to a checked, or unchecked checkbox icon (BTN_CHECK_ON:BTN_CHECK_OFF)
            //in image_icon.xml level-list; also use this in the file chooser
            String imageLevel = ((satCategoryShow[i]) ? BTN_CHECK_ON : BTN_CHECK_OFF);
            // check if category is allowed in free version
            if (i > 2 && MainActivity.version == FREE_VERSION) {
                imageLevel = BTN_CHECK_DIM;
            }
            map.put(KEY_THUMB, imageLevel);
            satCategoryList.add(map);
        }
        this.satCategoryList.setAdapter(new SatelliteFilterAdapter(this, satCategoryList));
        this.satCategoryList.setSelectionFromTop(index, top);
    }

    private void getWidgetIDs() {
        satCategoryList = (ListView) findViewById(R.id.velo_list);
        assert satCategoryList != null;
        satCategoryList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        showAll = (Button) findViewById(R.id.show_all_button);
        clearList = (Button) findViewById(R.id.clear_all_button);
        showInactiveSatsCB = (CheckBox) findViewById(R.id.showInactiveCheckBox);
    }

    @Override
    protected void onPause() {
        saveState();
        super.onPause();
    }

    private void saveState() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(SHOW_TELEVISION_SETTING_KEY, satCategoryShow[0]);
        editor.putBoolean(SHOW_RADIO_SETTING_KEY, satCategoryShow[1]);
        editor.putBoolean(SHOW_WEATHER_SETTING_KEY, satCategoryShow[2]);
        editor.putBoolean(SHOW_MILITARY_SETTING_KEY, satCategoryShow[3]);
        editor.putBoolean(SHOW_HAM_SETTING_KEY, satCategoryShow[4]);
        editor.putBoolean(SHOW_COMMUNICATION_SETTING_KEY, satCategoryShow[5]);
        editor.putBoolean(SHOW_EARTHOBSERVING_SETTING_KEY, satCategoryShow[6]);
        editor.putBoolean(SHOW_SCIENCE_SETTING_KEY, satCategoryShow[7]);
        editor.putBoolean(SHOW_DATA_SETTING_KEY, satCategoryShow[8]);
        editor.putBoolean(SHOW_GPS_SETTING_KEY, satCategoryShow[9]);
        editor.putBoolean(SHOW_ISS_SETTING_KEY, satCategoryShow[10]);
        editor.apply();
        SharedPreferences defaultSettings = getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor defaultEditor = defaultSettings.edit();
        //FREE version only has active GEO satellites, so the active / inactive switch makes no difference
        defaultEditor.putBoolean(SHOW_INACTIVE_SATS_KEY, showInactiveSatsCB.isChecked()).apply();
    }

}
