package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSession
import com.example.data.model.Converters
import com.example.data.model.CourseClass
import com.example.data.model.Student

@Database(
    entities = [
        CourseClass::class,
        Student::class,
        AttendanceSession::class,
        AttendanceRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AttendanceDatabase : RoomDatabase() {

    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AttendanceDatabase? = null

        fun getDatabase(context: Context): AttendanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AttendanceDatabase::class.java,
                    "attendance_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
