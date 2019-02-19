package com.caij.ijkplayer;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.caij.video.ExMediaPlayer;

import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IJKMediaPlayer implements ExMediaPlayer {

    private final IjkMediaPlayer ijkMediaPlayer;

    public IJKMediaPlayer(Context context){
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        ijkMediaPlayer = new IjkMediaPlayer();
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        ijkMediaPlayer.setDataSource(context, uri);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        ijkMediaPlayer.setDataSource(context, uri, headers);
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        ijkMediaPlayer.setDataSource(path);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        ijkMediaPlayer.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        ijkMediaPlayer.start();
    }

    @Override
    public void stop() throws IllegalStateException {
        ijkMediaPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        ijkMediaPlayer.pause();
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        ijkMediaPlayer.seekTo(msec);
    }

    @Override
    public void reset() {
        ijkMediaPlayer.reset();
    }

    @Override
    public void release() {
        ijkMediaPlayer.release();
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    public long getDuration() {
        return ijkMediaPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return ijkMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getVideoWidth() {
        return ijkMediaPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return ijkMediaPlayer.getVideoHeight();
    }

    @Override
    public boolean isPlaying() {
        return ijkMediaPlayer.isPlaying();
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        ijkMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setSpeed(float rate) {
        ijkMediaPlayer.setSpeed(rate);
    }

    @Override
    public boolean isLooping() {
        return ijkMediaPlayer.isLooping();
    }

    @Override
    public void setLooping(boolean looping) {
        ijkMediaPlayer.setLooping(looping);
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        ijkMediaPlayer.setDisplay(sh);
    }

    @Override
    public void setSurface(Surface surface) {
        ijkMediaPlayer.setSurface(surface);
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        ijkMediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        ijkMediaPlayer.setWakeMode(context, mode);
    }

    @Override
    public void setNextMediaPlayer(ExMediaPlayer nextMediaPlayer) throws UnsupportedOperationException {
//        ijkMediaPlayer.setn
    }

    @Override
    public void setOnPreparedListener(final OnPreparedListener listener) {
        if (listener == null) {
            ijkMediaPlayer.setOnPreparedListener(null);
        } else {
            ijkMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(IMediaPlayer iMediaPlayer) {
                    listener.onPrepared(IJKMediaPlayer.this);
                }
            });
        }
    }

    @Override
    public void setOnCompletionListener(final OnCompletionListener listener) {
        if (listener == null){
            ijkMediaPlayer.setOnCompletionListener(null);
        } else {
            ijkMediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IMediaPlayer iMediaPlayer) {
                    listener.onCompletion(IJKMediaPlayer.this);
                }
            });
        }
    }

    @Override
    public void setOnBufferingUpdateListener(final OnBufferingUpdateListener listener) {
        if (listener == null){
            ijkMediaPlayer.setOnBufferingUpdateListener(null);
        } else {
            ijkMediaPlayer.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
                    listener.onBufferingUpdate(IJKMediaPlayer.this, i);
                }
            });
        }
    }

    @Override
    public void setOnSeekCompleteListener(final OnSeekCompleteListener listener) {
        if (listener == null){
            ijkMediaPlayer.setOnSeekCompleteListener(null);
        } else {
            ijkMediaPlayer.setOnSeekCompleteListener(new IMediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(IMediaPlayer iMediaPlayer) {
                    listener.onSeekComplete(IJKMediaPlayer.this);
                }
            });
        }
    }

    @Override
    public void setOnVideoSizeChangedListener(final OnVideoSizeChangedListener listener) {
        if (listener == null){
            ijkMediaPlayer.setOnVideoSizeChangedListener(null);
        } else {
            ijkMediaPlayer.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int i, int i1, int i2, int i3) {
                    listener.onVideoSizeChanged(IJKMediaPlayer.this, i, i1);
                }
            });
        }
    }

    @Override
    public void setOnErrorListener(final OnErrorListener listener) {
        if (listener == null){
            ijkMediaPlayer.setOnErrorListener(null);
        } else {
            ijkMediaPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                    return listener.onError(IJKMediaPlayer.this, i, i1);
                }
            });
        }
    }

    @Override
    public void setOnInfoListener(final OnInfoListener listener) {
        if (listener == null){
            ijkMediaPlayer.setOnInfoListener(null);
        } else {
            ijkMediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                    return listener.onInfo(IJKMediaPlayer.this, i, i1);
                }
            });
        }
    }
}
