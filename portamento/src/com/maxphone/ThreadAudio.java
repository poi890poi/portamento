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

package com.maxphone;

import android.os.AsyncTask;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.util.Log;
import android.os.Handler;

//public class ThreadAudio extends Thread {
public class ThreadAudio {
	
	static final int SAMPLING_RATE = 22050;
	
	long mLastTime = 0;
	
	static final int mBufferSz = AudioTrack.getMinBufferSize(SAMPLING_RATE, AudioFormat.CHANNEL_OUT_MONO , AudioFormat.ENCODING_PCM_8BIT)*4;
	static final AudioTrack mAudio = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLING_RATE, AudioFormat.CHANNEL_OUT_MONO , AudioFormat.ENCODING_PCM_8BIT,
			mBufferSz, AudioTrack.MODE_STREAM);
	static final int mBufferInterval = 1000*mBufferSz/SAMPLING_RATE;
	
	static byte mSamples[] = new byte[mBufferSz];
	
	private boolean mPlay = false;
	
	// Fixed frequency for performance test
	float mVectorX = 0;
	float mVectorY = Byte.MAX_VALUE;
	static final float mFrequency = 261.626f;
	static final float mIncrement = (float)Math.PI*2*mFrequency/SAMPLING_RATE;
	static final float mCos = (float)Math.cos(mIncrement);
	static final float mSin = (float)Math.sin(mIncrement);
	
	static final Handler mHandler = new Handler() {
		
	};

    long elapsed_accum = -1;
    int cycle = 0;

    final Runnable mComposeAudio = new Runnable()
	{        
        public void run_postdelayed() {
        	long now = System.nanoTime()/1000000;
    		long elapsed = now-mLastTime;

    		if (elapsed_accum==-1) {
    			elapsed_accum = 0;
    		} else {
	    		elapsed_accum += elapsed;
	    		cycle++;
	    		if (cycle>=100) {
	    			Log.w(this.getClass().getName(), "write audio, "+String.valueOf(elapsed_accum/cycle));
	    			cycle = 0;
	    			elapsed_accum = 0;
	    		}
    		}
    		mLastTime = now;
	    	
    		if (mPlay) {
    			mHandler.post(this);
    		} else {
    			mAudio.stop();
    			elapsed_accum = -1;
    			cycle = 0;
    		}

	    }

	    public void run() {
        	/*long now = System.nanoTime()/1000000;
    		long elapsed = now-mLastTime;

    		if (mLastTime!=0) {
        		elapsed_accum += elapsed;
        		cycle++;
        		if (cycle>=10) {
        			Log.w(this.getClass().getName(), "write audio, "+String.valueOf(mBufferInterval)+", "+String.valueOf(elapsed_accum/cycle));
        			cycle = 0;
        			elapsed_accum = 0;
        		}    			
    		}*/
    		
    		//int targetlen = mBufferSz/2;
    		    		
    		//if (elapsed*SAMPLING_RATE/1000 >= targetlen) {
    		if (true) {
    			//targetlen = (int)(elapsed*SAMPLING_RATE/1000);
    			//targetlen = mBufferSz;
    			//if (targetlen>mBufferSz) targetlen = mBufferSz;
    			//float frequency = 261.626f;
	            //float increment = (float)(2*Math.PI)*frequency/SAMPLING_RATE; 
	            for (int i=0; i < mBufferSz; i++) {
	            	mSamples[i] = (byte)mVectorX;
	            	float x = mVectorX;
	            	float y = mVectorY;
	            	mVectorX = x*mCos-y*mSin;
	            	mVectorY = x*mSin+y*mCos;
	            }
	            
	            //Log.w(this.getClass().getName(), "write audio, "+String.valueOf(mBufferInterval)+", "+String.valueOf(elapsed));
	            mAudio.write(mSamples, 0, mBufferSz);
	
	            //mLastTime = now;
    		}

    		if (mPlay) {
    			mHandler.postDelayed(this, 6);
    		} else {
    			mAudio.flush();
    			mAudio.stop();
    			mLastTime = 0;
    			cycle = 0;
    		}
    		
	    }
	};

	
	
    //@Override
    public void run() {
    	mLastTime = 0;
        mAudio.play();
       	mHandler.post(mComposeAudio);
    }

    //@Override
    public void run_loop() { // For performance testing only
    	/*while (true) {
	        try {
	        	long now = System.nanoTime()/1000000;
	    		long elapsed = now - mLastTime;

	    		if (mLastTime!=0) {
		    		elapsed_accum += elapsed;
		    		cycle++;
		    		if (cycle>=100) {
		    			Log.w(this.getClass().getName(), "write audio, "+String.valueOf(elapsed_accum/cycle));
		    			cycle = 0;
		    			elapsed_accum = 0;
		    		}
	    		}
	    		mLastTime = now;
	            sleep(20);
	        } catch (InterruptedException e) {
	        	
	        }
    	}*/
    }

    public synchronized void setPlay(boolean play) {
    	Log.w(this.getClass().getName(), "setPlay, "+String.valueOf(play));
    	mPlay = play;
    }
}
