package com.closedcircles.client.adapters;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.closedcircles.client.R;
import com.closedcircles.client.model.*;
import com.closedcircles.client.activities.*;

import java.util.List;

public class AdapterCircles extends ArrayAdapter<Circle> {
    private Typeface typeface;
    public AdapterCircles(Context context, Typeface tf, List<Circle> circles) {
        super(context, android.R.layout.simple_list_item_activated_1, android.R.id.text1, circles );
        this.typeface = tf;
    }
    public AdapterCircles(Context context, Account account, List<Circle> circles) {
        super(context, android.R.layout.simple_list_item_activated_1, android.R.id.text1, circles);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView textView = (TextView) view.findViewById(android.R.id.text1);
        textView.setTypeface(typeface);
        textView.setTextColor(getContext().getResources().getColor(android.R.color.black));
        if ( getItem(position).isUnreadExists() )
            textView.setTextAppearance(getContext(), R.style.boldItalicText);
        return view;
    }
}
