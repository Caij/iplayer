package com.caij.video;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

public class OsExMediaPlayer implements ExMediaPlayer {

    private MediaPlayer mMediaPlayer;

    public OsExMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
    }


    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mMediaPlayer.setDataSource(context, uri);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mMediaPlayer.setDataSource(context, uri, headers);
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mMediaPlayer.setDataSource(path);
    }


    @Override
    public void prepareAsync() throws IllegalStateException {
        mMediaPlayer.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mMediaPlayer.start();
    }

    @Override
    public void stop() throws IllegalStateException {
        mMediaPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        mMediaPlayer.pause();
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        mMediaPlayer.seekTo(msec);
    }

    @Override
    public void reset() {
        mMediaPlayer.reset();
    }

    @Override
    public void release() {
        mMediaPlayer.setDisplay(null);
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    @Override
    public long getDuration() {
        return mMediaPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getVideoWidth() {
        return mMediaPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return mMediaPlayer.getVideoHeight();
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setSpeed(float speed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mMediaPlayer.setPlaybackParams(mMediaPlayer.getPlaybackParams().setSpeed(speed));
        }
    }

    @Override
    public boolean isLooping() {
        return mMediaPlayer.isLooping();
    }

    @Override
    public void setLooping(boolean looping) {
        mMediaPlayer.setLooping(looping);
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        mMediaPlayer.setDisplay(sh);
    }

    @Override
    public void setSurface(Surface surface) {
        mMediaPlayer.setSurface(surface);
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        mMediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        mMediaPlayer.setWakeMode(context, mode);
    }

    @Override
    public void setNextMediaPlayer(ExMediaPlayer nextMediaPlayer) throws UnsupportedOperationException {
//        mMediaPlayer.setNextMediaPlayer(nextMediaPlayer);
    }

    @Override
    public void setOnPreparedListener(final OnPreparedListener listener) {
        if (listener == null) {
            mMediaPlayer.setOnPreparedListener(null);
        } else {
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    listener.onPrepared(OsExMediaPlayer.this);
                }
            });
        }
    }

    @Override
    public void setOnCompletionListener(final OnCompletionListener listener) {
        if (listener == null){
            mMediaPlayer.setOnCompletionListener(null);
        } else {
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    listener.onCompletion(OsExMediaPlayer.this);
                }
            });
        }
    }

    @Override
    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
        if (listener == null){
            mMediaPlayer.setOnBufferingUpdateListener(null);
        } else {
            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    listener.onBufferingUpdate(OsExMediaPlayer.this, percent);
                }
            });
        }
    }

    @Override
    public void setOnSeekCompleteListener(final OnSeekCompleteListener listener) {
        if (listener == null){
            mMediaPlayer.setOnSeekCompleteListener(null);
        } else {
            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    listener.onSeekComplete(OsExMediaPlayer.this);
                }
            });
        }
    }

    @Override
    public void setOnVideoSizeChangedListener(final OnVideoSizeChangedListener listener) {
        if (listener == null){
            mMediaPlayer.setOnVideoSizeChangedListener(null);
        } else {
            mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    listener.onVideoSizeChanged(OsExMediaPlayer.this, width, height);
                }

            });
        }
    }

    @Override
    public void setOnErrorListener(final OnErrorListener listener) {
        if (listener == null){
            mMediaPlayer.setOnErrorListener(null);
        } else {
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return listener.onError(OsExMediaPlayer.this, what, extra);
                }
            });
        }
    }

    @Override
    public void setOnInfoListener(final OnInfoListener listener) {
        if (listener == null){
            mMediaPlayer.setOnInfoListener(null);
        } else {
            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    return listener.onInfo(OsExMediaPlayer.this, what, extra);
                }
            });
        }
    }
}
