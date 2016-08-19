package com.cyclebikeapp.upinthesky;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import java.util.ArrayList;
import java.util.HashMap;

import static com.cyclebikeapp.upinthesky.Constants.CC_MAP_KEY_NORAD_NUMBER;
import static com.cyclebikeapp.upinthesky.Constants.CC_MAP_KEY_SAT_NAME;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_NORAD_NUM;
import static com.cyclebikeapp.upinthesky.Constants.DB_KEY_SAT_NAME;

public class SearchableActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private static String appPackageName = "com.cyclebikeapp.lookup";
    private static String freeVersionBodyText = "\nLearn more about this satellite by upgrading to the Plus version\n\n";
    // all the database functions
    private SATDBAdapter dataBaseAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searchable);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // create readable satDB
        dataBaseAdapter = new SATDBAdapter(getApplicationContext());
        dataBaseAdapter.openAsReadable();
        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);
        }
    }

    private void satelliteDialog(String dialog_title, String dialog_message, final String link) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String negativeButtonText = getString(R.string.more_info);
        if (MainActivity.version == Constants.FREE_VERSION){
            dialog_message = freeVersionBodyText + dialog_message;
            negativeButtonText = getString(R.string.upgrade_button_text);
        }
        builder.setMessage(dialog_message)
                .setTitle(dialog_title)
                .setIcon(R.drawable.ic_ufo)
                // Add the buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button, just exit dialog
                        NavUtils.navigateUpFromSameTask(SearchableActivity.this);
                    }
                });
        if (!link.equals("")) {
            builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked More Info button; start Intent to navigate to provided link
                    if (MainActivity.version == Constants.FREE_VERSION) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
                            PackageManager packageManager = SearchableActivity.this.getPackageManager();
                            if (browseIntent.resolveActivity(packageManager) != null) {
                                startActivity(browseIntent);
                            } else {
                                Log.w(this.getClass().getName(), getString(R.string.no_browser));
                            }
                        }
                    } else {
                        String url = link.trim();
                        if (!link.startsWith(getString(R.string.http_)) && !link.startsWith(getString(R.string.https_))) {
                            url = getString(R.string.http_) + link;
                        }
                        // if !hasWiFiInternetConnection && !hasMobileInternetPermission ask for mobileInternetPermission
                        // else must be okay to send browseIntent. If no internet connection, browser will complain
                        final Intent browseIntent = new Intent(Intent.ACTION_VIEW);
                        browseIntent.setData(Uri.parse(url));
                        startActivity(browseIntent);
                    }
                }
            });
        }
        builder.show();
    }
    private void doMySearch(String query) {
        // 1) assemble the query for "starts with"
        // 2) if no data, try query that starts with a bunch of characters, then a "(",
        // then search characters, then a bunch of characters to pick up an alternate satellite name
        // wildcard '_' looks for a single character; '%' looks for any number of characters
        // search term is case insensitive
        String startsWithQuery = query + "%";
        String alternateNameQuery = "%(" + query + "%";
        int cursorSize = 0;
        Cursor searchResult = dataBaseAdapter.searchDataBaseByName(startsWithQuery);
        if (searchResult == null) {
            //no result, try alternate name
            searchResult = dataBaseAdapter.searchDataBaseByName(alternateNameQuery);
        }
        if (searchResult != null){
            cursorSize = searchResult.getCount();
        } else {
            // pop-up satellite Dialog with text = "No Satellites found matching query"
            String text = String.format(getString(R.string.no_results), query);
            satelliteDialog(getString(R.string.search_results), text, "");
        }
        // if cursorSize = 1, retrieve data via NoradNumber, pop-up Satellite Dialog
        if (cursorSize == 1 && searchResult.moveToFirst()){
            int noradNumber = searchResult.getInt(searchResult.getColumnIndexOrThrow(DB_KEY_NORAD_NUM));
            String[] satString = dataBaseAdapter.fetchDeviceData(noradNumber);
            // now using Norad num show satellite dialog with name, brief description and link
            String title = satString[0];
            String message = satString[1];
            String link = satString[2];
            satelliteDialog(title, message, link);
        } else if (cursorSize > 1) {
            // if search result != null and has more than one entry, populate the List, wait for itemClick
            final ArrayList<HashMap<String, String>> searchSatList = makeSatelliteList(searchResult);
            // how many satellites did I find?  print Name, Norad num
            //situate a View in the Layout to anchor this pop-up
            final View aView = findViewById(R.id.search_popup_anchor);
            if (aView != null) {
                aView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showPopupMenu(aView, searchSatList);
                    }
                }, 50);
            }
        }
    }

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
    }

    @Override
    public void onBackPressed() {
        if (MainActivity.DEBUG) { Log.i(this.getClass().getName(), "onBackPressed()"); }
        NavUtils.navigateUpFromSameTask(SearchableActivity.this);
        super.onBackPressed();

    }
    private void showPopupMenu(View v, ArrayList<HashMap<String, String>> clickSatList) {
        // Don't leave a pop-up on screen if we're leaving MainActivity
        if (SearchableActivity.this.isFinishing()) {
            return;
        }
        PopupMenu popup = new PopupMenu(SearchableActivity.this, v);
        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(SearchableActivity.this);
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
        long noradNumber = item.getItemId();
        String[] satString = dataBaseAdapter.fetchDeviceData(noradNumber);
        // now using Norad num show pop-up dialog with name, brief description and link
        String title = satString[0];
        String message = satString[1];
        String link = satString[2];
        satelliteDialog(title, message, link);
        return true;
    }
}
