package com.caij.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class XMediaPlayer implements ExMediaPlayer, ExMediaPlayer.OnInfoListener, ExMediaPlayer.OnPreparedListener,
        ExMediaPlayer.OnSeekCompleteListener, ExMediaPlayer.OnBufferingUpdateListener, ExMediaPlayer.OnCompletionListener,
        ExMediaPlayer.OnVideoSizeChangedListener, ExMediaPlayer.OnErrorListener {

    private static final String TAG = "XMediaPlayer";
    private ExMediaPlayer mExMediaPlayer;
    private TextureView textureView;
    private SurfaceHolder surfaceHolder;
    private ComponentListener componentListener;

    private Surface mSurface;
    private boolean ownsSurface;

    private List<OnInfoListener> infoListeners = new ArrayList<>();
    private List<ExMediaPlayer.OnPreparedListener> onPreparedListeners = new ArrayList<>();
    private List<ExMediaPlayer.OnSeekCompleteListener> onSeekCompleteListeners = new ArrayList<>();
    private List<ExMediaPlayer.OnBufferingUpdateListener> onBufferingUpdateListeners = new ArrayList<>();
    private List<ExMediaPlayer.OnCompletionListener> onCompletionListeners = new ArrayList<>();
    private List<ExMediaPlayer.OnVideoSizeChangedListener> onVideoSizeChangedListeners = new ArrayList<>();
    private List<ExMediaPlayer.OnErrorListener> onErrorListeners = new ArrayList<>();

    public XMediaPlayer(ExMediaPlayer exMediaPlayer) {
        mExMediaPlayer = exMediaPlayer;
        componentListener = new ComponentListener();
        exMediaPlayer.setOnInfoListener(this);
        exMediaPlayer.setOnPreparedListener(this);
        exMediaPlayer.setOnSeekCompleteListener(this);
        exMediaPlayer.setOnBufferingUpdateListener(this);
        exMediaPlayer.setOnCompletionListener(this);
        exMediaPlayer.setOnVideoSizeChangedListener(this);
        exMediaPlayer.setOnErrorListener(this);
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        setVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }

    public void setVideoSurfaceHolder(SurfaceHolder surfaceHolder) {
        removeSurfaceCallbacks();
        this.surfaceHolder = surfaceHolder;
        if (surfaceHolder == null) {
            setVideoSurfaceInternal(null, false);
        } else {
            surfaceHolder.addCallback(componentListener);
            Surface surface = surfaceHolder.getSurface();
            setVideoSurfaceInternal(surface != null && surface.isValid() ? surface : null, false);
        }
    }

    public void setTextureView(TextureView textureView) {
        removeSurfaceCallbacks();

        this.textureView = textureView;

        if (textureView == null) {
            setVideoSurfaceInternal(null, true);
        } else {
            this.textureView.setSurfaceTextureListener(componentListener);

            SurfaceTexture surfaceTexture = textureView.isAvailable() ? textureView.getSurfaceTexture()
                    : null;
            setVideoSurfaceInternal(surfaceTexture == null ? null : new Surface(surfaceTexture), true);
        }
    }

    public void clear(SurfaceView surfaceView) {
        if (surfaceHolder != null && surfaceView != null && surfaceView.getHolder() == surfaceHolder) {
            setSurfaceView((SurfaceView)null);
        }
    }

    public void clear(TextureView textureView) {
        if (textureView != null && this.textureView != null && textureView == this.textureView) {
            setTextureView((TextureView)null);
        }
    }

    public void clear() {
        clearListeners();
        if (textureView != null) {
            setTextureView((TextureView)null);
        } else if (surfaceHolder != null) {
            setSurfaceView(null);
        }
    }

    private void setVideoSurfaceInternal(Surface surface, boolean ownsSurface) {

        mExMediaPlayer.setSurface(surface);

        if (this.mSurface != null && this.mSurface != surface) {
            // We're replacing a surface. Block to ensure that it's not accessed after the method returns.

            // If we created the previous surface, we are responsible for releasing it.
            if (this.ownsSurface) {
                this.mSurface.release();
            }
        }
        this.mSurface = surface;
        this.ownsSurface = ownsSurface;

        Log.d(TAG, "setVideoSurfaceInternal: " + surface);
    }


    private void removeSurfaceCallbacks() {
        if (textureView != null) {
            if (textureView.getSurfaceTextureListener() != componentListener) {
                Log.w(TAG, "SurfaceTextureListener already unset or replaced.");
            } else {
                textureView.setSurfaceTextureListener(null);
            }
            textureView = null;
        }

        if (surfaceHolder != null) {
            surfaceHolder.removeCallback(componentListener);
            surfaceHolder = null;
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mExMediaPlayer.setDataSource(context, uri);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mExMediaPlayer.setDataSource(context, uri, headers);
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mExMediaPlayer.setDataSource(path);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        mExMediaPlayer.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mExMediaPlayer.start();
    }

    @Override
    public void stop() throws IllegalStateException {
        mExMediaPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        mExMediaPlayer.pause();
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        mExMediaPlayer.seekTo(msec);
    }

    private void clearListeners() {
        onPreparedListeners.clear();
        onSeekCompleteListeners.clear();
        onBufferingUpdateListeners.clear();
        onCompletionListeners.clear();
        onVideoSizeChangedListeners.clear();
        onErrorListeners.clear();
        infoListeners.clear();;
    }

    @Override
    public void reset() {
        mExMediaPlayer.reset();
    }

    public void release() {
        mExMediaPlayer.release();

        removeSurfaceCallbacks();

        if (mSurface != null && ownsSurface) {
            mSurface.release();
            Log.d(TAG, "mSurface.release()");
        }

       clearListeners();
    }

    @Override
    public long getDuration() {
        return mExMediaPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mExMediaPlayer.getCurrentPosition();
    }

    @Override
    public int getVideoWidth() {
        return mExMediaPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return mExMediaPlayer.getVideoHeight();
    }

    @Override
    public boolean isPlaying() {
        return mExMediaPlayer.isPlaying();
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mExMediaPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void setSpeed(float rate) {
        mExMediaPlayer.setSpeed(rate);
    }

    @Override
    public boolean isLooping() {
        return mExMediaPlayer.isLooping();
    }

    @Override
    public void setLooping(boolean looping) {
        mExMediaPlayer.setLooping(looping);
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        throw new IllegalStateException("不允许调用, 请使用setSurfaceView or setTextureView");
    }

    @Override
    public void setSurface(Surface surface) {
        throw new IllegalStateException("不允许调用, 请使用setSurfaceView or setTextureView");
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        mExMediaPlayer.setScreenOnWhilePlaying(screenOn);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        mExMediaPlayer.setWakeMode(context, mode);
    }

    @Override
    public void setNextMediaPlayer(ExMediaPlayer nextMediaPlayer) throws UnsupportedOperationException {
        mExMediaPlayer.setNextMediaPlayer(nextMediaPlayer);
    }

    @Deprecated
    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        if (listener != null) addOnPreparedListener(listener);
    }

    @Deprecated
    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        throw new IllegalStateException("不允许调用, 请使用addOnCompletionListener");
//        if (listener != null) addOnCompletionListener(listener);
    }

    @Deprecated
    @Override
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        throw new IllegalStateException("不允许调用, 请使用addOnBufferingUpdateListener");
//        if (listener != null) addOnBufferingUpdateListener(listener);
    }

    @Deprecated
    @Override
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        throw new IllegalStateException("不允许调用, 请使用addOnSeekCompleteListener");
//        if (listener != null) addOnSeekCompleteListener(listener);
    }

    @Deprecated
    @Override
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        throw new IllegalStateException("不允许调用, 请使用addOnVideoSizeChangedListener");
//        if (listener != null) addOnVideoSizeChangedListener(listener);
    }

    @Deprecated
    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        throw new IllegalStateException("不允许调用, 请使用addOnErrorListener");
