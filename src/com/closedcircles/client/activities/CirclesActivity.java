package com.closedcircles.client.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;


import com.closedcircles.client.R;
import com.closedcircles.client.WebConnection;
import com.closedcircles.client.WebConnectionManager;
import com.closedcircles.client.model.*;
import com.closedcircles.client.adapters.*;


import java.util.ArrayList;
import java.util.List;

public class CirclesActivity extends ActionBarActivity {
    private static final String TAGClass = " CirclesActivity: ";
    MyPageAdapter           pageAdapter;
    ViewPager               mPager;
    AdapterCircles          mCirlcesAdapter = null;

    public static final int        FRAGMENT_CIRCLES = 0;
    public static final int        FRAGMENT_THREADS = 1;
    public static final int        FRAGMENT_MESSAGES = 2;
    public static final String     EXTRA_USER_NAME = "user_name";

    private static final String    KEY_REPLY_MODE = "reply_mode";

    public final static int LOGIN_REQUEST_CODE = 10001;

    private Menu            mMenu; // reference to menu

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toast.makeText(this, "onCreate", Toast.LENGTH_LONG).show();

        Typeface italicTypeface = Typeface.createFromAsset(getAssets(), "Roboto-LightItalic.ttf");
        mCirlcesAdapter = new AdapterCircles(this, italicTypeface, WebConnectionManager.get().account().getCircles());
        //if ( savedInstanceState != null )
        //Toast.makeText(this, "savedInstanceState is not null, " + savedInstanceState.getString("xsrf")!=null? "xsrf exists": "xsrf is null", Toast.LENGTH_LONG).show();

        WebConnectionManager.get().setup(this, savedInstanceState);
        updateThreads();
        updateCircles();
        updateMessages(false);

        if ( savedInstanceState != null ){
            boolean mode = savedInstanceState.getBoolean(KEY_REPLY_MODE);
            WebConnectionManager.get().account().setClosedMode(mode);
        }

        mPager = (ViewPager)findViewById(R.id.pager);

        List<android.support.v4.app.Fragment> fragments = getFragments();
        pageAdapter = new MyPageAdapter(getSupportFragmentManager(), fragments);
        mPager.setAdapter(pageAdapter);

        mPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        if ( getSupportActionBar() == null || getSupportFragmentManager() == null ) return;
                        getSupportActionBar().setSelectedNavigationItem(position);
                        if ( position != FRAGMENT_MESSAGES ) {
                            android.support.v4.app.Fragment f = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":" + FRAGMENT_MESSAGES);
                            if (f != null)
                                ((FragmentMessages) f).setImeVisibility(false);
                        }
                    }
                });

        final ActionBar actionBar = getSupportActionBar();

        // Specify that tabs should be displayed in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create a tab listener that is called when the user changes tabs.
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                mPager.setCurrentItem(tab.getPosition());
                //if ( tab.getPosition() == FRAGMENT_THREADS ){
                //    updateThreads();
               // }
            }
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            }
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }
        };
        // Add 3 tabs, specifying the tab's text and TabListener
        actionBar.addTab(actionBar.newTab().setText("Circles").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("Threads").setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab().setText("Messages").setTabListener(tabListener));

        boolean runLoginActivity = true;
        Intent i = getIntent();
        // if activity was called from Login Activity
        if ( i.hasExtra(LoginActivity.INTENT_AUTHORIZED) && i.getBooleanExtra(LoginActivity.INTENT_AUTHORIZED, false) ) {
            runLoginActivity = false;
        }

        //String str = WebConnectionManager.get().XSRF().isEmpty()?"XSRF is empty":"XSRF is not empty";
        //Toast.makeText(this, str, Toast.LENGTH_LONG).show();

        // call login activity only of XSRF is empty and we have connection
        if (  !WebConnectionManager.get().checkConnection() || !WebConnectionManager.get().XSRF().isEmpty() )
            runLoginActivity = false;  // no need to login if we have no connection or we have XSRF initialized

        if ( runLoginActivity ){
            //Toast.makeText(this, "Application is not authorized - need SignIn", Toast.LENGTH_LONG).show();
            startLogin(false);
            return;
        }
        //Toast.makeText(this, "Token exists", Toast.LENGTH_LONG).show();
    }
    public void startLogin(boolean clearState){
        Log.w(getClass().getName(), "calling LoginActivity");
        WebConnectionManager.get().setXSRF("");  // clear cookie, to properly restart session
        //Toast.makeText(this, "XSRF cleared", Toast.LENGTH_LONG).show();
        Intent i = new Intent(this, LoginActivity.class);
        if ( clearState )
            i.putExtra(LoginActivity.INTENT_EXTRA_SIGNOUT, true);
        startActivity(i);
    }
    public AdapterCircles getAdapter() { return mCirlcesAdapter; }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        mMenu = menu;
        MenuItem menuItem = mMenu.findItem(R.id.action_closed_mode);
        MenuItem menuItem2 = mMenu.findItem(R.id.action_closed_mode2);
        if ( menuItem != null ) {
            if (WebConnectionManager.get().account().getClosedMode()) {
                menuItem.setIcon(R.drawable.ic_action_secure);
                menuItem2.setTitle(getResources().getString(R.string.action_set_open_mode));
            }
            else {
                menuItem.setIcon(R.drawable.ic_action_not_secure);
                menuItem2.setTitle(getResources().getString(R.string.action_set_closed_mode));
            }
        }
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public void onPause(){
        //Toast.makeText(this, "onPause", Toast.LENGTH_LONG).show();
        super.onPause();
        WebConnectionManager.get().doPause();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();

        WebConnectionManager.get().onCirclesActivityClosed();
    }
    @Override
    public void onResume(){
        //Toast.makeText(this, "onResume", Toast.LENGTH_LONG).show();
        super.onResume();
        WebConnectionManager.get().doResume();
    }
    public ViewPager getPager() { return mPager; }
    public AdapterCircles   getCirclesAdapter() { return mCirlcesAdapter; }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Account account = WebConnectionManager.get().account();
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_new_thread:
                if ( account.getSelectedCircle() != null ){
                    account.setSelectedMsgId(-1);
                    account.setSelectedThreadId(-1);
                    mPager.setCurrentItem(FRAGMENT_MESSAGES);
                    android.support.v4.app.Fragment f =  getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.pager+":"+FRAGMENT_MESSAGES);
                    if ( f != null )
                        ((FragmentMessages)f).clearList();
                }
                else
                    Toast.makeText(this, getResources().getText(R.string.select_circle), Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_mark_all_read:
                WebConnectionManager.get().markAllRead();
                return true;
            case R.id.action_closed_mode:
            case R.id.action_closed_mode2:
                account.setClosedMode(!account.getClosedMode());
                MenuItem menuItem = mMenu.findItem(R.id.action_closed_mode);
                MenuItem menuItem2 = mMenu.findItem(R.id.action_closed_mode2);
                if ( menuItem != null ) {
                    if (account.getClosedMode()) {
                        menuItem2.setTitle(getResources().getString(R.string.action_set_open_mode));
                        menuItem.setIcon(R.drawable.ic_action_secure);
                    }
                    else {
                        menuItem2.setTitle(getResources().getString(R.string.action_set_closed_mode));
                        menuItem.setIcon(R.drawable.ic_action_not_secure);

                    }
                }
                return true;
            case R.id.action_logout:
                startLogin(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        int id = mPager.getCurrentItem();
        if ( id == FRAGMENT_CIRCLES ){
            // open home screen on back pressed
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
        else if ( id == FRAGMENT_THREADS )
            mPager.setCurrentItem(FRAGMENT_CIRCLES);
        else if ( id == FRAGMENT_MESSAGES )
            mPager.setCurrentItem(FRAGMENT_THREADS);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w(getClass().getName(), "onActivityResult");
 /*       if (requestCode == LOGIN_REQUEST_CODE) {
            if (data != null && data.hasExtra(LoginActivity.KEY_TOKEN)) {
                WebConnectionManager.get().doAuth(data.getStringExtra(LoginActivity.KEY_TOKEN));
            } else {
                Log.e(getClass().getName(), "No result from LoginActivity");
                finish();
            }
        } else {*/
            super.onActivityResult(requestCode, resultCode, data);
        //}
    }
    public void updateCircles(){
        android.support.v4.app.Fragment f =  getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.pager+":"+FRAGMENT_CIRCLES);
        if ( f != null )
            mCirlcesAdapter.notifyDataSetChanged();
    }
    public void updateThreads(){
        android.support.v4.app.Fragment f =  getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.pager+":"+FRAGMENT_THREADS);
        if ( f != null )
            ((FragmentThreads)f).updateList();
    }
    public void updateMessages(boolean scroll_to_last){
        android.support.v4.app.Fragment f =  getSupportFragmentManager().findFragmentByTag("android:switcher:"+R.id.pager+":"+FRAGMENT_MESSAGES);
        if ( f != null )
            ((FragmentMessages)f).updateList(scroll_to_last, false);
    }
    private List<android.support.v4.app.Fragment> getFragments(){
        List<android.support.v4.app.Fragment> fList = new ArrayList<android.support.v4.app.Fragment>();
        fList.add(FragmentCircles.newInstance());
        fList.add(FragmentThreads.newInstance());
        fList.add(FragmentMessages.newInstance());
        return fList;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        WebConnectionManager.get().account().saveState(outState);
        outState.putBoolean(KEY_REPLY_MODE, WebConnectionManager.get().account().getClosedMode());
        super.onSaveInstanceState(outState);
    }

    private class MyPageAdapter extends FragmentPagerAdapter {
        private List<android.support.v4.app.Fragment> fragments;

        public MyPageAdapter(FragmentManager fm, List<android.support.v4.app.Fragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }
        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            return this.fragments.get(position);
        }

       /* public  CharSequence getPageTitle(int i){
            if ( i == 0 )
                return "Circles";
            if ( i == 1 )
                return "Threads";
            if ( i == 2 )
                return "Messages";
            return "test";
        }*/

        @Override
        public int getCount() {
            return this.fragments.size();
        }
    }
}

