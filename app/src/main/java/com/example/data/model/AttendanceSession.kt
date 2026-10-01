package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CourseClass::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId"), Index("dateMillis")]
)
data class AttendanceSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classId: Long,
    val dateMillis: Long,
    val title: String,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
