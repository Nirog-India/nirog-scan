package com.apps.hrgiri.nirogscan;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class CustomAdapter extends BaseAdapter {

    ArrayList<HashMap> list;
    Activity activity;

    public CustomAdapter(ArrayList<HashMap> list, Activity activity){
        super();
        this.list = list;
        this.activity = activity;
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = activity.getLayoutInflater().inflate(R.layout.list_item, viewGroup, false);
        }

        HashMap map = list.get(i);
        ((TextView) view.findViewById(R.id.tv_list_name)).setText((String)map.get(Constants.FIRST_COLUMN));
        ((TextView) view.findViewById(R.id.tv_list_lastread)).setText((String)map.get(Constants.SECOND_COLUMN));
        ((TextView) view.findViewById(R.id.tv_list_readings)).setText((String)map.get(Constants.THIRD_COLUMN));

        return view;
    }
}

