package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun AllStudentsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val allClasses by viewModel.allClasses.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterClassId by remember { mutableStateOf<Long?>(null) }

    val classMap = remember(allClasses) {
        allClasses.associateBy { it.id }
    }

    val filteredStudents = remember(allStudents, searchQuery, selectedFilterClassId) {
        allStudents.filter { student ->
            val matchesClass = selectedFilterClassId == null || student.classId == selectedFilterClassId
            val matchesQuery = searchQuery.isBlank() ||
                    student.name.contains(searchQuery, ignoreCase = true) ||
                    student.rollNumber.contains(searchQuery, ignoreCase = true)
            matchesClass && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Student Directory",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "${allStudents.size} enrolled students across ${allClasses.size} courses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name or roll number...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // Class Filter Chips
        if (allClasses.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterClassId == null,
                        onClick = { selectedFilterClassId = null },
                        label = { Text("All Courses") }
                    )
                }
                items(allClasses, key = { it.id }) { course ->
                    FilterChip(
                        selected = selectedFilterClassId == course.id,
                        onClick = {
                            selectedFilterClassId = if (selectedFilterClassId == course.id) null else course.id
                        },
                        label = { Text(course.code.ifBlank { course.name }) }
                    )
                }
            }
        }

        // Students List
        if (filteredStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No students match your search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStudents, key = { it.id }) { student ->
                    val course = classMap[student.classId]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.navigateTo(Screen.StudentDetail(student.id))
                            },
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

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                course?.let {
                                    Text(
                                        text = "${it.name} (${it.code})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "View Profile",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
