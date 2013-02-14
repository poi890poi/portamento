package com.portamento;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.view.Menu;
import android.view.MotionEvent;
import android.util.Log;
import android.widget.TextView;

import com.portamento.R;
import com.portamento.ThreadAudio;
import com.portamento.ThreadSurface.SurfaceControler;
import com.portamento.Constants;

public class MainActivity extends Activity {

    /** A handle to the thread that's actually running the animation. */
    private SurfaceControler mSurfaceController;

    /** A handle to the View in which the game is running. */
    private ThreadSurface mSurface;

    private ThreadAudio mAudio;

	static final Handler mHandler = new Handler() {
		
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // get handles to the LunarView from XML, and its LunarThread
        mSurface = (ThreadSurface) findViewById(R.id.surfaceView1);
        mSurfaceController = mSurface.getThread();

        // give the LunarView a handle to the TextView used for messages
        mSurface.setTextView((TextView) findViewById(R.id.textView1));

        mAudio = new ThreadAudio(mHandler);
		mAudio.start();

        /*if (savedInstanceState == null) {
            // we were just launched: set up a new game
        	mSurfaceController.setState(SurfaceControler.STATE_READY);
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            // we are being restored: resume a previous game
        	mSurfaceController.restoreState(savedInstanceState);
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }*/
    }

    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSurface.getThread().pause(); // pause game when Activity pauses
    }
    
    private long mLastTouchEvent = 0;
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	
    	final int action = event.getAction();
    	final float x = event.getX();
    	final float y = event.getY();
    	
    	mSurfaceController.actionMove(action, x, y);
    	//Log.w(this.getClass().getName(), String.valueOf(x) + "," + String.valueOf(y) + ", " + String.valueOf(action));
    	
    	if (action==MotionEvent.ACTION_DOWN) {
    		Log.w(this.getClass().getName(), "ACTION_DOWN, "+String.valueOf(event.getEventTime()));
    		Message msg = Message.obtain();
    		msg.what = MotionEvent.ACTION_DOWN;
    		//msg.arg1 = (int)(x*100000*mSurface.mCanvasWidthInverted);
    		msg.arg1 = 0;
    		msg.arg2 = (int)((mSurface.mCanvasHeight-y)*Constants.FREQUENCY_DELTA*mSurface.mCanvasHeightInverted);
    		mLastTouchEvent = event.getEventTime();
    		mAudio.mHandler.sendMessage(msg);
    	}
    	else if (action==MotionEvent.ACTION_MOVE) {
    		//Log.w(this.getClass().getName(), "ACTION_MOVE, "+String.valueOf(event.getEventTime()));
    	    Message msg = Message.obtain();
    		msg.what = MotionEvent.ACTION_MOVE;
    		//msg.arg1 = (int)(x*100000*mSurface.mCanvasWidthInverted);
    		if (mLastTouchEvent!=0) msg.arg1 = (int)(event.getEventTime()-mLastTouchEvent);
    		else msg.arg1 = 0;
    		msg.arg2 = (int)((mSurface.mCanvasHeight-y)*Constants.FREQUENCY_DELTA*mSurface.mCanvasHeightInverted);
    		mLastTouchEvent = event.getEventTime();
    		mAudio.mHandler.sendMessage(msg);    		
    	}
    	else if (action==MotionEvent.ACTION_UP) {
    		Log.w(this.getClass().getName(), "ACTION_UP, "+String.valueOf(event.getEventTime()));
    		Message msg = Message.obtain();
    		msg.what = MotionEvent.ACTION_UP;
    		if (mLastTouchEvent!=0) msg.arg1 = (int)(event.getEventTime()-mLastTouchEvent);
    		else msg.arg1 = 0;
    		msg.arg2 = 0;
    		mLastTouchEvent = 0;
    		mAudio.mHandler.sendMessage(msg);
    	}
    	
    	return super.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
