package com.example.bmd_filemanager;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ListAdapter extends BaseAdapter {

    private List<String> data = new ArrayList<>();
    private boolean[] selection;

    public void setSelection(boolean[] selection) {
        if(selection != null) {
            this.selection = new boolean[selection.length];
            this.selection = selection;
        }
        notifyDataSetChanged();
    }

    public void setData(List<String> data) {
        if(data != null) {
            this.data.clear();
            if(data.size() > 0) {
                this.data.addAll(data);
            }
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public String getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null) {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
            view.setTag(new ViewHolder((TextView) view.findViewById(R.id.item_text_view)));
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        String item = getItem(i);
        holder.info.setText(item.substring(item.lastIndexOf("/") + 1));

        if(selection != null) {
            if(selection[i]) {
                holder.info.setBackgroundColor(Color.GRAY);
            } else {
                holder.info.setBackgroundColor(Color.WHITE);
            }
        }

        return view;
    }

    class ViewHolder {
        private TextView info;

        public ViewHolder(TextView info) {
            this.info = info;
        }
    }
}
