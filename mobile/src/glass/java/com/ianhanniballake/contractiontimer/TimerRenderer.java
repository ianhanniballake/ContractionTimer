package com.ianhanniballake.contractiontimer;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
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
    private final static String TAG = TimerRenderer.class.getSimpleName();

    private final Context mContext;
    private final View mView;
    private final View mEmptyState;
    private final View mCurrentState;
    private final TextView mCurrentStartTime;
    private final TextView mCurrentDuration;
    private final TextView mCurrentFrequency;
    private final TextView mPreviousStartTime;
    private final TextView mPreviousDuration;
    private final TextView mPreviousFrequency;
    private final TextView mAverageDuration;
    private final TextView mAverageFrequency;
    private final ContentResolver mContentResolver;
    private final AsyncQueryHandler mAsyncQueryHandler;
    private final ContentObserver mContentObserver;
    private SurfaceHolder mHolder;
    private boolean mRenderingPaused;
    private boolean mContentObserverRegistered;

    public TimerRenderer(Context context) {
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        mView = inflater.inflate(R.layout.main, null);
        mEmptyState = mView.findViewById(R.id.empty_state);
        mCurrentState = mView.findViewById(R.id.current_state);
        mCurrentStartTime = (TextView) mView.findViewById(R.id.current_start_time);
        mCurrentDuration = (TextView) mView.findViewById(R.id.current_duration);
        mCurrentFrequency = (TextView) mView.findViewById(R.id.current_frequency);
        mPreviousStartTime = (TextView) mView.findViewById(R.id.previous_start_time);
        mPreviousDuration = (TextView) mView.findViewById(R.id.previous_duration);
        mPreviousFrequency = (TextView) mView.findViewById(R.id.previous_frequency);
        mAverageDuration = (TextView) mView.findViewById(R.id.average_duration);
        mAverageFrequency = (TextView) mView.findViewById(R.id.average_frequency);
        mContentResolver = context.getContentResolver();
        mAsyncQueryHandler = new AsyncQueryHandler(mContentResolver) {
            @Override
            protected void onQueryComplete(final int token, final Object cookie, final Cursor cursor) {
                final boolean hasContractions = cursor != null && cursor.moveToFirst();
                Log.d(TAG, "Has Contractions: " + hasContractions);
                if (hasContractions) {
                    updateWithContractions(cursor);
                } else {
                    updateWithNoContractions();
                }
                if (cursor != null) {
                    cursor.close();
                }
                mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
                draw();
            }

            private void updateWithContractions(final Cursor cursor) {
                mEmptyState.setVisibility(View.INVISIBLE);
                mCurrentState.setVisibility(View.VISIBLE);
                // Assume cursor is already at the first position
                Log.d(TAG, "Rendering first row");
                renderRow(cursor, mCurrentStartTime, mCurrentDuration, mCurrentFrequency);
                cursor.moveToNext();
                Log.d(TAG, "Rendering previous row");
                renderRow(cursor, mPreviousStartTime, mPreviousDuration, mPreviousFrequency);
                // TODO fill in average time information
            }

            private void renderRow(Cursor cursor, TextView startTimeView, TextView durationView,
                                   TextView frequencyView) {
                if (cursor.isAfterLast()) {
                    Log.d(TAG, "Rendering is after last row, aborting");
                    startTimeView.setText("");
                    durationView.setText("");
                    frequencyView.setText("");
                    return;
                }
                String timeFormat = "hh:mm:ssa";
                if (DateFormat.is24HourFormat(mContext))
                    timeFormat = "HH:mm:ss";
                final int startTimeColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_START_TIME);
                final long startTime = cursor.getLong(startTimeColumnIndex);
                final int endTimeColumnIndex = cursor.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_END_TIME);
                final boolean isContractionOngoing = cursor.isNull(endTimeColumnIndex);
                if (isContractionOngoing) {
                    durationView.setText(R.string.timing);
                    // TODO fill in ongoing contraction's duration
                } else {
                    final long endTime = cursor.getLong(endTimeColumnIndex);
                    durationView.setTag("");
                    final long durationInSeconds = (endTime - startTime) / 1000;
                    durationView.setText(DateUtils.formatElapsedTime(durationInSeconds));
                }
                // If we aren't the last entry, move to the next (previous in time)
                // contraction to get its start time to compute the frequency
                if (!cursor.isLast() && cursor.moveToNext()) {
                    final long prevContractionStartTime = cursor.getLong(startTimeColumnIndex);
                    final long frequencyInSeconds = (startTime - prevContractionStartTime) / 1000;
                    frequencyView.setText(DateUtils.formatElapsedTime(frequencyInSeconds));
                    // Go back to the previous spot
                    cursor.moveToPrevious();
                } else {
                    frequencyView.setText("");
                }
                startTimeView.setText(DateFormat.format(timeFormat, startTime));
                Log.d(TAG, "Rendering complete");
            }

            private void updateWithNoContractions() {
                mEmptyState.setVisibility(View.VISIBLE);
                mCurrentState.setVisibility(View.INVISIBLE);
            }
        };
        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(final boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(final boolean selfChange, final Uri uri) {
                mAsyncQueryHandler.startQuery(0, null, ContractionContract.Contractions.CONTENT_URI,
                        null, null, null, null);
            }
        };
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface Changed");
        // Measure and layout the view with the canvas dimensions.
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        mView.measure(measuredWidth, measuredHeight);
        mView.layout(0, 0, mView.getMeasuredWidth(), mView.getMeasuredHeight());
        draw();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "Surface Created");
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
        Log.d(TAG, "Surface Destroyed");
        mHolder = null;
        updateContentObserverRegistration();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        Log.d(TAG, "Rendering " + (paused ? "paused" : "unpaused"));
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
