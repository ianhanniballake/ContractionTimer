package com.ianhanniballake.contractiontimer.extensions

import android.database.Cursor
import android.database.CursorWrapper
import android.os.Build
import java.io.Closeable

/**
 * Ensure that the returned Cursor implements [Closeable].
 */
fun Cursor.closeable() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
    this
} else {
    CloseableCursorWrapper(this)
}

class CloseableCursorWrapper(cursor: Cursor) : CursorWrapper(cursor), Closeable
