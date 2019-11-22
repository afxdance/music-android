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

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chibde.visualizer.BarVisualizer;
import com.chibde.visualizer.LineBarVisualizer;

/**
 * Allows playback of a single MP3 file via the UI. It contains a {@link MediaPlayerHolder}
 * which implements the {@link PlayerAdapter} interface that the activity uses to control
 * audio playback.
 */
public final class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public static final int MEDIA_RES_ID = R.raw.jazz_in_paris;
    public static final int UPLOAD_REQUEST_CODE = 1;

    private TextView mTextDebug;

    private SeekBar mSeekbarAudio;
    private ScrollView mScrollContainer;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;

    private AlertDialog.Builder mBuilder;
    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private Context mContext = this;
    private LineBarVisualizer mBarVisualizer;
    private boolean enableVisualize = false;
    private boolean permissionChecked = false;
    private TextView curr_speed;

    private int loopMode = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("Music Visualizer");
        mBuilder.setMessage("Audio Recording is required for our visualizer to function properly. Please allow this permission. Thank you!");
        mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                askPermission();
            }
        });
        mBuilder.create();
        mBuilder.show();
//        if (permissionChecked){
//            enableVisualize = true;
//            setContentView(R.layout.activity_main);
//            curr_speed = (TextView) findViewById(R.id.speed);
//            initializeUI();
//            initializeSeekbar();
//            initializePlaybackController();
//            Log.d(TAG, "onCreate: finished");
//            Toast mToast = Toast.makeText(this, "Welcome to the slow.afx.dance mobile app!", Toast.LENGTH_LONG);
//            mToast.setGravity(Gravity.TOP,0,150);
//            mToast.show();
//
//        }
    }

    private void askPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

                    // MY_PERMISSIONS_REQUEST_RECORD_AUDIO is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.

                }
            } else {
                // Permission has already been granted
            }
        }

        //        AppCompatActivity.requestPermissions(MainActivity.this,
        //                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
        //                1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    enableVisualize = true;

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                permissionChecked = true;
                setContentView(R.layout.activity_main);
                curr_speed = (TextView) findViewById(R.id.speed);
                initializeUI();
                initializeSeekbar();
                initializePlaybackController();
                Log.d(TAG, "onCreate: finished");
                Toast mToast = Toast.makeText(this, "Welcome to the slow.afx.dance mobile app!", Toast.LENGTH_LONG);
                mToast.setGravity(Gravity.TOP,0,150);
                mToast.show();
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //mPlayerAdapter.loadMedia(MEDIA_RES_ID);
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
        mTextDebug = (TextView) findViewById(R.id.text_debug);
//        final Button mPlayButton = (Button) findViewById(R.id.button_play);
        final ImageButton mPlayButton = (ImageButton) findViewById(R.id.button_play);
//        Button mPauseButton = (Button) findViewById(R.id.button_pause);
        Button mUploadButton = (Button) findViewById(R.id.button_upload);
//       Button mSetLoopButton = (Button) findViewById(R.id.button_set_loop);
        Button mIncreaseSpeedButton = (Button) findViewById(R.id.button_increase_speed);
        Button mDecreaseSpeedButton = (Button) findViewById(R.id.button_decrease_speed);
        Button mSkipForwardButton = (Button) findViewById(R.id.button_skip_forward);
        Button mSkipBackwardButton = (Button) findViewById(R.id.button_skip_backward);
        Button mVisualizeButton = (Button) findViewById(R.id.button_visualize);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);
        mScrollContainer = (ScrollView) findViewById(R.id.scroll_container);
        mBarVisualizer = (LineBarVisualizer) findViewById(R.id.barvisualizer);

        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int state = mPlayerAdapter.play();
                        if(state == 1){
                            mPlayButton.setBackgroundResource(R.drawable.play);
                        }else if(state == 2){
                            mPlayButton.setBackgroundResource(R.drawable.pause);
                        }

                    }
                });
        mUploadButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onUpload();
                    }
                });
//        mSetLoopButton.setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        mPlayerAdapter.setLoop(loopMode);
//                    }
//                });
        if(enableVisualize) {
            mVisualizeButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(mPlayerAdapter.isPlaying()) {
                                mBarVisualizer = (LineBarVisualizer) findViewById(R.id.barvisualizer);
                                mPlayerAdapter.visualize(mBarVisualizer);
                            }
                        }
                    }
            );
        }else{
            mVisualizeButton.setText("Visualize (Disabled)");
        }
        mIncreaseSpeedButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int new_speed = Math.round(100 * mPlayerAdapter.adjustSpeed(1));
                            curr_speed.setText("Current Speed: " + ((Integer) new_speed).toString() + "%");

                    }
                });
        mDecreaseSpeedButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int new_speed = Math.round(100 * mPlayerAdapter.adjustSpeed(-1));
                        curr_speed.setText("Current Speed: " + ((Integer) new_speed).toString() + "%");
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode == UPLOAD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                    Uri uploadedMusic = intent.getData();
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
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            onLogUpdated(String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted() {
        }

        @Override
        public void onLogUpdated(String message) {
            if (mTextDebug != null) {
                mTextDebug.append(message);
                mTextDebug.append("\n");
                // Moves the scrollContainer focus to the end.
                mScrollContainer.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                mScrollContainer.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
            }
        }
    }
}