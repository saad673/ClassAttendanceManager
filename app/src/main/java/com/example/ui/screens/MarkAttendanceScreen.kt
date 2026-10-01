package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceStatus
import com.example.data.model.Student
import com.example.ui.components.AttendanceStatusPillButton
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import com.example.ui.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAttendanceScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val activeClass by viewModel.activeClass.collectAsStateWithLifecycle()
    val students by viewModel.activeClassStudents.collectAsStateWithLifecycle()

    val dateMillis by viewModel.markAttendanceDateMillis.collectAsStateWithLifecycle()
    val sessionTitle by viewModel.markAttendanceTitle.collectAsStateWithLifecycle()
    val statusMap by viewModel.markAttendanceStatuses.collectAsStateWithLifecycle()
    val notesMap by viewModel.markAttendanceNotes.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showNoteForStudentId by remember { mutableStateOf<Long?>(null) }

    val formattedDate = remember(dateMillis) {
        val sdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(dateMillis))
    }

    // Live counts
    val presentCount = remember(statusMap) {
        statusMap.values.count { it == AttendanceStatus.PRESENT }
    }
    val absentCount = remember(statusMap) {
        statusMap.values.count { it == AttendanceStatus.ABSENT }
    }
    val lateCount = remember(statusMap) {
        statusMap.values.count { it == AttendanceStatus.LATE }
    }
    val excusedCount = remember(statusMap) {
        statusMap.values.count { it == AttendanceStatus.EXCUSED }
    }
    val totalStudents = students.size
    val attendancePct = if (totalStudents > 0) {
        ((presentCount + lateCount).toFloat() / totalStudents.toFloat()) * 100f
    } else 0f

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students
        else {
            students.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.rollNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeClass?.name ?: "Take Attendance",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        activeClass?.let {
                            Text(
                                text = "${it.code} • ${it.section}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$presentCount of $totalStudents Present",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f%% Attendance Rate", attendancePct),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (attendancePct >= (activeClass?.targetAttendancePercent ?: 75)) StatusPresent else StatusAbsent
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveAttendance {
                                Toast.makeText(context, "Attendance saved successfully!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("submit_attendance_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Roll Call", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Session Details Card: Date picker & Topic
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Date selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance()
                                            newCal.set(y, m, d)
                                            viewModel.markAttendanceDateMillis.value = newCal.timeInMillis
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = sessionTitle,
                            onValueChange = { viewModel.markAttendanceTitle.value = it },
                            label = { Text("Session Topic / Title") },
                            placeholder = { Text("e.g. Lecture 4: Sorting Algorithms") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Quick Actions & Counters
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CountMetricBadge(
                            label = "Present",
                            count = presentCount,
                            color = StatusPresent,
                            modifier = Modifier.weight(1f)
                        )
                        CountMetricBadge(
                            label = "Absent",
                            count = absentCount,
                            color = StatusAbsent,
                            modifier = Modifier.weight(1f)
                        )
                        CountMetricBadge(
                            label = "Late",
                            count = lateCount,
                            color = StatusLate,
                            modifier = Modifier.weight(1f)
                        )
                        CountMetricBadge(
                            label = "Excused",
                            count = excusedCount,
                            color = StatusExcused,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.markAll(AttendanceStatus.PRESENT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Mark All Present", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.markAll(AttendanceStatus.ABSENT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Mark All Absent", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Search filter
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter student by roll number or name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Student Roster Roll Call Items
            if (filteredStudents.isEmpty()) {
                item {
                    Text(
                        text = if (students.isEmpty()) "No students enrolled in this course yet. Add students first!" else "No matching students found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(filteredStudents, key = { it.id }) { student ->
                    val currentStatus = statusMap[student.id] ?: AttendanceStatus.PRESENT
                    val studentNote = notesMap[student.id] ?: ""
                    val isNoteOpen = showNoteForStudentId == student.id

                    StudentRollCallItem(
                        student = student,
                        currentStatus = currentStatus,
                        note = studentNote,
                        isNoteOpen = isNoteOpen,
                        onStatusSelected = { status ->
                            viewModel.setStudentStatus(student.id, status)
                        },
                        onToggleNote = {
                            showNoteForStudentId = if (isNoteOpen) null else student.id
                        },
                        onNoteChanged = { newNote ->
                            viewModel.setStudentNote(student.id, newNote)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentRollCallItem(
    student: Student,
    currentStatus: AttendanceStatus,
    note: String,
    isNoteOpen: Boolean,
    onStatusSelected: (AttendanceStatus) -> Unit,
    onToggleNote: () -> Unit,
    onNoteChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_item_${student.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Roll number badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = student.rollNumber,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.isNotBlank()) {
                        Text(
                            text = "Note: $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onToggleNote,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Note",
                        tint = if (note.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attendance Action Pills: [P] [A] [L] [E]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendanceStatus.entries.forEach { status ->
                    AttendanceStatusPillButton(
                        status = status,
                        isSelected = currentStatus == status,
                        onClick = { onStatusSelected(status) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Note input dropdown
            AnimatedVisibility(visible = isNoteOpen) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChanged,
                        placeholder = { Text("Add remark (e.g. late by 15 mins, fever)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
private fun CountMetricBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}
