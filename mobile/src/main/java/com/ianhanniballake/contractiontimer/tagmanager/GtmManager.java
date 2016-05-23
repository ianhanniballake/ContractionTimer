package com.ianhanniballake.contractiontimer.tagmanager;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

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

    private GtmManager(Context context) {
        mContext = context;
        mTagManager = TagManager.getInstance(context);
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
            result = mTagManager.loadContainerPreferNonDefault(mContext.getString(R.string.container_id),
                    R.raw.gtm_default_container);
        }
        result.setResultCallback(new ResultCallback<ContainerHolder>() {
            @Override
            public void onResult(@NonNull final ContainerHolder containerHolder) {
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
}
