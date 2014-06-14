package com.closedcircles.client.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import android.os.Bundle;
import android.util.Log;

import com.closedcircles.client.WebConnectionManager;

public class Circle {
	private final long mId;
	private final String mName;
	private final ArrayList<Message> mMessages = new ArrayList<Message>();
    HashMap<Long, Integer> mMsgMap = new HashMap<Long, Integer>();
    private final Map<Long, Thread> mThreads = new HashMap<Long, Thread>();
    private long mCursor = -1;

	public Circle(long id, String name) {
		mId = id;
		mName = name;
	}

    public Map<Long, Thread> getThreads() { return mThreads; }

	public long getCursor() {
		return mCursor;
	}

	public void setCursor(long cursor) {
		mCursor = cursor;
	}

	public long getId() {
		return mId;
	}


	public String getName() {
		return mName;
	}
	
	@Override
	public String toString() {
		//return Long.toString(mId) + ": " + mName;
        return "#" + mName;
	}
	
	public void clearAll() {
		mThreads.clear();
		mMessages.clear();
        mMsgMap.clear();
		mCursor = -1;
	}
	
	public int getNumMessages() {
		return mMessages.size();
	}
	
	public Message getMessage(int position) {
		return mMessages.get(position);
	}
    public boolean isUnreadExists(){
        for (Circle.Thread t : mThreads.values()) {
            if ( t.hasUnread() )
                return true;
        }
        return false;
    }
	public void add(Message message) {
        if ( mMsgMap.containsKey( Long.valueOf(message.getId())) ) {
            mMessages.set( mMsgMap.get( Long.valueOf(message.getId())), message); // replace prev message
        }
        else {
            mMessages.add(message);
            mMsgMap.put( Long.valueOf(message.getId()), mMessages.size() - 1);
            long tid = message.getThread();
            Thread thread = mThreads.get(tid);
            if (thread == null) {
                thread = new Thread(tid);
                mThreads.put(tid, thread);
            }
            thread.add(mMessages.size() - 1);
        }
	}
    public void removeMessages(TreeSet<Integer> positions){
        int add = 0;
        for (Integer i: positions) {
            mMessages.remove(i + add);
            add--;
        }
    }
    public void deleteMessage(long msg_id){
        if ( mMsgMap.containsKey( Long.valueOf(msg_id)) ) {
            Message msg = mMessages.get(mMsgMap.get( Long.valueOf(msg_id)));
            msg.setText("");
            mMessages.set( mMsgMap.get(Long.valueOf(msg_id)), msg); // replace prev message
        }
    }
	
	public void setMarkers(Map<Long, Long> markers, long userId) {
		for (Entry<Long, Long> entry: markers.entrySet()) {
			if (mThreads.containsKey(entry.getKey())) {
				mThreads.get(entry.getKey()).setUnread(entry.getValue(), userId);
			} else {
				Log.w(getClass().getName(), "Unknown thread " + entry.getKey());
			}
		}
	}
	
	public void addAll(Collection<? extends Message> messages) {
		for (Message message: messages) {
			add(message);
		}
        sortThreads();
        removeOldThreads();
	}
    private void sortThreads(){
        for (Entry<Long, Thread> entry: mThreads.entrySet()) {
            Thread thread = entry.getValue();
            thread.sortMessages();
        }
    }
    // updates are downloaded continuously, so old messages should be removed
    private void removeOldThreads(){
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (Entry<Long, Thread> entry: mThreads.entrySet()) {
            threads.add(entry.getValue());
        }
        Collections.sort(threads, new Comparator<Thread>() {
            @Override
            public int compare(Thread t1, Thread t2) {
                long id1 = Circle.this.getMessage(t1.getLast()).getId();
                long id2 = Circle.this.getMessage(t2.getLast()).getId();
                return (int)(id2-id1);
            }
        });
        boolean wasRemoved = false;
        TreeSet<Integer> to_remove = new TreeSet<Integer>();
        for ( int i=WebConnectionManager.HISTORY_LENGTH;i<threads.size();++i){
            Thread t = threads.get(i);
            for ( int j=0;j<t.size();++j){
                to_remove.add(t.get(j));
                wasRemoved = true;
            }
        }
        Circle.this.removeMessages(to_remove);
        // refill threads one more time
        // TODO: think how to avoid second pass
        if ( wasRemoved ) {
            Map<Long, Thread> mapThreads = new HashMap<Long, Thread>();
            mapThreads.putAll(mThreads);
            mThreads.clear();
            mMsgMap.clear();
            ArrayList<Message> messages = new ArrayList<Message>();
            messages.addAll(mMessages);
            mMessages.clear();
            for (int i = 0; i < messages.size(); ++i)
                Circle.this.add(messages.get(i));
            // apply read markers
            for ( Entry<Long, Thread> e: mapThreads.entrySet() ){
                int firstUnread = e.getValue().getFirstUread();
                if( mThreads.get(e.getKey()) != null )
                    mThreads.get(e.getKey()).setFirstUnread(firstUnread);
            }
        }
    }
	public Map<Long, Long> lastMessages() {
		Map<Long, Long> result = new HashMap<Long, Long>();
        for (Entry<Long, Thread> entry: mThreads.entrySet()) {
            Thread thread = entry.getValue();
            if (thread.hasUnread()) {
                result.put(thread.getId(), mMessages.get(thread.getLast()).getId());
            }
        }
		return result;
	}
	
