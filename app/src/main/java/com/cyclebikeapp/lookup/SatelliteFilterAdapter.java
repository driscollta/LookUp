package com.cyclebikeapp.lookup;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import static com.cyclebikeapp.lookup.Constants.KEY_SATELLITE_CATEGORY;
import static com.cyclebikeapp.lookup.Constants.KEY_THUMB;
/*
 * Copyright 2013 cyclebikeapp. All Rights Reserved.
 * Displays a list of satellite categories that the user can select a check box to allow display
*/

class SatelliteFilterAdapter extends BaseAdapter {
	private ArrayList<HashMap<String, String>> data;
	private static LayoutInflater inflater = null;

	SatelliteFilterAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
		data = d;
		inflater = (LayoutInflater) a
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return data.size();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

    @SuppressLint("InflateParams")
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (convertView == null) {
            vi = inflater.inflate(R.layout.satellite_category_list_row, null);
        }
        TextView title = (TextView) vi.findViewById(R.id.title); // title
        ImageView thumb_image = (ImageView) vi.findViewById(R.id.list_image);
        HashMap<String, String> fileItem;
        fileItem = data.get(position);
        // Setting all values in listview
        title.setText(fileItem.get(KEY_SATELLITE_CATEGORY));
        //set text to dim if version = free and category is paid category; use image level showing disabled chcek box
        int imageLevel = Integer.valueOf(fileItem.get(KEY_THUMB));
        if (imageLevel == 12) {
            int color = ContextCompat.getColor(parent.getContext(), R.color.colorDisabledText);
            title.setTextColor(color);
        } else {
            title.setTextColor(Color.BLACK);
        }
        thumb_image.setImageLevel(imageLevel);
        return vi;
    }
}
