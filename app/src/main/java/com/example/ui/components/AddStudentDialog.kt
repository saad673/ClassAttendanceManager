package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Student

@Composable
fun AddStudentDialog(
    initialStudent: Student? = null,
    onDismiss: () -> Unit,
    onAddSingle: (rollNumber: String, name: String, email: String, phone: String) -> Unit,
    onBulkImport: (rawText: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Single, 1: Bulk

    // Single student state
    var rollNumber by remember { mutableStateOf(initialStudent?.rollNumber ?: "") }
    var name by remember { mutableStateOf(initialStudent?.name ?: "") }
    var email by remember { mutableStateOf(initialStudent?.email ?: "") }
    var phone by remember { mutableStateOf(initialStudent?.phone ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var rollError by remember { mutableStateOf(false) }

    // Bulk import state
    var bulkText by remember {
        mutableStateOf(
            "101, Alice Walker\n102, Bob Smith\n103, Charlie Brown\n104, Diana Prince\n105, Ethan Hunt"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialStudent == null) "Add Students" else "Edit Student",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (initialStudent == null) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Single Student") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Bulk Import (CSV)") }
                        )
                    }
                }

                if (selectedTab == 0) {
                    // Single Student Form
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = rollNumber,
                            onValueChange = {
                                rollNumber = it
                                if (it.isNotBlank()) rollError = false
                            },
                            label = { Text("Roll Number / Student ID *") },
                            placeholder = { Text("e.g. 2024-041 or 101") },
                            isError = rollError,
                            supportingText = if (rollError) {
                                { Text("Roll number is required") }
                            } else null,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_roll_input")
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (it.isNotBlank()) nameError = false
                            },
                            label = { Text("Full Name *") },
                            placeholder = { Text("e.g. Jane Doe") },
                            isError = nameError,
                            supportingText = if (nameError) {
                                { Text("Student name is required") }
                            } else null,
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_name_input")
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email (Optional)") },
                            placeholder = { Text("student@university.edu") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone (Optional)") },
                            placeholder = { Text("+1 555-0199") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Bulk Import Form
                    Column {
                        Text(
                            text = "Paste your student roster list. Each line can be \"RollNo, Name\" or tab-separated:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bulkText,
                            onValueChange = { bulkText = it },
                            label = { Text("Roster Data") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        val lineCount = remember(bulkText) {
                            bulkText.lines().count { it.isNotBlank() }
                        }
                        Text(
                            text = "$lineCount student(s) detected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        var hasError = false
                        if (rollNumber.isBlank()) {
                            rollError = true
                            hasError = true
                        }
                        if (name.isBlank()) {
                            nameError = true
                            hasError = true
                        }
                        if (!hasError) {
                            onAddSingle(rollNumber.trim(), name.trim(), email.trim(), phone.trim())
                        }
                    } else {
                        if (bulkText.isNotBlank()) {
                            onBulkImport(bulkText)
                        }
                    }
                },
                modifier = Modifier.testTag("confirm_student_btn")
            ) {
                Text(if (selectedTab == 0) "Save Student" else "Import Students")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
