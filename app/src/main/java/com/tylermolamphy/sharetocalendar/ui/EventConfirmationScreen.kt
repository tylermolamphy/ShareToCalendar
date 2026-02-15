package com.tylermolamphy.sharetocalendar.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tylermolamphy.sharetocalendar.viewmodel.EventConfirmationViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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

    LaunchedEffect(sharedText) {
        if (sharedText.isNotBlank()) {
            viewModel.parseSharedText(sharedText)
        }
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

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

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
            // Title
            OutlinedTextField(
                value = event.title,
                onValueChange = { viewModel.updateEvent(event.copy(title = it)) },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
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

            // Date
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
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    viewModel.updateEvent(
                                        event.copy(startDate = LocalDate.of(year, month + 1, day))
                                    )
                                },
                                event.startDate.year,
                                event.startDate.monthValue - 1,
                                event.startDate.dayOfMonth
                            ).show()
                        }
                )
            }

            // Start / End time (only if not all-day)
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
                                Icon(Icons.Default.Schedule, contentDescription = "Pick start time")
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    val st = event.startTime ?: LocalTime.of(9, 0)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            viewModel.updateEvent(
                                                event.copy(startTime = LocalTime.of(hour, minute))
                                            )
                                        },
                                        st.hour,
                                        st.minute,
                                        false
                                    ).show()
                                }
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
                                Icon(Icons.Default.Schedule, contentDescription = "Pick end time")
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    val et = event.endTime ?: event.startTime?.plusHours(1) ?: LocalTime.of(10, 0)
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            viewModel.updateEvent(
                                                event.copy(endTime = LocalTime.of(hour, minute))
                                            )
                                        },
                                        et.hour,
                                        et.minute,
                                        false
                                    ).show()
                                }
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
