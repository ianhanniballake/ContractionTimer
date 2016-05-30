package com.ianhanniballake.contractiontimer.data;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.widget.AdapterView;

import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Handles writing and reading contractions in CSV format
 */
public class CSVTransformer {
    public static void writeContractions(Context context, OutputStream outputStream) throws IOException {
        ArrayList<String[]> contractions = new ArrayList<>();
        Cursor data = context.getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, null, null, null,
                null);
        if (data != null) {
            while (data.moveToNext()) {
                String[] contraction = new String[5];
                final int startTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                        .COLUMN_NAME_START_TIME);
                final long startTime = data.getLong(startTimeColumnIndex);
                contraction[0] = Long.toString(startTime);
                contraction[1] = DateFormat.getDateTimeInstance().format(new Date(startTime));
                final int endTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                        .COLUMN_NAME_END_TIME);
                if (!data.isNull(endTimeColumnIndex)) {
                    final long endTime = data.getLong(endTimeColumnIndex);
                    contraction[2] = Long.toString(endTime);
                    contraction[3] = DateFormat.getDateTimeInstance().format(new Date(endTime));
                } else {
                    contraction[2] = "";
                    contraction[3] = "";
                }
                final int noteColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                        .COLUMN_NAME_NOTE);
                if (!data.isNull(noteColumnIndex)) {
                    final String note = data.getString(noteColumnIndex);
                    contraction[4] = note;
                } else
                    contraction[4] = "";
                contractions.add(contraction);
            }
            data.close();
        }
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream));
        writer.writeNext(new String[]{
                context.getString(R.string.detail_start_time_label),
                context.getString(R.string.detail_start_time_formatted_label),
                context.getString(R.string.detail_end_time_label),
                context.getString(R.string.detail_end_time_formatted_label),
                context.getString(R.string.detail_note_label)});
        writer.writeAll(contractions);
        writer.close();
    }

    public static void readContractions(Context context, InputStream inputStream) throws IOException,
            IllegalArgumentException, RemoteException, OperationApplicationException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        CSVReader reader = new CSVReader(new InputStreamReader(inputStream), CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER, 1);
        List<String[]> contractions = reader.readAll();
        reader.close();
        ContentResolver resolver = context.getContentResolver();
        String[] projection = new String[]{BaseColumns._ID};
        for (String[] contraction : contractions) {
            if (contraction.length != 5) {
                throw new IllegalArgumentException();
            }
            ContentValues values = new ContentValues();
            final long startTime = Long.parseLong(contraction[0]);
            values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                    startTime);
            if (!TextUtils.isEmpty(contraction[2]))
                values.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                        Long.parseLong(contraction[2]));
            if (!TextUtils.isEmpty(contraction[4]))
                values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE,
                        contraction[4]);
            Cursor existingRow = resolver.query(ContractionContract.Contractions.CONTENT_URI,
                    projection, ContractionContract.Contractions.COLUMN_NAME_START_TIME
                            + "=?", new String[]{Long.toString(startTime)}, null
            );
            long existingRowId = AdapterView.INVALID_ROW_ID;
            if (existingRow != null) {
                existingRowId = existingRow.moveToFirst() ? existingRow.getLong(0) : AdapterView.INVALID_ROW_ID;
                existingRow.close();
            }
            if (existingRowId == AdapterView.INVALID_ROW_ID)
                operations.add(ContentProviderOperation.newInsert(ContractionContract.Contractions.CONTENT_URI)
                        .withValues(values).build());
            else
                operations.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId
                        (ContractionContract.Contractions.CONTENT_ID_URI_BASE,
                                existingRowId)).withValues(values).build());
        }
        resolver.applyBatch(ContractionContract.AUTHORITY, operations);
    }
}
