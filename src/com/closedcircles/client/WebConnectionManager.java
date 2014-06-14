package com.closedcircles.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


import com.closedcircles.client.model.*;
import com.closedcircles.client.activities.*;
import com.closedcircles.client.adapters.*;

import com.loopj.android.http.*;

// singleton
public class WebConnectionManager {

    private static WebConnectionManager mInstance = null;
    private Context mContext = null;

    private CirclesActivity mActivity = null;
    private Account mAccount = new Account();
    private String mXSRF = new String();
    private BroadcastReceiver mReceiver = null;
    private int mFailedUpdateCount = 0;
    private long mLocalData = (long) (Math.random() * 1000) + 1;

    private Handler mHandler = new Handler();
    private Runnable mTimer = null;
    private int mReloadCount = 0;

    public final static int HISTORY_LENGTH = 10;
    private ProgressDialog mProgressDialog = null;

    private boolean mPause = true;
    public final static int MESSAGE_MAKE_READ_TIME = 1; // 1 second to read messages
    public final static int RESTART_UPDATES_TIME = 60; // 60 sec
    public final static int MAX_RELOAD_TOKEN_COUNT = 2;

    protected WebConnectionManager() {
    }

    public static WebConnectionManager get() {
        if (mInstance == null)
            mInstance = new WebConnectionManager();
        return mInstance;
    }

    public Account account() {
        return mAccount;
    }
    public Activity activity() { return mActivity; }

    public void setup(CirclesActivity activity, Bundle saveState) {
        mActivity = activity;
        if (mContext == null) { // if first time call
            mContext = activity.getApplicationContext();
            WebConnection.create(mContext);
            if (mReceiver == null)
                mReceiver = new ConnectivityChangeReceiver(mContext);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mReceiver, filter);
        } else
            mContext = activity.getApplicationContext();

        //String str = saveState != null?"setup: saveState is not null": "setup: saveState is null";
        //str += mAccount.getCircles().isEmpty() ? "circles are empty": "circles are not empty";
        //Toast.makeText(mContext, str, Toast.LENGTH_LONG).show();

