package com.caij.video;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

public class SimpleVideoView extends FrameLayout {


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            RESIZE_MODE_FIT,
            RESIZE_MODE_FIXED_WIDTH,
            RESIZE_MODE_FIXED_HEIGHT,
            RESIZE_MODE_FILL,
            RESIZE_MODE_ZOOM
    })
    public @interface ResizeMode {}

    /**
     * Either the width or height is decreased to obtain the desired aspect ratio.
     */
    public static final int RESIZE_MODE_FIT = 0;
    /**
     * The width is fixed and the height is increased or decreased to obtain the desired aspect ratio.
     */
    public static final int RESIZE_MODE_FIXED_WIDTH = 1;
    /**
     * The height is fixed and the width is increased or decreased to obtain the desired aspect ratio.
     */
    public static final int RESIZE_MODE_FIXED_HEIGHT = 2;
    /**
     * The specified aspect ratio is ignored.
     */
    public static final int RESIZE_MODE_FILL = 3;
    /**
     * Either the width or height is increased to obtain the desired aspect ratio.
     */
    public static final int RESIZE_MODE_ZOOM = 4;
    // LINT.ThenChange(../../../../../../res/values/attrs.xml)

    /**
     * The {@link FrameLayout} will not resize itself if the fractional difference between its natural
     * aspect ratio and the requested aspect ratio falls below this threshold.
     *
     * <p>This tolerance allows the view to occupy the whole of the screen when the requested aspect
     * ratio is very close, but not exactly equal to, the aspect ratio of the screen. This may reduce
     * the number of view layers that need to be composited by the underlying system, which can help
     * to reduce power consumption.
     */
    private static final float MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f;

    private float videoAspectRatio;
    private @ResizeMode int resizeMode;


    private static final int SURFACE_TYPE_SURFACE_VIEW = 1;
    private static final int SURFACE_TYPE_TEXTURE_VIEW = 2;
    private static final String TAG = "SimpleVideoView";

    private View surfaceView;
    private View shutterView;


    public SimpleVideoView(Context context) {
        this(context, null);
    }

    public SimpleVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int surfaceType = SURFACE_TYPE_SURFACE_VIEW;
        resizeMode = RESIZE_MODE_FIT;
        int shutterColor = 0;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SimpleVideoView, 0, 0);
            try {
                surfaceType = a.getInt(R.styleable.SimpleVideoView_surface_type, surfaceType);
                resizeMode = a.getInt(R.styleable.SimpleVideoView_resize_mode, RESIZE_MODE_FIT);
                shutterColor = a.getColor(R.styleable.SimpleVideoView_shutter_background_color, Color.parseColor("#000000"));
            } finally {
                a.recycle();
            }
        }

        surfaceView = surfaceType == SURFACE_TYPE_TEXTURE_VIEW
                ? new TextureView(context)
                : new SurfaceView(context);
        Log.d(TAG, "创建 playview " + surfaceView.getClass().getSimpleName());
        surfaceView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(surfaceView);

        shutterView = new View(getContext());
        shutterView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        shutterView.setBackgroundColor(shutterColor);

        addView(shutterView);
    }

    public void onRenderedFirstFrame() {
        if (shutterView != null) {
            shutterView.setVisibility(INVISIBLE);
        }
    }


    /**
     * Sets the aspect ratio that this view should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    public void setAspectRatio(float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio) {
            this.videoAspectRatio = widthHeightRatio;
            requestLayout();
        }
    }

    /**
     * Returns the resize mode.
     */
    public @ResizeMode
    int getResizeMode() {
        return resizeMode;
    }

    /**
     * Sets the resize mode.
     *
     * @param resizeMode The resize mode.
     */
    public void setResizeMode(@ResizeMode int resizeMode) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (videoAspectRatio <= 0) {
            // Aspect ratio not set.
            return;
        }

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        float viewAspectRatio = (float) width / height;
        float aspectDeformation = videoAspectRatio / viewAspectRatio - 1;
        if (Math.abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            // We're within the allowed tolerance.
            return;
        }

        switch (resizeMode) {
            case RESIZE_MODE_FIXED_WIDTH:
                height = (int) (width / videoAspectRatio);
                break;
            case RESIZE_MODE_FIXED_HEIGHT:
                width = (int) (height * videoAspectRatio);
                break;
            case RESIZE_MODE_ZOOM:
                if (aspectDeformation > 0) {
                    width = (int) (height * videoAspectRatio);
                } else {
                    height = (int) (width / videoAspectRatio);
                }
                break;
            case RESIZE_MODE_FIT:
                if (aspectDeformation > 0) {
                    height = (int) (width / videoAspectRatio);
                } else {
                    width = (int) (height * videoAspectRatio);
                }
                break;
            case RESIZE_MODE_FILL:
            default:
                // Ignore target aspect ratio
                break;
        }
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }



    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (surfaceView != null && surfaceView instanceof SurfaceView) surfaceView.setVisibility(visibility);
    }



    public View getSurfaceView() {
        return surfaceView;
    }

    public View getShutterView() {
        return shutterView;
    }
}
