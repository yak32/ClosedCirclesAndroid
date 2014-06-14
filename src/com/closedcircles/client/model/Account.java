package com.closedcircles.client.model;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.Toast;

import com.closedcircles.client.WebConnectionManager;

public class Account {
	private long mNotification = -1;
    private String mVersion;
	private final List<Circle> mCircles = new ArrayList<Circle>();
    private long  mSelectedCircleId = -1;
    private long  mSelectedThreadId = -1;
    private long  mSelectedMsgId = -1;
    private long  mNewMsgId = -1;
    private boolean mClosedMode = true;
    private long mId = -1;

	public Account() {
	}

    public List<Circle> getCircles() { return mCircles; }
	public Circle getSelectedCircle(){
        if ( mSelectedCircleId == -1 ) return null;
        return getCircleForId(mSelectedCircleId);
    }

    public void setSelectedCircle(Circle circle) {

        mSelectedCircleId = circle.getId();
    }
    public Long getNewMsgId() { return mNewMsgId; }
    public void setNewMsgId(long msg_id ) {
        mNewMsgId = msg_id;
    }
    public Long getSelectedMsgId() { return mSelectedMsgId; }
    public void setSelectedMsgId(long msg_id ) { mSelectedMsgId = msg_id; }
    public long getSelectedThreadId() {
        return mSelectedThreadId;
    }
    public void setClosedMode(boolean b){
        mClosedMode = b;
    }
    public boolean getClosedMode() { return mClosedMode; }
    public void setUserId(long id){ mId = id; }
    public long getUserId() { return mId; }
    public Circle.Thread getSelectedThread() {
        if ( mSelectedCircleId == -1 || mSelectedThreadId == -1 ) return null;
        return getSelectedCircle().getThreads().get(mSelectedThreadId);
    }
    public void setSelectedThreadId(long thread_id) { mSelectedThreadId = thread_id; }

	public int size() {
		return mCircles.size();
	}

    public void setNotification(long notification) {

        mNotification = notification;
    }

    public long getNotification() {
		return mNotification;
	}

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getVersion() {
        return mVersion;
    }

	public Circle getCircle(int position) {
		return mCircles.get(position);
	}
	
	public void add(Circle circle) {
		mCircles.add(circle);
	}
	
	public void clear() {
		mCircles.clear();
		mNotification = -1;
	}
	
	public Circle getCircleForId(long id) {
		for (int i = 0; i < mCircles.size(); ++i) {
			if (mCircles.get(i).getId() == id) {
				return mCircles.get(i);
			}
		}
		return null;
	}
	
	public int getPositionForId(long id) {
		for (int i = 0; i < mCircles.size(); ++i) {
			if (mCircles.get(i).getId() == id) {
				return i;
			}
		}
		return -1;		
	}

	private static final String KEY_NOTIFICATION = "notification";
    private static final String KEY_USERID = "userid";
	private static final String KEY_CIRCLES_SIZE = "circles_size";
	private static final String KEY_CIRCLES_PREFIX = "circle-";
    private static final String KEY_SELECTED_CIRCLE = "selected_circle_id";
    private static final String KEY_SELECTED_THREAD = "selected_thread_id";
    private static final String KEY_XSRF = "xsrf";
    private static final String KEY_VERSION = "version";
    private static final String KEY_CLOSED_MODE = "closed_mode";

	public void saveState(Bundle outState) {
        //String xsrf = WebConnectionManager.get().XSRF();
        //Toast.makeText(WebConnectionManager.get().getContext(), xsrf.isEmpty() ? "Save state:xsrf - empty": "Save state, xsrf" + xsrf, Toast.LENGTH_LONG).show();
        //if ( mCircles.isEmpty() ) return; // skip saving, if nothing to save
		outState.putLong(KEY_NOTIFICATION, mNotification);
        outState.putLong(KEY_USERID, mId);
		outState.putInt(KEY_CIRCLES_SIZE, mCircles.size());
		for (int i = 0; i < mCircles.size(); ++i) {
			Bundle circle = new Bundle();
			mCircles.get(i).saveState(circle);
			outState.putBundle(KEY_CIRCLES_PREFIX + i, circle);
		}
        outState.putLong(KEY_SELECTED_CIRCLE, mSelectedCircleId );
        outState.putLong(KEY_SELECTED_THREAD, mSelectedThreadId );
        outState.putString(KEY_XSRF, WebConnectionManager.get().XSRF());
        outState.putString(KEY_VERSION, mVersion);
        //outState.putBoolean(KEY_CLOSED_MODE, mClosedMode);
	}

	public void restoreState(Bundle inState) {
        clear();
		mNotification = inState.getLong(KEY_NOTIFICATION);
        mId = inState.getLong(KEY_USERID);
		int sz = inState.getInt(KEY_CIRCLES_SIZE);
		for (int i = 0; i < sz; ++i) {
			Circle circle = new Circle(inState.getBundle(KEY_CIRCLES_PREFIX + i), mId);
			mCircles.add(circle);
		}
        mSelectedCircleId = inState.getLong(KEY_SELECTED_CIRCLE);
        mSelectedThreadId = inState.getLong(KEY_SELECTED_THREAD);
        String xsrf =  inState.getString(KEY_XSRF);
        //Toast.makeText(WebConnectionManager.get().getContext(), xsrf == null? "Restore state - xsrf - null": "Restore state, xsrf" + xsrf, Toast.LENGTH_LONG).show();
        WebConnectionManager.get().setXSRF( xsrf == null? new String(): xsrf );


        mVersion = inState.getString(KEY_VERSION);
        //mClosedMode = inState.getBoolean(KEY_CLOSED_MODE);
	}

}