        if (saveState != null )
            mAccount.restoreState(saveState);
    }
    public String XSRF(){ return mXSRF; }
    public void   setXSRF(String s){ mXSRF = s; }
    public void onCirclesActivityClosed() {
        mActivity = null;
        doStop();
    }

    public boolean checkConnection() {
        ConnectivityManager conMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMgr == null) return false;
        NetworkInfo i = conMgr.getActiveNetworkInfo();
        return i != null && i.isConnectedOrConnecting();
    }
    public Context getContext() { return mContext; }
    public void reloadToken() {
        if (mContext == null ) return;
        boolean signOut = false;
        if ( mReloadCount >= MAX_RELOAD_TOKEN_COUNT ) {
            signOut = true;
            mReloadCount = 0;
        }
        else
            mReloadCount ++;
        //Toast.makeText(mContext, "reload Token", Toast.LENGTH_LONG).show();
        mFailedUpdateCount = 0;
        Log.w(getClass().getName(), "reloadToken...");
        if (checkConnection() && !mPause ) {  // reload token only if we have connection
            if (mActivity != null) {
                mActivity.startLogin(signOut);
            }
        } else
            onConnectionLost();
    }

    private void onConnectionLost() {
        if (mContext == null) return;
        mPause = true; // stop updates
        mFailedUpdateCount = 0;
    }

    public void doAuth(String token, String authType) {
        Log.w(getClass().getName(), "Doing auth with token...");

        mAccount.clear();
        mXSRF = "";

        String authPath;
        RequestParams params;
        if (authType.equals(LoginActivity.AUTH_TYPE_GOOGLE)) {
            params = new RequestParams(WebConnection.PARAM_GOOGLE_ACCESS_TOKEN, token);
            params.put("token_type", "Bearer");
            params.put("expires_in", "3600");
            params.put("state", "googleauthnext%3D%252Fchat");
            authPath = WebConnection.PATH_AUTH_GOOGLE;
        } else {
            params = new RequestParams(WebConnection.PARAM_ACCESS_TOKEN, token);
            authPath = WebConnection.PATH_AUTH;
        }

        WebConnection.get(authPath, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, org.apache.http.Header[] headers, byte[] responseBody) {
                authComplete();
            }

            @Override
            public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, java.lang.Throwable error) {
                Toast.makeText(mContext, "doAuth failed", Toast.LENGTH_LONG).show();
                Log.e(getClass().getName(), "OnFailure!", error);
                reloadToken();
            }
        });
    }

    private void authComplete() {
        Log.w(getClass().getName(), "Auth complete...");

        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setMessage("Joining...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        mXSRF = WebConnection.cookie("_xsrf");
        RequestParams params = new RequestParams();
        params.put(WebConnection.PARAM_XSRF_TOKEN, mXSRF);
        params.put(WebConnection.PARAM_HISTORY_LENGTH, Integer.toString(HISTORY_LENGTH));
        WebConnection.post(WebConnection.PATH_JOINROOM, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject obj) {
                if( mProgressDialog != null && mProgressDialog.isShowing() )
                    mProgressDialog.dismiss();
                joinComplete(obj);
            }

            @Override
            public void onFailure(int statusCode, org.apache.http.Header[] headers, java.lang.String responseBody, java.lang.Throwable e) {
                if( mProgressDialog != null && mProgressDialog.isShowing() )
                    mProgressDialog.dismiss();
                Log.e(getClass().getName(), "Join failed!", e);
                Toast.makeText(mContext, "join failed", Toast.LENGTH_LONG).show();
                reloadToken();
            }
        });
    }

    private void joinComplete(JSONObject initial) {
        if (mAccount.getNotification() == -1) {
            // circles : [{circleId, name}]
            // name
            // usersettings
            // userid
            // notificationCursor <-- Global messages, outside of circles
            // currentCircle
            // - name
            //   messages : [text, parentName, cookie, id, vislist: [], name,
            //               parentUserId, parentId, userId, flags, time, type]
            //   markers : [{"marker": 225625, "thread": 6794}, ...]
            //   cursor
            //   requests : []
            //   friends : [{"id":0,"profileLink":"http://www.facebook.com/sim0nsays","website":"http://sim0nsays.livejournal.com","status":0,"fullname":"Simon Kozlov","idFB":591568993,"name":"Simon"}]
            //   id
            //   serverVersion
            // pending
            long initial_circle = -1;
            try {
                mReloadCount = 0;  // successful login - we can clear reload count

                JSONObject currentCircle = initial.getJSONObject("currentCircle");
                initial_circle = currentCircle.getLong("id");
                postInitialCircles(initial.getJSONArray("circles"), initial.getLong("notificationCursor"), initial.getString(WebConnection.PARAM_SERVER_VERSION));
                postMessages(initial_circle, currentCircle.getLong("cursor"), currentCircle.getJSONArray("messages"));
                postUserInfo(initial.getJSONObject("userInfo"));
                postMarkers(initial_circle, currentCircle.getJSONArray("markers"));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(getClass().getName(), initial.toString());
            }

            // Get other circles.
            for (int i = 0; i < mAccount.size(); ++i) {
                requestCircleState(mAccount.getCircle(i));
            }
        }
        requestUpdates(false);
    }

    public void requestCircleState(Circle circle) {
        final Circle c = circle;
        Log.e(getClass().getName(), "Load circle " + c.getName() + ", id:" + c.getCursor());
        if (c.getCursor() == -1) {
            RequestParams params = new RequestParams();
            params.put(WebConnection.PARAM_XSRF_TOKEN, mXSRF);
            params.put(WebConnection.PARAM_HISTORY_LENGTH, Integer.toString(HISTORY_LENGTH));
            params.put(WebConnection.PARAM_CIRCLE, Long.toString(c.getId()));

            WebConnection.post(WebConnection.PATH_CIRCLESTATE, params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(JSONObject obj) {
                    Log.e(getClass().getName(), "get state success:" + c.getName());
                    stateComplete(obj, c.getId());
                }

                public void onFailure(int statusCode, org.apache.http.Header[] headers, java.lang.String responseBody, java.lang.Throwable e) {
                    Log.e(getClass().getName(), "get state failure, error: " + responseBody + " circle:  " + c.getName());
                }
            });
        }
    }

    private class ConnectivityChangeReceiver extends BroadcastReceiver {
        private Context mContext;

        public ConnectivityChangeReceiver(Context c) {
            mContext = c;
        }

        boolean checkConnection() {
            if (mContext == null) return false;
            ConnectivityManager conMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (conMgr == null) return false;
            NetworkInfo i = conMgr.getActiveNetworkInfo();
            return i != null && i.isConnectedOrConnecting();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("NET", "Broadcast started");

            boolean connectionExists = checkConnection();
            if (connectionExists == mPause) {
                mPause = !connectionExists;
                if ( connectionExists) {
                    // connection was restored -> request updates
                    requestUpdates(true);
                }
                mPause = !connectionExists;
            }
        }
    }
	public void requestUpdates(boolean bRequestCircleState) {
        if ( !checkConnection()  ) {  // reload token only if we have connection
            onConnectionLost();
        }
        if ( mPause || mXSRF.isEmpty() )
            return;

		RequestParams params = new RequestParams();

		params.put(WebConnection.PARAM_XSRF_TOKEN, mXSRF);
        params.put(WebConnection.PARAM_NOTIFICATION_CURSOR, Long.toString(mAccount.getNotification()));
        params.put(WebConnection.PARAM_VERSION, mAccount.getVersion());
		for (int i = 0; i < mAccount.size(); ++i) {
			Circle circle = mAccount.getCircle(i);
			if (circle.getCursor() > 0) {
				params.put(Long.toString(circle.getId()), Long.toString(circle.getCursor()));
			}
            else if ( bRequestCircleState )
                requestCircleState(circle); // circle failed to load -> query circle state one more time
		}
        if ( mTimer != null )
            mHandler.removeCallbacks(mTimer);
        mTimer = new Runnable(){
            public void run(){
                 WebConnectionManager.get().requestUpdates(false);
            }
        };
        mHandler.postDelayed(mTimer, 60*1000); // ask about updates after 1 minute
		WebConnection.post(WebConnection.PATH_UPDATES, params, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject obj) {
                mFailedUpdateCount = 0;
                updatesComplete(obj);
			}

            @Override
            public void onFailure(int statusCode, org.apache.http.Header[] headers, java.lang.String responseBody, java.lang.Throwable e) {
                Log.e(getClass().getName(), "OnFailure!", e);
                if ( e instanceof java.net.SocketTimeoutException  ) {
                    //Toast.makeText(mContext, "Update timeout", Toast.LENGTH_LONG).show();
                    requestUpdates(true);
                }
                else {
                    String str = "update failed " + responseBody;
                    //Toast.makeText(mContext, str, Toast.LENGTH_LONG).show();
                    mFailedUpdateCount ++;
                    if ( mFailedUpdateCount > 2 ) {
                        reloadToken();
                    }
                    requestUpdates(true);
                }
            }
		});
	}
	
	private void updatesComplete(JSONObject update) {
		try {
            if ( update.has("wrongVersion") && update.getBoolean("wrongVersion") ){
                Log.i(getClass().getName(), "Server was restarted");
                Toast.makeText(mContext, mActivity.getResources().getText(R.string.server_restart), Toast.LENGTH_LONG).show();
                reloadToken();  // server restart
                return;
            }
			mAccount.setNotification(update.getLong("notificationCursor"));
			JSONArray msgs = update.getJSONArray("multimsgs");
			for (int i = 0; i < msgs.length(); ++i) {
				JSONObject circle = msgs.getJSONObject(i);
				postMessages(circle.getLong("circleId"), circle.getLong("cursor"), circle.getJSONArray("messages"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			// Fall through
		}
		requestUpdates(true);
	}

    public void fetchThreads(Circle circle) {
        for ( Map.Entry<Long, Circle.Thread> t: circle.getThreads().entrySet() ){
            if ( t.getValue().size() > 0 && circle.getMessage( t.getValue().get(0)).getParent() != -1 ){
                // first message is not root message -> we need to fetch complete thread
                fetchThread(circle.getId(),  t.getKey());
            }
        }
    }

    public void fetchThread(final long circleId, final long threadId) {
        if ( mPause || mXSRF.isEmpty() )
            return;

        RequestParams params = new RequestParams();
        params.put(WebConnection.PARAM_XSRF_TOKEN, mXSRF);
        params.put(WebConnection.PARAM_CIRCLE, Long.toString(circleId));
        params.put(WebConnection.PARAM_THREAD_ID, Long.toString(threadId));
        WebConnection.post(WebConnection.PATH_FETCH_THREAD, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject obj) {
                fetchThreadComplete(circleId, threadId, obj);
            }
            @Override
            public void onFailure(int statusCode, org.apache.http.Header[] headers, java.lang.String responseBody, java.lang.Throwable e) {
                Log.v(getClass().getName(), "fetchThread failed");
            }
        });
    }
    private void fetchThreadComplete(long circleId, long threadId, JSONObject update) {
        try {
            postNewMessages(circleId, threadId, update.getJSONArray("messages"), update.getLong("marker"));
        } catch (JSONException e) {
            e.printStackTrace();
            // Fall through
        }
        if ( mActivity != null ) {
            mActivity.updateCircles();
            if ( mAccount.getSelectedCircle() != null && circleId == mAccount.getSelectedCircle().getId() ) {
                mActivity.updateThreads();
                mActivity.updateMessages(false);
            }
        }
    }
	private void stateComplete(JSONObject state, long id) {
		try {
			postMessages(id, state.getLong("cursor"), state.getJSONArray("messages"));
			postMarkers(id, state.getJSONArray("markers"));
		} catch (JSONException e) {
			e.printStackTrace();
			// Fall through
		}
	}
	
	void postInitialCircles(JSONArray circles, long notification, String version) {
		Log.w(getClass().getName(), "Posting circles: cursor " + notification + ", num " + circles.length());
        mAccount.clear();
		try {
			for (int i = 0; i < circles.length(); ++i) {
				JSONObject circle = circles.getJSONObject(i);
				long cur_id = circle.getLong("circleId");
				Circle c = new Circle(cur_id, circle.getString("name"));
				mAccount.add(c);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			// Fall through
		}
		mAccount.setNotification(notification);
        mAccount.setVersion(version);
        if ( mActivity != null )
		    mActivity.getAdapter().notifyDataSetChanged();
	}
	
	private void postMessages(long circleId, long cursor, JSONArray messages) {
		Log.w(getClass().getName(), "Posting: " + circleId + ", cursor " + cursor + ", num " + messages.length());
		final Circle circle = mAccount.getCircleForId(circleId);
		if (circle == null) {
            Log.w(getClass().getName(), "postMessages: " + circleId + "cirlce is null");
			return;
		}
	//	if (circle.getCursor() >= cursor) {
			// We already have more messages.
	//		Log.e(getClass().getName(), "Current cursor " + circle.getCursor() + ", new " + cursor);
	//		return;
	//	}
		List<Message> update = new ArrayList<Message>(messages.length());
		Map<Long, Long> markers = new HashMap<Long, Long>();
        long prev_id = -1;
		try {
			for (int i = 0; i < messages.length(); ++i) {
				JSONObject source = messages.getJSONObject(i);
				switch (source.getInt("type")) {
					case Message.TYPE_TEXT:
                        long id = source.getLong("id");
                        if ( id == prev_id  )
                            break;
                        Message msg = Message.fromJson(source);
                        update.add(msg);
                        if (msg.getId() == mAccount.getNewMsgId()) {
                            // we received message with selected message id -> make thread id selected
                            mAccount.setSelectedThreadId(msg.getThread()); // make thread of new message selected
                        }
                        prev_id = id;
						break;
					case Message.TYPE_THREAD_READ:
						JSONObject cookie = source.getJSONObject("cookie");
						markers.put(cookie.getLong("thread"), cookie.getLong("marker"));
						break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			// Fall through.
		}
        fetchThreads(circle);     // check circle for non-complete threads
		circle.addAll(update);
		circle.setMarkers(markers, mAccount.getUserId());
		circle.setCursor(cursor);
        if ( mActivity != null ) {
            mActivity.updateCircles();
            if ( account().getSelectedCircle() != null && circle == account().getSelectedCircle() ) {
                mActivity.updateThreads();
                mActivity.updateMessages(true);
            }
        }
	}
	
	private void postMarkers(long circleId, JSONArray markers) {
		Log.w(getClass().getName(), "Markers: " + circleId + ", num " + markers.length());
		Map<Long, Long> threads = new HashMap<Long, Long>();
		try {
			for (int i = 0; i < markers.length(); i++) {
				JSONObject elem = markers.getJSONObject(i);
				threads.put(elem.getLong("thread"), elem.getLong("marker"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
			// Fall through.
		}
		final Circle circle = mAccount.getCircleForId(circleId);
		if (circle == null) {
			return;
		}
		circle.setMarkers(threads, mAccount.getUserId());
		//mAdapter.notifyCircleChanged(circleId);
		//mActivity.scrollCircleView(circleId);
	}

    private void postNewMessages(long circleId, long threadId, JSONArray messages, long marker) {
        Log.w(getClass().getName(), "Posting new messages: " + circleId + ", num " + messages.length());
        final Circle circle = mAccount.getCircleForId(circleId);
        if (circle == null) {
            Log.w(getClass().getName(), "postNewMessages: " + circleId + "cirlce is null");
            return;
        }
        List<Message> update = new ArrayList<Message>(messages.length());
        Map<Long, Long> markers = new HashMap<Long, Long>();
        markers.put(threadId, marker);
        long prev_id = -1;
        try {
            for (int i = 0; i < messages.length(); ++i) {
                JSONObject source = messages.getJSONObject(i);
                switch (source.getInt("type")) {
                    case Message.TYPE_TEXT:
                        long id = source.getLong("id");
                        if ( id == prev_id  )
                            break;
                        Message msg = Message.fromJson(source);
                        update.add(msg);
                        if (msg.getId() == mAccount.getNewMsgId()) {
                            // we received message with selected message id -> make thread id selected
                            mAccount.setSelectedThreadId(msg.getThread()); // make thread of new message selected
                        }
                        prev_id = id;
                        break;
                    case Message.TYPE_THREAD_READ:
                        JSONObject cookie = source.getJSONObject("cookie");
                        markers.put(cookie.getLong("thread"), cookie.getLong("marker"));
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            // Fall through.
        }
        circle.addAll(update);
        circle.setMarkers(markers, mAccount.getUserId());

        if ( mActivity != null ) {
            mActivity.updateCircles();
            if ( circle == mAccount.getSelectedCircle() ) {
                mActivity.updateThreads();
                mActivity.updateMessages(true);
            }
        }
    }

    private void postUserInfo(JSONObject userInfo) {
        try{
            mAccount.setUserId(userInfo.getLong("id"));
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(getClass().getName(), "Exception during getLong(id)");
        }
    }
	public synchronized void doPause() {
		mPause = true;
        mFailedUpdateCount = 0;
		Log.w(getClass().getName(), "Pausing");
	}
	public synchronized void doResume() {
		mPause = false;
		if (!mXSRF.isEmpty() && !mAccount.getCircles().isEmpty() ) {
			Log.w(getClass().getName(), "Resuming updates");
			requestUpdates(true);
		}
	}
    public synchronized void doStop() {
        if ( mProgressDialog != null && mProgressDialog.isShowing() )
            mProgressDialog.dismiss();
    }
    public void sendMessage(String message) {
        if ( mContext == null || mAccount.getSelectedCircle() == null ) return;
		Log.w(getClass().getName(), "Sending " + message);

		RequestParams params = new RequestParams();
		params.put(WebConnection.PARAM_XSRF_TOKEN, mXSRF);
		params.put(WebConnection.PARAM_CIRCLE, Long.toString(mAccount.getSelectedCircle().getId()));
		params.put(WebConnection.PARAM_BODY, message);
		long parent = mAccount.getSelectedMsgId();
		params.put(WebConnection.PARAM_PARENT_MESSAGE_ID, parent == -1 ? "null" : Long.toString(parent));
		params.put(WebConnection.PARAM_TARGET_USER_ID, "null");
		params.put(WebConnection.PARAM_VISIBLE_COUNT, "0");
        params.put(WebConnection.PARAM_OPEN_ID, mAccount.getClosedMode()?"0":"1");
        params.put(WebConnection.PARAM_LOCAL_DATA, Long.toString(mLocalData));
        mLocalData++;

		WebConnection.post(WebConnection.PATH_NEW, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String content) {
                mAccount.setNewMsgId(Long.parseLong(content));
				Log.i(getClass().getName(), "Response to SendMessage: " + content);
                //Toast.makeText(mContext, "response " + content , Toast.LENGTH_LONG).show();
			}
			@Override
			public void onFailure(Throwable error, String content) {
				Log.e(getClass().getName(), "get updates failure " + error);
				// TODO: Notification?
                //Toast.makeText(mContext, "failure " + content , Toast.LENGTH_LONG).show();
			}
		});
	}
    public void editMessage(final long msg_id, String message) {
        Log.w(getClass().getName(), "Editing " + message);
        final Circle cirlce = mAccount.getSelectedCircle();
        if ( cirlce == null || mActivity == null ) return;

        RequestParams params = new RequestParams();
        params.put(WebConnection.PARAM_XSRF_TOKEN, mXSRF);
        params.put(WebConnection.PARAM_CIRCLE, Long.toString(mAccount.getSelectedCircle().getId()));
        params.put(WebConnection.PARAM_BODY, message);
        params.put(WebConnection.PARAM_MSGID, Long.toString(msg_id));

            WebConnection.post(WebConnection.PATH_EDIT, params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(JSONObject obj) {
                    Log.i(getClass().getName(), "editMessage suceeded");
                    try{
                        boolean succeeded = obj.getBoolean("result");
                        if ( !succeeded ){
                            Toast.makeText(mContext, mActivity.getResources().getText(R.string.delete_failed), Toast.LENGTH_LONG).show();
                        }
                        else {
                            cirlce.deleteMessage(msg_id);
                            Circle.Thread t = mAccount.getSelectedThread();
                            if ( t == null ) return;
                            if ( msg_id == mAccount.getSelectedMsgId() )
                                mAccount.setSelectedMsgId( cirlce.getMessage(t.get(0)).getId());
                            mActivity.updateMessages(false);
                        }
                    }
                    catch(JSONException e){
                        Log.e(getClass().getName(), "JSON exception" + e.toString());
                    }
                }
                @Override
                public void onFailure(int statusCode, org.apache.http.Header[] headers, java.lang.String responseBody, java.lang.Throwable e) {
                    Toast.makeText(mContext, "edit failed" + responseBody, Toast.LENGTH_LONG).show();
                }
            });
    }
	public void markAllRead() {
        Map<Long, Long> markers = null;
        Circle circle = mAccount.getSelectedCircle();
        if ( circle == null ) {
            Toast.makeText(mContext, "Please select circle first", Toast.LENGTH_LONG).show();
            return;
        }
        long cid = circle.getId();
        if ( mActivity == null ) return;
        if ( mActivity.getPager().getCurrentItem() == CirclesActivity.FRAGMENT_MESSAGES ) {
            if ( mAccount.getSelectedThread() == null ) return;
            Circle.Thread t = mAccount.getSelectedThread();
            markers = new HashMap<Long, Long>();
            markers.put(t.getId(), circle.getMessage(t.getLast()).getId());
        }
        else {
            markers = circle.lastMessages();
        }
        JSONArray all = new JSONArray();
		try {
			for (Map.Entry<Long, Long> entry: markers.entrySet()) {
				JSONObject marker = new JSONObject();
				marker.put("circle", cid);
				marker.put("thread", entry.getKey());
				marker.put("marker", entry.getValue());
				all.put(marker);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Log.e(getClass().getName(), "markAllRead failed");
			return;
		}
		if (all.length() == 0) {
			Log.w(getClass().getName(), "No unread messages, doing nothing.");
			return;
		}
		RequestParams params = new RequestParams();
		params.put(WebConnection.PARAM_XSRF_TOKEN, mXSRF);
		params.put(WebConnection.PARAM_READ_STATUS, all.toString());
		Log.w(getClass().getName(), "Sending " + all.toString());
		
		WebConnection.post(WebConnection.PATH_SETREADMARKER, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String content) {
				Log.i(getClass().getName(), "Response to SetReadMarker: " + content);
			}
			
			@Override
			public void onFailure(Throwable error, String content) {
				Log.e(getClass().getName(), "set read marker failure " + error);
				// TODO: Notification?
			}
		});
	}
    public void markRead(long threadId, long msgId) {
        Map<Long, Long> markers = null;
        Circle circle = mAccount.getSelectedCircle();
        if ( circle == null ) {
            Toast.makeText(mContext, "Please select circle first", Toast.LENGTH_LONG).show();
            return;
        }
        long cid = circle.getId();
        Circle.Thread t = circle.getThreads().get(threadId);
        if ( t == null ) return;
        markers = new HashMap<Long, Long>();
        markers.put(t.getId(), msgId);
        JSONArray all = new JSONArray();
        try {
            for (Map.Entry<Long, Long> entry: markers.entrySet()) {
                JSONObject marker = new JSONObject();
                marker.put("circle", cid);
                marker.put("thread", entry.getKey());
                marker.put("marker", entry.getValue());
                all.put(marker);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(getClass().getName(), "markAllRead failed");
            return;
        }
        if (all.length() == 0) {
            Log.w(getClass().getName(), "No unread messages, doing nothing.");
            return;
        }
        RequestParams params = new RequestParams();
        params.put(WebConnection.PARAM_XSRF_TOKEN, mXSRF);
        params.put(WebConnection.PARAM_READ_STATUS, all.toString());
        Log.w(getClass().getName(), "Sending " + all.toString());

        WebConnection.post(WebConnection.PATH_SETREADMARKER, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String content) {
                Log.i(getClass().getName(), "Response to SetReadMarker: " + content);
            }

            @Override
            public void onFailure(Throwable error, String content) {
                Log.e(getClass().getName(), "set read marker failure " + error);
                // TODO: Notification?
            }
        });
    }}
