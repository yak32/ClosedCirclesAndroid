package com.closedcircles.client.activities;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;


import com.closedcircles.client.R;
import com.closedcircles.client.WebConnectionManager;
import com.closedcircles.client.model.*;
import com.closedcircles.client.adapters.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class FragmentThreads extends android.support.v4.app.Fragment {
    private Typeface        msgTypeface = null;
    private Typeface        authourTypeface = null;
    private AdapterThreads  mThreadsAdapter = null;
    ListView mViewMessages;

    public static final FragmentThreads newInstance() {
        FragmentThreads f = new FragmentThreads();
        return f;
    }

    public FragmentThreads() {
        super();

    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_threads, container, false);

        mThreadsAdapter = new AdapterThreads(getActivity(),  msgTypeface, authourTypeface);
        mViewMessages =  (ListView)view.findViewById(R.id.thread_list);
        mViewMessages.setAdapter(mThreadsAdapter);
        authourTypeface = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-LightItalic.ttf");
        msgTypeface = Typeface.createFromAsset(getActivity().getAssets(), "Messages.ttf");
        updateList();

        mViewMessages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                CirclesActivity a = (CirclesActivity)getActivity();
                Circle c = WebConnectionManager.get().account().getSelectedCircle();
                if ( c != null ){
                    WebConnectionManager.get().account().setSelectedThreadId( mThreadsAdapter.getItem(position).thread_id );
                    WebConnectionManager.get().account().setSelectedMsgId( mThreadsAdapter.getItem(position).msg_id );
                    FragmentMessages fragmentMessages = (FragmentMessages)
                            a.getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":" + CirclesActivity.FRAGMENT_MESSAGES);
                    if ( fragmentMessages != null ) {
                        fragmentMessages.updateList(false, true);
                    }
                    mViewMessages.setItemChecked(position, true);
                    a.getPager().setCurrentItem(2, true);
                }
            }
        });
        return view;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    class ThreadComparator implements Comparator<Circle.MsgThread> {
        protected Map<Long, Circle.Thread> mThreads = null;
        protected Circle mCircle = null;

        public ThreadComparator(Map<Long, Circle.Thread> threads, Circle circle){
            super();
            mThreads = threads;
            mCircle = circle;
        }
        @Override
        public int compare(Circle.MsgThread a, Circle.MsgThread b) {
            Circle.Thread t1 = mThreads.get(a.thread_id);
            Circle.Thread t2 = mThreads.get(b.thread_id);
            long id1 = mCircle.getMessage(t1.getLast()).getId();
            long id2 = mCircle.getMessage(t2.getLast()).getId();
            return (int)(id2-id1);
        };
    };
    public void updateList(){
        Account account = WebConnectionManager.get().account();
        Circle circle = account.getSelectedCircle();
        if (  mThreadsAdapter == null || circle == null ) return;
        mViewMessages.clearChoices();
        // fill tread array to display in the list
        CirclesActivity activity = (CirclesActivity)getActivity();

        ArrayList<Circle.MsgThread> items = new ArrayList<Circle.MsgThread>();
        if ( circle == null ) return;
        Map<Long, Circle.Thread> threads = circle.getThreads();
        for (Map.Entry<Long, Circle.Thread> entry : threads.entrySet()){
            Circle.Thread thread = entry.getValue();
            Message msg = circle.getMessage(thread.get(0));

            Circle.MsgThread m = new Circle.MsgThread();
            m.msg = msg.getMessage();
            m.authour = msg.getName();
            m.thread_id = thread.getId();
            m.msg_id = msg.getId();
            m.date = circle.getMessage(thread.getLast()).getDate();  // set date of last message in the thread
            if (thread.hasUnread() )
                m.unreadMsg = "Unread: " + Long.toString(thread.getUnreadCount());
            else
                m.unreadMsg = "Messages: " + thread.size();
            items.add( m );
        }
        Collections.sort(items, new ThreadComparator(threads, circle));
        mThreadsAdapter.clear();
        mThreadsAdapter.addAll(items);
        for ( int i=0;i<items.size();++i )
            if ( items.get(i).thread_id == account.getSelectedThreadId() ) {
                mViewMessages.setItemChecked(i, true); // restore item selection
                break;
            }

    }
}


