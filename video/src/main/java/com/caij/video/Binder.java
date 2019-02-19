package com.caij.video;

import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import static android.view.View.VISIBLE;

public class Binder implements ExMediaPlayer.OnVideoSizeChangedListener, ExMediaPlayer.OnInfoListener, ExMediaPlayer.OnPreparedListener {

    private XMediaPlayer xMediaPlayer;
    private SimpleVideoView mSimpleVideoView;

    public Binder(XMediaPlayer mediaPlayer) {
        xMediaPlayer = mediaPlayer;
        xMediaPlayer.addOnVideoSizeChangedListener(this);
        xMediaPlayer.addOnInfoListener(this);
        xMediaPlayer.addOnPreparedListener(this);
    }

    public void binder(SimpleVideoView simpleVideoView) {
        View shutterView = simpleVideoView.getShutterView();
        if (shutterView != null) shutterView.setVisibility(View.VISIBLE);

        if (xMediaPlayer.isPlaying()) {
            xMediaPlayer.onVideoSizeChanged(xMediaPlayer, xMediaPlayer.getVideoWidth(), xMediaPlayer.getVideoHeight());
            if (shutterView != null) shutterView.setVisibility(View.GONE);
        }

        View displayView = simpleVideoView.getSurfaceView();
        mSimpleVideoView = simpleVideoView;
        if (displayView instanceof SurfaceView) {
            xMediaPlayer.setSurfaceView((SurfaceView) displayView);
        } else if (displayView instanceof TextureView) {
            xMediaPlayer.setTextureView((TextureView) displayView);
        }
    }

    @Override
    public boolean onInfo(ExMediaPlayer mp, int what, int extra) {
        if (what == ExMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            if (mSimpleVideoView != null) mSimpleVideoView.onRenderedFirstFrame();
        }
        return false;
    }

    @Override
    public void onVideoSizeChanged(ExMediaPlayer mp, int width, int height) {
        if (mSimpleVideoView != null) mSimpleVideoView.setAspectRatio(width * 1f / height);
    }

    @Override
    public void onPrepared(ExMediaPlayer mp) {
        //这个兼容部分版本问题
        if (mSimpleVideoView != null) mSimpleVideoView.onRenderedFirstFrame();
    }
}
