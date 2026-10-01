package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Student
import com.example.data.model.StudentAttendanceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Attendance", appName)
    }

    @Test
    fun `test student attendance percentage calculation`() {
        val student = Student(id = 1L, classId = 1L, rollNumber = "101", name = "Test Student")
        val summary = StudentAttendanceSummary(
            student = student,
            totalSessions = 10,
            presentCount = 8,
            absentCount = 2,
            lateCount = 0,
            excusedCount = 0
        )
        assertEquals(80.0f, summary.attendancePercentage, 0.01f)
        assertFalse(summary.isAtRisk(75))
    }

    @Test
    fun `test at risk threshold identification`() {
        val student = Student(id = 2L, classId = 1L, rollNumber = "102", name = "At Risk Student")
        val summary = StudentAttendanceSummary(
            student = student,
            totalSessions = 10,
            presentCount = 6,
            absentCount = 4,
            lateCount = 0,
            excusedCount = 0
        )
        assertEquals(60.0f, summary.attendancePercentage, 0.01f)
        assertTrue(summary.isAtRisk(75))
    }
}
