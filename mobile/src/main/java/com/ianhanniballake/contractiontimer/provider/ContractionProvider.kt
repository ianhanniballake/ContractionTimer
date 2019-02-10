package com.ianhanniballake.contractiontimer.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import android.support.v4.database.DatabaseUtilsCompat
import android.util.Log
import com.ianhanniballake.contractiontimer.BuildConfig
import java.util.HashMap

/**
 * Provides access to a database of contractions.
 */
class ContractionProvider : ContentProvider() {
    companion object {
        private const val TAG = "ContractionProvider"
        /**
         * The incoming URI matches the Contractions URI pattern
         */
        private const val CONTRACTIONS = 1
        /**
         * The incoming URI matches the Contraction ID URI pattern
         */
        private const val CONTRACTION_ID = 2
        /**
         * The database that the provider uses as its underlying data store
         */
        private const val DATABASE_NAME = "contractions.db"
        /**
         * The database version
         */
        private const val DATABASE_VERSION = 2
        /**
         * A UriMatcher instance
         */
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(ContractionContract.AUTHORITY, ContractionContract.Contractions.TABLE_NAME,
                    CONTRACTIONS)
            addURI(ContractionContract.AUTHORITY, ContractionContract.Contractions.TABLE_NAME + "/#",
                    CONTRACTION_ID)
        }
        /**
         * An identity all column projection mapping
         */
        private val allColumnProjectionMap = HashMap<String, String>().apply {
            this[BaseColumns._ID] = BaseColumns._ID
            this[ContractionContract.Contractions.COLUMN_NAME_START_TIME] =
                    ContractionContract.Contractions.COLUMN_NAME_START_TIME
            this[ContractionContract.Contractions.COLUMN_NAME_END_TIME] =
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME
            this[ContractionContract.Contractions.COLUMN_NAME_NOTE] =
                    ContractionContract.Contractions.COLUMN_NAME_NOTE
        }
    }

    /**
     * Handle to a new DatabaseHelper.
     */
    private lateinit var databaseHelper: DatabaseHelper

    override fun delete(uri: Uri, where: String?, whereArgs: Array<String>?): Int {
        // Opens the database object in "write" mode.
        val db = databaseHelper.writableDatabase
        val count: Int
        // Does the delete based on the incoming URI pattern.
        when (ContractionProvider.uriMatcher.match(uri)) {
            CONTRACTIONS ->
                // If the incoming pattern matches the general pattern for
                // contractions, does a delete based on the incoming "where"
                // column and arguments.
                count = db.delete(ContractionContract.Contractions.TABLE_NAME, where, whereArgs)
            CONTRACTION_ID -> {
                // If the incoming URI matches a single contraction ID, does the
                // delete based on the incoming data, but modifies the where
                // clause to restrict it to the particular contraction ID.
                val finalWhere = DatabaseUtilsCompat.concatenateWhere(where, BaseColumns._ID + "=?")
                val finalWhereArgs = DatabaseUtilsCompat.appendSelectionArgs(whereArgs,
                        arrayOf(ContentUris.parseId(uri).toString()))
                count = db.delete(ContractionContract.Contractions.TABLE_NAME, finalWhere, finalWhereArgs)
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    /**
     * Chooses the MIME type based on the incoming URI pattern
     */
    override fun getType(uri: Uri) = when (ContractionProvider.uriMatcher.match(uri)) {
        CONTRACTIONS ->
            // If the pattern is for contractions, returns the general
            // content type.
            ContractionContract.Contractions.CONTENT_TYPE
        CONTRACTION_ID ->
            // If the pattern is for contraction IDs, returns the
            // contraction ID content type.
            ContractionContract.Contractions.CONTENT_ITEM_TYPE
        else -> throw IllegalArgumentException("Unknown URI $uri")
    }

    override fun insert(uri: Uri, initialValues: ContentValues?): Uri? {
        // Validates the incoming URI. Only the full provider URI is allowed for
        // inserts.
        if (ContractionProvider.uriMatcher.match(uri) != ContractionProvider.CONTRACTIONS)
            throw IllegalArgumentException("Unknown URI $uri")
        val values = if (initialValues != null)
            ContentValues(initialValues)
        else
            ContentValues()
        if (!values.containsKey(ContractionContract.Contractions.COLUMN_NAME_START_TIME))
            values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME, System.currentTimeMillis())
        if (!values.containsKey(ContractionContract.Contractions.COLUMN_NAME_NOTE))
            values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE, "")
        val db = databaseHelper.writableDatabase
        val rowId = db.insert(ContractionContract.Contractions.TABLE_NAME,
                ContractionContract.Contractions.COLUMN_NAME_START_TIME, values)
        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            // Creates a URI with the contraction ID pattern and the new row ID
            // appended to it.
            val contractionUri = ContentUris.withAppendedId(
                    ContractionContract.Contractions.CONTENT_ID_URI_BASE,
                    rowId)
            context!!.contentResolver.notifyChange(contractionUri, null)
            return contractionUri
        }
        // If the insert didn't succeed, then the rowID is <= 0
        throw SQLException("Failed to insert row into $uri")
    }

    /**
     * Creates the underlying DatabaseHelper
     *
     * @see android.content.ContentProvider.onCreate
     */
    override fun onCreate(): Boolean {
        databaseHelper = DatabaseHelper(context!!)
        return true
    }

    override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?
    ): Cursor? {
        // Constructs a new query builder and sets its table name
        val qb = SQLiteQueryBuilder().apply {
            tables = ContractionContract.Contractions.TABLE_NAME
            setProjectionMap(allColumnProjectionMap)
        }
        val finalSortOrder = sortOrder ?: ContractionContract.Contractions.DEFAULT_SORT_ORDER
        when (ContractionProvider.uriMatcher.match(uri)) {
            CONTRACTIONS -> {
            }
            CONTRACTION_ID ->
                // If the incoming URI is for a single contraction identified by
                // its ID, appends "_ID = <contractionID>" to the where clause,
                // so that it selects that single contraction
                qb.appendWhere(BaseColumns._ID + "=" + uri.lastPathSegment)
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        val db = databaseHelper.readableDatabase
        val c = qb.query(db, projection, selection, selectionArgs, null, null, finalSortOrder, null)
        c.setNotificationUri(context!!.contentResolver, uri)
        return c
    }

    override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<String>?
    ): Int {
        val db = databaseHelper.writableDatabase
        val count: Int
        when (ContractionProvider.uriMatcher.match(uri)) {
            CONTRACTIONS ->
                // If the incoming URI matches the general contractions pattern,
                // does the update based on the incoming data.
                count = db.update(ContractionContract.Contractions.TABLE_NAME, values, selection, selectionArgs)
            CONTRACTION_ID -> {
                // If the incoming URI matches a single contraction ID, does the
                // update based on the incoming data, but modifies the where
                // clause to restrict it to the particular contraction ID.
                val finalWhere = DatabaseUtilsCompat.concatenateWhere(selection, BaseColumns._ID + "=?")
                val finalWhereArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs,
                        arrayOf(ContentUris.parseId(uri).toString()))
                count = db.update(ContractionContract.Contractions.TABLE_NAME, values, finalWhere, finalWhereArgs)
            }
            else -> throw IllegalArgumentException("Unknown URI $uri")
        }
        context!!.contentResolver.notifyChange(uri, null)
        return count
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    internal class DatabaseHelper(
            context: Context
    ) : SQLiteOpenHelper(context, ContractionProvider.DATABASE_NAME, null,
            ContractionProvider.DATABASE_VERSION) {

        /**
         * Creates the underlying database with table name and column names taken from the ContractionContract class.
         */
        override fun onCreate(db: SQLiteDatabase) {
            if (BuildConfig.DEBUG)
                Log.d(ContractionProvider.TAG, "Creating the ${ContractionContract.Contractions.TABLE_NAME} table")
            db.execSQL("CREATE TABLE " + ContractionContract.Contractions.TABLE_NAME + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ContractionContract.Contractions.COLUMN_NAME_START_TIME + " INTEGER,"
                    + ContractionContract.Contractions.COLUMN_NAME_END_TIME + " INTEGER,"
                    + ContractionContract.Contractions.COLUMN_NAME_NOTE + " TEXT);")
        }

        /**
         * Demonstrates that the provider must consider what happens when the underlying database is changed. Note that
         * this currently just destroys and recreates the database - should upgrade in place
         */
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (BuildConfig.DEBUG)
                Log.w(ContractionProvider.TAG, "Upgrading database from version $oldVersion to $newVersion, which will destroy all old data")
            db.execSQL("DROP TABLE IF EXISTS " + ContractionContract.Contractions.TABLE_NAME)
            onCreate(db)
        }
    }
}