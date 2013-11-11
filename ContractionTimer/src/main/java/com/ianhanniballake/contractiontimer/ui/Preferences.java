package com.ianhanniballake.contractiontimer.ui;

import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.backup.BackupController;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Activity managing the various application preferences
 */
public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    /**
     * Analytics preference name
     */
    public static final String ANALYTICS_PREFERENCE_KEY = "analytics";
    /**
     * Appwidget Background preference name
     */
    public static final String APPWIDGET_BACKGROUND_PREFERENCE_KEY = "appwidget_background";
    /**
     * Average Time Frame recently changed for ContractionAverageFragment preference name
     */
    public static final String AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY = "average_time_frame_changed_fragment";
    /**
     * Average Time Frame recently changed for MainActivity preference name
     */
    public static final String AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY = "average_time_frame_changed_main";
    /**
     * Average Time Frame preference name
     */
    public static final String AVERAGE_TIME_FRAME_PREFERENCE_KEY = "average_time_frame";
    /**
     * Keep Screen On preference name
     */
    public static final String KEEP_SCREEN_ON_PREFERENCE_KEY = "keepScreenOn";
    /**
     * Lock Portrait preference name
     */
    public static final String LOCK_PORTRAIT_PREFERENCE_KEY = "lock_portrait";
    private static final String CONTRACTIONS_FILE_NAME = "Contractions.json";
    /**
     * Reference to the ListPreference corresponding with the Appwidget background
     */
    private ListPreference appwidgetBackgroundListPreference;
    /**
     * Reference to the ListPreference corresponding with the average time frame
     */
    private ListPreference averageTimeFrameListPreference;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_settings);
        appwidgetBackgroundListPreference = (ListPreference) getPreferenceScreen().findPreference(
                Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY);
        appwidgetBackgroundListPreference.setSummary(appwidgetBackgroundListPreference.getEntry());
        averageTimeFrameListPreference = (ListPreference) getPreferenceScreen().findPreference(
                Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY);
        averageTimeFrameListPreference.setSummary(getString(R.string.pref_settings_average_time_frame_summary) + "\n"
                + averageTimeFrameListPreference.getEntry());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_settings, menu);
        // Only show export/import options on Froyo+ devices
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_export:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
                    new ExportContractionsAsyncTask().execute();
                return true;
            case R.id.menu_import:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
                    new ImportContractionsAsyncTask().execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY, getResources()
                .getBoolean(R.bool.pref_settings_lock_portrait_default));
        if (BuildConfig.DEBUG)
            Log.d(getClass().getSimpleName(), "Lock Portrait: " + isLockPortrait);
        if (isLockPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (key.equals(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY)) {
            final boolean newIsKeepScreenOn = sharedPreferences.getBoolean(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_settings_keep_screen_on_default));
            if (BuildConfig.DEBUG)
                Log.d(getClass().getSimpleName(), "Keep Screen On: " + newIsKeepScreenOn);
            EasyTracker.getTracker().sendEvent("Preferences", "Keep Screen On", Boolean.toString(newIsKeepScreenOn),
                    0L);
        } else if (key.equals(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY)) {
            final boolean newIsLockPortrait = sharedPreferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_settings_lock_portrait_default));
            if (BuildConfig.DEBUG)
                Log.d(getClass().getSimpleName(), "Lock Portrait: " + newIsLockPortrait);
            EasyTracker.getTracker()
                    .sendEvent("Preferences", "Lock Portrait", Boolean.toString(newIsLockPortrait), 0L);
            if (newIsLockPortrait)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else if (key.equals(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY)) {
            final String newAppwidgetBackground = appwidgetBackgroundListPreference.getValue();
            if (BuildConfig.DEBUG)
                Log.d(getClass().getSimpleName(), "Appwidget Background: " + newAppwidgetBackground);
            EasyTracker.getTracker().sendEvent("Preferences", "Appwidget Background", newAppwidgetBackground, 0L);
            appwidgetBackgroundListPreference.setSummary(appwidgetBackgroundListPreference.getEntry());
            AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
        } else if (key.equals(Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY)) {
            final String newAverageTimeFrame = averageTimeFrameListPreference.getValue();
            if (BuildConfig.DEBUG)
                Log.d(getClass().getSimpleName(), "Average Time Frame: " + newAverageTimeFrame);
            EasyTracker.getTracker().sendEvent("Preferences", "Average Time Frame", newAverageTimeFrame, 0L);
            final Editor editor = sharedPreferences.edit();
            editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, true);
            editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY, true);
            editor.commit();
            averageTimeFrameListPreference.setSummary(getString(R.string.pref_settings_average_time_frame_summary)
                    + "\n" + averageTimeFrameListPreference.getEntry());
        } else if (key.equals(Preferences.ANALYTICS_PREFERENCE_KEY)) {
            final boolean newCollectAnalytics = sharedPreferences.getBoolean(Preferences.ANALYTICS_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_privacy_analytics_default));
            if (BuildConfig.DEBUG)
                Log.d(getClass().getSimpleName(), "Analytics: " + newCollectAnalytics);
            EasyTracker.getTracker().sendEvent("Preferences", "Analytics", Boolean.toString(newCollectAnalytics), 0L);
            GAServiceManager.getInstance().dispatch();
            GoogleAnalytics.getInstance(this).setAppOptOut(newCollectAnalytics);
        }
        BackupController.createInstance().dataChanged(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
        EasyTracker.getTracker().sendView("Preferences");
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @TargetApi(8)
    private class ExportContractionsAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            ArrayList<JSONObject> contractions = new ArrayList<JSONObject>();
            Cursor data = getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, null, null, null,
                    null);
            if (data != null) {
                while (data.moveToNext()) {
                    try {
                        JSONObject contraction = new JSONObject();
                        final int startTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                                .COLUMN_NAME_START_TIME);
                        final long startTime = data.getLong(startTimeColumnIndex);
                        contraction.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME, startTime);
                        final int endTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                                .COLUMN_NAME_END_TIME);
                        if (!data.isNull(endTimeColumnIndex)) {
                            final long endTime = data.getLong(endTimeColumnIndex);
                            contraction.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, endTime);
                        } else
                            contraction.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME, null);
                        final int noteColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                                .COLUMN_NAME_NOTE);
                        if (!data.isNull(noteColumnIndex)) {
                            final String note = data.getString(noteColumnIndex);
                            if (!TextUtils.isEmpty(note))
                                contraction.put(ContractionContract.Contractions.COLUMN_NAME_NOTE, note);
                        }
                        contractions.add(contraction);
                    } catch (JSONException e) {
                        Log.e(Preferences.class.getSimpleName(), "Error creating JSON", e);
                        return "Error creating JSON";
                    }
                }
                data.close();
            }
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (path == null)
                return "Could not access external storage";
            File output = new File(path, CONTRACTIONS_FILE_NAME);
            BufferedOutputStream os = null;
            try {
                os = new BufferedOutputStream(new FileOutputStream(output));
                os.write(new JSONArray(contractions).toString().getBytes());
            } catch (IOException e) {
                Log.e(Preferences.class.getSimpleName(), "Error writing contractions", e);
                return "Error writing contractions";
            } finally {
                if (os != null)
                    try {
                        os.close();
                    } catch (IOException e) {
                        Log.e(Preferences.class.getSimpleName(), "Error closing output stream", e);
                    }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String error) {
            if (TextUtils.isEmpty(error))
                Toast.makeText(Preferences.this, "Export of " + CONTRACTIONS_FILE_NAME +
                        " to Download folder successful", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(Preferences.this, "Export failed: " + error, Toast.LENGTH_LONG).show();
        }
    }

    @TargetApi(8)
    private class ImportContractionsAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (path == null)
                return "Could not access external storage";
            File input = new File(path, CONTRACTIONS_FILE_NAME);
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            try {
                Scanner scanner = new Scanner(input);
                JSONArray contractions = new JSONArray(scanner.useDelimiter("\\A").next());
                scanner.close();
                final int size = contractions.length();
                ContentResolver resolver = getContentResolver();
                String[] projection = new String[]{BaseColumns._ID};
                for (int index = 0; index < size; index++) {
                    JSONObject contraction = contractions.getJSONObject(index);
                    ContentValues values = new ContentValues();
                    final long startTime = contraction.getLong(ContractionContract.Contractions
                            .COLUMN_NAME_START_TIME);
                    values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                            startTime);
                    if (contraction.has(ContractionContract.Contractions.COLUMN_NAME_END_TIME))
                        values.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                                contraction.getLong(ContractionContract.Contractions.COLUMN_NAME_END_TIME));
                    if (contraction.has(ContractionContract.Contractions.COLUMN_NAME_NOTE))
                        values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE,
                                contraction.getString(ContractionContract.Contractions.COLUMN_NAME_NOTE));
                    Cursor existingRow = resolver.query(ContractionContract.Contractions.CONTENT_URI,
                            projection, ContractionContract.Contractions.COLUMN_NAME_START_TIME
                            + "=?", new String[]{Long.toString(startTime)}, null);
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
            } catch (FileNotFoundException e) {
                Log.e(Preferences.class.getSimpleName(), "Could not find file", e);
                return "Could not find " + CONTRACTIONS_FILE_NAME + " in Download folder";
            } catch (JSONException e) {
                Log.e(Preferences.class.getSimpleName(), "Error parsing file", e);
                return "Error parsing file";
            }
            try {
                getContentResolver().applyBatch(ContractionContract.AUTHORITY, operations);
            } catch (RemoteException e) {
                Log.e(Preferences.class.getSimpleName(), "Error saving contractions", e);
                return "Error saving contractions";
            } catch (OperationApplicationException e) {
                Log.e(Preferences.class.getSimpleName(), "Error saving contractions", e);
                return "Error saving contractions";
            }
            return null;
        }

        @Override
        protected void onPostExecute(String error) {
            if (TextUtils.isEmpty(error))
                Toast.makeText(Preferences.this, "Import successful", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(Preferences.this, "Import failed: " + error, Toast.LENGTH_LONG).show();
        }
    }
}
