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
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	
    	final int action = event.getAction();
    	final float x = event.getX();
    	final float y = event.getY();
    	
    	mSurfaceController.actionMove(action, x, y);
    	//Log.w(this.getClass().getName(), String.valueOf(x) + "," + String.valueOf(y) + ", " + String.valueOf(action));
    	
    	if (action==MotionEvent.ACTION_DOWN) {
    		Message msg = Message.obtain();
    		msg.what = Constants.MSG_AUDIOCONTROL;
    		msg.arg1 = (int)(x*100000*mSurface.mCanvasWidthInverted);
    		msg.arg2 = (int)(y*100000*mSurface.mCanvasHeightInverted);
    		//msg.arg1 = (int)x;
    		//msg.arg2 = (int)y;
    		mAudio.mHandler.sendMessage(msg);
    	}
    	else if (action==MotionEvent.ACTION_MOVE) {
    		Message msg = Message.obtain();
    		msg.what = Constants.MSG_AUDIOCONTROL;
    		msg.arg1 = (int)(x*100000*mSurface.mCanvasWidthInverted);
    		msg.arg2 = (int)(y*100000*mSurface.mCanvasHeightInverted);
    		mAudio.mHandler.sendMessage(msg);    		
    	}
    	else if (action==MotionEvent.ACTION_UP) {
    		Message msg = Message.obtain();
    		msg.what = Constants.MSG_AUDIOCONTROL;
    		msg.arg1 = 0;
    		msg.arg2 = 0;
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
