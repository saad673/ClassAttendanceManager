package com.example.data.model

data class StudentAttendanceSummary(
    val student: Student,
    val totalSessions: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int
) {
    val effectivePresent: Float
        get() = presentCount.toFloat() + (lateCount.toFloat() * 0.5f)

    val attendancePercentage: Float
        get() = if (totalSessions > 0) {
            ((presentCount + lateCount).toFloat() / totalSessions.toFloat()) * 100f
        } else {
            100f
        }

    fun isAtRisk(targetPercentage: Int): Boolean {
        return totalSessions > 0 && attendancePercentage < targetPercentage
    }
}

data class SessionWithStats(
    val session: AttendanceSession,
    val totalStudents: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int
) {
    val attendancePercentage: Float
        get() = if (totalStudents > 0) {
            ((presentCount + lateCount).toFloat() / totalStudents.toFloat()) * 100f
        } else {
            0f
        }
}

data class ClassWithStats(
    val courseClass: CourseClass,
    val studentCount: Int,
    val sessionCount: Int,
    val averageAttendancePercentage: Float,
    val atRiskStudentCount: Int
)

data class StudentSessionStatus(
    val session: AttendanceSession,
    val status: AttendanceStatus,
    val note: String
)
