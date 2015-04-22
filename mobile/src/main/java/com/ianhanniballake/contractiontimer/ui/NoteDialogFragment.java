package com.ianhanniballake.contractiontimer.ui;

import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

/**
 * Reset Confirmation Dialog box
 */
public class NoteDialogFragment extends DialogFragment {
    /**
     * Argument key for storing/retrieving the contraction id associated with this dialog
     */
    public final static String CONTRACTION_ID_ARGUMENT = "com.ianhanniballake.contractiontimer.ContractionId";
    /**
     * Argument key for storing/retrieving the existing note associated with this dialog
     */
    public final static String EXISTING_NOTE_ARGUMENT = "com.ianhanniballake.contractiontimer.ExistingNote";
    /**
     * Action associated with this fragment closing
     */
    public final static String NOTE_CLOSE_ACTION = "com.ianhanniballake.contractiontimer.NOTE_CLOSE";
    private final static String TAG = NoteDialogFragment.class.getSimpleName();

    @Override
    public void onCancel(final DialogInterface dialog) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Received cancelation event");
        GtmManager.getInstance(this).pushEvent("Cancel");
        super.onCancel(dialog);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final long contractionId = getArguments().getLong(NoteDialogFragment.CONTRACTION_ID_ARGUMENT);
        final String existingNote = getArguments().getString(NoteDialogFragment.EXISTING_NOTE_ARGUMENT);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View layout = inflater.inflate(R.layout.dialog_note, null);
        final EditText input = (EditText) layout.findViewById(R.id.dialog_note_input);
        if (TextUtils.isEmpty(existingNote))
            builder.setTitle(R.string.note_dialog_title_add);
        else
            builder.setTitle(R.string.note_dialog_title_edit);
        input.setText(existingNote);
        final Context context = getActivity();
        final AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(context.getContentResolver()) {
            @Override
            protected void onUpdateComplete(final int token, final Object cookie, final int result) {
                AppWidgetUpdateHandler.createInstance().updateAllWidgets(context);
                NotificationUpdateService.updateNotification(context);
            }
        };
        final GtmManager gtmManager = GtmManager.getInstance(this);
        gtmManager.push("type", TextUtils.isEmpty(existingNote) ? "Add Note" : "Edit Note");
        return builder.setView(layout).setInverseBackgroundForced(true)
                .setPositiveButton(R.string.note_dialog_save, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Received positive event");
                        gtmManager.pushEvent("Positive");
                        final Uri updateUri = ContentUris.withAppendedId(
                                ContractionContract.Contractions.CONTENT_ID_URI_BASE, contractionId);
                        final ContentValues values = new ContentValues();
                        values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE, input.getText().toString());
                        asyncQueryHandler.startUpdate(0, 0, updateUri, values, null, null);
                    }
                }).setNegativeButton(R.string.note_dialog_cancel, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Received negative event");
                        gtmManager.pushEvent("Negative");
                    }
                }).create();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.sendBroadcast(new Intent(NOTE_CLOSE_ACTION));
        super.onDismiss(dialog);
    }
}
