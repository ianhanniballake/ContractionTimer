package com.ianhanniballake.contractiontimer.actionbar;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

/**
 * A <em>really</em> dumb implementation of the {@link android.view.MenuItem}
 * interface, that's only useful for our actionbar-compat purposes. See
 * <code>com.android.internal.view.menu.MenuItemImpl</code> in AOSP for a more
 * complete implementation.
 */
public class SimpleMenuItem implements MenuItem
{
	/**
	 * ActionBarHelper for enable state change callbacks
	 */
	private final ActionBarHelper mActionBarHelper;
	/**
	 * If this menu item is checkable
	 */
	private boolean mCheckable = false;
	/**
	 * If this menu item is checked
	 */
	private boolean mChecked = false;
	/**
	 * If this menu item is enabled
	 */
	private boolean mEnabled = true;
	/**
	 * Menu item icon
	 */
	private Drawable mIconDrawable;
	/**
	 * Resource id associated with the menu item icon
	 */
	private int mIconResId = 0;
	/**
	 * Unique menu item id
	 */
	private final int mId;
	/**
	 * Reference to the containing menu
	 */
	private final SimpleMenu mMenu;
	/**
	 * Menu item order
	 */
	private final int mOrder;
	/**
	 * Full title for this menu item
	 */
	private CharSequence mTitle;
	/**
	 * Condensed title for this menu item
	 */
	private CharSequence mTitleCondensed;
	/**
	 * If this menu item is visible
	 */
	private boolean mVisible = true;

	/**
	 * Creates a new SimpleMenuItem
	 * 
	 * @param menu
	 *            Reference to the containing menu
	 * @param id
	 *            Unique menu item id
	 * @param order
	 *            Menu item order
	 * @param title
	 *            Full title for this menu item
	 * @param actionBarHelper
	 *            ActionBarHelper for enable state change callbacks
	 */
	public SimpleMenuItem(final SimpleMenu menu, final int id, final int order,
			final CharSequence title, final ActionBarHelper actionBarHelper)
	{
		mMenu = menu;
		mId = id;
		mOrder = order;
		mTitle = title;
		mActionBarHelper = actionBarHelper;
	}

	@Override
	public boolean collapseActionView()
	{
		// Noop
		return false;
	}

	@Override
	public boolean expandActionView()
	{
		// Noop
		return false;
	}

	@TargetApi(14)
	@Override
	public ActionProvider getActionProvider()
	{
		// Noop
		return null;
	}

	@Override
	public View getActionView()
	{
		// Noop
		return null;
	}

	@Override
	public char getAlphabeticShortcut()
	{
		// Noop
		return 0;
	}

	@Override
	public int getGroupId()
	{
		// Noop
		return 0;
	}

	@Override
	public Drawable getIcon()
	{
		if (mIconDrawable != null)
			return mIconDrawable;
		if (mIconResId != 0)
			return mMenu.getResources().getDrawable(mIconResId);
		return null;
	}

	@Override
	public Intent getIntent()
	{
		// Noop
		return null;
	}

	@Override
	public int getItemId()
	{
		return mId;
	}

	@Override
	public ContextMenu.ContextMenuInfo getMenuInfo()
	{
		// Noop
		return null;
	}

	@Override
	public char getNumericShortcut()
	{
		// Noop
		return 0;
	}

	@Override
	public int getOrder()
	{
		return mOrder;
	}

	// No-op operations. We use no-ops to allow inflation from menu XML.
	@Override
	public SubMenu getSubMenu()
	{
		// Noop
		return null;
	}

	@Override
	public CharSequence getTitle()
	{
		return mTitle;
	}

	@Override
	public CharSequence getTitleCondensed()
	{
		return mTitleCondensed != null ? mTitleCondensed : mTitle;
	}

	@Override
	public boolean hasSubMenu()
	{
		// Noop
		return false;
	}

	@Override
	public boolean isActionViewExpanded()
	{
		// Noop
		return false;
	}

	@Override
	public boolean isCheckable()
	{
		return mCheckable;
	}

	@Override
	public boolean isChecked()
	{
		return mChecked;
	}

	@Override
	public boolean isEnabled()
	{
		return mEnabled;
	}

	@Override
	public boolean isVisible()
	{
		// Noop
		return mVisible;
	}

	@TargetApi(14)
	@Override
	public MenuItem setActionProvider(final ActionProvider actionProvider)
	{
		// Noop
		return this;
	}

	@Override
	public MenuItem setActionView(final int i)
	{
		// Noop
		return this;
	}

	@Override
	public MenuItem setActionView(final View view)
	{
		// Noop
		return this;
	}

	@Override
	public MenuItem setAlphabeticShortcut(final char c)
	{
		// Noop
		return this;
	}

	@Override
	public MenuItem setCheckable(final boolean checkable)
	{
		mCheckable = checkable;
		return this;
	}

	@Override
	public MenuItem setChecked(final boolean checked)
	{
		mChecked = checked;
		return this;
	}

	@Override
	public MenuItem setEnabled(final boolean enabled)
	{
		mEnabled = enabled;
		mActionBarHelper.setEnabled(this, enabled);
		return this;
	}

	@Override
	public MenuItem setIcon(final Drawable icon)
	{
		mIconResId = 0;
		mIconDrawable = icon;
		return this;
	}

	@Override
	public MenuItem setIcon(final int iconResId)
	{
		mIconDrawable = null;
		mIconResId = iconResId;
		return this;
	}

	@Override
	public MenuItem setIntent(final Intent intent)
	{
		// Noop
		return this;
	}

	@Override
	public MenuItem setNumericShortcut(final char c)
	{
		// Noop
		return this;
	}

	@TargetApi(14)
	@Override
	public MenuItem setOnActionExpandListener(
			final OnActionExpandListener onActionExpandListener)
	{
		// Noop
		return this;
	}

	@Override
	public MenuItem setOnMenuItemClickListener(
			final OnMenuItemClickListener onMenuItemClickListener)
	{
		// Noop
		return this;
	}

	@Override
	public MenuItem setShortcut(final char c, final char c1)
	{
		// Noop
		return this;
	}

	@Override
	public void setShowAsAction(final int i)
	{
		// Noop
	}

	@Override
	public MenuItem setShowAsActionFlags(final int i)
	{
		// Noop
		return null;
	}

	@Override
	public MenuItem setTitle(final CharSequence title)
	{
		mTitle = title;
		return this;
	}

	@Override
	public MenuItem setTitle(final int titleRes)
	{
		return setTitle(mMenu.getContext().getString(titleRes));
	}

	@Override
	public MenuItem setTitleCondensed(final CharSequence title)
	{
		mTitleCondensed = title;
		return this;
	}

	@Override
	public MenuItem setVisible(final boolean b)
	{
		mVisible = b;
		mActionBarHelper.setVisible(this, mVisible);
		return this;
	}
}
