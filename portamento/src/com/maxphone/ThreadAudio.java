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

public class ThreadAudio extends Thread {
	
	static final int SAMPLING_RATE = 22050;
	
	long mLastTime = 0;
	
	static final int mBufferSz = AudioTrack.getMinBufferSize(SAMPLING_RATE, AudioFormat.CHANNEL_OUT_MONO , AudioFormat.ENCODING_PCM_8BIT)*3;
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
	
	public ThreadAudio (Handler handler) {
	
		setPriority(Thread.MAX_PRIORITY);
	}
	
	@Override
	public void run(){
		mAudio.write(mSamples, 0, mBufferSz);
        mAudio.play();
       	mHandler.post(mComposeAudio);
	}
	
	static final Handler mHandler = new Handler() {
		
	};

    long elapsed_accum = -1;
    int cycle = 0;

    final Runnable mComposeAudio = new Runnable()
	{        

	    public void run() {

	    	if (mPlay) {
	            for (int i=0; i < mBufferSz; i++) {
	            	mSamples[i] = (byte)mVectorX;
	            	float x = mVectorX;
	            	float y = mVectorY;
	            	mVectorX = x*mCos-y*mSin;
	            	mVectorY = x*mSin+y*mCos;
	            }
	    	} else {
	            for (int i=0; i < mBufferSz; i++) {
	            	mSamples[i] = 0;
	            }
	    	}
            
            //Log.w(this.getClass().getName(), "write audio, "+String.valueOf(mBufferInterval)+", "+String.valueOf(elapsed));
            mAudio.write(mSamples, 0, mBufferSz);

   			mHandler.postDelayed(this, 6);
    		
	    }
	};
	
    public synchronized void setPlay(boolean play) {
    	Log.w(this.getClass().getName(), "setPlay, "+String.valueOf(play));
    	mPlay = play;
    }
}
