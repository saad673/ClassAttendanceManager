package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CourseClass
import com.example.ui.screens.AllStudentsScreen
import com.example.ui.screens.AnalyticsOverviewScreen
import com.example.ui.screens.ClassDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarkAttendanceScreen
import com.example.ui.screens.StudentDetailScreen
import com.example.ui.theme.AttendanceManagerTheme
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendanceManagerTheme {
                val viewModel: AttendanceViewModel = viewModel()
                MainAppNavHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppNavHost(viewModel: AttendanceViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    when (val screen = currentScreen) {
        is Screen.MainTabs -> {
            MainTabsScaffold(viewModel = viewModel)
        }
        is Screen.ClassDetail -> {
            ClassDetailScreen(
                viewModel = viewModel,
                classId = screen.classId,
                initialTab = screen.initialTab,
                onBack = { viewModel.navigateBack() }
            )
        }
        is Screen.MarkAttendance -> {
            MarkAttendanceScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateBack() }
            )
        }
        is Screen.StudentDetail -> {
            StudentDetailScreen(
                viewModel = viewModel,
                studentId = screen.studentId,
                onBack = { viewModel.navigateBack() }
            )
        }
    }
}

@Composable
fun MainTabsScaffold(viewModel: AttendanceViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.CLASSES,
                    onClick = { viewModel.currentTab.value = AppTab.CLASSES },
                    icon = { Icon(Icons.Default.School, contentDescription = "Courses") },
                    label = { Text("Courses") },
                    modifier = Modifier.testTag("nav_courses")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.TAKE_ATTENDANCE,
                    onClick = { viewModel.currentTab.value = AppTab.TAKE_ATTENDANCE },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Roll Call") },
                    label = { Text("Roll Call") },
                    modifier = Modifier.testTag("nav_roll_call")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.STUDENTS,
                    onClick = { viewModel.currentTab.value = AppTab.STUDENTS },
                    icon = { Icon(Icons.Default.People, contentDescription = "Students") },
                    label = { Text("Students") },
                    modifier = Modifier.testTag("nav_students")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.ANALYTICS,
                    onClick = { viewModel.currentTab.value = AppTab.ANALYTICS },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    modifier = Modifier.testTag("nav_reports")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                AppTab.CLASSES -> HomeScreen(viewModel = viewModel)
                AppTab.TAKE_ATTENDANCE -> SelectClassForRollCallTab(viewModel = viewModel)
                AppTab.STUDENTS -> AllStudentsScreen(viewModel = viewModel)
                AppTab.ANALYTICS -> AnalyticsOverviewScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectClassForRollCallTab(viewModel: AttendanceViewModel) {
    val classes by viewModel.allClasses.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Course for Roll Call",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Choose a course to start today's attendance roll call",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (classes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No courses available. Please add a course first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(classes, key = { it.id }) { course ->
                    Card(
                        onClick = {
                            viewModel.setupAttendanceTaking(course.id)
                            viewModel.navigateTo(Screen.MarkAttendance(course.id))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_course_${course.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${course.code}  •  ${course.section.ifBlank { "Regular" }}  •  ${course.roomNumber.ifBlank { "Standard Hall" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
