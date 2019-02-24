package com.caij.exoplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.caij.video.ExMediaPlayer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExoMediaPlayer implements ExMediaPlayer {

    private static final String TAG = "ExoMediaPlayer";

    private static final int PLAYER_STATUS_IDLE = 1;
    private static final int PLAYER_STATUS_INITIALIZIZED = 2;
    private static final int PLAYER_STATUS_PREPARING  = 3;
    private static final int PLAYER_STATUS_PREPARED  = 4;
    private static final int PLAYER_STATUS_END = 5;

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final int BUFFER_REPEAT_DELAY = 1000;

    private static final Map<String, String> mCacheUrls = new ConcurrentHashMap<>();

    private ThreadPoolExecutor mThreadPoolExecutor;

    private SimpleExoPlayer mExoPlayer;
    private Context mAppContext;

    private int mVideoWidth;
    private int mVideoHeight;

    private boolean isLooping = false;

    private StateStore mStateStore;

    private OnPreparedListener mOnPreparedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
    private Player.EventListener mExo2EventListener;
    private ExoVideoListener mExoVideoListener;

    private Repeater mBufferUpdateRepeater;

    private int mPlayerStatus = PLAYER_STATUS_IDLE;

    private Uri mUri;
    private Map<String, String> mHeaders;

    private AsyncTask mMediaAsyncTask;

    public ExoMediaPlayer(Context context) {
        mAppContext = context.getApplicationContext();
        TrackSelection.Factory selectionFactory = new AdaptiveTrackSelection.Factory(new DefaultBandwidthMeter());
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(selectionFactory);
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(mAppContext, trackSelector);

        mExoVideoListener = new ExoVideoListener();
        mExoPlayer.addVideoListener(mExoVideoListener);

        mExo2EventListener = new Exo2EventListener();
        mExoPlayer.addListener(mExo2EventListener);

        Handler mMainHandler = new Handler();

        mBufferUpdateRepeater = new Repeater(mMainHandler);
        mBufferUpdateRepeater.setRepeaterDelay(BUFFER_REPEAT_DELAY);
        mBufferUpdateRepeater.setRepeatListener(new Repeater.RepeatListener() {
            @Override
            public void onUpdate() {
                if (mExoPlayer != null) {
                    int state = mExoPlayer.getPlaybackState();
                    switch (state) {
                        case Player.STATE_IDLE:
                        case Player.STATE_ENDED:
                            setBufferRepeaterStarted(false);
                            break;
                        case Player.STATE_READY:
                        case Player.STATE_BUFFERING:
                            notifyOnBufferingUpdate(getBufferedPercentage());
                            break;
                    }
                }
            }
        });

        mStateStore = new StateStore();
    }

    private void initThreadPoolExecutor() {
        if (mThreadPoolExecutor == null) {
            mThreadPoolExecutor = new ThreadPoolExecutor(0, 1,
                    60 * 1000L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>());
        }
    }

    @Override
    public synchronized void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        checkIdlePlayerStatus();
        mUri = uri;
        mHeaders = null;
        mPlayerStatus = PLAYER_STATUS_INITIALIZIZED;
    }

    @Override
    public synchronized void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        checkIdlePlayerStatus();
        mUri = uri;
        mHeaders = headers;
        mPlayerStatus = PLAYER_STATUS_INITIALIZIZED;
    }

    @Override
    public synchronized void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        checkIdlePlayerStatus();
        mUri =  Uri.parse(path);
        mHeaders = null;
        mPlayerStatus = PLAYER_STATUS_INITIALIZIZED;
    }

    private void checkIdlePlayerStatus() {
        if (mPlayerStatus != PLAYER_STATUS_IDLE) {
            throw new IllegalStateException("没有重置播放器， 重新set url");
        }
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public synchronized void prepareAsync() throws IllegalStateException {
        Log.v(TAG, "prepareAsync");
        mExoPlayer.setPlayWhenReady(false);

        if (mMediaAsyncTask != null) {
            mMediaAsyncTask.cancel(true);
        }

        if (mUri != null) {
            MediaSource mediaSource = buildMediaSource(mAppContext, mUri, mHeaders, null);
            if (mediaSource != null) {
                mExoPlayer.prepare(mediaSource);
            } else {
                final String urlstr = mUri.toString();
                String realUrl = mCacheUrls.get(urlstr);
                if (!TextUtils.isEmpty(realUrl)) {
                    mediaSource = buildMediaSource(mAppContext, mUri, mHeaders, Uri.parse(realUrl));
                    if (mediaSource == null) {
                        mediaSource = buildDefaultMediaSource(mAppContext, mUri, mHeaders);
                    }
                    mExoPlayer.prepare(mediaSource);
                } else {
                    initThreadPoolExecutor();
                    mMediaAsyncTask = new AsyncTask<Void, Void, String>(){
                        @Override
                        protected String doInBackground(Void... voids) {
                            HttpURLConnection conn = null;
                            try {
                                URL url = new URL(urlstr);
                                conn = (HttpURLConnection) url.openConnection();
                                conn.getResponseCode();
                                return conn.getURL().toString();
                            } catch (Exception e) {
                                Log.d(TAG, "get video url Exception " + e.getMessage());
                            } finally {
                                if(conn != null)  conn.disconnect();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(String realUrl) {
                            if (realUrl != null) {
                                MediaSource mediaSource = buildMediaSource(mAppContext, mUri, mHeaders, Uri.parse(realUrl));
                                if (mediaSource == null) {
                                    mediaSource = buildDefaultMediaSource(mAppContext, mUri, mHeaders);
                                }
                                mExoPlayer.prepare(mediaSource);
                                mCacheUrls.put(urlstr, realUrl);
                            } else {
                                MediaSource mediaSource = buildDefaultMediaSource(mAppContext, mUri, mHeaders);
                                mExoPlayer.prepare(mediaSource);
                            }
                        }

                        @Override
                        protected void onCancelled(String realUrl) {
                            super.onCancelled(realUrl);
                            if (!TextUtils.isEmpty(realUrl)) {
                                mCacheUrls.put(urlstr, realUrl);
                            }
                        }
                    }.executeOnExecutor(mThreadPoolExecutor);
                }
            }
        }
        mPlayerStatus = PLAYER_STATUS_PREPARING;
    }

    @Override
    public void start() throws IllegalStateException {
        if (mExoPlayer == null) { return; }
        mExoPlayer.setPlayWhenReady(true);

//        if (!mFirstFrameDecodedEventSent &&
//                mFirstFrameDecoded) {
//            notifyOnInfo(MEDIA_INFO_VIDEO_RENDERING_START, 0);
//            mFirstFrameDecodedEventSent = true;
//        }
    }

    @Override
    public synchronized void stop() throws IllegalStateException {
        if (mExoPlayer == null) {
            return;
        }

        if (isPlaying()) {
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayer.stop();
        }
    }

    @Override
    public void pause() throws IllegalStateException {
        if (mExoPlayer == null) { return; }
        mExoPlayer.setPlayWhenReady(false);
    }

    @Override
    public void seekTo(int msec) throws IllegalStateException {
        if (mExoPlayer == null) {
            return;
        }
        mExoPlayer.seekTo(msec);
    }

    @Override
    public synchronized void reset() {
        Log.v(TAG, "reset");
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(false);
            mExoPlayer.stop(true);

            mStateStore.reset();

            isLooping = false;

            setBufferRepeaterStarted(false);
        }

        if (mMediaAsyncTask != null) mMediaAsyncTask.cancel(true);
        mPlayerStatus = PLAYER_STATUS_IDLE;
    }

    @Override
    public synchronized void release() {
        if (mExoPlayer != null) {
            mExoPlayer.removeListener(mExo2EventListener);
            mExoPlayer.removeVideoListener(mExoVideoListener);
            mExoPlayer.release();

            mExoPlayer = null;
            mExo2EventListener = null;


            setBufferRepeaterStarted(false);

            mOnPreparedListener = null;
            mOnCompletionListener = null;
            mOnBufferingUpdateListener = null;
            mOnSeekCompleteListener = null;
            mOnVideoSizeChangedListener = null;
            mOnErrorListener = null;
            mOnInfoListener = null;
        }

        if (mMediaAsyncTask != null) mMediaAsyncTask.cancel(true);
        if (mThreadPoolExecutor != null) mThreadPoolExecutor.shutdown();
        mPlayerStatus = PLAYER_STATUS_END;
    }

    @Override
    public long getDuration() {
        if (mExoPlayer == null)
            return 0;
        return mExoPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        if (mExoPlayer == null)
            return 0;
        return mExoPlayer.getCurrentPosition();
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public boolean isPlaying() {
        if (mExoPlayer == null)
            return false;
        int state = mExoPlayer.getPlaybackState();
        switch (state) {
            case Player.STATE_READY:
            case Player.STATE_BUFFERING:
                return mExoPlayer.getPlayWhenReady();
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
            default:
                return false;
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        mExoPlayer.setVolume(leftVolume);
    }

    @Override
    public void setSpeed(float rate) {
        PlaybackParameters params = new PlaybackParameters(rate);
        mExoPlayer.setPlaybackParameters(params);
    }

    @Override
    public boolean isLooping() {
        return isLooping;
    }

    @Override
    public void setLooping(boolean looping) {
        if (mExoPlayer != null) {
            mExoPlayer.setRepeatMode(looping ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
        }
        isLooping = looping;
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        if (sh == null) {
            setSurface(null);
        } else {
            setSurface(sh.getSurface());
        }
    }

    @Override
    public void setSurface(Surface surface) {
        if (mExoPlayer != null) mExoPlayer.setVideoSurface(surface);
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        // TODO: 2018/7/9
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        // TODO: 2018/7/9
    }

    @Override
    public void setNextMediaPlayer(ExMediaPlayer nextMediaPlayer) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("setNextMediaPlayer is not supported by ");
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    @Override
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    @Override
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
        setBufferRepeaterStarted(listener != null);
    }

    @Override
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    @Override
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    @Override
    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    private boolean notifyOnInfo(int what, int extra) {
        return mOnInfoListener != null && mOnInfoListener.onInfo(this, what, extra);
    }

    private boolean notifyOnError(int what, int extra) {
        Log.d(TAG, "notifyOnError [" + what + "," + extra + "]");
        return mOnErrorListener != null && mOnErrorListener.onError(this, what, extra);
    }

    /**
     * @param context
     * @param uri
     * @return
     */
    private static MediaSource buildMediaSource(Context context, Uri uri, Map<String, String> heads, Uri overrideUri) {
        @C.ContentType int type = Util.inferContentType(overrideUri == null ? uri : overrideUri);
        switch (type) {
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(buildHttpDataSourceFactory(context, BANDWIDTH_METER, heads))
                        .createMediaSource(uri);
            case C.TYPE_OTHER:
                String path = uri.getPath();
                if (!TextUtils.isEmpty(path)) {
                    if (path.endsWith(".mp4") || path.endsWith(".MP4")) {
                        return new ExtractorMediaSource.Factory(buildHttpDataSourceFactory(context, BANDWIDTH_METER, heads))
                                .createMediaSource(uri);
                    } else if (path.endsWith(".m3u8") || path.endsWith(".M3U8")) {
                        return new HlsMediaSource.Factory(buildHttpDataSourceFactory(context, BANDWIDTH_METER, heads))
                                .createMediaSource(uri);
                    }
                }
                break;
            case C.TYPE_DASH:
                Log.d(TAG, "build source TYPE_DASH " + uri.toString() + "  " + (overrideUri != null ? overrideUri.toString() : ""));
                throw new IllegalArgumentException("不支持这种类型，请扩展");
            case C.TYPE_SS:
                throw new IllegalArgumentException("不支持这种类型，请扩展");
        }
        return null;
    }

    private static MediaSource buildDefaultMediaSource(Context context, Uri uri, Map<String, String> heads) {
        return new ExtractorMediaSource.Factory(buildHttpDataSourceFactory(context, BANDWIDTH_METER, heads))
                .createMediaSource(uri);
    }

    private static DataSource.Factory buildHttpDataSourceFactory(Context context, DefaultBandwidthMeter bandwidthMeter, Map<String, String> heads) {
        DefaultHttpDataSourceFactory defaultHttpDataSourceFactory = new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "ExoPlayer"), bandwidthMeter,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true);
        if (heads != null) {
            defaultHttpDataSourceFactory.getDefaultRequestProperties().set(heads);
        }
        return new DefaultDataSourceFactory(context, null, defaultHttpDataSourceFactory);
    }

    private boolean isPlayerRunning() {
        return mPlayerStatus != PLAYER_STATUS_IDLE && mPlayerStatus != PLAYER_STATUS_END;
    }

    class ExoVideoListener implements VideoListener {
        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            if (isPlayerRunning()) {
                mVideoWidth = width;
                mVideoHeight = height;
                notifyOnVideoSizeChanged(width, height, 1, 1);
                if (unappliedRotationDegrees > 0) {
                    notifyOnInfo(MEDIA_INFO_VIDEO_ROTATION_CHANGED, unappliedRotationDegrees);
                }
            }
        }

        @Override
        public void onRenderedFirstFrame() {
            if (isPlayerRunning() && mExoPlayer != null) {
                notifyOnInfo(MEDIA_INFO_VIDEO_RENDERING_START, 0);
            }
        }
    };

    class Exo2EventListener implements Player.EventListener {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (isPlayerRunning()) {
                reportPlayerState();
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            if (mExoPlayer != null) { // cz no error state
                setBufferRepeaterStarted(false);
            }
            if (error != null && isPlayerRunning()) {
                Throwable cause = error.getCause();
                if (cause != null) {
                    if (cause instanceof HttpDataSource.HttpDataSourceException) {
                        if (cause.toString().contains("Unable to connect")) {
                            boolean hasNetwork = ExoMediaPlayerUtils.isNetworkAvailable(mAppContext);
                            Log.e(TAG, "ExoPlaybackException hasNetwork=" + hasNetwork
                                    + " caused by:\n"+ cause.toString());
                            if (!hasNetwork) {
                                notifyOnError(EXO_MEDIA_ERROR_WHAT_IO, EXO_MEDIA_ERROR_EXTRA_NETWORK);
                            } else {
                                notifyOnError(EXO_MEDIA_ERROR_WHAT_IO, EXO_MEDIA_ERROR_EXTRA_CONN);
                            }
                            return;
                        } else if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                            String shortReason = cause.toString();
                            if (shortReason.contains("403")) {
                                notifyOnError(EXO_MEDIA_ERROR_WHAT_IO, EXO_MEDIA_ERROR_RESPONSE_403);
                            } else if (shortReason.contains("404")) {
                                notifyOnError(EXO_MEDIA_ERROR_WHAT_IO, EXO_MEDIA_ERROR_RESPONSE_404);
                            } else if (shortReason.contains("500")) {
                                notifyOnError(EXO_MEDIA_ERROR_WHAT_IO, EXO_MEDIA_ERROR_RESPONSE_500);
                            } else if (shortReason.contains("502")) {
                                notifyOnError(EXO_MEDIA_ERROR_WHAT_IO, EXO_MEDIA_ERROR_RESPONSE_502);
                            } else {
                                notifyOnError(EXO_MEDIA_ERROR_WHAT_IO, EXO_MEDIA_ERROR_RESPONSE_OTHER);
                            }
                        }
                    } else if (cause instanceof UnrecognizedInputFormatException) {
                        Log.i(TAG, ExoMediaPlayerUtils.getLogcatContent());
                        notifyOnError(EXO_MEDIA_ERROR_WHAT_EXTRACTOR, EXO_MEDIA_ERROR_EXTRA_UNKNOWN);
                    } else if (cause instanceof IllegalStateException) { // maybe throw by MediaCodec dequeueInputBuffer
                        Log.i(TAG, ExoMediaPlayerUtils.getLogcatContent());
                        notifyOnError(EXO_MEIDA_ERROR_ILLEGAL_STATE, EXO_MEDIA_ERROR_EXTRA_UNKNOWN);
                    } else if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                        Log.i(TAG, ExoMediaPlayerUtils.getLogcatContent());
                        notifyOnError(EXO_MEIDA_ERROR_MEDIACODEC_DECODER_INIT, EXO_MEDIA_ERROR_EXTRA_UNKNOWN);
                    } else {
                        notifyOnError(EXO_MEDIA_ERROR_WHAT_UNKNOWN, EXO_MEDIA_ERROR_EXTRA_UNKNOWN);
                    }
                } else {
                    notifyOnError(EXO_MEDIA_ERROR_WHAT_UNKNOWN, EXO_MEDIA_ERROR_EXTRA_UNKNOWN);
                }

                Log.e(TAG, "ExoPlaybackException " + error + "\n"
                        + ExoMediaPlayerUtils.getPrintableStackTrace(error));
                Log.i(TAG, ExoMediaPlayerUtils.getLogcatContent(0, null, 30));
            } else {
                notifyOnError(EXO_MEDIA_ERROR_WHAT_UNKNOWN, EXO_MEDIA_ERROR_EXTRA_UNKNOWN);
            }
        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {
            if (isPlayerRunning()) {
                notifyOnSeekComplete();
            }
        }
    };

    // StateStore
    private static class StateStore {
        private static final int FLAG_PLAY_WHEN_READY = 0xF0000000;

        //We keep the last few states because that is all we need currently
        private int[] prevStates = new int[]{
                Player.STATE_IDLE,
                Player.STATE_IDLE,
                Player.STATE_IDLE,
                Player.STATE_IDLE
        };

        void setMostRecentState(boolean playWhenReady, int state) {
            int newState = getState(playWhenReady, state);
            Log.v(TAG, "request setMostRecentState [" + playWhenReady
                    + "," + state + "], lastState=" + prevStates[3] + ",newState=" + newState);
            if (prevStates[3] == newState) {
                return;
            }

            prevStates[0] = prevStates[1];
            prevStates[1] = prevStates[2];
            prevStates[2] = prevStates[3];
            prevStates[3] = newState; // TODO: 原处这里为 state 应该是笔误
            Log.v(TAG, "MostRecentState [" + prevStates[0]
                    + "," + prevStates[1]
                    + "," + prevStates[2]
                    + "," + prevStates[3] + "]");
        }

        int getState(boolean playWhenReady, int state) {
            return state | (playWhenReady ? FLAG_PLAY_WHEN_READY : 0);
        }

        int getMostRecentState() {
            return prevStates[3];
        }

        boolean isLastReportedPlayWhenReady() {
            return (prevStates[3] & FLAG_PLAY_WHEN_READY) != 0;
        }

        boolean matchesHistory(int[] states, boolean ignorePlayWhenReady) {
            boolean flag = true;
            int andFlag = ignorePlayWhenReady ? ~FLAG_PLAY_WHEN_READY : ~0x0;
            int startIndex = prevStates.length - states.length;

            for (int i = startIndex; i < prevStates.length; i++) {
                flag &= (prevStates[i] & andFlag) == (states[i - startIndex] & andFlag);
            }

            return flag;
        }

        void reset() {
            prevStates = new int[]{Player.STATE_IDLE, Player.STATE_IDLE, Player.STATE_IDLE, Player.STATE_IDLE};
        }
    }

    private int getBufferedPercentage() {
        if (mExoPlayer == null)
            return 0;
        return mExoPlayer.getBufferedPercentage();
    }

    private void notifyOnCompletion() {
        Log.v(TAG, "notifyOnCompletion");
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(this);
        }
    }

    private void notifyOnBufferingUpdate(int percent) {
        if (mOnBufferingUpdateListener != null) {
            mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
        }
    }

    private synchronized void notifyOnPrepared() {
        Log.v(TAG, "notifyOnPrepared");
        mPlayerStatus = PLAYER_STATUS_PREPARED;
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(this);
        }
    }

    private void notifyOnSeekComplete() {
        Log.v(TAG, "notifyOnSeekComplete");
        if (mOnSeekCompleteListener != null) {
            mOnSeekCompleteListener.onSeekComplete(this);
        }
    }

    private void setBufferRepeaterStarted(boolean start) {
        if (start && mOnBufferingUpdateListener != null) {
            mBufferUpdateRepeater.start();
        } else {
            mBufferUpdateRepeater.stop();
        }
    }

    private void notifyOnVideoSizeChanged(int width, int height,
                                          int sarNum, int sarDen) {
        Log.v(TAG, "notifyOnVideoSizeChanged [" + width + "," + height + "]");
        if (mOnVideoSizeChangedListener != null) {
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height/*,
                    sarNum, sarDen*/);
        }
    }

    // reportPlayerState
    private void reportPlayerState() {
        if (mExoPlayer == null) {
            return;
        }
        boolean playWhenReady = mExoPlayer.getPlayWhenReady();
        int playbackState = mExoPlayer.getPlaybackState();

        int newState = mStateStore.getState(playWhenReady, playbackState);
        if (newState != mStateStore.getMostRecentState()) {
            Log.d(TAG, "setMostRecentState [" + playWhenReady + "," + playbackState + "]");
            mStateStore.setMostRecentState(playWhenReady, playbackState);

//            Makes sure the buffering notifications are sent
            if (newState == Player.STATE_READY) {
                setBufferRepeaterStarted(true);
            } else if (newState == Player.STATE_IDLE || newState == Player.STATE_ENDED) {
                setBufferRepeaterStarted(false);
            }

            if (newState == mStateStore.getState(true, Player.STATE_ENDED)) {
                notifyOnCompletion();
                return;
            }

            // onPrepared
            boolean informPrepared = mStateStore.matchesHistory(new int[] {
                    mStateStore.getState(false, Player.STATE_IDLE),
                    mStateStore.getState(false, Player.STATE_BUFFERING),
                    mStateStore.getState(false, Player.STATE_READY)}, false);
            if (informPrepared) {
                notifyOnPrepared();
                return;
            }

            // Buffering Update
            boolean infoBufferingStart = mStateStore.matchesHistory(new int[] {
                    mStateStore.getState(true, Player.STATE_READY),
                    mStateStore.getState(true, Player.STATE_BUFFERING)
            }, false);

            if (infoBufferingStart) {
                notifyOnInfo(MEDIA_INFO_BUFFERING_START, getBufferedPercentage());
                return;
            }

            boolean infoBufferingEnd = mStateStore.matchesHistory(new int[] {
                    mStateStore.getState(true, Player.STATE_BUFFERING),
                    mStateStore.getState(true, Player.STATE_READY),
            }, false);

            if (infoBufferingEnd) {
                notifyOnInfo(MEDIA_INFO_BUFFERING_END, getBufferedPercentage());
                return;
            }
        }
    }
}
