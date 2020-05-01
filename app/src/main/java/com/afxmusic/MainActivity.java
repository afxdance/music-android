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

package com.afxmusic;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chibde.visualizer.LineBarVisualizer;

import android.view.View.OnTouchListener;
import android.view.MotionEvent;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Allows playback of a single MP3 file via the UI. It contains a {@link MediaPlayerHolder}
 * which implements the {@link PlayerAdapter} interface that the activity uses to control
 * audio playback.
 */
public final class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    public static final String TAG2 = "SHARING";

    public static final int UPLOAD_REQUEST_CODE = 1;


    private SeekBar mSeekbarAudio;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;

    private Uri uri;


    private AlertDialog.Builder mBuilder;
    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private Context mContext = this;
    private Activity mActivity = this;
    private LineBarVisualizer mBarVisualizer;
    private boolean enableVisualize = false;
    private boolean isVisualizing = false;
    private TextView curr_speed;
    private TextView curr_time;
    private TextView total_time;

    private int loopMode = -1;

    private class GetMusicFromIntent extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String ... params) {
            String decodedData = params[0];
            String jsonText = "";
            try
            {
                // Log.d(TAG2, decodedData);
                URL url = new URL(decodedData);
                // Log.d(TAG2, url.toString());

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                int code = urlConnection.getResponseCode();
                String codetostring = String.valueOf(code);
                codetostring += ": connection secured!";
                // Log.d(TAG2, codetostring);
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                String inputLine;
                StringBuilder jsonTextBuilder = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    jsonTextBuilder.append(inputLine);
                }
                in.close();
                jsonText = jsonTextBuilder.toString();


            }
            catch (Exception e)
            {
                // Log.d(TAG2, e.toString());
            }
            return jsonText;
        }

        @Override
        protected void onPostExecute(String result) {
            // Log.d(TAG2, result);
            try {
                JSONObject obj = new JSONObject(result);

                String music = obj.getString("music");

                // Log.d(TAG2, "music string is this: " + music);
                String prebase64 = "data:audio/mp3;base64,";
                String base64stuff = music.substring(prebase64.length());
                // Log.d(TAG2, "decoded base64 stuff: " + base64stuff);

                byte[] decodedString = Base64.decode(base64stuff, Base64.DEFAULT);

                // create temp file that will hold byte array
                File tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
                tempMp3.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(tempMp3);
                fos.write(decodedString);
                fos.close();

                // In case you run into issues with threading consider new instance like:
                // MediaPlayer mediaPlayer = new MediaPlayer();

                // Tried passing path directly, but kept getting
                // "Prepare failed.: status=0x1"
                // so using file descriptor instead
                FileInputStream fis = new FileInputStream(tempMp3);

                mPlayerAdapter.loadMedia(fis.getFD());
                loopMode = 0;
                
            }
            catch (Exception e)
            {
                // Log.d(TAG, e.toString());
            }


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        checkPermission();

        //String action = intent.getAction();
        Uri uri = this.getIntent().getData();
        if (uri != null) {
            String encodedData = uri.getEncodedQuery();
            String decodedData = Uri.decode(encodedData);
            decodedData = decodedData.substring(9);
            new GetMusicFromIntent().execute(decodedData);
        }

        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
        // Log.d(TAG2, "UWU");
        // Log.d(TAG, "onCreate: finished");

        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            askPermission();
            initializeUI();
        } else {
            // Permission has already been granted
            enableVisualize = true;
            initializeUI();
        }

    }

    public void askPermission() {

        mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("Music Visualizer");
        mBuilder.setMessage("Audio Recording is required for our visualizer to function properly. Please allow this permission. Thank you!");
        mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                // Permission is not granted
                // Here, thisActivity is the current activity

                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_RECORD_AUDIO is an
                // app-defined int constant. The callback method gets the
                // result of the request.


            }
        });
        mBuilder.create();
        mBuilder.show();

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
                    checkTurnOnVisualize();
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Log.d(TAG, "onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            // Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            //mPlayerAdapter.release();
            // Log.d(TAG, "onStop: release MediaPlayer");
        }
    }

    private void initializeUI() {
        setContentView(R.layout.activity_main);
        curr_speed = (TextView) findViewById(R.id.speed);
        curr_time = (TextView) findViewById(R.id.curr_time);
        total_time = (TextView) findViewById(R.id.total_time);

        Toast mToast = Toast.makeText(this, "Welcome to the slow.afx.dance mobile app!", Toast.LENGTH_LONG);
        mToast.setGravity(Gravity.TOP, 0, 150);
        mToast.show();
        final ImageButton mPlayButton = (ImageButton) findViewById(R.id.button_play);
        Button mUploadButton = (Button) findViewById(R.id.button_upload);
        final Button mSetLoopButton = (Button) findViewById(R.id.button_set_loop);
        final TextView mLoopStartText = (TextView) findViewById(R.id.text_loop_start);
        final TextView mLoopEndText = (TextView) findViewById(R.id.text_loop_end);
        ImageButton mIncreaseSpeedButton = (ImageButton) findViewById(R.id.button_increase_speed);
        ImageButton mDecreaseSpeedButton = (ImageButton) findViewById(R.id.button_decrease_speed);
        ImageButton mSkipForwardButton = (ImageButton) findViewById(R.id.button_skip_forward);
        ImageButton mSkipBackwardButton = (ImageButton) findViewById(R.id.button_skip_backward);
        final ImageButton mVisualizeButton = (ImageButton) findViewById(R.id.button_visualize);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);

        final View mStartMarker = findViewById(R.id.loop_start_marker);
        final View mBeforeLoopBlank = findViewById(R.id.before_loop_blank);
        final View mBetweenLoopBlank = findViewById(R.id.in_between_loop_blank);
        final View mEndMarker = findViewById(R.id.loop_end_marker);
        final View mAfterLoopBlank = findViewById(R.id.after_loop_blank);
        mBarVisualizer = (LineBarVisualizer) findViewById(R.id.barvisualizer);

        initializeSeekbar();
        initializePlaybackController();

        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int state = mPlayerAdapter.play();
                        if (state == 1) {
                            mPlayButton.setBackgroundResource(R.drawable.play);
                        } else if (state == 2) {
                            mPlayButton.setBackgroundResource(R.drawable.pause);
                        }

                    }
                });
        mUploadButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onUpload();
                        startSeekbar();

                    }
                });

        mVisualizeButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!enableVisualize) {
                            askPermission();
                        } else {
                            checkTurnOnVisualize();
                        }
                    }
                }
        );
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
                        int markerWidth = 10;

                        // Total weight of before, between and end sum to 1
                        // Individual weights are calculated considering a margin on both sides
                        // of the seekBar.
                        // TODO - Different displays may have different margin sizes relative to SeekBar
                        float margin = 0.04F;   // I think this is percentage of screen width?
                        float seekBarWeight = 1 - (2 * margin);     // seekBar's % of screen width
                        float beforeWeight = margin + (loopStart / songLength) * seekBarWeight;

                        loopMode++;     // switch to next mode

                        int mode = loopMode % 3;
                        if (mode == 0) {
                            mStartMarker.setVisibility(View.INVISIBLE);
                            mEndMarker.setVisibility(View.INVISIBLE);
                            LinearLayout.LayoutParams beforeBlankParams = new LinearLayout.LayoutParams(0, 0, 0);
                            mBeforeLoopBlank.setLayoutParams(beforeBlankParams);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(markerWidth, 100, 0);
                            mStartMarker.setLayoutParams(params);

                            LinearLayout.LayoutParams betweenBlankParams = new LinearLayout.LayoutParams(0, 0, 0);
                            mBetweenLoopBlank.setLayoutParams(betweenBlankParams);

                            LinearLayout.LayoutParams afterEndBlankParams = new LinearLayout.LayoutParams(0, 0, 0);
                            mAfterLoopBlank.setLayoutParams(afterEndBlankParams);

                            mSetLoopButton.setText("Set loop start");
                        } else if (mode == 1) {
                            LinearLayout.LayoutParams beforeBlankParams = new LinearLayout.LayoutParams(0, 0, beforeWeight);
                            mBeforeLoopBlank.setLayoutParams(beforeBlankParams);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(markerWidth, 100, 0);
                            mStartMarker.setLayoutParams(params);
                            LinearLayout.LayoutParams afterEndBlankParams = new LinearLayout.LayoutParams(0, 0, 1 - beforeWeight);
                            mAfterLoopBlank.setLayoutParams(afterEndBlankParams);

                            mStartMarker.setVisibility(View.VISIBLE);
                            mSetLoopButton.setText("Set loop end");
                        } else if (mode == 2) {

                            LinearLayout.LayoutParams beforeBlankParams = new LinearLayout.LayoutParams(0, 0, beforeWeight);
                            mBeforeLoopBlank.setLayoutParams(beforeBlankParams);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(markerWidth, 100, 0);
                            mStartMarker.setLayoutParams(params);

                            float betweenWeight = ((loopEnd - loopStart)/songLength) * seekBarWeight;
                            LinearLayout.LayoutParams betweenBlankParams = new LinearLayout.LayoutParams(0, 100, betweenWeight);
                            mBetweenLoopBlank.setLayoutParams(betweenBlankParams);

                            LinearLayout.LayoutParams afterEndBlankParams = new LinearLayout.LayoutParams(0, 0, 1 - beforeWeight - betweenWeight);
                            mAfterLoopBlank.setLayoutParams(afterEndBlankParams);

                            LinearLayout.LayoutParams endMarkerParams = new LinearLayout.LayoutParams(markerWidth, 100, 0);
                            mEndMarker.setLayoutParams(endMarkerParams);
                            mEndMarker.setVisibility(View.VISIBLE);

                            mSetLoopButton.setText("Clear loop");
                        }
                    }
                });
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

    private void checkTurnOnVisualize() {
            if (mPlayerAdapter.isInitialized()) {
                mBarVisualizer = (LineBarVisualizer) findViewById(R.id.barvisualizer);
                if (!isVisualizing) {
                    mPlayerAdapter.visualize(mBarVisualizer);
                    isVisualizing = true;
                } else {
                    mPlayerAdapter.stopVisualize(mBarVisualizer);
                    isVisualizing = false;
                }
            }
    }
    private void onUpload() {
        Intent myIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
        myIntent.setType("audio/*");
        startActivityForResult(myIntent, UPLOAD_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == UPLOAD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                final ImageButton mPlayButton = (ImageButton) findViewById(R.id.button_play);
                mPlayButton.setBackgroundResource(R.drawable.play);
                Uri uploadedMusic = intent.getData();
                mPlayerAdapter.loadMedia(uploadedMusic);
//                mBarVisualizer = new LineBarVisualizer();
                mPlayerAdapter.stopVisualize(mBarVisualizer);
                isVisualizing = false;
//                checkTurnOnVisualize();
//                initializeUI();
                loopMode = 0;
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);

    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        // Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        // Log.d(TAG, "initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        mSeekbarAudio.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
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
                        curr_time.setText("" + MediaPlayerHolder.convertToTime(progress) + "/");
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    private void startSeekbar() {
        mSeekbarAudio.setOnTouchListener(null);
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);
            total_time.setText("" + MediaPlayerHolder.convertToTime(duration));
            // Log.d(TAG, String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
                // Log.d(TAG, String.format("setPlaybackPosition: setProgress(%d)", position));
            }
        }

        @Override
        public void onStateChanged(@State int state) {
        }

    }
}