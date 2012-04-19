package com.ianhanniballake.contractiontimer.analytics;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.ianhanniballake.contractiontimer.BuildConfig;
import com.ianhanniballake.contractiontimer.R;
import com.ianhanniballake.contractiontimer.ui.Preferences;

/**
 * Provides access to the Analytics handler via asynchronous updates on a worker
 * thread
 */
public class AnalyticsManagerService extends IntentService
{
	/**
	 * Action associated with starting a new Analytics session
	 */
	public final static String ACTION_START_NEW_SESSION = "com.ianhanniballake.contractiontimer.START_NEW_SESSION";
	/**
	 * Action associated with stopping an existing Analytics session
	 */
	public final static String ACTION_STOP_SESSION = "com.ianhanniballake.contractiontimer.STOP_SESSION";
	/**
	 * Action associated with tracking page (Activity or DialogFragment) views
	 */
	public final static String ACTION_TOGGLE_ANALYTICS = "com.ianhanniballake.contractiontimer.TOGGLE_ANALYTICS";
	/**
	 * Action associated with tracking Analytics events
	 */
	public final static String ACTION_TRACK_EVENT = "com.ianhanniballake.contractiontimer.TRACK_EVENT";
	/**
	 * Action associated with tracking page (Activity or DialogFragment) views
	 */
	public final static String ACTION_TRACK_PAGE_VIEW = "com.ianhanniballake.contractiontimer.TRACK_PAGE_VIEW";
	/**
	 * Valid Google Analytics Web Property ID to log analytics to
	 */
	private final static String ANALYTICS_PROPERTY_ID = "UA-25785295-1";
	/**
	 * Extra holding an event's unique (for the given category) identifier
	 */
	public final static String EXTRA_ACTION = "com.ianhanniballake.contractiontimer.ACTION";
	/**
	 * Extra holding an event's category or grouping
	 */
	public final static String EXTRA_CATEGORY = "com.ianhanniballake.contractiontimer.CATEGORY";
	/**
	 * Extra holding an event's optional label
	 */
	public final static String EXTRA_LABEL = "com.ianhanniballake.contractiontimer.LABEL";
	/**
	 * Extra holding a page (Activity or DialogFragment) name
	 */
	public final static String EXTRA_PAGE_NAME = "com.ianhanniballake.contractiontimer.PAGE_NAME";
	/**
	 * Extra holding an event's optional value
	 */
	public final static String EXTRA_VALUE = "com.ianhanniballake.contractiontimer.VALUE";

	/**
	 * Helper method that asynchronously creates a new Analytics session
	 * 
	 * @param context
	 *            Context used to create the asynchronous IntentService instance
	 */
	public static void startSession(final Context context)
	{
		final Intent service = new Intent(context,
				AnalyticsManagerService.class);
		service.setAction(AnalyticsManagerService.ACTION_START_NEW_SESSION);
		context.startService(service);
	}

	/**
	 * Helper method that asynchronously stops an existing Analytics session
	 * 
	 * @param context
	 *            Context used to create the asynchronous IntentService instance
	 */
	public static void stopSession(final Context context)
	{
		final Intent service = new Intent(context,
				AnalyticsManagerService.class);
		service.setAction(AnalyticsManagerService.ACTION_STOP_SESSION);
		context.startService(service);
	}

	/**
	 * Stops the current session, dispatching events if not in debug mode
	 * 
	 * @param tracker
	 *            GoogleAnalyticsTracker instance
	 */
	private static void stopSession(final GoogleAnalyticsTracker tracker)
	{
		tracker.stopSession();
		if (!BuildConfig.DEBUG)
			tracker.dispatch();
	}

	/**
	 * Toogle Google Analytics on or off (new value should be added to the
	 * Preferences)
	 * 
	 * @param context
	 *            Context used to create the asynchronous IntentService instance
	 */
	public static void toggleAnalytics(final Context context)
	{
		if (context == null)
			return;
		final Intent service = new Intent(context,
				AnalyticsManagerService.class);
		service.setAction(AnalyticsManagerService.ACTION_TOGGLE_ANALYTICS);
		context.startService(service);
	}

