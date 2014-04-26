package com.ianhanniballake.contractiontimer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;

import com.google.android.glass.timeline.DirectRenderingCallback;

/**
 * SurfaceHolder.Callback used to draw the timer on the timeline {@link com.google.android.glass.timeline.LiveCard}.
 */
public class TimerRenderer implements DirectRenderingCallback {
    private final View mView;
    private SurfaceHolder mHolder;
    private boolean mRenderingPaused;

    public TimerRenderer(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mView = inflater.inflate(R.layout.main, null);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TimerRenderer.class.getSimpleName(), "Surface Changed");
        // Measure and layout the view with the canvas dimensions.
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        mView.measure(measuredWidth, measuredHeight);
        mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
        draw();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TimerRenderer.class.getSimpleName(), "Surface Created");
        // The creation of a new Surface implicitly resumes the rendering.
        mRenderingPaused = false;
        mHolder = holder;
        draw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TimerRenderer.class.getSimpleName(), "Surface Destroyed");
        mHolder = null;
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        Log.d(TimerRenderer.class.getSimpleName(), "Rendering " + (paused ? "paused" : "unpaused"));
        mRenderingPaused = paused;
        draw();
    }

    public void draw() {
        if (!mRenderingPaused && mHolder != null) {
            Canvas canvas;
            try {
                canvas = mHolder.lockCanvas();
            } catch (Exception e) {
                return;
            }
            if (canvas != null) {
                mView.draw(canvas);
                mHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
