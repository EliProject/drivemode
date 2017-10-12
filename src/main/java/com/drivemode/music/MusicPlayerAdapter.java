package com.drivemode.music;

/**
 * Created by liyuanqin on 17-9-7.
 */

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.eli.drivemodedemo.R;

import java.util.List;

public class MusicPlayerAdapter extends BaseAdapter {

    private Context context;

    private List<MusicPlayService.MusicPlayers> list;

    private int mSelectedItem;

    public MusicPlayerAdapter(Context context, List<MusicPlayService.MusicPlayers> list, int selectItem) {

        this.context = context;
        this.list = list;
        mSelectedItem = selectItem;

    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        Log.i("@@@@_getVew","position="+position+" mSelectedItem="+mSelectedItem);
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.music_player_item, null);
            holder = new ViewHolder();

            convertView.setTag(holder);

            holder.title = (TextView) convertView.findViewById(R.id.music_player_title);
            holder.icon = (ImageView) convertView.findViewById(R.id.music_player_icon);
            holder.selectIcon = (ImageView) convertView.findViewById(R.id.selected_icon);

        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.title.setTextColor(Color.BLACK);
        holder.title.setText(list.get(position).title);
        holder.icon.setImageDrawable(list.get(position).icon);

        if (mSelectedItem == position) {
            holder.selectIcon.setImageDrawable(context.getDrawable(R.drawable.tick));
            holder.selectIcon.setVisibility(View.VISIBLE);
        } else {
            holder.selectIcon.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
        ImageView selectIcon;
    }

}
