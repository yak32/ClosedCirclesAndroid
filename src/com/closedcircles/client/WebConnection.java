package com.closedcircles.client;

import android.content.Context;

import org.apache.http.client.CookieStore;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

public class WebConnection {
	private final static String SERVICE_HOST = "http://closedcircles.com";

    public final static String PATH_AUTH = "/auth";
    public final static String PATH_AUTH_GOOGLE = "/googleauth";
	public final static String PATH_JOINROOM = "/api/joinroom";
	public final static String PATH_CIRCLESTATE = "/api/getcirclestate";
	public final static String PATH_UPDATES = "/chat/updates";
	public final static String PATH_NEW = "/chat/new";
    public final static String PATH_EDIT = "/chat/edit";
	public final static String PATH_SETREADMARKER = "/chat/setreadmarker";
    public final static String PATH_FETCH_THREAD = "/api/fetchthread";

	public final static String PARAM_XSRF_TOKEN = "_xsrf";
	public final static String PARAM_ACCESS_TOKEN = "accessToken";
    public final static String PARAM_GOOGLE_ACCESS_TOKEN = "access_token";
	public final static String PARAM_HISTORY_LENGTH = "history_length";
	public final static String PARAM_CIRCLE = "circle";
    public final static String PARAM_THREAD_ID = "threadid";
	public final static String PARAM_NOTIFICATION_CURSOR = "notificationCursor";
    public final static String PARAM_VERSION = "version";
    public final static String PARAM_SERVER_VERSION = "serverVersion";
	public final static String PARAM_BODY = "body";
    public final static String PARAM_MSGID = "msgId";
	public final static String PARAM_PARENT_MESSAGE_ID = "parent_msg_id";
	public final static String PARAM_TARGET_USER_ID = "targetUserId";
	public final static String PARAM_TYPE = "type";
	public final static String PARAM_VISIBLE_COUNT = "voc";
    public final static String PARAM_OPEN_ID = "open";
	public final static String PARAM_READ_STATUS = "readStatus";
    public final static String PARAM_LOCAL_DATA = "localData";

	private static AsyncHttpClient sClient = new AsyncHttpClient();
    private static PersistentCookieStore myCookieStore = null;

    public static void create(Context context) {
        if ( myCookieStore == null ){
            myCookieStore = new PersistentCookieStore(context);
            sClient.setCookieStore(myCookieStore);
            sClient.getHttpClient().getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
            sClient.setTimeout(60000);
        }
    }

	public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		sClient.get(getAbsoluteUrl(url), params, responseHandler);
	}

	public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		sClient.post(getAbsoluteUrl(url), params, responseHandler);
	}
	
	public static String cookie(String name) {
        for (Cookie cookie: myCookieStore.getCookies()) {
        	if (cookie.getName().equals(name)) {
        		return cookie.getValue();
        	}
        }
        return null;
	}
	
	private static String getAbsoluteUrl(String relativeUrl) {
		return SERVICE_HOST + relativeUrl;
	}
}
