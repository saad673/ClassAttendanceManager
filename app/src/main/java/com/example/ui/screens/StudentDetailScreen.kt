package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceStatus
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusAtRisk
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import com.example.ui.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    viewModel: AttendanceViewModel,
    studentId: Long,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val student = remember(allStudents, studentId) {
        allStudents.firstOrNull { it.id == studentId }
    }

    val historyFlow = remember(studentId) {
        viewModel.getStudentSessionHistory(studentId)
    }
    val sessionHistory by historyFlow.collectAsStateWithLifecycle()

    val totalSessions = sessionHistory.size
    val presentCount = sessionHistory.count { it.status == AttendanceStatus.PRESENT }
    val absentCount = sessionHistory.count { it.status == AttendanceStatus.ABSENT }
    val lateCount = sessionHistory.count { it.status == AttendanceStatus.LATE }
    val excusedCount = sessionHistory.count { it.status == AttendanceStatus.EXCUSED }

    val attendancePct = if (totalSessions > 0) {
        ((presentCount + lateCount).toFloat() / totalSessions.toFloat()) * 100f
    } else 100f

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(student?.name ?: "Student Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (student == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Student not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Profile Card with Gauge
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
                            // Circular Gauge
                            Box(
                                modifier = Modifier.size(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val strokeWidth = 14.dp
                                val progress = (attendancePct / 100f).coerceIn(0f, 1f)
                                val gaugeColor = if (attendancePct >= 75f) StatusPresent else StatusAtRisk

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Background circle
                                    drawCircle(
                                        color = gaugeColor.copy(alpha = 0.15f),
                                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                                    )
                                    // Foreground arc
                                    drawArc(
                                        color = gaugeColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progress,
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format(Locale.US, "%.0f%%", attendancePct),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = gaugeColor
                                    )
                                    Text(
                                        text = if (attendancePct >= 75f) "Normal" else "At Risk",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = student.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Roll: ${student.rollNumber}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Contact buttons
                            if (student.email.isNotBlank() || student.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (student.email.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:${student.email}")
                                                    putExtra(Intent.EXTRA_SUBJECT, "Attendance Status Update")
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Send Email"))
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Email", fontSize = 12.sp)
                                        }
                                    }
                                    if (student.phone.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${student.phone}")
                                                }
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Call", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Stats Breakdown Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudentStatCard("Present", presentCount.toString(), StatusPresent, Modifier.weight(1f))
                        StudentStatCard("Absent", absentCount.toString(), StatusAbsent, Modifier.weight(1f))
                        StudentStatCard("Late", lateCount.toString(), StatusLate, Modifier.weight(1f))
                        StudentStatCard("Excused", excusedCount.toString(), StatusExcused, Modifier.weight(1f))
                    }
                }

                // Session History Header
                item {
                    Text(
                        text = "Session History (${sessionHistory.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (sessionHistory.isEmpty()) {
                    item {
                        Text(
                            text = "No sessions attended or recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(sessionHistory) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateFormat.format(Date(item.session.dateMillis)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.session.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (item.note.isNotBlank()) {
                                        Text(
                                            text = "Note: ${item.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                AttendanceStatusBadge(status = item.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentStatCard(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
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
