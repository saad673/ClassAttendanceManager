package com.example.data.repository

import com.example.data.db.AttendanceDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSession
import com.example.data.model.AttendanceStatus
import com.example.data.model.CourseClass
import com.example.data.model.Student
import com.example.data.model.StudentAttendanceSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceRepository(private val dao: AttendanceDao) {

    val allClasses: Flow<List<CourseClass>> = dao.getAllClasses()
    val allSessions: Flow<List<AttendanceSession>> = dao.getAllSessions()
    val allStudents: Flow<List<Student>> = dao.getAllStudents()

    fun getClassById(id: Long): Flow<CourseClass?> = dao.getClassById(id)
    suspend fun getClassByIdOnce(id: Long): CourseClass? = dao.getClassByIdOnce(id)

    suspend fun insertClass(courseClass: CourseClass): Long = dao.insertClass(courseClass)
    suspend fun updateClass(courseClass: CourseClass) = dao.updateClass(courseClass)
    suspend fun deleteClass(courseClass: CourseClass) = dao.deleteClass(courseClass)

    fun getStudentsForClass(classId: Long): Flow<List<Student>> = dao.getStudentsForClass(classId)
    suspend fun getStudentsForClassOnce(classId: Long): List<Student> = dao.getStudentsForClassOnce(classId)
    fun getStudentById(id: Long): Flow<Student?> = dao.getStudentById(id)
    suspend fun getStudentByIdOnce(id: Long): Student? = dao.getStudentByIdOnce(id)

    suspend fun insertStudent(student: Student): Long = dao.insertStudent(student)
    suspend fun insertStudents(students: List<Student>): List<Long> = dao.insertStudents(students)
    suspend fun updateStudent(student: Student) = dao.updateStudent(student)
    suspend fun deleteStudent(student: Student) = dao.deleteStudent(student)

    fun getSessionsForClass(classId: Long): Flow<List<AttendanceSession>> = dao.getSessionsForClass(classId)
    suspend fun getSessionsForClassOnce(classId: Long): List<AttendanceSession> = dao.getSessionsForClassOnce(classId)
    fun getSessionById(id: Long): Flow<AttendanceSession?> = dao.getSessionById(id)
    suspend fun getSessionByIdOnce(id: Long): AttendanceSession? = dao.getSessionByIdOnce(id)

    suspend fun insertSession(session: AttendanceSession): Long = dao.insertSession(session)
    suspend fun updateSession(session: AttendanceSession) = dao.updateSession(session)
    suspend fun deleteSession(session: AttendanceSession) = dao.deleteSession(session)

    fun getRecordsForSession(sessionId: Long): Flow<List<AttendanceRecord>> = dao.getRecordsForSession(sessionId)
    suspend fun getRecordsForSessionOnce(sessionId: Long): List<AttendanceRecord> = dao.getRecordsForSessionOnce(sessionId)
    fun getRecordsForStudent(studentId: Long): Flow<List<AttendanceRecord>> = dao.getRecordsForStudent(studentId)
    suspend fun getRecordsForStudentOnce(studentId: Long): List<AttendanceRecord> = dao.getRecordsForStudentOnce(studentId)

    fun getAllRecordsForClass(classId: Long): Flow<List<AttendanceRecord>> = dao.getAllRecordsForClass(classId)
    suspend fun getAllRecordsForClassOnce(classId: Long): List<AttendanceRecord> = dao.getAllRecordsForClassOnce(classId)

    suspend fun saveSessionWithRecords(
        session: AttendanceSession,
        records: List<AttendanceRecord>
    ): Long = withContext(Dispatchers.IO) {
        val sessionId = if (session.id == 0L) {
            dao.insertSession(session)
        } else {
            dao.updateSession(session)
            session.id
        }
        val recordsToSave = records.map { it.copy(sessionId = sessionId) }
        dao.insertOrUpdateRecords(recordsToSave)
        sessionId
    }

    suspend fun computeStudentSummariesForClass(classId: Long): List<StudentAttendanceSummary> =
        withContext(Dispatchers.IO) {
            val students = dao.getStudentsForClassOnce(classId)
            val sessions = dao.getSessionsForClassOnce(classId)
            val records = dao.getAllRecordsForClassOnce(classId)

            val recordsByStudent = records.groupBy { it.studentId }
            val totalSessionsCount = sessions.size

            students.map { student ->
                val studentRecords = recordsByStudent[student.id] ?: emptyList()
                val present = studentRecords.count { it.status == AttendanceStatus.PRESENT }
                val absent = studentRecords.count { it.status == AttendanceStatus.ABSENT }
                val late = studentRecords.count { it.status == AttendanceStatus.LATE }
                val excused = studentRecords.count { it.status == AttendanceStatus.EXCUSED }

                StudentAttendanceSummary(
                    student = student,
                    totalSessions = totalSessionsCount,
                    presentCount = present,
                    absentCount = absent,
                    lateCount = late,
                    excusedCount = excused
                )
            }
        }

    suspend fun exportAttendanceCsv(classId: Long): String = withContext(Dispatchers.IO) {
        val course = dao.getClassByIdOnce(classId) ?: return@withContext ""
        val students = dao.getStudentsForClassOnce(classId)
        val sessions = dao.getSessionsForClassOnce(classId).sortedBy { it.dateMillis }
        val records = dao.getAllRecordsForClassOnce(classId)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val recordsMap = records.associateBy { "${it.sessionId}_${it.studentId}" }

        val sb = StringBuilder()
        sb.appendLine("Class Attendance Report")
        sb.appendLine("Course: \"${course.name}\", Code: \"${course.code}\", Section: \"${course.section}\"")
        sb.appendLine("Generated: ${dateFormat.format(Date())}")
        sb.appendLine()

        // 1. Summary Section
        sb.appendLine("--- STUDENT ATTENDANCE SUMMARY ---")
        sb.appendLine("Roll No,Student Name,Total Sessions,Present,Absent,Late,Excused,Attendance %,Status")
        val summaries = computeStudentSummariesForClass(classId)
        for (sum in summaries) {
            val statusLabel = if (sum.isAtRisk(course.targetAttendancePercent)) "AT RISK (<${course.targetAttendancePercent}%)" else "NORMAL"
            val pctStr = String.format(Locale.US, "%.1f%%", sum.attendancePercentage)
            sb.appendLine("\"${sum.student.rollNumber}\",\"${sum.student.name}\",${sum.totalSessions},${sum.presentCount},${sum.absentCount},${sum.lateCount},${sum.excusedCount},$pctStr,\"$statusLabel\"")
        }

        // 2. Date-by-Date Attendance Matrix
        if (sessions.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("--- DATE-WISE ATTENDANCE MATRIX ---")
            val sessionHeaders = sessions.joinToString(",") { session ->
                val dateStr = dateFormat.format(Date(session.dateMillis))
                "\"$dateStr (${session.title.replace("\"", "\"\"")})\""
            }
            sb.appendLine("Roll No,Student Name,$sessionHeaders")

            for (student in students) {
                val rowValues = sessions.map { session ->
                    val record = recordsMap["${session.id}_${student.id}"]
                    when (record?.status) {
                        AttendanceStatus.PRESENT -> "P"
                        AttendanceStatus.ABSENT -> "A"
                        AttendanceStatus.LATE -> "L"
                        AttendanceStatus.EXCUSED -> "E"
                        null -> "-"
                    }
                }.joinToString(",")
                sb.appendLine("\"${student.rollNumber}\",\"${student.name}\",$rowValues")
            }
        }

        sb.toString()
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getClassCount() > 0) return@withContext

        // Preload sample classes with students and sessions for instant rich preview!
        val class1Id = dao.insertClass(
            CourseClass(
                name = "Data Structures & Algorithms",
                code = "CS201",
                section = "Section A",
                roomNumber = "Lab 302",
                targetAttendancePercent = 75,
                colorHex = "#2563EB"
            )
        )

        val class2Id = dao.insertClass(
            CourseClass(
                name = "Computer Networks",
                code = "CS305",
                section = "Morning",
                roomNumber = "Room 408",
                targetAttendancePercent = 80,
                colorHex = "#0D9488"
            )
        )

        val class3Id = dao.insertClass(
            CourseClass(
                name = "Mobile App Engineering",
                code = "SE410",
                section = "Cohort 2",
                roomNumber = "Tech Hub 1",
                targetAttendancePercent = 75,
                colorHex = "#7C3AED"
            )
        )

        // Students for CS201
        val cs201Students = listOf(
            Student(classId = class1Id, rollNumber = "CS-01", name = "Alex Johnson", email = "alex.j@example.edu"),
            Student(classId = class1Id, rollNumber = "CS-02", name = "Emma Watson", email = "emma.w@example.edu"),
            Student(classId = class1Id, rollNumber = "CS-03", name = "Lucas Garcia", email = "lucas.g@example.edu"),
            Student(classId = class1Id, rollNumber = "CS-04", name = "Olivia Chen", email = "olivia.c@example.edu"),
            Student(classId = class1Id, rollNumber = "CS-05", name = "Noah Miller", email = "noah.m@example.edu"),
            Student(classId = class1Id, rollNumber = "CS-06", name = "Sophia Rodriguez", email = "sophia.r@example.edu"),
            Student(classId = class1Id, rollNumber = "CS-07", name = "Liam Taylor", email = "liam.t@example.edu"),
            Student(classId = class1Id, rollNumber = "CS-08", name = "Ava Martinez", email = "ava.m@example.edu")
        )
        val sIds = cs201Students.map { dao.insertStudent(it) }

        // Sessions for CS201
        val dayMillis = 86400000L
        val now = System.currentTimeMillis()

        val session1Id = dao.insertSession(
            AttendanceSession(
                classId = class1Id,
                dateMillis = now - (dayMillis * 4),
                title = "Lecture 1: Arrays & Memory Models"
            )
        )
        val session2Id = dao.insertSession(
            AttendanceSession(
                classId = class1Id,
                dateMillis = now - (dayMillis * 2),
                title = "Lecture 2: Singly & Doubly Linked Lists"
            )
        )
        val session3Id = dao.insertSession(
            AttendanceSession(
                classId = class1Id,
                dateMillis = now - (dayMillis * 1),
                title = "Lab 1: Stack & Queue Implementations"
            )
        )

        // Records for session 1 (mostly present)
        val recs1 = sIds.mapIndexed { idx, sId ->
            AttendanceRecord(
                sessionId = session1Id,
                studentId = sId,
                status = if (idx == 4) AttendanceStatus.ABSENT else AttendanceStatus.PRESENT
            )
        }
        dao.insertOrUpdateRecords(recs1)

        // Records for session 2
        val recs2 = sIds.mapIndexed { idx, sId ->
            AttendanceRecord(
                sessionId = session2Id,
                studentId = sId,
                status = when (idx) {
                    2 -> AttendanceStatus.LATE
                    4 -> AttendanceStatus.ABSENT
                    7 -> AttendanceStatus.EXCUSED
                    else -> AttendanceStatus.PRESENT
                }
            )
        }
        dao.insertOrUpdateRecords(recs2)

        // Records for session 3
        val recs3 = sIds.mapIndexed { idx, sId ->
            AttendanceRecord(
                sessionId = session3Id,
                studentId = sId,
                status = when (idx) {
                    4 -> AttendanceStatus.ABSENT // Noah is at risk (<75%)
                    6 -> AttendanceStatus.LATE
                    else -> AttendanceStatus.PRESENT
                }
            )
        }
        dao.insertOrUpdateRecords(recs3)

        // Students for CS305
        val cs305Students = listOf(
            Student(classId = class2Id, rollNumber = "NET-101", name = "David Kim", email = "david.k@example.edu"),
            Student(classId = class2Id, rollNumber = "NET-102", name = "Mia Anderson", email = "mia.a@example.edu"),
            Student(classId = class2Id, rollNumber = "NET-103", name = "Ethan Brown", email = "ethan.b@example.edu"),
            Student(classId = class2Id, rollNumber = "NET-104", name = "Isabella Patel", email = "isabella.p@example.edu")
        )
        cs305Students.forEach { dao.insertStudent(it) }
    }
}
