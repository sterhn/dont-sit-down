package com.dontsitdown.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dontsitdown.data.model.ChainResult
import com.dontsitdown.data.model.ReminderEvent
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskCompletion

@Database(
    entities = [Task::class, ChainResult::class, TaskCompletion::class, ReminderEvent::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun chainResultDao(): ChainResultDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "dont-sit-down.db").build()
    }
}