	/**
	 * Helper method that asynchronously logs a new event without a label or
	 * value
	 * 
	 * @param context
	 *            Context used to create the asynchronous IntentService instance
	 * @param category
	 *            Event's category or grouping
	 * @param action
	 *            Event's unique (for the given category) identifier
	 */
	public static void trackEvent(final Context context, final String category,
			final String action)
	{
		if (context == null)
			return;
		final Intent service = new Intent(context,
				AnalyticsManagerService.class);
		service.setAction(AnalyticsManagerService.ACTION_TRACK_EVENT);
		service.putExtra(AnalyticsManagerService.EXTRA_CATEGORY, category);
		service.putExtra(AnalyticsManagerService.EXTRA_ACTION, action);
		service.putExtra(AnalyticsManagerService.EXTRA_LABEL, "");
		service.putExtra(AnalyticsManagerService.EXTRA_VALUE, 0);
		context.startService(service);
	}

	/**
	 * Helper method that asynchronously logs a new event without a value
	 * 
	 * @param context
	 *            Context used to create the asynchronous IntentService instance
	 * @param category
	 *            Event's category or grouping
	 * @param action
	 *            Event's unique (for the given category) identifier
	 * @param label
	 *            Event's label
	 */
	public static void trackEvent(final Context context, final String category,
			final String action, final String label)
	{
		if (context == null)
			return;
		final Intent service = new Intent(context,
				AnalyticsManagerService.class);
		service.setAction(AnalyticsManagerService.ACTION_TRACK_EVENT);
		service.putExtra(AnalyticsManagerService.EXTRA_CATEGORY, category);
		service.putExtra(AnalyticsManagerService.EXTRA_ACTION, action);
		service.putExtra(AnalyticsManagerService.EXTRA_LABEL, label);
		service.putExtra(AnalyticsManagerService.EXTRA_VALUE, 0);
		context.startService(service);
	}

	/**
	 * Helper method that asynchronously logs a new event
	 * 
	 * @param context
	 *            Context used to create the asynchronous IntentService instance
	 * @param category
	 *            Event's category or grouping
	 * @param action
	 *            Event's unique (for the given category) identifier
	 * @param label
	 *            Event's label
	 * @param value
	 *            Event's value
	 */
	public static void trackEvent(final Context context, final String category,
			final String action, final String label, final int value)
	{
		if (context == null)
			return;
		final Intent service = new Intent(context,
				AnalyticsManagerService.class);
		service.setAction(AnalyticsManagerService.ACTION_TRACK_EVENT);
		service.putExtra(AnalyticsManagerService.EXTRA_CATEGORY, category);
		service.putExtra(AnalyticsManagerService.EXTRA_ACTION, action);
		service.putExtra(AnalyticsManagerService.EXTRA_LABEL, label);
		service.putExtra(AnalyticsManagerService.EXTRA_VALUE, value);
		context.startService(service);
	}

	/**
	 * Helper method that asynchronously logs a new Activity view
	 * 
	 * @param activity
	 *            Activity to be viewed
	 */
	public static void trackPageView(final Activity activity)
	{
		if (activity == null)
			return;
		final Intent service = new Intent(activity,
				AnalyticsManagerService.class);
		service.setAction(AnalyticsManagerService.ACTION_TRACK_PAGE_VIEW);
		service.putExtra(AnalyticsManagerService.EXTRA_PAGE_NAME, "/"
				+ activity.getClass().getSimpleName());
		activity.startService(service);
	}

