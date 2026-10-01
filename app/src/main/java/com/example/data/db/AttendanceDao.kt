package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSession
import com.example.data.model.CourseClass
import com.example.data.model.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // --- Classes ---
    @Query("SELECT * FROM classes ORDER BY createdAt DESC")
    fun getAllClasses(): Flow<List<CourseClass>>

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    fun getClassById(id: Long): Flow<CourseClass?>

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    suspend fun getClassByIdOnce(id: Long): CourseClass?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClass(courseClass: CourseClass): Long

    @Update
    suspend fun updateClass(courseClass: CourseClass)

    @Delete
    suspend fun deleteClass(courseClass: CourseClass)

    // --- Students ---
    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY rollNumber ASC, name ASC")
    fun getStudentsForClass(classId: Long): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE classId = :classId ORDER BY rollNumber ASC, name ASC")
    suspend fun getStudentsForClassOnce(classId: Long): List<Student>

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    fun getStudentById(id: Long): Flow<Student?>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentByIdOnce(id: Long): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>): List<Long>

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    // --- Sessions ---
    @Query("SELECT * FROM attendance_sessions WHERE classId = :classId ORDER BY dateMillis DESC, id DESC")
    fun getSessionsForClass(classId: Long): Flow<List<AttendanceSession>>

    @Query("SELECT * FROM attendance_sessions WHERE classId = :classId ORDER BY dateMillis DESC, id DESC")
    suspend fun getSessionsForClassOnce(classId: Long): List<AttendanceSession>

    @Query("SELECT * FROM attendance_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<AttendanceSession>>

    @Query("SELECT * FROM attendance_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: Long): Flow<AttendanceSession?>

    @Query("SELECT * FROM attendance_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdOnce(id: Long): AttendanceSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSession): Long

    @Update
    suspend fun updateSession(session: AttendanceSession)

    @Delete
    suspend fun deleteSession(session: AttendanceSession)

    // --- Records ---
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    fun getRecordsForSession(sessionId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun getRecordsForSessionOnce(sessionId: Long): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    fun getRecordsForStudent(studentId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    suspend fun getRecordsForStudentOnce(studentId: Long): List<AttendanceRecord>

    @Query("SELECT r.* FROM attendance_records r INNER JOIN attendance_sessions s ON r.sessionId = s.id WHERE s.classId = :classId")
    fun getAllRecordsForClass(classId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT r.* FROM attendance_records r INNER JOIN attendance_sessions s ON r.sessionId = s.id WHERE s.classId = :classId")
    suspend fun getAllRecordsForClassOnce(classId: Long): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecords(records: List<AttendanceRecord>)

    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun deleteRecordsForSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM classes")
    suspend fun getClassCount(): Int
}
