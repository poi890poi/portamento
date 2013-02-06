/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.portamento;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
//import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import com.portamento.Constants;

/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 * 
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
class ThreadSurface extends SurfaceView implements SurfaceHolder.Callback {

    public int mCanvasWidth = -1;
    public int mCanvasHeight = -1;
    public float mCanvasWidthInverted = 0;
    public float mCanvasHeightInverted = 0;
    
	class SurfaceControler extends Thread {
        /*
         * Difficulty setting constants
         */
        public static final int DIFFICULTY_HARD = 1;
        public static final int DIFFICULTY_MEDIUM = 2;
        
        private int mCanvasWidth = -1;
        private int mCanvasHeight = -1;

    	private final Paint mBackground = new Paint();
    	private final Paint mTextDefault = new Paint();
    	
    	private float mTouchX = -1;
    	private float mTouchY = -1;
    	private String mDbgMsg1 = new String("INIT");

        /*
         * Member (state) fields
         */
        /** The drawable to use as the background of the animation canvas */
        private Bitmap mBackgroundImage;

        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** Used to figure out elapsed time between frames */
        private long mLastTime;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        public SurfaceControler(SurfaceHolder surfaceHolder, Context context,
            // get handles to some important objects
        		Handler handler) {
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            //mContext = context;
            
            mBackground.setColor(Color.WHITE);
            mBackground.setStyle(Paint.Style.FILL);
            
            mTextDefault.setColor(Color.BLACK);
            mTextDefault.setTextSize(16);
        }

        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
            }
        }

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
            }
        }

        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         * 
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
            	
            }
        }
        
        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        doDraw(c);
                    }
                    sleep(16);
                } catch (InterruptedException e) {
                	
                }
                finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         * 
         * @return Bundle with this view's state
         */
        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
                    //map.putInt(KEY_DIFFICULTY, Integer.valueOf(mDifficulty));
                    //map.putDouble(KEY_X, Double.valueOf(mX));
                }
            }
            return map;
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         * 
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;

        		Message msg = Message.obtain();
        		msg.what = Constants.MSG_SURFACESIZECHANGED;
        		msg.arg1 = width;
        		msg.arg2 = height;
        		mHandler.sendMessage(msg);
                // don't forget to resize the background image
                //mBackgroundImage = mBackgroundImage.createScaledBitmap(
                        //mBackgroundImage, width, height, true);
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
            }
        }

        /**
         * Draws the ship, fuel/speed bars, and background to the provided
         * Canvas.
         */
        private void doDraw(Canvas canvas) {
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            //canvas.drawBitmap(mBackgroundImage, 0, 0, null);
        	
        	canvas.drawPaint(mBackground);
        	canvas.drawText(mDbgMsg1, 15, 25, mTextDefault);

        	canvas.restore();
        }
        
        public synchronized void actionMove(int action, float x, float y) {
            synchronized (mSurfaceHolder) {
            	mTouchX = x;
            	mTouchY = y;
            	mDbgMsg1 = String.valueOf(mTouchX) + "," + String.valueOf(mTouchY) + ", " + String.valueOf(action);
            }
        }
        
    }

    /** Handle to the application context, used to e.g. fetch Drawables. */
    //private Context mContext;

    /** Pointer to the text view to display "Paused.." etc. */
    private TextView mStatusText;

    /** The thread that actually draws the animation */
    private SurfaceControler thread;

    public ThreadSurface(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new SurfaceControler(holder, context, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //mStatusText.setVisibility(m.getData().getInt("viz"));
                //mStatusText.setText(m.getData().getString("text"));
            	if (msg.what==Constants.MSG_SURFACESIZECHANGED) {
            		mCanvasWidth = msg.arg1;
            		mCanvasHeight = msg.arg2;
            		mCanvasWidthInverted = 1.0f/mCanvasWidth;
            		mCanvasHeightInverted = 1.0f/mCanvasHeight;
            	}
            }
        });

        setFocusable(true); // make sure we get key events
    }

    /**
     * Fetches the animation thread corresponding to this LunarView.
     * 
     * @return the animation thread
     */
    public SurfaceControler getThread() {
        return thread;
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }

    /**
     * Installs a pointer to the text view used for messages.
     */
    public void setTextView(TextView textView) {
        mStatusText = textView;
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}
