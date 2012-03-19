package com.ianhanniballake.contractiontimer.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * Provides access to a database of contractions.
 */
public class ContractionProvider extends ContentProvider
{
	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	static class DatabaseHelper extends SQLiteOpenHelper
	{
		/**
		 * Creates a new DatabaseHelper
		 * 
		 * @param context
		 *            context of this database
		 */
		DatabaseHelper(final Context context)
		{
			super(context, ContractionProvider.DATABASE_NAME, null,
					ContractionProvider.DATABASE_VERSION);
		}

		/**
		 * Creates the underlying database with table name and column names
		 * taken from the ContractionContract class.
		 */
		@Override
		public void onCreate(final SQLiteDatabase db)
		{
			Log.d(ContractionProvider.TAG, "Creating the "
					+ ContractionContract.Contractions.TABLE_NAME + " table");
			db.execSQL("CREATE TABLE "
					+ ContractionContract.Contractions.TABLE_NAME + " ("
					+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ ContractionContract.Contractions.COLUMN_NAME_START_TIME
					+ " INTEGER,"
					+ ContractionContract.Contractions.COLUMN_NAME_END_TIME
					+ " INTEGER,"
					+ ContractionContract.Contractions.COLUMN_NAME_NOTE
					+ " TEXT);");
		}

		/**
		 * 
		 * Demonstrates that the provider must consider what happens when the
		 * underlying database is changed. Note that this currently just
		 * destroys and recreates the database - should upgrade in place
		 */
		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion)
		{
			Log.w(ContractionProvider.TAG, "Upgrading database from version "
					+ oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS "
					+ ContractionContract.Contractions.TABLE_NAME);
			onCreate(db);
		}
	}

	/**
	 * The incoming URI matches the Contraction ID URI pattern
	 */
	private static final int CONTRACTION_ID = 2;
	/**
	 * The incoming URI matches the Contractions URI pattern
	 */
	private static final int CONTRACTIONS = 1;
	/**
	 * The database that the provider uses as its underlying data store
	 */
	private static final String DATABASE_NAME = "contractions.db";
	/**
	 * The database version
	 */
	private static final int DATABASE_VERSION = 2;
	/**
	 * Used for debugging and logging
	 */
	private static final String TAG = "ContractionProvider";
	/**
	 * A UriMatcher instance
	 */
	private static final UriMatcher uriMatcher = ContractionProvider
			.buildUriMatcher();

	/**
	 * Creates and initializes a column project for all columns
	 * 
	 * @return The all column projection map
	 */
	private static HashMap<String, String> buildAllColumnProjectionMap()
	{
		final HashMap<String, String> allColumnProjectionMap = new HashMap<String, String>();
		allColumnProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
		allColumnProjectionMap.put(
				ContractionContract.Contractions.COLUMN_NAME_START_TIME,
				ContractionContract.Contractions.COLUMN_NAME_START_TIME);
		allColumnProjectionMap.put(
				ContractionContract.Contractions.COLUMN_NAME_END_TIME,
				ContractionContract.Contractions.COLUMN_NAME_END_TIME);
		allColumnProjectionMap.put(
				ContractionContract.Contractions.COLUMN_NAME_NOTE,
				ContractionContract.Contractions.COLUMN_NAME_NOTE);
		return allColumnProjectionMap;
	}

	/**
	 * Creates and initializes the URI matcher
	 * 
	 * @return the URI Matcher
	 */
	private static UriMatcher buildUriMatcher()
	{
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		matcher.addURI(ContractionContract.AUTHORITY,
				ContractionContract.Contractions.TABLE_NAME,
				ContractionProvider.CONTRACTIONS);
		matcher.addURI(ContractionContract.AUTHORITY,
				ContractionContract.Contractions.TABLE_NAME + "/#",
				ContractionProvider.CONTRACTION_ID);
		return matcher;
	}

	/**
	 * An identity all column projection mapping
	 */
	final HashMap<String, String> allColumnProjectionMap = ContractionProvider
			.buildAllColumnProjectionMap();
	/**
	 * Handle to a new DatabaseHelper.
	 */
	private DatabaseHelper databaseHelper;

	@Override
	public int delete(final Uri uri, final String where,
			final String[] whereArgs)
	{
		// Opens the database object in "write" mode.
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		int count;
		// Does the delete based on the incoming URI pattern.
		switch (ContractionProvider.uriMatcher.match(uri))
		{
			case CONTRACTIONS:
				// If the incoming pattern matches the general pattern for
				// contractions, does a delete based on the incoming "where"
				// column and arguments.
				count = db.delete(ContractionContract.Contractions.TABLE_NAME,
						where, whereArgs);
				break;
			case CONTRACTION_ID:
				// If the incoming URI matches a single contraction ID, does the
				// delete based on the incoming data, but modifies the where
				// clause to restrict it to the particular contraction ID.
				final String finalWhere = DatabaseUtils.concatenateWhere(
						BaseColumns._ID + " = " + ContentUris.parseId(uri),
						where);
				count = db.delete(ContractionContract.Contractions.TABLE_NAME,
						finalWhere, whereArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(final Uri uri)
	{
		/**
		 * Chooses the MIME type based on the incoming URI pattern
		 */
		switch (ContractionProvider.uriMatcher.match(uri))
		{
			case CONTRACTIONS:
				// If the pattern is for contractions, returns the general
				// content type.
				return ContractionContract.Contractions.CONTENT_TYPE;
			case CONTRACTION_ID:
				// If the pattern is for contraction IDs, returns the
				// contraction ID content type.
				return ContractionContract.Contractions.CONTENT_ITEM_TYPE;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues initialValues)
	{
		// Validates the incoming URI. Only the full provider URI is allowed for
		// inserts.
		if (ContractionProvider.uriMatcher.match(uri) != ContractionProvider.CONTRACTIONS)
			throw new IllegalArgumentException("Unknown URI " + uri);
		ContentValues values;
		if (initialValues != null)
			values = new ContentValues(initialValues);
		else
			values = new ContentValues();
		if (!values
				.containsKey(ContractionContract.Contractions.COLUMN_NAME_START_TIME))
			values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME,
					System.currentTimeMillis());
		if (!values
				.containsKey(ContractionContract.Contractions.COLUMN_NAME_NOTE))
			values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE, "");
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		final long rowId = db
				.insert(ContractionContract.Contractions.TABLE_NAME,
						ContractionContract.Contractions.COLUMN_NAME_START_TIME,
						values);
		// If the insert succeeded, the row ID exists.
		if (rowId > 0)
		{
			// Creates a URI with the contraction ID pattern and the new row ID
			// appended to it.
			final Uri contractionUri = ContentUris
					.withAppendedId(
							ContractionContract.Contractions.CONTENT_ID_URI_BASE,
							rowId);
			getContext().getContentResolver()
					.notifyChange(contractionUri, null);
			return contractionUri;
		}
		// If the insert didn't succeed, then the rowID is <= 0
		throw new SQLException("Failed to insert row into " + uri);
	}

	/**
	 * Creates the underlying DatabaseHelper
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate()
	{
		databaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder)
	{
		// Constructs a new query builder and sets its table name
		final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(ContractionContract.Contractions.TABLE_NAME);
		qb.setProjectionMap(allColumnProjectionMap);
		String finalSortOrder = sortOrder;
		if (TextUtils.isEmpty(sortOrder))
			finalSortOrder = ContractionContract.Contractions.DEFAULT_SORT_ORDER;
		switch (ContractionProvider.uriMatcher.match(uri))
		{
			case CONTRACTIONS:
				break;
			case CONTRACTION_ID:
				// If the incoming URI is for a single contraction identified by
				// its ID, appends "_ID = <contractionID>" to the where clause,
				// so that it selects that single contraction
				qb.appendWhere(BaseColumns._ID + "=" + uri.getLastPathSegment());
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		final SQLiteDatabase db = databaseHelper.getReadableDatabase();
		final Cursor c = qb.query(db, projection, selection, selectionArgs,
				null, null, finalSortOrder, null);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs)
	{
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		int count = 0;
		switch (ContractionProvider.uriMatcher.match(uri))
		{
			case CONTRACTIONS:
				// If the incoming URI matches the general contractions pattern,
				// does the update based on the incoming data.
				count = db.update(ContractionContract.Contractions.TABLE_NAME,
						values, selection, selectionArgs);
				break;
			case CONTRACTION_ID:
				// If the incoming URI matches a single contraction ID, does the
				// update based on the incoming data, but modifies the where
				// clause to restrict it to the particular contraction ID.
				final String finalWhere = DatabaseUtils.concatenateWhere(
						BaseColumns._ID + " = " + ContentUris.parseId(uri),
						selection);
				count = db.update(ContractionContract.Contractions.TABLE_NAME,
						values, finalWhere, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}