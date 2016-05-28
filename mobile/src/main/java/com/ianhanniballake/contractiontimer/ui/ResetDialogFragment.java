package com.ianhanniballake.contractiontimer.ui;

import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

/**
 * Reset Confirmation Dialog box
 */
public class ResetDialogFragment extends DialogFragment {
    private final static String TAG = ResetDialogFragment.class.getSimpleName();

    @Override
    public void onCancel(final DialogInterface dialog) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Received cancelation event");
        super.onCancel(dialog);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View layout = inflater.inflate(R.layout.dialog_reset, null);
        final Context context = getActivity();
        final AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onDeleteComplete(final int token, final Object cookie, final int result) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(context);
                NotificationUpdateService.updateNotification(context);
            }
        };
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_dialog_alert)
                .setTitle(R.string.reset_dialog_title)
                .setView(layout)
                .setInverseBackgroundForced(true)
                .setPositiveButton(R.string.reset_dialog_confirm, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Received positive event");
                        FirebaseAnalytics.getInstance(getContext()).logEvent("reset_complete", null);
                        asyncQueryHandler.startDelete(0, 0, ContractionContract.Contractions.CONTENT_URI, null, null);
                    }
                }).setNegativeButton(R.string.reset_dialog_cancel, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Received negative event");
                    }
                }).create();
    }
}
