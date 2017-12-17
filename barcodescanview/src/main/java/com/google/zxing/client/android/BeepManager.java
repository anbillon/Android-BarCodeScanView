/*
 * Copyright (C) 2010 ZXing authors
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

package com.google.zxing.client.android;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;
import com.anbillon.barcodescanview.R;
import java.io.Closeable;
import java.io.IOException;

/**
 * Manages beeps and vibrations.
 */
public final class BeepManager implements MediaPlayer.OnErrorListener, Closeable {
  private static final String TAG = BeepManager.class.getSimpleName();

  private static final float BEEP_VOLUME = 0.10f;
  private static final long VIBRATE_DURATION = 200L;

  private final Context context;
  private MediaPlayer mediaPlayer;
  private boolean playBeepAndVibrate = true;

  public BeepManager(Context context) {
    this.context = context;
    this.mediaPlayer = null;
  }

  public synchronized void shouldPlayBeepAndVbirate(boolean flag) {
    playBeepAndVibrate = shouldBeep(context, flag);
  }

  public synchronized void playBeepSoundAndVibrate() {
    if (!playBeepAndVibrate) {
      return;
    }

    if (mediaPlayer == null) {
      mediaPlayer = buildMediaPlayer(context);
    }

    if (mediaPlayer != null) {
      mediaPlayer.start();
    }

    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    vibrator.vibrate(VIBRATE_DURATION);
  }

  private static boolean shouldBeep(Context context, boolean shouldPlayBeep) {
    if (shouldPlayBeep) {
      /* See if sound settings overrides this */
      AudioManager audioService = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
        shouldPlayBeep = false;
      }
    }

    return shouldPlayBeep;
  }

  private MediaPlayer buildMediaPlayer(Context context) {
    MediaPlayer mediaPlayer = new MediaPlayer();
    try {
      AssetFileDescriptor file = context.getResources().openRawResourceFd(R.raw.beep);
      try {
        mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(),
            file.getLength());
      } finally {
        file.close();
      }
      mediaPlayer.setOnErrorListener(this);
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mediaPlayer.setLooping(false);
      mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
      mediaPlayer.prepare();
      return mediaPlayer;
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      mediaPlayer.release();
      return null;
    }
  }

  @Override public synchronized boolean onError(MediaPlayer mp, int what, int extra) {
    if (what != MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
      close();
    }

    return true;
  }

  @Override public synchronized void close() {
    if (mediaPlayer != null) {
      mediaPlayer.release();
      mediaPlayer = null;
    }
  }
}
