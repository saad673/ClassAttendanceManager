package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class CourseClass(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val section: String = "",
    val roomNumber: String = "",
    val targetAttendancePercent: Int = 75,
    val colorHex: String = "#4F46E5",
    val createdAt: Long = System.currentTimeMillis()
)
