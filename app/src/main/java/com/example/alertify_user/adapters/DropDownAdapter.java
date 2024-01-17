package com.example.alertify_user.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.example.alertify_user.R;

import java.util.ArrayList;

public class DropDownAdapter extends ArrayAdapter<String> {
    private ArrayList<String> crimesList;

    public DropDownAdapter(Context context, ArrayList<String> crimesList) {
        super(context, R.layout.drop_down_item);
        this.crimesList = crimesList;
    }

    @Override
    public int getCount() {
        return crimesList.size();
    }

    @Override
    public String getItem(int position) {
        return crimesList.get(position);
    }
}
