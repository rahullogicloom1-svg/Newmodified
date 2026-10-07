package com.calmremind.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate

private enum class Picker { START, END, ADD }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    initial: Reminder,
    isNew: Boolean,
    onSave: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onClose: () -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var note by remember { mutableStateOf(initial.note) }
    var type by remember { mutableStateOf(initial.type) }
    var mode by remember { mutableStateOf(initial.mode) }
    var interval by remember { mutableIntStateOf(initial.intervalMinutes) }
    var wStart by remember { mutableIntStateOf(initial.windowStart) }
    var wEnd by remember { mutableIntStateOf(initial.windowEnd) }
    var times by remember { mutableStateOf(initial.times) }
    var repeat by remember { mutableStateOf(initial.repeat) }
    var weekdays by remember { mutableStateOf(initial.weekdays) }
    var anchor by remember { mutableLongStateOf(initial.anchorEpochDay) }
    var error by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val todayEpoch = remember { LocalDate.now().toEpochDay() }

    fun trySave() {
        val trimmed = name.trim()
        when {
            trimmed.isEmpty() -> error = "Please give this reminder a name."
            mode == ScheduleMode.INTERVAL && wEnd < wStart -> error = "The end time must be after the start time."
            mode == ScheduleMode.TIMES && times.isEmpty() -> error = "Add at least one time."
            repeat == RepeatMode.WEEKDAYS && weekdays.isEmpty() -> error = "Pick at least one day of the week."
            else -> onSave(
                initial.copy(
                    name = trimmed,
                    note = note.trim(),
                    type = type,
                    mode = mode,
                    intervalMinutes = interval,
                    windowStart = wStart,
                    windowEnd = wEnd,
                    times = times.distinct().sorted(),
                    repeat = repeat,
                    weekdays = weekdays,
                    anchorEpochDay = anchor
                )
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New reminder" else "Edit reminder") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---- What ----
            SectionCard("What is it for?") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderType.values().forEach { t ->
                        Pill("${t.emoji} ${t.label}", type == t, onClick = {
                            if (name.isBlank() || name == defaultName(type)) name = defaultName(t)
                            type = t
                        })
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional) — e.g. 1 tablet after food") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---- When ----
            SectionCard("When should it remind you?") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Every few hours", mode == ScheduleMode.INTERVAL, onClick = { mode = ScheduleMode.INTERVAL })
                    Pill("At exact times", mode == ScheduleMode.TIMES, onClick = { mode = ScheduleMode.TIMES })
                }

                if (mode == ScheduleMode.INTERVAL) {
                    Text("Repeat every", style = MaterialTheme.typography.labelLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilledTonalIconButton(onClick = {
                            interval = (interval - (if (interval <= 60) 15 else 30)).coerceAtLeast(15)
                        }) { Text("−", fontSize = 22.sp) }
                        Text(
                            fmtInterval(interval),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalIconButton(onClick = {
                            interval = (interval + (if (interval < 60) 15 else 30)).coerceAtMost(24 * 60)
                        }) { Text("+", fontSize = 22.sp) }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 60, 120, 180, 240, 360, 480).forEach { m ->
                            Pill(fmtInterval(m), interval == m, onClick = { interval = m })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeButton("First reminder", wStart, Modifier.weight(1f)) { picker = Picker.START }
                        TimeButton("Last reminder", wEnd, Modifier.weight(1f)) { picker = Picker.END }
                    }
                    val preview = Schedule.intervalDoses(wStart, wEnd, interval)
                    Text(
                        if (preview.isEmpty()) {
                            "No reminders — the last time must be after the first."
                        } else {
                            "${preview.size} reminders a day: " +
                                preview.take(8).joinToString(", ") { fmtTime(it) } +
                                (if (preview.size > 8) " …" else "")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        times.sorted().forEach { m ->
                            Pill("${fmtTime(m)}  ✕", true, onClick = { times = times - m })
                        }
                    }
                    OutlinedButton(onClick = { picker = Picker.ADD }) { Text("+ Add a time") }
                    if (times.isEmpty()) {
                        Text(
                            "No times yet — tap “Add a time”.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Tap a time to remove it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Which days ----
            SectionCard("On which days?") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Pill("Every day", repeat == RepeatMode.DAILY, onClick = { repeat = RepeatMode.DAILY })
                    Pill("Every other day", repeat == RepeatMode.ALTERNATE, onClick = { repeat = RepeatMode.ALTERNATE })
                    Pill("Choose days", repeat == RepeatMode.WEEKDAYS, onClick = { repeat = RepeatMode.WEEKDAYS })
                }
                when (repeat) {
                    RepeatMode.ALTERNATE -> {
                        Text("Start on", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("Today", anchor == todayEpoch, onClick = { anchor = todayEpoch })
                            Pill("Tomorrow", anchor == todayEpoch + 1, onClick = { anchor = todayEpoch + 1 })
                        }
                    }
                    RepeatMode.WEEKDAYS -> {
                        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            labels.forEachIndexed { i, label ->
                                val day = i + 1
                                val selected = day in weekdays
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { weekdays = if (selected) weekdays - day else weekdays + day },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    RepeatMode.DAILY -> Unit
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = { trySave() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Save reminder", style = MaterialTheme.typography.titleMedium) }

            if (!isNew) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Delete reminder", color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    picker?.let { p ->
        TimePickerDialog(
            title = when (p) {
                Picker.START -> "First reminder of the day"
                Picker.END -> "Last reminder of the day"
                Picker.ADD -> "Add a reminder time"
            },
            initialMinutes = when (p) {
                Picker.START -> wStart
                Picker.END -> wEnd
                Picker.ADD -> 9 * 60
            },
            onDismiss = { picker = null },
            onConfirm = { minutes ->
                when (p) {
                    Picker.START -> wStart = minutes
                    Picker.END -> wEnd = minutes
                    Picker.ADD -> times = (times + minutes).distinct().sorted()
                }
                picker = null
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this reminder?") },
            text = { Text("“${initial.name}” will stop reminding you. Your past history is kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(initial)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TimeButton(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(fmtTime(minutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = false
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .height(IntrinsicSize.Min)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    title,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    style = MaterialTheme.typography.labelMedium
                )
                TimePicker(state = pickerState)
                Row(Modifier.height(40.dp).fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) { Text("OK") }
                }
            }
        }
    }
}
