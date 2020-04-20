/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.mediaplayersample;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.chibde.visualizer.BarVisualizer;
import com.chibde.visualizer.LineBarVisualizer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.util.Log;
import android.widget.TextView;

/**
 * Exposes the functionality of the {@link MediaPlayer} and implements the {@link PlayerAdapter}
 * so that {@link MainActivity} can control music playback.
 */
public final class MediaPlayerHolder implements PlayerAdapter {
    public static final String TAG = "MediaPlayerHolder";
    public static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000;

    private final Context mContext;
    private MediaPlayer mMediaPlayer;
    private PlaybackInfoListener mPlaybackInfoListener;
    private ScheduledExecutorService mExecutor;
    private Runnable mSeekbarPositionUpdateTask;

    private float speed = 1.00f;


    private int loopStart = 0;
    private int loopEnd = 0;
    private int songLength = 0;

    private boolean looping;

    public MediaPlayerHolder(Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Once the {@link MediaPlayer} is released, it can't be used again, and another one has to be
     * created. In the onStop() method of the {@link MainActivity} the {@link MediaPlayer} is
     * released. Then in the onStart() of the {@link MainActivity} a new {@link MediaPlayer}
     * object has to be created. That's why this method is private, and called by load(int) and
     * not the constructor.
     */
    private void initializeMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setLooping(true);
        }
    }

    public void setPlaybackInfoListener(PlaybackInfoListener listener) {
        mPlaybackInfoListener = listener;
    }

    @Override
    public void setDuration() {
        if (mMediaPlayer != null) {
            songLength = mMediaPlayer.getDuration();
        }
    }

    @Override
    public int getLoopStart() {
        return loopStart;
    }

    @Override
    public int getLoopEnd() {
        return loopEnd;
    }

    @Override
    public int getSongLength() {
        return songLength;
    }

    // Implements PlaybackControl.
    @Override
    public void loadMedia(Uri uri) {

        initializeMediaPlayer();

        try {
            mMediaPlayer.setDataSource(mContext, uri);
        } catch (Exception e) {
            Log.d(TAG, "loadMedia error");
        }

        try {
            mMediaPlayer.prepare();
        } catch (Exception e) {
            Log.d(TAG, "loadMedia error");
        }

        initializeProgressCallback();
    }

    @Override
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public boolean isInitialized(){
        return !(mMediaPlayer == null);
    }

    @Override
    public int play() {
        if (mMediaPlayer != null){
            startUpdatingCallbackWithPosition();
            if(mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PAUSED);
                }
                return 1;
            }else{
                mMediaPlayer.start();
                mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(speed));
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);
                }
                return 2;
            }
        }
        return 3;
    }

    /** Same functionality as play() but
     *  is void and does not pause if playing already.
     *  Only plays.
     * */
    private void onlyPlay() {
        if (mMediaPlayer != null){
            startUpdatingCallbackWithPosition();
            if(!mMediaPlayer.isPlaying()) {
                mMediaPlayer.start();
                mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(speed));
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener.onStateChanged(PlaybackInfoListener.State.PLAYING);
                }
            }
        }
    }

    @Override
    public void visualize(LineBarVisualizer visualizer){

        if(visualizer.getVisibility() == View.INVISIBLE){
            visualizer.setVisibility(View.VISIBLE);
        }else {
            //visualizer.setColor(ContextCompat.getColor(mContext, R.color.lightblue)); // define custom number of bars you want in the visualizer between (10 - 256).
            visualizer.setColor(ContextCompat.getColor(mContext, R.color.colorPrimaryDark));
            visualizer.setDensity(60); // Set your media player to the visualizer.
            visualizer.setPlayer(mMediaPlayer.getAudioSessionId());
            visualizer.setVisibility(View.VISIBLE);
        }

    }

    public void stopVisualize(LineBarVisualizer visualizer) {
        visualizer.setVisibility(View.GONE);
    }

    @Override
    public void setLoop(int loopMode, TextView startText, TextView endText) {
        /**
         * When loop button is clicked, calls this based on current stage of loop creation.
         *
         * Stages:
         *  - -1: No music uploaded. No changes made to loop.
         *  - 0: Set start of loop
         *  - 1: Set end of loop. If start point is after end point, still creates loop properly.
         *  - 2: Clear loop start and end.
         *
         * Reference website implementation at function setLoop() in audio.js:
         * https://github.com/afxdance/music/blob/master/audio.js
         */

        setDuration();
        loopMode = loopMode % 3;    // Keep loopMode within the 3 possible valid inputs
        if (loopMode == -1) {
            return;
        } else if (loopMode == 0) {
            loopStart = mMediaPlayer.getCurrentPosition();
            startText.setText("Loop Start: " + convertToTime(loopStart));
        } else if (loopMode == 1) {
            loopEnd = mMediaPlayer.getCurrentPosition();
            Log.d(TAG, "Set loop end: " + loopEnd);
            if (loopStart > loopEnd) {  // Flip start/end if loop is inverted
                int temp = loopStart;
                loopStart = loopEnd;
                loopEnd = temp;
            }
            if (loopEnd == songLength) { //handle loopEnd is at very end of song
                Log.d(TAG, "LoopEnd == SongLength: " + loopEnd);
                loopEnd -= 250;
                Log.d(TAG, "Changing LoopEnd: " + loopEnd);
            }

            startText.setText("Loop Start: " + convertToTime(loopStart));
            endText.setText("Loop End: " + convertToTime(loopEnd));

            looping = true;
            mMediaPlayer.setLooping(false);
        } else {    // Clear loop
            looping = false;
            mMediaPlayer.setLooping(true);

            startText.setText("Loop Start: N/A");
            endText.setText("Loop End: N/A");
        }
    }

    private String convertToTime(int milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds - minutes * 60000) / 1000;

        if (seconds < 10) {
            return minutes + ":0" + seconds;
        } else {
            return minutes + ":" + seconds;
        }
    }

    @Override
    public void skipForward() {
        //Skips position forwards 5 seconds.
        if(isInitialized()) {
            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + 5000);
            updateProgressCallbackTask();
        }
    }

    @Override
    public void skipBackward() {
        //Skips position backwards 5 seconds.
        if(isInitialized()) {
            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() - 5000);
            updateProgressCallbackTask();
        }
    }


    @Override
    public float adjustSpeed(int crease) {
        //Increases playback speed by 5%
        if((speed > .25 && crease == -1) || (speed < 2.45 && crease == 1)) {
            speed += crease * 0.05f;
            if(mMediaPlayer == null){
                return speed;
            }
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(speed));
            }
        }
        return speed;
    }

    @Override
    public void seekTo(int position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    @Override
    public double[] getTime(){
        double[] time = new double[2];
        time[0] = mMediaPlayer.getCurrentPosition() * Math.floorDiv(1000, 60);
        time[1] = mMediaPlayer.getCurrentPosition() * Math.round(Math.floorMod(1000, 60));
        return time;
    }

    /**
     * Syncs the mMediaPlayer position with mPlaybackProgressCallback via recurring task.
     */
    private void startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        if (mSeekbarPositionUpdateTask == null) {
            mSeekbarPositionUpdateTask = new Runnable() {
                @Override
                public void run() {
                    updateProgressCallbackTask();
                    // Looping
                    if (looping) {
                        int curr = mMediaPlayer.getCurrentPosition();
                        if (curr >= songLength || curr >= loopEnd) {
                            Log.d(TAG, "Looping back from " + loopEnd + " to " + loopStart);
                            mMediaPlayer.seekTo(loopStart);
                            onlyPlay();
                        }
                    }
                }
            };
        }
        mExecutor.scheduleAtFixedRate(
                mSeekbarPositionUpdateTask,
                0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void updateProgressCallbackTask() {
        if (mMediaPlayer != null) { //&& mMediaPlayer.isPlaying()) {
            int currentPosition = mMediaPlayer.getCurrentPosition();
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onPositionChanged(currentPosition);
            }
        }
    }

    @Override
    public void initializeProgressCallback() {
        final int duration = mMediaPlayer.getDuration();
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener.onDurationChanged(duration);
            mPlaybackInfoListener.onPositionChanged(0);
        }
    }

}
