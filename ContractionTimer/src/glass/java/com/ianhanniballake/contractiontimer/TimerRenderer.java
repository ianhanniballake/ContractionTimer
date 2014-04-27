package com.ianhanniballake.contractiontimer;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.TextView;

import com.google.android.glass.timeline.DirectRenderingCallback;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * SurfaceHolder.Callback used to draw the timer on the timeline {@link com.google.android.glass.timeline.LiveCard}.
 */
public class TimerRenderer implements DirectRenderingCallback {
    private final View mView;
    private final View mEmptyState;
    private final TextView mCurrentState;
    private final ContentResolver mContentResolver;
    private final AsyncQueryHandler mAsyncQueryHandler;
    private final ContentObserver mContentObserver;
    private SurfaceHolder mHolder;
    private boolean mRenderingPaused;
    private boolean mContentObserverRegistered;

    public TimerRenderer(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mView = inflater.inflate(R.layout.main, null);
        mEmptyState = mView.findViewById(R.id.empty_state);
        mCurrentState = (TextView) mView.findViewById(R.id.current_state);
        mContentResolver = context.getContentResolver();
        mAsyncQueryHandler = new AsyncQueryHandler(mContentResolver) {
            @Override
            protected void onQueryComplete(final int token, final Object cookie, final Cursor cursor) {
                final boolean hasContractions = cursor != null && cursor.moveToFirst();
                Log.d(TimerRenderer.class.getSimpleName(), "Has Contractions: " + hasContractions);
                mEmptyState.setVisibility(hasContractions ? View.INVISIBLE : View.VISIBLE);
                mCurrentState.setVisibility(!hasContractions ? View.INVISIBLE : View.VISIBLE);
                final boolean contractionOngoing = hasContractions
                        && cursor.isNull(cursor.getColumnIndex(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
                Log.d(TimerRenderer.class.getSimpleName(), "Contraction Ongoing: " + contractionOngoing);
                if (contractionOngoing) {
                    mCurrentState.setText(R.string.contraction_ongoing);
                } else {
                    mCurrentState.setText(R.string.between_contractions);
                }
                draw();
            }
        };
        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(final boolean selfChange, final Uri uri) {
                mAsyncQueryHandler.startQuery(0, null, uri, null, null, null, null);
            }
        };
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
        // Kick off an initial pull to populate the data
        mAsyncQueryHandler.startQuery(0, null, ContractionContract.Contractions.CONTENT_URI, null, null, null, null);
        updateContentObserverRegistration();
        draw();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TimerRenderer.class.getSimpleName(), "Surface Destroyed");
        mHolder = null;
        updateContentObserverRegistration();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        Log.d(TimerRenderer.class.getSimpleName(), "Rendering " + (paused ? "paused" : "unpaused"));
        mRenderingPaused = paused;
        updateContentObserverRegistration();
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

    private void updateContentObserverRegistration() {
        if ((mRenderingPaused || mHolder == null) && mContentObserverRegistered) {
            mContentResolver.unregisterContentObserver(mContentObserver);
            mContentObserverRegistered = false;
        } else if (!mRenderingPaused && mHolder != null && !mContentObserverRegistered) {
            mContentResolver.registerContentObserver(ContractionContract.Contractions.CONTENT_URI,
                    true, mContentObserver);
            mContentObserverRegistered = true;
        }
    }
}
