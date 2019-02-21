package com.cyclebikeapp.lookup;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import static com.cyclebikeapp.lookup.Constants.SDF_KEY_LINK;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_NORAD_NUMBER;
import static com.cyclebikeapp.lookup.Constants.SDF_KEY_NUM_SEARCH_RESULTS;

public class SatelliteDialogFragment extends DialogFragment {
    static final String DDF_KEY_CALLINGTAG = "DDF_key_callingtag";
    static final String MAIN = "Main";
    static final String SEARCH = "Search";
    AlertDialog.Builder mDialog;

    @Override
    public Dialog onCreateDialog(final Bundle bundle) {
        String title = getArguments().getString(Constants.SDF_KEY_TITLE);
        String dialogMessage = getArguments().getString(Constants.SDF_KEY_MESSAGE);
        final String link = getArguments().getString(Constants.SDF_KEY_LINK);
        final int noradNumber = getArguments().getInt(Constants.SDF_KEY_NORAD_NUMBER);
        final int numSearchResults = getArguments().getInt(SDF_KEY_NUM_SEARCH_RESULTS);
        String status = getArguments().getString(Constants.SDF_KEY_STATUS);
        String negativeButtonText = getString(R.string.more_info);
        if (MainActivity.version == Constants.FREE_VERSION && !link.equals("")) {
            String freeVersionBodyText = "\nLearn more about this satellite by upgrading to the Plus version\n\n";
            dialogMessage = freeVersionBodyText + dialogMessage;
            negativeButtonText = getString(R.string.upgrade_button_text);
        }
        mDialog = new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setIcon(R.drawable.ic_ufo)
                .setMessage(dialogMessage)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getTag().contains(MAIN)) {
                            ((MainActivity) getActivity()).doSatelliteDialogPostiveClick();

                        } else if (getTag().contains(SEARCH)) {
                            Bundle bundle = new Bundle();
                            bundle.putInt(SDF_KEY_NUM_SEARCH_RESULTS, numSearchResults);
                            ((SearchableActivity) getActivity()).doSatelliteDialogPositiveClick(bundle);
                        }
                    }
                })
                .setNeutralButton(status, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getTag().contains(MAIN)) {
                            ((MainActivity) getActivity()).doSatelliteDialogPostiveClick();

                        } else if (getTag().contains(SEARCH)) {
                            Bundle bundle = new Bundle();
                            bundle.putInt(SDF_KEY_NUM_SEARCH_RESULTS, numSearchResults);
                            ((SearchableActivity) getActivity()).doSatelliteDialogPositiveClick(bundle);
                        }
                    }
                })
                .setNegativeButton(negativeButtonText,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Bundle bundle = new Bundle();
                                bundle.putCharSequence(SDF_KEY_LINK, link);
                                bundle.putInt(SDF_KEY_NORAD_NUMBER, noradNumber);
                                if (getTag().contains(MAIN)) {
                                    ((MainActivity) getActivity()).doSatelliteDialogNegativeClick(bundle);
                                } else if (getTag().contains(SEARCH)) {
                                    ((SearchableActivity) getActivity()).doSatelliteDialogNegativeClick(bundle);
                                }
                            }
                        }
                );
        return mDialog.create();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SatelliteDialogFragment.
     */
    public static SatelliteDialogFragment newInstance(Bundle b) {
        SatelliteDialogFragment frag = new SatelliteDialogFragment();
        String callingTag = frag.getTag();
        b.putCharSequence(DDF_KEY_CALLINGTAG, callingTag);
        frag.setArguments(b);
        return frag;
    }
    public SatelliteDialogFragment() {
        // Required empty public constructor
    }

}
