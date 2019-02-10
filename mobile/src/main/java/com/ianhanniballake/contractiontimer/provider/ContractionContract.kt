package com.ianhanniballake.contractiontimer.provider

import android.net.Uri
import android.provider.BaseColumns

/**
 * Defines a contract between the Contraction content provider and its clients. A contract defines
 * the information that a client needs to access the provider as one or more data tables. A
 * contract is a public, non-extendable (final) class that contains constants defining column
 * names and URIs. A well-written client depends only on the constants in the contract.
 */
object ContractionContract {
    /**
     * Base authority for this content provider
     */
    const val AUTHORITY = "com.ianhanniballake.contractiontimer"
    /**
     * The scheme part for this provider's URI
     */
    private const val SCHEME = "content://"

    /**
     * Contraction table contract
     */
    class Contractions private constructor() : BaseColumns {
        companion object {
            /**
             * Column name for the contraction's end time
             *
             * Type: INTEGER (long representing milliseconds)
             */
            const val COLUMN_NAME_END_TIME = "end_time"
            /**
             * Column name of a contraction's note
             *
             * Type: TEXT
             */
            const val COLUMN_NAME_NOTE = "note"
            /**
             * Column name of the contraction's start time
             *
             * Type: INTEGER (long representing milliseconds)
             */
            const val COLUMN_NAME_START_TIME = "start_time"
            /**
             * The MIME type of a [.CONTENT_URI] sub-directory of a single contraction.
             */
            const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ianhanniballake.contraction"
            /**
             * The MIME type of [.CONTENT_URI] providing a directory of contractions.
             */
            const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ianhanniballake.contraction"
            /**
             * The default sort order for this table
             */
            const val DEFAULT_SORT_ORDER = "${Contractions.COLUMN_NAME_START_TIME} DESC"
            /**
             * The table name offered by this provider
             */
            const val TABLE_NAME = "contractions"
            /**
             * The content URI base for a single contraction. Callers must append a numeric
             * contraction id to this Uri to retrieve a contraction
             */
            val CONTENT_ID_URI_BASE: Uri = Uri.parse(ContractionContract.SCHEME
                    + ContractionContract.AUTHORITY + "/" + Contractions.TABLE_NAME + "/")
            /**
             * The content URI match pattern for a single contraction, specified by its ID.
             * Use this to match incoming URIs or to construct an Intent.
             */
            val CONTENT_ID_URI_PATTERN: Uri = Uri.parse(ContractionContract.SCHEME
                    + ContractionContract.AUTHORITY + "/" + Contractions.TABLE_NAME + "/#")
            /**
             * The content:// style URL for this table
             */
            val CONTENT_URI: Uri = Uri.parse(ContractionContract.SCHEME + ContractionContract.AUTHORITY
                    + "/" + Contractions.TABLE_NAME)
        }
    }
}
