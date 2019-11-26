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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Allows playback of a single MP3 file via the UI. It contains a {@link MediaPlayerHolder}
 * which implements the {@link PlayerAdapter} interface that the activity uses to control
 * audio playback.
 */
public final class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public static final int UPLOAD_REQUEST_CODE = 1;

    private SeekBar mSeekbarAudio;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;

    private int loopMode = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
        Log.d(TAG, "onCreate: finished");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            mPlayerAdapter.release();
            Log.d(TAG, "onStop: release MediaPlayer");
        }
    }

    private void initializeUI() {
        final Button mPlayButton = (Button) findViewById(R.id.button_play);
        Button mPauseButton = (Button) findViewById(R.id.button_pause);
        Button mUploadButton = (Button) findViewById(R.id.button_upload);
        final Button mSetLoopButton = (Button) findViewById(R.id.button_set_loop);
        final TextView mLoopStartText = (TextView) findViewById(R.id.text_loop_start);
        final TextView mLoopEndText = (TextView) findViewById(R.id.text_loop_end);
        Button mIncreaseSpeedButton = (Button) findViewById(R.id.button_increase_speed);
        Button mDecreaseSpeedButton = (Button) findViewById(R.id.button_decrease_speed);
        Button mSkipForwardButton = (Button) findViewById(R.id.button_skip_forward);
        Button mSkipBackwardButton = (Button) findViewById(R.id.button_skip_backward);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);

        final TextView mStartMarker = (TextView) findViewById(R.id.loop_start_marker);
        final View mBeforeLoopBlank = findViewById(R.id.before_loop_blank);
        final View mBetweenLoopBlank = findViewById(R.id.in_between_loop_blank);
        final TextView mEndMarker = (TextView) findViewById(R.id.loop_end_marker);
        final View mAfterLoopBlank = findViewById(R.id.after_loop_blank);

        mPauseButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.pause();
                    }
                });
        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.play();
                    }
                });
        mUploadButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onUpload();
                    }
                });
        mSetLoopButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (loopMode == -1) {
                            return;
                        }

                        //start text and end text get handled in MediaPlayerHolder
                        mPlayerAdapter.setLoop(loopMode, mLoopStartText, mLoopEndText);

                        float songLength = (float) mPlayerAdapter.getSongLength();
                        float loopStart = (float) mPlayerAdapter.getLoopStart();
                        float loopEnd = (float) mPlayerAdapter.getLoopEnd();

                        loopMode++;     // switch to next mode

                        int mode = loopMode % 3;
                        if (mode == 0) {
                            mStartMarker.setVisibility(View.INVISIBLE);
                            mEndMarker.setVisibility(View.INVISIBLE);
                            LinearLayout.LayoutParams beforeBlankParams = new LinearLayout.LayoutParams(0, 0, 0);
                            mBeforeLoopBlank.setLayoutParams(beforeBlankParams);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0);
                            mStartMarker.setLayoutParams(params);

                            LinearLayout.LayoutParams betweenBlankParams = new LinearLayout.LayoutParams(0, 0, 0);
                            mBetweenLoopBlank.setLayoutParams(betweenBlankParams);

                            LinearLayout.LayoutParams afterEndBlankParams = new LinearLayout.LayoutParams(0, 0, 0);
                            mAfterLoopBlank.setLayoutParams(afterEndBlankParams);

                            mSetLoopButton.setText("Set loop start");
                        } else if (mode == 1) {
                            LinearLayout.LayoutParams beforeBlankParams = new LinearLayout.LayoutParams(0, 0, loopStart/songLength);
                            mBeforeLoopBlank.setLayoutParams(beforeBlankParams);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1 - loopStart/songLength);
                            mStartMarker.setLayoutParams(params);

                            mStartMarker.setVisibility(View.VISIBLE);
                            mSetLoopButton.setText("Set loop end");
                        } else if (mode == 2) {

                            LinearLayout.LayoutParams beforeBlankParams = new LinearLayout.LayoutParams(0, 0, loopStart/songLength);
                            mBeforeLoopBlank.setLayoutParams(beforeBlankParams);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0);
                            mStartMarker.setLayoutParams(params);

                            LinearLayout.LayoutParams betweenBlankParams = new LinearLayout.LayoutParams(0, 0, (loopEnd - loopStart)/songLength);
                            mBetweenLoopBlank.setLayoutParams(betweenBlankParams);

                            LinearLayout.LayoutParams afterEndBlankParams = new LinearLayout.LayoutParams(0, 0, (songLength - loopEnd)/songLength);
                            mAfterLoopBlank.setLayoutParams(afterEndBlankParams);

                            mEndMarker.setVisibility(View.VISIBLE);

                            mSetLoopButton.setText("Clear loop");
                        }
                    }
                });
        mIncreaseSpeedButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.increaseSpeed();
                    }
                });
        mDecreaseSpeedButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.decreaseSpeed();
                    }
                });
        mSkipForwardButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.skipForward();
                    }
                });
        mSkipBackwardButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.skipBackward();
                    }
                });
    }

    private void onUpload() {
        Intent myIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
        myIntent.setType("audio/*");
        startActivityForResult(myIntent, UPLOAD_REQUEST_CODE);

        loopMode = 0;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode == UPLOAD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                    Uri uploadedMusic = intent.getData();
                    Log.d(TAG, "uploaded music uri is:");
                    Log.d(TAG, uploadedMusic.toString());
                    mPlayerAdapter.loadMedia(uploadedMusic);
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        Log.d(TAG, "initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        mSeekbarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);
            Log.d(TAG, String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
                Log.d(TAG, String.format("setPlaybackPosition: setProgress(%d)", position));
            }
        }

        @Override
        public void onStateChanged(@State int state) {
        }

    }
}