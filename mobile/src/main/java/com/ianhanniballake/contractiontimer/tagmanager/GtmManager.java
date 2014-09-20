package com.ianhanniballake.contractiontimer.tagmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tagmanager.ContainerHolder;
import com.google.android.gms.tagmanager.DataLayer;
import com.google.android.gms.tagmanager.TagManager;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Centralized Manager for Google Tag Manager operations, including pushing values, events, and exceptions
 */
public class GtmManager {
    private static final String TAG = GtmManager.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static GtmManager INSTANCE;
    private final Context mContext;
    private final TagManager mTagManager;
    private final GtmExceptionParser mExceptionParser;
    private ContainerHolder mContainerHolder;

    private GtmManager(Context context) {
        mContext = context;
        mTagManager = TagManager.getInstance(context);
        mExceptionParser = new GtmExceptionParser(context);
    }

    @NonNull
    public static GtmManager getInstance(@NonNull Fragment fragment) {
        return getInstance(fragment.getActivity());
    }

    @NonNull
    public static GtmManager getInstance(@NonNull Context context) {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new GtmManager(context.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    public void init() {
        push("debug", BuildConfig.DEBUG);
        PendingResult<ContainerHolder> result;
        if (BuildConfig.DEBUG) {
            mTagManager.setVerboseLoggingEnabled(true);
            result = mTagManager.loadContainerDefaultOnly(mContext.getString(R.string.container_id),
                    R.raw.gtm_default_container);
        } else {
            final Thread.UncaughtExceptionHandler myHandler =
                    new GtmExceptionReporter(Thread.getDefaultUncaughtExceptionHandler());
            Thread.setDefaultUncaughtExceptionHandler(myHandler);
            result = mTagManager.loadContainerPreferNonDefault(mContext.getString(R.string.container_id),
                    R.raw.gtm_default_container);
        }
        result.setResultCallback(new ResultCallback<ContainerHolder>() {
            @Override
            public void onResult(final ContainerHolder containerHolder) {
                mContainerHolder = containerHolder;
            }
        }, 2, TimeUnit.SECONDS);
    }

    public void push(@NonNull String key, @NonNull Object value) {
        mTagManager.getDataLayer().push(key, value);
    }

    public void push(@NonNull Map<String, Object> update) {
        mTagManager.getDataLayer().push(update);
    }

    public void pushEvent(@NonNull String eventName) {
        mTagManager.getDataLayer().pushEvent(eventName, DataLayer.mapOf());
    }

    public void pushEvent(@NonNull String eventName, @NonNull Map<String, Object> update) {
        mTagManager.getDataLayer().pushEvent(eventName, update);
    }

    public void pushOpenScreen(@NonNull String screenName) {
        pushEvent("OpenScreen", DataLayer.mapOf("screenName", screenName));
    }

    public void pushPreferenceChanged(@NonNull String preference, @NonNull Object value) {
        pushEvent("Changed", DataLayer.mapOf("preference", preference, "value", value));
    }

    public void pushException(@NonNull final Throwable t) {
        pushEvent("Exception", mExceptionParser.getExceptionMapping(Thread.currentThread(), t));
    }

    /**
     * Exception parser which converts exceptions into a Map suitable for sending to Google Tag Manager
     */
    private static class GtmExceptionParser extends StandardExceptionParser {
        private final String mPackageName;

        public GtmExceptionParser(final Context context) {
            super(context, null);
            mPackageName = context.getPackageName();
        }

        public Map<String, Object> getExceptionMapping(final Thread thread, final Throwable t) {
            Throwable rootCause = getCause(t);
            StackTraceElement stackTraceElement = getBestStackTraceElement(rootCause);
            String className = stackTraceElement.getClassName();
            String shortClassName = className.replace(mPackageName, "");
            String[] splitClassName = className.split("\\.");
            String simpleClassName = splitClassName.length == 0 ? "unknown" : splitClassName[splitClassName.length - 1];
            return DataLayer.mapOf("threadName", thread.getName(),
                    "exceptionName", t.getClass().getSimpleName(),
                    "exceptionMessage", t.getMessage(),
                    "rootExceptionName", rootCause.getClass().getSimpleName(),
                    "rootExceptionMessage", rootCause.getMessage(),
                    "rootExceptionClass", shortClassName,
                    "rootExceptionSimpleClass", simpleClassName,
                    "rootExceptionMethod", stackTraceElement.getMethodName(),
                    "rootExceptionLine", stackTraceElement.getLineNumber());
        }
    }

    /**
     * UncaughtExceptionHandler that sends uncaught exceptions to Google Tag Manager as well as passing on the error
     * to the default Thread.UncaughtExceptionHandler
     */
    private class GtmExceptionReporter implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

        public GtmExceptionReporter(final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler) {
            mDefaultUncaughtExceptionHandler = defaultUncaughtExceptionHandler;
        }

        @Override
        public void uncaughtException(final Thread thread, final Throwable t) {
            Map<String, Object> dataMap = mExceptionParser.getExceptionMapping(thread, t);
            boolean tagManagerCrash = dataMap.get("rootExceptionClass").toString()
                    .contains("com.google.android.gms.tagmanager");
            boolean campaignTrackingServiceCrash = dataMap.get("rootExceptionClass").toString()
                    .contains("com.google.android.gms.analytics.CampaignTrackingService");
            if (tagManagerCrash) {
                Log.e(TAG, "TagManager crashed", t);
                pushException(t);
            } else if (campaignTrackingServiceCrash) {
                Log.e(TAG, "CampaignTrackingService crashed", t);
                pushException(t);
            } else {
                pushEvent("UncaughtException", dataMap);
                if (mDefaultUncaughtExceptionHandler != null) {
                    mDefaultUncaughtExceptionHandler.uncaughtException(thread, t);
                }
            }
        }
    }
}
