package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.AttendanceSession
import com.example.data.model.CourseClass
import com.example.data.model.SessionWithStats
import com.example.data.model.Student
import com.example.data.model.StudentAttendanceSummary
import com.example.ui.components.AddStudentDialog
import com.example.ui.components.ExportDialog
import com.example.ui.components.PercentageBadge
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusPresent
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    viewModel: AttendanceViewModel,
    classId: Long,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val activeClass by viewModel.activeClass.collectAsStateWithLifecycle()
    val sessions by viewModel.activeClassSessions.collectAsStateWithLifecycle()
    val studentSummaries by viewModel.activeClassSummaries.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var sessionToDelete by remember { mutableStateOf<AttendanceSession?>(null) }
    var exportCsvData by remember { mutableStateOf<String?>(null) }

    val courseAccent = remember(activeClass?.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(activeClass?.colorHex ?: "#4F46E5"))
        } catch (_: Exception) {
            Color(0xFF4F46E5)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeClass?.name ?: "Course Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.generateExportCsv(classId) { csv ->
                                exportCsvData = csv
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        viewModel.setupAttendanceTaking(classId)
                        viewModel.navigateTo(Screen.MarkAttendance(classId))
                    },
                    containerColor = courseAccent,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Take Attendance")
                }
            } else if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddStudentDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Add Students")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Header
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sessions (${sessions.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Students (${studentSummaries.size})") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Reports") }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> SessionsListTab(
                    sessions = sessions,
                    onSessionClick = { session ->
                        viewModel.setupAttendanceTaking(classId, session.id)
                        viewModel.navigateTo(Screen.MarkAttendance(classId, session.id))
                    },
                    onDeleteSession = { sessionToDelete = it },
                    onTakeAttendance = {
                        viewModel.setupAttendanceTaking(classId)
                        viewModel.navigateTo(Screen.MarkAttendance(classId))
                    }
                )
                1 -> StudentsListTab(
                    summaries = studentSummaries,
                    targetAttendance = activeClass?.targetAttendancePercent ?: 75,
                    onStudentClick = { student ->
                        viewModel.navigateTo(Screen.StudentDetail(student.id))
                    },
                    onEditStudent = { studentToEdit = it },
                    onDeleteStudent = { student ->
                        viewModel.deleteStudent(student)
                        Toast.makeText(context, "Student removed", Toast.LENGTH_SHORT).show()
                    },
                    onAddStudent = { showAddStudentDialog = true }
                )
                2 -> ClassReportsTab(
                    activeClass = activeClass,
                    summaries = studentSummaries,
                    sessions = sessions,
                    onExportClick = {
                        viewModel.generateExportCsv(classId) { csv ->
                            exportCsvData = csv
                        }
                    }
                )
            }
        }
    }

    // Add Student Dialog
    if (showAddStudentDialog) {
        AddStudentDialog(
            onDismiss = { showAddStudentDialog = false },
            onAddSingle = { roll, name, email, phone ->
                viewModel.addStudent(classId, roll, name, email, phone)
                showAddStudentDialog = false
                Toast.makeText(context, "Student added!", Toast.LENGTH_SHORT).show()
            },
            onBulkImport = { rawText ->
                viewModel.bulkImportStudents(classId, rawText) { count ->
                    showAddStudentDialog = false
                    Toast.makeText(context, "Imported $count students!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Edit Student Dialog
    studentToEdit?.let { targetStudent ->
        AddStudentDialog(
            initialStudent = targetStudent,
            onDismiss = { studentToEdit = null },
            onAddSingle = { roll, name, email, phone ->
                viewModel.updateStudent(
                    targetStudent.copy(
                        rollNumber = roll,
                        name = name,
                        email = email,
                        phone = phone
                    )
                )
                studentToEdit = null
                Toast.makeText(context, "Student updated!", Toast.LENGTH_SHORT).show()
            },
            onBulkImport = {}
        )
    }

    // Delete Session Dialog
    sessionToDelete?.let { targetSession ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Session Record?") },
            text = { Text("Are you sure you want to delete this session's attendance record? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(targetSession)
                        sessionToDelete = null
                        Toast.makeText(context, "Session deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Dialog
    exportCsvData?.let { csv ->
        ExportDialog(
            csvContent = csv,
            courseTitle = activeClass?.name ?: "Course",
            onDismiss = { exportCsvData = null }
        )
    }
}

@Composable
private fun SessionsListTab(
    sessions: List<SessionWithStats>,
    onSessionClick: (AttendanceSession) -> Unit,
    onDeleteSession: (AttendanceSession) -> Unit,
    onTakeAttendance: () -> Unit
) {
    if (sessions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Sessions Recorded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Take your first roll call session for this course.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onTakeAttendance) {
                Text("Take Attendance Now")
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sessions, key = { it.session.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSessionClick(item.session) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateFormat.format(Date(item.session.dateMillis)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                onClick = { onDeleteSession(item.session) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = item.session.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Stats bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.presentCount} Present  •  ${item.absentCount} Absent" +
                                        if (item.lateCount > 0) "  •  ${item.lateCount} Late" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            PercentageBadge(percentage = item.attendancePercentage)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (item.attendancePercentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (item.attendancePercentage >= 75) StatusPresent else StatusAbsent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentsListTab(
    summaries: List<StudentAttendanceSummary>,
    targetAttendance: Int,
    onStudentClick: (Student) -> Unit,
    onEditStudent: (Student) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onAddStudent: () -> Unit
) {
    if (summaries.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Roster Is Empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add students one-by-one or paste a roster list using bulk import.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddStudent) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Students")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(summaries, key = { it.student.id }) { item ->
                var menuExpanded by remember { mutableStateOf(false) }
                val isAtRisk = item.isAtRisk(targetAttendance)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStudentClick(item.student) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (isAtRisk) StatusAtRisk.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.student.rollNumber,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAtRisk) StatusAtRisk else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.student.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isAtRisk) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "At Risk",
                                        tint = StatusAtRisk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${item.presentCount + item.lateCount}/${item.totalSessions} sessions attended",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        PercentageBadge(
                            percentage = item.attendancePercentage,
                            targetPercent = targetAttendance
                        )

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Student") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEditStudent(item.student)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Student", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onDeleteStudent(item.student)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassReportsTab(
    activeClass: CourseClass?,
    summaries: List<StudentAttendanceSummary>,
    sessions: List<SessionWithStats>,
    onExportClick: () -> Unit
) {
    val targetPct = activeClass?.targetAttendancePercent ?: 75
    val atRiskStudents = remember(summaries, targetPct) {
        summaries.filter { it.isAtRisk(targetPct) }
    }
    val averagePct = remember(summaries) {
        if (summaries.isNotEmpty()) {
            summaries.map { it.attendancePercentage }.average().toFloat()
        } else 0f
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Class Attendance Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Class Overall Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ReportMetricItem(label = "Average Rate", value = String.format(Locale.US, "%.1f%%", averagePct))
                        ReportMetricItem(label = "Min Requirement", value = "$targetPct%")
                        ReportMetricItem(
                            label = "At Risk (<$targetPct%)",
                            value = "${atRiskStudents.size} Students",
                            valueColor = if (atRiskStudents.isNotEmpty()) StatusAtRisk else StatusPresent
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onExportClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Attendance Sheet (CSV)")
                    }
                }
            }
        }

        // At Risk Defaulter List
        if (atRiskStudents.isNotEmpty()) {
            item {
                Text(
                    text = "Students Requiring Attention (<$targetPct%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StatusAtRisk
                )
            }

            items(atRiskStudents, key = { it.student.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.student.rollNumber,
                            fontWeight = FontWeight.Bold,
                            color = StatusAtRisk
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.student.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Attended ${item.presentCount + item.lateCount} of ${item.totalSessions} sessions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PercentageBadge(
                            percentage = item.attendancePercentage,
                            targetPercent = targetPct
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
