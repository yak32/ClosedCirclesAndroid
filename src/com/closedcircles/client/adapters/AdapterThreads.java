package com.closedcircles.client.adapters;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.closedcircles.client.R;

import com.closedcircles.client.model.Circle;

public class AdapterThreads extends ArrayAdapter<Circle.MsgThread> {
    private final Context   mContext;
    private final Typeface  msgTypeface;
    private final Typeface  authorTypeface;

    public AdapterThreads( Context context,
                           Typeface msgTypeface,
                           Typeface authorTypeface) {
        super(context, R.layout.list_item_threads);
        this.mContext = context;
        this.msgTypeface = msgTypeface;
        this.authorTypeface = authorTypeface;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View rowView = inflater.inflate(R.layout.list_item_threads, parent, false);

        TextView textMsgs = (TextView) rowView.findViewById(R.id.secondLine);
        textMsgs.setTypeface(msgTypeface);
        TextView textDate = (TextView) rowView.findViewById(R.id.msgDate);
        textDate.setTypeface(msgTypeface);
        TextView  textUnread = (TextView) rowView.findViewById(R.id.unreadCount);
        textUnread.setTypeface(msgTypeface);
        TextView  textAuthors = (TextView) rowView.findViewById(R.id.firstLine);
        textAuthors.setTypeface(authorTypeface);
        ImageView image = (ImageView) rowView.findViewById(R.id.icon);
        View viewLinear = rowView.findViewById(R.id.viewLinear);

        Circle.MsgThread t = getItem(position);
        textMsgs.setText( t.msg );
        textAuthors.setText( t.authour );
        textDate.setText(AdapterMessages.convertDate(t.date));
        textUnread.setText( t.unreadMsg );
        if ( t.unreadMsg.substring(0, 6).compareTo("Unread") == 0 )
            textUnread.setTextAppearance(mContext, R.style.boldText);
        else {
            textUnread.setTextAppearance(mContext, R.style.normalText);
            /*rowView.setBackgroundResource(R.drawable.bg_key_unread);
            textMsgs.setBackgroundResource(R.drawable.bg_key_unread);
            textAuthors.setBackgroundResource(R.drawable.bg_key_unread);
            textUnread.setBackgroundResource(R.drawable.bg_key_unread);
            viewLinear.setBackgroundResource(R.drawable.bg_key_unread);*/
        }

       return rowView;
    }
}
