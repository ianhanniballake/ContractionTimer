package com.ianhanniballake.contractiontimer.ui

import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.provider.ContractionContract

/**
 * Fragment which displays the average duration and frequency
 */
class ContractionAverageFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val projection = arrayOf(
            ContractionContract.Contractions.COLUMN_NAME_START_TIME,
            ContractionContract.Contractions.COLUMN_NAME_END_TIME
        )
        val selection = ContractionContract.Contractions.COLUMN_NAME_START_TIME + ">?"
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val averagesTimeFrame = preferences.getString(
            Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
            getString(R.string.pref_average_time_frame_default)
        )!!.toLong()
        val timeCutoff = System.currentTimeMillis() - averagesTimeFrame
        val selectionArgs = arrayOf(timeCutoff.toString())
        return CursorLoader(
            requireContext(), requireActivity().intent!!.data!!, projection, selection,
            selectionArgs, null
        )
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_contraction_average, container, false)

    override fun onLoaderReset(loader: Loader<Cursor>) {
        val view = view ?: return
        val averageLayout = view.findViewById<View>(R.id.average_layout)
        val averageDurationView = view.findViewById<TextView>(R.id.average_duration)
        val averageFrequencyView = view.findViewById<TextView>(R.id.average_frequency)
        averageLayout.visibility = View.GONE
        averageDurationView.text = ""
        averageFrequencyView.text = ""
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        val view = view ?: return
        val averageLayout = view.findViewById<View>(R.id.average_layout)
        val averageDurationView = view.findViewById<TextView>(R.id.average_duration)
        val averageFrequencyView = view.findViewById<TextView>(R.id.average_frequency)
        if (data == null || !data.moveToFirst()) {
            averageLayout.visibility = View.GONE
            averageDurationView.text = ""
            averageFrequencyView.text = ""
            return
        }
        var averageDuration = 0.0
        var averageFrequency = 0.0
        var numDurations = 0
        var numFrequencies = 0
        while (!data.isAfterLast) {
            val startTimeColumnIndex = data.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_START_TIME)
            val startTime = data.getLong(startTimeColumnIndex)
            val endTimeColumnIndex = data.getColumnIndex(
                    ContractionContract.Contractions.COLUMN_NAME_END_TIME)
            if (!data.isNull(endTimeColumnIndex)) {
                val endTime = data.getLong(endTimeColumnIndex)
                val curDuration = endTime - startTime
                averageDuration = (curDuration + numDurations * averageDuration) / (numDurations + 1)
                numDurations++
            }
            if (data.moveToNext()) {
                val prevContractionStartTimeColumnIndex = data.getColumnIndex(
                        ContractionContract.Contractions.COLUMN_NAME_START_TIME)
                val prevContractionStartTime = data.getLong(prevContractionStartTimeColumnIndex)
                val curFrequency = startTime - prevContractionStartTime
                averageFrequency = (curFrequency + numFrequencies * averageFrequency) / (numFrequencies + 1)
                numFrequencies++
            }
        }
        averageLayout.visibility = View.VISIBLE
        val averageDurationInSeconds = (averageDuration / 1000).toLong()
        averageDurationView.text = DateUtils.formatElapsedTime(averageDurationInSeconds)
        val averageFrequencyInSeconds = (averageFrequency / 1000).toLong()
        averageFrequencyView.text = DateUtils.formatElapsedTime(averageFrequencyInSeconds)
    }

    override fun onResume() {
        super.onResume()
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val averageTimeFrameChanged = preferences.getBoolean(
                Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY, false)
        if (averageTimeFrameChanged) {
            preferences.edit().apply {
                remove(Preferences.AVERAGE_TIME_FRAME_CHANGED_FRAGMENT_PREFERENCE_KEY)
                apply()
            }
            loaderManager.restartLoader(0, null, this)
        }
    }
}
