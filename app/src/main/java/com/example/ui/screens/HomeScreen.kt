package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseClass
import com.example.ui.components.AddEditClassDialog
import com.example.ui.components.ClassCard
import com.example.ui.components.ExportDialog
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun HomeScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val classesWithStats by viewModel.classesWithStats.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<CourseClass?>(null) }
    var classToDelete by remember { mutableStateOf<CourseClass?>(null) }
    var exportCsvContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    val filteredClasses = remember(classesWithStats, searchQuery) {
        if (searchQuery.isBlank()) {
            classesWithStats
        } else {
            classesWithStats.filter {
                it.courseClass.name.contains(searchQuery, ignoreCase = true) ||
                        it.courseClass.code.contains(searchQuery, ignoreCase = true) ||
                        it.courseClass.section.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Hero Header
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Class Attendance",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Smart offline roll call & roster tracking",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick Stats Pill Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatSummaryChip(
                                label = "Courses",
                                count = classesWithStats.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatSummaryChip(
                                label = "Students",
                                count = allStudents.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatSummaryChip(
                                label = "Sessions",
                                count = allSessions.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Search Bar & Filter
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_classes_input"),
                        placeholder = { Text("Search course by name or code...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Courses List or Empty State
            if (filteredClasses.isEmpty()) {
                item {
                    EmptyClassesPlaceholder(
                        onAddClassClick = { showAddDialog = true }
                    )
                }
            } else {
                items(filteredClasses, key = { it.courseClass.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ClassCard(
                            classWithStats = item,
                            onCardClick = {
                                viewModel.selectedClassId.value = item.courseClass.id
                                viewModel.navigateTo(Screen.ClassDetail(item.courseClass.id))
                            },
                            onTakeAttendanceClick = {
                                viewModel.setupAttendanceTaking(item.courseClass.id)
                                viewModel.navigateTo(Screen.MarkAttendance(item.courseClass.id))
                            },
                            onEditClick = {
                                classToEdit = item.courseClass
                            },
                            onDeleteClick = {
                                classToDelete = item.courseClass
                            },
                            onExportClick = {
                                viewModel.generateExportCsv(item.courseClass.id) { csv ->
                                    exportCsvContent = (item.courseClass.name to csv)
                                }
                            }
                        )
                    }
                }
            }
        }

        // FAB to add new course
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_class_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Course")
        }
    }

    // Add Class Dialog
    if (showAddDialog) {
        AddEditClassDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, code, section, room, target, colorHex ->
                viewModel.createClass(name, code, section, room, target, colorHex) { newId ->
                    showAddDialog = false
                    Toast.makeText(context, "Class \"$name\" created!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Edit Class Dialog
    classToEdit?.let { targetCourse ->
        AddEditClassDialog(
            initialClass = targetCourse,
            onDismiss = { classToEdit = null },
            onConfirm = { name, code, section, room, target, colorHex ->
                viewModel.updateClass(
                    targetCourse.copy(
                        name = name,
                        code = code,
                        section = section,
                        roomNumber = room,
                        targetAttendancePercent = target,
                        colorHex = colorHex
                    )
                )
                classToEdit = null
                Toast.makeText(context, "Course updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    classToDelete?.let { targetCourse ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text("Delete Course?") },
            text = {
                Text("Are you sure you want to delete \"${targetCourse.name}\"? All student roster and attendance history records will be permanently removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClass(targetCourse)
                        classToDelete = null
                        Toast.makeText(context, "Course deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export CSV Dialog
    exportCsvContent?.let { (title, csv) ->
        ExportDialog(
            csvContent = csv,
            courseTitle = title,
            onDismiss = { exportCsvContent = null }
        )
    }
}

@Composable
private fun StatSummaryChip(
    label: String,
    count: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyClassesPlaceholder(
    onAddClassClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Courses Found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Create your first course to begin managing rosters and recording daily attendance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAddClassClick,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.padding(start = 4.dp))
            Text("Create Course")
        }
    }
}
