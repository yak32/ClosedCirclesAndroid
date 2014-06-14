package com.closedcircles.client.adapters;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.closedcircles.client.R;

import com.closedcircles.client.WebConnectionManager;
import com.closedcircles.client.activities.CirclesActivity;
import com.closedcircles.client.activities.FragmentMessages;
import com.closedcircles.client.activities.LoginActivity;
import com.closedcircles.client.activities.UserInfoActivity;
import com.closedcircles.client.model.Account;
import com.closedcircles.client.model.Circle;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class AdapterMessages extends ArrayAdapter<Circle.MsgThread> {
    private final Typeface  msgTypeface;
    private final Typeface  authorTypeface;
    private final FragmentMessages mFragmentMessages;

    private static final long MSECONDS_ONE_YEAR_DIFF = 60*60*24*365;
    private static final long MSECONDS_ONE_WEEK_DIFF = 60*60*24*7;
    private static final long MSECONDS_ONE_HOUR_DIFF = 60*60;
    private static final long MSECONDS_ONE_MINUTE_DIFF = 60;

    static class ViewHolder {
        public TextView textMsgs;
        public TextView textAuthors;
    }

    public AdapterMessages(FragmentMessages fragmentMessages,
                           Typeface msgTypeface,
                           Typeface authorTypeface) {

        super(fragmentMessages.getActivity(), R.layout.list_item_message);
        this.msgTypeface = msgTypeface;
        this.authorTypeface = authorTypeface;
        this.mFragmentMessages = fragmentMessages;
    }
    public class ItemOnClickListener implements View.OnClickListener{
        private int mPosition;
        public ItemOnClickListener(int position){
            mPosition = position;
        }
        @Override
        public void onClick(View v){
            mFragmentMessages.onMessageClicked((View)v.getParent(), mPosition);
        }
    }
    public class ItemOnLongClickListener implements View.OnLongClickListener{
        private int mPosition;
        public ItemOnLongClickListener (int position){
            mPosition = position;
        }
        @Override
        public boolean onLongClick(View v){
            return false;
        }
    }
    /*public class itemOnAttachStateChangeListener implements View.OnAttachStateChangeListener {
        private long mTimeCreated = 0;
        private int mPosition = -1;
        private long mThreadId = -1;
        public itemOnAttachStateChangeListener (int position, long thread_id){
            mPosition = position;
            mThreadId = thread_id;
        }
        public void onViewAttachedToWindow(android.view.View view){
            mTimeCreated = System.currentTimeMillis()/1000;
        }
        public void onViewDetachedFromWindow(android.view.View view){
            mFragmentMessages.onItemDetached(mPosition, mThreadId, System.currentTimeMillis()/1000-mTimeCreated);
        }
    }*/
    public static String convertDate(String strDate){
        String result = new String();
        try {
            Calendar today = Calendar.getInstance(TimeZone.getTimeZone("GMT"));// today in GMT time
            TimeZone gmt = TimeZone.getTimeZone("GMT");

            SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd kk:mm:ss yyyy", Locale.US);
            format.setTimeZone(gmt);
            Date date = format.parse(strDate); // parse date in GMT time

            Calendar thatDay = Calendar.getInstance(gmt);
            thatDay.setTime(date); // get date in GMT time

            long diff = (today.getTimeInMillis() - thatDay.getTimeInMillis())/1000;  // difference in seconds
            if ( diff < MSECONDS_ONE_MINUTE_DIFF )
                result = "Now"; // less than minute
            else if ( diff < MSECONDS_ONE_HOUR_DIFF ) // less than hour
                result = Long.toString(diff/60)+" mins"; // minutes ago
            else if ( diff < MSECONDS_ONE_WEEK_DIFF){ // less than 1 week
                SimpleDateFormat outFormat = new SimpleDateFormat("EEE kk:mm");
                result = outFormat.format(thatDay.getTime());
            }
            else if ( diff < MSECONDS_ONE_YEAR_DIFF){ //  more than 1 week and less than 1 year
                SimpleDateFormat outFormat = new SimpleDateFormat("MMM dd, kk:mm");
                result = outFormat.format(thatDay.getTime());
            }
            else {  // more than 1 year
                SimpleDateFormat outFormat = new SimpleDateFormat("yyyy MMM dd, kk:mm");
                result = outFormat.format(thatDay.getTime());
            }
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(WebConnectionManager.get().getContext(), e.toString(), Toast.LENGTH_LONG).show();
            result = strDate;
        }
        return result;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mFragmentMessages.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView;
        Circle.MsgThread t = getItem(position);
        //if ( position == mFragmentMessages.getSelectedPosition() ) {
        //    rowView = inflater.inflate(R.layout.list_item_message_selected, parent, false);
        //}
        //else {
            rowView = inflater.inflate(R.layout.list_item_message, parent, false);
        //}

        if ( t.isRead ) {
            rowView.setBackgroundResource(R.drawable.bg_key_unread);
        }

        TextView authorView = (TextView) rowView.findViewById(R.id.id_msgAuthor);
        authorView.setText(t.authour);
        authorView.setOnClickListener(new ItemOnClickListener(position));
        authorView.setOnLongClickListener(new ItemOnLongClickListener(position));

            /*textView.setLinksClickable(true);
            textView.setAutoLinkMask(Linkify.WEB_URLS);
            String text = "com.package.name://" + t.authour;
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setText(text);*/

        TextView textView = (TextView) rowView.findViewById(R.id.id_msgDate);
        textView.setText(convertDate(t.date));
        textView.setOnClickListener(new ItemOnClickListener(position));
        textView.setOnLongClickListener(new ItemOnLongClickListener(position));

        textView = (TextView) rowView.findViewById(R.id.id_msgText);
        textView.setText(t.msg);
        textView.setOnClickListener(new ItemOnClickListener(position));
        textView.setOnLongClickListener(new ItemOnLongClickListener(position));

        /*Button authorButton = (Button) rowView.findViewById(R.id.id_msgAuthorButton);
        authorView.setOnClickListener(new AuthorOnClickListener(position));
        authorView.setOnLongClickListener(new ItemOnLongClickListener(position));*/

        ((ViewGroup)rowView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        return rowView;
    }
}


