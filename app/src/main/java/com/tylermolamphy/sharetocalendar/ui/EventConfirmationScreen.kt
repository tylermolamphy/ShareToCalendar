package com.tylermolamphy.sharetocalendar.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tylermolamphy.sharetocalendar.viewmodel.EventConfirmationViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventConfirmationScreen(
    sharedText: String,
    onDismiss: () -> Unit,
    viewModel: EventConfirmationViewModel = viewModel()
) {
    val event by viewModel.event.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val context = LocalContext.current

    // Focus / keyboard
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Dialog visibility state
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(sharedText) {
        if (sharedText.isNotBlank()) {
            viewModel.parseSharedText(sharedText)  // suspend — awaits Default-dispatcher parse
        }
    }

    // Auto-focus title field and show keyboard on open
    LaunchedEffect(Unit) {
        delay(100L)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(saveResult) {
        when (saveResult) {
            is EventConfirmationViewModel.SaveResult.Success -> {
                Toast.makeText(context, "Event saved!", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveResult()
                onDismiss()
            }
            is EventConfirmationViewModel.SaveResult.Error -> {
                Toast.makeText(
                    context,
                    (saveResult as EventConfirmationViewModel.SaveResult.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetSaveResult()
            }
            EventConfirmationViewModel.SaveResult.Idle -> {}
        }
    }

    val dateFormatter = DATE_FORMATTER
    val timeFormatter = TIME_FORMATTER

    // M3 Date Picker Dialog
    if (showDatePicker) {
        val initialMillis = event.startDate
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        viewModel.updateEvent(event.copy(startDate = date))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // M3 Start Time Picker Dialog
    if (showStartTimePicker) {
        val st = event.startTime ?: LocalTime.of(9, 0)
        M3TimePickerDialog(
            initialHour = st.hour,
            initialMinute = st.minute,
            onDismiss = { showStartTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.updateEvent(event.copy(startTime = LocalTime.of(hour, minute)))
                showStartTimePicker = false
            }
        )
    }

    // M3 End Time Picker Dialog
    if (showEndTimePicker) {
        val et = event.endTime ?: event.startTime?.plusHours(1) ?: LocalTime.of(10, 0)
        M3TimePickerDialog(
            initialHour = et.hour,
            initialMinute = et.minute,
            onDismiss = { showEndTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.updateEvent(event.copy(endTime = LocalTime.of(hour, minute)))
                showEndTimePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Event") },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancelButton")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveEvent() },
                        modifier = Modifier.testTag("saveButton")
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title — auto-focused on open
            OutlinedTextField(
                value = event.title,
                onValueChange = { viewModel.updateEvent(event.copy(title = it)) },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("titleField"),
                singleLine = true
            )

            // All-day toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All-day event", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = event.isAllDay,
                    onCheckedChange = {
                        viewModel.updateEvent(
                            event.copy(
                                isAllDay = it,
                                startTime = if (it) null else event.startTime ?: LocalTime.of(9, 0),
                                endTime = if (it) null else event.endTime ?: LocalTime.of(10, 0)
                            )
                        )
                    }
                )
            }

            // Date — opens M3 DatePickerDialog
            Box {
                OutlinedTextField(
                    value = event.startDate.format(dateFormatter),
                    onValueChange = {},
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            // Start / End time (only if not all-day) — opens M3 TimePickerDialog
            if (!event.isAllDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = event.startTime?.format(timeFormatter) ?: "",
                            onValueChange = {},
                            label = { Text("Start") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = "Pick start time")
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showStartTimePicker = true }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = event.endTime?.format(timeFormatter) ?: "",
                            onValueChange = {},
                            label = { Text("End") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = "Pick end time")
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showEndTimePicker = true }
                        )
                    }
                }
            }

            // Location
            OutlinedTextField(
                value = event.location,
                onValueChange = { viewModel.updateEvent(event.copy(location = it)) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
            )

            // Description
            OutlinedTextField(
                value = event.description,
                onValueChange = { viewModel.updateEvent(event.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M3TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
