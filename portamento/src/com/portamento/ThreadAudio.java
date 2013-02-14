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

import android.os.AsyncTask;
import android.os.Looper;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.os.Handler;
import android.os.Message;
import com.portamento.Constants;

public class ThreadAudio extends Thread {
	
	static final int SAMPLING_RATE = 22050;
	static final int BIT_RATE = 8/8;
	
	static final int mMinBufferSz = AudioTrack.getMinBufferSize(SAMPLING_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT);
	static final int mBufferSz = mMinBufferSz*8;
	static final AudioTrack mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLING_RATE, AudioFormat.CHANNEL_OUT_MONO , AudioFormat.ENCODING_PCM_8BIT,
			mBufferSz, AudioTrack.MODE_STREAM);
	static final int mBufferInterval = 1000*mBufferSz/SAMPLING_RATE;
	
	static final int mChunkSz = mMinBufferSz/8;
	static byte mSamples[] = new byte[mBufferSz];
	
	private boolean mPlay = false;
	
	private float mVectorX = 0;
	private float mVectorY = Byte.MAX_VALUE;
	
	private int mFrequency = -1;
	private int mTargetFrequency = -1;

	static final float mRotateCos[] = new float[Constants.FREQUENCY_DELTA+1];
	static final float mRotateSin[] = new float[Constants.FREQUENCY_DELTA+1];
	
	
	private int mFreqLast;
	private float mFreqV;
	
	
	public ThreadAudio (Handler handler) {

		for (int i=0; i<=Constants.FREQUENCY_DELTA; i++){
			float frequency = Constants.FREQUENCY_LOW + i;
			float increment = (float)Math.PI*2*frequency/SAMPLING_RATE;
			mRotateCos[i] = (float)Math.cos(increment);
			mRotateSin[i] = (float)Math.sin(increment);
		}

		setPriority(Thread.MAX_PRIORITY);

		mTrack.setPositionNotificationPeriod(mChunkSz);
		mTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {

		    @Override
		    public void onMarkerReached(AudioTrack arg0) {

		    }

		    @Override
		    public void onPeriodicNotification(AudioTrack arg0) {
				
		    	if (mFrequency==-1) {
					mFrequency = mTargetFrequency;
				}
		    	int frequency = mTargetFrequency;
		    	if (frequency>=0) {
		            
		    		//Log.w(this.getClass().getName(), "write audio, "+String.valueOf(mChunkSz)+", "+String.valueOf(mFrequency));
		    		if (mTargetFrequency!=mFrequency && mDt>0) {
						
		    			//Log.w(this.getClass().getName(), "onPeriodicNotification, dt="+String.valueOf(mDt)+", f="+String.valueOf(mFrequency)+" -> "+String.valueOf(mTargetFrequency));
			    		
		    			int df = mTargetFrequency - mFrequency;
			    		int dt = mDt;
		    			int incrementf = 1;
		    			if (df<0) {
		    				incrementf = -1;
		    				df = -df;
		    			}
		    			if (df==dt) {
				            for (int i=0; i < mChunkSz; i++) {
				            	mSamples[i] = (byte)mVectorX;
				            	float x = mVectorX;
				            	float y = mVectorY;
				            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
				            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
				            	mFrequency += incrementf;
				            	if (mFrequency==mTargetFrequency) incrementf = 0;
				            }		    			
		    			} else if (df>dt) {
		    				int error = df/2;
				            for (int i=0; i < mChunkSz; i++) {
				            	mSamples[i] = (byte)mVectorX;
				            	float x = mVectorX;
				            	float y = mVectorY;
				            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
				            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
				            	while (error<df) {
				            		error += dt;
					            	mFrequency += incrementf;				            		
					            	if (mFrequency==mTargetFrequency) incrementf = 0;
				            	}
				            	error -= df;
				            }		    			
		    			} else {
		    				int error = dt/2;
				            for (int i=0; i < mChunkSz; i++) {
				            	mSamples[i] = (byte)mVectorX;
				            	float x = mVectorX;
				            	float y = mVectorY;
				            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
				            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
			            		error += df;
				            	if (error>dt) {
					            	mFrequency += incrementf;				            		
					            	if (mFrequency==mTargetFrequency) incrementf = 0;
					            	error -= dt;
				            	}
				            }		    			
		    			}
				    	mDt -= mChunkSz;
				    	if (mDt<0) {
		    				Log.w(this.getClass().getName(), "dt<0, df="+String.valueOf(df));
		    				mFrequency = mTargetFrequency;
				    		mDt = 0;
				    	}
		    		} else {
		    			if (mDt==0) {
				    		int df = mTargetFrequency - mFrequency;
		    				//Log.w(this.getClass().getName(), "dt=0, df="+String.valueOf(df));
		    			}
			            for (int i=0; i < mChunkSz; i++) {
			            	mSamples[i] = (byte)mVectorX;
			            	float x = mVectorX;
			            	float y = mVectorY;
			            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
			            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
			            }		    			
		    		}
		    	} else {
		            for (int i=0; i < mChunkSz; i++) mSamples[i] = 0;
		    	}
	            
	            mTrack.write(mSamples, 0, mChunkSz);
		    }
		});

        mTrack.play();
		mTrack.write(mSamples, 0, mBufferSz);
		Log.w(this.getClass().getName(), "ThreadAudio inited");

	}

	public Handler mHandler;
	
	private int mLastTargetFreq = -1;
	private int mDt, mDf;

	@Override
	public void run() {
		
        Looper.prepare();
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (msg.what==MotionEvent.ACTION_DOWN) {
					mTargetFrequency = msg.arg2;
					mLastTargetFreq = mTargetFrequency;
				} else if (msg.what==MotionEvent.ACTION_MOVE) {
					if (msg.arg2==0) {
						mTargetFrequency = -1;
					} else {
						mTargetFrequency = msg.arg2;
						if (mTargetFrequency<0) mTargetFrequency = 0;
						else if (mTargetFrequency>Constants.FREQUENCY_DELTA) mTargetFrequency = Constants.FREQUENCY_DELTA;
					}
					mDt = msg.arg1*SAMPLING_RATE/1000; // Convert elapsed time from millisecond to sample count
					mDf = mTargetFrequency-mLastTargetFreq;
	    			Log.w(this.getClass().getName(), "ACTION_MOVE, dt="+String.valueOf(mDt)+", f="+String.valueOf(mFrequency)+" -> "+String.valueOf(mTargetFrequency));
					//Log.w(this.getClass().getName(), "ACTION_MOVE, "+String.valueOf(mDt)+", "+String.valueOf(mDf));
					mLastTargetFreq = mTargetFrequency;
				} else if (msg.what==MotionEvent.ACTION_UP) {
					mTargetFrequency = -1;
					mLastTargetFreq = -1;
				}
            }
		};
       	//mHandler.post(mComposeAudio);
		Looper.loop();		
	}
	
//    final Runnable mComposeAudio = new Runnable()
//	{        
//
//	    public void run() {
//
//	    	if (mFrequency>=0) {
//	            for (int i=0; i < mChunkSz; i++) {
//	    	    	if (mTargetFrequency>mFrequency) mFrequency++;
//	    	    	else if (mTargetFrequency<mFrequency) mFrequency--;
//	            	mSamples[i] = (byte)mVectorX;
//	            	float x = mVectorX;
//	            	float y = mVectorY;
//	            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
//	            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
//	            }
//	    	} else {
//	            for (int i=0; i < mChunkSz; i++) mSamples[i] = 0;
//	    	}
//            
//            //Log.w(this.getClass().getName(), "write audio, "+String.valueOf(mBufferInterval)+", "+String.valueOf(elapsed));
//	    	mTrack.write(mSamples, 0, mChunkSz);
//
//   			mHandler.postDelayed(this, 6);
//    		
//	    }
//	};
	
}
