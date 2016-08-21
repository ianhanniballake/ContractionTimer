package com.ianhanniballake.contractiontimer.provider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Defines a contract between the Contraction content provider and its clients. A contract defines the information that
 * a client needs to access the provider as one or more data tables. A contract is a public, non-extendable (final)
 * class that contains constants defining column names and URIs. A well-written client depends only on the constants in
 * the contract.
 */
public final class ContractionContract {
    /**
     * Base authority for this content provider
     */
    public static final String AUTHORITY = "com.ianhanniballake.contractiontimer";
    /**
     * The scheme part for this provider's URI
     */
    private static final String SCHEME = "content://";

    /**
     * This class cannot be instantiated
     */
    private ContractionContract() {
    }

    /**
     * Contraction table contract
     */
    public static final class Contractions implements BaseColumns {
        /**
         * Column name for the contraction's end time
         * <p/>
         * Type: INTEGER (long representing milliseconds)
         * </P>
         */
        public static final String COLUMN_NAME_END_TIME = "end_time";
        /**
         * Column name of a contraction's note
         * <p/>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_NOTE = "note";
        /**
         * Column name of the contraction's start time
         * <p/>
         * Type: INTEGER (long representing milliseconds)
         * </P>
         */
        public static final String COLUMN_NAME_START_TIME = "start_time";
        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single contraction.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ianhanniballake.contraction";
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of contractions.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ianhanniballake.contraction";
        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = Contractions.COLUMN_NAME_START_TIME + " DESC";
        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "contractions";
        /**
         * The content URI base for a single contraction. Callers must append a numeric contraction id to this Uri to
         * retrieve a contraction
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse(ContractionContract.SCHEME
                + ContractionContract.AUTHORITY + "/" + Contractions.TABLE_NAME + "/");
        /**
         * The content URI match pattern for a single contraction, specified by its ID. Use this to match incoming URIs
         * or to construct an Intent.
         */
        public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(ContractionContract.SCHEME
                + ContractionContract.AUTHORITY + "/" + Contractions.TABLE_NAME + "/#");
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse(ContractionContract.SCHEME + ContractionContract.AUTHORITY
                + "/" + Contractions.TABLE_NAME);

        /**
         * This class cannot be instantiated
         */
        private Contractions() {
        }
    }
}