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

import android.net.Uri;
import android.widget.TextView;

import com.chibde.visualizer.BarVisualizer;
import com.chibde.visualizer.LineBarVisualizer;

/**
 * Allows {@link MainActivity} to control media playback of {@link MediaPlayerHolder}.
 */
public interface PlayerAdapter {

    int getLoopStart();

    int getLoopEnd();

    int getSongLength();

    void loadMedia(Uri uri);

    void release();

    boolean isPlaying();

    boolean isInitialized();

    int play();

    void setLoop(int loopMode, TextView text_start, TextView text_end);

    float adjustSpeed(int crease);

    void skipForward();

    void skipBackward();

    void visualize(LineBarVisualizer visualizer);

    void stopVisualize(LineBarVisualizer visualizer);

    void initializeProgressCallback();

    void setDuration();

    void seekTo(int position);

    double[] getTime();
}
