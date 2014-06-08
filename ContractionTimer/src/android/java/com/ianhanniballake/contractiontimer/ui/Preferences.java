package com.ianhanniballake.contractiontimer.ui;

import android.app.backup.BackupManager;
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

import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler;
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateService;
import com.ianhanniballake.contractiontimer.provider.ContractionContract;
import com.ianhanniballake.contractiontimer.tagmanager.GtmManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

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
    /**
     * Notification Enabled preference name
     */
    public static final String NOTIFICATION_ENABLE_PREFERENCE_KEY = "notification_enable";
    private final static String TAG = Preferences.class.getSimpleName();
    private static final String CONTRACTIONS_FILE_NAME = "contractions.csv";
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Preferences selected home");
            GtmManager.getInstance(this).pushEvent("Home");
        }
        switch (item.getItemId()) {
            case R.id.menu_export:
                new ExportContractionsAsyncTask().execute();
                return true;
            case R.id.menu_import:
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
            Log.d(TAG, "Lock Portrait: " + isLockPortrait);
        if (isLockPortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        GtmManager gtmManager = GtmManager.getInstance(this);
        if (key.equals(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY)) {
            final boolean newIsKeepScreenOn = sharedPreferences.getBoolean(Preferences.KEEP_SCREEN_ON_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_settings_keep_screen_on_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Keep Screen On: " + newIsKeepScreenOn);
            gtmManager.pushPreferenceChanged("Keep Screen On", newIsKeepScreenOn);
        } else if (key.equals(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY)) {
            final boolean newIsLockPortrait = sharedPreferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_settings_lock_portrait_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Lock Portrait: " + newIsLockPortrait);
            gtmManager.pushPreferenceChanged("Lock Portrait", newIsLockPortrait);
            if (newIsLockPortrait)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else if (key.equals(Preferences.APPWIDGET_BACKGROUND_PREFERENCE_KEY)) {
            final String newAppwidgetBackground = appwidgetBackgroundListPreference.getValue();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Appwidget Background: " + newAppwidgetBackground);
            gtmManager.pushPreferenceChanged("Appwidget Background", newAppwidgetBackground);
            appwidgetBackgroundListPreference.setSummary(appwidgetBackgroundListPreference.getEntry());
            AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
        } else if (key.equals(Preferences.NOTIFICATION_ENABLE_PREFERENCE_KEY)) {
            final boolean newNotifcationEnabled = sharedPreferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_notification_enable_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Notification Enabled: " + newNotifcationEnabled);
            gtmManager.pushPreferenceChanged("Notification Enabled", newNotifcationEnabled);
            NotificationUpdateService.updateNotification(this);
        } else if (key.equals(Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY)) {
            final String newAverageTimeFrame = averageTimeFrameListPreference.getValue();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Average Time Frame: " + newAverageTimeFrame);
            gtmManager.pushPreferenceChanged("Average Time Frame", newAverageTimeFrame);
            final Editor editor = sharedPreferences.edit();
            editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_MAIN_PREFERENCE_KEY, true);
            editor.putBoolean(Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY, true);
            editor.commit();
            averageTimeFrameListPreference.setSummary(getString(R.string.pref_settings_average_time_frame_summary)
                    + "\n" + averageTimeFrameListPreference.getEntry());
            AppWidgetUpdateHandler.createInstance().updateAllWidgets(this);
            NotificationUpdateService.updateNotification(this);
        } else if (key.equals(Preferences.ANALYTICS_PREFERENCE_KEY)) {
            final boolean newCollectAnalytics = sharedPreferences.getBoolean(Preferences.ANALYTICS_PREFERENCE_KEY,
                    getResources().getBoolean(R.bool.pref_privacy_analytics_default));
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Analytics: " + newCollectAnalytics);
            gtmManager.pushPreferenceChanged("Analytics", newCollectAnalytics);
            gtmManager.push("optOut", newCollectAnalytics);
        }
        new BackupManager(this).dataChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GtmManager.getInstance(this).pushOpenScreen("Preferences");
    }

    private class ExportContractionsAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            ArrayList<String[]> contractions = new ArrayList<String[]>();
            Cursor data = getContentResolver().query(ContractionContract.Contractions.CONTENT_URI, null, null, null,
                    null);
            if (data != null) {
                while (data.moveToNext()) {
                    String[] contraction = new String[3];
                    final int startTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                            .COLUMN_NAME_START_TIME);
                    final long startTime = data.getLong(startTimeColumnIndex);
                    contraction[0] = Long.toString(startTime);
                    final int endTimeColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                            .COLUMN_NAME_END_TIME);
                    if (!data.isNull(endTimeColumnIndex)) {
                        final long endTime = data.getLong(endTimeColumnIndex);
                        contraction[1] = Long.toString(endTime);
                    } else
                        contraction[1] = "";
                    final int noteColumnIndex = data.getColumnIndex(ContractionContract.Contractions
                            .COLUMN_NAME_NOTE);
                    if (!data.isNull(noteColumnIndex)) {
                        final String note = data.getString(noteColumnIndex);
                        contraction[2] = note;
                    } else
                        contraction[2] = "";
                    contractions.add(contraction);
                }
                data.close();
            }
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (path == null)
                return "Could not access external storage";
            File output = new File(path, CONTRACTIONS_FILE_NAME);
            CSVWriter writer = null;
            try {
                writer = new CSVWriter(new FileWriter(output));
                writer.writeNext(new String[]{"Start Time", "End Time", "Note"});
                writer.writeAll(contractions);
            } catch (IOException e) {
                Log.e(TAG, "Error writing contractions", e);
                return "Error writing contractions";
            } finally {
                if (writer != null)
                    try {
                        writer.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing output stream", e);
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

    private class ImportContractionsAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (path == null)
                return "Could not access external storage";
            File input = new File(path, CONTRACTIONS_FILE_NAME);
            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
            try {
                CSVReader reader = new CSVReader(new FileReader(input), CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.DEFAULT_QUOTE_CHARACTER, 1);
                List<String[]> contractions = reader.readAll();
                reader.close();
                final int size = contractions.size();
                ContentResolver resolver = getContentResolver();
                String[] projection = new String[]{BaseColumns._ID};
                for (String[] contraction : contractions) {
                    ContentValues values = new ContentValues();
                    final long startTime = Long.parseLong(contraction[0]);
                    values.put(ContractionContract.Contractions.COLUMN_NAME_START_TIME,
                            startTime);
                    if (!TextUtils.isEmpty(contraction[1]))
                        values.put(ContractionContract.Contractions.COLUMN_NAME_END_TIME,
                                Long.parseLong(contraction[1]));
                    if (!TextUtils.isEmpty(contraction[2]))
                        values.put(ContractionContract.Contractions.COLUMN_NAME_NOTE,
                                contraction[2]);
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
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Could not find file", e);
                return "Could not find " + CONTRACTIONS_FILE_NAME + " in Download folder";
            } catch (IOException e) {
                Log.e(TAG, "Error reading file", e);
                return "Error reading file";
            }
            try {
                getContentResolver().applyBatch(ContractionContract.AUTHORITY, operations);
            } catch (RemoteException e) {
                Log.e(TAG, "Error saving contractions", e);
                return "Error saving contractions";
            } catch (OperationApplicationException e) {
                Log.e(TAG, "Error saving contractions", e);
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