//        if (listener != null) addOnErrorListener(listener);
    }

    @Deprecated
    @Override
    public void setOnInfoListener(OnInfoListener listener) {
        throw new IllegalStateException("不允许调用, 请使用addOnInfoListener");
//        if (listener != null) addOnInfoListener(listener);
    }

    public class ComponentListener implements TextureView.SurfaceTextureListener, SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            setVideoSurfaceInternal(holder.getSurface(), false);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            setVideoSurfaceInternal(null, false);
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setVideoSurfaceInternal(new Surface(surface), true);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            setVideoSurfaceInternal(null, true);
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

    @Override
    public boolean onInfo(ExMediaPlayer mp, int what, int extra) {
        for (ExMediaPlayer.OnInfoListener onInfoListener : infoListeners) {
            onInfoListener.onInfo(mp, what, extra);
        }
        return true;
    }

    @Override
    public void onPrepared(ExMediaPlayer mp) {
        for (ExMediaPlayer.OnPreparedListener onPreparedListener : onPreparedListeners) {
            onPreparedListener.onPrepared(mp);
        }
    }

    @Override
    public void onSeekComplete(ExMediaPlayer mp) {
        for (ExMediaPlayer.OnSeekCompleteListener onSeekCompleteListener : onSeekCompleteListeners) {
            onSeekCompleteListener.onSeekComplete(mp);
        }
    }

    @Override
    public void onBufferingUpdate(ExMediaPlayer mp, int percent) {
        for (ExMediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener : onBufferingUpdateListeners) {
            onBufferingUpdateListener.onBufferingUpdate(mp, percent);
        }
    }

    @Override
    public void onCompletion(ExMediaPlayer mp) {
        for (ExMediaPlayer.OnCompletionListener onCompletionListener : onCompletionListeners) {
            onCompletionListener.onCompletion(mp);
        }
    }

    @Override
    public void onVideoSizeChanged(ExMediaPlayer mp, int width, int height) {
        for (ExMediaPlayer.OnVideoSizeChangedListener onVideoSizeChangedListener : onVideoSizeChangedListeners) {
            onVideoSizeChangedListener.onVideoSizeChanged(mp, width, height);
        }
    }

    @Override
    public boolean onError(ExMediaPlayer mp, int what, int extra) {
        for (ExMediaPlayer.OnErrorListener onErrorListener : onErrorListeners) {
            onErrorListener.onError(mp, what, extra);
        }
        return true;
    }

    public void addOnPreparedListener(ExMediaPlayer.OnPreparedListener listener) {
        onPreparedListeners.add(listener);
    }

    public void addOnCompletionListener(ExMediaPlayer.OnCompletionListener listener) {
        onCompletionListeners.add(listener);
    }

    public void addOnBufferingUpdateListener(
            ExMediaPlayer.OnBufferingUpdateListener listener) {
        onBufferingUpdateListeners.add(listener);
    }

    public void addOnSeekCompleteListener(
            ExMediaPlayer.OnSeekCompleteListener listener) {
        onSeekCompleteListeners.add(listener);
    }

    public void addOnVideoSizeChangedListener(
            ExMediaPlayer.OnVideoSizeChangedListener listener) {
        onVideoSizeChangedListeners.add(listener);
    }

    public void addOnErrorListener(ExMediaPlayer.OnErrorListener listener) {
        onErrorListeners.add(listener);
    }

    public void addOnInfoListener(ExMediaPlayer.OnInfoListener listener) {
        infoListeners.add(listener);
    }


    public void removeOnPreparedListener(ExMediaPlayer.OnPreparedListener listener) {
        onPreparedListeners.remove(listener);
    }

    public void removeOnCompletionListener(ExMediaPlayer.OnCompletionListener listener) {
        onCompletionListeners.remove(listener);
    }

    public void removeOnBufferingUpdateListener(
            ExMediaPlayer.OnBufferingUpdateListener listener) {
        onBufferingUpdateListeners.remove(listener);
    }

    public void removeOnSeekCompleteListener(
            ExMediaPlayer.OnSeekCompleteListener listener) {
        onSeekCompleteListeners.remove(listener);
    }

    public void removeOnVideoSizeChangedListener(
            ExMediaPlayer.OnVideoSizeChangedListener listener) {
        onVideoSizeChangedListeners.remove(listener);
    }

    public void removeOnErrorListener(ExMediaPlayer.OnErrorListener listener) {
        onErrorListeners.remove(listener);
    }

    public void removeOnInfoListener(ExMediaPlayer.OnInfoListener listener) {
        infoListeners.remove(listener);
    }
}
