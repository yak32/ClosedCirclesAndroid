package com.closedcircles.client.model;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.widget.Toast;

import com.closedcircles.client.R;
import com.closedcircles.client.WebConnectionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Message {
	public static final int TYPE_TEXT = 0;
	public static final int TYPE_FRIEND_OUTGOING = 1;
	public static final int TYPE_FRIEND_INCOMING = 2;
	public static final int TYPE_FRIEND_ACCEPTED = 3;
	public static final int TYPE_FRIEND_DECLINED = 4;
	public static final int TYPE_FRIEND_CANCELED = 5;
	public static final int TYPE_FRIEND_READ = 6;
	public static final int TYPE_THREAD_READ = 9;
    public static final int OPENREPLAY =4;
	
	protected final long mId;
    protected final String mName;
    protected final String mFullName;
    protected final long mThread;
    protected final long mParent;
    protected final String mParentName;
    protected final String mDate;
    protected boolean mRead;
    protected long mFlags;
    protected long mUserId;
    protected long mLocalData;
    protected String mMessage;

    Message(long id,
            String name,
            String fullname,
            String message,
            long thread,
            long parent,
            String parentName,
            String date,
            long flags,
            long userid,
            long localData) {
    	mId = id;
        mName = name;
        mFullName = fullname;
        mMessage = message;
        mThread = thread;
        mParent = parent;
        mParentName = parentName;
        mDate = date;
        mRead = false;
        mFlags = flags;
        mUserId = userid;
        mLocalData = localData;
    }

    public String getName() { return mName;	}
    public String getFullName() { return mFullName;	}
    public String getParentName() {
        return mParentName;
    }
    public String getDate() { return mDate; }
    public long   getFlags() { return mFlags;}
    public long   getUserId() { return mUserId;}
	public String getMessage() {
		return mMessage;
	}
	public long getId() {
		return mId;
	}
	public long getThread() {
		return mThread;
	}
	public long getParent() {
		return mParent;
	}
    public long getLocalData() { return mLocalData;}
    public void setText(String str){mMessage=str;}

	@Override
    public String toString() {return mName + ": " + mMessage;}
	public void doRead() {mRead = true;}
	public boolean isRead() {
		return mRead;
	}
	public static Message fromJson(JSONObject source) throws JSONException {
		int thread = -1;
		int parent = -1;
		if (source.getInt("type") == Message.TYPE_TEXT) {
			if (!source.isNull("parentId")) {
				parent = source.getInt("parentId");
			}
			if (!source.isNull("threadId")) {
				thread = source.getInt("threadId");
			}
		}
		return new Message(source.getLong("id"),
                           source.getString("name"),
				           source.getString("fullname"),
				           source.getString("text"),
				           thread,
				           parent,
                           source.getString("parentName"),
                           source.getString("time"),
                           source.getLong("flags"),
                           source.getLong("userid"),
                           //source.getLong("localData"));
                           0); // skip local data currently
	}
	
	private final static String KEY_ID = "id";
	private final static String KEY_NAME = "name";
    private final static String KEY_FULL_NAME = "fullname";
	private final static String KEY_MESSAGE = "message";
	private final static String KEY_THREAD = "thread";
	private final static String KEY_PARENT = "parent";
    private final static String KEY_PARENT_NAME = "parentName";
    private final static String KEY_DATE = "date";
    private final static String KEY_FLAGS = "flags";
    private final static String KEY_USERID = "userid";
    private final static String KEY_LOCAL_DATA = "localData";
	
	public void saveState(Bundle outState) {
		outState.putLong(KEY_ID, mId);
		outState.putString(KEY_NAME, mName);
        outState.putString(KEY_FULL_NAME, mFullName);
		outState.putString(KEY_MESSAGE, mMessage);
		outState.putLong(KEY_THREAD, mThread);
		outState.putLong(KEY_PARENT, mParent);
        outState.putString(KEY_PARENT_NAME, mParentName);
        outState.putString(KEY_DATE, mDate);
        outState.putLong(KEY_FLAGS, mFlags);
        outState.putLong(KEY_USERID, mUserId);
        outState.putLong(KEY_LOCAL_DATA, mLocalData);
	}
	
	public Message(Bundle inState) {
		mId = inState.getLong(KEY_ID);
		mName = inState.getString(KEY_NAME);
        mFullName = inState.getString(KEY_FULL_NAME);
		mMessage = inState.getString(KEY_MESSAGE);
		mThread = inState.getLong(KEY_THREAD);
		mParent = inState.getLong(KEY_PARENT);
        mParentName = inState.getString(KEY_PARENT_NAME);
        mDate = inState.getString(KEY_DATE);
        mFlags = inState.getLong(KEY_FLAGS);
        mUserId = inState.getLong(KEY_USERID);
        mLocalData = inState.getLong(KEY_LOCAL_DATA);
	}
}

