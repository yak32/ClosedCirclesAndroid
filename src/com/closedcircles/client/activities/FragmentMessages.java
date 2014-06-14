package com.closedcircles.client.activities;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.closedcircles.client.R;
import com.closedcircles.client.WebConnectionManager;
import com.closedcircles.client.model.*;
import com.closedcircles.client.adapters.*;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FragmentMessages extends android.support.v4.app.Fragment {
    ListView mViewMessages;
    EditText mEditText;
    int mSelectedPosition = 0;
    AdapterMessages mMessagesAdapter = null;
    int mSelected=-1;
    private Runnable mShowImeRunnable = null;
    private Runnable mScrollRunnable = null;
    private Runnable mPostReadRunnable = null;
    private ImageButton mbtnSend = null;
    HashMap<Long, Long> mItemsOnScreen = new HashMap<Long, Long>();

    private class PostReadRunnable implements Runnable{
        private long mThreadId = -1;
        private long mMsgId = -1;
        public PostReadRunnable(long thread_id, long msg_id){
            mThreadId = thread_id;
            mMsgId = msg_id;
        }
        public void run() {
            WebConnectionManager.get().markRead(mThreadId, mMsgId);
        }
    }

    public static final FragmentMessages newInstance() {
        FragmentMessages f = new FragmentMessages();
        return f;
    }

    public FragmentMessages() {
        super();
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view =  inflater.inflate(R.layout.fragment_messages, container, false);
        mViewMessages =  (ListView)view.findViewById(R.id.message_list);
        mEditText =  (EditText)view.findViewById(R.id.edit_message);
        mbtnSend =  (ImageButton)view.findViewById(R.id.send_message);

        Typeface authTypeface = Typeface.createFromAsset(getActivity().getAssets(), "Roboto-LightItalic.ttf");
        Typeface msgTypeface = Typeface.createFromAsset(getActivity().getAssets(), "Messages.ttf");

        mMessagesAdapter = new AdapterMessages(this, msgTypeface, authTypeface);
        mViewMessages.setAdapter(mMessagesAdapter);
        mViewMessages.setItemsCanFocus(true);
        updateList(false, false);
        mEditText.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent motionEvent) {
                setImeVisibility(true);
                return false;
            }
        });
        mShowImeRunnable = new Runnable() {
            public void run() {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null){
                    imm.showSoftInput(mEditText, 0);
                    mEditText.postDelayed(mScrollRunnable, 1000);
                }
            }
        };
        mScrollRunnable = new Runnable() {
            public void run() {
                mViewMessages.smoothScrollToPosition(mSelectedPosition);
            }
        };
        mViewMessages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onMessageClicked(v, position);
            }
        });
        mViewMessages.setOnScrollListener(new ListView.OnScrollListener() {
            int oldFirstVisibleItem = 0;
            int oldLastVisibleItem = 0;

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem > oldFirstVisibleItem) {
                    for(int i = oldFirstVisibleItem; i < firstVisibleItem; i++) {
                        onExit(i);
                    }
                }
                if (firstVisibleItem < oldFirstVisibleItem) {
                    for(int i = firstVisibleItem; i < oldFirstVisibleItem; i++) {
                        onEnter(i);
                    }
                }

                int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
                if (lastVisibleItem < oldLastVisibleItem) {
                    for(int i = oldLastVisibleItem+1; i <= lastVisibleItem; i++) {
                        onExit(i);
                    }
                }
                if (lastVisibleItem > oldLastVisibleItem) {
                    for(int i = oldLastVisibleItem+1; i <= lastVisibleItem; i++) {
                        onEnter(i);
                    }
                }

                oldFirstVisibleItem = firstVisibleItem;
                oldLastVisibleItem = lastVisibleItem;
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}
        });

        mbtnSend.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v){
                Editable text = mEditText.getText();
                if ( text != null )
                    sendMessage(text.toString());
            }
        });
        registerForContextMenu(mViewMessages);
        return view;
    }
    public void onEnter(int position) {
        if ( !mItemsOnScreen.containsValue(Long.valueOf(position)) )
            mItemsOnScreen.put(Long.valueOf(position), System.currentTimeMillis());
    }
    public void onExit(int position) {
        if ( mItemsOnScreen.containsKey(Long.valueOf(position))){
            if ( (System.currentTimeMillis()-mItemsOnScreen.get(Long.valueOf(position)))/1000 < WebConnectionManager.MESSAGE_MAKE_READ_TIME ){
                mItemsOnScreen.remove( Long.valueOf(position));
            }
        }
    }
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if ( mViewMessages == null || mItemsOnScreen == null ) return;
        if (!isVisibleToUser) {
            int start = mViewMessages.getFirstVisiblePosition();
            int end = mViewMessages.getLastVisiblePosition();
            for ( int i=start;i<=end;++i ){
                onExit(i);
            }
            //find largest visible position
            long largest = -1;
            for (long position : mItemsOnScreen.keySet()) {
                if ( position > largest )
                    largest = position;
            }
            if ( largest == -1 ) {
                mItemsOnScreen.clear();
                return;
            }
            CirclesActivity activity = (CirclesActivity) getActivity();
            Account account = WebConnectionManager.get().account();
            Circle circle = account.getSelectedCircle();
            if (circle == null) return;
            Circle.Thread thread = account.getSelectedThread();
            if (thread != null && largest >= thread.getFirstUread() && largest < mMessagesAdapter.getCount() ) {
                //thread.setFirstUnread(position + 1);
                String str = "item was read" + largest;
                Log.w(getClass().getName(), str);
                WebConnectionManager.get().markRead(thread.getId(), mMessagesAdapter.getItem((int) largest).msg_id);
            }
            mItemsOnScreen.clear();
        }
        else {
            int start = mViewMessages.getFirstVisiblePosition();
            int end = mViewMessages.getLastVisiblePosition();
            for ( int i=start;i<=end;++i ){
                onEnter(i);
            }
        }
    }
    public int getSelectedPosition() { return mSelectedPosition; }
    public void onMessageClicked(View v, int position){
        if (mMessagesAdapter != null)
            WebConnectionManager.get().account().setSelectedMsgId(mMessagesAdapter.getItem(position).msg_id);
        mViewMessages.setItemChecked(position, true);
        mSelectedPosition = position;
    }
    public void setImeVisibility(final boolean visible) {
        if ( mEditText == null ) return;
        if (visible) {
            mEditText.post(mShowImeRunnable);
        } else {
            mEditText.removeCallbacks(mShowImeRunnable);
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
            }
        }
    }
    private void sendMessage(String msg){
        if ( msg.isEmpty() ) return;
        if ( WebConnectionManager.get().checkConnection() == false ) {
            Toast.makeText(getActivity(), getResources().getText(R.string.no_connection), Toast.LENGTH_LONG).show();
            return;
        }
        WebConnectionManager.get().sendMessage(msg);
        mEditText.setText("");
    }
    private void editMessage(long msg_id, String msg){
        if ( WebConnectionManager.get().checkConnection() == false ) {
            Toast.makeText(getActivity(), getResources().getText(R.string.no_connection), Toast.LENGTH_LONG).show();
            return;
        }
        WebConnectionManager.get().editMessage(msg_id, msg);
        mEditText.setText("");
    }
    public void clearList(){
        mMessagesAdapter.clear();
    }

    class MsgComparator implements Comparator<Circle.MsgThread> {
        @Override
        public int compare(Circle.MsgThread a, Circle.MsgThread b) {
            return (int)(a.msg_id-b.msg_id);
        };
    };

    public void updateList(boolean scroll_to_last, boolean scroll_to_last_read){
        if( mMessagesAdapter == null ) return;
        mViewMessages.clearChoices();

        // fill tread array to display in the list
        CirclesActivity activity = (CirclesActivity)getActivity();
        Account account = WebConnectionManager.get().account();
        Circle circle = account.getSelectedCircle();
        if ( circle == null ) return;
        Circle.Thread thread = account.getSelectedThread();
        ArrayList<Circle.MsgThread> messages = new ArrayList<Circle.MsgThread>();
        int selected = -1;
        if ( thread == null ) {
            // add all circle messages
/*            for ( int i=0;i<circle.getNumMessages();++i ){
                Message msg = circle.getMessage(i);
                if (msg.getId() == account.getSelectedMsgId())
                    selected = messages.size();
                add_message(messages, msg);
            }*/
        }
        else {
            // add all thread messages
            int size = thread.size();
            for (int i = 0; i < size; ++i) {
                int position = thread.get(i);
                Message msg = circle.getMessage(position);
                if (msg.getId() == account.getSelectedMsgId())
                    selected = messages.size();
                add_message(messages, msg);
            }
        }
        // sort messages
        Collections.sort(messages, new MsgComparator());

        mMessagesAdapter.clear();
        mMessagesAdapter.addAll(messages);

        if ( selected != -1 )
            mViewMessages.setItemChecked(selected, true);

        // during adding new messages, try to scroll to the end, to see last added messages
        if ( scroll_to_last_read ) {
            if ( thread.hasUnread() )
                mViewMessages.setSelection(thread.getFirstUread());
            else
                mViewMessages.setSelection(mMessagesAdapter.getCount()-1);
        }
        else if ( scroll_to_last && messages.size()>0 && messages.get(messages.size()-1).msg_id == account.getNewMsgId() ){
            mViewMessages.smoothScrollToPosition(mMessagesAdapter.getCount()-1);
            account.setNewMsgId(-1);
        }
    }
    private void add_message( ArrayList<Circle.MsgThread> messages, Message msg){
        Circle.MsgThread m = new Circle.MsgThread();
        m.msg = msg.getMessage();
        m.authour = msg.getFullName();
        String to_text = (msg.getFlags() & (long) (Message.OPENREPLAY)) != 0 ? ":reply @" : ":closed @";
        if (msg.getParent() != -1) {
            to_text += msg.getParentName();
            m.authour += to_text;
        }
        m.date = msg.getDate();
        m.thread_id = msg.getThread();
        m.msg_id = msg.getId();
        m.isRead = msg.isRead();
        messages.add(m);
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.msg_context_menu, menu);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_copy_text:
                copy_text(mMessagesAdapter.getItem(info.position).msg);
                return true;
            case R.id.action_delete:
                String str = new String("");
                editMessage(mMessagesAdapter.getItem(info.position).msg_id, str);
                return true;
            case R.id.action_user_info:
                Intent i = new Intent(getActivity(), UserInfoActivity.class);
                i.putExtra(CirclesActivity.EXTRA_USER_NAME, mMessagesAdapter.getItem(info.position).authour);
                getActivity().startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void copy_text(String text){
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("text label",text);
            clipboard.setPrimaryClip(clip);
        }
    }
}
