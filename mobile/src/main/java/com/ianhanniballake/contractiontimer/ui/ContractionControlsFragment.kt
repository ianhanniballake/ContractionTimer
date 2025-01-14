package com.ianhanniballake.contractiontimer.ui

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.ianhanniballake.contractiontimer.BuildConfig
import com.ianhanniballake.contractiontimer.R
import com.ianhanniballake.contractiontimer.appwidget.AppWidgetUpdateHandler
import com.ianhanniballake.contractiontimer.database.Contraction
import com.ianhanniballake.contractiontimer.database.ContractionDatabase
import com.ianhanniballake.contractiontimer.notification.NotificationUpdateReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class ContractionControlsViewModel(application: Application): AndroidViewModel(application) {
    private val dao = ContractionDatabase.getInstance(application).contractionDao()

    val latestContraction = dao.latestContraction()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun startContraction() {
        dao.insert(Contraction())
    }

    suspend fun stopContraction(contraction: Contraction) {
        dao.update(contraction.copy(endTime = Date()))
    }
}

private const val TAG = "ContractionControls"

@Composable
fun ContractionControls() {
    val viewModel = viewModel<ContractionControlsViewModel>()
    val latestContraction by viewModel.latestContraction.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val analytics by remember(context) {
        derivedStateOf {
            FirebaseAnalytics.getInstance(context)
        }
    }
    val scope = rememberCoroutineScope()
    var ongoingJob by remember { mutableStateOf<Job?>(null) }
    ContractionControlsScreen(
        latestContraction,
        enabled = ongoingJob == null,
        onStartContraction = {
            ongoingJob = scope.launch {
                try {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Starting contraction")
                    analytics.logEvent("control_start", null)
                    viewModel.startContraction()
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                    NotificationUpdateReceiver.updateNotification(context)
                } finally {
                    ongoingJob = null
                }
            }
        },
        onStopContraction = { contractionToStop ->
            scope.launch {
                try {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "Stopping contraction")
                    analytics.logEvent("control_stop", null)
                    viewModel.stopContraction(contractionToStop)
                    AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                    NotificationUpdateReceiver.updateNotification(context)
                } finally {
                    ongoingJob = null
                }
            }
        }
    )
}

@Composable
fun ContractionControlsScreen(
    latestContraction: Contraction?,
    enabled: Boolean,
    onStartContraction: () -> Unit,
    onStopContraction: (contractionToStop: Contraction) -> Unit
) {
    val contractionOngoing = latestContraction != null && latestContraction.endTime == null
    val scope = rememberCoroutineScope()
    FloatingActionButton(
        onClick = {
            if (enabled) {
                if (contractionOngoing) {
                    onStopContraction(latestContraction!!)
                } else {
                    onStartContraction()
                }
            }
        },
        modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
    ) {
        if (contractionOngoing) {
            Icon(painterResource(R.drawable.ic_notif_action_stop), stringResource(R.string.appwidget_contraction_stop))
        } else {
            Icon(painterResource(R.drawable.ic_notif_action_start), stringResource(R.string.appwidget_contraction_start))
        }
    }
}

/**
 * Fragment which controls starting and stopping the contraction timer
 */
class ContractionControlsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        ContractionControls()
    }
}
