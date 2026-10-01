package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CourseClass
import kotlin.math.roundToInt

private val COLOR_PALETTE = listOf(
    "#2563EB", // Blue
    "#0D9488", // Teal
    "#7C3AED", // Violet
    "#E11D48", // Rose
    "#D97706", // Amber
    "#059669", // Emerald
    "#0284C7", // Sky
    "#4F46E5"  // Indigo
)

@Composable
fun AddEditClassDialog(
    initialClass: CourseClass? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, section: String, room: String, targetPercent: Int, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initialClass?.name ?: "") }
    var code by remember { mutableStateOf(initialClass?.code ?: "") }
    var section by remember { mutableStateOf(initialClass?.section ?: "") }
    var roomNumber by remember { mutableStateOf(initialClass?.roomNumber ?: "") }
    var targetPercent by remember {
        mutableFloatStateOf(initialClass?.targetAttendancePercent?.toFloat() ?: 75f)
    }
    var selectedColor by remember {
        mutableStateOf(initialClass?.colorHex ?: COLOR_PALETTE[0])
    }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialClass == null) "Create New Course" else "Edit Course",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Course Name *") },
                    placeholder = { Text("e.g. Data Structures") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Course name is required") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("class_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code") },
                        placeholder = { Text("CS201") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("class_code_input")
                    )

                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        placeholder = { Text("Sec A") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("class_section_input")
                    )
                }

                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("Room / Hall") },
                    placeholder = { Text("Room 304") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Target attendance requirement
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Attendance",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${targetPercent.roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = targetPercent,
                        onValueChange = { targetPercent = it },
                        valueRange = 50f..100f,
                        steps = 9
                    )
                }

                // Color picker
                Column {
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        COLOR_PALETTE.forEach { hex ->
                            val color = remember(hex) {
                                try { Color(android.graphics.Color.parseColor(hex)) }
                                catch (_: Exception) { Color.Blue }
                            }
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = hex }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else Modifier
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(
                            name.trim(),
                            code.trim(),
                            section.trim(),
                            roomNumber.trim(),
                            targetPercent.roundToInt(),
                            selectedColor
                        )
                    }
                },
                modifier = Modifier.testTag("save_class_btn")
            ) {
                Text(if (initialClass == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