	/**
	 * Helper method that asynchronously logs a new DialogFragment view
	 * 
	 * @param context
	 *            Context used to create the asynchronous IntentService instance
	 * @param dialogFragment
	 *            DialogFragment to be viewed
	 */
	public static void trackPageView(final Context context,
			final DialogFragment dialogFragment)
	{
		if (context == null)
			return;
		final Intent service = new Intent(context,
				AnalyticsManagerService.class);
		service.setAction(AnalyticsManagerService.ACTION_TRACK_PAGE_VIEW);
		service.putExtra(AnalyticsManagerService.EXTRA_PAGE_NAME, "/"
				+ dialogFragment.getClass().getSimpleName());
		context.startService(service);
	}

	/**
	 * Creates a new ControlAppWidgetService
	 */
	public AnalyticsManagerService()
	{
		super(AnalyticsManagerService.class.getSimpleName());
	}

	@Override
	protected void onHandleIntent(final Intent intent)
	{
		final String intentAction = intent.getAction();
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Handling " + intentAction);
		final GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker
				.getInstance();
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean enableAnalytics = preferences.getBoolean(
				Preferences.ANALYTICS_PREFERENCE_KEY, getResources()
						.getBoolean(R.bool.pref_privacy_analytics_default));
		if (AnalyticsManagerService.ACTION_TOGGLE_ANALYTICS
				.equals(intentAction))
		{
			if (enableAnalytics)
			{
				startSession(tracker);
				tracker.trackEvent("Preferences", "Analytics",
						Boolean.toString(enableAnalytics), 0);
			}
			else
			{
				tracker.trackEvent("Preferences", "Analytics",
						Boolean.toString(enableAnalytics), 0);
				AnalyticsManagerService.stopSession(tracker);
			}
			return;
		}
		// Don't collect analytics if analytics are disabled
		if (!enableAnalytics)
			return;
		if (AnalyticsManagerService.ACTION_START_NEW_SESSION
				.equals(intentAction))
			startSession(tracker);
		else if (AnalyticsManagerService.ACTION_TRACK_PAGE_VIEW
				.equals(intentAction))
		{
			final String pageName = intent
					.getStringExtra(AnalyticsManagerService.EXTRA_PAGE_NAME);
			tracker.trackPageView(pageName);
		}
		else if (AnalyticsManagerService.ACTION_TRACK_EVENT
				.equals(intentAction))
		{
			final String category = intent
					.getStringExtra(AnalyticsManagerService.EXTRA_CATEGORY);
			final String action = intent
					.getStringExtra(AnalyticsManagerService.EXTRA_ACTION);
			final String label = intent
					.getStringExtra(AnalyticsManagerService.EXTRA_LABEL);
			final int value = intent.getIntExtra(
					AnalyticsManagerService.EXTRA_VALUE, 0);
			tracker.trackEvent(category, action, label, value);
		}
		else if (AnalyticsManagerService.ACTION_STOP_SESSION
				.equals(intentAction))
			AnalyticsManagerService.stopSession(tracker);
	}

	/**
	 * Create a new sesion in manual dispatch mode, anonymizing IP addresses
	 * 
	 * @param tracker
	 *            GoogleAnalyticsTracker instance
	 */
	private void startSession(final GoogleAnalyticsTracker tracker)
	{
		// Start the tracker in manual dispatch mode
		tracker.startNewSession(AnalyticsManagerService.ANALYTICS_PROPERTY_ID,
				this);
		tracker.setAnonymizeIp(true);
		final String productVersion = "Android-" + Build.VERSION.RELEASE;
		String appVersion = "UNKNOWN";
		final PackageManager pm = getPackageManager();
		PackageInfo packageInfo = null;
		try
		{
			packageInfo = pm.getPackageInfo(getPackageName(), 0);
		} catch (final NameNotFoundException e)
		{
			// Nothing to do
		}
		if (packageInfo != null)
			appVersion = packageInfo.versionName;
		tracker.setProductVersion(productVersion, appVersion);
		if (BuildConfig.DEBUG)
			Log.d(getClass().getSimpleName(), "Product Version: "
					+ productVersion + "; " + appVersion);
		tracker.setDebug(BuildConfig.DEBUG);
	}
}
