package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = CourseClass::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId"), Index(value = ["classId", "rollNumber"], unique = true)]
)
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val classId: Long,
    val rollNumber: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val notes: String = ""
)
