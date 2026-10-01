package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class AttendanceStatus(val label: String, val shortLabel: String) {
    PRESENT("Present", "P"),
    ABSENT("Absent", "A"),
    LATE("Late", "L"),
    EXCUSED("Excused", "E");

    companion object {
        fun fromString(value: String?): AttendanceStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PRESENT
        }
    }
}

class Converters {
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus?): String {
        return status?.name ?: AttendanceStatus.PRESENT.name
    }

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus {
        return AttendanceStatus.fromString(value)
    }
}

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = AttendanceSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("studentId"),
        Index(value = ["sessionId", "studentId"], unique = true)
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val studentId: Long,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val note: String = ""
)
