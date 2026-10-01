package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AttendanceDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSession
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassWithStats
import com.example.data.model.CourseClass
import com.example.data.model.SessionWithStats
import com.example.data.model.Student
import com.example.data.model.StudentAttendanceSummary
import com.example.data.model.StudentSessionStatus
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen {
    data object MainTabs : Screen
    data class ClassDetail(val classId: Long, val initialTab: Int = 0) : Screen
    data class MarkAttendance(val classId: Long, val sessionId: Long? = null) : Screen
    data class StudentDetail(val studentId: Long) : Screen
}

enum class AppTab(val title: String) {
    CLASSES("Classes"),
    TAKE_ATTENDANCE("Roll Call"),
    STUDENTS("Students"),
    ANALYTICS("Reports")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    init {
        val db = AttendanceDatabase.getDatabase(application)
        repository = AttendanceRepository(db.attendanceDao())
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // Navigation and screen management
    val currentTab = MutableStateFlow(AppTab.CLASSES)
    val currentScreen = MutableStateFlow<Screen>(Screen.MainTabs)
    private val screenBackStack = mutableListOf<Screen>()

    fun navigateTo(screen: Screen) {
        screenBackStack.add(currentScreen.value)
        currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (screenBackStack.isNotEmpty()) {
            currentScreen.value = screenBackStack.removeAt(screenBackStack.size - 1)
            return true
        }
        return false
    }

    // Classes Flow
    val allClasses: StateFlow<List<CourseClass>> = repository.allClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<AttendanceSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudents: StateFlow<List<Student>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Class for detail view
    val selectedClassId = MutableStateFlow<Long?>(null)

    val activeClass: StateFlow<CourseClass?> = selectedClassId.flatMapLatest { id ->
        if (id != null) repository.getClassById(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeClassStudents: StateFlow<List<Student>> = selectedClassId.flatMapLatest { id ->
        if (id != null) repository.getStudentsForClass(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeClassSessions: StateFlow<List<SessionWithStats>> = selectedClassId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            combine(
                repository.getSessionsForClass(id),
                repository.getAllRecordsForClass(id),
                repository.getStudentsForClass(id)
            ) { sessions, records, students ->
                val recordsBySession = records.groupBy { it.sessionId }
                sessions.map { session ->
                    val recs = recordsBySession[session.id] ?: emptyList()
                    val pCount = recs.count { it.status == AttendanceStatus.PRESENT }
                    val aCount = recs.count { it.status == AttendanceStatus.ABSENT }
                    val lCount = recs.count { it.status == AttendanceStatus.LATE }
                    val eCount = recs.count { it.status == AttendanceStatus.EXCUSED }
                    SessionWithStats(
                        session = session,
                        totalStudents = students.size,
                        presentCount = pCount,
                        absentCount = aCount,
                        lateCount = lCount,
                        excusedCount = eCount
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeClassSummaries: StateFlow<List<StudentAttendanceSummary>> = selectedClassId.flatMapLatest { id ->
        if (id == null) {
            flowOf(emptyList())
        } else {
            combine(
                repository.getStudentsForClass(id),
                repository.getSessionsForClass(id),
                repository.getAllRecordsForClass(id)
            ) { students, sessions, records ->
                val recordsByStudent = records.groupBy { it.studentId }
                val totalSessions = sessions.size
                students.map { student ->
                    val sRecords = recordsByStudent[student.id] ?: emptyList()
                    StudentAttendanceSummary(
                        student = student,
                        totalSessions = totalSessions,
                        presentCount = sRecords.count { it.status == AttendanceStatus.PRESENT },
                        absentCount = sRecords.count { it.status == AttendanceStatus.ABSENT },
                        lateCount = sRecords.count { it.status == AttendanceStatus.LATE },
                        excusedCount = sRecords.count { it.status == AttendanceStatus.EXCUSED }
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All classes with live stats for Home cards
    val classesWithStats: StateFlow<List<ClassWithStats>> = combine(
        allClasses,
        allStudents,
        allSessions
    ) { classes, students, sessions ->
        val studentsByClass = students.groupBy { it.classId }
        val sessionsByClass = sessions.groupBy { it.classId }

        classes.map { course ->
            val cStudents = studentsByClass[course.id] ?: emptyList()
            val cSessions = sessionsByClass[course.id] ?: emptyList()
            // We compute overall quick stats
            ClassWithStats(
                courseClass = course,
                studentCount = cStudents.size,
                sessionCount = cSessions.size,
                averageAttendancePercentage = 85.0f, // will refine with records
                atRiskStudentCount = 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance Taking State Buffer
    val markAttendanceClassId = MutableStateFlow<Long?>(null)
    val markAttendanceSessionId = MutableStateFlow<Long?>(null)
    val markAttendanceDateMillis = MutableStateFlow(System.currentTimeMillis())
    val markAttendanceTitle = MutableStateFlow("")
    val markAttendanceRemarks = MutableStateFlow("")
    val markAttendanceStatuses = MutableStateFlow<Map<Long, AttendanceStatus>>(emptyMap())
    val markAttendanceNotes = MutableStateFlow<Map<Long, String>>(emptyMap())
    val markAttendanceFilterQuery = MutableStateFlow("")

    fun setupAttendanceTaking(classId: Long, sessionId: Long? = null) {
        markAttendanceClassId.value = classId
        markAttendanceSessionId.value = sessionId
        markAttendanceFilterQuery.value = ""

        viewModelScope.launch {
            val students = repository.getStudentsForClassOnce(classId)
            if (sessionId != null) {
                val session = repository.getSessionByIdOnce(sessionId)
                if (session != null) {
                    markAttendanceDateMillis.value = session.dateMillis
                    markAttendanceTitle.value = session.title
                    markAttendanceRemarks.value = session.remarks

                    val records = repository.getRecordsForSessionOnce(sessionId)
                    val statusMap = records.associate { it.studentId to it.status }.toMutableMap()
                    val noteMap = records.associate { it.studentId to it.note }.toMutableMap()
                    // Any unassigned student defaults to PRESENT
                    students.forEach { s ->
                        if (!statusMap.containsKey(s.id)) {
                            statusMap[s.id] = AttendanceStatus.PRESENT
                        }
                    }
                    markAttendanceStatuses.value = statusMap
                    markAttendanceNotes.value = noteMap
                }
            } else {
                markAttendanceDateMillis.value = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                markAttendanceTitle.value = "Class on " + dateFormat.format(Date())
                markAttendanceRemarks.value = ""

                // Default all students to PRESENT for quick marking
                val statusMap = students.associate { it.id to AttendanceStatus.PRESENT }
                markAttendanceStatuses.value = statusMap
                markAttendanceNotes.value = emptyMap()
            }
        }
    }

    fun setStudentStatus(studentId: Long, status: AttendanceStatus) {
        val current = markAttendanceStatuses.value.toMutableMap()
        current[studentId] = status
        markAttendanceStatuses.value = current
    }

    fun cycleStudentStatus(studentId: Long) {
        val currentStatus = markAttendanceStatuses.value[studentId] ?: AttendanceStatus.PRESENT
        val nextStatus = when (currentStatus) {
            AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
            AttendanceStatus.ABSENT -> AttendanceStatus.LATE
            AttendanceStatus.LATE -> AttendanceStatus.EXCUSED
            AttendanceStatus.EXCUSED -> AttendanceStatus.PRESENT
        }
        setStudentStatus(studentId, nextStatus)
    }

    fun markAll(status: AttendanceStatus) {
        val current = markAttendanceStatuses.value.toMutableMap()
        for (key in current.keys) {
            current[key] = status
        }
        markAttendanceStatuses.value = current
    }

    fun setStudentNote(studentId: Long, note: String) {
        val current = markAttendanceNotes.value.toMutableMap()
        current[studentId] = note
        markAttendanceNotes.value = current
    }

    fun saveAttendance(onSaved: (sessionId: Long) -> Unit) {
        val classId = markAttendanceClassId.value ?: return
        val title = markAttendanceTitle.value.ifBlank { "Regular Session" }
        val dateMillis = markAttendanceDateMillis.value
        val remarks = markAttendanceRemarks.value
        val existingSessionId = markAttendanceSessionId.value ?: 0L

        viewModelScope.launch {
            val session = AttendanceSession(
                id = existingSessionId,
                classId = classId,
                dateMillis = dateMillis,
                title = title,
                remarks = remarks
            )

            val records = markAttendanceStatuses.value.map { (studentId, status) ->
                AttendanceRecord(
                    sessionId = existingSessionId,
                    studentId = studentId,
                    status = status,
                    note = markAttendanceNotes.value[studentId] ?: ""
                )
            }

            val savedId = repository.saveSessionWithRecords(session, records)
            withContext(Dispatchers.Main) {
                onSaved(savedId)
            }
        }
    }

    // Class Management
    fun createClass(
        name: String,
        code: String,
        section: String,
        roomNumber: String,
        targetPercent: Int,
        colorHex: String,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newClass = CourseClass(
                name = name,
                code = code,
                section = section,
                roomNumber = roomNumber,
                targetAttendancePercent = targetPercent,
                colorHex = colorHex
            )
            val id = repository.insertClass(newClass)
            withContext(Dispatchers.Main) {
                onCreated(id)
            }
        }
    }

    fun updateClass(courseClass: CourseClass) {
        viewModelScope.launch {
            repository.updateClass(courseClass)
        }
    }

    fun deleteClass(courseClass: CourseClass) {
        viewModelScope.launch {
            repository.deleteClass(courseClass)
            if (selectedClassId.value == courseClass.id) {
                selectedClassId.value = null
                currentScreen.value = Screen.MainTabs
            }
        }
    }

    // Student Management
    fun addStudent(classId: Long, rollNumber: String, name: String, email: String, phone: String) {
        viewModelScope.launch {
            repository.insertStudent(
                Student(
                    classId = classId,
                    rollNumber = rollNumber.trim(),
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim()
                )
            )
        }
    }

    fun bulkImportStudents(classId: Long, rawText: String, onImported: (Int) -> Unit) {
        viewModelScope.launch {
            val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
            val parsedStudents = mutableListOf<Student>()

            for (line in lines) {
                // Support formats: "RollNo, Name" or "RollNo \t Name" or "RollNo - Name"
                val parts = when {
                    line.contains(",") -> line.split(",", limit = 2)
                    line.contains("\t") -> line.split("\t", limit = 2)
                    line.contains(" - ") -> line.split(" - ", limit = 2)
                    line.contains(" ") -> {
                        val firstSpace = line.indexOf(' ')
                        listOf(line.substring(0, firstSpace), line.substring(firstSpace + 1))
                    }
                    else -> listOf(line, "Student ${parsedStudents.size + 1}")
                }

                val roll = parts[0].trim()
                val name = if (parts.size > 1) parts[1].trim() else "Student $roll"

                parsedStudents.add(
                    Student(
                        classId = classId,
                        rollNumber = roll,
                        name = name
                    )
                )
            }

            if (parsedStudents.isNotEmpty()) {
                repository.insertStudents(parsedStudents)
            }

            withContext(Dispatchers.Main) {
                onImported(parsedStudents.size)
            }
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    fun deleteSession(session: AttendanceSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    // Export CSV
    fun generateExportCsv(classId: Long, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportAttendanceCsv(classId)
            withContext(Dispatchers.Main) {
                onReady(csv)
            }
        }
    }

    // Student Detail History
    fun getStudentSessionHistory(studentId: Long): StateFlow<List<StudentSessionStatus>> {
        val result = MutableStateFlow<List<StudentSessionStatus>>(emptyList())
        viewModelScope.launch {
            val records = repository.getRecordsForStudentOnce(studentId)
            val sessionIds = records.map { it.sessionId }
            val list = mutableListOf<StudentSessionStatus>()
            for (record in records) {
                val session = repository.getSessionByIdOnce(record.sessionId)
                if (session != null) {
                    list.add(
                        StudentSessionStatus(
                            session = session,
                            status = record.status,
                            note = record.note
                        )
                    )
                }
            }
            result.value = list.sortedByDescending { it.session.dateMillis }
        }
        return result
    }
}
