package com.ianhanniballake.contractiontimer.actionbar;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ianhanniballake.contractiontimer.R;

/**
 * A class that implements the ActionBar pattern for pre-Honeycomb devices.
 */
public class ActionBarHelperBase extends ActionBarHelper
{
	/**
	 * Layout that manages the Home button and up caret as a single unit
	 */
	private class HomeButtonLayout extends LinearLayout
	{
		/**
		 * Create a new HomeButtonLayout
		 * 
		 * @param context
		 *            Context used to create the layout
		 */
		@SuppressLint("InlinedApi")
		public HomeButtonLayout(final Context context)
		{
			super(context);
			setId(android.R.id.home);
			setContentDescription(mActivity.getString(R.string.app_name));
			final ImageButton upCaretButton = new ImageButton(mActivity, null, R.attr.actionbarCompatItemStyle);
			final int upCaretButtonWidth = (int) mActivity.getResources().getDimension(
					R.dimen.actionbar_compat_button_up_caret_width);
			upCaretButton.setLayoutParams(new ViewGroup.LayoutParams(upCaretButtonWidth,
					ViewGroup.LayoutParams.MATCH_PARENT));
			upCaretButton.setScaleType(ImageView.ScaleType.CENTER);
			upCaretButton.setDuplicateParentStateEnabled(true);
			upCaretButton.setClickable(false);
			addView(upCaretButton);
			final ImageButton homeButton = new ImageButton(mActivity, null, R.attr.actionbarCompatItemStyle);
			final int homeButtonWidth = (int) mActivity.getResources().getDimension(
					R.dimen.actionbar_compat_button_home_width);
			homeButton
					.setLayoutParams(new ViewGroup.LayoutParams(homeButtonWidth, ViewGroup.LayoutParams.MATCH_PARENT));
			homeButton.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.icon));
			homeButton.setScaleType(ImageView.ScaleType.CENTER);
			homeButton.setDuplicateParentStateEnabled(true);
			homeButton.setClickable(false);
			addView(homeButton);
			final SimpleMenu tempMenu = new SimpleMenu(mActivity, ActionBarHelperBase.this);
			final SimpleMenuItem homeItem = new SimpleMenuItem(tempMenu, android.R.id.home, 0,
					mActivity.getString(R.string.app_name), ActionBarHelperBase.this);
			setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(final View view)
				{
					mActivity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, homeItem);
				}
			});
		}

		@Override
		public void setClickable(final boolean clickable)
		{
			super.setClickable(clickable);
			final ImageButton upCaretButton = (ImageButton) getChildAt(0);
			if (clickable)
			{
				final Resources resources = mActivity.getResources();
				upCaretButton.setImageDrawable(resources.getDrawable(R.drawable.actionbar_up_caret));
			}
			else
				upCaretButton.setImageDrawable(null);
		}
	}

	/**
	 * A {@link android.view.MenuInflater} that reads ActionBar metadata.
	 */
	private class WrappedMenuInflater extends MenuInflater
	{
		/**
		 * XML Attribute associated with a menu id
		 */
		private static final String MENU_ATTR_ID = "id";
		/**
		 * XML Attribute associated with ActionBar metadata
		 */
		private static final String MENU_ATTR_SHOW_AS_ACTION = "showAsAction";
		/**
		 * Namespace used for parsing menu XML files
		 */
		private static final String MENU_RES_NAMESPACE = "http://schemas.android.com/apk/res/android";
		/**
		 * Default menu inflater
		 */
		MenuInflater mInflater;

		/**
		 * @param context
		 *            Context used to inflate menus
		 * @param inflater
		 *            Default menu inflater
		 */
		public WrappedMenuInflater(final Context context, final MenuInflater inflater)
		{
			super(context);
			mInflater = inflater;
		}

		@Override
		public void inflate(final int menuRes, final Menu menu)
		{
			loadActionBarMetadata(menuRes);
			mInflater.inflate(menuRes, menu);
		}

		/**
		 * Loads action bar metadata from a menu resource, storing a list of menu item IDs that should be shown
		 * on-screen (i.e. those with showAsAction set to always or ifRoom).
		 * 
		 * @param menuResId
		 *            Menu (by resource id) to parse for ActionBar metadata
		 */
		@SuppressLint("InlinedApi")
		private void loadActionBarMetadata(final int menuResId)
		{
			XmlResourceParser parser = null;
			try
			{
				parser = mActivity.getResources().getXml(menuResId);
				int eventType = parser.getEventType();
				int itemId;
				int showAsAction;
				boolean eof = false;
				while (!eof)
				{
					switch (eventType)
					{
						case XmlPullParser.START_TAG:
							if (!parser.getName().equals("item"))
								break;
							itemId = parser.getAttributeResourceValue(WrappedMenuInflater.MENU_RES_NAMESPACE,
									WrappedMenuInflater.MENU_ATTR_ID, 0);
							if (itemId == 0)
								break;
							showAsAction = parser.getAttributeIntValue(WrappedMenuInflater.MENU_RES_NAMESPACE,
									WrappedMenuInflater.MENU_ATTR_SHOW_AS_ACTION, 0);
							final boolean containsAlways = (MenuItem.SHOW_AS_ACTION_ALWAYS & showAsAction) > 0;
							final boolean containsIfRoom = (MenuItem.SHOW_AS_ACTION_IF_ROOM & showAsAction) > 0;
							if (containsAlways || containsIfRoom)
								mActionItemIds.add(itemId);
							break;
						case XmlPullParser.END_DOCUMENT:
							eof = true;
							break;
						default:
							break;
					}
					eventType = parser.next();
				}
			} catch (final XmlPullParserException e)
			{
				throw new InflateException("Error inflating menu XML", e);
			} catch (final IOException e)
			{
				throw new InflateException("Error inflating menu XML", e);
			} finally
			{
				if (parser != null)
					parser.close();
			}
		}
	}

	/**
	 * Menu ids to be added to the ActionBar
	 */
	final Set<Integer> mActionItemIds = new HashSet<Integer>();
	/**
	 * Reference to the created menu
	 */
	private Menu mMenu;
	/**
	 * Whether we are currently setting up the options menu
	 */
	private boolean settingUpCreateOptionsMenu = false;
	/**
	 * Whether we should show the home button as an 'Up' button
	 */
	private boolean showHomeAsUp = false;

	/**
	 * @param activity
	 *            Activity to decorate with ActionBar
	 */
	protected ActionBarHelperBase(final Activity activity)
	{
		super(activity);
	}

	/**
	 * Adds an action button to the compatibility action bar, using menu information from a
	 * {@link android.view.MenuItem}.
	 * 
	 * @param item
	 *            MenuItem to create as ActionBar icon
	 */
	private void addActionItemCompatFromMenuItem(final MenuItem item)
	{
		final int itemId = item.getItemId();
		final ViewGroup actionBar = getActionBarCompat();
		if (actionBar == null)
			return;
		// Create the button
		final ImageButton actionButton = new ImageButton(mActivity, null, R.attr.actionbarCompatItemStyle);
		actionButton.setId(itemId);
		final int buttonWidth = (int) mActivity.getResources().getDimension(R.dimen.actionbar_compat_button_width);
		actionButton.setLayoutParams(new ViewGroup.LayoutParams(buttonWidth, ViewGroup.LayoutParams.MATCH_PARENT));
		actionButton.setImageDrawable(item.getIcon());
		actionButton.setScaleType(ImageView.ScaleType.CENTER);
		actionButton.setContentDescription(item.getTitle());
		actionButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				mActivity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
			}
		});
		actionBar.addView(actionButton);
	}

	/**
	 * Returns the {@link android.view.ViewGroup} for the action bar on phones (compatibility action bar).
	 * 
	 * @return ViewGroup representing the ActionBar
	 */
	private ViewGroup getActionBarCompat()
	{
		return (ViewGroup) mActivity.findViewById(R.id.actionbar_compat);
	}

	/**
	 * Returns a {@link android.view.MenuInflater} that can read action bar metadata on pre-Honeycomb devices.
	 */
	@Override
	public MenuInflater getMenuInflater(final MenuInflater superMenuInflater)
	{
		return new WrappedMenuInflater(mActivity, superMenuInflater);
	}

	/** {@inheritDoc} */
	@Override
	public void onCreate()
	{
		mActivity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	}

	/**
	 * Action bar helper code to be run in {@link Activity#onCreateOptionsMenu(android.view.Menu)}.
	 * 
	 * NOTE: This code will mark on-screen menu items as invisible.
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		mMenu = menu;
		// Hides on-screen action items from the options menu.
		settingUpCreateOptionsMenu = true;
		for (final Integer id : mActionItemIds)
		{
			final MenuItem item = menu.findItem(id);
			if (item != null)
				item.setVisible(false);
		}
		settingUpCreateOptionsMenu = false;
		return menu.hasVisibleItems();
	}

	/** {@inheritDoc} */
	@Override
	public void onPostCreate()
	{
		mActivity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.actionbar_compat);
		setupActionBar();
		final SimpleMenu menu = new SimpleMenu(mActivity, this);
		mActivity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu);
		mActivity.onPrepareOptionsMenu(menu);
		for (int i = 0; i < menu.size(); i++)
		{
			final MenuItem item = menu.getItem(i);
			if (mActionItemIds.contains(item.getItemId()))
				addActionItemCompatFromMenuItem(item);
		}
	}

	@SuppressLint("InlinedApi")
	@Override
	public void setDisplayHomeAsUpEnabled(final boolean showHomeAsUp)
	{
		this.showHomeAsUp = showHomeAsUp;
		final ViewGroup actionBar = getActionBarCompat();
		if (actionBar == null)
			return;
		final View homeButton = actionBar.findViewById(android.R.id.home);
		if (homeButton == null)
			return;
		homeButton.setClickable(showHomeAsUp);
	}

	@Override
	public void setEnabled(final MenuItem item, final boolean enabled)
	{
		final int itemId = item.getItemId();
		if (mActionItemIds.contains(itemId))
		{
			final View menuButton = getActionBarCompat().findViewById(itemId);
			if (menuButton != null)
				menuButton.setEnabled(enabled);
		}
	}

	/**
	 * Sets up the compatibility action bar with the given title.
	 */
	private void setupActionBar()
	{
		final ViewGroup actionBarCompat = getActionBarCompat();
		if (actionBarCompat == null)
			return;
		final LinearLayout.LayoutParams springLayoutParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.MATCH_PARENT);
		springLayoutParams.weight = 1;
		// Add Home button
		final HomeButtonLayout homeButtonLayout = new HomeButtonLayout(mActivity);
		homeButtonLayout.setClickable(showHomeAsUp);
		actionBarCompat.addView(homeButtonLayout);
		// Add title text
		final TextView titleText = new TextView(mActivity, null, R.attr.actionbarCompatTitleStyle);
		titleText.setLayoutParams(springLayoutParams);
		titleText.setText(mActivity.getTitle());
		actionBarCompat.addView(titleText);
	}

	@Override
	public void setVisible(final MenuItem item, final boolean visible)
	{
		if (settingUpCreateOptionsMenu)
			return;
		final int itemId = item.getItemId();
		if (mActionItemIds.contains(itemId))
		{
			final View menuButton = getActionBarCompat().findViewById(itemId);
			if (menuButton != null)
				if (visible)
					menuButton.setVisibility(View.VISIBLE);
				else
					menuButton.setVisibility(View.GONE);
		}
	}

	@Override
	public void supportInvalidateOptionsMenu()
	{
		// Usually we wait until the menu is shown before invalidating options,
		// but if there are any action bar buttons, we need to invalidate now to
		// ensure their state is correct
		if (!mActionItemIds.isEmpty())
			mActivity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, mMenu);
	}
}
