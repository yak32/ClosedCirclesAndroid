package com.closedcircles.client.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import java.io.IOException;
import java.util.Arrays;


// Google authentication
import com.closedcircles.client.R;
import com.closedcircles.client.WebConnectionManager;
import com.facebook.widget.LoginButton;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;

// Facebook authentication
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

/**
 * Android Google+ Quickstart activity.
 *
 * Demonstrates Google+ Sign-In and usage of the Google+ APIs to retrieve a
 * users profile information.
 */
public class LoginActivity extends Activity implements
        ConnectionCallbacks, OnConnectionFailedListener, View.OnClickListener {

    private static final String TAG = "ClosedCircles.Login";
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;
    private static final int RC_SIGN_IN = 0;
    private static final int DIALOG_PLAY_SERVICES_ERROR = 0;
    private static final String SAVED_PROGRESS = "sign_in_progress";
    private boolean mFirstTimeAuth = true;
    private static final int REQUEST_CODE_TOKEN_AUTH = 9001;
    private final static String GOOGLE_AUTH_SCOPES =
            "oauth2:https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/userinfo.email  https://www.googleapis.com/auth/plus.me https://www.googleapis.com/auth/plus.login";

    public static final String INTENT_AUTHORIZED = "Authorized";
    public static final String INTENT_EXTRA_AUTH_TYPE = "authType";
    public static final String INTENT_EXTRA_SIGNOUT = "Signout";
    public static final String AUTH_TYPE_GOOGLE = "GoogleAuth";
    public static final String AUTH_TYPE_FACEBOOK = "FacebookAuth";


    // GoogleApiClient wraps our service connection to Google Play services and
    // provides access to the users sign in state and Google's APIs.
    private GoogleApiClient mGoogleApiClient;

    // We use mSignInProgress to track whether user has clicked sign in.
    // mSignInProgress can be one of three values:
    //
    //       STATE_DEFAULT: The default state of the application before the user
    //                      has clicked 'sign in', or after they have clicked
    //                      'sign out'.  In this state we will not attempt to
    //                      resolve sign in errors and so will display our
    //                      Activity in a signed out state.
    //       STATE_SIGN_IN: This state indicates that the user has clicked 'sign
    //                      in', so resolve successive errors preventing sign in
    //                      until the user has successfully authorized an account
    //                      for our app.
    //   STATE_IN_PROGRESS: This state indicates that we have started an intent to
    //                      resolve an error, and so we should not start further
    //                      intents until the current intent completes.
    private int mSignInProgress;

    // Used to store the PendingIntent most recently returned by Google Play
    // services until the user clicks 'sign in'.
    private PendingIntent mSignInIntent;

    // Used to store the error code most recently returned by Google Play services
    // until the user clicks 'sign in'.
    private int mSignInError;
    private boolean mSignOut;

    private SignInButton mSignInButton;

    // Facebook authentication
    private static UiLifecycleHelper mFacebookHelper;

    private Session.StatusCallback mFacebookCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mSignInButton = (SignInButton) findViewById(R.id.google_sign_in_button);
        mSignInButton.setOnClickListener(this);

        if (savedInstanceState != null) {
            mSignInProgress = savedInstanceState.getInt(SAVED_PROGRESS, STATE_DEFAULT);
        }
        // Google authentication helper
        mGoogleApiClient = buildGoogleApiClient();
        if ( getIntent().getBooleanExtra(LoginActivity.INTENT_EXTRA_SIGNOUT, false)  ) {

            if ( Session.getActiveSession() != null && Session.getActiveSession().isOpened() ) {
                // facebook signout
                try {
                    Session.getActiveSession().closeAndClearTokenInformation();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
                mSignOut = true;
        }

        LoginButton authButton = (LoginButton) findViewById(R.id.facebook_sign_in_button);
        authButton.setReadPermissions(Arrays.asList("basic_info", "email"));

        // facebook authentication helper
        mFacebookHelper = new UiLifecycleHelper(this, mFacebookCallback);
        mFacebookHelper.onCreate(savedInstanceState);
    }

    // Google authentication

    private GoogleApiClient buildGoogleApiClient() {
        // When we build the GoogleApiClient we specify where connected and
        // connection failed callbacks should be returned, which Google APIs our
        // app uses and which OAuth 2.0 scopes our app requests.
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, null)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if ( mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient  != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        mFacebookHelper.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_PROGRESS, mSignInProgress);
        mFacebookHelper.onSaveInstanceState(outState);
    }

    public void SignOut(){
        // We clear the default account on sign out so that Google Play
        // services will not return an onConnected callback without user
        // interaction.
        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        mGoogleApiClient.disconnect();
        mGoogleApiClient.connect();
        mSignInButton.setEnabled(true);
        mSignInProgress = STATE_DEFAULT;
    }

    @Override
    public void onClick(View v) {
        if (!mGoogleApiClient.isConnecting()) {
            // We only process button clicks when GoogleApiClient is not transitioning
            // between connected and not connected.
            switch (v.getId()) {
                case R.id.google_sign_in_button:
                    //mStatus.setText(R.string.status_signing_in);
                    resolveSignInError();
                    break;
/*                case R.id.revoke_access_button:
                    // After we revoke permissions for the user with a GoogleApiClient
                    // instance, we must discard it and create a new one.
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    // Our sample has caches no user data from Google+, however we
                    // would normally register a callback on revokeAccessAndDisconnect
                    // to delete user data so that we comply with Google developer
                    // policies.
                    Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
                    mGoogleApiClient = buildGoogleApiClient();
                    mGoogleApiClient.connect();
                    break;*/
            }
        }
    }

    /* onConnected is called when our Activity successfully connects to Google
     * Play services.  onConnected indicates that an account was selected on the
     * device, that the selected account has granted any requested permissions to
     * our app and that we were able to establish a service connection to Google
     * Play services.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Reaching onConnected means we consider the user signed in.
        Log.i(TAG, "onConnected");

        // Indicate that the sign in process is complete.
        mSignInProgress = STATE_DEFAULT;
        if ( mSignOut) {
            SignOut();
            mSignOut = false;
            return;
        }

        // Update the user interface to reflect that the user is signed in.
        mSignInButton.setEnabled(false);

        // We've resolved any connection errors.
        Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {
                    token = GoogleAuthUtil.getToken(LoginActivity.this,
                            Plus.AccountApi.getAccountName(mGoogleApiClient),
                            GOOGLE_AUTH_SCOPES);
                } catch (IOException transientEx) {
                    // Network or server error, try later
                    Log.e(TAG, transientEx.toString());
                } catch (UserRecoverableAuthException e) {
                    // Recover (with e.getIntent())
                    if ( mFirstTimeAuth ){
                        Log.e(TAG, e.toString());
                        Intent recover = e.getIntent();
                        startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
                        mFirstTimeAuth = false;
                    }
                    else
                        return token;
                } catch (GoogleAuthException authEx) {
                    // The call is not ever expected to succeed
                    // assuming you have already verified that
                    // Google Play services is installed.
                    Log.e(TAG, authEx.toString());
                }
                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                if ( token != null ){
                    Log.i(TAG, "Access token retrieved");
                    WebConnectionManager.get().doAuth(token, AUTH_TYPE_GOOGLE);
                    Intent i = new Intent(LoginActivity.this, CirclesActivity.class);
                    i.putExtra(INTENT_AUTHORIZED, true);
                    startActivity(i);
                }
            }
        };
        task.execute();
    }

    /* onConnectionFailed is called when our Activity could not connect to Google
     * Play services.  onConnectionFailed indicates that the user needs to select
     * an account, grant permissions or resolve an error in order to sign in.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.i(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());

        if (mSignInProgress != STATE_IN_PROGRESS) {
            // We do not have an intent in progress so we should store the latest
            // error resolution intent for use when the sign in button is clicked.
            mSignInIntent = result.getResolution();
            mSignInError = result.getErrorCode();

            if (mSignInProgress == STATE_SIGN_IN) {
                // STATE_SIGN_IN indicates the user already clicked the sign in button
                // so we should continue processing errors until the user is signed in
                // or they click cancel.
                resolveSignInError();
            }
        }

        // In this sample we consider the user signed out whenever they do not have
        // a connection to Google Play services.
        onSignedOut();
    }

    /* Starts an appropriate intent or dialog for user interaction to resolve
     * the current error preventing the user from being signed in.  This could
     * be a dialog allowing the user to select an account, an activity allowing
     * the user to consent to the permissions being requested by your app, a
     * setting to enable device networking, etc.
     */
    private void resolveSignInError() {
        if (mSignInIntent != null) {
            // We have an intent which will allow our user to sign in or
            // resolve an error.  For example if the user needs to
            // select an account to sign in with, or if they need to consent
            // to the permissions your app is requesting.

            try {
                // Send the pending intent that we stored on the most recent
                // OnConnectionFailed callback.  This will allow the user to
                // resolve the error currently preventing our connection to
                // Google Play services.
                mSignInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(),
                        RC_SIGN_IN, null, 0, 0, 0);
            } catch (SendIntentException e) {
                Log.i(TAG, "Sign in intent could not be sent: "
                        + e.getLocalizedMessage());
                // The intent was canceled before it was sent.  Attempt to connect to
                // get an updated ConnectionResult.
                mSignInProgress = STATE_SIGN_IN;
                mGoogleApiClient.connect();
            }
        } else {
            // Google Play services wasn't able to provide an intent for some
            // error types, so we show the default Google Play services error
            // dialog which may still start an intent on our behalf if the
            // user can resolve the issue.
            showDialog(DIALOG_PLAY_SERVICES_ERROR);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    // If the error resolution was successful we should continue
                    // processing errors.
                    mSignInProgress = STATE_SIGN_IN;
                } else {
                    // If the error resolution was not successful or the user canceled,
                    // we should stop processing errors.
                    mSignInProgress = STATE_DEFAULT;
                }

                if (!mGoogleApiClient.isConnecting()) {
                    // If Google Play services resolved the issue with a dialog then
                    // onStart is not called so we need to re-attempt connection here.
                    mGoogleApiClient.connect();
                }
                break;
        }
        mFacebookHelper.onActivityResult(requestCode, resultCode, data);
    }

    private void onSignedOut() {
        // Update the UI to reflect that the user is signed out.
        mSignInButton.setEnabled(true);
        //mSignOutButton.setEnabled(false);
        //mRevokeButton.setEnabled(false);

        //mStatus.setText(R.string.status_signed_out);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        // We call connect() to attempt to re-establish the connection or get a
        // ConnectionResult that we can attempt to resolve.
        mGoogleApiClient.connect();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_PLAY_SERVICES_ERROR:
                if (GooglePlayServicesUtil.isUserRecoverableError(mSignInError)) {
                    return GooglePlayServicesUtil.getErrorDialog(
                            mSignInError,
                            this,
                            RC_SIGN_IN,
                            new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Log.e(TAG, "Google Play services resolution cancelled");
                                    mSignInProgress = STATE_DEFAULT;
                                    //mStatus.setText(R.string.status_signed_out);
                                }
                            });
                } else {
                    return new AlertDialog.Builder(this)
                            .setMessage(R.string.play_services_error)
                            .setPositiveButton(R.string.close,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Log.e(TAG, "Google Play services error could not be "
                                                    + "resolved: " + mSignInError);
                                            mSignInProgress = STATE_DEFAULT;
                                            //mStatus.setText(R.string.status_signed_out);
                                        }
                                    }).create();
                }
            default:
                return super.onCreateDialog(id);
        }
    }

    // Facebook authentication
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if ( state == SessionState.CLOSED_LOGIN_FAILED ){
            String str = "Connection failed.";
            if ( exception != null )
                str += exception.toString();
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
        }
        else
        if ( state.isOpened() ) {
            // make request to the /me API
            Log.i(TAG, "Facebook: valid token is received. Starting Circles activity");
            Toast.makeText(this, "User is connected!", Toast.LENGTH_LONG).show();

            WebConnectionManager.get().doAuth(session.getAccessToken(), AUTH_TYPE_FACEBOOK);
            Intent i = new Intent(this, CirclesActivity.class);
            i.putExtra(INTENT_AUTHORIZED, true);
            startActivity(i);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        mFacebookHelper.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        mFacebookHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFacebookHelper.onDestroy();
    }


}
