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

import android.os.Looper;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.os.Handler;
import android.os.Message;
import com.portamento.Constants;
import java.util.Random;

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
	
	private float mVectorX = 0;
	private float mVectorY = Byte.MAX_VALUE;
	
	private int mFrequency = -1;
	private int mTargetFrequency = -1;

	static final float mRotateCos[] = new float[Constants.FREQUENCY_MAX+1];
	static final float mRotateSin[] = new float[Constants.FREQUENCY_MAX+1];
	
	private int mEnvelope = Constants.ENV_STANDBY;
	private float mVolAttack = 0.8f;
	private float mVolSustain = 0.56f;
	private float mVol = 0;
	private int mAttack = 250*SAMPLING_RATE/1000; // ADSR parameters are all in sample count
	private int mDecay = 200*SAMPLING_RATE/1000;
	private int mRelease = 200*SAMPLING_RATE/1000;
	private float mIncrementAttack = mVolAttack/mAttack;
	private float mIncrementDecay = (mVolSustain-mVolAttack)/mDecay;
	private float mIncrementRelease = -mVolSustain/mRelease;
	
	static final Random mNoise = new Random();
	static final float mNoiseLevel = 0;

	public void modelADSR() {
		if (mEnvelope==Constants.ENV_ATTACK) {
			mVol += mIncrementAttack;
			if (mVol>1) mVol = 1;
			if (mVol>=mVolAttack) {
				mEnvelope = Constants.ENV_DECAY;
			}
		} else if (mEnvelope==Constants.ENV_DECAY) {
			mVol += mIncrementDecay;
			if (mVol<=mVolSustain) {
				mEnvelope = Constants.ENV_SUSTAIN;
			}
		} else if (mEnvelope==Constants.ENV_SUSTAIN) {
			
		} else if (mEnvelope==Constants.ENV_RELEASE) {
			mVol += mIncrementRelease;
			if (mVol<=0) {
				mVol = 0;
				mEnvelope = Constants.ENV_STANDBY;
			}
	
		}    	
	}
    
	public ThreadAudio (Handler handler) {

		for (int i=0; i<=Constants.FREQUENCY_MAX; i++){
			float frequency = (int)i;
			float rad = (float)Math.PI*2*frequency/SAMPLING_RATE;
			mRotateCos[i] = (float)Math.cos(rad);
			mRotateSin[i] = (float)Math.sin(rad);
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
		    	
		    	if (mEnvelope==Constants.ENV_STANDBY) {
		            for (int i=0; i < mChunkSz; i++) mSamples[i] = 0;
		    	} else if (mTargetFrequency!=mFrequency && mDt>0 && mEnvelope!=Constants.ENV_RELEASE) {    
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
			            	mSamples[i] = (byte)(mVectorX*mVol+(mNoise.nextFloat()-0.5)*mNoiseLevel);
			            	float x = mVectorX;
			            	float y = mVectorY;
			            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
			            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
			            	mFrequency += incrementf;
			            	if (mFrequency==mTargetFrequency) incrementf = 0;
			            	modelADSR();
			            }		    			
	    			} else if (df>dt) {
	    				int error = df/2;
			            for (int i=0; i < mChunkSz; i++) {
			            	mSamples[i] = (byte)(mVectorX*mVol+(mNoise.nextFloat()-0.5)*mNoiseLevel);
			            	float x = mVectorX;
			            	float y = mVectorY;
			            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
			            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
			            	while (error<=df) {
			            		error += dt;
				            	mFrequency += incrementf;				            		
				            	if (mFrequency==mTargetFrequency) incrementf = 0;
			            	}
			            	error -= df;
			            	modelADSR();
			            }		    			
	    			} else {
	    				int error = dt/2;
			            for (int i=0; i < mChunkSz; i++) {
			            	mSamples[i] = (byte)(mVectorX*mVol+(mNoise.nextFloat()-0.5)*mNoiseLevel);
			            	float x = mVectorX;
			            	float y = mVectorY;
			            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
			            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
		            		error += df;
			            	if (error>=dt) {
				            	mFrequency += incrementf;				            		
				            	if (mFrequency==mTargetFrequency) incrementf = 0;
				            	error -= dt;
			            	}
			            	modelADSR();
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
		            	mSamples[i] = (byte)(mVectorX*mVol+(mNoise.nextFloat()-0.5)*mNoiseLevel);
		            	float x = mVectorX;
		            	float y = mVectorY;
		            	mVectorX = x*mRotateCos[mFrequency]-y*mRotateSin[mFrequency];
		            	mVectorY = x*mRotateSin[mFrequency]+y*mRotateCos[mFrequency];
		            	modelADSR();
		            }		    			
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
					mFrequency = msg.arg2;
					mLastTargetFreq = mTargetFrequency;
					if (mEnvelope!=Constants.ENV_STANDBY) {
						// a tone is still playing
						mVol = 0;
					}
					mEnvelope = Constants.ENV_ATTACK;
				} else if (msg.what==MotionEvent.ACTION_MOVE) {
					if (msg.arg2==0) {
						mTargetFrequency = -1;
					} else {
						mTargetFrequency = msg.arg2;
						if (mTargetFrequency<0) mTargetFrequency = 0;
						else if (mTargetFrequency>Constants.FREQUENCY_MAX) mTargetFrequency = Constants.FREQUENCY_MAX;
					}
					mDt = msg.arg1*SAMPLING_RATE/1000; // Convert elapsed time from millisecond to sample count
					mDf = mTargetFrequency-mLastTargetFreq;
	    			//Log.w(this.getClass().getName(), "ACTION_MOVE, dt="+String.valueOf(mDt)+", f="+String.valueOf(mFrequency)+" -> "+String.valueOf(mTargetFrequency));
					//Log.w(this.getClass().getName(), "ACTION_MOVE, "+String.valueOf(mDt)+", "+String.valueOf(mDf));
					mLastTargetFreq = mTargetFrequency;
				} else if (msg.what==MotionEvent.ACTION_UP) {
					mEnvelope = Constants.ENV_RELEASE;
					//mTargetFrequency = -1;
					//mLastTargetFreq = -1;
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
