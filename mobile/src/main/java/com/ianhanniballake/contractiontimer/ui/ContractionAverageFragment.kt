package com.ianhanniballake.contractiontimer.ui

import android.app.Application
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.database.ContractionDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ContractionAverageViewModel(
        application: Application
) : AndroidViewModel(application) {
    private val dao = ContractionDatabase.getInstance(application).contractionDao()
    private val preferences = PreferenceManager.getDefaultSharedPreferences(application)
    private val averagesTimeFrameFlow = callbackFlow {
        val updateAverage = {
            val averagesTimeFrame = preferences.getString(
                    Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY,
                    application.getString(R.string.pref_average_time_frame_default)
            )!!.toLong()
            trySendBlocking(averagesTimeFrame)
        }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Preferences.AVERAGE_TIME_FRAME_PREFERENCE_KEY) {
                updateAverage()
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        updateAverage()

        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val contractions = combine(
            averagesTimeFrameFlow,
            dao.latestContraction()
    ) { averagesTimeFrame, _ ->
        val timeCutoff = System.currentTimeMillis() - averagesTimeFrame
        dao.contractionsBackTo(timeCutoff)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
}

/**
 * Fragment which displays the average duration and frequency
 */
class ContractionAverageFragment : Fragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = content {
        val viewModel = viewModel<ContractionAverageViewModel>()
        val contractions by viewModel.contractions.collectAsStateWithLifecycle()
        AnimatedVisibility(visible = contractions.isNotEmpty(),
                enter = slideInVertically { it / 2 },
                exit = slideOutVertically { it / 2 },
                modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier
                    .background(colorResource(R.color.average_background)).padding(top = 5.dp, bottom = 5.dp)) {
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.contraction_average),
                        modifier = Modifier.weight(1.0F, true),
                        style = MaterialTheme.typography.titleLarge
                )
                val formattedAverageDuration by remember(contractions) {
                    derivedStateOf {
                        val averageDuration = contractions.mapNotNull { contraction ->
                            if (contraction.endTime != null) {
                                contraction.endTime.time - contraction.startTime.time
                            } else {
                                null
                            }
                        }.average()
                        val averageDurationInSeconds = (averageDuration / 1000).toLong()
                        DateUtils.formatElapsedTime(averageDurationInSeconds)
                    }
                }
                Text(formattedAverageDuration,
                        modifier = Modifier.weight(1.0F, true),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                )
                val formattedAverageFrequency by remember(contractions) {
                    derivedStateOf {
                        val averageFrequency = contractions.windowed(2) { window ->
                            window[1].startTime.time - window[0].startTime.time
                        }.average()
                        val averageFrequencyInSeconds = (averageFrequency / 1000).toLong()
                        DateUtils.formatElapsedTime(averageFrequencyInSeconds)
                    }
                }
                Text(formattedAverageFrequency,
                        modifier = Modifier.weight(1.0F, true),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(56.dp))
            }
        }
    }
}
