package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExportDialog
import com.example.ui.components.PercentageBadge
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.Screen
import java.util.Locale

@Composable
fun AnalyticsOverviewScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val allClasses by viewModel.allClasses.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    var selectedClassId by remember { mutableStateOf<Long?>(null) }
    var exportCsvData by remember { mutableStateOf<Pair<String, String>?>(null) }

    val currentClass = remember(allClasses, selectedClassId) {
        allClasses.firstOrNull { it.id == selectedClassId } ?: allClasses.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Attendance Reports",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "Class summaries & performance insights",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Class Selection Filter
        if (allClasses.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allClasses, key = { it.id }) { course ->
                    val isSelected = (currentClass?.id == course.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedClassId = course.id },
                        label = { Text(course.code.ifBlank { course.name }) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview KPI Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Total Courses",
                        value = allClasses.size.toString(),
                        subtitle = "Active this term",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Total Sessions",
                        value = allSessions.size.toString(),
                        subtitle = "Roll calls logged",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Visual Donut Chart Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentClass?.name ?: "Course Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        currentClass?.let {
                            Text(
                                text = "Target: ${it.targetAttendancePercent}% Attendance Minimum",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Donut Representation
                        Box(
                            modifier = Modifier.size(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val strokeWidth = 16.dp
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // 78% Present, 12% Absent, 6% Late, 4% Excused representation
                                drawArc(
                                    color = StatusPresent,
                                    startAngle = -90f,
                                    sweepAngle = 280f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = StatusLate,
                                    startAngle = 190f,
                                    sweepAngle = 30f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = StatusAbsent,
                                    startAngle = 220f,
                                    sweepAngle = 50f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "86.5%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Class Average",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Color Legends
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatusLegend(label = "Present", color = StatusPresent)
                            StatusLegend(label = "Absent", color = StatusAbsent)
                            StatusLegend(label = "Late", color = StatusLate)
                            StatusLegend(label = "Excused", color = StatusExcused)
                        }

                        currentClass?.let { course ->
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    viewModel.generateExportCsv(course.id) { csv ->
                                        exportCsvData = (course.name to csv)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Attendance Register (CSV)")
                            }
                        }
                    }
                }
            }
        }
    }

    // Export Dialog
    exportCsvData?.let { (title, csv) ->
        ExportDialog(
            csvContent = csv,
            courseTitle = title,
            onDismiss = { exportCsvData = null }
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}
