package com.cyclebikeapp.lookup;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import satellite.Satellite;

import static com.cyclebikeapp.lookup.Constants.CC_MAP_KEY_NORAD_NUMBER;
import static com.cyclebikeapp.lookup.Constants.CC_MAP_KEY_SAT_NAME;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_NORAD_NUM;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_INFO_LINK;
import static com.cyclebikeapp.lookup.Constants.DB_KEY_SAT_NAME;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_ALTITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_LATITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_LONGITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_DEFAULT_TIME;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_ALTITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_LATITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_LONGITUDE;
import static com.cyclebikeapp.lookup.Constants.PREFS_KEY_TIME;
import static com.cyclebikeapp.lookup.Constants.PREFS_NAME;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_LINK;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_MESSAGE;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_NORAD_NUMBER;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_NUM_SEARCH_RESULTS;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_STATUS;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_TITLE;
import static com.cyclebikeapp.lookup.Constants.badLink;
import static com.cyclebikeapp.lookup.Constants.nssdcLinkBase;
import static com.cyclebikeapp.lookup.SatelliteDialogFragment.SEARCH;

@SuppressWarnings("ConstantConditions")
public class SearchableActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    // all the database functions
    private SATDBAdapter dataBaseAdapter = null;
    private PopupMenu popup;
    private View popupAnchor;
    private SatelliteTabulator mSatCalculator;

    public SearchableActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSatCalculator = new SatelliteTabulator(getApplicationContext());
        setContentView(R.layout.activity_searchable);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override
    protected void onResume() {
        if (MainActivity.DEBUG) {
            Log.w(this.getClass().getName(), "onResume()");
        }
        // create readable satDB
        dataBaseAdapter = new SATDBAdapter(getApplicationContext());
        dataBaseAdapter.open();
        popupAnchor = findViewById(R.id.search_popup_anchor);
        popup = new PopupMenu(SearchableActivity.this, popupAnchor);
        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);
        }
        super.onResume();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (MainActivity.DEBUG) {
            Log.w(this.getClass().getName(), "onSaveInstanceState()");
        }
        super.onSaveInstanceState(outState);
    }
    private Location getLocFromSharedPrefs() {
        Location aLoc = new Location(LocationManager.NETWORK_PROVIDER);
        Context mContext = getApplicationContext();
        SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, 0);
        aLoc.setLongitude(Double.parseDouble(settings.getString(PREFS_KEY_LONGITUDE, PREFS_DEFAULT_LONGITUDE)));
        aLoc.setLatitude(Double.parseDouble(settings.getString(PREFS_KEY_LATITUDE, PREFS_DEFAULT_LATITUDE)));
        aLoc.setAltitude(Double.parseDouble(settings.getString(PREFS_KEY_ALTITUDE, PREFS_DEFAULT_ALTITUDE)));
        // this is just temporary until we get a location from LocationHelper
        aLoc.setTime(settings.getLong(PREFS_KEY_TIME, PREFS_DEFAULT_TIME));
        return aLoc;
    }
    @SuppressLint("DefaultLocale")
    private void showSatelliteDialogFragment(String status,
            String dialog_title,
            String dialog_message,
            final String link,
            final int numSearchResults,
            final int noradNumber) {
        long timeNow = Util.getNowTime();
        Satellite theSatellite = mSatCalculator.buildSatelliteForNoradNumber(getLocFromSharedPrefs(),
                timeNow, dataBaseAdapter, noradNumber);
        double azLook = -999.;
        double elLook = -999.;
        if (theSatellite != null) {
            azLook = theSatellite.getTLE().getLookAngleAz();
            elLook = theSatellite.getTLE().getLookAngleEl();
        }
        String azimuthString = (azLook <= -998.?"---":String.format(Constants.FORMAT_3_1F, azLook));
        String elString = (elLook <= -998.?"---":String.format(Constants.FORMAT_3_1F, elLook));
        String satelliteLocationString = getString(R.string.location_string_az_el, noradNumber, azimuthString, elString);
        Bundle dialogBundle = new Bundle();
        dialogBundle.putCharSequence(SDF_KEY_TITLE, dialog_title);
        dialogBundle.putCharSequence(SDF_KEY_MESSAGE, satelliteLocationString + dialog_message);
        dialogBundle.putCharSequence(SDF_KEY_LINK, link);
        dialogBundle.putCharSequence(SDF_KEY_STATUS, status);
        dialogBundle.putInt(SDF_KEY_NUM_SEARCH_RESULTS, numSearchResults);
        dialogBundle.putInt(SDF_KEY_NORAD_NUMBER, noradNumber);
        SatelliteDialogFragment newFragment = SatelliteDialogFragment.newInstance(dialogBundle);
        newFragment.show(getFragmentManager(), SEARCH);
    }

    public void doSatelliteDialogPositiveClick(Bundle aBundle) {
        // User clicked OK button, just exit dialog
        int numSearchResults = aBundle.getInt(SDF_KEY_NUM_SEARCH_RESULTS);
        if (numSearchResults > 1) {
            // go back to showing the popup from search
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.actions, popup.getMenu());
            popup.show();
        } else {
            NavUtils.navigateUpFromSameTask(SearchableActivity.this);
        }
    }

    public void doSatelliteDialogNegativeClick(Bundle aBundle) {
        String link = aBundle.getString(Constants.SDF_KEY_LINK);
        int noradNumber = aBundle.getInt(Constants.SDF_KEY_NORAD_NUMBER);

        if (MainActivity.version == Constants.FREE_VERSION) {
            // User clicked Upgrade button; start Intent to go to Play Store
            String appPackageName = "com.cyclebikeapp.lookup";
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                //Play Store app not found, use Browser
                Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
                PackageManager packageManager = SearchableActivity.this.getPackageManager();
                if (browseIntent.resolveActivity(packageManager) != null) {
                    startActivity(browseIntent);
                } else {
                    Log.w(this.getClass().getName(), getString(R.string.no_browser));
                }
            }
        } else {
            // User clicked More Info button; start Intent to navigate to provided link
            String url = link.trim();
            if (!link.startsWith(getString(R.string.http_)) && !link.startsWith(getString(R.string.https_))) {
                url = getString(R.string.http_) + link;
            }
            // if link.contains "badLink", use NSSDC link based on Int'l code
            if (url.toUpperCase().contains(badLink)){
                if (MainActivity.DEBUG){Log.w(this.getClass().getName(), "substituting NSSDC URL for bad link");}
                String[] someData = dataBaseAdapter.fetchDeviceData(noradNumber);
                String intlCode = someData[4];
                url = nssdcLinkBase + intlCode;
            }
            // run link tester and label this as a bad link if error; replace dblink with "badLink"
            new TestURL().execute(url, String.valueOf(noradNumber));
            // if !hasWiFiInternetConnection && !hasMobileInternetPermission ask for mobileInternetPermission
            // else must be okay to send browseIntent. If no internet connection, browser will complain
            final Intent browseIntent = new Intent(Intent.ACTION_VIEW);
            browseIntent.setData(Uri.parse(url));
            startActivity(browseIntent);
        }
    }

    private void doMySearch(String query) {
        // 1) if query is a number search by Norad Number or year
        // 2) if query is a Int'l code, search 'Other' column in DB
        // 3) assemble the query for "starts with" and alternate query that starts with a bunch of characters, then a "(",
        // then search characters, then a bunch of characters to pick up an alternate satellite name
        // wildcard '_' looks for a single character; '%' looks for any number of characters
        // search term is case insensitive
        Cursor searchResult;
        if (queryStartsWithNumber(query)){
            searchResult = searchByNumber(query);
        } else {
            query = getISSSynonyms(query);
            query = getHubbleSynonyms(query);
            searchResult = searchByName(query);
        }
        int cursorSize = getCursorSize(searchResult);
        if (cursorSize == 0){
            // didn't get anything using the full query
            // strip leading spaces, parse to get the first word of the query
            // .stripStart is part of org.apache.commons.lang3.StringUtils
            // if this was a number search, searchByName will probably still return no result
            String stripLeadingQuery = StringUtils.stripStart(query, " ");
            String queryFirstWord = Util.getFirstWord(stripLeadingQuery);
            Log.w(this.getClass().getName(), "query: " + query + " stripLeading: "  + stripLeadingQuery + " first word: " + queryFirstWord);
            searchResult = searchByName(queryFirstWord);
            cursorSize = getCursorSize(searchResult);
            if (cursorSize == 0) {
                // pop-up satellite Dialog with text = "No Satellites found matching query"
                String text = String.format(getString(R.string.no_results), query);
                showSatelliteDialogFragment("", getString(R.string.search_results), text, "", cursorSize, 0);
            }
        }
        // if cursorSize = 1, retrieve data via NoradNumber, pop-up Satellite Dialog
        if (cursorSize == 1 && searchResult.moveToFirst()){
            int noradNumber = searchResult.getInt(searchResult.getColumnIndexOrThrow(DB_KEY_NORAD_NUM));
            String[] satString = dataBaseAdapter.fetchDeviceData(noradNumber);
            // now using Norad num show satellite dialog with name, brief description and link
            String title = satString[0];
            String message = satString[1];
            String link = satString[2];
            String status = satString[5];
            showSatelliteDialogFragment(status, title, message, link, cursorSize, noradNumber);
        } else if (cursorSize > 1) {
            // if search result != null and has more than one entry, populate the List, wait for itemClick
            // has to be 'final' since it's in the run()
            final ArrayList<HashMap<String, String>> searchSatList = makeSatelliteList(searchResult);
            // how many satellites did I find?  print Name, Norad num
            //situate a View in the Layout to anchor this pop-up
            popupAnchor.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showPopupMenu(searchSatList);
                }
            }, 50);
        }
    }

    private boolean queryStartsWithNumber(String query) {
        return Character.isDigit(query.charAt(0));
    }

    private int getCursorSize(Cursor searchResult) {
        int cursorSize = 0;
        if (searchResult != null){
            cursorSize = searchResult.getCount();
        }
        return cursorSize;
    }

    private Cursor searchByName(String query) {
        String startsWithQuery = query + "%";
        String alternateNameQuery = "%(" + query + "%";

        Cursor searchResult = dataBaseAdapter.searchDataBaseByName(startsWithQuery);
        if (searchResult == null) {
            //no result, try alternate name
            searchResult = dataBaseAdapter.searchDataBaseByName(alternateNameQuery);
        }
        return searchResult;
    }

    private Cursor searchByNumber(String query) {
// try matching Norad Number exactly
        Cursor searchResult = dataBaseAdapter.searchDataBaseByNoradNumber(query);
        if (searchResult == null) {
            //no result, try exact Int'l code
            searchResult = dataBaseAdapter.searchDataBaseByIntlCode(query);
        }
        if (searchResult == null) {
            //no result, try year in Int'l code
            String startsWithQuery = query + "%";
            searchResult = dataBaseAdapter.searchDataBaseByYear(startsWithQuery);
        }
        return searchResult;
    }
    private String getHubbleSynonyms(String query) {
        String returnString = query;
        if (query.toUpperCase().startsWith("HUBBLE")){
            returnString = "HST";
        }
        return returnString;
    }

    private String getISSSynonyms(String query) {
        String returnString = query;
        if (query.toUpperCase().startsWith("INTERNATIONAL SPACE")
                || query.toUpperCase().startsWith("SPACE STATION")
                || query.toUpperCase().startsWith("ZARYA")){
            returnString = "ISS";
        }
        return returnString;
    }

    /**
     * Convert the Cursor into a Hashmap used for the pop-up menu
     * @param searchResult is a Cursor containing Norad numbers and satellite names from the match to the search
     * @return is a Hashmap of the Norad numbers and names to populate the list
     */
    private ArrayList<HashMap<String, String>> makeSatelliteList(Cursor searchResult) {
        ArrayList<HashMap<String, String>> returnData = new ArrayList<>();
        if (searchResult != null && searchResult.moveToFirst()) {
            do {
                HashMap<String, String> hmItem = new HashMap<>();
                hmItem.put(CC_MAP_KEY_SAT_NAME, searchResult.getString(searchResult.getColumnIndexOrThrow(DB_KEY_SAT_NAME)));
                hmItem.put(CC_MAP_KEY_NORAD_NUMBER, searchResult.getString(searchResult.getColumnIndexOrThrow(DB_KEY_NORAD_NUM)));
                returnData.add(hmItem);
            } while (searchResult.moveToNext());
        }
        return returnData;
    }

    @Override
    protected void onStop() {
        if (MainActivity.DEBUG) {
            Log.w(this.getClass().getName(), "onStop()");
        }
        super.onStop();
        if (dataBaseAdapter != null) {
            dataBaseAdapter.close();
        }
    popup.dismiss();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
            NavUtils.navigateUpFromSameTask(SearchableActivity.this);
    }

    private void showPopupMenu(ArrayList<HashMap<String, String>> clickSatList) {
        // Don't leave a pop-up on screen if we're leaving Activity
        if (SearchableActivity.this.isFinishing()) {
            return;
        }
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(SearchableActivity.this);
        //popup.setOnDismissListener(SearchableActivity.this);
        //add items to pop-up depending on # satellites
        int order = 1;
        // only 1 group
        int groupId = 0;
        for (HashMap<String, String> hm : clickSatList) {
            // use "itemId" entry to store Norad number.
            // When user clicks on List we're given itemId and we can then retrieve Norad Number
            int itemId = Integer.parseInt(hm.get(CC_MAP_KEY_NORAD_NUMBER));
            popup.getMenu().add(groupId, itemId, order, hm.get(CC_MAP_KEY_SAT_NAME));
            order++;
        }
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.actions, popup.getMenu());
        popup.show();
    }

    /**
     * This method will be invoked when a menu item is clicked if the item itself did
     * not already handle the event.
     *
     * @param item {@link MenuItem} that was clicked
     * @return <code>true</code> if the event was handled, <code>false</code> otherwise.
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int noradNumber = item.getItemId();
        String[] satString = dataBaseAdapter.fetchDeviceData(noradNumber);
        // now using Norad num show pop-up dialog with name, brief description and link
        String title = satString[0];
        String message = satString[1];
        String link = satString[2];
        String status = satString[5];
        showSatelliteDialogFragment(status, title, message, link, popup.getMenu().size(), noradNumber);
        return true;
    }


    /**
     * Check url for a bad link when clicking the "More Info" button in a satellite Dialog
     * Use the same routine as downloading TLEs from Celestrak. It will throw an IOException
     * if the link is bad, so then replace the link in the dataBase with "badLink". The next time
     * the user clicks on the MoreInfo button we'll re-direct to the default link to NSSDC
     */
    private class TestURL extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... url) {
            GetTLEsFromCelestrak ctfc = new GetTLEsFromCelestrak();
            try {
                ctfc.downloadUrl(url[0]);
            } catch (IOException e) {
                if (MainActivity.DEBUG) { Log.w(this.getClass().getName(), "testing URL... bad link"); }
                int noradNumber = Integer.parseInt(url[1]);
                ContentValues badLinkContent = new ContentValues();
                badLinkContent.put(DB_KEY_SAT_INFO_LINK, badLink);
                dataBaseAdapter.updateSatelliteRecord(noradNumber, badLinkContent);
                return null;
            }
            if (MainActivity.DEBUG) { Log.w(this.getClass().getName(), "testing URL... seems okay"); }
            return null;
        }
    }
}
