package com.ianhanniballake.contractiontimer.ui

import android.content.ContentUris
import android.content.Context
import android.content.pm.ActivityInfo
import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.cursoradapter.widget.CursorAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.viewpager.widget.ViewPager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.provider.ContractionContract.Contractions

/**
 * Stand alone activity used to view the details of an individual contraction
 */
class ViewActivity : AppCompatActivity(),
        LoaderManager.LoaderCallbacks<Cursor>,
        ViewPager.OnPageChangeListener {
    companion object {
        private const val TAG = "ViewActivity"
    }

    /**
     * Adapter for all contractions
     */
    internal lateinit var adapter: CursorAdapter
    /**
     * Currently shown page
     */
    internal var currentPosition = -1
    /**
     * Pager Adapter to manage view contraction pages
     */
    private lateinit var pagerAdapter: ViewFragmentPagerAdapter

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (findViewById<View>(R.id.pager) == null) {
            // A null pager means we no longer need this activity
            finish()
            return
        }
        adapter = object : CursorAdapter(this, null, 0) {
            override fun bindView(view: View, context: Context, cursor: Cursor) {
                // Nothing to do
            }

            override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View? {
                return null
            }
        }
        val pager = findViewById<ViewPager>(R.id.pager)
        pager.addOnPageChangeListener(this)
        pagerAdapter = ViewFragmentPagerAdapter(supportFragmentManager)
        supportLoaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
            this, Contractions.CONTENT_URI, null,
            null, null, Contractions.COLUMN_NAME_START_TIME
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_view, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.swapCursor(null)
        pagerAdapter.notifyDataSetChanged()
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
        pagerAdapter.notifyDataSetChanged()
        // A null content uri means we should cancel out as we are in an indeterminate state
        val contentUri = intent?.data
        if (contentUri == null) {
            finish()
            return
        }
        val contractionId: Long
        try {
            contractionId = ContentUris.parseId(contentUri)
        } catch (e: NumberFormatException) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, "NumberFormatException in onLoadFinished", e)
            else {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
            finish()
            return
        }

        val count = adapter.count
        for (position in 0 until count) {
            val id = adapter.getItemId(position)
            if (id == contractionId) {
                currentPosition = position
                break
            }
        }
        if (currentPosition == -1)
            finish()
        val pager = findViewById<ViewPager>(R.id.pager)
        pager.adapter = pagerAdapter
        pager.setCurrentItem(currentPosition, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "View selected home")
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // Nothing to do
    }

    override fun onPageScrollStateChanged(state: Int) {
        // Nothing to do
    }

    override fun onPageSelected(position: Int) {
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Swapped to $position")
        currentPosition = position
        val newContractionId = adapter.getItemId(position)
        intent.data = ContentUris.withAppendedId(
            Contractions.CONTENT_ID_URI_BASE, newContractionId
        )
    }

    override fun onResume() {
        super.onResume()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isLockPortrait = preferences.getBoolean(Preferences.LOCK_PORTRAIT_PREFERENCE_KEY,
                resources.getBoolean(R.bool.pref_lock_portrait_default))
        if (BuildConfig.DEBUG)
            Log.d(TAG, "Lock Portrait: $isLockPortrait")
        requestedOrientation = if (isLockPortrait)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else
            ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    override fun onStart() {
        super.onStart()
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            elevation = 0f
        }
        ViewCompat.setElevation(findViewById(R.id.pager_title_strip),
                resources.getDimension(R.dimen.action_bar_elevation))
        if (intent.hasExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA)) {
            val widgetIdentifier = intent.getStringExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA)
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Launched from $widgetIdentifier")
            FirebaseAnalytics.getInstance(this).logEvent("${widgetIdentifier}_view_launch", null)
            intent.removeExtra(MainActivity.LAUNCHED_FROM_WIDGET_EXTRA)
        }
    }

    /**
     * Creates ViewFragments as necessary
     */
    private inner class ViewFragmentPagerAdapter(
            fm: FragmentManager
    ) : FragmentStatePagerAdapter(fm) {

        override fun getCount(): Int {
            return adapter.count
        }

        override fun getItem(position: Int): Fragment {
            return ViewFragment.createInstance(adapter.getItemId(position))
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (currentPosition) {
                position + 1 -> getText(R.string.detail_previous_page)
                position - 1 -> getText(R.string.detail_next_page)
                else -> null
            }
        }
    }
}