	public int getFirstUnreadPosition() {
		for (int i = 0; i < mMessages.size(); ++i) {
			if (!mMessages.get(i).isRead()) {
				return i;
			}
		}
		return mMessages.size() - 1;
	}
	
	private static final String KEY_ID = "circle_id";
	private static final String KEY_THREAD = "thread_id";
	private static final String KEY_NAME = "circle_name";
	private static final String KEY_CURSOR = "cursor";
	private static final String KEY_MESSAGES_SIZE = "messages_size";
	private static final String KEY_MESSAGES_PREFIX = "message-";
	private static final String KEY_THREADS_SIZE = "threads_size";
	private static final String KEY_THREAD_ID_PREFIX = "thread_id-";
	private static final String KEY_THREAD_MARKER_PREFIX = "thread_marker-";
	
	public void saveState(Bundle outState) {
		outState.putLong(KEY_ID, mId);
		outState.putString(KEY_NAME, mName);
		outState.putLong(KEY_CURSOR, mCursor);
		outState.putInt(KEY_MESSAGES_SIZE, mMessages.size());
		for (int i = 0; i < mMessages.size(); ++i) {
			Bundle message = new Bundle();
			mMessages.get(i).saveState(message);
			outState.putBundle(KEY_MESSAGES_PREFIX + i, message);
		}
		outState.putInt(KEY_THREADS_SIZE, mThreads.size());
		int i = 0;
		for (Entry<Long, Thread> entry: mThreads.entrySet()) {
			Thread thread = entry.getValue();
			outState.putLong(KEY_THREAD_ID_PREFIX + i, thread.getId());
			outState.putLong(KEY_THREAD_MARKER_PREFIX + i, thread.getMarker());
			i++;
		}
	}
	
	public Circle(Bundle inState, long userId) {
		mId = inState.getLong(KEY_ID);
		mName = inState.getString(KEY_NAME);
		mCursor = inState.getLong(KEY_CURSOR);
		int sz = inState.getInt(KEY_MESSAGES_SIZE);
		for (int i = 0; i < sz; ++i) {
			Message message = new Message(inState.getBundle(KEY_MESSAGES_PREFIX + i));
			add(message);
		}
		long tid = 0;
		int num = inState.getInt(KEY_THREADS_SIZE);
		for (int i = 0; i < num; i++) {
			tid = inState.getLong(KEY_THREAD_ID_PREFIX + i);
			long marker = inState.getLong(KEY_THREAD_MARKER_PREFIX + i);
			if (mThreads.containsKey(tid)) {
				mThreads.get(tid).setUnread(marker, userId);
			}
		}
	}
	
	public class Thread {
		private final long mId;
		private final List<Integer> mMessages = new ArrayList<Integer>();
		private int mFirstUnread = 0;
		private long mMarker = -1;

		public Thread(long id) {
			mId = id;
		}
		
		public long getId() {
			return mId;
		}
		
		public void add(int message) {
			mMessages.add(message);
		}
		
		public int get(int position) {
			return mMessages.get(position);
		}
		
		public int getLast() {
			return mMessages.get(mMessages.size() - 1);
		}
		
		public int size() {
			return mMessages.size();
		}
		public int getUnreadCount(){
            return size()-getFirstUread();
        }
		public void setUnread(long marker, long userId) {
			mMarker = marker;
			while (mFirstUnread < mMessages.size()) {
				Message message = Circle.this.mMessages.get(mMessages.get(mFirstUnread));
				if (message.getId() <= marker) {
					message.doRead();
				} else {
					break;
				}
				mFirstUnread++;
			}
            while (mFirstUnread < mMessages.size()) {
                Message message = Circle.this.mMessages.get(mMessages.get(mFirstUnread));
                if (message.getUserId() == userId ) {
                    message.doRead();
                } else {
                    break;
                }
                mFirstUnread++;
            }
		}
        public void sortMessages(){
            Collections.sort(mMessages, new Comparator<Integer>(){
                @Override
                public int compare(Integer a, Integer b) {
                    Message m1 = Circle.this.getMessage(a);
                    Message m2 = Circle.this.getMessage(b);
                    return (int)(m1.getId()-m2.getId());
                };
            });
        }
		public boolean hasUnread() {
			return getUnreadCount() != 0;
		}
        public int getFirstUread() { return mFirstUnread;}
        public void setFirstUnread(int position) { mFirstUnread = position;}
		public long getMarker() {
			return mMarker;
		}
	}


    static public class MsgThread{
        public String toString() { return msg; }
        public  String authour;
        public  String date;
        public  String msg;
        public  String unreadMsg;
        public  long   thread_id;
        public  long   msg_id;
        public  boolean isRead; // is message read
    }
}
