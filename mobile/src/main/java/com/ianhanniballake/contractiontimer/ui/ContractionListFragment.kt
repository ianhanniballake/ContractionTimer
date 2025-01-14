package com.ianhanniballake.contractiontimer.ui

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
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
import com.ianhanniballake.contractiontimer.provider.ContractionContract
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ContractionListViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ContractionDatabase.getInstance(application).contractionDao()

    val allContractions = dao.allContractions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    suspend fun delete(contraction: Contraction) {
        dao.delete(contraction)
    }
}

private const val TAG = "ContractionListFragment"

sealed class ListState
object IndeterminateState : ListState()
object EmptyState : ListState()
data class NonEmptyState(val contractions: List<Contraction>) : ListState()

// TODO: Re-add contextual action bar that allows viewing details, notes, and multi-deletion
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContractionList(
    showNoteDialog: (contraction: Contraction) -> Unit
) {
    val context = LocalContext.current
    val viewModel = viewModel<ContractionListViewModel>()
    val contractions by viewModel.allContractions.collectAsStateWithLifecycle()
    val listState by remember(contractions) {
        derivedStateOf {
            val currentContractions = contractions
            if (currentContractions == null) {
                IndeterminateState
            } else if (currentContractions.isEmpty()) {
                EmptyState
            } else {
                NonEmptyState(currentContractions)
            }
        }
    }
    AnimatedContent(
        listState,
        contentAlignment = Alignment.Center,
        label = "contraction_list",
        contentKey = { state -> state.javaClass }
    ) { state ->
        when (state) {
            IndeterminateState -> {
                // Don't show anything
            }
            EmptyState -> {
                Box {
                    Column(modifier = Modifier.align(Alignment.Center).width(IntrinsicSize.Min)) {
                        Icon(painterResource(R.drawable.ic_list_empty),
                            contentDescription = null,
                            tint = colorResource(R.color.empty_list_text_color)
                        )
                        Text(stringResource(R.string.list_empty),
                            modifier = Modifier.fillMaxWidth(),
                            color = colorResource(R.color.empty_list_text_color),
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
            is NonEmptyState -> {
                val analytics by remember(context) {
                    derivedStateOf {
                        FirebaseAnalytics.getInstance(context)
                    }
                }
                val scope = rememberCoroutineScope()
                ContractionListScreen(
                    state.contractions,
                    onViewDetails = { contraction, fromPopup ->
                        if (fromPopup) {
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "Popup Menu selected view")
                            analytics.logEvent("view_popup", null)
                        }
                        val contractionUri = ContentUris.withAppendedId(
                            ContractionContract.Contractions.CONTENT_ID_URI_BASE, contraction.id)
                        val intent = Intent(Intent.ACTION_VIEW, contractionUri)
                            .setComponent(ComponentName(context, ViewActivity::class.java))
                        context.startActivity(intent)
                    },
                    onViewNote = { contraction ->
                        val type = if (contraction.note.isBlank()) "Add Note" else "Edit Note"
                        if (BuildConfig.DEBUG)
                            Log.d(TAG, "Popup Menu selected $type")
                        val noteEvent = if (contraction.note.isBlank()) "note_add_popup" else "note_edit_popup"
                        analytics.logEvent(noteEvent, null)
                        showNoteDialog(contraction)
                    },
                    onDelete = { contraction ->
                        scope.launch {
                            viewModel.delete(contraction)
                            AppWidgetUpdateHandler.createInstance().updateAllWidgets(context)
                            NotificationUpdateReceiver.updateNotification(context)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ContractionListScreen(
    contractions: List<Contraction>,
    onViewDetails: (contraction: Contraction, fromPopup: Boolean) -> Unit,
    onViewNote: (contraction: Contraction) -> Unit,
    onDelete: (contraction: Contraction) -> Unit
) {
    val context = LocalContext.current
    val timeFormat by remember(context) {
        derivedStateOf {
            if (DateFormat.is24HourFormat(context))
                "kk:mm:ss"
            else
                "hh:mm:ssa"
        }
    }
    val dateFormat by remember(context) {
        derivedStateOf {
            try {
                val dateFormatOrder = DateFormat.getDateFormatOrder(context)
                val dateFormatArray = charArrayOf(dateFormatOrder[0], dateFormatOrder[0], '/', dateFormatOrder[1], dateFormatOrder[1])
                String(dateFormatArray)
            } catch (e: IllegalArgumentException) {
                "MM/dd"
            }
        }
    }
    LazyColumn {
        if (contractions.isNotEmpty()) {
            item {
                ListHeader()
            }
            val lastContraction = contractions[0]
            if (lastContraction.endTime != null) {
                item {
                    FrequencyHeader(lastContraction)
                }
            }
        }
        itemsIndexed(
            contractions,
            key = { _, contraction -> contraction.startTime }
        ) { index, contraction ->
            val previousContraction = if (index + 1 != contractions.size) {
                contractions[index + 1]
            } else {
                null
            }
            Column(modifier = Modifier.fillMaxWidth().animateItem()) {
                ContractionItem(
                    timeFormat,
                    dateFormat,
                    contraction,
                    previousContraction,
                    onViewDetails = { fromPopup -> onViewDetails(contraction, fromPopup) },
                    onViewNote = { onViewNote(contraction) },
                    onDelete = { onDelete(contraction) }
                )
                if (index + 1 != contractions.size) {
                    Divider(thickness = Dp.Hairline)
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun ListHeader() {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 5.dp, bottom = 5.dp, start = 8.dp)
    ) {
        Spacer(modifier = Modifier.weight(1.0F, fill = true))
        Text(
            stringResource(R.string.list_header_duration),
            modifier = Modifier.weight(1.0F, fill = true),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            stringResource(R.string.list_header_frequency),
            modifier = Modifier.weight(1.0F, fill = true),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(56.dp))
    }
}

@Composable
private fun FrequencyHeader(lastContraction: Contraction) {
    val formatTimeSinceLastContractionStart: () -> String = {
        val duration = System.currentTimeMillis() - lastContraction.startTime.time
        val durationInSeconds = (duration / 1000)
        DateUtils.formatElapsedTime(durationInSeconds)
    }
    val timeSinceLastContractionStart by produceState(
        formatTimeSinceLastContractionStart(),
        lastContraction
    ) {
        while (true) {
            delay(1000)
            value = formatTimeSinceLastContractionStart()
        }
    }
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 3.dp, bottom = 3.dp, start = 8.dp)
    ) {
        Spacer(modifier = Modifier.weight(2.0F, fill = true))
        Text(
            timeSinceLastContractionStart,
            modifier = Modifier.weight(1.0F, fill = true),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.width(56.dp))
    }
}

@Composable
private fun ContractionItem(
    timeFormat: String,
    dateFormat: String,
    contraction: Contraction,
    previousContraction: Contraction?,
    onViewDetails: (fromPopup: Boolean) -> Unit,
    onViewNote: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(modifier = Modifier
            .height(IntrinsicSize.Min)
            .clickable {
                onViewDetails(false)
            }
            .padding(start = 8.dp)) {
            Column(
                modifier = Modifier.weight(1.0F, true)
            ) {
                val formattedEndTime by remember(contraction) {
                    derivedStateOf {
                        if (contraction.endTime != null) {
                            val startCal = Calendar.getInstance()
                            startCal.time = contraction.startTime
                            val endCal = Calendar.getInstance()
                            endCal.time = contraction.endTime
                            val showDateOnEndTime = startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR) ||
                                    startCal.get(Calendar.DAY_OF_YEAR) != endCal.get(Calendar.DAY_OF_YEAR)
                            DateFormat.format(timeFormat, contraction.endTime).toString() + if (showDateOnEndTime) {
                                " " + DateFormat.format(dateFormat, endCal)
                            } else {
                                ""
                            }
                        } else {
                            ""
                        }
                    }
                }
                Text(
                    text = formattedEndTime,
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
                val formattedStartTime by remember(contraction, previousContraction) {
                    derivedStateOf {
                        val startCal = Calendar.getInstance()
                        startCal.time = contraction.startTime
                        val showDateOnStartTime = if (previousContraction?.endTime != null) {
                            val endCal = Calendar.getInstance()
                            endCal.time = previousContraction.endTime
                            startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR) ||
                                    startCal.get(Calendar.DAY_OF_YEAR) != endCal.get(Calendar.DAY_OF_YEAR)
                        } else {
                            // Always show the date on the very first start time
                            true
                        }
                        DateFormat.format(timeFormat, contraction.startTime).toString() + if (showDateOnStartTime) {
                            " " + DateFormat.format(dateFormat, startCal)
                        } else {
                            ""
                        }
                    }
                }
                Text(
                    text = formattedStartTime,
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            val formatDuration: (endTime: Long) -> String = { endTime ->
                val duration = endTime - contraction.startTime.time
                val durationInSeconds = (duration / 1000)
                DateUtils.formatElapsedTime(durationInSeconds)
            }
            val formattedDuration by produceState(if (contraction.endTime != null) {
                formatDuration(contraction.endTime.time)
            } else {
                formatDuration(System.currentTimeMillis())
            }, contraction) {
                if (contraction.endTime != null) {
                    value = formatDuration(contraction.endTime.time)
                } else {
                    while(true) {
                        delay(1000)
                        value = formatDuration(System.currentTimeMillis())
                    }
                }
            }
            Text(formattedDuration,
                modifier = Modifier
                    .padding(top = 3.dp, bottom = 3.dp)
                    .weight(1.0F, true),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            val formattedFrequency by remember(contraction, previousContraction) {
                derivedStateOf {
                    if (previousContraction != null) {
                        val frequency = contraction.startTime.time - previousContraction.startTime.time
                        val frequencyInSeconds = (frequency / 1000).toLong()
                        DateUtils.formatElapsedTime(frequencyInSeconds)
                    } else {
                        ""
                    }
                }
            }
            Text(formattedFrequency,
                modifier = Modifier
                    .padding(top = 3.dp, bottom = 3.dp)
                    .weight(1.0F, true),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
            var showPopup by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
                    .width(56.dp)
                    .clickable {
                        showPopup = true
                    }
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    stringResource(R.string.overflow_content_description),
                    modifier = Modifier.align(Alignment.Center)
                )
                if (showPopup) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        offset = IntOffset(-8, 8),
                        onDismissRequest = {
                            showPopup = false
                        }
                    ) {
                        Surface(
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.width(IntrinsicSize.Max)
                            ) {
                                Text(stringResource(R.string.menu_context_view),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showPopup = false
                                            onViewDetails(true)
                                        }
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Divider(modifier = Modifier.fillMaxWidth(), thickness = Dp.Hairline)
                                Text(
                                    stringResource(if (contraction.note.isBlank()) {
                                        R.string.note_dialog_title_add
                                    } else {
                                        R.string.note_dialog_title_edit
                                    }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showPopup = false
                                            onViewNote()
                                        }
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Divider(modifier = Modifier.fillMaxWidth(), thickness = Dp.Hairline)
                                Text(pluralStringResource(R.plurals.menu_context_delete, count = 1),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showPopup = false
                                            onDelete()
                                        }
                                        .padding(8.dp),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
        if (contraction.note.isNotBlank()) {
            Text(contraction.note,
                modifier = Modifier.padding(bottom = 4.dp, start = 24.dp, end = 24.dp)
            )
        }
    }
}

/**
 * Fragment to list contractions entered by the user
 */
class ContractionListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        ContractionList(
            showNoteDialog = { contraction ->
                val noteDialogFragment = NoteDialogFragment()
                noteDialogFragment.arguments = Bundle().apply {
                    putLong(NoteDialogFragment.CONTRACTION_ID_ARGUMENT, contraction.id)
                    putString(NoteDialogFragment.EXISTING_NOTE_ARGUMENT, contraction.note)
                }
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Showing Dialog")
                noteDialogFragment.show(childFragmentManager, "note")
            }
        )
    }
}
