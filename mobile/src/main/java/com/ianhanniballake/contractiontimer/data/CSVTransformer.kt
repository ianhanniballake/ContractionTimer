package com.ianhanniballake.contractiontimer.data

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.os.RemoteException
import android.provider.BaseColumns
import android.widget.AdapterView
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.DateFormat
import java.util.ArrayList
import java.util.Date

/**
 * Handles writing and reading contractions in CSV format
 */
object CSVTransformer {
    @Throws(IOException::class)
    fun writeContractions(context: Context, outputStream: OutputStream) {
        val contractions = ArrayList<Array<String?>>()
        context.contentResolver.query(ContractionContract.Contractions.CONTENT_URI,
                null, null, null, null)?.use { data ->
            while (data.moveToNext()) {
                val contraction = arrayOfNulls<String>(5)
                val startTimeColumnIndex = data.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_START_TIME)
                val startTime = data.getLong(startTimeColumnIndex)
                contraction[0] = startTime.toString()
                contraction[1] = DateFormat.getDateTimeInstance().format(Date(startTime))
                val endTimeColumnIndex = data.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_END_TIME)
                if (!data.isNull(endTimeColumnIndex)) {
                    val endTime = data.getLong(endTimeColumnIndex)
                    contraction[2] = endTime.toString()
                    contraction[3] = DateFormat.getDateTimeInstance().format(Date(endTime))
                } else {
                    contraction[2] = ""
                    contraction[3] = ""
                }
                val noteColumnIndex = data.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_NOTE)
                if (!data.isNull(noteColumnIndex)) {
                    val note = data.getString(noteColumnIndex)
                    contraction[4] = note
                } else
                    contraction[4] = ""
                contractions.add(contraction)
            }
        }
        val writer = CSVWriter(OutputStreamWriter(outputStream))
        writer.writeNext(arrayOf(context.getString(R.string.detail_start_time_label), context.getString(R.string.detail_start_time_formatted_label), context.getString(R.string.detail_end_time_label), context.getString(R.string.detail_end_time_formatted_label), context.getString(R.string.detail_note_label)))
        writer.writeAll(contractions)
        writer.close()
    }

    @Throws(IOException::class, IllegalArgumentException::class, RemoteException::class, OperationApplicationException::class)
    fun readContractions(context: Context, inputStream: InputStream) {
        val operations = ArrayList<ContentProviderOperation>()
        val reader = CSVReader(InputStreamReader(inputStream), CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER, 1)
        val contractions = reader.readAll()
        reader.close()
        val resolver = context.contentResolver
        val projection = arrayOf(BaseColumns._ID)
        for (contraction in contractions) {
            if (contraction.size != 5) {
                throw IllegalArgumentException()
            }
            val values = ContentValues()
            val startTime = contraction[0].toLong()
            values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                    startTime)
            if (contraction[2].isNotBlank())
                values.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                        contraction[2].toLong())
            if (contraction[4].isNotBlank())
                values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE,
                        contraction[4])
            val existingRowId = resolver.query(ContractionContract.Contractions.CONTENT_URI,
                    projection, ContractionContract.Contractions.COLUMN_NAME_START_TIME + "=?",
                    arrayOf(startTime.toString()), null)?.use { existingRow ->
                if (existingRow.moveToFirst()) existingRow.getLong(0) else AdapterView.INVALID_ROW_ID
            } ?: AdapterView.INVALID_ROW_ID
            if (existingRowId == AdapterView.INVALID_ROW_ID)
                operations.add(ContentProviderOperation.newInsert(
                        ContractionContract.Contractions.CONTENT_URI)
                        .withValues(values).build())
            else
                operations.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                        ContractionContract.Contractions.CONTENT_ID_URI_BASE, existingRowId))
                        .withValues(values).build())
        }
        resolver.applyBatch(ContractionContract.AUTHORITY, operations)
    }
}
